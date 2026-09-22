# V221_CHANGED_FILES.md

**Base:** V220 (220/2.20) → **V221 (221/2.21)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।
**Scope:** শুধু §1 (Guard Bengali-off), §2 (cloud-confirm-এ পুরোনো Warning সরানো), §3 (Refund nonce persist)। Backup/Restore/DB-trigger/SQL/RLS **ছোঁয়া হয়নি**।

## Android (Kotlin)
| File | § | কী বদলাল |
|---|---|---|
| `app/build.gradle.kts` | — | versionCode 220→221, versionName 2.20→2.21 |
| `native/NoBengali.kt` | ১ | MAP-এ ২টি অনুবাদ যোগ: `"আটকে" to "stuck"`, `"আরও" to "more"` — Bengali-off স্টাফের sync-স্ট্যাটাস/লাল সতর্কবার্তার ২টি টুকরো এখন অনূদিত (পাহারাদার ৯.১৪ পাশ)। বাংলা-চালু ব্যবহারকারীর কিছুই বদলায় না |
| `native/CloudWriteQueue.kt` | ২ | নতুন `clearConfirmed(table,id)` — cloud-confirm হলে ঐ (table,id)-এর আটকে থাকা UPSERT/UPDATE (pending+failed) মুছে দেয়; DELETE/অন্য record নয়। সস্তা `hasQueue` পাহারা (`recountQueue`) যাতে সারি ফাঁকা থাকলে হট-পথে ফাইল না ছোঁয়া হয় (hasDeletes-এর মতো) |
| `native/SupabaseClient.kt` | ২ | `upsert` (ok) ও `updateById` (changed) — আসল cloud-নিশ্চয়তার পরেই `CloudWriteQueue.clearConfirmed(table,id)` ডাকা |
| `native/PaymentRepository.kt` | ৩ | `saveRefund`-এ persist-করা nonce (`refund_nonce_store` SharedPreferences, draft-key = মোবাইল+টাকা+কারণ+তারিখ)। `getOrCreateRefundNonce`/`clearRefundNonce`/`refundNonceKey` যোগ। nonce cloud-confirm (`ok`) হলে তবেই মোছে। Refund total/approval/Visit Fee/branch/payment হিসাব অপরিবর্তিত |

## Web (Netlify **ও** assets/www — parity, byte-identical)
| File | § | কী |
|---|---|---|
| `03_NETLIFY_READY/app.js` | ৩ | `wlv1RefundDraftKey`/`wlv1GetRefundNonce`/`wlv1ClearRefundNonce` (localStorage `rk_refund_nonce`)। `saveRefundWeb`-এ per-open memory nonce-এর বদলে persist-করা nonce; cloud-confirm (`okC`) হলে তবেই মোছে |
| `03_NETLIFY_READY/index.html` | — | cache-buster v220→v221 |
| `assets/www/app.js`, `index.html` | ৩/parity | Netlify-র সঙ্গে **হুবহু এক** (app.js diff=IDENTICAL, index.html diff=IDENTICAL) |

## নতুন doc/rollback
`V221_WORK_LOG.md`, `V221_CHANGED_FILES.md`, `V221_TEST_REPORT.md`, `V221_FINAL_DECLARATION.md`, `00_LOCK_NOTE_SESSION_2026-07-31_V221.md`, নতুন `V221_FILE_MANIFEST_SHA256.json`, **`ROLLBACK_V220/`** (V220 source-এর আসল কপি)।

## এই version-এ করা হয়নি (স্পষ্ট)
- Backup/Restore code, DB trigger, SQL, RLS — **কিছুই বদলানো হয়নি** (§Q4-A/B, §Q5 আলাদা version-এ, অনুমতির পরে)।
- কোনো Design/Layout/Colour/Button/Text-arrangement/Workflow/Permission/Branch/Payment/Diet Chart/Print/Login বদল নেই।
- Android build/device/live-Supabase test — Pending (এই পরিবেশে সম্ভব নয়; TK নিজে করবেন)।
