-- ═══════════════════════════════════════════════════════════════════════
-- V1005খ — বাকি ৭৫টা ডুপ্লিকেট: আগে ইতিহাস মিলিয়ে নেওয়া, তারপর মোছা
-- ০৩.০৯.২০২৬ · TK-এর নির্দেশে · পুরোটা এক লেনদেনে
--
-- মাপা সত্য (TK-এর চালানো কোয়েরি): এই ৭৫টার **দুটো সারিতেই একই অবস্থা** —
--   Inquiry: Cancelled + Cancelled (৬৯) · Patient: Closed + Closed (৬)
-- অর্থাৎ কোনো সিদ্ধান্ত উল্টে যাওয়ার ঝুঁকি নেই; শুধু একই মানুষ তালিকায়
-- দুবার দেখাচ্ছেন।
--
-- ⛔ ধাপ ১: বাড়তি সারির যে লেখাগুলো রাখা-সারিতে নেই, সেগুলো **রাখা-সারিতে
--    তুলে দেওয়া হয়** ⇒ একটা অক্ষরও হারায় না।
-- ⛔ ধাপ ২: তারপরই বাড়তি সারিটা মোছা হয় — এবং শুধু তখনই, যখন দুই সারির
--    অবস্থা হুবহু এক।
-- ═══════════════════════════════════════════════════════════════════════

begin;

create temporary table _rk_report2(step text, n bigint) on commit drop;

insert into _rk_report2
select 'আগে: বাকি ডুপ্লিকেট দল', count(*) from (
  select f."refId", f.stage from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
  group by f."refId", f.stage having count(*) > 1
) d;

-- ── ধাপ ১: বাড়তি সারির হারানো লেখা রাখা-সারিতে তুলে দেওয়া ───────────────
with ranked as (
  select f.id, f."refId", f.stage, f.status, f.history, f."createdAt",
         case when jsonb_typeof(f.history::jsonb) = 'array'
              then jsonb_array_length(f.history::jsonb) else 0 end as hlen,
         row_number() over (
           partition by f."refId", f.stage
           order by case when jsonb_typeof(f.history::jsonb) = 'array'
                         then jsonb_array_length(f.history::jsonb) else 0 end desc,
                    coalesce(f."createdAt",'') asc, f.id asc) as rn
  from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
),
grp as (select "refId", stage from ranked group by "refId", stage having count(*) > 1),
keeper as (select r.* from ranked r join grp g on g."refId" = r."refId" and g.stage = r.stage where r.rn = 1),
extra  as (select r.* from ranked r join grp g on g."refId" = r."refId" and g.stage = r.stage where r.rn > 1),
-- শুধু সেই দলগুলো, যেখানে প্রতিটা বাড়তি সারির অবস্থা রাখা-সারির হুবহু এক
ok_grp as (
  select k."refId", k.stage
  from keeper k join extra e on e."refId" = k."refId" and e.stage = k.stage
  group by k."refId", k.stage
  having bool_and(coalesce(e.status,'') = coalesce(k.status,''))
),
extra_items as (
  select k.id as keep_id, h.e as item
  from ok_grp g
  join keeper k on k."refId" = g."refId" and k.stage = g.stage
  join extra  x on x."refId" = g."refId" and x.stage = g.stage
  cross join lateral jsonb_array_elements(x.history::jsonb) h(e)
  where jsonb_typeof(x.history::jsonb) = 'array'
),
missing as (
  select ei.keep_id, ei.item
  from extra_items ei
  join keeper k on k.id = ei.keep_id
  where not exists (
    select 1 from jsonb_array_elements(coalesce(k.history::jsonb, '[]'::jsonb)) ke
    where coalesce(ke->>'date','')   = coalesce(ei.item->>'date','')
      and coalesce(ke->>'time','')   = coalesce(ei.item->>'time','')
      and coalesce(ke->>'staff','')  = coalesce(ei.item->>'staff','')
      and coalesce(ke->>'remark','') = coalesce(ei.item->>'remark','')
  )
),
agg as (select keep_id, jsonb_agg(distinct item) as add_items from missing group by keep_id),
merged as (
  update public.followups f
     set history = coalesce(f.history::jsonb, '[]'::jsonb) || a.add_items,
         "updatedAt" = to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    from agg a
   where f.id = a.keep_id
  returning f.id
)
insert into _rk_report2 select 'ধাপ ১: রাখা-সারিতে লেখা তুলে দেওয়া হলো', count(*) from merged;

-- ── ধাপ ২: এবার বাড়তি সারিটা মোছা (কিছুই আর হারানোর নেই) ────────────────
with ranked as (
  select f.id, f."refId", f.stage, f.status, f.history, f."createdAt",
         case when jsonb_typeof(f.history::jsonb) = 'array'
              then jsonb_array_length(f.history::jsonb) else 0 end as hlen,
         row_number() over (
           partition by f."refId", f.stage
           order by case when jsonb_typeof(f.history::jsonb) = 'array'
                         then jsonb_array_length(f.history::jsonb) else 0 end desc,
                    coalesce(f."createdAt",'') asc, f.id asc) as rn
  from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
),
grp as (select "refId", stage from ranked group by "refId", stage having count(*) > 1),
keeper as (select r.* from ranked r join grp g on g."refId" = r."refId" and g.stage = r.stage where r.rn = 1),
extra  as (select r.* from ranked r join grp g on g."refId" = r."refId" and g.stage = r.stage where r.rn > 1),
ok_grp as (
  select k."refId", k.stage
  from keeper k join extra e on e."refId" = k."refId" and e.stage = k.stage
  group by k."refId", k.stage
  having bool_and(coalesce(e.status,'') = coalesce(k.status,''))
),
safe_extra as (
  select x.id
  from ok_grp g
  join keeper k on k."refId" = g."refId" and k.stage = g.stage
  join extra  x on x."refId" = g."refId" and x.stage = g.stage
  where not exists (
    select 1 from jsonb_array_elements(coalesce(x.history::jsonb,'[]'::jsonb)) xe
    where not exists (
      select 1 from jsonb_array_elements(coalesce(k.history::jsonb,'[]'::jsonb)) ke
      where coalesce(ke->>'date','')   = coalesce(xe->>'date','')
        and coalesce(ke->>'time','')   = coalesce(xe->>'time','')
        and coalesce(ke->>'staff','')  = coalesce(xe->>'staff','')
        and coalesce(ke->>'remark','') = coalesce(xe->>'remark','')
    )
  )
),
removed as (
  delete from public.followups f using safe_extra s where f.id = s.id returning f.id
)
insert into _rk_report2 select 'ধাপ ২: বাড়তি সারি মোছা হলো', count(*) from removed;

insert into _rk_report2
select 'এখনো বাকি ডুপ্লিকেট দল', count(*) from (
  select f."refId", f.stage from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
  group by f."refId", f.stage having count(*) > 1
) d;

select * from _rk_report2;

commit;
