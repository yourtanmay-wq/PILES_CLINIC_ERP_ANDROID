-- ═══════════════════════════════════════════════════════════════════════════
-- V1144 (০৬.০৯.২০২৬) — Reminders
-- TK-অনুমোদিত ফটো-প্রুফ: "হ্যাঁ পাশ, বসিয়ে দিন, সাবধানে"
--
-- এক ব্রাঞ্চের স্টাফ অন্য ব্রাঞ্চের ডাক্তারকে রোগী-ভিত্তিক রিমাইন্ডার পাঠাবেন
-- (ওষুধ আনতে হবে · ট্রিটমেন্ট আছে · অন্য কিছু)। তাই সারিটা ক্লাউডে থাকতেই হয়।
--
-- ⛔ কোনো পুরনো টেবিল ছোঁয়া হয়নি — এটা সম্পূর্ণ নতুন একটা টেবিল।
-- ⛔ একবারই চালাতে হবে; আবার চালালেও কিছু ভাঙে না (if not exists)।
-- ═══════════════════════════════════════════════════════════════════════════

create table if not exists public.reminders (
  "id"            text primary key,
  "branch"        text,          -- কোন ব্রাঞ্চের রোগী
  "patientName"   text,
  "patientMobile" text,
  "disease"       text,
  "type"          text,          -- Medicine · Treatment · Other
  "details"       text,
  "remindOn"      text,          -- yyyy-MM-dd
  "toCode"        text,          -- কাকে পাঠানো হলো (স্টাফ-কোড / ডাক্তারের নাম)
  "toName"        text,
  "fromCode"      text,          -- কে পাঠালেন
  "fromName"      text,
  "status"        text,          -- sent · seen · accepted · done
  "sentAt"        text,
  "seenAt"        text,
  "acceptedAt"    text,
  "doneAt"        text,
  "createdBy"     text,
  "createdAt"     text,
  "updatedAt"     text
);

-- কাদের তালিকা বারবার পড়া হয় — সেই দুটো ঘরেই সূচি, যাতে পড়া সরু ও দ্রুত থাকে।
create index if not exists reminders_to_idx      on public.reminders ("toCode");
create index if not exists reminders_from_idx    on public.reminders ("fromCode");
create index if not exists reminders_patient_idx on public.reminders ("patientMobile");

alter table public.reminders disable row level security;
notify pgrst, 'reload schema';

-- ─── দেখার জন্য (কিছু বদলায় না) ─────────────────────────────────────────
select count(*) as reminders_row_count from public.reminders;
