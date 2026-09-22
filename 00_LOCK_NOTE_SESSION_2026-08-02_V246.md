# 🔒 LOCK NOTE — সেশন 02.08.2026 — V246 (নিরাপত্তা ত্রুটি সম্পূর্ণ ঠিক)

আগের: **V245 (245)** → এখন: **V246 (versionCode 246, versionName 2.46)**।
Base: `PILES_CLINIC_APP_V245_FINAL_2.zip` (git baseline রাখা হয়েছে)।

## 🛑 যে গুরুতর ত্রুটি ঠিক হয়েছে
আগের `hr.map_identity()` SECURITY DEFINER হলেও যে কেউ (authenticated) সেটা ডেকে
নিজেকে **Master** map করতে পারত। এখন:
- পুরনো `map_identity()` **সম্পূর্ণ মুছে ফেলা হয়েছে**।
- `hr.app_identity`-তে সাধারণ ইউজারের কোনো write policy নেই — কেউ সরাসরি নিজের row
  বদলে master হতে পারে না (RLS আটকায়)।
- নতুন `hr.admin_set_identity()` — ভিতরে `if not hr.is_master() then raise` — তাই
  **শুধু আগে থেকে Master হলে** কেউ identity যোগ/বদল করতে পারে।
- প্রথম Master শুধু SQL (service role) দিয়ে তৈরি হয় — কোনো callable path দিয়ে
  সাধারণ ইউজার Master হতে পারে না।

আসল Postgres-এ প্রমাণিত: staff self-promote → **ব্লকড** ("Only Master…"); direct
insert/update → **RLS ব্লকড**; master সব দেখে; staff শুধু নিজেরটা; anon কিছুই না;
দ্বিতীয়বার run → duplicate নেই।

## এক-রান SQL
`04_SUPABASE_DATABASE_SETUP/V246_ONE_RUN_SETUP_2026-08-02.sql` — একবার copy-paste
run করলেই: schemas hr/wn/fin + tables + RLS + security fix + ১৫টি secure identity
(Master + ৯ staff + ৪ doctor + ১ field, auto-mapped) + hr-private/fin-private bucket
+ storage policy + schema expose। Idempotent। temp password `Change#2026` (Master
পরে বদলাবেন)। Branch-login কখনো identity পায়নি।

## Module কোড (Android + Web — মূল source-এ, staging নয়)
- Android: `.../modules/` — ModuleAuth · ModuleUi · StaffProfileActivity ·
  WorkNotebookActivity · IncomeExpenseActivity + additive hooks (Dashboard card,
  MoreMenu My Profile, CallChooser tap, Manifest 3 activity)। কোনো `.kt.txt`/staging নেই।
- Web: `module_core.js · profile.js · notebook.js · finance.js` + additive hooks।

## ⛔ পুরোনো কিছু বদলায়নি
Patient/Payment/Refund/Follow-up/Briefing/Login/Print/Sync/Search/Design/Dashboard/
Logic/Permission/সতর্কবার্তা/Guard/Rollback/.git/Netlify asset — সব অক্ষত। কোনো
পুরোনো public table বদলানো হয়নি, পুরোনো টেবিলে RLS চালু হয়নি।

## যাচাই
- Guard `--release` — সব পাশ (bracket/XML/column/বাংলা/version V246/২৩ lock rule)।
- Web `node --check` পাশ; SQL আসল Postgres-এ নিরাপত্তা+RLS+idempotency প্রমাণিত।

## সৎ ঘোষণা
- APK আমি cloud-এ build করিনি — আপনি Android Studio-তে build করবেন।
- SQL section 8 Supabase-এর auth টেবিলে লেখে; সাধারণ project-এ চলে, restricted হলে
  শুধু ঐ block error দেবে (fallback নোটে লেখা)। আন্দাজে/ঝুঁকিপূর্ণ কিছু বসানো হয়নি।
