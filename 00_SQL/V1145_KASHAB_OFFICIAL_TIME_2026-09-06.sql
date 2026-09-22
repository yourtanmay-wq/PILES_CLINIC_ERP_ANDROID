-- ═══════════════════════════════════════════════════════════════════════════
-- V1145 (০৬.০৯.২০২৬) — KASHAB MANDAL: UNEXPECTED → OFFICIAL TIME
-- TK-নির্দেশ: "অফিসিয়াল টাইমে কল এসেছিল, স্টাফ ভুল করে আনএক্সপেক্টেড করেছে"
--
-- কেন তিনটে টেবিলেই: কার্ডের ব্যাজ পড়ে patients.timeType, টাকার হিসেব পড়ে
-- enquiries.timeType, আর ফলো-আপের সারিও নিজের ঘরে ওই লেখাটা রাখে।
--
-- ⛔ timeSource ঘরে হাত পড়ে না — Official Time-এ ওই ঘরটা দেখাই হয় না।
-- ⛔ টাকার কোনো সারি ছোঁয়া হয় না; পাওনা প্রতিবার নতুন করে গোনা হয়, তাই
--    সময়ের ঘর ঠিক হলেই স্টাফের বাড়তি পাওনা নিজে থেকেই কমে যাবে।
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── ১) আগে কী আছে (কিছু বদলায় না) ─────────────────────────────────────
select 'enquiries' as tbl, "id", "name", "branch", "date", "timeType"
  from public.enquiries  where "mobile" = '9832358300'
union all
select 'patients',  "id", "name", "branch", "date", "timeType"
  from public.patients   where "mobile" = '9832358300'
union all
select 'followups', "id", "name", "branch", "date", "timeType"
  from public.followups  where "mobile" = '9832358300';

-- ─── ২) শুধরানো ────────────────────────────────────────────────────────
update public.enquiries set "timeType" = 'Official Time', "updatedAt" = now()::text
 where "mobile" = '9832358300' and coalesce("timeType",'') <> 'Official Time';

update public.patients  set "timeType" = 'Official Time', "updatedAt" = now()::text
 where "mobile" = '9832358300' and coalesce("timeType",'') <> 'Official Time';

update public.followups set "timeType" = 'Official Time', "updatedAt" = now()::text
 where "mobile" = '9832358300' and coalesce("timeType",'') <> 'Official Time';

-- ─── ৩) এখন কী আছে — তিনটেতেই Official Time হওয়ার কথা ──────────────────
select 'enquiries' as tbl, "name", "branch", "timeType"
  from public.enquiries  where "mobile" = '9832358300'
union all
select 'patients',  "name", "branch", "timeType"
  from public.patients   where "mobile" = '9832358300'
union all
select 'followups', "name", "branch", "timeType"
  from public.followups  where "mobile" = '9832358300';

-- ─── ৪) আর কারও নেই তো (আজকের বাকি সারি ঠিক আছে কিনা দেখার জন্য) ───────
select "timeType", count(*) as koyti
  from public.enquiries where "date" >= '2026-09-01' group by "timeType" order by 1;

-- ═══════════════════════════════════════════════════════════════════════════
-- ৫) ০৬.০৯.২০২৬ — TK-এর ফলাফলে **শুধু patients** সারিটা এসেছে; enquiries ও
--    followups-এ ওই নম্বরে কিছু মেলেনি। অথচ কার্ডে "Enquiry Calls: 3" আছে,
--    তাই সারিগুলো আছেই — নম্বরটা অন্য ধাঁচে জমা (যেমন +91 বা ফাঁক সহ)।
--    ⚠️ টাকার হিসেব পড়ে **enquiries.timeType** — তাই ওটা না বদলালে স্টাফের
--       বাড়তি পাওনা এখনো কমেনি।
-- ⛔ নিচেরটা শুধু খুঁজে দেখা — কিছুই বদলায় না।
-- ═══════════════════════════════════════════════════════════════════════════
select 'enquiries' as tbl, "id", "name", "branch", "mobile", "timeType"
  from public.enquiries
 where regexp_replace(coalesce("mobile",''), '\D', '', 'g') like '%9832358300'
union all
select 'followups', "id", "name", "branch", "mobile", "timeType"
  from public.followups
 where regexp_replace(coalesce("mobile",''), '\D', '', 'g') like '%9832358300';

-- ═══════════════════════════════════════════════════════════════════════════
-- ৬) ০৬.০৯.২০২৬ — TK-এর ফলাফলে ধরা পড়ল: নম্বরটা **`+919832358300`** ধাঁচে
--    জমা, তাই ধাপ ২-এর সমান-মিল (`= '9832358300'`) enquiries ও followups-এ
--    কিছুই ছোঁয়নি। এখানে অঙ্ক-মাত্র মিলিয়ে শুধরানো হলো।
-- 🔴 বড় শিক্ষা: এক নম্বর প্রজেক্টে **দুই ধাঁচে** জমা হচ্ছে — ভবিষ্যতে সব
--    SQL-এ অঙ্ক-মাত্র মিল (regexp_replace) ব্যবহার করতে হবে, সমান-মিল নয়।
-- ═══════════════════════════════════════════════════════════════════════════
update public.enquiries set "timeType" = 'Official Time', "updatedAt" = now()::text
 where regexp_replace(coalesce("mobile",''), '\D', '', 'g') like '%9832358300'
   and coalesce("timeType",'') <> 'Official Time';

update public.followups set "timeType" = 'Official Time', "updatedAt" = now()::text
 where regexp_replace(coalesce("mobile",''), '\D', '', 'g') like '%9832358300'
   and coalesce("timeType",'') <> 'Official Time';

select 'enquiries' as tbl, "name", "branch", "mobile", "timeType"
  from public.enquiries
 where regexp_replace(coalesce("mobile",''), '\D', '', 'g') like '%9832358300'
union all
select 'followups', "name", "branch", "mobile", "timeType"
  from public.followups
 where regexp_replace(coalesce("mobile",''), '\D', '', 'g') like '%9832358300';
