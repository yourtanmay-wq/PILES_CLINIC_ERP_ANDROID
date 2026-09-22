-- =====================================================================
-- V1290 (১০.০৯.২০২৬) — তালিকা ৪১১-⑧ (ক): "সার্ভার কখন পেল" ঘর (server_at)
--   ফোন/কম্পিউটার তারিখ-সময় নিজের ঘড়ি থেকে লেখে; এই ঘরটা সার্ভার নিজে বসায়
--   (default now()) — অ্যাপ কিছু পাঠায় না, কিছু জানেও না। পরে createdAt/date-এর
--   সঙ্গে মিলিয়ে ঘড়ি ভুল/বদলানো ধরা যাবে।
-- ⛔ শুধু ঘর যোগ — কোনো সারি/মান বদলায় না; পুরনো সারিতে ফাঁকা (NULL) থাকবে (সত্যিই জানা নেই)।
-- ⛔ পরে কেউ পুরো সারি আবার লিখলে (upsert) আগের মানই থাকে — "প্রথম কখন পেল" সেটাই থাকে।
-- =====================================================================
begin;
alter table public.payments      add column if not exists server_at timestamptz default now();
alter table public.patients      add column if not exists server_at timestamptz default now();
alter table public.followups     add column if not exists server_at timestamptz default now();
alter table public.enquiries     add column if not exists server_at timestamptz default now();
alter table public.doctor_visits add column if not exists server_at timestamptz default now();
alter table public.medical       add column if not exists server_at timestamptz default now();
-- যাচাই: ৬টা টেবিলে ঘরটা আছে
select table_name, column_name, data_type, column_default
from information_schema.columns
where table_schema = 'public' and column_name = 'server_at'
order by table_name;
commit;
