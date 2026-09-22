-- শুধু দেখার জন্য (SELECT) -- এই ৫টা মোবাইলে আগে থেকেই কোনো রোগী আছে কিনা
select id, "patientId", name, mobile, bill, stage
from public.patients
where right(mobile, 10) in ('9957468191','7584937152','7478155578','7319394685','9365310365');
