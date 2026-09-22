-- ═══════════════════════════════════════════════════════════════════════
-- V1005 — এক রেকর্ডে দুটো followups সারি, আর ভুল সময় — সব একসাথে
-- ০৩.০৯.২০২৬ · TK-এর নির্দেশে · পুরো স্ক্রিপ্টটা একবারে চালানো যায়
--
-- ⛔ কিছু মোছার আগে প্রতিটা শর্ত মিলিয়ে দেখা হয় — কোনো তথ্য হারাবে না।
-- ⛔ শেষে একটাই রিপোর্ট-টেবিল দেখাবে: আগে কত ছিল, কত সারানো হলো।
-- ═══════════════════════════════════════════════════════════════════════

begin;

create temporary table _rk_report(step text, n bigint) on commit drop;

-- ── ০) আগে কত সমস্যা আছে, গুনে রাখা ────────────────────────────────────
insert into _rk_report
select 'আগে: এক refId-তে একাধিক সারি', count(*) from (
  select f."refId", f.stage
  from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
  group by f."refId", f.stage having count(*) > 1
) d;

insert into _rk_report
select 'আগে: history-র সময় অন্য দিনের', count(*)
from public.followups f,
     lateral jsonb_array_elements(f.history::jsonb) h
where jsonb_typeof(f.history::jsonb) = 'array'
  and coalesce(h->>'time','') <> '' and coalesce(h->>'date','') <> ''
  and left(h->>'time',10) <> left(h->>'date',10);

-- ── ১) ভুল সময় ঠিক করা ────────────────────────────────────────────────
-- শুধু তখনই ছোঁয়া হয় যখন সারির নিজের `createdAt` ওই এন্ট্রির তারিখের সাথে
-- মেলে — অর্থাৎ আসল সময়টা কী ছিল তার প্রমাণ হাতে আছে। নইলে ছোঁয়া হয় না।
with fixed as (
  select f.id,
         jsonb_agg(
           case
             when coalesce(h.e->>'time','') <> ''
              and coalesce(h.e->>'date','') <> ''
              and left(h.e->>'time',10) <> left(h.e->>'date',10)
              and left(coalesce(f."createdAt",''),10) = left(h.e->>'date',10)
             then jsonb_set(h.e, '{time}', to_jsonb(f."createdAt"))
             else h.e
           end
           order by h.ord
         ) as newhist
  from public.followups f,
       lateral jsonb_array_elements(f.history::jsonb) with ordinality as h(e, ord)
  where jsonb_typeof(f.history::jsonb) = 'array'
  group by f.id
),
touched as (
  update public.followups f
     set history = x.newhist,
         "updatedAt" = to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    from fixed x
   where f.id = x.id and f.history::jsonb is distinct from x.newhist
  returning f.id
)
insert into _rk_report select 'সারানো: সময় ঠিক করা সারি', count(*) from touched;

-- ── ২) ভুতুড়ে দ্বিতীয় সারি মোছা ────────────────────────────────────────
-- রাখা হয়: যার history সবচেয়ে বড়; সমান হলে যেটা আগে তৈরি হয়েছে।
-- মোছা হয় **শুধু তখনই**, যখন সবকটা শর্ত মেলে:
--   · history-তে ১টার বেশি এন্ট্রি নেই
--   · callCount ১-এর বেশি নয়
--   · রেজিস্ট্রেশনে রূপান্তরিত নয় (convertedPatientId ফাঁকা)
--   · status এখনো 'Active' (Reject/Closed-এর সিদ্ধান্ত এতে নেই)
--   · আর ওর প্রতিটা লেখা রাখা-সারিতেও হুবহু আছে ⇒ কিছুই হারায় না
with ranked as (
  select f.id, f."refId", f.stage, f.history, f.status, f."callCount",
         f."convertedPatientId",
         row_number() over (
           partition by f."refId", f.stage
           order by case when jsonb_typeof(f.history::jsonb) = 'array'
                         then jsonb_array_length(f.history::jsonb) else 0 end desc,
                    coalesce(f."createdAt",'') asc, f.id asc) as rn
  from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
),
keeper as (select * from ranked where rn = 1),
ghost as (
  select g.*, k.id as keep_id, k.history as keep_history
  from ranked g join keeper k on k."refId" = g."refId" and k.stage = g.stage
  where g.rn > 1
    and case when jsonb_typeof(g.history::jsonb) = 'array'
             then jsonb_array_length(g.history::jsonb) else 0 end <= 1
    -- ⚠️ `callCount` ঘরটা লেখা (text) হিসেবে রাখা আছে — সংখ্যা নয়।
    --    তাই শুধু অঙ্কগুলো নিয়ে তবেই তুলনা (ফাঁকা/অক্ষর থাকলেও ভাঙে না)।
    and coalesce(nullif(regexp_replace(coalesce(g."callCount"::text,''), '\D', '', 'g'), ''), '0')::bigint <= 1
    and coalesce(g."convertedPatientId",'') = ''
    and coalesce(g.status,'Active') = 'Active'
),
safe_ghost as (
  select * from ghost g
  where not exists (
    select 1
    from jsonb_array_elements(g.history::jsonb) ge
    where coalesce(ge->>'remark','') <> ''
      and not exists (
        select 1 from jsonb_array_elements(g.keep_history::jsonb) ke
        where coalesce(ke->>'remark','') = coalesce(ge->>'remark','')
      )
  )
),
removed as (
  delete from public.followups f
   using safe_ghost s
   where f.id = s.id
  returning f.id
)
insert into _rk_report select 'সারানো: ভুতুড়ে সারি মোছা', count(*) from removed;

-- বাকি যেগুলো নিরাপদে মোছা যায়নি (সিদ্ধান্ত/কল/টাকা আছে) — শুধু গোনা
insert into _rk_report
select 'হাতে দেখতে হবে: বাকি ডুপ্লিকেট', count(*) from (
  select f."refId", f.stage
  from public.followups f
  where f.stage in ('Inquiry','Patient') and coalesce(f."refId",'') <> ''
  group by f."refId", f.stage having count(*) > 1
) d;

-- ── ৩) ভবিষ্যতের পাহারা ────────────────────────────────────────────────
create table if not exists public.rk_blocked_dup_followup (
  id bigserial primary key,
  tried_id text,
  ref_id   text,
  stage    text,
  mobile   text,
  kept_id  text,
  at       timestamptz not null default now()
);

create or replace function public._rk_guard_dup_followup()
returns trigger
language plpgsql
as $fn$
declare k text;
begin
  -- ⛔ পুরনো সারিরই আপডেট (একই id) হলে কিছুই আটকানো হয় না
  if exists (select 1 from public.followups where id = new.id) then
    return new;
  end if;
  if new.stage in ('Inquiry','Patient') and coalesce(new."refId", '') <> '' then
    select f.id into k
      from public.followups f
     where f."refId" = new."refId" and f.stage = new.stage
     limit 1;
    if k is not null then
      begin
        insert into public.rk_blocked_dup_followup(tried_id, ref_id, stage, mobile, kept_id)
        values (new.id, new."refId", new.stage, new.mobile, k);
      exception when others then null;
      end;
      return null;   -- দ্বিতীয় সারিটা আর ঢোকে না
    end if;
  end if;
  return new;
end
$fn$;

drop trigger if exists rk_guard_dup_followup on public.followups;
create trigger rk_guard_dup_followup
before insert on public.followups
for each row execute function public._rk_guard_dup_followup();

insert into _rk_report values ('পাহারা বসানো (refId + stage = একটাই সারি)', 1);

select * from _rk_report;

commit;
