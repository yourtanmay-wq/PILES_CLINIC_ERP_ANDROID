# PILES CLINIC APP V328 — COMPLETE PROJECT HANDOFF

## Project identity

- Android `versionCode`: `328`
- Android `versionName`: `3.28`
- Android Studio project: `02_ANDROID_SOURCE_CODE/PilesClinicApp`
- Package status: `FINAL_BUILD_PENDING`

## Owner-approved V328 result — 2026-08-12 18:35–18:45 IST

- RMP list card counts normally use the protected small server result; the unchanged old patient download remains automatic fallback.
- Selected RMP View All normally uses the protected selected-patient result; the unchanged old patients + payments calculation remains automatic fallback.
- Master RMP Performance normally uses the protected small metric result; the unchanged old patient calculation remains automatic fallback.
- All three server functions passed the required live doorway proof: authenticated access `true`, anonymous access `false`.
- No Patient, Payment, Refund, RMP, Commission or Expense row was deleted or rewritten by these loading changes.
- Existing RMP designs, Branch picker, card order, labels, buttons and tap destinations were not changed by the loading work.

## Verification

- Guard confirmed Kotlin-aware bracket balance for all 223 main Kotlin files.
- All 279 Android XML files parsed successfully; binding/drawable and Supabase column checks passed.
- Web JavaScript and JSON syntax checks passed.
- Android identity is consistently V328 / 3.28.
- The broad internal name `Result` introduced by the RMP files was renamed to specific internal names so it cannot conflict with Android WorkManager's own `Result`; behavior is unchanged.
- The original V323 project already had Guard warnings about several old locked message strings and Bengali-disabled translations. They are outside the authorized RMP scope and were deliberately not changed.

## Honest build status

- A full Gradle build was attempted, but this environment cannot download Gradle 8.5 from the Gradle server.
- Therefore this package is honestly named `BUILD_PENDING`; it is not falsely described as Android Build Passed.
- Android Studio must perform the final Gradle Sync/Build. No additional SQL is required for the three V328 RMP loading functions already run and proved by the owner.

## Locked instruction

No person or AI may change any design, workflow, calculation, role, Branch rule, database rule or other existing feature without the owner's explicit permission. Only owner-approved work recorded in `V325_WORK_LOG_RMP_COMMISSION_2026-08-12.md` belongs to this handoff.

## Owner-approved Web parity update 2 — 2026-08-12 19:12–19:35 IST

- Web Doctor RMP access, small-result RMP counts/View All/Branch Performance, complete-mobile patient verification, professional Saved RMP cards and Prescription detail boxes were completed.
- The same live V328 protected functions are reused; no additional SQL is required.
- Old local calculations remain automatic fallback and all small read results are kept for two minutes to protect Supabase Free Plan usage.
- Web syntax and targeted parity checks passed. No Android application source, financial calculation or database row was changed.
- A fresh Gradle build was attempted after this update, but the environment could not download Gradle 8.5 (`Network is unreachable`). Android Studio build therefore remains honestly PENDING.
