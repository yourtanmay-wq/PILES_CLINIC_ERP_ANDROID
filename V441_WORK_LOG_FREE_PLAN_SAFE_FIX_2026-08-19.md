# V441 — Supabase Free Plan Safe Fix (approved 3 items only)

**তারিখ:** 19.08.2026 · **অনুমতি পাওয়া:** 09:18 IST · **কাজ শেষের যাচাই:** 09:24 IST  
**Base:** V440 / 4.40 → **V441 / 4.41**  
**Web cache:** `app.js?v=v472` → `v473`

## মালিকের নির্দেশ

Supabase Free Plan-এর বাকি ঝুঁকি সাবধানে ঠিক করতে হবে; আন্দাজে কোনো কাজ নয় এবং
আগে থেকে ভালো/অনুমোদিত কোনো কাজ নষ্ট করা যাবে না। এই অনুমতিতে শুধু আগের আলোচনায়
নির্ধারিত তিনটি low-risk কাজ করা হয়েছে।

## 1) Web non-photo update-এ পুরনো photo আবার cloud-write বন্ধ

**ফাইল:** `03_NETLIFY_READY/app.js`

- `upd()` আগে যে কোনো note/bill/discount/approval update-এ সম্পূর্ণ row direct
  cloud upsert করত; row-এ পুরনো `photo` থাকলে সেটাও আবার যেত।
- এখন patch নিজে `photo` বা `patientPhoto` বদলালে তবেই photo field direct payload-এ থাকে।
- Local row/photo এক অক্ষরও মুছে/ফাঁকা করা হয় না।
- Offline photo retry safety-ও রক্ষা করা হয়েছে: non-photo save কোনো আগের pending
  photo marker ভুল করে clear করতে পারবে না।

**Behavior test:**
- non-photo update → local photo থাকে, cloud payload-এ photo নেই ✅
- real photo update → photo cloud payload-এ থাকে ✅
- non-photo success → পুরনো pending-photo marker clear হয় না ✅
- photo upload ব্যর্থ → retry marker থাকে ✅

## 2) Web deleted_records 5000 সীমা সরানো — কোনো delete history মোছা নয়

**ফাইল:** `03_NETLIFY_READY/app.js`

- আগে `deleted_records.select('id').limit(5000)` ছিল।
- এখন 1000-row page ধরে শেষ পর্যন্ত সব tombstone id পড়ে।
- কোনো page error হলে নতুন অসম্পূর্ণ Set বসানো হয় না; আগের safe Set রাখা হয়।
- কোনো `deleted_records`/`trash` row delete করা হয়নি, SQL চালানো হয়নি।

**Behavior test:** 6203টি synthetic tombstone সম্পূর্ণ পড়েছে ✅; 2nd page fail করলে
আগের Set অপরিবর্তিত থেকেছে ✅।

## 3) Android Draft enquiry read সরু করা

**ফাইল:**
- `native/SupabaseClient.kt`
- `native/DraftRepository.kt`

`ENQUIRY_COLS_DRAFT` =
`id,date,branch,name,mobile,disease,remarks,timeType,receivedBy,stage,nextFollow,createdBy,updatedAt`

- Draft code-এ enquiry row থেকে যেসব field সত্যি পড়া হয় সেগুলো ধরে তালিকা করা হয়েছে।
- প্রত্যেক field active `public.enquiries` schema-তে আছে—script দিয়ে মিলিয়ে দেখা হয়েছে।
- same branch read 5000 limit এবং staff-এর own cross-branch read 2000 limit—দুটির
  row/filter/order নিয়ম অপরিবর্তিত; শুধু columns কমেছে।
- narrow read ব্যর্থ হলে existing `fetchListSlimOrNull()` full-row fallback অটুট।

## ইচ্ছে করে যা ছোঁয়া হয়নি

- `ModuleAuth` login / refresh token / logout session logic
- `GlobalSearch` All Branch behavior
- `DoctorQueue` photo refresh behavior
- BackgroundRefreshWorker V440 branch fix
- design / color / card / button / workflow
- payment/refund/commission/medical rules
- Supabase SQL/RLS/data deletion

## যাচাই

- `node --check 03_NETLIFY_READY/app.js` ✅
- `verify_version_json.py` → V441 / 4.41 match ✅
- `tk_guard.py` → সব machine guard pass ✅
- Supabase column guard pass ✅
- XML guard pass ✅
- locked 24 rules + 9 workflow rules pass ✅
- targeted web behavior tests A/B/C pass ✅
- Draft field/schema test pass ✅

### Android Gradle build সম্পর্কে সৎ নোট

`./gradlew assembleDebug --no-daemon` চালানো হয়েছে। Source/code error পর্যন্ত পৌঁছায়নি,
কারণ এই কাজের environment-এ Gradle 8.5 আগে থেকে নেই এবং internet DNS blocked থাকায়
`services.gradle.org` থেকে distribution download করতে পারেনি (`UnknownHostException`)।
তাই **actual Android Gradle build passed** বলে দাবি করা হচ্ছে না। Project-এর own
Kotlin/XML/Supabase/version guards সব পাশ করেছে। Android Studio-তে Gradle available
থাকা যন্ত্রে final build করা আবশ্যক।

## Release guard

- `tk_guard.py --release` সব machine check pass করেছে ✅
- নির্ধারিত unique ZIP নাম: `PILES_CLINIC_APP_V441_FINAL.zip`
- release record-এর সময় user timezone অনুযায়ী `19.08.2026 09.26 am` লেখা হয়েছে।
