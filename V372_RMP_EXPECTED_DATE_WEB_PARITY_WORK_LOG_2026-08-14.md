# 14-08-2026 — RMP Expected Patient Date Web/Desktop parity

- মালিকের অনুমতি অনুযায়ী শুধু Web/Desktop-এর RMP Call Remarks অংশে Android-এর সমান `Expected Patient Date` যোগ করা হয়েছে।
- `Next Call Date`-এর আগের কাজ বা নিয়ম পরিবর্তন করা হয়নি।
- Expected Patient Date ঐচ্ছিক; আগের তারিখ থাকলে ফর্মে অক্ষত থাকে, Clear করলে তবেই ফাঁকা হয়।
- সেভ করা Expected তারিখ আলাদা Expected Patient তালিকা এবং RMP কার্ডে দেখা যায়।
- নতুন Supabase টেবিল/কলাম বা নতুন SQL লাগেনি; আগে থেকেই থাকা `expectedPatientDate` ঘর ব্যবহার করা হয়েছে।
- JavaScript syntax check পাস করেছে। Android source পরিবর্তন করা হয়নি।
- মালিক ফাইল চাইবার আগে কোনো ZIP পাঠানো হবে না।

## 14-08-2026 — RMP Referral Income তিনবার চাপ সংশোধন

- V370 লাইভ স্ক্রিনে Referral Income সারিতে চাপলে ভুলভাবে Patient Report Card খুলছিল—কোডে সত্যতা যাচাই করে কারণ পাওয়া হয়েছে।
- Android-এ Referral Income সারির Report Card সংযোগ শুধু এই জায়গা থেকে সরানো হয়েছে।
- এখন একবার চাপলে কোনো ভুল পাতা খুলবে না; পরপর তিনবার দ্রুত চাপলে শুধু অনুমোদিত Edit/Delete খুলবে।
- Referred Patient সারির স্বাভাবিক Patient Timeline কাজ অপরিবর্তিত।
- Web/Desktop আগে থেকেই Referral Income সারিতে তিনবার চাপলে Edit/Delete খুলত এবং Report Card খুলত না; তাই Web কোড পরিবর্তনের প্রয়োজন হয়নি।
- Android build পরীক্ষা চালানো হয়েছিল; কোডের error দেখায়নি, কিন্তু এই পরিবেশে internet বন্ধ থাকায় Gradle 8.5 ডাউনলোড শুরুতেই থেমেছে। তাই Android Studio build সফল হয়েছে—এমন মিথ্যা দাবি করা হয়নি।
