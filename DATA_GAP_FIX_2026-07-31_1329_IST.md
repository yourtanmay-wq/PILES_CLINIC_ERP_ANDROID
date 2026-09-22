# DATA-GAP FIX — RMP Msg 4 Mode/Reference · Patient Receipt Number

**তারিখ ও সময়:** 31.07.2026, দুপুর ১.২৯ IST
**Owner Approval:** TK BISWAS — "হ্যাঁ, তবে অন্যান্য কোন কাজ খারাপ করবেন না" (31.07.2026)
**Source:** PILES_CLINIC_APP_V213_FINAL.zip (MD5 f14a95f8fb6b3342c6c88c3a43da6077)

## কী ঠিক হলো

### ১. RMP Msg 4 — Payment Mode ও Transaction/Reference No.
- **Add Referral Income** ফর্মে (RMP → Action → 💰 Referral Income) দুটো নতুন **অতিরিক্ত** ঘর যোগ হলো: Payment Mode (Cash/Online) ও Transaction/Reference No. (Online-এর জন্য) — আগের কোনো ঘর সরানো/বদলানো হয়নি।
- এই দুটো এখন `referralPayments`-এর প্রতিটা এন্ট্রির সঙ্গে সেভ হয় (Supabase JSON কলামের ভিতরে নতুন key — **কোনো নতুন Table/Column তৈরি হয়নি**, তাই এটা Database-এর গঠন বদল নয়)।
- Msg 4 পাঠানোর সময় এখন মিলে যাওয়া Referral Payment এন্ট্রি থেকে Mode ও Reference No. সরাসরি বসে। Cash হলে Reference "Not Applicable" (TK-এর নিয়ম অনুযায়ী)। পুরনো এন্ট্রি (যেগুলোতে Mode/Reference সেভ হওয়ার আগেই তৈরি হয়েছিল) তাদের জন্য আগের ফাঁকা-ঘর (______) প্যাটার্নই থাকে।

### ২. Patient Message 7 — Receipt Number
- "Send Receipt" চাপলে এখন সেই পেশেন্টের সবচেয়ে সাম্প্রতিক Saved payment row-এর নিজস্ব `id` আনা হয় (Supabase থেকে, ছোট্ট এক্সট্রা fetch) এবং Receipt Number হিসেবে বসে।
- কোনো payment row না পাওয়া গেলে লাইনটাই বাদ যায় (আগের আচরণ, Receipt পাঠানো কখনো আটকায় না)।
- `PatientMessage.show()/build()/buildWhatsApp()/block()`-এ নতুন **অপশনাল** `receiptNumber` প্যারামিটার (ডিফল্ট `""`) — তাই বাকি ৪০+ পুরনো কল-সাইট **একটুও বদলায়নি**, শুধু দুটো "Send Receipt" জায়গা (FollowUpActivity.kt, PatientTimelineActivity.kt) নতুন মান পাঠায়।

## যা ছোঁয়া হয়নি (নিরাপত্তা-যাচাই)
- অন্য কোনো Design/Layout/Button/Field/Workflow/Permission/Print-PDF System বদলায়নি।
- Add Referral Income ফর্মের আগের চারটে ঘর (Mobile/Name/Amount/Status) ও তাদের Save-লজিক অক্ষত।
- বাকি ১০টা Patient Message ও RMP Msg 1–3 এই সেশনের আগের কাজ থেকে **এক অক্ষরও বদলায়নি**।
- কোনো নতুন Database Table/Column তৈরি হয়নি — শুধু বিদ্যমান JSON কলামে নতুন key।

## Build Result
- পরিবর্তিত ৭টা ফাইলেই (PatientMessage.kt, EnquiryActivity.kt, PatientTimelineActivity.kt, DoctorMessage.kt, DoctorVisitActivity.kt, DoctorVisitRepository.kt, FollowUpActivity.kt) ব্র্যাকেট/প্যারেন গোনা পাশ।
- `kotlinx.coroutines.async/launch` fully-qualified প্যাটার্ন নেই।
- নতুন async fetch দুটো জায়গাতেই `lifecycleScope.launch { withContext(Dispatchers.IO) { ... } }` — প্রজেক্টের বিদ্যমান প্যাটার্নের হুবহু অনুসরণ, try/catch-এ মোড়া (fetch ব্যর্থ হলেও Receipt পাঠানো আটকায় না)।

## Owner Lock Rule
এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।
