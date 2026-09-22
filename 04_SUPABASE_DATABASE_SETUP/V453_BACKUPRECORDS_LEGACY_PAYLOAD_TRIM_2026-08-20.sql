-- =====================================================================
-- V453 — backuprecords পুরনো legacy payload নিরাপদে ছোট করা
-- তারিখ: 20.08.2026 · TK নিজে চালাবেন (Supabase SQL Editor-এ)
-- =====================================================================
-- কেন: V452-এর অডিটে ধরা পড়েছিল backuprecords টেবিলে ৩টা পুরনো row-তে
-- বড় legacy `payload` (base64 ছবিসহ পূর্ণ backup) জমা আছে (~10 MB)।
-- বর্তমান কোড (app.js: cloudUpsertExtra → cloudSafeRows) নতুন কোনো backup
-- upload করার সময় payload কলাম আর পাঠায়ই না — তাই এটা নতুন করে বাড়বে না,
-- শুধু পুরনো জঞ্জাল।
--
-- ঝুঁকি: payload-টাই কোনো কোনো ক্ষেত্রে "শেষ ভরসার" restore কপি (ফোন হারিয়ে
-- গেলে/লোকাল ব্যাকআপ মুছে গেলে ওখান থেকেই ফিরে আসতে পারত)। তাই এই স্ক্রিপ্ট
-- **সব payload মোছে না** — শুধু সবচেয়ে পুরনোগুলো মোছে, **সবচেয়ে নতুন backup
-- row-এর payload অক্ষত রাখা হয়** যাতে অন্তত একটা সাম্প্রতিক cloud-recovery
-- কপি সবসময় থাকে।
--
-- ⛔ যা এই স্ক্রিপ্ট কখনো করে না:
--   · কোনো row DELETE করে না (শুধু payload কলাম ফাঁকা করে, বাকি date/reason/
--     status/size/by/createdAt/updatedAt অক্ষত থাকে — ইতিহাস হারায় না)
--   · সবচেয়ে নতুন backup-এর payload ছোঁয় না
--   · অন্য কোনো টেবিল ছোঁয় না

-- ধাপ ১ (শুধু দেখা, নিরাপদ — আগে এটা চালিয়ে তালিকা দেখে নিন):
select id, date, reason, status, size, "createdAt",
       pg_column_size(payload) as payload_bytes
from public.backuprecords
order by "createdAt" desc;

-- ধাপ ২ (আসল কাজ — উপরের তালিকা দেখে সন্তুষ্ট হলে তবেই চালান):
-- সবচেয়ে নতুন ১টা payload বাদে বাকি সব পুরনো payload ফাঁকা করা হচ্ছে।
with newest as (
  select id from public.backuprecords
  order by "createdAt" desc
  limit 1
)
update public.backuprecords
set payload = null
where id not in (select id from newest)
  and payload is not null;

-- ধাপ ৩ (যাচাই — কাজ ঠিকমতো হয়েছে কিনা দেখুন):
select id, date, "createdAt", pg_column_size(payload) as payload_bytes
from public.backuprecords
order by "createdAt" desc;
