# RMP POST-CALL WHATSAPP INTRO MESSAGE — FINAL IMPLEMENTATION LOCK

**Lock Date:** 31-07-2026 · **Lock Time:** 10:54 AM IST (owner instruction)
**কোড শুরু/শেষ:** 31.07.2026, বেলা ১১.০৯
**Owner:** TK BISWAS · **Approval Status:** APPROVED FOR THIS EXACT SCOPE ONLY — Msg 1 (Intro) মাত্র

---

## 1. Current Source Version
V213 (versionCode 213 · versionName 2.13)

## 2. Source ZIP/File Name
`PILES_CLINIC_APP_V213_FINAL.zip`

## 3. Source File Hash
MD5 `f14a95f8fb6b3342c6c88c3a43da6077`

## 4. কাজ শুরুর তারিখ ও সময়
31.07.2026, বেলা ১১.০৯ (নির্দেশ পাওয়া 10.54 AM)

## 5. পরিবর্তন করা File-এর তালিকা
- `DoctorMessage.kt` — নতুন লকড bn/hi/en Intro টেমপ্লেট (`introLockedTemplate`), `intro()`-এর signature বদলেছে (doctorMobile সরে doctorArea যোগ হয়েছে), `introDoctorNameMissing()` গার্ড যোগ। **Msg 2/3/4 (arrived/details/referralPaid) ও তাদের `head()/foot()/headHi()/footHi()` একটুও ছোঁয়া হয়নি।**
- `DoctorVisitActivity.kt` — নতুন `withIntroLanguage()` (bn/hi/en, সব ব্রাঞ্চ, Doctor Name Required গার্ড) শুধু Msg 1-এর জন্য; পুরনো `withLanguage()` অক্ষত, Msg 2/3/4 এখনও সেটাই ব্যবহার করে (শুধু Kishanganj-এ hi/bn পপ-আপ)।

## 6. পরিবর্তনের আগের বর্তমান RMP Intro Message
- শুধু বাংলা/হিন্দি (Kishanganj-এই ভাষা-বাছাই পপ-আপ উঠত, বাকি ব্রাঞ্চে সরাসরি বাংলা)
- ডাক্তারের নাম/এলাকা বার্তায় ছিল না (নিয়ম ছিল উল্টো — ডাক্তারের নাম বার্তায় লেখা হতোই না)
- রোগের তালিকা, Google Map, Facebook, Website লিংক কিছুই ছিল না
- ডাক্তারের নাম না থাকলে মোবাইল নম্বর fallback হিসেবে ব্যবহার হতো

## 7. Owner Approval Status
✅ এই ডকুমেন্টই (উপরে TK আপলোড করেছেন) একমাত্র অনুমোদন — Msg 1/Intro-এর জন্য।
⚠️ **TK-কে জানানো হলো (কাজ থামানো হয়নি, কারণ প্রতিটির নিরাপদ সমাধান কোডেই পাওয়া গেছে):**
- **Cooch Behar/Birpara নম্বর:** এই ডকুমেন্টে Cooch Behar 8514001100 লেখা আছে, কিন্তু TK নিজে আজই সকালে (Enquiry ফিচারের সময়) নিশ্চিত করেছিলেন সঠিক নম্বর 8514002200। Birpara-এ এই ডকুমেন্টে 7501275078 লেখা, কিন্তু কোডে (BranchInfo.kt) আগে থেকেই TK-লকড 8538002200 আছে (খাতার B86)। **সিদ্ধান্ত:** কোনো ফোন নম্বর হার্ডকোড করা হয়নি — সব সময় `BranchCatalog`-এর (single source of truth) `phoneLine`-ই ব্যবহার হয়েছে, তাই ভুল/পুরনো নম্বর কোথাও যায়নি।
- **Falakata/Birpara Google Map:** এই ডকুমেন্টে "REQUIRED" লেখা ছিল; নোটের নিজের ১১ নং নিয়ম অনুযায়ী কোডে থাকা TK-verified লিংক পুনর্ব্যবহার করা হয়েছে (এই একই লিংক TK আজ সকালে Enquiry ফিচারের জন্য চ্যাটে পাঠিয়েছিলেন)।
- **[ACTIVE_BRANCH]:** RMP রেকর্ডের নিজস্ব `branch` ব্যবহার করা হলো (branch-isolation নিয়মে স্টাফ শুধু নিজের ব্রাঞ্চের RMP দেখেন, তাই এটাই Logged-in Staff-এর Active Branch-এর সমতুল্য)।
- **ভাষা-বাছাই UI:** নতুন Design তৈরি হয়নি — প্রজেক্টের আগে থেকে অনুমোদিত `PremiumAlert` + `AlertDialog.setItems` ধাঁচ পুনর্ব্যবহার হয়েছে, শুধু ২টার বদলে ৩টা অপশন ও সব ব্রাঞ্চে চালু করা হয়েছে।

## 8. পরিবর্তনের পরের Test Result
- তিন ভাষার টেমপ্লেট (bn/hi/en) TK-এর দেওয়া লেখার সঙ্গে Python স্ক্রিপ্টে **অক্ষরে-অক্ষরে মিলিয়ে দেখা হয়েছে (MATCH)** — এক অক্ষরও bদলায়নি।
- কোনো `*` (Asterisk) নেই, শুধু 📍 ও 🔵 — অন্য কোনো Emoji নেই, Heading-এ Emoji নেই।
- Link Order ঠিক আছে: Website → Google Map → Facebook।
- `DoctorMessage.kt`/`DoctorVisitActivity.kt` — ব্র্যাকেট/প্যারেন গোনা পাশ (স্ট্রিং+কমেন্ট বাদে), `kotlinx.coroutines.async/launch` fully-qualified প্যাটার্ন নেই।
- Doctor Name ফাঁকা থাকলে পপ-আপ ওঠার আগেই "Doctor Name Required" Toast দেখিয়ে থেমে যায় (কোনো fallback নেই)।
- Doctor Area ফাঁকা থাকলে শুধু সেই লাইনটা বাদ যায়, কোনো Placeholder পাঠানো হয় না।

## 9. Final Change Log
- 31.07.2026 ১১.০৯ — `DoctorMessage.kt`: নতুন `IntroBranchExtra` ডেটা, `introLockedTemplate()`, `introDoctorNameMissing()`; `intro()` signature বদল।
- 31.07.2026 ১১.০৯ — `DoctorVisitActivity.kt`: নতুন `withIntroLanguage()`; Msg 1 (case 4)-এর কল আপডেট।

## Owner Lock Rule (বহাল)
Bengali/Hindi/English Message, রোগের নাম, Doctor Name/Area নিয়ম, Branch Name, Chamber Days, Address, Contact, Website/Map/Facebook Link, Signature, Line Break, Link Order, Branch Dynamic Logic — TK BISWAS-এর স্পষ্ট লিখিত অনুমতি ছাড়া কোনোদিন পরিবর্তন করা যাবে না।

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।
