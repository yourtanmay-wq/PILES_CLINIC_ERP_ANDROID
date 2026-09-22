# ENQUIRY WHATSAPP MESSAGE — FINAL OWNER LOCK (Implementation Note)

**Lock Date:** 31-07-2026
**Lock Time:** 09:50 AM IST (owner instruction) · কোড বসানো শুরু: 31-07-2026 বেলা
**Owner:** TK BISWAS
**Status:** FINAL LOCKED REQUIREMENT — code implemented, awaiting TK's photo-proof approval

---

## 1. তারিখ ও সময়
- নির্দেশ পাওয়া: 31.07.2026, সকাল ৯.৫০ (TK-এর আপলোড করা Lock নোট)
- কোড শুরুর অনুমতি: 31.07.2026 (TK: "কোড বসানো শুরু করুন")

## 2. Source Version
- ভিত্তি: **V213** (versionCode 213 · versionName 2.13)
- এই কাজ শেষে ফাইল পাঠানোর সময় TK "ফাইল পাঠান" বললে তবেই versionCode/Name একধাপ বাড়বে (TK-এর 30.07.2026 রাতের স্থায়ী নিয়ম অনুযায়ী)।

## 3. Source ZIP/File Name
- `PILES_CLINIC_APP_V213_FINAL.zip`
- সহায়ক ফাইল: `NETLIFY_UPLOAD_READY_V212.zip` (branch mobile/map/facebook যাচাইয়ের জন্য)

## 4. বর্তমান File Hash
- `PILES_CLINIC_APP_V213_FINAL.zip` → MD5 `f14a95f8fb6b3342c6c88c3a43da6077`

## 5. Owner Approval Status
- ✅ ফাংশনাল রুল ও টেমপ্লেট (বাংলা/হিন্দি/English) — TK-এর আপলোড করা লক নোট থেকে হুবহু
- ✅ কোচবিহারের সঠিক নম্বর (8514002200) — TK নিজে চ্যাটে নিশ্চিত করেছেন
- ✅ পাঁচটা ব্রাঞ্চের Verified Google Map লিংক — TK নিজে চ্যাটে একে একে পাঠিয়েছেন
- ⏳ **চূড়ান্ত ফটো-প্রুফ এখনো TK পাশ করেননি** — এই নোট কোড-অডিট ও পরিবর্তনের তালিকা, চূড়ান্ত অনুমোদন বাকি
- ⚠️ **TK-কে যাচাই করতে অনুরোধ (অনুমান হিসেবে বসানো হয়েছে):**
  - হিন্দি ব্রাঞ্চ-নাম (जलपाईगुड़ी · कूचबिहार · फालाकाटा · बीरपाड़ा · किशनगंज) — প্রচলিত বানানে লেখা, কোনো সরকারি নথি থেকে যাচাই হয়নি
  - "Other" রোগের জন্য Educational Link — নির্দিষ্ট পাতা নেই বলে ওয়েবসাইটের মূল পাতার লিংক বসানো হয়েছে
  - Disease Educational Page লিংকগুলো (`?disease=piles` ইত্যাদি) এখনো ওয়েবসাইট কোডে Route করে টেস্ট করা হয়নি (এই সেশনে নেটওয়ার্ক অ্যাক্সেস নেই) — TK নিজে ফোনে চেপে যাচাই করবেন

## 6. কোন কোন File পরিবর্তন করা হয়েছে
- `PatientMessage.kt` — নতুন লকড Enquiry টেমপ্লেট (bn/hi/en), ভাষা-বাছাই পপ-আপ, `showEnquiryMessage()`; পুরনো `show()`-এর পপ-আপ কোড রিফ্যাক্টর করে `presentSendBox()`-এ (আচরণ অপরিবর্তিত, পুরনো ১২টা ডাক অক্ষত)
- `EnquiryActivity.kt` — নতুন Enquiry সেভের পরে বার্তা এখন `showEnquiryMessage()` দিয়ে যায়
- `PatientTimelineActivity.kt` — View All → Take Action → "Enquiry বার্তা পাঠান"-ও এখন `showEnquiryMessage()` দিয়ে যায়
- ⛔ কোনো Database/Workflow/Button/Field ছোঁয়া হয়নি; অন্য ১১ ধরনের বার্তা (Registration/Bill/Payment/Due Reminder ইত্যাদি) আগের মতোই তিন-ভাষা-একসাথে যায়, একটা অক্ষরও বদলায়নি

## 7. পরিবর্তনের আগে ও পরে Proof
- **আগে:** Enquiry বার্তা একসাথে বাংলা+হিন্দি+English, স্টাফের কোনো ভাষা বাছাই ছিল না, রোগ/ম্যাপ/ফেসবুক লিংক কিছুই ছিল না
- **পরে:** স্টাফ Send করলে প্রথমে প্রফেশনাল ভাষা-বাছাই বাক্স (Bengali/Hindi/English), তারপর বাছাই করা ভাষার লকড টেমপ্লেট (রোগের তালিকা, চেম্বার-দিন, ঠিকানা, ভেরিফায়েড Google Map, Facebook, নির্দিষ্ট রোগের Educational Link) — শুধু WhatsApp-এ যায়
- 📸 ছবি-প্রুফ (তিন ভাষার নমুনা + পাঁচ ব্রাঞ্চের ডাইনামিক প্রুফ) TK-কে পরের বার্তায় দেখানো হবে, Lock নোটের ৭–৮ নং ধাপ অনুযায়ী

## Owner Lock Rule (বহাল)
এই বার্তার লেখা, ফরম্যাট, রোগের নাম, Signature, Link Order, Branch Information Rule বা Language Structure TK BISWAS-এর স্পষ্ট লিখিত অনুমতি ছাড়া কোনোদিন পরিবর্তন করা যাবে না।

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।
