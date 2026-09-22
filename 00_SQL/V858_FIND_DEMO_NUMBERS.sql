-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V858 (৩০.০৮.২০২৬) — ডেমো নম্বর খুঁজে বার করা (কিছুই মোছে না)
--
-- TK-নির্দেশ: *"এই ধরনের ডেমো নাম্বার আরও কী কী আছে ভালো করে দেখুন…
-- সমস্ত প্রজেক্টে যে সমস্ত ডাক্তারের নাম্বার এবং স্টাফের নাম্বার আছে,
-- এগুলো যদি এনকোয়ারি ভিজিট বা পেশেন্ট হয়ে থাকে সেক্ষেত্রেই নাম্বারগুলো ডেমো"*
--
-- ⛔ এই ফাইলটা **একটাও সারি মোছে না / বদলায় না** — শুধু তালিকা দেখায়।
--    দেখে TK ঠিক করবেন কোনগুলো সত্যিই ডেমো, তারপর V857 দিয়ে মোছা হবে।
-- ⚠️ সৎ সতর্কতা: কোনো স্টাফ বা ডাক্তার **সত্যিই রোগী** হতে পারেন। তাই
--    তালিকাটা চোখে দেখে বাছাই করা ছাড়া কিছু মোছা যাবে না।
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── ১. যেসব নম্বর "ক্লিনিকের নিজের / স্টাফের / ডাক্তারের" ────────────────
create temporary table if not exists _own(m text, kind text);
delete from _own;

-- ক) পাঁচটা ব্রাঞ্চের নিজস্ব নম্বর + সর্বজনীন হেল্পলাইন (কোডে লেখা — BranchCatalog)
insert into _own values
  ('8676002200','Clinic — Kishanganj'),
  ('8436002200','Clinic — Jalpaiguri'),
  ('8514002200','Clinic — Cooch Behar'),
  ('8514001100','Clinic — Falakata'),
  ('8538002200','Clinic — Birpara'),
  ('9429690640','Clinic — Helpline');

-- খ) সব স্টাফ / ডাক্তার / ফিল্ড-এর লগইন নম্বর (লাইভ তালিকা থেকে)
do $$
begin
  if to_regclass('hr.staff_profiles') is not null then
    execute $q$
      insert into _own(m, kind)
      select regexp_replace(coalesce(link_mobile,''),'[^0-9]','','g'),
             'Staff/Doctor — ' || coalesce(person_code,'') || ' ' || coalesce(full_name,'')
        from hr.staff_profiles
       where length(regexp_replace(coalesce(link_mobile,''),'[^0-9]','','g')) = 10
    $q$;
  end if;
end $$;

-- গ) RMP / রেফারিং ডাক্তারদের নম্বর
do $$
begin
  if to_regclass('public.doctor_visits') is not null then
    execute $q$
      insert into _own(m, kind)
      select distinct right(regexp_replace(coalesce("mobile",''),'[^0-9]','','g'),10),
             'RMP / Doctor — ' || coalesce("name",'')
        from public.doctor_visits
       where length(regexp_replace(coalesce("mobile",''),'[^0-9]','','g')) >= 10
    $q$;
  end if;
end $$;

-- ─── ২. এই নম্বরগুলোর কোনটা এনকোয়ারি / ভিজিট / রোগী হয়ে বসে আছে ─────────
-- ⛔ প্রতিটা ঘর `::text` করে নেওয়া হয় — কোন ঘর text আর কোনটা সংখ্যা,
--    টেবিলভেদে আলাদা হতে পারে (V858-এর প্রথম চেষ্টায় ঠিক এখানেই
--    "COALESCE types text and integer cannot be matched" ভুলটা হয়েছিল)।
select o.kind::text                             as "কার নম্বর",
       o.m::text                                as "নম্বর",
       'enquiries'::text                        as "যেখানে আছে",
       coalesce(e."name"::text,'')              as "নাম",
       coalesce(e."branch"::text,'')            as "ব্রাঞ্চ",
       coalesce(e."date"::text,'')              as "তারিখ",
       ''::text                                 as "টাকা"
  from _own o join public.enquiries e on e."mobile" like '%'||o.m

union all
select o.kind::text, o.m::text, 'followups'::text,
       coalesce(f."name"::text,''), coalesce(f."branch"::text,''),
       coalesce(f."date"::text,''), ''::text
  from _own o join public.followups f on f."mobile" like '%'||o.m

union all
select o.kind::text, o.m::text, 'patients'::text,
       coalesce(p."name"::text,''), coalesce(p."branch"::text,''),
       coalesce(p."registrationDate"::text, p."date"::text, ''),
       coalesce(p."bill"::text,'')
  from _own o join public.patients p on p."mobile" like '%'||o.m or p."altMobile" like '%'||o.m

union all
select o.kind::text, o.m::text, 'payments'::text,
       ''::text, coalesce(y."branch"::text,''),
       coalesce(y."date"::text,''), coalesce(y."amount"::text,'')
  from _own o join public.payments y on y."mobile" like '%'||o.m

order by 1, 2, 3;

-- ─── ৩. এক নজরে — কোন নম্বরে কত সারি ─────────────────────────────────────
select o.m as "নম্বর", max(o.kind) as "কার নম্বর",
       (select count(*) from public.enquiries e where e."mobile" like '%'||o.m) as "এনকোয়ারি",
       (select count(*) from public.followups f where f."mobile" like '%'||o.m) as "ফলোআপ",
       (select count(*) from public.patients  p where p."mobile" like '%'||o.m) as "রোগী",
       (select count(*) from public.payments  y where y."mobile" like '%'||o.m) as "পেমেন্ট"
  from _own o
 group by o.m
having (select count(*) from public.enquiries e where e."mobile" like '%'||o.m) > 0
    or (select count(*) from public.followups f where f."mobile" like '%'||o.m) > 0
    or (select count(*) from public.patients  p where p."mobile" like '%'||o.m) > 0
    or (select count(*) from public.payments  y where y."mobile" like '%'||o.m) > 0
 order by 1;

-- ─── ৪. এরপর ─────────────────────────────────────────────────────────────
-- উপরের তালিকা দেখে যেগুলো সত্যিই ডেমো, সেই নম্বরগুলো
-- `V857_DELETE_DEMO_NUMBER.sql`-এর ধাপ ০-তে বসিয়ে চালান।
-- একাধিক নম্বর একসাথেও দেওয়া যায়, যেমন:
--     insert into _target values ('8676002200'), ('8436002200');
