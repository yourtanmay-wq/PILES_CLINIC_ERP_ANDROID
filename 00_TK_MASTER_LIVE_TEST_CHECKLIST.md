# 📋 সম্পূর্ণ প্রজেক্ট — লাইভ টেস্ট মাস্টার-তালিকা
প্রতিটা মডিউল/পাতা ধরে ধরে। যা যা "Pass" বলবেন, সেটার পাশে ✅ বসিয়ে
তারিখ লিখে রাখা হবে (খাতায়) — তারপর সেটা লক থাকবে, আপনার অনুমতি ছাড়া
আর কখনো বদলাবে না।

---
## ১) Login
- [ ] সঠিক person_code + পাসওয়ার্ড দিয়ে লগইন
- [ ] ভুল পাসওয়ার্ডে আটকায়
- [ ] Master/Staff/Doctor/Field Officer — প্রতিটা role ঠিক ড্যাশবোর্ডে যায়

## ২) Dashboard
- [ ] সব টাইল (Enquiry/Follow-up/Registration/Dialer/CHECK-UP/Payment/Print/Chamber Date/Dr. Visit/Draft) খোলে
- [ ] role অনুযায়ী সঠিক টাইল দেখা যায়/লুকায়
- [ ] "📞 X calls pending today" ব্যানার সাথে সাথে দেখায়, চাপলে Follow-up খোলে

## ৩) Enquiry (New Enquiry)
- [ ] নতুন এনকোয়ারি সেভ হয়, ফর্ম সাথে সাথে ফাঁকা হয় (ভাষা-পপ-আপ Cancel/ভাষা-বাছা দুই পথেই)
- [ ] ডুপ্লিকেট নম্বরে "already exists" — Restore/History/Cancel তিনটেই কাজ করে
- [ ] Disease/Address/Remarks/Call Timing/Next Follow-up সব সেভ হয়

## ৪) Registration
- [ ] নতুন রেজিস্ট্রেশন সেভ হয়, ফর্ম ফাঁকা হয়
- [ ] Previous Treatment History চেকবক্স ঠিক সেভ হয়
- [ ] Fee Cash/Online ঠিক সেভ হয়, Add Patient Photo কাজ করে
- [ ] পুরনো রোগীর এডিটে Update Existing ঠিক কাজ করে (ফর্ম বন্ধ হয়ে যায়, এটাই সঠিক)

## ৫) Follow-up (Enquiry/Visit/Patient তিনটে ট্যাব)
- [ ] তিনটে ট্যাবেই সঠিক তালিকা, সংখ্যা মেলে
- [ ] Today/Overdue/This Week ফিল্টার কাজ করে
- [ ] কার্ডে কল/মেসেজ/চোখ/তীর বোতাম কাজ করে
- [ ] রিমার্ক আপডেট সেভ হয়, সব স্ক্রিনে (Timeline/Report Card) একই দেখায়

## ৬) Doctor Check-up
- [ ] ৫টা ধাপই (History/Clinical/Counsel/Estimate/Photo) নতুন বক্স-ডিজাইনে
- [ ] সেভ হয়, নতুন রোগীর জন্য ফর্ম ফাঁকা শুরু হয় (আগের রোগীর তথ্য না আসে)
- [ ] 📜 History বোতামে পুরনো চেকআপ + কে করেছিলেন দেখা যায়

## ৭) Payment
- [ ] Cash/Online পেমেন্ট সেভ হয়
- [ ] একই দিনে দ্বিতীয় পেমেন্টে সতর্কতা আসে
- [ ] Delete/Refund request Master-এর কাছে যায়, বেলে দেখা যায়

## ৮) Patient Timeline / Full Journey
- [ ] সব তথ্য (Enquiry+Visit+Patient+Payment) মিলিয়ে দেখায়
- [ ] সাথে সাথে দেখায় (TimelineCache), টাকার হিসাব ঠিক

## ৯) Chamber Attendance / Chamber Date
- [ ] বক্স→হেডার→তালিকা ক্রম ঠিক
- [ ] স্ক্রল করলে হেডার আটকে থাকে
- [ ] Attendance মার্ক করা যায়, Chamber Close কাজ করে

## ১০) Doctor Visit / RMP / Doctor Queue
- [ ] ALL RMP/PENDING/CALLED/EXPECTED ঠিক গোনে
- [ ] Log Call ফর্ম, Expected Patient Date সেভ হয়
- [ ] Doctor Queue-তে রোগী সঠিক দেখায়, Take Action কাজ করে

## ১১) Work Notebook
- [ ] IN TIME/OUT TIME/Mark as Leave — সব বোতাম কাজ করে, নতুন প্রফেশনাল পপ-আপ
- [ ] ৬টা ঘর গ্রিডে, Today Patient/রিসিভ ফোন টাইপ করা যায়
- [ ] Submit to Master + WhatsApp শেয়ার কাজ করে

## ১২) Dialer
- [ ] All/Missed/Contacts তিনটে ট্যাব ঠিক কাজ করে, জমে যায় না
- [ ] চেম্বার-নম্বর প্রশ্ন ঠিকভাবে আসে (নতুন/এক-সিম ফোনে)
- [ ] কিবোর্ড দিয়ে কল করা যায়, পেস্ট করা যায়

## ১৩) Briefing / Notifications
- [ ] নোটিশ লেখা/পাঠানো যায় (সবাই), Master অনুমোদন করতে পারেন
- [ ] ঘন্টা → Notifications → 📜/➕ ঠিক কাজ করে
- [ ] ১০ মিনিট পরপর রিমাইন্ডার, স্নুজ কাজ করে

## ১৪) Print / Report Card
- [ ] সঠিক ব্রাঞ্চের হেডার-সহ প্রিন্ট হয়
- [ ] Report Card PDF ফাঁকা পাতা ছাড়া তৈরি হয়

## ১৫) Reports
- [ ] Today's Collection নতুন কার্ড-ডিজাইনে, টাকা ঠিক
- [ ] স্টাফ-সামারি সঠিক

## ১৬) Draft
- [ ] Draft তালিকা, Reject List, Delete Enquiry বোতাম কাজ করে

## ১৭) Income-Expense (Master)
- [ ] Add Collection/Add Expense সেভ হয়
- [ ] দিনের/মাসের সারাংশ সঠিক, সাথে সাথে দেখায়
- [ ] Ledger Sheet ঠিক যোগফল করে

## ১৮) Staff Profile / Password Centre
- [ ] স্টাফ তালিকা সাথে সাথে দেখায়, বেতন ঠিক
- [ ] Aadhaar আংশিক সংরক্ষণ, ছবি আপলোড কাজ করে
- [ ] Password Centre-এ পাসওয়ার্ড বদলানো যায়

## ১৯) Trash Bin / Backup
- [ ] মোছা রেকর্ড Trash-এ যায়, ফেরত আনা যায় (শুধু Master)
- [ ] Backup/Export কাজ করে

## ২০) Global Search / Today Call
- [ ] নাম/মোবাইল দিয়ে খোঁজা যায়, সব ব্রাঞ্চে
- [ ] Today Call তালিকা সঠিক

## ২১) NoBengali (Kishanganj স্টাফ)
- [ ] KNE-KISHAN5 দিয়ে লগইন করলে কোথাও বাংলা দেখা যায় না (EditText বাদে)

---
### 🔴 বিশেষভাবে জরুরি (এই সপ্তাহের নতুন কাজ, আগে দেখুন)
- Enquiry/Registration ফর্ম-ফাঁকা (B514)
- Dialer জমে যাওয়া (B489/B509)
- Work Notebook নতুন গ্রিড/পপ-আপ (B508/B510-512)
- সাথে-সাথে-দেখাবে (B500-B505)

---
যা "Pass" বলবেন তা সাথে সাথে খাতায় 🔒 লক করে লেখা হবে। যেটাতে সমস্যা
পাবেন সেটাও তারিখ-সময় সহ লেখা হবে ও ঠিক করা হবে।
