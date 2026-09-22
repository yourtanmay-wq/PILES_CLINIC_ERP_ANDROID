-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- V1588-এর ফলাফলে AMIT GOLDAR-এর Referred SQL-এ ৫৭ এসেছে, অথচ অ্যাপের পর্দায় ৫৬।
-- সন্দেহ: রোগী দেখানোর সার্ভার-ফাংশন (fin.rmp_legacy_view_all_v2) শুধু "সবচেয়ে
-- নতুন ৫০০০ রোগী" থেকে খোঁজে (V1067-তে অন্য একটা পুরনো পথে এই একই ধরনের
-- সমস্যা ধরা পড়েছিল ও ঠিক হয়েছিল — এটা কি সেই একই দোষের আরেকটা জায়গা?)।
-- এই কোয়েরি ক্লিনিকের মোট রোগীর সংখ্যা আর AMIT GOLDAR-এর সাথে মিল খাওয়া
-- রোগীদের মধ্যে ক'জন ওই "নতুন ৫০০০"-এর বাইরে পড়ে গেছেন তা বের করবে।
with total as (
  select count(*) as total_patients from public.patients
), rmp as (
  select id, "name", "mobile" from public.doctor_visits
  where right(regexp_replace(coalesce("mobile",''),'\D','','g'),10) = '9046366596'
  limit 1
), matched as (
  select p.id, p."updatedAt"
  from public.patients p, rmp r
  where lower(trim(coalesce(p."refBy",'')))     = lower(trim(r."name"))
     or lower(trim(coalesce(p."refDoctor",'')))  = lower(trim(r."name"))
     or right(regexp_replace(coalesce(p."refDoctorMobile",''),'\D','','g'),10)
        = right(regexp_replace(coalesce(r."mobile",''),'\D','','g'),10)
), ranked as (
  select m.id, m."updatedAt",
         row_number() over (order by p2."updatedAt" desc nulls last) as rn
  from public.patients p2
  left join matched m on m.id = p2.id
), cap_check as (
  select count(*) as matched_inside_top5000
  from ranked where id is not null and rn <= 5000
)
select
  (select total_patients from total)                    as total_patients_in_db,
  (select count(*) from matched)                         as amit_matched_patients_total,
  (select matched_inside_top5000 from cap_check)         as matched_inside_newest_5000,
  (select count(*) from matched) - (select matched_inside_top5000 from cap_check) as matched_missing_due_to_5000_cap;
