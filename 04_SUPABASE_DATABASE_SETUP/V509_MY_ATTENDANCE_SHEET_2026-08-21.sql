-- ════════════════════════════════════════════════════════════════════════
-- 🗓️ V509 (২১.০৮.২০২৬, TK-নির্দেশ) — **স্টাফ নিজের হাজিরা-খাতা দেখতে পাবেন।**
--
-- TK-এর কথা: *"staff এর এখানে attendance sheet এরকম থাকবে, যাতে সে দেখতে
-- পারে সারা মাসে কোন সময় এসেছে এবং কোন সময় ক্লিনিক থেকে গেছে, কবে সে ছুটি
-- নিয়েছিল।"*
--
-- এই ফাংশনটা **আগে থেকেই ছিল** এবং ঠিকঠাক কাজ করে — শুধু ভিতরের পাহারায়
-- লেখা ছিল `hr.is_master()`, অর্থাৎ **শুধু মাস্টার**। স্টাফ ডাকলে ফাঁকা ফিরত।
--
-- ⇒ এখানে **একটি মাত্র লাইন** বদলানো হলো:
--       where hr.is_master()
--   →   where hr.is_master() or p_code = hr.my_code()
--
-- ⛔ নিরাপত্তা এক চুলও আলগা হয়নি:
--    • স্টাফ **শুধু নিজের কোডের** সারি দেখতে পান (`hr.my_code()` সার্ভার নিজে
--      লগইন-টোকেন থেকে বার করে — ফোন থেকে পাঠানো কিছু নয়, তাই জাল করা যায় না)।
--    • অন্য কারও কোড দিলে আগের মতোই **ফাঁকা** ফেরে।
--    • মাস্টারের ক্ষমতা অপরিবর্তিত — সবার খাতা আগের মতোই দেখেন।
--    • ফেরত আসে শুধু চারটে ঘর: তারিখ · IN · OUT · ছুটি। বেতন/টাকা/রোগী নয়।
-- ⛔ কোনো টেবিল · কলাম · সারি তৈরি বা মোছা হয় না — শুধু এই একটি ফাংশন
--    আবার তৈরি (create or replace)।
-- ⚡ Egress: মাসে একবার চাপলে একটাই ছোট ফল (সর্বোচ্চ ৩১ সারি)।
-- ════════════════════════════════════════════════════════════════════════

create or replace function hr.perf_attendance_sheet(p_month text, p_code text)
returns table(work_date text, check_in text, check_out text, is_leave boolean)
language sql stable security definer set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master() or p_code = hr.my_code()),
  mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ),
  days as (
    select to_char(n.work_date, 'YYYY-MM-DD') as d, n.check_in, n.check_out
    from wn.notebook_days n, mon, guard
    where left(to_char(n.work_date, 'YYYY-MM-DD'), mon.klen) = mon.key
      and n.staff_code = p_code
  ),
  leaves as (
    select to_char(l.leave_date, 'YYYY-MM-DD') as d
    from wn.leave_requests l, mon, guard
    where left(to_char(l.leave_date, 'YYYY-MM-DD'), mon.klen) = mon.key
      and l.staff_code = p_code
      and lower(coalesce(l.status,'')) in ('approved','accepted')
  )
  select coalesce(days.d, leaves.d), days.check_in, days.check_out, (leaves.d is not null)
  from days full outer join leaves on days.d = leaves.d
  order by 1 desc;
$fn$;
revoke all on function hr.perf_attendance_sheet(text, text) from public, anon;
grant execute on function hr.perf_attendance_sheet(text, text) to authenticated;
