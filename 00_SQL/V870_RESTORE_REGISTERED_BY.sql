-- ═══════════════════════════════════════════════════════════════════════════
-- 🧑‍💼🔒 V870 (৩০.০৮.২০২৬) — আসল রেজিস্ট্রারের নাম ফিরিয়ে আনা
-- TK-রিপোর্ট: RAJA MANDAL কার্ডে JPE-CRP-এর নাম মুছে TK BISWAS হয়ে গিয়েছিল।
-- আসল নামটা ভিজিট-ফি-র সারিতে (`payments.receivedBy`) এখনো জমা আছে —
-- ওই সারি কখনো আবার লেখা হয় না, তাই ওটাই সত্যি।
-- ⛔ শুধু `patients.registeredBy` বদলায়। টাকা · তারিখ · নাম — কিছুই নয়।
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── ধাপ ১ — আগে শুধু দেখুন (কিছুই বদলায় না) ────────────────────────────
select p."patientId" as "রোগীর কোড", p."name" as "নাম", p."branch" as "ব্রাঞ্চ",
       p."registrationDate" as "তারিখ",
       p."registeredBy" as "এখন যার নাম আছে",
       y."receivedBy"    as "আসল যিনি করেছিলেন"
  from public.patients p
  join lateral (
      select t."receivedBy"
        from public.payments t
       where t."patientId" = p."id" and t."payType" = 'visit_fee'
         and coalesce(t."receivedBy", '') <> ''
       order by t."createdAt" asc nulls last
       limit 1
  ) y on true
 where coalesce(p."registeredBy", p."createdBy", '') <> y."receivedBy"
 order by p."registrationDate" desc;

-- ─── ধাপ ২ — সারানো (ব্যাকআপ আগে বসে, তারপর বদল) ───────────────────────
create temporary table if not exists _fix(pid text, now_by text, real_by text);
delete from _fix;
insert into _fix
select p."id", coalesce(p."registeredBy", p."createdBy", ''), y."receivedBy"
  from public.patients p
  join lateral (
      select t."receivedBy"
        from public.payments t
       where t."patientId" = p."id" and t."payType" = 'visit_fee'
         and coalesce(t."receivedBy", '') <> ''
       order by t."createdAt" asc nulls last
       limit 1
  ) y on true
 where coalesce(p."registeredBy", p."createdBy", '') <> y."receivedBy";

create table if not exists public.v870_regby_backup (
  "id" bigserial primary key, "patientRowId" text,
  "before" text, "after" text, "savedAt" timestamptz default now()
);
insert into public.v870_regby_backup("patientRowId","before","after")
select pid, now_by, real_by from _fix;

update public.patients p
   set "registeredBy" = f.real_by
  from _fix f
 where f.pid = p."id";

notify pgrst, 'reload schema';

-- ─── ধাপ ৩ — মিলিয়ে দেখা (০ আসা চাই) ───────────────────────────────────
select count(*) as "এখনো বেঠিক (০ হওয়া চাই)"
  from public.patients p
  join lateral (
      select t."receivedBy"
        from public.payments t
       where t."patientId" = p."id" and t."payType" = 'visit_fee'
         and coalesce(t."receivedBy", '') <> ''
       order by t."createdAt" asc nulls last
       limit 1
  ) y on true
 where coalesce(p."registeredBy", '') <> y."receivedBy";
