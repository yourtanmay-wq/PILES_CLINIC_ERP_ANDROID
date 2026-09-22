-- মাপার SQL (কিছু বদলায় না): কম্পিউটারের অ্যাপ প্রতিটা টেবিলের সব সারি ব্রাউজারে জমা রাখে (ছবি বাদে)।
-- ব্রাউজারের সীমা ≈ 5 MB। এতে দেখা যাবে এখন কত MB, কোন টেবিল সবচেয়ে বড়।
with s as (
  select 'patients' as tebil, count(*) as sari, sum(octet_length((to_jsonb(t) - 'photo' - 'patientPhoto')::text)) as byte from patients t
  union all select 'payments',      count(*), sum(octet_length((to_jsonb(t) - 'photo' - 'patientPhoto')::text)) from payments t
  union all select 'followups',     count(*), sum(octet_length((to_jsonb(t) - 'photo' - 'patientPhoto')::text)) from followups t
  union all select 'enquiries',     count(*), sum(octet_length((to_jsonb(t) - 'photo' - 'patientPhoto')::text)) from enquiries t
  union all select 'medical',       count(*), sum(octet_length((to_jsonb(t) - 'photo' - 'patientPhoto')::text)) from medical t
  union all select 'doctor_visits', count(*), sum(octet_length((to_jsonb(t) - 'photo' - 'patientPhoto')::text)) from doctor_visits t
  union all select 'products',      count(*), sum(octet_length(to_jsonb(t)::text)) from products t
  union all select 'briefings',     count(*), sum(octet_length(to_jsonb(t)::text)) from briefings t
  union all select 'address_tags',  count(*), sum(octet_length(to_jsonb(t)::text)) from address_tags t
)
select tebil, sari, round(coalesce(byte,0)/1048576.0, 2) as mb from s
union all
select 'MOT (সব মিলিয়ে)', sum(sari), round(sum(coalesce(byte,0))/1048576.0, 2) from s
order by mb desc;
