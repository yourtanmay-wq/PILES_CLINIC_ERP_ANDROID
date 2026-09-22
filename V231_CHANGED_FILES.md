# V231 — Changed files

**Base:** V230। **Build:** owner-এর মেশিনে (Android Studio); এই cloud-এ SDK নেই তাই **FINAL নয়** — compile owner করবেন।

## এই version-এর কাজ (একটাই, TK verified live-test)
**সমস্যা (ছবিসহ, 01.08.2026):** JONEKA BIBI Delete → ঠিকঠাক Trash-এ গেল, তালিকা 9→8। কিন্তু **DEMO TEST ও TK BISWAS** Delete করলে *"Record not found — it may already be deleted"* দেখাচ্ছিল, আর ওই দুই রেকর্ড **Incomplete Patient তালিকা ও পুরোনো Detail Screen-এ থেকে যাচ্ছিল, Action-ও খোলা যাচ্ছিল**।

**কারণ (কোড ধরে):** Delete করার সময় রেকর্ড cloud-এ (id ও mobile — দুভাবেই) সত্যিই না পাওয়া গেলে কোড `"NOT_FOUND"` ফেরত দিত, আর শুধু একটা toast দেখিয়ে **সারিটা তালিকায়/পর্দায় রেখে দিত** — কোথাও থেকে সরাত না।

**সমাধান:** এখন `NOT_FOUND` পেলে ওই "ভূতুড়ে" (database-এ আর নেই) সারি —
- বর্তমান তালিকা থেকে সঙ্গে সঙ্গে সরে (count refresh হয়),
- Detail Screen বন্ধ হয়,
- এই ফোনের জমানো (display-cache) ছায়া-কপি মুছে যায়, তাই ফিরে এলেও আর দেখা যায় না।

## 🔒 নিরাপত্তা — যা করা হয়নি (ইচ্ছাকৃত)
- **কোনো cloud লেখা নেই, কোনো `DeletedGuard` tombstone বসানো হয়নি।** তাই **অন্য ফোনের এখনো sync না-হওয়া আসল রেকর্ড** কেবল "not found" পাওয়ার কারণে **স্থায়ীভাবে মুছবে না** — পরে sync হলে স্বাভাবিকভাবেই ফিরে আসবে। (আপনার স্পষ্ট শর্ত।)
- pending-sync queue (`LocalWorkflowStore`) ছোঁয়া হয়নি; নিজের ফোনের এখনো না-ওঠা enquiry আগের মতোই `mergeOwnPhoneEnquiries`-এ load-এর সময় ফিরে মেশে।
- সরানো **শুধু এই ফোনের চোখে-দেখা (display) স্তরে** — app বন্ধ হলে in-memory অংশ চলে যায়, তখন cloud-ই একমাত্র সত্য।
- **অপরিবর্তিত:** UI design/layout/রং/font, Trash/Restore logic, Payment/Follow-up history, permission ও Master-approval নিয়ম, database rule, sync system, অন্য কোনো feature। কোনো broad refactor/optimization হয়নি।

## পরিবর্তিত ফাইল (৩টি)
- `…/native/DraftRepository.kt` — নতুন `purgeGhostFromCache(id, mobile)` (শুধু display-cache মুছে) + নতুন in-memory `DraftRepository.GhostHide` (এক পর্দায় সরালে অন্য পর্দাও জানে)। কোনো পুরনো ফাংশন বদলায়নি।
- `…/native/DraftListActivity.kt` — Incomplete/Draft তালিকায় Delete-এর `NOT_FOUND` শাখা: এখন সারি সরায় + cache purge + count refresh; আর `dropDeletedFromEntries()`-এ `GhostHide` যুক্ত (Detail থেকে সরালে Back-এ তালিকাও পরিষ্কার)।
- `…/native/PatientTimelineActivity.kt` — Detail Screen-এর **দুই** Delete পথ (Enquiry ও Patient/Registration)-এর `NOT_FOUND` শাখা: cache purge + `finish()` (পর্দা বন্ধ)। মোবাইল/id দিয়ে খোঁজা, permission, Trash-move — সব আগের মতোই।

## যা ছোঁয়া হয়নি (একই class-এর delete, কিন্তু reported scenario-র বাইরে)
আপনার নির্দেশ "শুধু এই verified সমস্যাটি" মেনে নিচের দুটি **বদলানো হয়নি** — চাইলে একই কায়দায় করে দেব, বলবেন:
- `DoctorVisitActivity.kt` (Doctor Visit তালিকার Delete) — এখনো `NOT_FOUND`-এ শুধু toast।
- `BriefingActivity.kt` (Master-এর "Approve & Delete") — এখনো `NOT_FOUND`-এ শুধু toast।

## যা এই cloud-এ করা যায়নি (সৎ)
- **Compile/Build/APK — হয়নি** (SDK নেই)। কোড static-ভাবে যাচাই করা হয়েছে (syntax, import, package, reference), কিন্তু "Build Pass" দাবি করা হচ্ছে **না**।
- Version bump (231 / 2.31 / v231) ও index.html `?v=` — **করা হয়নি**, কারণ আপনি শুধু এই bug ঠিক করতে বলেছেন। চাইলে আলাদা করে করে দেব।

## Rollback
`ROLLBACK_V231/native/` — তিনটি ফাইলের সত্যিকারের **pre-V231 (=V230)** কপি (zip থেকে সরাসরি নেওয়া, কার্যকর)। ফেরত চাইলে ওই তিনটি ফাইল দিয়ে বদলে দিলেই V230।

## Owner-এর যাচাই (Android Studio-তে build করে)
1. দুটি ফোনে একই রেকর্ড দেখা যাচ্ছে এমন অবস্থা বানান; A-ফোনে Delete করুন।
2. B-ফোনে ওই রেকর্ডে Delete চাপুন → এখন **"Already deleted — this record is gone"** এসে পর্দা বন্ধ হবে ও Incomplete তালিকা থেকেও সরে যাবে; count কমবে।
3. **সবচেয়ে জরুরি নিরাপত্তা-টেস্ট:** B-ফোনে **নেট বন্ধ করে** একটা নতুন enquiry/patient সেভ করুন (এখনো cloud-এ ওঠেনি)। সেটিতে Delete চাপলে **"not found" আসবে না** — কারণ সেটি local-এ আসল; আগের মতোই Trash-এ যাবে। এতে নিশ্চিত হবে আসল unsynced রেকর্ড ভুল করে মুছছে না।
