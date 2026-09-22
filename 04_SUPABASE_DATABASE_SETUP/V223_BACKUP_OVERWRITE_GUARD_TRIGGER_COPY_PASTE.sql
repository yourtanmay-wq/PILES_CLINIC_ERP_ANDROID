-- ════════════════════════════════════════════════════════════════════════
-- V223 — Backup/Restore OVERWRITE GUARD (Database Trigger)  ·  COPY-PASTE
-- ════════════════════════════════════════════════════════════════════════
-- মালিক: TK BISWAS · তারিখ: 01.08.2026 IST
-- (App code নিজে থেকেই Restore-পথে নিরাপদ — cloud না-পড়া গেলে লেখা বন্ধ; এই
--  Trigger সব পথের, অন্য ফোন সহ, Database-স্তরের সর্বজনীন দ্বিতীয় পাহারা।)
--
-- এটা কী করে (সহজ বাংলায়):
--   পুরোনো Data (Backup/Trash/Local/Web Restore বা আটকে থাকা পুরোনো Pending
--   UPSERT থেকে) যেন **নতুন Cloud Data চাপা দিতে না পারে**। প্রতিটা মূল টেবিলে
--   একটা পাহারা বসে — কেউ যদি এমন একটা row লিখতে চায় যার `updatedAt` **পুরোনো**
--   (মানে এখন cloud-এ যেটা আছে সেটার চেয়ে আগের), তাহলে সেই লেখা **বাতিল** হয়,
--   cloud-এর নতুন row-টাই থেকে যায়। সংঘর্ষ হলে সবসময় **নতুন Data জেতে**।
--
--   ⛔ এটা App-এর কাজের কোনো নিয়ম/ডিজাইন/টাকা-হিসাব বদলায় না — শুধু "পুরোনো
--      দিয়ে নতুন চাপা দেওয়া" আটকায়। App-code-এও আলাদা পাহারা আছে (Trash/JSON
--      restore-এ newer-wins); এই Trigger হলো **সব পথের জন্য একটাই নিশ্চিত
--      Database-স্তরের পাহারা** (অন্য ফোন/ব্রাউজার থেকে এলেও কাজ করে)।
--
-- ────────────────────────────────────────────────────────────────────────
-- কখন / কীভাবে চালাবেন (ধাপে ধাপে):
--   1) আগে একটা LIVE BACKUP নিন (App → Settings → Backup Now, অথবা Supabase
--      dashboard থেকে)। এটা বাধ্যতামূলক।
--   2) Supabase Dashboard → SQL Editor খুলুন।
--   3) নিচের "PART 1" ও "PART 2" (দুটো function) একসাথে চালান — একবারই।
--   4) তারপর "PART 3" থেকে **শুধু একটা টেবিল** (যেমন `payments`)-এর দুই লাইন
--      চালিয়ে ওই এক টেবিলে টেস্ট করুন (নিচের PART 5 verify + হাতে যাচাই)।
--   5) সব ঠিক থাকলে "PART 3"-এর বাকি টেবিলগুলোও চালান।
--   6) কিছু ভুল মনে হলে "PART 6" (রোলব্যাক) চালিয়ে সব Trigger তুলে ফেলুন —
--      কোনো Data নষ্ট হয় না, শুধু পাহারা উঠে যায়।
--
--   ⚠️ এই ফাইলটা নিজে থেকে চলবে না — আপনি নিজে হাতে চালাবেন। (আমি চালাইনি।)
-- ════════════════════════════════════════════════════════════════════════


-- ════════════════════════════════════════════════════════════════════════
-- PART 1 — নিরাপদ সময়-পড়া (text → timestamptz, ভাঙবে না)
-- ────────────────────────────────────────────────────────────────────────
-- `updatedAt` কলামটা text (যেমন "2026-07-31T18:00:00.123Z")। এটা timestamptz-এ
-- বদলে তুলনা করা হয়। কিন্তু কোনো row-এ যদি ফাঁকা/অদ্ভুত/NULL লেখা থাকে, cast
-- ভাঙলে গোটা লেখা fail করত — তাই এই helper ভাঙে না, ওরকম হলে NULL ফেরত দেয়
-- (আর NULL মানে "জানি না" — পাহারা তখন আটকায় না, নিচে দেখুন)।
CREATE OR REPLACE FUNCTION public._rk_safe_ts(t text)
RETURNS timestamptz AS $$
BEGIN
  IF t IS NULL OR btrim(t) = '' THEN
    RETURN NULL;
  END IF;
  RETURN t::timestamptz;
EXCEPTION WHEN others THEN
  RETURN NULL;   -- পার্স না হলে "জানি না" — পাহারা আটকাবে না
END;
-- STABLE (IMMUTABLE নয়): text→timestamptz offset-হীন লেখায় session-TZ-নির্ভর হতে
-- পারে। আমাদের লেখায় 'Z' থাকে তবু নিরাপত্তার জন্য STABLE। শুধু trigger-এর ভিতরে
-- per-row ডাকা হয় (index/cached-plan-এ নয়), তাই কোনো পার্থক্য হয় না।
$$ LANGUAGE plpgsql STABLE;


-- ════════════════════════════════════════════════════════════════════════
-- PART 2 — পাহারা (পুরোনো দিয়ে নতুন চাপা দেওয়া আটকায়)
-- ────────────────────────────────────────────────────────────────────────
-- নিয়ম: শুধু তখনই লেখা বাতিল হয় যখন —
--   (ক) আসছে-লেখার updatedAt **আছে** (NULL/ফাঁকা নয়), এবং
--   (খ) cloud-এর বর্তমান updatedAt-ও **আছে**, এবং
--   (গ) আসছে-লেখা cloud-এর চেয়ে **কড়া পুরোনো** (NEW < OLD)।
-- এই তিনটে একসাথে সত্যি হলে → পুরোনো লেখা বাদ, cloud-এর নতুন row থাকে (RETURN OLD)।
-- অন্য সব ক্ষেত্রে লেখা স্বাভাবিকভাবে হয় (RETURN NEW)।
--
-- ✅ কেন এটা যাচাই করা নিরাপদ (আপনার ৫টি চিন্তা মিলিয়ে):
--   • NULL/ফাঁকা date: NEW বা OLD-এর যেকোনোটা NULL হলে শর্ত মিথ্যা → **আটকায় না**
--     (পুরোনো/legacy NULL-row বা date-হীন লেখা ভাঙবে না)।
--   • পুরোনো/অদ্ভুত timestamp format: _rk_safe_ts পার্স না পারলে NULL → **আটকায় না**।
--   • Healing write (createdAt-কে updatedAt বসিয়ে পুরোনো heal-লেখা): cloud যদি
--     আসলেই নবীন হয় তবে heal বাদ পড়বে (ঠিক — নতুন থাকবে); cloud পুরোনো/সমান/নেই
--     হলে heal বসবে। অর্থাৎ heal শুধু তখনই বাদ পড়ে যখন সেটা সত্যিই পুরোনো — নিরাপদ।
--   • Subset UPDATE (updatedAt না পাঠিয়ে শুধু ২-১টা ঘর বদল): তখন NEW.updatedAt =
--     OLD.updatedAt (অপরিবর্তিত) → NEW < OLD মিথ্যা → **আটকায় না** (remark/status-only
--     PATCH আগের মতোই বসে)।
--   • Legacy sync (SyncManager, বর্তমানে নিষ্ক্রিয়): চালু হলেও তার লেখায় updatedAt
--     থাকে, তাই এই একই তুলনায় নিরাপদে ধরা পড়ে।
CREATE OR REPLACE FUNCTION public._rk_guard_no_older_overwrite()
RETURNS trigger AS $$
DECLARE
  n timestamptz := public._rk_safe_ts(NEW."updatedAt");
  o timestamptz := public._rk_safe_ts(OLD."updatedAt");
BEGIN
  IF n IS NOT NULL AND o IS NOT NULL AND n < o THEN
    RETURN OLD;   -- আসছে-লেখা পুরোনো → বাদ, cloud-এর নতুন row-ই থাকে
  END IF;
  RETURN NEW;     -- বাকি সব ক্ষেত্রে স্বাভাবিক
END;
$$ LANGUAGE plpgsql;


-- ════════════════════════════════════════════════════════════════════════
-- PART 3 — প্রতিটা মূল টেবিলে পাহারা বসানো
-- ────────────────────────────────────────────────────────────────────────
-- ⚠️ প্রথমে শুধু একটা টেবিল (payments) চালিয়ে টেস্ট করুন, তারপর বাকিগুলো।
-- (BEFORE UPDATE — তাই নতুন row INSERT-এ কিছু হয় না; শুধু একই id-তে সংঘর্ষ হলে
--  পাহারা চলে। প্রতিটা টেবিলের আগে DROP আছে যাতে দুবার চালালেও সমস্যা না হয়।)

-- payments (আগে এটাই টেস্ট করুন)
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.payments;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.payments
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- patients
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.patients;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.patients
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- followups
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.followups;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.followups
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- enquiries
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.enquiries;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.enquiries
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- medical
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.medical;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.medical
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- doctor_visits
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.doctor_visits;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.doctor_visits
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- briefings
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.briefings;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.briefings
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- products
DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.products;
CREATE TRIGGER rk_guard_no_older_overwrite BEFORE UPDATE ON public.products
  FOR EACH ROW EXECUTE FUNCTION public._rk_guard_no_older_overwrite();

-- ⛔ ইচ্ছাকৃতভাবে বাদ রাখা: trash, deleted_records, activity_logs, usercredentials,
--    এবং approval/request টেবিল (payment_backdate_requests, payment_edit_requests,
--    chamber_close) — এগুলোর কাজের ধরন আলাদা (tombstone/লগ/লগইন/অনুমোদন), তাই
--    updatedAt-পাহারা এখানে বসানো হয়নি যাতে কোনো workflow ভুল করে না আটকায়।
--    দরকার হলে TK-এর সিদ্ধান্তে পরে যোগ করা যাবে (একই দুই লাইন)।


-- ════════════════════════════════════════════════════════════════════════
-- PART 4 — (ঐচ্ছিক) কোন টেবিলে পাহারা আছে দেখা
-- ────────────────────────────────────────────────────────────────────────
-- SELECT event_object_table AS table_name, trigger_name
-- FROM information_schema.triggers
-- WHERE trigger_name = 'rk_guard_no_older_overwrite'
-- ORDER BY table_name;


-- ════════════════════════════════════════════════════════════════════════
-- PART 5 — এক-টেবিল টেস্ট (payments-এ, Data নষ্ট না করে)
-- ────────────────────────────────────────────────────────────────────────
-- একটা সত্যিকারের id নিয়ে নিচের মতো করে দেখুন (id বসান):
--   ধাপ-ক) এখনকার updatedAt দেখুন:
--     SELECT id, "updatedAt" FROM public.payments WHERE id = '<PAY_ID>';
--   ধাপ-খ) **পুরোনো** updatedAt দিয়ে remark বদলের চেষ্টা — বদলানো উচিত নয়:
--     UPDATE public.payments
--       SET remarks = 'TEST_OLD', "updatedAt" = '2000-01-01T00:00:00.000Z'
--       WHERE id = '<PAY_ID>';
--     SELECT id, remarks, "updatedAt" FROM public.payments WHERE id = '<PAY_ID>';
--     -- remarks 'TEST_OLD' হওয়া **উচিত নয়** (পাহারা আটকেছে)।
--   ধাপ-গ) **নতুন** updatedAt দিয়ে — বদলানো উচিত:
--     UPDATE public.payments
--       SET remarks = 'TEST_NEW', "updatedAt" = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
--       WHERE id = '<PAY_ID>';
--     -- এবার remarks 'TEST_NEW' হওয়া **উচিত** (নতুন জেতে)।
--   ধাপ-ঘ) টেস্টের remark ফেরত দিন (নতুন updatedAt দিয়ে) — আসল remark বসান।


-- ════════════════════════════════════════════════════════════════════════
-- PART 6 — রোলব্যাক (সব পাহারা তুলে ফেলা) — দরকার হলে
-- ────────────────────────────────────────────────────────────────────────
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.payments;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.patients;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.followups;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.enquiries;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.medical;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.doctor_visits;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.briefings;
-- DROP TRIGGER IF EXISTS rk_guard_no_older_overwrite ON public.products;
-- DROP FUNCTION IF EXISTS public._rk_guard_no_older_overwrite();
-- DROP FUNCTION IF EXISTS public._rk_safe_ts(text);
-- ════════════════════════════════════════════════════════════════════════
