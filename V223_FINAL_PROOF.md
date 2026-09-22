# V223 — লিখিত প্রমাণ (Final ZIP-এর আগে)

**Base:** V222 → **V223 (223/2.23)** · তারিখ: 01.08.2026 IST · Owner: TK BISWAS

## ১. কোন কোন Restore / Retry / Refund path যাচাই হয়েছে
| Path | কী নিশ্চিত হলো |
|---|---|
| **Android Trash Restore** (`TrashRepository.restore`) | cloud পড়া না গেলে/তুলনা অসম্ভব → BLOCKED (কিছুই লেখা নয়, trash অক্ষত); cloud নবীন → KEPT_NEWER; নইলে restore |
| **Android Cloud JSON Restore** (`SettingsActivity.doCloudJsonRestore`) | টেবিল-প্রতি `fetchListOrNull`; পড়া ব্যর্থ → পুরো টেবিল skip (blocked গোনা); cloud নবীন → keptNewer; তুলনা অসম্ভব → blocked; net-fail → failedNet (retry-তে, নীরব নয়) |
| **Android Local DB Restore** (`BackupManager.restore`) | লোকাল ফাইল swap; পরে স্বাভাবিক sync — LANDED-যাচাই + DB trigger পাহারা |
| **Web Trash Restore** (`wlv1RestoreTrash`) | `wlv1CloudStampMs` −2 (পড়া ব্যর্থ)→BLOCK, −1 (row নেই)→WRITE, নবীন→KEEP_NEWER, তুলনা অসম্ভব→BLOCK |
| **Web Bulk Restore** (`applyBackupPayload`→`cloudPush`) | `mergeForCloudPush` newer-wins + `if(!remoteKnown) continue` (cloud না-পড়া গেলে overwrite নয়) |
| **পুরোনো Pending UPSERT replay** (`CloudWriteQueue.flush`) | trigger আটকালে `upsert`→SUPERSEDED→`clearConfirmed` **নয়** (নতুন Pending মোছে না), flush obsolete drop (infinite retry নয়) |
| **অন্য ফোন থেকে পুরোনো Data** | DB trigger (সর্বজনীন) + LANDED-detection যাতে app blocked-লেখাকে success না ধরে |
| **Refund (দুই রোগী একই mobile / crash-retry)** | id ও nonce উভয়ে `patient.id`; crash-এ একই draft-key→একই nonce→একই id; দুই বৈধ Refund আলাদা |

## ২. প্রতিটি automated test-এর ফল (`node 11_V223_TESTS/V223_logic_tests.js`)
**PASS: 41 · FAIL: 0 · ALL PASS ✅** (সম্পূর্ণ আউটপুট: `11_V223_TESTS/V223_test_results.txt`)
- **A (LANDED detection):** no-trigger→LANDED · trigger-block old write→SUPERSEDED · newer→LANDED · http-fail→FAILED · updatedAt-less table→LANDED · effects: শুধু LANDED clears pending, SUPERSEDED neither clears nor retries।
- **B (clearConfirmed):** older same-id cleared · **newer pending (at>writeStart) preserved** · other-id/DELETE untouched · UPDATE শুধু subset clears (disjoint preserved, UPSERT preserved)।
- **C (Android restore outcome):** read-FAIL→BLOCKED · no-row→WRITTEN · cloud-newer→KEPT_NEWER · incoming-newer→WRITTEN · can't-compare→BLOCKED · write-fail→BLOCKED।
- **D (web restore):** −2→BLOCK · −1→WRITE · newer→KEEP_NEWER · can't-compare→BLOCK।
- **E (আসল web app.js ফাংশন load করে):** same-mobile দুই রোগী→আলাদা id **ও** আলাদা nonce-key · **Android↔Web id byte-parity** (দুই রোগীর জন্যই) · crash-retry same id · দুই বৈধ Refund আলাদা id · javaHash==Java String.hashCode।
- **F (simultaneous-save):** SUPERSEDED replay clearConfirmed ডাকে না; time-guard-এ নতুন pending টেকে।

## ৩. Exact changed-file list ও before/after diff
৮টি in-scope ফাইল (rollback-diff-এ নিশ্চিত), diff: `V223_BEFORE_AFTER_DIFF.txt`:
1. `app/build.gradle.kts` — 222→223 / 2.22→2.23
2. `native/SupabaseClient.kt` — §C2 upsert/updateById LANDED/SUPERSEDED; §C1 `upsertRestoreSafe`+`RestoreOutcome`
3. `native/TrashRepository.kt` — restore → `upsertRestoreSafe`, BLOCKED→false
4. `security/SettingsActivity.kt` — Cloud JSON restore: `fetchListOrNull`, block/keptNewer/failedNet গোনা
5. `03_NETLIFY_READY/app.js` — `wlv1CloudStampMs` −2/−1, `wlv1RestoreTrash` block, `cloudPush` !remoteKnown
6. `03_NETLIFY_READY/index.html` — cache-buster v222→v223
7. `assets/www/app.js`, `index.html` — Netlify-র সঙ্গে **byte-identical**
+ নতুন: `04_.../V223_BACKUP_OVERWRITE_GUARD_TRIGGER_COPY_PASTE.sql` (V222→V223 rename, logic+STABLE), `11_V223_TESTS/` (test+results)

## ৪. Guard ও সব Review সম্পূর্ণ PASS
- `tk_guard.py` — **সব ✅ পাশ** (V223)। `node --check` দুই app.js পাশ। app.js+index.html parity **IDENTICAL**।
- **৩টি স্বাধীন Review (আলাদা এজেন্ট) — সব PASS:**
  1. **Data-loss/Restore:** read-fail→block, SUPERSEDED≠success, conflict→newer — সব ঠিক; কোনো silent overwrite/loss/false-success নেই।
  2. **Pending/Retry/Refund race:** নতুন pending কখনো হারায় না; SUPERSEDED replay clearConfirmed ডাকে না ও flush obsolete drop করে (loop নেই); refund id/nonce দুই রোগীর আলাদা, crash-retry no-dup।
  3. **Android/Web/SQL parity ও regression:** compile clean; normal save অপরিবর্তিত; `updatedAt`-হীন টেবিল guarded; app.js byte-identical; SQL PL/pgSQL সঠিক (NULL/subset/heal আটকায় না, RETURN OLD সঠিক)।
- Review-এ তোলা ২টি ছোট বিষয় **সঙ্গে সঙ্গে ঠিক** করা হয়েছে: (ক) Cloud JSON restore net-fail row এখন `failedNet` হিসেবে জানানো হয় (নীরব নয়); (খ) SQL `_rk_safe_ts` `IMMUTABLE`→`STABLE`। ঠিক করার পর guard+৪১ test আবার সব PASS।

## ৫. কোনো known issue বা code-level সন্দেহ বাকি নেই?
**নেই** (৫টি concern-এর জন্য)। সৎ প্রেক্ষাপট (নতুন নয়, V223-এর regression নয়, ৫ concern-এর বাইরে): (i) `approveRefund`/`rejectRefund` newer-wins-এর অধীন — concurrent নবীন edit থাকলে approve superseded হতে পারে; approve সবসময় `updatedAt=now` (সর্বনবীন) দেয় বলে বাস্তবে আটকায় না; workflow বলে ছোঁয়া হয়নি। (ii) refund id 32-bit hashCode — collision অতি-অসম্ভব, পুরোনো নকশা।

## ৬. Design/Workflow/Permission/Payment rule অপরিবর্তিত?
**হ্যাঁ, অপরিবর্তিত।** `CloudWriteQueue.kt`, `PaymentModel.kt`, `PaymentRepository.kt` — V222-এর সঙ্গে **byte-identical** (refund হিসাব/approval/nonce logic অটুট)। কোনো Design/Layout/Colour/Button/Text/Print/Diet Chart/Workflow/Permission/Branch/Login/Payment-Refund হিসাব বদল নেই। শুধু write-নিশ্চিতকরণ (LANDED) ও restore-নিরাপত্তা (block-on-unverified) যোগ।

**Android build/device/live-Supabase test ও SQL-প্রয়োগ — TK নিজে করবেন (Pending)। কোনো untested জিনিসকে Pass বলা হয়নি।**
