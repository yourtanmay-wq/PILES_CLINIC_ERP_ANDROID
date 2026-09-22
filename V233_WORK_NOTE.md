# V233 — Work Note (কোড পরিবর্তনের আগে লেখা)

**Base:** V232 (সর্বশেষ working project — এটাই একমাত্র base)।
**তারিখ ও সময় (শুরু):** 01.08.2026, 12:33 PM IST।
**Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই FINAL নয়।

---

## ১. সমস্যা কী (VERIFIED)
বর্তমান তারিখ 1 August হলেও একটি Calendar emoji/icon-এর ভিতরে **fixed "July 17"** দেখা যাচ্ছে। কারণ — Android-এর কিছু emoji-ফন্টে **📅 (U+1F4C5) ইমোজির নিজের আঁকা ছবিতেই** একটা স্থির "JUL 17" বসানো থাকে; ওটা অ্যাপের আসল তারিখ নয়, ইমোজির artwork। তাই যেখানে **একলা 📅 ইমোজি** আজকের/তারিখ বোঝাতে বসানো, সেখানে সবসময় "Jul 17"-এর মতো দেখায়।

## ২. সম্পূর্ণ Project-এ কোথায় কোথায় fixed "July 17" calendar আছে — AUDIT
পুরো Android + Web + Website (drawable/XML/Kotlin/JS/HTML/CSS) Unicode-স্ক্যান করা হলো:

- ✅ **আগে থেকেই ঠিক (live badge):** Follow-up পর্দা (`activity_followup.xml` + `FollowUpActivity.kt`) ও Chamber Attendance পর্দা (`activity_chamber_attendance.xml` + `ChamberAttendanceActivity.kt`) — এখানে আগেই raw 📅 বাদ দিয়ে **২-লাইনের live badge** (`tvCalMonth`/`tvCalDay`, drawable `bg_cal_badge_top`/`bottom`) বসানো আছে, যা আজকের/selected তারিখ দেখায়। **এগুলো ছোঁয়া হবে না।**
- ❌ **এখনো raw 📅 (এই বাগ):** **Doctor Visit পর্দা** — `activity_doctorvisit.xml:115`, "EXPECTED" stat-কার্ডের আইকন এখনো **একলা `📅` (18sp) ইমোজি** → ফোনে fixed "Jul 17" আঁকা দেখায়। **এটাই সম্পূর্ণ project-এ একমাত্র বাকি জায়গা।**
- ➖ **শুধু সাজানো (label prefix, তারিখ নয় — অপরিবর্তিত থাকবে):** "📅 Next Visit Date: <live date>", "📅 Visit Reminder", "📅 Custom Date", "📅 Calendar" বোতাম, web-এর `📅` আইকন যেগুলোর **পাশেই live তারিখ** আছে — এগুলো কোনো fixed তারিখ *দেখায় না*, তাই TK-এর নির্দেশ মতো **ইমোজি সরানো হবে না, বদলানোও হবে না।**
- ➖ `ic_pd_calendar.xml` (vector, ফাঁকা ক্যালেন্ডার, কোনো তারিখ বসানো নেই) — Public Site পর্দায় শুধু আইকন; ঠিক আছে।

## ৩. কোন File পরিবর্তন হবে (২টি)
1. `…/res/layout/activity_doctorvisit.xml` — "EXPECTED" কার্ডের একলা `📅` TextView-এর বদলে **২-লাইনের live calendar badge** (`tvDvCalMonth` + `tvDvCalDay`, একই `bg_cal_badge_top`/`bottom` drawable, একই জায়গা ও মাপ — ঠিক Follow-up/Chamber-এর মতো)।
2. `…/native/DoctorVisitActivity.kt` — `onCreate`-এ ওই badge-এ আজকের (device-local = IST) **MMM ও দিন** বসানো (Follow-up পর্দার হুবহু একই কোড)।

## ৪. কোন PASS/LOCKED কাজ পরিবর্তন করা যাবে না
- Follow-up ও Chamber-এর existing live badge — অপরিবর্তিত।
- "EXPECTED" কার্ডের **count (`tvTodayCount`), filter (`currentFilter="expected"`), click** — কিছুই বদলাবে না; শুধু আইকনটা emoji→live badge।
- অন্য সব 📅 ইমোজি (label prefix) — সরানো/বদলানো হবে না।
- UI design/layout/color/spacing/buttons; date calculation/filter/payment/collection/enquiry/follow-up/database/sync logic; Registration/Trash-Restore/permission — কিছুই নয়।
- আগে সম্পন্ন V231 (delete/stale) ও V232 (First Visit বার্তা) — অপরিবর্তিত।

**পরিকল্পনা:** শুধু এই একটিমাত্র বাকি fixed-July-17 calendar ঠিক হবে (emoji → live IST month/day badge)। কোনো broad refactor/cleanup/optimization/redesign নয়।

---

**তারিখ ও সময় (শেষ):** 01.08.2026, 12:40 PM IST।

## ৫. কোথায় কোথায় fixed July 17 পাওয়া গেছে
সম্পূর্ণ project scan-এ **একমাত্র** জায়গা: **Doctor Visit পর্দা → "EXPECTED" stat-কার্ড**-এর একলা `📅` ইমোজি (`activity_doctorvisit.xml`)। বাকি সব ক্যালেন্ডার হয় আগেই live badge (Follow-up, Chamber), নয়তো live তারিখের পাশে শুধু-সাজানো ইমোজি (§২ দ্রষ্টব্য)। Web App ও Website-এ কোনো fixed-তারিখ ক্যালেন্ডার নেই — সব 📅 live তারিখের পাশে।

## ৬. পরিবর্তিত File-এর তালিকা (২টি)
- `…/res/layout/activity_doctorvisit.xml` — "EXPECTED" কার্ডের `📅` TextView → ২-লাইনের live badge (`tvDvCalMonth` + `tvDvCalDay`, drawable `bg_cal_badge_top`/`bottom`)।
- `…/native/DoctorVisitActivity.kt` — `onCreate`-এ ওই badge-এ আজকের (device-local = IST) MMM ও দিন সেট (Follow-up পর্দার হুবহু একই কোড)।

## ৭. প্রতিটি Screen-এ live/selected date পরীক্ষা (owner Android Studio-তে যাচাই করবেন)
- **Doctor Visit → EXPECTED কার্ড:** এখন আজকের তারিখ (যেমন 1 August → **"Aug / 1"**) দেখাবে; আগের fixed "Jul 17" আর নয়। কাল খুললে "Aug 2" — নিজে থেকে বদলাবে।
- **Follow-up পর্দা:** আগের মতোই live badge (আজকের তারিখ) — অপরিবর্তিত।
- **Chamber Attendance পর্দা:** selected তারিখ থাকলে সেটিই badge-এ; আজ হলে আজকের — অপরিবর্তিত।
- ⚠️ এই cloud-এ SDK নেই তাই device-run হয়নি; static-ভাবে যাচাই + একটি স্বতন্ত্র review-তে **BUILD-SAFE**।

## ৮. অন্য design/feature/logic বদলায়নি — Declaration
শুধু ওই একটি ইমোজি→live-badge swap হয়েছে। **এক অক্ষরও বদলায়নি:** EXPECTED কার্ডের count/filter/click; অন্য সব 📅 ইমোজি (label prefix); Follow-up/Chamber-এর existing badge; UI design/layout/color/spacing/buttons; date calculation/filter/payment/collection/enquiry/follow-up/database/sync; Registration/Trash-Restore/permission; আগের V231/V232 কাজ। কোনো broad refactor/cleanup/optimization/redesign নয়। "Build/Test Pass" দাবি করা হচ্ছে না — owner Android Studio-তে build ও যাচাই করবেন।
