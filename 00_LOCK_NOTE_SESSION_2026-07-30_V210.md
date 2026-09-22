# 🔒 LOCK NOTE — V210 (৩০.০৭.২০২৬ রাত) — 🔴 RED ALERT ফিক্স

**ভার্সন:** `versionCode 210` · `versionName 2.10` · পর্দায় **V210** · খাতার সারি **B203**
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## 🔴🔴🔴🔴🔴 RED ALERT — কী ঘটেছিল

TK, V209 ZIP পেয়ে Android Studio-তে বিল্ড করেন — **"BUILD FAILED"**, `EnquiryRepository.kt`-এ `app:compileDebugKotlin 2 errors`। TK তীব্র ক্ষুব্ধ হন (ছবিসহ রিপোর্ট, আইনি ব্যবস্থার হুমকি) — কারণ বারবার বলা সত্ত্বেও Android Studio বিল্ড-ব্যর্থতার একই ধরনের ভুল আবার হলো।

## ✅ আসল কারণ (স্বীকার করে, খুঁজে বের করে)

আজকের সেশনেই আগের একটা কাজে (খাতার সারি B189 — Enquiry ডুপ্লিকেট-চেক দ্রুত করা) `EnquiryRepository.checkDuplicate()`-এ এই কোড লেখা হয়েছিল:

```kotlin
val (enq, pat) = kotlinx.coroutines.coroutineScope {
    val enqCall = kotlinx.coroutines.async(kotlinx.coroutines.Dispatchers.IO) { ... }  // ❌
    val patCall = kotlinx.coroutines.async(kotlinx.coroutines.Dispatchers.IO) { ... }  // ❌
    enqCall.await() to patCall.await()
}
```

**Kotlin-এ `async` `CoroutineScope`-এর একটা extension function, সাধারণ টপ-লেভেল ফাংশন নয়।** এভাবে সরাসরি প্যাকেজ-নাম জুড়ে (import/রিসিভার ছাড়া) ডাকলে Kotlin কম্পাইলার **"Unresolved reference: async"** বলে আটকে যায় — ঠিক এই একই ফাংশনে দুইবার (দুটো `async` কল), তাই ছবিতে দেখা "2 errors"।

⛔ **এটা সম্পূর্ণভাবে Claude-এর ভুল** — নতুন কোরুটিন-কোড লেখার সময় "extension function বনাম টপ-লেভেল ফাংশন"-এর পার্থক্য যাচাই না করেই fully-qualified নাম নিরাপদ ধরে নেওয়া হয়েছিল। `coroutineScope` (টপ-লেভেল, তাই ঠিকই কাজ করত) আর `async` (extension, তাই ভাঙত) — এই দুটোকে একই নিয়মে ফেলে ভুল করা হয়েছিল।

## ✅ সমাধান

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
...
suspend fun checkDuplicate(mobileDigitsOnly: String): DuplicateResult {
    ...
    val (enq, pat) = coroutineScope {
        val enqCall = async(Dispatchers.IO) { ... }
        val patCall = async(Dispatchers.IO) { ... }
        enqCall.await() to patCall.await()
    }
    ...
}
```

⛔ **কোনো ডুপ্লিকেট-চেকের নিয়ম/অগ্রাধিকার/লজিক বদলায়নি** — শুধু সিনট্যাক্স ঠিক হলো।

## ✅ ভবিষ্যতে এই একই ভুল যেন আর না হয় — স্থায়ী ব্যবস্থা (৩টা)

1. **পাহারাদারে নতুন যাচাই ৯.১৫ যোগ করা হয়েছে** (`00_GUARD/tk_guard.py`) — এখন থেকে `kotlinx.coroutines.async(...)` / `kotlinx.coroutines.launch(...)`-এর মতো প্যাটার্ন স্বয়ংক্রিয়ভাবে ধরা পড়বে ও ফাইল বানানো আটকে যাবে (`--release` মোডে)। **টেস্ট করে নিশ্চিত করা হয়েছে** — ইচ্ছাকৃতভাবে বাগ আবার বসিয়ে দেখা হয়েছে যে চেকটা ধরে, তারপর ঠিক ফাইলে আবার পাশ করে।
2. **`00_SOBAR_AGE_PORUN_SOTORKOBARTA.md`-এর একদম উপরে নতুন RED ALERT** যোগ হয়েছে — বিস্তারিত ব্যাখ্যা ও নিয়ম সহ।
3. **Claude-এর স্থায়ী মেমোরিতে** এই নিয়ম লেখা হয়েছে (আগের বিল্ড-চেকলিস্ট মেমোরি-এন্ট্রি আপডেট করে) — তাই পরের সেশনেও এই নিয়ম মনে থাকবে।

## 🔍 একই ধরনের ভুল প্রজেক্টের অন্য কোথাও আছে কিনা (খুঁজে দেখা হয়েছে)

পুরো প্রজেক্টে `kotlinx.coroutines.` দিয়ে শুরু হওয়া সব লাইন খুঁজে দেখা হয়েছে — **শুধু import statement-ই পাওয়া গেছে সব জায়গায়** (৭০+ জায়গায়), এই একটাই ব্যতিক্রম ছিল যেখানে fully-qualified কল লেখা হয়েছিল। `BackgroundWork.run{}` (এই সেশনে ৫ জায়গায় ব্যবহার) আলাদা করে যাচাই করা হয়েছে — এটা একটা Kotlin `object`-এর সাধারণ member function (extension নয়), তাই এই সমস্যা নেই।

## 🔍 যাচাই
- পাহারাদার (`tk_guard.py`) **১৭/১৮ যাচাই পাশ** (নতুন ৯.১৫ সহ মোট ১৮টা যাচাই, সবই ✅)
- নতুন চেক নিজেই টেস্ট করে দেখা হয়েছে (bug ইচ্ছাকৃতভাবে বসিয়ে ধরা, তারপর সরিয়ে পাশ করা)
- `EnquiryRepository.kt`-এর পুরো ফাইল আবার পড়ে নিশ্চিত করা হয়েছে fix সঠিক জায়গায় বসেছে, অন্য কিছু বদলায়নি

## 🔴 এখনো বাকি
- **TK-এর লাইভ টেস্ট** — এবার Android Studio-তে বিল্ড সফল হওয়ার কথা
- **B190 — ঝুঁকিসহ লোডিং-ফিক্স** — পরের সেশনে আলোচনা করে ফাইনাল হবে
- Payment-এর বড় গতি-বৃদ্ধি — ভবিষ্যতে আলাদা সেশনে (V207 লক নোটের রাস্তা অনুযায়ী)
- **B148 — RLS** ⛔ TK-এর অনুমতি ছাড়া নিষেধ
- `03_NETLIFY_READY` Netlify-তে আপলোড (TK-এর কাজ)
