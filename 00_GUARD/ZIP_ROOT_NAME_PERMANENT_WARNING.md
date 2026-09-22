# ⛔ স্থায়ী সতর্কবার্তা — ZIP ও ভেতরের নাম

ফাইল পাঠানোর আগে `verify_zip_root_name.py` চালানো বাধ্যতামূলক।

- বাইরের ZIP নামের `.zip` বাদ দিলে যে নাম থাকে, ভেতরের একমাত্র root folder-এর নাম হুবহু সেটিই হতে হবে।
- একটি অক্ষর, `_FINAL`, version বা suffix-ও আলাদা হলে ফাইল পাঠানো নিষিদ্ধ।
- Android `versionCode/versionName`, Dashboard `V...` এবং Web cache version-ও একই release হতে হবে।
- এই পরীক্ষা PASS না করলে Library-তে upload/replace বা ব্যবহারকারীকে file link দেওয়া যাবে না।

V380-এ TK-এর live objection-এর পরে এই নিয়ম স্থায়ীভাবে যোগ করা হলো।
