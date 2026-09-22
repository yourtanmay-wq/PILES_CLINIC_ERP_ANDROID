# LOCK NOTE — V650 (25.08.2026)

## এই সেশনে যা হয়েছে (V626 → V650, Android+Web)
V626-V649: (আগের LOCK NOTE-এ বিস্তারিত) — Anatomy fix, RMP call-ID,
Statement, Sheet-entry, Chamber branch-leak, বহু-ভিজিট টাই-ব্রেকার,
কিশানগঞ্জ বাংলা-ফাঁক, Draft/Follow-up কার্ড সম্পূর্ণ একীকরণ (My
Enquiry/Unexpected Time/Incomplete/Complete/Running Patient),
Supabase Egress কমাতে Draft-এ ২০-সেকেন্ড ক্যাশ, navy-blue/সিরিয়াল-
নম্বর/LAST CALL/ছবি-বাক্স ঠিক করা (Android+Web দুই জায়গাতেই)।

V650: নির্দিষ্ট এক জলপাইগুড়ি স্টাফের (+918167096595, RMP field-visit)
জন্য IN TIME-এ GPS-ছাড় — শুধু আঙুলের ছাপ/পাসওয়ার্ড, শুধু এই নম্বরের
জন্যই। বাকি সব স্টাফের GPS-পাহারা অপরিবর্তিত।

বিস্তারিত: `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর V626–V650 এন্ট্রি।

## ফাইনাল / TK-এর অনুমতি ছাড়া বদলানো যাবে না
সব পুরনো লক করা ডিজাইন অক্ষত। FollowUpActivity.kt-এর `buildFollowCard`
ও ওয়েবের `fuCard()` — কোনোটাতেই হাত পড়েনি। `gpsExemptMobiles`
allowlist (WorkNotebookActivity.kt) — শুধু TK-নির্দেশে যোগ/বাড়ানো
যাবে, কখনো নিজে থেকে না।

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি
V626–V650 এখনো Android Studio-তে লাইভ টেস্ট হয়নি।

## যাচাই
`tk_guard.py --release` পাশ · `node --check` app.js/finance.js পাশ ·
ভার্সন V650/6.50 (build.gradle.kts + version.json দুই জায়গাতেই মেলানো)
