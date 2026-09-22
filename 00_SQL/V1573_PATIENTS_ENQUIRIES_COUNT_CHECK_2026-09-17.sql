-- শুধু দেখার জন্য (SELECT) -- TK-র প্রশ্নের সৎ উত্তর: patients ও enquiries
-- টেবিলে আসলে কত সারি আছে -- আন্দাজ না করে সরাসরি গুনে দেখা হচ্ছে।
-- কিচ্ছু বদলায় না, শুধু পড়া হয়।
select 'patients' as table_name, count(*) as total_rows from public.patients
union all
select 'enquiries' as table_name, count(*) as total_rows from public.enquiries;
