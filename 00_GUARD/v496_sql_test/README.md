# V496 SQL — স্থানীয় PostgreSQL-এ চালানো পরীক্ষা

⛔ **লাইভ Supabase-এ কিছুই চালানো হয়নি।** এগুলো একটা আলাদা, খালি
PostgreSQL 16-এ চালানো হয়েছে — TK-এর তথ্য ছোঁয়া হয়নি।

    psql -f harness.sql              # নকল hr/wn schema ও ৭ জন পরীক্ষার লোক
    psql -f ../../04_SUPABASE_DATABASE_SETUP/V496_MARK_CHECK_IN_2026-08-21.sql
    psql -f test_roles.sql           # প্রতিটি ভূমিকা
    psql -f test_once_a_day.sql      # দিনে একবার

## ফল

| পরীক্ষা | ফল |
|---|---|
| সাধারণ Staff — প্রথমবার | `saved` · 08:31 · Kishanganj ✅ |
| একই Staff — দ্বিতীয়/তৃতীয়বার | `already`, সময় **বদলায়নি** ✅ |
| পুরনো সময় (07:05) বসিয়ে আবার | `already` · **07:05 অটুট** ✅ |
| ডাক্তার | `not_staff` — হাজিরা বসেনি ✅ |
| মাস্টার | `not_staff` ✅ |
| বাদ দেওয়া (active=false) | `inactive` ✅ |
| Suspend করা | `suspended` (তারিখসহ) ✅ |
| আজ অনুমোদিত ছুটি | `on_leave` ✅ |
| Field অফিসার | `saved` — আগের মতোই পান ✅ |
| লগইন ছাড়া (anon) | **আটকেছে** — `Sign-in required` ✅ |
| **৮টা একসাথে (৮ সংযোগ)** | **১টা `saved`, ৭টা `already`, DB-তে ১টাই সারি** ✅ |

## এই পরীক্ষাতেই দুটো আসল বাগ ধরা পড়েছে ও ঠিক হয়েছে

1. `returns table(... work_date ...)`-এর নাম টেবিলের ঘরের নামের সঙ্গে সংঘর্ষ
   করছিল → `column reference "work_date" is ambiguous`।
   সমাধান: `#variable_conflict use_column`।
2. একই মিনিটে দ্বিতীয়বার চাপলে ভুল করে `saved` দেখাত (যদিও সারি অপরিবর্তিতই
   ছিল)। সমাধান: সময় মেলানোর বদলে `get diagnostics row_count`।
