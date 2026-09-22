# V222_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম:** Android build/device/live-Supabase test আমি করিনি — স্পষ্ট **Pending**। SQL নিজে চালাইনি। যা সত্যিই চালানো হয়নি তাকে Pass বলা হয়নি।
**পরিবেশ:** cloud container — আসল Android device/দ্বিতীয় ফোন/Gradle build/live Supabase নেই।

## STATIC / GUARD — PASS
| পরীক্ষা | ফল |
|---|---|
| `python3 00_GUARD/tk_guard.py` — **সব যাচাই** | ✅ **সব পাশ** (V222) |
| ৯.১ Kotlin bracket-balance (১৮৯ ফাইল, পরিবর্তিত ৫ Kotlin সহ) | ✅ BALANCED |
| ৯.৭ Supabase কলাম মেলে · ৯.১০ static-call · ৯.১২/৯.১৩ locked/work rules · ৯.১৪ No-Bengali | ✅ পাশ |
| ৯.৮ ভার্সন এক — **V222** সর্বত্র | ✅ পাশ |
| `node --check` — `03_NETLIFY_READY/app.js` ও `assets/www/app.js` | ✅ PASS |
| app.js parity (Netlify vs assets/www) · index.html parity | ✅ **IDENTICAL** |
| স্বাধীন সাব-এজেন্ট code-review — ৩ requirement + compile + parity | ✅ ঠিক (তোলা দুর্বলতা সঙ্গে সঙ্গে ঠিক) |
| Rollback-diff (ROLLBACK_V221 vs current) = ১১ in-scope ফাইল + ১ নতুন SQL, আর কিছু নয় | ✅ নিশ্চিত |

## Pending — device/build/live/SQL
| Test | Status |
|---|---|
| §১ একই row-এ পুরোনো Save সফল হলে পুরোনো Pending সরে, কিন্তু নতুন Remark/Date/Payment/Follow-up Pending **টেকে** | ⛔ PENDING (device + দুই দ্রুত save) — কোড-লজিক verified |
| §২ এক মোবাইলে দুই আলাদা রোগীর Refund আলাদা id; crash/restart-এ duplicate নয়; দুই বৈধ আলাদা Refund আলাদা — Android **ও** web | ⛔ PENDING (device/browser + live) — কোড-লজিক verified |
| §৩ Trash/Cloud-JSON/Web Restore-এ পুরোনো data নতুন cloud data চাপা দেয় না; kept-newer জানায় | ⛔ PENDING (device/browser + live) — কোড-লজিক verified |
| §৩ DB trigger — এক-টেবিল টেস্ট (PART 5) | ⛔ PENDING — TK Supabase SQL Editor-এ চালাবেন (আমি চালাইনি) |
| Gradle build / Signed APK | ⛔ PENDING — Android Studio + TK keystore; signed দাবি করা হয়নি |

## সৎ নোট (আচরণ ও সীমা)
- **§১:** clearConfirmed **শুধু সফল লেখা শুরুর আগে (at ≤ writeStart) জমা** কাজ পরিষ্কার করে। এই লেখা চলাকালীন/পরে জমা নতুন কাজ কখনো নয়। UPSERT সফল হলে পুরো-row বলে ঐ id-র পুরোনো UPSERT/UPDATE বাতিল; UPDATE সফল হলে শুধু **subset-ঘরের** পুরোনো UPDATE (আলাদা-ঘরের Remark/Date বা পুরো-row UPSERT নয়)। অন্য id/DELETE কখনো নয়।
- **§২:** id ও nonce উভয়ে `patient.id`। এক রোগীর retry একই id; confirm হলে nonce মোছে বলে পরের বৈধ আলাদা Refund আলাদা id। Refund total/approval/Visit Fee/branch/payment হিসাব অপরিবর্তিত (শুধু id-র ভিতরের উপাদান বাড়ল)। **উন্নয়ন-সীমা:** V221-এ চালু থাকা কোনো *অসম্পূর্ণ* Refund (পুরোনো mobile-key nonce) V222-তে retry করলে key-format বদলের কারণে নতুন id হতে পারে — অত্যন্ত বিরল (app-update পার হওয়া unsynced refund), আর `maxRefundable` backstop paid-এর বেশি বেরোতে দেয় না।
- **§৩:** App-স্তরে newer-wins (Trash/Cloud-JSON/web-Trash) — cloud কড়া নবীন হলে overwrite নয়, "restored/kept newer" দেখানো হয় (silent loss/false success নয়); read ব্যর্থ হলে স্বাভাবিক upsert (restore আটকায় না)। DB trigger সব পথের (অন্য ফোন সহ) সর্বজনীন backstop — **TK চালালে** সম্পূর্ণ সুরক্ষা।
- **Free-plan:** §১/§২ নতুন cloud read/write নয়। §৩-এর read শুধু **Restore-পথে** (Trash = ১ read; Cloud-JSON = টেবিল-প্রতি ১ read) — রোজকার সেভে নয়; বরং §১ পুরোনো ভুল-data resend *কমায়*।
- কোনো Design/Layout/Colour/Button/Print/Diet Chart/Workflow/Permission/Branch/Login বদল নেই।
