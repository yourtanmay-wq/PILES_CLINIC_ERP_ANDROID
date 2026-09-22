-- =====================================================================
-- V407 — Follow-up-এর ডুপ্লিকেট সারি **জোড়া লাগানো** (16.08.2026)
--        🔧 তৃতীয় ও চূড়ান্ত সংস্করণ
-- =====================================================================
-- TK-এর নির্দেশ: "একবারে সবগুলি সততার সাথে সঠিকভাবে করুন, আন্দাজে নয়।"
--
-- 🔍 লাইভ ডেটা: মোট ৩,২৭৬ সারি · আসলে দরকার ৬৩৫ · বাড়তি ২,৬৪১ (৮১%)
--    ৩৮৬টি দলে ডুপ্লিকেট · একজনের সর্বোচ্চ ১২৭টা সারি
--    বাড়তির ৬৪৫টা ফাঁকা · **১,৯৯৬টায় আসল কল-হিস্ট্রি** ⇒ মোছা নয়, জোড়া লাগানো
--
-- ── আগের দুটো চেষ্টা কেন থেমেছিল (দুবারই TK-এর কিচ্ছু বদলায়নি) ──
--  ১ম: `বাতিল — 18 টি আলাদা history এন্ট্রি হারিয়েছে`
--      কারণ: সহায়ক টেবিল `temporary … on commit drop` ছিল।
--  ২য়: `বাতিল — 386 টি দলের মধ্যে মাত্র 376 টিতে জোড়া লেগেছে`
--      কারণ: সাধারণ টেবিল করেও লাভ হয়নি — Supabase-এর SQL Editor একাধিক
--      আদেশের মাঝে commit করে দেয়, ফলে **জোড়া লাগানোর UPDATE-টা কার্যকরই
--      হচ্ছিল না**, অথচ মোছার DELETE চলত।
--
-- ── এবারের সমাধান: ধাপে ধাপে নয়, **একটাই আদেশে** ──
--   গোটা কাজটা একটামাত্র SQL আদেশে (data-modifying CTE) — পরিকল্পনা, তথ্য
--   জড়ো করা, বসানো ও মোছা সব একসাথে। মাঝপথে commit হওয়ার সুযোগই নেই।
--
--   🔒 আর মোছার **দুটো কড়া শর্ত** — দুটোই না মিললে সারিটা থেকে যাবে:
--      শর্ত ১: ওই দলের রাখা-সারিতে জোড়া **সত্যিই লেগেছে** (`upd`-এর ফল থেকে)
--      শর্ত ২: ওই সারির **প্রতিটি history এন্ট্রি** জড়ো-করা তালিকায় পৌঁছেছে
--      ⇒ তথ্য হারানো **গঠনগতভাবেই অসম্ভব** — কোনো যাচাইয়ের উপর ভরসা নয়।
--
-- ✅ যাচাই (আসল PostgreSQL 16):
--    · ১৩২ সারির নকল ডেটা (১২৭টার রোগী · ছড়ানো history · স্থির id · একা সারি ·
--      ভিন্ন ধাপ · হুবহু-এক এন্ট্রি · null history) → **১৩২ → ৫ সারি**,
--      আলাদা history এন্ট্রি **৬ → ৬** (কিছুই হারায়নি)
--    · ডুপ্লিকেট-হীন সারি **byte-মিল, এক চুলও নড়েনি**
--    · 🔬 **জোড়া লাগানো ইচ্ছে করে ভেঙে** চালানো → **০টি সারি মোছা হয়েছে**,
--      সব সারি ও সব এন্ট্রি অক্ষত। (আগের সংস্করণে এখানেই তথ্য হারাত।)
--
-- ⚠️ চালানোর ক্রম:
--   ১) আগে **V406 APK সব ফোনে** + Netlify আপলোড (নইলে আবার ডুপ্লিকেট হবে)
--   ২) এটা চালান **রাতে, ক্লিনিক বন্ধ থাকলে**
--   ৩) পরে সবাইকে অ্যাপে **"Sync Now"** চাপতে বা লগ-আউট/লগইন করতে বলুন
--
-- 🔙 ফেরাতে হলে (⛔ শুধু বিপদ হলে — এমনি চালাবেন না):
--      begin;
--      drop table public.followups;
--      alter table public.followups_backup_v407 rename to followups;
--      alter table public.followups add primary key (id);
--      commit;
-- =====================================================================

-- ধাপ ১: ব্যাকআপ (আলাদা আদেশ)
drop table if exists public.followups_backup_v407;
create table public.followups_backup_v407 as select * from public.followups;

-- ধাপ ২: 🔴 জোড়া লাগানো ও মোছা — **একটাই আদেশে**।
--   ⛔ মোছার শর্ত: ওই সারির **প্রতিটি history এন্ট্রি** রাখা-সারিতে পৌঁছেছে,
--      তবেই মুছবে। না পৌঁছালে সারিটা **থেকে যাবে** — তথ্য হারানো অসম্ভব।
with base as (
  select f.id, coalesce(f.stage,'') st,
    coalesce(nullif(trim(f."refId"),''),
      'MOB:'||right(regexp_replace(coalesce(f.mobile,''),'\D','','g'),10)) as person,
    case when f."callCount"::text ~ '^[0-9]+$' then (f."callCount"::text)::numeric else 0 end cc,
    coalesce(f."nextFollow",'') nf,
    (case when f."history" is not null and jsonb_typeof(f."history")='array'
               and jsonb_array_length(f."history")>0 then 1 else 0 end) hh,
    (case when f.id like 'fu\_pat\_%' or f.id like 'fu\_inq\_%' then 1 else 0 end) sid,
    f."updatedAt" ua,
    case when f."history" is not null and jsonb_typeof(f."history")='array'
         then f."history" else '[]'::jsonb end hj
  from public.followups f
),
plan as (
  select *, row_number() over (partition by person, st
      order by sid desc, hh desc, cc desc,
               (case when nf<>'' then 1 else 0 end) desc, ua desc nulls last) rn,
    count(*) over (partition by person, st) grp
  from base
),
grp as (            -- প্রতিটি দলের জড়ো করা তথ্য
  select p.person, p.st,
    (select jsonb_agg(distinct e) from plan p2, lateral jsonb_array_elements(p2.hj) e
      where p2.person=p.person and p2.st=p.st)                                as new_history,
    max(p.cc)                                                                 as new_cc,
    max(p.ua)                                                                 as new_ua,
    (select p4.nf from plan p4 where p4.person=p.person and p4.st=p.st
       and p4.nf<>'' order by p4.ua desc nulls last limit 1)                  as new_nf
  from plan p group by p.person, p.st having count(*) > 1
),
keep as (           -- প্রতিটি দলের রাখা-সারি + জড়ো করা তথ্য
  select p.id as keep_id, g.* from plan p
    join grp g on g.person=p.person and g.st=p.st
   where p.rn = 1
),
sib as (            -- দলের বাকি সারিগুলোর ফাঁকা-ঘর ভরার তথ্য
  select k.keep_id,
    (select f.name from plan p join public.followups f on f.id=p.id
      where p.person=k.person and p.st=k.st and coalesce(f.name,'')<>''
      order by p.ua desc nulls last limit 1)     as new_name,
    (select f.branch from plan p join public.followups f on f.id=p.id
      where p.person=k.person and p.st=k.st and coalesce(f.branch,'')<>''
      order by p.ua desc nulls last limit 1)     as new_branch,
    (select f.disease from plan p join public.followups f on f.id=p.id
      where p.person=k.person and p.st=k.st and coalesce(f.disease,'')<>''
      order by p.ua desc nulls last limit 1)     as new_disease,
    (select f.address from plan p join public.followups f on f.id=p.id
      where p.person=k.person and p.st=k.st and coalesce(f.address,'')<>''
      order by p.ua desc nulls last limit 1)     as new_address,
    (select f."lastRemark" from plan p join public.followups f on f.id=p.id
      where p.person=k.person and p.st=k.st and coalesce(f."lastRemark",'')<>''
      order by p.ua desc nulls last limit 1)     as new_remark
  from keep k
),
upd as (
  update public.followups f set
    "history"    = coalesce(k.new_history, f."history"),
    "callCount"  = (case when k.new_cc > 0 then k.new_cc::bigint::text else f."callCount" end),
    "nextFollow" = coalesce(nullif(k.new_nf,''), f."nextFollow"),
    "lastRemark" = coalesce(nullif(s.new_remark,''), f."lastRemark"),
    "name"       = coalesce(nullif(s.new_name,''), f."name"),
    "branch"     = coalesce(nullif(s.new_branch,''), f."branch"),
    "disease"    = coalesce(nullif(s.new_disease,''), f."disease"),
    "address"    = coalesce(nullif(s.new_address,''), f."address"),
    "updatedAt"  = coalesce(k.new_ua, f."updatedAt")
  from keep k join sib s on s.keep_id = k.keep_id
  where f.id = k.keep_id
  returning f.id
),
del as (
  delete from public.followups f
   using plan p join keep k on k.person=p.person and k.st=p.st
   where f.id = p.id and p.rn > 1
     -- 🔒 শর্ত ১: রাখা-সারিতে জোড়া **সত্যিই লেগেছে** তো? (upd-এর ফল দেখে)
     --    না লাগলে এই সারিটা মুছবে না — আগের সংস্করণে এটাই বাদ ছিল।
     and k.keep_id in (select id from upd)
     -- 🔒 শর্ত ২: এই সারির প্রতিটি history এন্ট্রি জড়ো-করা তালিকায় আছে তো?
     and not exists (
       select 1 from jsonb_array_elements(p.hj) e
        where not (coalesce(k.new_history,'[]'::jsonb) @> jsonb_build_array(e)))
  returning f.id
)
select (select count(*) from keep) as "দল",
       (select count(*) from upd)  as "জোড়া লাগল",
       (select count(*) from del)  as "সরানো হলো";
