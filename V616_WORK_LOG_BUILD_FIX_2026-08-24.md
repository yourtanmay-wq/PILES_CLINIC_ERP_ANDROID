# V616 — Build Error ঠিক (আমার নিজের ভুল, V608 থেকে)
**তারিখ:** ২৪.০৮.২০২৬ · **ভার্সন:** V616 / 6.16

## TK-এর রিপোর্ট
Android Studio-তে Build failed — "Unresolved reference: TextView",
"Unresolved reference: Context" (ও তার থেকে cascade হয়ে ১২টা এরর),
`DashboardActivity.kt`-এর লাইন ৫৯০-৬৩২।

## সততার সাথে স্বীকার
এটা **আমারই ভুল** — V608-এ (ব্রাঞ্চ-ভিত্তিক ব্রেকডাউন ফিচার) যে কোড যোগ
করেছিলাম, সেখানে `TextView` ও `Context` **bare** (import ছাড়া/fully-
qualify ছাড়া) ব্যবহার করেছিলাম। `DashboardActivity.kt`-এ এই দুটোর কোনো
import নেই (ফাইলটা অন্য জায়গায় সবসময় `android.content.Context...`
পুরো নাম লিখে ব্যবহার করে) — আমার কোড সেই নিয়ম মানেনি।

## কেন গার্ড ধরতে পারেনি
`tk_guard.py`-এর import-checker প্রজেক্টের **নিজের** ক্লাসের জন্য
তৈরি — Android-এর নিজস্ব (`TextView`, `Context` ইত্যাদি) SDK ক্লাসের
bare-ব্যবহার এই checker-এর নজরে পড়ে না। এটা গার্ডের একটা সীমাবদ্ধতা,
আসল Kotlin compiler-এর মতো পুরোপুরি নয়।

## সমাধান
দুই জায়গায় fully-qualify করা হলো — ফাইলের নিজের প্রতিষ্ঠিত রীতি
অনুযায়ী (line 444/447-এ যেভাবে `android.content.Context...` লেখা
হয়েছে, ঠিক সেই একই রকম):
- লাইন ৫৯০: `TextView(...)` → `android.widget.TextView(...)`
- লাইন ৬৩২: `Context.MODE_PRIVATE` → `android.content.Context.MODE_PRIVATE`

⛔ এই দুটো লাইন ছাড়া আর কিছুই বদলায়নি — বাকি সব যুক্তি/ডেটা/ডিজাইন অক্ষত।

## যাচাই (এবার আরও সতর্কভাবে)
পুরো V607/V608-এ যোগ করা কোড হাতে ধরে আবার পড়ে দেখা হয়েছে — আর কোনো
bare Android SDK ক্লাস-ব্যবহার নেই। বাকি ক্লাস (`BranchFilterStore`,
`BriefingRepository`, `FollowUpActivity`) একই package
(`com.tkbiswas.pilesclinic.native`)-এর, তাই import ছাড়াই সঠিক।

| পরীক্ষা | ফল |
|---|---|
| `tk_guard.py --release` | ✅ পাশ |
| `verify_version_json.py` | ✅ V616/6.16 |
| হাতে-ধরে bare-SDK-class পুনরায় স্ক্যান | ✅ আর কিছু পাওয়া যায়নি |

## দুঃখিত
এই ভুলের জন্য — এখন থেকে নতুন Android SDK ক্লাস ব্যবহারের সময় সবসময়
হয় import যোগ করব, নয়তো ফাইলের প্রতিষ্ঠিত রীতি অনুযায়ী fully-qualify
করব, এবং সেটা নিজে থেকে দুবার-চেক করব।
