-- ============================================================
-- ব্রাঞ্চ গোলমাল খোঁজা ও ঠিক করা — 2026-07-26
-- Supabase → SQL Editor-এ চালাতে হবে।
-- ধাপ ১ শুধু দেখায়, কিছু বদলায় না। ধাপ ২ বদলায়।
-- ধাপ ১ চালিয়ে তালিকা দেখে নিশ্চিত হয়ে তবেই ধাপ ২ চালাবেন।
-- ============================================================

-- ------------------------------------------------------------
-- ধাপ ১ (নিরাপদ, শুধু দেখা): যেসব patients সারিতে Patient ID-র
-- ব্রাঞ্চ কোড আর branch ঘরের লেখা মিলছে না।
-- COB=Cooch Behar, JPE=Jalpaiguri, KNE=Kishanganj,
-- FLK=Falakata, BIR=Birpara
-- ------------------------------------------------------------
SELECT
    "patientId",
    name,
    mobile,
    branch                                   AS branch_ekhon_ache,
    CASE split_part("patientId", '-', 1)
        WHEN 'COB' THEN 'Cooch Behar'
        WHEN 'JPE' THEN 'Jalpaiguri'
        WHEN 'KNE' THEN 'Kishanganj'
        WHEN 'FLK' THEN 'Falakata'
        WHEN 'BIR' THEN 'Birpara'
    END                                      AS branch_hoya_uchit,
    bill,
    "registrationDate"
FROM patients
WHERE "patientId" IS NOT NULL
  AND split_part("patientId", '-', 1) IN ('COB','JPE','KNE','FLK','BIR')
  AND lower(coalesce(branch, '')) <> lower(
        CASE split_part("patientId", '-', 1)
            WHEN 'COB' THEN 'Cooch Behar'
            WHEN 'JPE' THEN 'Jalpaiguri'
            WHEN 'KNE' THEN 'Kishanganj'
            WHEN 'FLK' THEN 'Falakata'
            WHEN 'BIR' THEN 'Birpara'
        END)
ORDER BY "registrationDate" DESC;


-- ------------------------------------------------------------
-- ধাপ ১ক (নিরাপদ, শুধু দেখা): একই মোবাইল একাধিক ব্রাঞ্চে আছে
-- কিনা — এখান থেকেই টাকা ভুল রেকর্ডে বসার সমস্যা হয়।
-- ------------------------------------------------------------
SELECT
    right(regexp_replace(mobile, '\D', '', 'g'), 10) AS mobile_10,
    count(*)                                          AS koyta_record,
    string_agg("patientId" || ' [' || coalesce(branch,'—') || ']', '  |  ') AS records
FROM patients
GROUP BY 1
HAVING count(*) > 1
ORDER BY 2 DESC;


-- ============================================================
-- ধাপ ২ (বদলায়): উপরের তালিকা দেখে নিশ্চিত হলে তবেই চালাবেন।
-- Patient ID-কে সত্যি ধরে branch ঘরটা ঠিক করে দেয়।
-- ⚠️ কোনো রোগী সত্যিই অন্য ব্রাঞ্চে বদলি হয়ে থাকলে সেই সারিটা
--    ধাপ ১-এর তালিকা থেকে আগে বাদ দিয়ে নিন (নিচে AND শর্ত যোগ
--    করে, যেমন:  AND "patientId" <> 'COB-26072026-001'  )।
-- ============================================================
-- UPDATE patients
-- SET branch = CASE split_part("patientId", '-', 1)
--         WHEN 'COB' THEN 'Cooch Behar'
--         WHEN 'JPE' THEN 'Jalpaiguri'
--         WHEN 'KNE' THEN 'Kishanganj'
--         WHEN 'FLK' THEN 'Falakata'
--         WHEN 'BIR' THEN 'Birpara'
--     END,
--     "updatedAt" = now()
-- WHERE "patientId" IS NOT NULL
--   AND split_part("patientId", '-', 1) IN ('COB','JPE','KNE','FLK','BIR')
--   AND lower(coalesce(branch, '')) <> lower(
--         CASE split_part("patientId", '-', 1)
--             WHEN 'COB' THEN 'Cooch Behar'
--             WHEN 'JPE' THEN 'Jalpaiguri'
--             WHEN 'KNE' THEN 'Kishanganj'
--             WHEN 'FLK' THEN 'Falakata'
--             WHEN 'BIR' THEN 'Birpara'
--         END);

-- ধাপ ২ক: followups সারির branch-ও একই ভাবে patients থেকে মিলিয়ে দেওয়া।
-- UPDATE followups f
-- SET branch = p.branch, "updatedAt" = now()
-- FROM patients p
-- WHERE right(regexp_replace(f.mobile, '\D', '', 'g'), 10)
--     = right(regexp_replace(p.mobile, '\D', '', 'g'), 10)
--   AND coalesce(f.branch, '') <> coalesce(p.branch, '')
--   AND coalesce(p.branch, '') <> '';

-- 🔒 LOCK NOTE: এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন
-- TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।
