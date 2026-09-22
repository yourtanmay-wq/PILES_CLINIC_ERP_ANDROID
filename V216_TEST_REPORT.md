# V216_TEST_REPORT.md — সৎ পরীক্ষা রিপোর্ট

**নিয়ম:** যা সত্যিই চালানো হয়নি তাকে "Pass" লেখা হয়নি।
**পরিবেশ:** cloud container — আসল Android device/দ্বিতীয় ফোন/Gradle build/live Supabase নেই।

## এখানে সত্যিই চালানো (STATIC) — PASS
| পরীক্ষা | ফল |
|---|---|
| Bracket/paren/brace balance — V216-এ পরিবর্তিত/নতুন ১০ Kotlin file | ✅ সব BALANCED |
| activity_briefing.xml well-formed (XML parse) | ✅ PASS |
| index.html/config.js — পরিবর্তন সীমিত ও বৈধ | ✅ |
| call-site vs signature হাতে-মিলানো: saveRefund/approveRefund/rejectRefund/fetchPendingRefundRequests, HasCustom(password,hash), storePasswordHash, buildRefundRow, TimelineCache.load/save, PasswordHasher API, SupabaseAuth | ✅ মিলেছে |
| OkHttp body idiom project-এর নিজস্ব (`toRequestBody`/`toMediaType`)-এর সঙ্গে মিল | ✅ |
| SQL PART B (RLS) ইচ্ছাকৃত COMMENT | ✅ |

> ⚠️ Static balance = compile নয়; চূড়ান্ত নিশ্চয়তা TK-এর Android Studio build থেকে।

## §19 device-tests — সৎ status (V216-সংশ্লিষ্ট)
| Test | Status |
|---|---|
| Refund Test (#9) | ⛔ NOT RUN HERE — কোড-লজিক verified (approved refund per-patient paid ও collection থেকে বিয়োগ, visit fee অক্ষত)। ফোনে যাচাই দরকার। |
| Staff Refund Approval Test (#10) | ⛔ NOT RUN HERE — Staff→pending+briefing, Master Briefing পর্দায় Approve/Reject → approved হলে total কমে। device-এ যাচাই দরকার। |
| Report loading/Back (#14) | ⛔ NOT RUN HERE — cache-first কোড-লজিক verified; scroll preserve করা হয়নি (নিচে)। |
| Password Security (#19) | ⛔ NOT RUN HERE — hash/verify/lazy-migration কোড-লজিক verified; আসল login-flow device-এ যাচাই দরকার। |
| Build (#21) / Signed APK (#20.10) | ⛔ NOT RUN HERE — Android Studio + TK-এর keystore। signed দাবি করা হয়নি। |
| Weak-internet / No-internet / Retry / Other-phone sync (#1-5) | ⛔ NOT RUN HERE — দ্বিতীয় ফোন/নেট নেই। |

## গুরুত্বপূর্ণ সৎ নোট (V216)
- **§13 Refund টাকা-হিসাব:** per-patient paid total থেকে approved refund বিয়োগ যাচাই করা কোড-লজিকে দৃঢ়। today/range **collection screen**-এ refund negative-row হিসেবে যোগফল কমায় — কিন্তু collection **তালিকা UI** negative row কীভাবে দেখায়/সমষ্টি করে তা device-এ যাচাই করা দরকার (এই পরিবেশে দেখা যায়নি)। TK: Refund Test-এ মোট ঠিক কমছে কিনা মিলিয়ে নেবেন।
- **§4 password:** নতুন password সেভে fresh hash হয় বলে password বদলালে পুরোনো hash আটকায় না। পুরোনো custom password lazy-migration-এ hash হবে (প্রথম সফল login-এ)। plaintext কলাম এখনো আছে (web/পুরোনো version চলে) — মুছবে আলাদা ধাপে (MANUAL_SETUP)।
- **§10:** Report cache না থাকলে আগের মতোই "Loading..." (regression নেই)। Journey/Action instant-header ও scroll করা হয়নি (কারণ CHANGED_FILES-এ)।
- কোনো untested item "Pass" লেখা হয়নি।
