# V221_FINAL_DECLARATION — সৎ ঘোষণা

**Base:** V220 (220/2.20) → **V221 (221/2.21)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।

### কোন কোন File — `V221_CHANGED_FILES.md`। ধাপে ধাপে — `V221_WORK_LOG.md`।

### সত্যিই কোড-এ যা হয়েছে (static + guard-verified; device/build/live Pending)
- **§১** পাহারাদার ৯.১৪-এর ২টি Bengali-off সমস্যা ঠিক (`NoBengali.kt` MAP-এ "আটকে"→"stuck", "আরও"→"more")। **বাংলা চালু থাকা ব্যবহারকারীর লেখা/ডিজাইন অপরিবর্তিত** (fix() active না হলে হুবহু ফেরত)।
- **§২** UPSERT/UPDATE **সত্যিকারের cloud confirmation** পেলে ঐ একই Table+Record-এর পুরোনো pending/failed HTTP 400 entry সম্পূর্ণ পরিষ্কার (`clearConfirmed`) — **লাল Warning শুধু আসল Cloud success-এর পরে সরে**। অন্য Record-এর pending **কখনো** মোছে না; DELETE ছোঁয়া হয় না।
- **§৩** Refund nonce **persist** — App বন্ধ/Crash/Restart-এও একই অসম্পূর্ণ Refund **একই id-তেই** retry (Duplicate নয়); nonce **Cloud confirmation-এর পরেই** মোছে; **একই দিনে দুটি বৈধ আলাদা Refund-এর সুবিধা অক্ষত**। Refund total/approval/Visit Fee/branch/payment হিসাব **অপরিবর্তিত**। Android + Web (parity)।

### যা বদলানো হয়নি (স্পষ্ট)
Design/Layout/Colour/Button/Text-arrangement/Workflow/Permission/Branch Rule/Payment Rule/Diet Chart/Print/Login — **কিচ্ছু বদলানো হয়নি**। Backup/Restore code, DB Trigger, SQL, RLS — **এই version-এ ছোঁয়া হয়নি** (§Q4-A/B ও §Q5 আলাদা version-এ, আপনার অনুমতির পরে)। Supabase Free-plan-এ নতুন read/write **যোগ হয়নি**।

### Guard
`python3 00_GUARD/tk_guard.py` — **সব যাচাই ✅ পাশ** (V221)। একটিও ব্যর্থতা নেই — তাই FINAL।

### Test — static + guard Pass; সব device/build/live **Pending** (`V221_TEST_REPORT.md`)। signed APK দাবি করা হয়নি।

### Rollback
`ROLLBACK_V220/` — সম্পাদনার আগের V220 source-এর হুবহু কপি। দরকারে V220-এ ফেরা যায়।

**⛔ "সব শেষ ও tested" নয়। §১/২/৩ কোড-এ হয়েছে (static + guard-verified), device/live Pending। কোনো untested জিনিসকে Pass বলা হয়নি।**
