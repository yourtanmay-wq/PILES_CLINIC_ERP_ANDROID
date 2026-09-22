-- শুধু দেখা (তালিকা ৪১৯): জলপাইগুড়ির আজকের+বকেয়া কল — ড্যাশবোর্ড ব্যানারের নিয়মে (nextFollow ≤ আজ, চালু সারি, "আর কল নয়" নয়)
with fu as (
  select id, name, mobile, stage, status, branch, "nextFollow", coalesce("noMoreCalls"::text,'') as nomore, "updatedAt"
  from public.followups
  where branch = 'Jalpaiguri'
    and stage in ('Inquiry','Patient','Treatment')
    and coalesce(status,'') not in ('Cancelled','Incomplete','Rejected','Closed')
    and coalesce("nextFollow",'') <> ''
    and left("nextFollow",10) <= to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD')
    and lower(coalesce("noMoreCalls"::text,'')) not in ('true','1')
)
select 'মোট' as ki, count(*)::text as man, '' as stage, '' as name, '' as "nextFollow", '' as "updatedAt" from fu
union all
select 'ধাপ-ধরে', count(*)::text, stage, '', '', '' from fu group by stage
union all
select 'সারি', '', stage, name, "nextFollow", "updatedAt" from fu
order by 1, 3, 5;
