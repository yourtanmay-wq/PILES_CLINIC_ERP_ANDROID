-- ═══════════════════════════════════════════════════════════════════════
-- V1088 (০৫.০৯.২০২৬) — মুছে ফেলা রোগী যেন আর কোনোদিন ফিরে না আসে
--
-- 🔴 সমস্যা (TK, সারি ১৯২): SQL দিয়ে ডেমো রোগী মুছলেও কদিন পরে ফিরে আসত।
--    কারণ: ডেটাবেসের পাহারা `tk_block_deleted_return` (BEFORE INSERT OR
--    UPDATE ON patients) শুধু তখনই আটকায় যখন `deleted_records`-এ
--    `'patients|<id>'` চিহ্নটা থাকে। অ্যাপ থেকে মুছলে চিহ্নটা বসে, কিন্তু
--    SQL দিয়ে মুছলে বসত না — তাই চেম্বারের কম্পিউটারে থেকে যাওয়া পুরনো কপি
--    পরের সেভে আবার সার্ভারে উঠে যেত।
--
-- ✅ সমাধান: চিহ্ন বসানোর কাজটা ডেটাবেসের ভিতরেই। যেভাবেই মোছা হোক
--    (অ্যাপ · SQL · অন্য যেকোনো পথ) চিহ্নটা নিজে থেকেই বসবে।
--
-- ⛔ যা বদলায় না:
--    · Master-এর নিয়ম অটুট — Trash ও Delete Forever আগের মতোই শুধু Master
--    · Master Restore করলে অ্যাপ আগে চিহ্নটা তোলে, তাই রেকর্ড ঠিকই ফেরে
--    · পুরনো `tk_block_deleted_record_return()` ছোঁয়া হয়নি
--    · পর্দায় কিছু বদলায় না · নতুন APK লাগে না · Egress-এ প্রভাব নেই
--
-- ⚠️ যেটা TK-কে জেনে রাখতে হবে: এর পরে SQL দিয়ে মোছা রোগীর Trash-কপি
--    থাকে না, তাই সেটা আর ফেরানো যাবে না — মোছা মানে সত্যিই চিরতরে।
-- ═══════════════════════════════════════════════════════════════════════

create or replace function public.tk_mark_deleted_on_delete()
returns trigger
language plpgsql
as $fn$
begin
  insert into public.deleted_records (id, "tableName", "rowId", "deletedBy", "deletedAt")
  values (
    TG_TABLE_NAME || '|' || OLD.id,
    TG_TABLE_NAME,
    OLD.id,
    'db-auto',
    to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
  )
  on conflict (id) do nothing;
  return OLD;
end;
$fn$;

drop trigger if exists tk_mark_deleted_patients on public.patients;

create trigger tk_mark_deleted_patients
after delete on public.patients
for each row execute function public.tk_mark_deleted_on_delete();

-- যাচাই: নিচেরটা চালালে নতুন পাহারাটা তালিকায় দেখা যাবে
-- select tgname from pg_trigger where tgrelid = 'public.patients'::regclass and not tgisinternal;
