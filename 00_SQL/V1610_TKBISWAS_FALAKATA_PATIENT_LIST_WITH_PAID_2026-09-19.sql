-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র নির্দেশ (১৯.০৯.২০২৬): Falakata-য় RMP TK BISWAS-এর পাঠানো ১৯ জন
-- রোগীর individual তালিকা — নাম, প্রথমবার আসার তারিখ, আর প্রত্যেকে
-- সর্বমোট কত জমা করেছেন (refund বাদ দিয়ে, আগের হিসাবের সাথে মেলানো)।

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
)
select
  r."name"                as "রোগীর নাম",
  r."patientId"           as "রোগীর কোড",
  r."registrationDate"    as "প্রথমবার আসার তারিখ",
  r.bill_net              as "বিল",
  coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
     where x."patientId" = r."id" and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
  - coalesce((select sum(fin.rmp_safe_number(rf."amount")) from public.payments rf
       left join public.payments o on o.id = rf."refundOfPaymentId"
      where rf."patientId" = r."id"
        and lower(coalesce(rf."payType",'')) = 'refund'
        and lower(coalesce(rf."refundApprovalStatus",'')) = 'approved'
        and (trim(coalesce(rf."refundOfPaymentId",'')) = '' or fin.rmp_is_treatment(o."payType", o."remarks"))), 0)
                          as "সর্বমোট জমা করেছেন"
from referred r
order by r."registrationDate";
