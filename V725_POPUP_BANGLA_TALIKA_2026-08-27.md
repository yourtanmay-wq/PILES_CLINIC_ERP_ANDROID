# 📋 পপ-আপে বাংলা লেখার সম্পূর্ণ তালিকা (V725 — শুধু যাচাই, কোড বদলায়নি)

**তারিখ:** ২৭.০৮.২০২৬ · **TK-নির্দেশ:** *"কোনটা কোনটা বাংলা ও হিন্দিতে আছে যাচাই করে দশটা করে পাঠান"*

## সারসংক্ষেপ

| | সংখ্যা |
|---|---|
| ফোনের পপ-আপে আলাদা বাংলা লেখা | **73** |
| — ইংরেজি অনুবাদ **আছে** (বাংলা-বন্ধ স্টাফ ইংরেজি দেখেন) | 52 |
| — ইংরেজি অনুবাদ **নেই** | 21 |
| **হিন্দি লেখা** | **০** (একটাও নেই) |

> ⚠️ প্রথমে ভুল করে কয়েকটাকে "হিন্দি" বলেছিলাম — আসলে ওগুলো বাংলা;
> দাঁড়ি চিহ্ন `।` দুই ভাষাতেই এক, তাই যন্ত্র ভুল ধরেছিল। সংশোধন করা হলো।

---

## ক-ভাগ · ইংরেজি অনুবাদ **নেই** (২১টি) — সবচেয়ে জরুরি

1. **আয়-ব্যয়** — ` — খরচের বিবরণ`  
   ↳ IncomeExpenseActivity.kt:947
2. **Dr. Visit / RMP** — `.format(pay - due)}\n\nএই বাড়তি টাকা শুধু Master অনুমোদন করতে পারেন।`  
   ↳ DoctorVisitActivity.kt:4513
3. **আয়-ব্যয়** — `Amount লিখুন`  
   ↳ IncomeExpenseActivity.kt:1336
4. **Dr. Visit / RMP** — `Delete request পাঠাবেন?`  
   ↳ DoctorVisitActivity.kt:943
5. **প্রেসক্রিপশন** — `Prescription যাচাই করা যায়নি`  
   ↳ PrescriptionActivity.kt:155
6. **ডাক্তার চেক-আপ** — `\" ছবিটা তালিকা থেকে সরাব?\n\n`  
   ↳ DoctorCheckupActivity.kt:2409
7. **Chamber Date** — `অন্য কারণ`  
   ↳ ChamberAttendanceActivity.kt:1678
8. **রোগীর ডিটেলস** — `আসার কথা বাতিল`  
   ↳ PatientTimelineActivity.kt:1406
9. **Chamber Date** — `আসার কথা বাতিল — ${row.name.ifBlank { digits }}`  
   ↳ ChamberAttendanceActivity.kt:1646
10. **প্রেসক্রিপশন** — `ইন্টারনেট সংযোগ পরীক্ষা করে আবার Save করুন। কোনো Prescription সেভ হয়নি।`  
   ↳ PrescriptionActivity.kt:156
11. **প্রেসক্রিপশন** — `এই রোগীর আজ একটি Prescription সেভ হয়েছে। আপনি কি আবার Prescription করতে চান?`  
   ↳ PrescriptionActivity.kt:139
12. **Report Card** — `কোন পেমেন্ট Edit করবেন?`  
   ↳ ReportCardActivity.kt:452
13. **ডাক্তার চেক-আপ** — `ঘড়ির কাঁটা অনুযায়ী জায়গা`  
   ↳ DoctorCheckupActivity.kt:2800
14. **ডাক্তার চেক-আপ** — `ছবির সব দাগ মুছে যাবে। মুছব?`  
   ↳ DoctorCheckupActivity.kt:2272
15. **রেজিস্ট্রেশন** — `নতুন আলাদা রোগী?`  
   ↳ RegistrationActivity.kt:1003
16. **আয়-ব্যয়** — `ব্রাঞ্চ বাছুন`  
   ↳ IncomeExpenseActivity.kt:1428
17. **আয়-ব্যয়** — `মাস বাছুন`  
   ↳ IncomeExpenseActivity.kt:283
18. **Chamber Date** — `সব কাজ / পরবর্তী কাজ`  
   ↳ ChamberAttendanceActivity.kt:3885
19. **Chamber Date** — `⏰ আসার কথা দেওয়া আছে`  
   ↳ ChamberAttendanceActivity.kt:1429
20. **রেজিস্ট্রেশন** — `⚠️ যাচাই করা গেল না`  
   ↳ RegistrationActivity.kt:810
21. **আয়-ব্যয়** — `🗑️ এই খরচটি মুছবেন?`  
   ↳ IncomeExpenseActivity.kt:1116

---

## খ-ভাগ · ইংরেজি অনুবাদ **আছে** (৫২টি)

এগুলো বাংলা-বন্ধ স্টাফের পর্দায় এমনিতেই ইংরেজি দেখায় (`NoBengali`)।

1. **Chamber Date** — `$amtT\n\n⛔ এখনই কিছুই মুছবে না। Master-এর ঘন্টায় অনুরোধ যাবে; তিনি অনুমোদন দিলে তবেই ডিলিট হবে।`
2. **BriefingActivity.kt** — `$patientCode-এর জন্য এখন আর কোনো Pending Refund নেই — হয়তো আগেই Approve/Reject হয়ে গেছে, বা অন্য ফোন থেকে এখনো ক্লাউডে পৌঁছায়নি।`
3. **Dr. Visit / RMP** — `${item.name.ifBlank { item.mobile }}\nRequested by: $requesterName\n\nআসলেই ডিলিট হবে (Trash Bin-এ যাবে)।`
4. **Dr. Visit / RMP** — `${item.name.ifBlank { item.mobile }}\n\nAdmin অনুমোদন দিলে তবেই ডিলিট হবে — এখনই কিছু মুছবে না।`
5. **Chamber Date** — `.format(due)}। আপনি ₹${`
6. **Chamber Date** — `.format(enteredBill)} · এখনো বাকি ₹${`
7. **Chamber Date** — `.format(value)} নিতে চাইছেন — এটা বিলের থেকে বেশি। তবুও এগোবেন?`
8. **রোগীর ডিটেলস** — `Mark ${currentPatientName.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?`
9. **GlobalSearchActivity.kt** — `Mark ${hit.name.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?`
10. **FollowUpActivity.kt** — `Mark ${name.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?`
11. **Chamber Date** — `Master-এর অনুমতি লাগবে`
12. **WorkNotebookActivity.kt** — `WhatsApp-এ পাঠানো হয়ে গেছে?`
13. **BriefingActivity.kt** — `\n\n⚠️ অনুমোদন দিলে রেকর্ডটা Trash Bin-এ চলে যাবে (পরে ফেরানো যাবে)।`
14. **Chamber Date** — `আজ কেউ আসেননি (Arrived 0)। তবুও চেম্বার বন্ধ করবেন?`
15. **রোগীর ডিটেলস** — `আজকের Check-up`
16. **রোগীর ডিটেলস** — `আজকের চেকআপ আগেই সেভ করা আছে — দেখবেন নাকি এডিট করবেন?`
17. **WorkNotebookActivity.kt** — `আজকের তথ্য এখন আনা গেল না। OUT TIME নিরাপদে বসাতে আজকের তথ্যটা দরকার (নইলে আগের IN TIME মুছে যেতে পারত)। একবার আবার চেষ্টা করুন।`
18. **WorkNotebookActivity.kt** — `আবার চেষ্টা করুন`
19. **Chamber Date** — `এই দিনের ($dateDisplay, $br) চেম্বার আবার খুলবেন?`
20. **Chamber Date** — `এই দিনের ($dateDisplay, $br) চেম্বার আবার খোলার অনুরোধ Master-এর কাছে পাঠাবেন?`
21. **WorkNotebookActivity.kt** — `এই ফোনে কি চেম্বার/ব্রাঞ্চের নম্বর আছে?`
22. **WorkNotebookActivity.kt** — `এই ফোনে ব্রাঞ্চের নম্বর কোন SIM?`
23. **DialerActivity.kt** — `এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন — তাহলে Dialer-এ কোনো কল দেখানো হবে না।`
24. **WorkNotebookActivity.kt** — `এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন।`
25. **Chamber Date** — `এই রোগীর আসার কথা ইতিমধ্যে দেওয়া হয়েছে — ${FollowUpModel.displayDate(existing)}\n\nনতুন তারিখ ${FollowUpModel.displayDate(chosenDate)} বসাতে চান?`
26. **WorkNotebookActivity.kt** — `এড়িয়ে যান`
27. **WorkNotebookActivity.kt** — `কেন ব্যক্তিগত কাজে যাচ্ছেন?`
28. **ডাক্তার চেক-আপ** — `ছবির নাম`
29. **WorkNotebookActivity.kt** — `ছুটির আবেদন করুন`
30. **Chamber Date** — `তারিখ বদলান`
31. **WorkNotebookActivity.kt** — `না`
32. **WorkNotebookActivity.kt** — `না, পাঠানো হয়নি`
33. **WorkNotebookActivity.kt** — `বন্ধ`
34. **WorkNotebookActivity.kt** — `ভরে OUT TIME বসান`
35. **WorkNotebookActivity.kt** — `ভুল করে ছুটি দিয়েছিলেন? ছুটি বাতিল করে আজকের হাজিরা আবার চালু করবেন?`
36. **Chamber Date** — `হিসাব এখনো আসেনি — এক মুহূর্ত`
37. **WorkNotebookActivity.kt** — `হ্যাঁ`
38. **রোগীর ডিটেলস** — `হ্যাঁ, Return করুন`
39. **Chamber Date** — `হ্যাঁ, এগোন`
40. **Chamber Date** — `হ্যাঁ, খুলুন`
41. **Chamber Date** — `হ্যাঁ, পাঠান`
42. **WorkNotebookActivity.kt** — `হ্যাঁ, পাঠানো হয়েছে`
43. **Chamber Date** — `হ্যাঁ, বন্ধ করুন`
44. **WorkNotebookActivity.kt** — `হ্যাঁ, বাতিল করুন`
45. **রোগীর ডিটেলস** — `হ্যাঁ, সরান`
46. **FollowCalendarActivity.kt** — `⏰ Next Follow-up Call — বাধ্যতামূলক`
47. **Chamber Date** — `⚠️ Bill-এর থেকে বেশি হয়ে যাচ্ছে`
48. **রোগীর ডিটেলস** — `⚠️ Return Fees — স্থায়ী`
49. **রোগীর ডিটেলস** — `⚠️ এই ব্রাঞ্চ-বদল স্থায়ী`
50. **WorkNotebookActivity.kt** — `⚠️ কিছু ঘর ফাঁকা আছে`
51. **WorkNotebookActivity.kt** — `🏖️ ছুটির আবেদন`
52. **WorkNotebookActivity.kt** — `🔄 আবার চেষ্টা`
