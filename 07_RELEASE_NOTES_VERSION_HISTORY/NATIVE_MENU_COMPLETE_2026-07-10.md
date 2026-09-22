# NATIVE — Menu features + 100% native (2026-07-10)

Added native: DraftActivity, ReportsActivity, TrashBinActivity (+repos/adapter),
MoreMenuActivity. SettingsActivity (existing native) now hosts Backup.
Dashboard "Menu" tile now opens MoreMenuActivity (native) instead of the WebView.
Added SupabaseClient.deleteById for Trash restore.

Result: every screen reachable in normal use is native. The WebView (MainActivity)
and app.js remain in the project but are no longer opened from the dashboard.

Role gates match menu(): Draft = master/staff; Reports/Backup/Trash = master only.

Scoped limitations: Reports shows headline totals + month comparison (per-staff /
conversion / branch breakdown can be added next). Draft "Incomplete" treats
follow-up rows as unpaid for the 60-day rule. Build in Android Studio and report
any errors.
