# 📘 পুরো প্রজেক্টের কাজের নিয়ম ও লজিক — এক জায়গায় (A to Z)

**কী এই ফাইল:** অ্যাপে কোন জিনিস কীভাবে কাজ করে, কোথায় কী নিয়ম বসানো আছে — তার সম্পূর্ণ তালিকা।
**কেন:** ভবিষ্যতে যে কেউ (TK, স্টাফ, নতুন ডেভেলপার বা AI) এই একটা ফাইল পড়েই পুরো ব্যবস্থাটা বুঝতে পারবে।
**তারিখ:** 26.07.2026 · **ভার্সন:** V134

---

## ১) কে কী দেখতে ও করতে পারে (Role)

| Role | কী দেখে | বিশেষ ক্ষমতা |
|---|---|---|
| **Master Admin** (TK) | সব ব্রাঞ্চ | সব কিছু — Reports, Backup, Export, Password Centre, Trash Bin, পুরনো পেমেন্ট সংশোধন, অনুমোদন |
| **Staff** | শুধু নিজের ব্রাঞ্চ | রোজকার সব কাজ (Enquiry, Registration, Payment, Chamber, Follow-up) |
| **Doctor** | Staff-এর সমান | Doctor Queue ও Check-up |
| **Field Officer** | Staff-এর সমান | Dr. Visit (RMP) |

**ব্রাঞ্চের নিয়ম:** স্টাফ শুধু নিজের ব্রাঞ্চের রোগী দেখে। **একমাত্র ব্যতিক্রম — Global Search**, ওখানে ইচ্ছে করেই সব ব্রাঞ্চে খোঁজা যায়।

**🔒 "এক নম্বরে সব কল" (TK, 27.07.2026 — এটা আগে কোথাও লেখা ছিল না, তাই যোগ করা হলো):**
বিজ্ঞাপনে (ফেসবুক অ্যাড, লোকাল পোস্টার) **একটাই নম্বর** দেওয়া থাকে — সুপারফোন। ওই নম্বরে আসা কল **যে কোনো ব্রাঞ্চের যে কোনো স্টাফ** ধরতে পারেন।
- কল ধরে স্টাফ Enquiry ফর্মে **রোগী যে ব্রাঞ্চে আসতে চান সেই ব্রাঞ্চ** বেছে দেন।
- তখন নম্বরটা **ওই ব্রাঞ্চের** — ওই ব্রাঞ্চের সব স্টাফ · ডাক্তার · মাস্টার দেখবেন, এবং **৫ বারের ফলো-আপ কল ওই ব্রাঞ্চের স্টাফরাই করবেন**।
- **যিনি ফর্ম ভরেছিলেন** তিনি নম্বরটা দেখবেন **নিজের Draft → "My Enquiry (All Branch)"**-এ (কল করা তাঁর ইচ্ছা)।
- **রেজিস্ট্রেশনে:** শুধু **সিলেক্ট করা ব্রাঞ্চই** দেখবে; ফর্ম যিনি ভরেছেন তাঁর Draft-এ যাবে না।
- **মাস্টার:** ব্রাঞ্চ বেছে অথবা All Branch — **যা বেছেছেন ঠিক তাই**।
**ব্রাঞ্চ:** Kishanganj (KNE) · Jalpaiguri (JPE) · Cooch Behar (COB) · Falakata (FLK) · Birpara (BIR)।

---

## ২) রোগীর পুরো যাত্রা (মূল ওয়ার্কফ্লো)

```
ফোন এল → 📝 Enquiry → 🔁 Follow-up (কল) → 🧾 Registration (ID তৈরি + ফি)
      → 🩺 Doctor Check-up → 💰 Advance/Treatment Payment
      → 📋 Chamber Attendance (এসেছেন/আসার কথা) → 🖨️ Print / Share
      → 🔁 Follow-up (পরের কল/ভিজিট) → 📊 Report Card
```

### ধাপে ধাপে

**ক) Enquiry (📝)**
- মোবাইল ১০ ডিজিট হলেই **সঙ্গে সঙ্গে ডুপ্লিকেট পপ-আপ** (View / Close)।
- Save-এর সময় ডুপ্লিকেট ধরা পড়লে **নতুন রেকর্ড হবে না** → পুরনো রেকর্ডই সরে আসে (restoreAndMove): status = Active, history অক্ষত, পেমেন্ট ছোঁয়া হয় না।
- বাধ্যতামূলক: Mobile, Disease, Remarks, Next Follow, Branch। ফাঁকা থাকলে ওই ঘরে স্ক্রল করে দেখায়।
- "Call Received By" = সব ব্রাঞ্চের স্টাফ + Admin; ৩-ট্যাপে বদলানো যায়।
- ৫ বার কল হয়ে গেলে সিদ্ধান্ত চাওয়া হয় (Continue / Reject)।

**খ) Follow-up (🔁)** — তিনটে ট্যাব: **Enquiry · Visit · Patient**
- কার্ডে: নাম, মোবাইল, ব্রাঞ্চ, রোগ, পরের কলের তারিখ, শেষ Remark (বক্সটা বাঁ প্রান্ত থেকে ডান প্রান্ত পর্যন্ত চওড়া)।
- ফিল্টার: All / Today / Overdue / This Week / This Month।
- ৩-ট্যাপ: signal/ছবিতে → Continue/Reject; Patient কার্ডে → Continue/Incomplete (Incomplete গেলে Draft-এ যায়)।
- Visit Advance দিয়ে বিল+advance সেভ করলে রোগী **অটোমেটিক Patient (Treatment) সেকশনে** চলে যায়।
- Master চাইলে ব্রাঞ্চ বেছে দেখতে পারেন (ব্রাঞ্চ পিল)।

**গ) Registration (🧾)**
- সেকশন: Present Details (Occupation এখানে) · Address · Disease · Symptoms (+Complaint) · Previous Treatment History · RMP/Ref By · Registration Timing · Fees · Patient Photo।
- **Patient ID** = ব্রাঞ্চ কোড + তারিখ + সিরিয়াল (যেমন `COB-26072026-001`)।
  সিরিয়াল হিসাব হয় **ক্লাউড ও ফোনের হিসাব — যেটা বড়** সেটার পরেরটা; দেওয়ার আগে ক্লাউডে ওই ID আছে কিনা দেখে নেয় (সর্বোচ্চ ২০ বার)। তাই নেট ছাড়াও ০০১, ০০২, ০০৩ ঠিকঠাক হয়।
- ডুপ্লিকেট নম্বর হলে → **Update Existing**, নতুন রোগী নয়।
- Ref By = "Dr. Visit" হলে তবেই ডাক্তার/RMP-র নাম ও মোবাইলের ঘর খোলে।
- Branch ৩-ট্যাপে লক খোলে (ভুলে বদলানো আটকাতে)।

**ঘ) Doctor Check-up (🩺)** — ৭ ধাপ
1. Basic History (Chief Complaint, Duration, Occupation)
2. Previous Treatment History (কী নিয়েছেন, ফল, খরচ, কতদিন)
3. Clinical Findings — A. Visual · B. DRE · C. Proctoscopy Grade · D. Investigations
4. Counselling & Advice
5. Financial Discussion (আনুমানিক খরচ, সেরে ওঠার সময়, Advance আলোচনা)
6. Patient Decision
7. Media & Documents (Before / During / After ছবি, রিপোর্টের নোট)
- পাশে Quick Actions: Prescription · Medicine Slip · Blood Test · Diet Chart।
- যেকোনো সিদ্ধান্তেই Save চলে; Registration থেকে তথ্য নিজে থেকে বসে যায়।

**ঙ) Payment (💰)**
- ধরন: **Registration/Visit Fee**, **Advance**, **2nd/3rd… Payment**, **Medicine Payment**।
- মোড শুধু **CASH / ONLINE**।
- বিল একবার বসলে **bill-only correction** আলাদা করে রাখা হয় (অডিট থাকে) — মূল পেমেন্ট সারি নষ্ট হয় না।
- **সংশোধনের সময়সীমা:** স্টাফ শুধু **আজ বা গতকালের** এন্ট্রি ঠিক করতে পারেন; তার চেয়ে পুরনো হলে **Master-এর অনুমোদন** লাগে (Dashboard/Briefing-এ অনুরোধ যায়, Master Approve/Reject করেন)।
- Backdate (পুরনো তারিখে পেমেন্ট) — অনুমোদনের ব্যবস্থা আছে।
- এক জায়গায় নেওয়া টাকা **সব স্ক্রিনে** দেখাবে: Chamber, Follow-up, Timeline, Report Card।

**চ) Chamber Attendance (📋)**
- কলাম: PATIENT · TREATMENT PROGRESS · FEES · CASH · ONLINE।
- **আসার কথা (Expected):** একজনের জন্য একটাই সারি (`exp_<শেষ ১০ ডিজিট>`), তারিখ বদলালে সেটাই সরে যায়। বাতিল করলে লোকাল, কিউ ও ক্লাউড — তিন জায়গা থেকেই যায়।
- **এসেছেন (Arrived):** টাকা নিলে বা Mark Arrived করলেই।
- **Close Chamber (Save & Print Arrived):**
  Treatment ঘর ফাঁকা থাকলে → ৩ বার চাপার সতর্কতা → **Master "All Branch"-এ থাকলে ব্রাঞ্চ বেছে নিতে হবে** → REVIEW (ভুল থাকলে ৩-ট্যাপে ঠিক) → **Confirm & Print** → রেজিস্টার (SL · PATIENT · TREATMENT PROGRESS · VISIT · CASH · ONLINE · TOTAL) → **SAVE / SHARE / PRINT**।
- VISIT ঘরে: যেদিন ফি নেওয়া হয় সেদিন "CASH ₹500", নইলে "৩rd Visit" (TK-র সিদ্ধান্ত, ২২.০৭.২০২৬)।

**ছ) Print / Share (🖨️)**
- সব কাগজ একই পর্দায় যায়: **SAVE PDF · SHARE PDF · PRINT**।
- কাগজের তালিকা: Prescription · Medicine Slip · Diet Chart · Investigation · Registration · Payment রসিদ · Blood Test · Dr. Visit · Chamber Register · **Report Card**।
- হেডার ব্রাঞ্চ অনুযায়ী: কিশনগঞ্জ = **TK BISWAS PILES CLINIC**, বাকি সব = **MAA AYURVED PILES CLINIC**। ডাক্তার: Dr. K.H. Mandal, Regd 12386।

**জ) Report Card (📊)** — নাম, ID, বয়স/লিঙ্গ, ঠিকানা, রোগ + **TOTAL BILL / PAID / DUE** + টেবিল **VISIT · DATE · PROGRESS · PAID · DUE**।

**ঝ) Patient Timeline / Full Journey (🧭)** — **Date/Time · Type / By · Note**; সকাল ৯টার আগে বা সন্ধ্যা ৬টার পরে করা কলের সময় **লাল**; "By:" ঘরে সবসময় স্টাফের **নাম** (মোবাইল নয়)।

**ঞ) Draft (📂)** — My Enquiry · Enquiry Reject List · Visit Reject List · Incomplete Patient; ফিল্টার All / This Month / Custom Date।

**ট) Dr. Visit / RMP (👨‍⚕️)** — ডাক্তারের কার্ড, কল টাইমলাইন, **কতজন রোগী পাঠিয়েছেন ও কত আয় (Paid/Due)**, রেফারেল আয় এন্ট্রি; ডিলিট করতে Admin-এর অনুমোদন।

**ঠ) Trash Bin (🗑️)** — Master-only। **♻️ Restore** ও **🗑️ Delete Forever**। ডিলিট করা রেকর্ড আগে Trash-এ যায়, সেখান থেকে ফেরানো যায়।

---

## ৩) পর্দার আড়ালের ব্যবস্থা (সবচেয়ে জরুরি লজিক)

**ক) আগে ফোনে, পরে ক্লাউডে (offline-first)**
প্রতিটা সেভ **প্রথমে ফোনে** জমা হয় (স্টাফকে নেটের জন্য অপেক্ষা করতে হয় না), তারপর ক্লাউডে (Supabase) যায়।
ক্লাউডে যেতে ব্যর্থ হলে **অপেক্ষমাণ তালিকায় (queue)** জমা থাকে। মোট **৯টা তালিকা**: Registration · Payment · Enquiry · Follow-up (২টা) · Chamber · Prescription/Medical · Briefing · সংশোধন (Generic)।
এগুলো আবার পাঠানোর চেষ্টা হয় — **যেকোনো স্ক্রিন খুললেই**, আর অ্যাপ বন্ধ থাকলেও **প্রতি ১৫ মিনিটে ও নেট ফিরলেই** (background worker)।

**খ) সতর্কবাতি (নতুন, V134)** — কিছু আটকে থাকলে Dashboard-এ **লাল বার**: "৩টি তথ্য এখনো ক্লাউডে যায়নি (১ রেজিস্ট্রেশন, ২ পেমেন্ট)" + **পাঠান** বোতাম। ন'টা তালিকাই গোনা হয়। কিছু বাকি না থাকলে বার দেখায় না।

**গ) Delete-guard (নতুন, V134)** — ডিলিট করলেই ওই সারির id একটা খাতায় ওঠে; কোনো তালিকা পাঠানোর আগে খাতা মিলিয়ে দেখে, নাম থাকলে বাদ দেয় → **মোছা রেকর্ড আর ফিরে আসে না**। Trash থেকে Restore করলে নাম কেটে যায়। "আসার কথা"-র মতো পুনর্ব্যবহারযোগ্য id নতুন করে দিলে নামও কেটে যায়।

**ঘ) মোবাইল নম্বর বদলালে (নতুন, V134)** — followups / patients / enquiries-এর সাথে **payments ও doctor_visits**-ও নতুন নম্বরে সরে। পরিবারে এক নম্বর হলে শুধু ওই রোগীর নিজের রেকর্ড সরে। Prescription/Investigation/Diet Patient ID ধরে চলে, তাই ওগুলো কখনো হারায় না।

**ঙ) followups টেবিলে আপডেটের নিয়ম** — Supabase-এ ভুল id-তে আপডেট করলেও "সফল" বলে ফেরত আসে। তাই সবসময় **আসল সারি খুঁজে (resolveFollowUpId / মোবাইল+stage)** আপডেট করা হয়।

**চ) ব্রাঞ্চ যাচাই** — রেকর্ডের branch ঘর **অথবা** Patient ID-র ব্রাঞ্চ কোড — যেকোনো একটা মিললেই সেই ব্রাঞ্চ দেখতে পায় (branch ঘর ফাঁকা/ভুল থাকলেও রোগী হারায় না, তৃতীয় ব্রাঞ্চ দেখতে পায় না)।

**গ্লোবাল নিয়ম (সব স্ক্রিনে)**
- তারিখ সবসময় ডট-এ: **31.12.2026** / **31.12.2026 5.40 pm**। স্ল্যাশ বা ISO নয়।
- টাইপ করা ইংরেজি লেখা **CAPITAL**-এ (পাসওয়ার্ড বাদে)।
- পপ-আপ/ডায়ালগ/টোস্ট **ইংরেজিতে**।
- "By:" ঘরে সবসময় স্টাফের **নাম**।
- বাধ্যতামূলক ঘর ফাঁকা রেখে Save চাপলে ওই ঘরে স্ক্রল হয়।
- ফর্মে Branch ৩-ট্যাপে খোলে।
- ছবি ৬০০px-এ ছোট করে রাখা হয় (~৭২ KB), যাতে অ্যাপ ভারী না হয়।

---

## ৪) কম্পিউটার/ট্যাবলেটের ওয়েব অ্যাপ

- একই Supabase ডেটাবেস — যেটাতেই কাজ করুন, হিসাব এক থাকে।
- ছয়টা মূল পর্দা নেটিভের সাথে মিলিয়ে দেওয়া: Registration · Chamber Attendance (Close Chamber সহ) · Payment · Global Search · Full Journey · Report Card।
- **📤 Share** বোতাম — ট্যাবলেটে WhatsApp-এ সরাসরি পাঠানো যায়।
- বড় পর্দায় লেআউট চওড়া হয় (৯০০px+ = ৯২০px, ১২৮০px+ = ১১২০px), টাইল পাশাপাশি ৪/৬টা। **ফোনে কিছুই বদলায় না।**
- **ফোনে আছে, ওয়েবে নেই:** অফলাইনে কাজ, সতর্কবাতি, Delete-guard, মোবাইল-বদল সিঙ্ক — এগুলো ফোনের নিজস্ব ব্যবস্থার উপর দাঁড়ানো।

---

## ৫) কাজের ধারা (সংক্ষেপে, একটা দিনে)

1. সকালে অ্যাপ খুলুন → Dashboard-এ লাল বার আছে কিনা দেখুন (থাকলে "পাঠান")।
2. ফোন এলে → Enquiry। আজ যারা আসবে → Chamber-এ **Mark Expected**।
3. রোগী এলে → Registration (ফি) অথবা Mark Arrived।
4. ডাক্তার দেখলে → Check-up → Prescription/Slip প্রিন্ট বা শেয়ার।
5. টাকা নিলে → Payment (Advance/2nd/3rd)।
6. দিনের শেষে → **Close Chamber** → REVIEW → Confirm & Print → রেজিস্টার Save/Share/Print।
7. Follow-up ট্যাবে আজকের কল সেরে Remark লিখুন।

---

**🔒 LOCK NOTE:** এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না, অ্যাপ স্লো করা যাবে না।
