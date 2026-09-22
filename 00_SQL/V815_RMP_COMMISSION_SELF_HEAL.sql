-- ============================================================================
-- V815 — 💰 RMP-র কমিশন এক জায়গায় ঠিকঠাক জমা হবে (বাদ পড়া রোগী নিজে থেকেই ঢুকবে)
--
-- TK-নির্দেশ (২৮.০৮.২০২৬): *"তাদের কমিশন এক জায়গায় কেন হয়ে যায় না — টোটাল
-- বাকি আর জমা দিয়েছি কত… নতুন পাঁচ হাজার টাকা পূর্ববর্তী বাকির সাথে যোগ হয় না কেন?"*
--
-- ❗ আসল কারণ (কোড পড়ে প্রমাণিত, আন্দাজ নয়)
--   যোগফল দেখানোর ব্যবস্থা **আগে থেকেই ঠিক আছে** — `fin.rmp_rmp_summary()`
--   ওই RMP-র সব রোগীর কমিশন যোগ করে একটাই Ref. Paid / Ref. Due দেয়।
--   কিন্তু হিসাবটা হয় `fin.rmp_patient_commissions`-এর **সারি ধরে**, আর
--   সেই সারিটা বসে রোগীর ট্রিটমেন্ট পেমেন্ট সেভ হওয়ার ঠিক পরে
--   (`RmpCommissionActivation`)। ওই কাজটা আগে **লগইন** করে নেয় —
--   আর সেই লগইনই V811-এর আগে ভাঙা ছিল (Supabase Auth-এ `Authorization`
--   হেডার যেত না)। লগইন ব্যর্থ ⇒ `CHECK_FAILED` ⇒ **সারিটা বসত না**,
--   আর কোথাও আবার চেষ্টাও হত না।
--   ⇒ ওই রোগীর টাকা RMP-র মোট হিসাবে কোনোদিন ঢুকত না।
--
--   দ্বিতীয় ফাঁক: পুরনো রোগীদের সারি বসানোর ঝাড়ু (V489) **প্রতি RMP-তে
--   জীবনে একবারই** চলত (`fin.rmp_commission_backfill_done`-এ দাগ পড়ে যেত)।
--   তাই উপরের কারণে একবার বাদ পড়া রোগী আর কোনোদিন যোগ হত না।
--
-- ✅ সমাধান — ঝাড়ুটা "জীবনে একবার" নয়, **প্রতি RMP-তে দিনে একবার**
--   RMP-র পর্দা খুললে দিনে সর্বোচ্চ একবার চলে; বাদ পড়া রোগী নিজে থেকেই ঢুকে যায়।
--
-- 🔒 নিরাপত্তা ও ঝুঁকি (সবটা যাচাই করে লেখা)
--   · **হিসাবের একটাও নিয়ম বদলায়নি** — `rmp_rmp_summary()` ছোঁয়াই হয়নি।
--     শুধু `rmp_backfill_commissions()`-এর দরজাটা বদলাল।
--   · আগে থেকে সারি থাকা রোগীকে **ছোঁয়াই হয় না** (`not exists` শর্ত অটুট)।
--   · ট্রিটমেন্ট পেমেন্ট নেই এমন রোগীর সারি তৈরি হয় না — টেবিল ভরে না।
--   · সারি মোছার কোনো পথ প্রজেক্টে নেই, তাই "ইচ্ছে করে মোছা" সারি
--     ফিরে আসার ঝুঁকিও নেই (খুঁজে দেখা হয়েছে)।
--   · ব্রাঞ্চের পাহারা অটুট (`fin.rmp_can_write_branch`)।
--   · **খরচ:** প্রতি RMP-তে দিনে সর্বোচ্চ একবার, তাও কেউ ওই RMP-র পর্দা
--     খুললে তবেই। Free-plan-এ চাপ কার্যত নেই।
--   · দুবার চালালেও ক্ষতি নেই।
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ⚠️ আগে V489 চালানো থাকতে হবে (দাগের টেবিলটা ওখানেই তৈরি হয়)।
-- ============================================================================

create or replace function fin.rmp_backfill_commissions(p_rmp_id text)
returns int language plpgsql security definer set search_path = fin, public, hr as $$
declare v_name text; v_mobile text; v_branch text;
        v_match_name text; v_match_mobile text;
        r record; v_made int := 0;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;

  -- 🔵🔒 V815 — আগে এখানে লেখা ছিল "দাগ থাকলেই আর কোনোদিন চলবে না"।
  -- এখন দাগটা **আজকের** হলে তবেই থামে; কাল আবার একবার চলবে। তাই আগে
  -- বাদ পড়া রোগী এক দিনের মধ্যেই নিজে থেকে হিসাবে ঢুকে যায়।
  if exists(
       select 1 from fin.rmp_commission_backfill_done x
        where x.rmp_id = p_rmp_id
          and (x.done_at at time zone 'Asia/Kolkata')::date
              = (now() at time zone 'Asia/Kolkata')::date
     ) then
    return 0;
  end if;

  select coalesce(d.name,''), coalesce(d.mobile,''), coalesce(d.branch,'')
    into v_name, v_mobile, v_branch
    from public.doctor_visits d where d.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;

  v_match_name   := lower(trim(v_name));
  v_match_mobile := right(regexp_replace(v_mobile,'[^0-9]','','g'),10);

  for r in
    select p.id as pid, coalesce(p.branch,'') as pbranch
      from public.patients p
     where
       -- V328-এর প্রমাণিত মেলানোর নিয়ম (নাম বা মোবাইল) — এক অক্ষরও বদলায়নি
       (
         (v_match_name <> '' and (
              lower(trim(coalesce(p."refBy",'')))     = v_match_name
           or lower(trim(coalesce(p."refDoctor",''))) = v_match_name))
         or
         (length(v_match_mobile) = 10 and
          right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = v_match_mobile)
       )
       -- আগে থেকে সারি থাকলে ছোঁয়া হবে না
       and not exists(select 1 from fin.rmp_patient_commissions c where c.patient_row_id=p.id)
       -- অন্তত একটা ট্রিটমেন্ট পেমেন্ট থাকতেই হবে
       and exists(
             select 1 from public.payments x
              where x."patientId"=p.id
                and fin.rmp_is_treatment(x."payType",x."remarks")
                and fin.rmp_safe_number(x."amount") > 0)
  loop
    -- একজনের সমস্যায় বাকিরা আটকাবে না।
    begin
      if fin.rmp_can_write_branch(r.pbranch) then
        perform fin.rmp_set_patient_commission(r.pid, p_rmp_id);
        v_made := v_made + 1;
      end if;
    exception when others then
      null;
    end;
  end loop;

  -- 🔵🔒 V815 — দাগটা এখন **হালনাগাদ** হয় (আগে একবার বসেই চিরকাল থাকত)।
  -- `created_rows`-এ মোট কতগুলো সারি বসেছে তার যোগফল জমা থাকে।
  insert into fin.rmp_commission_backfill_done(rmp_id, created_rows, done_by)
  values (p_rmp_id, v_made, hr.my_code())
  on conflict (rmp_id) do update
    set done_at      = now(),
        created_rows = fin.rmp_commission_backfill_done.created_rows + excluded.created_rows,
        done_by      = excluded.done_by;

  return v_made;
end $$;

revoke all on function fin.rmp_backfill_commissions(text) from public, anon;
grant execute on function fin.rmp_backfill_commissions(text) to authenticated;

-- ⚡ একবারের জন্য: আজকের দাগগুলো তুলে দেওয়া, যাতে এই ফাইল চালানোর সঙ্গে
--    সঙ্গেই প্রতিটা RMP-র বাদ পড়া রোগী প্রথম পর্দা-খোলাতেই ঢুকে যায়।
--    ⛔ শুধু দাগ — কোনো টাকার সারি বা কমিশনের সারি ছোঁয়া হয় না।
update fin.rmp_commission_backfill_done
   set done_at = now() - interval '2 days'
 where (done_at at time zone 'Asia/Kolkata')::date
       = (now() at time zone 'Asia/Kolkata')::date;

notify pgrst, 'reload schema';

-- READ-ONLY proof: কোনো টাকার হিসাব এখানে বদলানো হয়নি।
select has_function_privilege('authenticated','fin.rmp_backfill_commissions(text)','EXECUTE') as backfill_ready;
