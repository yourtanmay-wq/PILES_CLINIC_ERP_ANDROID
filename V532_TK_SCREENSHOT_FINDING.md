# TK-এর ছবিতে ধরা পড়া — কোড ধরে যাচাই করা আসল কারণ (২২.০৮.২০২৬)

TK-এর প্রশ্ন: *"পেমেন্টের এখানে ড্রেসিং-এর কথা উল্লেখ কেন হবে? তাছাড়া
জিরো পেমেন্ট আবার কেন দেখাবে এখানে?"*

## সমস্যা ১ — ₹0 "Marked Arrived" সারি পেমেন্টের তালিকায়

**কোথায়:** `PaymentActivity.showCollectionDetails()`
**কারণ:** এটা **ইচ্ছে করে** দেখানো হয়। কোডে লেখা মন্তব্য (২০২৬-০৭-১৬):
> *"Chamber Attendance's 'Marked Arrived' (₹0, payType="attendance_mark")
> entries still show in the list below (**TK wanted that**), but they are NOT a
> real payment, so they should not inflate the 'X টি পেমেন্ট' count."*

⇒ অর্থাৎ **TK নিজেই আগে এটা চেয়েছিলেন**। এখন চান না। এটা সিদ্ধান্ত বদল,
কোডের ভুল নয়। **সরিয়ে দিতে এক লাইনই যথেষ্ট** এবং সেটা নিরাপদ:
`attendance_mark` · `chamber_expected` · `bill_edit` — এই তিনটে ইতিমধ্যেই
"আসল পেমেন্ট নয়" বলে গোনায় বাদ যায় (`realPaymentCount`), তালিকাতেও বাদ দিলে
টাকার কোনো হিসাব বদলায় না।

## সমস্যা ২ — 🔴 এটাই গুরুতর: পেমেন্টের নিজের Remark **মুছে যাচ্ছে**

**কোথায়:** `ChamberAttendanceActivity.kt:2298` এবং `PatientTimelineActivity.kt:2889`

Chamber-এ চিকিৎসার নোট ("DRESSING করা হল") লিখলে অ্যাপ সেই লেখাটা **আজকের
প্রতিটা payment সারির `remarks` ঘরে** বসিয়ে দেয়:

```
todaysPayments = fetchList("payments", "mobile=like.*<digits>&date=eq.<today>", 20)
… প্রত্যেকটায় → updateById("payments", pid, {"remarks": text})
```

`bill_edit` ও `chamber_expected` বাদ যায় — কিন্তু **Advance/Treatment ও
attendance_mark বাদ যায় না**। তাই ছবিতে ₹1,000 Advance আর দুটো ₹0 Marked
Arrived — তিনটেতেই "DRESSING করা হল"।

**এটা কেন বসানো হয়েছিল:** TK-এর ২৪.০৭.২০২৬-এর অনুরোধ — Report Card-এর
Progress কলাম payments.remarks থেকে পড়ে, তাই দুই পর্দা যেন এক থাকে।

**🔴 আসল ক্ষতি (শুধু দেখার সমস্যা নয়):** স্টাফ পেমেন্ট ফর্মে নিজের হাতে যে
Remark লেখেন (`PaymentActivity.kt:1068`), Medicine পেমেন্টের remark
(`MedicinePaymentActivity.kt:320`), এমনকি **Refund-এর কারণ**
(`PaymentModel.kt:798`) — সেদিন Chamber-এ নোট লিখলে **এগুলো মুছে গিয়ে**
চিকিৎসার নোট বসে যায়।

**⛔ তাই এটা নিজে থেকে বদলানো হয়নি** — সমাধানে TK-এর সিদ্ধান্ত দরকার,
কারণ যেটাই করি তাতে TK-এরই আগের একটা ভাল কাজে হাত পড়ে।
