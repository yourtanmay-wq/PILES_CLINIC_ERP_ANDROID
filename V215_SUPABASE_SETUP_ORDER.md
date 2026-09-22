# V215_SUPABASE_SETUP_ORDER.md — SQL কোনটা আগে, কোনটা পরে

⛔ **পুরোনো কোনো SQL ফাইল মোছা হয়নি** (§8.3)। নিচে শুধু কোন ফাইল **Current Final** ও কোন ক্রমে চালাতে হবে।

## Live DB-তে চালানোর ক্রম (নতুন কিছু থাকলে)

1. **`04_SUPABASE_DATABASE_SETUP/PILES_CLINIC_DB_SETUP.sql`** — মূল base setup (নতুন project হলে একবার)। পুরোনো project-এ আবার চালাতে হয় না।
2. পুরোনো PATCH ফাইলগুলো (তারিখ অনুসারে, যেগুলো আগে চালানো হয়নি) — 2026-07-18 থেকে 2026-07-30 পর্যন্ত। **যেগুলো আগেই live-এ চালানো, আবার চালাবেন না** (`add column if not exists` বলে ক্ষতি নেই, তবু অপ্রয়োজন)।
3. **➡️ CURRENT FINAL (V215): `V215_SAFE_MIGRATION_2026-07-31.sql` — PART A শুধু।**
   - PART A নিরাপদ, additive: `usercredentials.password_hash`/`password_algo` কলাম, `payments`-এ refund কলাম। এখনই চালানো যায়, কিছু ভাঙে না, কোনো data মোছে না।
   - PART A3-এর duplicate-finder SELECT গুলো চালিয়ে **ফল দেখুন** (duplicate patient/orphan payment আছে কিনা)।

## ⛔ এখন চালাবেন না (আলাদা, সতর্ক ধাপ)
- **PART B (UNIQUE/FK):** আগে PART A3 duplicate-finder ফাঁকা হতে হবে। duplicate থাকলে আগে ঠিক করুন, নইলে UNIQUE fail করবে। তারপর একটা একটা করে, backup নিয়ে।
- **PART C (RLS enable):** ⛔⛔ **TK-এর সরাসরি নির্দেশ: এখন নয়।** RLS চালু করলেই anon key আটকে **live app বন্ধ** হবে। আগে Supabase Auth (`V215_MANUAL_SETUP_IF_REQUIRED.md` ধাপ ২)।

## Duplicate দেখার পুরোনো SQL (রেফারেন্স, শুধু SELECT)
`DEKHAR_SQL_2026-07-28_DUPLICATE_ROGI.sql`, `..._DUPLICATE_ASOL_TAKA.sql`, `..._DUPLICATE_SIDDHANTO.sql` — এগুলোও duplicate খুঁজতে ব্যবহার করা যায়, কিছু বদলায় না।

## কোনটা আর ব্যবহার হবে না
`SUPABASE_SETUP_FROM_MASTER_BASE.md`-এর ভিতরের RLS/policy নমুনা টেবিল-নাম (`registrations`, `follow_ups`) **আসল schema-র সঙ্গে মেলে না** (আসল: `patients`, `followups`) — ওটা কখনো চালানো হয়নি, চালাবেনও না। V215-এর PART C-ই সঠিক রেফারেন্স।

---

## ✅ চালানো হয়ে গেছে — খাতা (SQL LEDGER)

| তারিখ | ফাইল | কী করেছে | ফল |
|---|---|---|---|
| **২২.০৮.২০২৬** | `V533_PAYMENTS_PROGRESS_COLUMN_2026-08-22.sql` | `payments`-এ নতুন ফাঁকা ঘর `progress` (+ PostgREST schema reload) | ✅ **TK নিজে চালিয়েছেন — "Success. No rows returned"** (প্রজেক্ট `biswasayurvedkis…`, branch `main`) |

**কেন লাগল:** Chamber-এ চিকিৎসার নোট ("DRESSING করা হল") লিখলে অ্যাপ সেটা ওইদিনের
প্রতিটা payment সারির `remarks` ঘরে বসিয়ে দিত — ফলে স্টাফের লেখা পেমেন্ট-Remark,
Medicine-এর Remark, এমনকি **Refund-এর কারণ** মুছে যেত। এখন নোটের নিজের আলাদা ঘর।

**⛔ আর চালাতে হবে না।** দু'বার চালালেও ক্ষতি নেই (`if not exists`), কিন্তু দরকার নেই।

**⛔ কোনো পুরোনো তথ্য মোছেনি/বদলায়নি।** পুরোনো সব সারিতে `progress` ফাঁকা, তাই অ্যাপ
সেখানে আগের মতোই `remarks` পড়ে — একটা পুরোনো নোটও হারায়নি।

**এর সাথে যে ভার্সন লাগে:** **V533 বা তার পরের বিল্ড** (V534 চলছে)।

---

## 🟠 Supabase বিলিং — TK-এর পর্দায় দেখা সতর্কবার্তা (২২.০৮.২০২৬)

TK-এর SQL Editor-এর ছবিতে উপরে হলুদ বার্তা:

> **"Grace period is over — Your projects will not be able to serve requests
> when you use up your quota. Review billing"**

**এর মানে:** Free Plan-এর ছাড়ের সময়টা শেষ। **কোটা ফুরিয়ে গেলে প্রজেক্ট
অনুরোধ নেওয়া বন্ধ করে দেবে** — অর্থাৎ অ্যাপ ও ওয়েব দুটোই সাময়িকভাবে
কাজ করবে না।

⛔ এটা কোডের কোনো সমস্যা নয়, আর এই সেশনের কোনো কাজের ফলও নয় — এটা
Supabase-এর নিজের অ্যাকাউন্ট/বিলিং-এর ব্যাপার।

**TK-এর করণীয়:** Supabase-এ *Review billing*-এ গিয়ে দেখে নেওয়া কোটা কতটা
বাকি আছে। ⛔ এই সেশনে বিলিং-এর কিছুই ছোঁয়া হয়নি ও হবে না — TK-এর সিদ্ধান্ত।

*(প্রসঙ্গত: এই প্রজেক্টে Egress কমানোর কাজ V513–V515-এ করা হয়েছে, আর এই
সেশনের কোনো কাজেই নতুন ক্লাউড-অনুরোধ যোগ হয়নি।)*
