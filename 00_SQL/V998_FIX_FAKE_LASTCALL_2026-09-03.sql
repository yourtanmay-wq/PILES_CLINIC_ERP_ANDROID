-- V998 (০৩.০৯.২০২৬) — মিথ্যা "LAST CALL" তারিখ মোছা
-- TK-রিপোর্ট: Susmita Das · MD RAJ — "NEXT CALL, LAST CALL-এর আগের তারিখ"।
-- মাপা: ৫০টা সারিতে lastCallDate সারির নিজের updatedAt-এর চেয়েও নতুন, অথচ
-- history-তে ওই দিনের কল একটাও লেখা নেই (৩০টায় কল-গোনা ০)। সবগুলোই
-- ০১.০৯.২০২৬-এর আগের; তারপর থেকে একটাও নতুন হয়নি।
-- কাজ: ওই মিথ্যা তারিখটা ফাঁকা করা। অ্যাপ তখন history-র আসল শেষ কলের
-- তারিখই দেখাবে (FollowUpModel.kt:149 — যাচাই করা)।
-- ⛔ nextFollow ছোঁয়া হয় না: ওই রোগীদের সত্যিই কল বাকি, তাই তালিকা
--    থেকে সরানো যাবে না।

-- ধাপ ১ — আগে দেখে নিন কোন কোন সারি বদলাবে
select "name","mobile","lastCallDate","nextFollow","callCount","updatedAt"
from public.followups
where "lastCallDate" is not null and "lastCallDate" <> ''
  and "updatedAt" is not null and "updatedAt" <> ''
  and "lastCallDate" > left("updatedAt",10)
  and not exists (
    select 1 from jsonb_array_elements(coalesce("history",'[]'::jsonb)) h
    where h->>'date' = "lastCallDate")
order by "lastCallDate" desc;

-- ধাপ ২ — ঠিক ওই সারিগুলোই সারানো
update public.followups
set "lastCallDate" = '',
    "updatedAt" = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where "lastCallDate" is not null and "lastCallDate" <> ''
  and "updatedAt" is not null and "updatedAt" <> ''
  and "lastCallDate" > left("updatedAt",10)
  and not exists (
    select 1 from jsonb_array_elements(coalesce("history",'[]'::jsonb)) h
    where h->>'date' = "lastCallDate");
