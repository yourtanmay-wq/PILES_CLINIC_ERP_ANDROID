# V383 — Android + Web সম্পূর্ণ প্রজেক্ট

- `Ref. Paid` চাপলে RMP-কে সরাসরি টাকা দেওয়ার ফর্ম খুলবে; Patient list খুলবে না।
- Payment Date-এ বর্তমান তারিখ থাকবে; আগের তারিখ বাছা যাবে; ভবিষ্যৎ তারিখ নিষিদ্ধ।
- Payment সফল হলে Ref. Paid বাড়বে এবং Ref. Due একই টাকায় কমবে। পরে Patient-এর সঙ্গে মিলালে একই টাকা দ্বিতীয়বার গণনা হবে না।
- Paid ঘর হালকা সবুজ এবং Due ঘর হালকা লাল। দৃশ্যমান `Unpaid`-এর পরিবর্তে `Due` রাখা হয়েছে; পুরোনো Cloud value অপরিবর্তিত।
- Android versionCode `383`, versionName `3.83`; Web cache version `v383`। বাইরের ZIP ও ভিতরের সংস্করণ একই V383।
- Supabase-এ `04_SUPABASE_DATABASE_SETUP/V383_RMP_DIRECT_PAYMENT_DUE_CORRECTION_2026-08-14.sql` একবার Run করতে হবে।

## যাচাই

- Web JavaScript syntax: Passed.
- Android source/version/file structure: Checked.
- এই পরিবেশে Gradle 8.5 download বন্ধ থাকায় পূর্ণ Android Build চালানো যায়নি; Android Studio-তে প্রথম Build-এ প্রয়োজনীয় Gradle download হতে পারে।
