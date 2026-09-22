# V220_WORK_LOG.md

**Base:** PILES_CLINIC_APP_V219_FINAL (219/2.19) → **V220 (220/2.20)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।
**নির্দেশ:** ১, ২, ৪ কোড-এ করা; ৩ শুধু আবার সঠিক audit। কোনো Design/Layout/Colour/Button/Permission/Branch/Payment rule বদলানো হয়নি; Diet Chart ছোঁয়া হয়নি; untested-কে "Pass" বলা হয়নি; Android build/device/live test আমি করিনি (TK করবেন)।

## STEP 0 — Rollback
`ROLLBACK_V219/` — V219 source-এর **আসল** কপি (9.3M; refundIdFor=2 যাচাই করা)।

## STEP 1 — Fix (audit করে, তারপর শুধু নির্দিষ্ট পরিবর্তন; প্রতিটার পর static check)

| # | কাজ | কী করা হলো | File |
|---|-----|-----------|------|
| ১ | HTTP 400-এর আসল ভুল Field | ব্যর্থ (4xx) লেখায় PostgREST-এর error **body** (code·message·details) পড়ে ~200 অক্ষরে `httpReason`-এ যোগ — §4-এর সতর্কবার্তায় Table·Record-এর সঙ্গে **কোন column/constraint ভুল** দেখা যায়। নতুন request নেই, data বদলায় না | `native/SupabaseClient.kt` (errSummary + upsert/updateById/deleteById ব্যর্থ-শাখা) |
| ২ | একই ভুল বারবার পাঠানো বন্ধ (retry অটুট) | permanent 4xx park-এ `permanent`+`permBodyHash` রাখা; `remember()`-এ একই ভুল body আবার এলে pending-এ যোগ **না** (parked থাকে), body বদলালে (record ঠিক) parked মুছে আবার retry। ⛔ retry বন্ধ নয় — নেট-ব্যর্থ/সংশোধিত/"পাঠান" আগের মতোই | `native/CloudWriteQueue.kt` (bodyHash + remember + flush park + withFailedAdded) |
| ৪ | Refund double বন্ধ + বৈধ দুই refund আলাদা | Refund ফর্ম **একবার খোলা**র জন্য nonce — একই ফর্মের retry একই id (double নয়), **নতুন ফর্মে** করা বৈধ দ্বিতীয় Refund আলাদা id | `native/PaymentActivity.kt`, `native/PaymentRepository.kt`, `native/PaymentModel.kt` · web `03_NETLIFY_READY/app.js` (+ parity `assets/www/app.js`) |
| — | version + cache-buster | 219→220 / 2.19→2.20; web v219→v220 | build.gradle.kts, index.html×2 |

**Static:** পরিবর্তিত ৫ Kotlin file bracket-balance PASS; Netlify + assets/www app.js `node --check` PASS ও diff = IDENTICAL (§5 parity অটুট)।

## STEP 2 — §3 শুধু re-audit (কোড-এ নয়)
বিস্তারিত: **`V220_SECTION3_BACKUP_RESTORE_REAUDIT.md`**। সারাংশ: মূল ফাঁক Trash-Restore (Android/web) ও stale queued-UPSERT; DB `BEFORE UPDATE updatedAt`-trigger ঠিক পথ, কিন্তু আগে NULL-updatedAt, createdAt-as-updatedAt backfill (৪ লাইন), legacy SyncManager — এই ৩টা সামলাতে হবে; তাই আলাদা version-এ live-backup+এক-টেবিল টেস্ট করে করা উচিত। **এই version-এ §3 কোড-এ করা হয়নি।**

## Pending (§10 — সৎভাবে)
- Android Gradle build / device / দুই-ফোন / live Supabase test — এই পরিবেশে সম্ভব নয়, তাই Pending (V220_TEST_REPORT)।
- §3 — শুধু re-audit; আপনার অনুমতির পরে V221-এ ঝুঁকিহীনভাবে।
