# PILES CLINIC APP — V36 চেঞ্জলগ ও স্থায়ী লক নোট (FINAL, এই সেশনের শেষ আপডেট)
(পরবর্তী যেকোনো Developer/AI/সেশন — কাজ শুরুর আগে এই গোটা ফাইলটা প্রথমে পড়বে)

## 🚫 এই ফাইলের সবচেয়ে গুরুত্বপূর্ণ নিয়ম (TK-এর নিজের কথায়, বদলানো যাবে না)
- এই প্রজেক্টের **কোনো কিছু owner (TK)-কে না জানিয়ে ধ্বংস/মুছে ফেলা যাবে না।**
- **কোনো ডিজাইন owner-কে না জানিয়ে খারাপ/বদলানো যাবে না।**
- পরবর্তীতে TK যেটুকু আপডেট করতে বলবেন, **শুধু ততটুকুই** করতে হবে — তাও **TK-র অনুমতি ছাড়া না।**
- **TK-কে প্রুফ না দেখিয়ে কোনো কাজ (ফাইল পাঠানো/লক করা) করা যাবে না।**
- **একটা ইস্যু ঠিক করতে গিয়ে দশটা নতুন ইস্যু তৈরি করা যাবে না** — যেটুকু বলা হয়েছে ঠিক ততটুকুই, সাবধানে, বাকি সব অক্ষত রেখে ঠিক করতে হবে।
- **Android Studio-তে APK Build করার সময় কোনো Error যেন না আসে** — প্রতিটা পরিবর্তিত/নতুন ফাইল ডেলিভারির আগে brace/bracket/XML/resource-reference মিলিয়ে চেক করে নিতে হবে (এই সেশনে করা হয়েছে, নিচে "Verification" অংশে প্রমাণ)।

## Base ফাইল
`PILES_CLINIC_APP_V35_REAL_APPROVED_UPDATES.zip`

## 🚨 অ্যাপ ক্র্যাশ করলে / সমস্যা হলে কোন ফাইলে ফিরে যেতে হবে
এই V36-এর সব ফিক্স এখনও TK লাইভ টেস্ট করে "Pass" বলেননি — তাই নিরাপদ/base কপি এখনো
**`PILES_CLINIC_APP_V35_REAL_APPROVED_UPDATES.zip`**। TK যখন V36 লাইভ টেস্ট করে
"ঠিক আছে/পাশ" বলবেন, তখন থেকে এই V36-ই নতুন safe base — পরের সেশনে এটাই ধরে এগোতে হবে।

---

## এই সেশনে যা যা বাস্তবে ঠিক/পরিবর্তন করা হয়েছে

### Follow-up কার্ড (Enquiry/Visit/Patient)
1. Visit-Advance বাটন: "👣 Visit / Advance" → এক লাইনে "💰 Advance Here"।
2. Advance Payment পপ-আপ প্রিমিয়াম (নেভি হেডার, গোলাকার ইনপুট, সবুজ Save) — সব ফিল্ড/লজিক অটুট।
3. Visit কার্ডের VISITED ব্যাজে ৩-ট্যাপ এখন সঠিকভাবে Continue/Reject মেনু খোলে (আগে ভুল করে Photo Editor খুলত)। ছবিতে ৩-ট্যাপ = Photo Editor, অপরিবর্তিত।
4. Draft → Restore বাটনের অদৃশ্য (সাদা-অন-সাদা) লেখা ফিক্স, রঙ #0B4F2A।
5. Bill সম্পূর্ণ পরিশোধ (Due ₹0) হলে সেই রোগী Patient ফলো-আপ ট্যাব থেকে বাদ, শুধু Draft-এর Complete লিস্টে থাকবে।
6. Clinical Document পপ-আপ প্রিমিয়াম — Prescription (সবুজ)/Medicine Slip (নীল) বড়/প্রধান সারিতে, Blood Test/Diet Chart ছোট/সেকেন্ডারি সারিতে, হালকা scale+fade অ্যানিমেশন।
7. Prescription/Medicine Slip মেডিসিন-বাছাই সম্পূর্ণ নতুন প্রিমিয়াম ডিজাইন (`MedicinePickerDialog.kt` শেয়ার্ড ফাইল): সার্চ বক্স, ডিফল্ট লিস্ট অপরিবর্তিত (একটা নামও যোগ/বদল করা হয়নি), সার্চ-করা/আগে-অ্যাড-করা নাম শুধু সার্চেই দেখাবে, "Add as new" দিয়ে TK/স্টাফ নিজে যা টাইপ করবেন শুধু সেটাই মনে রাখা হবে (Claude/AI নিজে থেকে কোনো ওষুধের নাম বানায়নি), নিচে সবসময়-দেখা Add/Cancel বার।
8. প্রিন্ট হেডার: ক্লিনিকের নাম/ঠিকানা/মোবাইল এখন লোগো ও পাতার মাঝখানে center — বাকি প্রিন্ট ডিজাইন (আগে ফাইনাল করা) অপরিবর্তিত।
9. প্রিন্টে ℞ ওয়াটারমার্কের সাথে মেডিসিন-লেখা ওভারল্যাপ হওয়া বন্ধ — এখন পরিষ্কার ফাঁকা জায়গা।
10. **[TK খুঁজে বার করা, root-cause fix]** ভুল "Patient ID" (internal `refId` কোড, যেমন "pat_f76c...") leak — Visit ট্যাবেও এখন Patient ট্যাবের মতোই আসল Patient ID "patients" টেবিল থেকে টানে; `refId`-কে patientId-এর ব্যাকআপ হিসেবে ব্যবহার বন্ধ (এই ভুল শুধু দেখানোই না, নতুন patient তৈরির সময় ভুল ID সেভও হয়ে যেতে পারত — এখন বন্ধ)।
11. Patient ID এখন এক লাইনেই থাকে (ভাঙে না), জায়গা কম পড়লে লেখা নিজে থেকে ছোট হয়ে পুরোটা দেখায় (আগে silently কাটা পড়ত)।

### Visit Card-এর Advance Payment পপ-আপ (আলাদা রিফাইনমেন্ট, একাধিক রাউন্ডে TK-approved)
12. Total Amount লক এখন **শুধু** আসল Advance টাকা সেভ হলেই হয় — অন্য কোনো কারণে (যেমন Registration Fee) লক হয় না।
13. Total/Advance বক্সের **যেকোনো জায়গায়** চাপলেই কিবোর্ড খোলে (শুধু সংখ্যায় চাপতে হয় না)।
14. Advance = 0 হলে সেভ হয় না, লক হয় না, Patient কার্ডেও যায় না।
15. "₹0" প্লেসহোল্ডার সরানো — ফাঁকা থাকলে হালকা রঙে "Enter amount" দেখায়, আসল জিরো মনে হয় না।
16. সেভ সফল হলে অটো Patient ট্যাবে — আলাদা Payment স্ক্রিন লাগে না।

### Patient Card-এর Payment Ring + নতুন Payment popup (নতুন এই সেশনে তৈরি)
17. পুরনো সলিড-ভরা % সার্কেলের বদলে আসল আংশিক-ভরা প্রোগ্রেস রিং (`PaymentRingView.kt`, নতুন কাস্টম View) — % অনুযায়ী ঠিক ততটুকুই ভরবে, মাঝে সংখ্যা।
18. Ring-এ ট্যাপ করলে এখন সরাসরি একটা ছোট প্রিমিয়াম "Nth Payment" পপ-আপ খোলে (`dialog_nth_payment.xml`, নতুন) — পুরো Payment Collection স্ক্রিন খোলে না।
    - উপরে: পেশেন্টের ছবি + নাম + Patient ID + মোবাইল + ঠিকানা, কমপ্যাক্ট এক লাইনে (পপ-আপের জন্য বেশি জায়গা নেয় না)।
    - উপরে ডান কোণে ছোট সোনালী 📜 ব্যাজ — চাপলে **শুধু এই পেশেন্টের** সম্পূর্ণ Payment History (তারিখ, কততম কিস্তি, টাকা, মোড) আলাদা পপ-আপে দেখায়।
    - Total Bill: silent ৩-ট্যাপে এডিট (কোথাও "৩ বার চাপুন" লেখা নেই, আগের প্যাটার্নের মতোই)।
    - Already Paid + Due: এক বক্সে পাশাপাশি (সবুজ/লাল), এই দুটো auto-calculated — সরাসরি এডিট করার ঘর রাখা হয়নি (ভুল হিসেব সেভ হওয়ার ঝুঁকি এড়াতে)।
    - This Payment: বক্সের যেকোনো জায়গায় চাপলেই কাজ করে, 0 দিলে Save হয় না, কিস্তির নম্বর (Advance/2nd/3rd…) বাকি অ্যাপের মতোই একই কাউন্টার থেকে সঠিকভাবে গোনা হয়।
    - Save হলে Follow-up লিস্ট নিজে থেকে রিফ্রেশ হয়।

## পরিবর্তিত/নতুন Source ও Resource ফাইল
- `FollowUpActivity.kt`, `FollowUpRepository.kt`, `FollowUpModel.kt`
- `PaymentRingView.kt` — নতুন
- `clinical/MedicinePickerDialog.kt` — নতুন, `clinical/ClinicalRepository.kt`
- `clinical/PrescriptionActivity.kt`, `clinical/MedicineSlipActivity.kt`
- `print/ClinicPdfBuilder.kt`
- `layout/item_draft_card.xml`, `layout/dialog_advance.xml`
- `layout/dialog_nth_payment.xml` — নতুন
- `drawable/bg_header_navy_top_round.xml`, `drawable/bg_badge_gold.xml` — নতুন

## Verification performed here (Google Android Studio build error এড়াতে)
- সমস্ত পরিবর্তিত/নতুন Kotlin ফাইল: brace `{}` ও bracket `()` কাউন্ট মিলিয়ে zero-mismatch যাচাই — PASS (সব ৯টা .kt ফাইল)
- সমস্ত পরিবর্তিত/নতুন XML: well-formed parse যাচাই — PASS (৫টা XML)
- `dialog_nth_payment.xml` ও `dialog_advance.xml`-এর প্রতিটা `R.id.*` Kotlin-এ যা ব্যবহার হয়েছে, তার প্রতিটা XML-এ সত্যিই আছে কিনা — cross-check করে PASS, একটাও missing id নেই
- সব নতুন/পরিবর্তিত ফাইলে ব্যবহৃত প্রতিটা `@drawable/...` রিসোর্স আসলে ফোল্ডারে আছে কিনা — cross-check করে PASS, একটাও missing drawable নেই
- এখানে internet/Gradle না থাকায় real compile/APK করা যায় না — TK নিজে Android Studio-তে বিল্ড করে চূড়ান্ত টেস্ট করবেন। উপরের চেকগুলো compile-error-এর সবচেয়ে সাধারণ কারণগুলো (unmatched braces, broken XML, missing id/drawable reference) আগেই ধরে ফেলার জন্য করা হয়েছে।

## এখনো আলোচনা বাকি (পরের সেশনে)
- Prescription / Medicine Slip নিয়ে বিস্তারিত আলোচনা (Patient Card অধ্যায়ের পর হওয়ার কথা ছিল) — এখনো শুরু হয়নি।
