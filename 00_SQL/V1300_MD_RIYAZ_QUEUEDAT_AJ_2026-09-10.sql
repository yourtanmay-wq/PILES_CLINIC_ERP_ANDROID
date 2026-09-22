-- ৪১৬: MD RIYAZ (KNE-01082026-001) — লাইনে ওঠার দিন (queuedAt) ফাঁকা বলে CHECK-UP Queue-তে দেখাচ্ছে না; আজকের দিন বসানো
update public.patients
set "queuedAt" = '2026-09-10', "updatedAt" = now()::text
where id = 'pat_8337885258' and "patientId" = 'KNE-01082026-001' and coalesce("queuedAt",'') = '';
select id, "patientId", stage, queue, "doctorComplete", "queuedAt" from public.patients where id = 'pat_8337885258';
