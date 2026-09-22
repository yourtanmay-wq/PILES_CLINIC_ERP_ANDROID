# V216_FINAL_DECLARATION — সৎ ঘোষণা

**Base:** V215 (215/2.15) → **V216 (216/2.16)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।

### কোন কোন File পরিবর্তন করা হয়েছে
Android ১১টা (Kotlin/XML, ২টা নতুন) + web ১ + SQL ১ নতুন + doc/asset — বিস্তারিত `V216_CHANGED_FILES.md`।

### কোন কোন সমস্যা সত্যিই কোড-এ ঠিক/যোগ হয়েছে (static-verified, device test বাকি)
- **§13 Refund/টাকা ফেরত — সম্পূর্ণ কার্যকর code:** refund row (আলাদা entry, পুরোনো payment অটুট), Master সরাসরি refund / Staff refund request → Master-এর ঘন্টায় → Briefing পর্দায় Approve/Reject; **শুধু approved refund** per-patient paid ও today/range collection থেকে বিয়োগ; Visit Fee অক্ষত; History-তে received+refunded।
- **§10 Report cache-first** — cache থাকলে সঙ্গে সঙ্গে আঁকে, আলাদা Loading Screen নেই; cache না থাকলে আগের মতোই।
- **§4 Password hashing** — PBKDF2, backward-compatible: নতুন password fresh hash, পুরোনো plaintext lazy-migrate, login hash-preferred; কিছু ভাঙে না।
- **§5 Supabase Auth prep** — signIn/signUp helper (JWT) প্রস্তুত (unused, behavior বদলায় না) + Auth-prep SQL।
- **§15** — near-realtime bell (V215/V216) অক্ষত + FCM drop-in source প্রস্তুত।

### কোন Test Pass হয়েছে
শুধু **static** (bracket-balance ১০ file — সব PASS; XML well-formed; call-site/signature ও OkHttp idiom মিলিয়ে দেখা)। বিস্তারিত `V216_TEST_REPORT.md`।

### কোন Test চালানো যায়নি
সব device/multi-phone/weak-internet/Gradle-build/signed-APK/Refund-money/Password-login — এই cloud session-এ সম্ভব নয়, "Pass" লেখা হয়নি।

### Supabase Free Plan নিরাপদ রাখা হয়েছে কি না
হ্যাঁ — refund/approval বিদ্যমান `payments`/`briefings` টেবিলেই (নতুন টেবিল নেই), pending refund-এর জন্য একটা সস্তা index; notification আলাদা polling যোগ করেনি।

### Approved Design/Workflow/Permission/Money-rule অপরিবর্তিত কি না
হ্যাঁ — Refund শুধু **যোগ** হয়েছে, পুরোনো payment row edit/delete হয় না; card/print/popup design ছোঁয়া হয়নি; branch money-rule (MoneyBranchGuard) refund-এও একই; locked back flow (§2.13/§2.14) ছোঁয়া হয়নি। §14 same-day-delete gate অপরিবর্তিত।

### Patient/Payment Data Delete হয়নি কি না
হয়নি — কোনো row/table drop নেই; RLS/UNIQUE/FK live-এ চালানো হয়নি।

### কোন অসম্পূর্ণ External/Manual Setup বাকি
`V216_MANUAL_SETUP_IF_REQUIRED.md`: (১) refund কলামের PART A SQL চালানো, (২) web login hashing + plaintext column drop, (৩) Auth+RLS, (৪) FCM instant push, (৫) signed APK।

### যা এই সেশনে কোড-এ করা হয়নি (স্পষ্টভাবে)
§10 Journey/Action instant-header ও Follow-up scroll (কারণ CHANGED_FILES/TEST_REPORT-এ), §4 web-side hashing live rewire, §4 RLS enable। এগুলো ঝুঁকি/নির্ভরতার কারণে বাদ — spec/manual দেওয়া হলো।

**⛔ কাজ "১০০% সব শেষ" নয়। যা কোড-এ হয়েছে তা উপরে; যা বাকি তা-ও স্পষ্ট লেখা। কোনো untested জিনিসকে "Pass"/"Done" বলা হয়নি।**
