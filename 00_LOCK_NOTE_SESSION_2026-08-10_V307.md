# 🔒 LOCK NOTE — সেশন 10.08.2026 · V307

**কে:** Claude (claude-opus-4-8), Anthropic · **মালিক:** TK Biswas · `versionCode 307 / 3.07`

## এই সেশনে যা হয়েছে (বিস্তারিত: `00_TK_KAJER_TARIKH_SOMOY_LOG.md`, B593–B596)

1. **গার্ড-ফিক্স (tk_guard.py):** `kotlin_balance()`-এর `"""` raw-string + `${...}` ভুল-মোড বাগ ঠিক — আর ভালো কোডকে মিথ্যা "brace ±2" বলে না; আসল ভুল ঠিকই ধরে।
2. **B593 — row_not_matched লাল সতর্কবার্তা পাকাপাকি সমাধান:** `SupabaseClient.updateById` (terminal, remember নয়, clearConfirmed) + `CloudWriteQueue.flush` (নিঃশব্দে বাদ)। তথ্য হারায় না। SQL লাগে না।
3. **B594 — Cooch Behar অংশীদার তারিখ সংশোধন:** SQL `V309_COOCH_BEHAR_PARTNER_TIMELINE_FIX_2026-08-10.sql` (TK চালিয়ে Success পেয়েছেন)। Jan–Jul: TK 50/K.H 50; Aug 1+: 40/40/10/10।
4. **B595 — Partner Setup-এ "ভাগ শুরুর তারিখ" (ফোন+ওয়েব):** মাঝ-বছরে নতুন অংশীদার আর সবাইকে জানুয়ারি থেকে ধরবে না। accrual অটুট।
5. **B596 — 📅/📆/🗓 (17 July) সব বাদ → ⏰:** ৭৭ বদল, ২২ ফাইল, দুই অ্যাপ। লজিক/অনুবাদ/তারিখ-ফরম্যাট অটুট।

## যাচাই (ফাইল বানানোর আগে)
- guard [৯.১] brace ✅ · [৯.৬] XML (277) ✅ · [৯.৮] V307 ✅ · [৯.৯] web ✅ · [৯.১১] ✅
- সব web `node --check` ✅ · সব XML `xmllint` ✅ · source-এ 📅/📆/🗓 শূন্য ✅ · এই সেশনের ফাইলে নতুন guard-সমস্যা নেই।
- guard-এর পুরনো [৯.১২]/[৯.১৩]/[৯.১৪]: লক করা স্বাক্ষর (TK-এর ০২.০৮ অনুমোদিত) ও পুরনো B158 বাংলা-অনুবাদ — এই সেশনের নয়, অ্যাপ ভাঙে না।

## 🔴 TK-কে করতে হবে
- ফোনে V307 বিল্ড; কম্পিউটারে Netlify রি-ডিপ্লয় (নতুন `app.js/finance.js/partners.js`)।
- লাইভ টেস্ট: লাল "যায়নি" বার্তা গেছে · Partner ভাগ ঠিক · "Share from" কাজ করে · কোথাও 📅 নেই।

⛔ Android SDK এখানে নেই — কোড-যাচাই আমি করেছি, বিল্ড-টেস্ট TK ফোনে।
