# 🎨🔴 B513 — TK-নির্দেশ (06.08.2026): "সম্পূর্ণ প্রজেক্টে যেখানে যেখানে
নোটিফিকেশন/বার্তা এখনো প্লেইন-টেক্সট আছে, সবকিছু প্রফেশনাল বানাতে হবে"
— কাজ শুরুর আগে সম্পূর্ণ তালিকা

## পদ্ধতি
সম্পূর্ণ `app/src/main/java/` জুড়ে দুই ধরনের বার্তা-পাঠানোর জায়গা
খোঁজা হয়েছে: (১) `BriefingRepository().post(...)` — ঘন্টা/Notifications
পাতায় যা দেখা যায়, (২) `WhatsAppMessageChooser.sendGeneric(...)` —
WhatsApp-এ যা পাঠানো হয়।

---

## ১) ঘন্টা/Notifications পাতার বার্তা (৭টা জায়গা, সবগুলোই এখনো প্লেইন টেক্সট)

| # | কোথা থেকে | শিরোনাম | এখনকার বার্তা (প্লেইন) | অবস্থা |
|---|---|---|---|---|
| ১ | `WorkNotebookActivity.kt` (IN TIME) | "Staff IN TIME" | "[staff] ([branch]) marked IN TIME at [time]" | 🔴 বাকি (আজকের আলোচনার মূল বিষয়) |
| ২ | `WorkNotebookActivity.kt` (OUT TIME, যদি থাকে — যাচাই বাকি) | — | — | ⚠️ যাচাই বাকি — afterOutTimeMarked-এ Master-নোটিশ নেই (B465-এ ইচ্ছাকৃত, শুধু WhatsApp) |
| ৩ | `DoctorCheckupActivity.kt` | "Patient Decision" | "[নাম] ([ID]) — Doctor checkup done. Patient decision: [...]. Please follow up." | 🔴 বাকি |
| ৪ | `PaymentRepository.kt` (delete) | "Payment deleted" | প্লেইন বার্তা (বিস্তারিত কোডে) | 🔴 বাকি |
| ৫ | `PaymentRepository.kt` (refund) | "Refund request" | "Patient ID [...] · Branch [...] · Reason: [...] · by [...]. Approve/Reject from the bell." | 🔴 বাকি |
| ৬ | `DeletePermission.kt` | "🗑️ Delete request — [নাম]" | লাইন-বাই-লাইন প্লেইন টেক্সট (Mobile/Patient ID/Branch/Requested by/Reason) | 🔴 বাকি |
| ৭ | `ChamberReopenPermission.kt` | "🔓 Chamber reopen request — [branch] [date]" | লাইন-বাই-লাইন প্লেইন টেক্সট | 🔴 বাকি |

## ২) WhatsApp-এ পাঠানো বার্তা (৩টা জায়গা)
- IN TIME শেয়ার (`WorkNotebookActivity.kt`) — "IN TIME- [সময়]\nStaff: [...]\nDate: [...]"
- OUT TIME শেয়ার (`WorkNotebookActivity.kt`) — একই ধাঁচ
- আরও ১টা (যাচাই করে ঠিক কোথায় বাকি আছে তালিকায় পরে যোগ হবে)

⚪ **WhatsApp বার্তা প্লেইন-টেক্সটই থাকা স্বাভাবিক** — WhatsApp নিজেই ফরম্যাটিং
(বোল্ড/ইমোজি ছাড়া রঙিন কার্ড) সমর্থন করে না, তাই এখানে "প্রফেশনাল" মানে
শুধু গোছানো লেখা/ইমোজি — রঙিন কার্ড-ডিজাইন অ্যাপের ভিতরের নোটিফিকেশনের
মতো এখানে সম্ভব না। TK চাইলে আলাদা করে বলবেন কতটা বদলাতে হবে।

## ৩) অ্যাপের ভিতরের পপ-আপ (ইতিমধ্যে আজ প্রফেশনাল করা হয়েছে)
- ✅ "Why are you leaving?" (B510)
- ✅ "Personal Work" — সরাসরি সেভ, লেখা নেই (B511)
- ✅ "কিছু ঘর ফাঁকা আছে" (B512)
- ✅ "Mark Today as Leave" (B512)

## ৪) এখনো যাচাই করা হয়নি এমন সম্ভাব্য জায়গা (পরের ধাপে)
- Notifications পাতায় এই বার্তাগুলো আসলে **কেমন কার্ডে** দেখা যায় (রঙিন
  হেডার/আইকন আছে কিনা) — এটা বার্তার লেখার বাইরেও, দেখানোর কনটেইনারও
  যাচাই করা দরকার।
- প্রজেক্টের অন্য সব মডিউলে (StaffProfile/IncomeExpense/ChamberAttendance
  ইত্যাদি) কোথাও আরও Briefing/notification-পাঠানোর জায়গা আছে কিনা —
  উপরের সাতটার বাইরে grep-এ আর পাওয়া যায়নি, তবু আরও গভীর যাচাই বাকি।

---

## 📋 আজ যতটা সম্ভব করা হচ্ছে (ক্রম)
1. **IN TIME নোটিশ (#১)** — আজকের আলোচনার মূল বিষয়, প্রথমে এটাই।
2. এরপর সময় অনুযায়ী ২-৭ নম্বর একে একে।

⛔ **এখনো কোনো কোড বদলানো হয়নি #২-৭-এর জন্য — শুধু তালিকা। #১ এখনই
শুরু হচ্ছে।**
