# V228 — Changed files

**Base:** V227। **Build:** এই cloud-এ Android SDK নেই → build owner-এর মেশিনে; FINAL নয়।

## এই version-এর কাজ
নাম ফাঁকা/null/missing হলে যেখানে **"Name Not Available"** দেখাত, সব জায়গায় শুধু ওই লেখা বদলে **"UNKNOWN"** করা হয়েছে। কোনো রোগীর আসল নাম বা database data বদলানো হয়নি; শুধু display fallback text। কোনো design/card/font/size/layout/workflow বদলায়নি।

## পরিবর্তিত ফাইল (১৮টি)
- Android display fallback "Name Not Available" → "UNKNOWN": ১৫টি `.kt` ফাইল (DoctorQueueAdapter, DoctorVisitActivity, DoctorVisitAdapter, FollowUpActivity, FollowUpAdapter, DraftCardAdapter, DraftRepository, PatientTimelineActivity, PatientTimelineRepository, PaymentActivity, ReportCardActivity, BriefingActivity, ChamberAttendanceActivity, ChamberAttendanceAdapter, PasswordCenterAdapter)।
- `app/build.gradle.kts` — version 228 / 2.28।
- `assets/www/index.html` ও `03_NETLIFY_READY/index.html` — web `?v=v228`।

## Web App ও Website প্রসঙ্গে (সৎ নোট)
Web App-এ ও Website-এ **"Name Not Available" লেখাটি কোথাও নেই** — তাই বদলানোর মতো ওই text নেই। Web-এ নাম ফাঁকা হলে সাধারণত **ফোন নম্বর** দেখানো হয় (আলাদা placeholder নয়)। ফোন নম্বরের বদলে "UNKNOWN" দেখানো একটি আচরণ-পরিবর্তন হবে, তাই owner-এর নিশ্চিতি ছাড়া করা হয়নি (নিয়ম ৪)। owner চাইলে web-এও নাম ফাঁকা হলে "UNKNOWN" দেখানো যাবে।

## Rollback
`ROLLBACK_V228/` — ১৮টি ফাইলের সত্যিকারের pre-V228 (=V227) কপি (কার্যকর, যাচাইকৃত)।

## যা বদলানো হয়নি
এই ১৮টি ছাড়া কিছু নয়; design/layout/font/size/card/workflow/payment/permission/branch/print/login/data/`.git`/asset সব অক্ষত।
