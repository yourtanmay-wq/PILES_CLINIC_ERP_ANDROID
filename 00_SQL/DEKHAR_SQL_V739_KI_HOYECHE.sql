-- ============================================================================
-- 🔎 শুধু দেখার SQL — V739 চালানোয় কী হয়েছে, নিজের চোখে দেখুন
-- TK-এর প্রশ্ন (২৭.০৮.২০২৬): *"Sql যে চালানো হয়েছে তার জন্য কোন অসুবিধা
--   হবে না তো? ভালো করে যাচাই করে তারপরে বলবেন।"*
--
-- ⛔ এটা কিচ্ছু বদলায় না · কিচ্ছু মোছে না — শুধু পড়ে দেখায়।
--    Supabase → SQL Editor → পেস্ট করে Run।
-- ============================================================================

-- ১) V739 কী কী নতুন টেবিল বানিয়েছে, আর তাতে কত সারি আছে
select 'hr.staff_leave'          as jinis,
       (select count(*) from hr.staff_leave)          as koyta_sari
union all
select 'hr.branch_chamber_days',
       (select count(*) from hr.branch_chamber_days);
-- আশা করা ফল: staff_leave = 0 (ফাঁকা, কেউ ব্যবহার করছে না)
--              branch_chamber_days = 5 (পাঁচটা ব্রাঞ্চের চেম্বার-ডেট)

-- ২) পুরনো ছুটির ব্যবস্থা (wn.leave_requests) অক্ষত আছে তো
select count(*) as puronoo_chutir_sari,
       count(*) filter (where status = 'confirmed') as monjur,
       count(*) filter (where status = 'pending')   as opekkhay
  from wn.leave_requests;

-- ৩) চেম্বারের ডেট ঠিক বসেছে কিনা (রবি=0 · সোম=1 … শনি=6)
select branch, weekdays from hr.branch_chamber_days order by branch;
-- আশা করা ফল: Birpara {3,0} · Cooch Behar {1,5} · Falakata {2,4,6}
--              Jalpaiguri {6,2} · Kishanganj {3,6}

-- ৪) V739 কি পুরনো কোনো টেবিলে হাত দিয়েছে — hr স্কিমার সব টেবিল ও তাদের বয়স
select table_name from information_schema.tables
 where table_schema = 'hr' order by table_name;
-- staff_leave ও branch_chamber_days ছাড়া বাকি সব আগের মতোই থাকার কথা
