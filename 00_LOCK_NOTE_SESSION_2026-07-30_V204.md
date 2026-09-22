# 🔒 LOCK NOTE — V204 (৩০.০৭.২০২৬ রাত)

**ভার্সন:** `versionCode 204` · `versionName 2.04` · পর্দায় **V204** · খাতার সারি **B196**
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## এই ভার্সন কী (এবং কী না)
এটা **Chamber Attendance-এর লোডিং ফিক্সের ধাপে-ধাপে পরিকল্পনার প্রথম ধাপ মাত্র** — শুধু **Mark Arrived**। TK-এর নিজের নির্দেশ: *"সাবধানে করতে হবে... সময় লাগবে লাগুক, আমি আছি।"* Remark ও Payment-এর ফিক্স **এখনো বাকি** — পরের কোনো সেশনে/ধাপে TK অনুমতি দিলে হবে।

## ✅ আসল কারণ (কোড ধরে)
`ChamberAttendanceRepository.markArrived()` **আগে থেকেই** অফলাইন-ফার্স্ট ছিল:
1. ফোনে আগে সেভ (`LocalWorkflowStore.upsertPayment`)
2. তারপর ক্লাউডে পাঠানোর চেষ্টা
3. ব্যর্থ হলে retry-queue-এ

কিন্তু **ধাপ ২-৩ (ক্লাউড-পাঠানো) এই ফাংশনের ভিতরেই ব্লক করে বসেছিল** — অর্থাৎ ফাংশনটা ফেরত না দেওয়া পর্যন্ত কলকারী (৫টা জায়গা) কিছুই দেখতে পেতেন না। তাই ফোনে সেভ হয়ে যাওয়ার পরেও "Marked Arrived ✅" বার্তা/পর্দা-রিফ্রেশ ক্লাউড-কল শেষ না হওয়া পর্যন্ত আসত না — দুর্বল নেটে এটাই "লোডিং" মনে হত।

## ✅ সমাধান
```kotlin
fun markArrived(context, mobile, name, branch, staffMobile): String {
    val row = PaymentModel.buildAttendanceMarkRow(...)
    LocalWorkflowStore(context).upsertPayment(row)   // আগের মতোই, এখনও সিঙ্ক্রোনাস
    val appCtx = context.applicationContext
    BackgroundWork.run {                              // ⬅️ নতুন — পিছনে চলে
        val ok = try { SupabaseClient.upsert("payments", row) } catch (_: Throwable) { false }
        if (ok) LocalWorkflowStore(appCtx).upsertPayment(row, "SYNCED")
        else { /* retry-queue-এ তোলা, আগের মতোই */ }
    }
    return row.s("id")                                // আগের মতোই, একই সময়ে গণনা
}
```

## ✅ নিরাপত্তা যাচাই (৫টা কল-সাইট, প্রতিটা আলাদাভাবে)

| কল-সাইট | কী নির্ভর করে | নিরাপদ কেন |
|---|---|---|
| `ChamberAttendanceActivity.kt:1147` (Undo-সাপোর্টেড মোড) | ফেরত-আসা `id` দিয়ে `markedRowId` সেট করে | id সবসময় স্থানীয় সেভের পরেই, সিঙ্ক্রোনাসভাবে গণনা করা হয় — বদলায়নি |
| `ChamberAttendanceActivity.kt:1218` (`markArrivedFromRow`) | শুধু "ok" বুলিয়ান (try-catch-এ true) | ফাংশন এক্সেপশন না দিলেই true — আগেও প্রায় সবসময় true-ই ফিরত |
| `FollowUpActivity.kt:3653` | শুধু Toast দেখায় | কিছুর উপর নির্ভর করে না |
| `GlobalSearchActivity.kt:252` | শুধু Toast দেখায় | কিছুর উপর নির্ভর করে না |
| `PatientTimelineActivity.kt:918` | শুধু Toast দেখায় | কিছুর উপর নির্ভর করে না |

**⛔ পাঁচটা কল-সাইটের একটাও পরিবর্তন করা হয়নি** — শুধু রেপোজিটরির একটা ফাংশনের ভিতরের কাজের ক্রম বদলেছে।

## 🔍 একই ধরনের সমস্যা প্রজেক্টে আর কোথায় (পাওয়া গেছে, ইচ্ছাকৃতভাবে ছোঁয়া হয়নি)
`ChamberAttendanceRepository.kt`-এর **`markExpected()`** ফাংশনে (ঠিক markArrived()-এর নিচেই) **হুবহু একই প্যাটার্ন** — ক্লাউড-পাঠানো ফাংশনের ভিতরেই ব্লক করে। TK-এর সঙ্গে সম্মত ধাপে-ধাপে পরিকল্পনা অনুযায়ী **এটা এই ভার্সনে ছোঁয়া হয়নি** — ভবিষ্যতে TK চাইলে একই নিরাপদ প্যাটার্নে ঠিক করা যাবে।

## ⛔ যা এখনো বাকি (Chamber Attendance-এর মধ্যেই)
- **Remark লেখা/এডিট করা** (showRemarkDialog, editRemarkInReview)
- **Payment নেওয়া/ঠিক করা** (confirmedTakePayment, fixPaymentInReview, editOnePaymentRow) — টাকা জড়িত বলে সবচেয়ে সাবধানে, সবার শেষে করার কথা
- **markExpected()** (উপরে উল্লেখ)

## 🔍 যাচাই
- পাহারাদার (`tk_guard.py`) **১৭/১৭ পাশ**
- ব্র্যাকেট-গণনা `ChamberAttendanceRepository.kt`-এ আলাদা করে মিলিয়ে দেখা হয়েছে
- ৫টা কল-সাইটই এডিটের পরে আবার গ্রেপ করে নিশ্চিত করা হয়েছে — একটাও বদলায়নি

## 🔴 এখনো বাকি
- **TK-এর লাইভ টেস্ট** — Mark Arrived-এর গতি
- **Chamber Attendance-এর বাকি ধাপ** (Remark, Payment, markExpected) — TK-এর অনুমতিতে পরের ধাপ
- **B148 — RLS** ⛔ TK-এর অনুমতি ছাড়া নিষেধ
- `03_NETLIFY_READY` Netlify-তে আপলোড (TK-এর কাজ)
