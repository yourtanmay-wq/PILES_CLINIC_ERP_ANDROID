# Final Changed Files List (complete, cumulative across the whole engagement)
Diffed directly against the original upload (`PilesClinicApp-Phase9-10.zip`).

## Modified (3 files)
1. `app/src/main/java/com/tkbiswas/pilesclinic/MainActivity.kt`
   - Registered `AndroidBridge` on the WebView (native print access).
   - `shouldOverrideUrlLoading` hands off tel:/wa.me/mailto:/geo:/meet.google.com (and any non-`file://` navigation) to the matching system app instead of trapping it in the WebView.

2. `app/src/main/assets/www/app.js`
   - `doPrintNow()` calls `window.Android.printPage()` when available (native OS print), falls back to `window.print()` otherwise.
   - `fuCard()` — Patient-stage card's pill now has the same triple-tap Continue/Cancelled handler Enquiry/Visit stages already had.
   - `visitedPillMenu()` / `visitedCancel()` — stage-aware wording ("Patient" vs "Visit") instead of always "Visit."
   - `attachMobileWatch()` — removed a self-corrupting live-rewrite of the mobile input value (root cause of backspace/delete/repeated-digit bugs).
   - Added `sanitizeMobileInput()` / `toggleMobPrefixBadge()` + one delegated `input` listener; `enquiryForm()`/`registration()` got `maxlength="10"` and a non-destructive `+91` badge.
   - `v280EnquiryMobileCheck()` / `v280RegistrationMobileCheck()` — fixed a dead length check that silently prevented the live duplicate-mobile popup from ever appearing.
   - `v280ContinueRegistration()` — fixed calls to two non-existent functions that made the "Continue Registration" button do nothing.
   - `isSeededRecord()` — **critical fix**: changed substring match to exact match, stopping silent deletion of real patients whose names contained "sumita"/"sobita" (or any other seed word) as part of a longer real name.

3. `app/src/main/assets/www/styles.css`
   - Added `.mobPrefixBadge` rule only (append-only).

## Added (1 file)
4. `app/src/main/java/com/tkbiswas/pilesclinic/bridge/AndroidBridge.kt`
   - `printPage()`, `nativeToast()`, `isNativePrintAvailable()`.

## Documentation added this final package (not app code)
- `SUPABASE_SETUP_GUIDE_ACTIVE_SYSTEM.md` — corrected setup guide for the actual active Supabase integration (the original `SUPABASE_SETUP.md` describes the unused native layer).
- `FINAL_BUILD_REPORT.md`, `FINAL_TEST_REPORT.md`, `FUTURE_UPDATE_GUIDE.md`, this file.

## Untouched
`index.html`, `config.js`, all Room/Retrofit/Sync/Clinical/Print/Security
native files, all layout XMLs, gradle files, manifest, tests — byte-identical
to the original upload. No V280 UI/design was redesigned at any point across
this entire engagement. No workflow order was changed. Every fix above is a
logic/wiring/data-integrity correction.
