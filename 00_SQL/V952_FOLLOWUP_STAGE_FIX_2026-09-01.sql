-- ০১.০৯.২০২৬ — TK-রিপোর্ট (FAIZ ANWAR): বিল ক্লিয়ার, চিকিৎসা চালু, তবু
-- Follow-up-এর "Visit" তালিকায় ভেসে ছিলেন।
--
-- কারণ (কোড ধরে যাচাই): Advance নেওয়ার সময় নতুন `stage='Treatment'` সারি বসে
-- আর পুরনো `stage='Patient'` সারিটা **মুছে ফেলার কথা** (app.js — Visit-stage
-- Advance-এ `filter(f => f.id !== fid)`)। কোনো কারণে সেটা হয়নি।
--
-- মাপা তথ্য (TK-এর ডেটাবেসে): ১৮ জনের Treatment সারি **নেই** (আসল আটকে থাকা),
-- আর ১৭১ জনের Treatment সারি **আছেই** (পুরনো Patient সারিটা বাড়তি)।
--
-- ⛔ কোনো সারি **মোছা হয়নি** — বাড়তিগুলো শুধু `status='Closed'` করা হয়েছে,
--    তাই ইতিহাস/রিমার্ক সব অক্ষত থাকে।
-- ⛔ বদলানোর আগে সবকিছু `public.bak_stage_fix_01092026`-এ জমা।

create table if not exists public.bak_stage_fix_01092026 as
select to_jsonb(f) j from public.followups f
join public.patients p on p.mobile = f.mobile
where f.stage='Patient' and f.status='Active'
  and p.stage in ('Treatment Running','Treatment Complete');

-- ১) যাঁদের Treatment সারি নেই — সারিটাকেই Treatment বানানো
update public.followups f set stage='Treatment', "updatedAt"=now()
from public.patients p
where p.mobile=f.mobile and f.stage='Patient' and f.status='Active'
  and p.stage in ('Treatment Running','Treatment Complete')
  and not exists (select 1 from public.followups t where t.mobile=f.mobile and t.stage='Treatment');

-- ২) যাঁদের Treatment সারি আছে — বাড়তি পুরনো সারিটা বন্ধ (মোছা নয়)
update public.followups f set status='Closed', "updatedAt"=now()
from public.patients p
where p.mobile=f.mobile and f.stage='Patient' and f.status='Active'
  and p.stage in ('Treatment Running','Treatment Complete')
  and exists (select 1 from public.followups t where t.mobile=f.mobile and t.stage='Treatment');
