-- ═══════════════════════════════════════════════════════════════════════
-- V1093-খ (০৫.০৯.২০২৬, TK: *"হ্যাঁ ₹৪০০ করে দিন, সঙ্গে বাকিগুলোও মিলিয়ে দেখুন"*)
--
-- 🔴 আমার পুরনো SQL-এ হাতে বসানো সারিতে **পুরো ₹৫০০** লেখা হয়েছিল, অথচ
--    ওটা Treatment ধাপের সারি — নিয়ম অনুযায়ী ₹৪০০। ওই রোগীর Registration
--    ধাপের ₹১০০ অ্যাপ আলাদা করে আগেই দিয়েছে ⇒ মোট ৬০০ হয়ে যাচ্ছিল।
-- ⛔ শুধু **এই একটাই** সারি, আর শুধু DUE হলে — দেওয়া টাকায় হাত পড়ে না।
-- ═══════════════════════════════════════════════════════════════════════

update hr.salary_payments
set amount = 400
where kind = 'EXTRA'
  and person_code = 'COB-UTTAMA'
  and coalesce(src_key,'') = ''
  and status = 'DUE'
  and extra_reason = 'Treatment · COB-21082026-004'
  and amount = 500;

-- ── বাকি সব মিলিয়ে দেখা (শুধু পড়া) ────────────────────────────────────
-- নিয়ম: Registration ধাপ → ₹১০০ (দুজন স্টাফ হলে ৫০+৫০) · Treatment ধাপ → ₹৪০০
select person_code, amount, status, paid_on,
       case when coalesce(src_key,'') = '' then 'hand' else 'app' end as source,
       extra_reason
from hr.salary_payments
where kind = 'EXTRA'
  and ( coalesce(src_key,'') = ''
        or (extra_reason ilike '%registration%' and amount not in (50, 100))
        or (extra_reason ilike '%treatment%'    and amount not in (400)) )
order by person_code, paid_on desc;
