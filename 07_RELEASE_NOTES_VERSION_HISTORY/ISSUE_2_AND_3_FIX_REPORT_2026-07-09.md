# Issue #2 & Issue #3 Fix Report — 2026-07-09
Scope: ONLY the two remaining locked issues from `01_MASTER_LOCK_BOOK_SOURCE_OF_TRUTH/PILES_CLINIC_ERP_MASTER_LOCK_BOOK_V1.md`
that were still missing. Nothing else was touched.

---

## Issue #2 — Branch-wise Collection / Today Collection extra blank search bar

### Root cause
Every screen is rendered through one shared function, `page(title, body)` in
`app.js`. That function always auto-injects a generic global patient-search
capsule bar (`globalCapsuleWrap` — an empty rounded search input) above
whatever content the screen passes in, whenever the page title contains the
word "collection" or "payment". Every Collection-related screen
(`branchWiseCollectionPage`, `branchCollectionDetail`,
`collectionBranchBreakdown`, `paymentHome`, `collectionList`) has "Collection"
or "Payment" in its title, so each one got this extra blank search bar even
though these screens only show finance summaries/lists — nothing on them is
searched through that box. That is exactly the "extra blank search/input
area" described in the locked issue.

### Fix
- `page()` now accepts an optional third parameter, `hideSearch`. When true,
  the search bar is skipped for that render; every other existing call to
  `page()` (dashboard, enquiry, doctor, follow-up, etc.) is untouched and
  keeps the search bar exactly as before.
- The 5 Collection-only screens above now call `page(title, body, true)`.
- Two later in-file "wrapper" patches that re-decorate `page()` for other
  cosmetic rules (search-box placeholder cleanup, briefing card cleanup) were
  only forwarding `(title, body)` to the real function, silently dropping the
  new third argument. Both wrappers were updated to forward `hideSearch` too
  — otherwise the fix above would have been overridden and the bar would
  still have appeared. This was caught by the functional test below, not by
  code reading alone.

### What was NOT touched
Cash/Online/Total summary, the payment list, branch/staff breakdowns, and all
collection calculation logic are byte-identical — only whether the search bar
renders was changed. No other page lost its search bar (verified below).

### Verification (real functional test, headless Chromium via Playwright,
running the actual unmodified `app.js`/`styles.css`/`config.js`)
See `test-evidence/issue2_collection_search_bar_test.js` and its output
`issue2_collection_search_bar_results.json`. Seeded real payment records for
two branches, then rendered all 5 affected screens plus the Dashboard as a
control:

| Screen | Search bar gone? | Real data still shown? |
|---|---|---|
| Branch-wise Collection (Today) | ✅ | ✅ branch cash/online/total |
| Branch Collection Detail | ✅ | ✅ patient-level rows |
| Payment Collection (Today Collection hub) | ✅ | ✅ summary + list |
| Collection List – Cash | ✅ | ✅ correct filtered row |
| Dashboard (control, must be unaffected) | search bar **still present** ✅ | — |

Also re-ran the existing regression suite from the prior engagement
(`mobile_input_test.js`, `duplicate_mobile_test.js`, `role_branch_test.js`,
`patient_journey_test.js`) against the updated `app.js` — all results are
identical to the pre-fix baseline in this same package (`dup_test_results.json`,
`role_branch_results.json`, `final_journey_results.json`), confirming nothing
else regressed. Saved as `regression_after_fix_*.json`.

### Still needed before calling this fully done
Real Android device/emulator visual check (this was verified in headless
Chromium, not the Android WebView engine itself).

---

## Issue #3 — Global Touch/Click Stability (root cause)

### Root cause identified
`activity_main.xml` wraps the WebView in a `SwipeRefreshLayout` (for
pull-to-refresh). `MainActivity.kt` registered `setOnRefreshListener` but
never told `SwipeRefreshLayout` how to reliably tell whether the WebView can
still scroll up. This app re-renders the *entire* page via `innerHTML` on
every navigation (`page()` in `app.js`), which resets scroll to the top on
every screen change. Right after that reset, `SwipeRefreshLayout`'s default
internal check for "can this child still scroll up?" can read a stale value
from the WebView, so it starts intercepting the very next touch as a
possible pull-to-refresh gesture — swallowing or delaying the first tap,
most noticeably on buttons/cards near the top of a freshly drawn screen.
This matches several of the root-cause areas the lock book asked to check:
touch event blocking, state/render issue, performance lag.

### Fix
Added `swipeRefresh.setOnChildScrollUpCallback { _, _ -> webView.canScrollVertically(-1) }`
in `MainActivity.kt`. This is the officially documented mechanism for this
exact `SwipeRefreshLayout` + scrollable-child combination — it makes the
scroll-position check immediate and accurate instead of relying on the
default (unreliable-with-WebView) internal helper, so ordinary taps are no
longer intercepted as a possible refresh gesture.

### What was NOT touched
Pull-to-refresh itself still works exactly as before. No button, card, menu,
layout, or workflow code was touched — this is a one-line native Android
wiring fix, not a per-button workaround.

### Verification status — please read carefully
This fix could only be verified by static reasoning and Kotlin
bracket/syntax balance checking in this sandbox (no Android SDK, emulator,
or physical device is available here — same limitation noted throughout this
package's other test reports). This is the most plausible, standard,
low-risk root cause for the exact symptoms described in the lock book, and
carries no risk to any other feature since it only affects *when* the pull
gesture is armed. **A real-device test is still required to confirm the
touch-stability complaint is actually resolved** — if buttons still misbehave
after this on a real phone, the cause is something else and needs a fresh
investigation with real touch logs.

---

## Files changed in this pass (complete list)
1. `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/assets/www/app.js`
   (and the identical copy in `03_NETLIFY_READY/app.js`) — Issue #2 only,
   as described above.
2. `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/MainActivity.kt`
   — Issue #3 only, one `setOnChildScrollUpCallback` block added.

Nothing else in the package was modified.

---

## Additional fix — SecurityGuard.kt build error (found after user opened project in Android Studio)

### What Android Studio showed
Build failed with `e: kaptGenerateStubsDebugKotlin ... 1 error` and the file
itself showed **"Unclosed comment :35"** in `SecurityGuard.kt`.

### Real root cause
Inside the file's top doc-comment (`/** ... */`), one line of explanatory
text contained the words **"clinical/*Activity"** — meaning "the *Activity
screens in the clinical package" — but written with a `/` right before the
`*`. Kotlin (unlike plain Java) allows comments *inside* comments
(nested `/* */`), so the `/*` in that sentence was read as the start of a
second, inner comment. Only one closing `*/` existed (at the real end of the
comment block), so it closed the inner one and left the actual outer comment
open all the way to the end of the file — hence "Unclosed comment" and the
build failing.

This was a wording/typo issue in an explanatory comment, not a logic bug —
the actual code (functions, imports, types) was always correct, which is why
it wasn't caught by earlier static checks that focus on code logic rather
than incidental `/*` character sequences inside comment text.

### Fix
Reworded that one line so it no longer contains a literal `/*` sequence:
"clinical/*Activity" → "the clinical package's *Activity". No functional
code was touched.

### Verification
- Re-counted every `/*` / `*/` pair in the file: 3 opened, 3 closed
  (matching each other) — confirmed balanced.
- Searched the entire Android source tree for the same accidental pattern
  (a word character immediately followed by `/*` immediately followed by a
  letter) in every `.kt` file — no other occurrences found, so this was a
  one-off, not a repeating problem elsewhere in the project.

### File changed
`02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/security/SecurityGuard.kt`
— one line of comment text only.
