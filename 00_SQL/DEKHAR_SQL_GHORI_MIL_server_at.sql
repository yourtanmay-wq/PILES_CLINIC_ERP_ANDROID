-- শুধু দেখা (⑧, V1290-এর পরে যেকোনো দিন): ফোনের লেখা সময় (createdAt) আর সার্ভারের পাওয়ার সময় (server_at) কতটা আলাদা
-- স্বাভাবিক তফাত ≈ +5:30 ঘণ্টা (ফোনের সময়-লেখার স্থির নিয়ম); তার থেকে ১ ঘণ্টার বেশি এদিক-ওদিক = ঘড়ি সন্দেহ
select 'payments' as tebil, coalesce("receivedBy",'') as ke, count(*) as sari,
       round(avg(extract(epoch from (("createdAt")::timestamptz - server_at)))/3600.0, 2) as gor_tofat_ghonta,
       count(*) filter (where abs(extract(epoch from (("createdAt")::timestamptz - server_at))/3600.0 - 5.5) > 1) as sondeho
from payments
where server_at is not null and "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
group by 2
union all
select 'patients', coalesce("registeredBy",''), count(*),
       round(avg(extract(epoch from (("createdAt")::timestamptz - server_at)))/3600.0, 2),
       count(*) filter (where abs(extract(epoch from (("createdAt")::timestamptz - server_at))/3600.0 - 5.5) > 1)
from patients
where server_at is not null and "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
group by 2
order by 5 desc, 1, 3 desc;
