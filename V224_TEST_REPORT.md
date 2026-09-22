# V224 — Test Report (2026-08-01)

## Build / Static test — REAL, this machine
- Toolchain: JDK 21 (Temurin), Gradle 8.5, AGP 8.2.2, Kotlin 1.9.22, Android SDK android-34 / build-tools 34.0.0.
- Command: `./gradlew.bat --no-daemon assembleDebug`
- **Result: BUILD SUCCESSFUL — 0 compile errors.** APK produced: `app/build/outputs/apk/debug/app-debug.apk` (~12.5 MB).
- Ran 3 times across the change batches (baseline, +item87, +item8/20) — all green.
- Only pre-existing Kotlin warnings (unused vars, deprecated scaledDensity, etc.) — none introduced by V224.

## What is NOT claimed (honesty — per owner rule 17)
- **No real-device UI test** was possible in this environment → button colour/size and any visual result must be confirmed by the owner in Android Studio + on a phone (the owner's normal live-test loop).
- **No live Supabase test** → the SQL (item 82/83) is schema-verified but must be run by the owner (STEP 1 duplicate-check first). Data-dependent items (sync 400 root cause, ghost records, orphan patient, current-month report) could not be diagnosed without live data and are listed as Manual Review in the MASTER WORK NOTE.

## Regression safety
- Only 3 Kotlin lines + 1 new SQL file changed. No data-path, permission, print, login, sync, or payment logic touched. Build green confirms no compile-level breakage of the 192-file project.
