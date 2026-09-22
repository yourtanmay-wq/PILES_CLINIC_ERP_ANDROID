# V598 — DoctorCheckupActivity.kt বিল্ড-এরর সারানো
**তারিখ:** ২৩.০৮.২০২৬, ~১০:৪০ PM IST · **ভার্সন:** V598 / 5.98

## TK যা দেখিয়েছেন (Android Studio স্ক্রিনশট)
    Unresolved reference: NoBengali  (লাইন 939, 1676, 1820, 1830, 1836, 1985, 2013, 2014 ...)
    Cannot infer a type for this parameter. Please specify it explicitly (× ২)
    Gradle build failed in 1 m 13 s 741 ms
ফাইল: `clinical/DoctorCheckupActivity.kt`

## আসল কারণ
ফাইলের ওপরের import ভুল ছিল:
    import com.tkbiswas.pilesclinic.native.s
কিন্তু আসল অবজেক্টের নাম `NoBengali` — `s` তার **ভেতরের একটা ফাংশন মাত্র**
(`fun s(text: String?): String`)। তাই কম্পাইলার `NoBengali` শব্দটাই চিনতে
পারছিল না — যেখানে যেখানে `NoBengali.s(...)` লেখা ছিল (~১৫ জায়গা), সবকটাই
"Unresolved reference"। নিচের দুটো "Cannot infer type" এররও এই একই ভাঙনের
চেইন-রিঅ্যাকশন (lambda-র প্যারামিটার টাইপ অনুমান করতে ব্যর্থ হয় যখন
আশেপাশের কল-ই আনরিজলভড থাকে)।

## সংশোধন — এক লাইন
    import com.tkbiswas.pilesclinic.native.s
    → import com.tkbiswas.pilesclinic.native.NoBengali

⛔ ফাইলের আর কিছু ছোঁয়া হয়নি — `NoBengali.s(...)` কলগুলো আগের মতোই আছে,
শুধু import ঠিক হলো।

## যাচাই
| পরীক্ষা | ফল |
|---|---|
| `tk_guard.py --release` (সব ২১টা মেশিন-যাচাই, নতুন [৯.১৮] Unresolved-reference চেকসহ) | ✅ সম্পূর্ণ পাশ |
| `verify_version_json.py` | ✅ V598 / 5.98 মিলেছে |
| `node --check app.js` (ওয়েব) | কাজ নেই এই ফাইলে, তাই ছোঁয়া হয়নি |
| brace/paren balance | parens ঠিক; brace-এর ছোট ফারাক (৩) আগে থেকেই ছিল, এই এক-লাইন বদলের কারণে নয় |

**সৎ সীমা:** এই পরিবেশে Android SDK/Gradle নেই বলে সত্যিকারের Gradle build
চালানো যায়নি (V597-এর নতুন android.jar-ভিত্তিক `verify_kotlin_compile.py`ও
এই সেশনে network না থাকায় kotlinc নামাতে পারেনি, তাই সেটা SKIPPED দেখাচ্ছে —
PASS ধরা হয়নি)। কিন্তু import-fix-টা নিজে সরাসরি স্ক্রিনশটের এরর-লাইন ও
কারণের সাথে হুবহু মেলানো, আর `tk_guard.py`-র `[৯.১৮] import ঠিক আছে` চেক
পাশ করেছে। TK Android Studio-তে বিল্ড করে চূড়ান্ত নিশ্চিত করবেন।

## বদলানো ফাইল
- `clinical/DoctorCheckupActivity.kt` — ১ লাইন (import)
- `app/build.gradle.kts` · `03_NETLIFY_READY/version.json` — V597→V598/5.98
