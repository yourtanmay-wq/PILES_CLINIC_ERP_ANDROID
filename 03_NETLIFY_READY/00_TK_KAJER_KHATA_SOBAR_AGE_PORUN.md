
---

## ✅ ধাপ ১০–১৩ যাচাই শেষ — কোনো নতুন ফাঁক পাওয়া যায়নি (৩১.০৮.২০২৬, সন্ধ্যা)

**ধাপ ১০ · Print · Report Card · Prescription · Diet · Investigation —** ফোনের
Print Center-এ যে ৭টা ছাপা আছে (Prescription · Medicine Slip · Blood Test ·
Diet Chart · Registration · Payment Receipt · Doctor Checkup), কম্পিউটারে
সাতটাই আছে, উপরন্তু "Doctor Visit Print"-ও। ✅

**ধাপ ১১ · Reports —** ফোনে যা যা আছে (Enquiry · Patients · Collection ·
এই মাস বনাম গত মাস · Branch-wise · Staff-wise · Conversion), কম্পিউটারেও
সবই আছে; শুধু "Staff-wise Summary"-র নাম কম্পিউটারে **"Staff Performance"**,
কাজ একই (প্রতিটা স্টাফ আলাদা কার্ড, চাপলে বিস্তারিত)। ⚠️ প্রথমে ভেবেছিলাম
এটা নেই — নিজে যাচাই করে দেখলাম আছে, তাই ভুল দাবি করিনি। ✅

**ধাপ ১২ · Work Notebook —** IN TIME · OUT TIME · Today Patient ·
Superfone/Clinic কল · Daily Report · Monthly Report · ছুটির আবেদন — সবই
কম্পিউটারে আছে। ✅

**ধাপ ১৩ · Search · Timeline · Trash · Briefing —** সবই আছে; Trash-এর নিজস্ব
রিফ্রেশ আর Briefing realtime-এ আসে। ✅

**বাড়তি একটা যাচাই (লেখার দিক):** কম্পিউটার থেকে রেজিস্ট্রেশন করলে
`stage='Doctor Queue'` · `queue=true` · `doctorComplete=false` · `bill=0` —
ফোনের হুবহু একই চারটে ঘর বসে, তাই কম্পিউটারে ভর্তি করা রোগী ফোনের CHECK-UP
তালিকায় ঠিকই ওঠে। ✅

**১৩ ধাপের পূর্ণ ফল:** ৫টা আসল ফাঁক পাওয়া গিয়েছিল — CHECK-UP তালিকা (V906) ·
অ্যানাটমি ছবি (V910) · অ্যাপ থেকে করা কলের গোনা (V911) · Draft ও Dr. Visit-এর
তাজা তথ্য (V912)। বাকি সব ভাগ মিলিয়ে দেখে ঠিক পাওয়া গেছে।

---

## 🔒 MASTER AUDIT — Safe Work 1: Web Login Safety Restore (22.09.2026, TK অনুমোদিত)

**Owner rule:** কোনো ভালো কাজ নষ্ট করা যাবে না; আগে ভালো-মন্দ জানিয়ে অনুমতি, তারপর কাজ।
এই ধাপে **Live Supabase / Live Website / Android / Patient / Payment / Finance কোনো কিছু বদলানো হয়নি**।
শুধু V1658-এর local working copy-তে Web Login-এর একটি পুরোনো override নিরাপদ করা হয়েছে।

### ধরা পড়া আসল সমস্যা
`app.js`-এর আগের নিরাপদ login()-এ custom-password lock, PBKDF2 hash check,
Suspend/Remove check এবং cloud-added staff gate ছিল। কিন্তু পরে V265 stabilization patch
`window.login` আবার override করে role default password-কে custom password-এর পাশাপাশি
গ্রহণ করছিল এবং Suspend/hash/cloud-staff gate এড়িয়ে যাচ্ছিল।

### করা পরিবর্তন
নতুন login logic বানানো হয়নি। V265-এর আগে থাকা যাচাইকৃত secure login-কে আবার কার্যকর রাখা হয়েছে।
V265-এর ভালো **Login button double-tap lock / "Login..." state** অক্ষত রাখা হয়েছে।

### Targeted verification — PASS 7/7
1. Custom password থাকলে পুরোনো role default rejected — PASS
2. সঠিক custom password login — PASS
3. Hash থাকলে plaintext bypass বন্ধ — PASS
4. Valid hash login — PASS
5. Suspended/Removed staff blocked — PASS
6. Cloud-added staff secure path দিয়ে login — PASS
7. Login button double-tap lock শেষে স্বাভাবিক অবস্থায় ফেরে — PASS

**Web JavaScript syntax:** PASS (সব `03_NETLIFY_READY/*.js`)
**Scope lock:** এই ধাপে `app.js` ছাড়া runtime code বদলানো হয়নি।
**Release status:** local audited copy only; live deploy / database change / file handoff করা হয়নি।

---

## 🔒 MASTER AUDIT — Safe Work 2: Password DB Safe Path Preparation (22.09.2026, TK অনুমোদিত)

**Owner rule:** কোনো ভালো কাজ নষ্ট করা যাবে না। এই ধাপে existing Login/Password Center/Money Handover বন্ধ করা যাবে না; RLS ON বা plaintext delete এখনো করা যাবে না।

### আগে যাচাই করে যা পাওয়া গেছে
- `usercredentials` শুধু Login নয় — Android/Web Password Center এবং Chamber Money Handover password verification-ও পড়ে।
- তাই সরাসরি RLS ON করলে ভালো কাজ ভাঙার ঝুঁকি ছিল; সেই কাজ করা হয়নি।
- Supabase Auth identity path (`ModuleAuth` / `MOD`) আগে থেকেই আছে; Master-only authenticated পথ ব্যবহার করা সম্ভব।

### নিরাপদে যা যোগ করা হয়েছে
1. Live DB-তে **additive** `tk_credential_verify_v2(mobile,password)` — stored password/hash ফেরায় না; শুধু `OK / WRONG / NO_CUSTOM`।
2. Live DB-তে Master-only authenticated `tk_password_center_list_v2()` এবং `tk_password_center_set_v2(...)`। `anon` execute বন্ধ।
3. Local Web Login ও Android Login এখন **V2-first**; V2 unavailable হলে পুরোনো proven path fallback।
4. Web + Android Money Handover password check-ও V2-first + old fallback।
5. Web + Android Password Center-এ authenticated Master V2-first + old fallback। Web password save-এ Android-এর একই PBKDF2 format hash তৈরি হয়।

### যেগুলো ইচ্ছা করে এখনো করা হয়নি
- `usercredentials`-এ RLS ON করা হয়নি।
- existing anon table permission revoke করা হয়নি।
- plaintext password delete/blank করা হয়নি।
- পুরোনো RPC/table path delete করা হয়নি।
- Live Website/APK deploy করা হয়নি।

### Verification
- V2 function permission: verify = anon/authenticated execute; Password Center list/set = anon blocked, authenticated allowed + DB-side `hr.is_master()` guard।
- Web JavaScript syntax: PASS (সব `03_NETLIFY_READY/*.js`)।
- Updated targeted Login behavior test: **PASS 9/9** — OK, WRONG, NO_CUSTOM, fallback custom lock, hash priority, suspended block, cloud-added staff, double-tap সব PASS।
- Android full Gradle compile: **NOT CLAIMED** — environment থেকে Gradle 8.5 download network-blocked। Source/static path inspected only।

**Safety result:** বর্তমান production behavior বদলায়নি; live DB-তে শুধু unused/additive V2 দরজা যোগ হয়েছে। পরের ধাপে RLS/permission বদলানোর আগে আলাদা TK অনুমতি আবশ্যক।

## MASTER AUDIT SAFE SECURITY WORK — 22.09.2026 (STEP 3 TEST/FREEZE)

- Original V1658 untouched; all client changes remain in separate working copy.
- Login V2 targeted behavior test: 9/9 PASS.
- Password Center + Money Handover targeted safety test: 12/12 PASS.
- All Netlify-ready JavaScript syntax checks: PASS.
- Live V2 Password Center master test used rollback only; no test data persisted.
- Non-master access to V2 Password Center list/set: BLOCKED as intended.
- Important issue found before live save: 22/24 existing credential rows use legacy/non-canonical IDs. The first V2 set function could have inserted a second row for the same mobile.
- Fixed safely on live DB: tk_password_center_set_v2 now locates by normalized mobile, updates the existing row, inserts only when no row exists, and refuses duplicate-mobile state.
- Rollback tests confirmed existing-row update does not increase row count; new-row test rolled back; duplicate mobile groups = 0 after test.
- usercredentials RLS/permissions/plaintext are NOT changed yet. This is deliberate: old clients must keep working until the modified Web/Android build is actually deployed and the three real flows are checked.
- Full Android compile was NOT claimed: Gradle 8.5 distribution is not cached and this environment cannot reach services.gradle.org.
- No patient/payment/finance data was changed by this step.

## 🔒 MASTER AUDIT — V1659 Test Build Preparation (22.09.2026, TK অনুমোদিত)

- Safe Work 1–3-এর audited client changes-ই রাখা হয়েছে; নতুন business feature যোগ করা হয়নি।
- Release number Android/Web দু'জায়গায় V1659 / 16.59 করা হয়েছে।
- বদলানো Web `app.js` যেন পুরোনো browser cache থেকে না আসে, `index.html`-এ শুধু `app.js` cache key V1659 করা হয়েছে।
- Login targeted test 9/9 PASS; Password Center + Money Handover safety test 12/12 PASS; সব Web JS syntax PASS।
- `usercredentials` RLS/permission/plaintext এখনো lock/remove করা হয়নি — real-use test PASS না হওয়া পর্যন্ত ইচ্ছা করে pending।
- Full Android Gradle compile claim করা হয়নি; environment-এ Gradle distribution নেই এবং network download blocked।
- Patient/Payment/Finance/Partner/Treatment/Attendance/Print/Report business logic এই release preparation-এ বদলানো হয়নি।

## 🔒 V1660 — RMP Calling Access + More Layout (22.09.2026)
- Android/Web release label: V1660 / 16.60.
- More: Reports + Password পাশাপাশি।
- Master Staff Profile থেকে নির্দিষ্ট অন্য branch-এর **RMP Calling Access** ON/OFF করতে পারবেন; confirmation + mandatory reason লাগে।
- অনুমোদিত Staff remote branch-এ শুধু RMP **Call + mandatory Remark** করতে পারবেন। Patient/Payment/Commission/View All/Edit/Delete/Add RMP পথ remote mode-এ নেই।
- Remote list/write authenticated `hr` RPC দিয়ে; permission save-এর সময়ও server re-check হয়; revoke করলে খোলা screen থেকেও পরের save block হবে।
- Permission change audit + call audit আলাদা HR table-এ।
- Patient/payment/finance/commission-এর existing business rule এই কাজে বদলানো হয়নি।
