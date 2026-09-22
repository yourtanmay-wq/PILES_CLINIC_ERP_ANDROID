# LOCK NOTE — V646 (25.08.2026)

## এই সেশনে যা হয়েছে (V626 → V646, Android+Web)
V626-V640: Anatomy fix, RMP call-ID, Statement, Sheet-entry, Chamber
branch-leak, Timeline নাম-fallback, Call Remark, বহু-ভিজিট patient
টাই-ব্রেকার, PDF Share WebView-attachment, Payment সিরিয়াল-নম্বর,
কিশানগঞ্জ বাংলা-ফাঁক (Doctor Check-up A4 রিপোর্ট), Draft-এ Running
Patient, ৬০-দিন Incomplete-নিয়ম দুই প্ল্যাটফর্মে এক।

V641-V646: Briefing/Notice Board প্রিমিয়াম+কমপ্যাক্ট ডিজাইন, Reply
করলে অনুরোধকারী নোটিফিকেশন পান, **Draft/Follow-up কার্ড সম্পূর্ণ
একীকরণ** (My Enquiry/Unexpected Time/Incomplete/Complete/Running
Patient — সবই এখন Follow-up-এর আসল কার্ড ব্যবহার করে, Payment/
Prescription-সহ পুরো লাইভ কাজ করে; My Enquiry বর্তমান অবস্থা
অনুযায়ী বদলায়; Enquiry Reject/Visit Reject/Refunded/Return Visit
অপরিবর্তিত), Follow-up-এর নিজের ট্যাবেও বাড়তি ফিল্টার-চিপ।

বিস্তারিত: `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর V626–V646 এন্ট্রি।

## ফাইনাল / TK-এর অনুমতি ছাড়া বদলানো যাবে না
সব পুরনো লক করা ডিজাইন অক্ষত। FollowUpActivity.kt-এর নিজস্ব
`buildFollowCard` কোডে (Android) এক অক্ষরও হাত পড়েনি — Draft-এর
কার্ড-একীকরণ একটা আলাদা, স্বাধীন `FollowUpAdapter` পুনর্ব্যবহার করে,
ঝুঁকি না নেওয়ার জন্য (TK-নির্দেশ)। একই কারণে Draft-এর নতুন কার্ডে
ট্রিপল-ট্যাপ এডিট (Status Menu/Call Signal/Edit/Photo) কাজ করে না —
শুধু Follow-up-এর নিজের পর্দায় (Android) বা Follow-up ট্যাব-ফিল্টার
হিসেবে (Web, যেখানে সবকিছুই কাজ করে) থাকে।

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি
V626–V646 এখনো Android Studio-তে লাইভ টেস্ট হয়নি।

## যাচাই
`tk_guard.py --release` পাশ · `node --check` app.js/finance.js পাশ ·
ভার্সন V646/6.46 (build.gradle.kts + version.json দুই জায়গাতেই মেলানো)
