# V232 — Changed files

**Base:** V231। **Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই **FINAL নয়**।
**তারিখ:** 01.08.2026, 12:24 PM IST।

## এই version-এর কাজ (একটাই, TK verified live-test)
**সমস্যা:** Enquiry ব্যক্তি (এখনো ক্লিনিকে আসেননি) Action Menu → "আসার তারিখ মনে করিয়ে দিন" চাপলে ভুলভাবে **"NEXT VISIT SCHEDULED"** বার্তা দেখাচ্ছিল — যা শুধু Visit/Patient-এর পরবর্তী ভিজিটের বার্তা।

**সমাধান:** Enquiry-এর জন্য এখন **Final-Locked "FIRST VISIT APPOINTMENT CONFIRMED"** বার্তা যায় (heading + "আপনার অ্যাপয়েন্টমেন্ট সফলভাবে নিশ্চিত করা হয়েছে।" + Appointment Date + Visiting Time)। পাঠানোর আগে **বাংলা/হিন্দি/English ভাষা-বাছাই** দেখায়, আর **শুধু নির্বাচিত একটি ভাষার** বার্তা preview ও send হয় (তিন ভাষা একসাথে নয়)। Visit/Patient-এর "NEXT VISIT SCHEDULED" **সম্পূর্ণ অপরিবর্তিত**।

## পরিবর্তিত ফাইল (২টি)
- `…/native/PatientMessage.kt` — নতুন `buildFirstVisitAppointment(lang,…)` (bn/hi/en, FINAL-LOCK) + নতুন `showFirstVisitAppointment(…)` (আগে থেকে থাকা `showLanguagePicker` + `presentSendBox` পুনর্ব্যবহার, single-language)। **শুধু যোগ** — পুরনো কোনো বার্তা/`Kind.VISIT_DATE` বদলায়নি।
- `…/native/PatientTimelineActivity.kt` — "আসার তারিখ মনে করিয়ে দিন" হ্যান্ডলারে stage-branch: `!isRegistered` (Enquiry) → নতুন বার্তা; নইলে (Visit/Patient) → আগের মতোই `Kind.VISIT_DATE`।

## যা ছোঁয়া হয়নি (যাচাইকৃত)
- Visit/Patient-এর **"NEXT VISIT SCHEDULED"** (`Kind.VISIT_DATE`) — এক অক্ষরও নয়।
- `…/native/FollowUpActivity.kt`-এর VISIT_DATE ডাক **`item.stage == "Treatment"` দিয়ে গার্ড করা** (শুধু Patient কার্ড) — Enquiry কখনো পৌঁছায় না, তাই অপরিবর্তিত।
- New Enquiry Save · Duplicate Mobile Check · Branch Filter/Search · Complete Patient Delete → Trash Bin ও Delete-পরবর্তী list/count refresh · UI design/layout/color/spacing/buttons · Registration/Payment/Follow-up/Trash-Restore/permission/database/sync · আগের `showEnquiryMessage`/ENQUIRY লক টেমপ্লেট ও বাকি বার্তা।

## যা এই cloud-এ করা যায়নি (সৎ)
- **Compile/Build/APK — হয়নি** (SDK নেই)। কোড static-ভাবে যাচাই (syntax/import/reference) + একটি স্বতন্ত্র review-তে **BUILD-SAFE** ও "NEXT VISIT SCHEDULED untouched"। "Build/Test Pass" দাবি করা হচ্ছে না।
- Version bump ও index.html `?v=` — করা হয়নি (শুধু এই bug-fix চাওয়া হয়েছিল)।

## Rollback
`ROLLBACK_V232/native/` — দুই ফাইলের সত্যিকারের **pre-V232 (=V231)** কপি (uploaded zip থেকে সরাসরি)। ফেরত চাইলে ওই দুটি ফাইল বদলে দিলেই V231।

## Owner-এর দুই-stage যাচাই (Android Studio-তে build করে)
1. **Enquiry** (এখনো আসেননি): View All → ⚡ Take Action → "আসার তারিখ মনে করিয়ে দিন" → **ভাষা-বাছাই** আসবে → একটি ভাষা বাছলে **"FIRST VISIT APPOINTMENT CONFIRMED"** (এক ভাষায়) preview। "NEXT VISIT SCHEDULED" আসবে **না**।
2. **Visit/Patient** (আগে এসেছেন): একই বোতাম → সরাসরি **"NEXT VISIT SCHEDULED"** (আগের মতোই)। কোনো পরিবর্তন নেই।
3. ভাষা-বাছাইয়ে Bengali/Hindi/English তিনটেই আলাদা করে দেখে নিন — প্রতিবার **একটাই ভাষা** preview/send হচ্ছে কি না।
