-- ============================================================
--  PATCH 30.07.2026 : ডুপ্লিকেট followups সারি বন্ধ করা (এক-বারের কাজ)
--  খাতার সারি B141 . TK-এর নির্দেশ (30.07.2026 সকাল, ছবিসহ —
--  SUSMITA DAS · +917797923412) : "রিজেক্ট করা নম্বর আবার ফিরে এলো কি করে?"
--
--  এটা কী করে (সহজ কথায়):
--  একই মোবাইল ও একই stage (যেমন Inquiry)-এর জন্য কখনো কখনো দুটো আলাদা
--  followups সারি তৈরি হয়ে গিয়েছিল (পুরনো একটা বাগ, কোড-সাইডে আজই ঠিক
--  করা হয়েছে — FollowUpRepository.kt-এর updateStatus())। Reject করলে
--  শুধু একটা সারি বন্ধ হত, অন্যটা Active থেকে যেত — তাই রোগী তালিকায়
--  থেকেই যেতেন, পুরনো/অস্পর্শিত রিমার্ক নিয়ে।
--
--  এই SQL সেই পুরনো "থেকে যাওয়া" সারিগুলো একবারে বন্ধ করে দেয় — যেখানেই
--  একই মোবাইল+stage-এর একটা সারি ইতিমধ্যে Cancelled/Incomplete, অথচ
--  আরেকটা সারি এখনো Active, সেই Active সারিটাকেও Cancelled করে দেওয়া হয়।
--
--  ⛔ কোনো সারি মোছা হয় না — শুধু status বদলায়।
--  ⛔ শুধু "সত্যিকারের ডুপ্লিকেট" ছোঁয়া হয় — যে রোগীর একটাই সারি আছে,
--     বা যাঁর কোনো সারিই Rejected/Incomplete নয়, তাঁর কিছু বদলায় না।
--  ⛔ Chamber/Payment/Treatment — কোনো টাকার হিসাব ছোঁয়া হয় না।
--  ⛔ এই SQL একাধিকবার চালালেও কোনো ক্ষতি নেই (দ্বিতীয়বার চালালে কিছুই
--     বদলাবে না, কারণ প্রথমবারই সব ঠিক হয়ে যাবে)।
--
--  TK-কে যা করতে হবে: Supabase → SQL Editor → এই ফাইলের লেখাটা পেস্ট
--  করে RUN করুন। "Success" দেখালেই কাজ শেষ।
-- ============================================================

update public.followups f
set status = 'Cancelled',
    "updatedAt" = to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where f.status is distinct from 'Cancelled'
  and f.status is distinct from 'Incomplete'
  and exists (
    select 1
    from public.followups g
    where g.id <> f.id
      and g.stage = f.stage
      and right(regexp_replace(coalesce(g.mobile, ''), '[^0-9]', '', 'g'), 10)
          = right(regexp_replace(coalesce(f.mobile, ''), '[^0-9]', '', 'g'), 10)
      and length(right(regexp_replace(coalesce(f.mobile, ''), '[^0-9]', '', 'g'), 10)) = 10
      and g.status in ('Cancelled', 'Incomplete')
  );

-- যাচাইয়ের জন্য (এটা চালানোর দরকার নেই, শুধু দেখতে চাইলে):
-- এই কোয়েরিটা দেখাবে কতগুলো সারি সত্যিই বদলাল -- উপরের UPDATE চালানোর
-- ঠিক আগে চালালে "কতগুলো ঠিক হতে চলেছে" সংখ্যাটা দেখা যাবে।
--
-- select count(*) from public.followups f
-- where f.status is distinct from 'Cancelled'
--   and f.status is distinct from 'Incomplete'
--   and exists (
--     select 1 from public.followups g
--     where g.id <> f.id
--       and g.stage = f.stage
--       and right(regexp_replace(coalesce(g.mobile, ''), '[^0-9]', '', 'g'), 10)
--           = right(regexp_replace(coalesce(f.mobile, ''), '[^0-9]', '', 'g'), 10)
--       and length(right(regexp_replace(coalesce(f.mobile, ''), '[^0-9]', '', 'g'), 10)) = 10
--       and g.status in ('Cancelled', 'Incomplete')
--   );
