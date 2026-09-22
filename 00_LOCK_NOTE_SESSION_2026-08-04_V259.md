# V259 LOCK NOTE (B381–B389)

Base: V258 FINAL.

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না,
কোনো working flow খারাপ করা যাবে না, অ্যাপ স্লো করা যাবে না।

## এই ডেলিভারিতে যা হয়েছে (B381–B389, খাতায় বিস্তারিত)

TK স্ক্রিনশট দেখিয়ে CHECK-UP Queue-তে ১২৫ জন আটকে থাকার কারণ জিজ্ঞেস করলেন —
সেখান থেকে শুরু হয়ে বেশ কয়েকটা ধাপে কাজ হলো:

- **B381/B382:** Doctor Checkup-এ "Agree for Treatment" ছাড়া অন্য সিদ্ধান্তেও এখন
  checkup শেষ হলে রোগী Queue থেকে সরে যান (আগে চিরকাল আটকে থাকতেন) — ফোন ও
  কম্পিউটার দুটোতেই। সাথে সেই রোগীর ব্রাঞ্চের স্টাফের ঘন্টায় নোটিশ যায়।
- **B383:** Queue কার্ডের "Report Card" বোতাম এখন শুধু Advance/বিল-থাকা রোগীর
  জন্য সক্রিয় (bill > 0), নতুন রোগীতে ধূসর — Take Action-এর প্রমাণিত একই নিয়ম।
- **B384/B385:** Queue-এর "Journey" বোতামের নাম "History" — এখন পূর্ণ চিকিৎসা-
  ইতিহাস খোলে, আর দ্বিতীয়বার+ আসা রোগীর জন্য উপরে Treatment Summary
  (Disease/Since/Previous Treatment/Previous Result), কল হিস্ট্রি নিচে (মোছা হয়নি)।
- **B386/B387:** কম্পিউটারেও একই কাজ — এবং TK-এর সাথে কয়েক দফা ডিজাইন-প্রুফ
  আলোচনার পরে ("লক" অনুমোদিত) একটা সম্পূর্ণ নতুন, প্রফেশনাল ডেস্কটপ-লুক
  Patient History পাতা (`patientHistoryDesktop()`) তৈরি হলো — শুধু "History"
  বোতাম এটা খোলে, "Action" বোতাম/পুরনো `summary()` অপরিবর্তিত।
- **B388:** TK-এর নির্দেশে সব কাজ দ্বিতীয়বার যাচাই করে ৩টা আসল বাগ ধরে ঠিক করা
  হলো (Duration/Treatment Duration গুলিয়ে যাওয়া, "checkup" লেখার বেমানানতা, আর
  সবচেয়ে গুরুত্বপূর্ণ — History টেবিলের Note কলাম esc() ছাড়া কাঁচা বসছিল,
  নিরাপত্তার আসল ঝুঁকি ছিল)।
- **B389:** ৯ জন স্টাফের Joining Date-এর SQL — TK নিজে চালিয়ে স্ক্রিনশটে ফলাফল
  নিশ্চিত করেছেন। ফোন ও কম্পিউটারের `join_date` ফিল্ডের ফরম্যাট-দ্বন্দ্ব
  (DOT বনাম HTML date input-এর ISO) TK-কে জানিয়ে, TK-এর সিদ্ধান্তে ISO বেছে
  নেওয়া হয়েছে।

## অপরিবর্তিত (নিশ্চিত)

- Login/Enquiry/Registration/Staff Profile-এর অন্য কোনো অংশ, ডিজাইন, salary/
  password/permission — কিছুই ছোঁয়া হয়নি।
- `⚡ Action` বোতাম ও পুরনো `summary()` (ওয়েব) এক অক্ষরও বদলায়নি।
- Kishanganj-এর বাংলা-বন্ধ নিয়ম অক্ষত — নতুন Treatment Summary-র লেখা ইংরেজিতে।

## ফাইল বদলেছে

`DoctorCheckupActivity.kt` · `DoctorQueueModel.kt` · `DoctorQueueRepository.kt` ·
`DoctorQueueAdapter.kt` · `DoctorQueueActivity.kt` · `item_queue_card.xml` ·
`PatientTimelineActivity.kt` · `app.js` · `styles.css` ·
`04_SUPABASE_DATABASE_SETUP/PATCH_2026-08-04_staff_join_dates.sql` ·
`build.gradle.kts` (versionCode 258→259, versionName 2.58→2.59)।

⛔ Staff join_date-এর SQL ছাড়া আর কোনো SQL লাগেনি (আগে থেকে থাকা `bill` কলামই
ব্যবহার হয়েছে)।

## যাচাই

ব্র্যাকেট-প্যারেন গোনা (Kotlin-সচেতন) ✅ পাশ · `node --check app.js` ✅ ·
Node-এ আসল `app.js` লোড করে বাস্তব ডেটা দিয়ে `patientHistoryDesktop()` সত্যিই
চালিয়ে HTML যাচাই (দুই অবস্থাতেই, XSS-ধরনের ইনপুট দিয়েও) · প্রতিটা Kotlin ফাইল
হাতে দ্বিতীয়বার পড়ে ফাংশন সিগনেচার/ওভারলোড মিলিয়ে দেখা হয়েছে ·
`00_GUARD/tk_guard.py --release` **সব ✅ পাশ**।

🔴 **TK-এর লাইভ টেস্ট বাকি — বিশেষ করে B381/B382 (checkup decision দিয়ে Queue
থেকে সরা ও স্টাফের ঘন্টা), B385 (Treatment Summary), আর B387-এর কম্পিউটারের
নতুন History পাতা।** B389 (Joining Date) ইতিমধ্যে TK-এর স্ক্রিনশটে নিশ্চিত।
