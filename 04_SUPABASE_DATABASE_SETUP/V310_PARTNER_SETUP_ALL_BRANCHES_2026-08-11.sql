-- =====================================================================
-- PILES CLINIC — V310 · PARTNER SETUP (all branches) + ENABLE DOCTOR
--                 INCOME/EXPENSE ENTRY
-- Owner: TK BISWAS · 11.08.2026 (IST)
--
-- উদ্দেশ্য (TK-নির্দেশ, খুঁটিয়ে যাচাই করে বসানো ভাগ):
--   • প্রতিটি ব্রাঞ্চের অংশীদার ও তাদের %-ভাগ ঠিকভাবে বসানো — "একদম প্রথম
--     থেকে" মানে ০১.০১.২০২৬ থেকে (অ্যাপের Net Profit "Jan → আজ" ধরেই চলে)।
--   • যে ডাক্তার-অংশীদাররা নিজের ব্রাঞ্চে আয়-ব্যয় লিখবেন, তাঁদের
--     can_entry = true (master-কে হাত দিয়ে টগল করতে হবে না)।
--
-- ভাগ (যোগফল প্রতি ব্রাঞ্চে ১০০):
--   Kishanganj : TK BISWAS 100
--   Jalpaiguri : TK 50 · Dr. Jay Banik 50
--   Falakata   : Dr. Saikat Roy 100
--   Birpara    : TK 30 · Dr. Pranab Biswas 40 · Dr. Saikat Roy 30
--   Cooch Behar: (V309-এ আগেই বসানো — এই ফাইল ওর %-এ হাত দেয় না,
--                শুধু ৩ ডাক্তারের can_entry চালু করে)
--
-- আয়-ব্যয় লিখতে পারবেন (নিজের ব্রাঞ্চে, can_entry=true):
--   Cooch Behar: K.H Mandal · J.H Mandal · Gokul Sarkar
--   Jalpaiguri : Jay Banik    | Birpara: Pranab | Falakata: Saikat
--   (Saikat বীরপাড়ায় নীরব অংশীদার — ওখানে can_entry=false, লেখেন Pranab)
--   Amit Goldar · P.K Roy — অংশীদার নন, লিখতে পারবেন না।
--
-- মোবাইল: TK 8001080080 · Jay Banik 8001800148 · Pranab 9242009205 ·
--   Saikat 7810907954 · K.H Mandal 7980993652 · J.H Mandal 7479173399 ·
--   Gokul 9002610352
--
-- ⛔ নিরাপত্তা: collections/expenses/drawings/opening — কিছুই ছোঁয় না।
--   বিদ্যমান কোনো অংশীদারের pct/opening OVERWRITE করে না (ON CONFLICT-এ
--   শুধু can_entry ও active সেট হয়)। Cooch Behar-এর %-timeline অটুট।
--   বারবার চালানো নিরাপদ (idempotent)।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → পুরো ফাইল পেস্ট → Run।
-- =====================================================================
begin;

-- ---------------------------------------------------------------------
-- 1) অংশীদার-সারি (নতুন ব্রাঞ্চ). আগে থেকে থাকলে pct/opening বদলায় না —
--    শুধু can_entry ও active নিশ্চিত করা হয়।
-- ---------------------------------------------------------------------
insert into fin.partners
       (branch, mobile, name, pct, can_entry, in_app, active, created_by)
values
  -- Kishanganj (পুরোটা master)
  ('Kishanganj','8001080080','TK BISWAS',        100, false, true, true, 'V310-setup'),
  -- Jalpaiguri
  ('Jalpaiguri','8001080080','TK BISWAS',         50, false, true, true, 'V310-setup'),
  ('Jalpaiguri','8001800148','Dr. Jay Banik',     50, true,  true, true, 'V310-setup'),
  -- Falakata
  ('Falakata',  '7810907954','Dr. Saikat Roy',   100, true,  true, true, 'V310-setup'),
  -- Birpara
  ('Birpara',   '8001080080','TK BISWAS',         30, false, true, true, 'V310-setup'),
  ('Birpara',   '9242009205','Dr. Pranab Biswas', 40, true,  true, true, 'V310-setup'),
  ('Birpara',   '7810907954','Dr. Saikat Roy',    30, false, true, true, 'V310-setup')
on conflict (branch, mobile) do update
  set can_entry = excluded.can_entry,
      active    = true,
      updated_at = now();
--  ⛑️ NOTE: এখানে pct ইচ্ছে করেই আপডেট করা হয়নি — আগে থেকে বসানো কারো
--     ভাগ নষ্ট না হয়। নতুন সারির pct উপরের VALUES থেকেই বসে।

-- ---------------------------------------------------------------------
-- 2) Cooch Behar-এর ৩ ডাক্তার-অংশীদারের can_entry চালু (সারি আগেই আছে;
--    %-এ হাত দিচ্ছি না)। master (TK)-এর জন্য can_entry দরকার নেই।
-- ---------------------------------------------------------------------
update fin.partners
   set can_entry = true, updated_at = now()
 where branch = 'Cooch Behar'
   and mobile in ('7980993652','7479173399','9002610352');

-- ---------------------------------------------------------------------
-- 3) %-TIMELINE (fin.partner_pct_history) — শুধু এই নতুন ব্রাঞ্চগুলোর জন্য,
--    একদম প্রথম থেকে = ০১.০১.২০২৬. Cooch Behar অটুট (V309)।
--    আগে ভুল টাইমলাইন থাকলে সরিয়ে সঠিকটা বসানো হয় (idempotent)।
-- ---------------------------------------------------------------------
delete from fin.partner_pct_history
 where (branch, mobile) in (
   ('Kishanganj','8001080080'),
   ('Jalpaiguri','8001080080'), ('Jalpaiguri','8001800148'),
   ('Falakata',  '7810907954'),
   ('Birpara',   '8001080080'), ('Birpara','9242009205'), ('Birpara','7810907954')
 );

insert into fin.partner_pct_history
       (partner_id, branch, mobile, pct, effective_from, created_by)
select p.id, p.branch, p.mobile, v.pct, date '2026-01-01', 'V310-setup'
from fin.partners p
join (values
  ('Kishanganj','8001080080', 100::numeric),
  ('Jalpaiguri','8001080080',  50::numeric),
  ('Jalpaiguri','8001800148',  50::numeric),
  ('Falakata',  '7810907954', 100::numeric),
  ('Birpara',   '8001080080',  30::numeric),
  ('Birpara',   '9242009205',  40::numeric),
  ('Birpara',   '7810907954',  30::numeric)
) as v(branch, mobile, pct)
  on v.branch = p.branch and v.mobile = p.mobile;

commit;

-- ---------------------------------------------------------------------
-- 4) যাচাই (শুধু-দেখা, চাইলে চালান):
-- select p.branch, p.name, p.mobile, p.pct, p.can_entry, h.effective_from
--   from fin.partners p
--   left join fin.partner_pct_history h
--     on h.branch = p.branch and h.mobile = p.mobile
--  where p.branch in ('Kishanganj','Jalpaiguri','Falakata','Birpara','Cooch Behar')
--  order by p.branch, p.pct desc, h.effective_from;
-- =====================================================================
