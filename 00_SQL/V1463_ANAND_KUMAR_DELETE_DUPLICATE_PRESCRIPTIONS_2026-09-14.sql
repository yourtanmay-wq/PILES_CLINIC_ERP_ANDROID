-- ═══════════════════════════════════════════════════════════════════════════
-- V1463 (১৪.০৯.২০২৬) — ANAND KUMAR (KNE-21082026-002)-এর ৬টা ডুপ্লিকেট
-- Prescription সারি মোছা, TK-র "হ্যাঁ, মুছে দিন" অনুমোদনে (V1462-এর ফল
-- দেখে বাছাই করা, একটা একটা করে id ধরে — কোনো অনুমান/pattern-match নয়)।
--
-- রাখা হচ্ছে (প্রতিটা রকমের প্রথমটা):
--   med_b7d1b3ecb5494f6985ed125b900b15f7  (16:27:52 — Sameerpanag/Rasamanikya/Kankayan)
--   med_69206a4623ef46cfb4d66d36b6c6f218  (16:40:12 — Arshakuthar/Bolbadha)
--
-- মোছা হচ্ছে এই ৬টাই (id ধরে, patientId দিয়েও দ্বিতীয়বার বাঁধা — অন্য কোনো
-- রোগীর সারি ভুল করেও ছুঁতে পারবে না):
--   med_60284cbc0833411197c61ab375d216d5  (16:32:07)
--   med_c03405e2ce4343ae95af465328de17cf  (16:32:44)
--   med_41dac1276e65427baff664ac0c1b36f0  (16:36:21)
--   med_4a76858f116b4da0b30d74fadb1807d6  (16:39:06)
--   med_e9ad93ce57864b8aa2c0af14e4e5d41e  (16:49:31)
--   med_0ab63e21e828464fa4b1018ef7c7a8fb  (16:50:03)
--
-- Supabase → SQL Editor → New query → Run। ৬টা সারি "DELETE 6" দেখাবে।
-- ═══════════════════════════════════════════════════════════════════════════

delete from public.medical
where "patientId" = 'KNE-21082026-002'
  and id in (
    'med_60284cbc0833411197c61ab375d216d5',
    'med_c03405e2ce4343ae95af465328de17cf',
    'med_41dac1276e65427baff664ac0c1b36f0',
    'med_4a76858f116b4da0b30d74fadb1807d6',
    'med_e9ad93ce57864b8aa2c0af14e4e5d41e',
    'med_0ab63e21e828464fa4b1018ef7c7a8fb'
  )
returning id, "patientId", type, date, "createdAt";
