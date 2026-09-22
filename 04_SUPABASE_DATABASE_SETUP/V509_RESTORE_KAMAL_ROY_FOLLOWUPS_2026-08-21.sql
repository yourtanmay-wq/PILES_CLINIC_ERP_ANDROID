-- ════════════════════════════════════════════════════════════════════════
-- 🔁 KAMAL ROY — ফলো-আপের দুটো সারি ফেরানো
--    DATE: 21/08/2026   ·   fu_mskbykrr_bqgyt  ও  fu_mskc7kvo_wfdm1
--
-- ⛔ কোনো সারি মোছা হয় না। কোনো টাকা ছোঁয়া হয় না। রোগীর কোনো তথ্য বদলায় না।
--    শুধু দুটো জিনিস — (১) status: Cancelled → Active,
--                      (২) "আর ফিরিয়ে এনো না" চিহ্ন তোলা।
--    অ্যাপের নিজের Restore বোতাম ঠিক এই দুটো কাজই করে (DeletedGuard.unmark)।
-- ════════════════════════════════════════════════════════════════════════

-- ধাপ ১ · বদলানোর আগে হুবহু নকল রেখে দেওয়া (কিছু ভুল হলে এখান থেকে ফেরানো যাবে)
create table if not exists public.followups_backup_v509_20260821 as
  select * from public.followups
   where "id" in ('fu_mskbykrr_bqgyt', 'fu_mskc7kvo_wfdm1');

-- ধাপ ২ · status ফেরানো — ⛔ শুধু তখনই, যদি এখনো 'Cancelled' থাকে
update public.followups
   set "status"    = 'Active',
       "updatedAt" = to_char(now() at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
 where "id" in ('fu_mskbykrr_bqgyt', 'fu_mskc7kvo_wfdm1')
   and coalesce("status", '') = 'Cancelled';

-- ধাপ ৩ · "আর ফিরিয়ে এনো না" চিহ্ন তোলা
--   ⛔ এটা রোগীর তথ্য নয় — শুধু "এই সারিটা মুছে ফেলা হয়েছে" এই চিহ্নটুকু।
--      অ্যাপের Restore বোতামও ঠিক এটাই করে (removeDeletedFromCloud)।
delete from public.deleted_records
 where "tableName" = 'followups'
   and "rowId" in ('fu_mskbykrr_bqgyt', 'fu_mskc7kvo_wfdm1');

-- ধাপ ৪ · এখন অবস্থা কী — এটাই পর্দায় দেখতে পাবেন
select
  f."id"                                        as "আইডি",
  f."name"                                      as "নাম",
  f."stage"                                     as "ধাপ",
  f."status"                                    as "অবস্থা",
  f."branch"                                    as "ব্রাঞ্চ",
  case when d."rowId" is null then '✅ চিহ্ন উঠে গেছে'
       else '❌ চিহ্ন এখনো আছে' end              as "লুকানোর চিহ্ন",
  case when coalesce(f."status",'') in ('Cancelled','Incomplete','Rejected','Closed')
       then '❌ এখনো তালিকায় আসবে না'
       else '✅ তালিকায় দেখা যাবে' end          as "ফল"
from public.followups f
left join public.deleted_records d
       on d."rowId" = f."id" and d."tableName" = 'followups'
where f."id" in ('fu_mskbykrr_bqgyt', 'fu_mskc7kvo_wfdm1')
order by f."id";
