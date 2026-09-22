# WHATSAPP PERSONAL/BUSINESS CHOOSER — V214

**তারিখ ও সময়:** 31.07.2026, দুপুর ১.৩৩ IST
**Owner নির্দেশ:** TK BISWAS — "যেকোনো বার্তা পাঠানোর সময় কোন WhatsApp (Personal/Business) দিয়ে পাঠানো হবে তা চুস করা যাক"
**Version:** V213 → **V214** (versionCode 213→214, versionName 2.13→2.14) — TK "ফাইল পাঠাবেন" বলায় এই মুহূর্তেই একধাপ বাড়ানো হলো।
**Source Hash (আগের):** PILES_CLINIC_APP_V213_FINAL.zip — MD5 f14a95f8fb6b3342c6c88c3a43da6077

## কী হলো

নতুন শেয়ার্ড ফাইল **`WhatsAppMessageChooser.kt`** — ঠিক প্রজেক্টের আগে থেকে প্রমাণিত `CallChooser.kt`-এর ধাঁচেই (একই পদ্ধতি, নতুন কোনো ডিজাইন-প্যাটার্ন নয়):
- ফোনে **Personal ও Business WhatsApp দুটোই** থাকলে → Android-এর নিজস্ব chooser দেখায় ("Send message with" — WhatsApp / WhatsApp Business), স্টাফ ট্যাপ করে বেছে নেন।
- **শুধু একটা** থাকলে → আগের মতোই সরাসরি সেটাই খোলে (এমনি এমনি বাছাই দেখানো হয় না, অপ্রয়োজনীয় step বাড়েনি)।
- **কোনোটাই না থাকলে** → আগের মতোই সাধারণ চেষ্টা, তাতেও না হলে "WhatsApp is not installed" বার্তা।

এই একটা ফাইল বদলেই **সব জায়গায়** (Patient Message ও RMP/Doctor Message — যেখানেই বার্তা পাঠানো হয়) একই আচরণ এসেছে:
- `PatientMessage.kt` → `sendWhatsApp()` এখন `WhatsAppMessageChooser.send()` ডাকে।
- `DoctorVisitActivity.kt` → `sendDoctorMessage()`-এর WhatsApp বোতাম এখন একই ফাংশন ডাকে।

## যা ছোঁয়া হয়নি
- বার্তার লেখা (`text`/`waText`), URL বানানো (`wa.me/91...?text=...`), পপ-আপের চেহারা, SMS পথ — **কিছুই বদলায়নি**।
- সাধারণ চ্যাট-খোলা (কোনো টেক্সট ছাড়া, শুধু নম্বরে WhatsApp খোলা — যেমন Briefing/Draft List/Follow-up/Global Search-এর 💬 আইকন) **ইচ্ছাকৃতভাবে ছোঁয়া হয়নি** — TK-এর নির্দেশ ছিল "বার্তা পাঠানোর" সময়, এগুলো বার্তা পাঠায় না শুধু চ্যাট খোলে।
- AndroidManifest.xml-এ `com.whatsapp`/`com.whatsapp.w4b` আগে থেকেই `<queries>`-এ ছিল, তাই নতুন কোনো Manifest পরিবর্তন লাগেনি।

## Build যাচাই (ফাইল পাঠানোর আগে বাধ্যতামূলক তালিকা অনুযায়ী)
- ৮টা পরিবর্তিত/নতুন ফাইলেই (PatientMessage.kt, EnquiryActivity.kt, PatientTimelineActivity.kt, DoctorMessage.kt, DoctorVisitActivity.kt, DoctorVisitRepository.kt, FollowUpActivity.kt, WhatsAppMessageChooser.kt) স্ট্রিং+কমেন্ট বাদ দিয়ে ব্র্যাকেট/প্যারেন গোনা — সব পাশ।
- `kotlinx.coroutines.async/launch` fully-qualified প্যাটার্ন (আগের রেড অ্যালার্ট) — কোথাও নেই।
- `Intent.createChooser`/`EXTRA_INITIAL_INTENTS` — প্রজেক্টের নিজস্ব `CallChooser.kt`-এ ইতিমধ্যে প্রমাণিত API-এর হুবহু পুনর্ব্যবহার।
- নতুন ফাইলের প্যাকেজ, import, object/fun গঠন হাতে-হাতে মিলিয়ে দেখা হয়েছে।
- `sendWhatsApp()`-এর signature অপরিবর্তিত (এখনও `branch` প্যারামিটার নেয়, ভিতরে অব্যবহৃত — শুধু warning, error নয়) তাই এর একমাত্র কল-সাইট ভাঙেনি।

## এই সেশনে (আজ, 31.07.2026) মোট যা যা হয়েছে — এক নজরে
1. সকাল ৯.৫০–১০.২৩ — Enquiry WhatsApp বার্তা (ভাষা-বাছাই + লকড টেমপ্লেট)
2. সকাল ১০.৫৪–১১.০৯ — RMP Msg 1 (Intro) FINAL LOCK টেমপ্লেট
3. দুপুর ১২.৩০–১.১৫ — STRICT MESSAGE-ONLY: RMP Msg 2–4 ও Patient Msg 1–10 নতুন লকড টেক্সট
4. দুপুর ১.১৫–১.২৯ — Data-gap ফিক্স: RMP Msg 4 Mode/Reference, Patient Receipt Number
5. দুপুর ১.২৯–১.৩৩ — WhatsApp Personal/Business Chooser (এই কাজ)

বিস্তারিত প্রতিটা ধাপ `00_TK_KAJER_TARIKH_SOMOY_LOG.md`-এ তারিখ-সময় সহ আলাদা আলাদা লেখা আছে।

## Owner Lock Rule
এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না। TK BISWAS-এর অনুমতি ছাড়া এই সেশনের কোনো কাজ (Enquiry/RMP/Patient বার্তা, WhatsApp Chooser) ভবিষ্যতে কোনো AI/ডেভেলপার বদলাতে পারবে না।
