-- ════════════════════════════════════════════════════════════════════════
-- 👀 শুধু-দেখার SQL — DATE: 21/08/2026  TIME: 3.52Pm
--
-- TK-এর প্রশ্ন: "আমার মোবাইলে 56 Patient, বর্ণালীর মোবাইলে 48 — দুই রকম কেন?"
-- (Enquiry 51 বনাম 46 · Visit 53 বনাম 47 · Patient 56 বনাম 48)
--
-- ⛔⛔ এই ফাইলে **একটাও পরিবর্তন নেই** — সবগুলোই `select`।
--    কিছু বসবে না, মুছবে না, বদলাবে না। শুধু গুনে দেখাবে।
--
-- ─── ট্যাবের নাম কীভাবে মেলাবেন ─────────────────────────────────────────
--    ডেটাবেসে "Inquiry"    → পর্দায় **Enquiry** ট্যাব
--    ডেটাবেসে "Patient"    → পর্দায় **Visit**   ট্যাব
--    ডেটাবেসে "Treatment"  → পর্দায় **Patient** ট্যাব
--    (এটা অ্যাপের পুরনো নাম — FollowUpActivity লাইন ২৪২–২৪৪)
-- ════════════════════════════════════════════════════════════════════════


-- ══════════════════════════════════════════════════════════════════
-- প্রশ্ন ১ · কোন ট্যাবে কোন ব্রাঞ্চের কতগুলো?
--
-- এটাই মূল উত্তর। দেখুন —
--   • JALPAIGURI-র সংখ্যা যদি 46 / 47 / 48 হয় এবং অন্য ব্রাঞ্চের সারিও
--     থাকে → আপনার 🏥 বোতামে **"All"** বাছা আছে। কোনো সমস্যাই নেই।
--   • JALPAIGURI-র সংখ্যা যদি 51 / 53 / 56 হয় → তাহলে **"(ব্রাঞ্চ ফাঁকা)"**
--     বা ভুল-বানান ব্রাঞ্চের সারিগুলোই বর্ণালীর কাছে লুকিয়ে যাচ্ছে।
-- ══════════════════════════════════════════════════════════════════
select
  case coalesce(trim("stage"),'')
    when 'Inquiry'   then '1. Enquiry ট্যাব'
    when 'Patient'   then '2. Visit ট্যাব'
    when 'Treatment' then '3. Patient ট্যাব'
    else '(অন্য: '||coalesce(trim("stage"),'ফাঁকা')||')'
  end                                                        as "ট্যাব",
  coalesce(nullif(trim("branch"),''),'⚠️ (ব্রাঞ্চ ফাঁকা)')   as "ব্রাঞ্চ",
  count(*)                                                   as "কতগুলো"
from public.followups
where coalesce(trim("stage"),'') in ('Inquiry','Patient','Treatment')
  and coalesce(trim("status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
group by 1, 2
order by 1, 3 desc;


-- ══════════════════════════════════════════════════════════════════
-- প্রশ্ন ২ · ব্রাঞ্চের নাম ফাঁকা — এমন সারি ঠিক কতগুলো?
--
-- এই সারিগুলোই **মাস্টার দেখেন, স্টাফ দেখেন না**।
-- (কোডে: FollowUpRepository লাইন ১৩৪৬ — `allBranch || branchAllows(...)`)
-- সংখ্যাটা 0 হলে কারণ ২ বাদ, অর্থাৎ কারণ ১-ই ঠিক।
-- ══════════════════════════════════════════════════════════════════
select
  case coalesce(trim("stage"),'')
    when 'Inquiry'   then '1. Enquiry ট্যাব'
    when 'Patient'   then '2. Visit ট্যাব'
    when 'Treatment' then '3. Patient ট্যাব'
    else '(অন্য)'
  end                                          as "ট্যাব",
  count(*)                                     as "ব্রাঞ্চ-ফাঁকা সারি"
from public.followups
where coalesce(trim("stage"),'') in ('Inquiry','Patient','Treatment')
  and coalesce(trim("status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
  and coalesce(trim("branch"),'') = ''
group by 1
order by 1;


-- ══════════════════════════════════════════════════════════════════
-- প্রশ্ন ৩ · ব্রাঞ্চের বানান কি সব জায়গায় এক?
--
-- "Jalpaiguri" · "JALPAIGURI" · " Jalpaiguri " — চোখে এক, কিন্তু
-- ফাঁকা-জায়গা বা বানান আলাদা হলে স্টাফের ছাঁকনিতে আটকে যেতে পারে।
-- এখানে হুবহু যা লেখা আছে তাই দেখানো হচ্ছে (দুই পাশে | চিহ্ন দিয়ে)।
-- ══════════════════════════════════════════════════════════════════
select
  '|'||coalesce("branch",'(null)')||'|'   as "ব্রাঞ্চে হুবহু যা লেখা",
  count(*)                                as "কতগুলো"
from public.followups
where coalesce(trim("stage"),'') in ('Inquiry','Patient','Treatment')
  and coalesce(trim("status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
group by 1
order by 2 desc;


-- ══════════════════════════════════════════════════════════════════
-- প্রশ্ন ৪ · ব্রাঞ্চ ফাঁকা, কিন্তু রোগী আসলে কোন ব্রাঞ্চের?
--
-- ফাঁকা-ব্রাঞ্চ সারিগুলোর আসল ব্রাঞ্চ `patients` টেবিল থেকে মিলিয়ে
-- দেখানো হচ্ছে (মোবাইলের শেষ ১০ অঙ্ক ধরে)। এতে বোঝা যাবে ঠিক কতগুলো
-- জলপাইগুড়ির রোগী বর্ণালীর তালিকা থেকে হারিয়ে যাচ্ছে।
-- ⛔ কিছু লেখা হচ্ছে না — শুধু মিলিয়ে দেখানো।
-- ══════════════════════════════════════════════════════════════════
select
  coalesce(nullif(trim(p."branch"),''),'⚠️ patients-এও ফাঁকা')  as "আসল ব্রাঞ্চ",
  count(*)                                                      as "কতগুলো"
from public.followups f
left join public.patients p
  on right(regexp_replace(coalesce(p."mobile",''),'\D','','g'),10)
   = right(regexp_replace(coalesce(f."mobile",''),'\D','','g'),10)
where coalesce(trim(f."stage"),'') in ('Inquiry','Patient','Treatment')
  and coalesce(trim(f."status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
  and coalesce(trim(f."branch"),'') = ''
group by 1
order by 2 desc;
