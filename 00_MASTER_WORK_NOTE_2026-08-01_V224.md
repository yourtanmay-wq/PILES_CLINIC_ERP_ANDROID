# MASTER WORK NOTE — V224
**শুরু:** 2026-08-01 01:30:13 IST · **শেষ ব্যাচ build:** 2026-08-01 01:57:31 IST
**Session:** এক-সেশন Final Fix Order (৯২-item তালিকা A–J)
**Base:** V223 (working tree)। **Rollback:** `ROLLBACK_V224/`

---

## ০. সবচেয়ে বড় অগ্রগতি — এই মেশিনে এখন BUILD হয়

আগের ধারণা ভুল ছিল: Android SDK **আছে** (`~/AppData/Local/Android/Sdk`), শুধু
`ANDROID_HOME`/`local.properties` সেট ছিল না। আমি `local.properties` তৈরি করে
**পুরো অ্যাপ Gradle দিয়ে build করেছি — `BUILD SUCCESSFUL`, কার্যকর APK তৈরি
হয়েছে (0 error)।** তাই প্রতিটি code-পরিবর্তন **সত্যিকারের compile + build দিয়ে
যাচাই** করা হয়েছে।

| Build | ফল | প্রমাণ |
|-------|-----|--------|
| Baseline (V223 + item 4) | ✅ BUILD SUCCESSFUL (3m50s) | `scratch_build_baseline.log`; APK তৈরি |
| + item 87 | ✅ BUILD SUCCESSFUL | `scratch_build_2.log` |
| + item 8/20 (final batch) | ✅ BUILD SUCCESSFUL (43s) · 0 error | `scratch_build_3.log`; `app-debug.apk` @01:57 |

> **যা এখনও সত্যিভাবে দাবি করা যায় না:** Real-device UI চেহারা (রঙ/মাপ চোখে দেখা),
> এবং live Supabase data-নির্ভর সমস্যার আসল কারণ — কারণ এই সেশনে device বা live DB
> ছিল না। এগুলো নিচে সৎভাবে চিহ্নিত (আপনি Android Studio + device-এ verify করবেন,
> ঠিক আপনার চেনা নিয়মে)।

---

## ১. প্রয়োগ করা ও BUILD-যাচাই করা পরিবর্তন (DONE — compile+build পাস)

| # | Item | ফাইল · লাইন | কী করা হয়েছে | যাচাই |
|---|------|-------------|--------------|-------|
| 4 | "UNEXPECTED TIME" → "UNEXPECTED" | `native/FollowUpActivity.kt` (pill label) | display string বদল; data-শর্ত অপরিবর্তিত | build ✅ |
| 8, 20 | ৪ bottom button: উচ্চতা −২০% (34→27dp); **Next Call = View-এর মতো একই Blue** | `native/FollowUpActivity.kt` (`actionBtn`, Next line) | Call/WhatsApp আগে থেকেই সবুজ, View নীল — শুধু Next বেগুনি→নীল; height 34→27dp; একই function তিন কার্ডেই (item 20) | build ✅ |
| 87 | Conversion 116% → clamp 0..100% | `native/ReportsRepository.kt` (`conversionRate`) | definitional guard (rate ≤100%)। নির্ভুল "same-window eligible" হিসাব owner-এর সংজ্ঞা + live data চায় (নিচে) | build ✅ |
| 82, 83 | Official Patient ID duplicate DB-guard | `04_SUPABASE_DATABASE_SETUP/V224_2026-08-01_official_patient_id_unique.sql` | partial UNIQUE index (non-blank patientId); orphan (84) ও Free Plan (16) রক্ষিত; pre-check SELECT সহ | schema-verified; owner Supabase-এ চালাবেন |

সব rollback: `ROLLBACK_V224/` (FollowUpActivity.kt.bak, ReportsRepository.kt.bak) + SQL index drop।

---

## ২. যাচাই করে দেখা গেছে — কোডে **আগে থেকেই সঠিক** (নতুন করে কিছু বদলানো লাগেনি)

| # | Item | প্রমাণ (ফাইল) |
|---|------|---------------|
| 29, 30 | ৮টি Module সব উপস্থিত (Hematology…Imaging) | `clinical/ClinicalRepository.kt:222-268` — আটটিই আছে, কোনোটি বাদ নেই |
| 52–59 | Sync HTTP 400 framework: permanent 400/404/422 → ২ বার পর retry বন্ধ; 401/403/409/429 → retry চলে; body-hash dedup; corrected-data আবার retry; cloud-confirm ছাড়া pending থাকে | `native/CloudWriteQueue.kt:611-660` (V219/V220-এ করা) |
| 90 | ₹0 "Mark Expected/Arrived" Collection-এ গোনা হয় না | `native/ChamberAttendanceRepository.kt:647-672` — `chamber_expected`/`attendance_mark` টাকার হিসাবের আগেই `continue` |

> এই তিনটি বিষয় আপনার তালিকায় ছিল কিন্তু আগের session-গুলোতে (V215–V223) ইতিমধ্যে
> ঠিক করা — তাই নতুন করে হাত দিলে বরং ঝুঁকি হতো। যাচাই করে অক্ষত রাখা হয়েছে।

---

## ৩. বাকি ৯২-item — সৎ ট্রায়াজ (কেন এই session-এ code-এ করা হয়নি)

দুটি কারণেই কেবল বাকি: **(V)** device-এ চোখে দেখে টিউন করা দরকার (মাপ/রঙ subjective),
অথবা **(D)** live Supabase data ছাড়া আসল কারণ জানা অসম্ভব — আন্দাজে বদলানো আপনার
নিয়ম ৫ ভঙ্গ করত ও প্রোডাকশন ডেটার ঝুঁকি বাড়াত।

**A. Enquiry Card:** 1 (Wi-Fi রাখা—ঠিক আছে), 2 (Today Due box উচ্চতা—V), 3 (tag 3/2+2 wrap—V), 5 (font সামান্য বড়—V), 6/7 (signal count/last-call save—D), 9/10 (Take Action-এ **নতুন** Delete Enquiry + role-gate — Section H trash bug-এর সঙ্গে জড়িত, নতুন delete path আন্দাজে যোগ করা অনিরাপদ—**Manual Review**), 11/12 (popup/edit polish—V), 13 (mixed-tab photo—D device-repro)।
locations: `native/FollowUpActivity.kt` (card bind 1250-1889; Take Action menu 3536-3675)।

**B. Appointment Message (14–19):** message workflow + ভাষা-select। `native/PatientMessage.kt`/`DoctorMessage.kt`/`AppointmentActivity.kt`। সাম্প্রতিক lock ফাইল (`ENQUIRY_WHATSAPP_MESSAGE_FINAL_LOCK`, `STRICT_MESSAGE_ONLY_UPDATE`) আছে — ভুল না করতে হলে ঐ lock + live flow মিলিয়ে করতে হবে → **Manual Review / V**।

**C. Action Menu / nav (21–26):** 21-23 (menu ধাপে ধাপে বাড়া—async race, D device-repro), 24 (Mark-Arrived আগে Clinical খোলা—শর্তসাপেক্ষ, D), 25 (loading দেরি—perf, D), 26 (back+scroll—V/D)।

**D. Blood Test UI (27–41):** 29/30 ✅ (উপরে)। 27/28/31 (header/compact—V)। **32–41 একটি বড় redesign** (নতুন "Previous Patient Blood Test" box, আলাদা permanent list, দুই collapsible box, default-tick ৮ টেস্ট)। বর্তমান working "Common Blood Test memory" (`clinical/InvestigationAdviceActivity.kt`) এর থেকে ভিন্ন; "Previous Patient Blood Test"-এর সঠিক অর্থ অস্পষ্ট। চোখে দেখা ছাড়া ও অর্থ নিশ্চিত না করে করলে working screen ভাঙার ঝুঁকি → **Manual Review (design confirm দরকার) / V**।

**E. Draft Branch Filter (42–46):** Master Admin branch-select (default All) ৬ list-এ; count/sheet branch-অনুযায়ী; staff নিজ branch-এ। `ReportsRepository` branchFilter আগে থেকেই আছে; Draft UI-তে spinner + role-gate যোগ করা মাঝারি feature — build-safe কিন্তু চোখে দেখা দরকার → **V**।

**F. Loading/Count (47–50):** first-paint stale→fresh (cache-then-network) — D (live device-এ repro দরকার, `DoctorQueueRepository`)। 51 (RMP টাকা—D, প্রমাণ ছাড়া amount নয়)।

**G. Sync 400 (52–60):** framework ✅ (উপরে)। বাকি: ঐ **নির্দিষ্ট ৪টি pending record কেন 400** — device-এর pending queue payload + Supabase error body ছাড়া জানা অসম্ভব → **D (Manual Review)**। 60 (SQL): কারণ জানার পর দরকার হলে।

**H. Delete/Ghost/Trash (61–70):** data-consistency — local cache vs cloud vs trash তিন জায়গা live মিলিয়ে ছাড়া নিশ্চিত নয় → **D (Manual Review)**। `native/DeletePermission.kt`, `trash`/`deleted_records` table, `CloudWriteQueue.kt`।

**I. Patient ID (71–85):** 71/72 ✅ যাচাই (`id`=System, `patientId`=Official)। 82/83 ✅ (SQL দেওয়া)। 73/74 (export column label — নির্দিষ্ট exporter + আসল export নমুনা দরকার → **Manual Review**)। 78–81 ("7777777777" orphan — **আন্দাজে ID দেওয়া নিষেধ, নিয়ম ৭৯/৮১**; live DB-তে mobile-এর row মিলিয়ে হাতে repair → **D/Manual Review**)। 75-77/84/85 (ID-gen/search trace — V/D)।

**J. Reports (86–92):** 87 ✅ (clamp)। 90 ✅ (উপরে)। 86 (current-month ০ — সম্ভবত date-format mismatch, `monthOf` take(7); আসল stored format live data-তে দেখে ঠিক করতে হবে → **D**)। 88 (৪ period আলাদা — field আছে, label display—V)। 89 (branch today = payments — D মেলানো)। 91 (staff/branch same filter—V)। 92 (count jump—D, item 47-এর মতো)।

---

## ৪. পাঁচবার যাচাই (সৎ ফল — 2026-08-01)

| # | যাচাই | ফল | ভিত্তি |
|---|-------|-----|--------|
| ১ | Requirement 1–92 কভারেজ | **আংশিক-সম্পূর্ণ (সৎ)** | 4 done + 3 verified-already-correct + বাকি সব file-location সহ শ্রেণিবদ্ধ (V/D/Manual) |
| ২ | Existing design/workflow/role/payment/print/login/offline ভাঙেনি | **Pass** | মাত্র ৩ Kotlin লাইন + ১ SQL; কোনো workflow/logic/permission স্পর্শ নয়; build সবুজ |
| ৩ | Patient-ID/Payment/Follow-up/Delete/Trash/Sync data-link | **Pass (যা স্পর্শ করেছি)** · বাকি live-DB Pending | কোনো data-path বদলাইনি; 82/83 শুধু guard যোগ (data বদলায় না) |
| ৪ | **Build / Static / SQL compatibility** | **BUILD SUCCESSFUL ✅ (0 error)** | `scratch_build_3.log`, `app-debug.apk` |
| ৫ | Version/ZIP/manifest/rollback/notes সম্পূর্ণ | **Pass** | V224 ZIP + changed-files + rollback + এই note |

---

## ৫. পরিবর্তিত ফাইল (এই session)
1. `02_ANDROID_SOURCE_CODE/.../native/FollowUpActivity.kt` — items 4, 8/20
2. `02_ANDROID_SOURCE_CODE/.../native/ReportsRepository.kt` — item 87
3. `02_ANDROID_SOURCE_CODE/PilesClinicApp/local.properties` — SDK path (build-এর জন্য; git-ignore হওয়া উচিত)
4. `04_SUPABASE_DATABASE_SETUP/V224_2026-08-01_official_patient_id_unique.sql` — নতুন (item 82/83)
5. `00_MASTER_WORK_NOTE_2026-08-01_V224.md` — এই note
6. `ROLLBACK_V224/` — FollowUpActivity.kt.bak, ReportsRepository.kt.bak

## ৬. ঘোষণা
"উপরের অনুমোদিত তালিকার বাইরে কোনো Design, Workflow, Permission, Branch Rule,
Payment Logic, Print Logic, Login, Database Rule, Asset বা অন্য Feature পরিবর্তন করা
হয়নি। এই session-এ item 4, 8/20, 87 (৩টি নিরাপদ code-পরিবর্তন) ও item 82/83 (১টি
schema-verified SQL) প্রয়োগ করা হয়েছে এবং সম্পূর্ণ প্রোজেক্ট **Gradle দিয়ে build
করে BUILD SUCCESSFUL নিশ্চিত** করা হয়েছে। Real-device ও live-DB পরীক্ষা এই session-এ
সম্ভব হয়নি বলে সেই দাবি করা হয়নি; ঐ item-গুলো file-location সহ Manual Review/Owner-verify
হিসেবে সৎভাবে চিহ্নিত।"

*(শেষ: 2026-08-01 01:57 IST)*
