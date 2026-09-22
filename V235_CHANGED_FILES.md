# V235 — Changed files

**Base:** V234। **Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই **FINAL নয়**।
**তারিখ:** 01.08.2026, 02:30 PM IST। **Demo/Audit TK-অনুমোদিত ("ok final")।**

## তিনটি কাজ — সংক্ষেপে
- **কাজ-১ Report Card:** এক A4 পৃষ্ঠা; table ৪ column (VISIT | DATE | TREATMENT PROGRESS | PAID — DUE column বাদ); square+বড় photo; "AGE: 30 MALE" এক লাইন; উপরের লাল DUE Summary Box বহাল; 20 row; navy/teal/green/red অপরিবর্তিত।
- **কাজ-২ Primary/Alternate Mobile:** Registration/Edit-এ দুই field; Enquiry থেকে খুললে পুরনো নম্বর Alternate-এ auto; একই হলে dedup; Search ও Duplicate-check দুই field-এ (additive); Contact default Primary; Details-এ দুটোই label-সহ; Android + Web।
- **কাজ-৩ Address:** Android View header এখন `followups[0].address`-এও fallback করে (আগে করত না); web `viewFollow`-এ Branch-এর নিচে Address (non-blank হলে)। কখনো অনুমান/ভুয়া address নয়।

## পরিবর্তিত production file (১০টি)
1. `…/native/ReportCardPrinter.kt` — কাজ-১।
2. `…/native/PatientModel.kt` — কাজ-২ (altMobile field + save)।
3. `…/native/RegistrationActivity.kt` — কাজ-২ (Alternate field, Enquiry-নম্বর auto-move, dedup)।
4. `…/res/layout/activity_registration.xml` — কাজ-২ (etAltMobile)।
5. `…/native/RegistrationRepository.kt` — কাজ-২ (dup-check Alternate-এও, additive)।
6. `…/native/GlobalSearchActivity.kt` — কাজ-২ (Search Alternate-এও, additive)।
7. `…/native/PatientTimelineRepository.kt` — কাজ-২ (TimelineData.altMobile) + কাজ-৩ (address followups fallback)।
8. `…/native/PatientTimelineActivity.kt` — কাজ-২ (Details-এ Alt নম্বর)।
9. `03_NETLIFY_READY/app.js` — কাজ-২ + কাজ-৩ (web)।
10. `…/assets/www/app.js` — #9-এর mirror (byte-identical)।

## নতুন file
- `04_SUPABASE_DATABASE_SETUP/V235_MOBILE_READONLY_VERIFY.sql` (read-only)
- `04_SUPABASE_DATABASE_SETUP/V235_MOBILE_ALTER_PROPOSED.sql` (**owner চালাবেন** — শুধু নতুন column যোগ)
- `04_SUPABASE_DATABASE_SETUP/V235_ADDRESS_READONLY_AUDIT.sql` (read-only)
- `V235_WORK_NOTE.md`, এই file, `ROLLBACK_V235/` (১০ ফাইলের V234 কপি)।

## Before / After
| বিষয় | আগে (V234) | পরে (V235) |
|---|---|---|
| Report Card পৃষ্ঠা | ২ পৃষ্ঠা (২য় ফাঁকা) | ১ A4 পৃষ্ঠা |
| Table column | ৫ (…PAID, **DUE**) | ৪ (…PAID) — DUE column বাদ |
| উপরের DUE box | আছে | আছে (অপরিবর্তিত) |
| Photo | গোল, 52px | **square**, বড় (74px) |
| AGE / SEX | দুই লাইন | "AGE: 30 MALE" এক লাইন |
| Patient mobile | একটাই (`mobile`) | Primary + Alternate/Enquiry |
| Enquiry নম্বর ভিন্ন হলে | হারাত | Alternate-এ থাকে, Patient ID-তে যুক্ত |
| Search / Dup-check | শুধু Primary | Primary **ও** Alternate (additive) |
| পুরনো record Address (followups-only) | View-তে দেখাত না | দেখায় (DB-তে থাকলে) |
| Web enquiry View Address | ছিল না | Branch-এর নিচে দেখায় |

## Test result (সৎ)
- Web: দুই `app.js` **`node --check` OK**, byte-identical।
- Android: স্বতন্ত্র review-তে সব পরিবর্তন **BUILD-SAFE** (data-class default-সহ; positional breakage নেই)।
- **Compile/APK এই cloud-এ হয়নি** (SDK নেই) — owner Android Studio-তে build ও দুই-নম্বর/Report-Card live-test করবেন। "Build/Test Pass" দাবি করা হচ্ছে না।

## SQL তালিকা (আলাদা)
- এখনই নিরাপদ (read-only): `V235_MOBILE_READONLY_VERIFY.sql`, `V235_ADDRESS_READONLY_AUDIT.sql`।
- অনুমোদনের পরে: `V235_MOBILE_ALTER_PROPOSED.sql` (patients-এ `altMobile` column যোগ; কিছু মোছে না; idempotent)।

## Rollback
`ROLLBACK_V235/` — ১০ ফাইলের সত্যিকারের V234 কপি (একই সাব-পাথে)। ফেরত চাইলে বদলে দিলেই V234। (নতুন column যোগ করা থাকলে সেটা আলাদা করে drop করতে হবে — তবে column থাকলেও পুরনো code ভাঙে না।)

## Declaration
অন্য কোনো PASS/LOCKED design, color, button, workflow, permission, print design, sync বা data পরিবর্তন করা হয়নি। সব mobile/search/dup পরিবর্তন additive (altMobile ফাঁকা হলে হুবহু আগের আচরণ)। কোনো broad refactor/cleanup/optimization/redesign হয়নি।
