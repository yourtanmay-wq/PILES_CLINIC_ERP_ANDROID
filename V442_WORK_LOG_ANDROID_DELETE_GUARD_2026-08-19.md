# V442 — Android DeletedGuard 3000-limit safety fix

**তারিখ:** 19.08.2026 · **অনুমতি:** 09:44 IST  
**Base:** V441 / 4.41 → **V442 / 4.42**

## মালিকের অনুমোদিত কাজ

শুধু Android `DeletedGuard`-এর 3000 tombstone সীমা নিরাপদভাবে সরানো।
Login/session, Global Search, Doctor Queue, design, payment, medical workflow,
Supabase SQL বা Web business logic পরিবর্তনের অনুমতি ছিল না এবং সেগুলো ছোঁয়া হয়নি।

## যাচাই করে পাওয়া আসল ঝুঁকি

V441-এ একই 3000 সীমা দুই জায়গায় ছিল:
1. Cloud থেকে `deleted_records` সর্বোচ্চ 3000 id পড়ত।
2. Phone-এর `cache` ও `localOnly`-ও 3000 ছাড়ালে সবচেয়ে পুরনো marker ফেলে দিত।

তাই শুধু cloud pagination করলে কাজ অসম্পূর্ণ থাকত। দুটো একসাথে ঠিক করা হয়েছে।

## V442-এ যা করা হয়েছে

- `DeletedGuard`-এর local 3000-size pruning সম্পূর্ণ সরানো।
- Cloud sync-এ 1000 row/page pagination।
- Multi-page read-এর আগে/পরে exact count + newest id মিলিয়ে stable/complete প্রমাণ।
- কোনো page/network failure বা cloud change হলে fail-closed: **কোনো local tombstone remove হয় না**;
  শুধু যেগুলো নিশ্চিতভাবে পাওয়া গেছে সেগুলো guard-এ যোগ হয়।
- `SupabaseClient.fetchListOrNull` / `fetchListSlimOrNull`-এ optional `offset` যোগ করা হয়েছে।
  `offset=0` default হওয়ায় পুরনো সব caller-এর URL/behavior অপরিবর্তিত; positive offset শুধু DeletedGuard ব্যবহার করে।
- `deleted_records` বা `trash` থেকে কোনো row delete করা হয়নি; কোনো SQL নেই।

## Version

- Android: V442 / 4.42
- Web `version.json`: V442 / 4.42 (version parity only; web business logic unchanged)

## বিশেষ নিরাপত্তা

`deleted_records` table sync চলার মধ্যে বদলালে Restore propagation সাময়িকভাবে
পরের sync পর্যন্ত দেরি হতে পারে, কিন্তু delete guard ভুল করে খুলে যাবে না। এই
fail-safe ইচ্ছাকৃত — “ডিলিট করা রেকর্ড যেন ফিরে না আসে” নিয়মকে অগ্রাধিকার দেওয়া হয়েছে।

## Verification — 19.08.2026

- `tk_guard.py` — all machine checks PASS ✅
- `verify_version_json.py` — V442 / 4.42 parity PASS ✅
- Synthetic pagination — 6203 tombstone ids: all 6203 read ✅
- Simulated server-side cap 250 rows/page: still all 6203 read ✅
- Stable Restore rule: cloud-absent marker removed, `localOnly` pending marker retained ✅
- Unstable/mid-change cloud rule: no marker removed ✅
- `DeletedGuard.kt` Kotlin type/syntax compile with dependency stubs PASS ✅
- Search confirms old `MAX=3000`, 3000 cloud fetch and size-pruning loops are gone ✅

### Android Gradle build honesty

`./gradlew :app:compileDebugKotlin --offline --no-daemon` was attempted. The wrapper
could not start Gradle because this execution environment cannot resolve
`services.gradle.org` to download Gradle 8.5 (`UnknownHostException`). Therefore an
actual Android Gradle build is **not claimed as passed** here. This was an environment
download limitation, not a reported Kotlin/Gradle compile error from the project.

## Release guard

`tk_guard.py --release` PASS ✅  
Unique release name reserved: `PILES_CLINIC_APP_V442_FINAL.zip`.
