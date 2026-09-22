# V219_WORK_LOG.md

**Base:** PILES_CLINIC_APP_V218_FINAL (218/2.18) → **V219 (219/2.19)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।
**নিয়ম:** আগে audit, তারপর শুধু নির্দিষ্ট ১০টা fix; কোনো Design/Layout/Colour/Button/Permission/Branch Rule/Workflow/approved feature বদলানো হয়নি; Diet Chart ছোঁয়া হয়নি; untested-কে "Pass" বলা হয়নি; অসম্পূর্ণ কাজ লুকানো হয়নি।
**Environment:** cloud container — আসল Android build/device/live Supabase test নেই। তাই static check; বাকিটা Pending।

## STEP 0 — Audit (কোড পড়ে সত্যতা যাচাই)
- দুটো Explore পাস: (A) web delete + parity, (B) queue-400 + auth + free-plan। প্রতিটা item file:line সহ যাচাই — নিচের fix সেই অনুযায়ী।
- **§3 Rollback:** ROLLBACK_V218/ তৈরি — V218 source-এর **আসল** কপি (9.0M, 192 kt file; নকল/একই কপি নয়)।

## STEP 1 — শুধু অনুমোদিত fix (প্রতিটার পর static check)

| # | কাজ | কী করা হলো | File |
|---|-----|-----------|------|
| 1 | Refund double-on-retry বন্ধ | random UUID → **deterministic id** (রোগী+টাকা+কারণ+তারিখ+যিনি করলেন)। আবার চাপলে upsert পুরোনো row-ই overwrite করে, দ্বিতীয় Refund হয় না। pending-সমষ্টি থেকে একই refund বাদ (retry না আটকায়)। web-এও একই (Java hashCode মিলিয়ে), local dedup + briefing dedup | Android: PaymentModel.kt, PaymentRepository.kt · Web: 03_NETLIFY_READY/app.js |
| 2 | Web Delete ঠিক Record ID ধরে | `wlv1DeleteDraftEntry` আর মোবাইল দিয়ে সারি খোঁজে না — **recId (id) দিয়ে**। caller enquiries হলে enquiry-id, patients হলে patient-id পাঠায় (visit/incomplete সারি followup, তাই patient p.id) | 03_NETLIFY_READY/app.js |
| 4 | আটকে থাকা HTTP 400 | (a) failed-এ সরানোর সময় আসল কারণ (`lastError`) আর মুছে যায় না; (b) HTTP 400/404/422 = permanent → ২ বার চেষ্টার পর সঙ্গে সঙ্গে "যায়নি" ঘরে (কারণসহ), ৫০ বার বৃথা নয় (নিরাপদ retry, কোটাও বাঁচে); (c) সতর্কবার্তায় **Table · Record · সহজ কারণ** (সর্বোচ্চ ৩) দেখানো | CloudWriteQueue.kt, PendingSyncStatus.kt |
| 5 | তিন copy মিলানো | native app = অনুমোদিত অ্যাপ (feature native-এ)। Android-এর ভিতরের web copy (assets/www) = পুরোনো ছিল → **Netlify web app-এর সঙ্গে হুবহু এক** করা হলো (app.js/config.js/index.html/styles.css), logo অক্ষত। assets/www runtime-এ চলে না (launcher=native LoginActivity), তাই regression নেই | assets/www/* |
| 6 | Auth/RLS/password নিরাপদ | আলাদা **copy-paste SQL file** — PART A নিরাপদ additive (কলাম, hash-বাকি খোঁজা, index); PART B RLS ও PART C plaintext-drop ইচ্ছে করে COMMENT (RLS এখন নয়)। ⛔ Login/Master Password Center কোড **অপরিবর্তিত** (PBKDF2 hashing V216 থেকেই আছে) | 04_.../V219_SECURITY_SQL_COPY_PASTE_2026-07-31.sql |
| 7 | Free-plan full-table কমানো | `briefings` পুরো টেবিল প্রতিবার (পর্দা+ঘন্টা) নামত → প্রজেক্টের নিজের `CloudReadCache` (২০s) দিয়ে dedupe। ⛔ **কোনো সারি বাদ যায় না** (একই সম্পূর্ণ তালিকা); ফাঁকা/ব্যর্থ কখনো cache হয় না (অসম্পূর্ণ হবে না) | BriefingRepository.kt |
| 8 | FCM ছাড়া "instant" দাবি নয় | কোথাও "Instant Notification সম্পূর্ণ" বলা হয়নি — near-realtime (~১৫ মিনিট) হিসেবেই লেখা; FCM ধাপ আগের README-তে | (doc) |
| — | version + cache-buster | 218→219 / 2.18→2.19; web ?v=v218/safefix1 → v219 | build.gradle.kts, index.html×2 |

**Static:** পরিবর্তিত সব Kotlin file bracket-balance PASS; Netlify + assets/www app.js `node --check` PASS; SQL copy-paste।

## যা করা হয়নি / Pending (সৎভাবে — §10)
- **Android Gradle build / device / দুই-ফোন / live Supabase test — চালানো হয়নি** (এই পরিবেশে সম্ভব নয়)। §1 refund idempotency, §2 delete-by-id, §4 400-flow, §7 cache — সব **device-এ যাচাই দরকার**।
- **§7 বাকি টেবিল** (Draft-এর ৪-৫ টেবিল, Trash, Queue open-time, Chamber followups) একই `CloudReadCache`/HEAD-gate দিয়ে কমানো যায় — ঝুঁকি/টেস্টের কারণে এই session-এ শুধু briefings করা হলো; বাকিগুলো V219_TEST_REPORT-এ সুপারিশ হিসেবে।
- **§6 RLS live / plaintext drop / Supabase Auth wiring** — manual, SQL-এ ধাপ দেওয়া; এই session-এ enable করা হয়নি (করলে live app বন্ধ)।

## নতুন সন্দেহ (লুকানো হয়নি)
- §7 briefings ২০s cache: staff Refund/Delete **request** পাঠালে master-এর Briefing পর্দায় সেটা ≤২০ সেকেন্ড দেরিতে দেখাতে পারে (তথ্য হারায় না, শুধু সামান্য দেরি) — প্রজেক্টের অন্য তালিকাও একই ২০s নিয়মে চলে। device-এ মিলিয়ে নেবেন।
- §1 deterministic refund id: একই রোগীর **হুবহু একই টাকা+কারণ+তারিখ+একই স্টাফ** দুটো *ইচ্ছাকৃত* আলাদা refund দিলে দ্বিতীয়টা প্রথমটাকে overwrite করবে (একটাই থাকবে)। বাস্তবে বিরল, আর এটাই double-refund ঠেকায়; দরকারে কারণ/টাকা একটু আলাদা দিলে দুটোই থাকবে।
