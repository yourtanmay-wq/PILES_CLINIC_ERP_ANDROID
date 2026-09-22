# V223_WORK_LOG.md — কাজের ধারাবাহিক লগ

**তারিখ:** 01.08.2026 IST · **Base:** V222 → **V223** · Owner: TK BISWAS

## নির্দেশ (TK)
নিজে বারবার Audit + ৩টি আলাদা Review + প্রতিটি নিয়মের runnable automated test; সব সন্দেহ শেষ না হলে Final ZIP নয়। (১) cloud-read ব্যর্থ হলে পুরোনো data নতুন cloud data overwrite করবে না — নতুনত্ব নিশ্চিত না হলে Restore বন্ধ+Error, আন্দাজে লেখা নয়। (২) DB trigger পুরোনো লেখা আটকালে App যেন success না ভাবে ও Pending না মোছে — সত্যিকারের নতুন Data বসেছে নিশ্চিত হলেই clear। (৩) নতুন Pending কখনো হারাবে না। (৪) সব restore পথ+cross-device নিরাপদ; সংঘর্ষে নতুন জেতে। (৫) refund দুই রোগীর আলাদা, crash-এ no-dup।

## ধাপে ধাপে
1. **Audit:** upsert/updateById/flush/restore/refund সব পড়া। tooling: node আছে (kotlinc নেই)। DB `updatedAt` = text; upsert-target-এ deleted_records/activity_logs/trash-এ updatedAt নেই (guard দরকার)।
2. **Rollback আগে:** V222 source → `ROLLBACK_V222/`।
3. **§C2 (SupabaseClient):** `upsert`/`updateById`-এ row/fields-এ updatedAt থাকলে `return=representation`; ফেরা updatedAt মিল→LANDED, না-মিল→SUPERSEDED, নেট-fail→FAILED। clearConfirmed শুধু LANDED-এ; SUPERSEDED-এ clear/remember নয়, return true (flush obsolete drop, loop নেই)। updatedAt-হীন row-এ return=minimal (regression নেই)।
4. **§C1 (SupabaseClient/Trash/Settings/web):** `upsertRestoreSafe`+`RestoreOutcome` (`fetchListOrNull` দিয়ে পড়া-ব্যর্থ vs row-নেই আলাদা)। Trash restore BLOCKED→false। Cloud JSON restore টেবিল-প্রতি fetchListOrNull (ব্যর্থ→skip), per-row block/keptNewer/failedNet গোনা। web `wlv1CloudStampMs` −2/−1, `wlv1RestoreTrash` block-on-unverified, `cloudPush` !remoteKnown→skip।
5. **SQL:** V222 trigger বহাল; `_rk_safe_ts` STABLE; V223 rename।
6. **Automated tests:** `11_V223_TESTS/V223_logic_tests.js` — Kotlin সিদ্ধান্ত-লজিক হুবহু port + আসল web app.js ফাংশন load; ৪১ test, সব scenario (old/newer, read-fail, simultaneous, pending-preserve, false-success, same-mobile ২ রোগী, crash-retry)। **PASS 41/0।**
7. **৩টি স্বাধীন Review (আলাদা এজেন্ট):** data-loss/restore · race/pending/refund · parity/SQL/regression — সব PASS। তোলা ২ বিষয় (net-fail count; SQL STABLE) সঙ্গে সঙ্গে ঠিক → guard+test আবার PASS।
8. **Guard/parity:** tk_guard.py সব ✅; node --check দুই app.js; app.js+index.html IDENTICAL; rollback-diff = ৮ ফাইল + SQL + tests।

## Pending (সৎ)
Android build/APK/device/live + SQL-প্রয়োগ — TK-এর হাতে। কোনো untested জিনিসকে Pass বলা হয়নি।
