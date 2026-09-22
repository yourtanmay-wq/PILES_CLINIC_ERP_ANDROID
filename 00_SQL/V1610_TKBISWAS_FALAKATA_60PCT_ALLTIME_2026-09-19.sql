-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র নির্দেশ (১৯.০৯.২০২৬): শুধু Falakata ব্রাঞ্চে RMP TK BISWAS-এর পাঠানো
-- সব রোগী (২০২৫ থেকে আজ পর্যন্ত), প্রত্যেকে কত জমা করেছেন, আর "60%" ধরলে
-- কমিশন কত হতো (আসল নিয়ম অনুযায়ী — নিট জমা, বিলের বেশি নয় — এটাই
-- fin.rmp_branch_due()-এর হুবহু সূত্র, নতুন কোনো হিসাব বানানো হয়নি)।

-- ── ধাপ ১: প্রতিটা রোগী আলাদা করে ──────────────────────────────────────
with referred as (
  select p."id", p."patientId", p."name", p."registrationDate",
         greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill_net
  from public.patients p
  where p."branch" = 'Falakata'
    and (
      p."refDoctorMobile" in ('8001080080','918001080080','+918001080080')
      or p."refDoctor" ilike '%TK BIS%'
    )
    and p."registrationDate" >= '2025-01-01'
),
paid_t as (
  select r."id",
    coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
       where x."patientId" = r."id" and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
    - coalesce((select sum(fin.rmp_safe_number(rf."amount")) from public.payments rf
         left join public.payments o on o.id = rf."refundOfPaymentId"
        where rf."patientId" = r."id"
          and lower(coalesce(rf."payType",'')) = 'refund'
          and lower(coalesce(rf."refundApprovalStatus",'')) = 'approved'
          and (trim(coalesce(rf."refundOfPaymentId",'')) = '' or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as net_paid
  from referred r
)
select
  r."patientId"           as "রোগীর কোড",
  r."name"                as "নাম",
  r."registrationDate"    as "রেজিস্ট্রেশন তারিখ",
  r.bill_net              as "বিল (ছাড়ের পরে)",
  pt.net_paid             as "রোগী যা জমা করেছেন",
  c."commission_mode"     as "এখনকার নিয়ম",
  c."commission_value"    as "এখনকার হার",
  round(
    case when r.bill_net > 0 then
      case when c."commission_mode" = 'PERCENT'
        then least(greatest(0, pt.net_paid), r.bill_net) * c."commission_value" / 100
        else c."commission_value" * least(greatest(0, pt.net_paid), r.bill_net) / r.bill_net end
    else 0 end, 2)          as "এখনকার নিয়মে কমিশন",
  round(least(greatest(0, pt.net_paid), r.bill_net) * 0.60, 2) as "60% ধরলে কমিশন হতো"
from referred r
join paid_t pt on pt."id" = r."id"
left join fin.rmp_patient_commissions c on c.patient_row_id = r."id"
order by r."registrationDate";

-- ── ধাপ ২: মোট (সব রোগী মিলিয়ে) ────────────────────────────────────────
with referred as (
  select p."id",
         greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill_net
  from public.patients p
  where p."branch" = 'Falakata'
    and (
      p."refDoctorMobile" in ('8001080080','918001080080','+918001080080')
      or p."refDoctor" ilike '%TK BIS%'
    )
    and p."registrationDate" >= '2025-01-01'
),
paid_t as (
  select r."id", r.bill_net,
    coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
       where x."patientId" = r."id" and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
    - coalesce((select sum(fin.rmp_safe_number(rf."amount")) from public.payments rf
         left join public.payments o on o.id = rf."refundOfPaymentId"
        where rf."patientId" = r."id"
          and lower(coalesce(rf."payType",'')) = 'refund'
          and lower(coalesce(rf."refundApprovalStatus",'')) = 'approved'
          and (trim(coalesce(rf."refundOfPaymentId",'')) = '' or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as net_paid
  from referred r
)
select
  count(*)                                              as "মোট রোগী",
  sum(bill_net)                                         as "মোট বিল",
  sum(net_paid)                                         as "মোট জমা",
  round(sum(least(greatest(0, net_paid), bill_net)), 2)   as "কমিশন-যোগ্য মোট টাকা (বিলের বেশি নয়)",
  round(sum(least(greatest(0, net_paid), bill_net)) * 0.60, 2) as "এটার 60%"
from paid_t;
