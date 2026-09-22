-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- এখন পর্যন্ত যাচাই হওয়া তথ্য: Cooch Behar, stage='Patient', সচল status —
-- সব বছর মিলিয়ে ৪১৪টা, শুধু ২০২৬-এর ১৭১টা, বানান/হরফেও কোনো গরমিল নেই।
-- অথচ অ্যাপ রিফ্রেশ করেও ৩০০-ই দেখাচ্ছে। সন্দেহ: বছর-ছাঁকনিটা আসলে এই মুহূর্তে
-- Visit ট্যাবে কাজই করছে না (কোনো কারণে), আর অ্যাপ আসলে **সব বছরের** ৪১৪টা
-- থেকে যাদের Treatment-ধাপে টাকা জমা পড়ে গেছে তাদের বাদ দিয়ে যা থাকে সেটাই
-- দেখাচ্ছে। নিচের প্রশ্নটা দিয়ে সেটা মিলিয়ে দেখা হচ্ছে।

select
  count(*) as total_all_years,
  count(*) filter (
    where exists (
      select 1 from public.followups f2
      where f2.mobile = f.mobile
        and f2.stage = 'Treatment'
        and f2.status not in ('Cancelled','Incomplete','Rejected','Closed')
    )
  ) as already_advanced_all_years,
  count(*) filter (
    where not exists (
      select 1 from public.followups f2
      where f2.mobile = f.mobile
        and f2.stage = 'Treatment'
        and f2.status not in ('Cancelled','Incomplete','Rejected','Closed')
    )
  ) as remaining_after_treatment_exclusion
from public.followups f
where f.stage = 'Patient'
  and f.status not in ('Cancelled','Incomplete','Rejected','Closed')
  and f.branch = 'Cooch Behar';
