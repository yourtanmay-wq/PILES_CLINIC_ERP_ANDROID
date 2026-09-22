-- ============================================================================
-- V1484 — এক-বারের হাতে-চালানো হিসাব-মেলানো (অ্যাপের বোতাম কোনো কারণে ধরছে না)
--
-- ⚠️ এটা লেখার SQL (আগেরগুলোর মতো শুধু-দেখার না) — কিন্তু নতুন কোনো নিয়ম নয়,
--    V1482-এ TK যে নিয়ম আগেই অনুমোদন করেছেন (হ্যাঁ বলেছেন AskUserQuestion-এ)
--    সেটাই। অ্যাপে Salary Statement পর্দা খুললে এটা আপনা থেকে চলার কথা
--    (hr.incentive_sync), কিন্তু কয়েকঘণ্টা/বারবার চেষ্টাতেও MANIK ROY ও
--    DIPANKAR ROY-র বকেয়া বদলায়নি (V1483-এর ফলাফলে ধরা পড়েছে) — তাই এখন
--    সরাসরি SQL দিয়ে সেই একই মেলানোর কাজটা একবার করিয়ে দেওয়া হচ্ছে।
--
-- ⛔ এই SQL হুবহু hr.incentive_sync()-এর ভিতরের কাজ — নতুন কোনো টাকার নিয়ম নেই।
-- ⛔ ইতিমধ্যে PAID (দেওয়া হয়ে গেছে) কোনো সারি কখনো ছোঁয়া হয় না।
-- ⛔ শুধু এখনো-বাকি (DUE) সারির অঙ্ক মিলিয়ে নেওয়া হয়, বা আর পাওনা না হলে
--    মুছে দেওয়া হয় (V1482-এর নতুন নিয়ম মতো)।
-- ⚠️ এটা শুধু মানিক/দীপঙ্কর নয় — সব ব্রাঞ্চের সব এখনো-বাকি Extra Income
--    সারিই এক সাথে নতুন হিসাবে মিলে যাবে (এটাই TK-র চাওয়া নিয়ম)।
-- ============================================================================

insert into hr.salary_payments
  (person_code, paid_on, amount, mode, paid_by, remark, for_month, kind, extra_reason, status, src_key)
select w.person_code, current_date, w.amount, '', 'auto', '', '', 'EXTRA', w.reason, 'DUE', w.src_key
from hr.incentive_wanted() w
on conflict (src_key) do update
   set amount = excluded.amount,
       extra_reason = excluded.extra_reason
   where salary_payments.status = 'DUE';

with want as materialized (select w.src_key from hr.incentive_wanted() w)
delete from hr.salary_payments s
where s.src_key like 'INC:%'
  and coalesce(s.status, 'PAID') = 'DUE'
  and not exists (select 1 from want q where q.src_key = s.src_key);

-- মিলিয়ে দেখা (শুধু পড়া) — MANIK ROY আর DIPANKAR ROY-র সারি এখন কী দেখাচ্ছে
select p.name, s.person_code, s.src_key, s.amount, s.status
from hr.salary_payments s
join public.patients p on s.src_key like 'INC:%:' || p.id || ':%'
where right(regexp_replace(coalesce(p.mobile, ''), '\D', '', 'g'), 10) in ('8927947851', '6294514785')
order by p.name, s.src_key;
