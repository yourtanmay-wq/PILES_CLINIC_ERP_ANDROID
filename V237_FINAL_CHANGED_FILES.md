# V237 — FINAL Changed Files + Proof

**Base:** **V236** (সর্বশেষ completed — Chamber Date-এর ৩টি fix সহ)।
**এই version = V236 + ৭টি নতুন কাজ।** কোনো কাজ বাদ যায়নি, V236-এর কিছু হারায়নি।

**Version নোট:** deliverable "V237 FINAL", তাই `versionCode=237 / versionName=2.37` (কাজ-৭-এ "235" লেখা ছিল যখন base ভুলবশত V235 ধরা হয়েছিল; base এখন V236 → সঠিক পরবর্তী V237)। বলুন যদি 235 চান — এক লাইনে বদলে দেব।

**সততা (environment সীমা):** এই cloud-এ **Python নেই → `tk_guard.py` চালানো হয়নি**; **Android SDK নেই → build হয়নি**; **device নেই → Vivo/Samsung live-test হয়নি**। সব proof static/code-level। "Guard PASS / Build / device-tested" দাবি করা হচ্ছে না।

---

## V236 থেকে যা বহাল আছে (অপরিবর্তিত, merge-এ অক্ষত)
Chamber Date-এর ৩ fix — `ChamberAttendanceActivity.kt` (Print থেকে ফিরে ভুল "Nobody has arrived yet" সরানো), `ChamberAttendanceRepository.kt` (Treatment Progress-এ auto-label লুকানো), `PaymentRepository.kt` (টাকা নিলে আসল progress আর মুছবে না)। এই ৩ ফাইল V237-এ **আর ছোঁয়া হয়নি** — V236-এর মতোই।

## এই version-এ যোগ হওয়া ৭টি কাজ (V236-এর সঙ্গে non-overlapping ফাইলে)

- **কাজ-১ Report Card Single-A4:** `native/ReportCardPrinter.kt` — `td.pr` থেকে `-webkit-line-clamp:2`+`overflow:hidden` সরানো (লেখা কাটা নয়, পূর্ণ wrap); pagination-এ multi-page লুপের বদলে **fit-to-one-A4 scale** (`fitScale=min(widthScale,heightScale)`) → সবসময় ১ A4, কিছুই কাটা যায় না। ৪ column/২০ row/square photo/AGE-SEX এক লাইন/DUE box অপরিবর্তিত।
- **কাজ-২ Primary/Alt Mobile:** `V237_FINAL_SQL_RUN.sql` (PART A idempotent `add column if not exists "altMobile"`) + guard-visibility-এ `PILES_CLINIC_DB_SETUP.sql`-এ altMobile; `native/PatientModel.kt` — altMobile শুধু non-blank ও ≠Primary হলে পাঠায় (column না থাকলেও 400/crash নয়)। Search+dup দুই নম্বরে, dedup — আগেই wired।
- **কাজ-৩ Call Chooser:** `AndroidManifest.xml <queries>`-এ `tel:` (ACTION_DIAL+ACTION_VIEW) যোগ → Android 11+ (Vivo/Samsung)-এ সব dialer chooser-এ দেখা যায় (default সরাসরি খোলে না)। ১১টি native call-site আগেই `CallChooser`-এ।
- **কাজ-৪ WhatsApp Chooser:** `native/WhatsAppMessageChooser.kt` — একটি installed থাকলেও silent default নয় (Personal/Business selection dialog); ৬টি bypass site routed (`DoctorVisitActivity`,`FollowUpActivity`,`DraftListActivity`,`GlobalSearchActivity`,`BriefingActivity`×2)। message text/language/order (WhatsApp→Later→SMS)/SMS অপরিবর্তিত।
- **কাজ-৫ Address:** কোড আগেই ঠিক (real-data-only fallback, কোনো ভুয়া/UNKNOWN নয়); read-only যাচাই `V237_FINAL_SQL_RUN.sql` PART C-তে।
- **কাজ-৬ Ghost Record:** native আগেই সম্পূর্ণ (tombstone/`deleted_records`/restore/localOnly); এখন **web-ও tombstone পড়ে** — `03_NETLIFY_READY/app.js`(+mirror) `wlv1WebNotDeleted` (প্রতি pull-এ ১ bulk অনুরোধে deleted_records নামিয়ে merge-এর পরে বাদ, fail-open)।
- **কাজ-৭ Version:** `build.gradle.kts` `versionCode=237`/`versionName="2.37"`; web cachebuster `?v=v237` (দুই copy); Dashboard label dynamic (→V237); folder `PILES_CLINIC_APP_V237`।

---

## পরিবর্তিত file (V236-এর উপরে ১৬টি + ১ নতুন SQL)
build.gradle.kts · AndroidManifest.xml · native/ReportCardPrinter.kt · native/WhatsAppMessageChooser.kt · native/DoctorVisitActivity.kt · native/FollowUpActivity.kt · native/DraftListActivity.kt · native/GlobalSearchActivity.kt · native/BriefingActivity.kt · native/PatientModel.kt · assets/www/app.js · assets/www/index.html · 03_NETLIFY_READY/app.js · 03_NETLIFY_READY/index.html · 04_…/PILES_CLINIC_DB_SETUP.sql · **নতুন** 04_…/V237_FINAL_SQL_RUN.sql
**Rollback:** `ROLLBACK_V237/` (এই ১৫ ফাইলের pre-change/V236-state কপি)। V236-এর নিজস্ব `ROLLBACK_V236/` ও `V236_CHANGED_FILES.md`-ও ZIP-এ বহাল।

## আপনাকে চালাতে হবে
- **একটি SQL:** `04_SUPABASE_DATABASE_SETUP/V237_FINAL_SQL_RUN.sql` (PART A একবার; PART B/C read-only)।
- `python3 00_GUARD/tk_guard.py` → PASS নিশ্চিত করুন। Android Studio build → APK → Vivo/Samsung-এ Call/WhatsApp chooser + Report Card single-A4 live-test।

## যা ছোঁয়া হয়নি
Payment/Refund/Follow-up/Login/Sync core, permission/branch rule, DeletedGuard native logic, SMS workflow, message text/language, V236-এর Chamber ৩ fix — সব অক্ষত। সব পরিবর্তন targeted+additive; কোনো broad refactor/redesign নয়।

## Test (static)
- Web `app.js` `node --check` **OK**; দুই app.js **byte-identical**।
- Kotlin brace-balance OK; guard locked-strings (B154) অক্ষত; AndroidManifest wellformed; altMobile setup.sql-এ (৯.৭ pass হবে)।
