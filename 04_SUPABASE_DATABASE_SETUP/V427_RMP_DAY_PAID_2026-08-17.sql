-- ============================================================================
-- V427 — "আজ কোন RMP-কে কত টাকা সত্যিই দেওয়া হয়েছে" — এক ডাকে
--   TK-নির্দেশ ১৭.০৮.২০২৬: *"ক করুন, আর আলাদা লাইনে 'আজ কত দিলাম'ও রাখুন"*
--
--   ⛔ এই সংখ্যাটা **মোট টাকা থেকে বাদ যায় না** — শুধু জানার জন্য।
--      বাদ যায় শুধু V426-এর *আজকের প্রাপ্য* কমিশন (TK-এর সিদ্ধান্ত "ক")।
--      কারণ RMP-কে প্রায়ই কয়েক দিনের টাকা একসাথে/অ্যাডভান্স দেওয়া হয় — সেটা
--      আজকের মোট থেকে বাদ দিলে দুই দিনের হিসাব মিশে যেত।
--
--   দুই জায়গা থেকেই ধরা হয়:
--     • fin.rmp_commission_payments — রোগীভিত্তিক কমিশন দেওয়া
--     • fin.rmp_advance_payments    — RMP-কে সরাসরি অ্যাডভান্স
--
--   ⛔ শুধু পড়ে (stable) — একটাও সারি লেখা/বদলানো হয় না।
--   ⛔ পাহারা হুবহু অন্য RMP-ফাংশনের মতো: fin.rmp_can_use() + ব্রাঞ্চ-পরীক্ষা।
-- ============================================================================

create or replace function fin.rmp_day_paid(p_branch text, p_date text)
returns table(
  rmp_id          text,
  rmp_name        text,
  commission_paid numeric,   -- আজ রোগীভিত্তিক কমিশন দেওয়া
  advance_paid    numeric,   -- আজ অ্যাডভান্স দেওয়া
  total_paid      numeric    -- দুইয়ের যোগ
)
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
  v_date   date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;
  if coalesce(p_date,'') !~ '^\d{4}-\d{2}-\d{2}$' then raise exception 'Invalid date'; end if;
  v_date := p_date::date;

  return query
  with c as (
    select x.rmp_id as rid, max(x.rmp_name) as nm, sum(x.amount) as amt
    from fin.rmp_commission_payments x
    where x.treatment_branch = v_branch and x.paid_on = v_date
    group by x.rmp_id
  ),
  a as (
    select y.rmp_id as rid, max(y.rmp_name) as nm, sum(y.amount) as amt
    from fin.rmp_advance_payments y
    where y.branch = v_branch and y.paid_on = v_date
    group by y.rmp_id
  ),
  j as (
    select coalesce(c.rid, a.rid) as rid,
           coalesce(c.nm, a.nm)   as nm,
           coalesce(c.amt, 0)     as cpaid,
           coalesce(a.amt, 0)     as apaid
    from c full outer join a on a.rid = c.rid
  )
  select j.rid, coalesce(j.nm,''), round(j.cpaid,2), round(j.apaid,2),
         round(j.cpaid + j.apaid, 2)
  from j
  where (j.cpaid + j.apaid) > 0
  order by 5 desc, 2;
end $$;

revoke all on function fin.rmp_day_paid(text, text) from public, anon;
grant execute on function fin.rmp_day_paid(text, text) to authenticated;
notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া) — আজকের কোচবিহার ─────────────────────────────────
-- select * from fin.rmp_day_paid('Cooch Behar',
--        to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM-DD'));
