# V216_WORK_LOG.md

**Base:** PILES_CLINIC_APP_V215_FINAL → **V216 (versionCode 216 / 2.16)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।
**নিয়ম:** সত্যিই code-এ বসানো (শুধু spec নয়), নিরাপদে, RLS live-এ চালু নয়, design/workflow/logic না ভেঙে, untested-কে "Pass" না বলে।
**Environment:** Claude (Cowork) cloud container — আসল device/দ্বিতীয় ফোন/Gradle build/live Supabase নেই। তাই static check করা হয়েছে; device/build test TK-এর।

## STEP 0 — Scaffolding
- ROLLBACK_V215/ তৈরি (V215 source-এর নিরাপদ কপি, 8.9M) ✅
- V216_WORK_LOG.md ✅

## STEP 1 — কোড পরিবর্তন (প্রতিটার পর static check)

| § | কাজ (সত্যিই code-এ) | File | Static |
|---|-----|------|--------|
| §13 | **Refund/টাকা ফেরত full feature** — refund row model, saveRefund (Master direct / Staff request+briefing), approved refund per-patient paid ও today/range collection থেকে বিয়োগ, PaymentActivity-তে Refund button+form, BriefingActivity-তে Master Approve/Reject তালিকা (+layout container) | PaymentModel.kt, PaymentRepository.kt, PaymentActivity.kt, BriefingActivity.kt, activity_briefing.xml | ✅ balanced + XML OK |
| §10 | **Report cache-first** — TimelineCache থাকলে সঙ্গে সঙ্গে আঁকে, "আলাদা Loading Screen" নেই; full load-এ cache সেভ। cache না থাকলে আগের মতোই (regression নেই) | ReportCardActivity.kt | ✅ |
| §4 | **Password hashing (backward-compatible)** — PBKDF2 hasher; login hash থাকলে hash দিয়ে, নয়তো plaintext + সফল হলে lazy migration; নতুন password সেভে fresh hash (stale hash bug নেই) | PasswordHasher.kt (new), CloudPasswordCheck.kt, LoginActivity.kt, PasswordCenterRepository.kt | ✅ |
| §5 | **Supabase Auth prep** — SupabaseAuth helper (signIn/signUp→JWT), unused (behavior বদলায় না); Auth-prep SQL (auth_user_id, refund index; RLS commented) | SupabaseAuth.kt (new), V216_AUTH_PREP_2026-07-31.sql | ✅ |
| §15 | **FCM-ready source** (drop-in, build-এ নয়) + V215-এর near-realtime bell অক্ষত | 10_FUTURE_PLANS/fcm_push_ready/ | — |
| §20 | version 215→216 / 2.15→2.16; web cache-buster v215→v216 | build.gradle.kts, index.html | — |

**Static:** ১০টা পরিবর্তিত/নতুন Kotlin file bracket-balance — সব PASS। activity_briefing.xml well-formed। কোনো unresolved idiom নেই (OkHttp body project-এর নিজস্ব idiom-এ)।

## যা এখনো বাকি (সৎভাবে)
- **§10 বাকি অংশ:** Journey/Action instant-header (QueuePatient-এ stage নেই বলে guess করলে design-flash হত — করা হয়নি), openClinical non-blocking (B179 address-on-print lock ছোঁয় বলে করা হয়নি), Follow-up ScrollView scroll (async timing device-test সংবেদনশীল)।
- **§4 web hashing live:** web login (app.js) async SubtleCrypto — testing ছাড়া live-এ rewire করা হয়নি; DB-তে hash এখন Android থেকেই বসছে, web এখনো plaintext-এ চলে (coexist)। ধাপ MANUAL_SETUP-এ।
- **§4 RLS enable / plaintext column drop / §15 FCM setup / signed APK** — manual (V216_MANUAL_SETUP_IF_REQUIRED.md)।

## STEP 2 — Final ZIP আগে যাচাই
- versionCode/Name নতুন (216/2.16) ✅ · doc সব ✅ · ROLLBACK_V215/ ও ROLLBACK_V214/ ✅
- নতুন SHA-256 manifest ✅ · Test failure লুকানো নেই ✅ · signed APK দাবি নেই ✅
