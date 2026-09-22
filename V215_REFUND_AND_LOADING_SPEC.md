# V215_REFUND_AND_LOADING_SPEC.md — §13 Refund ও §10 Loading/Scroll (কোড-প্রস্তুত spec)

⛔ **সততা:** এই দুটো কাজ এই সেশনে **কোড-এ বসানো হয়নি** — কারণ §13 live payment-এর টাকা-হিসাব ছোঁয়, আর testing ছাড়া তা live-এ বসানো TK-এর নিজের নিয়মের (payment logic না ভাঙা, আন্দাজ না করা) বিরুদ্ধে। নিচে **হুবহু কোথায় কী বসাতে হবে** তা দেওয়া — testing সহ বসানো যাবে (আলাদা focused সেশন ভালো)।

DB প্রস্তুত: `V215_SAFE_MIGRATION` PART A2 `payments`-এ refund কলাম যোগ করেছে।

---

## §13 — Refund / টাকা ফেরত

### নীতি (§13 মেনে)
- পুরোনো payment row কখনো Delete/Edit হবে না। Refund = **আলাদা নতুন row**।
- History-তে Received ও Refunded — দুটোই দেখাবে।
- Collection/Paid total থেকে **শুধু Refund Amount** কমবে। Non-refundable Visit Fee অপরিবর্তিত।

### ধাপ ১ — Refund row model (`native/PaymentModel.kt`)
প্রমাণিত audit-row প্যাটার্ন (`buildAttendanceMarkRow` L332 / `buildBillEditRow` L357)-এর হুবহু ধাঁচে একটা `buildRefundRow(...)`:
```
payType = "refund"
amount  = refundAmount            // ধনাত্মক রাখুন (নীচের if(amount>0) guard-এর সঙ্গে সামঞ্জস্য)
mode    = "CASH" / "ONLINE"
date, patientId, patientCode, mobile, branch, name, receivedBy, createdBy, createdAt, updatedAt
refundReason, refundApprovalStatus ("approved" যদি master সরাসরি করেন, নয়তো "pending"),
refundRequestedBy, refundApprovedBy
```
⛔ `isTreatmentPaymentRow`/`isOrdinalTreatmentPayment` (L186-192) `payType=="refund"` কে **treatment হিসেবে ধরবে না** — তাই paid total এমনিতেই বাড়বে না। ভালো।

### ধাপ ২ — Total থেকে refund বিয়োগ (⚠️ সবচেয়ে সংবেদনশীল — এখানেই টেস্ট)
দুই জায়গায় শুধু **approved** refund বিয়োগ করতে হবে:
1. **Per-patient paid total** — `PaymentRepository.findPatientByMobile()` (L438-506, sum block L477-491): treatment paid যোগ করার পরে `payType=="refund" && refundApprovalStatus=="approved"` row-গুলোর amount **বিয়োগ** করুন। ফল ঋণাত্মক হলে 0-এ আটকান।
2. **Today/range collection** — `fetchTodayCollection()` (L136-254) ও `fetchCollectionRange()` (L313-359): এখানে `if(amount>0)` guard আছে, তাই refund row (ধনাত্মক amount) **যোগ** হয়ে যাবে — ভুল। তাই: collection যোগফলে refund row বাদ দিন, তারপর মোট থেকে approved refund **বিয়োগ** করুন। (অথবা refund row-এর জন্য আলাদা "refundedTotal" রেখে `collection = raw - refundedTotal`।)
   ⛔ এই ফাংশনগুলোর **narrowed column list** (L171-174, L326-329)-এ `payType, refundApprovalStatus` যোগ করতে হবে যাতে refund চেনা যায়।

উদাহরণ যাচাই (§13): Visit Fee ₹400 + Advance ₹5,000, Refund ₹5,000 (approved) → Visit Fee থাকবে ₹400, Advance Balance ₹0, History-তে সব entry। (Visit fee refund-এ ছোঁয়া হয় না কারণ ওটা treatment paid-এর বাইরে।)

### ধাপ ৩ — UI (Refund option)
`PaymentActivity.showCollectionDetails(row)`-এর action-button row (L508-526)-এ একটা "💸 Refund / টাকা ফেরত" বোতাম। চাপলে ছোট form: Amount, Date, Cash/Online, Reason।

### ধাপ ৪ — Permission ও Approval (প্রমাণিত template পুনর্ব্যবহার)
- **Master** → সরাসরি refund row লেখে `refundApprovalStatus="approved"`, `refundApprovedBy=master`।
- **Staff** → `refundApprovalStatus="pending"` refund row + `briefings`-এ master-কে request (হুবহু `DeletePermission.sendRequest()` / `PaymentModel.BackdateRequest`+`requestBackdatePayment` ধাঁচে)। Master **Approve** করলে row-এর `refundApprovalStatus="approved"` (তখনই total থেকে কমবে); **Reject** করলে `="rejected"` (total-এ কোনো প্রভাব নেই, history-তে দেখা যায়)।
- ⛔ নতুন টেবিল লাগে না — `briefings` + `payments`-এর নতুন কলামেই হয় (§15-এর bell এই request-ও সাউন্ডসহ জানাবে, কারণ briefings HEAD-count-এ যোগ করা হয়েছে)।

### ধাপ ৫ — Test (§19 #9, #10)
Refund Test + Staff Refund Approval Test — ফোনে চালিয়ে total ঠিক কমছে, visit fee অক্ষত, reject-এ প্রভাব নেই, দুই ফোনে sync — যাচাই করে তবেই live।

---

## §10 — Check-up/Journey/Report দ্রুত খোলা + scroll (spec)

§11-এর one-Back অংশ **কোড-এ করা হয়েছে**। নিচেরগুলো spec (device test সংবেদনশীল, তাই এখানে বসানো হয়নি):

1. **Journey/Action instant header** — `DoctorQueueActivity`-এর `onFullJourney`/`onAction` (L92-99) এখন `PatientTimelineActivity` খোলে **`pre*` extras ছাড়া**। Follow-up-এর `openTimelineFor()` (L2154-2170)-এর মতো `preStage/preName/preBranch/preDisease/preAge/preSex/preAddress/prePatientId` পাঠালে `paintInstantHeader()` (L2187) সঙ্গে সঙ্গে header আঁকবে — "Loading..." দেখা কমবে। **Additive, back-stack ছোঁয় না — কম ঝুঁকি।**
2. **Check-up দ্রুত** — `DoctorQueueActivity.openClinical()` (L321-346) screen খোলার আগে blocking `fetchDemographics()` করে। cached demographics দিয়ে আগে খুলে background-এ fetch করলে দ্রুত হবে।
3. **Report cache-first** — `ReportCardActivity.load()` (L88-106)-এ `TimelineCache`-এর মতো local-first render যোগ করলে আলাদা loading screen যাবে।
4. **Scroll/branch preserve** — RecyclerView-এর `layoutManager.onSaveInstanceState()`/`onRestoreInstanceState()` ব্যবহার করে scroll offset রাখা; branch/tab/filter ইতিমধ্যে instance field-এ টেকে (normal back-এ)। ⛔ §2.13/§2.14 locked back flow ছোঁয়া যাবে না।

প্রতিটা ধাপ device-এ যাচাই করে (§19 #14, #16) তবেই final।
