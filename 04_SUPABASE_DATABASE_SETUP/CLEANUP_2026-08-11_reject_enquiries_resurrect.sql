-- ============================================================================
-- এক-বারের পরিষ্কার (11.08.2026) — পুরনো ভার্সনে Reject হওয়া enquiries সারি বন্ধ করা
--
-- সমস্যা: পুরনো বিল্ডে কোনো নম্বর Reject করলে শুধু `followups` সারিতে "Cancelled"
--         দাগ পড়ত, `enquiries` টেবিলে পড়ত না। তাই Follow-up-এর Enquiry-ট্যাবের
--         সেফটি-জাল ওই এখনো-"Active" থাকা `enquiries` সারি থেকে কার্ডটা আবার
--         বানিয়ে দেয় (বিশেষত নতুন ইনস্টল/ক্যাশ মুছলে) — Reject করা নম্বর ফিরে আসে।
--
-- এই স্ক্রিপ্ট শুধু সেই `enquiries` সারিগুলোকেই "Cancelled" করে, যাদের—
--   • stage = 'Inquiry' এবং এখনো Active (বন্ধ নয়);
--   • ওই নম্বরে একটা Cancelled/Incomplete Inquiry-followup আছে (সত্যিই Reject হয়েছিল);
--   • ওই নম্বরে কোনো Active Inquiry-followup নেই (অর্থাৎ চালু/নতুন এনকোয়ারি নয়)।
--
-- ⛔ টাকা / patients / payments — কিচ্ছু ছোঁয় না।
-- ⛔ idempotent — বারবার চালালেও একই ফল, বাড়তি কিছু হয় না।
-- ⛔ চালু কোনো এনকোয়ারি লুকোয় না (Active followup থাকলে বাদ)।
-- মোবাইল শেষ-১০-অঙ্ক ধরে মেলানো — ফরম্যাট আলাদা হলেও ঠিক মেলে।
-- ============================================================================

-- ── ধাপ ১ (আগে এটাই চালান — শুধু দেখা, কিছুই বদলায় না) ──────────────────────
--    কোন সারিগুলো বদলাবে, তার তালিকা:
SELECT e.id, e.name, e.mobile, e.branch, e.status, e.date
FROM public.enquiries e
WHERE e.stage = 'Inquiry'
  AND COALESCE(NULLIF(TRIM(e.status), ''), 'Active') = 'Active'
  AND EXISTS (
        SELECT 1 FROM public.followups f
        WHERE right(regexp_replace(f.mobile, '\D', '', 'g'), 10)
            = right(regexp_replace(e.mobile, '\D', '', 'g'), 10)
          AND f.stage = 'Inquiry'
          AND f.status IN ('Cancelled', 'Incomplete')
      )
  AND NOT EXISTS (
        SELECT 1 FROM public.followups f2
        WHERE right(regexp_replace(f2.mobile, '\D', '', 'g'), 10)
            = right(regexp_replace(e.mobile, '\D', '', 'g'), 10)
          AND f2.stage = 'Inquiry'
          AND COALESCE(NULLIF(TRIM(f2.status), ''), 'Active') = 'Active'
      )
ORDER BY e.date;


-- ── ধাপ ২ (উপরের তালিকা ঠিক মনে হলে তবেই এটা চালান — আসল পরিবর্তন) ──────────
UPDATE public.enquiries e
SET status = 'Cancelled',
    "updatedAt" = now()
WHERE e.stage = 'Inquiry'
  AND COALESCE(NULLIF(TRIM(e.status), ''), 'Active') = 'Active'
  AND EXISTS (
        SELECT 1 FROM public.followups f
        WHERE right(regexp_replace(f.mobile, '\D', '', 'g'), 10)
            = right(regexp_replace(e.mobile, '\D', '', 'g'), 10)
          AND f.stage = 'Inquiry'
          AND f.status IN ('Cancelled', 'Incomplete')
      )
  AND NOT EXISTS (
        SELECT 1 FROM public.followups f2
        WHERE right(regexp_replace(f2.mobile, '\D', '', 'g'), 10)
            = right(regexp_replace(e.mobile, '\D', '', 'g'), 10)
          AND f2.stage = 'Inquiry'
          AND COALESCE(NULLIF(TRIM(f2.status), ''), 'Active') = 'Active'
      );
-- চলার পরে Supabase দেখাবে: UPDATE <N> — N = যতগুলো পুরনো Reject পরিষ্কার হলো।
