# V222_WORK_LOG.md — কাজের ধারাবাহিক লগ

**তারিখ:** 01.08.2026 IST · **Base:** V221 → **V222** · Owner: TK BISWAS

## নির্দেশ (TK, এই সেশন)
V221-কে Base ধরে একবারেই সব: (১) একই Table+Record-এ নতুন Pending থাকলে পুরোনো Save সফল হওয়ায় নতুন Pending যেন কখনো না মোছে — শুধু সত্যিকারের পুরোনো/সম্পন্ন কাজ পরিষ্কার। (২) একই Mobile-এর দুই আলাদা রোগীর Refund কখনো এক নয় — id ও স্থায়ী nonce Patient/Record ID ধরে আলাদা; crash-এও duplicate নয়, দুই বৈধ আলাদা Refund আলাদা; total/approval/Visit Fee/payment অপরিবর্তিত। (৩) Android Cloud/Local/Trash Restore, Web Restore, পুরোনো Pending UPSERT — সব পথ নিরাপদ; পুরোনো Data নতুন Cloud Data overwrite করবে না; সংঘর্ষে নতুন জেতে; silent loss/false success নয়। App code + DB protection দুটোই; SQL আলাদা copy-paste file-এ (নিজে চালানো নয়); NULL/timestamp/heal/subset/legacy যাচাই করে trigger।

## ধাপে ধাপে
1. **সম্পূর্ণ Audit আগে:** সব restore-পথ (TrashRepository, SettingsActivity doCloudJsonRestore, BackupManager local, web wlv1RestoreTrash/cloudPush/mergeForCloudPush) ও refund call-site পড়া। DB schema — `updatedAt` = **text** (ISO-8601), Android+web একই format।
2. **Rollback আগে:** V221-এর `02/03/04` হুবহু কপি → `ROLLBACK_V221/`।
3. **§1 — CloudWriteQueue + SupabaseClient:** `clearConfirmed`-এ (ক) সময়-পাহারা `writeStart` (নেট-কল শুরুর আগে ধরা) — এই লেখার সময়/পরে জমা নতুন কাজ (at > writeStart) কখনো মোছে না; (খ) supersede-পাহারা — UPSERT সফল হলে পুরো-row বলে পুরোনো UPSERT/UPDATE বাতিল নিরাপদ, UPDATE সফল হলে শুধু **subset-ঘরের** পুরোনো UPDATE মোছে (আলাদা-ঘরের Remark/Date নয়, পুরো-row UPSERT নয়)। `withFailedAdded`-এ `at` সংরক্ষণ যাতে failed-ঘরেও সময়-পাহারা কাজ করে।
4. **§2 — PaymentModel + PaymentRepository + web:** `refundIdFor` raw ও `refundNonceKey`-এর সামনে `patient.id`; web `wlv1RefundIdFor` raw ও `wlv1RefundDraftKey`-এও `p.id` — হুবহু এক ক্রম (hashCode parity)। এক মোবাইলে দুই রোগীর id/nonce কখনো মেলে না। একই রোগীর retry একই id (idempotency অক্ষত); confirm-এর পরে nonce মোছে বলে দুই বৈধ আলাদা Refund আলাদা।
5. **§3 App — SupabaseClient + TrashRepository + SettingsActivity + web:** নতুন `rowStampMs`/`upsertNewerWins`; Trash restore (Android) ও `wlv1RestoreTrash` (web) newer-wins; Cloud JSON restore টেবিল-প্রতি map (per-row read নয়) + "kept newer" গোনা। Web bulk restore আগে থেকেই `mergeForCloudPush` newer-wins (যাচাই করা)। Local DB restore = লোকাল ফাইল swap → restart-এর পরে স্বাভাবিক sync (trigger + §1 পাহারা)। পুরোনো Pending UPSERT = §1 (একই ফোন) + DB trigger (অন্য ফোন) — সম্পূর্ণ।
6. **§3 DB — SQL file:** `V222_BACKUP_OVERWRITE_GUARD_TRIGGER_COPY_PASTE.sql` — নিরাপদ `_rk_safe_ts` (text→timestamptz, ভাঙে না) + `BEFORE UPDATE` guard (শুধু দুই stamp থাকলে ও NEW<OLD হলে বাদ)। NULL/format/heal/subset/legacy — সব যাচাই করে নিরাপদ প্রমাণ। ৮ টেবিল, এক-টেবিল টেস্ট + verify + rollback, সহজ বাংলায় কখন-কী। **নিজে চালানো হয়নি।**
7. **যাচাই:** স্বাধীন সাব-এজেন্ট রিভিউ — তিন requirement মেটে, compile ঠিক, parity ঠিক; তার তোলা এক দুর্বলতা (failed-ঘরে `at` নেই) সঙ্গে সঙ্গে ঠিক করা। `node --check` দুই app.js পাশ; `tk_guard.py` **সব ✅ পাশ** (V222); rollback-diff = ঠিক ১১ ফাইল + ১ নতুন SQL, আর কিছু নয়।

## Call-site ও parity যাচাই
- `clearConfirmed` — ৩ কল-সাইটই নতুন signature-এ (upsert/updateById); পুরোনো ২-arg কল নেই; test-এ রেফারেন্স নেই।
- `refundIdFor`/`buildRefundRow`/`refundNonceKey` — শুধু PaymentModel/PaymentRepository-এর ভিতরে; `saveRefund` কল-সাইট একটাই (PaymentActivity)।
- `upsertNewerWins`/`rowStampMs` — শুধু restore-পথে; রোজকার সেভে নয়।
- Web refund creator শুধু `saveRefundWeb`; Trash restore শুধু `wlv1RestoreTrash` — দুটোই ঢাকা।

## Pending (সৎ)
Android Gradle build / signed APK / দ্বিতীয় ফোন / live-Supabase / SQL-প্রয়োগ — এই পরিবেশে সম্ভব নয় / TK-এর হাতে। **Pending**; কোনো untested জিনিসকে Pass বলা হয়নি।
