# V620 — Registration "Unexpected Time": Enquiry-নির্ভর নিয়ম
**তারিখ:** ২৪.০৮.২০২৬ · **ভার্সন:** V620 / 6.20

## TK-এর নির্দেশ (স্পষ্ট প্রশ্নে নিশ্চিত হয়ে)
"Enquiry-তে Unexpected থাকলে তবেই Registration-এ Unexpected হতে হবে,
অন্যথায় না। Unexpected বোতামটাই লুকিয়ে ফেলব (শুধু Official Time দেখাবে)।"

## প্রেক্ষাপট
TOMIJ UDDIN MIYA-র হিস্টরিতে কোনো "Enquiry created" সারি ছাড়াই সরাসরি
"Registration / Visit" এন্ট্রি ছিল, অথচ "⏰ UNEXPECTED TIME" ট্যাগ
দেখাচ্ছিল। যাচাই করে বোঝা গেল — Registration ফর্মে স্টাফ **স্বাধীনভাবে**
"Unexpected Time" বেছে নিতে পারতেন, কোনো Enquiry না থাকলেও।

## সমাধান (Android + Web দুটোতেই)
### Android — `RegistrationActivity.kt`
- `setupTimingButtons()` — "Unexpected Time" বোতাম এখন **ডিফল্ট লুকানো**
  (`View.GONE`)।
- `autofillFromEnquiry()` — Enquiry পাওয়া গেলে ও তার `timeType` নিজেই
  "Unexpected Time" হলে **তখনই** বোতাম দেখা যায় ও অটো-বাছা হয়।
- **নিজে ধরে ঠিক করা ঝুঁকি:** এক নম্বর থেকে অন্য নম্বরে বদলালে (আগের
  নম্বরের Enquiry Unexpected ছিল, নতুনটায় নেই) বোতাম যেন পুরনো অবস্থায়
  আটকে না থাকে — তাই প্রতিটা নতুন নম্বর লেখার সাথে সাথেই (নেটওয়ার্ক
  ফলাফল আসার আগেই) বোতাম রিসেট করে লুকানো হয়।

### Web — `app.js` (`registration()` ফাংশন)
- একই নিয়ম — `pref.timeType === 'Unexpected Time'` হলে তবেই বোতাম
  দেখাবে ও অটো-বাছা হবে, নইলে শুধু Official Time (hidden ক্লাস,
  আগে থেকেই প্রতিষ্ঠিত CSS প্যাটার্ন পুনর্ব্যবহার)।
- hidden input `pRegTiming`-এর প্রাথমিক মানও সঠিকভাবে বসানো হয়েছে।

## নিরাপত্তা
- Official Time বোতাম/আচরণ এক অক্ষরও বদলায়নি।
- সেভের সময় `timeType` পড়ার নিয়ম (Android/Web দুটোতেই) অপরিবর্তিত —
  শুধু বোতাম কখন দেখা/বাছা যাবে তার নিয়ম বদলেছে।
- ফর্ম-রিসেট (নতুন রোগীর জন্য আবার খোলা) স্বয়ংক্রিয়ভাবে বোতাম আবার
  লুকিয়ে দেয় (Android — `setupTimingButtons()` পুনরায় ডাকা হয়)।

## যাচাই
| পরীক্ষা | ফল |
|---|---|
| `tk_guard.py --release` | ✅ পাশ |
| `node -c app.js` | ✅ JS সিনট্যাক্স ঠিক |
| `verify_version_json.py` | ✅ V620/6.20 |
| ছবি-প্রুফ (৩টা অবস্থা) | ✅ দেখানো ও অনুমোদিত |
