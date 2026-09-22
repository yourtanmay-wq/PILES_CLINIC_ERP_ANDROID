# Native Rebuild — Step 2: Enquiry (2026-07-10)

## What this is
The second step of the native rebuild. Enquiry is now a real native Kotlin
screen (`EnquiryActivity`), reached from the native Dashboard's "Enquiry"
tile. Every other module (Registration, Follow-up, Payment, Print, etc.)
is still unchanged, still reached via the WebView as before.

## What was built
- `EnquiryModel.kt` — builds the exact same "enquiries" and "followups" row
  shape the WebView's `saveEnq()`/`ensureFollow()` already produce, so a
  native-saved enquiry is fully interchangeable with a WebView-saved one —
  same Follow-up list, same Global Search, no special casing anywhere else.
- `SupabaseClient.kt` — shared REST helper (upsert a row, look up a row by
  mobile) reused by this and future native screens.
- `EnquiryRepository.kt` — duplicate-mobile check (same two tables the
  WebView checks: enquiries + patients), save, and a simple local
  pending-queue fallback if the device is offline when Save is tapped
  (retried automatically next time the screen opens).
- `EnquiryActivity.kt` + `activity_enquiry.xml` — the form itself: Mobile,
  Date, Branch, Name, Disease, Address, Remarks, Call Timing, Next
  Follow-up Date.

## Validation — matched exactly to the WebView's saveEnq()
| Rule | Native | WebView |
|---|---|---|
| Mobile must be a valid 10-digit number | ✅ | ✅ |
| Branch mandatory | ✅ | ✅ |
| Remarks mandatory | ✅ | ✅ |
| Name — **not** mandatory | ✅ (matches, even though this looks like it should be required — the WebView genuinely allows saving with no name, so this was kept identical rather than "fixed" without being asked) | ✅ |
| Date cannot be in the future | ✅ (DatePicker's max date is today) | ✅ |
| Next Follow-up cannot be a past date | ✅ (DatePicker's min date is today) | ✅ |

## Duplicate-number handling — scoped for this step
Tapping Save checks both the `enquiries` and `patients` tables for the
same mobile number, exactly like the WebView does, and shows the existing
record's name/branch/status if found.

**Scoped limitation, on purpose:** the WebView's duplicate popup can jump
straight into Registration or Follow-up for that existing record — those
screens don't have a native version yet, so the native popup currently
only offers "Save Anyway" / "Cancel". Once Registration and Follow-up get
their own native rebuild steps, this popup will naturally gain those same
options.

## Offline behavior
If Save is tapped with no internet, the enquiry is queued locally
(SharedPreferences) instead of being lost, and the app tells the user
"Saved — will sync when online" rather than showing an error. The queue is
retried automatically the next time the Enquiry screen is opened. This is
a deliberately simple fallback for this step, not the full offline-sync
architecture — see FUTURE_UPDATE_GUIDE.md's original notes on the
disconnected Phase 5 Room/WorkManager stack for the more complete approach
if that level of offline robustness becomes a priority later.

## Verified in this sandbox
- All 69 Kotlin files: brace/parenthesis/string balance check passed.
- All 31 XML files: well-formed (caught and fixed the same "double-hyphen
  inside an XML comment" mistake twice more while writing this step —
  now double-checked with an explicit sweep across every new file).
- Every `binding.xxx` reference in `EnquiryActivity.kt` cross-checked
  against `activity_enquiry.xml`'s actual `android:id`s — all match.
- Every `@style/...`, `@drawable/...`, `@color/...` reference in the new
  layout and style files cross-checked against what's actually defined —
  all exist.
- Full existing JS regression suite re-run — unaffected (this step touched
  no `app.js` code at all, only new Kotlin/XML files).

## NOT verified — same standing limitation as every step so far
No Android SDK/Gradle/emulator/device in this sandbox — **not compiled or
run**. Please Gradle-sync in Android Studio and test on a device:
1. From the native Dashboard, tap "Enquiry".
2. Fill in a real staff mobile number that's known to already exist in the
   system (e.g. a demo patient's number) — confirm the duplicate warning
   appears with the correct existing name/branch.
3. Fill in a genuinely new mobile number, save, and confirm it appears in
   the WebView's Follow-up → Enquiry tab exactly like a normal entry.
4. Turn off WiFi/data, save an enquiry, confirm it says "will sync when
   online" instead of failing; turn connectivity back on, reopen Enquiry,
   confirm it silently uploads (check Supabase table or the WebView list).

## Next step
Registration, as previously discussed — same pattern: real native form,
same validation/duplicate rules as the WebView's `registration()`/
`savePatient()`, Dashboard's "Registration" tile switched over once built.
