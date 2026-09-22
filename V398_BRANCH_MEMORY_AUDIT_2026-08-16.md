# V398 — মাস্টারের ব্রাঞ্চ নির্বাচন: সম্পূর্ণ সত্যতা যাচাই রিপোর্ট
**তারিখ:** 16.08.2026 · **সময়:** 12:10 PM IST · **অবস্থা:** শুধুই যাচাই — এখনো কোনো কোড বদলানো হয়নি (TK-এর অনুমতির অপেক্ষায়)

TK-এর প্রশ্ন: *"মাস্টার এর ক্ষেত্রে যদি সমস্ত সময় All Branch সিলেক্টেড থাকে তাহলে ডাটা বেশি খরচ হচ্ছে কিনা?"*
এবং দাবি: *"লাস্ট যে ব্রাঞ্চ সিলেক্ট করা থাকবে, প্রতিবার প্রতিটা সেকশনের সেই ব্রাঞ্চই থেকে যাবে।"*

---

## ১. সরাসরি উত্তর

**হ্যাঁ — কিছু জায়গায় সত্যিই বেশি ডাটা খরচ হচ্ছে। কিন্তু সব জায়গায় নয়।** নিচে ঠিক কোথায় হচ্ছে আর কোথায় হচ্ছে না, ফাইল ও লাইন নম্বর সহ।

**আর হ্যাঁ — আপনার অভিযোগ সঠিক। কাজটা ঠিক করা হয়নি।**
সম্পূর্ণ প্রজেক্টে মাত্র **২টি** জায়গায় ব্রাঞ্চ মনে রাখা হয়:
- Android: `DoctorQueueActivity.kt` (B670, 15.08.2026) — SharedPreferences `"doctor_queue_pick"`
- Web: RMP/Doctor Visit — `sessionStorage['doctorVisitMasterBranch']` (ট্যাব বন্ধ করলেই মুছে যায়)

**বাকি প্রতিটি স্ক্রিনে প্রতিবার খোলার সময় জোর করে "All" বসে যায়।** কোনো global/shared "current branch" নেই — প্রতিটি স্ক্রিনের নিজের আলাদা ভেরিয়েবল।

---

## ২. Android — কোন স্ক্রিনে ব্রাঞ্চ পিকার আছে, খুললে কী থাকে, মনে রাখে কি না

ভিত্তি: `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/`

| # | স্ক্রিন | পিকার | খুললে ডিফল্ট | মনে রাখে? |
|---|---|---|---|---|
| 1 | `native/DoctorQueueActivity.kt` | L279–296 | `rememberedBranch()` L278 | ✅ **হ্যাঁ** — `doctor_queue_pick` |
| 2 | `native/FollowUpActivity.kt` | L258–259, L619–632 | `countBranch = "All"` L257 | ❌ না |
| 3 | `native/FollowCalendarActivity.kt` | L114–135 | `"All"` L119 | ❌ না |
| 4 | `native/DraftActivity.kt` | L107–128 | `"All"` L112 | ❌ না |
| 5 | `native/DraftListActivity.kt` | L84–104 | Intent extra, নইলে `"All"` L87 | ❌ না |
| 6 | `native/ChamberAttendanceActivity.kt` | L422–455 | `"All"` L269 | ❌ না |
| 7 | `native/ChamberCloseActivity.kt` | L78–93 | `"All"` L39/L56 | ❌ না |
| 8 | `native/CollectionListActivity.kt` | L122–138 | `"All"` L43 | ❌ না |
| 9 | `native/TrashBinActivity.kt` | L75–97 | `"All"` L28 | ❌ না |
| 10 | `native/PaymentActivity.kt` | L165–187 | `"All Branch"` L172 | ❌ না |
| 11 | `native/DoctorVisitActivity.kt` (RMP) | L180–207 | `"Select Branch"` (BRANCH_NONE) L170 — **"All" অপশনই নেই** | ❌ না |
| 12 | `modules/IncomeExpenseActivity.kt` — আজকের হিসাব | L739–758 | `"All Branches"` L37 | ❌ না |
| 13 | `modules/IncomeExpenseActivity.kt` — টাকার খাতা | L198–219 | `"All Branches"` L168 | ❌ না |
| 14 | `modules/IncomeExpenseActivity.kt` — Monthly Summary | L1620 | `"All Branches"` | ❌ না |
| 15 | RMP Performance (DoctorVisit-এর ভিতরে) | L3612–3625 | `"All"` L3467 | ❌ না |
| 16 | `print/PrintCenterActivity.kt` | L1088–1137 | `BranchSession.current` | ⚠ শুধু app চালু থাকা পর্যন্ত; "All" নেই |
| — | `native/ReportsActivity.kt` | পিকারই নেই | মাস্টারের জন্য সবসময় সব ব্রাঞ্চ (`null`) | — |

**খুবই গুরুত্বপূর্ণ নিরাপত্তা তথ্য:** মাস্টারের নিজের `branch` মানটাই আক্ষরিক `"All"` —
`native/StaffDirectory.kt:26` → `StaffAccount("8001080080", "TK BISWAS", "All", "master")`
এবং `03_NETLIFY_READY/config.js:15` → `master: [{mobile:'8001080080', name:'TK BISWAS', branch:'All'}]`
👉 তাই নতুন "মনে রাখা ব্রাঞ্চ" **আলাদা একটা চাবিতে** রাখতে হবে; সেশনের `branch` মান কখনো বদলানো যাবে না — বদলালে মাস্টারের সব permission ভেঙে যাবে।

---

## ৩. Android — "All" রাখলে কি সত্যিই বেশি ডাটা নামে?

### ✅ হ্যাঁ, নামে (সার্ভারেই ফিল্টার হয় — ব্রাঞ্চ বাছলে সত্যি ডাটা বাঁচে)

| Repository | কোড | সীমা |
|---|---|---|
| `DoctorQueueRepository.kt:129–131,183` | `"or=(branch.eq.X,branch.is.null)"`, না-হলে `null` | `patients` limit 5000, **তারিখের কোনো সীমা নেই**, ছবি সহ হতে পারে |
| `DraftRepository.kt:332` | `branch=eq.X` / বাদ | ৪টি টেবিল × limit 5000, ডিফল্টে **তারিখ সীমা নেই** |
| `PaymentRepository.kt:154,367` | `&branch=eq.X` / বাদ | limit 5000, তারিখ সীমা আছে (History mode-এ 2000-01-01 থেকে) |
| `ChamberAttendanceRepository.kt:253` | `&branch=eq.X` / বাদ | limit 5000; **তবে followups পড়াটায় (L308) তারিখ সীমা নেই** |
| `ReportsRepository.kt:134` | `branch=ilike.X` / বাদ | limit 5000, তারিখ সীমা নেই |
| `DoctorVisitRepository.kt` (একাধিক) | `branch=eq.X` / বাদ | — |
| `RefundedRecords.kt:69–70` | `branch=eq.X` / বাদ | — |
| `IncomeExpenseActivity.kt:282,883,1643` | `&branch=eq.X` / বাদ | মাসের সীমা + একটা **সীমাহীন** আগের-ব্যালেন্স পড়া |

**সবচেয়ে ভারী:** Doctor Queue — তারিখের সীমা নেই, 5000 সারি, ছবি থাকতে পারে। ঠিক এই কারণেই 15.08-এ শুধু ওই একটা স্ক্রিনে মনে-রাখা যোগ হয়েছিল। বাকিগুলোতে হয়নি।

### ⚠ আংশিক
`FollowUpRepository.kt:115–120` — ব্রাঞ্চ ফিল্টার শুধু `patients` (L614–617) ও `payments` (L621–623)-এ লাগে। `followups`/`enquiries`-এর সব পড়া (L575, 582, 589, 594, 629, 634, 641) ব্রাঞ্চ-নিরপেক্ষ, limit 5000। → Follow-up, Follow Calendar, Dialer, Notifications-এ ব্রাঞ্চ বাছলে অর্ধেক বাঁচে, পুরোটা নয়।

### ❌ না — ব্রাঞ্চ বাছলেও ডাটা একটুও বাঁচে না (client-side ফিল্টার)
- `TrashRepository.kt:39–45` — `fetchList("trash", null, 5000)`, ফিল্টার হার্ড-`null`
- `ChamberUnclosedRepository.kt:57–78` — দুটো পড়াই ব্রাঞ্চ-ছাড়া; ফিল্টার Kotlin-এ L78 (৩০ দিনের সীমা আছে)

---

## ৪. Web — একই যাচাই

### ব্রাঞ্চ পিকার ও ডিফল্ট
| # | সেকশন | কোড | ডিফল্ট | মনে রাখে? |
|---|---|---|---|---|
| 1 | Follow-up | app.js:3428, 3458–3460 | `'All'` | ❌ |
| 2 | Follow-up Calendar | app.js:8116, 8137 | Follow-up-এর সঙ্গে ভাগ করা | ❌ |
| 3 | Doctor Queue | app.js:4495, 4527 | `'All'` | ❌ |
| 4 | Collection | app.js:5086, 5104 | `'All'` | ❌ |
| 5 | Trash Bin | app.js:6022, 6024 | `'All'` | ❌ |
| 6 | Doctor Visit / RMP | app.js:6074, 6140, 6162 | `sessionStorage` | ⚠ শুধু ট্যাব খোলা থাকা পর্যন্ত; "All" অপশন নেই |
| 7 | RMP Performance | app.js:6211, 6215 | `'All'` — প্রতিবার খুললেই | ❌ |
| 8 | Chamber Attendance | app.js:8618, 8815–8827 | `'All'` | ❌ |
| 9 | Chamber Close | app.js:10737, 10791 | `'All'` | ❌ |
| 10 | I&E আজকের হিসাব | finance.js:16, 86–92 | `'All Branches'` | ❌ |
| 11 | I&E Ledger Sheet | finance.js:275, 279 | `'All Branches'` | ❌ প্রতিবার নতুন |
| 12 | I&E Monthly Summary | finance.js:663 | `'__all'` | ❌ প্রতিবার নতুন |
| 13 | Follow-up Filter modal | app.js:3494 | `''` = All Branch | ❌ প্রতিবার নতুন |

**localStorage-এ ব্রাঞ্চের কোনো চাবি নেই** (সম্পূর্ণ তালিকা যাচাই করা হয়েছে: `rk_session`, `rk_pending_cloud`, `rk_sync_meta`, … কোথাও ব্রাঞ্চ নেই)। `sessionStorage`-এ মাত্র দুটি চাবি: `doctorVisitSearch`, `doctorVisitMasterBranch`।
👉 তাই **পেজ রিফ্রেশ (F5) বা নতুন করে লগইন করলেই সব "All"-এ ফিরে যায়।**

### Web-এ ডাটা খরচ
**মূল ১০টি টেবিল কখনোই ব্রাঞ্চ দিয়ে ফিল্টার হয় না** — সবসময় সব ব্রাঞ্চের সারি নামে:
```
app.js:651-652   sb.from(ct).select(...).limit(2000)      // লগইনের সময়
app.js:892-893   sb.from(ct).select(__sel).limit(2000)    // ১৫ মিনিট পরপর
```
টেবিলগুলি (app.js:5): `enquiries, patients, payments, followups, medical, products, doctor_visits, briefings, trash, address_tags`.
👉 **Follow-up, Doctor Queue, Collection, Chamber, Trash, Draft, Reports, Chamber Close — এই সেকশনগুলোতে ওয়েবে ব্রাঞ্চ বাছলে এক বাইটও বাঁচে না।** ফিল্টারটা শুধু নামানোর পরে `Array.filter` দিয়ে হয়।

**যেখানে ওয়েবেও সত্যিই বাঁচে:**
- RMP: `app.js:6167` → `.eq('branch',branch).limit(2000)` (সবসময় ফিল্টার, "All" নেই)
- I&E আজকের হিসাব: `finance.js:173–175` — `'All Branches'` হলে সব ব্রাঞ্চ নামে, **কোনো `limit()` নেই**
- I&E Ledger Sheet: `finance.js:308–319` — এবং আগের-ব্যালেন্স পড়াটা (`pq`) **শুরু থেকে সব সারি** টানে, limit নেই
- I&E Monthly Summary: `finance.js:686–699` — একই ধরন
- RMP Performance RPC: `rmp_commission.js:72` — `p_branch: key==='All' ? null : key`

---

## ৫. সারমর্ম (সহজ ভাষায়)

1. **Android-এ "All" রাখলে সত্যিই বেশি ডাটা খরচ হয়** — বিশেষ করে Doctor Queue, Draft, Collection, Chamber, Income & Expense-এ। পাঁচ ব্রাঞ্চের সারি নামে।
2. **ওয়েবে বেশিরভাগ স্ক্রিনে "All" বাড়তি খরচ করায় না** (কারণ ওয়েব এমনিতেই সব টেবিল পুরো নামায়) — তবে **Income & Expense ও RMP**-এ সত্যিই বেশি খরচ হয়।
3. **আপনার অভিযোগ সঠিক** — মনে-রাখার কাজটা মাত্র ১টি Android স্ক্রিনে হয়েছে, বাকি ~২৮টি জায়গায় হয়নি।
4. এখনো **কোনো কোড বদলানো হয়নি** — আপনার সিদ্ধান্ত ও অনুমতির অপেক্ষায়।
