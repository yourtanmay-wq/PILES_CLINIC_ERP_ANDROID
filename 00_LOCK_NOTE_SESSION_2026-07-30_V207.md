# 🔒 LOCK NOTE — V207 (৩০.০৭.২০২৬ রাত)

**ভার্সন:** `versionCode 207` · `versionName 2.07` · পর্দায় **V207** · খাতার সারি **B200**
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## TK-এর নির্দেশ (হুবহু)
*"সাবধানে করুন, কারণ একবার করলে তো সারা জীবনের জন্য হয়ে যাবে। এখনো যেহেতু পেমেন্ট বহুত পুরনো হয়ে যায়নি... একটু সাবধানে করুন যাতে ভুল না হয়। একটু সময় নিয়ে করুন, আমি লাইনে আছি। রাস্তা যেন ভালো হয় সেইভাবে করুন, সময় নিয়ে করুন, অসুবিধা নেই।"*

## ✅ গভীর অনুসন্ধান — একটা লুকানো ঝুঁকি ধরা পড়ল

Payment-এর কোড খুলে গভীরভাবে দেখা হয়েছিল: `takePaymentPopup()` dialog খোলার সময়েই একবার `findPatientByMobile()` দিয়ে রোগী খোঁজে (Bill/Paid দেখানোর জন্য)। তারপর "Yes, Save" চাপলে `confirmedTakePayment()`-এর ভিতরে **আবার** সেই একই `findPatientByMobile()` কল হয় — প্রথম নজরে এটা **অপ্রয়োজনীয় সদৃশ কাজ** মনে হচ্ছিল, সরিয়ে দিলে একটা গোটা নেটওয়ার্ক-রাউন্ড-ট্রিপ বাঁচত।

**কিন্তু গভীরে গিয়ে দেখা গেল এটা অপরিহার্য:**
```kotlin
class PaymentRepository(private val context: Context? = null) {
    ...
    private val treatmentPaymentCounts = mutableMapOf<String, Int>()
    private val treatmentPaymentDates = mutableMapOf<String, List<String>>()
    private val treatmentPaidOnDate = mutableMapOf<String, Double>()   // ⬅️ day-guard এখান থেকে পড়ে
```

এই তিনটে ম্যাপ **প্রতিটা `PaymentRepository()` instance-এর নিজস্ব আলাদা মেমোরিতে** থাকে — `companion object` (শেয়ার্ড/গ্লোবাল) নয়। `takePaymentPopup()` ও `confirmedTakePayment()` — দুটোই **আলাদা আলাদা `PaymentRepository(...)` instance** তৈরি করে। তাই:

- যদি `confirmedTakePayment()`-এর ভিতরের দ্বিতীয় খোঁজাটা বাদ দিয়ে প্রথম dialog-এর সময়ের রোগী-তথ্য পুনর্ব্যবহার করা হত, তাহলে `confirmedTakePayment()`-এর নিজের নতুন `repo` instance-এর `treatmentPaidOnDate` ম্যাপ **কখনো ভরাটই হত না**।
- তখন `repo.paidOnDateFor(patient.id)` (দিনে দুবার টাকা নেওয়া আটকানোর সুরক্ষা-চেক) **সবসময় ০.০** ফেরত দিত — যদিও ওই রোগীর নামে আজ সত্যিই আগে টাকা নেওয়া হয়ে থাকত।
- ফল: স্টাফ একই রোগীর নামে ভুলবশত দুবার টাকা নিলেও সতর্কবার্তা **আর কখনো আসত না** — নিঃশব্দে ভেঙে যেত।

## ✅ সিদ্ধান্ত: এই লুকআপ ছোঁয়া হয়নি

TK-এর সবচেয়ে বড় দুশ্চিন্তা টাকার হিসাব ও ডুপ্লিকেট-প্রতিরোধ — তাই এই ঝুঁকি নেওয়া হয়নি। `confirmedTakePayment()`-এর ভিতরের রোগী-খোঁজা, day-guard-চেক, ডুপ্লিকেট-প্রতিরোধ — **সবকিছু হুবহু আগের মতোই** আছে।

## ✅ যেটুকু ঝুঁকিহীনভাবে করা হলো

"Yes, Save" চাপার **সঙ্গে সঙ্গে**ই একটা "Saving…" স্বীকৃতি-বার্তা দেখানো হয়:

```kotlin
.setPositiveButton("Yes, Save") { _, _ ->
    android.widget.Toast.makeText(this, "Saving…", android.widget.Toast.LENGTH_SHORT).show()
    confirmedTakePayment(row, mode, digits, value, enteredBill, pickedActualDate)
}
```

⛔ এর নিচের `confirmedTakePayment()` ফাংশনের **একটা অক্ষরও বদলায়নি**।

## 🔮 ভবিষ্যতের জন্য — সঠিক (কিন্তু বড়) সমাধানের রাস্তা

যদি ভবিষ্যতে Payment-এর গতি সত্যিই বড় সমস্যা তৈরি করে, নিরাপদ সমাধান:

`PaymentRepository`-র `treatmentPaymentCounts` / `treatmentPaymentDates` / `treatmentPaidOnDate` — এই তিনটে ম্যাপ **`companion object`-এ (শেয়ার্ড/গ্লোবাল)** সরিয়ে নেওয়া। তাহলে যেকোনো `PaymentRepository()` instance একই রোগীর জন্য একবার এই তথ্য জানলে, পরের যেকোনো instance-ও সেটা জানবে — তখন `confirmedTakePayment()`-এ দ্বিতীয়বার খোঁজা নিরাপদে বাদ দেওয়া যাবে।

⚠️ **এটা এখনই করা হয়নি**, কারণ:
- `PaymentRepository` ক্লাসটা প্রজেক্টের **অনেক জায়গায়** ব্যবহার হয় (শুধু Chamber Attendance নয় — PaymentActivity, FollowUpActivity-র Advance/2nd-payment, ইত্যাদি)
- companion object-এ সরালে **মাল্টি-থ্রেড সেফটি** (একই সময়ে দুটো পেমেন্ট প্রসেস হলে) আলাদা করে যাচাই করতে হবে
- এটা একটা বড়, আলাদা করে যত্ন নিয়ে করার কাজ — TK চাইলে ভবিষ্যতে আলাদা সেশনে সাবধানে করা হবে

## 🔍 যাচাই
- পাহারাদার (`tk_guard.py`) **১৭/১৭ পাশ**
- `ChamberAttendanceActivity.kt`-এ ব্র্যাকেট-গণনা আলাদা করে মিলিয়ে দেখা হয়েছে
- `confirmedTakePayment()` ফাংশনের একটা লাইনও বদলায়নি — শুধু তার আগের Toast যোগ হয়েছে, ফাইলে সরাসরি তুলনা করে নিশ্চিত করা হয়েছে

## 🔴 এখনো বাকি
- **TK-এর লাইভ টেস্ট**
- Payment-এর বড় গতি-বৃদ্ধি — ভবিষ্যতে আলাদা সেশনে (উপরের রাস্তা অনুযায়ী)
- `markExpected()` — একই পুরনো markArrived-জাতীয় সমস্যা, ছোঁয়া হয়নি
- **B148 — RLS** ⛔ TK-এর অনুমতি ছাড়া নিষেধ
- `03_NETLIFY_READY` Netlify-তে আপলোড (TK-এর কাজ)
