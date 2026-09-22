-- শুধু দেখার জন্য (SELECT) -- কোচবিহারের বাকি থাকা ৫ জন ঢোকানোর আগে যাচাই
-- ১. এই ৫টা মোবাইলে আগে থেকেই কোনো রোগী আছে কিনা (+91 সহ/ছাড়া দুই রকমই ধরে)
select id, "patientId", name, mobile, bill, stage
from public.patients
where right(mobile, 10) in ('9957468191','7584937152','7478155578','7319394685','9365310365');

-- ২. এই ৫টা রেজিস্ট্রেশন-তারিখে কোচবিহারে কোন কোন সিরিয়াল আগে থেকেই আছে
select "patientId" from public.patients
where "patientId" like 'COB-28022025-%'
   or "patientId" like 'COB-23082025-%'
   or "patientId" like 'COB-29092025-%'
   or "patientId" like 'COB-29082025-%'
   or "patientId" like 'COB-02082025-%'
order by "patientId";
