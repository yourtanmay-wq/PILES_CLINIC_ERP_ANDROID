-- ═══════════════════════════════════════════════════════════════════════
--  V592 · App Call গোনা কেন 0 — সত্যিটা দেখার SQL
--  তারিখ: ২৩.০৮.২০২৬
--
--  ⛔⛔ এটা শুধু **দেখার** SQL। একটাও সারি লেখে না, বদলায় না, মোছে না।
--
--  🔵 এটা **একটাই** কোয়েরি — Run চাপলে একটাই টেবিলে সবটা দেখাবে
--     (আগের বার চার টুকরো ছিল, তাই শেষেরটা ছাড়া কিছু দেখা যেত না)।
--  🔵 শুধু `staff_code` আর `call_date` ঘর দুটো ব্যবহার করা হয়েছে — অ্যাপ
--     নিজেই ওই দুটো দিয়েই গোনে, তাই ওগুলো নিশ্চিত আছে।
--
--  Supabase → SQL Editor → পুরোটা পেস্ট → Run → ফলাফলের ছবি পাঠান।
-- ═══════════════════════════════════════════════════════════════════════

SELECT bhag, ek, dui, tin, char FROM (

  -- ── ১ · টেবিলে কোন কোন ঘর আছে, আর তারিখের ডিফল্ট আছে কি না ──────────
  SELECT 1 AS srl,
         '1. GHOR (columns)'::text                       AS bhag,
         column_name::text                               AS ek,
         data_type::text                                 AS dui,
         COALESCE(column_default, '(no default)')::text   AS tin,
         is_nullable::text                               AS char
  FROM information_schema.columns
  WHERE table_schema = 'wn' AND table_name = 'call_taps'

  UNION ALL

  -- ── ২ · কোন নামে কত কল জমা আছে · কতগুলোর তারিখ ফাঁকা ────────────────
  --    এখানেই দেখা যাবে Staff/Branch ID-র কল ঢুকছে কি না।
  SELECT 2,
         '2. KON NAME KOTO'::text,
         staff_code::text,
         count(*)::text,
         count(*) FILTER (WHERE call_date IS NULL)::text,
         COALESCE(max(call_date)::text, '(NO DATE)')
  FROM wn.call_taps
  GROUP BY staff_code

  UNION ALL

  -- ── ৩ · দিনে দিনে কতগুলো ───────────────────────────────────────────
  SELECT 3,
         '3. DINE DINE'::text,
         COALESCE(call_date::text, '(DATE FAKA)'),
         count(*)::text,
         ''::text,
         ''::text
  FROM wn.call_taps
  GROUP BY call_date

) t
ORDER BY srl, ek;
