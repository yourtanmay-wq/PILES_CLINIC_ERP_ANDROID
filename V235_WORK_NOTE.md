# V235 — Work Note (কোড পরিবর্তনের আগে · Demo/Audit-অনুমোদনের অপেক্ষায়)

**Base:** V234 (সর্বশেষ working project)।
**তারিখ ও সময় (শুরু):** 01.08.2026, 01:29 PM IST।
**অবস্থা:** 🛑 এখনো **কোনো production source file বদলানো হয়নি**। আগে Demo Proof (কাজ-১, ২) ও read-only Audit (কাজ-৩); TK-এর অনুমোদনের পরেই কোড।

> সবকিছু বর্তমান code, database fields ও workflow **যাচাই করে** লেখা — অনুমান নয়। নিচে file path ও line উল্লেখ করা হলো।

---

## কাজ-১ · PATIENT REPORT CARD (single A4)

**১. সমস্যা:** Report Card দুই পৃষ্ঠা হচ্ছে, ২য় পৃষ্ঠা ফাঁকা। এছাড়া table-এ DUE column আছে (দরকার নেই), photo গোল, AGE ও SEX আলাদা লাইনে।

**২. আসল কারণ (code যাচাই):** Report Card তৈরি হয় HTML→PDF-এ — `…/native/ReportCardPrinter.kt`, `buildHtml()` (line 107–218)।
- ২য় ফাঁকা পৃষ্ঠা: `buildPdfAndPreview()` line 348 — `pages = Math.ceil(contentH / RENDER_H_PX)` (RENDER_H_PX=1754)। HTML-এর উচ্চতা 1754px সামান্য ছাড়ালেই `ceil` = ২, ২য় পৃষ্ঠা প্রায় ফাঁকা। Patient Details বড়, summary→table-এ gap, ও ৫ম column—সব মিলে উচ্চতা বাড়ায়।
- Table এখন **৫ column** (line 213): VISIT|DATE|TREATMENT PROGRESS|PAID|**DUE**; width 7/12/56/12.5/12.5% (line 189–193)। DUE cell line 157।
- Photo: `.photo` line 175 — `border-radius:50%` (গোল), 52px।
- AGE (line 203, col-1) ও SEX (line 204, col-2) **আলাদা লাইনে**।
- 20-row padding আছে (line 161–162) — ঠিক আছে, বহাল থাকবে।

**৩. পরিবর্তনযোগ্য file (অনুমোদনের পরে):** শুধু `…/native/ReportCardPrinter.kt` (buildHtml-এর HTML/CSS)। *(On-screen `ReportCardActivity.kt`-এ DUE column আগে থেকেই নেই ও photo আলাদা; দরকার হলে photo square-এ মেলাব — TK বললে।)*

**৪. আগে কীভাবে:** ৫ column (DUE সহ), গোল ছোট photo, AGE/SEX আলাদা লাইন, বড় Patient Details, মাঝে gap → উচ্চতা 1754px ছাড়িয়ে ২ পৃষ্ঠা।

**৫. পরে কীভাবে:** ৪ column (VISIT|DATE|TREATMENT PROGRESS|PAID — DUE column বাদ); Treatment Progress চওড়া (≈62%); photo **exact square**, বড়; "AGE: 30  MALE" এক লাইনে (আলাদা SEX row নেই); Patient Details compact; summary→table gap কমিয়ে table উপর থেকে শুরু; ঠিক 20 row; উপরের লাল DUE Summary Box **বহাল**। সব মিলে এক A4 পৃষ্ঠায়। navy/teal/green/red রঙ ও border অপরিবর্তিত।

**৬. LOCKED (অপরিবর্তিত):** উপরের TOTAL BILL/PAID/লাল DUE box; 20 row; navy/teal/green/red রঙ; PAID/DUE হিসাবের logic (`paidEffect`); watermark; clinic header।

---

## কাজ-২ · PRIMARY ও ALTERNATE / ENQUIRY MOBILE

**১. সমস্যা:** রোগী Enquiry-তে এক নম্বর, Registration-এ অন্য নম্বর দেন — পুরোনো Enquiry নম্বর হারিয়ে যায় / Patient record-এর সঙ্গে ঠিকমতো যুক্ত থাকে না।

**২. আসল কারণ (code যাচাই):** সারা সিস্টেমে রোগীর ফোন একটাই field — JSON key/DB column **`mobile`** (normalized `+91XXXXXXXXXX`)। **কোনো alternate/secondary mobile field নেই**।
- Registration save: `PatientModel.buildPatientRow` line 97 `.put("mobile", …)`; Android `RegistrationActivity` etMobile; Web `registration()`/`savePatient` `#pMob`।
- Enquiry→Registration: নতুন নম্বর টাইপ করলে `autofillFromEnquiry` শুধু Name/Branch/Disease আনে; পুরোনো Enquiry নম্বর কোথাও **সংরক্ষিত হয় না**।
- Duplicate check (`RegistrationRepository.checkDuplicatePatient`, `SupabaseClient.findByMobile*`; web `duplicate()`), Search (`GlobalSearchActivity.match`, web `recordsByMobile`), Contact (Android CallChooser call-sites; web `contact()`) — সব **একটাই `mobile`** দেখে।
- DB: `patients."mobile"` (line 35) ও `enquiries."mobile"` (line 9)। নজির: একই টেবিলে দ্বিতীয় ফোন-জোড়া `refDoctor`/`refDoctorMobile` (line 42–43) আগে থেকেই আছে — নতুন column ঠিক এভাবেই যোগ হবে।

**৩. পরিবর্তনযোগ্য file (অনুমোদন + SQL-এর পরে):** SQL patch (`altMobile` column, alter table); `PatientModel.kt`/`RegistrationDraft`; `activity_registration.xml` + `RegistrationActivity.kt`; Web `app.js` (SCHEMA line 386–387, `registration()`/`savePatient`, `duplicate()`/`recordsByMobile`, search, `contact()`); Search select-cols + match; Contact chooser; display (`ReportCardActivity`/web summary)। **তালিকা Demo-অনুমোদনের পরে চূড়ান্ত হবে।**

**৪. আগে কীভাবে:** একটাই Mobile field; Enquiry নম্বর ও Registration নম্বর ভিন্ন হলে আগেরটা Patient-এ থাকে না; Search/Contact একটাই নম্বরে।

**৫. পরে কীভাবে:** Registration/Edit-এ **দুটি field — Primary Mobile ও Alternate/Enquiry Mobile**। Enquiry থেকে খুললে পুরোনো Enquiry নম্বর আপনা থেকেই Alternate-এ বসবে; Chamber-এর নতুন নম্বর Primary। একই নম্বর হলে Alternate-এ duplicate হবে না। Search ও Duplicate-check **দুই field-এই**; Call/WhatsApp/SMS default **Primary**, দরকারে Alternate বেছে যোগাযোগ; পুরোনো Enquiry History একই Patient ID-তে যুক্ত থাকবে; Card/Report-এ Primary, Details/History-তে দুটোই label-সহ; Android ও Web একই rule।

**৬. LOCKED (অপরিবর্তিত):** existing `mobile` (=Primary) data ও তার সব linkage; Enquiry/Patient/Follow-up/Payment History; Patient ID linkage; duplicate-check মূল নিয়ম; অন্য রোগীর record edit না-হওয়া। **DB change শুধু নতুন column যোগ (কিছু মোছা/বদলানো নয়); SQL আলাদা করে আগে দেওয়া হবে, read-only verification প্রথমে।**

---

## কাজ-৩ · ENQUIRY ADDRESS SAVE ও VIEW (read-only audit)

**১. সমস্যা:** নতুন record (DHIRENDRA, "RAMTHANGA")-এ View-এর উপরে Address দেখা যায়; পুরোনো "UNKNOWN" record-এ Address দেখা যায় না।

**২. আসল কারণ (code যাচাই — অনুমান নয়):** Address সব জায়গায় **একটাই key `address`** (enquiries line 11 / patients line 39 / followups line 99)। **field-name mismatch নেই।**
- Enquiry save ঠিক আছে: `EnquiryActivity` (address line 254) → `EnquiryModel.buildEnquiryRow` line 50 `.put("address", …)`; Web `saveEnq` key `address`।
- Android View header (`PatientTimelineRepository.kt` line 869–871): `address = patients.address ?: enquiries[0].address ?: ""`। **followups[0].address-এ fallback নেই** — অথচ branch/disease-এ followups fallback **আছে** (line 807–812)।
- View দেখায় শুধু **non-blank** হলে (`PatientTimelineActivity` line 2407/2467/2600 — blank হলে GONE)।
- **Web:** `viewFollow()` (enquiry timeline, line 2828–2880) header-এ **Address row-ই নেই** — তাই web-এ enquiry-origin View-তে address কখনো দেখায় না।

**উপসংহার:** সাধারণ enquiry→patient পথে mapping ঠিক। পুরোনো "UNKNOWN"-এ Address না দেখার দুই সম্ভাব্য কারণ — (ক) ওই record-এ `patients.address` ও `enquiries.address` সত্যিই ফাঁকা (data), অথবা (খ) record শুধু `followups` row হিসেবে টিকে আছে যার `address` আছে কিন্তু header ওতে fallback করে না (code gap)। **কোনটা — তা read-only SQL দিয়ে প্রমাণ করে তবেই ঠিক করা হবে।**

**৩. পরিবর্তনযোগ্য file (audit + অনুমোদনের পরে):** সম্ভবত `PatientTimelineRepository.kt` (address-এ followups fallback যোগ) ও Web `viewFollow()` (Branch–Disease-এর নিচে Address row যোগ)। **আন্দাজে data বদলানো হবে না; ভুয়া/অনুমান Address কখনো দেখানো হবে না; UNKNOWN শুধু Name-এর জন্য।**

**৪–৫. আগে/পরে:** আগে — followups-only পুরোনো record-এ Address hidden, web enquiry-View-এ Address নেই। পরে — followups.address থাকলে Android View-তেও দেখাবে, web View-এও Branch–Disease-এর নিচে Address দেখাবে; সত্যি ফাঁকা হলে কিছুই দেখাবে না (মিথ্যা নয়)।

**৬. LOCKED:** Call History table-এর Date/Time · Type/By · Note column; existing address save; UNKNOWN-এর Name-ব্যবহার; অন্য কিছু।

---

## সব কাজে অভিন্ন LOCKED
অনুমোদিত Color/design; existing Button/Workflow/Permission; Report Card-এর BILL/PAID/লাল DUE box; single A4; বড় square photo; "AGE: 30 MALE" এক লাইন; 20 Visit Row; Enquiry/Patient History, Payment, Follow-up, Patient ID linkage; Branch ও Disease display; অন্য screen/design। কোনো broad refactor/cleanup/optimization/redesign নয়।

## এই cloud-এর সীমা (সৎ)
Android SDK নেই — Build/APK এখানে হবে না; owner Android Studio-তে build করবেন। DB-তে কিছু run করা হয়নি; প্রয়োজনীয় SQL আলাদা ফাইলে (read-only আগে) দেওয়া হবে।

---
### পরবর্তী ধাপ (এই turn-এ, কোড ছাড়া)
- কাজ-১: single-A4 Report Card **Demo Proof** (ছবি পাঠানো হয়েছে)।
- কাজ-২: Registration/Edit **Mobile Demo Proof** + data-flow + `V235_MOBILE_READONLY_VERIFY.sql` + প্রস্তাবিত `V235_MOBILE_ALTER_PROPOSED.sql` (run করা হয়নি)।
- কাজ-৩: **read-only audit SQL** `V235_ADDRESS_READONLY_AUDIT.sql`।

---

# ✅ অনুমোদনের পরে সম্পন্ন (TK "ok final" — 01.08.2026)
**তারিখ ও সময় (শেষ):** 01.08.2026, 02:30 PM IST।

## পরিবর্তিত production file (১০টি)
**কাজ-১:** `…/native/ReportCardPrinter.kt` — ৪ column (DUE column বাদ), square+বড় photo, "AGE: 30 MALE" এক লাইন, compact details, gap কমানো, progress ২-লাইন clamp (single A4 নিশ্চিত)। লাল DUE Summary Box, 20 row, navy/teal/green/red অপরিবর্তিত।
**কাজ-২ (Android):** `PatientModel.kt` (altMobile field+save), `RegistrationActivity.kt` (Alternate field wire + Enquiry-নম্বর auto-move + dedup), `res/layout/activity_registration.xml` (etAltMobile), `RegistrationRepository.kt` (dup-check Alternate-এও, additive), `GlobalSearchActivity.kt` (Search Alternate-এও, additive), `PatientTimelineRepository.kt` (TimelineData.altMobile), `PatientTimelineActivity.kt` (Details-এ "Alt: +91…" label-সহ)।
**কাজ-২ (Web):** `03_NETLIFY_READY/app.js` + mirror `…/assets/www/app.js` — SCHEMA altMobile, registration form-এ Alternate field, savePatient-এ dedup+save, `duplicate()` ও `recordsByMobile` Alternate-এও (additive), Patient Details-এ Alternate Mobile line।
**কাজ-৩:** `PatientTimelineRepository.kt` (address এখন followups[0]-এও fallback), `03_NETLIFY_READY/app.js` + mirror (web `viewFollow` header-এ Branch-এর নিচে 📍 Address, non-blank হলে)।
**নতুন SQL/doc:** `04_.../V235_MOBILE_READONLY_VERIFY.sql`, `V235_MOBILE_ALTER_PROPOSED.sql`, `V235_ADDRESS_READONLY_AUDIT.sql`; `V235_WORK_NOTE.md`, `V235_CHANGED_FILES.md`; `ROLLBACK_V235/` (১০ ফাইলের V234 কপি)।

## Test (static — এই cloud-এ SDK নেই, device-run হয়নি)
- Web: দুই app.js `node --check` **OK** ও byte-identical।
- Android: একটি স্বতন্ত্র review-তে সব পরিবর্তন **BUILD-SAFE** (data-class default-সহ যোগ, কোনো positional breakage নেই; `binding.etAltMobile`, `draft.altMobileDigitsOnly`, `data.altMobile` সব resolve করে)।
- Report Card single-A4: demo A4-তে আঁটে; production-এ progress ২-লাইন clamp + compact layout-এ উচ্চতা 1754px-এর অনেক নিচে → এক পৃষ্ঠা।

## Final order / রঙ
Report Card ৪ column: **VISIT | DATE | TREATMENT PROGRESS | PAID**; উপরে লাল DUE box। navy/teal/green/red অপরিবর্তিত।

## Declaration
LOCKED-এর কিছুই ভাঙা হয়নি: BILL/PAID/লাল DUE box, single A4, square photo, AGE+SEX এক লাইন, 20 row, Branch/Disease display; existing `mobile`(=Primary) ও সব linkage; Enquiry/Patient/Payment/Follow-up History, Patient ID linkage; duplicate-check মূল নিয়ম (শুধু additive); Call History table; permission/database/sync; আগের V231–V234 কাজ। সব mobile/search/dup পরিবর্তন **additive** — altMobile ফাঁকা হলে আচরণ হুবহু আগের মতো। কোনো broad refactor/redesign নয়। DB-তে শুধু নতুন column (`altMobile`) যোগ হবে — owner-ই `V235_MOBILE_ALTER_PROPOSED.sql` চালাবেন (তার আগে code চললেও ভাঙবে না; altMobile ফাঁকা থাকবে)।

## owner-test-pending (সৎ)
- Android APK build + দুই-নম্বর live test (Registration auto-fill, dedup, Search, Details display)।
- কাজ-৩: `V235_ADDRESS_READONLY_AUDIT.sql` চালিয়ে পুরোনো record প্রমাণ (data নাকি code-gap)।
- #8-এর "Alternate-এ এক-চাপে কল" আলাদা বোতাম — Primary সর্বত্র default ও দুই নম্বরই label-সহ দেখা যায়; চাইলে dedicated Alternate-call বোতাম পরে যোগ করা যাবে (বললে)।
