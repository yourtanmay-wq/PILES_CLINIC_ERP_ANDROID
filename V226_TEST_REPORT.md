# V226 — Test report (সৎ)

## যা এই পরিবেশে সত্যিই করা হয়েছে
- **Isolated logic test — `monthOf()` (item 86):** পৃথক পরিবেশে হুবহু একই যুক্তি চালিয়ে যাচাই করা হয়েছে —
  - `2026-08-01`, `2026-08-01T10:30:00Z`, `2026-08` → সব `2026-08` (আগের মতোই)।
  - `01.08.2026`, `01/08/2026`, `31.12.2026` → সঠিক `2026-08` / `2026-12` (আগে ভুল ছিল)।
  - সব `yyyy-MM-dd`/ISO ইনপুটে **নতুন ফল = পুরনো `take(7)` ফল** (backward-compatible) — নিশ্চিত।
  - ফল: **ALL EXPECTED**।
- **File-diff / hash যাচাই:** V225→V226-এ ঠিক ৪টি app file বদলেছে (build.gradle, ReportsRepository, ২টি index.html) — বাস্তব `diff -rq` ও SHA-256 দিয়ে নিশ্চিত। এর বাইরে কিছু বদলায়নি।
- **Rollback যাচাই:** `ROLLBACK_V226`-এর প্রতিটি কপি rollback==V225 (সঠিক base) ও rollback≠V226 (কার্যকর) — যাচাই করা।
- **Web parity:** Netlify `index.html` ও Android-embedded `assets/www/index.html` byte-identical (একই hash `86b069db…`)।

## যা এই পরিবেশে করা যায়নি (সৎভাবে দাবি করা হচ্ছে না)
- **Android compile / `assembleDebug` build:** ❌ **করা যায়নি।** এই cloud sandbox-এ **Android SDK নেই** এবং `dl.google.com` / `services.gradle.org` **network-blocked (HTTP 403)** — তাই SDK/AGP download বা build অসম্ভব। (JDK 21 ও Gradle 8.14.3 আছে, কিন্তু SDK ছাড়া Android build হয় না।) **কোনো APK/AAB/BUILD SUCCESSFUL দাবি করা হচ্ছে না।** owner Android Studio-তে (SDK যেখানে আছে) `assembleDebug` চালিয়ে নিশ্চিত করবেন।
- **Kotlin/XML পূর্ণ static compile:** ❌ Android classpath ছাড়া সম্পূর্ণ compile সম্ভব নয়। `monthOf` পরিবর্তন pure Kotlin/stdlib (নতুন import নেই) — logic-test পাস, কিন্তু build owner-এর মেশিনে।
- **Physical device UI / real-device test:** ❌ device নেই।
- **Live Supabase test:** ❌ live DB নেই; কোনো SQL চালানো হয়নি (শুধু read-only ফাইল দেওয়া)।

## গুরুত্বপূর্ণ database নোট
`V226_..._READ_ONLY_CHECKS.sql` ও আগের `V224_..._official_patient_id_unique.sql` **auto-run হয় না**। আগে backup নিয়ে, read-only checks চালিয়ে ফল দেখে, তবেই owner সিদ্ধান্ত নেবেন। Existing duplicate থাকলে আগে তালিকা মিলিয়ে ঠিক করতে হবে; আন্দাজে Patient ID বদলানো নিষেধ।

## উপসংহার
V226-এ প্রয়োগ করা পরিবর্তনগুলো (version bump + `monthOf` robustness) নিরাপদ ও logic-যাচাইকৃত, কিন্তু **Android build এখানে সম্ভব না হওয়ায় এটিকে build-verified FINAL বলা হচ্ছে না।** owner build + device + live-DB যাচাই করবেন — সেই অংশগুলো `V226_74_ITEM_STATUS.md`-তে সৎভাবে চিহ্নিত।
