# V215_MANUAL_SETUP_IF_REQUIRED.md — যে এক-বারের manual কাজ বাকি

⛔ **সততা:** নিচের কাজগুলো এই cloud session-এ করা **সম্ভব নয়** (আসল Supabase dashboard/Firebase console/ফোন লাগে)। তাই এগুলো "done" বলা হয়নি — TK/dev-কে একবার হাতে করতে হবে। কোনটা না করলে কী চালু হবে না, তা-ও লেখা।

## ১. Netlify security header enforce করা (§4) — সহজ
- `03_NETLIFY_READY/_headers` deploy folder-এর root-এ আছে; Netlify নিজে থেকেই পড়বে। এখন CSP **Report-Only** — কিছু block করে না।
- কাজ: site deploy করে কয়েকদিন ব্যবহার করুন, browser Console-এ CSP violation দেখুন। কোনো দরকারি জিনিস block না হলে `_headers`-এ `Content-Security-Policy-Report-Only` লাইনটার নাম বদলে `Content-Security-Policy` করুন → তখন enforce হবে।
- না করলে: বাকি সব header কাজ করবে, শুধু CSP enforce হবে না (report হবে)।

## ২. Password hashing + Supabase Auth + RLS (§4) — বড়, ধাপে ধাপে
**কেন manual:** এখন সব read/write anon key দিয়ে, login = plaintext string ম্যাচ। RLS এখনই চালু করলে live app বন্ধ। তাই ক্রম:
1. **DB প্রস্তুত (হয়ে গেছে):** `V215_SAFE_MIGRATION` PART A `password_hash`/`password_algo` কলাম যোগ করেছে।
2. **হ্যাশ বসানো:** প্রতিটা `usercredentials` row-এর `password`→PBKDF2/bcrypt hash করে `password_hash`-এ লিখুন (একবারের migration script; plaintext `password` এখনই মুছবেন না)।
3. **App login বদলানো:** Android `LoginActivity`/`CloudPasswordCheck` ও web `app.js` — hash যাচাই করবে (এই সেশনে **করা হয়নি**, কারণ hash বসার আগে বদলালে login ভাঙবে)। hash বসে গেলে তবেই।
4. **Supabase Auth:** প্রতিটা staff/doctor/master-এর জন্য Auth user (email/phone) — যাতে JWT-তে role/branch claim থাকে।
5. **RLS enable:** `V215_SAFE_MIGRATION` PART C-এর policy (JWT role/branch ধরে) — সব app Auth-এ সরার **পরে**। এর আগে নয়।
6. তারপর plaintext `password` কলাম ও source-এর hardcoded password map (`StaffDirectory.kt:68-73`, `config.js:39`) মুছুন।
- না করলে: app আগের মতোই চলবে, কিন্তু §4-এর security ফাঁক (plaintext password, RLS-off, anon-key দিয়ে সরাসরি DB access) **থেকে যাবে**। এটা সবচেয়ে জরুরি বকেয়া।

## ৩. §15 সত্যিকারের instant push (FCM) — ঐচ্ছিক
- এখন §15 notification **near-realtime** (~১৫ মিনিট background cadence, রাত ১০টা-৬টা বন্ধ), সাউন্ড+ভাইব্রেশন+background সহ — কিন্তু **তাৎক্ষণিক (instant) নয়**।
- সত্যিকারের instant চাইলে Firebase Cloud Messaging লাগবে: (a) Firebase project + `google-services.json`, (b) `com.google.gms.google-services` plugin + FCM dependency, (c) একটা `FirebaseMessagingService`, (d) Supabase-এ নতুন briefing এলে FCM পাঠানোর একটা Edge Function/trigger।
- এই সেশনে FCM কোড বসানো **হয়নি**। তাই "Notification Fully Working (instant)" **দাবি করা হচ্ছে না** (§15.10 মেনে)। দরকার হলে আলাদা সেশনে source সহ প্রস্তুত করা যাবে।

## ৪. §13 Refund feature বসানো
- DB কলাম প্রস্তুত (PART A)। UI+repository+approval কোড `V215_REFUND_AND_LOADING_SPEC.md`-তে ধাপে ধাপে দেওয়া — money-path বলে testing সহ বসাতে হবে (এই সেশনে untested কোড live payment-এ বসানো হয়নি)।

## ৫. Signed APK (§20.10)
- Release signing key এই repo-তে নেই। তাই **signed APK তৈরি হয়েছে বলে দাবি করা হচ্ছে না**। TK-এর keystore দিয়ে Android Studio-তে signed build করতে হবে।
