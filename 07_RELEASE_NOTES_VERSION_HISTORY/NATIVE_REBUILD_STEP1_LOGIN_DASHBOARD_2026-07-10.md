# Native Rebuild — Step 1: Login + Dashboard (2026-07-10)

## What this is
The first step of converting the app from a WebView-shell to a genuinely
native app, as requested. This step covers **only** Login and Dashboard.
Every other screen (Enquiry, Registration, Follow-up, Payment, Print, etc.)
is completely unchanged — still the same WebView-based screens as before,
reached from the new native Dashboard.

## What actually changed

### New native screens
- `native/LoginActivity.kt` + `res/layout/activity_login.xml` — real native
  Kotlin login screen (EditText/Button, not HTML).
- `native/DashboardActivity.kt` + `res/layout/activity_dashboard.xml` +
  `res/layout/item_dashboard_tile.xml` — real native dashboard with module
  tiles (Enquiry, Registration, Follow Up, Payment, Doctor Visit, Menu).
- `native/StaffDirectory.kt` — bundled copy of the same staff/master/doctor
  accounts and role passwords already in `config.js`, so native Login
  authenticates the exact same accounts.
- `native/CloudPasswordCheck.kt` — checks the live Supabase
  `usercredentials` table for a per-user password override (same priority
  order the WebView login already uses: cloud override first, then the
  bundled role default).
- `native/NativeSession.kt` — keeps the logged-in session in
  SharedPreferences (native equivalent of the WebView's
  `localStorage.rk_session`), and builds the handoff data described below.

### How Login connects to the rest of the app (not yet rebuilt)
Tapping any Dashboard tile opens the existing WebView (`MainActivity`)
directly on that screen, carrying the already-verified session — **staff
are not asked to log in a second time.** Two small, targeted additions
made this possible, nothing else in `app.js` was touched:
- `MainActivity.kt` now builds the WebView's start URL with
  `?nativeSession=<encoded session>&openModule=<enquiry|registration|
  followup|payment|doctorvisit>` when launched from the native Dashboard.
- `app.js`'s `boot()` now recognizes `?nativeSession=` (treats it exactly
  like an existing `localStorage` session) and `?openModule=` (opens that
  screen directly instead of the WebView's own Dashboard first).

### What did NOT change
- Every existing screen's own logic (Enquiry, Registration, Follow-up,
  Payment, Print, everything fixed earlier today) is untouched.
- The WebView's own Login screen and Dashboard still exist in `app.js` —
  they are simply skipped now when arriving from the native Login/Dashboard
  (a plain browser opening the Netlify site, for example, still uses them
  normally).

## Verified in this sandbox (no Android SDK/device available here)
- All 65 Kotlin files: brace/parenthesis/string balance check passed.
- All 29 XML files: parsed as well-formed XML (this pass caught and fixed a
  real bug — a stray `--` inside an `AndroidManifest.xml` comment, which
  would have failed the actual build).
- Every `binding.xxx` / `tile.xxx` reference in the new Kotlin files was
  cross-checked against the actual `android:id` values declared in the
  corresponding layout XML — all match.
- Every `@drawable/...` and `@color/...` reference in the new layouts was
  cross-checked against the actual drawable files and `colors.xml` entries
  — all exist.
- The `app.js` side (native-session handoff, deep-link) was tested for real
  with headless Chromium: a Bengali-script staff name round-trips correctly
  through the whole encode → URL → decode path, the deep-link correctly
  opens the Enquiry screen directly, and the full existing regression suite
  (mobile input, duplicate-mobile handling, branch visibility, full patient
  journey) still passes unchanged.

## NOT verified — needs your Android Studio + a real device
Exactly the same limitation as every native change made so far in this
project: no Android SDK, Gradle, emulator, or device exists in this
sandbox, so **the app has not actually been compiled or run**. Before
relying on this:
1. Open the project in Android Studio, let Gradle sync (View Binding was
   turned on for this step — `viewBinding = true` in `app/build.gradle.kts`
   — that alone can occasionally need a clean/rebuild the first time).
2. Fix anything the real compiler flags that this level of static checking
   couldn't catch (rare, but possible — e.g. an exact dependency version
   detail).
3. Run on a device/emulator: confirm Login accepts a real staff account,
   Dashboard shows correctly, tapping a tile opens the WebView already
   logged in (no second login prompt) and on the right screen, and that
   Logout on the native Dashboard correctly returns to native Login.

## Next steps (not started yet)
Step 2 onward will rebuild one more module at a time (starting with
Enquiry, as discussed) as a real native screen, switching its Dashboard
tile from opening the WebView to opening the new native screen — the same
pattern used for this step.
