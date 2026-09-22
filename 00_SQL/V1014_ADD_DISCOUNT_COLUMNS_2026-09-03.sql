-- 🏷️ V1014 (03.09.2026) — Give Discount
-- TK-অনুমোদিত: বিল কমবে, আর ছাড়ের হিসাব চিরকাল থাকবে।
-- শুধু নতুন ঘর যোগ — পুরনো কোনো সারি ছোঁয়া হয় না, কিছু মোছে না।
alter table public.patients add column if not exists "billBeforeDiscount" numeric;
alter table public.patients add column if not exists "discountReason"     text;
alter table public.patients add column if not exists "discountBy"         text;
alter table public.patients add column if not exists "discountAt"         text;

select 'ঘর যোগ হলো' as step,
       count(*) filter (where column_name in
         ('billBeforeDiscount','discountReason','discountBy','discountAt')) as n
from information_schema.columns
where table_schema='public' and table_name='patients';
