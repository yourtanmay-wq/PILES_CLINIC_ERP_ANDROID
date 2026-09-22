-- ============================================================================
-- V490 — নতুন Enquiry · Registration · Advance-এ **স্বয়ংক্রিয় নোটিফিকেশন**
-- TK-নির্দেশ, ২০.০৮.২০২৬
-- ============================================================================
--
-- TK-এর রিপোর্ট: ফালাকাটা ব্রাঞ্চ থেকে কিশনগঞ্জের জন্য একটা এনকোয়ারি হয়েছিল।
-- ব্রাঞ্চ ফোন করেছিল বলেই জানা গেল — নইলে হারিয়ে যেত।
--
-- TK-এর নির্দেশ:
--   • যে ব্রাঞ্চের জন্য কাজটা, সেই ব্রাঞ্চের **সবাই** + **মাস্টার** ঘন্টায়
--     নোটিফিকেশন পাবেন। চাপলে দেখা যাবে, চাইলে অ্যাকশন, নইলে Seen করে ছাড়া।
--   • তিনটে ঘটনায়: নতুন **Enquiry** · নতুন **Registration** · **Advance**।
--   • যিনি নিজে লিখলেন, **তাঁর কাছে নোটিফিকেশন যাবে না**।
--   • বারবার যেন অ্যালার্ম না বাজে।
--
-- ── কেন ডেটাবেসে (অ্যাপে নয়) ─────────────────────────────────────────────
--   এনকোয়ারি/রেজিস্ট্রেশন/পেমেন্ট — তিনটেই ফোন **ও** কম্পিউটার (ওয়েব)
--   দুই জায়গা থেকেই হতে পারে। নিয়মটা ডেটাবেসে বসালে **যেখান থেকেই হোক**
--   নোটিফিকেশন তৈরি হয়। অ্যাপে বসালে ওয়েবের কাজগুলো বাদ পড়ত।
--
-- ── 🔒 নিরাপত্তা (কোড পড়ে যাচাই করা, আন্দাজ নয়) ──────────────────────────
--   ⛔ কোনো পুরনো টেবিল/কলাম/হিসাব বদলানো হয়নি। শুধু `public.briefings`-এ
--      (যেটা ঘন্টার নোটিশের নিজের টেবিল) নতুন সারি যোগ হয়।
--   ⛔ নোটিফিকেশন তৈরিতে কিছু ভুল হলেও **মূল কাজটা (এনকোয়ারি/রেজিস্ট্রেশন/
--      পেমেন্ট সেভ হওয়া) কখনো আটকাবে না** — পুরো অংশটা exception-এ মোড়া।
--   ⛔ নোটিশের id সারির id ধরে **স্থির** (brief_auto_enq_<id> …) + ON CONFLICT
--      DO NOTHING — তাই নেট দুর্বল হয়ে একই সারি আবার এলেও **ডবল হয় না**।
--   ⛔ শুধু **আজকের তারিখের** সারিতে নোটিশ হয়। তাই পুরনো ডেটা restore/import
--      করলে শত শত নোটিফিকেশনের বন্যা হবে না।
--   ⛔ যিনি লিখলেন তাঁর মোবাইল `hiddenFor`-এ বসে যায় — অ্যাপ ও ওয়েব দুটোরই
--      আগে-থেকে-থাকা নিয়ম (isBriefingDeletedForMe / isDeletedForMe) মেনে
--      তাঁর কাছে নোটিশটা যায়ই না। নতুন কোনো নিয়ম বানানো হয়নি।
--   ⛔ শব্দ: অ্যাপের BellNotifier আগে থেকেই **সংখ্যা বাড়লে একবারই** বাজায়
--      (BellNotifier.onCount)। এখানে নতুন করে কোনো শব্দ যোগ করা হয়নি।
--   ⛔ ৭ দিনের বেশি পুরনো এই স্বয়ংক্রিয় নোটিশ নিজে থেকেই মুছে যায় — তাই
--      Supabase-এ টেবিল বাড়তে থাকে না (TK-এর কোটা-সমস্যার কথা মাথায় রেখে)।
--      ⚠️ শুধু **এই স্বয়ংক্রিয়** নোটিশই মোছে; হাতে লেখা Briefing, Refund/
--         Delete/Leave request — কোনোটাই ছোঁয়া হয় না।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ============================================================================

begin;

-- ── ধাপ ১: পুরনো নোটিশ দ্রুত খুঁজে মোছার জন্য index ──────────────────────
create index if not exists briefings_date_idx on public.briefings ("date");


-- ── ধাপ ২: এই তিন ধরনের নোটিশের নাম একটাই জায়গায় ───────────────────────
--    অ্যাপ ও ওয়েব এই নামগুলো দেখেই চিনবে।
create or replace function public.auto_notice_titles()
returns text[] language sql immutable as $$
  select array['New Enquiry','New Registration','Advance Received']
$$;


-- ── ধাপ ৩: নোটিশ বসানোর একটাই সাধারণ ফাংশন ──────────────────────────────
create or replace function public.auto_notice_post(
  p_id text, p_title text, p_message text, p_branch text, p_creator text)
returns void language plpgsql security definer set search_path = public as $$
declare v_now text; v_today text; v_creator text;
begin
  if coalesce(trim(p_branch),'') = '' then return; end if;
  v_now   := to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
  v_today := to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD');
  -- মোবাইলের শেষ ১০ অঙ্ক — অ্যাপ/ওয়েবের mob() নিয়মের হুবহু একই।
  v_creator := right(regexp_replace(coalesce(p_creator,''),'[^0-9]','','g'),10);

  insert into public.briefings
    ("id","date","title","message","targets","seen","replies","hiddenFor",
     "deletedAt","deletedBy","branch","createdBy","createdAt","updatedAt")
  values (
    p_id, v_today, p_title, p_message,
    -- ওই ব্রাঞ্চের সবাই + মাস্টার
    jsonb_build_object('branches', jsonb_build_array(p_branch),
                       'roles',    jsonb_build_array('master')),
    '[]'::jsonb, '[]'::jsonb,
    -- 🔵 যিনি নিজে লিখলেন, তাঁর কাছে নোটিশটা যাবে না (TK-নির্দেশ)
    case when length(v_creator)=10 then jsonb_build_array(v_creator) else '[]'::jsonb end,
    '', '', p_branch, coalesce(p_creator,''), v_now, v_now
  )
  on conflict ("id") do nothing;   -- 🔒 একই খবর কখনো দুবার নয়

  -- ৭ দিনের বেশি পুরনো **স্বয়ংক্রিয়** নোটিশ মুছে ফেলা (কোটা বাঁচাতে)
  delete from public.briefings b
   where b."id" like 'brief\_auto\_%'
     and b."title" = any(public.auto_notice_titles())
     and b."date" < to_char((now() at time zone 'Asia/Kolkata') - interval '7 days','YYYY-MM-DD');
exception when others then
  null;   -- নোটিশ না হলেও মূল কাজ কখনো আটকাবে না
end $$;

revoke all on function public.auto_notice_post(text,text,text,text,text) from public, anon;


-- ── ধাপ ৪: নতুন Enquiry ─────────────────────────────────────────────────
create or replace function public.trg_notice_new_enquiry()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_msg text;
begin
  begin
    -- শুধু আজকের এনকোয়ারি (পুরনো ডেটা import-এ বন্যা ঠেকাতে)
    if coalesce(NEW."date",'') <> to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD') then
      return NEW;
    end if;
    v_msg := trim(coalesce(NEW."name",'Unknown'))
          || ' · ' || coalesce(NEW."mobile",'')
          || case when coalesce(trim(NEW."disease"),'')<>'' then ' · ' || NEW."disease" else '' end
          || ' · ' || coalesce(NEW."branch",'') || ' branch';
    perform public.auto_notice_post(
      'brief_auto_enq_' || NEW."id", 'New Enquiry', v_msg, NEW."branch", NEW."createdBy");
  exception when others then null;
  end;
  return NEW;
end $$;

drop trigger if exists trg_notice_new_enquiry on public.enquiries;
create trigger trg_notice_new_enquiry
  after insert on public.enquiries
  for each row execute function public.trg_notice_new_enquiry();


-- ── ধাপ ৫: নতুন Registration (Visit) ────────────────────────────────────
create or replace function public.trg_notice_new_registration()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_msg text;
begin
  begin
    if coalesce(NEW."date",'') <> to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD') then
      return NEW;
    end if;
    v_msg := trim(coalesce(NEW."name",'Unknown'))
          || ' · ' || coalesce(NEW."mobile",'')
          || case when coalesce(trim(NEW."patientId"),'')<>'' then ' · ' || NEW."patientId" else '' end
          || case when coalesce(trim(NEW."disease"),'')<>''  then ' · ' || NEW."disease"  else '' end
          || ' · ' || coalesce(NEW."branch",'') || ' branch';
    perform public.auto_notice_post(
      'brief_auto_reg_' || NEW."id", 'New Registration', v_msg, NEW."branch", NEW."createdBy");
  exception when others then null;
  end;
  return NEW;
end $$;

drop trigger if exists trg_notice_new_registration on public.patients;
create trigger trg_notice_new_registration
  after insert on public.patients
  for each row execute function public.trg_notice_new_registration();


-- ── ধাপ ৬: Advance (রোগীর **প্রথম** ট্রিটমেন্ট পেমেন্ট) ─────────────────
--    ⚠️ অ্যাপে "Advance" আলাদা কোনো payType নয় — TK-এর নিয়ম অনুযায়ী রোগীর
--       **১ম** টাকা = Advance, তারপর 2nd Payment · 3rd Payment …
--       (PaymentModel.ordinalPaymentLabel)। তাই এখানেও ঠিক সেটাই দেখা হয়।
--    ⚠️ Registration Fee · Visit Fee · Medicine — এগুলো Advance নয়, তাই বাদ
--       (fin.rmp_is_treatment — V325-এর ইতিমধ্যে প্রমাণিত একই নিয়ম)।
create or replace function public.trg_notice_advance_payment()
returns trigger language plpgsql security definer set search_path = public, fin as $$
declare v_msg text; v_prior int; v_name text;
begin
  begin
    if coalesce(NEW."date",'') <> to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD') then
      return NEW;
    end if;
    if not fin.rmp_is_treatment(NEW."payType", NEW."remarks") then return NEW; end if;
    if fin.rmp_safe_number(NEW."amount") <= 0 then return NEW; end if;
    if coalesce(trim(NEW."patientId"),'') = '' then return NEW; end if;

    -- এর আগে এই রোগীর আর কোনো ট্রিটমেন্ট পেমেন্ট আছে কি? থাকলে এটা Advance নয়।
    select count(*) into v_prior from public.payments x
     where x."patientId" = NEW."patientId"
       and x."id" <> NEW."id"
       and fin.rmp_is_treatment(x."payType", x."remarks")
       and fin.rmp_safe_number(x."amount") > 0;
    if v_prior > 0 then return NEW; end if;

    select coalesce(nullif(trim(NEW."name"),''), nullif(trim(p.name),''), 'Unknown')
      into v_name from public.patients p where p.id = NEW."patientId";
    v_name := coalesce(v_name, coalesce(nullif(trim(NEW."name"),''),'Unknown'));

    v_msg := v_name
          || ' · ₹' || trim(to_char(fin.rmp_safe_number(NEW."amount"),'FM999999999'))
          || ' · ' || upper(coalesce(nullif(trim(NEW."mode"),''),'CASH'))
          || ' · ' || coalesce(NEW."branch",'') || ' branch';
    perform public.auto_notice_post(
      'brief_auto_adv_' || NEW."id", 'Advance Received', v_msg, NEW."branch", NEW."createdBy");
  exception when others then null;
  end;
  return NEW;
end $$;

drop trigger if exists trg_notice_advance_payment on public.payments;
create trigger trg_notice_advance_payment
  after insert on public.payments
  for each row execute function public.trg_notice_advance_payment();


-- ── ধাপ ৭: যাচাই ─────────────────────────────────────────────────────────
do $$
declare n int;
begin
  select count(*) into n from pg_trigger
   where tgname in ('trg_notice_new_enquiry','trg_notice_new_registration','trg_notice_advance_payment');
  if n <> 3 then raise exception 'V490 থেমে গেল — ৩টির বদলে % টি trigger বসেছে', n; end if;
  raise notice '── V490 সফল ────────────────────────────────';
  raise notice 'নতুন Enquiry · Registration · Advance — তিনটেতেই';
  raise notice 'ওই ব্রাঞ্চের সবাই ও মাস্টার ঘন্টায় নোটিফিকেশন পাবেন।';
  raise notice 'যিনি নিজে লিখবেন, তাঁর কাছে যাবে না।';
  raise notice 'ফোন ও কম্পিউটার — দুই জায়গার কাজেই চলবে।';
  raise notice '─────────────────────────────────────────────';
end $$;

commit;
