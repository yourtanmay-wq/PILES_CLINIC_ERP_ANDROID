# 🔎 বোতামের গায়ে বাংলা লেখা — সম্পূর্ণ প্রজেক্ট খুঁজে পাওয়া তালিকা

**তারিখ:** ২৩.০৮.২০২৬ · **TK-এর নির্দেশ:** *"দেখুন / A4 প্রিন্ট / হোয়াটসঅ্যাপে
পাঠান — এই ধরনের বাংলা লেখা থাকবে না, ইংরেজিতে হবে সব। তাছাড়া সম্পূর্ণ
প্রজেক্ট খুঁটিয়ে খুঁটিয়ে দেখুন এই ধরনের বাংলা কোথায় কোথায় আছে।
**নিজে থেকে সরাবেন না**, প্রয়োজনে আমার থেকে জিজ্ঞাসা করে তবেই সরাবেন।"*

⛔ **এই তালিকার একটাও লেখা এখনো বদলানো হয়নি** — শুধু খুঁজে দেখা হয়েছে।
✅ **শুধু নতুন যে কাজটা এখনো লেখাই হয়নি** (চেক-আপ হিস্ট্রির তিনটে বোতাম),
   সেটা শুরু থেকেই ইংরেজিতে হবে: **View · A4 Print · Send on WhatsApp**।

যন্ত্র দিয়ে খোঁজা হয়েছে — Android-এর সব layout XML-এর `<Button>`,
Kotlin-এর সব পপ-আপ বোতাম (`setPositive/Negative/NeutralButton`), আর
ওয়েবের সব `<button>`। **মোট ৩৪টা** পাওয়া গেছে।

---

## ১. ফোনের পর্দার বোতাম (৫টা)
| এখন | কোথায় | প্রস্তাবিত ইংরেজি |
|---|---|---|
| 🔓 Reopen-এর অনুরোধ পাঠান (Master-এ) | `activity_chamber_attendance.xml:181` | 🔓 Send Reopen Request (to Master) |
| ✅ Close Chamber (Save & Print) — বাংলা অংশ | `activity_chamber_attendance.xml:331` | ✅ Close Chamber (Save & Print) |
| Registration / নতুন পেশেন্টের নাম লেখা | `item_chamber_header.xml:27` | Registration / New patient entry |
| Mark Expected / আসার কথা | `item_chamber_header.xml:43` | Mark Expected |
| Patient / পেশেন্ট খুঁজুন | `item_chamber_header.xml:59` | Patient / Search |

## ২. ফোনের পপ-আপের বোতাম (২৫টা)
| এখন | কোথায় | প্রস্তাবিত ইংরেজি |
|---|---|---|
| বাতিল | `DoctorCheckupActivity.kt:924` | Cancel |
| যোগ করুন | `DoctorCheckupActivity.kt:925` | Add |
| হ্যাঁ · না | `DoctorCheckupActivity.kt:1804-1805` | Yes · No |
| ← ফিরে যান | `DoctorCheckupActivity.kt:2082` | ← Back |
| ঠিক আছে | `PrescriptionActivity.kt:157` | OK |
| সেভ করুন | `ChamberAttendanceActivity.kt:1656` | Save |
| অনুরোধ পাঠান | `ChamberAttendanceActivity.kt:3331` | Send Request |
| বন্ধ করুন · বন্ধ | `ChamberAttendanceActivity.kt:3348, 3715` | Close |
| আবার দেখুন | `RegistrationActivity.kt:778` | Check Again |
| তবুও সেভ করুন | `RegistrationActivity.kt:779` | Save Anyway |
| হ্যাঁ, আলাদা রোগী | `RegistrationActivity.kt:974` | Yes, Different Patient |

## ৩. কম্পিউটারের বোতাম (৪টা)
| এখন | কোথায় | প্রস্তাবিত ইংরেজি |
|---|---|---|
| পরে পাঠাব | `app.js:3988` | Send Later |
| 📞 কল | `app.js:17457` | 📞 Call |
| ⏰ পরের তারিখ | `app.js:17460` | ⏰ Next Date |
| **বাংলা (Bengali)** | `app.js:17787` | ⚠️ **এটা ভাষা বাছাইয়ের বোতাম** — ভাষার নিজের নাম, তাই বাংলাই থাকা উচিত |

---

## ❓ TK-কে যা জিজ্ঞাসা করার

১. উপরের **৩৩টা** বোতাম কি ইংরেজি করে দেব? (৩৪ নম্বরটা — "বাংলা (Bengali)" —
   ভাষার নাম, ওটা বাদ রাখার পরামর্শ দিচ্ছি)
২. নাকি **শুধু কয়েকটা** — যেগুলো আপনি বেছে দেবেন?
৩. **বোতাম ছাড়াও** কি লেবেল/শিরোনামের বাংলা (যেমন "প্রধান সমস্যা",
   "রোগ ও অভ্যাস") ইংরেজি করতে হবে? ⚠️ ওগুলো **শত শত** জায়গায় আছে আর
   ডাক্তার-স্টাফের রোজকার কাজে লাগে — তাই আলাদা করে অনুমতি ছাড়া ছোঁব না।

⛔ **আপনার "হ্যাঁ" না পেলে একটা অক্ষরও বদলাব না।**
