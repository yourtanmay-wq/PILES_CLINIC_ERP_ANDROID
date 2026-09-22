-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1428 (১৩.০৯.২০২৬, TK-নির্দেশ "বাকি ভয়েস প্রশ্নগুলোর কাজ শুরু করুন", তালিকা ৫৩৮)
-- ভয়েস-প্রশ্ন, তৃতীয় ধাপ — ২টা নতুন রিপোর্ট-ফাংশন (reports.*), শুধু পড়া, Master-only
-- (reports.can_access_branch), ব্রাঞ্চ-ধরে, সর্বোচ্চ ৫০০ সারি (ফ্রি-প্ল্যান নিরাপদ)।
--
--   ১৭) "গতকাল কোন RMP-কে কত কমিশন দেওয়া হয়েছে" — RMP-কে সত্যিই দেওয়া টাকা:
--       fin.rmp_commission_payments (রোগীর নামে) + fin.rmp_advance_payments (আগাম)।
--       ⛔ এটা RMP Commission Sheet-এর (V1252/V1309) **হুবহু একই দুটো উৎস** — অ্যাপের
--          পর্দার সাথে সংখ্যা মিলবে (নিয়ম ৭ক-২)। হাতে-লেখা পুরনো Paid (তারিখহীন) এখানে নেই,
--          শিটেও নেই। নিয়ম ৭গ: এখানে কোনো নতুন যোগ-বিয়োগের নিয়ম বসানো হয়নি — শুধু সারি পড়া।
--   ৩১) "কতদিন IN টাইম চাপা হয়নি" — wn.notebook_days-এ যেদিন খাতা খোলা হয়েছে (সারি আছে),
--       ছুটি নয়, অথচ check_in ফাঁকা/'null'। OUT-বাদ (V1421 out_missing_list)-এর জোড়া নিয়ম।
--       ⚠️ সৎ সীমা: যেদিন স্টাফ খাতাই খোলেননি (সারি নেই) সেদিন ধরা পড়ে না — সার্ভারে
--          ব্রাঞ্চ-ধরে "কোন দিন কার আসার কথা" রোস্টার নেই।
--
-- ⛔ TK-কে চালাতে হবে: Supabase SQL Editor-এ এই পুরো ফাইল একবার (V1415–V1422-এর পরে)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ১৭) RMP-কে দেওয়া কমিশন (রোগীর নামে + আগাম) ─────────────────────────────
create or replace function reports.rmp_paid_list(p_branch text, p_from date, p_to date)
returns table(payment_id text, rmp_id text, rmp_name text, paid_on text, amount numeric, kind text, patient_name text, mode text)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select x.pid, x.rid, x.rname, x.pon, x.amt, x.knd, x.pname, x.md
      from (
        select cp.id::text as pid, cp.rmp_id as rid, coalesce(cp.rmp_name,'') as rname,
               to_char(cp.paid_on,'YYYY-MM-DD') as pon, cp.amount as amt, 'patient'::text as knd,
               coalesce(pc.patient_name,'') as pname, coalesce(cp.mode,'') as md,
               cp.paid_on as d, cp.recorded_at as r
          from fin.rmp_commission_payments cp
          left join fin.rmp_patient_commissions pc on pc.id = cp.patient_commission_id
         where lower(trim(coalesce(cp.treatment_branch,''))) = lower(trim(p_branch))
           and cp.paid_on >= p_from and cp.paid_on <= p_to
        union all
        select ap.id::text, ap.rmp_id, coalesce(ap.rmp_name,''),
               to_char(ap.paid_on,'YYYY-MM-DD'), ap.amount, 'advance'::text,
               ''::text, coalesce(ap.mode,''), ap.paid_on, ap.recorded_at
          from fin.rmp_advance_payments ap
         where lower(trim(coalesce(ap.branch,''))) = lower(trim(p_branch))
           and ap.paid_on >= p_from and ap.paid_on <= p_to
      ) x
     order by x.d desc, x.r desc
     limit 500;
end $$;
revoke all on function reports.rmp_paid_list(text, date, date) from public, anon;
grant execute on function reports.rmp_paid_list(text, date, date) to authenticated;

create or replace function reports.rmp_paid_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, rmp_count int, payment_count int)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(sum(x.amt),0), count(distinct x.rid)::int, count(*)::int
      from (
        select cp.rmp_id as rid, cp.amount as amt
          from fin.rmp_commission_payments cp
         where lower(trim(coalesce(cp.treatment_branch,''))) = lower(trim(p_branch))
           and cp.paid_on >= p_from and cp.paid_on <= p_to
        union all
        select ap.rmp_id, ap.amount
          from fin.rmp_advance_payments ap
         where lower(trim(coalesce(ap.branch,''))) = lower(trim(p_branch))
           and ap.paid_on >= p_from and ap.paid_on <= p_to
      ) x;
end $$;
revoke all on function reports.rmp_paid_summary(text, date, date) from public, anon;
grant execute on function reports.rmp_paid_summary(text, date, date) to authenticated;

-- ── ৩১) IN টাইম চাপা হয়নি (খাতা খোলা আছে, ছুটি নয়, check_in ফাঁকা) ────────────
create or replace function reports.in_missing_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, staff_name text, work_date text, check_out text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select n.staff_code, coalesce(s.full_name,''), to_char(n.work_date,'YYYY-MM-DD'),
           coalesce(nullif(lower(btrim(coalesce(n.check_out,''))),'null'),'')
      from wn.notebook_days n
      join hr.staff_profiles s on s.person_code = n.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and n.work_date >= p_from and n.work_date <= p_to
       and coalesce(n.is_leave,false) = false
       and coalesce(n.is_wfh,false) = false
       and coalesce(n.is_other_branch,false) = false
       and coalesce(nullif(lower(btrim(coalesce(n.check_in,''))),'null'),'') = ''
     order by n.work_date desc, n.staff_code
     limit 500;
end $$;
revoke all on function reports.in_missing_list(text, date, date) from public, anon;
grant execute on function reports.in_missing_list(text, date, date) to authenticated;

create or replace function reports.in_missing_summary(p_branch text, p_from date, p_to date)
returns table(total int, staff_count int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  return query select count(*)::int, count(distinct l.staff_code)::int from reports.in_missing_list(p_branch, p_from, p_to) l;
end $$;
revoke all on function reports.in_missing_summary(text, date, date) from public, anon;
grant execute on function reports.in_missing_summary(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
