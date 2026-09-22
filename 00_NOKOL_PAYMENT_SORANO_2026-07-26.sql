-- ================================================================
--  দুবার-চাপার কারণে তৈরি হওয়া নকল পেমেন্ট সরানো  ·  ২৬.০৭.২০২৬
--
--  নিয়ম: একই রোগী + একই দিন + একই টাকা + একই ধরন, আর দুটো সারি
--  তৈরি হয়েছে ১০ মিনিটের মধ্যে → ওটা দুবার-চাপার ফল।
--  (আসল দ্বিতীয় পেমেন্ট কেউ ১০ মিনিটের মধ্যে নেয় না।)
--
--  প্রতিটা দলের সবচেয়ে পুরনো সারিটা থাকবে, বাকিগুলো সরবে।
--  ⚠️ কিছুই চিরতরে মুছবে না — সরানো সারি Trash-এ জমা থাকবে,
--     দরকারে ফেরত আনা যাবে।
-- ================================================================


-- ============ ধাপ ১ — শুধু দেখা (কিছুই বদলায় না) ============
-- কোন কোন সারি সরবে, আগে চোখে দেখে নিন

with dup as (
  select p.*,
         row_number() over (
           partition by right(regexp_replace(p.mobile,'[^0-9]','','g'),10),
                        p.date, p.amount, p."payType"
           order by p."createdAt", p.id) as rn,
         min(p."createdAt") over (
           partition by right(regexp_replace(p.mobile,'[^0-9]','','g'),10),
                        p.date, p.amount, p."payType") as first_at
  from public.payments p
  where p.amount ~ '^[0-9]+(\.[0-9]+)?$'
    and p.amount::numeric > 0
    and coalesce(p."payType",'') not in ('attendance_mark','bill_edit','chamber_expected')
)
select name        as "নাম",
       mobile      as "মোবাইল",
       date        as "তারিখ",
       amount      as "টাকা",
       "payLabel"  as "যে নামে লেখা",
       "createdAt" as "কখন লেখা হলো",
       'সরবে'      as "কী হবে"
from dup
where rn > 1
  and ("createdAt"::timestamp - first_at::timestamp) < interval '10 minutes'
order by mobile, date, "createdAt";


-- ============ ধাপ ২ — Trash-এ জমা করা ============
-- ধাপ ১-এর তালিকা ঠিক মনে হলে তবেই চালাবেন

with dup as (
  select p.*,
         row_number() over (
           partition by right(regexp_replace(p.mobile,'[^0-9]','','g'),10),
                        p.date, p.amount, p."payType"
           order by p."createdAt", p.id) as rn,
         min(p."createdAt") over (
           partition by right(regexp_replace(p.mobile,'[^0-9]','','g'),10),
                        p.date, p.amount, p."payType") as first_at
  from public.payments p
  where p.amount ~ '^[0-9]+(\.[0-9]+)?$'
    and p.amount::numeric > 0
    and coalesce(p."payType",'') not in ('attendance_mark','bill_edit','chamber_expected')
)
insert into public.trash (id, "table", record, "deletedAt", "deletedBy")
select 'trash_dup_' || d.id,
       'payments',
       to_jsonb(pp.*),
       to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
       'DUPLICATE-CLEANUP-2026-07-26'
from dup d
join public.payments pp on pp.id = d.id
where d.rn > 1
  and (d."createdAt"::timestamp - d.first_at::timestamp) < interval '10 minutes'
on conflict (id) do nothing;


-- ============ ধাপ ৩ — পেমেন্ট তালিকা থেকে সরানো ============
-- ধাপ ২ সফল হলে তবেই চালাবেন

delete from public.payments p
where exists (
  select 1 from public.trash t
  where t.id = 'trash_dup_' || p.id
    and t."deletedBy" = 'DUPLICATE-CLEANUP-2026-07-26');


-- ============ ধাপ ৪ — মিলিয়ে দেখা (শুধু দেখা) ============
-- এখন আর নকল আছে কিনা

select name, mobile, date, amount, count(*) as koybar
from public.payments
where amount ~ '^[0-9]+(\.[0-9]+)?$'
  and amount::numeric > 0
  and coalesce("payType",'') not in ('attendance_mark','bill_edit','chamber_expected')
group by name, mobile, date, amount
having count(*) > 1
order by date desc
limit 30;


-- ================================================================
--  ভুল হলে ফেরত আনার উপায় (দরকার হলে তবেই)
-- ================================================================
-- insert into public.payments
-- select (record #>> '{}')::jsonb ... ;
-- ↑ নিজে থেকে চালাবেন না। দরকার হলে আগে জানাবেন,
--    সরানো সব সারি Trash-এ অক্ষত আছে।
