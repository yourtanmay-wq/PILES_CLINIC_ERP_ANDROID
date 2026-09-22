-- ============================================================
--  হারিয়ে যাওয়া রোগীদের ফিরিয়ে আনা  ·  ২৬.০৭.২০২৬
--  দুই ধাপ: আগে ধাপ ১ (শুধু দেখা), তারপর ধাপ ২ (ফেরানো)
-- ============================================================


-- ============================================================
-- ধাপ ১ — শুধু দেখা (কিছুই বদলায় না)
-- কারা ফিরবে, কী নাম, কী ID পাবে — আগে চোখে দেখে নিন
-- ============================================================

with missing as (
  select p."patientId" as row_id, p.name, p.mobile, p.branch, p.date,
         coalesce(nullif(p."createdBy", ''), p."receivedBy") as staff,
         p."createdAt" as created_at
  from public.payments p
  where p."payType" = 'visit_fee'
    and p.date >= '2026-07-21'
    and coalesce(p."patientId", '') <> ''
    and right(regexp_replace(p.mobile, '[^0-9]', '', 'g'), 10) <> '7777777777'
    and not exists (
      select 1 from public.patients t
      where right(regexp_replace(t.mobile, '[^0-9]', '', 'g'), 10)
          = right(regexp_replace(p.mobile, '[^0-9]', '', 'g'), 10))
    and not exists (
      select 1 from public.patients t2 where t2.id = p."patientId")
),
coded as (
  select m.*,
         case lower(trim(m.branch))
           when 'kishanganj'  then 'KNE'
           when 'jalpaiguri'  then 'JPE'
           when 'cooch behar' then 'COB'
           when 'coochbehar'  then 'COB'
           when 'falakata'    then 'FLK'
           when 'birpara'     then 'BIR'
           else upper(left(regexp_replace(m.branch, '[^A-Za-z]', '', 'g'), 3))
         end || '-' || to_char(m.date::date, 'DDMMYYYY') || '-' as prefix
  from missing m
),
numbered as (
  select c.*,
         (select coalesce(max((regexp_replace(t."patientId", '^.*-', ''))::int), 0)
          from public.patients t
          where t."patientId" ~ ('^' || c.prefix || '[0-9]+$')) as base,
         row_number() over (partition by c.prefix
                            order by c.created_at, c.row_id) as rn
  from coded c
)
select name        as "নাম",
       mobile      as "মোবাইল",
       branch      as "ব্রাঞ্চ",
       date        as "তারিখ",
       prefix || lpad((base + rn)::text, 3, '0') as "যে ID পাবে"
from numbered
order by branch, date;


-- ============================================================
-- ধাপ ২ — ফেরানো
-- ধাপ ১-এর তালিকা ঠিক মনে হলে তবেই এটা চালাবেন
--
-- এটা শুধু নতুন রোগীর সারি যোগ করে।
-- কোনো পুরনো রোগী, পেমেন্ট বা তথ্য মোছে না, বদলায় না।
-- ============================================================

with missing as (
  select p."patientId" as row_id, p.name, p.mobile, p.branch, p.date,
         coalesce(nullif(p."createdBy", ''), p."receivedBy") as staff,
         p."createdAt" as created_at
  from public.payments p
  where p."payType" = 'visit_fee'
    and p.date >= '2026-07-21'
    and coalesce(p."patientId", '') <> ''
    and right(regexp_replace(p.mobile, '[^0-9]', '', 'g'), 10) <> '7777777777'
    and not exists (
      select 1 from public.patients t
      where right(regexp_replace(t.mobile, '[^0-9]', '', 'g'), 10)
          = right(regexp_replace(p.mobile, '[^0-9]', '', 'g'), 10))
    and not exists (
      select 1 from public.patients t2 where t2.id = p."patientId")
),
coded as (
  select m.*,
         case lower(trim(m.branch))
           when 'kishanganj'  then 'KNE'
           when 'jalpaiguri'  then 'JPE'
           when 'cooch behar' then 'COB'
           when 'coochbehar'  then 'COB'
           when 'falakata'    then 'FLK'
           when 'birpara'     then 'BIR'
           else upper(left(regexp_replace(m.branch, '[^A-Za-z]', '', 'g'), 3))
         end || '-' || to_char(m.date::date, 'DDMMYYYY') || '-' as prefix
  from missing m
),
numbered as (
  select c.*,
         (select coalesce(max((regexp_replace(t."patientId", '^.*-', ''))::int), 0)
          from public.patients t
          where t."patientId" ~ ('^' || c.prefix || '[0-9]+$')) as base,
         row_number() over (partition by c.prefix
                            order by c.created_at, c.row_id) as rn
  from coded c
)
insert into public.patients
  (id, "patientId", date, "registrationDate", "visitDate",
   name, mobile, branch, disease, stage, queue, "doctorComplete", bill,
   "createdBy", "registeredBy", "createdAt", "updatedAt")
select row_id,
       prefix || lpad((base + rn)::text, 3, '0'),
       date, date, date,
       name, mobile, branch,
       'Piles', 'Doctor Queue', 'true', 'false', '0',
       staff, staff,
       created_at, created_at
from numbered
on conflict (id) do nothing;


-- ============================================================
-- ধাপ ৩ — মিলিয়ে দেখা (শুধু দেখা)
-- এখন আর কেউ বাকি আছে কিনা
-- ============================================================

select p.branch, count(*) as baki
from public.payments p
where p."payType" = 'visit_fee'
  and p.date >= '2026-07-21'
  and not exists (
    select 1 from public.patients t
    where right(regexp_replace(t.mobile, '[^0-9]', '', 'g'), 10)
        = right(regexp_replace(p.mobile, '[^0-9]', '', 'g'), 10))
group by p.branch
order by 2 desc;
