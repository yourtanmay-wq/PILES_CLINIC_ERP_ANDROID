# V232 — Work Note (কোড পরিবর্তনের আগে লেখা)

**Base:** V231 (সর্বশেষ working project — এটাই একমাত্র base, পুরনো কোনো version-এ ফেরা হয়নি)।
**তারিখ ও সময় (শুরু):** 01.08.2026, 12:18 PM IST।
**Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই FINAL নয়।

---

## ১. সমস্যা কী (VERIFIED, ভিডিওতে দেখা)
একজন **Enquiry** ব্যক্তি এখনও ক্লিনিকে **একবারও আসেননি**। কিন্তু তার Action Menu → **"আসার তারিখ মনে করিয়ে দিন"** চাপলে ভুলভাবে **"NEXT VISIT SCHEDULED"** বার্তা দেখাচ্ছে। এটা ভুল Stage-এর বার্তা — "NEXT VISIT SCHEDULED" শুধু আগে ক্লিনিকে আসা Visit/Patient-এর *পরবর্তী* ভিজিটের জন্য।

## ২. Code-এ সমস্যা কোথায়
`…/native/PatientTimelineActivity.kt` — View All → "⚡ Take Action" শিটের **"আসার তারিখ মনে করিয়ে দিন"** হ্যান্ডলার (≈ line 793–813)।
বোতামটা `currentFollowupId.isNotBlank()` হলেই দেখায় — **stage দেখে নয়**। চাপলে nextFollow তারিখ এনে সব stage-এ একই ডাক দেয়:
```
PatientMessage.show(… kind = PatientMessage.Kind.VISIT_DATE, dateText = …)
```
`Kind.VISIT_DATE` = "NEXT VISIT SCHEDULED" (PatientMessage.kt:192/294/396 — তিন ভাষা একসাথে)। তাই Enquiry-তেও ভুল করে "NEXT VISIT SCHEDULED" যায়।

**যাচাই — দ্বিতীয় VISIT_DATE ডাক নিরাপদ:** `…/native/FollowUpActivity.kt:3437-3444`-এর VISIT_DATE **`item.stage == "Treatment"` দিয়ে গার্ড করা** (শুধু Patient কার্ড)। Enquiry কখনো এখানে পৌঁছায় না — তাই ওটা **ছোঁয়া হবে না**।

## ৩. কোন কোন File পরিবর্তন হবে (২টি)
1. `…/native/PatientMessage.kt` — নতুন **Final-Locked "FIRST VISIT APPOINTMENT CONFIRMED"** বার্তা (`buildFirstVisitAppointment`, bn/hi/en) + নতুন `showFirstVisitAppointment(...)` যা **আগে থেকে থাকা** `showLanguagePicker` (ভাষা-বাছাই) ও `presentSendBox` পুনর্ব্যবহার করে — **একটি ভাষা** preview ও send। পুরনো কোনো বার্তা/ফাংশন বদলাবে না।
2. `…/native/PatientTimelineActivity.kt` — শুধু ওই এক হ্যান্ডলারে stage-branch: `!isRegistered` (Enquiry) হলে নতুন First Visit বার্তা; নইলে **আগের মতোই** VISIT_DATE।

## ৪. পরিবর্তনের আগে বর্তমান behavior
- Enquiry (isRegistered=false) → "আসার তারিখ মনে করিয়ে দিন" → **"NEXT VISIT SCHEDULED"** (ভুল), তিন ভাষা একসাথে, SMS/WhatsApp/Later বাক্স।
- Visit/Patient (isRegistered=true) → একই বোতাম → "NEXT VISIT SCHEDULED" (**সঠিক**, অপরিবর্তিত থাকবে)।
- `isRegistered = currentPatientRowId.isNotBlank() || followupStage ∈ {Patient, Treatment}` (PatientTimelineActivity:438-440) — এটাই "ক্লিনিকে এসেছেন কি না" বোঝার চিহ্ন।

## ৫. কোন PASS/LOCKED কাজ পরিবর্তন করা যাবে না
- Visit/Patient-এর existing **"NEXT VISIT SCHEDULED"** বার্তা ও logic (`Kind.VISIT_DATE`, PatientMessage.kt + FollowUpActivity.kt:3425-3445)।
- New Enquiry Save flow · Duplicate Mobile Check · Branch Filter ও Search।
- Complete Patient Delete → Trash Bin · Delete-এর পরে list ও count refresh (V231-এ করা)।
- বর্তমান UI design, layout, color, spacing, buttons।
- Registration, Payment, Follow-up, Trash/Restore, permission, database ও sync logic।
- আগে থেকে থাকা `showEnquiryMessage`/ENQUIRY লক টেমপ্লেট ও বাকি ১১ ধরনের বার্তা।
- আগে সম্পন্ন অন্য কোনো কাজ।

**পরিকল্পনা:** শুধু এই একটি verified wrong-stage বার্তা ঠিক হবে। কোনো broad refactor, cleanup, optimization, redesign বা unrelated change নয়।

---

**তারিখ ও সময় (শেষ):** 01.08.2026, 12:24 PM IST।

## ৬. বাস্তবে কী পরিবর্তন করা হলো
- `PatientMessage.kt`-এ নতুন **Final-Locked "FIRST VISIT APPOINTMENT CONFIRMED"** বার্তা যোগ:
  - `buildFirstVisitAppointment(lang, …)` — বাংলা/হিন্দি/English, heading `FIRST VISIT APPOINTMENT CONFIRMED`, মূল বক্তব্য "আপনার অ্যাপয়েন্টমেন্ট সফলভাবে নিশ্চিত করা হয়েছে।", সঙ্গে Appointment Date ও Visiting Time; উপরে ক্লিনিকের নাম-ঠিকানা, নিচে Helpline (সব BranchCatalog থেকে, হাতে লেখা নয়)।
  - `showFirstVisitAppointment(…)` — **আগে থেকে থাকা** `showLanguagePicker` (ভাষা-বাছাই) ও `presentSendBox` পুনর্ব্যবহার করে; **শুধু নির্বাচিত একটি ভাষা** preview ও send (SMS/WhatsApp/Later)। তিন ভাষা একসাথে নয়।
- `PatientTimelineActivity.kt`-এ "আসার তারিখ মনে করিয়ে দিন" হ্যান্ডলারে stage-branch: `!isRegistered` (Enquiry) → নতুন বার্তা; নইলে (Visit/Patient) → **আগের মতোই** `Kind.VISIT_DATE`।

## ৭. পরিবর্তিত File-এর তালিকা (২টি)
- `…/native/PatientMessage.kt` — শুধু **যোগ** (২টি নতুন ফাংশন)। পুরনো কোনো বার্তা/ফাংশন/`Kind.VISIT_DATE` বদলায়নি।
- `…/native/PatientTimelineActivity.kt` — শুধু ওই এক হ্যান্ডলারের `else` শাখায় stage-branch (Enquiry বনাম Visit/Patient)।

## ৮. Enquiry ও Visit/Patient — দুই Stage আলাদাভাবে কীভাবে Test করা যায়
- **Enquiry** (এখনো ক্লিনিকে আসেননি, `isRegistered=false`): View All → ⚡ Take Action → "আসার তারিখ মনে করিয়ে দিন" → **ভাষা-বাছাই পপ-আপ** → একটি ভাষা বাছলে **"FIRST VISIT APPOINTMENT CONFIRMED"** preview। "NEXT VISIT SCHEDULED" **আসবে না**।
- **Visit/Patient** (আগে এসেছেন, `isRegistered=true`): একই বোতাম → **সরাসরি** "NEXT VISIT SCHEDULED" (আগের মতোই তিন ভাষা একসাথে, ভাষা-বাছাই ছাড়া)। কোনো পরিবর্তন নেই।
- ⚠️ এই cloud-এ SDK নেই তাই আসল device-run হয়নি — owner Android Studio-তে দুই stage আলাদা করে দেখে নিশ্চিত করবেন।

## ৯. Language Selection ও single-language Preview
- ভাষা-বাছাই পপ-আপে তিনটি অপশন: Bengali / Hindi / English (আগে থেকে থাকা `showLanguagePicker`)।
- যেটি বাছা হবে **শুধু সেই একটি ভাষার** লেখা Send-বাক্সে দেখায় ও পাঠায় — অন্য দুই ভাষা যুক্ত হয় না (`buildFirstVisitAppointment(lang)` একবারে এক lang বানায়)।
- ⚠️ Preview চোখে দেখা owner-এর build-এ যাচাইযোগ্য (static-ভাবে যুক্তি নিশ্চিত করা হয়েছে)।

## ১০. PASS/LOCKED অপরিবর্তিত — Declaration
নিচের সব **এক অক্ষরও বদলানো হয়নি**: Visit/Patient-এর "NEXT VISIT SCHEDULED" (`Kind.VISIT_DATE`, FollowUpActivity Treatment-guard সহ); New Enquiry Save; Duplicate Mobile Check; Branch Filter ও Search; Complete Patient Delete → Trash Bin ও Delete-পরবর্তী list/count refresh; UI design/layout/color/spacing/buttons; Registration/Payment/Follow-up/Trash-Restore/permission/database/sync logic; আগের `showEnquiryMessage`/ENQUIRY লক টেমপ্লেট ও বাকি বার্তা। কোনো broad refactor/cleanup/optimization/redesign করা হয়নি।

**সৎ বিবৃতি:** এই cloud-এ compile/build/APK হয়নি (SDK নেই)। কোড static-ভাবে যাচাই করা হয়েছে (একটি স্বতন্ত্র review-তেও **BUILD-SAFE** ও "NEXT VISIT SCHEDULED untouched")। "Build/Test Pass" দাবি করা হচ্ছে না — owner Android Studio-তে build ও দুই-stage live-test করবেন।
