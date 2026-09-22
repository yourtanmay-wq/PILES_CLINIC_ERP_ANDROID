# 🔒 LOCK NOTE — V193 (৩০.০৭.২০২৬ রাত) · খাতার সারি **B179**

> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

**ভার্সন:** `versionCode 193` · `versionName 1.93` · পর্দায় **V193**
⛔ **কোনো SQL লাগবে না।**

---

## TK-এর অনুমতি

সারি B174-এর বাকি অংশে (Chamber/Global Search/Doctor Queue-তে address/age/sex লোড না হওয়া) TK জিজ্ঞেস করেছিলেন বোঝানোর পরে বলেন — **"জায়গাতেও ঠিক করতে চাই।"**

## যা করা হলো

### ১) `ChamberAttendanceActivity.showClinicalMenu()`

patientId খোঁজার **সেই একই অনুরোধেই** কলাম বাড়ানো হলো (`id` → `id,address,age,sex`) — **তাই এখানে বাড়তি কোনো ক্লাউড-কলই লাগেনি।** আগে ভাবা হয়েছিল নতুন কল লাগবে, কিন্তু যেহেতু এখানে আগে থেকেই patients টেবিলে একটা কল হচ্ছিল, শুধু তার select-কলাম বাড়ানোই যথেষ্ট হলো।

### ২) `GlobalSearchActivity.openClinicalDoc()`

`SearchHit`-এ এই তথ্য নেই, তাই এখানে সত্যিই একটা **নতুন ছোট, সরু ক্লাউড-কল** লাগল। ফাংশনটা `lifecycleScope.launch { }`-এ মোড়ানো হলো (আগে সরাসরি ছিল)।

### ৩) `DoctorQueueActivity.openClinical()` → `ClinicalModulesActivity`

একই রকম একটা নতুন সরু ক্লাউড-কল দিয়ে address/age/sex এনে **নতুন তিনটে Intent extra** (`EXTRA_PATIENT_ADDRESS`/`EXTRA_PATIENT_AGE`/`EXTRA_PATIENT_SEX` — RoleSession.kt-এ আগে থেকেই সংজ্ঞায়িত ধ্রুবক, এতদিন অব্যবহৃত ছিল) দিয়ে পাঠানো হচ্ছে। `ClinicalModulesActivity.onCreate()`-এ এই তিনটে extra এখন পড়ে `RoleSession.applyFrom()`-এ পাঠানো হচ্ছে।

### নতুন শেয়ার্ড ফাংশন

`AddressTagRepository.fetchDemographics(mobile: String): Triple<String,String,String>` — মোবাইল ধরে একটাই সরু (`address,age,sex` শুধু) অনুরোধ। ব্যর্থ হলে খালি মান ফেরত — কোনো পর্দা ভাঙে না, শুধু ওই তিনটে ঘর আগের মতোই ফাঁকা থাকে।

## ⛔ যা ছোঁয়া হয়নি

- Patient ID (সারি B175) · মেডিসিন কম্বাইন (B176) · পিকার ব্যাক-নেভিগেশন (B177) — কিছুই ছোঁয়া হয়নি।
- অন্য কোনো ডিজাইন বা কাজের নিয়ম বদলায়নি।

## যাচাই (কাজের পরে আবার, TK-এর নিয়ম)

- নিজের হাতে ব্র্যাকেট গোনা — **৫টা ফাইলেই পাশ**
- পাহারাদার `tk_guard.py` — **১৭টা যাচাই, সব পাশ**
- আগের অনুমোদিত কাজের যাচাই — **৫৯/৫৯ পাশ**
- ভার্সন চার জায়গায় এক — ZIP · `versionCode 193` · `versionName 1.93` · পর্দায় **V193**

**ফাইল:** `AddressTagRepository.kt` (নতুন ফাংশন) · `ChamberAttendanceActivity.kt` · `GlobalSearchActivity.kt` · `DoctorQueueActivity.kt` · `ClinicalModulesActivity.kt` · `build.gradle.kts` · `DashboardActivity.kt` · `app.js` (শুধু ভার্সনের লেখা)

---

## 🔴 যা এখনো বাকি

এই সেশনের সব কোডের কাজ এখন শেষ। বাকি শুধু:

- **B148 — RLS** (⛔ TK-এর অনুমতি ছাড়া নিষেধ)
- `03_NETLIFY_READY` Netlify-তে আপলোড — শুধু TK-ই করতে পারেন
- TK-এর নিজের লাইভ টেস্ট — V180 থেকে V193 পর্যন্ত সবকিছু
