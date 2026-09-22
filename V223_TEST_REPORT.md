# V223_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম:** Android build/device/live-Supabase test আমি করিনি — **Pending**। SQL নিজে চালাইনি। যা চালানো হয়নি তাকে Pass বলা হয়নি।

## AUTOMATED / STATIC / GUARD — PASS
| পরীক্ষা | ফল |
|---|---|
| **`node 11_V223_TESTS/V223_logic_tests.js`** — ৪১টি scenario test | ✅ **PASS 41 / FAIL 0** (`V223_test_results.txt`) |
| — LANDED/SUPERSEDED/FAILED detection (old/newer/read-fail/no-trigger) | ✅ |
| — clearConfirmed: নতুন pending preserved, other-id/DELETE/disjoint untouched | ✅ |
| — Restore outcome: read-fail→BLOCK, no-row→WRITE, cloud-newer→KEEP, can't-compare→BLOCK | ✅ |
| — আসল web app.js: same-mobile ২ রোগী আলাদা id+nonce, Android↔Web byte-parity, crash-retry no-dup | ✅ |
| — simultaneous-save: SUPERSEDED replay pending মোছে না | ✅ |
| `python3 00_GUARD/tk_guard.py` — সব যাচাই | ✅ **সব পাশ (V223)** |
| `node --check` — দুই app.js · app.js/index.html parity | ✅ OK · IDENTICAL |
| ৩টি স্বাধীন Review (data-loss / race / parity-regression) | ✅ সব PASS (তোলা ২ বিষয় ঠিক করে re-verify) |
| Rollback-diff = ৮ in-scope ফাইল + ১ SQL + tests, আর কিছু নয় | ✅ নিশ্চিত |

## Pending — device/build/live/SQL
| Test | Status |
|---|---|
| §C1 cloud-read-fail-এ Restore বন্ধ ও Error (Trash/CloudJSON/Web/Bulk) | ⛔ PENDING (device/browser + নেট-fail সৃষ্টি) — লজিক verified |
| §C2 trigger পুরোনো লেখা আটকালে "success" নয় ও Pending অক্ষত (LANDED-নিশ্চয়তার পর clear) | ⛔ PENDING (live + trigger প্রয়োগ) — লজিক verified |
| §3/§4 সব restore + cross-device — সংঘর্ষে নতুন জেতে | ⛔ PENDING (দুই ফোন + live) — লজিক verified |
| DB trigger এক-টেবিল টেস্ট (SQL PART 5) | ⛔ PENDING — TK চালাবেন |
| Gradle build / Signed APK | ⛔ PENDING — Android Studio + keystore |

## সৎ নোট
- **C2 (normal-save regression নেই):** trigger না-থাকা অবস্থায় (এখন) merge-duplicates সবসময় আমাদের row বসায় → ফেরা updatedAt == পাঠানো → LANDED → আগের মতোই clearConfirmed। subset PATCH (updatedAt-হীন) → verify=false → row মিললেই LANDED। `updatedAt`-হীন টেবিল (deleted_records/activity_logs/trash) → return=minimal (আগের মতোই)।
- **C1:** cloud না-পড়া গেলে/তুলনা অসম্ভব হলে **কিছুই লেখা হয় না** (BLOCKED) — পুরোনো data কখনো আন্দাজে লেখা হয় না; ব্যবহারকারী সহজ Error দেখে; net-fail rows retry-তে গোনা হয় (নীরব নয়)।
- **অপরিবর্তিত:** Refund হিসাব/approval/nonce (CloudWriteQueue/PaymentModel/PaymentRepository byte-identical); Design/Workflow/Permission/Payment/Login কিছুই নয়। SQL নিজে চালানো হয়নি।
