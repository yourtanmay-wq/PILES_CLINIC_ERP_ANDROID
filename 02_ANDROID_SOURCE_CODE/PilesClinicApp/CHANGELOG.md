# Changelog — TK Biswas Piles Clinic (PilesClinicApp)

## IMPORTANT — please read first
This changelog only lists phases that were actually designed and built in
this project history. **Phases 7 and 8 were never specified or built** —
this request jumped from "Phase 6: Professional Print System" straight to
"Phase 9–10: Security + Final Production Release". Nothing under a "Phase 7"
or "Phase 8" label exists in this codebase. If those phases had planned
scope (e.g. a native Dashboard, native patient records UI, etc.), please
send that scope separately — right now Phase 9-10 has been built on top of
Phases 1-6 only.

Also, Phases 1-3 ("Login", "Dashboard", "Core Modules") are **not native** —
they are the original WebView + JavaScript ERP (`assets/www/app.js` etc.).
This was discovered and flagged when Phase 4 started, and every phase since
has been built as new, standalone native screens alongside that WebView
shell (see each phase's own notes below) rather than replacing it.

---

## v2.0.0 (versionCode 2) — this submission (Phase 9-10)
**A. Security**
- Role Security: centralized `security/SecurityGuard.kt` (Doctor vs Staff
  checks), reusing Phase 4's `RoleSession`.
- Branch Security: Staff can no longer switch the print branch
  (`print/PrintCenterActivity`) — only Doctor can. Reuses Phase 6's
  `BranchSession`.
- Session Management: `security/SessionTimeoutManager.kt` auto-clears the
  Supabase session (Phase 5) after a configurable idle period (default 30
  min), enforced app-wide via `ActivityLifecycleCallbacks` in
  `PilesClinicApplication` — no existing screen's code was touched to add this.
- Backup & Restore: `security/BackupManager.kt` — local `.db` file backup/restore
  for the offline-first Room database (Phase 5), independent of Supabase sync.
- App Settings: new `security/SettingsActivity` — session timeout, auto-sync
  toggle, crash-logging toggle, backup/restore, view last crash log.
- Error Handling / Crash Protection: `security/CrashHandler.kt` registered as
  the app's default uncaught-exception handler; logs a timestamped crash
  file locally before handing off to the system (doesn't try to "swallow"
  crashes, which would be unsafe).

**B. Final Production**
- Regression: added a `src/test` JVM unit-test suite covering pure-logic
  regressions (clinical reference data, role parsing, branch catalog, sync
  summary success/failure) — see FINAL_TEST_REPORT.md for what is and isn't
  covered this way vs. what still needs a manual/emulator pass.
- Performance: added Room indices on `syncStatus` (and `patientId` where
  relevant) across all 4 Phase 5 entities, for faster pending-sync lookups.
- Memory: `print/ClinicPdfBuilder` now recycles logo/QR bitmaps immediately
  after each print job instead of waiting on GC.
- Version Management: versionCode 1 → 2, versionName "1.0" → "2.0.0".
- Release signing: `app/build.gradle.kts` now supports a real release
  signing config, safely read from `local.properties` (never hardcoded) —
  see RELEASE_GUIDE.md. **No signed binary was produced by me** — see that
  guide for why and how to produce one yourself.
- ProGuard/R8 rules pre-staged for Gson/Retrofit/Room/ZXing in
  `proguard-rules.pro`, ready if `isMinifyEnabled` is turned on later.

## Phase 6 — Professional Print System
Native A4 PDF generation (`android.graphics.pdf`, no WebView), branch-wise
header/footer, QR code (ZXing) on each document, native print preview
(`PdfRenderer`), Save PDF / Share PDF / Android Print framework integration,
for Registration, Prescription, Medicine Slip, Investigation Advice, and
Diet Chart. Standalone `PrintCenterActivity` hub.

## Phase 5 — Supabase Integration + Offline-First
Room database (offline-first source of truth), Supabase Auth + PostgREST
CRUD over Retrofit/OkHttp (raw REST, not the supabase-kt SDK — see
SUPABASE_SETUP.md), WorkManager-based auto-sync on connectivity return,
Pending/Synced/Failed status per record, conflict-safe upsert-by-id sync.
Standalone `SyncStatusActivity` test/status screen.

## Phase 4 — Clinical Modules
Native Doctor Check-up, Prescription, Medicine Slip (preview), Investigation
Advice, Diet Chart, Patient History timeline — all native Kotlin/XML,
role-gated (Doctor write / Staff view where applicable). Standalone
`ClinicalModulesActivity` hub.

## Phase 1-3 (pre-existing, not built in this engagement)
WebView shell (`MainActivity`) loading the existing Login/Dashboard/Core
Modules ERP as bundled HTML/CSS/JS (`assets/www/`), unchanged throughout
every phase above.
