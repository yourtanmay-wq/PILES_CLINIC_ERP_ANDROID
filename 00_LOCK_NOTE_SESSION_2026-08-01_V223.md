# 🔒 LOCK NOTE — SESSION 2026-08-01 · V223

**তারিখ-সময়:** 01.08.2026 · **Base:** V222 → **V223 (223/2.23)** · Owner: TK BISWAS

## এই সেশনে যা লক হলো
1. **§C1 — Restore নিরাপদ (block-on-unverified):** `SupabaseClient.upsertRestoreSafe`+`RestoreOutcome`; `fetchListOrNull` (পড়া-ব্যর্থ vs row-নেই)। `TrashRepository.restore` BLOCKED→false। `SettingsActivity.doCloudJsonRestore` টেবিল-map + block/keptNewer/failedNet। web `wlv1CloudStampMs(−2/−1)`, `wlv1RestoreTrash` block, `cloudPush !remoteKnown`।
2. **§C2 — LANDED detection:** `upsert`/`updateById` representation-এ updatedAt মিলিয়ে LANDED/SUPERSEDED/FAILED; `clearConfirmed` শুধু LANDED-এ। updatedAt-হীন row/table → return=minimal (আগের আচরণ)।
3. **SQL:** `V223_BACKUP_OVERWRITE_GUARD_TRIGGER_COPY_PASTE.sql` (৮ টেবিল; `_rk_safe_ts` STABLE)। নিজে চালানো নয়।
4. **Tests:** `11_V223_TESTS/V223_logic_tests.js` (node, ৪১ test, ALL PASS) — Kotlin লজিক port + আসল web ফাংশন।

## লক করা সিদ্ধান্ত (না ভাঙা)
- Restore-এ cloud না-পড়া গেলে/তুলনা অসম্ভব হলে **কখনো আন্দাজে লেখা নয়** (BLOCKED)। `fetchListOrNull` null=ব্যর্থ, খালি=row-নেই — এই পার্থক্য রাখতেই হবে।
- `clearConfirmed` **শুধু সত্যিকারের LANDED-এর পর** (representation-এ ফেরা updatedAt == পাঠানো)। SUPERSEDED-এ clear/remember কিছুই নয়, return true।
- `verify = row.has("updatedAt")` — updatedAt-হীন টেবিলে representation/select চাওয়া যাবে না (400 এড়াতে)।
- app.js: Netlify ও assets/www **byte-identical**।
- `CloudWriteQueue.kt`/`PaymentModel.kt`/`PaymentRepository.kt` V222-এর সঙ্গে byte-identical রাখা (refund/pending logic অটুট)।

## যাচাই
guard সব ✅ (V223); node --check দুই app.js; ৪১ automated test PASS; ৩ স্বাধীন Review PASS। device/build/live/SQL Pending (TK)। Rollback: `ROLLBACK_V222/`।
