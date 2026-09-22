-- ============================================================================
-- V489 — পুরনো রেফার করা রোগীদের কমিশন **নিজে থেকেই** বসবে
-- TK-নির্দেশ, ২০.০৮.২০২৬  ·  (V488-এর পরের ধাপ)
-- ============================================================================
--
-- TK-এর রিপোর্ট (ছবিসহ): Dr. JAFAR-এর রোগী ANIKUL — Bill ₹৩০,০০০, Paid
-- ₹৩০,০০০ — তবু Referral Income, Ref. Paid, Ref. Due সব **₹0**।
--
-- ── আসল কারণ (কোড পড়ে যাচাই করা, আন্দাজ নয়) ─────────────────────────────
--   কমিশনের হিসাব `fin.rmp_patient_commissions` টেবিলের সারি ধরে হয়। ওই
--   সারিটা তৈরি হয় **রোগীর ট্রিটমেন্ট পেমেন্ট সেভ হওয়ার ঠিক পরে** (V325,
--   ১২.০৮.২০২৬ থেকে চালু)। ANIKUL-এর টাকা জমা পড়েছে **১২.০৭.২০২৬** —
--   ব্যবস্থাটা চালু হওয়ারও এক মাস আগে। তাই তাঁর কোনো সারি কখনো তৈরিই
--   হয়নি, আর `fin.rmp_summary()` সারি না পেয়ে সব ০ ফেরত দেয় (V325, লাইন
--   ২৫৭: `if not found then return query select 0,0,0,0,0,0`)।
--   ⇒ হিসাবে কোনো ভুল নেই — শুধু পুরনো রোগীদের সারিটাই নেই।
--
-- ── সমাধান ───────────────────────────────────────────────────────────────
--   কোনো RMP-র পর্দা প্রথমবার খোলার সময় সার্ভার নিজেই দেখে নেয়: এই RMP-র
--   রেফার করা যে রোগীরা **টাকা দিয়েছেন কিন্তু কমিশনের সারি নেই**, তাঁদের
--   সারিটা তৈরি করে দেয় (হার আসে V488-এর সেই একই অগ্রাধিকার-ক্রম থেকে)।
--
--   ⭐ কাজটা **সার্ভারে** করা হলো, অ্যাপে নয় — তাই **ফোন ও কম্পিউটার
--      (ওয়েব) দুটোতেই** এক নিয়মে চলে। ওয়েবের কোড বদলানোর দরকার নেই,
--      অ্যাপের কোডও নয়। দুটোই একই `fin.rmp_rmp_summary()` ডাকে।
--
-- ── 🔒 নিরাপত্তা ─────────────────────────────────────────────────────────
--   ⛔ যে রোগীর কমিশনের সারি **আগে থেকেই আছে**, তাঁকে ছোঁয়াই হয় না —
--      পুরনো কোনো অঙ্ক বদলানোর সুযোগ নেই।
--   ⛔ যে রোগী **এখনো কোনো ট্রিটমেন্ট পেমেন্ট করেননি**, তাঁর সারি তৈরি
--      হয় না — অকারণে টেবিল ভরে না, Free-plan-এ চাপ পড়ে না।
--   ⛔ প্রতি RMP-র জন্য কাজটা **জীবনে একবারই** চলে (নিচের ছোট টেবিলে দাগ
--      পড়ে যায়)। তাই পর্দা বারবার খুললেও বাড়তি খরচ হয় না।
--   ⛔ ব্রাঞ্চের পাহারা অটুট — স্টাফ শুধু নিজের ব্রাঞ্চের রোগীর সারিই
--      তৈরি করতে পারবেন, Master সবার। (`fin.rmp_can_write_branch`)
--   ⛔ একটা রোগীতে কোনো সমস্যা হলে সেটুকুই বাদ যায়, বাকিরা ঠিকঠাক হয় —
--      আর পুরো পর্দা কখনো ভাঙে না।
--   ⛔ কোনো পুরনো ফাংশনের হিসাবের নিয়ম বদলানো হয়নি; `rmp_rmp_summary`-তে
--      শুধু শুরুতে একটা লাইন যোগ হয়েছে, নিচের গণনা হুবহু আগের মতোই।
--
-- ── রোগী ও RMP মেলানোর নিয়ম ──────────────────────────────────────────────
--   V328-এর ইতিমধ্যে-প্রমাণিত নিয়মই — মোবাইলের শেষ ১০ অঙ্ক মিলল, অথবা
--   নাম মিলল (`refBy` বা `refDoctor`, যেটাই ভরা থাকুক)। নতুন কোনো
--   মেলানোর নিয়ম বানানো হয়নি।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ⚠️ আগে V488 ফাইলটা চালানো থাকতে হবে।
-- ============================================================================

begin;

-- ── ধাপ ১: কোন RMP-র কাজ একবার হয়ে গেছে, তার দাগ ─────────────────────────
create table if not exists fin.rmp_commission_backfill_done (
  rmp_id text primary key,
  done_at timestamptz not null default now(),
  created_rows int not null default 0,
  done_by text not null default ''
);

comment on table fin.rmp_commission_backfill_done is
  'V489: কোন RMP-র পুরনো রোগীদের কমিশন-সারি একবার বসানো হয়ে গেছে। এখানে দাগ থাকলে কাজটা আর চলে না (বারবার খরচ ঠেকাতে)।';

alter table fin.rmp_commission_backfill_done enable row level security;
revoke all on table fin.rmp_commission_backfill_done from public, anon;


-- ── ধাপ ২: আসল কাজ — অনুপস্থিত সারিগুলো তৈরি করা ─────────────────────────
create or replace function fin.rmp_backfill_commissions(p_rmp_id text)
returns int language plpgsql security definer set search_path = fin, public, hr as $$
declare v_name text; v_mobile text; v_branch text;
        v_match_name text; v_match_mobile text;
        r record; v_made int := 0;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;

  -- এই RMP-র কাজ কি আগেই হয়ে গেছে? হলে সঙ্গে সঙ্গে ফিরে যাও (এক লাইনের খোঁজ)।
  if exists(select 1 from fin.rmp_commission_backfill_done x where x.rmp_id=p_rmp_id) then
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
       -- V328-এর প্রমাণিত মেলানোর নিয়ম (নাম বা মোবাইল)
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
       -- অন্তত একটা ট্রিটমেন্ট পেমেন্ট থাকতেই হবে (নইলে সারি বানানোর মানে নেই)
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
      null;   -- এই রোগীকে বাদ দিয়ে পরেরজনে যাও
    end;
  end loop;

  insert into fin.rmp_commission_backfill_done(rmp_id, created_rows, done_by)
  values (p_rmp_id, v_made, hr.my_code())
  on conflict (rmp_id) do nothing;

  return v_made;
end $$;

revoke all on function fin.rmp_backfill_commissions(text) from public, anon;
grant execute on function fin.rmp_backfill_commissions(text) to authenticated;


-- ── ধাপ ৩: RMP-র পর্দা খুললেই কাজটা নিজে থেকে চলবে ───────────────────────
--    ⛔ V398-এর `rmp_rmp_summary`-র **প্রতিটা হিসাবের লাইন হুবহু নকল** —
--       একটাও সংখ্যা/নিয়ম বদলায়নি। শুধু পাহারার ঠিক পরে একটা লাইন যোগ:
--       `perform fin.rmp_backfill_commissions(p_rmp_id);`
--    ⛔ ফোন ও ওয়েব দুটোই এই একই ফাংশন ডাকে — তাই দুটোতেই একসাথে কাজ করে।
create or replace function fin.rmp_rmp_summary(p_rmp_id text)
returns table(patient_count bigint, earned numeric, paid_to_this_rmp numeric,
              previous_rmp_paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare c record; s record; v_count bigint:=0; v_earned numeric:=0; v_due numeric:=0;
        v_over numeric:=0; v_paid numeric:=0; v_previous numeric:=0;
        v_unallocated numeric:=0; v_branch text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;

  -- 🔵🔒 V489 (TK, 20.08.2026) — পুরনো রেফার করা রোগীদের কমিশন-সারি না
  -- থাকলে এখানেই একবার তৈরি হয়ে যায়। জীবনে একবারই চলে; কিছু ভুল হলেও
  -- নিচের হিসাব থেমে যায় না।
  begin
    perform fin.rmp_backfill_commissions(p_rmp_id);
  exception when others then
    null;
  end;

  for c in select patient_row_id from fin.rmp_patient_commissions where rmp_id=p_rmp_id loop
    select * into s from fin.rmp_summary(c.patient_row_id);
    v_count:=v_count+1; v_earned:=v_earned+coalesce(s.earned,0);
    v_due:=v_due+coalesce(s.due,0); v_over:=v_over+coalesce(s.overpaid,0);
  end loop;
  select coalesce(sum(x.amount),0) into v_paid
    from fin.rmp_commission_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount-x.allocated_amount),0) into v_unallocated
    from fin.rmp_advance_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount),0) into v_previous
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions pc on pc.id=x.patient_commission_id
   where pc.rmp_id=p_rmp_id and x.rmp_id<>p_rmp_id;
  return query select v_count,round(v_earned,2),round(v_paid+v_unallocated,2),round(v_previous,2),
    round(greatest(0,v_due-v_unallocated),2),round(v_over+greatest(0,v_unallocated-v_due),2);
end $$;

revoke all on function fin.rmp_rmp_summary(text) from public,anon;
grant execute on function fin.rmp_rmp_summary(text) to authenticated;


-- ── ধাপ ৪: যাচাই ─────────────────────────────────────────────────────────
do $$
declare n_patient int; n_done int;
begin
  select count(*) into n_patient from fin.rmp_patient_commissions;
  select count(*) into n_done    from fin.rmp_commission_backfill_done;
  raise notice '── V489 সফল ────────────────────────────────';
  raise notice 'এখন পর্যন্ত সেভ হওয়া রোগীর কমিশন : % টি', n_patient;
  raise notice 'কাজ হয়ে যাওয়া RMP                 : % জন', n_done;
  raise notice 'এবার অ্যাপ বা ওয়েবে যে RMP-র পর্দা খুলবেন,';
  raise notice 'তাঁর পুরনো রোগীদের কমিশন সেখানেই বসে যাবে।';
  raise notice '─────────────────────────────────────────────';
end $$;

commit;
