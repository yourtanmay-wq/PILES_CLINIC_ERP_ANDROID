-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V864 (৩০.০৮.২০২৬) — **ভুয়া / ডেমো ধাঁচের নম্বর ও নাম** খুঁজে বার করা
--    ⛔ একটাও সারি মোছে না — শুধু তালিকা দেখায়। **একটাই query**, তাই
--       Supabase ফলটা দেখাবে।
--
-- TK: *"9876543210 · 1234567890 · 11111122222 · 3334445556 — এই ধরনের যত
-- নম্বর আছে সেগুলোও ডেমো… প্রয়োজনে আরও গভীরে গিয়ে যাচাই করে দেখুন"*
--
-- যে যে কারণে সন্দেহজনক ধরা হয়:
--   ১. **একই অঙ্ক বারবার** — নম্বরে ৪ রকমের কম আলাদা অঙ্ক (1111122222, 3334445556)
--   ২. **ধারাবাহিক অঙ্ক** — 1234567890 / 9876543210 ধাঁচের
--   ৩. **ভারতীয় মোবাইল নয়** — ১০ অঙ্ক নয়, বা ৬/৭/৮/৯ দিয়ে শুরু নয়
--   ৪. **নামেই ডেমো** — নামে TEST / DEMO / ABC / XYZ / ASDF আছে
-- ═══════════════════════════════════════════════════════════════════════════

with raw as (
  select coalesce("mobile",'') as mm, coalesce("name",'')  as nm, 'enquiries' as t from public.enquiries
  union all
  select coalesce("mobile",''),        coalesce("name",''),        'followups'  from public.followups
  union all
  select coalesce("mobile",''),        coalesce("name",''),        'patients'   from public.patients
  union all
  select coalesce("mobile",''),        coalesce("name",''),        'payments'   from public.payments
),
norm as (
  select regexp_replace(mm,'[^0-9]','','g') as digits,
         right(regexp_replace(mm,'[^0-9]','','g'),10) as m10,
         nm, t
    from raw
   where coalesce(mm,'') <> ''
),
agg as (
  select m10,
         min(digits) as "পুরো নম্বর",
         max(nm)     as "নাম",
         count(*) filter (where t='enquiries') as "এনকোয়ারি",
         count(*) filter (where t='followups') as "ফলোআপ",
         count(*) filter (where t='patients')  as "রোগী",
         count(*) filter (where t='payments')  as "পেমেন্ট"
    from norm group by m10
),
flag as (
  select a.*,
         (select count(distinct ch) from regexp_split_to_table(a.m10,'') ch) as dd,
         (position(a.m10 in '01234567890123456789') > 0
          or position(a.m10 in '98765432109876543210') > 0)                  as seq,
         (length(a.m10) <> 10 or left(a.m10,1) not in ('6','7','8','9'))     as badfmt,
         (upper(coalesce(a."নাম",'')) ~ '(TEST|DEMO|ABC|XYZ|ASDF|QWER)')     as badname
    from agg a
)
select "পুরো নম্বর"  as "নম্বর",
       "নাম",
       case
         when badfmt   then 'ভারতীয় মোবাইল নয়'
         when seq      then 'ধারাবাহিক অঙ্ক (1234…/9876…)'
         when dd <= 4  then 'একই অঙ্ক বারবার (' || dd || ' রকম অঙ্ক)'
         when badname  then 'নামেই ডেমো'
       end                                                as "কেন সন্দেহ",
       "এনকোয়ারি", "ফলোআপ", "রোগী", "পেমেন্ট"
  from flag
 where badfmt or seq or dd <= 4 or badname
 order by 3, 1;
