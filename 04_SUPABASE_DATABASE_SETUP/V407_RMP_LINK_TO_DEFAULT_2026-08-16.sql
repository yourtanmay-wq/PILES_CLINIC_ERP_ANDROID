-- =====================================================================
-- V407 — RMP: "জমাট" রোগী-হিসাব Default-এর সঙ্গে জুড়ে দেওয়া (16.08.2026)
-- =====================================================================
-- TK-এর সিদ্ধান্ত (16.08.2026): **"হ্যাঁ, জুড়ে দিন"**
--
-- 🔍 লাইভ ডেটাবেসে যা পাওয়া গেছে (TK নিজে চালিয়ে):
--      ডেটাবেস হালনাগাদ  : V380 ✅ · V383/V398 ✅ (আগের সন্দেহ মিটেছে)
--      RMP-র Default     : ৫ জনের বসানো
--      মোট রোগী-হিসাব    : ২২  →  ১৪টা ইতিমধ্যেই Default মানছে
--                               ৮টা "জমাট", কিন্তু **মান Default-এর সমানই**
--      🟢 মান সত্যিই আলাদা : **০** — একজনেরও নয়
--      🟢 নতুন পথে দেওয়া   : **₹০** — এক পয়সাও নয়
--
-- 🟢 তাই আগে যে ঝুঁকিগুলোর কথা বলেছিলাম, সেগুলো **এখানে খাটে না**:
--    · "ইচ্ছাকৃত আলাদা % মুছে যাবে" → আলাদা % কারোরই নেই (০)
--    · "দেওয়া কমিশন 'বেশি দেওয়া' দেখাবে" → নতুন পথে ₹০ দেওয়া হয়েছে
--    · "ডেটাবেস হালনাগাদ কিনা অনিশ্চিত" → দুটোই বসানো, নিশ্চিত
--
-- এই ফাইল যা করে — **একটাই কাজ**:
--    যাদের `use_rmp_default = false` **কিন্তু মান হুবহু Default-এর সমান**,
--    শুধু তাদের `use_rmp_default = true` করে দেয়।
--    ⇒ আজকের টাকার অঙ্ক **এক পয়সাও বদলায় না** (মান তো একই ছিল)।
--    ⇒ শুধু **ভবিষ্যতে Default বদলালে এরাও মানবে** — TK ঠিক এটাই চেয়েছেন।
--
-- ⛔ যা এই ফাইল কখনো ছোঁয় না:
--    · যাদের মান সত্যিই আলাদা — একটিও নয় (TK ইচ্ছে করে বসিয়ে থাকতে পারেন)
--    · কারো `commission_mode` বা `commission_value` — একটিও নয়
--    · `fin.rmp_commission_payments` (দেওয়া টাকা) — একটিও সারি নয়
--    · পুরনো `referralPayments`-এর ₹৪২,০০০ — ওখানে %-ই নেই, তাই বাইরে
--
-- 🔒 নিজেই মিলিয়ে দেখে: কারো কমিশনের **মান** বদলালে পুরো কাজ **বাতিল**।
--
-- ✅ যাচাই: V325-এর হুবহু টেবিল-গঠনে আসল PostgreSQL 16-এ চালানো —
--    মান-সমান সারিটা জুড়েছে, মান-আলাদা সারিটা ছোঁয়াই হয়নি,
--    কারো কমিশনের অঙ্ক বদলায়নি।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
--
-- 🔙 ফেরাতে হলে:
--      update fin.rmp_patient_commissions pc set use_rmp_default = b.use_rmp_default
--        from fin.rmp_patient_commissions_backup_v407 b
--       where b.patient_row_id = pc.patient_row_id;
-- =====================================================================

begin;

-- ধাপ ০: 🔒 ব্যাকআপ
drop table if exists fin.rmp_patient_commissions_backup_v407;
create table fin.rmp_patient_commissions_backup_v407 as
  select * from fin.rmp_patient_commissions;

-- ধাপ ১: 🔴 শুধু সেগুলোই — যাদের মান **হুবহু Default-এর সমান**।
--    ⛔ মান আলাদা হলে ছোঁয়াই হয় না (TK ইচ্ছে করে বসিয়ে থাকতে পারেন)।
--    ⇒ তাই আজকের টাকার অঙ্ক **এক পয়সাও বদলায় না** — শুধু ভবিষ্যতে
--      Default বদলালে এরাও মানবে।
update fin.rmp_patient_commissions pc
   set use_rmp_default = true,
       updated_at = now()
  from fin.rmp_commission_defaults d
 where d.rmp_id = pc.rmp_id
   and pc.use_rmp_default = false
   and pc.commission_mode  = d.commission_mode
   and pc.commission_value = d.commission_value;

-- ধাপ ২: 🔒 মিলিয়ে দেখা — ভুল কিছু বদলায়নি তো?
do $$
declare n_changed int; n_left int; n_diff_touched int;
begin
  select count(*) into n_changed
    from fin.rmp_patient_commissions pc
    join fin.rmp_patient_commissions_backup_v407 b on b.patient_row_id = pc.patient_row_id
   where b.use_rmp_default = false and pc.use_rmp_default = true;

  -- এখনো "জমাট" কারা (এদের মান সত্যিই আলাদা — ছোঁয়া হয়নি)
  select count(*) into n_left
    from fin.rmp_patient_commissions where use_rmp_default = false;

  -- 🔴 সবচেয়ে জরুরি: কারো **মান** (mode/value) বদলে গেছে কিনা — একটাও নয়
  select count(*) into n_diff_touched
    from fin.rmp_patient_commissions pc
    join fin.rmp_patient_commissions_backup_v407 b on b.patient_row_id = pc.patient_row_id
   where pc.commission_mode <> b.commission_mode
      or pc.commission_value <> b.commission_value;

  raise notice '───────────────────────────────────────────';
  raise notice 'Default-এর সঙ্গে জোড়া লাগল : % টি', n_changed;
  raise notice 'এখনো আলাদা (ছোঁয়া হয়নি)   : % টি', n_left;
  raise notice 'কারো %% বা টাকার মান বদলেছে : % টি', n_diff_touched;
  raise notice '───────────────────────────────────────────';

  if n_diff_touched > 0 then
    raise exception 'বাতিল — % টি সারির কমিশনের মান বদলে গেছে, যা হওয়ার কথা ছিল না', n_diff_touched;
  end if;
  raise notice '✅ কারো কমিশনের অঙ্ক বদলায়নি — শুধু "Default মানবে" চিহ্নটা বসল।';
end $$;

commit;
