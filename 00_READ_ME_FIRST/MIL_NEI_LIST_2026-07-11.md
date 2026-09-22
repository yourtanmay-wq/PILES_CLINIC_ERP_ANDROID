# পুরনো ওয়েব অ্যাপ ↔ নতুন Android অ্যাপ — মিল-অমিলের লিস্ট
তারিখ: ২০২৬-০৭-১১ · পুরো কোড মিলিয়ে দেখা (আন্দাজে নয়)

---

## ✅ যেগুলো এখন মিলে গেছে (কাজ করছে)
- Menu / সব বোতাম, Enquiry, Registration (ছবি সহ), Payment (Cash/Online,
  Treatment, Advance, Edit), Follow-up-এর তিন ধরনের card, Doctor Queue,
  Doctor Visit (referral income সহ), Appointment, Clinical (Checkup /
  Prescription / Investigation / Diet / History), Reports (conversion,
  branch-wise, staff-wise), Briefing, Search, Draft, Trash, Password Center,
  Medicine Payment, Patient Timeline — সব আছে ও কাজ করার অবস্থায়।

## ✅ আজ যেগুলো ঠিক করে দিলাম
1. **Field Officer login** — আগে ঢুকতে পারত না, এখন পারবে।
2. **Print (Prescription / Diet / Medicine Slip)** — আগে খালি/ভুল রোগীর
   ছাপত। এখন মোবাইল নম্বর দিলে সেই রোগীর সেভ করা তথ্য বের করে ছাপে (ওয়েবের মতো)।
3. **নিজের তৈরি record দেখা** — staff অন্য branch দেখলেও এখন নিজের ঢোকানো
   follow-up দেখতে পাবে।

---

## ❗ এখনো যা মেলে না (বাকি ৩টি)

### ১) রোগীদের Public ওয়েবসাইট
ওয়েবে login-এর আগে রোগীদের জন্য একটা পাতা আসে (branch ঠিকানা, রোগের তথ্য,
যোগাযোগ, appointment)। নতুন Android অ্যাপ সরাসরি Login-এ যায় — এই রোগী-পাতা
নেই। (অ্যাপটা শুধু স্টাফদের, তাই এটা লাগবে কিনা সেটা আলাদা ব্যাপার — এটা একটা
বড় নতুন অংশ, ভালোভাবে বানাতে হবে।)

### ২) ইন্টারনেট ছাড়া কাজ (Offline)
অনেক পাতা (Doctor Queue, Briefing, Calendar, Draft, Reports, Trash, Password,
Photo, Clinical সেভ) ইন্টারনেট ছাড়া কাজ করে না — খালি দেখায় বা "connection"
বলে। মূল Enquiry/Registration/Payment/Follow-up-ও কার্যত ইন্টারনেট চায়।
ওয়েবেও অনেকটা এমনই, কিন্তু পুরোপুরি offline করতে ৯টা পাতায় বড় কাজ লাগবে।

### ৩) Backup সম্পূর্ণ নয়
Android backup মূল তথ্য (Enquiry/Registration/Payment/Follow-up) রাখে, কিন্তু
ওয়েবের মতো *সব* (Doctor Checkup/Prescription, Doctor Visit, Briefing) রাখে না।
আর এটা ফোনের ভেতরের ফাইল — ওয়েবের মতো download করা backup ফাইল নয়।

---

## কাজের নিরাপদ ক্রম (দায়িত্ব নিয়ে)
- বাকি ৩টির মধ্যে **Offline** আর **Backup** ঠিকভাবে ও নিরাপদে করতে হলে আগে
  অ্যাপটা একবার ফোনে চালু (build) হওয়া দরকার — নাহলে এখন-চালু-থাকা পাতাগুলো
  ভেঙে যাওয়ার আসল ঝুঁকি, আর কোথায় ভাঙল বোঝাও যাবে না। তাই এগুলো build সফল
  হওয়ার পর ধাপে ধাপে করা হবে।
- **Public ওয়েবসাইট** একটা বড় আলাদা অংশ; আলাদা করে বানানো হবে (পুরনো কিছু
  ভাঙবে না)।
