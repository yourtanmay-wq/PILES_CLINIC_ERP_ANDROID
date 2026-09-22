-- ✅ TK-অনুমোদিত (০১.০৯.২০২৬) — যে দুজন সত্যিই অফিশিয়াল সময়ে এনকোয়ারি করেছিলেন
--    কিন্তু ভুলে "Unexpected Time" হয়ে আছেন, তাঁদের Official করা।
--    · MUSTAFA HUSSAIN · JPE-01092026-005 · এনকোয়ারি ২৯.০৮ সকাল ৯.২০
--    · SUMITI ROY      · JPE-29082026-005 · এনকোয়ারি ২৮.০৮ সকাল ১০.৫১
--    দুজনেরই এক্সট্রা **দেওয়া ₹০**, শুধু "বাকি" ₹১০০ করে — সেটা নিজে থেকেই উঠে যাবে।
--
-- ⛔ কোনো টাকার সারি এখানে মোছা হয় না। মাস্টার অ্যাপে কোনো স্টাফের Salary পর্দা
--    খুললেই `incentive_sync` চলে, আর সেটা **শুধু "বাকি" সারি** তোলে — দেওয়া টাকা
--    কখনো ছোঁয় না।
-- ⚠️ `updatedAt` ফোনের চলতি রীতিতেই (ভারতীয় সময় + 'Z') লেখা হলো, যাতে ফোনের
--    পুরনো কোনো কপি এই সংশোধনকে "পুরনো" ভেবে ফিরিয়ে দিতে না পারে।

update public.patients
   set "timeType" = 'Official Time',
       "updatedAt" = to_char(now() at time zone 'Asia/Kolkata', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
 where "patientId" in ('JPE-01092026-005', 'JPE-29082026-005')
   and coalesce("timeType",'') = 'Unexpected Time';

-- যাচাই (শুধু পড়া):
select "patientId", name, "timeType"
  from public.patients
 where "patientId" in ('JPE-01092026-005', 'JPE-29082026-005');
