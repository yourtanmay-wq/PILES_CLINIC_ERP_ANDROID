# 🔒 LOCK NOTE — V211 (৩১.০৭.২০২৬) — RMP/Payment/Chamber সেশনের সব কাজ একসাথে

**ভার্সন:** `versionCode 211` · `versionName 2.11` · পর্দায় **V211** · খাতার সারি **B212** পর্যন্ত
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**
> এই ফাইল/ZIP-এর কোনো অংশ ভবিষ্যতে কোনো AI/ডেভেলপার TK-এর অনুমতি ছাড়া বদলাতে পারবে না।

---

## 📋 এই ভার্সনে যা যা এসেছে (V210 → V211, একবারে)

| সারি | কী হলো | ফাইল |
|---|---|---|
| B204 | RMP → View All-এর "লোড হচ্ছে…" টোস্ট আসল ডেটার পরেও লেগে থাকা — ঠিক | DoctorVisitActivity.kt |
| B205 | RMP-এ "🩹 Fix Last Note" — ভুল রিমার্ক সংশোধন, নতুন কল হিসেবে গোনা হয় না | DoctorVisitActivity.kt, DoctorVisitRepository.kt |
| B206 | Edit Remark পপ-আপ প্রফেশনাল + Doctor Call Remarks থেকে Remarks/Referral বাদ, শুধু তারিখ | DoctorVisitActivity.kt |
| B207 | Next Call Date আজ হলে ঘন্টায় সাউন্ড-নোটিফিকেশন | DoctorVisitRepository.kt, BellCounter.kt |
| B208 | RMP-এর ৩ বাটন এক লাইনে ফিক্সড 10sp, Note কলাম ফন্ট 11.5sp | DoctorVisitActivity.kt |
| B209 | Payment Collection-এর ৪ পপ-আপে Kishanganj বাংলা — ঠিক; Audit-ট্রেইলের "থেকে"/"করেছেন" → "→"/"by" | PaymentActivity.kt, ChamberAttendanceActivity.kt, FollowUpActivity.kt, PatientTimelineActivity.kt |
| B210 | সম্পূর্ণ প্রজেক্ট স্ক্যান — ৪০টা বাংলা UI-টেক্সট (১১ ফাইলে) NoBengali.s() দিয়ে ঢাকা | Briefing/Chamber Attendance/Chamber Close/Doctor Visit/Enquiry/Follow Calendar/Follow-up/Global Search/Patient Timeline/Report Card |
| B211 | নতুন Master-only "RMP Performance Report" — This Month/All-Time/Ref. Paid, সাম্প্রতিক রেফারেল অনুযায়ী সাজানো | DoctorVisitActivity.kt, DoctorVisitRepository.kt, activity_doctorvisit.xml, নতুন bg_rmp_performance_btn.xml |
| B212 | ৯টা Quick Chip — KNE-KISHAN5-এর জন্য ইংরেজি+হিন্দি, বাকি সবার বাংলা অক্ষত (B137) | ChamberAttendanceActivity.kt, ReportCardActivity.kt |

বিস্তারিত পুরো ব্যাখ্যা প্রতিটা সারির জন্য `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এ B204 থেকে B212 পর্যন্ত লেখা আছে।

---

## 🔴 বাকি সিদ্ধান্ত (TK-এর কাছে)
কিছুই নেই — এই সেশনে TK-এর সব প্রশ্নের উত্তর/সিদ্ধান্ত হয়ে গেছে।

## 🔴 TK-কে করতে হবে
শুধু **লাইভ টেস্ট** — Android Studio-তে বিল্ড করে দেখা, বিশেষত:
- RMP পর্দার নতুন "🏆 RMP Performance Report" বাটন (শুধু Master লগইনে দেখা উচিত)
- Payment Collection / Chamber Attendance / Report Card-এর পপ-আপ (KNE-KISHAN5 দিয়ে লগইন করে)
- RMP-এর Fix Last Note, Edit Remark, Doctor Call Remarks পপ-আপ

## ⛔ কোনো SQL লাগবে না।

---

## 🔍 ফাইল দেওয়ার আগে চূড়ান্ত যাচাই (৩১.০৭.২০২৬, TK-এর স্পষ্ট নির্দেশে — "পুনরায় যাচাই করে নিন")

- ১৩টা পরিবর্তিত Kotlin ফাইলের প্রতিটাতেই ব্র্যাকেট (`{`/`}`) ও প্যারেন (`(`/`)`) গোনা — **সবগুলো সমান, ব্যতিক্রম নেই**
- প্রজেক্টের **২৩১টা XML ফাইলই** পুনরায় `well-formed` যাচাই — **সব পাশ**
- XML কমেন্টে কোথাও `--` নেই (পুনরায় স্ক্যান করে নিশ্চিত)
- `kotlinx.coroutines.async(...)`/`launch(...)` fully-qualified প্যাটার্ন (V209-এর RED ALERT বাগ) — পুরো প্রজেক্টে আবার খুঁজে **কোথাও নেই**
- Bengali-স্ক্যান আবার চালিয়ে নিশ্চিত হওয়া হয়েছে — ঝুঁকিপূর্ণ বাংলা UI-টেক্সট **০** (শুধু ২টা ইচ্ছাকৃত ব্যতিক্রম: PublicSiteActivity ও TK-অনুমোদিত Quick Chip ব্যবস্থা)
- নতুন view id (`btnRmpPerformance`) XML ও Kotlin দুই জায়গাতেই মিলিয়ে দেখা হয়েছে (ঠিক ১টা করে, ডুপ্লিকেট নেই)
- নতুন drawable resource (`bg_rmp_performance_btn.xml`) — লেখার সময় namespace-এ একটা টাইপো (`api/res/android` বদলে `apk/res/android` হওয়া উচিত ছিল) ধরা পড়েছিল, **সাথে সাথেই ঠিক করা হয়েছে**, ফাইল দেওয়ার প্রশ্নই ওঠেনি
- `DoctorVisitRepository.RmpPerformanceRow`/`RmpReferredPatient` — নতুন ডেটা ক্লাস, ব্যবহারের জায়গার সাথে টাইপ মিলিয়ে দেখা হয়েছে
