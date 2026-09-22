# V216_CHANGED_FILES.md

**Base:** V215 (215/2.15) → **V216 (216/2.16)**। তারিখ: 31.07.2026 IST।

## Android (Kotlin/XML) — পরিবর্তিত/নতুন

| File | § | কী |
|---|---|---|
| `app/build.gradle.kts` | §20 | versionCode 215→216, versionName 2.15→2.16 |
| `native/PaymentModel.kt` | §13 | `buildRefundRow`, refund constants, `isRefundRow`/`isApprovedRefund`, sourceLabel-এ "Refund / টাকা ফেরত" |
| `native/PaymentRepository.kt` | §13 | `saveRefund`/`approveRefund`/`rejectRefund`/`fetchPendingRefundRequests`/`fetchPendingRefundCount`; per-patient paid ও today/range collection-এ approved refund **বিয়োগ**; slim column list-এ `refundApprovalStatus` |
| `native/PaymentActivity.kt` (new fn) | §13 | payment form-এ "💸 Refund / টাকা ফেরত" button + `showRefundDialog` (amount/mode/reason → saveRefund; Master direct / Staff request) |
| `native/BriefingActivity.kt` (new fn) | §13 | Master-only `loadRefundRequests()` — Pending Refund Requests তালিকা + Approve/Reject; onCreate/onResume-এ ডাকা |
| `res/layout/activity_briefing.xml` | §13 | নতুন `refundRequestsContainer` (GONE by default) |
| `native/ReportCardActivity.kt` | §10 | cache-first render (TimelineCache) — আলাদা Loading Screen সরানো; full load-এ cache সেভ |
| `native/PasswordHasher.kt` (**new**) | §4 | PBKDF2-HMAC-SHA256 hash/verify (backward-compatible) |
| `native/CloudPasswordCheck.kt` | §4 | password_hash আনা; `HasCustom(password, passwordHash)`; `storePasswordHash` (lazy migration) |
| `native/LoginActivity.kt` | §4 | hash থাকলে hash-verify, নয়তো plaintext + সফল হলে background hash সেভ |
| `native/PasswordCenterRepository.kt` | §4 | নতুন password সেভে fresh `password_hash` (stale-hash bug নেই) |
| `native/SupabaseAuth.kt` (**new**) | §5 | Supabase Auth (GoTrue) helper — signIn/signUp→JWT (prepared, unused, behavior বদলায় না) |

## Web
| File | § | কী |
|---|---|---|
| `03_NETLIFY_READY/index.html` | §9.4 | cache-buster v215→v216 |

## Supabase SQL (new)
| File | § | কী |
|---|---|---|
| `04_SUPABASE_DATABASE_SETUP/V216_AUTH_PREP_2026-07-31.sql` | §4/5/13 | PART A additive: `usercredentials.auth_user_id`, password_hash নিশ্চিত, `payments_refund_pending_idx`; PART B RLS commented (DO NOT RUN) |

## নতুন doc/asset
`V216_WORK_LOG.md`, `V216_CHANGED_FILES.md`, `V216_TEST_REPORT.md`, `V216_MANUAL_SETUP_IF_REQUIRED.md`, `V216_FINAL_DECLARATION.md`, নতুন `V216_FILE_MANIFEST_SHA256.json`, `ROLLBACK_V215/`, `10_FUTURE_PLANS/fcm_push_ready/` (PilesFirebaseMessagingService.kt.txt + README_FCM.md)।

## যা কোড-এ করা হয়নি (সৎভাবে — কারণসহ)
- §10 Journey/Action instant-header (QueuePatient-এ stage নেই → guess design-flash), openClinical non-blocking (B179 lock), Follow-up ScrollView scroll (async timing device-test)।
- §4 web login hashing live rewire (async SubtleCrypto, testing দরকার — DB hash এখন Android থেকে বসছে, web plaintext-এ coexist)।
- §4 RLS enable / plaintext column drop / FCM full setup / signed APK — manual (V216_MANUAL_SETUP_IF_REQUIRED.md)।
