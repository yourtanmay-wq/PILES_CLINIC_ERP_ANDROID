-- ═══════════════════════════════════════════════════════════════════════════
-- ⛔🔒 V890 (৩০.০৮.২০২৬) — BIR-5 · RESAM KHATUN (9339139852) বাদ
-- TK-নির্দেশ: *"তাকে কাজ থেকে বের করে দেওয়া হয়েছে, তার কোনো ডিটেল যেন
-- অ্যাপে না থাকে, তার নম্বর দিয়ে যেন লগইন না করা যায়।"*
-- ⛔ রোগীর তথ্য · টাকা · ইতিহাস কিছুই মোছে না — শুধু এই কর্মীর নিজের সারি।
-- ⛔ মোছার আগে ব্যাকআপ বসে।
-- ═══════════════════════════════════════════════════════════════════════════

create table if not exists public.v890_staff_backup (
  "id" bigserial primary key, "row" jsonb, "savedAt" timestamptz default now()
);

insert into public.v890_staff_backup("row")
select to_jsonb(t) from hr.staff_profiles t
 where t."person_code" = 'BIR-5'
    or right(regexp_replace(coalesce(t."link_mobile",''),'[^0-9]','','g'),10) = '9339139852';

delete from hr.staff_profiles t
 where t."person_code" = 'BIR-5'
    or right(regexp_replace(coalesce(t."link_mobile",''),'[^0-9]','','g'),10) = '9339139852';

notify pgrst, 'reload schema';

select count(*) as "এখনো বাকি (০ হওয়া চাই)"
  from hr.staff_profiles t
 where t."person_code" = 'BIR-5'
    or right(regexp_replace(coalesce(t."link_mobile",''),'[^0-9]','','g'),10) = '9339139852';
