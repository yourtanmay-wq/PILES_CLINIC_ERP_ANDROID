-- ═══════════════════════════════════════════════════════════════════════════
-- V1361 (১১.০৯.২০২৬) — TK-নির্দেশ (পুরো-প্রজেক্ট দেরি-অডিট, তালিকা ৪৬২-গ,
-- TK-বাছাই "ক"): Today's Collection/Chamber Board/Income-Expense প্রতিবার
-- খুললে/সেভ করলে ব্রাঞ্চের **পুরো** followups (৫০০০ পর্যন্ত) + patients
-- (৫০০০ পর্যন্ত) নামাত — শুধু "কার টাকা বাতিল হিসেবে লুকাতে হবে" জানতে।
--
-- এই ফাংশন `RefundedRecords.kt`-এর `fetch()`-এর **হুবহু একই নিয়ম** (B110 +
-- B621) সার্ভারে করে দেয়, ফেরত শুধু মোবাইল নম্বরের ছোট তালিকা।
-- ⛔ শুধু **পড়া** — কোনো টেবিল/সারি/টাকা বদলায় না।
-- ⛔ নিয়ম এক অক্ষরও বদলায়নি, শুধু হিসাবটা কোথায় হচ্ছে সেটা বদলাল।
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ═══════════════════════════════════════════════════════════════════════════

create or replace function public.tk_refunded_mobiles(p_branch text default null)
returns table(mobile text)
language sql
stable
as $$
  with f as (
    select right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as m,
           coalesce(nullif(trim(coalesce(status,'')),''),'Active') as st,
           coalesce(stage,'') as stg
    from public.followups
    where p_branch is null or p_branch = '' or p_branch = 'All' or branch = p_branch
  ),
  agg as (
    -- B110: এই মোবাইলের ফিল্টার-করা সব সারিই কি Cancelled?
    -- B621: এর মধ্যে কোনো Cancelled সারি Patient/Treatment/Visit-স্টেজের (আসল রেজিস্ট্রেশন বাতিল)?
    select m,
           bool_and(st = 'Cancelled') as all_cancelled,
           bool_or(st = 'Cancelled' and stg in ('Patient','Treatment','Visit')) as cancelled_registration
    from f
    where length(m) = 10
    group by m
  ),
  base as (
    select m from agg where all_cancelled
  ),
  registered as (
    -- B621: এই মোবাইলের আসল রেজিস্টার্ড রোগী (patients টেবিলে) আছে কিনা।
    select distinct right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as m
    from public.patients
    where (p_branch is null or p_branch = '' or p_branch = 'All' or branch = p_branch)
      and length(right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10)) = 10
  )
  select b.m
  from base b
  left join registered r on r.m = b.m
  left join agg a on a.m = b.m
  where r.m is null or a.cancelled_registration
$$;

revoke all on function public.tk_refunded_mobiles(text) from public;
grant execute on function public.tk_refunded_mobiles(text) to anon, authenticated;

create index if not exists followups_branch_mobile_idx on public.followups(branch, mobile);
create index if not exists patients_branch_mobile_idx  on public.patients(branch, mobile);

notify pgrst, 'reload schema';
