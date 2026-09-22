-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- আগের ৪টা প্রশ্নের ফল: Cooch Behar-এ stage='Patient' সব মিলিয়ে ৪১৪টা সারি,
-- তার মধ্যে ২০২৬ = ১৭১টা (followups টেবিলের নিজের registrationDate/date/visitDate
-- ধরে বছর বার করলে) -- কিন্তু অ্যাপ দেখাচ্ছে ৩০০। ফারাকটা এখানেই হতে পারে:
-- অ্যাপ বছর বার করার সময় followups-এর নিজের তারিখ নয়, বরং একই মোবাইল নম্বরের
-- patients টেবিলের registrationDate/date আগে ধরে (মিল পেলে), সেটা না পেলে
-- তবেই followups-এর নিজের তারিখ। এই SQL সেই একই নিয়মে বছর বার করছে -- অ্যাপের
-- আসল সংখ্যাটা এখানে মেলে কিনা দেখার জন্য।

with fu as (
  select
    f.mobile,
    regexp_replace(f.mobile, '[^0-9]', '', 'g') as mobile_digits,
    coalesce(
      nullif(left(f."registrationDate", 4), ''),
      nullif(left(f."date", 4), ''),
      nullif(left(f."visitDate", 4), '')
    ) as fu_own_year
  from public.followups f
  where f.stage = 'Patient'
    and f.status not in ('Cancelled','Incomplete','Rejected','Closed')
    and f.branch = 'Cooch Behar'
),
pat_year as (
  select
    regexp_replace(p.mobile, '[^0-9]', '', 'g') as mobile_digits,
    min(coalesce(
      nullif(left(p."registrationDate", 4), ''),
      nullif(left(p."date", 4), '')
    )) as patient_reg_year
  from public.patients p
  group by 1
)
select
  coalesce(py.patient_reg_year, fu.fu_own_year) as app_style_year,
  count(*) as total_rows
from fu
left join pat_year py on py.mobile_digits = fu.mobile_digits
group by 1
order by 1 desc nulls last;
