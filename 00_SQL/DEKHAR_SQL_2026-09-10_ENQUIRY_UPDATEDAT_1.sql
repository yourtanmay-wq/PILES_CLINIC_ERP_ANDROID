-- শুধু দেখা: enquiries-এর যে ১টা সারির "updatedAt" এখনো অ্যাপের ছাঁচে নয়
select id, name, mobile, "updatedAt", "createdAt", status
from enquiries
where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T' and "updatedAt" <> '';
