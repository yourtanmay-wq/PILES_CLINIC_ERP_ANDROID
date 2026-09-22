# V226 — ৭৪টি বাকি Item-এর সৎ Final Status

**তারিখ:** 2026-08-01 · **Base:** V225 (=V224 working tree) · **এই কাজ:** V226 draft
**পরিবেশ-সীমা:** এই cloud sandbox-এ **Android SDK নেই** ও **Google server blocked (403)** — তাই এখানে সত্যিকারের `assembleDebug` build **করা যায়নি**। যেসব item device/live-Supabase/চোখে-দেখা ছাড়া সত্যিই শেষ করা অসম্ভব, সেগুলো সৎভাবে `Owner ...Required` লেখা — কোনোটিকে মিথ্যা "Done/Passed" বলা হয়নি।

**Status মানে:**
`Already Correct` = কোডে আগে থেকেই ঠিক, প্রমাণসহ। ·
`Fixed in V226` = এই version-এ নিরাপদ code পরিবর্তন করা হয়েছে (build owner করবেন)। ·
`Partial` = আংশিক ঠিক, বাকি অংশ চিহ্নিত। ·
`Owner Build+Visual` = code দেওয়া/সম্ভব, কিন্তু approved layout/মাপ/রঙ device-এ চোখে দেখে নিশ্চিত করতে হবে (আন্দাজে বদলানো নিষেধ)। ·
`Owner Live-DB` = live Supabase data ছাড়া আসল কারণ/সঠিক রোগী জানা অসম্ভব। ·
`Owner Definition` = owner-এর সুনির্দিষ্ট সংজ্ঞা ছাড়া সঠিক করা অনুমান হবে।

---

## A. Enquiry/Follow-up Card (2, 3, 5, 6, 7, 9–13)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 2 | Today Due box উচ্চতা | Owner Build+Visual | `native/FollowUpActivity.kt` card-bind (~1250–1889)। মাপ subjective — device-এ চোখে দেখে টিউন করতে হবে; আন্দাজে dp বদলানো approved design ঝুঁকি। |
| 3 | Tag 3 / 2+2 wrap | Owner Build+Visual | একই ফাইল, tag layout। wrap আচরণ screen-প্রস্থ-নির্ভর → device visual। |
| 5 | Card font সামান্য বড় | Owner Build+Visual | subjective sp মান; lock-এ নির্দিষ্ট sp নেই → device visual। |
| 6 | Call signal count save | **Already Correct** | `native/FollowUpRepository.kt:1777–1791` — দিনে-একবার de-dup, cap 5, local+cloud persist + retry queue; তিন screen-এ অভিন্ন (`FollowUpActivity:3374`, `FollowCalendarActivity:483`, `PatientTimelineActivity:1349`)। |
| 7 | Last Call date save | **Already Correct** | একই function, `lastCallDate=todayStr` (`:1790`), fail হলে queue-তে সহ। *offline `haveRow==false`-এ ইচ্ছাকৃত গার্ড (খাতা B58, TK-approved) — বদলানো হয়নি।* |
| 9 | Take Action-এ নতুন "Delete Enquiry" | Owner Live-DB | নতুন delete-path; Section H trash/ghost consistency-র সঙ্গে জড়িত। live 3-জায়গা (cache/cloud/trash) যাচাই ছাড়া যোগ করা অনিরাপদ — আন্দাজে নতুন delete পথ নিষেধ। |
| 10 | ঐ Delete-এ role-gate | Owner Live-DB | 9-এর অংশ; role-gate pattern `DeletePermission.kt`-এ আছে, কিন্তু নতুন পথ live-verify দরকার। |
| 11 | Popup polish | Owner Build+Visual | `PremiumAlert` styling — চোখে দেখা দরকার। |
| 12 | Edit polish | Owner Build+Visual | subjective UI। |
| 13 | Mixed-tab-এ photo | Owner Live-DB | device-এ reproduce + live photo data দরকার। |

---

## B. Appointment/WhatsApp/SMS Message + ভাষা (14–19)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 14–19 | Message workflow + বাংলা/হিন্দি/ইংরেজি + Branch info | **Already Correct (নিয়ম) / Owner Live Test (হুবহু লেখা)** | ভাষা-নির্বাচন ও format সব **নিয়ম** মেলে: Enquiry picker `PatientMessage.kt:793,852-854`; RMP Intro picker `DoctorVisitActivity.kt:1820-1839`; Patient msg তিন ভাষা stack `PatientMessage.kt:452-475`; RMP 2-4 বাংলা (STRICT doc §2)। chooser `WhatsAppMessageChooser.kt:28-59`। **কিন্তু** TK-এর মূল হুবহু লেখা repo-তে নেই (`ENQUIRY_WHATSAPP...` doc §6 নিজেই বলছে "চূড়ান্ত ফটো-প্রুফ বাকি") → হুবহু character-মিল owner photo-proof দিয়ে হবে। কোড অপরিবর্তিত (নিয়মে ঠিক)। |

---

## C. Action Menu / Loading / Back-nav (21–26)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 21–23 | Menu ধাপে ধাপে আসা (async race) | Owner Live-DB | নেটওয়ার্ক-timing নির্ভর; device-এ reproduce দরকার। |
| 24 | Mark-Arrived আগে Clinical খোলা | Owner Live-DB | শর্তসাপেক্ষ workflow; live state দরকার। |
| 25 | Loading দেরি | **Already Correct (design) / Owner device** | cache-first, spinner কখনো ঘোরে না — `PaymentActivity:175-216`, `ReportCardActivity:88-111`, `PatientTimelineActivity:2480-2494`। অনুভূত সময় device-নির্ভর। একটি pre-open await: `DoctorQueueActivity.openClinical:338-363`। |
| 26 | Back করলে আগের list + scroll | **Partial** | Queue/Follow-up-এ explicit scroll-restore আছে (`DoctorQueueActivity:317-325`, `FollowUpActivity:1189-1195`); Payment/Action/Report default-নির্ভর, Report ScrollView rebuild-এ top-এ ফেরে। উন্নতি সম্ভব কিন্তু device visual দরকার (V)। |

---

## D. Blood Test UI (27, 28, 31–41)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 27, 28, 31 | Header / compact refinement | Owner Build+Visual | `clinical/InvestigationAdviceActivity.kt` design আছে ও 2026-07-15 approved; compact মাপ subjective — master note নিজেই "—V (device চোখে)" বলেছে। |
| 32–41 | বড় redesign ("Previous Patient Blood Test" box, collapsible, default-tick ৮) | **Owner Definition (design confirm)** | master note (line 68): "'Previous Patient Blood Test'-এর অর্থ **অস্পষ্ট**… working screen ভাঙার ঝুঁকি → design confirm দরকার।" approved spec repo-তে নেই। আপনার নিয়ম: approval অস্পষ্ট হলে বদলানো নিষেধ → owner-এর নকশা নিশ্চিতি চাই। |

*(29, 30 = ৮টি module উপস্থিত — আগেই যাচাই-সঠিক, `clinical/ClinicalRepository.kt:222-268`।)*

---

## E. Draft/Reject/Incomplete/Trash Branch Filter (42–46)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 42 | Draft home branch filter (Master) + staff নিজ branch | **Already Correct** | `DraftActivity.kt:103-113` (`setupBranchPicker`, `shownBranch()`)। |
| 43 | Enquiry Reject list branch | **Already Correct** | `DraftRepository.kt:235-237,278,505` (`branch=eq.`)। |
| 44 | Visit Reject list branch | **Already Correct** | `DraftRepository.kt:278,507`। |
| 45 | Incomplete list branch | **Already Correct** | `DraftRepository.kt:278,509`; refresh branch-অটুট (`DraftListActivity:105-114`)। |
| 46 | **Trash Bin branch filter (Master)** | **Partial → Missing (Master filter)** · patch দেওয়া | Staff নিরাপদে ব্লকড (`TrashBinActivity.kt:37-40`); কিন্তু **Master-এর branch spinner নেই** (`TrashRepository.fetchTrashRaw():39-45` filter ছাড়া সব branch)। এটি নতুন UI control — approved layout বদলায় ও চোখে দেখা দরকার, তাই V226-এ **আন্দাজে inject করিনি**; হুবহু ready-to-apply patch দেওয়া: `V226_TRASH_BRANCH_FILTER_PATCH.md`। owner paste করে build+device-এ দেখে merge করবেন। |

---

## F. Local-first Loading / ভুল-লাফানো Count (47–51)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 47–50 | First-paint stale→fresh (cache-then-network), count লাফানো | Owner Live-DB | cache-first কাঠামো আছে (`DoctorQueueRepository`), কিন্তু "ভুল/লাফানো count" live device-এ reproduce করে data মিলিয়ে ছাড়া আসল কারণ নিশ্চিত নয়। |
| 51 | RMP টাকা-সংক্রান্ত | Owner Live-DB | প্রমাণ-ডেটা ছাড়া amount বদলানো নিষেধ। |

---

## G. HTTP 400 Sync (60) + লাল সতর্কবার্তা প্রয়োজন

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| Framework (52–59) | 400/404/422 permanent, retry বন্ধ, body-hash dedup, corrected-data retry, cloud-confirm ছাড়া pending, লাল banner | **Already Correct** | `CloudWriteQueue.kt:611-660`; আসল ভুল-Field body থেকে `SupabaseClient.errSummary:159-172`; pending/failed গণনা+কারণ `PendingSyncStatus.kt:96-97`, `CloudWriteQueue.stuckDetail:514-540`; লাল সতর্কবার্তা `NoBengali.kt:259+` (V221 §1)। **সতর্কবার্তা section-এর সব শর্ত পূরণ।** |
| 60 | ঐ নির্দিষ্ট আটকে-থাকা record-এর SQL | Owner Live-DB | কোন record/table/field কেন 400 — device-এর pending queue payload + Supabase error body ছাড়া জানা অসম্ভব। কারণ জানার পর read-only চেক আগে (উপরে V226 read-only SQL কাঠামো দেওয়া)। |

---

## H. Delete/Reject/Incomplete/Trash/Restore + Ghost record (61–70)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 61–70 | Delete/Restore-এর পর ghost record ফিরে না আসা; backup overwrite না করা; duplicate না হওয়া | **Already Correct (live path) / Owner Live multi-device** | Tombstone: `DeletedGuard.kt:143-179`; প্রতিটি read `isDeleted` ছাঁকে; delete cloud tombstone `SupabaseClient.kt:658`; restore un-tombstone + pending DELETE বাতিল `DeletedGuard.kt:202`; restore-safe upsert `SupabaseClient.kt:317-348` (backup overwrite রোধ)। **একমাত্র latent ঝুঁকি:** বন্ধ-থাকা `SyncManager` pull (`SyncWorker.kt:47-84` — live app-এ অকার্যকর) — ভবিষ্যতে চালু করলে `isDeleted` filter যোগ করতে হবে। নির্দিষ্ট reported ghost কেস দুই-ডিভাইস + live DB ছাড়া নিশ্চিত নয়। |

---

## I. Official Patient ID / duplicate / orphan / export (73–85)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 73, 74 | Export column label | Owner (নমুনা দরকার) | নির্দিষ্ট exporter + আসল export নমুনা ছাড়া সঠিক label নিশ্চিত নয় (`CsvExportHelper.kt`/`*SheetExporter.kt`)। আন্দাজে label বদলানো নিষেধ। |
| 75–77 | ID-generation / search trace | Owner Build+Visual/DB | `PatientIdGenerator.kt:118-161` guard আছে (local ledger + cloud re-check); trace device/live-DB নির্ভর। |
| 78–81 | "7777777777" orphan সঠিক রোগীর সঙ্গে | **Owner Live-DB** | কোডে আন্দাজে বদলানো হয় না (নিরাপদ: `PatientIdentity.kt:42-52`, `MobileChangeSync.kt:80`)। সঠিক রোগী মেলানো live DB + owner সিদ্ধান্ত — **read-only তালিকা SQL দেওয়া** (`V226_...READ_ONLY_CHECKS.sql` §C/§D)। নিয়ম ৭৯/৮১: আন্দাজে ID নিষেধ। |
| 82, 83 | Duplicate-guard unique index | **File Ready / Owner run (read-only আগে)** | SQL সম্পূর্ণ ও সঠিক: `04_SUPABASE_DATABASE_SETUP/V224_..._official_patient_id_unique.sql` (partial unique, pre-check SELECT সহ)। **live-তে চালানো হয়নি** — owner চালাবেন। index আছে কিনা + duplicate আছে কিনা যাচাইয়ের **read-only SQL দেওয়া** (§A/§B)। |
| 84 | Blank/orphan patientId রক্ষা | **Already Correct / Owner Live-DB (link)** | partial index blank বাদ রাখে; read-side নিরাপদ। সঠিক রোগীর সঙ্গে link = live DB (§C SQL)। |
| 85 | Search trace | Owner Build+Visual/DB | `GlobalSearchActivity.kt`; device/live নির্ভর। |
| 86 | **Current-month report ০ (date-format)** | **Fixed in V226** | `native/ReportsRepository.kt` `monthOf()` এখন `yyyy-MM-dd`/ISO **এবং** `dd.MM.yyyy`/`dd/MM/yyyy` তিন ধাঁচই সঠিক `yyyy-MM` বানায় (আগে শুধু ISO)। ISO সারির ফল **হুবহু অপরিবর্তিত** (isolated logic-test-এ প্রমাণিত)। কোনো টাকা/সংখ্যার নিয়ম বদলায়নি। **build + live data-তে owner যাচাই করবেন।** |

*(71, 72 = `id`=System, `patientId`=Official — আগেই যাচাই-সঠিক।)*

---

## J. Reports (88, 89, 91, 92)

| # | বিষয় | Status | প্রমাণ / কারণ |
|---|------|--------|----------------|
| 88 | ৪টি period আলাদা হিসাব | Owner Build+Visual | field আছে; period-label display চোখে দেখা দরকার (`ReportsActivity`/`ReportCardActivity`)। |
| 89 | Branch today = payments মেলানো | Owner Live-DB | `ReportsRepository` branchRows (`:181-190`); live payment data মিলিয়ে যাচাই দরকার। |
| 91 | Staff ও Branch একই সময়সীমা filter | **Owner Definition** | বর্তমানে Conversion/Branch/Staff **lifetime** (`ReportsRepository.kt:181-220`), month-count এর মতো windowed নয়। "same-window"-এ আনতে owner-এর সংজ্ঞা লাগবে (eligible enquiry মানে কী; Branch/Staff current-month না lifetime) — আন্দাজে করা নিষেধ। |
| 92 | Count jump | Owner Live-DB | item 47-এর মতো; live device reproduce দরকার। |

*(86 উপরে Fixed; 87 = clamp আগেই done; 90 = ₹0 mark টাকায় গোনা হয় না, `ChamberAttendanceRepository.kt:647-672`, আগেই যাচাই-সঠিক।)*

---

## সারাংশ

- **Fixed in V226 (নিরাপদ code, build owner-এর):** 86 (date-format robust) + version bump 226/2.26 (Android + Web parity)।
- **Ready-to-apply patch দেওয়া:** 46 (Trash Master branch filter) — `V226_TRASH_BRANCH_FILTER_PATCH.md`।
- **Already Correct (প্রমাণসহ):** 6, 7, 14–19 (নিয়ম), 25 (design), 42–45, 52–59, 61–70 (live path), 71, 72, 84, 90।
- **File ready / owner run:** 82, 83 (+ read-only যাচাই SQL)।
- **Owner Live-DB:** 9, 10, 13, 21–24, 47–51, 60, 78–81, 89, 92।
- **Owner Build+Visual:** 2, 3, 5, 11, 12, 26 (আংশিক), 27, 28, 31, 73–77, 85, 88।
- **Owner Definition:** 32–41 (design confirm), 91 (same-window সংজ্ঞা)।

কোনো item-কে মিথ্যা Done/Passed বলা হয়নি। যেগুলো এই পরিবেশে (device/live-DB/চোখে-দেখা/owner-সংজ্ঞা ছাড়া) সত্যিই শেষ করা অসম্ভব, সেগুলোই কেবল Owner-required — অজুহাত হিসেবে নয়, বাস্তব সীমা হিসেবে, প্রতিটির নির্দিষ্ট কারণ ও ফাইল-অবস্থান সহ।
