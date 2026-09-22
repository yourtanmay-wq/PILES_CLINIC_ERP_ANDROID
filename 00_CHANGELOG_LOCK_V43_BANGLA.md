# PILES CLINIC APP — V43 চেঞ্জলগ ও লক নোট

## 🚫 এই ফাইলে যা আছে তা ধ্বংস করা যাবে না। কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না। কোনো working flow খারাপ করা যাবে না। **TK-এর স্পষ্ট অনুমতি ছাড়া কোনো ফাইলে, কোনো ফিচারে, ডিজাইনে বা ওয়ার্কফ্লোতে এক লাইনও পরিবর্তন করা যাবে না — এমনকি "ভালো করার" উদ্দেশ্যেও না।**

## Base ফাইল
`PILES_CLINIC_APP_V36_SESSION_UPDATES.zip`

## 🚨 TK এখনো লাইভ টেস্ট করে "Pass" বলেননি — তাই নিরাপদ কপি এখনো V36।

---

## V43 — 🔴 আরেকটা কম্পাইল-এরর ফিক্স (TK আবার বিল্ড ফেইল দেখিয়েছিলেন)

`FollowUpActivity.kt`-তে `Unresolved reference: WRAP` / `MATCH`।

**আসল কারণ:** এই ফাইলে একটা পুরনো ফাংশন `showPaymentHistoryDialog` (Payment History পপ-আপ — এটা আমার এই সেশনের লেখা কোড না, আগে থেকেই এই ফাইলে ছিল) `MATCH`/`WRAP` নামের শর্টকাট ব্যবহার করেছিল, কিন্তু এই দুটো শুধু একটা **সম্পূর্ণ ভিন্ন ফাংশনে** (`buildFollowCard`) ডিফাইন করা ছিল — অন্য ফাংশন থেকে সেটা দেখা যায় না। এটা এই সেশনে তৈরি হয়নি, আগে থেকেই ভাঙা ছিল, internet/Gradle না থাকায় আমরা কখনো real compile করে ধরতে পারিনি।

**ফিক্স:** `showPaymentHistoryDialog` ফাংশনের নিজের ভেতরেও `MATCH`/`WRAP` ডিফাইন করে দেওয়া হয়েছে (ব্যবহারের আগেই)।

**পুরো প্রজেক্টে script দিয়ে খুঁজে দেখা হয়েছে — এই ধরনের ভুল (একটা ফাংশনে ডিফাইন করা কনস্ট্যান্ট অন্য ফাংশনে ব্যবহার) আর কোনো ফাইলে নেই, শুধু এই একটা জায়গাতেই ছিল।**

---

## V42 — 🔴 জরুরি কম্পাইল-এরর ফিক্স (TK-এর Android Studio বিল্ড ফেইল করেছিল)

TK Android Studio-তে বিল্ড করে দেখিয়েছিলেন: `InvestigationCategoryActivity.kt`-তে `Unresolved reference: dp` — Gradle build failed।

**আসল কারণ:** নতুন এই ফাইলে `dp()` নামের ছোট হেল্পার ফাংশনটা **ব্যবহার হওয়ার পরে ডিক্লেয়ার করা হয়েছিল** (লাইন ৩২-এ ব্যবহার, কিন্তু ডিক্লেয়ার লাইন ৫১-এ) — Kotlin-এ local function ব্যবহারের আগেই ডিক্লেয়ার করতে হয়, এটা মিস হয়েছিল।

**ফিক্স:** `dp()` ফাংশনটা `onCreate`-এর সবচেয়ে উপরে (প্রথম ব্যবহারের আগে) নিয়ে আসা হয়েছে।

**এই সেশনে নতুন লেখা প্রতিটা ফাইলে একই প্যাটার্নের ভুল আর কোথাও আছে কিনা আলাদাভাবে চেক করা হয়েছে (script দিয়ে) — আর কোথাও পাওয়া যায়নি।**

⚠️ **গুরুত্বপূর্ণ শিক্ষা:** brace/bracket গোনা দিয়ে এই ধরনের "ব্যবহারের আগে ডিক্লেয়ার হয়নি" ভুল ধরা পড়ে না — এরপর থেকে নতুন কোনো লোকাল হেল্পার ফাংশন লেখার সময় সবসময় ফাংশনের একদম শুরুতেই সেটা বসানো হবে, যাতে এই ভুল আর না হয়।

---

## V41-এ নতুন যা ফাইনাল হলো (V40-এর উপরে) — 🔒 ব্রাঞ্চ ডেটা-লিক নিরাপত্তা ফিক্স

TK নিজে ধরেছিলেন: Kishanganj স্টাফের লগ-ইনে Jalpaiguri-র একটা পেশেন্ট (Bill ₹0, Due ₹0) কার্ড দেখা যাচ্ছিল। পুরো প্রজেক্ট খুঁটিয়ে খুঁজে ৪টা জায়গায় root cause + একই ধরনের সমস্যা পাওয়া গেছে ও ঠিক করা হয়েছে:

### ১. আসল কারণ: `LocalWorkflowStore.upsertFollowUp()` — সাইলেন্ট ডেটা-লস
আগে এই ফাংশন পুরনো লোকাল রেকর্ডকে নতুন ডেটা দিয়ে **সম্পূর্ণ replace** করত। কোনো কারণে (যেমন cloud sync-এর কোনো row) branch ছাড়া ডেটা এলে, আগের সঠিক branch **মুছে ফাঁকা** হয়ে যেত — যদিও Enquiry/Registration ফর্মে branch বাছাই বাধ্যতামূলক, তাই ঢোকার সময় কখনো ফাঁকা হতে পারত না। এখন এটা **field-by-field merge** করে (Remark/Next-Follow আপডেটের মতোই নিরাপদ প্যাটার্ন) — কোনো ফিল্ড নতুন ডেটায় না থাকলে পুরনোটাই থেকে যায়, মুছে যায় না।

### ২. `FollowUpRepository.kt` — Follow-up ট্যাবে leak বন্ধ
আগে branch ফাঁকা থাকা রেকর্ড **সব ব্রাঞ্চের সবার কাছে** দেখা যেত (`rb.isBlank()` শর্ত)। এখন শুধু Master/All-branch viewer, একই ব্রাঞ্চ, বা যে creator বানিয়েছে সে-ই দেখবে।
**ঝুঁকি TK-কে জানানো হয়েছে ও গ্রহণ করা হয়েছে:** এর ফলে আগের যেসব রেকর্ডের branch ইতিমধ্যে ফাঁকা হয়ে গেছে (যেমন ETA ORAIN), সেগুলো এখন শুধু Master-এর কাছেই দেখা যাবে — TK-কে Registration/Enquiry খুলে branch আবার বসিয়ে সেভ করে ঠিক করতে হবে।

### ৩. `PaymentActivity.kt` — Payment এডিট-অনুমতিতে একই ধরনের leak
Payment রেকর্ডের branch ফাঁকা থাকলে যেকোনো ব্রাঞ্চের স্টাফ সেটা এডিট করতে পারত। এখন শুধু Master বা ঠিক নিজের ব্রাঞ্চ মিললে তবেই এডিট করা যাবে।

### ৪. `GlobalSearchActivity.kt` — Dashboard Search সবার জন্য একইরকম (TK approved, এটা bug না, ইচ্ছাকৃত)
আগে Doctor role-এর জন্য সার্চেও ব্রাঞ্চ-সীমাবদ্ধতা ছিল (Master/Staff ছিল না — অসামঞ্জস্য)। TK confirm করেছেন: Dashboard-এ নম্বর দিয়ে Search করলে **সব role-এর জন্য (Doctor-সহ) সব ব্রাঞ্চ একইভাবে দেখাবে** — এখন সেটাই হয়।

### যাচাই করে নিরাপদ পাওয়া গেছে (হাত দেওয়া হয়নি)
Doctor Queue, Doctor Visit, Draft List, Reports, Briefing — এই সবকটাই strict branch-filter ব্যবহার করে, কোথাও blank-branch leak নেই।

---

## আগের (V40 পর্যন্ত) সব কাজ অক্ষত আছে
Prescription/Medicine Slip ডেটা-মিক্স ফিক্স, Save/Save & Print, Medicine Type রঙিন বক্স+হাসপাতাল-স্টাইল প্রিন্ট, Blood Test ৯৮-টেস্ট ক্যাটেগরি স্ক্রিন + Common Blood Test, Patient Timeline সেকশন-অনুযায়ী ফিল্টার — সব আগের মতোই আছে, এই সেশনে ছোঁয়া হয়নি।

## Verification
- এই সেশনে ছোঁয়া **সবক'টা ১৮টা .kt ফাইল** brace/paren যাচাই — **PASS, zero mismatch**।
- সব বদলানো/নতুন .xml ও AndroidManifest.xml well-formed parse — **PASS**।
- ইন্টারনেট/Gradle না থাকায় real compile এখানে হয় না — TK Android Studio-তে বিল্ড করে টেস্ট করবেন। বিল্ড করার সময় কোনো Error না আসার জন্য ব্রেস/আইডি/রিসোর্স সব হাতে মিলিয়ে চেক করা হয়েছে (উপরে লেখা)।

## এখনো আলোচনা বাকি
- Patient Timeline-এর প্রিমিয়াম ভিজ্যুয়াল রিডিজাইন — মকআপ দেখানো হয়েছে, TK-এর "কোডে বসাও" কনফার্মেশন এখনো বাকি।


## V40-এ নতুন যা ফাইনাল হলো (V39-এর উপরে)

### Patient Timeline (View All) — সেকশন-অনুযায়ী দেখানো
TK approved — Follow-up-এর কোন ট্যাব থেকে 👁 চেপে খোলা হচ্ছে তার উপর ভিত্তি করে টাইমলাইনে শুধু প্রাসঙ্গিক তথ্য দেখাবে:
- **Enquiry ট্যাব** থেকে → শুধু "Enquiry created"
- **Visit ট্যাব** থেকে → "Registration / Visit" + "Advance" পেমেন্ট
- **Patient ট্যাব** থেকে → বাকি সব Payment (2nd/3rd...) + Medical (Prescription/Diet/Blood Test/Checkup)
- **Dashboard → Global Search** থেকে (বা অন্য যেকোনো জায়গা থেকে) → সব কিছু (A–Z), আগের মতোই — কোনো পরিবর্তন হয়নি।

ফাইল: `PatientTimelineRepository.kt` (`build()`-এ নতুন `section` প্যারামিটার + `matchesSection()`), `PatientTimelineActivity.kt` (intent থেকে "section" পড়া), `FollowUpActivity.kt` (`openTimelineFor()`-এ `currentStage` পাঠানো)।
**গুরুত্বপূর্ণ:** Registration/Enquiry-এর ভেতরে "Duplicate patient" পপ-আপের "View" বাটন এবং Draft List-এর "View" — এগুলো section পাঠায় না, তাই এখনো সব দেখাবে (আগের আচরণ অক্ষত)।

## আগের (V39) সব কাজ অক্ষত আছে
Prescription/Medicine Slip ডেটা-মিক্স ফিক্স, Save/Save & Print, Medicine Type রঙিন বক্স+হাসপাতাল-স্টাইল প্রিন্ট লাইন, Address-লাইন স্পেসিং ফিক্স, Blood Test ৯৮-টেস্ট ক্যাটেগরি স্ক্রিন + Common Blood Test — সব আগের মতোই আছে, এই সেশনে ছোঁয়া হয়নি।

## Verification
- এই সেশনে নতুন ছোঁয়া ৩টা .kt ফাইল brace/paren যাচাই — **PASS**।
- আগের সেশনের সব ফাইলও পুনরায় PASS (উপরে অপরিবর্তিত হিসেবে confirmed)।
- ইন্টারনেট/Gradle না থাকায় real compile এখানে হয় না — TK Android Studio-তে বিল্ড করে টেস্ট করবেন।

## এখনো আলোচনা বাকি
- Patient Timeline-এর প্রিমিয়াম ভিজ্যুয়াল রিডিজাইন (নেভি হেডার+initials অ্যাভাটার+connected টাইমলাইন লাইন) — মকআপ দেখানো হয়েছে, TK-এর চূড়ান্ত "কোডে বসাও" কনফার্মেশন এখনো বাকি।


## এই সেশনে যা ফাইনাল হয়েছে (TK approved)

### ১. Prescription ↔ Medicine Slip ডেটা মিক্সিং বাগ — Root cause fix
আগে দুটো স্ক্রিন একই লিস্ট শেয়ার করত। এখন সম্পূর্ণ আলাদা: `currentPrescription` (Ayurvedic) vs `currentSlip` (Allopathic) — নতুন। Print-ও এখন সঠিক লিস্ট থেকে হয়।

### ২. Prescription স্ক্রিন
- "Generate Medicine Slip" বাটন বাদ (Prescription আর Slip সম্পূর্ণ আলাদা ডকুমেন্ট) — বদলে **Save** / **Save & Print** (Prescription-ই প্রিন্ট করে)।
- Clinical Document গ্রিড থেকে "Prescription" চাপলে সরাসরি "Add from Reference List" খোলে (মাঝের খালি স্ক্রিন বাদ)। ওষুধ বেছে Dose লিখে "Add ✓" চাপলেই সরাসরি সেভ হয়ে স্ক্রিন বন্ধ (ক্লাউড সেভ শেষ হওয়ার পরই বন্ধ হয়, তাড়াহুড়োয় ডেটা হারানোর ঝুঁকি নেই)। Dose ঘর আগের মতোই ২-লাইন ফরম্যাটে (নাম + Dose একলাইনে টাইপ করা যায়, যেমন "2 BDPC × 7 Days") — আলাদা Frequency/Days ঘর যোগ করা হয়নি (TK না চাওয়ায়)।

### ৩. Medicine Slip স্ক্রিন
"Medicine Slip" চাপলে এখন সরাসরি "Add from Medicine List" পপ-আপ খোলে (মাঝের খালি "No medicines added yet" প্রিভিউ প্রথমে দেখায় না)। বাকি স্ক্রিন (Share as Text / Print) অপরিবর্তিত।

### ৪. Medicine Type বক্স (Tab/Cap/Syp/Oint/Inj/Other)
- ওষুধ যোগ করার সময় (Prescription ও Medicine Slip দুটোতেই, Reference List ও Outside List দুটো ডায়ালগেই) Dose-এর পাশে একটা চিপ ট্যাপ করে Type বাছা যায়। একবার বাছলে সেই ওষুধের জন্য চিরকাল মনে থাকে (rememberRxType) — বারবার জিজ্ঞেস করে না। প্রথমে ফাঁকা থাকে, Claude নিজে থেকে কোনো টাইপ অনুমান করেনি।
- প্রিন্টে (Prescription ও Medicine Slip) প্রতিটা ওষুধের আগে রঙিন Type বক্স বসে (TAB=নীল, CAP=বেগুনি, SYP=সোনালি, OINT=টিল, INJ=লাল, OTHER=ধূসর), তারপর নাম, তারপর ছোট দাগ (badge রঙের সাথে মেলানো), তারপর Dose/Frequency/Days — হাসপাতাল-প্রেসক্রিপশন স্টাইলে (TK approved mockup অনুযায়ী)।
- এই বদল শুধু Rx/Slip সেকশনেই সীমাবদ্ধ — Registration, Diet Chart, Blood Test প্রিন্ট ইত্যাদি আগের মতোই অপরিবর্তিত (কোড-লেভেলে আলাদা করে রাখা হয়েছে, `PrintSection.rxTypes`/`rxNames` null থাকলে পুরনো রেন্ডারিং-ই হয়)।

### ৫. প্রিন্টের হেডার — Address লাইন ফিক্স
Patient info বক্সে "Address" লাইনটা বক্সের নিচের দাগের সাথে প্রায় লেগে যাচ্ছিল (মাত্র ২pt গ্যাপ ছিল) — এখন ৬pt গ্যাপ, আর কিছু ছোঁয়া হয়নি।

### ৬. Blood Test / Investigation — সম্পূর্ণ নতুন ক্যাটেগরি-ভিত্তিক স্ক্রিন
- TK-এর ল্যাব রেফারেন্স ছবি থেকে প্রতিটা টেস্ট হাতে গুনে-মিলিয়ে ৮টা ক্যাটেগরিতে সাজানো হয়েছে: **Hematology(13, CBC-সহ যা আগে ছিল), Bio-Chemistry(30), Immunology(14), Special Test(29), Urine(5), Stool(2), Semen(1), Imaging(4, সম্পূর্ণ আগে থেকে ছিল)** = মোট ৯৮টা টেস্ট। কোনো নাম Claude নিজে থেকে বানায়নি — যা ছবিতে ছিল তাই, ইংরেজিতেই।
- Clinical Document গ্রিড থেকে "Blood Test" চাপলে সরাসরি এই ক্যাটেগরি-গ্রিড স্ক্রিন খোলে (২-কলাম, প্রিমিয়াম গ্র্যাডিয়েন্ট কার্ড)। কার্ডে ট্যাপ করলে সেই ক্যাটেগরির চেকলিস্ট আলাদা স্ক্রিনে খোলে (`InvestigationCategoryActivity`, নতুন)।
- উপরে "⭐ Apply Common Blood Test" বাটন — গত বার যা Save করা হয়েছিল ঠিক সেই সেট এক-ট্যাপে টিক করে দেয় (প্রথমবার ফাঁকা/কাজ করে না, TK একবার Save করলে তখন থেকে কাজ করে; পরের প্রতিটা Save-এ এই "Common" সেট আপডেট হয়ে যায়)।
- Approve/Save/Print লজিক অপরিবর্তিত — আগের মতোই `ClinicalRepository.currentInvestigations`-এই সব সেভ হয়, তাই Print (`PrintMappers.investigationAdvice`) আগের মতোই কাজ করবে।

## পরিবর্তিত/নতুন ফাইল
- `ClinicalRepository.kt`, `ClinicalModels.kt`
- `MedicinePickerDialog.kt`, `MedicineSlipActivity.kt`, `PrescriptionActivity.kt`
- `InvestigationAdviceActivity.kt` (rewrite), `InvestigationCategoryActivity.kt` (নতুন)
- `PrintDocumentModel.kt`, `PrintMappers.kt`, `PrintCenterActivity.kt`, `ClinicPdfBuilder.kt`
- `layout/activity_prescription.xml`, `layout/activity_investigation_advice.xml`
- `AndroidManifest.xml` (নতুন Activity রেজিস্টার্ড)

## Verification (compile-error এড়াতে)
- এই সেশনে ছোঁয়া প্রতিটা .kt ফাইল (১১টা) brace `{}` / bracket `()` মিলিয়ে যাচাই — **PASS, zero mismatch**।
- বদলানো প্রতিটা .xml (activity_prescription, activity_investigation_advice, AndroidManifest) well-formed parse — **PASS**।
- Kotlin ফাইলে ব্যবহৃত সব `R.id.*` XML-এ আছে কিনা cross-check — **PASS**, একটাও missing id নেই।
- ব্যবহৃত সব `@drawable/...` ফোল্ডারে আছে কিনা cross-check (bg_app_gradient, bg_login_hero) — **PASS**।
- ইন্টারনেট/Gradle না থাকায় real compile/APK এখানে করা যায় না — TK নিজে Android Studio-তে বিল্ড করে চূড়ান্ত টেস্ট করবেন।

## এখনো আলোচনা বাকি
- Blood Test-এর ৩ডি/গ্লো অ্যানিমেশন (হেডার ফেড-ইন, বাটনে glow-pulse, কার্ড স্ট্যাগার এন্ট্রি, ট্যাপে scale) — ডিজাইন/GIF proof দেখানো হয়েছে, TK-এর চূড়ান্ত "ঠিক আছে" এখনো বাকি ছিল যখন সেশন শেষ হলো।
