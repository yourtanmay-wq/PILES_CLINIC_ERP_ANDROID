# 🔒 LOCK NOTE — SESSION 2026-07-31 · V221

**তারিখ-সময়:** 31.07.2026 · 11.41 PM IST · **Base:** V220 → **V221 (221/2.21)** · Owner: TK BISWAS

## এই সেশনে যা লক হলো
1. **§1 (Guard Bengali-off):** `NoBengali.kt` MAP-এ `"আটকে" to "stuck"` ও `"আরও" to "more"`। পাহারাদার ৯.১৪-এর ২টি ব্যর্থতা শেষ। বাংলা-চালু ব্যবহারকারীর কিছু বদলায়নি।
2. **§2 (পুরোনো Warning সরানো):** `CloudWriteQueue.clearConfirmed(table,id)` — cloud-confirm হলে ঐ একই (table,id)-এর UPSERT/UPDATE pending+failed মোছে। `SupabaseClient.upsert` (ok) ও `updateById` (changed) থেকে ডাকা। DELETE/অন্য record ছোঁয় না। সস্তা `hasQueue` পাহারা।
3. **§3 (Refund nonce persist):** Android `PaymentRepository.saveRefund` — `refund_nonce_store` (draft-key)। Web `saveRefundWeb` — localStorage `rk_refund_nonce`। nonce cloud-confirm হলে তবেই মোছে। Refund হিসাব/approval/branch/Visit Fee অপরিবর্তিত।

## লক করা সিদ্ধান্ত (ভবিষ্যতে না ভাঙা)
- §2-এর clear **শুধু আসল cloud-success**-এ; local-এ নয়। শুধু UPSERT/UPDATE, একই row। DELETE-এর নিয়ম আগের মতোই (`forget`/DeletedGuard)।
- §3-এর persist-nonce **cloud-confirm-এর আগে মোছা যাবে না** — নইলে crash-এ Duplicate ফিরবে।
- app.js: Netlify ও assets/www **byte-identical** রাখতে হবে (এই সেশনে IDENTICAL যাচাই করা)।

## বাকি (আলাদা version, TK-এর অনুমতির পরে)
- §Q4-A/B (Trash-Restore overwrite, Android+Web) ও §Q5 (DB `updatedAt`-trigger, NULL-guard + createdAt-heal ৪ লাইন) — Backup/Restore/SQL, তাই **এই version-এ ইচ্ছাকৃতভাবে করা হয়নি**; live backup + এক-টেবিল টেস্ট করে V222+-এ।

## যাচাই
`tk_guard.py` সব ✅ পাশ (V221); দুই app.js `node --check` পাশ; rollback-diff = ৯টি in-scope ফাইল। device/build/live **Pending** (TK করবেন)। Rollback: `ROLLBACK_V220/`।
