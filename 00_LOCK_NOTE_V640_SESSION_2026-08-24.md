# LOCK NOTE — V640 (24.08.2026)

## এই সেশনে যা হয়েছে (V623 → V640, Android+Web)
V623 Change Branch/Return Fees web · V624 Add Optional Test · V626 Anatomy
bulge জায়গা-পাহারা · V627 Checkup History · V628 All Branches বাদ +
Edit This Day · V629 Statement · V630 Sheet-এ সরাসরি এন্ট্রি · V631
Anatomy aspect-ratio bug · V632 RMP কল-শনাক্তকরণ · V633 কল-শনাক্তকরণ
লগইন-হোয়াইটলিস্ট · V634 callCount সিঙ্ক · V635 Chamber branch-leak ·
V636 Timeline নাম-fallback · V637 Call Remark-এ আগের রিমার্কস · V638
বহু-ভিজিট রোগীর Treatment Progress টাই-ব্রেকার · V639 PDF Share
WebView-attachment · V640 Payment তালিকায় সিরিয়াল নম্বর।

বিস্তারিত: `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর V623–V640 এন্ট্রি।

## ফাইনাল / TK-এর অনুমতি ছাড়া বদলানো যাবে না
সব পুরনো লক করা ডিজাইন অক্ষত। Anatomy bulge-এর আকৃতি/মাপ অপরিবর্তিত।
কল-শনাক্তকরণ লগইন-হোয়াইটলিস্ট (8676002200/8436002200/8514002200/
8001080080) শুধু TK-ই বদলাতে পারবেন। সিরিয়াল-নম্বর ডিজাইন ডেমো-প্রুফে
TK-অনুমোদিত (নামের ঠিক আগে, একই লাইনে)।

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি
V626–V640 এখনো Android Studio-তে লাইভ টেস্ট হয়নি।

## যাচাই
`tk_guard.py --release` পাশ · `node --check` app.js/finance.js পাশ ·
ভার্সন V640/6.40 (build.gradle.kts + version.json দুই জায়গাতেই মেলানো)
