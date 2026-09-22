-- V1402 (১২.০৯.২০২৬ রাত) — শুধু পড়া, কিছু বদলায় না।
-- TK: "স্টাফদের বক্তব্য এই সমস্ত নাম্বার অনেকবার নো মোর কলস করেছে, তারপরও Today pending call-এ চলে আসে"
-- ১) Jalpaiguri-র আজকের/বকেয়া কল-তালিকায় যে সারিগুলো উঠছে (অ্যাপের একই নিয়ম: চালু সারি, nextFollow আজ বা আগের, noMoreCalls নয়)
-- ২) ওই নম্বরগুলোর **সব** সারি (জোড়াসহ) — কোনটায় থামানোর চিহ্ন আছে, কোনটা কবে তৈরি/বদল হয়েছে

with pending as (
  select distinct right(regexp_replace(coalesce(mobile,''), '\D', '', 'g'), 10) as mob10
  from public.followups
  where branch = 'Jalpaiguri'
    and coalesce(status,'') not in ('Cancelled','Incomplete','Rejected','Closed')
    and coalesce("noMoreCalls", false) = false
    and coalesce("nextFollow",'') <> ''
    and "nextFollow" <= to_char(now() at time zone 'Asia/Kolkata', 'YYYY-MM-DD')
)
select
  right(regexp_replace(coalesce(f.mobile,''), '\D', '', 'g'), 10) as mob10,
  f.name, f.branch, f.stage, f.status,
  f."nextFollow", f."noMoreCalls", f."lastCallDate", f."callCount",
  left(coalesce(f."lastRemark",''), 60) as last_remark,
  f.id,
  f."createdAt", f."updatedAt",
  case
    when coalesce(f."noMoreCalls",false) then 'STOPPED ROW'
    when coalesce(f.status,'') in ('Cancelled','Incomplete','Rejected','Closed') then 'CLOSED ROW'
    when coalesce(f."nextFollow",'') <> '' and f."nextFollow" <= to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD') then 'SHOWS IN PENDING'
    else 'active, not due'
  end as why
from public.followups f
join pending p on p.mob10 = right(regexp_replace(coalesce(f.mobile,''), '\D', '', 'g'), 10)
order by mob10, f."createdAt";
