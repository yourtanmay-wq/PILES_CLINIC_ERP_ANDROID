# LOCK NOTE — V628 (24.08.2026)

## এই সেশনে যা হয়েছে (V623 → V628, Android+Web)
- **V623** — Change Branch ও Return Fees, ওয়েবে প্রথমবার (Android আগে থেকেই ছিল)
- **V624** — Blood Test-এর প্রতিটা ক্যাটাগরিতে "Add Optional Test" (Android+Web)
- **V626** — "ফোলান" (Anatomy bulge tool) শুধু মলদ্বারের কাছেই কাজ করবে — জায়গা-পাহারা যোগ, আকৃতি/মাপ অপরিবর্তিত (Android+Web)
- **V627** — "History" → "Checkup History", চেকআপ না থাকলে সরাসরি ফর্ম খোলে (Android+Web)
- **V628** — হিসাবের খাতা থেকে "All Branches" বাদ + Monthly Summary-তে Master-only "Edit This Day" (Android+Web)

বিস্তারিত: `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর V623–V628 এন্ট্রি দেখুন।

## ফাইনাল / TK-এর অনুমতি ছাড়া বদলানো যাবে না
- সব পুরনো লক করা ডিজাইন (আগের LOCK NOTE-গুলোতে যা যা লেখা আছে) — অক্ষত, এই সেশনে ছোঁয়া হয়নি।
- Anatomy bulge-এর আকৃতি/মাপ (`drawLump`/`bulgeFromDrag`/`RADIUS_MAX`) — TK স্পষ্ট বলেছেন বদলাবে না, বদলানো হয়নি।
- Blood Test Add Optional-এর ডিজাইন (ডেমো-প্রুফ পাশ হওয়া) — চিপ+ইনপুট বক্সের চেহারা।
- Anatomy toolbar-এর ক্রম (চিহ্ন→নালী→ফোলান) — **এখনো বদলানো হয়নি**, TK-এর উত্তরের অপেক্ষায়।

## 🔴 এখনো TK-এর লাইভ টেস্ট বাকি (এই ZIP-এ প্রথমবার)
V626 (bulge position-gate), V627 (Checkup History), V628 (All Branches বাদ + Edit This Day) — তিনটেই এখনো Android Studio-তে লাইভ টেস্ট হয়নি।

## যাচাই
- `tk_guard.py --release` — সব মেশিন-চেক পাশ
- `node --check` app.js ও finance.js — পাশ
- ভার্সন: V628 / 6.28 (build.gradle.kts + version.json দুই জায়গাতেই মেলানো)
