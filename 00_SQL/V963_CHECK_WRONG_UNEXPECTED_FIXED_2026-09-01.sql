-- 🔎 শুধু পড়া — সংশোধিত।
-- ⚠️ আগের লেখাটায় আমার ভুল ছিল: ফোন এনকোয়ারির সময় **স্থানীয় (ভারতীয়) সময়েই**
--    জমা করে, কিন্তু শেষে ভুল করে 'Z' (UTC) লিখে দেয় (`EnquiryModel.isoNow()`-এ
--    TimeZone বসানো নেই)। তাই আগের লেখায় সময়টা ৫ ঘণ্টা ৩০ মিনিট এগিয়ে দেখাচ্ছিল।
--    এখানে জমা থাকা ঘড়ির সময়টাই হুবহু ধরা হলো — কোনো রূপান্তর নয়।
with p as (
  select
    x.id, x."patientId" as patient_code, x.name, x.mobile,
    left(coalesce(nullif(x."registrationDate",''), x."date", ''),10) as reg_date,
    right(regexp_replace(coalesce(x.mobile,''),'\D','','g'),10) as mob10
  from public.patients x
  where coalesce(x."timeType",'') = 'Unexpected Time'
    and left(coalesce(nullif(x."registrationDate",''), x."date", ''),10) >= '2026-08-18'
), e as (
  select distinct on (p.id)
    p.id,
    q."receivedBy" as enquiry_by,
    case when q."createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
         then (left(q."createdAt",19))::timestamp end as enq_clock
  from p
  left join public.enquiries q
    on right(regexp_replace(coalesce(q.mobile,''),'\D','','g'),10) = p.mob10
  order by p.id, coalesce(q."createdAt", q."date", '') desc
), m as (
  select
    p.id,
    coalesce(sum(case when coalesce(s.status,'PAID') <> 'DUE' then s.amount else 0 end),0) as extra_paid,
    coalesce(sum(case when coalesce(s.status,'PAID')  = 'DUE' then s.amount else 0 end),0) as extra_due
  from p
  left join hr.salary_payments s on s.src_key like 'INC:%:' || p.id || ':%'
  group by p.id
)
select
  p.patient_code, p.name, p.mobile, p.reg_date,
  to_char(e.enq_clock, 'DD.MM.YYYY  HH12:MI AM') as enquiry_taken_at,
  case
    when e.enq_clock is null then 'সময় জানা নেই'
    when e.enq_clock::time between time '09:00' and time '18:00' then 'OFFICIAL হওয়ার কথা'
    else 'সত্যিই UNEXPECTED'
  end as verdict,
  m.extra_paid, m.extra_due
from p
left join e on e.id = p.id
left join m on m.id = p.id
order by verdict, p.reg_date desc, p.patient_code;
