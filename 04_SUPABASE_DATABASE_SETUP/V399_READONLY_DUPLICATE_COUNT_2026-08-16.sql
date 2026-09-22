-- =====================================================================
-- V399 (16.08.2026) — **শুধু পড়া · কিচ্ছু বদলায় না · কিছুই মোছে না**
--
-- TK-এর নির্দেশ: "আগে গুনে দেখান, তারপর সিদ্ধান্ত"।
--
-- কী গোনা হচ্ছে: একই রোগীর জন্য **একাধিক Follow-up (Visit) সারি**।
-- কারণ: `PatientModel.buildVisitFollowUpRow` প্রতিবার নতুন আইডি বসাত, তাই
-- একই রোগীকে দ্বিতীয়বার সেভ করলে ক্লাউডে আরেকটা সারি ঢুকে যেত।
-- (V399-এ কোডে ঠিক করা হয়েছে — নতুন করে আর বাড়বে না। এই ফাইলটা শুধু
--  দেখায় আগে কতগুলো জমেছে।)
--
-- চালানোর নিয়ম: Supabase → SQL Editor → পুরোটা পেস্ট করে Run।
-- ⛔ একটাও insert / update / delete নেই — শুধু select।
-- =====================================================================

-- ১) মোট কত রোগীর একাধিক Visit-সারি আছে, আর মোট কত বাড়তি সারি
select 'সারাংশ' as part,
       count(*)                                as রোগী_সংখ্যা,
       coalesce(sum(cnt - 1), 0)               as বাড়তি_সারি
  from (
    select "refId", count(*) cnt
      from public.followups
     where coalesce("refId",'') <> '' and coalesce(stage,'') = 'Patient'
     group by "refId"
    having count(*) > 1
  ) d;

-- ২) কোন রোগীর কতগুলো (সবচেয়ে বেশি আগে), নাম ও মোবাইল সহ — সর্বোচ্চ ৫০টি
select "refId"                                  as রোগীর_সারি_আইডি,
       max(name)                                as নাম,
       max(mobile)                              as মোবাইল,
       max(branch)                              as ব্রাঞ্চ,
       count(*)                                 as কতগুলো_সারি,
       min(coalesce("createdAt",''))            as প্রথম,
       max(coalesce("createdAt",''))            as শেষ
  from public.followups
 where coalesce("refId",'') <> '' and coalesce(stage,'') = 'Patient'
 group by "refId"
having count(*) > 1
 order by count(*) desc, max(name)
 limit 50;

-- ৩) একই মোবাইল + একই stage-এ একাধিক সারি (refId ফাঁকা থাকলেও ধরা পড়বে)
select right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as মোবাইল_১০,
       coalesce(stage,'')                        as ধাপ,
       count(*)                                  as কতগুলো_সারি,
       max(name)                                 as নাম
  from public.followups
 where length(right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10)) = 10
 group by 1,2
having count(*) > 1
 order by count(*) desc
 limit 50;

-- =====================================================================
-- ফল কীভাবে পড়বেন:
--   অংশ ১ — মোট কত রোগী ও মোট কত বাড়তি সারি (এক নজরে ছবিটা)
--   অংশ ২ — কোন রোগীর কতগুলো (নাম-মোবাইল সহ, যাচাই করার জন্য)
--   অংশ ৩ — মোবাইল ধরে গোনা (refId ফাঁকা থাকলেও ধরা পড়ে)
--
-- ⛔ কিছুই মোছা হয়নি। সংখ্যা দেখে TK সিদ্ধান্ত নেবেন — তারপরই পরিষ্কার করার
--    কথা ভাবা হবে, এবং সেটাও আলাদা করে দেখিয়ে অনুমতি নিয়ে।
-- =====================================================================
