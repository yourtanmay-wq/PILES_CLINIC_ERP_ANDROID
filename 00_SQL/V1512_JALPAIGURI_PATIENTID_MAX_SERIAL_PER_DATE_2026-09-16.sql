-- শুধু SELECT, কিছু বদলায় না। আগের SQL-এ ১০০ সারির সীমায় আটকে গিয়েছিল।
-- এখানে প্রতিটা তারিখের জন্য শুধু সবচেয়ে বড় সংখ্যাটা (কতজন পর্যন্ত ব্যবহার
-- হয়ে গেছে) বের করা হচ্ছে -- তাই সারি অনেক কম হবে, ১০০-র সীমায় আটকাবে না।

select
  substring("patientId" from 5 for 8) as date_part,
  max(substring("patientId" from 14)::int) as max_serial_used,
  count(*) as total_on_date
from public.patients
where "patientId" ~ '^JPE-\d{8}-\d+$'
group by date_part
order by date_part;
