# V221_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম:** Android build/device/live-Supabase test আমি করিনি — স্পষ্ট **Pending**। যা সত্যিই চালানো হয়নি তাকে Pass বলা হয়নি।
**পরিবেশ:** cloud container — আসল Android device/দ্বিতীয় ফোন/Gradle build/live Supabase নেই।

## STATIC / GUARD — PASS
| পরীক্ষা | ফল |
|---|---|
| `python3 00_GUARD/tk_guard.py` — **সব যাচাই** (৯.১–৯.১৫, ৪.৫/৪.৬, ১০, ১১) | ✅ **সব পাশ** (আগের ২টি ৯.১৪ ব্যর্থতা এখন ঠিক) |
| ৯.১৪ 🚫 বাংলা-বন্ধ স্টাফের পর্দা — "আরও"/"আটকে:" অনূদিত | ✅ পাশ |
| ৯.১ Kotlin bracket-balance (১৮৯ ফাইল, পরিবর্তিত ৪ Kotlin সহ) | ✅ BALANCED |
| ৯.৭ Supabase কলাম মেলে · ৯.১০ static-call · ৯.১২/৯.১৩ locked/work rules অক্ষত | ✅ পাশ |
| ৯.৮ ভার্সন এক — **V221** সর্বত্র | ✅ পাশ |
| `node --check` — `03_NETLIFY_READY/app.js` | ✅ PASS |
| `node --check` — `assets/www/app.js` | ✅ PASS |
| app.js parity: Netlify vs assets/www | ✅ **IDENTICAL** |
| index.html parity: Netlify vs assets/www | ✅ **IDENTICAL** |
| Rollback-diff (ROLLBACK_V220 vs current) = ঠিক ৯টি in-scope ফাইল, আর কিছু নয় | ✅ নিশ্চিত |
| Call-site: `saveRefund`, `refundIdFor`, `buildRefundRow`, `clearConfirmed` signature-মিল | ✅ মিলেছে |

## Pending — device/build/live
| Test | Status |
|---|---|
| §১ Bengali-off স্টাফ ফোনে sync-স্ট্যাটাসে বাংলা নেই (আসল লগইন) | ⛔ PENDING — কোড/guard-verified |
| §২ HTTP 400 park → record ঠিক করে সফল সেভ → লাল Warning সরে (একই row); অন্য row-এর pending অক্ষত | ⛔ PENDING (live 400 + device দরকার) — কোড-লজিক verified |
| §৩ Refund cloud-fail → App kill/restart → একই Refund আবার = একই id (Duplicate নয়); confirm-এর পরে বৈধ আলাদা Refund = আলাদা id — Android **ও** web | ⛔ PENDING (device/browser + live) — কোড-লজিক verified |
| Gradle build / Signed APK | ⛔ PENDING — Android Studio + TK keystore; signed দাবি করা হয়নি |

## সৎ নোট (আচরণের সূক্ষ্ম কথা)
- **§২:** `clearConfirmed` শুধু **আসল cloud-success** (`upsert` ok / `updateById` changed)-এ চলে; local save-এ নয় — তাই Warning কেবল সত্যিকারের success-এর পরেই সরে। শুধু ঐ (table,id)-এর UPSERT/UPDATE মোছে; **DELETE ও অন্য record অক্ষত**। সারি ফাঁকা থাকলে হট-পথে কোনো ফাইল পড়া হয় না (`hasQueue`)।
- **§৩:** persist-nonce draft-key = মোবাইল+টাকা(পয়সা)+কারণ+আজকের তারিখ। **দুটি বৈধ আলাদা Refund** (ভিন্ন টাকা বা কারণ, অথবা আগেরটা cloud-confirm হওয়ার পরে করা) সবসময় **আলাদা id** পায় — সুবিধা অক্ষত। একটাই ব্যতিক্রম, ইচ্ছাকৃত ও নিরাপদ: হুবহু একই (রোগী+টাকা+কারণ+একই দিন) Refund যদি **আগেরটা cloud-এ বসার আগেই** আবার করা হয়, সেটিকে একই অসম্পূর্ণ Refund ধরে একই id দেওয়া হয় (Duplicate ঠেকাতে) — এটিই "একই অসম্পূর্ণ Refund একই id-তে retry" নিয়মের সরাসরি ফল। মধ্যরাত পার হয়ে retry হলে তারিখ বদলায় বলে id বদলাতে পারে — V220-এও একই আচরণ ছিল (তারিখ id-র অংশ), তাই regression নয়।
- **Free-plan:** §১/§২/§৩ কোনোটাই নতুন cloud read/write যোগ করে না। §২ বরং পুরোনো ভুল-data resend *কমায়*; nonce store পুরোপুরি লোকাল।
- কোনো Design/Layout/Colour/Button/Text-arrangement/Workflow/Permission/Branch/Payment rule বদলানো হয়নি; Diet Chart/Print/Login ছোঁয়া হয়নি; Backup/Restore/SQL/RLS ছোঁয়া হয়নি।
