-- শুধু দেখা: Laxmi-র এনকোয়ারি সারিটা কেন V1289-এ বদলায়নি — মোছা-তালিকায় আছে কিনা, আর enquiries-এ কোন কোন পাহারা (trigger) বসানো
select 'deleted_records' as ki, id as man, "tableName" as tebil, "deletedAt"::text as kokhon
from deleted_records
where "rowId" = 'enq_cdf9dca57e97425c93fd1f7410295c3b' or id like '%enq_cdf9dca57e97425c93fd1f7410295c3b%'
union all
select 'trigger', tgname, tgrelid::regclass::text, case when tgenabled = 'D' then 'off' else 'on' end
from pg_trigger
where tgrelid = 'public.enquiries'::regclass and not tgisinternal
union all
select 'function', proname, '', prosrc from pg_proc where proname = 'tk_block_deleted_record_return';
