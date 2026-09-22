-- 🗑️ V1383 (১২.০৯.২০২৬, TK-নির্দেশ, নকল ডেটাবেসে হাতে-বসানো টেস্ট-ডেটা দিয়ে
-- যাচাই করা) — V1382-এর ফলে পাওয়া ৬টা ডুপ্লিকেট জোড়ার মধ্যে যে ৪টা প্রায়
-- নিশ্চিতভাবেই ভুল করে দুইবার-চাপা (দুটো সময়ের ফারাক ১ সেকেন্ড থেকে ২৮
-- সেকেন্ড — ডাবল-ট্যাপের চিহ্ন), শুধু সেই ৪টাই এখানে মোছা হচ্ছে।
--
-- বাকি ২টা (Falakata/TK BISWAS: Rahim Munda Hasda ₹15,900 ও RABINDRA DAS
-- ₹20,700 — দুটো এন্ট্রির মধ্যে ৩৭-৪১ মিনিট ফারাক) **ইচ্ছাকৃতভাবে বাদ**
-- — এতটা সময়ের ফারাক ডাবল-ট্যাপ নয়, তাই TK-র সরাসরি নিশ্চয়তা ছাড়া
-- ছোঁয়া হয়নি (আন্দাজে নয়)।
--
-- প্রতিটা জোড়ায় **পরেরটা** (দ্বিতীয়বার সেভ হওয়া কপি) মোছা হচ্ছে, প্রথমটা
-- (আসল) অক্ষত থাকছে। referralPaid/referralDue দুটোই মোছার পরে ঠিক
-- হিসাব করে বসানো হচ্ছে (bug-এর আগে যা হওয়ার কথা ছিল)। আগের array-অর্ডার
-- (নতুন সবচেয়ে উপরে) অক্ষত রাখা হয়েছে।
--
-- ⛔ নিরাপত্তা: BEGIN...COMMIT-এর ভিতরে; শেষে যাচাই সহ SELECT আছে —
--   ফলাফল না মিললে COMMIT-এর আগে ROLLBACK লিখে থামানো যাবে।

begin;

-- ── AMIT GOLDAR (Kishanganj) — ৩টা ডুপ্লিকেট এই একই সারিতে ──
update public.doctor_visits
set "referralPayments" = (
      select coalesce(jsonb_agg(e order by e->>'createdAt' desc), '[]'::jsonb)
      from jsonb_array_elements("referralPayments") e
      where e->>'id' not in (
        'ref_1789198633450',  -- ANAND KUMAR ₹3,250 (দ্বিতীয়বার, ~1 সেকেন্ড পরে)
        'ref_1787833370433',  -- MD HASIM ₹5,250 (দ্বিতীয়বার, ~1 সেকেন্ড পরে)
        'ref_1789142862947'   -- BINOD ROY ₹7,500 (দ্বিতীয়বার, ~28 সেকেন্ড পরে)
      )
    ),
    "updatedAt" = to_char(now() at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where id = 'dv_0408e708e4294c7b9f528d58a116c9a8';

update public.doctor_visits dv
set "referralPaid" = t.paid::text,
    "referralDue"  = t.due::text
from (
  select dv2.id,
    coalesce(sum(case when lower(e->>'status')='paid' then (e->>'amount')::numeric else 0 end),0) as paid,
    coalesce(sum(case when lower(e->>'status')!='paid' then (e->>'amount')::numeric else 0 end),0) as due
  from public.doctor_visits dv2
  cross join lateral jsonb_array_elements(coalesce(dv2."referralPayments",'[]'::jsonb)) e
  where dv2.id = 'dv_0408e708e4294c7b9f528d58a116c9a8'
  group by dv2.id
) t
where dv.id = t.id;

-- ── RABINDRA NATH SARKAR (Cooch Behar) — MAJID MANDAL ₹4,600 (দ্বিতীয়বার, ~0.4 সেকেন্ড পরে) ──
update public.doctor_visits
set "referralPayments" = (
      select coalesce(jsonb_agg(e order by e->>'createdAt' desc), '[]'::jsonb)
      from jsonb_array_elements("referralPayments") e
      where e->>'id' not in ('ref_1784790720599')
    ),
    "updatedAt" = to_char(now() at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where id = 'dv_0ce31bef23b8450cbd0f937c33f8eac8';

update public.doctor_visits dv
set "referralPaid" = t.paid::text,
    "referralDue"  = t.due::text
from (
  select dv2.id,
    coalesce(sum(case when lower(e->>'status')='paid' then (e->>'amount')::numeric else 0 end),0) as paid,
    coalesce(sum(case when lower(e->>'status')!='paid' then (e->>'amount')::numeric else 0 end),0) as due
  from public.doctor_visits dv2
  cross join lateral jsonb_array_elements(coalesce(dv2."referralPayments",'[]'::jsonb)) e
  where dv2.id = 'dv_0ce31bef23b8450cbd0f937c33f8eac8'
  group by dv2.id
) t
where dv.id = t.id;

-- ── যাচাই — COMMIT করার আগে এই ফলাফল নিজে চোখে দেখে নিন ──
select id, name, "referralPaid", "referralDue", jsonb_array_length("referralPayments") as total_entries
from public.doctor_visits
where id in ('dv_0408e708e4294c7b9f528d58a116c9a8','dv_0ce31bef23b8450cbd0f937c33f8eac8');

commit;
