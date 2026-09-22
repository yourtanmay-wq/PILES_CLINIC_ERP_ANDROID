# PILES CLINIC — App Settings System · Gate 0 Audit Report (শুধু-পড়া)

**তারিখ:** ১৩.০৯.২০২৬ বিকেল · **ভিত্তি:** এই মুহূর্তের সর্বশেষ source (V1405, commit 046259e-এর পরের অবস্থা) · **কোনো code / database / design বদল হয়নি।**
**পদ্ধতি:** তিনজন স্বাধীন যাচাইকারী (Android · Web · লগইন/Supabase) পুরো source পড়ে প্রতিটা দাবির পাশে file:line প্রমাণ দিয়েছে; এখানে শুধু ফল ও সিদ্ধান্ত।
**রায়ের অর্থ:** Found = আছে ও চলে · Partially Working = আছে কিন্তু আংশিক/মৃত · Not Found = নেই · Runtime Test Required = কোডে নিশ্চিত নয়, চালিয়ে দেখতে হবে।

---

## ১. ব্লুপ্রিন্ট §৩-এর আটটা বাধ্যতামূলক প্রশ্ন — এক নজরে

| # | প্রশ্ন | রায় | এক লাইনে সত্য |
|---|---|---|---|
| ১ | Android-এ Settings screen / local preference / session timeout / sync control / crash logging / backup control আছে? | **Partially Working** | "Backup" বোতামটাই আসলে `App Settings` পর্দা (৪টা কার্ড); তার মধ্যে **২টা মৃত** (Session timeout, Auto Sync — মান সেভ হয়, কাজ করে না), ২টা চলে (Crash logging, Backup/Restore)। |
| ২ | Web-এ Settings page / theme variable / fixed colour / local storage preference / session timeout? | **Partially Working** | Master-এর "App Settings" পাতা আছে কিন্তু শুধু তথ্য (কোনো সুইচ নেই); theme token মাত্র ২১টা (~১% জায়গায় ব্যবহৃত), বাকি ~৫,৭০০ রং হাতে-লেখা; ব্যবহারকারীর preference বলতে একটাই key (A4 ভাষা); ১৫ মিনিটে auto sign-out **চলে**। |
| ৩ | Dashboard ও Menu কোথায় hard-coded, কোথায় role-অনুযায়ী? | **Found** | দুই জায়গাতেই তালিকা hard-coded (ফোনে XML-এর ক্রম, ওয়েবে ৩টা আলাদা array); দেখানো/লুকানো role ও **নির্দিষ্ট ব্যক্তির মোবাইল নম্বর** দিয়ে; ব্যবহারকারী-ভিত্তিক hide/order কোথাও নেই। |
| ৪ | একই setting Android ও Web-এ আলাদা মান? | **Found (অমিল আছে)** | Auto sign-out: ওয়েব ১৫ মিনিট (চলে) · ফোন ৩০ মিনিট (মৃত) · ফোনে আলাদা ৭-দিন নিয়ম (চলে)। Backup: ওয়েব ১০ টেবিল/৩০ মিনিট পরপর · ফোন ৭ টেবিল/দিনে একবার। ভার্সন: ওয়েবে দুটো উৎস। |
| ৫ | মান সেভ হয় কিন্তু consumer নেই — এমন পুরনো setting? | **Found (৪টা মৃত, সব ফোনে)** | `auto_sync_enabled` · `session_timeout_minutes` · `last_active_at` · `sync_meta/last_pull_*` — ওয়েবে মৃত key নেই। |
| ৬ | বর্তমান authentication identity, role source, branch source — কোনটা সত্য ও সক্রিয়? | **Found (দুই স্তর)** | মূল লগইন = মোবাইল+পাসওয়ার্ড, **ক্লায়েন্টে** hard-coded তালিকা (+ ক্লাউড override), anon key — এটা "verified" নয়। মডিউল লগইন = আসল Supabase Auth (`auth.uid()` → `hr.app_identity`) — **এটাই একমাত্র সার্ভার-যাচাইকৃত পরিচয়**। Role/branch: আগে hard-coded তালিকা, না পেলে ক্লাউড। |
| ৭ | Supabase schema, RLS, exposed schema, usage pattern — Settings-এর সাথে সংঘর্ষ? | **Found + ১টা Runtime Test** | `hr/wn/fin` exposed, RLS enable+force, শুধু authenticated; `public`-এর বেশিরভাগ টেবিলে RLS **বন্ধ** (anon পড়া-লেখা)। `reports` schema exposed কিনা repo-র SQL-এ নেই → Dashboard-এ দেখতে হবে। নতুন preference টেবিল `hr`-এ রাখলে সংঘর্ষ নেই। |
| ৮ | ভালো চলা feature ও locked design — Protected Area? | **Found (ব্যাপক)** | `TK-LOCKED` ৫৮ · `TK-APPROVED` ১৮৫ · বাংলা "লক" চিহ্ন ১৫১ জায়গায়; Dashboard · More menu · Follow-up · Chamber · Timeline · Doctor Check-up · Registration · form-style — ঠিক যে পর্দাগুলো theme/text-size ছোঁবে সেগুলোই লক করা। Dark mode TK নিজে নিষিদ্ধ করেছেন (০৬.০৮.২০২৬, `forceDarkAllowed=false`)। |

---

## ২. Android — বিস্তারিত ফল

**২.১ বর্তমান Settings-জাতীয় পর্দা (Found)**
- More menu → "Backup" (শুধু Master) → `security/SettingsActivity` — Manifest-এ নাম "App Settings"। কার্ড: Session timeout (মিনিট), Auto Sync সুইচ, Crash Logging সুইচ + Last Crash Log, Backup Now / Restore (local DB + cloud JSON + CSV)।
- আলাদা, একক-কাজের setting-পর্দা: Call ID Banner সেটআপ (V1427) · User Photo · Password Center · Export Data · A4 ভাষা (Doctor Check-up) · Prescription print options · Dialer tag hide · Master branch filter (১৮ পর্দা ব্যবহার করে)।
- ⚠️ `SecurityGuard.canChangeSettings()` ও `canRunBackupOrRestore()` সবসময় `true` — "Master-only" কথাটা মন্তব্যে, কোডে নয় (ফোনে Restore যে কেউ পেলে চালাতে পারে; পর্দাটা More menu থেকে Master ছাড়া খোলে না, সেটাই একমাত্র আড়াল)।

**২.২ Local preference (Found — ~১০০টা SharedPreferences ফাইল, কোনো কেন্দ্রীয় registry নেই)**
- দল: পরিচয়/সেশন (৯) · app_settings (১) · ব্যবহারকারীর পছন্দ (~১২) · **অফলাইন লেখার সারি (২৩ — টাকা/রোগীর তথ্য, ছোঁয়া নিষেধ)** · পড়ার cache (~৩০, বড়) · একবারের flag/scheduler (~২৫)।
- **মৃত (সেভ হয়, কেউ পড়ে না):** `app_settings/auto_sync_enabled` (TK-নির্দেশ ২৭.০৭.২০২৬: staff-এর সুইচ কখনো upload থামাতে পারবে না — তাই ইচ্ছে করে বাদ; কিন্তু পর্দায় সুইচ ও "next start-এ কার্যকর" toast এখনো আছে = **ভুয়া সুইচ**) · `session_timeout_minutes` · `session_timeout/last_active_at` · `sync_meta/last_pull_*` (SyncManager কখনো তৈরিই হয় না)।
- `piles_clinic_session` (EncryptedSharedPreferences, Supabase token) — কখনো ভরা হয় না (পুরনো AuthRepository পথ মৃত)।

**২.৩ Session timeout (Partially Working)**
- পর্দায় দেখানো ৩০-মিনিট মান মৃত (`SessionTimeoutManager.isTimedOut()`-এর কোনো caller নেই; "Auto-logout removed intentionally")।
- আসল নিয়ম, সব hard-coded: দিনে একবার biometric lock (২৪ ঘণ্টা, সবার) · Master বাদে ৭ দিন অ্যাপ না খুললে logout (clock-rollback-proof) · সার্ভারে suspend-চেক ১৫ মিনিটে একবার · লেখার আগে ১ মিনিটের freshness।

**২.৪ Sync control (Partially Working)**
- Status **চলে**: Dashboard-এ "☁️ Synced · V1405" / "⏳ N to sync", লাল banner + retry, pending-তালিকা — সব IO-থ্রেডে `PendingSyncStatus.summary()` থেকে।
- সুইচ **মৃত** (উপরে)। Upload সবসময় চালু: SyncWorker ১৫ মিনিট, reconnect-এ, BackgroundRefresh ১৫ মিনিট (নিজে ≥৬০ মিনিটে সীমিত)।

**২.৫ Crash logging (Found, চলে)** — handler আছে, ফাইল লেখে, সুইচটা আসলেই কাজ করে; কিন্তু Dashboard পরের বার খুললেই log দেখিয়ে path মুছে দেয়, তাই Settings-এর "View Last Crash Log" প্রায় সবসময় "No crash recorded" বলে (দুই consumer, এক path)।

**২.৬ Theme / text size / density (Not Found)**
- একটাই style `Theme.PilesClinic`, light-only, `forceDarkAllowed=false` (TK-ORDER); `values-night`/`dimens.xml` নেই; colors.xml-এ ২১টা নাম।
- **হাতে-লেখা রং:** Kotlin `Color.parseColor("#…")` **১,৫৮৯** (DoctorVisit ১৪৫, IncomeExpense ১২৮, StaffProfile ১২৬…) + `0xFF…` ৬২; XML `textColor="#…"` **৪৪৪** (বনাম `@color` ৩৪১), `background="#…"` ৪৪; `themes.xml` নিজেও hex লেখে।
- Text size: ৮৫৬টা `sp` (০ dp/px) — ফোনের নিজের font-scale মানে, অ্যাপে কোনো text-size/density preference নেই।
- Print সম্পূর্ণ আলাদা: HTML template inline-CSS (৮ ফাইল, ~৩০০ hex) + WebView print, Canvas/PDF builder, `print-color-adjust`, ৩০০ dpi রং (V1351) — screen theme এখানে পৌঁছায় না (ভালো)।

**২.৭ Dashboard ও More menu (Found — hard-coded)**
- `tile(binding.x, emoji, label, roles…)` ২০টা call; role-filter + `isDoctorVisitOnly` (নির্দিষ্ট মোবাইল) + doctor-এর আলাদা ৪-টাইল ছক (`arrangeDoctorGrid`); ক্রম XML GridLayout-এর include-ক্রমে বাঁধা; ১০টা টাইল setup-এর পরেই জোর করে GONE।
- More menu (TK-LOCKED ০৪.০৮.২০২৬): সারি XML-এ fixed, role-gate কোডে, Call ID কার্ড কোডে ঢোকানো। ব্যবহারকারী-ভিত্তিক hide/order — নেই।

**২.৮ App info (Found)** — ভার্সন More menu-তে ("App Version: 14.05") ও Dashboard-এ; role·branch Dashboard header-এ; platform কোথাও দেখানো হয় না (শুধু push-token-এ "android" পাঠায়)। TK-ORDER ৩১.০৭.২০২৬: দ্বিতীয় hard-coded ভার্সন-লেখা নিষেধ।

---

## ৩. Web — বিস্তারিত ফল

**৩.১ Settings/Backup (Found, তথ্য-মাত্র)** — `settings()` (Master, ☰ ও desktop sidebar): Clinic · User · Branch · Role · "Auto sign-out: after 15 minutes" (**হাতে-লেখা string, IDLE_MS-এর সাথে বাঁধা নয়**) · "Cloud sync: on login · nightly 2 AM · Sync Now" · Backup Center বোতাম · App Version। Backup Center: Create/Download/Restore + localStorage-এ history, save-এর পরে ≤৩০ মিনিটে auto-backup।
**৩.২ Theme (Partially Working)** — `:root` token ২১টা, `var(--…)` ব্যবহার মাত্র ৫১ জায়গায়; styles.css-এ hex **৪,০৪৪** (unique ১,১১৫) + rgba ৪২৫; app.js inline hex **৯৫৯**, profile.js ২৪৪, finance.js ২২৭, partners.js ১২০…; টাইলের রং-map hard-coded (`wlv1TileTheme`)। `prefers-color-scheme`/`data-theme`/font-size root/density — **কিছুই নেই**; ১,৫১৫টা `font-size` সব px। ছোট তথ্য: role-token `.admin{--blue}` কখনো লাগে না (role string "master")।
**৩.৩ Local storage (Found)** — টেবিল `rk_<table>` IndexedDB-wrapper-এ; ~৫০টা ছোট key; ব্যবহারকারীর preference বলতে **একটাই**: `wlv1A4Lang`। মৃত key: নেই। ⚠️ `rk_` prefix big-store logic-এর, নতুন preference key `rk_`-এ রাখলে গোলমাল।
**৩.৪ Session timeout (Found, চলে)** — IIFE-এর ভিতরে `IDLE_MS = 15 মিনিট`, ৬০ সেকেন্ডে চেক, timeout-এ pending flush → logout; logout শুধু `rk_session` মোছে (cache/pending/backup থাকে, ইচ্ছাকৃত)। ৭-দিন = sync-নিয়ম, session-নিয়ম নয়।
**৩.৫ Dashboard/Menu (Found, hard-coded)** — টাইল array `dashboard()`-এ (ক্রম ফোনের সাথে মিলিয়ে V430), ☰ menu array, desktop sidebar array — **তিনটা আলাদা তালিকা**, label-ও আলাদা; role filter + doctor-এর আলাদা ৪ · এক স্টাফের মোবাইল-ভিত্তিক override (`__armanOnly`)। per-user hide/order: নেই।
**৩.৬ Print (Found, প্রায় আলাদা — ছোট leak)** — ৩১টা `@media print` block, `@page A4`, receipt/A4 iframe-srcdoc/new-window isolation; **কিন্তু** `styles.css:2`-এর print block `var(--blue/--ink/--mut/--shadow)` inherit করে — theme token বদলালে print-এ ঢুকবে, print-এ light মান জোর করে দিতে হবে।
**৩.৭ Sync (Partially Working)** — Sync Now ৩ জায়গায়; status label শুধু Backup Center-এ; header badge/offline banner নেই। Realtime: ৬টা live channel + ৩ ভারী টেবিলে ৬০ s poll (V1295) + ৩০ s পর্দা-timer + ৬০ s flush + nightly tick।
**৩.৮ Auth/role/branch (Found)** — `config.js`-এর hard-coded `users` + `usercredentials` override + PBKDF2, `staff_login_list` fallback; session = `rk_session` JSON (mobile/name/branch/role); মডিউল: `<code>@staff.piles` Supabase Auth (`rk_module_auth`)। ⚠️ `name`/`code` পুরনো-নতুন স্টাফে আলাদা — preference-এর চাবি হিসেবে অনিরাপদ।
**৩.৯ App info (Found)** — ভার্সন = `app.js?v=` (index.html) — `version.json` আলাদা উৎস (Staff Performance-এ ব্যবহার); আজ দুটো মিলে আছে (1405) কিন্তু ভবিষ্যতে drift সম্ভব।

---

## ৪. পরিচয় · role · Supabase নিরাপত্তা (Settings-টেবিলের ভিত্তি)

| বিষয় | সত্য অবস্থা | সিদ্ধান্তের জন্য |
|---|---|---|
| মূল লগইন | মোবাইল+পাসওয়ার্ড, hard-coded তালিকা (`StaffDirectory.kt` ↔ `config.js` twin) + `usercredentials` override, সব **anon key**-তে | client-asserted — preference-এর নিরাপত্তা-চাবি হতে পারে না |
| মডিউল লগইন | Supabase Auth, synthetic email `<person_code>@staff.piles`, JWT → `auth.uid()` → `hr.app_identity(uid, person_code, role_kind, is_master, link_mobile)` | **এটাই verified identity** — preference টেবিলের primary key = `uid` |
| ব্যবহার | Android-এ মডিউল sign-in **lazy** (Work Notebook/RMP/Attendance খুললে), Dashboard-এ নয়; ওয়েবে প্রতি page-load-এ `MOD.restore()` | Android-এ Dashboard-এ একবার sign-in যোগ করলে = প্রতি app-start-এ ১টা auth POST + ১টা select (~১-২ KB) — সেটাই "login-এ একবার" hook |
| Role/branch উৎস | hard-coded তালিকা প্রথম, ক্লাউড (`staff_login_list` → `hr.staff_profiles`) fallback; `displayRole` বনাম permission-role (doctor/field → "staff") | Settings role বাড়াতে পারবে না — role-gate কোডে (মোবাইল-ভিত্তিক ব্যতিক্রম ১০+ জায়গায়: ARMAN, RUPAM, Dr KH Mandal, Amit Goldar/PK Roy, BIR-5…) |
| Exposed schema | `hr, wn, fin` (V246 `pgrst.db_schemas`), `public` | `reports` — **Runtime Test Required** (repo-তে statement নেই, Dashboard-এ হাতে করা) |
| RLS | `hr/wn/fin` enable+force, policy `is_master() or person_code = my_code()`, grant শুধু `authenticated` (anon নয়) | নতুন টেবিল `hr`-এ রাখলে default privilege inherit করে |
| `public` টেবিল | RLS **বন্ধ** (patients, payments, followups, enquiries, medical, products, doctor_visits, briefings, trash, usercredentials, activity_logs, chamber_close, reminders, device_tokens, device_logins…) | ⛔ preference টেবিল `public`-এ নয় (anon-লেখা হয়ে যাবে); ⛔ RLS-বিষয় TK-র "চিরতরে বন্ধ" তালিকায় — এখানে শুধু নতুন টেবিলের কথা |
| security definer | ২৯২ জায়গায় (identity helper, RMP/fin, reports.*, atomic writer, trigger) | নতুন টেবিলে লাগবে না — সাধারণ RLS policy-ই যথেষ্ট |
| আগের per-user cloud storage | `hr.app_identity`, `hr.staff_profiles`, `hr.salary_config` (Master-write), `public.device_logins/device_tokens` (per-device, RLS off) | নামের সংঘর্ষ নেই; প্রস্তাবিত নাম `hr.user_settings` (⛔ `app_devices` নয় — function নাম) |
| Backup/Restore | ফোন: ৭ public টেবিল JSON + local DB; ওয়েব: ১০ টেবিল localStorage payload, `backuprecords` | preference টেবিল backup-এর বাইরে রাখা; "Reset settings" আলাদা confirm, cloud-restore পথ share নয় |

**ফ্রি-প্ল্যান ভিত্তি (কোড থেকে):** Android — BackgroundRefresh ≥৬০ মিনিট, SyncWorker ১৫ মিনিট, LiveRefresh ৩০ s (পর্দা খোলা থাকলে), repository full refresh ২–৬ ঘণ্টা, reminder worker ১০–১৫ মিনিট, ফিল্ড GPS ১৮০ s; Web — ৬ realtime channel, ৬০ s delta poll, ৩০ s পর্দা-timer, nightly। **login-এ একবার ১ সারি (<১ KB) পড়া = নগণ্য।** ⚠️ আগস্টে egress ৫.৮৭/৫ GB (TK-র নিজের মাপ, ২৩.০৮) — আজকের usage পাতায় নতুন cycle <১%।

---

## ৫. সংঘর্ষ-তালিকা (Conflict Report) — Settings করতে গেলে যেখানে বাধা

| # | সংঘর্ষ | মাত্রা | কী করতে হবে (Gate 1/2-তে সিদ্ধান্ত) |
|---|---|---|---|
| C1 | **Theme-এর কোনো hook নেই** — ফোনে ১,৫৮৯+৪৪৪, ওয়েবে ~৫,৭০০ হাতে-লেখা রং; token layer বানাতে হলে **locked পর্দাগুলোই** বদলাতে হয় (নিয়ম ৪: প্রতিটার ফটো-প্রুফ) | 🔴 সবচেয়ে বড় | প্রথম release-এ theme শুধু "Classic Light" (= এখনকার) রেখে Soft Green/Navy পরে; অথবা token-migration-কে আলাদা প্রকল্প ধরা |
| C2 | Dark mode TK-নিষিদ্ধ (`forceDarkAllowed=false`, ০৬.০৮.২০২৬) | 🟡 | ব্লুপ্রিন্ট নিজেই বাদ রেখেছে — মিল আছে |
| C3 | Text size: ফোনে সব `sp` — অ্যাপের নিজস্ব scale বসাতে Configuration override লাগবে; ওয়েবে ১,৫১৫ px font-size, root scale নেই | 🟠 | Large text = ফোনের নিজের font-scale-এর উপর ভরসা (বলে দেওয়া), অথবা সীমিত পর্দায় | 
| C4 | Dashboard ক্রম XML/array-বাঁধা, role + **মোবাইল-ভিত্তিক ব্যক্তিগত নিয়ম** (ARMAN-only, doctor 2×2, TK-ORDER V1380/V1401) | 🟠 | registry বানিয়ে role-filter আগে, user-hide পরে; ব্যক্তিগত নিয়ম registry-র বাইরে অটুট |
| C5 | ওয়েবে টাইল/☰/sidebar — ৩টা আলাদা তালিকা, label আলাদা | 🟠 | এক registry থেকে তিনটে আঁকা (label এক করার সিদ্ধান্ত TK-র) |
| C6 | Session timeout-এর তিন সত্য: ওয়েব ১৫ মিনিট (চলে) · ফোন ৩০ মিনিট (মৃত) · ফোন ৭ দিন (চলে) | 🟠 | Settings-এ শুধু **সত্য** লেখা: "Web: 15 min idle → sign-out · Phone: 7 days not opened → sign-out"; মৃত ৩০-মিনিট ঘর সরানো |
| C7 | ফোনের Auto Sync সুইচ ভুয়া (TK-নির্দেশ ২৭.০৭: staff upload থামাতে পারবে না) | 🟡 | সুইচ সরিয়ে শুধু status; ব্লুপ্রিন্ট §৬.৩/৬.৫-এর "ভুয়া switch নয়" নিয়মের সাথে মিল |
| C8 | Crash log: Dashboard আগে দেখিয়ে path মুছে দেয় | 🟢 | Settings-এ log-ফোল্ডারের তালিকা দেখানো |
| C9 | Preference-এর চাবি: মূল লগইন client-asserted; verified চাবি `auth.uid()` শুধু মডিউল-JWT-তে | 🟠 | টেবিল `hr.user_settings(uid pk)`; Android Dashboard-এ একবার মডিউল sign-in যোগ (নতুন network call, মাপা দরকার) |
| C10 | `public`-এর RLS-বন্ধ টেবিলের ধাঁচ | 🔴 | preference টেবিল কখনো `public`-এ নয় |
| C11 | ওয়েব print block `styles.css:2` token inherit করে | 🟡 | print-এ light মান জোর করা |
| C12 | ভার্সন দুই উৎস (ওয়েব `?v=` বনাম `version.json`) | 🟢 | App info-তে একটাই উৎস |
| C13 | ফোনে ~১০০ SharedPreferences, নিয়ম ৫খ (প্রতিবার পর্দা খোলায় sync পড়া নিষেধ) | 🟠 | নতুন preference ছোট, একবার পড়া, memory-cache |

---

## ৬. Runtime Test Required (কোড দিয়ে নিশ্চিত নয়)
1. `reports` schema Supabase Dashboard-এর Exposed schemas-এ আছে কিনা (ভয়েস-প্রশ্ন চলছে বলে সম্ভবত হ্যাঁ — তবু দেখা)।
2. live project-এ `hr` schema-য় anon-এর কোনো grant নেই — নিশ্চিত করা (SQL-এ নেই, Dashboard-এ হাতে দেওয়া থাকতে পারে)।
3. Android-এ Dashboard-এ মডিউল sign-in যোগ করলে cold-start সময় কত বাড়ে — device-এ মাপা (ব্লুপ্রিন্ট §১৩)।
4. ফোনের font-scale বাড়ালে locked পর্দাগুলো (Follow-up, Chamber, Timeline) কেটে যায় কিনা — এটা "Large text" দেওয়ার আগে দেখা।

---

## ৭. Protected Areas — নিশ্চিতকরণ
ব্লুপ্রিন্ট §১৫-এর তালিকার সবকটা এই অডিটে **ছোঁয়া হয়নি** এবং Settings-প্রকল্পে ছোঁয়ার দরকার নেই: patient/enquiry/registration/follow-up/check-up/payment/RMP/attendance/salary/print/sync-queue/backup-algorithm/notification-delivery। একমাত্র সীমানা-স্পর্শ: **theme token migration** (C1) — সেটা locked পর্দার চেহারা বদলায়, তাই আলাদা অনুমোদন ছাড়া নয়।

## ৮. Gate 1-এর জন্য আমার সুপারিশ (সিদ্ধান্ত TK-র)
1. প্রথম release-এ Settings = **App info · Dashboard shortcut show/hide/order (registry) · সত্য session-নিয়ম দেখানো · Sync status · Backup entry · Call ID Banner entry** — theme/text/density **বাদ** বা শুধু "Classic Light"।
2. মৃত দুটো control (Session timeout ঘর, Auto Sync সুইচ) সরানো — ব্লুপ্রিন্টের "ভুয়া switch নয়" নিয়ম।
3. Cloud preference: `hr.user_settings(uid)` + RLS `uid = auth.uid()`, login-এ একবার পড়া, Save-এ একবার upsert, local cache প্রথম।
4. Theme/text-size চাইলে আলাদা প্রকল্প: token layer আগে locked পর্দায় ফটো-প্রুফসহ।

*এই রিপোর্টে কোনো কোড, ডেটাবেস বা ডিজাইন বদলানো হয়নি। পরের ধাপ (Gate 2: demo image + exact change list) TK-র লিখিত অনুমোদনের পরে।*
