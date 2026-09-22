-- V1467 (১৪.০৯.২০২৬) — শুধুমাত্র পড়া। "TK BISWAS" (মোবাইল +918001080080)
-- নামে RMP-রেকর্ড ডাটাবেসে ক'টা আছে, কোন কোন ব্রাঞ্চে — এই একটাই প্রশ্ন,
-- আলাদা ফাইলে (আগেরগুলোয় একাধিক প্রশ্ন একসাথে থাকায় শুধু শেষটার ফল আসছিল)।

select id, name, mobile, branch, status, "createdAt"
from public.doctor_visits
where mobile like '%8001080080'
order by "createdAt";
