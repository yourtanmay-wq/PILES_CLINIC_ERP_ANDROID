# V216_MANUAL_SETUP_IF_REQUIRED.md

⛔ নিচের কাজগুলো এই সেশনে করা **সম্ভব নয়** (Supabase dashboard/Firebase console/keystore/ফোন লাগে)। "done" বলা হয়নি।

## ১. SQL চালানো (নিরাপদ, এখনই)
- `04_SUPABASE_DATABASE_SETUP/V215_SAFE_MIGRATION_2026-07-31.sql` — PART A (password_hash, refund কলাম)।
- `04_SUPABASE_DATABASE_SETUP/V216_AUTH_PREP_2026-07-31.sql` — PART A (auth_user_id, `payments_refund_pending_idx`)।
- দুটোরই PART B/C (RLS/UNIQUE/FK) **এখন নয়** — নিচের ২ নম্বরের পরে।
> refund feature ঠিকমতো চলতে **PART A অবশ্যই** চালাতে হবে (payments-এ refund কলামগুলো নইলে refund row-এর reason/approval সেভ হবে না)।

## ২. Password hashing সম্পূর্ণ করা (§4)
- এখন: নতুন password সেভে Android **fresh hash** বসায়; পুরোনো custom password প্রথম সফল login-এ hash হয় (lazy)। login hash থাকলে hash দিয়ে, নয়তো plaintext — **কিছু ভাঙে না**।
- বাকি (manual): (a) **web (app.js) login-এও** hash-verify যোগ করা (এখন web plaintext-এ চলে; hash Android থেকে বসছে, coexist) — SubtleCrypto PBKDF2, testing সহ; (b) সব ফোন hash-ready হলে DB-র plaintext `password` ও source-এর hardcoded map (`StaffDirectory.kt`, `config.js:39`) মুছে ফেলা।

## ৩. Supabase Auth + RLS (§5) — RLS এখন নয়
- `SupabaseAuth.kt` প্রস্তুত (signIn/signUp)। ধাপ: প্রতিটা staff/doctor/master-কে Auth user বানান (signUp; role/branch metadata), `usercredentials.auth_user_id`-এ uid বসান, app-এর সব request Auth JWT দিয়ে পাঠান, তারপর `V216_AUTH_PREP` PART B-র RLS policy enable। ⛔ এর আগে RLS enable করলে live app বন্ধ।

## ৪. §15 Instant Push (FCM) — ঐচ্ছিক
- `10_FUTURE_PLANS/fcm_push_ready/README_FCM.md`-এর ৭ ধাপ। এখন near-realtime bell কোড-এ চালু (V215/V216), instant নয়।

## ৫. Signed APK (§20.10)
- Release keystore repo-তে নেই — signed APK দাবি করা হয়নি; TK-এর keystore দিয়ে Android Studio-তে signed build।

## ৬. Refund feature — device যাচাই (§13)
- PART A SQL চালানোর পর: Master direct refund → collection/paid ঠিক কমছে ও visit fee অক্ষত মিলিয়ে নিন; Staff refund → Master ঘন্টায় request → Approve করলে কমছে, Reject-এ প্রভাব নেই — দুই ফোনে দেখে নিন।
