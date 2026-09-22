-- V1411 (১২.০৯.২০২৬ রাত, TK-নির্দেশ "JH MANDAL কোন টাকা পাবে না, সম্পূর্ণ কমিশন
-- তাকে দিয়ে দেয়া হয়েছে", তালিকা ৫১৫) — JH MANDAL (Cooch Behar)-র রোগী
-- GULGAR HOSSAIN-এর কমিশন এখানেই স্থির ₹2,205.50 (যা দেওয়া হয়েছে);
-- আর বাকি দেখাবে না। ⛔ শুধু এই এক সারি; অন্য কিছু বদলায় না।
begin;

with target as (
  select c.id, c.patient_row_id, c.rmp_id, c.patient_name, c.capped_amount as old_cap
    from fin.rmp_patient_commissions c
    join public.doctor_visits d on d.id = c.rmp_id
   where upper(trim(coalesce(d.name,''))) = 'JH MANDAL'
     and trim(coalesce(d.branch,'')) = 'Cooch Behar'
     and right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10) = '6002075357'
     and upper(trim(coalesce(c.patient_name,''))) = 'GULGAR HOSSAIN'
), upd as (
  update fin.rmp_patient_commissions c
     set capped_amount = 2205.50, capped_by = 'TK-SQL-V1411', capped_at = now(), updated_at = now()
    from target t where c.id = t.id
  returning c.id, c.patient_row_id, c.rmp_id, c.patient_name, c.capped_amount
), aud as (
  insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
  select 'CAP_PATIENT_COMMISSION', u.patient_row_id,
         jsonb_build_object('capped_amount', t.old_cap),
         jsonb_build_object('capped_amount', u.capped_amount, 'rmp_id', u.rmp_id),
         'No further commission — fixed by Master (TK, SQL V1411)', 'TK-SQL-V1411'
    from upd u join target t on t.id = u.id
  returning 1
)
select u.patient_name, u.capped_amount, (select count(*) from aud) as audit_rows from upd u;

commit;
