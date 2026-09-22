-- =====================================================================
-- 🔒 খাতার সারি B107 — শুধু **লিখে রাখা**, নতুন কিছু নয়
-- তারিখ: 29.07.2026 · ভার্সন V158
-- =====================================================================
--
-- ⛔⛔ TK-কে এই SQL চালাতে হবে না। ⛔⛔
--
-- নিচের প্রতিটা ঘর **আসল ডেটাবেসে আগে থেকেই আছে** — অ্যাপ রোজ এগুলো
-- পড়ে ও লেখে। সমস্যা হলো, `PILES_CLINIC_DB_SETUP.sql` ফাইলটা আসল
-- ডেটাবেসের চেয়ে **পুরনো**, তাই ওই ফাইল দেখলে মনে হয় ঘরগুলো নেই।
--
-- 🚨 এটা কেন বিপজ্জনক ছিল (২৯.০৭.২০২৬-এ ধরা পড়েছে, খাতার সারি B105):
-- `SupabaseClient.FOLLOWUP_COLS_NO_PHOTO` তালিকাটা কেউ একজন ওই পুরনো
-- ফাইল দেখে বানিয়েছিলেন, ফলে ছ'টা আসল ঘর তাতে বাদ পড়ে গিয়েছিল —
-- তার মধ্যে **`patientId`**-ও। ওই তালিকা ব্যবহার করলে ব্রাঞ্চ যাচাইয়ে
-- ফাঁকা `patientId` দেখে **সদ্য রেজিস্টার হওয়া রোগী তালিকা থেকে
-- হারিয়ে যেতে পারত** (২৭.০৭.২০২৬-এ ঠিক এই বিপদেই একবার কাজ ফিরিয়ে
-- নিতে হয়েছিল)।
--
-- তাই ঘরগুলো এখানে লিখে রাখা হলো, যাতে —
--   ১) পাহারাদার (`00_GUARD/tk_guard.py`) এগুলোকে সত্যি বলে চেনে;
--   ২) ভবিষ্যতে কোনো AI বা ডেভেলপার পুরনো ফাইল দেখে ঘর বাদ না দেন।
--
-- 🔒 নিরাপত্তা: প্রতিটা লাইনে `if not exists` আছে। ঘর আগে থেকেই থাকায়
--    ভুল করে চালিয়ে ফেললেও **কিছুই ঘটবে না** — কোনো তথ্য বদলাবে না,
--    মুছবে না। তবু দরকার নেই বলে চালানোর প্রয়োজন নেই।
-- =====================================================================

-- followups — ফোনের কোড এই ঘরগুলো রোজ পড়ে
-- (উৎস: FollowUpRepository.FOLLOWUP_COLS — TK নিজে ২৭.০৭.২০২৬-এ
--  লাইভ ডেটাবেসে চালিয়ে যাচাই করেছিলেন)
alter table public.followups add column if not exists "age" text;
alter table public.followups add column if not exists "sex" text;
alter table public.followups add column if not exists "patientId" text;
alter table public.followups add column if not exists "timeType" text;
alter table public.followups add column if not exists "lastCallDate" text;
alter table public.followups add column if not exists "convertedPatientId" text;

-- patients — একই কারণে
-- (উৎস: FollowUpRepository.PATIENT_COLS, একই লাইভ যাচাই)
alter table public.patients add column if not exists "timeType" text;
alter table public.patients add column if not exists "completeRequestedBy" text;
alter table public.patients add column if not exists "completeApprovedBy" text;

-- =====================================================================
-- ⛔ এই ফাইলের কোনো লাইন মোছা যাবে না। কোনো ঘর সত্যিই বাদ দিতে হলে
--    আগে TK-কে জিজ্ঞাসা করতে হবে।
-- =====================================================================
