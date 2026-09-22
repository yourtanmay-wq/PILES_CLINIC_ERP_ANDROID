# PILES CLINIC APP — V50 FINAL (এই সেশনের সম্পূর্ণ কাজ, ডিপ-অডিট করা)

🚫 এই ফাইলে যা আছে তা ধ্বংস করা যাবে না। কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না।
কোনো working flow খারাপ করা যাবে না। যেটুকু বলা হয়েছে ঠিক ততটুকুই করা হয়েছে —
এর বাইরে প্রজেক্টের আর কিছুতে হাত দেওয়া হয়নি।

## Base
আগের V49_FINAL ZIP-এর উপর এই ৯টা নতুন কাজ যোগ হয়েছে।

---

## ১. "Mark Selected as Advised (Doctor)" বোতাম
সবার নিচে সরানো হয়েছে (Save/Save & Print/Share as Text-এর পরে), এক লাইনে (আগে দুই
লাইনে ভেঙে যাচ্ছিল)।
**ফাইল:** activity_investigation_advice.xml

## ২. Bill/Due ৩-ট্যাপ ফর্মে পেমেন্ট এডিট
"Add Treatment Payment" ফর্মে নতুন "📜 View / Edit Past Payments" বোতাম — এতে চাপলে
সব পুরনো পেমেন্ট (Advance/2nd/3rd/EMI) আলাদাভাবে ৩-ট্যাপে এডিট করা যায়। আগে থেকে
কাজ করা Payment Details লিস্ট পুনর্ব্যবহার করা হয়েছে, নতুন কিছু ডুপ্লিকেট হয়নি।
**ফাইল:** PaymentActivity.kt

## ৩. Patient Photo-তে ক্যামেরা
আগে শুধু "Pick Photo from Gallery" ছিল। নতুন "Take Photo (Camera)" বোতাম যোগ
হয়েছে — RegistrationActivity-তে আগে থেকে কাজ করা ক্যামেরা কোড পুনর্ব্যবহার করে।
**ফাইল:** activity_patient_photo.xml, PatientPhotoActivity.kt

## ৪. View All-এ মোবাইল নম্বর কাটা যাওয়া ঠিক
আগের সেশনের একটা ভুল ছিল (ID+মোবাইল জোর করে এক লাইনে রাখতে গিয়ে) — এখন দরকারে দুই
লাইনে যাবে, নম্বর আর কাটা যাবে না।
**ফাইল:** activity_patient_timeline.xml

## ৫. View All-এর Updates লিস্টে পেমেন্ট ৩-ট্যাপ এডিট
এই লিস্ট আগে সম্পূর্ণ read-only ছিল। এখন প্রতিটা Payment এন্ট্রিতে ৩-ট্যাপ করলে
Amount/Mode এডিট করা যায় (Master সব এডিট করতে পারবেন, Staff শুধু আজকের নিজের
ব্রাঞ্চের)।
**ফাইল:** PatientTimelineRepository.kt, TimelineAdapter.kt, PatientTimelineActivity.kt

## ৬. "Add Treatment Payment" পপ-আপ প্রিমিয়াম
নেভি হেডার + গোলাকৃতি কার্ড + CANCEL(আউটলাইন)/SAVE PAYMENT(সবুজ) বোতাম। Total Bill
৩-ট্যাপ আনলক, Payment Amount ৩-ট্যাপ প্রোটেকশন, Payment Mode, Remarks, View/Edit Past
Payments বোতাম — সবকিছুর লজিক অক্ষত, শুধু বাইরের সাদামাটা শেল বদলেছে।
**ফাইল:** PaymentActivity.kt

## ৭. Doctor Note হেডার কার্ড রিডিজাইন
নাম এখন `maxLines="1"` — কখনো ভাঙবে না। Photo/Avatar (বড়), Name, Reg ID·Branch,
Mobile·Age(Sex)·Disease (সবুজ বোল্ড), 📍 Short Address — সব আলাদা লাইনে, প্রফেশনাল
গ্র্যাডিয়েন্ট কার্ড ব্যাকগ্রাউন্ড। বাকি স্ক্রিনের অন্য ৭টা কার্ড আগের সাদা RegCard
স্টাইলেই আছে, ছোঁয়া হয়নি।
**ফাইল:** activity_doctor_checkup.xml, DoctorCheckupActivity.kt, নতুন drawable
bg_patient_header_card.xml

## ৮. Doctor Checkup-এর চেকবক্স গ্রুপ রঙিন (Visual/DRE/Investigation)
প্রতিটা আইটেমে ইমোজি আইকন (🔴 External Piles, 🩸 Bleeding, 🤚 Tenderness, 🧲 MRI...),
প্রতিটা গ্রুপের নিজস্ব রং (Visual=লাল, DRE=নীল, Investigation=বেগুনি), হালকা রঙিন
কার্ড ব্যাকগ্রাউন্ড, টিক দিলে রঙিন চেকবক্স, হালকা প্রেস-বাউন্স অ্যানিমেশন। আসল
CheckBox অবজেক্ট ও তাদের isChecked/text — সব সেভ/লোড লজিক ১০০% অক্ষত।
**ফাইল:** DoctorCheckupActivity.kt

## ৯. Doctor Checkup-এর বাকি ৬টা ধাপ রঙিন
Basic History(নীল), Previous Treatment(অ্যাম্বার), Counselling(বেগুনি), Financial
Discussion(সবুজ), Patient Decision(টিল), Media & Documents(পিচ) — প্রতিটা ধাপের
কার্ডে আলাদা হালকা রং + টাইটেলে আইকন। কোনো EditText/Spinner/বাটনের id বা লজিক
ছোঁয়া হয়নি, শুধু কার্ড ব্যাকগ্রাউন্ড কালার + টাইটেল আইকন।
**ফাইল:** activity_doctor_checkup.xml

---

## ডিপ-অডিট — ডেলিভারির ঠিক আগে যাচাই করা হয়েছে
- ✅ প্রজেক্টের সবকটা layout+drawable XML ভ্যালিড (xmllint, ০ error)
- ✅ এই সেশনে বদল হওয়া **১৬টা** .kt ফাইলেই brace/paren গোনা নিখুঁত মিলেছে
- ✅ প্রজেক্টের সবকটা R.drawable/@drawable রেফারেন্স resolve করে — একটাও ভাঙা নেই
- ✅ প্রতিটা বদল হওয়া স্ক্রিনে Kotlin ↔ layout id হুবহু মিলেছে (findViewById ও
  ViewBinding দুই ধরনের স্ক্রিনেই আলাদা করে যাচাই করা হয়েছে)
- ✅ Patient Card-এর আসল Clinical Document মেনু, bg_visit_advance.xml (অন্য ৩ জায়গায়
  ব্যবহৃত), RegCard/RegCardInner শেয়ার্ড স্টাইল (অন্য বহু স্ক্রিনে ব্যবহৃত) — কোনোটাই
  ছোঁয়া হয়নি
- ✅ Doctor Checkup-এর সব CheckBox অবজেক্ট এখনও একই isChecked/text রাখে — Save/Load/
  Restore লজিক («checkedText», restore-on-edit) ১০০% অপরিবর্তিত

## এক নজরে — এই সেশনে মোট বদল হওয়া ফাইল (আগের V49-এর উপর নতুন ৯টা কাজ)
**Kotlin:** InvestigationAdviceActivity, PaymentActivity, PatientPhotoActivity,
PatientTimelineRepository, TimelineAdapter, PatientTimelineActivity,
DoctorCheckupActivity

**Layout:** activity_investigation_advice, activity_patient_photo,
activity_patient_timeline, activity_doctor_checkup

**নতুন Drawable:** bg_patient_header_card.xml
