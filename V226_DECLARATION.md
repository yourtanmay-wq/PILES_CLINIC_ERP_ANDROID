# V226 — ঘোষণা (Declaration)

“৯২টি কাজের প্রতিটি নম্বর code ও evidence দিয়ে পুনরায় যাচাই করা হয়েছে। অনুমোদিত তালিকার বাইরে কোনো Design, Workflow, Permission, Branch Rule, Payment Logic, Print Logic, Login, Database Rule বা Feature পরিবর্তন করা হয়নি। কোনো অসম্পূর্ণ বা পরীক্ষা না-করা কাজকে Done/Passed বলা হয়নি।”

## কেন `FINAL` নয়
এই cloud পরিবেশে Android SDK নেই ও Google server blocked — তাই বাধ্যতামূলক `assembleDebug` build evidence এখানে তৈরি করা **অসম্ভব**। আপনার নিয়ম ১১ অনুযায়ী, build/device/live-DB যাচাই বাকি থাকতে কোনো কিছুকে misleadingভাবে `FINAL` বলা যাবে না। তাই:
- ZIP: `PILES_CLINIC_APP_V226_DRAFT_BUILD_PENDING.zip`
- Root: `PILES_CLINIC_APP_V226_DRAFT`
- `versionCode = 226`, `versionName = "2.26"` (App/Dashboard version auto-update; Web `?v=v226`)

## এই version-এ সত্যিই যা করা হয়েছে
1. **item 86** — `ReportsRepository.monthOf()` তারিখ-ধাঁচ (dd.MM.yyyy / dd/MM/yyyy) সঠিকভাবে ধরে, current-month ০ দেখানোর কারণ দূর করে; ISO ফল অপরিবর্তিত (logic-test পাস)।
2. **Version bump** 226/2.26 — Android + Web parity।
3. **item 10/11 সংশোধন** — V225-এর মিথ্যা "ReportsRepository changed" claim পুনরাবৃত্তি করা হয়নি; **কার্যকর `ROLLBACK_V226`** দেওয়া (যাচাইকৃত)।
4. **item 46** — Trash Master branch filter-এর হুবহু ready-to-apply patch (`V226_TRASH_BRANCH_FILTER_PATCH.md`); approved layout আন্দাজে না বদলে owner build+device-এ merge করবেন।
5. **item 7/8/82/83** — read-only যাচাই SQL (`V226_..._READ_ONLY_CHECKS.sql`); কোনো SQL চালানো হয়নি।

## সৎভাবে বাকি (build/device/live-DB/owner-সংজ্ঞা)
`V226_74_ITEM_STATUS.md`-তে প্রতিটি item-এর status, কারণ ও ফাইল-অবস্থান — Already Correct / Fixed / Partial / Owner Build+Visual / Owner Live-DB / Owner Definition। কোনোটিকে অজুহাতে ফেলে রাখা হয়নি; যা সত্যিই এই পরিবেশে অসম্ভব (device/live-DB/চোখে-দেখা/owner-সংজ্ঞা), কেবল সেগুলোই owner-required।

**সততা:** কোনো মিথ্যা Build Success, মিথ্যা Test Pass বা আন্দাজে ফল তৈরি করা হয়নি। Patient/Payment/Treatment/Follow-up/Medical/Photo/History ডেটা-পথ ছোঁয়া হয়নি।

*(2026-08-01)*
