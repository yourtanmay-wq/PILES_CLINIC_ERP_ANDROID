# V597 — Android Studio-তে বিল্ড ভাঙা সারানো
**তারিখ:** ২৩.০৮.২০২৬ · **ভার্সন:** V597 / 5.97

---

## TK যা দেখিয়েছেন

V596-এর ফাইল Android Studio-তে Build দিতে গিয়ে —

    Type mismatch: inferred type is Double but Float was expected   (× অনেকগুলো)
    None of the following functions can be called with the arguments supplied
    Gradle build failed in 1 m 2 s 592 ms

ফাইল: `clinical/AnatomyView.kt` · ফাংশন: `ksHealLump()`

TK: *"এরকম একটা নোংরা কাজ আপনাকে কে করতে বলল? কেন আপনি যে ফাইল পাঠিয়েছেন
এন্ড্রয়েড স্টুডিওতে বিল্ড হচ্ছে না"*

---

## আসল কারণ (খুঁজে বার করা, অনুমান নয়)

`AnatomyModel.Lump`-এর `len` ও `wide` দুটোই **Double**।

    val sc = Math.min(dst.width(), dst.height()) / 100f      // Float
    val r  = Math.max(g.len * sc * 0.62f, g.wide * sc * 0.72f)
             //       Double × Float  ⇒  Double

কিন্তু Canvas-এর ঘরগুলো **Float** চায় — `saveLayerAlpha`, `RectF`,
`RadialGradient`, `drawCircle`। তাই একটাই ভুল থেকে **১২টা** ভুল বেরোয়।

কবে ঢুকেছিল: **V589** (`0d119f4`) — অর্থাৎ V589 থেকে V596 পর্যন্ত কোনো
ফাইলই Android Studio-তে বিল্ড হত না।

### সারানো — এক লাইন, হিসাব অটুট

    val r = Math.max(g.len * sc * 0.62f, g.wide * sc * 0.72f).toFloat()

গুণ-ভাগ আগের মতোই, শুধু শেষে Float-এ নামানো হলো। ছবিতে কিচ্ছু বদলাবে না।

---

## 🔴 পাহারাদার কেন ধরেনি — সৎ উত্তর

`verify_kotlin_compile.py` কম্পাইল করত ঠিকই, কিন্তু —

1. **ক্লাসপাথে android.jar ছিল না**। `Canvas`/`RectF` কম্পাইলার চিনতই না,
   তাই টাইপ মিলছে কি না দেখার প্রশ্নই উঠত না।
2. পাহারাদারের `CASCADE` তালিকায় **"type mismatch"** লেখা ছিল — অর্থাৎ ওই
   বার্তাটাকে ধরেই নেওয়া হত "লাইব্রেরি নেই বলে গোলমাল"। **ঠিক যে বার্তাটা
   TK-র বিল্ড ভেঙেছে, সেটাই আমি নিজের হাতে চাপা দিয়ে রেখেছিলাম।**

### দুটোই বন্ধ করা হলো

1. **আসল android.jar (compileSdk 34-এর সমান)** ক্লাসপাথে যোগ করা হলো।
   Google-এর সাইট এই পরিবেশ থেকে বন্ধ, তাই Maven Central-এর robolectric
   `android-all-14` ব্যবহার — API 34, অ্যাপের `compileSdk = 34`-এর সঙ্গে মেলে।
   একবার নামে, `00_GUARD/.kotlinc/android34.jar`-এ থাকে (গিটে যায় না)।
2. `"type mismatch"` **CASCADE তালিকা থেকে তুলে দেওয়া হলো**।
3. নতুন কড়া নিয়ম `real_type_errors()` — **যে ফাইলে একটাও
   "unresolved reference" নেই, অথচ অন্য ভুল আছে**, সেটা লাইব্রেরি
   না-থাকার গোলমাল হতেই পারে না ⇒ সরাসরি FAIL, বেসলাইনের **আগেই**।

---

## যাচাই (চালিয়ে, অনুমানে নয়)

| পরীক্ষা | ফল |
|---|---|
| android.jar দিয়ে `clinical/` কম্পাইল — সারানোর **আগে** | ঠিক **১২টা** ভুল, TK-র পর্দার সঙ্গে মেলে ✅ |
| একই কম্পাইল — সারানোর **পরে** | **০** ✅ |
| গোটা প্রকল্প (২৬৯ ফাইল) android.jar দিয়ে | "unresolved শূন্য অথচ ভুল আছে" এমন ফাইল **০** ✅ |
| **উল্টো পরীক্ষা** — ভুলটা ইচ্ছে করে ফেরত বসানো | পাহারাদার **FAIL** বলে, ১২টা ভুলই নাম ধরে দেখায় ✅ |
| ভুল সারিয়ে আবার | **PASS**, নতুন ভুল ০ ✅ |
| 00_GUARD-এর সব পাহারাদার | সবগুলো সবুজ ✅ |
| `node --check app.js` | ঠিক ✅ |

---

## ⛔ যা এখনো এখানে যাচাই হয় না (লুকানো হয়নি)

- **androidx / retrofit / okhttp / gson** এই পরিবেশে নামানো যায় না
  (`maven.google.com` বন্ধ)। তাই যেসব ফাইল androidx-এর ক্লাস থেকে আসে
  (Activity-গুলো), তাদের ভিতরের টাইপ এখনো পুরোপুরি মেলানো যায় না।
- resource (R.java) · manifest · dex · ProGuard — কিছুই এখানে যাচাই হয় না।

`AnatomyView` `android.view.View` থেকে আসে, androidx থেকে নয় — তাই এই
ভুলটা নতুন নিয়মে ধরা পড়ে।

---

## বদলানো ফাইল

- `clinical/AnatomyView.kt` — এক লাইন (`.toFloat()`)
- `00_GUARD/verify_kotlin_compile.py` — android.jar + নতুন নিয়ম
- `00_GUARD/kotlin_noise_baseline.txt` — নতুন ক্লাসপাথে নতুন বেসলাইন (২৯৭ → ৩১৮)
- `build.gradle.kts` · `version.json` — V597 / 5.97
