-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার (V1194, ০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ):
--   • "কেউ যদি ভুল করে বার্তাটা পাঠিয়ে দেয় তাহলে সে ডিলিট করতে পারবে"
--     ⇒ বোতামের লেখা **Cancel** (TK: *"ডিলিট লেখা থাকলে তো বিভ্রান্ত হতে পারে"*)
--   • "রোগের নাম দরকার তো"
--
-- চারটে নতুন ঘর —
--   disease         : রোগের নাম (পাঠানোর সময় রোগীর সারি থেকে বসে)
--   cancelledBy     : যিনি বাতিল করলেন তাঁর মোবাইল
--   cancelledByName : তাঁর নাম
--   cancelledAt     : কখন বাতিল হলো
--
-- ⚠️ শুধু চারটে নতুন কলাম যোগ হয় — পুরনো কোনো ঘর/সারি/ডেটা ছোঁয়া হয় না।
-- সারিটা **মুছে ফেলা হয় না**, তাই History-তে "CANCELLED · কে · কখন" চিরকাল
-- লেখা থাকে (TK-র নিয়ম: *"যাতে কেউ অস্বীকার না করতে পারে"*)।
-- একাধিকবার চালালেও কোনো সমস্যা নেই।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard → SQL Editor → New query
-- ২) নিচের পুরো লেখাটা কপি-পেস্ট করে Run (সবুজ "Success" দেখা উচিত)
-- ============================================================

alter table public.doctor_reminders
    add column if not exists "disease" text default '';

alter table public.doctor_reminders
    add column if not exists "cancelledBy" text default '';

alter table public.doctor_reminders
    add column if not exists "cancelledByName" text default '';

alter table public.doctor_reminders
    add column if not exists "cancelledAt" text default '';

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
