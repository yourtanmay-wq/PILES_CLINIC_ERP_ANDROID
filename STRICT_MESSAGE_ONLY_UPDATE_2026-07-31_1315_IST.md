# STRICT MESSAGE-ONLY UPDATE — RMP Msg 2–4 · Patient Msg 1–10

**তারিখ ও সময়:** 31.07.2026, দুপুর ১.১৫ IST
**Source:** PILES_CLINIC_APP_V213_FINAL.zip (MD5 f14a95f8fb6b3342c6c88c3a43da6077)
**Scope:** শুধু `DoctorMessage.kt` ও `PatientMessage.kt`-এর Message Text (+ যেটুকু data-wiring টেক্সটের জন্য বাধ্যতামূলক ছিল)

## 1. Changed File List
- `DoctorMessage.kt` — Msg 2 (Arrival)/Msg 3 (Details)/Msg 4 (Referral Paid) নতুন লকড টেমপ্লেট, শুধু Bengali (TK-এর নিয়ম), Common Footer যোগ।
- `DoctorVisitActivity.kt` — `ReferredPatient`-এ `patientId` ঘর যোগ (Saved registration record থেকে), Msg 2/3/4-এর কল-সাইট নতুন signature-এ আপডেট, Msg 4-এ Amount/Payment Date এখন Saved `referralPayments` রেকর্ড থেকে (আন্দাজ নয়)।
- `PatientMessage.kt` — 10টা Patient Message-এর bn/hi/en টেক্সট নতুন লকড টেমপ্লেট অনুযায়ী (heading, Patient ID, Amount/Mode/Date ইত্যাদি)। `build()`/`buildWhatsApp()`/`show()` ফাংশনের **signature বা flow কিছুই বদলায়নি** — শুধু `block()`-এর ভিতরের টেক্সট।
- নতুন: এই Lock Note ফাইল।
- ⛔ Enquiry বার্তা (আলাদা আগের LOCK) ছোঁয়া হয়নি। RMP Msg 1 (Intro, আগের LOCK) ছোঁয়া হয়নি। Design/Layout/Button/Workflow/Database/Permission/Print-PDF System/BranchCatalog ডেটা — কিছুই বদলায়নি।

## 2. Exact Diff (সারাংশ — TK-এর নির্দেশমতো পুরো লেখা ফের কপি করা হয়নি)
- **RMP Msg 2/3/4:** আগে ছিল bn+hi দুই-ভাষা (শুধু Kishanganj-এ পপ-আপ), সাধারণ "শ্রদ্ধেয় Dr. …" শুরু, পুরনো `head()/foot()` ফরম্যাট। এখন: নতুন heading (PATIENT ARRIVAL CONFIRMATION ইত্যাদি), ডাক্তারের এলাকা, Patient ID, নতুন Common Footer — **শুধু Bengali** (TK-এর স্পষ্ট নিয়ম, ভাষা-বাছাই UI অক্ষত রইলো কিন্তু বার্তা এখন সবসময় বাংলা)।
- **Patient Msg 1–10:** আগে ছিল ছোট এক/দুই-লাইনের বাক্য (bn+hi+en তিনটে স্ট্যাক করা এক বার্তায়, উপরে ক্লিনিক হেডার + নিচে Helpline — এই **Existing Language Flow অক্ষত রাখা হয়েছে**)। এখন প্রতিটা ভাষার অংশে নতুন ইংরেজি HEADING + বিস্তারিত ফিল্ড (Patient ID, Amount, Mode, Date, Total Paid, Amount Due ইত্যাদি) — ঠিক TK-এর দেওয়া Bengali লেখা Source of Truth ধরে, Hindi/English সরাসরি সমমানের অনুবাদ।

## 3. ⚠️ TK-কে জানানো (কাজ থামানো হয়নি, প্রতিটির নিরাপদ সমাধান কোডেই আছে)
- **RMP Msg 4 — Payment Mode ও Transaction/Reference No.:** `referralPayments` রেকর্ডে (Supabase) শুধু amount/status/date/patient সেভ হয় — **mode ও reference number কখনো সেভ হয়নি**, Database পরিবর্তনও নিষেধ ছিল। তাই শুধু এই দুটো ঘর আগের TK-অনুমোদিত ফাঁকা-ঘর প্যাটার্নে (______) আছে; Amount ও Payment Date এখন সত্যিকারের Saved রেকর্ড থেকে আসে।
- **Patient Msg 7 (Receipt) — Receipt Number:** কোনো Call Site-এ আলাদা Receipt/Payment-ID পাঠানো হয় না (শুধু bill/paid/date), তাই এই একটা লাইন বাদ রাখা হয়েছে (Placeholder বসানো হয়নি, খালি "Payment Amount" শুধু amount সত্যিই থাকলে দেখায়)।
- **Patient Msg 4 (Payment) — পুরনো "কততম পেমেন্ট" (2nd/3rd…) শব্দ:** নতুন TK-টেমপ্লেটে এই তথ্য নেই, তাই বাদ পড়েছে (B92-এর addition superseded)। যদি এটা রাখা দরকার হয়, জানালে ফিরিয়ে আনা যাবে।
- **"Existing Language Flow" ব্যাখ্যা:** Patient Msg 1–10-এর জন্য পুরনো তিন-ভাষা-স্ট্যাক-করা-এক-বার্তা কাঠামো (উপরে ক্লিনিক নাম-ঠিকানা, নিচে Helpline) অক্ষত রাখা হয়েছে; ডকুমেন্টের "COMMON FOOTER" প্রতি ভাষায় আলাদা করে বসানো হয়নি (তাহলে flow/structure বদলে যেত) — তার বদলে বিদ্যমান উপরের হেডার + নিচের Helpline-ই এই কাজ করছে। ভুল ধরলে জানাবেন, ঠিক করে দেব।

## 4. SMS Test (হাতে-হাতে যাচাই — বাস্তব ডিভাইস টেস্ট না, কোড-লজিক)
- `sendSms()`/SMS পথ অপরিবর্তিত — `build()`-এর প্লেইন টেক্সট (নতুন কনটেন্ট সহ) আগের মতোই `smsto:` ইন্টেন্টে যায়। কোনো `*`/`_` markup যোগ হয়নি SMS পথে।

## 5. WhatsApp Test (কোড-লজিক যাচাই)
- `buildWhatsApp()`/`sendWhatsApp()`/`presentSendBox()`/`sendDoctorMessage()` — কোনোটাই ছোঁয়া হয়নি। নতুন কনটেন্টে "Label: value" ফরম্যাট (TK-এর ডকুমেন্টের হুবহু) ব্যবহার হওয়ায় পুরনো `bold()`-এর " : " প্যাটার্ন কিছু লাইনে আর মিলবে না (crash হয় না, শুধু সেই লাইন বোল্ড হবে না) — হেডিং লাইনটা (idx==0) এখন বোল্ড হয়, আগে ছিল সম্বোধন লাইন।

## 6. PDF Attachment Test
- Msg 9 (Document)-এর PDF-attach ফ্লো (Prescription/Diet Chart/Blood Test স্ক্রিনে গিয়ে SAVE/SHARE/PRINT) **অপরিবর্তিত** — শুধু আগের ছোট বার্তার টেক্সট বদলেছে, workflow same।

## 7. Build Result
- পাঁচটা ফাইলেই (PatientMessage.kt, EnquiryActivity.kt, PatientTimelineActivity.kt, DoctorMessage.kt, DoctorVisitActivity.kt) ব্র্যাকেট/প্যারেন গোনা পাশ (স্ট্রিং+কমেন্ট বাদ দিয়ে)।
- `kotlinx.coroutines.async/launch` fully-qualified প্যাটার্ন কোথাও নেই।
- নতুন/পরিবর্তিত সব লাইনের API/method/type হাতে-হাতে মেলানো হয়েছে (`patientId`, `data.refIncome`, `pat.s("patientId")` ইত্যাদি বিদ্যমান ফিল্ড/এক্সটেনশন-ফাংশনের সঙ্গে মিলিয়ে)।
- `PatientMessage.build()/buildWhatsApp()/show()`-এর signature অপরিবর্তিত — তাই RegistrationActivity.kt/FollowUpActivity.kt/ChamberAttendanceActivity.kt/PatientTimelineActivity.kt-এর কোনো কল-সাইট ভাঙেনি।

## Owner Lock Rule (বহাল)
এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।
