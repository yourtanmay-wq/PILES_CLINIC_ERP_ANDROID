# 🔒 LOCK NOTE — SESSION 2026-08-01 · V222

**তারিখ-সময়:** 01.08.2026 · 12.25 AM IST · **Base:** V221 → **V222 (222/2.22)** · Owner: TK BISWAS

## এই সেশনে যা লক হলো
1. **§1 নতুন Pending কখনো হারাবে না:** `CloudWriteQueue.clearConfirmed(confirmedKind, table, id, confirmedBody, writeStart)` — সময়-পাহারা (at ≤ writeStart) + supersede (UPSERT সব / UPDATE শুধু subset-ঘর)। `SupabaseClient.upsert`/`updateById`-এ নেট-কল শুরুর আগে `writeStart`। `withFailedAdded`-এ `at` সংরক্ষণ।
2. **§2 Refund কখনো এক নয়:** `refundIdFor` raw ও `refundNonceKey` — সামনে `patient.id`; web `wlv1RefundIdFor` raw ও `wlv1RefundDraftKey`-ও (hashCode parity)।
3. **§3 Restore overwrite বন্ধ:** `SupabaseClient.upsertNewerWins`/`rowStampMs`; `TrashRepository.restore` ও web `wlv1RestoreTrash` newer-wins; `SettingsActivity.doCloudJsonRestore` টেবিল-map newer-wins; DB trigger SQL (`V222_...TRIGGER_COPY_PASTE.sql`, নিজে চালানো নয়)।

## লক করা সিদ্ধান্ত (ভবিষ্যতে না ভাঙা)
- clearConfirmed **শুধু** সফল লেখা শুরুর আগের কাজ + (UPSERT=সব / UPDATE=subset) পরিষ্কার করে; নতুন/আলাদা-ঘরের/অন্য-id কাজ কখনো নয়। `writeStart` অবশ্যই নেট-কলের **আগে** ধরতে হবে।
- Refund id ও nonce — দুটোতেই `patient.id` থাকবে; Android ও web-এর raw **হুবহু এক ক্রম** (parity)।
- `upsertNewerWins`/newer-wins guard **শুধু Restore-পথে** — রোজকার সেভে নয় (Free-plan)।
- DB trigger: শুধু দুই updatedAt থাকলে ও NEW<OLD হলে বাদ; NULL/heal/subset কখনো আটকাবে না। SQL আলাদা file-এ, live backup + এক-টেবিল টেস্ট করে TK চালাবেন।
- app.js: Netlify ও assets/www **byte-identical** (এই সেশনে IDENTICAL যাচাই)।

## যাচাই
`tk_guard.py` সব ✅ পাশ (V222); দুই app.js `node --check` পাশ; স্বাধীন সাব-এজেন্ট review পাশ; rollback-diff = ১১ ফাইল + ১ নতুন SQL। device/build/live/SQL **Pending** (TK করবেন)। Rollback: `ROLLBACK_V221/`।
