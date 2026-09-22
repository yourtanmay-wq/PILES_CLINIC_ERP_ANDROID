# V215_BEFORE_AUDIT.md — কোড পড়ে সত্যতা যাচাই (কোনো পরিবর্তন করা হয়নি)

**Base:** V214 (versionCode 214 / 2.14). **তারিখ:** 31.07.2026 IST.
**পদ্ধতি:** প্রতিটা claimed সমস্যা আসল source code-এ (Kotlin + web app.js + Supabase SQL) খুঁজে যাচাই করা হয়েছে। নিচে প্রতিটার: **অভিযোগ → কোড আসলে কী করে (file:line) → আসল কারণ → কোন file বদলালে ঠিক হবে**।
**গুরুত্বপূর্ণ:** এটি শুধু যাচাই। এখনো কোনো code বদলানো হয়নি (Order rule 1.2 মেনে)।

> সততা নোট: এই যাচাই আসল ফোন/আসল DB ছাড়া, শুধু source code পড়ে করা। যেখানে code থেকে নিশ্চিত হওয়া যায়নি, সেখানে পরিষ্কার লেখা আছে "নিশ্চিত নয়"।

---

## ✅ যেটা আগেই ঠিক আছে (নতুন করে "ঠিক" করার দরকার নেই — না ভাঙলেই হলো)

**§10.3 — Check-up থেকে Action-এর পর Back → সরাসরি Queue-তে ফেরা (Doctor Check-up path):**
`ClinicalModulesActivity.kt:113` — `autoOpen=="CHECKUP"` হলে হাব নিজেকে `finish()` করে দেয় (TK-এর নোট L100-105 সহ)। তাই Doctor Check-up থেকে একবার Back-এ Queue-তে ফেরে — এই নির্দিষ্ট back-অভিযোগ কোডে **আগেই সমাধান করা**। এটি নষ্ট করা যাবে না।

*(বাকি Journey/Report/Action path-এর back সমস্যা এখনো আছে — নিচে §10/§11 দেখুন।)*

---

## SECTION 10 — CHECK-UP Queue navigation

মূল file: `native/DoctorQueueActivity.kt`, `native/PatientTimelineActivity.kt`, `native/ReportCardActivity.kt`, `native/ClinicalModulesActivity.kt`.

| অভিযোগ | কোড আসলে কী করে | আসল কারণ | Fix কোথায় |
|---|---|---|---|
| Check-up খুলতে দেরি | `DoctorQueueActivity.openClinical()` L321-346 — screen খোলার **আগে** blocking cloud call `AddressTagRepository.fetchDemographics()` L327-330, তারপর `startActivity` L344 | নেটওয়ার্ক demographics fetch শেষ হওয়া পর্যন্ত অপেক্ষা | `openClinical()` — cache দিয়ে আগে খুলে background-এ fetch |
| Journey/Action খুলতে দেরি | Queue `PatientTimelineActivity` খোলে **`pre*` extras ছাড়াই** L92-99; `paintInstantHeader()` preStage ফাঁকা হলে return করে L2187-88 → instant header নেই, "Loading..." L2356 | Queue থেকে instant-header data পাঠানো হয় না (Follow-up পাঠায়, তাই দ্রুত) | `DoctorQueueActivity` onFullJourney/onAction — `pre*` extras পাঠানো |
| Action-এর পর একবার Back → Patient Details-এ আটকায় | Stack: Queue → PatientTimeline(Details) → sub-screen; সব Back = `finish()` L145; কোনো `CLEAR_TOP` নেই; action menu পুরো cloud load-এর পরে খোলে L2652 | মাঝের Details activity terminal action চালানোর সময় finish হয় না | `PatientTimelineActivity.showTakeActionMenu()` launch site-গুলো |
| Report আলাদা loading screen | `ReportCardActivity.load()` L88-106 — "Loading..." L93-100 তারপর full network build, **কোনো local cache নেই** | cache-first path নেই | `ReportCardActivity.load()` — cache-first render |
| Scroll/Branch একই রাখা | branch = instance field (survives normal back), কিন্তু scroll কোথাও save/restore হয় না; `onResume` L134-139 প্রতিবার `loadQueue()` | scroll preservation **implement করা নেই** | `DoctorQueueActivity` scroll save/restore |

Queue-এর তালিকা নিজে local-first (`loadCachedQueue` L233-243 তারপর background) — ভালো। সমস্যা শুধু Journey/Action/Report সাব-স্ক্রিনে।

---

## SECTION 11 — Follow-up navigation/loading

মূল file: `native/FollowUpActivity.kt`, `native/PatientTimelineActivity.kt`, `native/PaymentActivity.kt`, `native/RegistrationActivity.kt`।

- **Blue Eye → Details:** `openTimelineFor()` L2154-70 সব `pre*` extras পাঠায় → header instant। বাকি ধীরগতি timeline body-র network build-এ (L2372)। **আংশিক সত্য** (header দ্রুত, body ধীর)।
- **Action → Payment "Opening…":** `PaymentActivity.searchAndOpenPaymentForm()` — toast "Opening…" L860 তারপর blocking `findPatientByMobile()` L867, তারপর form L873। **সত্য, design-গত** (bill/paid cloud থেকে আনে, cache নেই)।
- **Report loading:** §10-এর মতোই `ReportCardActivity`। সত্য।
- **একবার Back → Follow-up-এ ফেরে না (Register/Payment থেকে):** Stack FollowUp → Timeline(Detail) → Register(L528)/Payment(L560); মাঝের Detail finish হয় না। **সত্য।**
- **Branch/Tab/Filter/Scroll:** tab/filter/branch instance field → normal back-এ টেকে; `onResume` L672 একই stage/filter দিয়ে reload → **visually preserved**। কিন্তু **scroll** নির্ভরযোগ্যভাবে রাখা হয় না (Visit/Patient ট্যাব plain ScrollView L1164-68, rebuild-এ reset)।

---

## SECTION 12 — Action menu থেকে Remark সরানো  ✅ সহজ, নিরাপদ

**শুধু একটি লাইন সরাতে হবে:** `PatientTimelineActivity.kt:524` —
`actionRow("📝", "Remark", …) { … showQuickRemarkDialog() }` — এটাই একমাত্র "Remark" যা "⚡ Take Action" popup-এর ভিতরে আছে।
**যা অপরিবর্তিত থাকবে (আলাদা Remark button/flow):** `PatientTimelineActivity.showQuickRemarkDialog()` L1290-1338; `DoctorVisitActivity.showRemarkOnlyEdit()` L477-575; `FollowUpActivity.showRemarkDialog()` L3321-3363 (dashed "Last Remark" box)। FollowUp-এর action popup (`entryActionMenu` L3457)-এ Remark নেই — ওখানে কিছু সরাতে হবে না।

---

## SECTION 13 — Refund / টাকা ফেরত (নতুন feature — এখন নেই)

- **বর্তমানে কোনো refund-transaction নেই।** `RefundedRecords.kt` L45-58 শুধু *display filter* — যে mobile-এর সব follow-up Cancelled, তার পুরো টাকা total থেকে বাদ দেয়; আংশিক refund বা refund entry লেখে না। Web-ও একই (`app.js:1185`)।
- **Payment model (`PaymentModel.kt`):** এক payment = `payments` table-এ এক JSON row। Installment = আলাদা row (array নয়); Advance/2nd label দিন-ভিত্তিক হিসাব হয় (`dayBasedLabelById` L168-184)। শূন্য-amount "audit row" প্যাটার্ন আছে (`buildAttendanceMarkRow` L332, `buildBillEditRow` L357) — **refund row-এর জন্য সবচেয়ে পরিষ্কার template**।
- **Total হিসাব:** per-patient paid total শুধু `isTreatmentPaymentRow` যোগ করে (`PaymentRepository` L477-491) — **visit fee এমনিতেই এর বাইরে**, তাই treatment থেকে refund কাটলে visit fee ঠিক থাকবে (§13-এর নিয়মের সঙ্গে মেলে)। কিন্তু collection fetch-এ `if(amount>0)` guard আছে (L197/237/333/344) → **negative-amount refund row চুপচাপ বাদ পড়বে** — এটা মাথায় রেখে বানাতে হবে।
- **Approval template তৈরি আছে:** `DeletePermission.sendRequest()`/`approveAndDelete()`, `PaymentModel.BackdateRequest` + `requestBackdatePayment`/`approveBackdateRequest` — staff→master approval-এর প্রমাণিত নমুনা। Refund request একইভাবে `briefings` টেবিল দিয়ে master-এর ঘণ্টায় পাঠানো যাবে।

**অর্থাৎ §13 বানানো যাবে, কিন্তু এটা নতুন feature — ছোট edit নয়। সাবধানে design + build + device test দরকার।**

---

## SECTION 14 — Same-day delete → Trash / Restore  (আংশিক আছে, ফাঁক আছে)

- **Soft-delete (Trash), permanent নয়** — মূল record-এর জন্য সত্য (`TrashHelper.moveToTrash` L82)। Permanent শুধু Master-only "Delete Forever" (`TrashRepository.permanentDelete` L113)।
- **"সব linked record একসঙ্গে Trash-এ" — আক্ষরিকভাবে সত্য নয়:** শুধু মূল `patients`/`enquiries` row trash-এ যায়; follow-up/enquiry শুধু `status=Cancelled` হয় (L246,338); **payment/medical/photo কখনো সরে না বা trash-এ যায় না** — তারা table-এ থেকেই যায়।
- **Restore কাজ করে** (`TrashRepository.kt:71`) — কারণ linked data আসলে সরানোই হয়নি।
- **⚠️ ফাঁক:** trash-এ থাকা patient-এর payment এখনো `payments` table-এ, mobile-ভিত্তিক report/collection-এ গোনা হতে পারে (`PaymentRepository`-তে trashed patient বাদ দেওয়ার filter পাওয়া যায়নি)।
- **⚠️ নিয়মের সঙ্গে সংঘর্ষ:** §14 চায় staff একই দিনে approval ছাড়া delete করুক; কিন্তু UI gate `DeletePermission.canDeleteNow()` (`PatientTimelineActivity.kt:1561`) staff-কে master approval-এ পাঠায়। `TrashHelper.canDelete` L71-77-এর same-day নিয়ম UI-তে ব্যবহার হয় না।

---

## SECTION 16 — Incomplete Patient তালিকা থেকে সরছে না  (সত্য)

- Incomplete করলে `FollowUpRepository.updateStatus()` — cloud fail করলেও `return cloudOk || context != null` (`FollowUpRepository.kt:1949`) → **context সবসময় non-null, তাই cloud fail করলেও `true` ফেরত** → caller "Saved" দেখায় (`PatientTimelineActivity.kt:1468`)। **মিথ্যা "Saved" — §16.7-এর সরাসরি বিপরীত।**
- সফল হলে `load(currentMobile,...)` — **একই Timeline screen reload করে, `finish()` করে না** (L1471) → "patient screen একই জায়গায় ছিল" ঠিক এই কারণে।
- তালিকা-filter নিজে ঠিক আছে: active query `status=not.in.(Cancelled,Incomplete)` (`FollowUpRepository.kt:541`), `FollowUpActivity` `onResume`-এ reload করে — তাই ফিরে গেলে সরে যায়, কিন্তু Timeline থেকে পরিষ্কার বোঝা যায় না।
- **Fix:** `updateStatus` সত্যিকারের cloud-confirmed success ফেরাবে; Incomplete-এর পর `finish()` করে source list-এ ফিরবে; confirmed না হলে "Saved" বলবে না।

---

## SECTION 17 — Follow-up Call Signal / Last Call Date বাড়ছে না  (সত্য, স্পষ্ট কারণ)

- Remark save path: `showRemarkDialog` → `updateRemark(id, remark, staff, incrementCall)` যেখানে **`incrementCall = (item.stage == "Inquiry")`** (`FollowUpActivity.kt:3343`)।
- `updateRemark`-এ count/lastCall শুধু `if (incrementCall && haveRow)` ব্লকে লেখা হয় (`FollowUpRepository.kt:1777-1790`)।
- **আসল কারণ:** Jalpaiguri staff যে patient-এর সঙ্গে কথা বলেছেন তিনি **Visit (stage=Patient)** বা **Patient (stage=Treatment)** ট্যাবে → `incrementCall=false` → remark save হয় কিন্তু `callCount`/`lastCallDate` **কখনো touch হয় না**। এটাই লক্ষণ (23.07.2026 আটকে ছিল)।
- এক-দিনে-এক-call de-dup ইতিমধ্যে ঠিক আছে (`FollowUpRepository.kt:1788`) — কিন্তু শুধু যে path গোনে সেখানে।
- signal meter শুধু Inquiry card-এ আঁকা হয় (`FollowUpActivity.kt:1245`); patient card-এ meter-ই নেই।
- **Fix:** Visit/Patient stage-এও post-call remark save হলে সেটাকে completed call ধরে count/lastCall বাড়ানো। **নিশ্চিত নয়:** ঐ নির্দিষ্ট record কোন stage-এ ছিল — কিন্তু non-Inquiry stage-এ গঠনগতভাবে count অসম্ভব, যা লক্ষণের সঙ্গে মেলে।

---

## SECTION 18 — Draft/Visit-Reject delete করলেও তালিকায় থেকে যায়  (সত্য — তিনটি কারণ একসাথে)

1. **Visit Reject তালিকা** = `followups` যেখানে `stage=Patient && status=cancelled` (`DraftRepository.kt:489-490`)।
2. Delete চালায় `moveToTrashWithFollowupCascade("patients",…)` — কিন্তু cascade লুপ **আগে থেকেই Cancelled follow-up row skip করে**: `if (status=="Cancelled") continue` (`TrashHelper.kt:246`)। Visit-Reject record নিজেই একটা Cancelled row → তার follow-up row কখনো সরে/trash হয় না, শুধু `patients` row trash হয়। তাই fresh load-এ আবার আসে, count কমে না।
3. **DraftListActivity stale snapshot** — `entries` Intent দিয়ে serialized আসে (L51-52), `onResume` reload নেই; pull-to-refresh শুধু `renderList()` করে, cloud re-fetch করে না (L75-80)।
4. **DraftRepository DeletedGuard filter করে না** (FollowUpRepository করে L213,241) — tombstoned id-ও বাদ পড়ে না।
- **Fix:** পুরো record delete-এ ঐ Cancelled follow-up row-ও trash/tombstone করা; `DraftRepository` DeletedGuard filter; `DraftListActivity` pull-to-refresh + `onResume`-এ সত্যিকারের re-fetch।

---

## SECTION 6 — Offline / Sync / মিথ্যা "Saved"  (সত্য, গঠনগত)

- Remark/Next-follow toast **cloud ack-এর আগে** দেখায় (`FollowUpActivity.kt:3354, 3389`), আর repo return করে `x || context != null` (`FollowUpRepository.kt:1817, 1837, 2000, 2009`) → screen সবসময় success ভাবে।
- **ভালো দিক:** `updateRemark`-এ এখন read-back verify আছে (L1805-1815) এবং `updateById` "matched no row" ধরে (`SupabaseClient.kt:432-438`); দুই queue আছে — feature-queue (id দিয়ে dedup) ও global `CloudWriteQueue` (`kind|table|id` dedup, 50 try তারপর `failed`, id-ভিত্তিক idempotency, deleted rows skip)। মোটামুটি মজবুত।
- **ফাঁক:** user-facing "Saved" toast এখনো cloud-confirm-এর আগে; read-back শুধু retry queue করে, user-কে যা বলা হয়েছে তা বদলায় না। `CloudWriteQueue.flush()`-এ in-flight guard নেই (তবে idempotent বলে duplicate পাঠানো ক্ষতিকর নয়)।

---

## SECTION 15 — Briefing/Notice Notification  (sound/vibration আছে, near-realtime নেই)

- **কোনো FCM/Firebase নেই** (build.gradle/manifest-এ শূন্য hit; কোনো `<service>` নেই, VIBRATE permission নেই — শুধু POST_NOTIFICATIONS L8)।
- সব request `briefings` টেবিলে row লিখে master-কে পাঠানো হয় (`BriefingRepository.post` L89-95)।
- **Local notification + sound + vibration আসলে আছে:** `BellNotifier.notify()` L41-66 — PRIORITY_HIGH + DEFAULT_SOUND|DEFAULT_VIBRATE, শুধু bell count বাড়লে; channel `NoticeChannels.ensure()` L35-62 vibration+sound সহ।
- **আসল দুর্বলতা:** `BellNotifier.onCount()` ডাকা হয় শুধু (a) Dashboard খুললে, আর (b) `CallReminderWorker` — **দিনে ৩ বার (১০টা/১২টা/২টা)**। 15-মিনিটের `BackgroundRefreshWorker` list refresh করে কিন্তু `BellNotifier` **ডাকে না**। ফল: নতুন request-এ near-realtime sound আসে না।
- **Fix:** `BackgroundRefreshWorker`-এ সস্তা count-check করে `BellNotifier` ডাকা (Free-plan বাঁচিয়ে); badge count-only query করা। Push সত্যিই লাগলে FCM = **external manual setup** (§15.10 অনুযায়ী আলাদা লিখতে হবে, "fully working" দাবি করা যাবে না)।

---

## SECTION 4/5/8/9 — Security, Data Integrity, Parity  ⚠️ সবচেয়ে গুরুত্বপূর্ণ

### 🔴 CRITICAL — 1: Password সব জায়গায় খোলা (plaintext)
- Web `config.js:39` — `passwords:{master:'admin123',staff:'staff123',doctor:'doctor123',field:'field123'}` — এটা public static file, browser-এ যে কেউ দেখতে পায়।
- আবার `app.js:3824-3827, 4436-4439, 4551-4554`-এ hardcoded।
- Android `StaffDirectory.kt:68-73` — একই password APK-এর ভিতরে।
- Login = plain string ম্যাচ, কোনো hashing নেই (`LoginActivity.kt:94-97`; web `app.js:1108,4498`)।
- DB `usercredentials.password` = plaintext column, RLS off (`PILES_CLINIC_DB_SETUP.sql:223-234`)।

### 🔴 CRITICAL — 2: RLS প্রতিটা টেবিলে বন্ধ
- `PILES_CLINIC_DB_SETUP.sql` line 26,67,90,114,136,159,181,199,208,221,234,249 — সব টেবিলে `disable row level security`।
- **ফল:** anon key (নিচে) দিয়ে যে কেউ সব branch-এর patient/payment/password/trash সরাসরি পড়তে-লিখতে-মুছতে পারে। App-এর role/branch নিয়ম শুধু client-side, curl দিয়ে bypass হয়।

### 🟠 3: Supabase anon key উন্মুক্ত (RLS off থাকায় বিপজ্জনক)
- `config.js:5-6` ও `SupabaseClient.kt:16-17` — `sb_publishable_…` (anon key, service key নয়)। RLS চালু থাকলে anon key public থাকা স্বাভাবিক; কিন্তু RLS off বলে এটা কার্যত public read/write।

### 🟠 4: DB-তে কোনো UNIQUE/Foreign Key নেই
- প্রতিটা টেবিলের একমাত্র constraint `id text primary key`; `patients.mobile`/`patientId`-এ UNIQUE নেই, `payments.patientId → patients.id` FK নেই।
- Duplicate patient / orphan payment শুধু app code আটকায় (best-effort; network fail-এ empty set → দুই ফোনে collision সম্ভব — `PatientIdGenerator.kt:34-49,84-88`)। code comment-এ আসল ঘটনা লেখা আছে (দুই branch-এ ₹10,000 cross-post — `PaymentRepository.kt:414-437`)।

### 🟡 5: Web parity / header
- `assets/www/config.js`-এ Cooch Behar নম্বর এখনো ভুল (Falakata-র `8514001100`); Netlify `config.js:10`-এ ঠিক (`8514002200`)। (assets/www অব্যবহৃত — LoginActivity.kt:14-24 — তাই প্রভাব কম, তবু ঠিক করা উচিত।)
- **কোনো security header নেই** — `03_NETLIFY_READY/`-তে `_headers`/`netlify.toml` নেই; index.html-এ CSP/X-Frame-Options নেই।
- `app.js`-এ duplicate function: `login()` L1085 ও L4475; `dashboard()` L1417 ও L4898; `defaultPasswordFor` L3824 ও L4432 — পরেরটা জেতে, আগেরটা dead (maintenance বিপদ)।

**⚠️⚠️ অত্যন্ত জরুরি সতর্কতা (সততা):** RLS **এখনই চালু করে দিলে** live app **সঙ্গে সঙ্গে বন্ধ** হয়ে যাবে — কারণ সব read/write ঐ anon key দিয়েই হয়, RLS চালু হলে anon key আটকে যাবে। তাই RLS ঠিক করতে হলে **আগে Supabase Auth-এ সরানো** দরকার — এটা বড়, ধাপে-ধাপে, live-এ সাবধানে করার কাজ। এই session-এ RLS "চালু করে দেওয়া" SQL হাতে ধরিয়ে দেওয়া হবে **না**, কারণ তা tested না হয়ে live clinic ভেঙে দিতে পারে।
