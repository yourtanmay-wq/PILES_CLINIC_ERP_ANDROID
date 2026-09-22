# 🔒 LOCK NOTE — V93 FINAL (2026-07-20, শেষ আপডেট)

**এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

কোড ও Lock Book-এর দ্বন্দ্বে → Lock Book চূড়ান্ত।

## এই সেশনে সম্পূর্ণ হওয়া কাজ
**ডিজাইন (প্রিভিউ দেখিয়ে TK-অনুমোদিত):**
1. CHECK-UP Queue কার্ড — বড় ফটো, নাম+মোবাইল, ৩ বোতাম (🧭 Journey · 🩺 Check-up · ⚡ Action); Action→Take Action মেনু; Summary/Print বাদ।
2. CHAMBER DATE — গ্রিড টেবিল, রঙে স্ট্যাটাস (সবুজ/হলুদ/লাল), Treatment Progress কলাম, SL শুধু প্রিন্টে, ডিফল্ট Arrived ফিল্টার, ৬টা কম্প্যাক্ট বক্স, Master branch চিপ।
3. Cash/Online ঘরে ট্যাপ → Treatment payment (লক + ৩-ট্যাপ edit, audit সহ)।
4. Treatment ঘরে ট্যাপ → লেখা (৯ চিপ + সবাই টাইপ)।
5. REVIEW পপআপ → গ্রিড টেবিল + ৩-ট্যাপ edit।
6. Expected তালিকায় "✅ এসেছেন" বোতাম।
7. Queue section হেডার — Today সবুজ / Overdue কমলা।

**ফিক্স:**
8. bottom bar সব স্ক্রিনে লুকানো (শুধু Dashboard-এর Menu থাকবে)।
9. spinner কোথাও ঘোরে না।
10. slow-net-এ callTimeout (25s)।
11. Delete→Trash id ফিক্স (Enquiry/Patient/Doctor সব)।
12. R unresolved build error ফিক্স।

সব কাজ সোর্সেই লেখে → সব সেকশনে অটো-আপডেট।

## সীমা (সৎ)
এখানে APK build হয় না — গঠন ঠিক, চূড়ান্ত compile/type নিশ্চয়তা শুধু Android Studio build-এ। TK build করে live-test করবেন, তারপর নির্দিষ্ট সমস্যা জানালে শুধু সেটাই ঠিক হবে।

বিস্তারিত: 00_PROJECT_STATE_MASTER_NOTE.md সেকশন ৯৬–১০৮।
