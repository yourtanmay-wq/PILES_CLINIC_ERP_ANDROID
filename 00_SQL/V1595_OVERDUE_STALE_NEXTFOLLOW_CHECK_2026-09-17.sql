-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- TK-র রিপোর্ট: স্টাফ কল করেও/রিমার্ক লিখেও কার্ড চিরকাল Overdue থেকে যাচ্ছে,
-- NEXT CALL, LAST CALL-এর চেয়ে পুরনো দেখাচ্ছে (স্ক্রিনশটের ৩ জন —
-- UNKNOWN/Jubeda Khatoon/Neha Parvin)। এই কোয়েরি সার্ভারে আসল সংরক্ষিত
-- মান দেখাবে — যদি সার্ভারেই nextFollow পুরনো থাকে সেটা এক সমস্যা (কোডের
-- নিয়ম কোনো একটা পথে এখনো কাজ করছে না), আর সার্ভার ঠিক থাকলেও ফোনে পুরনো
-- দেখালে সেটা সম্পূর্ণ আলাদা সমস্যা (ফোনের জমানো পুরনো কপি)।
select "name", mobile, stage, status, "lastRemark", "nextFollow", "updatedAt", "noMoreCalls",
       jsonb_array_length(coalesce(history, '[]'::jsonb)) as history_count,
       (select (h->>'date') from jsonb_array_elements(coalesce(history,'[]'::jsonb)) h
         order by (h->>'date') desc, (h->>'time') desc limit 1) as latest_history_date,
       (select (h->>'remark') from jsonb_array_elements(coalesce(history,'[]'::jsonb)) h
         order by (h->>'date') desc, (h->>'time') desc limit 1) as latest_history_remark
from public.followups
where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) in
  ('9339534082','8158867364','8328704622')
order by mobile;
