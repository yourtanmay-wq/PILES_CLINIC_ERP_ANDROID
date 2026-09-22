# V523 — Reports পর্দার তিনটে সমস্যা
**তারিখ:** ২২.০৮.২০২৬ · **ভিত্তি:** V522 · সবই TK-এর প্রশ্ন থেকে, যাচাই করে।

TK তিনটে প্রশ্ন করেছিলেন। **কোড পড়ে দেখলাম তিনটেই ঠিক।**

---

## ১. উপরের তিনটে বাক্সে চাপ দিলে কিছুই হত না

**প্রমাণ:** `activity_reports.xml`-এ ওগুলো শুধু লেখা (`tvEnqTotal` ·
`tvPatTotal` · `tvCollTotal`)। যে বাক্সে বসানো, তার **কোনো id ছিল না,
কোনো click ছিল না**। তাই ছুঁলে কিছু হওয়ার কথাই নয়।

**এখন প্রতিটা বাক্স ঐ সংখ্যার আসল তালিকাটাই খোলে:**

| বাক্স | কোথায় নিয়ে যায় |
|---|---|
| Enquiry | Follow-Up → **Enquiry** ট্যাব |
| Patients | Follow-Up → **Patient** ট্যাব |
| Collection | **Payment Collection** পর্দা |

⛔ **নতুন কোনো পর্দা বানানো হয়নি** — অ্যাপের চলতি, পরীক্ষিত পর্দাগুলোই
খোলা হয়। তাই সংখ্যা দুই জায়গায় আলাদা হওয়ার ভয় নেই, আর ঐ পর্দাগুলোর
ব্রাঞ্চ/অনুমতির নিয়মও নিজে থেকেই বহাল থাকে।
⛔ **Supabase-এ বাড়তি পড়া নেই** — চাপ না দিলে আগের মতোই কিছুই হয় না।
⛔ `FollowUpActivity`-তে নতুন `startStage` ঘর — না এলে আচরণ অবিকল আগের।

---

## ২. ₹0-এর সারি "Today's Collection"-এ কেন — **এটাই আসল ভুল ছিল**

**প্রমাণ:** `ReportsActivity.showBranchDetail()` আজকের **সব** payments-সারি
তালিকায় ঢোকাত, অঙ্ক শূন্য হলেও। আর Chamber-এর *"Mark Expected"* বোতাম ঠিক
ওই টেবিলেই ₹0-এর সারি লেখে
(`PaymentModel.buildExpectedMarkRow` → `payType="chamber_expected"`,
`amount=0.0`, `payLabel="Marked Expected"`)।
⇒ কেউ টাকা দেয়নি, তবু Collection-এর তালিকায় নাম উঠত।

**এখন:**
- চিহ্ন-মাত্র সারিগুলো তালিকা থেকে বাদ।
- কতজনকে "আজ আসবেন" বলা হয়েছে, সেটা উপরে এক লাইনে —
  `⏰ Expected today: 7 (no payment yet)`। **তথ্য হারায় না।**

**কোন সারি "চিহ্ন-মাত্র" — নতুন কোনো আন্দাজ নয়।** নতুন
`PaymentModel.isMarkerOnlyRow()` হুবহু সেই তিনটে ধরনই ধরে যেগুলো
`isOrdinalTreatmentPayment()` (৪৭১ নং লাইন) ও `PatientTimelineRepository`
**আগে থেকেই** বাদ দেয় — অর্থাৎ প্রজেক্টের চলতি নিয়মেরই নাম দেওয়া হলো।

**🔒 টাকার হিসাব:** cash/online যোগফল **আলাদাভাবেই** গোনা হয়, তালিকা থেকে
নয় — আর ₹0 যোগ হলে যোগফল বদলায়ও না। **পরীক্ষায় হাতে-কলমে প্রমাণিত:**
চিহ্ন-সারি থাকা ও না-থাকা অবস্থায় Cash ও Online **হুবহু এক**। Refund-এর
বিয়োগ (approved) ও pending-refund-এর নিয়মও অক্ষত।

---

## ৩. একই প্রজেক্টে দুই জায়গায় স্টাফের হিসাব

**যাচাই করে দেখলাম দুটো এক নয়:**

| | Reports → Staff-wise | Staff Profiles → 🏆 Staff Performance |
|---|---|---|
| উৎস | **ফোনেই গোনা** (`ReportsRepository`) | **সার্ভারের RPC** (`hr.staff_performance`) |
| চেনে | `createdBy`/`receivedBy` **মোবাইল** | `staff_profiles`-এর **কোড** |
| দেখায় | শুধু **Enquiry ও Patient** | কল · কালেকশন · RMP · রিপোর্ট · হাজিরা · Extra Income |

⇒ দুই নিয়মে গোনা, তাই **সংখ্যা মিলবে না** — এটাই বিপদ ছিল।

**TK-এর সিদ্ধান্ত (জিজ্ঞাসা করে নেওয়া): অংশটা থাকবে, শুধু স্পষ্ট করে লেখা থাকবে।**
- উপরে ব্যাখ্যা: *"Counts Enquiry & Patient only, from this phone. Full
  performance … is in Staff Profiles → Staff Performance."*
- নিচে একটা বোতাম — **🏆 Staff Performance** — যেটা **আসল পর্দাটাই** খোলে
  (`StaffProfileActivity`-তে নতুন `openPerformance` ঘর, `salaryFor`-এর
  হুবহু একই প্যাটার্ন)।
- ⛔ ড্রিল-ডাউন (স্টাফের নামে চাপ → তাঁর এনকোয়ারি/রোগীর তালিকা) **অক্ষত**।
- ⛔ কোনো হিসাব বদলায়নি — শুধু একটা লাইন ও একটা বোতাম যোগ।

---

## যাচাই

| যাচাই | ফল |
|---|---|
| V523 Reports (আসল হিসাব-লুপ হুবহু তুলে) | ২৩/২৩ PASS |
| V522 Report Card (Android ২০ · Web ৮) | ALL PASSED |
| V521 Extra-income কারণ (Android ১৭ · Web ১৫) | ALL PASSED |
| V520 walk-in + offline (৩৪) · ওয়েব টাকা (১৪) | ALL PASSED |
| V516 Registration · V517 Search · Timeline | ALL PASSED |
| ওয়েব: webfu · webfam · websearch · dq · mask · rc | ALL PASSED |
| ৮টি ওয়েব ফাইল `node --check` | ৮/৮ OK |

পরীক্ষার কোড **আসল ফাইল থেকে হুবহু তোলা** — নকল করে লেখা নয়।

## সৎ সীমাবদ্ধতা

**Activity ফাইল কম্পাইল করা যায়নি** — Android SDK (`dl.google.com`) এই
মেশিন থেকে পৌঁছয় না (403)। যা compile করে চালানো গেছে: `PaymentModel`-এর
আসল ফাংশনগুলো ও `showBranchDetail()`-এর **পুরো হিসাব-লুপ** — হুবহু তোলা ও
প্রমাণিত। বাকিটা হাতে যাচাই — একটা ভুল এভাবেই ধরা পড়েছে ও ঠিক করা হয়েছে:
`dp()` এই ফাইলে শুধু পপ-আপ-ফাংশনের **ভিতরের** সহায়ক, `renderSummary()`-তে
নেই; তাই সেখানে স্থানীয় `sdp()` বানানো হয়েছে। layout XML-ও well-formed
কিনা যাচাই করা হয়েছে, আর ব্রেস-ভারসাম্য `git HEAD`-এর সঙ্গে মিলিয়ে দেখা
হয়েছে (চারটে ফাইলেই `+0`)। **TK-কে বিল্ড করে একবার দেখে নিতে হবে।**

## ⛔ যেখানে কিছুই বদলায়নি
- Database schema · RLS · production data — **ছোঁয়া হয়নি**
- Supabase-এ **একটাও বাড়তি query নেই**
- Reports-এর কোনো সংখ্যা/যোগফল — পরীক্ষায় প্রমাণিত
- Follow-Up · Collection · Staff Performance পর্দার নিজস্ব নিয়ম অক্ষত
