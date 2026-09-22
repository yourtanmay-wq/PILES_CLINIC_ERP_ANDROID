# 🔒 LOCK NOTE — V212 (৩১.০৭.২০২৬)

**ভার্সন:** `versionCode 212` · `versionName 2.12` · পর্দায় **V212** · খাতার সারি **B215** পর্যন্ত
⛔ **কোনো SQL লাগবে না।**

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## 📋 এই ভার্সনে যা যা এসেছে (V211 → V212)

| সারি | কী হলো | ফাইল |
|---|---|---|
| B213 | Follow-up-এর Enquiry/Visit/Patient তিন ট্যাবেই নতুন "My Call" ফিল্টার (All-এর ঠিক পরে) — এই স্টাফ শেষ যাঁর সাথে কথা বলেছেন তাঁরাই, তারিখ যাই হোক। `item.lastCallBy` ব্যবহার করে, নতুন কোনো ক্লাউড-কল/কলাম লাগেনি। | FollowUpActivity.kt, activity_followup.xml |
| B214 | Follow-up-এ NEXT CALL, LAST CALL-এর চেয়েও পুরনো তারিখ দেখানোর আসল কারণ — ওয়েব অ্যাপের "Add Remark → Save Remark" পথে next-follow-date কখনো আপডেট হত না। এখন রিমার্ক সেভের পরেই ক্যালেন্ডার পপ-আপ খোলে (ফোনের অ্যাপ আগে থেকেই নিরাপদ)। | 03_NETLIFY_READY/app.js, assets/www/app.js |
| B215 | নতুন Enquiry Save হলে 📶 Wifi Signal (কল-সিগন্যাল বার) ভুল করে ০ দেখাত — এখন ১ (Save হওয়াটাই প্রথম "কল")। ঠিকানা-ট্যাগ Enquiry কার্ডে আগে থেকেই auto কাজ করে বলে যাচাই করে নিশ্চিত করা হয়েছে। | EnquiryModel.kt, 03_NETLIFY_READY/app.js, assets/www/app.js |

বিস্তারিত ব্যাখ্যা `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এ B213–B215।

---

## 🔴 বাকি সিদ্ধান্ত
কিছুই নেই।

## 🔴 TK-কে করতে হবে
শুধু **লাইভ টেস্ট**:
- Follow-up-এর "My Call" ফিল্টার (All-এর পাশে) তিন ট্যাবেই
- একটা নতুন রিমার্ক লিখে দেখা — ক্যালেন্ডার পপ-আপ উঠছে কিনা
- একটা নতুন Enquiry Save করে 📶 সিগন্যাল ১ বার দেখাচ্ছে কিনা (পুরনো Enquiry-র সিগন্যাল এই ফিক্সে বদলাবে না)

## ⛔ কোনো SQL লাগবে না।

---

## 🔍 ফাইল দেওয়ার আগে চূড়ান্ত যাচাই (৩১.০৭.২০২৬)

- পরিবর্তিত সব Kotlin ফাইলে ব্র্যাকেট/প্যারেন গোনা পাশ (FollowUpActivity.kt, EnquiryModel.kt)
- `activity_followup.xml` well-formed যাচাই পাশ
- `03_NETLIFY_READY/app.js` ও `assets/www/app.js` — দুই ফাইলেই `node --check` পাশ
- `kotlinx.coroutines.async(...)`/`launch(...)` fully-qualified প্যাটার্ন (V209-এর RED ALERT বাগ) — নতুন কোডে কোথাও নেই
- `callCount == 0` বা এই ধরনের কোনো বিশেষ-শর্ত-চেক প্রজেক্টে অন্য কোথাও নেই বলে নিশ্চিত হওয়া হয়েছে (তাই B215-এর ফিক্স অন্য কিছু ভাঙেনি)
