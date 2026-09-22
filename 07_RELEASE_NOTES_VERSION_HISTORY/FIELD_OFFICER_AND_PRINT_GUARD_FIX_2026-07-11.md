# CHANGE NOTE — 2026-07-11 (2টি contained fix)

> এই পরিবেশে অ্যাপ **build/run করা যায় না** (gradle/SDK/net নেই)। কোড
> পরিবর্তন pattern মেনে ও brace-balance যাচাই করে করা হয়েছে, কিন্তু চূড়ান্ত
> নিশ্চয়তার জন্য Android Studio-তে একবার Debug build করতে হবে।

---

## ফাইল ১ — StaffDirectory.kt
**পথ:** `app/src/main/java/com/tkbiswas/pilesclinic/native/StaffDirectory.kt`

**সমস্যা (mismatch #1):** web `config.js`-এ Field Officer
(`9002003540`, role `field`) login ছিল, কিন্তু native directory-তে ছিল না —
তাই field user native অ্যাপে ঢুকতে পারত না (যদিও field role-এর tile ও
password আগে থেকেই wired ছিল)।

**ঠিক করা হলো:**
- নতুন `field` তালিকা যোগ: `StaffAccount("9002003540", "Field Officer", "All", "field")`
- `all = master + staff + doctor + field` করা হলো।

**ফল:** web ও native-এ এখন হুবহু ১৯টি login (diff = 0)। Login + Password
Center দুই জায়গাতেই Field Officer দেখাবে/কাজ করবে। `field→field123`
password map-এ আগেই ছিল, তাই আর কিছু লাগেনি।

**যা ছোঁয়া হয়নি:** অন্য কোনো account, password, role, UI।

---

## ফাইল ২ — PrintCenterActivity.kt
**পথ:** `app/src/main/java/com/tkbiswas/pilesclinic/print/PrintCenterActivity.kt`

**সমস্যা (mismatch #2, মূল bug):** Print Center-এ Prescription / Medicine
Slip / Diet Chart — এই ৩টি print শুধু *current in-memory clinical session*
থেকে তৈরি হয়। active session না থাকলে (app restart / সরাসরি Print খোলা)
এগুলো খালি বা ভুল document print করত।

**ঠিক করা হলো (নিরাপদ guard):**
- নতুন helper `hasActiveClinicalPatient()` — RoleSession-এ সত্যিকারের রোগী
  লোড আছে কিনা দেখে।
- ৩টি card-এ guard: রোগী-session না থাকলে বা working list খালি হলে print না
  করে একটি পরিষ্কার বার্তা দেখায় ("আগে Doctor Queue থেকে রোগী খুলে
  Checkup/Prescription/Diet-এ তথ্য যোগ করুন")।
- Registration / Payment / Doctor Visit / Blood Test print অপরিবর্তিত
  (ওগুলো আগে থেকেই mobile দিয়ে Supabase খোঁজে)।

**যা ছোঁয়া হয়নি:** print layout, PDF builder, অন্য ৪টি print type, branch toggle।

---

## এখনো বাকি (আপনার সিদ্ধান্ত / বড় কাজ — এই দফায় করা হয়নি)
- **#2-full:** Prescription/Diet-কে ওয়েবের মতো *যেকোনো পুরনো রোগীর* saved
  Rx মোবাইল দিয়ে খুঁজে print করা (নতুন feature, বড়)।
- **#3 Public Site:** login-এর আগের রোগী-facing website — staff APK-তে
  লাগবে কিনা সিদ্ধান্ত দরকার।
- **#5 Offline cache:** ~৯টি online-only screen-এ Room যোগ (build/test ছাড়া
  ঝুঁকিপূর্ণ; আলাদা phase)।
- **#6 creator-override:** staff অন্য branch-এ থাকলেও নিজের তৈরি record দেখা।

## আগেই ঠিক ছিল (রিপোর্টে সংশোধন)
- **#4 RoleSession "Demo Patient" fallback** — এই version-এ default আগেই
  খালি ("") করা হয়ে গেছে; শুধু ফাইলের উপরের কমেন্ট পুরনো। আলাদা করে কিছু
  করতে হয়নি।

## Build-এর পর যাচাই করবেন
- [ ] Field Officer (9002003540 / field123) দিয়ে login হয় কিনা।
- [ ] Password Center-এ Field Officer দেখায় কিনা।
- [ ] Print Center সরাসরি খুলে Prescription/Diet/Slip চাপলে খালি print না
      হয়ে বার্তা আসে কিনা।
- [ ] Doctor Queue → রোগী → Prescription যোগ করে Print করলে ঠিক আসে কিনা।

---

## যোগ হলো (দ্বিতীয় দফা) — #2-full: Prescription/Diet/Medicine Slip patient-lookup print

**ফাইল:**
- `print/PrintMappersCloud.kt` — নতুন ৩টি mapper: `prescriptionFromMedical`,
  `medicineSlipFromMedical`, `dietFromMedical` (saved `medical` row → print model)।
- `print/PrintCenterActivity.kt` — Prescription/Medicine Slip/Diet card এখন:
  active clinical session থাকলে সেটা print করে (দ্রুত পথ, doctor-এর জন্য),
  নাহলে **mobile দিয়ে রোগী খুঁজে তার সর্বশেষ saved Prescription/Diet**
  Supabase `medical` টেবিল থেকে এনে print করে — ওয়েবের printRx()/printDiet()-এর মতো।

**ফল:** এখন যেকোনো পুরনো রোগীর Rx/Diet Print Center থেকে mobile দিয়ে ছাপা যায়,
আবার doctor-এর live workflow-ও অক্ষত।

**যাচাই (build-এর পর):**
- [ ] Doctor Queue → রোগী → Prescription যোগ → Print Center → Prescription →
      current session ঠিক ছাপে কিনা।
- [ ] Print Center সরাসরি → Prescription → mobile দিলে সেই রোগীর saved Rx আসে কিনা।
- [ ] saved Diet নেই এমন রোগীতে পরিষ্কার "No saved ... yet" বার্তা আসে কিনা।

---

## এখনো বাকি — কেন এই দফায় blind করা হয়নি (সৎ কারণ)

- **#3 Public Site** — এটা product সিদ্ধান্ত: staff-only APK-তে রোগী-facing
  public website আদৌ দরকার? (ওয়েবে একই codebase রোগী+staff দুইয়ের, তাই সেখানে
  আছে; native APK শুধু staff-এর।) মালিকের সিদ্ধান্ত পেলে আলাদা pre-login Activity
  হিসেবে বানানো যাবে (পুরনো screen ভাঙবে না)।

- **#5 Offline cache** — ৯টি screen-এ Room+sync; build/test ছাড়া blind দিলে
  চালু screen ভাঙার আসল ঝুঁকি। প্রথম সফল Debug build-এর পর screen ধরে ধরে করা
  একমাত্র নিরাপদ পথ।

- **#6 creator-override** — native `followups` row-এ `createdBy`/`receivedBy`
  field **নেই** (আছে শুধু `staff` = নাম)। Web rule mobile মেলায়। ঠিকভাবে করতে
  হলে (ক) followups টেবিলে `createdBy` column থাকা নিশ্চিত করতে হবে, (খ) native
  save-এ সেটা লিখতে হবে। **কিন্তু column না থাকলে সেই write enquiry save-ই ভেঙে
  দেবে (Supabase 400)।** তাই আগে Supabase schema যাচাই দরকার — তারপর নিরাপদে
  read+write দুটোই করা যাবে।

---

## যোগ হলো (তৃতীয় দফা) — #6 creator-override
**ফাইল:** `native/FollowUpRepository.kt` (+ caller `FollowUpActivity.kt`,
`FollowCalendarActivity.kt`)। fetchTab এখন নিজের branch ছাড়াও ওই user-এর নামে
(`staff` field) তৈরি follow-up rows এনে merge করে — schema পরিবর্তন ছাড়াই।
ভুল হলেও fetchList খালি ফেরায়, তাই মূল লিস্ট ভাঙে না বা duplicate হয় না।

---

## যোগ হলো (চতুর্থ দফা) — WORKFLOW FIX: Doctor Queue এখন clear হয়

**আসল সমস্যা:** native-এ কোথাও `doctorComplete=true` সেট হতো না। ফলে register
হওয়া প্রতিটা রোগী চিরকাল Doctor Queue-তে জমে থাকত — ডাক্তার checkup করলেও
queue থেকে সরত না। (ওয়েবে ডাক্তারের decision "Treatment Started" হলে
doctorComplete=true হয়ে রোগী queue ছাড়ে।)

**ফাইল:** `clinical/DoctorCheckupActivity.kt`
- checkup save-এ decision **"Agree for Treatment"** (ওয়েবের "Treatment Started"-এর
  সমতুল্য) হলে, রোগীর patient row খুঁজে `doctorComplete=true` সেট করে — রোগী
  Doctor Queue থেকে সরে যায়। best-effort, error হলেও checkup save ভাঙে না।

**যাচাই (build-এর পর):** রোগীকে Doctor Queue-তে খুলে Checkup-এ decision
"Agree for Treatment" দিয়ে save → রোগী queue থেকে চলে যায় কিনা দেখুন। অন্য
decision-এ (Will Think ইত্যাদি) queue-তেই থাকবে (ঠিক আচরণ)।

## যা মিলিয়ে দেখা হলো, ঠিক আছে
- Enquiry→Registration: stage ঠিকভাবে Inquiry→Registered হয়, Visit card তৈরি হয়।
- Payment/bill দিলে follow-up Visit→Treatment stage-এ যায় (Patient tab-এ আসে)।
- টাকার হিসাব (%, due, conversion, call-signal, badge) ওয়েবের সাথে মেলে।

---

## যোগ হলো — GLOBAL রুল: তিন চাপ = Edit
- নতুন `native/TripleTapEdit.kt` — reusable helper। যেকোনো view-তে বসালে
  দ্রুত তিন চাপে edit খোলে (২য় চাপে "Tap 1 more time to edit")।
- **Follow-up card** (Enquiry/Visit/Patient তিন tab-ই): নামে তিন চাপ →
  Name / Mobile / Branch / Disease edit → followups row আপডেট + best-effort
  patients/enquiries rows-ও মেলানো।
- **Payment**: bill/amount তিন-চাপ edit + bill বসলে auto-lock — আগে থেকেই ছিল, বজায়।
- একই helper দিয়ে বাকি list (Doctor Visit / Appointment / Briefing)-এও একইভাবে বসবে।
