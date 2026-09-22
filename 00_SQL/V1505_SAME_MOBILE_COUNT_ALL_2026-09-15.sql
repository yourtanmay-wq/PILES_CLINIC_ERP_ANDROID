-- শুধু SELECT — কিছু মোছে না।
-- একই মোবাইল নম্বরে কতজন রোগী আছে (পেমেন্ট থাকুক বা না থাকুক), সব ব্রাঞ্চ মিলিয়ে।
with dup_mobile as (
  select right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) as m
  from public.patients
  where length(right(regexp_replace(coalesce(mobile,''),'\D','','g'),10)) = 10
  group by m having count(*) > 1
)
-- ⚠️ V1505 সংশোধন (১৫.০৯.২০২৬) — আগের সংস্করণে ছাঁকা কলামের নাম "mobile"
-- রাখা হয়েছিল, যেটা patients টেবিলের আসল "mobile" ঘরের নামের সাথে গুলিয়ে
-- গিয়ে PostgreSQL ভুল করে কাঁচা (raw) নম্বর ধরে দলে ভাগ করছিল — তাই
-- "9641062348" আর "+919641062348" (আসলে একই নম্বর) দুটো আলাদা সারি হয়ে
-- যাচ্ছিল। এখন "norm_mobile" নামে বদলানো হলো, যাতে গুলিয়ে না যায়।
select right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10) as norm_mobile,
       count(*) as total_patients,
       string_agg(p.branch || ': ' || p.name || ' (' || p."patientId" || ')', '  |  '
                  order by p."registrationDate") as patients
  from public.patients p
  join dup_mobile dm
    on right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10) = dm.m
 group by norm_mobile
 order by total_patients desc, norm_mobile;
