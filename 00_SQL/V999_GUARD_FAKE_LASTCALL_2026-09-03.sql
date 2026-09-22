-- V999 (০৩.০৯.২০২৬) — ডেটাবেসের পাহারা: মিথ্যা "LAST CALL" তারিখ আর ঢুকবে না
--
-- কেন দরকার: ৫০টা সারিতে `lastCallDate` এমনভাবে বসেছিল যে সারির নিজের
-- `updatedAt` এক চুলও নড়েনি, `history`-তেও ওই দিনের কল লেখা নেই। কোন পুরনো
-- কোড এটা করেছিল সেটা প্রমাণ করা যায়নি, আর `device_logins` বলছে পুরনো
-- ভার্সনের ফোন এখনো চালু (৯.১৩ · ৯.৪২ · ৯.৮৫)। তাই পাহারাটা **ডেটাবেসেই**
-- বসছে — ফোন যে ভার্সনেরই হোক, কাজ করবে।
--
-- নিয়মটা একটাই: `lastCallDate` বদলাচ্ছে অথচ `updatedAt` **এক চুলও বদলায়নি**
-- ⇒ ওটা আসল কল নয়, তাই পুরনো তারিখটাই রাখা হয়।
--   · অ্যাপের চারটে আসল পথই `updatedAt` বসায় (কোড খুলে গোনা) ⇒ কখনো আটকাবে না
--   · সময়-অঞ্চলের কোনো হিসাব নেই ⇒ রাত-দিনে আচরণ এক
--   · বাকি ঘরগুলো (রিমার্ক · nextFollow · callCount · history) স্বাভাবিকভাবেই বসে
--   · সারি বাতিল হয় না — শুধু ওই একটা ঘর পুরনো মানে ফেরে ⇒ কোনো তথ্য হারায় না
--
-- ⚠️ হাতে SQL দিয়ে `lastCallDate` সারাতে হলে **সঙ্গে `updatedAt`-ও বসাতে হবে**
--    (V998-এর সারানোর SQL সেটাই করে), নইলে এই পাহারা সেটাও ফিরিয়ে দেবে।

-- ── অংশ ১: ধরা-পড়া চেষ্টার ছোট খাতা (নতুন টেবিল, কিছুই ছোঁয় না) ──
create table if not exists public.rk_blocked_lastcall (
  "id" text primary key,
  "rowId" text,
  "attemptedDate" text,
  "keptDate" text,
  "at" text
);
alter table public.rk_blocked_lastcall disable row level security;

-- ── অংশ ২: পাহারা ─────────────────────────────────────────────
create or replace function public._rk_guard_fake_lastcall()
returns trigger as $$
declare
  attempted text;
begin
  if NEW."lastCallDate" is distinct from OLD."lastCallDate"
     and NEW."updatedAt" is not distinct from OLD."updatedAt" then
    -- যে তারিখটা বসাতে চাওয়া হয়েছিল, আগে ধরে রাখা (নইলে খাতায় ভুল উঠত)
    attempted := NEW."lastCallDate";
    -- মিথ্যা লেখা — পুরনো তারিখই রাখা হয়
    NEW."lastCallDate" := OLD."lastCallDate";
    -- কী বসাতে চাওয়া হয়েছিল, নীরবে জমা (ব্যর্থ হলেও মূল লেখা কখনো আটকাবে না)
    begin
      insert into public.rk_blocked_lastcall("id","rowId","attemptedDate","keptDate","at")
      values (md5(random()::text || clock_timestamp()::text), NEW."id", attempted, OLD."lastCallDate",
              to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'));
    exception when others then
      null;   -- জমা রাখা ব্যর্থ হলেও কিচ্ছু আটকাবে না
    end;
  end if;
  return NEW;
end;
$$ language plpgsql;

-- ── অংশ ৩: পাহারাটা চালু করা ──────────────────────────────────
drop trigger if exists rk_guard_fake_lastcall on public.followups;
create trigger rk_guard_fake_lastcall before update on public.followups
for each row execute function public._rk_guard_fake_lastcall();

-- ── ফিরিয়ে নিতে হলে (দরকার হলে) ───────────────────────────────
-- drop trigger if exists rk_guard_fake_lastcall on public.followups;
