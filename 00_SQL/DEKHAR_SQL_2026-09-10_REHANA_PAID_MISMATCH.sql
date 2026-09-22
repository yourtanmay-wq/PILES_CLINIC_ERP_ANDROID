-- DEKHAR SQL (শুধু দেখা, কিছু বদলায় না) — তালিকা 417
-- ক) REHANA BAGUM (8653956960)-এর payments / patients / followups সারি
-- খ) পুরো DB: যে টাকার সারির branch রোগীর branch-এর সঙ্গে মেলে না
--    (Follow-up কার্ড শুধু নিজের ব্রাঞ্চ + ফাঁকা-ব্রাঞ্চের টাকা টানে ⇒ এগুলো কার্ডে বাদ পড়ে,
--     কিন্তু ভিতরের Timeline মোবাইল ধরে সব টাকা দেখায়)
with pay as (
  select id, "patientId", "patientCode", regexp_replace(coalesce(mobile,''),'\D','','g') as mob,
         branch, amount, "payType", date, "createdBy", "createdAt", "updatedAt", name
  from public.payments
), pat as (
  select id, "patientId" as code, regexp_replace(coalesce(mobile,''),'\D','','g') as mob,
         branch, bill, stage, name
  from public.patients
)
select 'ক-payments' as part, id, "patientId", "patientCode", mob as mobile, branch, amount::text, "payType", date, "createdBy", "createdAt", "updatedAt"
from pay where mob like '%8653956960'
union all
select 'ক-patients', id, code, null, mob, branch, bill::text, stage, null, null, null, null
from pat where mob like '%8653956960'
union all
select 'ক-followups', id, "refId", "patientId", regexp_replace(coalesce(mobile,''),'\D','','g'), branch, null, stage, status, "createdBy", "createdAt", "updatedAt"
from public.followups where regexp_replace(coalesce(mobile,''),'\D','','g') like '%8653956960'
union all
select 'খ-branch-mismatch', y.id, y."patientId", coalesce(p.code, y."patientCode"), y.mob, 'pay=' || coalesce(y.branch,'NULL') || ' / patient=' || coalesce(p.branch,'NULL'),
       y.amount::text, y."payType", y.date, y."createdBy", y."createdAt", coalesce(p.name, y.name)
from pay y
join pat p on (p.mob = y.mob and length(y.mob) >= 10) or p.id = y."patientId" or (p.code <> '' and p.code = y."patientId")
where y.branch is not null and y.branch <> ''
  and y.branch is distinct from p.branch
  and lower(coalesce(y."payType",'')) not in ('visit_fee','visitfee','registration','attendance_mark','chamber_expected','bill_edit','refund')
order by 1, 12 desc nulls last;
