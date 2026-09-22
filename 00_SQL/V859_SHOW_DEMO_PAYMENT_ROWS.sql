-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V859 (৩০.০৮.২০২৬) — ওই নম্বরগুলোর **টাকার সারিগুলো চোখে দেখা**
--    (কিছুই মোছে না — শুধু দেখায়)
--
-- কেন দরকার: V858-এর ফলাফলে দেখা গেল কয়েকজনের **রোগী-রেকর্ড নেই, অথচ
-- পেমেন্ট আছে** (যেমন COB-UTTAMA ৭টা, JPE-RUPAM ৫টা, FIELD-OFFICER ১৩টা)।
-- কোড যাচাই করে দেখা গেছে `payments.mobile`-এ সবসময় **রোগীর** নম্বরই বসে
-- (`patient.mobile` / `row.mobile`) — স্টাফের নম্বর কখনো নয়। তাই এগুলো হয়
-- (ক) সত্যিকারের ডেমো, নয়তো (খ) আগে রোগী মুছে ফেলায় পড়ে থাকা অনাথ সারি।
-- ⛔ টাকা কখনো আন্দাজে মোছা যাবে না — TK নিজে দেখে বলবেন।
-- ═══════════════════════════════════════════════════════════════════════════

create temporary table if not exists _look(m text);
delete from _look;
insert into _look values
  ('6207841890'),  -- KNE-KISHAN5 MOHSINA ANJUM
  ('7321960416'),  -- KNE-KISHAN8 ROHINI KUMARI
  ('7583973566'),  -- RMP CHANDAN DAS
  ('7679751521'),  -- COB-UTTAMA
  ('8001080080'),  -- RMP "Tk Bisaws"
  ('8101397763'),  -- JPE-JALPAI-13 BARNALI ROY
  ('8167096595'),  -- JPE-RUPAM
  ('8210342405'),  -- RMP Dr Angar Alam
  ('8676002200'),  -- Clinic - Kishanganj
  ('9002003540'),  -- FIELD-OFFICER
  ('9647840067'),  -- JPE-CRP
  ('9883605917'),  -- KNE-LAXMI
  ('9883623823');  -- FLK-1 RINA BARMAN

-- ─── প্রতিটা টাকার সারি, তারিখ অনুসারে ───────────────────────────────────
select l.m::text                                as "নম্বর",
       coalesce(y."date"::text,'')              as "তারিখ",
       coalesce(y."name"::text,'')              as "নাম",
       coalesce(y."branch"::text,'')            as "ব্রাঞ্চ",
       coalesce(y."amount"::text,'')            as "টাকা",
       coalesce(y."payType"::text,'')           as "ধরন",
       coalesce(y."mode"::text,'')              as "মোড",
       coalesce(y."receivedBy"::text,'')        as "কে নিল",
       coalesce(y."patientId"::text,'')         as "রোগীর আইডি",
       coalesce(y."createdAt"::text,'')         as "কখন লেখা"
  from _look l join public.payments y on y."mobile" like '%'||l.m
 order by 1, 2;

-- ─── ওই নম্বরে রোগীর সারি আদৌ আছে কি না ──────────────────────────────────
select l.m::text as "নম্বর",
       coalesce(p."name"::text,'(রোগীর সারি নেই)') as "রোগীর নাম",
       coalesce(p."patientId"::text,'')            as "রোগীর আইডি",
       coalesce(p."branch"::text,'')               as "ব্রাঞ্চ",
       coalesce(p."bill"::text,'')                 as "বিল"
  from _look l left join public.patients p
    on p."mobile" like '%'||l.m or p."altMobile" like '%'||l.m
 order by 1;
