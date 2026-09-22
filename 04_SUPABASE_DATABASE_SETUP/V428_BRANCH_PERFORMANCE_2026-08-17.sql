-- ============================================================================
-- V428 — চেম্বারের সাধারণ নম্বরে করা কাজ **ব্রাঞ্চের নিজের** হিসাবে
--   TK-নির্দেশ ১৭.০৮.২০২৬: *"8514002200 — ওটা ব্রাঞ্চ হিসাবে ই গন্য হোক"*
--
--   কেন দরকার: আজ কোচবিহারে মোট ₹৪৫,৭০০ উঠেছে, কিন্তু Performance-এ দেখাচ্ছিল
--   ₹৩৮,২০০ — কারণ ₹৭,৫০০ (৯টি পেমেন্ট) তোলা হয়েছে চেম্বারের সাধারণ লগইন
--   8514002200 দিয়ে, যেটা কোনো staff-এর সঙ্গে বাঁধা নেই। TK-এর সিদ্ধান্ত:
--   ওই কাজ **ব্রাঞ্চের নিজের** হিসেবে দেখানো হবে — কারও নামে বসানো হবে না।
--
--   ⛔ TK একটা নম্বরের কথা বলেছেন; একই অবস্থা বাকি সাধারণ নম্বরগুলোরও
--      (8436002200 · 8001080080 · 6294178845 · 8514001100)। তাই নিয়মটা
--      **সব ক্ষেত্রেই এক**: যে নম্বর কোনো active staff-এর সঙ্গে মেলে না, তার
--      কাজ ওই ব্রাঞ্চের নিজের হিসাবে ওঠে। ভুল মনে হলে TK বললেই বদলে দেব।
--
--   ⛔ `hr.staff_performance()`-এ **এক অক্ষরও হাত দেওয়া হয়নি** — সেটা পরীক্ষিত
--      ও চালু। এটা আলাদা একটা ছোট ফাংশন; অ্যাপ দুটোর ফল জুড়ে দেখায়।
--   ⛔ শুধু পড়ে (stable) · পাহারা hr.is_master() — staff_performance-এর মতোই।
--   ⛔ হিসাবের সূত্র hr.staff_performance()-এর হুবহু নকল, শুধু "staff-এর সঙ্গে
--      মেলে" শর্তটা উল্টে "কারও সঙ্গে মেলে না" করা হয়েছে।
-- ============================================================================

create or replace function hr.branch_performance(p_month text)
returns table(
  person_code        text,
  full_name          text,
  branch             text,
  enquiry_count      int,
  registration_count int,
  treatment_count    int,
  rmp_added          int,
  app_calls          int,
  outside_calls      int,
  cash_collected     numeric,
  online_collected   numeric,
  present_days       int,
  reports_sent       int,
  leave_days         int
)
language sql
stable
security definer
set search_path = hr, public, wn
as $fn$
  with guard as (
    select 1 as ok where hr.is_master()
  ), mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), known as (
    -- চালু (ডাক্তার বাদে) সব staff-এর মোবাইল — এদের কাজ ব্রাঞ্চে গোনা হবে না
    select hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s
    where s.active is not false
      and lower(coalesce(s.role_kind, '')) <> 'doctor'
      and upper(coalesce(s.person_code, '')) not like 'DR-%'
      and length(hr.perf_m10(s.link_mobile)) = 10
  ), br as (
    select unnest(array['Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara']) as b
  )
  select
    ('BRANCH-' || br.b)      as person_code,
    (upper(br.b) || ' (BRANCH)') as full_name,
    br.b                     as branch,

    (select count(*)::int from public.enquiries e, mon
       where left(coalesce(e."date", ''), mon.klen) = mon.key
         and e."branch" = br.b
         and length(hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy"))) = 10
         and hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy"))
             not in (select k.m10 from known k)
    ) as enquiry_count,

    (select count(*)::int from public.patients p, mon
       where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
         and p."branch" = br.b
         and length(hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy"))) = 10
         and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy"))
             not in (select k.m10 from known k)
    ) as registration_count,

    (select count(*)::int from public.patients p, mon
       where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
         and p."branch" = br.b
         and length(hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy"))) = 10
         and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy"))
             not in (select k.m10 from known k)
         and exists (select 1 from public.payments y
                      where y."patientId" = p.id
                        and lower(coalesce(y."payType",'')) = 'treatment'
                        and hr.perf_num(y."amount") > 0)
    ) as treatment_count,

    (select count(*)::int from public.doctor_visits d, mon
       where left(coalesce(d."createdAt", d."date", ''), mon.klen) = mon.key
         and coalesce(d."branch",'') = br.b
         and length(hr.perf_m10(d."createdBy")) = 10
         and hr.perf_m10(d."createdBy") not in (select k.m10 from known k)
    ) as rmp_added,

    -- কল · হাজিরা · রিপোর্ট staff-ভিত্তিক, ব্রাঞ্চের জন্য প্রযোজ্য নয়
    0 as app_calls,
    0 as outside_calls,

    (select coalesce(sum(hr.perf_num(y."amount")), 0) from public.payments y, mon
       where left(coalesce(y."date", ''), mon.klen) = mon.key
         and y."branch" = br.b
         and upper(coalesce(y."mode", 'CASH')) <> 'ONLINE'
         and lower(coalesce(y."payType",'')) <> 'refund'
         and length(hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy"))) = 10
         and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy"))
             not in (select k.m10 from known k)
    ) as cash_collected,

    (select coalesce(sum(hr.perf_num(y."amount")), 0) from public.payments y, mon
       where left(coalesce(y."date", ''), mon.klen) = mon.key
         and y."branch" = br.b
         and upper(coalesce(y."mode", 'CASH')) = 'ONLINE'
         and lower(coalesce(y."payType",'')) <> 'refund'
         and length(hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy"))) = 10
         and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy"))
             not in (select k.m10 from known k)
    ) as online_collected,

    0 as present_days,
    0 as reports_sent,
    0 as leave_days
  from br, guard
$fn$;

revoke all on function hr.branch_performance(text) from public, anon;
grant execute on function hr.branch_performance(text) to authenticated;
notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (Master হিসেবে অ্যাপ থেকে) ─────────────────────────────────
-- select * from hr.branch_performance(to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM-DD'));
