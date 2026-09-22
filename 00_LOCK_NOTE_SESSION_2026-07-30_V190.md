# 🔒 LOCK NOTE — V190 (৩০.০৭.২০২৬ রাত) · খাতার সারি **B174**

> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

**ভার্সন:** `versionCode 190` · `versionName 1.90` · পর্দায় **V190**
⛔ **কোনো SQL লাগবে না।**

---

## TK কী রিপোর্ট করেছেন

ছবি-প্রুফসহ — NOOR ALAM-এর Patient Timeline-এ বয়স/লিঙ্গ/ঠিকানা স্পষ্ট আছে ("MALE-40", পুরো ঠিকানা "FARA BARI, BARBALLA, GOULPOKHAR, UTTAR DINAJPUR"), অথচ একই রোগীর Prescription ডকুমেন্টে Age/Sex/Address তিনটেই "-" দেখাচ্ছিল।

> *"পেশেন্ট এর বয়স, পেশেন্টের লিংগ, পেশেন্টের ঠিকানা — সবকিছু আছে কিন্তু প্রেসক্রিপশনে অটোফিল কেন হচ্ছে না।"*

---

## আসল কারণ (কোড ধরে, আন্দাজ নয়)

Prescription/Diet Chart ইত্যাদি খোলার সময় একটা ভাগাভাগি করা "সেশন" (`RoleSession`) ব্যবহার হয় — যেখানে রোগীর নাম, আইডি, ব্রাঞ্চ, মোবাইল, ঠিকানা, বয়স, লিঙ্গ, রোগ — এই সবকিছু সাময়িকভাবে জমা রাখা হয়, তারপর Prescription পর্দা সেখান থেকে পড়ে।

`RoleSession.applyFrom()` ফাংশনের নিয়ম হলো — **কোনো ঘরে ফাঁকা/`null` পাঠালে সেই ঘরটা বদলায় না, পুরনো মানই থেকে যায়**।

Timeline-এর "Take Action" মেনু থেকে Prescription খোলার সময় এই ফাংশনকে address/age/sex-এর জায়গায় সরাসরি **`null`** পাঠানো হত — অথচ এই তথ্য Timeline পর্দাতেই আগে থেকে ছিল (হেডারে "MALE-40" ও ঠিকানা দেখায়), শুধু আলাদা করে ধরে রাখা ও পাঠানো হত না।

---

## যা করা হলো (দুটো জায়গায়, বাড়তি কোনো ক্লাউড-কল ছাড়াই)

### ১) `PatientTimelineActivity.showClinicalDocumentMenu()`

এটাই TK-এর রিপোর্ট করা ঠিক পথ (স্ক্রিনশটের "pat_..." প্যাটার্নের আইডি মিলিয়ে দেখা)।

- নতুন তিনটে ঘর যোগ করা হলো: `currentPatientAge`, `currentPatientSex`, `currentPatientAddress`।
- ডেটা লোড হওয়ার সময় (`currentBranch = data.branch`-এর ঠিক পাশেই) এই তিনটেও ধরে রাখা হচ্ছে — `currentPatientAge = data.age` ইত্যাদি।
- `RoleSession.applyFrom()`-এ এখন `null, null, null`-এর বদলে `currentPatientAddress, currentPatientAge, currentPatientSex` পাঠানো হচ্ছে।

### ২) `FollowUpActivity.openClinicalDocForItem()`

Follow-up কার্ডের নিজের Prescription/Diet Chart/Blood Test অ্যাকশন — এখানেও একই বাগ। `item` (FollowUpItem)-এর নিজের `address`/`age`/`sex` ঘর আগে থেকেই ছিল, শুধু ব্যবহার করা হয়নি — খালি স্ট্রিং (`"", "", ""`) পাঠানো হত।

- এখন `item.address, item.age, item.sex` পাঠানো হচ্ছে (পাশের লাইনেই এই তথ্য ছিল)।

---

## ⛔ একই ধরনের বাগ প্রজেক্টে আরও কোথায় আছে খুঁজে দেখা হয়েছে

`RoleSession.applyFrom()`-এর **ছয়টা ডাকার জায়গার সবকটা মিলিয়ে দেখা হয়েছে** (আন্দাজে নয়)। আরও **তিনটে জায়গায় এই একই সমস্যা আছে**:

- `ChamberAttendanceActivity.showClinicalMenu()`
- `GlobalSearchActivity.openClinicalDoc()`
- `DoctorQueueActivity` → `ClinicalModulesActivity` (Intent-extra দিয়ে)

**কিন্তু এই তিনটেতে ঠিক করা হয়নি** — কারণ এখানে age/sex/address তথ্যটাই এখনো লোড করা হয় না (`ChamberAttendanceRow` ও `SearchHit` মডেলে এই ঘরগুলোই নেই)। ঠিক করতে হলে **প্রতিটাতে একটা করে নতুন ক্লাউড-কল** লাগবে (patients টেবিল থেকে আলাদা করে আনতে হবে)। ⛔ **এটা TK-এর অনুমতি ছাড়া করা হয়নি** (বাড়তি কোটার প্রশ্ন)।

## 📌 আরেকটা লক্ষ করা বিষয় (এখনই ঠিক করা হয়নি, শুধু জানানো হলো)

`RoleSession.applyFrom()`-এর "ফাঁকা পেলে পুরনো মান রাখো" নিয়মটা **নাম/ব্রাঞ্চ/রোগ**-এর ক্ষেত্রেও একই ভাবে কাজ করে — এটা প্রজেক্টের অনেক পুরনো নকশা। তাত্ত্বিকভাবে: এক রোগীর Prescription দেখার পরে, যদি আরেক রোগীর সত্যিই age/sex ফাঁকা থাকে, তাহলে আগের রোগীর পুরনো মান রয়ে যাওয়ার একটা ছোট ঝুঁকি থেকেই যায়। এটা আজ বদলানো হয়নি — বড় কাজ (এই ফাংশনের ছয়টা ব্যবহারকারী স্থানকেই প্রভাবিত করবে), TK-কে জানানো হলো।

## ⛔ যা ছোঁয়া হয়নি

কোনো ডিজাইন · টাকার হিসাব · SQL কিছুই লাগেনি। Prescription/Diet Chart/Medicine Slip-এর অন্য কোনো ফিল্ড বা নিয়ম বদলায়নি।

## যাচাই (কাজের পরে আবার, TK-এর নিয়ম)

- নিজের হাতে ব্র্যাকেট গোনা — **২টা ফাইলেই পাশ**
- পাহারাদার `tk_guard.py` — **১৭টা যাচাই, সব পাশ**
- আগের অনুমোদিত কাজের যাচাই — **৫৯/৫৯ পাশ**
- ভার্সন চার জায়গায় এক — ZIP · `versionCode 190` · `versionName 1.90` · পর্দায় **V190**

**ফাইল:** `PatientTimelineActivity.kt` · `FollowUpActivity.kt` · `build.gradle.kts` · `DashboardActivity.kt` · `app.js` (শুধু ভার্সনের লেখা)

---

## 🔴 যা এখনো বাকি

- TK-এর লাইভ টেস্ট — **NOOR ALAM-এর Prescription আবার প্রিন্ট করে দেখতে হবে** (Age/Sex/Address এখন ঠিক আসছে কিনা)।
- **সিদ্ধান্ত দরকার:** Chamber/Global Search/Doctor Queue-তেও এই ফিক্স চাই কিনা (নতুন ক্লাউড-কল লাগবে)।
- **B148 — RLS** (⛔ TK-এর অনুমতি ছাড়া নিষেধ) · `03_NETLIFY_READY` Netlify-তে আপলোড (V184) · সপ্তাহখানেক পরে Supabase-এর খরচ দেখা।
