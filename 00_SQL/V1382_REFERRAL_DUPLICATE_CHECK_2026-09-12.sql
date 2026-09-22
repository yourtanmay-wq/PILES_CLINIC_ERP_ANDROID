-- 🔍 V1382 (১২.০৯.২০২৬, TK-নির্দেশ) — শুধুমাত্র পড়া (READ-ONLY)।
-- কিছুই বদলায় না বা মোছে না। উদ্দেশ্য: TK-এর রিপোর্ট করা ANAND KUMAR-এর
-- ₹3,250 Due দুইবার সেভ হওয়ার মতো ডুপ্লিকেট Referral Income সারা প্রজেক্টে
-- (সব RMP/ডাক্তার) আর কোথায় কোথায় আছে, তা প্রথমে দেখা — মোছার আগে যাচাই।
--
-- "ডুপ্লিকেট" এখানে ঠিক সেই একই নিয়মে ধরা হয়েছে যা কোডের bug-টা তৈরি
-- করেছিল (DoctorVisitRepository.todaysReferralLike, V1381): একই ডাক্তারের
-- ঘরে (doctor_visits সারি), একই রোগীর মোবাইল, একই তারিখ, একই পরিমাণ (₹0.5
-- পর্যন্ত পার্থক্য মেনে নেওয়া) — এমন দুই বা তার বেশি সারি।
--
-- ফল দেখে TK নিজে বেছে নেবেন কোনগুলো সত্যিই ভুল করে দুইবার হয়েছে
-- (দুইবারই একই স্টাফ/একই মুহূর্তের কাছাকাছি হলে প্রায় নিশ্চিত ভুল),
-- আর কোনগুলো হয়তো সত্যিই ইচ্ছাকৃত দুইবার (যেমন সকালে একবার, বিকেলে
-- আবার একই পরিমাণ) — সেগুলো ছোঁয়া হবে না।

select
  dv.id            as doctor_visit_id,
  dv.name          as doctor_name,
  dv.branch        as doctor_branch,
  entry->>'id'         as entry_id,
  entry->>'patient'    as patient_name,
  entry->>'patientMobile' as patient_mobile,
  entry->>'amount'     as amount,
  entry->>'status'     as status,
  entry->>'date'       as entry_date,
  entry->>'createdAt'  as created_at
from public.doctor_visits dv
cross join lateral jsonb_array_elements(coalesce(dv."referralPayments", '[]'::jsonb)) as entry
where (entry->>'patientMobile') is not null
  and (entry->>'patientMobile') <> ''
  and exists (
    select 1
    from jsonb_array_elements(coalesce(dv."referralPayments", '[]'::jsonb)) e2
    where e2 <> entry
      and e2->>'patientMobile' = entry->>'patientMobile'
      and e2->>'date' = entry->>'date'
      and abs(coalesce(nullif(e2->>'amount','')::numeric, 0)
              - coalesce(nullif(entry->>'amount','')::numeric, 0)) <= 0.5
  )
order by dv.name, entry->>'patientMobile', entry->>'date', entry->>'createdAt';
