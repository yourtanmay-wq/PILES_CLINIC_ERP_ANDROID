# V220_CHANGED_FILES.md

**Base:** V219 (219/2.19) → **V220 (220/2.20)**। তারিখ: 31.07.2026 IST।

## Android (Kotlin)
| File | § | কী |
|---|---|---|
| `app/build.gradle.kts` | — | versionCode 219→220, versionName 2.19→2.20 |
| `native/SupabaseClient.kt` | ১ | `errSummary()` (PostgREST error body → code·message·details, ~200 অক্ষর); upsert/updateById/deleteById-এর ব্যর্থ-শাখায় body পড়ে `httpReason`-এ যোগ |
| `native/CloudWriteQueue.kt` | ২ | `bodyHash()`; flush permanent-park-এ `permanent`+`permBodyHash`; `withFailedAdded` ঐ দুটো কপি করে; `remember()`-এ একই-ভুল-body হলে re-queue বন্ধ, body বদলালে parked মুছে retry |
| `native/PaymentModel.kt` | ৪ | `refundIdFor(..., nonce)` ও `buildRefundRow(..., nonce)` |
| `native/PaymentRepository.kt` | ৪ | `saveRefund(..., nonce)` — refundId ও buildRefundRow-এ nonce |
| `native/PaymentActivity.kt` | ৪ | `showRefundDialog`-এ per-open `refundNonce`, `saveRefund`-এ পাঠানো |

## Web (Netlify **ও** assets/www — parity, identical)
| File | § | কী |
|---|---|---|
| `03_NETLIFY_READY/app.js` | ৪ | `wlv1RefundIdFor(...,nonce)`; `openRefundFormWeb` per-open `window.__wlv1RefundNonce`; `saveRefundWeb` ঐ nonce ব্যবহার |
| `03_NETLIFY_READY/index.html` | — | cache-buster v219→v220 |
| `assets/www/app.js`, `index.html` | ৪/৫ | Netlify-র সঙ্গে **হুবহু এক** রাখা (app.js diff=IDENTICAL) |

## নতুন doc/rollback
`V220_WORK_LOG.md`, `V220_CHANGED_FILES.md`, `V220_TEST_REPORT.md`, `V220_FINAL_DECLARATION.md`, **`V220_SECTION3_BACKUP_RESTORE_REAUDIT.md`** (§3 re-audit), নতুন `V220_FILE_MANIFEST_SHA256.json`, **`ROLLBACK_V219/`** (V219 source-এর আসল কপি)।

## এই version-এ করা হয়নি (স্পষ্ট)
- **§3 (backup-safe overwrite guard)** — শুধু re-audit; কোড/SQL-এ করা হয়নি (আলাদা version, আপনার অনুমতির পরে)।
- Android build/device/live-Supabase test — Pending (এই পরিবেশে সম্ভব নয়)।
