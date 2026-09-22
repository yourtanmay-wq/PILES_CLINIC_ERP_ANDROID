-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- TK-র প্রশ্ন: Cooch Behar-এর "Visit" ট্যাবে (followups টেবিলের stage='Patient')
-- এই বছরের (চলতি বছর) জন্য 300 দেখাচ্ছে, যা TK-র মতে ভুল। নিচের ৪টা প্রশ্ন দিয়ে
-- আসল ডেটা দেখা হচ্ছে -- অ্যাপের কোড কী করে সেটা অনুমান না করে, ডেটাবেসেই যাচাই।

-- ১) সব ব্রাঞ্চ মিলিয়ে stage='Patient'-এর সারি কতগুলো, ব্রাঞ্চ ধরে ধরে
--    (Cooch Behar আসলে কত, আর নামের বানান/স্পেস ঠিক আছে কিনা তা দেখার জন্য)
select branch, count(*) as total_rows
from public.followups
where stage = 'Patient'
  and status not in ('Cancelled','Incomplete','Rejected','Closed')
group by branch
order by total_rows desc;

-- ২) Cooch Behar-এর মধ্যে, বছর ধরে ধরে কতগুলো সারি ("registrationDate" না থাকলে date, সেটাও না থাকলে "visitDate" ধরে বছর বার করা হচ্ছে)
select
  coalesce(
    nullif(left("registrationDate", 4), ''),
    nullif(left("date", 4), ''),
    nullif(left("visitDate", 4), ''),
    'UNKNOWN'
  ) as year_used,
  count(*) as total_rows
from public.followups
where stage = 'Patient'
  and status not in ('Cancelled','Incomplete','Rejected','Closed')
  and branch = 'Cooch Behar'
group by 1
order by 1 desc;

-- ৩) Cooch Behar + চলতি বছর (2026) -- মোট সারি কতগুলো, আর distinct মোবাইল নম্বর কতগুলো
--    (সারি বেশি কিন্তু distinct মোবাইল কম হলে বুঝা যাবে একই নম্বরের একাধিক সারি জমেছে)
select
  count(*) as total_rows_2026,
  count(distinct mobile) as distinct_mobiles_2026
from public.followups
where stage = 'Patient'
  and status not in ('Cancelled','Incomplete','Rejected','Closed')
  and branch = 'Cooch Behar'
  and coalesce(
        nullif(left("registrationDate", 4), ''),
        nullif(left("date", 4), ''),
        nullif(left("visitDate", 4), '')
      ) = '2026';

-- ৪) এই ২০২৬-এর Cooch Behar Visit-সারিগুলোর মধ্যে কতগুলোর মোবাইল নম্বর ইতিমধ্যে
--    Treatment-ধাপেও চলে গেছে (টাকা জমা পড়েছে) -- এগুলো অ্যাপে Visit ট্যাব থেকে
--    বাদ পড়ে যাওয়ার কথা, তাই এই সংখ্যাটা টেবিলের কাঁচা গণনা আর অ্যাপের দেখানো
--    সংখ্যার মধ্যে ফারাক ব্যাখ্যা করতে পারে
select count(*) as already_advanced_to_treatment
from public.followups f
where f.stage = 'Patient'
  and f.status not in ('Cancelled','Incomplete','Rejected','Closed')
  and f.branch = 'Cooch Behar'
  and coalesce(
        nullif(left(f."registrationDate", 4), ''),
        nullif(left(f."date", 4), ''),
        nullif(left(f."visitDate", 4), '')
      ) = '2026'
  and exists (
    select 1 from public.followups f2
    where f2.mobile = f.mobile
      and f2.stage = 'Treatment'
      and f2.status not in ('Cancelled','Incomplete','Rejected','Closed')
  );
