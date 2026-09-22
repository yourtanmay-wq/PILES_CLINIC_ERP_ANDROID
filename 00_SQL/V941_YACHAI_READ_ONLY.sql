-- পড়া-মাত্র যাচাই — কিছুই বদলায় না। চালানোর পরে প্রতিটা রোগীর জন্য
-- পুরনো হার · নতুন হার · কবে থেকে · আগের জমা · পরের জমা · এখনকার কমিশন দেখাবে।
select c.patient_name, c.patient_code, c.treatment_branch,
       c.prev_value as "পুরনো %", c.commission_value as "নতুন %", c.rate_changed_on as "কবে থেকে",
       fin.rmp_net_paid_between(c.patient_row_id, null, c.rate_changed_on - 1) as "আগের জমা",
       fin.rmp_net_paid_between(c.patient_row_id, c.rate_changed_on, null)     as "পরের জমা",
       (select earned from fin.rmp_summary(c.patient_row_id))                  as "মোট কমিশন"
from fin.rmp_patient_commissions c
join public.doctor_visits d on d.id = c.rmp_id
where c.treatment_branch = 'Cooch Behar'
  and (replace(coalesce(d.mobile,''),' ','') like '%7479173399%'
    or replace(coalesce(d.mobile,''),' ','') like '%8001080080%')
order by c.patient_name;
