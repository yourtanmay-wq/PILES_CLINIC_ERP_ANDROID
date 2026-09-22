# V621 — "Return Fees" নতুন ফিচার (Visit Card → Chamber Date বাদ → Draft "Return Visit")
**তারিখ:** ২৪.০৮.২০২৬ · **ভার্সন:** V621 / 6.21

## TK-এর চূড়ান্ত নির্দেশ
"ঝুঁকি থাকলে Chamber Date থেকে বাদ রাখুন — Visit Card থেকে Return করা
যাবে সেটাই রাখুন। Visit Card থেকে Fees Amount Return করলে Chamber Date
থেকে যেন অটোমেটিক ডিলিট হয়ে যায়। Draft-এ সম্পূর্ণ নতুন 'Return Visit'
কাটাগরি চাই (Visit Reject থেকে আলাদা)।"

## যা বানানো হলো (৩টা সংযুক্ত অংশ)

### ১) Visit Card-এ "💸 Return Fees" (PatientTimelineActivity.kt)
- শুধু রেজিস্টার্ড রোগীর জন্য (Enquiry-only-তে Fees-ই নেই)।
- **আন্দাজ ছাড়াই** — বিদ্যমান, প্রমাণিত `PaymentRepository.saveRefund()`
  (V509-এ Visit Fee-সহ refundable ধরে) পুনর্ব্যবহার — নতুন টাকা-হিসাবের
  পথ বানানো হয়নি।
- একই দিন-ভিত্তিক নিয়ম: চেম্বার আজ খোলা থাকলে (বা Master) সরাসরি, নইলে
  আটকে যায়।
- সফল Refund-এর পরে `followups.status = "Returned"` বসে।

### ২) Chamber Date থেকে সম্পূর্ণ বাদ (ChamberAttendanceRepository.kt + RefundedRecords.kt)
- নতুন, **সম্পূর্ণ আলাদা** ফাংশন `RefundedRecords.fetchReturnedVisits()`
  — বিদ্যমান "Cancelled" (শুধু টাকা লুকায়, রোগী থাকে) সিস্টেম **এক
  অক্ষরও ছোঁয়া হয়নি**।
- "Returned" status-ধারী রোগীর **পুরো সারিই** বোর্ড থেকে বাদ (নামসহ)।
- ব্যর্থ হলে (নেট/এরর) খালি সেট — কারো সারি ভুলবশত বাদ যায় না।

### ৩) Draft-এ নতুন "Return Visit" ক্যাটেগরি (DraftRepository.kt + DraftActivity.kt + XML)
- সম্পূর্ণ নতুন bucket (`returnVisit`) — "Visit Reject"-এর পাশাপাশি,
  আলাদা tab-id ("returnvisit"), আলাদা কার্ড (↩️ আইকন, কমলা রং)।
- Cache read/write দুই জায়গাতেই যোগ (পুরনো cache-ও নিরাপদে চলবে)।
- **Restore বোতাম আপনা থেকেই কাজ করে** — `DraftCardAdapter`-এর
  বিদ্যমান "else" শাখা (যেটা visitreject/notcomplete-এও ব্যবহৃত) কোনো
  পরিবর্তন ছাড়াই "returnvisit"-কেও ধরে নেয়।
- Delete বোতাম **ইচ্ছাকৃতভাবে দেখানো হয়নি** (Incomplete/Complete-এর
  মতোই) — টাকা জড়িত বলে Master-অনুমতি লাগবে, সরাসরি Delete না।

## নতুন আবিষ্কার (এই টার্নেই ধরা পড়া, নিজে ঠিক করা)
Chamber Date-এর Fees ঘর **আগে ইচ্ছাকৃতভাবে (TK-LOCKED, ২৫.০৭.২০২৬) ট্যাপ-
অযোগ্য বানানো ছিল** ("display only, no tap at all")। যেহেতু আপনি এবার
স্পষ্ট নতুন নির্দেশ দিয়েছেন (Visit Card থেকেই, Chamber Date থেকে না) —
সেই পুরনো লক স্পর্শই করা হয়নি, Fees ঘর এখনো আগের মতোই ট্যাপ-অযোগ্য।

## নিরাপত্তা
- বিদ্যমান "Cancelled"/"Visit Reject" সিস্টেম সম্পূর্ণ অক্ষত।
- Refund-এর টাকার হিসাব প্রমাণিত পথেই (কোনো নতুন গণনা-লজিক নেই)।
- Restore করলে শুধু status Active হয়, Refund-এর টাকা/রেকর্ড অক্ষতই থাকে
  (visitreject-এর মতোই আচরণ)।

## যাচাই
| পরীক্ষা | ফল |
|---|---|
| `tk_guard.py --release` | ✅ পাশ (প্রতিটা ধাপে আলাদাভাবে) |
| XML ভ্যালিড | ✅ |
| Restore/Delete বোতাম আচরণ | ✅ কোড পড়ে নিশ্চিত করা হয়েছে |

## সততার সাথে যা এখনো বাকি
- **ওয়েব সংস্করণ এখনো করা হয়নি** — সময়ের সীমার কারণে। জানালে পরের
  সেশনে একই ডিজাইনে ওয়েবেও করে দেব।
- Bulk Restore বোতাম (একাধিক একসাথে) "Return Visit"-এ eligibility-তে
  যোগ করা হয়েছে, কিন্তু checkbox (bulk-select) এখনো দেখানো হচ্ছে না —
  তাই এখন শুধু একটা একটা করে (individual) Restore কাজ করে, bulk না।
  প্রয়োজন হলে জানাবেন।
