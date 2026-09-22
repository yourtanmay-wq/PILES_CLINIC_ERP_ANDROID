-- ═══════════════════════════════════════════════════════════════════════════
-- 📋🔒 V1435 (১৩.০৯.২০২৬ সন্ধ্যা, তালিকা ৫৫৪, TK "পাশ") — Daily/Monthly Report-এর
-- **পাঠানো লেখাটা হুবহু জমা রাখার ঘর** (report_text)। এতদিন শুধু সংখ্যা (auto_stats)
-- ও Notes জমা হত; মাস্টার Briefing-এর "View" থেকে এখন রিপোর্টটা দেখতে পারবেন।
-- · নতুন ঘর খালি থাকতে পারে (nullable) — পুরনো অ্যাপ/ওয়েব আগের মতোই লিখবে, কিছু ভাঙে না।
-- · পুরনো রিপোর্টে এই ঘর ফাঁকা থাকবে; অ্যাপ তখন সংখ্যা + notes + IN/OUT থেকে লেখাটা বানিয়ে দেখায়।
-- ⛔ কোনো সারি বদলায়/মোছে না। RLS/নীতি অপরিবর্তিত (মাস্টার আগে থেকেই সব পড়তে পারেন)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;
alter table wn.work_reports add column if not exists report_text text;
comment on column wn.work_reports.report_text is 'V1435: the exact report text sent to Master/WhatsApp (nullable; old rows blank)';
notify pgrst, 'reload schema';
commit;
select column_name, data_type from information_schema.columns
 where table_schema = 'wn' and table_name = 'work_reports' and column_name = 'report_text';
