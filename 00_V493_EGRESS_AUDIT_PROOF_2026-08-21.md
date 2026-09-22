# 📊 V493 — Supabase Egress: যাচাই, কাজ ও প্রমাণ

**তারিখ:** ২১.০৮.২০২৬ · রাত ~২:০০ (ভারতীয় সময়)
**ভিত্তি:** V492 · **নতুন সংস্করণ:** V493 / 4.93

---

## ০. TK-এর রিপোর্ট

> *"Supabase-এর লাইভ লগে patients, followups, payments ও enquiries টেবিলের বড়
> তালিকা কাছাকাছি সময়ে বারবার — কখনো একই অনুরোধ দুবার — নামার প্রমাণ পাওয়া গেছে।"*

**এই রিপোর্ট সঠিক।** কোড ধরে খুঁজে কারণ ও প্রমাণ দুটোই পাওয়া গেছে।

---

## ১. আগে কোন function থেকে কোন table কতবার পড়ছিল

### যন্ত্রে গোনা মোট হিসাব (V492)

| | সংখ্যা |
|---|---|
| অ্যাপে মোট Supabase Read কল | **২৬২** |
| তার মধ্যে বড় পড়া (limit ≥ ৫০০, delta নয়) | **৮৪** |

বড় পড়াগুলো টেবিল ধরে: `payments` ১৭ · `patients` ১৬ · `followups` ১০ ·
`enquiries` ১০ · `doctor_visits` ৭ · `briefings` ৬ · `medical` ৪

### 🔴 আসল ডুপ্লিকেট — Follow-up পর্দা (সবচেয়ে বেশি ব্যবহৃত)

`FollowUpActivity` একবার খুললে **দুটো আলাদা পথ** একই কাজ করে:

| পথ | লাইন | কী করে |
|---|---|---|
| `refreshTabCounts()` | 608–610 | `fetchTab` × ৩ ট্যাব (`async`, একসাথে) |
| `loadTodayAllSections()` | 437–439 | `fetchTab` × ৩ ট্যাব (`async`, একসাথে) |

⇒ এক পর্দা খুললেই **`fetchTab` ৬ বার** চলে।

আর `fetchTab()`-এর ভিতরে এই তিনটে পড়া **ট্যাবের উপর নির্ভরই করে না** —
অর্থাৎ ছয়বারই **হুবহু একই URL**:

| # | পড়া | ঘর | সারি |
|---|---|---|---|
| ১ | `fetchListOrNull("enquiries", "stage=eq.Inquiry", 5000)` | **`select=*`** | ৫০০০ |
| ২ | `fetchListSlimOrNull("enquiries", "status=neq.Active", 5000, …)` | সরু | ৫০০০ |
| ৩ | `fetchListOrNull("payments", branchScope, 5000, PAYMENT_COLS_LIST)` | সরু | ৫০০০ |

**প্রমাণ যে ট্যাব-নিরপেক্ষ:** ছাঁকনিতে `stage` চলক নেই; `payments`-এর ছাঁকনি
`branchScopeFilterPlain(branchFilter)` — শুধু ব্রাঞ্চের উপর নির্ভর করে
(`FollowUpRepository.kt:805`), ট্যাবের উপর নয়।

### একই তথ্য আরও যারা চাইত (একে অপরের কথা না জেনে)

| জায়গা | কী | কতবার |
|---|---|---|
| `DialerRepository.dialerLists` :156-158 | `fetchTab` × ৩ | Dialer খুললেই |
| `DashboardActivity` :467 | `fetchTab` | Dashboard-এ |
| `FollowCalendarActivity` :174 | `fetchTab` | ক্যালেন্ডারে |
| `ChamberAttendanceRepository` | `fetchTab` × ৩ | চেম্বার বোর্ডে |
| `CallReminderWorker` :83 | `fetchTab` × ৩ | **দিনে ৩ বার, প্রতি ফোনে** |
| `LiveRefresh` | বদল-খোঁজা | চলাকালীন |

`CloudReadCache` আগে থেকেই ছিল, কিন্তু তা **মাত্র ১৩ জায়গায় হাতে বসানো** —
বাকি ২৪৯টা পড়া সরাসরি নেটে যেত, আর তার TTL ছিল মাত্র **২০ সেকেন্ড**।

### 🖼️ ছবি নামার পথ

| জায়গা | সমস্যা |
|---|---|
| `SupabaseClient.fetchListSlimOrNull` শেষ ধাপ | সরু পড়া ব্যর্থ ⇒ `select=*` ⇒ **`patients.photo` · `followups.photo` · `medical.photos`** |
| `SupabaseClient.fetchListSlim` শেষ ধাপ | একই |
| ওয়েব `wlv1FollowUpCloudPull` (app.js:850) | **প্রতি ৪৫ সেকেন্ডে** `select('*')` — গত ৩ ঘণ্টায় বদলানো সব সারি **ছবি-সহ** |
| ওয়েব `wlv1FetchPaged` fallback | ব্যর্থ ⇒ `select('*')` ⇒ ছবি |
| ওয়েব realtime chunk (app.js:1185/1192) | ব্যর্থ ⇒ `select('*')` ⇒ ছবি |

---

## ২. পরে কতবার এবং কী filter/column দিয়ে পড়বে

### ক) `CloudReadDedupe` — একই অনুরোধ দুবার নয় *(নতুন ফাইল)*

বসানো হয়েছে **একদম নিচের স্তরে** — `SupabaseClient.fetchListOrNull`-এর ভিতরে।
তাই অ্যাপের **২৬২টা পড়াই** এর ভিতর দিয়ে যায়, অথচ **কোনো পর্দার কোড বদলাতে হয়নি**।

- **একসঙ্গে (in-flight):** হুবহু একই URL এখনই নেটে থাকলে দ্বিতীয়জন অপেক্ষা করে।
- **সদ্য শেষ (TTL ৬০ সেকেন্ড):** একই URL আবার চাইলে সদ্য পাওয়া উত্তরটাই।

**Follow-up পর্দা একবার খোলার হিসাব:**

| পড়া | আগে | পরে |
|---|---|---|
| `enquiries` (stage=eq.Inquiry, 5000) | **৬ বার** | **১ বার** |
| `enquiries` (status=neq.Active, 5000) | **৬ বার** | **১ বার** |
| `payments` (branch scope, 5000) | **৬ বার** | **১ বার** |

৬০ সেকেন্ডের মধ্যে Dialer / Dashboard / ক্যালেন্ডার খুললে সেগুলোর জন্য
**অতিরিক্ত একটাও অনুরোধ যায় না**।

### খ) `SafeWideColumns` — শেষ চেষ্টাতেও ছবি নয় *(নতুন ফাইল)*

`select=*`-এর বদলে: **ওই টেবিলের সব ঘর − ভারী ঘর + ডাকার জায়গা যা চেয়েছিল**

| টেবিল | যে ভারী ঘর বাদ যায় |
|---|---|
| `patients` | `photo` · `medicalHistory` |
| `followups` | `photo` · `history` |
| `medical` | `photos` · `details` |
| `doctor_visits` | `callHistory` · `referralPayments` |
| `payments` | `editHistory` |

**"+ ডাকার জায়গা যা চেয়েছিল" — এটাই আসল সুরক্ষা।** কেউ ইচ্ছে করে ভারী ঘর
চাইলে (Prescription-এর `details`, RMP-র `callHistory`) সেটা কখনো বাদ পড়ে না।

### গ) `CallReminderWorker` — দিনে ৩ বারের পূর্ণ পড়া বন্ধ

`fetchTab()` → `fetchTabDelta()` (V457 থেকে `BackgroundRefreshWorker`-এ প্রমাণিত)।
ব্যর্থ/প্রথমবার হলে নিজে থেকেই পূর্ণ পড়ায় ফিরে যায়।

### ঘ) ওয়েব — একই নিয়ম

| জায়গা | আগে | পরে |
|---|---|---|
| ৪৫-সেকেন্ডের pull | `select('*')` (ছবি-সহ) | `RT_NO_PHOTO_COLS` |
| `wlv1FetchPaged` fallback | সরাসরি `'*'` | আগে ছবি-ছাড়া পূর্ণ ঘর, **তারপরই** `'*'` |
| realtime chunk × ২ | সরাসরি `'*'` | আগে ছবি-ছাড়া আবার, **তারপরই** `'*'` |

**ছবি হারানোর ঝুঁকি নেই:** `mergeById` field-wise (`{...a,...r}`) — নতুন সারিতে
`photo` না থাকলে আগের জমানো ছবিই থেকে যায় (app.js:409)।

---

## ৩. Duplicate Read বন্ধ হওয়ার test proof

Kotlin কম্পাইলার এই পরিবেশে আনা যায়নি (দুটো উৎসই ব্লকড)। তাই যুক্তিটা **হুবহু
Java-তে লিখে সত্যিকারের থ্রেড দিয়ে** চালানো হয়েছে (`00_GUARD/dedupe_test/`)।

```
── ১. একসঙ্গে ৮টা একই অনুরোধ (Follow-up-এর ৩ ট্যাব × ২ পথ) ──
  ✅ নেটে গেল মাত্র একবার          →  network hit = 1 (আগে হত ৮)
  ✅ আটজনই একই উত্তর পেল           →  আলাদা উত্তর = 1

── ২. ৬০ সেকেন্ডের ভিতরে বারবার (onCreate→onResume→LiveRefresh) ──
  ✅ ৫ বার চাওয়া, নেটে একবার        →  network hit = 1
  ✅ ৬০ সেকেন্ড পেরোলে আবার নেটে যায় →  network hit = 2

── ৩. নিজে কিছু সেভ করার পরে (upsert → clear) ──
  ✅ সেভের পরে টাটকা তথ্যই আসে      →  network hit = 2

── ৪. নেট আটকালে (খাতার সারি B446) ──
  ✅ ব্যর্থ পড়া জমা হয় না           →  network hit = 2
  ✅ পরের চেষ্টায় আসল টাকা ফেরে     →  ১ম=null ২য়=[{"amount":400}]
  ✅ ব্যর্থতা cache-এ ঢোকেনি        →  জমা = 1

── ৫. আলাদা অনুরোধ আলাদাই থাকে ──
  ✅ দুটো আলাদা URL = দুটো পড়া      →  network hit = 2

── ৬. মেমরির সীমা ──
  ✅ ৮ MB-র বেশি জমে না             →  জমা = 2 টি (৩ MB × ৪ চাওয়া হয়েছিল)

পাশ: 16   ব্যর্থ: 0
```

**৪ নম্বর পরীক্ষাটা সবচেয়ে জরুরি** — খাতার সারি **B446** ("নেট আটকালে
Collection Summary ₹0 দেখাত") ফিরে আসেনি, তার সরাসরি প্রমাণ।

---

## ৪. রোগীর ছবি অকারণে নামছে না — proof

`00_GUARD/verify_no_photo_and_dedupe.py` (নতুন পাহারাদার):

```
  PATIENT_COLS_NO_PHOTO          40 টি ঘর, ছবি নেই ✅
  FOLLOWUP_COLS_NO_PHOTO         25 টি ঘর, ছবি নেই ✅
  ENQUIRY_COLS_DRAFT             13 টি ঘর, ছবি নেই ✅
  FOLLOWUP_COLS_CHAMBER_BOARD     8 টি ঘর, ছবি নেই ✅
  PAYMENT_COLS_LIST              21 টি ঘর, ছবি নেই ✅
  সরু-পড়া ব্যর্থতার পথ SafeWideColumns দিয়ে ঢাকা: 2 টি ✅
  fetchListOrNull → CloudReadDedupe ✅
  প্রতিটা লেখার পরে দুটো cache-ই খালি হয়: 6 জায়গা ✅
  ওয়েবের ৪৫-সেকেন্ডের pull ছবি ছাড়া ✅
```

Java পরীক্ষায় সরাসরি:

```
  ✅ তালিকার পড়ায় photo নেই   → id,patientId,name,mobile,branch,address,bill,updatedAt
  ✅ medicalHistory-ও নেই
  ✅ চাওয়া ঘর সবই আছে
  ✅ কেউ ইচ্ছে করে photo চাইলে সেটা থাকে
  ✅ followups-এ photo ও history নেই
  ✅ অচেনা টেবিলে আগের আচরণ (select=* অপরিবর্তিত)
```

---

## ৫. পরিবর্তিত সব ফাইলের তালিকা

### নতুন (৪টি)
| ফাইল | কাজ |
|---|---|
| `…/native/CloudReadDedupe.kt` | একই অনুরোধ দুবার আটকানো |
| `…/native/SafeWideColumns.kt` | শেষ চেষ্টাতেও ছবি না নামা |
| `00_GUARD/verify_no_photo_and_dedupe.py` | নতুন পাহারাদার |
| `00_V493_EGRESS_AUDIT_PROOF_2026-08-21.md` | এই নথি |

### বদলানো (৪টি)
| ফাইল | কী বদলাল |
|---|---|
| `…/native/SupabaseClient.kt` | `fetchListOrNull` → dedupe · দুটো fallback → SafeWideColumns · ৬ জায়গায় `CloudReadDedupe.clear()` |
| `…/native/CallReminderWorker.kt` | `fetchTab` → `fetchTabDelta` |
| `03_NETLIFY_READY/app.js` | ৪ জায়গায় `select('*')` → ছবি-ছাড়া ঘর |
| `…/app/build.gradle.kts` · `version.json` | V493 / 4.93 |

---

## ৬. Web syntax ও Android build

| পরীক্ষা | ফল |
|---|---|
| `node --check app.js` | **PASS ✅** |
| Java যুক্তি-পরীক্ষা (১৬টি) | **PASS ✅** |
| Android Studio বিল্ড | ⚠️ **TK-কে করতে হবে** — এই পরিবেশে Android SDK ব্লকড |

**সৎ কথা:** এখানে Kotlin কম্পাইল করা যায় না। নতুন দুটো Kotlin ফাইলে কোনো
Android-নির্ভর API নেই (শুধু `java.util` ও Kotlin stdlib), আর সব পাহারাদার
পাশ করেছে — তবু **বিল্ডই চূড়ান্ত প্রমাণ**।

---

## ৭. পুরনো কাজ নষ্ট হয়নি — regression

| যা রক্ষা করা হয়েছে | কীভাবে |
|---|---|
| **B446** (নেট আটকালে ₹0) | ব্যর্থ পড়া কখনো জমা হয় না — Java পরীক্ষা #৪ |
| নিজের সেভ সঙ্গে সঙ্গে দেখা | প্রতি লেখায় `clear()` — পরীক্ষা #৩ |
| কেউ কারো তালিকা ছোঁয় না | জমা থাকে **কাঁচা লেখা**; প্রত্যেকে নিজের `JSONArray` বানায় |
| ভারী ঘর সত্যিই দরকার হলে | `SafeWideColumns` চাওয়া ঘর কখনো বাদ দেয় না |
| ওয়েবে জমানো ছবি | `mergeById` field-wise — ছবি মুছে না |
| কল-রিমাইন্ডারের সংখ্যা | `fetchTabDelta` ব্যর্থ হলে পূর্ণ পড়ায় ফেরত |
| অচেনা টেবিল | আচরণ হুবহু আগের মতো |
| tk_guard · version · rpc · নিষিদ্ধ API | সব PASS |

**ছোঁয়া হয়নি:** DB schema · RLS · role · branch permission · অনুমোদিত ডিজাইন ·
টাকার হিসাব · payment/refund · notification · কোনো তথ্য মোছা হয়নি ·
**এই কাজে একটাও নতুন SQL লাগেনি।**

---

## ৮. লক্ষ্য ও সিদ্ধান্ত

লক্ষ্য: দৈনিক **০.১০–০.১২ GB**। এখন ছিল **০.৩৮–০.৭০ GB**।

**"Free Plan-এ নিশ্চিত চলবে" — এটা লেখা হচ্ছে না।** কতটা কমবে তা নির্ভর করে
স্টাফরা দিনে কতবার কোন পর্দা খোলেন তার উপর, যা এখান থেকে মাপা যায় না।

**সিদ্ধান্ত হবে Deployment-এর ২৪–৪৮ ঘণ্টা পরে Supabase-এর চার্ট দেখে।**

লক্ষ্যে না পৌঁছালে পরের ধাপে যা বাকি আছে:
`DoctorVisitActivity`-র ৪টে পূর্ণ পড়া · `Reports`/`Draft`-এর পূর্ণ পড়া ·
`fetchTab`-এর `enquiries select=*` সরু করা · মাস্টারের Visit-Fee হিসাব।

⚠️ **মনে রাখতে হবে:** চলতি চক্রে (১৩ অগাস্ট থেকে) ইতিমধ্যেই ৪.৫৮ GB খরচ হয়ে
গেছে — সেটা ফেরত আসবে না। আসল ফল বোঝা যাবে **১৩ সেপ্টেম্বরের পরের চক্রে**।

*লেখা হলো ২১.০৮.২০২৬ · রাত ~২:০০ (ভারতীয় সময়)*
