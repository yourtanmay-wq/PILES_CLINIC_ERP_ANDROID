# V446 — Android Build Error Fix

**Date/Time:** 19.08.2026 · 11:29 AM IST  
**Base:** V445 / 4.45 → **V446 / 4.46**

## Live problem
Android Studio screenshot showed APK build failure in `IncomeExpenseActivity.kt` line 1435:
- 2 × Type mismatch
- 1 × Too many arguments

## Verified root cause
`saveDaySummaryCache()` is defined as:
`saveDaySummaryCache(iso: String, branchKey: String, s: DaySummary)`

But the Today Summary card was calling it with four arguments:
`saveDaySummaryCache(iso, incTot, expTot, incTot - expTot)`

That call could not match the function signature.

## Safe fix
Only that call was corrected to pass the existing branch and the already-calculated Cash/Online values in the required `DaySummary` object:
`saveDaySummaryCache(iso, homeBranch, DaySummary(incCash, incOnline, expCash, expOnline))`

No calculation formula, database field, payment rule, design, Follow-up logic, Supabase rule, or Web application code was changed.

## Verification
- Both `saveDaySummaryCache()` call sites now match the 3-argument function signature.
- Isolated Kotlin type-check for the exact corrected signature: PASS.
- `python3 00_GUARD/tk_guard.py`: PASS (all machine checks, V446 aligned).
- Actual Android Gradle compile could not start in this environment because Gradle 8.5 cannot be downloaded (`services.gradle.org` DNS/Internet unavailable). Therefore no false full-build-pass claim is made.

No SQL change is required.
