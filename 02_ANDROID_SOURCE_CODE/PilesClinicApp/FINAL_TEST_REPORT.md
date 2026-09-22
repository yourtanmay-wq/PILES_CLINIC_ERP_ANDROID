# Final Test Report

## How this was actually verified (be precise about this)
This project was built in a sandbox with **no Android SDK, no Gradle, no
emulator, and no internet access**. That means:
- ❌ No real compile was run. No `assembleDebug`/`assembleRelease` executed.
- ❌ No instrumented/UI test or emulator run was performed.
- ✅ Static verification was performed for every phase: all XML files parsed
  as well-formed, all Kotlin files' braces/parentheses balanced, every
  `findViewById`/`R.id.*` reference cross-checked against its actual layout
  file, every `@color/*` reference checked against `colors.xml`.
- ✅ A JVM unit-test suite (`app/src/test/...`) was added for logic that
  doesn't require the Android framework — see below. These are real,
  runnable JUnit tests, they just haven't been executed here (no JVM/Gradle
  available in this sandbox either).

**Before shipping, run this in a real Android Studio**: open the project,
let Gradle sync, fix anything that sync/compile surfaces (there's a real
chance of small issues even after this level of static checking — e.g. an
exact dependency version conflict that only a real resolver would catch),
then work through the manual checklist below on a device or emulator.

## Automated unit tests included (run with `./gradlew test`)
| Test class | Covers |
|---|---|
| `ClinicalRepositoryTest` | reference lists never empty, visit history ordering, role parsing incl. invalid input |
| `BranchCatalogTest` | branch catalog integrity, branch toggling |
| `SyncSummaryTest` | push/pull success vs. failure classification |

## Manual regression checklist (do this on a real device/emulator)
**Phase 1-3 (WebView ERP) — must still work unchanged**
- [ ] App launches to the existing WebView Login/Dashboard exactly as before
- [ ] Camera/gallery photo picker still works from within the WebView
- [ ] Back button still navigates WebView history correctly

**Phase 4 — Clinical Modules** (`adb shell am start -n com.tkbiswas.pilesclinic/.clinical.ClinicalModulesActivity`)
- [ ] Doctor Check-up: save + reopen shows the saved record
- [ ] Prescription: add from reference list, add custom, remove, save
- [ ] Medicine Slip: preview shows current prescription; Print now opens real PDF preview (Phase 6)
- [ ] Investigation Advice: Staff can request, only Doctor can mark Advised
- [ ] Diet Chart: Allowed/Avoid selection saves
- [ ] Patient History: shows all of the above as a timeline

**Phase 5 — Sync** (`adb shell am start -n com.tkbiswas.pilesclinic/.sync.SyncStatusActivity`)
- [ ] Sign in with a real Supabase user succeeds
- [ ] Add Test Record for all 4 tables while offline → status Pending
- [ ] Reconnect → auto-sync fires (or tap Sync Now) → status Synced
- [ ] Force a bad URL/anon key → push shows Failed, app doesn't crash, data still present

**Phase 6 — Print** (`adb shell am start -n com.tkbiswas.pilesclinic/.print.PrintCenterActivity`)
- [ ] Each of the 5 print types renders a correct A4 preview with header/footer/QR
- [ ] Multi-page prescription (10+ medicines) paginates correctly with repeated header/footer
- [ ] No patient photo appears anywhere in any generated PDF
- [ ] Save PDF, Share PDF, and Print (via a real/virtual printer) all succeed

**Phase 9 — Security & Settings** (`adb shell am start -n com.tkbiswas.pilesclinic/.security.SettingsActivity`)
- [ ] Staff role sees the read-only banner and cannot edit settings
- [ ] Session timeout: sign in, background the app past the configured
      timeout, resume → session is cleared
- [ ] Backup Now creates a `.db` file under `Android/data/.../files/Backups`
- [ ] Restore Latest Backup restores correctly after an app restart
- [ ] Force a crash (e.g. temporarily throw in a button handler) → confirm a
      crash log file appears and is viewable from Settings, and the app
      relaunches normally afterward
- [ ] Only Doctor can switch the Print Center branch toggle

## Known gaps / not testable without a real environment
- Actual battery/perf profiling (frame rendering, memory profiler) — needs
  Android Studio Profiler on a real build.
- Play Store internal testing track upload/rollout — needs a real signed AAB.
- Any Gradle dependency resolution conflict that only a live sync would surface.
