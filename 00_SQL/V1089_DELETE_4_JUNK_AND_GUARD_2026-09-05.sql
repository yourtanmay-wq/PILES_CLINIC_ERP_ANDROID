-- ═══════════════════════════════════════════════════════════════════════
-- V1089-খ (০৫.০৯.২০২৬) — বাকি ৪টে আজেবাজে সারি মোছা + পাহারা সব ঘরে
--
-- ① একই ফেরা-আটকানোর পাহারা এখন `enquiries` · `doctor_visits` · `medical`-এও।
--    (আগে ছিল patients · payments · followups) ⇒ রুল ৭ — এক দোষের সব জায়গা।
--    ⛔ `trash` ও `deleted_records`-এ ইচ্ছে করেই বসানো হয়নি — Restore-এর সময়
--       ওই সারিগুলো মোছা-বসা হয়, পাহারা বসালে Restore ভেঙে যেত।
-- ② মোছা হলো (টাকার কোনো সারি নেই, তাই কোনো হিসাব বদলায় না):
--    fhj (এনকোয়ারি, জলপাইগুড়ি) · Ghh · F · A Z (ফলো-আপ, জলপাইগুড়ি)
-- ⛔ যেগুলো রাখা হলো — আসল: SOTTTAN ROY · Dr Sanjit Kumar Biswas ·
--    NONOTA MURMU · RAJ (এবং তাঁদের ফলো-আপ)।
-- ═══════════════════════════════════════════════════════════════════════

drop trigger if exists tk_block_deleted_return on public.enquiries;
create trigger tk_block_deleted_return before insert or update on public.enquiries
for each row execute function public.tk_block_deleted_record_return();
drop trigger if exists tk_mark_deleted_enquiries on public.enquiries;
create trigger tk_mark_deleted_enquiries after delete on public.enquiries
for each row execute function public.tk_mark_deleted_on_delete();

drop trigger if exists tk_block_deleted_return on public.doctor_visits;
create trigger tk_block_deleted_return before insert or update on public.doctor_visits
for each row execute function public.tk_block_deleted_record_return();
drop trigger if exists tk_mark_deleted_doctor_visits on public.doctor_visits;
create trigger tk_mark_deleted_doctor_visits after delete on public.doctor_visits
for each row execute function public.tk_mark_deleted_on_delete();

drop trigger if exists tk_block_deleted_return on public.medical;
create trigger tk_block_deleted_return before insert or update on public.medical
for each row execute function public.tk_block_deleted_record_return();
drop trigger if exists tk_mark_deleted_medical on public.medical;
create trigger tk_mark_deleted_medical after delete on public.medical
for each row execute function public.tk_mark_deleted_on_delete();

delete from enquiries where id = 'enq_d0efc649dd0b49f7a4b8a8b809419fda';

delete from followups where id in (
  'fu_574b7201e48f4ef1bdc468ce869dff63',
  'fu_93f544726ffa44e2a822e4941affcf08',
  'fu_b29b81d0014c46f398cd0e2272fd98d6');
