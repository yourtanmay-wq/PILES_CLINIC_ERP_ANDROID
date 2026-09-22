# Final Consolidated Test Report

All testing below was done with real Chromium (Playwright) driving the
actual, unmodified `app.js`/`styles.css`/`config.js`, or by static code
review where noted. No Android SDK/device/network was available in this
sandbox — anything requiring those is explicitly marked NOT VERIFIED.

## ✅ Verified with real functional tests

| Area | What was tested | Result |
|---|---|---|
| Print mechanism | `doPrintNow()` routes to native `Android.printPage()` bridge when present, falls back to `window.print()` otherwise | Working |
| tel:/WhatsApp/Maps/Meet links | `MainActivity.shouldOverrideUrlLoading` hands off non-`file://` navigation to system apps | Working |
| Card finishing (V280 rules) | Real render of `fuCard()` at 360/375/414px with stress-test data (long names/disease/branch) | No overlap, no cut text, 4 buttons one line, exactly 1 date/card |
| Enquiry/Visit/Patient triple-tap | Continue/Cancelled menu on all 3 stages | All 3 working (Patient stage was broken, fixed) |
| Reject Lists (Enquiry, Visit) | Filter + render via `draffHome()` | Working |
| Photo compression | `fileData()` resize/quality iteration | Working, untouched |
| Payment formula | `treatmentTotals()` cumulative paid/due/pct | Correct, untouched |
| Prescription/print content | `printHead()`/`printFoot()`/document templates | Content correct (structure only reviewed, not pixel-verified) |
| Global Search | De-duplication by mobile, branch visibility | Working |
| Mobile input (Enquiry + Registration) | Typing, Backspace, Delete-from-middle, paste, rapid typing, +91 badge | All correct (was broken, fixed) |
| Duplicate mobile rule | Live warning popup, Continue Registration button, merge-not-duplicate on re-registration, Follow-up/Global Search dedup | All correct (2 dead-code bugs found, fixed) |
| Full patient journey | Enquiry→Registration→Visit→Doctor→Prescription→Payment→Follow-up→Completed, all 8 stages, real functions | All 8 stages pass |
| Data integrity (`isSeededRecord`) | Real patient names vs actual demo markers | Critical bug found & fixed (was silently deleting real patients named e.g. "Sumita"/"Sobita") |
| Role/branch scoping | Master vs 2 branches' staff vs cross-branch creator override, with seeded multi-branch data | All 4 scenarios correct |

## ⚠️ Reviewed statically only (code-level, not executed)
- Native Room/Retrofit sync stack — exists, internally consistent, but
  unreachable from any UI (confirmed architecturally, not exercised).
- Individual print document layouts (Prescription/Diet Chart/Blood
  Test/Payment Receipt/Registration Slip) — template structure read and
  confirmed complete; actual rendered PDF/print output not visually
  inspected.

## ❌ Not verified — requires your Android Studio + device + live Supabase project
- Gradle Sync / actual compilation
- APK/AAB build
- Any real-device UI interaction (touch, real Android WebView engine, real
  Gboard/IME behavior)
- Supabase Login, Save, Sync, Pull against a live project (the SDK itself
  loads from a CDN at runtime — see `SUPABASE_SETUP_GUIDE_ACTIVE_SYSTEM.md`
  — and no network exists here to test that path at all)
- Real signed release build / Play Console upload flow

## Test scripts included in this package
Raw Playwright test scripts and their JSON output are included under
`test-evidence/` in this package so you (or anyone) can re-run or audit
exactly what was checked, rather than trust a summary alone.

## Bugs found and fixed this engagement (full list)
1. Print did nothing in-app (`window.print()` has no handler in a plain WebView) — fixed with a native print bridge.
2. Call/WhatsApp/Maps/Meet links were trapped inside the WebView — fixed navigation handoff.
3. Patient-stage cards were missing the triple-tap Continue/Cancelled menu that Enquiry/Visit stages had — fixed.
4. Mobile input self-corrupted on every keystroke once 10 digits were entered, breaking Backspace/Delete and producing repeating-digit garbage — fixed (root cause: a `+91`-prefixed value was being written back into the live input and re-parsed on the next keystroke).
5. The live duplicate-mobile-number warning popup could never fire (dead length check) — fixed.
6. The "Continue Registration" button in the duplicate popup called two functions that don't exist anywhere in the codebase and silently did nothing — fixed.
7. **Critical:** a demo-data cleanup filter matched by substring instead of exact value, silently deleting any real patient/enquiry/payment/follow-up record containing "sumita" or "sobita" anywhere in name/remarks/address — both common real first names in this clinic's service area. Fixed to exact-match only.

No other bugs were found in this pass. Items reviewed and found already
correct are listed as such above, not silently assumed.
