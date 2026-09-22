# Future Update Guide

Practical notes for whoever maintains this codebase next, based on what
this session learned about its actual structure.

## 1. The app has two layers — know which one you're editing
- **Active/real ERP**: `app/src/main/assets/www/` (`index.html`, `app.js`,
  `styles.css`, `config.js`) — a WebView-hosted JS single-page app. All
  business logic, all workflow, all UI live here. This is what runs when
  you open the app.
- **Native Kotlin layer**: Activities under `clinical/`, `print/`, `sync/`,
  `settings/`, plus the Room/Retrofit `data/` stack — built, but not
  launched from anywhere. Only `AndroidBridge.kt` (added this session) is
  actually wired in, for native print only.
- **Rule of thumb going forward**: if a change is about workflow, business
  logic, forms, or the ERP itself — it goes in `app.js`. If it's about a
  native-OS capability the WebView can't do itself (camera, notifications,
  share sheets, etc.) — extend `AndroidBridge.kt` and call it from `app.js`
  via `window.Android.yourNewMethod()`, following the same pattern as
  `printPage()`. Don't duplicate business logic into the native side.

## 2. `app.js` is intentionally minified/dense — a survival guide
- It's one file, ~4,000 lines, no build step, no bundler — edits are
  direct string/function replacements.
- Search by function name first (`grep -n "function xyz"`), not by feature
  guesswork — most things are named plainly once found.
- Watch for **duplicate function declarations** — this file has several
  historical "Vxxx layer" patches (V266 through V280+) that sometimes
  redefine the same function name later in the file. The LAST declaration
  in source order wins at runtime. Before editing a function, grep for
  every occurrence of its name to make sure you're editing the one that's
  actually active, not a dead earlier version.
- Watch for **dead code that looks live**: this session found two examples
  (a length-check that could never pass, and calls to two functions that
  don't exist anywhere) that silently did nothing for an unknown length of
  time before being caught. When adding a new guard/condition, write a
  quick Playwright/browser test for it rather than trusting a code read
  alone — this session repeatedly found real bugs that were invisible from
  reading the code but obvious the moment it was actually run.
- Internal stage naming is **not** what's shown to staff: `stageLabel()` /
  `stageName()` translate internal values (`'Inquiry'`, `'Patient'`,
  `'Treatment'`) to what's displayed (`'Enquiry'`, `'Visit'`, `'Patient'`).
  Yes, internal `'Patient'` means the *Visit* stage. Always check
  `stageLabel()` before assuming what a stage string means.

## 3. Testing pattern that worked well this session
For any future change to `app.js`/`styles.css`, a lightweight offline test
harness (no login, no network needed) can catch real bugs before a device
test:
```html
<!-- test.html: loads the real app.js/styles.css unmodified -->
<link rel="stylesheet" href="styles.css">
<script src="config.js"></script>
<script src="app.js"></script>
<div id="app"></div><div id="modalRoot"></div><div id="toast"></div>
<script>
window.addEventListener('load', () => setTimeout(() => {
  user = { mobile: '...', name: 'TEST', branch: 'All', role: 'master' };
  enquiryForm(); // or registration(), etc.
}, 700));
</script>
```
Then drive it with Playwright (`chromium.launch()` + `page.goto('file://...')`)
to click, type, and assert on real DOM state — this is how every bug in
this session was actually found, not by reading code alone.

## 4. Data safety
- `isSeededRecord()` (in `app.js`) silently removes rows matching its word
  list on every `load()`. If you ever add words to that list, use exact
  match (`f.trim()===w`), never substring `.includes()` — see this
  session's Final Stabilization report for why that caused real data loss.
- All saves are offline-first (`localStorage` first, cloud sync is
  best-effort background). Don't assume a successful `save()` means the
  cloud has it — check `markPendingCloud`/`clearPendingCloud` state if you
  need that guarantee.

## 5. Before your next release
- Confirm `gradlew`/`gradle-wrapper.jar` still exist (this session found
  them missing from the very first upload — if they ever go missing again,
  Android Studio can regenerate them).
- Re-run the test scripts under `test-evidence/` in this package after any
  `app.js` change touching forms, cards, or duplicate-checking — they're
  fast (seconds) and catch regressions in exactly the areas that had real
  bugs this session.
