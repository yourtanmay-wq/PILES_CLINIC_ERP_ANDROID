-- ============================================================================
-- V418 — খাতার ডুপ্লিকেট এন্ট্রি: বাড়তিগুলো মোছা + ভবিষ্যতে আটকে দেওয়া
--   TK-নির্দেশ (১৭.০৮.২০২৬): *"একদম মুছে দিন · ভবিষ্যতে ডুপ্লিকেট এন্ট্রির
--   জন্য আটকে দেয়"*
--
-- যা ধরা পড়েছিল: ১৩/০৩/২০২৬ · Cooch Behar · ৯,৫০০ / ৫,০০০ — **তিনটে সারি**,
--   তিনটেই ১৭/০৮ ০৮:২২:৫১ → ০৮:২২:৫২, অর্থাৎ এক সেকেন্ডের মধ্যে।
--   Save-এ পরপর চাপ পড়েছিল।
--
-- ⛔ প্রতিটা দলে **সবচেয়ে পুরনো সারিটা থেকেই যায়** — শুধু বাড়তিগুলো যায়।
-- ⛔ আলাদা তারিখ · আলাদা ব্রাঞ্চ · আলাদা অঙ্ক — কিচ্ছু ছোঁয়া হয় না।
-- ⛔ খরচের (fin.expenses) টেবিলে হাত দেওয়া হয়নি — সেখানে একই দিনে একই অঙ্কের
--    দুটো আসল খরচ থাকা স্বাভাবিক (যেমন দুবার একই ভাড়া)।
-- ============================================================================

-- ── ধাপ ০ (শুধু দেখা): মোছার আগে কী কী যাবে, একবার চোখ বুলিয়ে নিন ──────────
-- select entry_date, branch, cash, online, count(*)
--   from fin.collections where ignored = false
--   group by 1,2,3,4 having count(*) > 1 order by 1;

-- ── ধাপ ১: বাড়তি সারি মোছা (প্রতিটা দলে সবচেয়ে পুরনোটা থাকে) ──────────────
with ranked as (
  select id,
         row_number() over (
           partition by entry_date, branch, cash, online
           order by created_at, id
         ) as rn
  from fin.collections
  where ignored = false
)
delete from fin.collections c
using ranked r
where c.id = r.id
  and r.rn > 1;

-- ── ধাপ ২: ভবিষ্যতের পাহারা — ডেটাবেসই আর দ্বিতীয়বার বসতে দেবে না ─────────
-- একই তারিখ + একই ব্রাঞ্চ + হুবহু একই Cash + হুবহু একই Online ⇒ দ্বিতীয় সারি
-- বসবেই না। যে ফোন/কম্পিউটার থেকেই চেষ্টা হোক, যত দ্রুতই চাপ পড়ুক — আটকাবে।
-- ⛔ "বাদ দেওয়া" (ignored = true) সারি এই নিয়মের বাইরে, তাই পুরনো সংশোধন আটকায় না।
create unique index if not exists fin_collections_no_duplicate_uidx
  on fin.collections(entry_date, branch, cash, online)
  where ignored = false;

notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া) ────────────────────────────────────────────────
-- select entry_date, branch, cash, online, count(*)
--   from fin.collections where ignored = false
--   group by 1,2,3,4 having count(*) > 1;      -- ০ সারি আসা উচিত
