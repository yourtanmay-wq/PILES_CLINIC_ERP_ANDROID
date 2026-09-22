-- ═══════════════════════════════════════════════════════════════════════════
-- V1374 (১২.০৯.২০২৬) — TK-প্রশ্ন যাচাই (তালিকা ৪৭৭): Draft-এ Kishanganj-এ
-- "Refunded 5, Return Visit 0" দেখাচ্ছিল। কোড অনুযায়ী এটা পুরনো (V1355-এর
-- আগের) APK-র উপসর্গ হওয়ার কথা — আসল সার্ভার-ডেটা ঠিক আছে কিনা এই SQL
-- সেটাই প্রমাণ করবে (DraftRepository.kt-এর হুবহু একই নিয়মে, আন্দাজ নয়)।
--
-- নিয়ম (কোড থেকে হুবহু তোলা):
--  · Visit Fee/attendance_mark-এর টাকা "paid"-এ ধরা হয় না।
--  · Approved Refund হলে টাকা বিয়োগ হয়, বাকি Refund (pending/rejected) ধরা হয় না।
--  · মোবাইল-ধরে যোগফল (এক মোবাইলে একাধিক রোগী-সারি থাকলেও একসাথে)।
--  · যে মোবাইলের Approved Refund আছে ও নিট "paid" ₹0-এর কাছাকাছি (≤0.5),
--    আর হাতে করে Restore করা হয়নি (refundRestoredBy ফাঁকা) — সে-ই এখন
--    "Refunded" ও "Return Visit" দুটো ঘরেই দেখানোর কথা (V1355)।
-- ⛔ শুধু পড়া, কিছু বদলায় না। Supabase → SQL Editor → New query → Run।
-- ═══════════════════════════════════════════════════════════════════════════

-- (লাইভে আগে থেকেই আছে এই দুটো ঘর — নিরাপদ নিশ্চয়তার জন্য শুধু, কিছু বদলায় না)
alter table public.payments add column if not exists "refundOfPaymentId" text;
alter table public.payments add column if not exists "refundApprovalStatus" text;

with branch_patients as (
  select id, name, mobile, "refundRestoredBy",
         right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as mkey
  from public.patients
  where lower(trim(coalesce(branch,''))) = lower('Kishanganj')
),
mobiles as (
  select distinct mkey from branch_patients where mkey <> ''
),
pay_by_mobile as (
  select right(regexp_replace(coalesce(x.mobile,''),'[^0-9]','','g'),10) as mkey,
    sum(case
      when coalesce(x."payType",'') in ('visit_fee','attendance_mark') then 0
      when lower(coalesce(x."payType",''))='refund'
       and lower(coalesce(x."refundApprovalStatus",''))='approved'
        then -fin.rmp_safe_number(x.amount)
      when lower(coalesce(x."payType",''))='refund' then 0
      else fin.rmp_safe_number(x.amount)
    end) as net_paid,
    bool_or(lower(coalesce(x."payType",''))='refund'
            and lower(coalesce(x."refundApprovalStatus",''))='approved') as has_approved_refund
  from public.payments x
  join mobiles m on m.mkey = right(regexp_replace(coalesce(x.mobile,''),'[^0-9]','','g'),10)
  group by right(regexp_replace(coalesce(x.mobile,''),'[^0-9]','','g'),10)
)
select
  bp.name as patient_name, bp.mobile,
  round(coalesce(pb.net_paid,0),2) as net_paid_after_refund,
  coalesce(pb.has_approved_refund,false) as has_approved_refund,
  coalesce(nullif(trim(coalesce(bp."refundRestoredBy",'')),''),'-') as manually_restored_by,
  case
    when coalesce(pb.has_approved_refund,false)
     and coalesce(pb.net_paid,0) <= 0.5
     and trim(coalesce(bp."refundRestoredBy",'')) = ''
    then 'SHOULD SHOW in Refunded + Return Visit'
    else '-'
  end as expected_in_draft
from branch_patients bp
left join pay_by_mobile pb on pb.mkey = bp.mkey
where coalesce(pb.has_approved_refund,false) = true
order by expected_in_draft desc, patient_name;
