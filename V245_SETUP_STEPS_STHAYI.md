# V245 মডিউল (Staff Profile · Work Notebook · Income-Expense) — সেটআপ গাইড

সহজ কথায়: শুধু **একটাই SQL** একবার চালাতে হবে। বাকি সব আগে থেকেই কোডে করা আছে।

## যা করতে হবে (Supabase SQL Editor-এ)

1. Supabase খুলুন → SQL Editor
2. এই ফাইলের পুরো লেখা কপি করুন: `04_SUPABASE_DATABASE_SETUP/V249_KEEP_EXISTING_PASSWORDS_ONE_RUN.sql`
3. পেস্ট করে **Run** চাপুন
4. "Success. No rows returned" দেখলেই কাজ শেষ

⛔ **V246 / V247 / V248-এর SQL আলাদা করে চালানোর দরকার নেই** — V249-এর SQL-ই এখন চূড়ান্ত, আগেরগুলো এর ভিতরেই ধরা আছে।

## এরপর কী হবে

- পাসওয়ার্ড বদলাবে না — Master `admin123`, Staff `staff123`, Doctor `doctor123`, Field `field123` — এগুলোই থাকবে।
- Work Notebook / Profile / Income-Expense খুললে **আলাদা করে আর কোনো পাসওয়ার্ড চাইবে না** — মূল লগইনই যথেষ্ট।
- পুরনো কোনো ফিচার (রোগী, পেমেন্ট, ফলো-আপ) এতে ছোঁয়া হয়নি।

## এরপর আপনাকে যা করতে হবে

1. Android Studio-তে নতুন APK বানানো
2. `V245_LIVE_TEST_CHECKLIST.md` ধরে নিজে একবার হাতে-কলমে টেস্ট করা
3. ঠিক থাকলে তবেই স্টাফদের হাতে APK দেওয়া
