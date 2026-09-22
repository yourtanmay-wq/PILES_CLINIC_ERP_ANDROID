# V358 ANDROID + WEB PARITY — WORK LOG

**তারিখ:** 13.08.2026 IST  
**কাজের কর্তা:** ChatGPT

## Web-এ মিলিয়ে সম্পন্ন

- Login: `TK BISWAS` এক লাইন; role-login লেখা ও নিচের clinic/footer লেখা বাদ; Login button সবুজ।
- আলাদা `Staff Photos` menu বাদ।
- Master Staff Profile-এর ভিতর সরাসরি Add/Change Photo; পুরনো local photo fallback হিসেবে অক্ষত।
- Master Profile Edit/Save ও Salary Settings/Save কার্যকর পথ যাচাই করা হয়েছে।
- Master Doctor/RMP-তে branch selector; নির্বাচিত branch সরাসরি Cloud থেকে আসে, তাই বড় all-branch তালিকার সীমায় কম দেখায় না।
- কোনো নতুন SQL/table/bucket নেই; Free Plan-এ all-branch অপ্রয়োজনীয় download কমেছে।
- Android-এর V355–V357 কাজ অক্ষত।

## যাচাই

- সব Web JavaScript syntax: PASS।
- সব Android XML: PASS।
- Android Gradle Build চেষ্টা করা হয়েছে; এই পরিবেশে প্রয়োজনীয় Build file download-এর সংযোগ না থাকায় Build শুরু হয়নি। তাই Android Studio Build/real-device Live Test-কে মিথ্যাভাবে PASS বলা হয়নি।
