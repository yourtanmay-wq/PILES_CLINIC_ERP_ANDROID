# 🔒 LOCK NOTE — V195 (৩০.০৭.২০২৬ বিকেল) · খাতার সারি **B182**

> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

**ভার্সন:** `versionCode 195` · `versionName 1.95` · পর্দায় **V195**
⛔ **কোনো SQL লাগবে না।**

---

## TK কী রিপোর্ট করেছেন

Android Studio-এর স্ক্রিনশট — **"Build PilesClinicApp: failed"**, `app:compileDebugKotlin 2 errors`, `DoctorVisitActivity.kt`-এ:
- "No value passed for parameter 'text'" — লাইন ১৯৯১
- "No value passed for parameter 'text'" — লাইন ২০০৫

## ⚠️ গুরুত্বপূর্ণ — এটা এই সেশনের কাজের ফল নয়

**`DoctorVisitActivity.kt` এই পুরো সেশনে (খাতার সারি B164–B181) একবারও ছোঁয়া হয়নি।** এটা প্রজেক্টের **আগে থেকে থাকা** (V179 বেসলাইনের) একটা টাইপো, যেটা **TK নিজে Android Studio-তে সত্যিকারের বিল্ড চালিয়ে ধরেছেন**। আমার হাতে-হাতে ব্র্যাকেট/API মেলানোর যাচাই আসল Kotlin কম্পাইলারের মতো প্যারামিটার-গোনা নিখুঁতভাবে ধরতে পারে না — তাই এই ভুলটা আগে ধরা পড়েনি। TK-এর নিজের বিল্ড-টেস্ট এখানে সত্যিকারের মূল্যবান যাচাই হিসেবে কাজ করেছে।

## আসল কারণ (কোড ধরে, আন্দাজ নয়)

`sendDoctorMessage(mobile: String, doctorName: String, text: String)` — **তিনটে** প্যারামিটার লাগে।

দুই জায়গায় (RMP-কে "Arrived" ও "Referral Paid" বার্তা পাঠানোর কোডে) মাঝের `item.name` (doctorName) বাদ পড়ে গিয়েছিল:

```kotlin
sendDoctorMessage(
    item.mobile,                    // ⛔ item.name বাদ পড়ে গিয়েছিল
    DoctorMessage.arrived(...)
)
```

Kotlin তখন `DoctorMessage.arrived(...)`-এর ফলাফলটাকে ভুলবশত `doctorName`-এর জায়গায় বসিয়ে দিত (যেহেতু সেটাও একটা String), আর আসল `text` প্যারামিটার সম্পূর্ণ ফাঁকাই থেকে যেত — কম্পাইল এরর।

## সমাধান

দুই জায়গাতেই মাঝে `item.name` যোগ করা হলো, ঠিক পাশের সঠিক কলের প্যাটার্ন মিলিয়ে (option 3-এর "Intro" বার্তার কল, যেখানে `sendDoctorMessage(item.mobile, item.name, DoctorMessage.intro(...))` — সঠিকভাবেই তিনটে আর্গুমেন্ট আছে):

```kotlin
sendDoctorMessage(
    item.mobile, item.name,
    DoctorMessage.arrived(...)
)
```

## ⛔ যা যাচাই করা হয়েছে, যা ছোঁয়া হয়নি

- এই একই ফাংশনের **বাকি সব কল** (এই ফাইলে মোট ৪টা) মিলিয়ে দেখা হয়েছে — আর কোথাও এই ভুল নেই।
- কোনো বার্তার লেখা, কোন ভাষায় (বাংলা/ইংরেজি/হিন্দি) পাঠানো হবে তার নিয়ম, RMP চেনার নিয়ম — কিছুই বদলায়নি। শুধু দুটো কলে ভুলে-বাদ-পড়া নামটা যোগ হয়েছে।

## যাচাই (কাজের পরে আবার, TK-এর নিয়ম)

- নিজের হাতে ব্র্যাকেট গোনা — **পাশ**
- পাহারাদার `tk_guard.py` — **১৭টা যাচাই, সব পাশ**
- আগের অনুমোদিত কাজের যাচাই — **৫৯/৫৯ পাশ**
- ভার্সন চার জায়গায় এক — ZIP · `versionCode 195` · `versionName 1.95` · পর্দায় **V195**

**ফাইল:** `DoctorVisitActivity.kt` · `build.gradle.kts` · `DashboardActivity.kt` · `app.js` (শুধু ভার্সনের লেখা)

---

## 🔴 যা এখনো বাকি

- TK-এর নিজের Android Studio-তে **আবার বিল্ড করে দেখা** — এবার সফল হওয়া উচিত।
- **B148 — RLS** (⛔ TK-এর অনুমতি ছাড়া নিষেধ) · `03_NETLIFY_READY` Netlify-তে আপলোড (V184) · সপ্তাহখানেক পরে Supabase-এর খরচ দেখা।
