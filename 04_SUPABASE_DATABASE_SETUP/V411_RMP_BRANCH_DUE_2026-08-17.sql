-- ============================================================================
-- V411 — RMP Due List-এর জন্য: এক ব্রাঞ্চের সব RMP-র পাওনা এক ডাকে
--   TK নিজে চালিয়েছেন ১৭.০৮.২০২৬ · Success
--   ⛔ শুধু পড়ে (stable) — কোনো সারি বদলায় না।
--   ⛔ পাহারা: fin.rmp_can_use() + ব্রাঞ্চ-পরীক্ষা, অন্য RMP-ফাংশনের মতোই।
--   হিসাবের নিয়ম fin.rmp_summary()-এর হুবহু নকল (বিল−ছাড় · চিকিৎসার জমা
--   − অনুমোদিত ফেরত · PERCENT/AMOUNT · দেওয়া বাদ · অ্যাডভান্স বাদ)।
-- ============================================================================

create or replace function fin.rmp_branch_due(p_branch text)
returns table(rmp_id text, rmp_name text, rmp_mobile text, branch text,
              patient_count bigint, earned numeric, paid numeric, due numeric)
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare v_branch text := nullif(trim(coalesce(p_branch,'')),'');
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;

  return query
  with base as (
    select c.id, c.rmp_id, c.rmp_name, c.rmp_mobile, c.treatment_branch, c.patient_row_id,
           c.commission_mode, c.commission_value,
           greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill
    from fin.rmp_patient_commissions c
    left join public.patients p on p.id = c.patient_row_id
    where c.treatment_branch = v_branch
  ),
  paid_t as (
    select b.id,
      coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
         where x."patientId" = b.patient_row_id
           and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
    - coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
         left join public.payments o on o.id = r."refundOfPaymentId"
        where r."patientId" = b.patient_row_id
          and lower(coalesce(r."payType",'')) = 'refund'
          and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
          and (trim(coalesce(r."refundOfPaymentId",'')) = ''
               or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as net_paid
    from base b
  ),
  earn as (
    select b.*, case when b.bill > 0 then
        case when b.commission_mode = 'PERCENT'
          then least(greatest(0, pt.net_paid), b.bill) * b.commission_value / 100
          else b.commission_value * least(greatest(0, pt.net_paid), b.bill) / b.bill end
      else 0 end as earned
    from base b join paid_t pt on pt.id = b.id
  ),
  given as (select x.patient_commission_id, sum(x.amount) g
              from fin.rmp_commission_payments x group by 1),
  per_row as (
    select e.*, coalesce(g.g,0) as given, greatest(e.earned - coalesce(g.g,0), 0) as due
    from earn e left join given g on g.patient_commission_id = e.id
  ),
  per_rmp as (
    select r.rmp_id as rid, max(r.rmp_name) nm, max(r.rmp_mobile) mb, max(r.treatment_branch) br,
           count(*) pts, round(sum(r.earned),2) ea, round(sum(r.given),2) gi, round(sum(r.due),2) du
    from per_row r group by r.rmp_id
  ),
  adv as (select a.rmp_id as aid, coalesce(sum(a.amount - a.allocated_amount),0) un
            from fin.rmp_advance_payments a group by 1)
  select q.rid, q.nm, q.mb, q.br, q.pts, q.ea,
         round(q.gi + coalesce(v.un,0), 2),
         round(greatest(0, q.du - coalesce(v.un,0)), 2)
  from per_rmp q left join adv v on v.aid = q.rid
  where round(greatest(0, q.du - coalesce(v.un,0)), 2) > 0
  order by 8 desc;
end $$;

revoke all on function fin.rmp_branch_due(text) from public, anon;
grant execute on function fin.rmp_branch_due(text) to authenticated;
notify pgrst, 'reload schema';
