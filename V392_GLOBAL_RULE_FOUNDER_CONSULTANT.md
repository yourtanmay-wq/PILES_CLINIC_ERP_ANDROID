# V392 — গ্লোবাল রুল: **TK BISWAS / Founder & Consultant**

তারিখ: ১৬.০৮.২০২৬ · আদেশ: **"এটাই গ্লোবাল রুলস তৈরি করুন, সম্পূর্ণ প্রজেক্ট
খুলে দেখুন কোথায় কোথায় এটা নেই, এখানে সেখানে এটা বসিয়ে দিন।"**

## ১. পুরো প্রজেক্ট স্ক্যান করে যা পাওয়া গিয়েছিল

স্ক্রিপ্ট দিয়ে `02_ANDROID_SOURCE_CODE` ও `03_NETLIFY_READY`-র **সব**
`.kt/.java/.xml/.js/.html/.css` ফাইল খোঁজা হয়েছে। **তিন রকম** লেখা ছিল:

| রূপ | কতগুলো | কোথায় |
|---|---|---|
| `Founder & Consultant` ✅ | ১৮ | PatientMessage.kt · DoctorMessage.kt (WhatsApp/SMS বার্তা) |
| `founder/consultant` ❌ | ৮ | ClinicPdfBuilder.kt (ফোনের ছাপা কাগজের ফুটার) · পাবলিক সাইটের হেডার · Check-up A4 · WhatsApp/SMS সই · ডকুমেন্ট ফুটার |
| `Founder / Consultant` ❌ | ২ | ওয়েবের Prescription ও কিশানগঞ্জ প্রিন্ট |

## ২. যা করা হলো — ১১ জায়গা

**১০ জায়গায় লেখা মিলিয়ে দেওয়া** (সব → `Founder & Consultant`):

| # | কোথায় | আগে |
|---|---|---|
| ১ | `ClinicPdfBuilder.kt:677` — ফোনের ছাপা কাগজের ফুটার | `founder/consultant` |
| ২ | `app.js` — পাবলিক ওয়েবসাইটের হেডার | `founder/consultant` |
| ৩ | `app.js` — Check-up A4-এর সই | `founder/consultant` |
| ৪–৭ | `app.js` — WhatsApp/SMS-এর ৪টি সই | `founder/consultant` |
| ৮–৯ | `app.js` — Prescription ও কিশানগঞ্জ প্রিন্ট | `Founder / Consultant` |
| ১০ | `app.js` — কোডের মন্তব্য | `founder/consultant` |

**১১ নম্বর — যেখানে লাইনটাই একেবারে ছিল না, সেখানে বসানো হলো:**
`printDoctorsHtml()`-এর শেষ পথে (কিশানগঞ্জ ছাড়া অন্য ব্রাঞ্চের
Prescription-বহির্ভূত প্রিন্ট — Medicine Slip · Blood Test · Payment Receipt)
**দুই পাশই ফাঁকা** থাকত, TK BISWAS-এর লাইনটাই ছাপা হত না। এখন বাঁ-পাশে বসে।

## ৩. চূড়ান্ত যাচাই (স্ক্রিপ্টে)

* সঠিক রূপ `Founder & Consultant` — **৩১ জায়গায়**
* ভুল রূপ (`/`, `-`, `and`, ছোট হাতের) — **একটিও নেই ✅**
* SQL-এ **০** বদল · `node --check app.js` পাশ
* Diet Chart এখনো এক পাতায়: জলপাইগুড়ি ২৯২.৫mm · কিশানগঞ্জ ২৮৮.৫mm (A4 = ২৯৭)

## ৪. ⚠️ যা জানিয়ে রাখা দরকার

1. **`ClinicPdfBuilder.kt` এবার ছুঁতে হয়েছে** — ওটা "OWNER LOCKED" লেখা ফাইল,
   কিন্তু ফোনের ছাপা কাগজের ফুটারের ঐ লেখাটা ওখানেই আছে, আর আপনি "সম্পূর্ণ
   প্রজেক্টে" বসাতে বলেছেন। **শুধু ঐ একটা লেখা** বদলেছে — মাপ, জায়গা, রং,
   অন্য কোনো প্রিন্ট কিছুই ছোঁয়া হয়নি।
2. **ডান পাশের ডাক্তারের সই আন্দাজে বসাইনি।** কোন কাগজে কোন ডাক্তারের নাম
   বসবে সেটা আপনার সিদ্ধান্ত — বললে বসিয়ে দেব।
3. **এখনো দুটো অমিল বাকি (আপনার নির্দেশের অপেক্ষায়):**
   · ডাক্তারের নাম — `Dr. K.H. MANDAL` (config.js, StaffDirectory.kt) বনাম
     `Dr. KH MANDAL` (ClinicPdfBuilder.kt:680)
   · রেজিস্ট্রেশন — `B.A.M.S (Regd. No. 12386)` বনাম `(B.A.M.S) Regd 12386`
   **বলুন কোনটা রাখব — সব জায়গায় এক করে দেব।**

## ৫. আপনার কাজ

* ওয়েব: Netlify-তে তুলুন (`?v=v419`)
* **অ্যান্ড্রয়েড: নতুন APK বিল্ড লাগবে** (৩টি Kotlin ফাইল বদলেছে)

---

# V393 — গ্লোবাল রুল ২: **Dr. K.H MANDAL / (B.A.M.S) Regd 12386**

TK-নির্দেশ: **"এটাও গ্লোবাল রুলস, সমস্ত জায়গায় এখনই বসিয়ে দিন।"**

## আগে যা ছিল (৩ রকম)

| রূপ | কোথায় |
|---|---|
| `Dr. K.H. MANDAL` (H-এর পরে ফুলস্টপ) | StaffDirectory.kt · config.js · DietChartHtmlPrint.kt · app.js |
| `Dr. KH MANDAL` (ফুলস্টপ ছাড়া) | ClinicPdfBuilder.kt (ফোনের ছাপা কাগজ) |
| `B.A.M.S (Regd. No. 12386)` / `B.A.M.S.` + `Regd. No. 12386` | ClinicPdfBuilder.kt · ওয়েবের Prescription |

## এখন — ১০ জায়গায় বসানো হলো

| ফাইল | কতগুলো |
|---|---|
| `native/StaffDirectory.kt` (ডাক্তারের তালিকা) | ১ |
| `print/ClinicPdfBuilder.kt` (ছাপা কাগজের ফুটার — নাম + যোগ্যতা) | ২ |
| `print/DietChartHtmlPrint.kt` | ২ |
| `config.js` (ডাক্তারের তালিকা) | ১ |
| `app.js` (Prescription প্রিন্ট + Diet Chart) | ৪ |

## যাচাই (স্ক্রিপ্টে)

* `Dr. K.H MANDAL` — **৮ জায়গায়** ✅
* `(B.A.M.S) Regd 12386` — **৪ জায়গায়** ✅
* পুরনো কোনো রূপ — **একটিও নেই** ✅
* `node --check` — app.js ও config.js দুটোই পাশ
* Diet Chart এখনো এক পাতায় (২৯২.৫ / ২৮৮.৫ মিমি)

## ⚠️ দুটো কথা

1. **নাম দিয়ে কোথাও মিলানো হয় না** — যাচাই করে দেখেছি, লগইন ও ডাক্তার-শনাক্তি
   সবই **মোবাইল নম্বর** ধরে হয় (`StaffAccount(mobile, name, …)` ও
   `config.js users.doctor`)। তাই নাম বদলে কিছু ভাঙে না।
2. **আগে সেভ হওয়া পুরনো সারিতে** (Supabase-এ `createdBy`/`receivedBy` ইত্যাদি)
   পুরনো বানানটাই লেখা থাকবে — ওগুলো ইতিহাস, নতুন কিছু ভাঙে না।

**ওয়েব:** `?v=v420` (app.js ও config.js দুটোই) · **অ্যান্ড্রয়েড: নতুন APK লাগবে**
