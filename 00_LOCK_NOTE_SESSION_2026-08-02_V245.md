# 🔒 LOCK NOTE — সেশন 02.08.2026 — V245 (তিনটি নতুন Module)

আগের ভার্সন: **V244 (versionCode 244)** → এখন: **V245 (versionCode 245, versionName 2.45)**।

**এই সেশনে কী হয়েছে (মালিকের চূড়ান্ত অনুমতিতে — তিনটি আলাদা নিরাপদ Module):**

1. **Professional Profile & Salary** — schema `hr` (নতুন)।
2. **My Work Notebook + Daily/Monthly Report + Outside Call** — schema `wn` (নতুন)।
3. **Master-only Income & Expense** — schema `fin` (নতুন)।

## ⛔ কোনো পুরোনো জিনিস বদলায়নি
- পুরোনো কোনো Table বদলানো হয়নি; পুরোনো Table-এ RLS চালু করা হয়নি।
- Patient · Payment · Refund · Product · Follow-up · Briefing · Login · Print ·
  Sync · Design · পুরোনো Logic — সব **অপরিবর্তিত**।
- কোনো Card/Button/লেখা/সতর্কবার্তা/Lock Note/Guard/Rollback/.git মোছা হয়নি।
- Dashboard-এর পুরোনো কার্ড, ডিজাইন, রং, সাইজ, ক্রম, ফাঁক — সব একই। শুধু
  **শেষে** নতুন কার্ড যোগ হয়েছে (Staff: My Work Notebook · Master: Staff Profiles,
  Income & Expense)। "My Profile" Staff-এর More Menu-তে।

## যা যোগ হয়েছে (additive)
- **DB (verified):** `04_SUPABASE_DATABASE_SETUP/V245_MODULES_HR_WN_FIN_2026-08-02.sql`
  — schemas hr/wn/fin, tables, RLS (নতুন টেবিলে only, FORCED), audit triggers,
  outside-call dedupe, identity map + helper। আসল Postgres-এ চালিয়ে যাচাই করা:
  Master সব দেখে · প্রতি Staff শুধু নিজেরটা · anon/public key কিছুই দেখে না ·
  audit পুরোনো মান রাখে · idempotent।
- **Website (live):** `module_core.js · profile.js · notebook.js · finance.js`
  (নতুন) + additive hooks: `index.html`-এ 4টি script, `app.js`-এ Dashboard/More
  Menu-তে কার্ড। In-app Call বোতাম চাপ গোনার additive logger (কল বদলায় না,
  connected/duration কখনো বলে না)।
- **Android (native, আসল কোড):** `.../java/com/tkbiswas/pilesclinic/modules/`-এ
  ৫টি আসল `.kt` — `ModuleAuth.kt · ModuleUi.kt · StaffProfileActivity.kt ·
  WorkNotebookActivity.kt · IncomeExpenseActivity.kt` (compiled source set-এ,
  staging নয়)। additive integration: Manifest-এ ৩ Activity, DashboardActivity-তে
  ৩ কার্ড (programmatic), MoreMenu-তে My Profile, CallChooser-এ ১ লাইন call-tap।
  নতুন কোনো Gradle dependency নেই (আগের OkHttp ব্যবহার)। Guard bracket/XML/column/
  বাংলা সব পাশ। ⚠️ **APK আমি cloud-এ build করতে পারিনি — আপনি Android Studio-তে
  build করবেন** (সৎ ঘোষণা; কোড প্রজেক্টের প্যাটার্ন মেনে লেখা ও Guard-পাশ)।

## যাচাই
- `00_GUARD/tk_guard.py --release` — সব মেশিন-যাচাই **পাশ** (২৩টি লক নিয়ম, bracket,
  XML, column-match 9.7, বাংলা-বন্ধ 9.14, version 9.8 = V245, web app 9.9)।
- নতুন web ফাইল সব `node --check` পাশ।
- DB আসল Postgres-এ চালিয়ে RLS/audit/dedupe/idempotency যাচাই।

## ⛔ যা মালিককে নিজে করতে হবে (cloud-এ করা যায় না — সৎ ঘোষণা)
- Supabase-এ SQL চালানো, schema expose, bucket তৈরি, Auth identity তৈরি ও map
  (`V245_SETUP_STEPS_STHAYI.md`)।
- Android native module Android Studio-তে build ও device test।
- ফোন ও কম্পিউটারে live multi-phone test (`V245_LIVE_TEST_CHECKLIST.md`)।
