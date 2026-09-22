# V215_WORK_LOG.md — ZERO-RISK MASTER FIX ORDER

**Base:** PILES_CLINIC_APP_V214_FINAL.zip (versionCode 214, versionName 2.14)
**Owner:** TK BISWAS
**Rule:** সৎভাবে কাজ। কাজ না করে "Fixed" লেখা হবে না। কোনো Test সত্যিই না চালিয়ে "Pass" লেখা হবে না।
**Environment:** Claude (Cowork) cloud session — Linux container. No physical Android device, no second phone, no real clinic Supabase project connected.

---

## STEP 0 — Scaffolding (non-destructive, no code change)

| # | কাজ | ফলাফল |
|---|-----|--------|
| 0.1 | Base zip খুলে structure যাচাই | DONE — 190 Kotlin file, ~52,667 line; Netlify web app; Supabase SQL; বহু lock note |
| 0.2 | git state দেখা | DONE — working tree-তে uncommitted modification আছে (Android + web file গুলো last commit থেকে আলাদা) |
| 0.3 | ROLLBACK_V214/ তৈরি (02/03/04 folder-এর নিরাপদ copy) | DONE — 8.8M, build artifact বাদ |
| 0.4 | V215_WORK_LOG.md তৈরি | DONE (এই file) |
| 0.5 | V215_BEFORE_AUDIT.md তৈরি | IN PROGRESS |

---

## STEP 1 — BEFORE AUDIT (code পড়ে সত্যতা যাচাই — কোনো পরিবর্তন নয়) — DONE

বিস্তারিত: **V215_BEFORE_AUDIT.md**। প্রতিটা §10-§18 + §4/5/8/9 আসল code-এ file:line সহ যাচাই করা হয়েছে। সারাংশ:

- §10 Check-up→Action-এর পর Back = **আগেই ঠিক** (`ClinicalModulesActivity.kt:113`)। বাকি nav/loading সমস্যা সত্য।
- §11 back-nav (Register/Payment থেকে একবার Back) — **সত্য**।
- §12 Action-menu Remark — **শুধু ১ লাইন** সরাতে হবে (`PatientTimelineActivity.kt:524`)। নিরাপদ।
- §13 Refund — **এখন নেই**, বানাতে হবে; template আছে (BackdateRequest + audit-row)। বড় কাজ।
- §14 delete→Trash — soft-delete আছে, কিন্তু payment/medical/photo একসঙ্গে যায় না; same-day staff-delete UI gate নিয়মের বিপরীত।
- §16 Incomplete — **মিথ্যা "Saved"** (`FollowUpRepository.kt:1949`) + `finish()` না করা (`PatientTimelineActivity.kt:1471`)। সত্য।
- §17 Call signal — `incrementCall = stage=="Inquiry"` only (`FollowUpActivity.kt:3343`) → patient card-এ call গোনে না। সত্য, স্পষ্ট কারণ।
- §18 Draft/Visit-Reject delete — তিন কারণ (TrashHelper Cancelled-skip L246 + DraftListActivity stale snapshot + DraftRepository DeletedGuard filter নেই)। সত্য।
- §6 মিথ্যা "Saved" — গঠনগত; queue মজবুত কিন্তু toast cloud-confirm-এর আগে।
- §15 Notification — sound/vibration আছে, near-realtime নেই (দিনে ৩ বার trigger)। FCM নেই।
- **§4/5/8 🔴 CRITICAL:** plaintext password (config.js:39, StaffDirectory.kt:68, DB column), RLS সব টেবিলে **বন্ধ**, anon key উন্মুক্ত → live DB কার্যত public read/write। DB-তে UNIQUE/FK নেই।
- §9: web-এ CSP/security header নেই; app.js-এ duplicate function; assets/www/config.js-এ Cooch Behar নম্বর ভুল।

**⚠️ RLS এখনই চালু করলে live app বন্ধ হবে (anon key আটকাবে)। আগে Supabase Auth দরকার — staged কাজ। এই session-এ "enable RLS" SQL দেওয়া হবে না।**

---

## STEP 2 — কোড পরিবর্তন (প্রতিটার পর static check)

প্রতিটা পরিবর্তনের বিস্তারিত `V215_CHANGED_FILES.md`, ফল `V215_TEST_REPORT.md`, ঘোষণা `V215_FINAL_DECLARATION.md`-এ।

| § | কাজ | কোন file | Static check |
|---|-----|----------|--------------|
| §12 | Action popup থেকে Remark সরানো | PatientTimelineActivity.kt | ✅ balanced |
| §17 | সব stage-এ post-call remark = call (৩ entry-point) | FollowUpActivity, FollowCalendarActivity, PatientTimelineActivity | ✅ balanced |
| §16/§6 | মিথ্যা "Saved" বন্ধ + source list-এ finish() | FollowUpRepository (out-param), PatientTimelineActivity | ✅ balanced |
| §18 | Delete tombstone+snapshot, list filter, restore unmark, real refresh | TrashHelper, TrashRepository, DraftRepository, DraftActivity, DraftListActivity | ✅ balanced |
| §15 | near-realtime bell (HEAD count + BellNotifier) | BackgroundRefreshWorker.kt | ✅ balanced |
| §11 | Payment/Register থেকে one-Back to list | PatientTimelineActivity.kt | ✅ balanced |
| §9 | Cooch Behar নম্বর, web cache-buster v215 | assets/www/config.js, index.html | ✅ node --check |
| §4 | Netlify security header (_headers, CSP Report-Only) | 03_NETLIFY_READY/_headers | ✅ |
| §4/5/8 | নিরাপদ additive DB migration (RLS/FK bন্ধ) | V215_SAFE_MIGRATION_2026-07-31.sql | ✅ |
| §20 | version 214→215 / 2.14→2.15 | app/build.gradle.kts | — |

**Static:** ১০টা পরিবর্তিত Kotlin file bracket-balance — সব PASS। config.js `node --check` PASS।

**কোড-এ করা হয়নি (spec/manual — সৎভাবে):** §13 Refund (money-path, testing দরকার — spec: V215_REFUND_AND_LOADING_SPEC.md), §10 loading/scroll (spec), §4 password-hash live + RLS enable (Auth ধাপ — V215_MANUAL_SETUP_IF_REQUIRED.md), FCM instant push (ঐচ্ছিক), signed APK (keystore TK-এর)।

## STEP 3 — Final ZIP আগে যাচাই (§20)
- versionCode/Name নতুন (215/2.15) ✅
- প্রয়োজনীয় doc সব আছে ✅ · ROLLBACK_V214/ আছে ✅
- নতুন SHA-256 manifest তৈরি ✅ (V215_FILE_MANIFEST_SHA256.json)
- কোনো Test failure লুকানো হয়নি ✅ · signed APK দাবি করা হয়নি ✅

---

## HONEST ENVIRONMENT LIMITS (শুরুতেই পরিষ্কার)

এই cloud session-এ যা **সত্যিই করা যায়**:
- Real source code পড়ে প্রতিটা claimed সমস্যা যাচাই (root cause সহ)।
- Kotlin/JS/XML/SQL-এ code পরিবর্তন এবং static syntax check (bracket/paren/import, project-এর নিজস্ব guard style)।
- Supabase RLS / security migration SQL লেখা।
- Web security header, duplicate function যাচাই।
- Hash manifest, rollback, doc তৈরি।

এই session-এ যা **সত্যিই করা যায় না** (তাই "Pass" লেখা হবে না):
- আসল ফোনে বা দুইটা ফোনে Sync/Notification/Weak-internet test (Order §19-এর অধিকাংশ device test)।
- পুরো Gradle Android build compile করে APK/AAB বানানো (Android SDK + সব dependency এই container-এ নিশ্চিত নয়; চেষ্টা করা হবে, ফল সৎভাবে লেখা হবে)।
- Signed APK (release key repo-তে নেই → §20.10 অনুযায়ী "signed" দাবি করা হবে না)।
- আসল live Supabase project-এ migration চালানো (শুধু নিরাপদ SQL দেওয়া হবে, চালানো owner-এর হাতে)।

এই দুটো তালিকা মেনেই নিচের সব কাজ লেখা হবে।
