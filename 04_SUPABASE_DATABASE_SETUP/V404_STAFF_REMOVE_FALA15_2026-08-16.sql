-- =====================================================================
-- V404 — SWAPNA ADHIKARI (FALA-15) কাজ ছেড়ে দিয়েছেন · সম্পূর্ণ বাদ (16.08.2026)
-- =====================================================================
-- TK-এর নির্দেশ (হুবহু):
--   "SWAPNA ADHIKARI / কাজ ছেড়ে দিয়েছে"
--   "ওই নাম্বারের কোনো অংশ যেন না থাকে"
--   মাইনে — "সব ক্লিয়ার" (কিছু বাকি নেই)
--   পুরনো কাজের রেকর্ড — "রেকর্ড অটুট থাক"
--
-- এই ফাইলটা যা করে (৫টা ধাপ, সবই FALA-15-এর জন্য, আর কারো নয়):
--   ১) তাঁর লগইন-পরিচয় (hr.app_identity) মুছে ফেলে
--   ২) Supabase-এর লগইন অ্যাকাউন্ট (auth.users + auth.identities) মুছে ফেলে
--   ৩) আয়-খরচের চাবি (fin.entry_permits) মুছে ফেলে
--   ৪) মাইনে বন্ধ করে (hr.salary_config.salary_enabled = false)
--   ৫) স্টাফ প্রোফাইল "বাদ" চিহ্নিত করে (active = false) ও ব্রাঞ্চ ঠিক করে
--      (ডেটাবেসে ভুল করে Birpara লেখা ছিল, আসলে Falakata)
--
-- ⛔ যা এই ফাইল কখনো ছোঁয় না:
--   • রোগী · ফলোআপ · এনকোয়ারি · চেম্বার · কল-হিস্ট্রি — একটাও সারি নয়
--   • hr.salary_payments — তাঁকে কবে কত মাইনে দেওয়া হয়েছে, সব অটুট
--   • wn.leave_requests · hr.profile_audit — অটুট
--   • অন্য কোনো কর্মী/ডাক্তার/ব্রাঞ্চ — একজনও নয়
--   ⇒ "কে কোন রোগী তুলেছিলেন" সেই তথ্য থেকেই যাবে (TK-সিদ্ধান্ত)।
--
-- বারবার চালালেও ক্ষতি নেই (idempotent)। সব একসাথে হয় বা কিছুই হয় না।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- =====================================================================

begin;

-- ---------------------------------------------------------------------
-- ০) চালানোর আগে — কী কী পাওয়া গেল, চোখে দেখে নেওয়ার জন্য
-- ---------------------------------------------------------------------
do $$
declare n_id int; n_auth int; n_permit int; n_pay int;
begin
  select count(*) into n_id     from hr.app_identity     where person_code = 'FALA-15';
  select count(*) into n_auth   from auth.users          where lower(email) = 'fala-15@staff.piles';
  select count(*) into n_permit from fin.entry_permits   where person_code = 'FALA-15';
  select count(*) into n_pay    from hr.salary_payments  where person_code = 'FALA-15';
  raise notice 'V404 আগে — পরিচয়:% · লগইন-অ্যাকাউন্ট:% · চাবি:% · মাইনের রসিদ:% (রসিদ ছোঁয়া হবে না)',
    n_id, n_auth, n_permit, n_pay;
end $$;

-- ---------------------------------------------------------------------
-- ১) লগইন-পরিচয় মুছে ফেলা — এটাই সবচেয়ে জরুরি।
--    hr.my_code() এই টেবিল থেকেই কোড পড়ে; সারি না থাকলে সব RLS নীতিতে
--    তিনি "কেউ নন" — কোনো তথ্য পড়তেও পারবেন না, লিখতেও পারবেন না।
-- ---------------------------------------------------------------------
delete from hr.app_identity where person_code = 'FALA-15';

-- ---------------------------------------------------------------------
-- ২) Supabase-এর লগইন অ্যাকাউন্ট মুছে ফেলা।
--    ⛔ শুধু এই একটাই ই-মেল, আর কারো নয়। identities আগে, তারপর users
--    (কিছু প্রজেক্টে cascade থাকে, কিছুতে থাকে না — তাই দুটোই স্পষ্ট করে)।
-- ---------------------------------------------------------------------
delete from auth.identities
 where user_id in (select id from auth.users where lower(email) = 'fala-15@staff.piles');

delete from auth.users where lower(email) = 'fala-15@staff.piles';

-- ---------------------------------------------------------------------
-- ৩) আয়-খরচের চাবি মুছে ফেলা (থাকলে)।
-- ---------------------------------------------------------------------
delete from fin.entry_permits where person_code = 'FALA-15';

-- ---------------------------------------------------------------------
-- ৪) মাইনে বন্ধ — TK: "সব ক্লিয়ার"। ⇒ আর কখনো "Salary Due"-তে নাম উঠবে না।
--    ⛔ hr.salary_payments (কবে কত দেওয়া হয়েছে) কিছুই মোছা হচ্ছে না।
-- ---------------------------------------------------------------------
update hr.salary_config
   set salary_enabled = false, updated_by = 'V404', updated_at = now()
 where person_code = 'FALA-15';

-- ---------------------------------------------------------------------
-- ৫) স্টাফ প্রোফাইল — "বাদ" চিহ্নিত (active=false) + ব্রাঞ্চ শোধরানো।
--    active=false হওয়ায় V403-এর সুরক্ষাও এখন তাঁর উপরে খাটবে।
--    ব্রাঞ্চ: ডেটাবেসে ভুল করে Birpara ছিল, TK বলেছেন তিনি Falakata-র।
--    যাচাই করা — hr.salary_payments-এ ব্রাঞ্চের ঘরই নেই আর
--    wn.leave_requests-এ প্রতিটি সারির নিজের ব্রাঞ্চ লেখা ⇒ কোনো পুরনো
--    হিসাব এতে নড়ে না।
-- ---------------------------------------------------------------------
update hr.staff_profiles
   set active = false, branch = 'Falakata', updated_at = now()
 where person_code = 'FALA-15';

-- ---------------------------------------------------------------------
-- ৬) 🔑 ভবিষ্যতের জন্য — "বাদ" মানেই লগইন বন্ধ।
--
--    আগে এই ছিদ্রটা ছিল: কাউকে `active=false` করলেও সে **লগইন করতে পারত**,
--    কারণ লগইনের গেট শুধু `suspended_until` দেখত।
--
--    ওয়েব (`app.js`) ও ফোন (`CloudPasswordCheck.kt`) — দুটোই লগইনের সময়
--    এই একটাই ফাংশন ডাকে। তাই এখানে একবার ঠিক করলেই দুই জায়গায় কাজ হয়,
--    অ্যাপের লগইন-কোডে হাত দিতে হয় না (⇒ লগইন ভাঙার ঝুঁকি শূন্য)।
--
--    নতুন নিয়ম: বাদ-দেওয়া (active=false) হলে অনেক দূরের একটা তারিখ ফেরায়
--    ⇒ গেট তাকে "সাসপেন্ডেড" ধরে আটকে দেয়।
--    ⛔ যাঁর staff_profiles সারিই নেই (BIR-BRANCH ইত্যাদি ব্রাঞ্চ-লগইন) —
--       আগের মতোই null ফেরে, কিচ্ছু বদলায় না।
--    ⛔ সচল কর্মীর ক্ষেত্রেও আগের মতোই suspended_until ফেরে।
-- ---------------------------------------------------------------------
create or replace function public.suspended_until_for(p_mobile text) returns date
  language sql stable security definer set search_path = public, hr as $$
  select case when coalesce(active, true) = false then date '2999-12-31'
              else suspended_until end
    from hr.staff_profiles
   where right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10)
       = right(regexp_replace(coalesce(p_mobile,''),'\D','','g'),10)
   limit 1;
$$;
revoke all on function public.suspended_until_for(text) from public;
grant execute on function public.suspended_until_for(text) to anon, authenticated;

-- ---------------------------------------------------------------------
-- ৭) চালানোর পরে — সত্যিই সব বন্ধ হলো কিনা, নিজেই মিলিয়ে নেয়
-- ---------------------------------------------------------------------
do $$
declare n_id int; n_auth int; n_permit int; n_sal int; n_act int;
begin
  select count(*) into n_id     from hr.app_identity   where person_code = 'FALA-15';
  select count(*) into n_auth   from auth.users        where lower(email) = 'fala-15@staff.piles';
  select count(*) into n_permit from fin.entry_permits where person_code = 'FALA-15';
  select count(*) into n_sal    from hr.salary_config
        where person_code = 'FALA-15' and salary_enabled = true;
  select count(*) into n_act    from hr.staff_profiles
        where person_code = 'FALA-15' and coalesce(active, true) = true;

  if n_id > 0 or n_auth > 0 or n_permit > 0 or n_sal > 0 or n_act > 0 then
    raise exception 'V404 সম্পূর্ণ হয়নি — পরিচয়:% লগইন:% চাবি:% মাইনে-চালু:% এখনো-সচল:%',
      n_id, n_auth, n_permit, n_sal, n_act;
  end if;
  raise notice 'V404 ✅ FALA-15 সম্পূর্ণ বাদ — লগইন নেই · চাবি নেই · মাইনে বন্ধ · প্রোফাইল বাদ-চিহ্নিত (ব্রাঞ্চ Falakata)। পুরনো কোনো রেকর্ড ছোঁয়া হয়নি।';
end $$;

commit;

-- =====================================================================
-- ⛔ মনে রাখার কথা: অ্যাপের ভিতরের লগইন-তালিকা থেকেও নম্বরটা সরানো হয়েছে
--    (ওয়েব `config.js` · ফোন `StaffDirectory.kt`), আর ওয়েবসাইটে Birpara-র
--    প্রকাশ্য ফোন থেকেও বাদ (`app.js` publicBranchPhones) — V404-এর সঙ্গেই।
--    ⇒ নতুন ওয়েব ফাইল Netlify-তে আপলোড ও নতুন APK না বসালে কাজ অসম্পূর্ণ।
-- =====================================================================
