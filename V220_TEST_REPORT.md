# V220_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম:** Android build/device/live-Supabase test আমি করিনি — স্পষ্ট **Pending**। যা সত্যিই চালানো হয়নি তাকে Pass বলা হয়নি।
**পরিবেশ:** cloud container — আসল Android device/দ্বিতীয় ফোন/Gradle build/live Supabase নেই।

## STATIC — PASS
| পরীক্ষা | ফল |
|---|---|
| Bracket-balance — পরিবর্তিত ৫ Kotlin file (SupabaseClient, CloudWriteQueue, PaymentModel, PaymentRepository, PaymentActivity) | ✅ সব BALANCED |
| `node --check` — 03_NETLIFY_READY/app.js | ✅ PASS |
| `node --check` — assets/www/app.js + diff Netlify = **IDENTICAL** (§5 parity অটুট) | ✅ PASS |
| Call-site vs signature: errSummary, bodyHash, remember-guard, refundIdFor/buildRefundRow/saveRefund(nonce), refundNonce | ✅ মিলেছে |

## Pending — device/build/live (§10)
| Test | Status |
|---|---|
| §১ 400 হলে আসল Table·Record·ভুল Field সতর্কবার্তায় দেখা যায় | ⛔ PENDING (আসল 400 ঘটাতে live/device দরকার) — কোড-লজিক verified |
| §২ একই ভুল data auto-resend বন্ধ; record ঠিক করলে/"পাঠান" চাপলে যায় | ⛔ PENDING (live 4xx দরকার) — কোড-লজিক verified |
| §৪ Refund cloud-fail → আবার চাপা = একটাই Refund; নতুন ফর্মে বৈধ দ্বিতীয় = আলাদা — Android **ও** web | ⛔ PENDING (device/browser + live) — কোড-লজিক verified |
| Gradle build / Signed APK | ⛔ PENDING — Android Studio + TK keystore; signed দাবি করা হয়নি |

## সৎ নোট
- §২: 401/403/409/429 permanent ধরা হয়নি (transient হতে পারে) — আগের মতোই retry। permanent = শুধু 400/404/422। "পাঠান" (retryFailed) permanent সারিও সুযোগ দেয় (২ চেষ্টার পর আবার park) — তাই server-side ঠিক হলে যায়, কিন্তু অ্যাপ **নিজে থেকে** একই ভুল বারবার পাঠায় না।
- §৪: nonce ফর্ম-খোলা-ভিত্তিক। app crash/restart-এ ফর্ম গেলে nonce হারায় — তখন একই refund আবার করলে আলাদা id হতে পারে; তবে `maxRefundable` (paid − pending) মোট refund জমার বেশি হতে দেয় না, তাই টাকা-ঝুঁকি সীমিত। (এটাই retry-double vs বৈধ-দুই-refund-এর সঠিক ভারসাম্য।)
- §৩ কোড-এ করা হয়নি — `V220_SECTION3_BACKUP_RESTORE_REAUDIT.md` দ্রষ্টব্য।
- কোনো Design/Layout/Colour/Button/Permission/Branch/Payment rule বদলানো হয়নি; Diet Chart ছোঁয়া হয়নি।
