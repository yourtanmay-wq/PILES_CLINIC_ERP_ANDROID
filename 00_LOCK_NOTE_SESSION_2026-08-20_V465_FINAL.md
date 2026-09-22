# V465 LOCK NOTE — 20.08.2026 (V453 থেকে V465 পর্যন্ত সম্পূর্ণ সেশন)

## এই ডেলিভারিতে যা আছে (তারিখ-সময়সহ বিস্তারিত: 00_TK_KAJER_TARIKH_SOMOY_LOG.md)

1. ModuleAuth session persist/reuse fix + JWT reAuth বাগ-ফিক্স (V453, V465)
2. Password Center — ২২ জনের individual password (TK SQL চালিয়েছেন ✅)
3. KNE-KISHAN5→KNE-KISHAN6 — Android+Web
4. Dr. K.H MANDAL cross-branch checkup — যাচাই করা, আগে থেকেই কাজ করত
5. JPE-CRP cross-branch (Falakata+Birpara, টাকা বাদে) — Android+Web
6. Delta-fetch ("শুধু বদলানো অংশ নামুক"):
   - Doctor Queue (V454, TK লাইভ টেস্ট করে ✅ পাশ)
   - Follow-up Inquiry ট্যাব (V456)
   - Follow-up Patient/Treatment ট্যাব (V457, টাকা-সংশ্লিষ্ট, সবচেয়ে সাবধানে)
   - Chamber Attendance (আজকের/খোলা বোর্ড, V462)
   - BackgroundRefreshWorker prewarm (V458)
7. Auto-refresh (Android): Payment (V455), Monthly Collection (V458)
8. Web live-refresh: Payment (V463), CHECK-UP Queue (V464) — আগে-থেকে-
   প্রমাণিত cloud-pull ফাংশন পুনর্ব্যবহার করে, নতুন ঝুঁকিপূর্ণ কোড নেই
9. Bug fixes: "EDITED BY null" (V456), BriefingAdapter null (V460),
   CollectionListActivity বাংলা-টেক্সট (V460)
10. Backdate Payment Grant: RLS-missing বাগ (TK SQL চালিয়েছেন ✅),
    duplicate-warning পপ-আপ (V459)
11. backuprecords legacy payload — যাচাই করে "সমস্যা নেই" নিশ্চিত

## যা কখনো ছোঁয়া হয়নি (TK-এর অনুমতি ছাড়া বদলানো যাবে না)

- Payment/Bill/Advance branch-lock (`MoneyBranchGuard.kt` Android,
  `searchCanSeeFinance()` Web) — সম্পূর্ণ অক্ষত, বাইট-বাই-বাইট যাচাই করা
- `PaymentRepository.kt`, `PaymentModel.kt`, `RegistrationActivity.kt`,
  `PatientTimelineActivity.kt`, `GlobalSearchActivity.kt`,
  `DeletePermission.kt` — সম্পূর্ণ অক্ষত
- ডিজাইন, রং, বোতাম, ওয়ার্কফ্লো, RLS/DB স্কিমা (নতুন SQL ছাড়া)

## এখনো বাকি (ভবিষ্যতের কাজ, হ্যান্ডওভার নোটে বিস্তারিত)

- Chamber Attendance-এর বন্ধ-হওয়া দিনের বোর্ড/Close-workflow delta
- Web-এর বাকি সব পাতা (Follow-up/Enquiry/Registration) — এখনো live-refresh নেই
- Repository-স্তরের বাকি ~৪৫০ জায়গার তুলনা-লজিক বাগ অডিট
- একটা অনিশ্চিত RLS-lead (payment_backdate_requests ইত্যাদি) — লাইভ
  SQL দিয়ে TK নিজে যাচাই করতে পারেন (লগে SQL দেওয়া আছে)

## সততার সাথে সীমাবদ্ধতা

- এই ডেলিভারি-পরিবেশে ইন্টারনেট/DNS ব্লকড থাকায় **কোনো Android Gradle
  build বা লাইভ ডিভাইস/ব্রাউজার টেস্ট করা যায়নি।** যাচাই শুধু কোড-স্তরে:
  brace/paren-balance, `node --check`, প্রজেক্টের নিজস্ব `tk_guard.py`
  (সাধারণ + release মোড) — সবই PASS।
- **Android Studio-তে build ও প্রতিটা ফিচারের লাইভ টেস্ট TK/ডিভাইসেই
  করতে হবে** — এই ZIP পাঠানোর মুহূর্ত পর্যন্ত একটাও ফিচার লাইভ ফোনে/
  ব্রাউজারে প্রমাণিত হয়নি।
- সেশনের মাঝে একটা রিগ্রেশন (JWT expired আটকে থাকা) নিজেই তৈরি হয়েছিল ও
  নিজেই ধরে ঠিক করা হয়েছে (V465) — সততার সাথে স্বীকার করা হয়েছে, লুকানো
  হয়নি।

সম্পূর্ণ তারিখ-সময়সহ বিস্তারিত: `00_TK_KAJER_TARIKH_SOMOY_LOG.md`-এর
২০.০৮.২০২৬-এর সব V453-V465 এন্ট্রি দেখুন।
