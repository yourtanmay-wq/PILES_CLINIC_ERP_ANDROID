-- ═══════════════════════════════════════════════════════════════════════════
-- 🗑️🔒 V865 (৩০.০৮.২০২৬) — **৩৫টা ভুয়া / ডেমো নম্বর** চিরতরে মোছা
--
-- TK-এর কথা: *"সব ডেমো"* (V864-এর পুরো তালিকা দেখে)।
-- ⛔ TK নিজে বলেছেন এই ৬টা **আসল** ⇒ এই তালিকায় নেই, ছোঁয়াও হবে না:
--    917477462764 (SANKAR NATTA) · 918944998496 (PURNIMA DAS) ·
--    917001772002 · 917047804847 · 918388853135 · 918822024042
--
-- ⛔⛔ মোছা **ফেরানো যায় না**। ধাপ ১-এর ব্যাকআপ আগে বসে, তারপর মোছা।
-- ⛔ **ছোঁয়া হবে না**: `doctor_visits` (RMP ডাক্তার) ও `hr.staff_profiles`।
-- ⛔ মেলানো হয় **শেষ ১০ অঙ্ক** ধরে — অ্যাপ নিজেও ঠিক এভাবেই নম্বর মেলায়।
-- ═══════════════════════════════════════════════════════════════════════════

create temporary table if not exists _demo(m text);
delete from _demo;
insert into _demo values
  ('9876543210'),   -- (নাম নেই) — ধারাবাহিক অঙ্ক  (919876543210)
  ('9153915300'),   -- TEST DEMO  (9153915300)
  ('9085236147'),   -- Abcd  (919085236147)
  ('9365298651'),   -- TEST  (919365298651)
  ('9563322399'),   -- TEST HERO  (919563322399)
  ('9653208129'),   -- TEST  (919653208129)
  ('2222222222'),   -- DEMO  (2222222222)
  ('5555555555'),   -- JOY  (5555555555)
  ('1111111111'),   -- DEMO  (911111111111)
  ('1111222233'),   -- TEST  (911111222233)
  ('1211211222'),   -- DEMO  (911211211222)
  ('1234537890'),   -- LALU  (911234537890)
  ('1472580369'),   -- DEMO  (911472580369)
  ('2225555555'),   -- হ্যাঁ  (912225555555)
  ('2580369147'),   -- Ramu  (912580369147)
  ('3333333332'),   -- (নাম নেই)  (913333333332)
  ('3333333333'),   -- (নাম নেই)  (913333333333)
  ('3596856989'),   -- SURAJ  (913596856989)
  ('5656565656'),   -- BON  (915656565656)
  ('6666666666'),   -- AZ  (916666666666)
  ('9999999999'),   -- RAKESH BALA  (919999999999)
  ('7777755555'),   -- TEST  (917777755555)
  ('8080808080'),   -- TEST ENQUIRY  (918080808080)
  ('8282828282'),   -- (নাম নেই)  (918282828282)
  ('8282828288'),   -- TRY  (918282828288)
  ('8686868686'),   -- BARNA  (918686868686)
  ('8989898989'),   -- আজ  (918989898989)
  ('9969999999'),   -- GHJBHJ  (919969999999)
  ('9999988888'),   -- TEST ZONE  (919999988888)
  ('8882238882'),   -- DEMO BULTI  (918882238882)
  ('8989898956'),   -- FACK TEST  (918989898956)
  ('7872272742'),   -- JHINUK BISWAS  (7872272742)
  ('7237237235'),   -- DINA  (917237237235)
  ('7979927897'),   -- AJIT KUMAR  (917979927897)
  ('9197711722');   -- Boni Roy  (919197711722)

-- ─── ধাপ ১ — ব্যাকআপ ─────────────────────────────────────────────────────
create table if not exists public.v865_demo_backup (
  "id" bigserial primary key, "tableName" text, "row" jsonb,
  "savedAt" timestamptz default now()
);
insert into public.v865_demo_backup("tableName","row")
select 'enquiries', to_jsonb(t) from public.enquiries t, _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
insert into public.v865_demo_backup("tableName","row")
select 'followups', to_jsonb(t) from public.followups t, _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
insert into public.v865_demo_backup("tableName","row")
select 'patients', to_jsonb(t) from public.patients t, _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m
    or right(regexp_replace(coalesce(t."altMobile",''),'[^0-9]','','g'),10) = g.m;
insert into public.v865_demo_backup("tableName","row")
select 'payments', to_jsonb(t) from public.payments t, _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
insert into public.v865_demo_backup("tableName","row")
select 'medical', to_jsonb(t) from public.medical t, _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
insert into public.v865_demo_backup("tableName","row")
select 'products', to_jsonb(t) from public.products t, _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;

-- ─── ধাপ ২ — চিরতরে মোছা ─────────────────────────────────────────────────
delete from public.payments  t using _demo g where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
delete from public.medical   t using _demo g where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
delete from public.products  t using _demo g where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
delete from public.followups t using _demo g where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
delete from public.enquiries t using _demo g where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m;
delete from public.patients  t using _demo g
 where right(regexp_replace(coalesce(t."mobile",''),'[^0-9]','','g'),10) = g.m
    or right(regexp_replace(coalesce(t."altMobile",''),'[^0-9]','','g'),10) = g.m;

do $$
begin
  if to_regclass('public.call_remarks') is not null then
    execute 'delete from public.call_remarks t using _demo g where right(regexp_replace(coalesce(t."mobile",''''),''[^0-9]'','''',''g''),10) = g.m';
  end if;
  if to_regclass('public.dialer_calls') is not null then
    execute 'delete from public.dialer_calls t using _demo g where right(regexp_replace(coalesce(t."dialedNumber",''''),''[^0-9]'','''',''g''),10) = g.m';
  end if;
end $$;

notify pgrst, 'reload schema';

-- ─── ধাপ ৩ — মিলিয়ে দেখা (সব ঘরে ০ আসা চাই) ─────────────────────────────
select count(*) as "এখনো বাকি (০ হওয়া চাই)"
  from _demo g
 where exists (select 1 from public.enquiries e where right(regexp_replace(coalesce(e."mobile",''),'[^0-9]','','g'),10)=g.m)
    or exists (select 1 from public.followups f where right(regexp_replace(coalesce(f."mobile",''),'[^0-9]','','g'),10)=g.m)
    or exists (select 1 from public.patients  p where right(regexp_replace(coalesce(p."mobile",''),'[^0-9]','','g'),10)=g.m)
    or exists (select 1 from public.payments  y where right(regexp_replace(coalesce(y."mobile",''),'[^0-9]','','g'),10)=g.m);

-- ─── ধাপ ৪ — প্রতিটা ফোনে অ্যাপ একবার বন্ধ করে খুলুন ────────────────────
