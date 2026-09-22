-- V1413 (১৩.০৯.২০২৬, TK-নির্দেশ: "গতকাল অনলাইনে ₹14,000 দেওয়া হয়েছে JAYANTA
-- BANE-কে, কিন্তু কোথাও লেখা ছিল না" — তালিকা ৫১৫) — এই টাকাটা এখন ঠিক জায়গায়
-- বসানো হলো: fin.rmp_advance_payments (RMP-কে সরাসরি দেওয়া টাকা, কোনো একজন
-- রোগীর সাথে জোড়া নয়) + fin.expenses (ব্রাঞ্চের খরচের হিসাবেও দেখা যাবে) +
-- audit-সারি। এটাই Master-এর "RMP Payment" বোতাম যা করে, হুবহু সেই নিয়মে।
-- ⛔ অন্য কোনো সারি ছোঁয়া হয়নি।
begin;

with rmp as (
  select d.id, coalesce(d.name,'') as name, coalesce(d.branch,'') as branch
    from public.doctor_visits d
   where trim(coalesce(d.branch,'')) = 'Jalpaiguri' and upper(trim(coalesce(d.name,''))) = 'JAYANTA BANE'
), exp as (
  insert into fin.expenses(entry_date, branch, category, paid_to, amount, mode, note, ignored, created_by)
  select '2026-09-12', r.branch, 'RMP Commission Advance', r.name, 14000, 'ONLINE', 'Entered later — paid 12.09.2026 (TK, SQL V1413)', false, 'TK-SQL-V1413'
    from rmp r
  returning id, paid_to, amount
), adv as (
  insert into fin.rmp_advance_payments(rmp_id, rmp_name, branch, paid_on, amount, mode, reference_no, expense_id, recorded_by)
  select r.id, r.name, r.branch, '2026-09-12', 14000, 'ONLINE', null, e.id, 'TK-SQL-V1413'
    from rmp r, exp e
  returning id, rmp_id, amount
), aud as (
  insert into fin.rmp_commission_audit(action, entity_id, new_value, changed_by)
  select 'RMP_ADVANCE_PAYMENT', a.id::text, jsonb_build_object('rmp_id', a.rmp_id, 'amount', a.amount, 'paid_on', '2026-09-12'), 'TK-SQL-V1413'
    from adv a
  returning 1
)
select r.name as rmp, a.amount, e.id as expense_id, (select count(*) from aud) as audit_rows
  from rmp r, adv a, exp e;

commit;
