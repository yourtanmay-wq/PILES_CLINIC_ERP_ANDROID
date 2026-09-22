-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1504-ভয়েস-প্রশ্ন (১৫.০৯.২০২৬, TK-নির্দেশ: "ভবিষ্যতে যেন এরকম সমস্যা
-- না হয়, তার পাহারা ব্যবস্থা রাখতে হবে") — "ডুপ্লিকেট পেমেন্ট আছে কিনা"
-- জিজ্ঞাসা করলেই এখন সব ব্রাঞ্চে চেক করা যাবে, TK নিজেই যেকোনো সময়।
--
-- একই ধরন-শনাক্তকরণ যা আজ ৫ জন রোগীর আসল ডুপ্লিকেট ধরতে ব্যবহার ও যাচাই
-- হয়েছে (V1504_DUPLICATE_PAYMENT_PREVIEW_V3): একই রোগী (patientId দিয়ে,
-- patientCode নয় — সেটা ফাঁকা থাকতে পারে) + একই দিন + একই পেমেন্ট-লেবেল +
-- হুবহু একই অঙ্ক + একই ধরন (CASH/ONLINE), সবগুলো একে অপরের ১৫ মিনিটের
-- মধ্যে তৈরি হলে তবেই "ডুপ্লিকেট" — ভিন্ন অঙ্কের সত্যিকারের একাধিক কিস্তি
-- এখানে ধরা পড়ে না (নিশ্চিত না হয়ে কাউকে সন্দেহভাজন দেখানো হয় না)।
--
-- ⛔ শুধু পড়া/রিপোর্ট — এই ফাংশন নিজে কিছু মোছে না, কাউকে সতর্কও করে না
--    (নোটিফিকেশন নেই) — TK/স্টাফ নিজে জিজ্ঞাসা করলে তবেই উত্তর দেখায়,
--    ঠিক অন্য সব ভয়েস-প্রশ্নের মতোই।
-- ⛔ payType='treatment'-এই সীমাবদ্ধ (এটাই যে পথে V1504-এর আসল ফাঁকটা ছিল)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

create or replace function reports.duplicate_payments_summary(p_branch text)
returns table(total int, patient_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with clean as (
      select p.* from public.payments p
       where p."payType" = 'treatment'
         and lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
         and p."amount" ~ '^[0-9]+(\.[0-9]+)?$'
         and p."amount"::numeric > 0
         and p."createdAt" ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'
         and coalesce(p."patientId",'') <> ''
    ),
    grp as (
      select c."patientId" as pid, c."date" as d, c."payLabel" as lbl, c."amount" as amt, c."mode" as md,
             count(*) as cnt, min(c."createdAt") as gmin, max(c."createdAt") as gmax
        from clean c
       group by c."patientId", c."date", c."payLabel", c."amount", c."mode"
      having count(*) >= 2
    ),
    dup as (
      select * from grp
       where extract(epoch from (gmax::timestamptz - gmin::timestamptz)) / 60.0 <= 15
    )
    select coalesce(sum(cnt - 1), 0)::int, coalesce(count(distinct pid), 0)::int from dup;
end $$;
revoke all on function reports.duplicate_payments_summary(text) from public, anon;
grant execute on function reports.duplicate_payments_summary(text) to authenticated;

create or replace function reports.duplicate_payments_list(p_branch text)
returns table(
  patient_row_id text, patient_code text, name text, mobile text,
  pay_date text, pay_label text, amount numeric, mode text,
  extra_count int, extra_amount numeric, branch text
)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with clean as (
      select p.* from public.payments p
       where p."payType" = 'treatment'
         and lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
         and p."amount" ~ '^[0-9]+(\.[0-9]+)?$'
         and p."amount"::numeric > 0
         and p."createdAt" ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'
         and coalesce(p."patientId",'') <> ''
    ),
    grp as (
      select c."patientId" as pid, c."date" as d, c."payLabel" as lbl, c."amount" as amt, c."mode" as md,
             count(*) as cnt, min(c."createdAt") as gmin, max(c."createdAt") as gmax,
             min(c."name") as nm, min(c."mobile") as mob, min(c."patientCode") as pcode, min(c."branch") as br
        from clean c
       group by c."patientId", c."date", c."payLabel", c."amount", c."mode"
      having count(*) >= 2
    ),
    dup as (
      select * from grp
       where extract(epoch from (gmax::timestamptz - gmin::timestamptz)) / 60.0 <= 15
    )
    select pid, coalesce(pcode,''), coalesce(nm,''), coalesce(mob,''), d, lbl, amt::numeric, md,
           (cnt - 1)::int, (amt::numeric * (cnt - 1)), coalesce(br,'')
      from dup
     order by gmax desc
     limit 500;
end $$;
revoke all on function reports.duplicate_payments_list(text) from public, anon;
grant execute on function reports.duplicate_payments_list(text) to authenticated;

notify pgrst, 'reload schema';

commit;
