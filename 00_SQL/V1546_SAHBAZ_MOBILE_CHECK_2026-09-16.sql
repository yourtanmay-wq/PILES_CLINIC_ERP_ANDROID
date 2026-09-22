-- শুধু দেখার জন্য (SELECT) -- 9546873144 নম্বরে ডাটাবেসে কয়টা সারি আছে সেটা যাচাই
select id, "patientId", name, mobile, bill, stage, "createdBy"
from public.patients
where right(mobile, 10) = '9546873144';
