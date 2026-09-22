# V226 — Changed files (সত্যিকারের file-hash অনুযায়ী)

**Base:** V225 (=V224 working tree)। **তুলনা:** V225 → V226, বাস্তব byte-diff দিয়ে যাচাই।
**পরিবেশ নোট:** এই cloud-এ Android SDK নেই + Google blocked → `assembleDebug` **করা হয়নি**; তাই এটি build-verified `FINAL` নয় (নাম `V226_DRAFT_BUILD_PENDING`)।

## পরিবর্তিত application file (মাত্র ৪টি — এর বাইরে কিছু নয়)

| # | ফাইল | কী বদলেছে | নতুন SHA-256 |
|---|------|-----------|--------------|
| 1 | `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts` | `versionCode 225→226`, `versionName "2.25"→"2.26"` (App/Dashboard version BuildConfig থেকে auto-update) | `e7c0b29a…f6f654` |
| 2 | `…/native/ReportsRepository.kt` | item 86: `monthOf()` এখন `dd.MM.yyyy`/`dd/MM/yyyy`-ও সঠিক `yyyy-MM` বানায় (ISO ফল অপরিবর্তিত) | `eea5f426…8f0e0e` |
| 3 | `…/app/src/main/assets/www/index.html` | web asset version stamp `?v=v223→?v=v226` (Android-embedded web) | `86b069db…4a905b` |
| 4 | `03_NETLIFY_READY/index.html` | web asset version stamp `?v=v223→?v=v226` (Netlify web) — #3-এর সঙ্গে byte-identical | `86b069db…4a905b` |

## নতুন যোগ হওয়া ফাইল

| ফাইল | কী | SHA-256 |
|------|-----|---------|
| `04_SUPABASE_DATABASE_SETUP/V226_2026-08-01_READ_ONLY_CHECKS.sql` | read-only যাচাই SQL (index-exist, duplicate, orphan, 7777, row-count) — **কিছু চালানো হয়নি** | `7bfa7b82…1909e0` |
| `ROLLBACK_V226/…` | ৪টি পরিবর্তিত ফাইলের **সত্যিকারের pre-V226 (=V225) কপি** (কার্যকর rollback) | manifest দ্রষ্টব্য |
| `V226_74_ITEM_STATUS.md` | ৭৪ item-এর সৎ status | — |
| `V226_TRASH_BRANCH_FILTER_PATCH.md` | item 46 ready-to-apply patch | — |
| `V226_BEFORE_AFTER_DIFF.md`, `V226_DECLARATION.md`, `V226_TEST_REPORT.md`, `V226_FILE_MANIFEST_SHA256.json` | evidence | — |

## V225-এর ভুল claim সংশোধন (item 10 · 11)

- **V225 দাবি করেছিল** `ReportsRepository.kt` "conversion now uses Current Month for both counts" বদলেছে — **কিন্তু V224 ও V225-এর ঐ ফাইল byte-identical ছিল (কোনো বদল হয়নি)।** V226 সেই মিথ্যা claim **পুনরাবৃত্তি করে না**; V226-এ ঐ ফাইলে সত্যিকারের একটি বদল (item 86 `monthOf`) করা হয়েছে এবং তা উপরের হুবহু diff/hash দিয়ে দেখানো।
- **V225 rollback-এর `ReportsRepository.kt` current file-এর সঙ্গে হুবহু সমান ছিল (অকার্যকর)।** V226-এর `ROLLBACK_V226/` প্রতিটি পরিবর্তিত ফাইলের **আসল pre-V226 কপি** রাখে — যাচাই করে দেখানো হয়েছে rollback≠V226 (কার্যকর) ও rollback==V225 (সঠিক base)।

## যা বদলানো হয়নি
উপরের ৪টি ছাড়া কোনো application source, design, layout, colour, button, card, permission, payment হিসাব, print, login, branch rule, database rule, web logic বা asset বদলানো হয়নি। `.git`, ZIP history, website, SQL (আগেরগুলো), documentation — সব অক্ষত।
