-- 🗑️ V1384 (১২.০৯.২০২৬, TK-নির্দেশ "দুইটাই মুছে দিন") — V1382-এর বাকি
-- ২টা ডুপ্লিকেট (Falakata, RMP "TK BISWAS")। TK নিজে পেশেন্টের পাতা
-- দেখে নিশ্চিত হয়েছেন এই টাকাগুলো রোগীর নিজের বিলে নেই — এগুলো RMP-কে
-- দেওয়ার কমিশন (Referral Income), তাই ডুপ্লিকেট।
--
-- প্রতিটাতে **পরেরটা** (দ্বিতীয়বার সেভ হওয়া কপি, ~37-41 মিনিট পরে) মোছা
-- হচ্ছে, প্রথমটা অক্ষত থাকছে। নকল ডেটাবেসে হাতে-বসানো টেস্ট-ডেটা দিয়ে
-- যাচাই করা।

begin;

update public.doctor_visits
set "referralPayments" = (
      select coalesce(jsonb_agg(e order by e->>'createdAt' desc), '[]'::jsonb)
      from jsonb_array_elements("referralPayments") e
      where e->>'id' not in (
        'ref_1786709019452',  -- Rahim Munda Hasda ₹15,900 (দ্বিতীয়বার, ~41 মিনিট পরে)
        'ref_1786709002031'   -- RABINDRA DAS ₹20,700 (দ্বিতীয়বার, ~37 মিনিট পরে)
      )
    ),
    "updatedAt" = to_char(now() at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where id = 'dv_8ad730969a2542f2bef8e26b97c6cbc7';

update public.doctor_visits dv
set "referralPaid" = t.paid::text,
    "referralDue"  = t.due::text
from (
  select dv2.id,
    coalesce(sum(case when lower(e->>'status')='paid' then (e->>'amount')::numeric else 0 end),0) as paid,
    coalesce(sum(case when lower(e->>'status')!='paid' then (e->>'amount')::numeric else 0 end),0) as due
  from public.doctor_visits dv2
  cross join lateral jsonb_array_elements(coalesce(dv2."referralPayments",'[]'::jsonb)) e
  where dv2.id = 'dv_8ad730969a2542f2bef8e26b97c6cbc7'
  group by dv2.id
) t
where dv.id = t.id;

-- ── যাচাই — এই ফলাফল দেখে নিন ──
select id, name, "referralPaid", "referralDue", jsonb_array_length("referralPayments") as total_entries
from public.doctor_visits
where id = 'dv_8ad730969a2542f2bef8e26b97c6cbc7';

commit;
