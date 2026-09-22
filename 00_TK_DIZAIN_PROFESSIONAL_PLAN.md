# 00_TK — পপ-আপ/নোটিফিকেশন প্রফেশনাল বানানোর প্ল্যান (চলমান)

**তারিখ শুরু:** 2026-08-06 · **নিয়ম:** শুধু চেহারা বদল, কোনো লজিক ছোঁয়া হবে না। প্রতিটা দল TK-এর প্রুফ-দেখে-অনুমোদনের পরেই কোড হবে। আন্দাজে কিছু নয়।

## পুরো প্রজেক্ট স্ক্যানের ফল
বেশিরভাগ পপ-আপ (~৮০টা) আগে থেকেই প্রফেশনাল (`PremiumAlert` / `premiumDialogShell` দিয়ে — রঙিন হেডার + রাউন্ডেড কার্ড + রঙিন বোতাম)। "সাদা মিঠা" যা বাকি, ৪ দলে:

### দল ১ — অপশন-লিস্ট পপ-আপ (৪৫টা)  [সবচেয়ে বড়]
রঙিন হেডার আছে, কিন্তু নিচের `.setItems()` অপশন-সারি সাদা-প্লেইন (OUT TIME-এর মতো)।
- সবচেয়ে বেশি: "Branch" পিকার (~১৫ স্ক্রিনে), Camera/Gallery পিকার (৪), অ্যাকশন-মেনু (১৪), Cancel-reason, Language, SIM/Dialer পিকার।
- Dialer/WorkNotebook-এর লিস্টগুলোতে হেডারও নেই (সবচেয়ে প্লেইন)।
- **সমাধান-ধারণা:** শেয়ার্ড লিস্ট-সারির স্টাইল একবার ঠিক করলে অনেকগুলো একসাথে ভালো হবে।
- **স্ট্যাটাস:** ✅ প্রুফ · ✅ TK-অনুমোদন (2026-08-06, **পথ ক** — নমুনা ২, হাইলাইট ছাড়া) · ✅ **কোড হয়ে গেছে**
  - শুধু শেয়ার্ড `native/PremiumAlert.kt`-এ ২টো বদল (কোনো স্ক্রিনে হাত দেওয়া হয়নি):
    (১) `header()` → গ্র্যাডিয়েন্ট সবুজ হেডার (জরুরি হলে লাল/হলুদ গ্র্যাডিয়েন্ট) — সব PremiumAlert পপ-আপে।
    (২) `paint()` → অপশন-লিস্টের সারির মাঝে হালকা দাগ + ফাঁক; লেখা theme থেকে গাঢ়।
  - **৪টে ছোট পপ-আপও করা হয়েছে (2026-08-06, TK-অনুমোদন):** Dialer SIM (`DialerActivity.kt:261`), Dialer কল-অপশন মেনু (`:437`, নতুন হেডার "Number Options"), WorkNotebook SIM (`:137`), WorkNotebook OUT/IN TIME পপ-আপ (`:276`) — প্রতিটায় রঙিন হেডার + paint যোগ, লজিক/অপশন অপরিবর্তিত। ✅
  - **চূড়ান্ত সিদ্ধান্ত:** TK **পথ ক** বেছেছেন (ঝুঁকি নেই)। "বাছা সারির সবুজ হাইলাইট" (পথ খ) **করা হবে না** — এটা বাকি কাজ নয়, TK-এর সিদ্ধান্তে বন্ধ। দল ১ **সম্পূর্ণ শেষ।**

### দল ২ — একদম প্লেইন পপ-আপ (১৩টা)
কোনো রঙ নেই। ফাইল অনুযায়ী:
- Dialer: `DialerActivity.kt:235` (chamber-number প্রশ্ন), `:633` (tag hide)
- WorkNotebook: `:107` (chamber-number), `:485` (leave confirm), `:616`/`:632` (কেন যাচ্ছেন — reason)
- Income-Expense: `:438`, `:474` (day summary), `:630` (duplicate confirm)
- ModuleUi: `:158` ("Could not open")
- Chamber: `ChamberAttendanceActivity.kt:2118` (Request Reopen)
- Briefing: `BriefingActivity.kt:140` (Unread Notice — দল ৪ও), `:199` (Not found)
- **স্ট্যাটাস:** ✅ TK-অনুমোদন (2026-08-06) · ✅ **কোড হয়ে গেছে** (১১টা পপ-আপে রঙিন হেডার + paint)
  - করা: Dialer (chamber-number, tag-hide), WorkNotebook (chamber-number, "Why leaving", personal-reason), IncomeExpense (২টা day-summary, duplicate-confirm), ModuleUi (Could not open), Chamber (Request Reopen), Briefing (Not found)।
  - বাদ: `WorkNotebookActivity.confirmCheckOut()` — অব্যবহৃত dead code, কখনো দেখানো হয় না।

### দল ৩ — ফোনের নোটিফিকেশন (৫টা)  [অল্প]
সব সাধারণ ডিফল্ট চেহারা (launcher আইকন + টাইটেল + লাইন)। রঙিন accent + বড়-লেখা (BigText) দিয়ে সুন্দর করা যায়।
- `CallReminderWorker` (কল রিমাইন্ডার — এটায় BigText আছে), `AttendanceReminderWorker` (IN/OUT), `BriefingReminderWorker` (নোটিশ), `ChamberCloseReminderWorker` (চেম্বার বন্ধ হয়নি), `BellNotifier` (নতুন নোটিশ ঘণ্টা)।
- **স্ট্যাটাস:** ✅ প্রুফ · ✅ TK-অনুমোদন (2026-08-06) · ✅ **কোড হয়ে গেছে**
  - `ic_notif_bell.xml` (নতুন আইকন) + ৫টা ফাইলে setSmallIcon→bell, setColor #0B3B73, BigText: CallReminderWorker, AttendanceReminderWorker, BriefingReminderWorker, ChamberCloseReminderWorker, BellNotifier।

### দল ৪ — "Unread Notice" পপ-আপ (১টা)  [সবচেয়ে ছোট]
`BriefingActivity.kt:139` — অর্ধেক সাজানো (রাউন্ডেড কার্ড আছে), শুধু রঙিন হেডার নেই। এক লাইনের ফিক্স (`setCustomTitle(PremiumAlert.header(...))`)।
- **স্ট্যাটাস:** ✅ প্রুফ · ✅ TK-অনুমোদন (2026-08-06) · ✅ **কোড হয়ে গেছে** (`BriefingActivity.kt` setCustomTitle)

## সাজানো যায় না
ছোট **Toast বার্তা (৩৮১টা)** — Android থিম করতে দেয় না। বাদ। (দরকারে ভবিষ্যতে ইন-অ্যাপ ব্যানার দিয়ে বদলানো যায়।)

## কাজের ক্রম (TK-নির্দেশ: অল্পগুলো আগে)
১) দল ৪ (১টা) → ২) দল ৩ (৫টা) → ৩) দল ২ (১৩টা) → ৪) দল ১ (৪৫টা)।

## রেফারেন্স (কোথা থেকে নকল করব)
`native/PremiumAlert.kt` (হেডার+কার্ড+বোতাম রঙ), `premiumDialogShell` (DoctorVisitActivity/ChamberAttendanceActivity), `res/layout/dialog_advance.xml` ইত্যাদি।

---
**⚠️ পরের সেশনের জন্য:** এই ফাইল পড়লেই বোঝা যাবে ডিজাইন-কাজের কোনটা শেষ, কোনটা বাকি। প্রতিটা দল শেষ হলে উপরের স্ট্যাটাস ✅ করা হবে।
