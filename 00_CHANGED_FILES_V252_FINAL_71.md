# পরিবর্তিত ফাইলের সম্পূর্ণ তালিকা — এই ডেলিভারি (V252_FINAL_71)

তুলনা করা হয়েছে সরাসরি TK-এর কাছে সর্বশেষ পাঠানো `PILES_CLINIC_APP_V252_FINAL_31.zip`-এর সাথে (`diff -rq`, পুরো প্রজেক্ট)। এই ডেলিভারিতে B315 থেকে B338 পর্যন্ত সব কাজ ধরা আছে, প্লাস সাইজ-পরিষ্কার।

## 🚨🚨🚨 সবচেয়ে জরুরি — এই ডেলিভারিতে যা ঠিক হলো

**B338 — আসল Build Error ফিক্স:** TK-এর Android Studio-তে `BriefingActivity.kt`-এ "No value passed for parameter 'mine'" + boolean-type-mismatch এরর এসেছিল। কারণ: B335-এ `bubble()` ফাংশনে নতুন `title` প্যারামিটার যোগ করার সময় ৩টার মধ্যে ১টা কল-সাইট বাদ পড়ে গিয়েছিল। এখন ঠিক করা হয়েছে, আর এই সেশনের সব নতুন/বদলানো ফাংশনের সব কল-সাইট আবার হাতে গুনে মেলানো হয়েছে।

## 🔴 সাইজ পরিষ্কার (TK-এর নির্দেশে)

- `.git` ফোল্ডার সরানো হয়েছে (TK-এর স্পষ্ট অনুমতিতে — দায়বদ্ধতার নোট খাতায় লেখা আছে)। পাহারাদারের নিয়ম ৪.৫ সেই অনুযায়ী আপডেট।
- `09_ORIGINAL_UPLOADED_FILES/` (পুরনো zip স্ন্যাপশট) সরানো হয়েছে।
- পুরনো `ROLLBACK_V221`...`ROLLBACK_V240` (এই সেশনের আগে থেকে ছিল) সরানো হয়েছে।
- বর্তমান রোলব্যাক (`ROLLBACK_V252_FINAL_31_BEFORE_SESSION2`) **সরানো হয়েছে** — TK-এর নির্দেশে ("এখনো লাইভ টেস্ট চলছে, ফাইনাল না হওয়া পর্যন্ত রোলব্যাক-সিস্টেম বন্ধ")। **⚠️ TK নিজে প্রতিটা ডেলিভার করা ZIP সেভ করে রাখবেন — ZIP-এর ভিতরে আর ব্যাকআপ কপি নেই, যতক্ষণ না সব ফাইনাল পাশ হয়ে রোলব্যাক-সিস্টেম আবার চালু হয়।**
- **সাইজ:** ৯৭MB → **~২১MB**।

## বদলানো/নতুন কোড-ফাইল

1. `PilesClinicApplication.kt` — B325/B326: IN TIME/OUT TIME রিমাইন্ডার শিডিউল চালু।
2. `modules/ModuleAuth.kt` — B316: `getRowsChecked()` (additive)। B317: `expectedCode()`।
3. `modules/ModuleUi.kt` — B317: `ensureSignedIn()`-এ Module-পরিচয় মেলানো।
4. `modules/StaffProfileActivity.kt` — B316: Save-race ফিক্স। B318: খালি বক্স ফিক্স।
5. `modules/WorkNotebookActivity.kt` — B319–B333: IN/OUT TIME, Mark as Leave, hero ডিজাইন, spacing, null-ফিক্স, Time picker, Notes সরানো, Outside Calls সংখ্যা, Daily Report reformat, race-condition ফিক্স, "Today Patient"।
6. `native/AttendanceReminderScheduler.kt` (নতুন) — B325/B326।
7. `native/AttendanceReminderWorker.kt` (নতুন) — B325/B326।
8. `native/BackdatePaymentGrant.kt` (নতুন) — B337।
9. `native/BriefingActivity.kt` — B335: চ্যাট বাবল ফরম্যাট। B337: "Backdate Payment Permissions" নতুন সেকশন।
10. `native/ChamberAttendanceActivity.kt` — B334: ডাবল-ট্যাপ গার্ড (payment delete)।
11. `native/DraftListActivity.kt` — B334: ডাবল-ট্যাপ গার্ড (২ জায়গা)।
12. `native/MoreMenuActivity.kt` — B315: Logout + Module সাইনআউট।
13. `native/PatientTimelineActivity.kt` — B334: ডাবল-ট্যাপ গার্ড (Enquiry delete)।
14. `native/PaymentActivity.kt` — B334: ডাবল-ট্যাপ গার্ড। B337: Grant-চেক + `doDirectSave()` রিফ্যাক্টর।
15. `native/PaymentRepository.kt` — B336: Patient ID কলাম ফিক্স। B337: ডিলিটে Grant-চেক।
16. `res/layout/activity_briefing.xml` — B337: নতুন `backdateGrantsContainer`।
17. `03_NETLIFY_READY/app.js` — B315: Logout confirmation + Module signout।
18. `03_NETLIFY_READY/index.html` — B315/B317: cache-বাস্টার আপডেট।
19. `03_NETLIFY_READY/module_core.js` — B315/B317: signOut + expectedCode + gate() মেলানো।
20. `04_SUPABASE_DATABASE_SETUP/V252_BACKDATE_PAYMENT_GRANT.sql` (নতুন) — B337 (TK ইতিমধ্যে চালিয়েছেন, "Success" নিশ্চিত)।
21. `00_GUARD/tk_guard.py` — নিয়ম ৪.৫ আপডেট (.git বাধ্যতামূলক না, TK-অনুমতি)।

## বদলানো নোট-ফাইল

22. `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md` — B315 থেকে B337 + সাইজ-পরিষ্কার + দায়বদ্ধতার নোট + রোলব্যাক-পুনরায়-চালুর ভবিষ্যৎ পরিকল্পনা।
23. `00_TK_KAJER_TARIKH_SOMOY_LOG.md` — সংশ্লিষ্ট তারিখ-সময় সারি।

## ছোঁয়া হয়নি

- Android `versionCode`/`versionName` অপরিবর্তিত (252/2.52)। RLS ছোঁয়া হয়নি। কোনো ডিজাইন/কাজের লজিক বদলায়নি (শুধু ব্যাকআপ-ফাইল সরানো)।
