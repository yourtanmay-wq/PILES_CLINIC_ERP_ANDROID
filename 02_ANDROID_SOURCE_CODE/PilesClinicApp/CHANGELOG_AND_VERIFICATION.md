# TK Biswas Piles Clinic — Native Android Project
## CHANGE LOG & VERIFICATION STATUS (সততার সাথে, অনুমান না করে)

======================================================================
## এই প্রজেক্ট কী

একটা **native Android WebView-shell app** — মানে:
- একটা ছোট Kotlin `MainActivity` যেটা একটা পূর্ণ-স্ক্রিন WebView খোলে
- সেই WebView-এর ভেতরে তোমার **আসল, অপরিবর্তিত** ERP (app.js/styles.css/
  index.html/config.js — phase37 থেকে হুবহু, একটা লাইনও বদলানো হয়নি)
  `assets/www/` ফোল্ডারে বান্ডিল করা আছে, LOCAL ফাইল হিসেবে চলে (internet
  ছাড়াই খোলে, ঠিক এখন যেমন browser-এ চলে)
- Camera/Gallery photo picker, Android Back বাটন, Supabase-এর জন্য Internet
  permission — সব যোগ করা হয়েছে

এটা genuinely একটা **native app** — .apk হিসেবে build হবে, নিজস্ব icon নিয়ে
install হবে, Play Store-এ publish করা যাবে ভবিষ্যতে।

======================================================================
## ✅ VERIFIED (আমি নিজে সত্যিই যাচাই করেছি)

- সব XML ফাইল (AndroidManifest.xml, layout, values, xml/file_paths) —
  Python-এর XML parser দিয়ে সরাসরি parse করে **well-formed XML** নিশ্চিত
- MainActivity.kt — bracket/paren balance check করা হয়েছে (basic sanity,
  সম্পূর্ণ compile-check না)
- Directory structure — standard Android Studio Gradle project layout মেনে
  তৈরি (app/src/main/java, res, assets ইত্যাদি)
- Bundled web app (assets/www/) — এটাই তোমার approved phase37 zip-এর
  হুবহু কপি, business logic-এ কিছুই বদলায়নি
- App icon — তোমার আসল logo (icon-512.png) থেকে সবগুলো প্রয়োজনীয় mipmap
  সাইজ (48/72/96/144/192px) সঠিকভাবে generate করা হয়েছে

======================================================================
## ❌ UNVERIFIED (আমি sandbox-এ যাচাই করতে পারিনি — sandbox-এ Android
   Studio/Gradle/Android SDK নেই, internet download-ও বন্ধ)

- **Kotlin কোড সত্যিই compile হবে কিনা** — না
- **Gradle sync সফল হবে কিনা** — না
- **APK সত্যিই build হবে কিনা** — না
- **App emulator/real device-এ চালু হবে কিনা** — না
- **Camera/Gallery picker বাস্তবে কাজ করবে কিনা** — না
- **gradle-wrapper.jar (বাইনারি ফাইল) আমি তৈরি করতে পারিনি** — এটা মিসিং।
  Android Studio-তে প্রথমবার প্রজেক্ট খুললে এটা সাধারণত নিজে থেকেই
  regenerate করে (File → Sync Project with Gradle Files), অথবা যদি
  কম্পিউটারে আলাদা Gradle install করা থাকে তাহলে টার্মিনালে
  `gradle wrapper` একবার চালালেই এটা তৈরি হয়ে যাবে।

======================================================================
## পরবর্তী ধাপ (একজন real Android Studio ব্যবহারকারীর জন্য)

1. এই zip unzip করে পুরো `PilesClinicApp` ফোল্ডার Android Studio-তে
   **Open** করুন (File → Open → ফোল্ডার বেছে নিন)
2. Android Studio "Sync Now" দেখালে ক্লিক করুন (এটা gradle-wrapper.jar
   নিজে থেকে ডাউনলোড/তৈরি করে নেবে)
3. যদি কোনো compile error দেখা যায়, সেটা ঠিক করা লাগবে (আমি এই ধাপ
   sandbox-এ করতে পারিনি)
4. Sync সফল হলে ▶ Run চাপুন (emulator বা USB-connected ফোনে)
5. সফল হলে Build → Generate Signed Bundle/APK দিয়ে সরাসরি APK ফাইল
   বানিয়ে WhatsApp-এ পাঠানো যাবে

======================================================================
## সততার সাথে চূড়ান্ত কথা

আমি এই প্রজেক্ট **best-effort হিসেবে, standard, সুপরিচিত Android
patterns মেনে** বানিয়েছি — কিন্তু **"Complete" বা "Ready to Run" বলছি না**,
কারণ আমি নিজে build করে দেখাতে পারিনি। এটা একজন real Android Studio
ব্যবহারকারীর হাতে প্রথম compile attempt-এর জন্য একটা শক্ত ভিত্তি, চূড়ান্ত
নিশ্চয়তা না।
