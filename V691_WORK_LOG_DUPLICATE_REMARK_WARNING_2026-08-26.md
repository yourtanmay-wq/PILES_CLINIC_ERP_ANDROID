# V691 — একই দিনে দ্বিতীয় Remark লিখলে সাবধানবাণী

**তারিখ:** ২৬.০৮.২০২৬ (IST)
**নির্দেশ:** TK — *"ডুপ্লিকেট Remarks লেখার সময় যেন Warning আসে ২য় ফটোর মত"*
(দুটো ছবিসহ পাঠানো)

---

## ১. TK যা দেখিয়েছেন

**১ম ছবি — দোষটা।** নম্বর **+919707360144** (UNKNOWN · COOCH BEHAR — PILES
DHUBRI), Enquiry Calls: 2 —

| তারিখ | সময় | কে | Remark |
|---|---|---|---|
| ২৬.০৮.২০২৬ | 8.58 AM | COB-4 | `1 SAPTAHO PRE ASBEN` |
| ২৬.০৮.২০২৬ | 8.57 AM | COB-4 | `1 SAPTAHO PRE ASBENI` |

এক মিনিটের ব্যবধানে একই স্টাফের প্রায়-এক দুটো Remark বসে গেছে। **লেখার
সময় কোনো সাবধানবাণী আসেনি** — চুপচাপ দুটোই জমা হয়ে গেছে।

**২য় ছবি — TK যা চান।** একটা পপ-আপ —

> ⚠️ **Same-day Remark**
> A remark has already been saved today.
> Previous Remark: 1 SAPTAHO PRE ASBENI
> What would you like to do?
> **[ Update Previous ]  [ Save New Remark ]  [ Cancel ]**

---

## ২. যা করা হলো

আজ এই রোগীর একটা Remark আগেই সেভ করা থাকলে, নতুন Remark-এ **Save** চাপলে
আর সোজা জমা হয় না — আগে উপরের সাবধানবাণীটা ওঠে। তিনটে পথই আছে:

| বোতাম | কী হয় |
|---|---|
| **Update Previous** | আজকের **আগের সারিটার লেখাই** বদলে যায় — ইতিহাসে নতুন সারি জমে না |
| **Save New Remark** | আগে যা হত **ঠিক তাই** — নতুন একটা সারি জমে |
| **Cancel** | কিছুই হয় না |

⛔ **আজ কিছু লেখা না থাকলে পপ-আপ ওঠেই না** — এক চাপেই আগের মতো সেভ হয়।
পুরনো অভ্যাসে কোনো বাড়তি ধাপ যোগ হয়নি।

### ফোন ও কম্পিউটার — দুটোতেই (নিয়ম §৬.৬)

| | ফাইল | ফাংশন |
|---|---|---|
| ফোন | `native/PatientTimelineActivity.kt` | `showSameDayRemarkWarning()` |
| কম্পিউটার | `03_NETLIFY_READY/app.js` | `wlv1SameDayRemarkWarn()` |

দুটোরই নিয়ম হুবহু এক — "আজকের **শেষ** Remark সারিটা" ধরা হয়, তার লেখাটাই
পপ-আপে দেখানো হয়।

---

## ৩. নেটের খরচ — শূন্য

⚠️ এই পাহারার জন্য **একটাও নতুন Supabase অনুরোধ যায় না।**
পর্দায় ইতিমধ্যে নামানো তালিকা থেকেই দেখা হয় —
ফোনে `currentEntries`, কম্পিউটারে `load('followups')`।
Egress-এর চাপ যেখানে চলছে, সেখানে এটা জরুরি ছিল।

---

## ৪. কল-গোনা (Signal) ছোঁয়া হয়নি

**Update Previous**-এ `callCount` বা `lastCallDate` বদলায় না — আজকের কল
আগেই একবার গোনা হয়ে গেছে, লেখা শুধরানো নতুন কল নয়।
**Save New Remark**-এ আগের নিয়মই চলে (repository-তে "দিনে একবার" de-dup
আগে থেকেই আছে — খাতার সারি B53)।

---

## ৫. পপ-আপের রং — ✅ TK-এর সিদ্ধান্ত হয়ে গেছে: **হলুদই থাকবে**

TK-এর ছবিতে হেডারটা **কমলা-লাল**। প্রজেক্টের সব পপ-আপের রং একটাই জায়গায়
লক করা (`PremiumAlert.kt` — *"DO NOT change these colours without TK's
permission"*), আর সেখানে এই ধরনের "দেখে নিন" সাবধানবাণীর জন্য বাঁধা রং
**হলুদ (caution)** — যেমন *"এই নম্বর আগেই সিস্টেমে আছে"* পপ-আপটা।

তাই নিজে থেকে নতুন কমলা রং বানানো হয়নি — **প্রজেক্টের লক করা হলুদ
caution হেডারই** ব্যবহার হলো। লেখা, তিনটে বোতাম, আর কাজ — সবই ছবির হুবহু।

✅ **TK-এর সিদ্ধান্ত (২৬.০৮.২০২৬):** *"হলুদ থাকলে অসুবিধা নেই"* — তাই
**হলুদই চূড়ান্ত**, আর বদলানোর দরকার নেই। ⛔ ভবিষ্যতে TK নিজে না বললে এই
রং বদলানো যাবে না।

---

## ৬. যাচাই

| যাচাই | ফল |
|---|---|
| `tk_guard.py --release` | ✅ মেশিনের সবকটা পাশ (ভার্সন V691 দুই জায়গাতেই মেলে) |
| `verify_kotlin_compile.py` | ✅ PASS — নতুন ভুল ০ |
| `node --check app.js` | ✅ পাশ |

### 🔴 নিজে ধরা একটা আসল বিল্ড-ভুল

প্রথমে ফাংশনের প্যারামিটারের নাম রেখেছিলাম `text`. কিন্তু ভিতরে
`TextView(this).apply { text = "…" }` লেখা ছিল — Kotlin তখন `text` বলতে
**প্যারামিটারটাই** ধরে (val), TextView-এর ঘরটা নয়। ফল: *"val cannot be
reassigned"* — Android Studio-তে বিল্ড ভাঙত।
`verify_kotlin_compile.py` এটা ধরেছে; নাম `newRemark` করে ঠিক করা হলো।

### ⛔ পাহারাদারের বেসলাইন হালনাগাদ (আলাদা কথা, TK জেনে রাখুন)

`00_GUARD/kotlin_noise_baseline.txt` V597-এর সময়ের ছিল। V690-এ আসা **নতুন
৬টা ফাইল** (CallRemarkActivity, DoctorReminderWorker, ExpectedTomorrowActivity,
DoctorCheckupActivity, InvestigationCategoryActivity, DashboardActivity) সেখানে
ছিল না, তাই পাহারাদার **প্রতিবারই ফেল** দেখাচ্ছিল — ৯টা লাইনে।

ওই ৯টা মিলিয়ে দেখা হয়েছে — সবকটাই **মিথ্যে অ্যালার্ম**: ফাইলগুলো
`AppCompatActivity` / `CoroutineWorker` থেকে আসে, আর পাহারাদারের কাছে
androidx-এর লাইব্রেরি নেই, তাই সে `intent`/`Result`/`Context` চিনতে পারে না।
পাহারাদারের নিজের কড়া গেট (*"আসল ভুল"*) বলছে **০**।

ফেল-ই থাকলে পাহারাদারটা অকেজো হয়ে যেত (আর একদিন সত্যিকারের ভুলও চাপা
পড়ত), তাই বেসলাইন হালনাগাদ করা হলো — ওই ৯টা লাইনই যোগ হয়েছে, **আমার
লেখা কোড থেকে একটাও নয়**।

---

## ৭. বদলানো ফাইল

```
02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/PatientTimelineActivity.kt
02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts      (V690 → V691)
03_NETLIFY_READY/app.js
03_NETLIFY_READY/version.json                                    (V690 → V691)
03_NETLIFY_READY/index.html                                      (app.js cache-buster)
00_GUARD/kotlin_noise_baseline.txt                               (উপরে §৬)
```

---

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি

V691 এখনো Android Studio-তে লাইভ টেস্ট হয়নি। দেখার কথা —

- [ ] একই রোগীর আজ **প্রথম** Remark → পপ-আপ **ওঠে না**, সোজা সেভ হয়
- [ ] **দ্বিতীয়** Remark → ⚠️ Same-day Remark পপ-আপ ওঠে, আগের লেখাটা দেখায়
- [ ] **Update Previous** → তালিকায় সারি **একটাই** থাকে, লেখা বদলায়
- [ ] **Save New Remark** → তালিকায় **দুটো** সারি (আগের আচরণ)
- [ ] **Cancel** → কিছুই বদলায় না
- [ ] কম্পিউটারেও (Add Remark → Save Remark) ঠিক একই পাঁচটা
