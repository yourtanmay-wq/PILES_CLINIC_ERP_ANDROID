# V230 — অবস্থা (সৎ, সংক্ষিপ্ত)

**V230 = এ পর্যন্ত সব নিরাপদ code-কাজের সম্পূর্ণ রিলিজ।** এই cloud-এ Android build সম্ভব নয় (SDK নেই) — তাই FINAL নয়; owner Android Studio-তে build করবেন।

## ✅ যা code দিয়ে করা হয়েছে (V226→V230, সব নিরাপদ ও যাচাইকৃত)
- Report-এর current-month তারিখ-গণনা দৃঢ় (dd.MM.yyyy/ISO সব ধাঁচ)।
- Trash Bin-এ Master-এর Branch Filter (Staff অপরিবর্তিত)।
- Payment ও Patient-Action list-এ Back/refresh-এ scroll জায়গা ধরে রাখা।
- নাম ফাঁকা হলে সব জায়গায় "Name Not Available" → "UNKNOWN"।
- Follow-up card-এ section নাম ও তারিখ সামান্য বড় (তিন card-এই)।
- Official Patient ID duplicate-guard (SQL) — index তৈরি, duplicate নেই।
- Android/Web/Website version parity (230 / 2.30 / v230)।

## ⏳ যা এই পরিবেশে code দিয়ে নিরাপদে শেষ করা যায় না (owner-এর live-test/নির্দেশ দরকার)
এগুলো আন্দাজে বদলানো হয়নি (আপনার নিয়ম: approved design অপরিবর্তিত + live data ছাড়া অনুমান নয়):
- **Live data-নির্ভর:** নির্দিষ্ট HTTP 400-এ আটকে থাকা record-এর কারণ; ghost/trash multi-device আচরণ; count লাফানো; branch-today মেলানো; orphan/"7777777777" রেকর্ড সঠিক রোগীর সঙ্গে যুক্ত করা। → নিচের read-only SQL দিয়ে owner শনাক্ত করবেন।
- **চোখে-দেখা রুচি (একটি করে বললে করব):** card-এর box উচ্চতা, tag সাজানো, অন্য font, Blood Test compact, Report-এর period label। (section নাম/তারিখ যেমন একটি করে বলে করালেন, তেমনই।)
- **অস্পষ্ট নকশা:** Blood Test-এর "Previous Patient Blood Test" redesign — অর্থ নিশ্চিত না হলে working screen ভাঙার ঝুঁকি; owner-এর design নিশ্চিতি দরকার।

## 📄 প্রস্তুত read-only SQL তালিকা
`04_SUPABASE_DATABASE_SETUP/V230_SQL_LIST_READONLY.sql` — ২টি শুধু-দেখা query (নাম/ID-ফাঁকা রোগী; "7777777777" রেকর্ড)। owner-এর live-test-এর পর একবারে একটি করে চালানোর জন্য দেওয়া হবে।

**কোনো মিথ্যা Build/Test দাবি নেই; কোনো approved design/workflow/payment/permission/data বদলানো হয়নি।**
