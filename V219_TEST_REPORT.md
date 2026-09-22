# V219_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম:** Android build/device/live-Supabase test না করলে স্পষ্ট **Pending** (§10)। যা সত্যিই চালানো হয়নি তাকে Pass বলা হয়নি।
**পরিবেশ:** cloud container — আসল Android device/দ্বিতীয় ফোন/Gradle build/live Supabase নেই।

## এখানে সত্যিই চালানো (STATIC) — PASS
| পরীক্ষা | ফল |
|---|---|
| Bracket-balance — পরিবর্তিত ৫ Kotlin file (PaymentModel, PaymentRepository, CloudWriteQueue, PendingSyncStatus, BriefingRepository) | ✅ সব BALANCED |
| `node --check` — 03_NETLIFY_READY/app.js | ✅ PASS |
| `node --check` — assets/www/app.js (Netlify-র সঙ্গে diff = IDENTICAL) | ✅ PASS |
| Call-site vs signature: refundIdFor/pendingRefundSum(excludeId), wlv1DeleteDraftEntry(recId), stuckDetail, withFailedAdded, CloudReadCache.get | ✅ মিলেছে |
| assets/www logo subfolder অক্ষত | ✅ |

## Pending — device/build/live (§10)
| Test | Status |
|---|---|
| §1 Refund retry (cloud fail → আবার চাপা → একটাই Refund) — Android **ও** web | ⛔ PENDING (device/browser + live Supabase দরকার) — কোড-লজিক verified |
| §1 Refund total/visit-fee ঠিক আছে | ⛔ PENDING |
| §2 এক মোবাইলে দুই enquiry → দ্বিতীয়টা Delete করলে দ্বিতীয়টাই যায় (প্রথমটা নয়) | ⛔ PENDING (browser + live) — কোড-লজিক verified |
| §4 HTTP 400 আটকালে Table·Record·কারণ দেখা যায় + ২ চেষ্টার পর park | ⛔ PENDING (আসল 400 ঘটাতে device/live দরকার) |
| §5 assets/www runtime-এ চলে না — native app আচরণ অপরিবর্তিত | ⛔ PENDING (device) — code-analysis: launcher native LoginActivity, assets/www unused |
| §6 PART A SQL live-এ চালানো + Login/Master Center অটুট | ⛔ PENDING (live Supabase) |
| §7 briefings cache — তালিকা সম্পূর্ণ থাকে, কোটা কমে, request ≤২০s-এ master-এ পৌঁছায় | ⛔ PENDING (device/live) |
| §21 Gradle build / Signed APK | ⛔ PENDING — Android Studio + TK keystore; signed দাবি করা হয়নি |

## গুরুত্বপূর্ণ সৎ নোট
- §1: deterministic id দুই প্ল্যাটফর্মে (Java hashCode ও JS মিলিয়ে) একই — retry-তে দ্বিতীয় Refund তৈরি হয় না। ইচ্ছাকৃত হুবহু-একই দ্বিতীয় refund overwrite হবে (WORK_LOG-এর "নতুন সন্দেহ" দ্রষ্টব্য)।
- §2: enqreject সারি enquiry (x.id ঠিক), visitreject/notcomplete সারি followup — তাই delTable=patients-এ p.id ব্যবহার; ভুল রোগী আর মুছবে না।
- §4: 401/403/409/429 permanent ধরা হয়নি (transient হতে পারে) — সেগুলো আগের মতোই retry।
- §7: briefings ২০s cache-এ request-notice ≤২০s দেরি হতে পারে (তথ্য হারায় না)। device-এ মিলিয়ে নেবেন; দরকার হলে post-এর পরে cache clear যোগ করা যায় (পরবর্তী ধাপ)।
- §8: FCM নেই — instant push "সম্পূর্ণ" দাবি করা হয়নি; notification near-realtime (~১৫ মিনিট)।
- কোনো Design/Layout/Colour/Button/Permission/Branch Rule/Workflow বদলানো হয়নি; Diet Chart ছোঁয়া হয়নি।
