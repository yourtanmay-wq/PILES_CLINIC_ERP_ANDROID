# LOCK NOTE — V633 (24.08.2026)

## এই সেশনে যা হয়েছে (V623 → V633, Android+Web)
- V623 — Change Branch ও Return Fees, ওয়েবে প্রথমবার
- V624 — Blood Test-এর প্রতিটা ক্যাটাগরিতে "Add Optional Test"
- V626 — Anatomy "ফোলান" শুধু মলদ্বারের কাছেই — জায়গা-পাহারা যোগ
- V627 — "History" → "Checkup History", না থাকলে সরাসরি ফর্ম খোলে
- V628 — হিসাবের খাতা থেকে "All Branches" বাদ + "Edit This Day"
- V629 — নতুন "📄 Statement" (ব্যাংক-স্টেটমেন্টের মতো, running balance)
- V630 — "আয়"/"ব্যয়" আলাদা পর্দা বাদ, Sheet-এই সরাসরি Cash/Online এন্ট্রি
- V631 — Anatomy bulge-zone/eraser/ks-nearest — aspect-ratio bug ঠিক
- V632 — RMP-এর নম্বরে কল এলে এখন সঠিকভাবে "🩺 RMP" চেনে (Android-only)
- V633 — কল-শনাক্তকরণ শুধু ৪টা নির্দিষ্ট লগইনেই (Android-only, গোপনীয়তা)

বিস্তারিত: `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর V623–V633 এন্ট্রি দেখুন।

## ফাইনাল / TK-এর অনুমতি ছাড়া বদলানো যাবে না
- সব পুরনো লক করা ডিজাইন — অক্ষত, এই সেশনে ছোঁয়া হয়নি।
- Anatomy bulge-এর আকৃতি/মাপ — অপরিবর্তিত।
- কল-শনাক্তকরণ হোয়াইটলিস্ট (8676002200/8436002200/8514002200/8001080080)
  — TK-এর স্পষ্ট নির্দেশে বসানো, শুধু TK-ই বদলাতে পারবেন।

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি (এই ZIP-এ প্রথমবার)
V626–V633 এখনো Android Studio-তে লাইভ টেস্ট হয়নি।

## যাচাই
- `tk_guard.py --release` — সব মেশিন-চেক পাশ
- `node --check` app.js ও finance.js — পাশ
- ভার্সন: V633 / 6.33 (build.gradle.kts + version.json দুই জায়গাতেই মেলানো)
