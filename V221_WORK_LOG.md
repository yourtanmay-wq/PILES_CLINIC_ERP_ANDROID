# V221_WORK_LOG.md — কাজের ধারাবাহিক লগ

**তারিখ:** 31.07.2026 IST · **Base:** V220 → **V221** · Owner: TK BISWAS

## নির্দেশ (TK, এই সেশন)
V220-কে Base ধরে একবারেই ঝুঁকিহীনভাবে: (১) Guard-এর Bengali-off ২টি সমস্যা ঠিক; (২) UPSERT/UPDATE cloud-confirm হলে একই Table+Record-এর পুরোনো pending/failed HTTP 400 সম্পূর্ণ পরিষ্কার (Warning শুধু আসল success-এ সরবে, অন্য record নয়); (৩) Refund nonce persist (crash-এও একই অসম্পূর্ণ Refund একই id, confirm-এর পরেই nonce মোছা, দুটি বৈধ আলাদা Refund-এর সুবিধা অক্ষত); (৪) সব call-site ও Android/Web parity যাচাই। Backup/Restore/SQL/RLS নয়; design/workflow/payment-rule নয়।

## ধাপে ধাপে যা করা হলো
1. **Rollback আগে:** সম্পাদনার আগে V220-এর `02/03/04` source হুবহু কপি করে `ROLLBACK_V220/` বানানো।
2. **§1 — NoBengali.kt:** পাহারাদার চালিয়ে ঠিক ২টি ব্যর্থতা নিশ্চিত (`CloudWriteQueue.kt:420` "আরও", `PendingSyncStatus.kt:97` "আটকে:")। MAP-এ `"আটকে" to "stuck"` ও `"আরও" to "more"` যোগ। `fix()` active না হলে (বাংলা-চালু ব্যবহারকারী) লেখা হুবহু ফেরত — তাদের কিছু বদলায় না।
3. **§2 — CloudWriteQueue.kt + SupabaseClient.kt:** `clearConfirmed(table,id)` (শুধু UPSERT/UPDATE, একই table+id; DELETE/অন্য record নয়)। হট-পথে বাড়তি ভার এড়াতে `hasQueue` (`recountQueue`, `hasDeletes`-এর অনুরূপ)। `upsert` ok ও `updateById` changed শাখায় কল — অর্থাৎ **শুধু আসল cloud-নিশ্চয়তার পরে**।
4. **§3 — PaymentRepository.kt (Android):** `saveRefund`-এ draft-key ভিত্তিক persist-করা nonce (`refund_nonce_store`)। nonce cloud-confirm (`ok`) হলে তবেই মোছে। `refundIdFor`/`buildRefundRow`/maxRefundable/approval/branch যুক্তি অপরিবর্তিত — শুধু nonce-এর উৎস memory→persist।
5. **§3 — app.js (Netlify) + assets/www:** একই নিয়মে `wlv1GetRefundNonce`/`wlv1ClearRefundNonce` (localStorage `rk_refund_nonce`)। `saveRefundWeb` persist-nonce ব্যবহার করে, `okC` হলে মোছে। Netlify→assets/www হুবহু কপি (parity)।
6. **Version:** build.gradle 220→221 / 2.20→2.21; index.html cache-buster v220→v221 (দুই জায়গায়)।
7. **যাচাই:** `node --check` দুই app.js পাশ; `tk_guard.py` **সব ✅ পাশ** (V221); rollback-diff = ঠিক ৯টি in-scope ফাইল, আর কিছু নয়।

## Call-site ও parity যাচাই (§4)
- `saveRefund` call-site: শুধু `PaymentActivity.kt:938` — persist-nonce সেটাকেই ঢাকে (passed nonce fallback)।
- `buildRefundRow`/`refundIdFor`: শুধু PaymentModel/PaymentRepository-এর ভিতরে — বাইরে নেই।
- cloud-লেখা সব `SupabaseClient` দিয়েই যায়; §2 তাই `upsert`+`updateById` দুই জায়গাতেই — সম্পূর্ণ।
- Web refund creator শুধু `saveRefundWeb` — ঢাকা হয়েছে।
- কোনো অর্ধেক-path বাকি নেই।

## Pending (সৎ)
Android Gradle build / signed APK / দ্বিতীয় ফোন / live-Supabase — এই পরিবেশে সম্ভব নয়, **Pending**; TK নিজে করবেন। কোনো untested জিনিসকে Pass বলা হয়নি।
