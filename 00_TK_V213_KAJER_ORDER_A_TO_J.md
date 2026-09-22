# 📋 TK-এর V213 কাজের অর্ডার (সম্পূর্ণ, 31.07.2026) — ধাপে ধাপে হবে, একবারে না

> ⛔ **এই ফাইলের কোনো লাইন TK-কে না জানিয়ে বদলানো/মোছা যাবে না।**
> ⛔ **Base Project:** PILES_CLINIC_APP_V212_FINAL.zip — এর কোনো approved Design/Layout/Colour/Button/Card/Print/Permission/Branch Rule/Payment হিসাব/Workflow নষ্ট করা যাবে না। Broad refactor/cleanup/rewrite/অপ্রয়োজনীয় optimisation নিষিদ্ধ।
> ⛔ Owner স্পষ্টভাবে "V213 ফাইল পাঠান" না বলা পর্যন্ত Final ZIP পাঠানো যাবে না।
> ⛔ প্রতিটা ধাপ শুরুর আগে: changed-file list + design লাগবে কিনা + SQL লাগবে কিনা + ঝুঁকি — এইটুকু আগে দিতে হবে।
> ⛔ Popup-এর কাজে কোড লেখার আগে full-screen Design Proof দেখিয়ে থামতে হবে।

## অগ্রগতি ট্র্যাকার (31.07.2026 রাত — TK "ফাইল পাঠান" বলার সময়ের অবস্থা)

**✅ শেষ:** C (Loading Speed — অডিট+Fix), E (Guard আপডেট), Master List Item 1 (Popup Professional), Item 3 (Next Call যাচাই), Item 4 (Name/Mobile ডুপ্লিকেট), Item 5 (REJECT Card)
**⛔ স্থগিত (TK নিজে করবেন):** Item 2 / Section G (RMP Message)
**🔴 বাকি (সবচেয়ে ঝুঁকিপূর্ণ, এখনো শুরু হয়নি):** Section B (Background Sync/Delete/Restore — টাকা+ডুপ্লিকেট-প্রতিরোধ+সিঙ্ক, পুরো অ্যাপ জুড়ে)
**🔴 বাকি (ছোট, শেষে হবে):** A (নিশ্চয়তা-যাচাই), D (এই ডেলিভারিতেই হচ্ছে), I (বাধ্যতামূলক টেস্ট — TK-এর হাতে), J (Final Report — B শেষ হলে)

- [✅ Fix সম্পূর্ণ] **C — Loading Speed** — অডিট + মূল Fix দুটোই শেষ (খাতার সারি B225): BottomNav-এর retry burst-এ দেড় সেকেন্ড বিলম্ব, Chamber Search-এ ছবি বাদ। ⛔ কোনো Design/Workflow বদলায়নি।
- [⏳ বাকি] A — অক্ষত-থাকা-উচিত জিনিসের নিশ্চয়তা (সাধারণত আগের কাজ যাচাই করেই দেখানো হবে)
- [⏳ বাকি] B — Background Sync/Delete/Restore/Duplicate-protection technical fix
- [⏳ বাকি] D — V213 ভার্সন রিলিজ (সবার শেষে, TK "ফাইল পাঠান" বললেই)
- [✅ সম্পূর্ণ] E — Guard আপডেট (খাতার সারি B226) — আসল tk_guard.py চালিয়ে ২টা বাগ ধরা হলো (Dashboard-এ হার্ডকোড V210, Guard-এর পুরনো ডিজাইন-প্রত্যাশা), দুটোই ঠিক, Guard এখন সব ✅ পাশ।
- [⏳ বাকি] F — Plain Popup Professional Design (আনুমানিক তালিকা দেওয়া হয়েছে 31.07.2026-এ, ফাইনাল লিস্ট+Design Proof বাকি)
- [⏳ বাকি] G — RMP/Doctor Message আপডেট (৪টা মেসেজ, Kishanganj বাংলা+হিন্দি টেক্সট TK দিয়েছেন)
- [⏳ বাকি] I — বাধ্যতামূলক টেস্ট (সব ধাপ শেষে)
- [⏳ বাকি] J — Final Work Report + Declaration

### 📋 C — Loading Speed Audit — সম্পূর্ণ ফলাফল (31.07.2026, দুই দফায় যাচাই, কোনো Fix হয়নি)

**পদ্ধতি:** সরাসরি স্টপওয়াচ দিয়ে মাপা হয়নি (আমার হাতে ফোন নেই) — কোড পড়ে Cloud-call সংখ্যা/প্যাটার্ন যাচাই করা হয়েছে।

**🎯 সবচেয়ে বড় ও গুরুত্বপূর্ণ কারণ (প্রায় সব স্ক্রিনের কমন সমস্যা):**
- **ফাইল:** `BottomNav.kt` → `retryStuckSaves()` (ডাকা হয় `wire()` থেকে, যেটা ৩৪টা ফাইলে/প্রায় প্রতিটা স্ক্রিনের `onCreate`-এ আছে)
- **কারণ:** স্ক্রিন খুললেই ব্যাকগ্রাউন্ডে ১১টা আলাদা repository-র flush/retry একসাথে চলে (CloudWriteQueue, DeletedGuard, Enquiry/Registration/Payment/FollowUp/Chamber/Clinical/Briefing/GenericUpdate/ChamberClose)। ২-মিনিটের বিরতি আছে কিন্তু pending কিছু থাকলে সেটা মানা হয় না — TK-এর দুর্বল লাইনে (কোডে লেখা "0.16–2.00 KB/s") প্রায় সবসময়ই কিছু pending থাকার কথা, তাই এই ভারী burst প্রায় প্রতিটা স্ক্রিন-ওপেনেই bandwidth দখল করে রাখে।
- **প্রভাব:** এমনকি যেসব স্ক্রিনের নিজস্ব cache-first কোড ঠিকঠাক আছে (নিচে দেখুন), সেগুলোও এই শেয়ার-করা burst-এর কারণে ধীর অনুভব হয়।
- **প্রস্তাবিত নিরাপদ সমাধান (এখনো করা হয়নি):** `retryStuckSaves`-এর burst-টা স্ক্রিনের নিজের data-load শেষ হওয়ার **পরে** শুরু করা (এখন সমান্তরালে/সাথে সাথে চলে) — কোনো Design/Workflow/হিসাব না ছুঁয়ে, শুধু ক্রম বদলে।

**✅ ঠিক আছে (নিজস্ব cache-first কোড আগে থেকেই আছে):**
- Follow-up (`FollowUpActivity.kt`) — local list cache + ২৭.০৭-এর `CloudReadCache` (৪ বার একই টেবিল নামা আটকায়)
- Chamber Attendance (`ChamberAttendanceActivity.kt`) — `loadCachedBoard()`, বোর্ড-লোডে ছবি বাদ (`PATIENT_COLS_NO_PHOTO`)
- Payment আজকের Collection (`PaymentActivity.kt`) — `loadCachedTodayCollection()`
- RMP/Doctor Visit (`DoctorVisitActivity.kt`) — `loadCachedDoctors()`
- Patient Timeline (`PatientTimelineActivity.kt`) — `TimelineCache` + তাৎক্ষণিক হেডার-পেইন্ট
- Spinner/cache স্ক্রিন-ছাড়ার সময় মোছা হয় না (`onPause()` যাচাই করা হয়েছে Follow-up/Chamber-এ) — এখানে কোনো bug নেই

**⚠️ Local-cache-first এখনো নেই:**
- Report Card (`ReportCardActivity.kt`) — ছোট single-record fetch (২টা), তাই ভারী না কিন্তু প্রতিবারই নেটওয়ার্কের অপেক্ষা
- Registration (`RegistrationActivity.kt`) — মূলত ফর্ম, ডুপ্লিকেট-চেক গতি আলাদা প্রসঙ্গ
- Dashboard (`DashboardActivity.kt`) — সরাসরি বড় fetch নেই, সামারি-ভিত্তিক, কিন্তু cache-first নেই
- Global Search (`GlobalSearchActivity.kt`) — ২টা `fetchListSlim` কল, cache নেই

**📌 আরেকটা চিহ্নিত (ছোট) সুযোগ, নোট রাখা — এখনো Fix হয়নি:**
- `ChamberAttendanceRepository.kt` লাইন ৮৪৩ (`searchPatients()`) — সর্বোচ্চ ২০ জনের সম্পূর্ণ row (ছবিসহ) আনে, অথচ শুধু নাম/মোবাইল/আইডি/ব্রাঞ্চ ব্যবহার হয় — `fetchListSlim` দিয়ে ছবি বাদ দিলে নিরাপদে গতি বাড়বে।

⛔ **২০ সেকেন্ডের `CloudReadCache` মেয়াদ শেষ হলে আবার fetch হওয়াটা bug না — এটা ইচ্ছাকৃত ডিজাইন** (stale-while-revalidate), যাতে টাকা/রিমার্কের তথ্য কখনো পুরনো না দেখায়।

⛔ **এই পুরো অডিটে একটাও কোড বদলানো হয়নি।** TK-এর "সব কাজ একসঙ্গে অনুমোদন দেওয়া হবে" নির্দেশ অনুযায়ী অপেক্ষা করা হচ্ছে।

## 📋 Master List — Cloud AI-কে পরে একসঙ্গে পাঠানোর বর্তমান তালিকা (TK, 31.07.2026 — শুধু নোট, এখনো কাজ শুরু হয়নি)

> ⛔ ছবি (Photo) সংক্রান্ত বিষয় আপাতত বাদ। Loading-এর বিষয় আগেই আলাদাভাবে পাঠানো হয়েছে (দেখুন উপরের "C — Loading Speed Audit" অংশ), তাই এই তালিকায় আলাদা করে নেই।

1. **সব Plain/Default Popup Professional করা** — আগে Proof দেখাবে। Popup-এর field, button, save, validation ও workflow বদলাবে না। (= উপরের Section F-এর সাথে একই)

2. **RMP Message Professional আপডেট** — বাংলা ও হিন্দি থাকবে। Kishanganj-এর নাম–ঠিকানা–ফোন আলাদা এবং অন্য Branch-গুলোর নিজস্ব তথ্য থাকবে। WhatsApp ও SMS — দুটিতেই কাজ করবে। (= উপরের Section G-এর সাথে একই)

3. **Next Call Date সমস্যা যাচাই** — Last Call নতুন হলেও Next Call পুরোনো থেকে যাচ্ছে। কিছু Card-এ Last Call আছে, কিন্তু Next Call ঠিকভাবে দেখাচ্ছে না। Next Call নির্বাচন ও Save করা হলে কেন সঠিকভাবে দেখাচ্ছে না — Code দেখে যাচাই করতে হবে। ⛔ বর্তমান Follow-up Rule বদলাবে না। *(নোট: B214-এ ওয়েব অ্যাপের একটা কারণ আগেই ধরে ঠিক করা হয়েছিল ("Save Remark" পথে next-follow ছোঁয়া হত না) — TK এখন বলছেন এখনো কিছু Card-এ সমস্যা দেখা যাচ্ছে, তাই আরও গভীরে/অন্য কোনো কারণ থাকতে পারে কিনা যাচাই করতে হবে।)*

4. **Patient Name-এর জায়গায় Mobile দুবার দেখানো** — Database-এ নাম থাকলে সঠিক নাম দেখাবে। নাম না থাকলে "Name Not Available" দেখাবে এবং Mobile Number একবারই থাকবে (এখন সম্ভবত দুইবার দেখাচ্ছে — নামের জায়গাতেও মোবাইল, মোবাইলের জায়গাতেও মোবাইল)।

5. **REJECT লেখা Card যাচাই** — REJECT শুধু সাধারণ Remark (স্টাফ নিজে টেক্সট হিসেবে লিখেছেন) হলে কোনো পরিবর্তন নয়। কিন্তু সত্যিকারের Reject Action ব্যবহার করা হয়ে থাকলে — কেন সেই Card Active Visit List-এ আছে এবং Advance/Test বোতাম সক্রিয় — তা যাচাই করতে হবে।

✅ **TK-এর অনুমতি পাওয়া গেছে (31.07.2026, "অনুমতি দেয়া হলো")। অগ্রগতি:**
- Item ১ (Popup Professional) — ✅ **সম্পূর্ণ শেষ।** পুরো প্রজেক্ট (৩০+ ফাইল) চেক করে ১৯টা পপ-আপ প্রিমিয়াম করা হয়েছে, বাকি সব আগে থেকেই ঠিক ছিল। খাতার সারি B219-B221।
- Item ২ (RMP Message) — ⛔ **স্থগিত, TK-এর নির্দেশে** (31.07.2026): "বার্তা চ্যাপ্টার আপাতত যা ছিল সেটা রাখুন, আমি এটা পরিবর্তন করব সমস্ত লেখা।" প্রুফ দেখানো হয়েছিল, TK নিজেই লেখা ঠিক করে দেবেন — কোনো কোড বদলানো হয়নি, DoctorMessage.kt অক্ষত।
- Item ৩ (Next Call Date) — ✅ গভীরে যাচাই সম্পূর্ণ, নতুন কোনো আলাদা বাগ পাওয়া যায়নি (B214-এর ফিক্সই মূল কারণ কভার করে)
- Item ৪ (Name-এর জায়গায় Mobile দুবার) — ✅ **সম্পূর্ণ শেষ** (খাতার সারি B218, B223, B224) — মোট ১৭টা ফাইলে ঠিক হয়েছে। **গুরুত্বপূর্ণ আবিষ্কার:** Patient Timeline/Report Card ও Draft-এর আসল কারণ ডেটা-রিপোজিটরি লেভেলেই ছিল (শুধু স্ক্রিনে ফিক্স বসালে কাজ করত না) — সেটাও গভীরে গিয়ে ধরে ঠিক করা হয়েছে। প্রিন্ট হওয়া Chamber Register-এও ঠিক হয়েছে।
- Item ৫ (REJECT Card) — ✅ **হয়েছে** (খাতার সারি B217) — ওয়েব অ্যাপে sibling-closing পোর্ট করা হয়েছে (ফোনের অ্যাপ আগে থেকেই ঠিক ছিল)

---

---

## A. অবশ্যই অক্ষত রাখতে হবে
1. Internet দুর্বল হলেও Save/Edit/Payment/Remark নিজের ফোনে সঙ্গে সঙ্গে দেখা যাবে।
2. Cloud Sync পিছনে চলবে, অপ্রয়োজনীয় Cloud call কম।
3. App slow/freeze/crash/দীর্ঘ Loading করবে না।
4. অন্য ফোনে তথ্য দেরিতে পৌঁছাতে পারে, হারাতে/ভুল হতে পারবে না।
5. সব বর্তমান approved Design ও Working অক্ষত।
6. V212-এর APK Owner ইতিমধ্যে সফলভাবে Build করেছেন।

## B. Background Technical Fix
1. Delete/Reject করা তথ্য অন্য ফোন/Web/পুরনো cache/pending data থেকে ফিরে আসতে পারবে না।
2. Restore করলে সব ফোন ও Web-এ সঠিকভাবে ফিরবে।
3. Failed Delete পরে সফল হলে local phone থেকেও record/card পরিষ্কার হবে।
4. Save/Delete/Restore/Payment-এর মাঝপথে Internet বন্ধ হলে কাজটি নিরাপদে পরে সম্পন্ন হবে।
5. Background, Manual "পাঠান", Screen-open, WorkManager Retry — একই shared control ব্যবহার করবে।
6. একই Save/Payment একসঙ্গে দুবার পাঠানোর চেষ্টা হবে না।
7. অনেক failed/pending কাজ জমলেও পুরনো কাজ নীরবে বাদ যাবে না।
8. "Synced" শুধু তখনই দেখাবে যখন সত্যিই সব pending কাজ শেষ।
9. অন্য ফোনের নতুন তথ্য নির্ভরযোগ্যভাবে আসবে।
10. একই পরিবর্তনের জন্য বারবার Full Refresh হবে না।
11. Refresh ব্যর্থ হলে সেই পরিবর্তন পরেরবার আবার ধরতে হবে।
12. Android ও Web — দুই জায়গার Delete/Restore/Sync protection একই।

## C. Loading ও Speed সমস্যা (🟢 এখন এইটাই চলছে — শুধু অডিট)
বর্তমানে প্রায় প্রতিটি Section খুলতে সময় লাগে; বের হয়ে সঙ্গে সঙ্গে ঢুকলেও একই Loading।
1. Section খুললেই আগে local/cached data প্রায় সঙ্গে সঙ্গে দেখাতে হবে।
2. Cloud refresh নীরবে background-এ।
3. একই Section-এ আবার ঢুকলে অপ্রয়োজনীয় Full Reload না।
4. Screen খোলার সময় Sync/Retry/Auto Refresh একসঙ্গে duplicate না।
5. আগের Screen data/cache অপ্রয়োজনে ফেলে দেওয়া যাবে না।
6. Patient photo/পুরনো বড় data বারবার download না।
7. পরিবর্তন না থাকলে পুরো list Cloud থেকে আবার নামানো যাবে না।
8. কোনো stale/ভুল তথ্য দেখানো যাবে না।
⛔ কাজ শেষে প্রতিটি Section-এর Before/After loading time ও Cloud-call count লাগবে।
⛔ **এই ধাপে Payment section-এ কোনো গতি-বৃদ্ধির Code Change হবে না — শুধু সমস্যা মাপা ও রিপোর্ট।**
⛔ **Audit Report দিয়ে থামতে হবে — TK-এর অনুমতি ছাড়া Fix শুরু করা যাবে না।**

## D. সঠিক Version ও Release
- ZIP: `PILES_CLINIC_APP_V213_FINAL.zip` · versionCode 213 · versionName "2.13" · Dashboard/App-এ "V213" · ZIP-এর root folder নাম `PILES_CLINIC_APP_V213_FINAL`।
- App-এর ভিতরে পুরনো V210/V212/অন্য ভুল ভার্সন দেখা যাবে না।
- পুরনো `.git`/ZIP/Netlify assets/history মুছে ফেলা যাবে না। V212 rollback copy রাখতে হবে, checksum manifest regenerate করতে হবে।

## E. Guard ঠিক করা
বর্তমান নতুন approved Button Design ফেরত বদলানো যাবে না। Guard যদি পুরনো Design খুঁজে Fail করে, Guard-কেই বর্তমান approved Design অনুযায়ী আপডেট করতে হবে — Design-কে Guard-এর পুরনো নিয়মে ফেরানো নিষিদ্ধ।

## F. Plain Popup Professional Design
পুরো Project audit করে সব plain/default AlertDialog/Popup-এর exact list আগে দিতে হবে।
অন্তত যেখানে যাচাই করতে হবে: Follow-up ও Follow Calendar (Update Remark, Payment) · Patient Timeline (Edit Patient, Add Remark, Referral, Payment/Edit Note) · Chamber (Payment, Treatment Progress, Treatment Remark, Edit Amount) · Doctor/RMP (Report, Fix Note, Details/Call Summary) · Payment forms · Briefing (Reply, Post Notice) · Report Card (Progress/Edit) · Appointment Edit · Password View · Print Options · Blood Test · Add Medicine Outside List · বাকি সব plain AlertDialog-based form।
ইতিমধ্যে Premium/approved-গুলো পরিবর্তন করা যাবে না — শুধু plain-গুলোকে বর্তমান approved premium medical blue/green/gold theme অনুযায়ী রঙিন/প্রফেশনাল/সহজ করতে হবে। কোনো Field/Button action/Save logic/Validation/Workflow বদলাবে না। **কোড করার আগে full-screen Design Proof দেখিয়ে অনুমতি নিতে হবে।**

**31.07.2026-এ দেওয়া আনুমানিক তালিকা (স্ক্রিপ্ট-ভিত্তিক, ফাইনাল না):** মোট ~৮৫টা প্লেইন পপ-আপ, ২০টা ফাইলে — PatientTimelineActivity.kt(১৪) · ChamberAttendanceActivity.kt(১৩) · DoctorVisitActivity.kt(১৩, আংশিক আগেই হয়েছে) · ReportCardActivity.kt(৮) · FollowUpActivity.kt(৮) · BriefingActivity.kt(৮) · FollowCalendarActivity.kt(৩) · PaymentActivity.kt(৩) · PasswordCenterActivity.kt(৩) · TrashBinActivity.kt/DraftListActivity.kt/RegistrationActivity.kt(৩ করে) · AppointmentActivity.kt(২) · EnquiryActivity.kt/GlobalSearchActivity.kt/ReportsActivity.kt/PatientMessage.kt/MoreMenuActivity.kt/PatientPhotoActivity.kt/SpinnerPicker.kt(১ করে)।

## G. RMP/Doctor Message Update
৪টা মেসেজ (Greeting/Introduction, Patient Arrived, Patient Treatment Update, Referral Income Sent) যাচাই। Kishanganj বাদে বাকি ৩টার অর্থ/dynamic data/Workflow অক্ষত রেখে শুধু ছোট professional wording আপডেট।

**Kishanganj Greeting — বাংলা ও হিন্দি টেক্সট (TK দিয়েছেন, হুবহু এটাই ব্যবহার হবে):**
> (বাংলা) শ্রদ্ধেয় Dr. [DOCTOR NAME], ... সবিনয়ে, T.K. BISWAS, Founder & Consultant, TK BISWAS PILES CLINIC, KISHANGANJ, Caltex Chowk, Modi Gola, Contact: 8676002200
> (হিন্দি) आदरणीय Dr. [DOCTOR NAME], ... सादर, T.K. BISWAS, Founder & Consultant, TK BISWAS PILES CLINIC, KISHANGANJ, Caltex Chowk, Modi Gola, Contact: 8676002200
> *(সম্পূর্ণ, হুবহু টেক্সট এই সেশনের কথোপকথনে TK-এর বার্তায় আছে — কাজ শুরুর সময় সেখান থেকে কপি করা হবে।)*

অন্য সব Branch একই বাংলা+হিন্দি মেসেজ, Footer শুধু বদলাবে: "MAA AYURVED PILES CLINIC" + সেই Branch-এর নাম/ঠিকানা/নম্বর (BranchCatalog থেকে auto)। সব Branch-এ Bengali/Hindi নির্বাচন থাকবে। Android+Web দুই জায়গাতেই WhatsApp+SMS। WhatsApp-এ পূর্ণ মেসেজ; SMS ছোট করতে হলে TK-এর অনুমতি লাগবে। Native ও Web-এর সব কপিতে টেক্সট একই থাকতে হবে।

## H. কঠোর নিষেধ
অনুমতি ছাড়া SQL/Database change নয় · RLS/Policy change নয় · নতুন Feature নয় · approved Design ফেরত/বদল নয় · Payment হিসাব/Permission পরিবর্তন নয় · পুরো Project rewrite নয়।

## I. বাধ্যতামূলক Test
সব পুরনো Guard Pass · নতুন technical issue-র machine-check · সব Android XML check · Web JS syntax check · সত্যিকারের assembleDebug Build · দুই ফোনে টেস্ট (Save তৎক্ষণাৎ নিজের ফোনে, অন্য ফোনে পরে ঠিকমতো, Delete ফিরে না আসা, Restore সব ফোনে ফেরা, duplicate না হওয়া, re-entry-তে দীর্ঘ Loading না হওয়া) · Popup-এর প্রতিটা বোতাম/সেভ ওয়ার্কফ্লো আগের মতো কাজ করছে কিনা · RMP বাংলা/হিন্দি WhatsApp+SMS টেস্ট। Build না হলে স্পষ্ট লিখতে হবে "Build verified নয়"।

## J. কাজের রিপোর্ট
**শুরুর আগে (প্রতি ধাপে):** Planned changed-file list · Design লাগবে কিনা · SQL লাগবে কিনা · ঝুঁকি আছে কিনা। Popup-এর Design Proof আগে দেখিয়ে থামতে হবে।
**সব শেষে (Final):** Exact changed-file list · V212→V213 diff · Before/After proof · Test evidence · Build result · এই ঘোষণা: *"সব approved Design, Working, Button, Permission, Branch Rule, Payment হিসাব, Print এবং Workflow অপরিবর্তিত রাখা হয়েছে।"*
⛔ Owner স্পষ্টভাবে "V213 ফাইল পাঠান" না বলা পর্যন্ত Final ZIP পাঠানো যাবে না।
