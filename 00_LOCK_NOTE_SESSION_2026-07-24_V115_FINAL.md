# 🔒 LOCK NOTE — Session 2026-07-24, V115 FINAL

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।

**⚠️ পূর্ণাঙ্গ, সহজ-ভাষায় সারসংক্ষেপের জন্য দেখুন:** `00_SESSION_SUMMARY_2026-07-24_FINAL.md` (রুটে) — সব ফিচার + সব বাগ (কারণ+ফিক্স+ভবিষ্যতে-যা-মনে-রাখতে-হবে) সহজ ভাষায় লেখা আছে।

**বিস্তারিত টেকনিক্যাল নোট:** `00_PROJECT_STATE_MASTER_NOTE.md` সেকশন ৯৮–১১৫।

**Version:** V113 → V114 → **V115**

## এই ZIP-এ যা আছে (সংক্ষেপে)
- Patient Timeline (Enquiry/Visit/Patient) — হেডার রিডিজাইন, বাটন-রো পরিবর্তন, Registration Cancel, Complete despite Due, Call আইকন
- Registration — Official/Unexpected Timing
- Payment — Backdate Payment (Master-approval)
- Follow-up — Registration Date এডিট, Name বাধ্যতামূলক না (Enquiry), **একাধিক গুরুতর বাগ ফিক্স** (fallback-id ভুল টেবিলের হওয়ায় এডিট/আপডেট নীরবে ব্যর্থ হওয়া — ১১ জায়গায় ফিক্স করা হয়েছে)
- Dashboard/Briefing — নতুন Master-only নোটিফিকেশন সেকশন (Backdate + Visit Fee Missing)

## ⚠️ সততার সাথে জানানো (TK-কে সরাসরি বলা হয়েছে)
- এখানে আসল Android compiler/emulator নেই — প্রতিটা ফাইল কোড-স্তরে (brace/paren balance, XML well-formed, id ক্রস-চেক) গভীরভাবে যাচাই করা হয়েছে, কিন্তু **আসল build ও লাইভ টেস্ট TK নিজেই Android Studio-তে করবেন** — সেটাই একমাত্র চূড়ান্ত প্রমাণ।
- একটা পুরনো ডেটা-ভুল (Supriya Roy-এর ব্রাঞ্চ, ভুল ID-বাগের শিকার হয়ে আগে ঠিক হয়নি) এখনো ম্যানুয়ালি সংশোধন বাকি — কোড এখন থেকে সঠিকভাবে কাজ করবে, কিন্তু এই একটা পুরনো রেকর্ড আগে থেকেই ভুল অবস্থায় আছে (TK চাইলে SQL দিয়ে এখনই ঠিক করে দেওয়া যাবে)।

## পরিবর্তিত ফাইল (সম্পূর্ণ তালিকা)
`activity_patient_timeline.xml`, `PatientTimelineActivity.kt`, `PatientTimelineRepository.kt`, `DraftRepository.kt`, `FollowUpRepository.kt`, `RegistrationActivity.kt`, `PatientModel.kt`, `activity_registration.xml`, `PaymentModel.kt`, `PaymentRepository.kt`, `PaymentActivity.kt`, `DashboardActivity.kt`, `BriefingActivity.kt`, `activity_briefing.xml`, `FollowUpActivity.kt`, `FollowCalendarActivity.kt`, `build.gradle.kts`

## SQL migration (TK ইতিমধ্যে Run করে দিয়েছেন, দুটোই)
- `04_SUPABASE_DATABASE_SETUP/PATCH_2026-07-24_patients_completeApproval.sql`
- `04_SUPABASE_DATABASE_SETUP/PATCH_2026-07-24_payment_backdate_requests.sql`

**TK-এর স্থায়ী নিয়ম:** এই ফাইলে যা ফাইনাল হয়েছে তার ডিজাইন/লজিক TK-এর নতুন স্পষ্ট অনুমতি ছাড়া কখনো বদলানো যাবে না।
