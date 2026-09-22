-- শুধু দেখা (তালিকা ৪২২): ছবির ৩ জনের রোগী-সারি হুবহু কী আছে (তারিখের ঘরগুলোর ছাঁচসহ)
select "patientId", name, stage, coalesce(queue::text,'') as queue, coalesce("doctorComplete"::text,'') as dc,
       coalesce("queuedAt",'') as "queuedAt", coalesce("visitDate",'') as "visitDate", coalesce("registrationDate",'') as "registrationDate",
       coalesce("createdAt",'') as "createdAt", coalesce("updatedAt",'') as "updatedAt", coalesce("nextVisitPlan"::text,'') as "nextVisitPlan"
from public.patients
where "patientId" in ('KNE-02062026-001','KNE-24072026-006','KNE-18082026-003')
order by "patientId";
