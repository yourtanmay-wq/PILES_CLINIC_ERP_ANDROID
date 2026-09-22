-- =====================================================================
-- PILES CLINIC — V309 · COOCH BEHAR PARTNER SHARE TIMELINE CORRECTION
-- Owner: TK BISWAS · 10.08.2026 (IST)
--
-- সমস্যা: চারজন অংশীদারকে একসাথে আগস্টে বসানোয় অ্যাপ ধরে নিয়েছে চারজনই
-- ১ জানুয়ারি থেকে অংশীদার (launch), তাই পুরো বছরের লাভ 40/40/10/10 ভাগ করেছে।
-- 🔒 21.09.2026 TK-approved correction (supersedes the old wording below):
--   • 01 Jan → 31 Jul : TK BISWAS 50% · Dr. K.H MANDAL 30% · J.H MANDAL 10% · GOKUL SARKAR 10%
--     (J.H + Gokul-এর 10%+10% Dr. K.H MANDAL-এর মূল 50% অংশ থেকেই)
--   • 01 Aug → আজ পর্যন্ত : TK 40% · K.H 40% · J.H MANDAL 10% · GOKUL SARKAR 10%
--
-- এই ফাইল শুধু Cooch Behar-এর ওই ৪ জনের %-TIMELINE (fin.partner_pct_history)
-- ঠিক করে। ⛔ collections / expenses / drawings / opening / অন্য কোনো ব্রাঞ্চ —
-- কিছুই ছোঁয় না। মোট Net Profit অপরিবর্তিত; শুধু প্রত্যেকের ভাগ তারিখ ধরে
-- ঠিকভাবে ভাগ হয়। বারবার চালালেও নিরাপদ (idempotent)। ফোন+কম্পিউটার দুটোই
-- একই cloud ডেটা পড়ে, তাই এক-রানেই দুটোতে ঠিক হয়ে যায়।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → পুরো ফাইল পেস্ট → Run।
-- =====================================================================
begin;

-- 1) এই ৪ জনের ভুল (সব-জানুয়ারি-থেকে) timeline মুছে ফেলা — শুধু Cooch Behar।
delete from fin.partner_pct_history
 where branch = 'Cooch Behar'
   and mobile in ('8001080080', '7980993652', '9002610352', '7479173399');

-- 2) সঠিক timeline বসানো।
insert into fin.partner_pct_history
       (partner_id, branch, mobile, pct, effective_from, created_by)
select p.id, p.branch, p.mobile, v.pct, v.eff, 'V309-correction'
from fin.partners p
join (values
  -- TK BISWAS : 50% জানুয়ারি থেকে, 40% আগস্ট থেকে
  ('8001080080', 50::numeric, date '2026-01-01'),
  ('8001080080', 40::numeric, date '2026-08-01'),
  -- Dr. K.H MANDAL : 30% জানুয়ারি থেকে, 40% আগস্ট থেকে
  ('7980993652', 30::numeric, date '2026-01-01'),
  ('7980993652', 40::numeric, date '2026-08-01'),
  -- GOKUL SARKAR : 10% জানুয়ারি থেকেই (K.H-এর মূল 50% অংশ থেকে)
  ('9002610352', 10::numeric, date '2026-01-01'),
  ('9002610352', 10::numeric, date '2026-08-01'),
  -- J.H MANDAL : 10% জানুয়ারি থেকেই (K.H-এর মূল 50% অংশ থেকে)
  ('7479173399', 10::numeric, date '2026-01-01'),
  ('7479173399', 10::numeric, date '2026-08-01')
) as v(mobile, pct, eff)
  on v.mobile = p.mobile
where p.branch = 'Cooch Behar';

commit;

-- 3) যাচাই (শুধু-দেখা, চাইলে চালান):
-- select p.name, h.pct, h.effective_from
--   from fin.partners p
--   join fin.partner_pct_history h
--     on h.branch = p.branch and h.mobile = p.mobile
--  where p.branch = 'Cooch Behar'
--  order by p.name, h.effective_from;
