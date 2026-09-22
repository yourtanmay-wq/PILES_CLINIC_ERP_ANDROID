# V222_CHANGED_FILES.md

**Base:** V221 (221/2.21) → **V222 (222/2.22)**। তারিখ: 01.08.2026 IST। Owner: TK BISWAS।
**Scope:** §1 (নতুন Pending কখনো হারাবে না), §2 (একই মোবাইলে দুই রোগীর Refund কখনো এক নয়), §3 (Backup/Restore overwrite — App code + DB trigger)।

## Android (Kotlin)
| File | § | কী বদলাল |
|---|---|---|
| `app/build.gradle.kts` | — | versionCode 221→222, versionName 2.21→2.22 |
| `native/CloudWriteQueue.kt` | ১ | `clearConfirmed(confirmedKind, table, id, confirmedBody, writeStart)` — **সময়-পাহারা** (শুধু লেখা শুরুর আগে জমা কাজ, at ≤ writeStart) + **supersede-পাহারা** (UPSERT সব supersede; UPDATE শুধু subset-ঘরের UPDATE)। `withFailedAdded`-এ enqueue-সময় `at` সংরক্ষণ (failed-ঘরেও সময়-পাহারা কাজ করে)। নতুন Remark/Date/Payment/Follow-up কখনো মোছে না; অন্য id/DELETE ছোঁয় না |
| `native/SupabaseClient.kt` | ১,৩ | §1: `upsert`/`updateById`-এ নেট-কল শুরুর আগে `writeStart` ধরা ও নতুন signature-এ `clearConfirmed` কল। §3: নতুন `rowStampMs`/`parseIsoMs` (updatedAt→ms) ও `upsertNewerWins(table,row)` — Restore-নিরাপদ upsert (cloud কড়া নবীন হলে overwrite নয়, ব্যর্থও নয়) |
| `native/PaymentModel.kt` | ২ | `refundIdFor` raw-এর সামনে `patient.id` — এক মোবাইলে দুই আলাদা রোগীর Refund id কখনো মেলে না (web-এর সঙ্গে hashCode parity) |
| `native/PaymentRepository.kt` | ২ | `refundNonceKey`-এর সামনে `patient.id` — persist-nonce draft-key দুই রোগীর কখনো মেলে না |
| `native/TrashRepository.kt` | ৩ | Trash `restore`-এ `upsert` → `upsertNewerWins` (পুরোনো snapshot নতুন cloud data চাপা দেয় না) |
| `security/SettingsActivity.kt` | ৩ | Cloud JSON Restore (`doCloudJsonRestore`) — টেবিল-প্রতি একবার cloud `id,updatedAt` নামিয়ে map, পুরোনো row বাদ (নতুন জেতে), কতগুলো "kept newer" জানানো (silent loss নয়)। per-row read নয় → Free-plan-এ চাপ কম |

## Web (Netlify **ও** assets/www — parity, byte-identical)
| File | § | কী |
|---|---|---|
| `03_NETLIFY_READY/app.js` | ২,৩ | §2: `wlv1RefundIdFor` raw + `wlv1RefundDraftKey`-এর সামনে `p.id` (Android-এর সঙ্গে হুবহু এক)। §3: `wlv1CloudStampMs` + `wlv1RestoreTrash`-এ newer-wins guard (cloud নবীন হলে পুরোনো snapshot চাপা দেয় না)। (Web bulk restore আগে থেকেই `mergeForCloudPush` newer-wins) |
| `03_NETLIFY_READY/index.html` | — | cache-buster v221→v222 |
| `assets/www/app.js`, `index.html` | ২/৩/parity | Netlify-র সঙ্গে **হুবহু এক** (app.js diff=IDENTICAL, index.html diff=IDENTICAL) |

## Database (আলাদা Copy-Paste SQL — সরাসরি চালানো হয়নি)
| File | § | কী |
|---|---|---|
| `04_SUPABASE_DATABASE_SETUP/V222_BACKUP_OVERWRITE_GUARD_TRIGGER_COPY_PASTE.sql` | ৩ | ৮টি মূল টেবিলে `BEFORE UPDATE` trigger — পুরোনো `updatedAt` দিয়ে নতুন row চাপা দেওয়া আটকায় (সংঘর্ষে নতুন জেতে)। NULL/অদ্ভুত-format/heal/subset/legacy — সব নিরাপদে যাচাই করা। কখন কী চালাতে হবে সহজ বাংলায়, এক-টেবিল টেস্ট ও রোলব্যাকসহ। **আমি চালাইনি — TK নিজে চালাবেন** |

## নতুন doc/rollback
`V222_WORK_LOG.md`, `V222_CHANGED_FILES.md`, `V222_TEST_REPORT.md`, `V222_FINAL_DECLARATION.md`, `00_LOCK_NOTE_SESSION_2026-08-01_V222.md`, নতুন `V222_FILE_MANIFEST_SHA256.json`, **`ROLLBACK_V221/`** (V221 source-এর আসল কপি)।

## এই version-এ করা হয়নি (স্পষ্ট)
- SQL নিজে চালানো হয়নি (আলাদা file-এ, TK চালাবেন)।
- কোনো Design/Layout/Colour/Button/Text-arrangement/Print/Diet Chart/Workflow/Permission/Branch/Login বদল নেই।
- Broad refactor/optimization/cleanup নেই। Free-plan-এ অপ্রয়োজনীয় read/write নেই (Restore-এর read Restore-পথেই, রোজকার সেভে নয়)।
- Android build/device/live-Supabase test — Pending (TK নিজে করবেন)।
