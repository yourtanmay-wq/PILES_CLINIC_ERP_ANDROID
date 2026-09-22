# V219_CHANGED_FILES.md

**Base:** V218 (218/2.18) → **V219 (219/2.19)**। তারিখ: 31.07.2026 IST।

## Android (Kotlin)
| File | § | কী |
|---|---|---|
| `app/build.gradle.kts` | — | versionCode 218→219, versionName 2.18→2.19 |
| `native/PaymentModel.kt` | §1 | `refundIdFor()` (deterministic refund id); `buildRefundRow` random UUID → deterministic id |
| `native/PaymentRepository.kt` | §1 | `pendingRefundSumForPatient(excludeId)`; `saveRefund` refundId বের করে exclude পাঠায় (retry না আটকায়) |
| `native/CloudWriteQueue.kt` | §4 | `withFailedAdded` per-entry `why` + `lastError` রাখে (কারণ আর মুছে যায় না); flush-এ HTTP 400/404/422 = permanent → ২ চেষ্টার পর park; নতুন `stuckDetail()` (Table·Record·কারণ) |
| `native/PendingSyncStatus.kt` | §4 | সতর্কবার্তার লেখায় `stuckDetail` (সর্বোচ্চ ৩) যোগ (নতুন design/বোতাম নয়) |
| `native/BriefingRepository.kt` | §7 | `fetchRaw` এখন `CloudReadCache` (২০s) দিয়ে dedupe; ফাঁকা/ব্যর্থ কখনো cache নয় |

## Web (Netlify **এবং** assets/www — এখন হুবহু এক, §5)
| File | § | কী |
|---|---|---|
| `03_NETLIFY_READY/app.js` | §1, §2 | §1: `wlv1RefundIdFor` + `saveRefundWeb` deterministic id + local/briefing dedup + pending exclude। §2: `wlv1DeleteDraftEntry(table, recId, ...)` — মোবাইল নয়, **Record ID** ধরে; caller সঠিক id পাঠায় |
| `03_NETLIFY_READY/index.html` | — | cache-buster v218→v219 |
| `assets/www/app.js`, `config.js`, `index.html`, `styles.css` | §5 | Netlify web app-এর সঙ্গে **হুবহু এক** করা হলো (logo subfolder অক্ষত) |

## Supabase SQL (new)
| File | § | কী |
|---|---|---|
| `04_.../V219_SECURITY_SQL_COPY_PASTE_2026-07-31.sql` | §6 | PART A নিরাপদ (কলাম/hash-বাকি/index); PART B RLS ও PART C plaintext-drop COMMENT (এখন নয়) |

## নতুন doc/rollback
`V219_WORK_LOG.md`, `V219_CHANGED_FILES.md`, `V219_TEST_REPORT.md`, `V219_FINAL_DECLARATION.md`, নতুন `V219_FILE_MANIFEST_SHA256.json`, **`ROLLBACK_V218/`** (V218 source-এর আসল কপি)।

## কোড-এ করা হয়নি (Pending/manual — §9/§10)
- Android build/device/live-Supabase test (এই পরিবেশে সম্ভব নয়) — V219_TEST_REPORT-এ Pending।
- §7 বাকি টেবিল (Draft/Trash/Queue/Chamber) — একই নিরাপদ pattern-এ কমানো যায়, এই session-এ শুধু briefings।
- §6 RLS enable / plaintext drop / Supabase Auth live-wiring — manual (SQL-এ ধাপ)।
- §8 FCM instant push — করা হয়নি; "instant complete" দাবি করা হয়নি।
