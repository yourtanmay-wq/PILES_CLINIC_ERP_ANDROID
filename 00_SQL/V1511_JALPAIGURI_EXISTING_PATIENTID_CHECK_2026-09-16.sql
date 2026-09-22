-- শুধু SELECT, কিছু বদলায় না। জলপাইগুড়ি ব্রাঞ্চের লাইভ patients টেবিলে
-- "JPE-" দিয়ে শুরু হওয়া কোন কোন patientId ইতিমধ্যে আছে তার সম্পূর্ণ তালিকা।
-- এটা দেখে ইতিহাস-আমদানির নতুন patientId সংখ্যা এড়িয়ে বানানো হবে।

select "patientId", name, mobile, date, "createdBy"
from public.patients
where "patientId" like 'JPE-%'
order by "patientId";
