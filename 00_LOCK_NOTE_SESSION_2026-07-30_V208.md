# 🔒 LOCK NOTE — V208 (৩০.০৭.২০২৬ রাত)

**ভার্সন:** `versionCode 208` · `versionName 2.08` · পর্দায় **V208** · খাতার সারি **B201**
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## TK-এর নির্দেশ
"ঝুঁকি না থাকলে করুন" — `markExpected()` (Chamber Attendance)।

## ✅ আসল কারণ ও সমাধান
`markArrived()`-এর (খাতার সারি B196) **হুবহু একই সমস্যা** — ফোনে আগে সেভ হত ঠিকই, কিন্তু ক্লাউড-পাঠানো অংশ ফাংশনের ভিতরেই ব্লক করে বসেছিল। **একই সমাধান:** ক্লাউড-সিঙ্ক ও retry-queue এখন `BackgroundWork.run{}`-এ পিছনে চলে, আইডি আগের মতোই সঙ্গে সঙ্গে ফেরত।

```kotlin
fun markExpected(context, mobile, name, branch, expectedDate, staffMobile): String {
    val row = PaymentModel.buildExpectedMarkRow(...)
    DeletedGuard.unmark(...)                          // লোকাল-শুধু, অপরিবর্তিত
    LocalWorkflowStore(context).upsertPayment(row)     // আগের মতোই, সিঙ্ক্রোনাস
    val appCtx = context.applicationContext
    BackgroundWork.run { /* ক্লাউড + retry-queue, পিছনে */ }
    return row.s("id")                                 // আগের মতোই
}
```

## ✅ নিরাপত্তা যাচাই
- `DeletedGuard.unmark()` লোকাল-শুধু (SharedPreferences, নেটওয়ার্ক নেই) — কোড পড়ে নিশ্চিত করা হয়েছে, তাই সিঙ্ক্রোনাস রাখা নিরাপদ
- **৭টা কল-সাইট** (ChamberAttendanceActivity ১, FollowCalendarActivity ১, FollowUpActivity ২, PatientTimelineActivity ২ — এর মধ্যে ২টা ইতিমধ্যেই `BackgroundWork.run{}`-এর ভিতরে ছিল) — কেউই ফেরত-আসা আইডির উপর নির্ভর করে না (markArrived()-এর Undo-ফিচারের মতো কিছু এখানে নেই), সবাই শুধু Toast দেখায়/board রিফ্রেশ করে
- ঝুঁকি markArrived()-এর সমান — কম, কারণ এখানে Payment-এর মতো রোগী-খোঁজা/day-guard-এর জটিলতা নেই

## ⛔ যা ছোঁয়া হয়নি
- ৭টা কল-সাইটের একটাও
- ডেটা মডেল/টেবিল কাঠামো
- retry-queue-এর নিয়ম

## 🔍 যাচাই
- পাহারাদার (`tk_guard.py`) **১৭/১৭ পাশ**
- ৭টা কল-সাইটই এডিটের পরে আবার গ্রেপ করে নিশ্চিত করা হয়েছে

## 📌 সারসংক্ষেপ — Chamber Attendance-এর লোডিং ফিক্স (সম্পূর্ণ)
| ধাপ | কাজ | ভার্সন | অবস্থা |
|---|---|---|---|
| ১ | Mark Arrived | V204 | 🟢 সম্পূর্ণ |
| ২ | Remark | V205 | 🟢 সম্পূর্ণ |
| ২.১ | Remark (Review পপ-আপ) | V206 | 🟢 সম্পূর্ণ |
| ৩ | Payment | V207 | 🟡 ইচ্ছাকৃতভাবে স্থগিত (ঝুঁকির কারণে) |
| ৪ | Expected | V208 | 🟢 সম্পূর্ণ |

## 🔴 এখনো বাকি
- **TK-এর লাইভ টেস্ট**
- Payment-এর বড় গতি-বৃদ্ধি — ভবিষ্যতে আলাদা সেশনে (V207 লক নোটের রাস্তা অনুযায়ী)
- **B148 — RLS** ⛔ TK-এর অনুমতি ছাড়া নিষেধ
- `03_NETLIFY_READY` Netlify-তে আপলোড (TK-এর কাজ)
