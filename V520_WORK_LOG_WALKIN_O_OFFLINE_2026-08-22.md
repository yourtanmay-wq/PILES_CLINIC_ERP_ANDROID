# V520 — Walk-in ও Offline (এক মোবাইলে একাধিক রোগী, শেষ দুই ধাপ)
**তারিখ:** ২২.০৮.২০২৬ · **ভিত্তি:** V519 · **TK-এর নির্দেশ:** *"বাকি কাজগুলো ধরুন — walk-in আর offline"*

---

## এক কথায় কী ঠিক হলো

| | আগে যা হত (ভুল) | এখন |
|---|---|---|
| **Walk-in** | Payment-এ শুধু নম্বর টাইপ করলে অ্যাপ **আন্দাজে** একজনকে বেছে নিত, আর ওই নম্বরের **সব** সারির টাকা একসাথে দেখাত — স্বামীর ₹৫০০০ স্ত্রীর পর্দায় | ঠিক রোগীটাই বাছা হয়; দুজন থাকলে **নাম দেখিয়ে জিজ্ঞাসা** করা হয়; টাকা কখনো মেশে না |
| **Offline** | নেট ছাড়া ডুপ্লিকেট-চেক ফোনের তালিকা থেকে **একজনকেই** দেখাত; দ্বিতীয় রোগীর Follow-up সারি প্রথমজনের উপরে **বসে যেত**; এক জনের টাকা **দুজনের নামেই** বসত | তিনটেই বন্ধ |

---

## ১. Walk-in

### `PatientModel.kt` — একটাই সাধারণ নিয়ম
`isDeclaredSeparateRowId(rowId, mobileDigits)` — `pat_<১০ সংখ্যা>_<লেজ>` ধাঁচ চেনে।
এই ধাঁচ **একমাত্র** `newRowIdForSameMobile()` বানায়, আর সেটা ডাকা হয় কেবল স্টাফ
*"Different Patient — Same Mobile"* চাপলে। পুরোনো সব সারির আইডিতে লেজ নেই →
সবসময় `false` → **পুরোনো আচরণ অবিকল একই**।

### `PaymentRepository.kt`
- `findPatientByMobile(...)` — নতুন `preferPatientCode` / `preferRowId`।
  দুটোই ফাঁকা রাখলে **হুবহু আগের পথ** (`PatientIdentity.pickPatientRow`)।
- **টাকা মেশা বন্ধ:** ওই নম্বরে ঘোষিত আলাদা রোগী থাকলে হিসাব হয় **শুধু বেছে
  নেওয়া রোগীর নিজের আইডি ধরে**। ⛔ না থাকলে খাতার সারি **B30**-এর যোগফল
  (একই রোগীর ভুলে-হওয়া দুটো সারির টাকা একসাথে) **অটুট**।
- `identitiesOnMobile(mobile, branch)` — এই নম্বরে কারা কারা।
  **নতুন কোনো cloud-read নয়** — `findPatientByMobile()` ঠিক একই অনুরোধ করে,
  `CloudReadDedupe` দ্বিতীয়বার নেটে পাঠায় না।
- `searchPatients()` — ঘোষিত আলাদা রোগী আর চাপা পড়ে না (দুটো কার্ড)।
  ভুলে-দুবার সারি আগের মতোই একটাই কার্ড।
- `findOrMakePatient()` — কার্ডের Patient ID দিয়েই ঠিক রোগী।

### `PaymentActivity.kt`
- কার্ডে চাপ দিলে **সেই সারির** আইডি সাথে যায়।
- Intent-এ `patientRowId` / `patientCode` — ডাকা পর্দা জানলে কিছু জিজ্ঞাসা নয়।
- **শুধু** যখন স্টাফ বেয়ার নম্বর দিয়েছেন **আর ওই নম্বরে ≥২ জন** — তখনই ছোট
  পছন্দ-বাক্স (নাম · Patient ID · ব্রাঞ্চ)। **একজন থাকলে বাক্স কখনো খোলে না।**

### ডাকার জায়গা যেগুলো এখন পরিচয় পাঠায়
`ChamberAttendanceActivity` (৪টি) · `FollowUpActivity` (৩টি) ·
`GlobalSearchActivity` · `PatientTimelineActivity` (২টি)।

---

## ২. Offline

### `LocalWorkflowStore.kt`
- `findPatientsByMobile()` — **নতুন**, ফোনের তালিকার **সব** মিল।
  পুরোনো `findPatientByMobile()` **এক অক্ষরও বদলায়নি**।
- `upsertFollowUp()` / `upsertFollowUps()` — "একই মোবাইল + একই stage = একই
  যাত্রা" নিয়মে দুই রোগীর সারি এক হয়ে যেত। এখন `provablyDifferentPatients()`
  **প্রমাণ** করতে পারলে তবেই আলাদা রাখে; নইলে **আচরণ হুবহু আগের**।
- `promoteToTreatment()` — টাকা বসত ওই নম্বরের **সব** সারিতে। এখন এই রোগীর
  নিজের আইডি ধরে চেনা সারিতেই; চেনা না গেলে **পুরোনো নিয়ম অক্ষত**।

### `RegistrationRepository.kt`
নেট ছাড়া ডুপ্লিকেট-চেক এখন ফোনের তালিকা থেকে **সবাইকে** দেখায় (`matches`),
ঠিক যেমন ক্লাউড-পথে হয়। পুরোনো ঘরগুলো আগের মতোই প্রথমজনকে ধরে।

---

## ৩. ওয়েব (কম্পিউটার)

`app.js`-এ **একই জায়গায়** একই ভুল ছিল:

```js
function payOwnedBy(x,p){ … return !!pm&&!!xm&&pm===xm; }   // মোবাইল-fallback
```
এই একটাই ফাংশন ওয়েবের **১৫টা** টাকা-গোনার জায়গার ভিত্তি — তাই এখানেই একবার
ঠিক করা হয়েছে। ঘোষিত আলাদা রোগী জড়িত থাকলে fallback আর চলে না।
সঙ্গে `treatmentTotals`, রসিদ ছাপা, ও দুটো Journey/History তালিকা থেকে
বাড়তি কাঁচা মোবাইল-মিল সরানো হয়েছে (নইলে সুরক্ষা ফাঁকি দিয়ে যেত)।

**টাকা হারানো অসম্ভব:** প্রতিটা পেমেন্টের সারিতে মালিকের আইডি সবসময় লেখা হয়
(`wlv1BuildTreatmentEventRow` → `patientId:p.id`; ফোনেও একই) — কোডে যাচাই করা।

---

## ৪. যাচাই (সব সবুজ)

| যাচাই | ফল |
|---|---|
| V520 walk-in + offline (নতুন) | ৩৪/৩৪ PASS |
| V520 ওয়েব টাকা-আলাদা (নতুন) | ১৪/১৪ PASS |
| V516 Registration (+৪টি নতুন offline) | ALL PASSED |
| V517 Search · V517 Timeline | ALL PASSED |
| V518 Follow-up tab | ALL PASSED |
| V515 Trash · V513 Revalidation · V519 Attendance | ALL PASSED |
| ওয়েব: webfu · webfam · websearch · dq | ALL PASSED |
| ৮টি ওয়েব ফাইল `node --check` | ৮/৮ OK |

`PatientModel.kt` · `PatientIdentity.kt` · `LocalWorkflowStore.kt` ·
`RegistrationRepository.kt` — **আসল ফাইলগুলোই** কম্পাইল হয়েছে।
`PaymentRepository`-র তিনটে ফাংশন **হুবহু তুলে এনে** চালানো হয়েছে।

---

## ৫. সৎ সীমাবদ্ধতা (লুকানো হয়নি)

1. **Activity ফাইল কম্পাইল করা যায়নি** — এই মেশিন থেকে Android SDK
   (`dl.google.com`) পৌঁছয় না (403)। `PaymentActivity` · `ChamberAttendanceActivity` ·
   `FollowUpActivity` · `GlobalSearchActivity` · `PatientTimelineActivity`-র
   বদলগুলো **হাতে যাচাই** করা হয়েছে: ব্যবহৃত প্রতিটা নাম/ঘর কোডে খুঁজে দেখা
   হয়েছে (`directFormOnly`, `PremiumAlert.header`, `row.patientId`,
   `item.refId`, `hit.rowId`, `preferPatientRowId`, `currentPatientCode`), আর
   ব্রেস-ভারসাম্য `git HEAD`-এর সঙ্গে মিলিয়ে দেখা হয়েছে (সব `+0`)।
   **TK-কে বিল্ড করে একবার দেখে নিতে হবে।**
2. **Chamber Attendance বোর্ড এখনো মোবাইল ধরে সারি বানায়** (`byMobile`) — তাই
   এক নম্বরে দুজন থাকলে বোর্ডে **একটাই সারি** দেখাবে। টাকা এখন ঠিক রোগীর
   নামেই যায় (উপরের ফিক্স), কিন্তু বোর্ডে দুটো আলাদা সারি দেখানো একটা **আলাদা
   বড় কাজ** — TK বললে পরে ধরব। এটা walk-in/offline-এর অংশ নয়।
3. **Report Card পর্দা এখনো শুধু মোবাইল নিয়ে খোলে** — তাই এক নম্বরে দুজন
   থাকলে সেখানে আগের নিয়মেই একজন দেখাবে। এটাও আলাদা কাজ।

---

## ৬. কোনো ঝুঁকি নেই যেখানে
- Database schema · RLS · production data — **কিছুই ছোঁয়া হয়নি**।
- পুরোনো কোনো রোগীর আইডি বা তথ্য বদলানো/মোছা হয়নি; কোনো migration লাগে না।
- Supabase-এ **একটাও বাড়তি query নেই** (chooser-এর তথ্য dedupe থেকেই আসে)।
- UI/workflow/permission — শুধু একটাই নতুন বাক্স, আর সেটা কেবল সত্যিই দুজন
  থাকলে।
