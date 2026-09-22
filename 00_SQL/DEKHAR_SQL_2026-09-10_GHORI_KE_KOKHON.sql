-- শুধু দেখা (⑧): রাত-ঘণ্টায় (IST ২৩–০৫) তৈরি সারিগুলো কে, কোন ঘণ্টায়, কী ধরনের — মানুষের হাতে না স্বয়ংক্রিয়
select 'followups' as tebil,
       extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata'))::int as ist_ghonta,
       case when id like 'fu_pat_%' then 'fu_pat_ (heal)' when id like 'fu_msk%' then 'fu_msk (web)' else 'fu_<hex> (phone)' end as dhoron,
       coalesce("createdBy",'') as ke, coalesce(stage,'') as stage_or_type,
       count(*) as koto, min("createdAt") as prothom, max("createdAt") as shesh
from followups
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
  and extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata')) not between 6 and 22
group by 2,3,4,5
union all
select 'payments',
       extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata'))::int,
       case when id like 'pay_vf_%' then 'pay_vf_' when id like 'pay_%' then 'pay_' else left(id,6) end,
       coalesce("receivedBy",''), coalesce("payType",''),
       count(*), min("createdAt"), max("createdAt")
from payments
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
  and extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata')) not between 6 and 22
  and coalesce("payType",'') not in ('chamber_expected','bill_edit','attendance_mark')
group by 2,3,4,5
order by 1, 6 desc;
