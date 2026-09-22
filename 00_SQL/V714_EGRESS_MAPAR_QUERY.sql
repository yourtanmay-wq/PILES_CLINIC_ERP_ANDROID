-- ═══════════════════════════════════════════════════════════════════════
-- V714 (২৬.০৮.২০২৬) — **Egress কোথায় খরচ হচ্ছে, সেটা মেপে দেখার query**
--
-- ⛔ এই query কিছুই বদলায় না · মোছে না · লেখে না। শুধু **পড়ে দেখায়**।
--    যতবার খুশি চালানো যায়, কোনো ঝুঁকি নেই।
--
-- 📍 কোথায় চালাবেন: Supabase → বাঁ পাশের মেনু → **Logs** → **Log Explorer**
--    (SQL Editor-এ নয়! SQL Editor ডেটাবেস পড়ে, এটা **সার্ভারের লগ** পড়ে।)
--    নিচের একটা query কপি করে বাক্সে বসিয়ে **Run**।
--
-- ⚠️ যদি লাল ভুলের বার্তা আসে, ওই বার্তাটার স্ক্রিনশট পাঠাবেন — ঘরের নাম
--    Supabase-এর সংস্করণভেদে একটু আলাদা হতে পারে, তখন ঠিক করে দেব।
-- ═══════════════════════════════════════════════════════════════════════


-- ─────────────────────────────────────────────────────────────────────────
-- QUERY ১ — কোন টেবিল থেকে কত **বাইট** নামছে (বড় থেকে ছোট)
--   → এটাই আসল উত্তর: "৫০০ MB-টা কে খাচ্ছে?"
--   → উপরের ৩-৪টা লাইনই সাধারণত পুরো খরচের ৮০%+
-- ─────────────────────────────────────────────────────────────────────────
select
  request.path                                   as poth,
  count(*)                                       as koto_bar,
  sum(cast(response_headers.content_length as int64))                as mot_bytes,
  round(sum(cast(response_headers.content_length as int64)) / 1048576.0, 1) as mot_MB
from edge_logs
  cross join unnest(metadata) as m
  cross join unnest(m.request) as request
  cross join unnest(m.response) as response
  cross join unnest(response.headers) as response_headers
where request.path like '/rest/v1/%'
group by poth
order by mot_bytes desc
limit 40;


-- ─────────────────────────────────────────────────────────────────────────
-- QUERY ২ — খরচটা **ফোনের অ্যাপের** না **কম্পিউটারের ওয়েবের**?
--   → ওয়েবের supabase-js নিজের পরিচয় পাঠায় (x_client_info),
--     ফোনের অ্যাপ পাঠায় না (ফাঁকা দেখাবে)।
-- ─────────────────────────────────────────────────────────────────────────
select
  ifnull(request_headers.x_client_info, 'FONER APP (khali)')          as ke,
  count(*)                                       as koto_bar,
  round(sum(cast(response_headers.content_length as int64)) / 1048576.0, 1) as mot_MB
from edge_logs
  cross join unnest(metadata) as m
  cross join unnest(m.request) as request
  cross join unnest(request.headers) as request_headers
  cross join unnest(m.response) as response
  cross join unnest(response.headers) as response_headers
where request.path like '/rest/v1/%'
group by ke
order by mot_MB desc
limit 20;


-- ─────────────────────────────────────────────────────────────────────────
-- QUERY ৩ — দিনের কোন **ঘণ্টায়** কত খরচ
--   → চেম্বারের সময়ে না রাতেও? রাতে বেশি হলে বুঝব কোনো টাইমার/ব্যাকআপ
--     নিজে নিজে চলছে।
-- ─────────────────────────────────────────────────────────────────────────
select
  format_timestamp('%Y-%m-%d %H:00', timestamp_trunc(timestamp, hour)) as ghonta,
  round(sum(cast(response_headers.content_length as int64)) / 1048576.0, 1) as mot_MB
from edge_logs
  cross join unnest(metadata) as m
  cross join unnest(m.request) as request
  cross join unnest(m.response) as response
  cross join unnest(response.headers) as response_headers
where request.path like '/rest/v1/%'
group by ghonta
order by ghonta desc
limit 48;
