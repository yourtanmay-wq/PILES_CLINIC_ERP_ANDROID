# V236 — Changed files (CHAMBER DATE — সমস্যা ১ ও ৩)

**Base:** V235। **Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই **FINAL নয়** — compile/APK owner করবেন।
**তারিখ:** 01.08.2026 IST।
**আপনি live যে APK-তে সমস্যা দেখেছেন:** **V230**। যাচাই করে দেখা গেছে V231→V235-এর কোনোটাই Chamber Date-এর কোড ছোঁয়নি, তাই সমস্যা তিনটে V235-এও ছিল — শুধু V235 build করলেই যেত না। V236-এ #1 ও #3 ঠিক করা হলো।

---

## এই version-এ কী হলো (আপনার নির্দেশমতো — শুধু CHAMBER DATE)

### ✅ সমস্যা ১ — Print থেকে ফিরে "Arrived 0 / Nobody has arrived yet"
আপনার সিদ্ধান্ত: **"0/ফাঁকা থাক (আপনার আগের LOCKED নিয়ম B35 বহাল), শুধু ভুল লেখা সারাও।"**
- **আসল কারণ:** Close Chamber → Print-এর পরে `applyDayState()` (B35) সঠিক সবুজ summary ও "তালিকা ফাঁকা" বসায়; কিন্তু `loadBoard()`-এর ঠিক পরের ব্লকটা চলে সেটা মুছে statFilter-এর **"Nobody has arrived yet."** বসিয়ে দিত। তাই রোগী এসেছিলেন জেনেও পর্দায় "Arrived 0 / Nobody arrived" দেখাত (তথ্য কখনো হারায়নি — শুধু ভুল বার্তা)।
- **সমাধান:** বন্ধ-করা আজকের দিনে ওই override আর চলবে না। এখন দেখাবে সঠিক সবুজ লাইন — **"✅ আজকের চেম্বার বন্ধ করা হয়েছে · N জন এসেছিলেন · জমা ₹X · উপরের ক্যালেন্ডারে চাপলে আজকের তালিকা আবার দেখতে পাবেন"** + "তালিকা ফাঁকা"।
- ⛔ **B35-এর 0/ফাঁকা নিয়ম এক অক্ষরও বদলায়নি** — শুধু বিভ্রান্তিকর "Nobody has arrived yet" গেল।

### ✅ সমস্যা ৩ — Treatment Progress-এ "Treatment Payment / Advance Received"
আপনার সিদ্ধান্ত: **"আসল progress আর মুছবে না + board auto-label লুকাবে।"**
- **আসল কারণ:** Advance/Treatment **payment নিলে** `PaymentRepository.kt` নিজেই `followups.lastRemark`-এ জোর করে **"Treatment payment / Advance received"** বসিয়ে দিত — Chamber Date-এর "TREATMENT PROGRESS" ঘর ওই field পড়ে, তাই টাকা নিলেই আপনার লেখা progress মুছে ওই লেখাটা দেখাত এবং বারবার ফিরে আসত।
- **সমাধান (দুই স্তরে, additive):**
  1. **টাকা নিলে আর আসল লেখা মোছে না** — আগের lastRemark মানুষের সত্যিকারের লেখা হলে সেটাই থাকে; শুধু ফাঁকা বা অ্যাপের নিজের auto-label হলে তখনই "Treatment payment / Advance received" বসে।
  2. **Board auto-label লুকায়** — Chamber Date-এর progress ঘরে অ্যাপের নিজের বসানো payment-label (Report Card/Timeline-এর মতোই `PaymentModel.isAutoPaymentRemark` + আরও ৩টি followup label) কখনো "রোগীর progress" সেজে দেখাবে না; না থাকলে "—"।
- ⛔ টাকার অঙ্ক/বিল/advance/হিসাব/stage/status/permission কিছুই বদলায়নি। মানুষের লেখা কোনো remark এতে লুকোয় না।

### ⏳ সমস্যা ২ — "Fix Payment popup-এ শুধু Cancel" — এখনো বাকি (আপনার screenshot দরকার)
- **কারণ:** V230/V235 দুটোতেই Fix Payment editor **সম্পূর্ণ কাজ করছে** কোড-এ — `editOnePaymentRow` (Master সরাসরি amount/mode edit), Staff free-window-এ সরাসরি, তার বাইরে Master approval (`showRequestPaymentEditDialog`) — ঠিক আপনার #2 দাবি অনুযায়ীই।
- এই কোড থেকে **"শুধু Cancel" popup তৈরিই হতে পারে না** (তালিকা ফাঁকা হলে toast আসে, popup নয়)। তাই আসল সমস্যাটা অন্য কিছু (device/offline/data বা UI-বোঝাপড়া)।
- **money-code অন্ধভাবে বদলাইনি।** একটা screenshot পাঠান (ঠিক কোন পর্দায়, কী চাপলেন, কী দেখলেন) — V237-এ নিশ্চিত করে ঠিক করব।

---

## পরিবর্তিত production file (৩টি — সবই Android native, Chamber Date)
1. `…/native/ChamberAttendanceActivity.kt` — সমস্যা ১ (`loadBoard()`-এ বন্ধ-দিনে ভুল empty-message override বন্ধ)।
2. `…/native/ChamberAttendanceRepository.kt` — সমস্যা ৩ অংশ-২ (নতুন `isAppAutoRemark()` + board-এর progress ঘরে auto-label লুকানো, ২ জায়গায়)।
3. `…/native/PaymentRepository.kt` — সমস্যা ৩ অংশ-১ (`promoteFollowUpToTreatment`-এ টাকা নিলে আসল lastRemark না-মোছা)।

## যা ছোঁয়া হয়নি (যাচাইকৃত)
- **B35** (Close Chamber-এর পরে 0/ফাঁকা), ChamberClose/Reminder logic, Save & Print, রেজিস্টার PDF — অপরিবর্তিত।
- টাকার অঙ্ক/Fees/Cash/Online/Bill/Advance/Refund হিসাব, Payment edit/approval নিয়ম, permission, database schema, sync — অপরিবর্তিত।
- Web app (`03_NETLIFY_READY/app.js` ও mirror), Registration, Report Card, Follow-up, Enquiry, অন্য সব screen/design/রং — অপরিবর্তিত (এই তিনটে Android native Chamber-এর সমস্যা, web নয়)।
- কোনো broad refactor/cleanup/optimization/redesign হয়নি। কোনো version bump/`?v=` করা হয়নি (চাইলে করে দেব)।

## যা এই cloud-এ করা যায়নি (সৎ)
- **Compile/Build/APK — হয়নি** (Android SDK নেই)। কোড static যাচাই করা হয়েছে: exact before/after diff (শুধু ৩টি টার্গেট বদল), brace-balance ঠিক, `PaymentModel.isAutoPaymentRemark`/`row.s()`/`dateIsClosed()`/`viewingPast()` সব একই package/ফাইলে resolve করে। **"Build/Test Pass" দাবি করা হচ্ছে না।**

## Rollback
`ROLLBACK_V236/…/native/` — তিনটি ফাইলের সত্যিকারের **pre-V236 (=V235)** কপি (uploaded zip থেকে সরাসরি নেওয়া)। ফেরত চাইলে ওই তিনটি ফাইল দিয়ে বদলে দিলেই V235।

## Owner-এর যাচাই (Android Studio-তে build করে)
1. **সমস্যা ১:** আজকের চেম্বারে ২-৩ জন Arrived করুন → Close Chamber → Confirm & Print → ফিরে আসুন। এখন **"Nobody has arrived yet" আর দেখাবে না**; সবুজ লাইনে "N জন এসেছিলেন · জমা ₹X" ও তারিখে চাপার কথা দেখাবে। (0/ফাঁকা থাকা আপনার নিয়ম — বহাল।)
2. **সমস্যা ৩:** এক রোগীর Treatment Progress-এ কিছু লিখে Save → তালিকায় দেখাবে → আবার খুললে সেটাই থাকবে → **তারপর ওই রোগীর Advance/Treatment payment নিন → progress মুছবে না** (আগে "Treatment payment / Advance received" হয়ে যেত)। কোনো progress না-লেখা রোগীতে payment নিলে ঘরে "—" (payment-label নয়)।
3. **সমস্যা ২:** screenshot পাঠাবেন — তারপর।
