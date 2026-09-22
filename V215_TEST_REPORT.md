# V215_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম (TK):** "যে Test সত্যিই চালানো হয়নি, তাকে Pass লিখবেন না।" — এই রিপোর্টে তা কঠোরভাবে মানা হয়েছে।

**পরিবেশ:** Claude (Cowork) cloud Linux container। এখানে **আসল Android device নেই, দ্বিতীয় ফোন নেই, live Supabase project connected নেই, Android SDK/Gradle দিয়ে পুরো build করা হয়নি।** তাই device/multi-phone/weak-internet/build test এখানে **চালানো সম্ভব নয়** — সেগুলো নিচে সততার সঙ্গে "NOT RUN HERE — TK-এর Android Studio + ফোনে চালাতে হবে" লেখা।

## এখানে সত্যিই যা চালানো হয়েছে (STATIC)

| পরীক্ষা | ফল |
|---|---|
| Bracket/paren/brace balance — ১০টা পরিবর্তিত Kotlin file (string/comment-aware) | ✅ PASS — সব BALANCED (unclosed=0, mismatched=0) |
| `node --check` — Netlify config.js ও assets/www/config.js | ✅ PASS |
| পরিবর্তিত অংশে fully-qualified `kotlinx.coroutines.async/launch` red-alert প্যাটার্ন | ✅ নেই (কোথাও যোগ করা হয়নি) |
| নতুন/বদলানো call-site vs signature হাতে মিলিয়ে দেখা (updateStatus out-param default, updateRemark param, showList bucket arg, DeletedGuard 2/3-arg) | ✅ মিলেছে |
| `_headers` CSP Report-Only (কিছু block করে না — নিরাপদ) | ✅ যাচাই |
| SQL PART B/C ইচ্ছাকৃত COMMENT (live app না ভাঙার জন্য) | ✅ যাচাই |

> ⚠️ Static balance check **compile নয়** — এটা bracket-ভারসাম্য দেখে, type-check করে না। চূড়ান্ত নিশ্চয়তা TK-এর Android Studio build থেকেই আসবে।

## §19-এর ২৩টা টেস্ট — সৎ status

| # | Test | Status | নোট |
|---|---|---|---|
| 1 | Weak Internet | ⛔ NOT RUN HERE | ফোনে TK চালাবেন |
| 2 | No-Internet Save | ⛔ NOT RUN HERE | queue যুক্তি অপরিবর্তিত রাখা হয়েছে |
| 3 | Retry | ⛔ NOT RUN HERE | |
| 4 | Same-phone immediate display | ⛔ NOT RUN HERE | §16/§18-এ local-first পথ ব্যবহার করা হয়েছে |
| 5 | Other-phone sync | ⛔ NOT RUN HERE | দ্বিতীয় ফোন নেই |
| 6 | Delete → Trash | ⛔ NOT RUN HERE (কোড-লজিক verified) | §18: tombstone+snapshot যোগ; device-এ যাচাই বাকি |
| 7 | Trash → Restore | ⛔ NOT RUN HERE (কোড-লজিক verified) | §18: restore-এ unmark যোগ |
| 8 | Incomplete Patient | ⛔ NOT RUN HERE (কোড-লজিক verified) | §16: finish()+honest message |
| 9 | Refund | ⛔ NOT RUN — feature কোড-এ বসানো হয়নি (spec দেওয়া হলো) | §13 |
| 10 | Staff Refund Approval | ⛔ NOT RUN — একই | §13 |
| 11 | Same-Day Staff Delete | ⛔ NOT RUN HERE | §14 বর্তমান gate অপরিবর্তিত (নিচে দ্রষ্টব্য) |
| 12 | Call Signal Increase | ⛔ NOT RUN HERE (কোড-লজিক verified) | §17: সব stage-এ count |
| 13 | Last Call Update | ⛔ NOT RUN HERE (কোড-লজিক verified) | §17 |
| 14 | CHECK-UP Queue Back | ⛔ NOT RUN — Queue loading/scroll কোড-এ করা হয়নি (spec) | §10 |
| 15 | Follow-up Back | ⛔ NOT RUN HERE (কোড-লজিক verified) | §11: Payment/Register one-Back finish() |
| 16 | Scroll Position Preserve | ⛔ NOT RUN — করা হয়নি (spec) | §10/§11 |
| 17 | Briefing Notification | ⛔ NOT RUN HERE (কোড-লজিক verified) | §15: near-realtime (~15 min), instant push নয় (FCM লাগবে) |
| 18 | Android/Web Permission | ⛔ NOT RUN HERE | client-side rule অপরিবর্তিত (RLS আলাদা ধাপ) |
| 19 | Password Security | ⛔ NOT RUN — hashing live-এ চালু করা হয়নি (Auth ধাপ) | §4 |
| 20 | Backup Completeness | ⛔ NOT RUN HERE | |
| 21 | Build Test (Gradle) | ⛔ NOT RUN HERE | Android Studio-তে TK করবেন |
| 22 | JavaScript Syntax | ✅ PASS (config.js `node --check`) | পুরো app.js-এ পরিবর্তন হয়নি |
| 23 | XML/Resource | ⛔ NOT RUN HERE | XML পরিবর্তন করা হয়নি |

## গুরুত্বপূর্ণ সৎ নোট
- §17-এ call **গোনা ও Last Call Date** ঠিক হয়েছে; কিন্তু patient-card-এ signal **meter** (wifi-bar) আঁকা হয়নি — কারণ ওটা approved card design বদলাবে (§2)। TK চাইলে আলাদা করে অনুমতি নিয়ে যোগ করা যাবে।
- §14 same-day staff-delete: বর্তমান UI gate `DeletePermission.canDeleteNow` (staff→master approval) **ইচ্ছাকৃতভাবে অপরিবর্তিত** — এটা বদলানো permission-নিয়ম বদলানো, testing ছাড়া live-এ ঝুঁকি। §14 চাইলে আলাদা সিদ্ধান্ত লাগবে।
- কোনো Test এখানে "Pass" লেখা হয়নি যেটা সত্যিই চালানো যায়নি।
