# V227 — Changed files

**Base:** V226 draft। **Build:** এই cloud-এ Android SDK নেই → build হয়নি; owner Android Studio-তে করবেন। FINAL নয়।

## এই version-এ সত্যিই করা কাজ
1. **Trash Bin — Master branch filter (item 46):** Master এখন Trash-এও branch বেছে নিতে পারে (default All = আগের মতো সব), ঠিক Draft/Reject/Incomplete-এর মতো। Staff behavior অপরিবর্তিত (তারা আগের মতোই Trash-এ ঢুকতে পারে না)। Cloud query অপরিবর্তিত — filter শুধু client-side, তাই বাড়তি cloud খরচ নেই।
2. **Back করলে scroll ঠিক থাকা (item 26):** Payment ও Patient-Action (Timeline) list-এ refresh/ফেরার পর আগের scroll জায়গা ধরে রাখে — Doctor Queue/Follow-up-এ থাকা প্রমাণিত pattern-ই ব্যবহার করা হয়েছে।
3. **Version:** 227 / 2.27 (Android auto-display + Web `?v=v227`)।
4. **(V226 থেকে বহাল)** ReportsRepository `monthOf()` তারিখ-ধাঁচ robustness (item 86)।

## পরিবর্তিত ফাইল (৭টি)
- `…/app/build.gradle.kts` — version 227/2.27
- `…/native/TrashBinActivity.kt` — Master branch filter
- `…/res/layout/activity_trash_bin.xml` — filter view (Master-only, default hidden)
- `…/native/PaymentActivity.kt` — scroll preserve
- `…/native/PatientTimelineActivity.kt` — scroll preserve
- `…/assets/www/index.html` ও `03_NETLIFY_READY/index.html` — web `?v=v227`

## Rollback
`ROLLBACK_V227/` — সাতটি ফাইলের সত্যিকারের pre-V227 (=V226) কপি; যাচাই: rollback==V226, rollback≠V227 (কার্যকর)।

## যা বদলানো হয়নি
এই ৭টি ছাড়া কোনো design, layout, colour, payment হিসাব, print, login, permission, branch rule, database rule, patient data, `.git`, asset বা পুরোনো file নয়।
