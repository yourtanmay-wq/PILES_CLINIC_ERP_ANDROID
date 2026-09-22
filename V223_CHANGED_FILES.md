# V223_CHANGED_FILES.md

**Base:** V222 (222/2.22) → **V223 (223/2.23)**। তারিখ: 01.08.2026 IST। Owner: TK BISWAS।
**Scope:** §C1 (cloud না-পড়া গেলে Restore বন্ধ, আন্দাজে overwrite নয়), §C2 (trigger পুরোনো লেখা আটকালে App যেন success না ভাবে ও Pending না মোছে)। §1/§2/§3/refund V222-এর মতোই অটুট (verify করা)।

## Android (Kotlin)
| File | § | কী বদলাল |
|---|---|---|
| `app/build.gradle.kts` | — | versionCode 222→223, versionName 2.22→2.23 |
| `native/SupabaseClient.kt` | C2,C1 | **C2:** `upsert` — row-এ updatedAt থাকলে `return=representation&select=id,updatedAt`; ফেরা updatedAt আমাদেরটার সঙ্গে মিললে **LANDED**, না মিললে (trigger আটকেছে) **SUPERSEDED**; নেট-ব্যর্থ **FAILED**। `clearConfirmed` **শুধু LANDED**-এ; SUPERSEDED-এ clearConfirmed/remember কিছুই নয়, return true (flush obsolete drop)। `updateById`-ও একই (fields-এ updatedAt থাকলে)। **C1:** `upsertNewerWins`→`upsertRestoreSafe` যা `RestoreOutcome{WRITTEN,KEPT_NEWER,BLOCKED}` ফেরায়; `fetchListOrNull` দিয়ে **পড়া-ব্যর্থ(null) vs row-নেই(খালি)** আলাদা করে — পড়া ব্যর্থ/তুলনা অসম্ভব → BLOCKED (আন্দাজে লেখা নয়) |
| `native/TrashRepository.kt` | C1 | `restore` → `upsertRestoreSafe`; BLOCKED হলে false (কিছুই লেখা নয়, trash অক্ষত) |
| `security/SettingsActivity.kt` | C1 | Cloud JSON restore — `fetchListOrNull` (পড়া ব্যর্থ→পুরো টেবিল skip=blocked); per-row cloud-নবীন→keptNewer, তুলনা অসম্ভব→blocked, net-fail→failedNet; সব গোনা ও জানানো (নীরব নয়) |

## Web (Netlify **ও** assets/www — byte-identical)
| File | § | কী |
|---|---|---|
| `03_NETLIFY_READY/app.js` | C1 | `wlv1CloudStampMs`: **−2 পড়া ব্যর্থ / −1 row নেই / ≥0 stamp**। `wlv1RestoreTrash`: −2→restore বন্ধ+Error, তুলনা অসম্ভব→বন্ধ, cloud নবীন→keep-newer। `cloudPush`: `if(!remoteKnown) continue` — cloud না-পড়া গেলে পুরোনো local দিয়ে overwrite নয় |
| `03_NETLIFY_READY/index.html` | — | cache-buster v222→v223 |
| `assets/www/app.js`, `index.html` | parity | Netlify-র সঙ্গে হুবহু এক (diff=IDENTICAL) |

## Database (আলাদা Copy-Paste SQL — নিজে চালানো হয়নি)
| File | § | কী |
|---|---|---|
| `04_.../V223_BACKUP_OVERWRITE_GUARD_TRIGGER_COPY_PASTE.sql` | C1/C2 | V222-এর trigger (৮ টেবিল) — logic এক; `_rk_safe_ts` `IMMUTABLE`→`STABLE`; header V223। App code এখন নিজে থেকেই Restore-পথে নিরাপদ; এই trigger সব পথের (অন্য ফোন সহ) সর্বজনীন backstop |

## Tests / নতুন doc
`11_V223_TESTS/V223_logic_tests.js` (৪১ test, node-runnable) + `V223_test_results.txt` (ALL PASS)। `V223_FINAL_PROOF.md`, `V223_WORK_LOG.md`, `V223_CHANGED_FILES.md`, `V223_TEST_REPORT.md`, `V223_FINAL_DECLARATION.md`, `00_LOCK_NOTE_SESSION_2026-08-01_V223.md`, নতুন `V223_FILE_MANIFEST_SHA256.json`, **`ROLLBACK_V222/`**।

## অপরিবর্তিত (verify করা)
`CloudWriteQueue.kt`, `PaymentModel.kt`, `PaymentRepository.kt` — V222-এর সঙ্গে **byte-identical**। কোনো Design/Workflow/Permission/Branch/Login/Payment-Refund হিসাব বদল নেই। SQL নিজে চালানো হয়নি। Android build/device/live — Pending।
