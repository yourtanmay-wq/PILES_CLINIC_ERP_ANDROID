# V215_CHANGED_FILES.md

**Base:** V214 (214/2.14) → **V215 (215/2.15)**. তারিখ: 31.07.2026 IST.
নিচে এই সেশনে **সত্যিই পরিবর্তিত/নতুন** প্রতিটা ফাইল ও কোন §-এর জন্য।

## Android (Kotlin) — কোড পরিবর্তন

| File | § | কী বদলেছে |
|---|---|---|
| `app/build.gradle.kts` | §20 | versionCode 214→215, versionName 2.14→2.15 |
| `native/PatientTimelineActivity.kt` | §12, §16, §17, §11 | (§12) Take-Action popup থেকে "Remark" row সরানো; (§16) Incomplete/Reject-এ মিথ্যা "Saved" বন্ধ + সফল হলে `finish()` করে source list-এ ফেরা; (§17) quick-remark যে কোনো stage-এ call গোনে; (§11) Take-Action → Payment/Register-এর পর `finish()` (একবার Back-এ তালিকায়) |
| `native/FollowUpActivity.kt` | §17 | card remark save সব stage-এ Completed Call হিসেবে গোনে (আগে শুধু Inquiry) |
| `native/FollowCalendarActivity.kt` | §17 | একই — calendar-এর remark save সব stage-এ call গোনে |
| `native/FollowUpRepository.kt` | §16, §6 | `updateStatus`-এ ঐচ্ছিক `cloudConfirmedOut` out-param (non-breaking) — caller সত্যিকারের cloud-confirm জানে, মিথ্যা "Saved" আর নয় |
| `native/TrashHelper.kt` | §18, §3 | পুরো record Delete-এ already-Cancelled follow-up/enquiry সারিও snapshot-এ ঢোকে ও `DeletedGuard`-এ tombstone হয় (Free-plan: redundant cloud write বাদ) — Visit-Reject/Reject তালিকা থেকে সঙ্গে সঙ্গে সরে |
| `native/TrashRepository.kt` | §18, §14 | Restore-এ প্রতিটা cascade সারির tombstone `unmark` — restore করলে linked record সঠিকভাবে ফেরে |
| `native/DraftRepository.kt` | §18 | Visit-Reject/Reject/Incomplete bucket এখন `DeletedGuard.isDeleted` মেনে বাদ দেয় (fresh load ও cached — flash-back বন্ধ) |
| `native/DraftActivity.kt` | §18 | list খোলার সময় `bucket`+branch/date extra পাঠায় (pull-to-refresh-এ সত্যিকারের reload-এর জন্য) |
| `native/DraftListActivity.kt` | §18 | `onResume`-এ deleted কার্ড সরায়; pull-to-refresh এখন সত্যিকারের cloud reload (§18.6), শুধু memory redraw নয় |
| `native/BackgroundRefreshWorker.kt` | §15, §3 | briefings-ও সস্তা HEAD-count-এ; পরিবর্তন ধরা পড়লে `BellCounter`+`BellNotifier` — near-realtime সাউন্ড/ভাইব্রেশন/background alert (de-dup সহ) |

## Web / Netlify

| File | § | কী বদলেছে |
|---|---|---|
| `03_NETLIFY_READY/_headers` (নতুন) | §4.9/4.10 | Security header: X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy, HSTS, COOP; CSP **Report-Only** মোডে (কিছু ভাঙে না) |
| `03_NETLIFY_READY/index.html` | §9.4 | cache-buster `?v=v145`→`?v=v215` (browser নতুন version আনে) |
| `assets/www/config.js` | §9 | Cooch Behar নম্বর `8514001100`(ভুল, Falakata-র)→`8514002200` (Netlify config ও StaffDirectory-র সঙ্গে মিল) |

## Supabase SQL

| File | § | কী |
|---|---|---|
| `04_SUPABASE_DATABASE_SETUP/V215_SAFE_MIGRATION_2026-07-31.sql` (নতুন) | §4,5,8,13 | PART A নিরাপদ additive (password_hash কলাম, refund কলাম, duplicate-finder); PART B (UNIQUE/FK) ও PART C (RLS) ইচ্ছে করে COMMENT — live-এ চালানোর আগে duplicate ঠিক করা ও Auth দরকার |

## নতুন doc ফাইল (এই সেশনে তৈরি)
`V215_WORK_LOG.md`, `V215_BEFORE_AUDIT.md`, `V215_CHANGED_FILES.md`, `V215_TEST_REPORT.md`, `V215_SUPABASE_SETUP_ORDER.md`, `V215_MANUAL_SETUP_IF_REQUIRED.md`, `V215_REFUND_AND_LOADING_SPEC.md`, `V215_FINAL_DECLARATION.md`, এবং `ROLLBACK_V214/` (V214 source-এর নিরাপদ কপি)।

## যা এই সেশনে **কোড-এ করা হয়নি** (সৎভাবে — spec হিসেবে দেওয়া হলো)
- **§13 Refund** — সম্পূর্ণ implementation spec: `V215_REFUND_AND_LOADING_SPEC.md`। DB কলাম যোগ করা হয়েছে; UI+totals-subtraction কোড money-path বলে testing ছাড়া live-এ বসানো হয়নি।
- **§10 Check-up/Journey/Report দ্রুত খোলা (cache-first) ও scroll preservation** — spec একই ফাইলে। §11-এর one-Back অংশ কোড-এ করা হয়েছে; loading-speed/scroll অংশ নয়।
- **§4 password hashing live-এ চালু ও §4 RLS enable** — Auth migration দরকার; `V215_MANUAL_SETUP_IF_REQUIRED.md`-এ ধাপে ধাপে।
