-- ═══════════════════════════════════════════════════════════════════════
-- V1088-খ (০৫.০৯.২০২৬ ৯:১১) — TK নিজে চালিয়েছেন, যাচাইয়ে তিনটেই 0
--
-- ① পাহারা `payments` ও `followups`-এও বসানো হলো (আগে শুধু `patients`-এ ছিল)।
--    নইলে রোগী মুছে যেত কিন্তু চেম্বারের কম্পিউটারে থেকে যাওয়া টাকার সারি ও
--    ফলো-আপ আবার সার্ভারে উঠে আসত।
-- ② তিনটে ডেমো রোগী ও তাঁদের সব সারি মোছা:
--    AMAR (JPE-05082026-001, ₹৫,৪০০) · Ramu (KNE-16072026-002, ₹৪০০)
--    · TK (COB-10082026-006, ₹৫০০)  ⇒ পুরনো Collection ₹৬,৩০০ কমল।
-- ⛔ Master-এর নিয়ম অটুট। ⛔ পর্দায় কিছু বদলায়নি, APK লাগে না।
-- ═══════════════════════════════════════════════════════════════════════

drop trigger if exists tk_block_deleted_return on public.payments;
create trigger tk_block_deleted_return before insert or update on public.payments
for each row execute function public.tk_block_deleted_record_return();

drop trigger if exists tk_mark_deleted_payments on public.payments;
create trigger tk_mark_deleted_payments after delete on public.payments
for each row execute function public.tk_mark_deleted_on_delete();

drop trigger if exists tk_block_deleted_return on public.followups;
create trigger tk_block_deleted_return before insert or update on public.followups
for each row execute function public.tk_block_deleted_record_return();

drop trigger if exists tk_mark_deleted_followups on public.followups;
create trigger tk_mark_deleted_followups after delete on public.followups
for each row execute function public.tk_mark_deleted_on_delete();

delete from payments
where "patientId" in ('pat_9641558709','pat_dd3f3f889e274413bbdac107be838135','pat_8001080080');

delete from followups
where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10)
      in ('9641558709','2580369147','8001080080');

delete from patients
where id in ('pat_9641558709','pat_dd3f3f889e274413bbdac107be838135','pat_8001080080');
