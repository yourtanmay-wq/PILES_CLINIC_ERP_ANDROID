-- V1407 (১২.০৯.২০২৬ রাত, TK-নির্দেশ "হ্যাঁ, বন্ধ করে দিন", তালিকা ৫১২) —
-- PKB (Cooch Behar)-র রোগী FRANCIS SOREN-এর কমিশন এখানেই বন্ধ: এখন পর্যন্ত যা
-- দেওয়া ধরা (₹5,496) সেটাই স্থির; আর বাড়বে না, বাকি দেখাবে না।
-- (অ্যাপের Master-বোতাম fin.rmp_cap_patient-ই এই কাজটা করে; SQL Editor-এ লগইন-পরিচয়
--  থাকে না বলে এখানে সরাসরি একই দুটো লেখা — কমিশন-সারিতে cap + audit-সারি।)
-- ⛔ শুধু এই এক রোগীর সারি; অন্য কিছু বদলায় না।
begin;

with target as (
  select c.id, c.patient_row_id, c.rmp_id, c.patient_name, c.capped_amount as old_cap
    from fin.rmp_patient_commissions c
    join public.doctor_visits d on d.id = c.rmp_id
   where upper(trim(coalesce(d.name,''))) = 'PKB'
     and trim(coalesce(d.branch,'')) = 'Cooch Behar'
     and right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10) = '7384105795'
     and upper(trim(coalesce(c.patient_name,''))) = 'FRANCIS SOREN'
), upd as (
  update fin.rmp_patient_commissions c
     set capped_amount = 5496, capped_by = 'TK-SQL-V1407', capped_at = now(), updated_at = now()
    from target t where c.id = t.id
  returning c.id, c.patient_row_id, c.rmp_id, c.patient_name, c.capped_amount
), aud as (
  insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
  select 'CAP_PATIENT_COMMISSION', u.patient_row_id,
         jsonb_build_object('capped_amount', t.old_cap),
         jsonb_build_object('capped_amount', u.capped_amount, 'rmp_id', u.rmp_id),
         'No further commission — fixed by Master (TK, SQL V1407)', 'TK-SQL-V1407'
    from upd u join target t on t.id = u.id
  returning 1
)
select u.patient_name, u.capped_amount, (select count(*) from aud) as audit_rows from upd u;

commit;
