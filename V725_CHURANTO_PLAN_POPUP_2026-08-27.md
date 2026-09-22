# ✅ চূড়ান্ত প্ল্যান — পপ-আপের লেখা ইংরেজি করা (TK-নিশ্চিত)

**তারিখ:** ২৭.০৮.২০২৬ · **অবস্থা:** 🟡 কোড এখনো বদলানো হয়নি

TK প্রতিটা ব্যাচ ধরে সিদ্ধান্ত দিয়েছেন। নিচেই সব এক জায়গায়।


## 🟢 বাংলাই থাকবে — 12টি

| # | পর্দা | লেখা |
|---|---|---|
| ক1 | আয়-ব্যয় | ` — খরচের বিবরণ` |
| ক8 | রোগীর ডিটেলস | `আসার কথা বাতিল` |
| ক9 | Chamber Date | `আসার কথা বাতিল — ${row.name.ifBlank { digits }}` |
| খ8 | রোগীর ডিটেলস | `Mark ${currentPatientName.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?` |
| খ9 | Search | `Mark ${hit.name.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?` |
| খ10 | Follow-up | `Mark ${name.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?` |
| খ21 | Work Notebook | `এই ফোনে কি চেম্বার/ব্রাঞ্চের নম্বর আছে?` |
| খ22 | Work Notebook | `এই ফোনে ব্রাঞ্চের নম্বর কোন SIM?` |
| খ23 | Dialer | `এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" ` |
| খ24 | Work Notebook | `এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" ` |
| খ25 | Chamber Date | `এই রোগীর আসার কথা ইতিমধ্যে দেওয়া হয়েছে — ${FollowUpModel.displayDate(existing)}\n\nনতুন ` |
| খ29 | Work Notebook | `ছুটির আবেদন করুন` |

## 🔵 ইংরেজি হবে — 61টি

| # | পর্দা | লেখা | কোথায় |
|---|---|---|---|
| ক2 | Dr. Visit / RMP | `.format(pay - due)}\n\nএই বাড়তি টাকা শুধু Master অনুমোদন করতে পারেন।` | DoctorVisitActivity.kt:4513 |
| ক3 | আয়-ব্যয় | `Amount লিখুন` | IncomeExpenseActivity.kt:1336 |
| ক4 | Dr. Visit / RMP | `Delete request পাঠাবেন?` | DoctorVisitActivity.kt:943 |
| ক5 | প্রেসক্রিপশন | `Prescription যাচাই করা যায়নি` | PrescriptionActivity.kt:155 |
| ক6 | ডাক্তার চেক-আপ | `\" ছবিটা তালিকা থেকে সরাব?\n\n` | DoctorCheckupActivity.kt:2409 |
| ক7 | Chamber Date | `অন্য কারণ` | ChamberAttendanceActivity.kt:1678 |
| ক10 | প্রেসক্রিপশন | `ইন্টারনেট সংযোগ পরীক্ষা করে আবার Save করুন। কোনো Prescription সেভ হয়নি।` | PrescriptionActivity.kt:156 |
| ক11 | প্রেসক্রিপশন | `এই রোগীর আজ একটি Prescription সেভ হয়েছে। আপনি কি আবার Prescription করতে চান?` | PrescriptionActivity.kt:139 |
| ক12 | Report Card | `কোন পেমেন্ট Edit করবেন?` | ReportCardActivity.kt:452 |
| ক13 | ডাক্তার চেক-আপ | `ঘড়ির কাঁটা অনুযায়ী জায়গা` | DoctorCheckupActivity.kt:2800 |
| ক14 | ডাক্তার চেক-আপ | `ছবির সব দাগ মুছে যাবে। মুছব?` | DoctorCheckupActivity.kt:2272 |
| ক15 | রেজিস্ট্রেশন | `নতুন আলাদা রোগী?` | RegistrationActivity.kt:1003 |
| ক16 | আয়-ব্যয় | `ব্রাঞ্চ বাছুন` | IncomeExpenseActivity.kt:1428 |
| ক17 | আয়-ব্যয় | `মাস বাছুন` | IncomeExpenseActivity.kt:283 |
| ক18 | Chamber Date | `সব কাজ / পরবর্তী কাজ` | ChamberAttendanceActivity.kt:3885 |
| ক19 | Chamber Date | `⏰ আসার কথা দেওয়া আছে` | ChamberAttendanceActivity.kt:1429 |
| ক20 | রেজিস্ট্রেশন | `⚠️ যাচাই করা গেল না` | RegistrationActivity.kt:810 |
| ক21 | আয়-ব্যয় | `🗑️ এই খরচটি মুছবেন?` | IncomeExpenseActivity.kt:1116 |
| খ1 | Chamber Date | `$amtT\n\n⛔ এখনই কিছুই মুছবে না। Master-এর ঘন্টায় অনুরোধ যাবে; তিনি অনুমোদন দিলে তবেই ডিলি` | ChamberAttendanceActivity.kt:3479 |
| খ2 | Notice Board | `$patientCode-এর জন্য এখন আর কোনো Pending Refund নেই — হয়তো আগেই Approve/Reject হয়ে গেছে,` | BriefingActivity.kt:1925 |
| খ3 | Dr. Visit / RMP | `${item.name.ifBlank { item.mobile }}\nRequested by: $requesterName\n\nআসলেই ডিলিট হবে (Tra` | DoctorVisitActivity.kt:962 |
| খ4 | Dr. Visit / RMP | `${item.name.ifBlank { item.mobile }}\n\nAdmin অনুমোদন দিলে তবেই ডিলিট হবে — এখনই কিছু মুছব` | DoctorVisitActivity.kt:944 |
| খ5 | Chamber Date | `.format(due)}। আপনি ₹${` | ChamberAttendanceActivity.kt:1887 |
| খ6 | Chamber Date | `.format(enteredBill)} · এখনো বাকি ₹${` | ChamberAttendanceActivity.kt:1887 |
| খ7 | Chamber Date | `.format(value)} নিতে চাইছেন — এটা বিলের থেকে বেশি। তবুও এগোবেন?` | ChamberAttendanceActivity.kt:1887 |
| খ11 | Chamber Date | `Master-এর অনুমতি লাগবে` | ChamberAttendanceActivity.kt:3478 |
| খ12 | Work Notebook | `WhatsApp-এ পাঠানো হয়ে গেছে?` | WorkNotebookActivity.kt:474 |
| খ13 | Notice Board | `\n\n⚠️ অনুমোদন দিলে রেকর্ডটা Trash Bin-এ চলে যাবে (পরে ফেরানো যাবে)।` | BriefingActivity.kt:1839 |
| খ14 | Chamber Date | `আজ কেউ আসেননি (Arrived 0)। তবুও চেম্বার বন্ধ করবেন?` | ChamberAttendanceActivity.kt:2589 |
| খ15 | রোগীর ডিটেলস | `আজকের Check-up` | PatientTimelineActivity.kt:4119 |
| খ16 | রোগীর ডিটেলস | `আজকের চেকআপ আগেই সেভ করা আছে — দেখবেন নাকি এডিট করবেন?` | PatientTimelineActivity.kt:4120 |
| খ17 | Work Notebook | `আজকের তথ্য এখন আনা গেল না। OUT TIME নিরাপদে বসাতে আজকের তথ্যটা দরকার (নইলে আগের IN TIME মু` | WorkNotebookActivity.kt:609 |
| খ18 | Work Notebook | `আবার চেষ্টা করুন` | WorkNotebookActivity.kt:608 |
| খ19 | Chamber Date | `এই দিনের ($dateDisplay, $br) চেম্বার আবার খুলবেন?` | ChamberAttendanceActivity.kt:2507 |
| খ20 | Chamber Date | `এই দিনের ($dateDisplay, $br) চেম্বার আবার খোলার অনুরোধ Master-এর কাছে পাঠাবেন?` | ChamberAttendanceActivity.kt:2537 |
| খ26 | Work Notebook | `এড়িয়ে যান` | WorkNotebookActivity.kt:1498 |
| খ27 | Work Notebook | `কেন ব্যক্তিগত কাজে যাচ্ছেন?` | WorkNotebookActivity.kt:1431 |
| খ28 | ডাক্তার চেক-আপ | `ছবির নাম` | DoctorCheckupActivity.kt:1218 |
| খ30 | Chamber Date | `তারিখ বদলান` | ChamberAttendanceActivity.kt:1431 |
| খ31 | Work Notebook | `না` | WorkNotebookActivity.kt:334 |
| খ32 | Work Notebook | `না, পাঠানো হয়নি` | WorkNotebookActivity.kt:479 |
| খ33 | Work Notebook | `বন্ধ` | WorkNotebookActivity.kt:612 |
| খ34 | Work Notebook | `ভরে OUT TIME বসান` | WorkNotebookActivity.kt:1493 |
| খ35 | Work Notebook | `ভুল করে ছুটি দিয়েছিলেন? ছুটি বাতিল করে আজকের হাজিরা আবার চালু করবেন?` | WorkNotebookActivity.kt:2063 |
| খ36 | Chamber Date | `হিসাব এখনো আসেনি — এক মুহূর্ত` | ChamberAttendanceActivity.kt:1832 |
| খ37 | Work Notebook | `হ্যাঁ` | WorkNotebookActivity.kt:330 |
| খ38 | রোগীর ডিটেলস | `হ্যাঁ, Return করুন` | PatientTimelineActivity.kt:2257 |
| খ39 | Chamber Date | `হ্যাঁ, এগোন` | ChamberAttendanceActivity.kt:1888 |
| খ40 | Chamber Date | `হ্যাঁ, খুলুন` | ChamberAttendanceActivity.kt:2508 |
| খ41 | Chamber Date | `হ্যাঁ, পাঠান` | ChamberAttendanceActivity.kt:2538 |
| খ42 | Work Notebook | `হ্যাঁ, পাঠানো হয়েছে` | WorkNotebookActivity.kt:475 |
| খ43 | Chamber Date | `হ্যাঁ, বন্ধ করুন` | ChamberAttendanceActivity.kt:2590 |
| খ44 | Work Notebook | `হ্যাঁ, বাতিল করুন` | WorkNotebookActivity.kt:2064 |
| খ45 | রোগীর ডিটেলস | `হ্যাঁ, সরান` | PatientTimelineActivity.kt:2201 |
| খ46 | FollowCalendar | `⏰ Next Follow-up Call — বাধ্যতামূলক` | FollowCalendarActivity.kt:571 |
| খ47 | Chamber Date | `⚠️ Bill-এর থেকে বেশি হয়ে যাচ্ছে` | ChamberAttendanceActivity.kt:1886 |
| খ48 | রোগীর ডিটেলস | `⚠️ Return Fees — স্থায়ী` | PatientTimelineActivity.kt:2251 |
| খ49 | রোগীর ডিটেলস | `⚠️ এই ব্রাঞ্চ-বদল স্থায়ী` | PatientTimelineActivity.kt:2193 |
| খ50 | Work Notebook | `⚠️ কিছু ঘর ফাঁকা আছে` | WorkNotebookActivity.kt:1491 |
| খ51 | Work Notebook | `🏖️ ছুটির আবেদন` | WorkNotebookActivity.kt:891 |
| খ52 | Work Notebook | `🔄 আবার চেষ্টা` | WorkNotebookActivity.kt:611 |
