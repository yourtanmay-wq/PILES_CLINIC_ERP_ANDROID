-- ═══════════════════════════════════════════════════════════════════════
-- V1013 — CHECK-UP Queue: "আজ কে এসেছেন" চেনার নিজস্ব ঘর
-- ০৩.০৯.২০২৬ · TK-এর বাছাই "খ" · একবারে চালানো যায়
--
-- কেন দরকার: এতদিন "আজকের রোগী" ঠিক হত `updatedAt` দেখে — অর্থাৎ সারিটা
-- শেষ কবে **লেখা** হয়েছে। অন্য কারণে সারি ছোঁয়া হলেই পুরনো রোগী আজকের
-- তালিকায় ফিরে আসতেন। এখন তালিকায় ওঠার দিনটা নিজের ঘরেই থাকবে।
--
-- ⛔ পুরনো কোনো ঘর ছোঁয়া হয় না (`visitDate` · `registrationDate` অটুট),
--    তাই কাগজে/কার্ডে তারিখ এক অক্ষরও বদলাবে না।
-- ⛔ একাধিকবার চালালেও ক্ষতি নেই।
-- ═══════════════════════════════════════════════════════════════════════

begin;

alter table public.patients add column if not exists "queuedAt" text;

-- পুরনো সারিতে যা সবচেয়ে কাছের সত্য: রোগী কবে এসেছিলেন
update public.patients
   set "queuedAt" = coalesce(nullif("visitDate", ''), nullif("registrationDate", ''), nullif("date", ''))
 where coalesce("queuedAt", '') = ''
   and coalesce(nullif("visitDate", ''), nullif("registrationDate", ''), nullif("date", '')) is not null;

select 'queuedAt বসানো হলো' as step, count(*) as n
  from public.patients where coalesce("queuedAt",'') <> '';

commit;
