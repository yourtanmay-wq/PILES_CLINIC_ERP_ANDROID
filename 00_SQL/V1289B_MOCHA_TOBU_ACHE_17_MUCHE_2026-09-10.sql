-- V1289-খ (১০.০৯.২০২৬) — মোছা-তালিকায় চিহ্ন আছে অথচ টেবিলে ফিরে-আসা ১৭টা সারি (সব Cancelled) আবার মোছা
-- ⛔ patients/payments/doctor_visits/medical-এ এমন সারি ০ — টাকার কোনো হিসাব বদলায় না।
-- ⛔ শুধু সেই সারিই মোছে যার 'টেবিল|id' চিহ্ন deleted_records-এ আগে থেকেই আছে **এবং** status = 'Cancelled'।
begin;
with d as (select "tableName" as t, "rowId" as rid from deleted_records)
delete from enquiries e using d where d.t = 'enquiries' and d.rid = e.id and e.status = 'Cancelled';
with d as (select "tableName" as t, "rowId" as rid from deleted_records)
delete from followups f using d where d.t = 'followups' and d.rid = f.id and f.status = 'Cancelled';
-- যাচাই: দুটোই ০ হওয়ার কথা
select 'enquiries' as tebil, count(*) as baki from enquiries e join deleted_records d on d."tableName" = 'enquiries' and d."rowId" = e.id
union all
select 'followups', count(*) from followups f join deleted_records d on d."tableName" = 'followups' and d."rowId" = f.id;
commit;
