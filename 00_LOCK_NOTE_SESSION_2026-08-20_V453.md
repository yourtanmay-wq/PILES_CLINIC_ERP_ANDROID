# V453 LOCK NOTE — 20.08.2026

## এই ডেলিভারিতে যা চূড়ান্ত (TK-অনুমোদিত, ছাড়া বদলানো যাবে না)

1. **Supabase Free Plan ঝুঁকি অডিট** — Egress 82% (4.103/5GB) সংকট চিহ্নিত।
   ModuleAuth session persist/reuse fix (Android-only)।
2. **backuprecords legacy payload** — যাচাই করে দেখা গেছে সমস্যা নেই
   (শুধু নতুন backup-এর payload আছে, আগে থেকেই সুরক্ষিত)। SQL দেওয়া হয়েছে
   (`V453_BACKUPRECORDS_LEGACY_PAYLOAD_TRIM_2026-08-20.sql`), TK চালিয়েছেন।
3. **Password Center** — ২২ জনের individual password `usercredentials`-এ
   বসানো হয়েছে (TK নিজে SQL চালিয়েছেন, ফলাফল 22 rows, ফটো-প্রুফ দেখা হয়েছে)।
   Master: `Tkbiswas@002200`। বাকি সবার প্যাটার্ন: `<Surname>@<মোবাইলের
   প্রথম ৪ সংখ্যা>`।
4. **KNE-KISHAN5 → KNE-KISHAN6** — পুরনো নম্বর (6207841890)
   দিয়ে আর লগইন হবে না, পুরনো রেকর্ড অক্ষত। নতুন নম্বর ।
   Android (`StaffDirectory.kt`) ও Web (`config.js`) দুটোতেই।
5. **Dr. K.H MANDAL cross-branch checkup** — আগে থেকেই কোডে ছিল (V456
   Android, V461 Web), শুধু যাচাই করে নিশ্চিত করা হয়েছে, নতুন কাজ লাগেনি।
6. **JPE-CRP cross-branch (Falakata + Birpara)** — Enquiry/Visit/Patient
   দেখা ও Edit — **টাকা/Payment বাদে** (MoneyBranchGuard/searchCanSeeFinance
   সম্পূর্ণ অক্ষত, উভয় প্ল্যাটফর্মে)। Android **ও** Web দুটোতেই করা হয়েছে।

## যা কখনো বদলানো হয়নি (TK-এর অনুমতি ছাড়া বদলানো যাবে না)

- Payment/Bill/Advance branch-lock (`MoneyBranchGuard.kt` Android,
  `searchCanSeeFinance()` Web) — শুধু নিজের ব্রাঞ্চ, Master ছাড়া কেউ না।
- অন্য ব্রাঞ্চের রেকর্ড Delete — এখনো শুধু Master।
- ডিজাইন, রং, বোতাম, ওয়ার্কফ্লো, RLS/DB স্কিমা।

## সততার সাথে সীমাবদ্ধতা (TK-কে জানানো জরুরি)

- এই ডেলিভারি-পরিবেশে ইন্টারনেট/DNS ব্লকড থাকায় **Android Gradle build
  বা কোনো লাইভ ডিভাইস/ব্রাউজার টেস্ট করা যায়নি।** যাচাই শুধু কোড-স্তরে:
  brace/paren-balance (Kotlin) ও `node --check` (Web JS) — দুটোই PASS।
  **Android Studio-তে build ও লাইভ টেস্ট TK/ডিভাইসেই করতে হবে।**
- এই কারণে "পরের সেশনে নিশ্চিত কার্যকরী হবে" — এই নিশ্চয়তা কোড-স্তরের
  যাচাই পর্যন্তই সীমাবদ্ধ, প্রকৃত ডিভাইস-টেস্ট পর্যন্ত না।

## এই সেশনে ছোঁয়া ফাইল (তারিখ ২০.০৮.২০২৬)

| ফাইল | কাজ |
|---|---|
| `modules/ModuleAuth.kt` | Session persist/reuse fix (Android) |
| `native/MoreMenuActivity.kt` | Logout session-clear |
| `native/StaffDirectory.kt` | KISHAN5→KISHAN6 (Android) |
| `native/CrossBranchStaffAccess.kt` | নতুন — JPE-CRP exception (Android) |
| `native/FollowUpRepository.kt` | multi-branch filter (Android) |
| `native/FollowUpActivity.kt` | effectiveBranch() (Android) |
| `03_NETLIFY_READY/app.js` | JPE-CRP exception (Web) |
| `03_NETLIFY_READY/config.js` | KISHAN5→KISHAN6 (Web) |
| `03_NETLIFY_READY/index.html` | cache token bump |
| `03_NETLIFY_READY/version.json` | V453/4.53 |
| `app/build.gradle.kts` | V453/4.53 |
| `04_SUPABASE_DATABASE_SETUP/V453_BACKUPRECORDS_LEGACY_PAYLOAD_TRIM_2026-08-20.sql` | নতুন SQL |

সম্পূর্ণ তারিখ-সময়সহ বিস্তারিত: `00_TK_KAJER_TARIKH_SOMOY_LOG.md`-এর
২০.০৮.২০২৬-এর সব V453 এন্ট্রি দেখুন।
