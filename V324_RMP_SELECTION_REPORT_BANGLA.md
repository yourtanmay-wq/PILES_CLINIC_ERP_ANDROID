# V324 — Registration-এ Saved RMP নির্বাচন

তারিখ: 12.08.2026

## মালিকের অনুমোদিত কাজ

- Registration-এ `Ref By → Dr. Visit` বাছলে `Select Saved RMP / Doctor` বোতাম দেখা যাবে।
- নাম, মোবাইল বা এলাকা লিখে ফোন/কম্পিউটারে আগে থেকে জমা থাকা RMP তালিকার মধ্যে Search হবে।
- RMP বাছলে নাম ও মোবাইল আগের দুই ঘরে নিজে থেকে বসবে।
- তালিকা না থাকলে আগের মতো হাতে নাম ও মোবাইল লেখা যাবে; Registration আটকাবে না।

## Supabase Free Plan সুরক্ষা

- এই Search থেকে Supabase-এ কোনো নতুন Query যায় না।
- প্রতিটি অক্ষর লেখার সময় কোনো Cloud Call হয় না।
- Android-এ Doctor Visit-এর বর্তমান phone cache এবং Web-এ বর্তমান local cache ব্যবহার হয়।
- নতুন Table, SQL, ছবি, Call History বা কমিশনের ইতিহাস নামানো হয়নি।

## পরিবর্তিত ফাইল

1. `RegistrationActivity.kt`
2. `activity_registration.xml`
3. `03_NETLIFY_READY/app.js`
4. `03_NETLIFY_READY/index.html` — শুধু নতুন app.js cache version
5. `app/build.gradle.kts` — নতুন আলাদা V324 version

## যা বদলায়নি

- Registration Save, Patient ID, Fee, Photo, Branch, Disease, Payment বা Follow-up নিয়ম।
- RMP Commission/Referral Income-এর বর্তমান হিসাব ও অনুমোদন নিয়ম।
- অন্য কোনো Android/Web screen বা design।
- Supabase SQL/Database।

## যাচাই

- Registration XML ঠিক আছে।
- Web JavaScript syntax ঠিক আছে।
- নতুন Search অংশে Supabase/Cloud Call নেই।
- পুরোনো ও নতুন ফাইল মিলিয়ে দেখা হয়েছে: শুধু উপরে লেখা নির্দিষ্ট ফাইল বদলেছে।
- Android SDK/Gradle dependency এই পরিবেশে নেই; তাই চূড়ান্ত Android Build ও আসল ফোনের Live Test বাকি।
