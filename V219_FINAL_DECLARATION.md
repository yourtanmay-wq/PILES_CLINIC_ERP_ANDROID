# V219_FINAL_DECLARATION — সৎ ঘোষণা

**Base:** V218 (218/2.18) → **V219 (219/2.19)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।

### কোন কোন File পরিবর্তন — বিস্তারিত `V219_CHANGED_FILES.md`।

### সত্যিই কোড-এ যা ঠিক হয়েছে (static-verified; device/build/live test Pending)
- **§1** Refund double-on-retry বন্ধ — deterministic id (Android + web); retry-তে দ্বিতীয় Refund হয় না।
- **§2** Web Delete এখন **Record ID** ধরে (মোবাইল নয়) — ভুল রোগী মুছবে না।
- **§3** ROLLBACK_V218/ — V218 source-এর **আসল** কপি (নকল/একই নয়)।
- **§4** আটকে থাকা HTTP 400 — Table·Record·সহজ কারণ দেখানো হয়, কারণ আর হারায় না, permanent 4xx ২ চেষ্টার পর নিরাপদে park (৫০ বার বৃথা নয়)।
- **§5** Android app (native) · ভিতরের web copy · Netlify — approved feature/logic মিলানো (assets/www = Netlify হুবহু; native-এ feature আগেই native)।
- **§6** Auth/RLS/password — আলাদা copy-paste SQL (PART A নিরাপদ; RLS/plaintext-drop COMMENT)। Login/Master Password Center কোড অপরিবর্তিত।
- **§7** Free-plan — briefings full-table fetch `CloudReadCache`-এ dedupe (কোনো সারি বাদ যায় না)।
- **§8** FCM ছাড়া "instant" দাবি করা হয়নি।

### কোন Test Pass — শুধু static (bracket-balance, node --check, signature-মিলানো)।

### কোন Test চালানো যায়নি — সব Android build/device/দুই-ফোন/live-Supabase (§10 মেনে **Pending** লেখা, `V219_TEST_REPORT.md`)।

### Design/Workflow/Permission/Branch/Diet Chart
কিচ্ছু বদলানো হয়নি। §4-এর তথ্য বিদ্যমান সতর্কবার্তার লেখাতেই (নতুন বোতাম/layout নয়)। §2 delete-এর **আচরণ** (কী Trash-এ যায়) একই — শুধু চেনা হয় id দিয়ে। Diet Chart ছোঁয়া হয়নি।

### Patient/Payment Data — কিছু Delete/drop হয়নি; RLS live-এ চালানো হয়নি।

### বাকি / manual (লুকানো হয়নি)
- Android build + device + live Supabase test (Pending)।
- §7 বাকি টেবিল (Draft/Trash/Queue/Chamber) একই নিরাপদ pattern-এ কমানো — পরবর্তী ধাপ।
- §6 RLS enable / plaintext drop / Supabase Auth live-wiring — manual (SQL-এ ধাপ; এখন করলে live app বন্ধ)।
- §8 FCM instant push — করা হয়নি।

### নতুন সন্দেহ (সৎভাবে)
- §7 briefings ২০s cache: staff request master-এর পর্দায় ≤২০s দেরিতে আসতে পারে (তথ্য হারায় না) — device-এ মিলিয়ে দেখবেন।
- §1: হুবহু-একই ইচ্ছাকৃত দ্বিতীয় refund overwrite হবে (বিরল; double-refund ঠেকানোর মূল্য)।

**⛔ "১০০% সব শেষ ও tested" নয়। কোড-এ যা হয়েছে উপরে; device/live যাচাই বাকি (Pending)। কোনো untested জিনিসকে Pass বলা হয়নি।**
