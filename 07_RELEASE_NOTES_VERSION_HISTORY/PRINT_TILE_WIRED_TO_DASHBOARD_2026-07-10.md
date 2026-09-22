# FIX — Print button added to native Dashboard (2026-07-10)

## Problem
The full native Print system (PrintCenterActivity + PrintPreviewActivity +
ClinicPdfBuilder) already existed and was registered in AndroidManifest.xml,
but nothing on the native Dashboard opened it. Manifest comment even said it
was "standalone for now — no native Dashboard exists yet." When the native
Dashboard was later built, the Print tile was never added, so Print was
unreachable from the native UI.

## Fix (exactly 2 files touched, nothing else)
1. `app/src/main/res/layout/activity_dashboard.xml`
   - Added one tile to the module grid, between Doctor Visit and Menu:
     `<include android:id="@+id/tilePrint" layout="@layout/item_dashboard_tile" />`

2. `app/src/main/java/com/tkbiswas/pilesclinic/native/DashboardActivity.kt`
   - Wired that tile:
     `setupTile(binding.tilePrint, "🖨️", "Print") { startActivity(Intent(this, com.tkbiswas.pilesclinic.print.PrintCenterActivity::class.java)) }`

## What was NOT touched
- No change to Print logic, PDF building, layouts, or any other screen.
- No change to workflow, calculations, database, or the WebView.
- PrintCenterActivity is self-contained (needs no Intent extras), so opening
  it directly from the tile is safe.

## Still pending (unchanged by this fix)
- APK/AAB must still be built in Android Studio.
- Real device test still required.
