# 🔒 LOCK NOTE — V206 (৩০.০৭.২০২৬ রাত)

**ভার্সন:** `versionCode 206` · `versionName 2.06` · পর্দায় **V206** · খাতার সারি **B199**
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## TK-এর নির্দেশ
"ঝুঁকিহীন ভাবে কাজটা করুন" — `editRemarkInReview()` (Chamber Attendance-এর "Close Chamber Review" পপ-আপের ভিতরের Remark এডিট)।

## ✅ আসল কারণ
রিমার্ক বদলে Save চাপলে আগে `refreshBoardAndReopenReview()` চলত:
```kotlin
private fun refreshBoardAndReopenReview() {
    currentReviewDialog?.dismiss(); currentReviewDialog = null
    loadBoard(onRendered = { lastBoard?.let { showCloseReview(it) } })
}
```
`loadBoard()` **পুরো বোর্ড নতুন করে ক্লাউড থেকে নামায়** (৪টা প্যারালাল নেটওয়ার্ক-কল) — তাই Review পপ-আপ আবার খুলতে দেরি হত।

## ✅ সমাধান (showRemarkDialog()-এর/সারি B197-এর হুবহু একই প্যাটার্ন)
`showCloseReview(board: ChamberAttendanceBoard)` ফাংশনটা যাচাই করে দেখা গেছে — এটা শুধু একটা বোর্ড-অবজেক্ট নিয়ে পপ-আপ **বানায়়**, ভিতরে কোনো নেটওয়ার্ক-কল নেই। তাই নতুন করে ক্লাউড থেকে আনার দরকারই নেই — স্টাফ যা টাইপ করেছেন তা দিয়ে **এই স্ক্রিনের মেমোরিতে থাকা বোর্ড** (`lastBoard`) সরাসরি আপডেট করে সেটাই `showCloseReview()`-এ পাঠানো হয়।

```kotlin
val board0 = lastBoard
if (board0 != null) {
    val updatedRows = board0.rows.map { if (it.mobile == r.mobile) it.copy(remark = remark) else it }
    val updatedBoard = board0.copy(rows = updatedRows)
    lastBoard = updatedBoard
    currentReviewDialog?.dismiss(); currentReviewDialog = null
    showCloseReview(updatedBoard)     // নেটওয়ার্ক ছাড়াই, সঙ্গে সঙ্গে
}
// Toast + আসল সেভ পিছনে (BackgroundWork.run{})
```

⛔ **`FollowUpRepository.updateRemark()`-এর ভিতরে একটা অক্ষরও ছোঁয়া হয়নি** — ইতিহাস-মেলানো ও ২৮.০৭.২০২৬-এর যাচাই-বাগ-ফিক্স সম্পূর্ণ অক্ষত, ঠিক যেমন B197-এ ছিল।

## ✅ নিরাপত্তা যাচাই
- `showCloseReview()`-এর ভিতরে নেটওয়ার্ক-কল নেই — হাতে পড়ে নিশ্চিত করা হয়েছে
- `currentReviewDialog` সঠিকভাবে dismiss+reset হয়, তারপর নতুন করে দেখানো হয় — পুরনো প্যাটার্নের সঙ্গেই মেলে (আগেও `refreshBoardAndReopenReview()` একই কাজ করত)
- এই একটাই কল-সাইট বদলেছে; `updateRemark()`-এর অন্য ১১টা কল-সাইট অপ্রভাবিত

## 🔍 যাচাই
- পাহারাদার (`tk_guard.py`) **১৭/১৭ পাশ**
- ব্র্যাকেট-গণনা `ChamberAttendanceActivity.kt`-এ আলাদা করে মিলিয়ে দেখা হয়েছে

## 🔴 এখনো বাকি
- **TK-এর লাইভ টেস্ট**
- **Payment (ধাপ ৩)** — ইচ্ছাকৃতভাবে স্থগিত (খাতার সারি B198), ভবিষ্যতের সমাধান-রাস্তা লেখা আছে
- `markExpected()` — একই পুরনো markArrived-জাতীয় সমস্যা, ছোঁয়া হয়নি
- **B148 — RLS** ⛔ TK-এর অনুমতি ছাড়া নিষেধ
- `03_NETLIFY_READY` Netlify-তে আপলোড (TK-এর কাজ)
