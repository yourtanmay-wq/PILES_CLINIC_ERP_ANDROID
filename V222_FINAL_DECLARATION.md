# V222_FINAL_DECLARATION — সৎ ঘোষণা

**Base:** V221 (221/2.21) → **V222 (222/2.22)**। তারিখ: 01.08.2026 IST। Owner: TK BISWAS।

### কোন কোন File — `V222_CHANGED_FILES.md`। ধাপে ধাপে — `V222_WORK_LOG.md`।

### সত্যিই যা হয়েছে (static + guard + স্বাধীন review-verified; device/build/live/SQL Pending)
- **§১ নতুন Pending কখনো হারাবে না:** `clearConfirmed`-এ **সময়-পাহারা** (শুধু সফল লেখা শুরুর আগে জমা কাজ) + **supersede-পাহারা** (UPSERT সব; UPDATE শুধু subset-ঘর)। নতুন Remark/Date/Payment/Follow-up কখনো মোছে না; অন্য Record/DELETE ছোঁয় না। failed-ঘরেও `at` রেখে সময়-পাহারা সম্পূর্ণ।
- **§২ একই মোবাইলে দুই রোগীর Refund কখনো এক নয়:** Refund id **ও** স্থায়ী nonce উভয়ে `patient.id` ধরে আলাদা (Android+Web সমান)। App বন্ধ/Crash/Restart-এও একই অসম্পূর্ণ Refund **duplicate নয়**; দুই বৈধ আলাদা Refund **আলাদা**। Refund total/approval/Visit Fee/payment হিসাব **অপরিবর্তিত**।
- **§৩ Backup/Restore overwrite বন্ধ (App + DB):**
  - **App:** Trash Restore (Android+Web) ও Cloud JSON Restore (Android)-এ **newer-wins** — পুরোনো data নতুন cloud data চাপা দেয় না; সংঘর্ষে নতুন জেতে; kept-newer জানানো হয় (silent loss/false success নয়)। Web bulk restore আগে থেকেই newer-wins (`mergeForCloudPush`)। পুরোনো Pending UPSERT — §1 (একই ফোন) + DB trigger (অন্য ফোন)।
  - **DB:** `04_SUPABASE_DATABASE_SETUP/V222_BACKUP_OVERWRITE_GUARD_TRIGGER_COPY_PASTE.sql` — সব পথের সর্বজনীন `BEFORE UPDATE` পাহারা। NULL/অদ্ভুত-format/heal/subset/legacy — সব নিরাপদে যাচাই। **আমি চালাইনি**; কখন-কী সহজ বাংলায়, এক-টেবিল টেস্ট ও রোলব্যাকসহ — TK চালাবেন।

### যা বদলানো হয়নি (স্পষ্ট)
Design/Layout/Colour/Button/Text-arrangement/Print/Diet Chart/Workflow/Permission/Branch Rule/Login — **কিচ্ছু নয়**। Broad refactor/optimization/cleanup **নেই**। Supabase Free-plan-এ **অপ্রয়োজনীয় read/write নেই** (Restore-এর read শুধু Restore-পথে)।

### Guard
`python3 00_GUARD/tk_guard.py` — **সব যাচাই ✅ পাশ** (V222)। একটিও ব্যর্থতা নেই — তাই FINAL।

### Test — static + guard + স্বাধীন review Pass; device/build/live/SQL **Pending** (`V222_TEST_REPORT.md`)।

### Rollback
`ROLLBACK_V221/` — সম্পাদনার আগের V221 source-এর হুবহু কপি।

**⛔ "সব শেষ ও tested" নয়। §১/২/৩ কোড-এ হয়েছে (static + guard + review-verified), device/live ও DB-SQL প্রয়োগ Pending। কোনো untested জিনিসকে Pass বলা হয়নি।**
