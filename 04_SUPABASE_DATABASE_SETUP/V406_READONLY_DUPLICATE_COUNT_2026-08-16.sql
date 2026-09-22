-- =====================================================================
-- V406 — Follow-up-এর বাড়তি সারি কতগুলো, শুধু **গুনে দেখা** (16.08.2026)
--        🔧 সংশোধিত সংস্করণ (TK-এর লাইভ এরর দেখে)
-- =====================================================================
-- ⛔⛔ এই ফাইল কিচ্ছু বদলায় না, কিচ্ছু মোছে না। শুধু পড়ে ও গুনে দেখায়।
--     নিরাপদে যতবার খুশি চালানো যায়।
--
-- 🔧 কী ভুল ছিল ও কীভাবে সারানো হলো:
--    TK চালিয়ে পেয়েছিলেন — `ERROR: 42804: COALESCE types text and integer
--    cannot be matched`।
--    কারণ: আমি ধরে নিয়েছিলাম `callCount` ঘরটা **সংখ্যা**। আসলে এই
--    প্রজেক্টে ওটা **লেখা (text)** হিসেবে রাখা — ঠিক যেমন `bill`,
--    `discount`, `referralPaid` ঘরগুলোও text (V325-এর `rmp_safe_number`
--    সেই কারণেই লেখা হয়েছিল)।
--    সমাধান: সংখ্যা হলে তবেই সংখ্যা ধরা হয়, নইলে ০ —
--            `case when "callCount"::text ~ '^[0-9]+$' then …::numeric else 0 end`
--    ⇒ ঘরটা text হোক, সংখ্যা হোক, ফাঁকা হোক বা আবোল-তাবোল লেখা হোক —
--      কোনো অবস্থাতেই আর এরর হবে না।
--
-- ✅ যাচাই: আসল PostgreSQL 16-এ `callCount` **text** বানিয়ে, ফাঁকা মান ও
--    `history = null` সহ ৬৬টি সারিতে চালানো হয়েছে — চলেছে, আর চালানোর
--    পরেও **৬৬টি সারিই অক্ষত**।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- চারটে ফল আলাদা আলাদা দেখাবে।
-- =====================================================================

-- ---------------------------------------------------------------------
-- ১) এক নজরে — মোট কত, বাড়তি কত
-- ---------------------------------------------------------------------
with grp as (
  select coalesce(nullif(trim("refId"),''),
         'MOB:'||right(regexp_replace(coalesce(mobile,''),'\D','','g'),10)) as person,
         coalesce(stage,'') as stage, count(*) as n
  from public.followups group by 1,2
)
select
  (select count(*) from public.followups)            as "মোট সারি",
  (select count(*) from grp)                         as "আসল সংখ্যা",
  (select coalesce(sum(n-1),0) from grp where n > 1) as "বাড়তি সারি",
  (select count(*) from grp where n > 1)             as "কতজনের ডুপ্লিকেট",
  (select coalesce(max(n),0) from grp)               as "সর্বোচ্চ একজনের";

-- ---------------------------------------------------------------------
-- ২) সবচেয়ে খারাপ ৩০টা — কার কত, নাম-ব্রাঞ্চ সহ
-- ---------------------------------------------------------------------
select
  coalesce(nullif(trim("refId"),''),
    'MOB:'||right(regexp_replace(coalesce(mobile,''),'\D','','g'),10)) as "রোগী",
  max(name) as "নাম", max(branch) as "ব্রাঞ্চ", coalesce(stage,'') as "ধাপ",
  count(*) as "কত সারি", count(*) - 1 as "বাড়তি",
  min("createdAt") as "প্রথম", max("createdAt") as "শেষ",
  count(*) filter (where "history" is null
        or "history"::text in ('[]','null','')) as "ফাঁকা history",
  -- 🔧 text-নিরাপদ
  count(*) filter (where "callCount"::text ~ '^[0-9]+$'
        and ("callCount"::text)::numeric > 0)   as "কল-গোনা আছে এমন",
  count(*) filter (where coalesce("nextFollow",'') <> '') as "পরের তারিখ আছে এমন"
from public.followups
group by 1,4 having count(*) > 1
order by count(*) desc limit 30;

-- ---------------------------------------------------------------------
-- ৩) কোন ধরনের সারি জমেছে
--    · `fu_pat_…` / `fu_inq_…` = স্থির id (V406-এর পরে এগুলোই তৈরি হবে)
--    · বাকি `fu_…` = পুরনো random id — এগুলোই বেড়েছিল
-- ---------------------------------------------------------------------
select
  case when id like 'fu\_pat\_%' then 'স্থির id fu_pat_'
       when id like 'fu\_inq\_%' then 'স্থির id fu_inq_'
       when id like 'fu\_%'      then 'পুরনো random id'
       else 'অন্য' end as "ধরন",
  count(*) as "কত সারি",
  count(*) filter (where "history" is null
        or "history"::text in ('[]','null','')) as "ফাঁকা history"
from public.followups group by 1 order by 2 desc;

-- ---------------------------------------------------------------------
-- ৪) 🔴 মোছার আগে জানা দরকার — বাড়তি সারিগুলোয় সত্যিকারের কাজের তথ্য
--    আছে কিনা। সবচেয়ে ভরা সারিটা রাখা হয় (rn = 1), বাকিগুলো "বাড়তি"।
-- ---------------------------------------------------------------------
with ranked as (
  select id,
    case when "callCount"::text ~ '^[0-9]+$' then ("callCount"::text)::numeric else 0 end as cc,
    coalesce("nextFollow",'') as nf, "history",
    row_number() over (
      partition by coalesce(nullif(trim("refId"),''),
                   'MOB:'||right(regexp_replace(coalesce(mobile,''),'\D','','g'),10)),
                   coalesce(stage,'')
      order by (case when "history" is not null
                     and "history"::text not in ('[]','null','') then 1 else 0 end) desc,
               (case when "callCount"::text ~ '^[0-9]+$' then ("callCount"::text)::numeric else 0 end) desc,
               (case when coalesce("nextFollow",'') <> '' then 1 else 0 end) desc,
               "updatedAt" desc nulls last) as rn
  from public.followups
)
select
  count(*) filter (where rn > 1)                        as "বাড়তি সারি মোট",
  count(*) filter (where rn > 1 and cc > 0)             as "কল-গোনা আছে",
  count(*) filter (where rn > 1 and nf <> '')           as "পরের তারিখ আছে",
  count(*) filter (where rn > 1 and "history" is not null
        and "history"::text not in ('[]','null',''))    as "history আছে",
  count(*) filter (where rn > 1 and cc = 0 and nf = ''
        and ("history" is null or "history"::text in ('[]','null','')))
                                                        as "🟢 ফাঁকা, মোছা নিরাপদ"
from ranked;

-- =====================================================================
-- ⛔ এখানেই শেষ। একটাও সারি মোছা হয়নি, একটাও ঘর বদলায়নি।
--    ৪ নম্বরের শেষ ঘরটা (🟢 ফাঁকা, মোছা নিরাপদ) দেখুন — ওইগুলো মুছলে
--    কোনো কাজের তথ্য হারায় না। TK "হ্যাঁ" বললে তবেই মোছার SQL লেখা হবে।
-- =====================================================================
