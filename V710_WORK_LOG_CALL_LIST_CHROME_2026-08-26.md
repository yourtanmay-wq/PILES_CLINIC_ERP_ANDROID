# V710 — "আজকের কল" তালিকায় ট্যাব ও ফিল্টার থাকবে না
**তারিখ:** ২৬.০৮.২০২৬ · **ভার্সন:** V710 / 7.10

## TK কী বলেছেন
> "staff রা বিভ্রান্ত হয়ে যাচ্ছে — sir Enquiry এর মধ্যে patient কেন দেখাচ্ছে"
> "ওখান থেকে আসলে এখানে উপরে Enquiry Visit Patient, তা ছাড়া নিচের ফিল্টারগুলিও
> যদি না রাখা হয় তাহলে আমার মনে হয় ভালো হয়"
> "হ্যাঁ শুরু করুন, তবে **খাতায় আলাদাভাবে লিখে রাখবেন — আমার যদি পছন্দ না হয়
> তাহলে আমি যেন চেঞ্জ করতে পারি পরে**"

ডেমো প্রুফ: `V710_DEMO_CLEAN_CALL_LIST_FULL.png` (এবং প্রথম প্রস্তাব
`V710_DEMO_ALL_SECTIONS_STRIP.png`, TK যেটা বাতিল করে আরও পরিষ্কারটা চেয়েছেন)

## ✅ আসল কারণ (কোড ধরে যাচাই)
ড্যাশবোর্ডের **"N calls pending"** ব্যানার `todayOnly=true` পাঠায় ⇒
`todayAllSections = true` ⇒ `loadTodayAllSections()` **তিন সেকশন**
(Inquiry + Patient + Treatment) এক তালিকায় মেশায়।
**এটা TK-এরই ২৯.০৭.২০২৬-এর নির্দেশ (খাতার সারি B90) এবং ঠিক আছে** —
ব্যানারের সংখ্যা আর তালিকার সংখ্যা তাতেই মেলে।
**দোষটা ছিল ইঙ্গিতে:** `currentStage` "Inquiry"-তেই থাকত, তাই উপরে
"Enquiry" ট্যাবটা সবুজ হয়ে থাকত — স্টাফ ভাবতেন এটা শুধু Enquiry-র তালিকা।

## কী করা হয়েছে
| ফাইল | কী |
|---|---|
| `res/layout/activity_followup.xml` | ট্যাবের সারিতে `tabRow`, ছাঁকনির সারিতে `filterRow` — **শুধু id যোগ**; নতুন `tvAllSectionsHead` (ডিফল্ট gone) |
| `res/drawable/bg_all_sections_head.xml` | **নতুন** — বেগুনি মাথার লাইন |
| `native/FollowUpActivity.kt` | `HIDE_TABS_IN_CALL_LIST` সুইচ + `applyCallListChrome()` |
| `03_NETLIFY_READY/app.js` · `styles.css` | হুবহু একই (`__wlv1FuHideChrome` সুইচ + `.wlv1FuAllHead`) |

## 🔁🔁 TK পছন্দ না করলে কীভাবে ফেরাবেন (আলাদা করে লিখে রাখা)
**ফোনে —** `native/FollowUpActivity.kt`:
```kotlin
private val HIDE_TABS_IN_CALL_LIST = true      // ← এটাকে false করুন
```
**কম্পিউটারে —** `03_NETLIFY_READY/app.js`:
```js
const __wlv1FuHideChrome = true;               // ← এটাকে false করুন
```
এই **দুটো লাইন `false`** করলেই V709-এর হুবহু আগের চেহারা ফিরে আসে
(ট্যাব + ফিল্টার + পুরোনো নোট)। আর কোথাও কিছু বদলাতে হবে না —
বাকি সব কোড এই একটা মান দেখেই চলে।

## ⚠️ একটাই সত্যিকারের ফল (TK-কে আগেই জানানো, তিনি মেনে নিয়েছেন)
ওই তালিকায় ট্যাব না থাকায় **সেখান থেকে সরাসরি Enquiry/Visit/Patient-এ
যাওয়া যাবে না** — ব্যাক চেপে Follow-up আলাদা করে খুলতে হবে।

## ⛔ যা বদলায়নি
তালিকার তথ্য · কার্ড · Remark বক্স · LAST/NEXT CALL · চারটে বোতাম · খোঁজার ঘর ·
ব্রাঞ্চ বাছাই · সেভ · ক্লাউড — **কিছুই না**। সারি দুটো **লুকানো**, মোছা নয়।
Follow-up আলাদা করে খুললে সব হুবহু আগের মতোই।

## যাচাই
| যাচাই | ফল |
|---|---|
| XML গঠন (দুটো ফাইল) | ✅ valid |
| `verify_kotlin_compile.py` | ✅ নতুন ভুল ০ |
| `verify_android_resources.py` | ✅ নতুন id/drawable রেজলভ হয় (একমাত্র ❌ পুরোনো, মন্তব্যের ভিতরের `etHistoryNote`) |
| `app.js` JS syntax | ✅ OK |
| `tk_guard.py` (২২টা মেশিন-যাচাই) | ✅ সব পাশ |

⚠️ Android চালানোর উপায় নেই — TK দেখে বলবেন।
