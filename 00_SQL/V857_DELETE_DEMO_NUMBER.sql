-- ═══════════════════════════════════════════════════════════════════════════
-- 🗑️🔒 V857 (৩০.০৮.২০২৬) — একটা নম্বরের সব চিহ্ন চিরতরে মুছে ফেলা
--
-- TK-নির্দেশ: *"এটা কিশানগঞ্জ ক্লিনিকের নম্বর, ডেমো হিসাবে কোনো এক সময়
-- এটা করা হয়েছিল, কিন্তু এখনো রয়ে গেছে — এটা ডিলিট কেন করতে পারছি না
-- চিরতরে"* · *"যখন ডেমো তাহলে সেটা টাকা দিয়ে আমি কী করব"*
--
-- ─── কেন অ্যাপ থেকে মোছা যায় না (কোড ধরে যাচাই করা) ────────────────────
-- এক নম্বরের তথ্য **অনেকগুলো আলাদা টেবিলে** ছড়ানো থাকে। অ্যাপের Delete
-- একটা রেকর্ড সরায় ও tombstone বসায়, কিন্তু বাকি টেবিলের সারিগুলো থেকে
-- যায়। Patient Timeline নম্বর ধরে **সব টেবিল খুঁজে** পর্দা জোড়া লাগায়
-- (`PatientTimelineRepository.byMobile()`), তাই কার্ডটা ফিরে আসে।
--
-- ⛔⛔ সাবধান — এটা **চিরতরে মোছে, ফেরানো যায় না**।
--     · শুধু নিচের একটাই নম্বরে চলে (ধাপ ০-তে বসানো)।
--     · চালানোর **আগে ধাপ ১ (ব্যাকআপ) অবশ্যই চালাবেন** — কিছু ভুল হলে
--       ওখান থেকেই ফেরানো যাবে।
--     · এটা ডেমো নম্বর বলেই টাকার সারিও মোছা হচ্ছে (TK-এর স্পষ্ট নির্দেশ)।
--       ⇒ ওই মাসের কালেকশন/রিপোর্ট থেকে ওই টাকা চলে যাবে — এটাই চাওয়া।
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── ধাপ ০ — কোন নম্বর (শেষ ১০ অঙ্ক) ──────────────────────────────────────
-- ⚠️ শুধু এই লাইনটাই বদলাতে হয়।
create temporary table if not exists _target(m text);
delete from _target;
insert into _target values ('8676002200');   -- কিশানগঞ্জ ক্লিনিকের নিজস্ব নম্বর

-- ─── ধাপ ১ — ব্যাকআপ (আগে এটাই চালান) ────────────────────────────────────
-- মোছার আগে ওই নম্বরের প্রতিটা সারি হুবহু এখানে জমা থাকে।
create table if not exists public.v857_demo_backup (
  "id"        bigserial primary key,
  "tableName" text,
  "row"       jsonb,
  "savedAt"   timestamptz default now()
);

insert into public.v857_demo_backup("tableName","row")
select 'enquiries',    to_jsonb(t) from public.enquiries    t, _target g where t."mobile"    like '%'||g.m;
insert into public.v857_demo_backup("tableName","row")
select 'followups',    to_jsonb(t) from public.followups    t, _target g where t."mobile"    like '%'||g.m;
insert into public.v857_demo_backup("tableName","row")
select 'patients',     to_jsonb(t) from public.patients     t, _target g where t."mobile"    like '%'||g.m or t."altMobile" like '%'||g.m;
insert into public.v857_demo_backup("tableName","row")
select 'payments',     to_jsonb(t) from public.payments     t, _target g where t."mobile"    like '%'||g.m;
insert into public.v857_demo_backup("tableName","row")
select 'medical',      to_jsonb(t) from public.medical      t, _target g where t."mobile"    like '%'||g.m;
insert into public.v857_demo_backup("tableName","row")
select 'products',     to_jsonb(t) from public.products     t, _target g where t."mobile"    like '%'||g.m;
insert into public.v857_demo_backup("tableName","row")
select 'doctor_visits',to_jsonb(t) from public.doctor_visits t, _target g where t."mobile"   like '%'||g.m;

-- 🔎 রোগীর আইডি ধরে বাঁধা সারিগুলোও (mobile ঘর ফাঁকা থাকলেও ধরা পড়ে)
create temporary table if not exists _pids(pid text);
delete from _pids;
insert into _pids
select t."id" from public.patients t, _target g where t."mobile" like '%'||g.m or t."altMobile" like '%'||g.m
union
select t."patientId" from public.patients t, _target g where (t."mobile" like '%'||g.m or t."altMobile" like '%'||g.m) and coalesce(t."patientId",'') <> '';

insert into public.v857_demo_backup("tableName","row")
select 'payments_by_pid', to_jsonb(t) from public.payments t where t."patientId" in (select pid from _pids);
insert into public.v857_demo_backup("tableName","row")
select 'medical_by_pid',  to_jsonb(t) from public.medical  t where t."patientId" in (select pid from _pids);

-- ⚠️ এখানে থামুন। উপরের ব্যাকআপ টেবিলে সারিগুলো এসেছে কিনা দেখে নিন:
--     select "tableName", count(*) from public.v857_demo_backup group by 1;
-- সব ঠিক থাকলে তবেই নিচের ধাপ ২ চালান।

-- ─── ধাপ ২ — চিরতরে মোছা ─────────────────────────────────────────────────
delete from public.payments      t using _target g where t."mobile" like '%'||g.m;
delete from public.payments      t where t."patientId" in (select pid from _pids);
delete from public.medical       t using _target g where t."mobile" like '%'||g.m;
delete from public.medical       t where t."patientId" in (select pid from _pids);
delete from public.products      t using _target g where t."mobile" like '%'||g.m;
delete from public.doctor_visits t using _target g where t."mobile" like '%'||g.m;
delete from public.followups     t using _target g where t."mobile" like '%'||g.m;
delete from public.enquiries     t using _target g where t."mobile" like '%'||g.m;
delete from public.patients      t using _target g where t."mobile" like '%'||g.m or t."altMobile" like '%'||g.m;

-- call_remarks · dialer_calls · trash · deleted_records — থাকলে সেগুলোও
do $$
begin
  if to_regclass('public.call_remarks') is not null then
    execute 'delete from public.call_remarks t using _target g where t."mobile" like ''%''||g.m';
  end if;
  if to_regclass('public.dialer_calls') is not null then
    execute 'delete from public.dialer_calls t using _target g where t."dialedNumber" like ''%''||g.m';
  end if;
  if to_regclass('public.trash') is not null then
    execute 'delete from public.trash t using _target g where t."record"::text like ''%''||g.m||''%''';
  end if;
  if to_regclass('public.deleted_records') is not null then
    execute 'delete from public.deleted_records t using _target g where t."rowId" like ''%''||g.m||''%''';
  end if;
end $$;

-- ─── ধাপ ৩ — মিলিয়ে দেখা (সব শূন্য আসা চাই) ──────────────────────────────
select 'enquiries' t, count(*) n from public.enquiries    x, _target g where x."mobile" like '%'||g.m
union all select 'followups',    count(*) from public.followups    x, _target g where x."mobile" like '%'||g.m
union all select 'patients',     count(*) from public.patients     x, _target g where x."mobile" like '%'||g.m
union all select 'payments',     count(*) from public.payments     x, _target g where x."mobile" like '%'||g.m
union all select 'medical',      count(*) from public.medical      x, _target g where x."mobile" like '%'||g.m
union all select 'products',     count(*) from public.products     x, _target g where x."mobile" like '%'||g.m
union all select 'doctor_visits',count(*) from public.doctor_visits x, _target g where x."mobile" like '%'||g.m;

notify pgrst, 'reload schema';

-- ─── ধাপ ৪ — ফোনে ─────────────────────────────────────────────────────────
-- প্রতিটা ফোনে অ্যাপ একবার বন্ধ করে আবার খুলুন (ফোনে জমানো পুরনো কপি
-- সরে যাবে)। তবু দেখা গেলে: Settings → Apps → TK Biswas Piles Clinic →
-- Storage usage → Clear cache।
