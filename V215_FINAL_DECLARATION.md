# V215_FINAL_DECLARATION — সৎ ঘোষণা

**Base:** V214 (214/2.14) → **V215 (215/2.15)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।

### কোন কোন File পরিবর্তন করা হয়েছে
১১টা Android/web/SQL ফাইল + নতুন doc — বিস্তারিত `V215_CHANGED_FILES.md`-এ।

### কোন কোন সমস্যা সত্যিই কোড-এ ঠিক হয়েছে (static-verified, device test বাকি)
- **§12** Action popup থেকে Remark সরানো (আলাদা Remark flow অক্ষত)।
- **§17** যে কোনো stage-এ post-call remark = Completed Call → Last Call Date/Signal আপডেট (তিনটে remark entry-point-এই)।
- **§16** Incomplete/Reject-এ মিথ্যা "Saved" বন্ধ; সফল হলে source list-এ ফেরা; cloud-confirm না হলে সৎ বার্তা।
- **§18** Draft/Visit-Reject/Incomplete Delete করলে সারি সত্যিই tombstone+Trash হয়, তালিকা থেকে সরে, Back/Refresh-এ ফেরে না; Restore-এ সঠিকভাবে ফেরে; pull-to-refresh সত্যিকারের reload।
- **§6** status-change-এ সত্যিকারের cloud-confirm আলাদা করা (মিথ্যা "Saved" কমানো)।
- **§15** Briefing/Approval/Refund-request notification near-realtime (সাউন্ড+ভাইব্রেশন+background, de-dup, Free-plan-safe HEAD count)।
- **§11** Payment/Register থেকে একবার Back-এ source list-এ ফেরা (locked Blood-Test/Edit-Record flow অক্ষত)।
- **§9** Cooch Behar নম্বর ঠিক; web cache-buster v215।
- **§4 (web)** Netlify security header (`_headers`), CSP Report-Only।
- **§4/5/8 (DB)** নিরাপদ additive migration (password_hash/refund কলাম, duplicate-finder)।

### কোন Test Pass হয়েছে
শুধু **static** (bracket-balance ১০ Kotlin file — সব PASS; config.js `node --check` PASS; call-site/signature মিলিয়ে দেখা)। বিস্তারিত `V215_TEST_REPORT.md`।

### কোন Test চালানো যায়নি (সৎভাবে)
§19-এর সব **device/multi-phone/weak-internet/Gradle-build/signed-APK** test — এই cloud session-এ সম্ভব নয়, তাই "Pass" লেখা হয়নি। TK-এর Android Studio + ফোনে চালাতে হবে।

### Supabase Free Plan নিরাপদ রাখা হয়েছে কি না
হ্যাঁ। §15 notification আলাদা কোনো polling যোগ করেনি — বিদ্যমান সস্তা HEAD-count-এই briefings যোগ হয়েছে; §18 delete redundant cloud write বাদ দেয়; §18 refresh শুধু pull/onResume-এ।

### Approved Design/Workflow/Permission অপরিবর্তিত কি না
হ্যাঁ। Card/print/popup design ছোঁয়া হয়নি; §2.13/§2.14 locked back flow ছোঁয়া হয়নি; branch/role/permission rule অপরিবর্তিত (§14 same-day-delete gate ইচ্ছাকৃতভাবে অপরিবর্তিত — testing ছাড়া বদলানো ঝুঁকি)। §17-এ patient-card signal meter design বদলানো হয়নি।

### Patient/Payment Data Delete হয়নি কি না
হয়নি। কোনো row/table drop হয়নি; delete soft-delete (Trash) থেকেছে; RLS/UNIQUE/FK live-এ চালানো হয়নি।

### কোন অসম্পূর্ণ External/Manual Setup বাকি
`V215_MANUAL_SETUP_IF_REQUIRED.md`: (১) CSP enforce flip, (২) password-hash+Auth+RLS (সবচেয়ে জরুরি security বকেয়া), (৩) FCM instant push (ঐচ্ছিক), (৪) §13 Refund কোড বসানো (`V215_REFUND_AND_LOADING_SPEC.md`), (৫) signed APK (keystore TK-এর)।

### যা এই সেশনে করা হয়নি (স্পষ্টভাবে)
- **§13 Refund** — spec + DB কলাম দেওয়া হলো, UI/totals কোড বসানো হয়নি (money-path, testing দরকার)।
- **§10 Check-up/Report দ্রুত খোলা ও scroll preserve** — spec দেওয়া হলো (§11-এর one-Back অংশ কোড-এ হয়েছে)।
- **§4 password hashing live + RLS enable** — Auth migration দরকার, ধাপে ধাপে।

**⛔ কাজ ১০০% "সব ঠিক" নয়। উপরের তালিকা অনুযায়ী কিছু কাজ কোড-এ হয়েছে, কিছু spec/manual হিসেবে বাকি। কোনো untested জিনিসকে "Pass"/"Done" বলা হয়নি — TK-এর নিয়ম মেনে।**
