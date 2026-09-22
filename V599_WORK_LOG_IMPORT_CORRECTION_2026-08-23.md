# V599 — V598-এর নিজের ভুল সংশোধন (import বাদ পড়ে গিয়েছিল)
**তারিখ:** ২৩.০৮.২০২৬, ~১০:৪১ PM IST · **ভার্সন:** V599 / 5.99

## আমার ভুল (V598-এ)
V598-এ `import com.tkbiswas.pilesclinic.native.s` **মুছে ফেলে**
`import com.tkbiswas.pilesclinic.native.NoBengali` বসিয়েছিলাম — ধরে
নিয়েছিলাম `s` মানেই `NoBengali.s`। কিন্তু TK-এর পরের স্ক্রিনশটে দেখা গেল
নতুন এরর: `Unresolved reference: s` — লাইন 682-687-এ `p.s("mobile")`,
`p.s("age")` ইত্যাদি।

## আসল অবস্থা (যাচাই করে)
`s` আসলে **দুটো ভিন্ন জিনিস**, একই নামে:
1. `NoBengali.s(text)` — `NoBengali` অবজেক্টের ভেতরের member ফাংশন।
2. `fun JSONObject.s(key)` (`native/JsonExt.kt`) — সম্পূর্ণ আলাদা, top-level
   extension ফাংশন, Supabase-এর JSON থেকে null-safe স্ট্রিং পড়ার জন্য।
   এটাই `p.s("mobile")`-এর মতো জায়গায় ব্যবহার হয়।

এই ফাইলে দুটোই লাগে — তাই **দুটো import-ই দরকার**, একটা বাদ দিলেই চলবে না।

## সংশোধন
    import com.tkbiswas.pilesclinic.native.s
    import com.tkbiswas.pilesclinic.native.NoBengali
দুটো লাইনই এখন আছে। প্রজেক্টের `IncomeExpenseActivity.kt`-এ গিয়ে যাচাই করে
নিশ্চিত হওয়া গেছে — ওই ফাইলেও দুটো ফাংশনই আলাদা, একজায়গায় fully-qualified
নাম (`com.tkbiswas.pilesclinic.native.NoBengali.s(...)`) দিয়ে ব্যবহার হয়েছে
বলে সেখানে শুধু `s`-এর import যথেষ্ট ছিল — এই ফাইলে fully-qualified নয়,
তাই দুটো import-ই লাগবে।

## যাচাই
| পরীক্ষা | ফল |
|---|---|
| `.s("...")` কল মোট | 24 |
| তার ভেতরে `NoBengali.s(...)` | 15 |
| বাকি `p.s(...)` (JSON extension) | 9 |
| `tk_guard.py --release` | ✅ সম্পূর্ণ পাশ (২১টা যাচাই) |
| `verify_version_json.py` | ✅ V599/5.99 মিলেছে |

**সৎ স্বীকার:** V598-এর ফিক্স তাড়াহুড়ো করে ধরে নিয়ে করা হয়েছিল, যাচাই না করে
— TK-এর পরের স্ক্রিনশটে ধরা পড়ল। এবার দুটো `.s(` ব্যবহারের ধরন আলাদা করে
গুনে, প্রজেক্টের আরেকটা ফাইলে একই প্যাটার্ন কীভাবে সামলানো হয়েছে তা দেখে,
তারপর সংশোধন করা হয়েছে।

## বদলানো ফাইল
- `clinical/DoctorCheckupActivity.kt` — ১ লাইন import ফিরিয়ে আনা
- `app/build.gradle.kts` · `03_NETLIFY_READY/version.json` — V598→V599/5.99
