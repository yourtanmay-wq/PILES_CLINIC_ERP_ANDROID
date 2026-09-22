# LOCK NOTE — V649 (25.08.2026)

## এই সেশনে যা হয়েছে (V626 → V649, Android+Web)
V626-V646: (আগের LOCK NOTE-এ বিস্তারিত) — Anatomy fix, RMP call-ID,
Statement, Sheet-entry, Chamber branch-leak, বহু-ভিজিট টাই-ব্রেকার,
Payment সিরিয়াল-নম্বর, কিশানগঞ্জ বাংলা-ফাঁক, Draft/Follow-up কার্ড
সম্পূর্ণ একীকরণ (My Enquiry/Unexpected Time/Incomplete/Complete/
Running Patient — Follow-up-এর আসল কার্ড ব্যবহার করে), Supabase
Egress কমাতে Draft-এ ২০-সেকেন্ড ক্যাশ।

V648-V649: TK-এর ছবি-তুলনা করা রিপোর্টে ধরা পড়া ভুল ঠিক করা —
Android: navy-blue বোতাম (থিমের colorPrimary-র ওভাররাইড, Button→
TextView করে ঠিক), সিরিয়াল-নম্বর ব্যাজ যোগ, LAST CALL/NEXT CALL
লাইন যোগ (আগে ছিলই না), ছবি-বাক্স সরানো (আসল কার্ডে নেই)। Web:
সিরিয়াল-নম্বর CSS-counter #followRows id-নির্ভরতার কারণে দেখাতই
না — এখন ঠিক করা।

বিস্তারিত: `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর V626–V649 এন্ট্রি।

## ফাইনাল / TK-এর অনুমতি ছাড়া বদলানো যাবে না
সব পুরনো লক করা ডিজাইন অক্ষত। FollowUpActivity.kt-এর `buildFollowCard`
(Android) ও ওয়েবের `fuCard()` — কোনোটাতেই এই সেশনে হাত পড়েনি (Draft
আলাদা/নতুন-খোঁজা-বাগ-ঠিক-করা `FollowUpAdapter` পুনর্ব্যবহার করে,
FollowUpActivity নিজে অক্ষত)। Draft-এর নতুন কার্ডে ট্রিপল-ট্যাপ এডিট
(Status Menu/Call Signal/Edit/Photo) Android-এ কাজ করে না — ঝুঁকি না
নেওয়ার সিদ্ধান্ত (TK-নির্দেশ), Web-এ সবই কাজ করে।

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি
V626–V649 এখনো Android Studio-তে লাইভ টেস্ট হয়নি।

## যাচাই
`tk_guard.py --release` পাশ · `node --check` app.js/finance.js পাশ ·
ভার্সন V649/6.49 (build.gradle.kts + version.json দুই জায়গাতেই মেলানো)
