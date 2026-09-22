# Native Rebuild — Step 5: Payment (2026-07-10)

## What this is
Payment is now a real native Kotlin screen (`PaymentActivity`), reached
from the native Dashboard's "Payment" tile. This completes the five
modules named at the start of this rebuild: Login/Dashboard, Enquiry,
Registration, Follow-up, and now Payment. Doctor Visit, Print, and Menu
(Settings/other admin screens) are still reached via the WebView — they
were never part of this five-step plan; whether to continue rebuilding
them natively is a separate decision for you to make.

## What was built
- `PaymentModel.kt` — matches `collectionPaymentLabel()` (Visit Fee /
  Medicine Payment / Treatment Payment classification) and
  `ordinalPaymentLabel()` ("Advance", "2nd Payment", "3rd Payment"...)
  exactly.
- `PaymentRepository.kt` — Today's Collection (payments + medicine/product
  payments combined, same as `collectionRows()`), patient lookup by mobile
  with correct bill/paid totals, and the treatment-payment save logic.
- `PaymentActivity.kt` + `activity_payment.xml` + `item_collection_row.xml`
  — Today's Collection Summary (Total/Cash/UPI) and list, plus "Add
  Treatment Payment" (search → bill/paid/due info → amount → save).

## The 3-tap Bill protection — carried over correctly
The Total Bill field, once already set for a patient, is locked
(non-editable) and requires **exactly 3 taps within 1.4 seconds** to
unlock — the same protection added to the WebView earlier this session,
after you specifically asked for it there. This was re-verified by tracing
through the tap-counter logic by hand line-by-line before packaging, not
assumed to be correct just because it looked similar to the WebView
version.

## A mistake caught during this step, before it shipped
My first draft of `fetchTodayCollection()` didn't filter by branch at all
— it would have shown every branch's collection to every staff member,
which is exactly the kind of thing you've told me matters a lot. I caught
this myself while writing the "scoped limitations" section for this file
(the same habit that caught the Follow-up documentation mistake last
step), and fixed it before packaging — staff now only see their own
branch's Today's Collection, Master sees all, same rule as Follow-up.

## Scoped limitations for this step (real, disclosed clearly)
- **Medicine Sale Payment**: not started from this native screen yet —
  Today's Collection still correctly *includes* any medicine payments made
  elsewhere (WebView), this screen just doesn't let you begin a new one.
- **Visit Fee payments**: not started here either — Registration (Step 3)
  already handles that at the point of registering a patient, so it's not
  a missing capability, just not duplicated in this screen too.
- **Editing a past payment entry**: the WebView's same-day-correction flow
  (`editPaymentEntry()`) is not in this native screen — only adding new
  payments.

## Verified in this sandbox
- All 81 Kotlin files: brace/parenthesis/string balance check passed.
- All 43 XML files: well-formed (also cleaned up two non-root namespace
  declarations for consistency, both already valid but not matching the
  convention used in every other file this session).
- Every `binding.xxx` reference in `PaymentActivity.kt` and
  `CollectionAdapter.kt` cross-checked against the actual layout IDs: 100%
  match on both.
- A leftover, meaningless code fragment in `CollectionAdapter.kt`'s amount
  formatting (harmless but sloppy) was caught and cleaned up before
  packaging.
- Full existing JS regression suite re-run — unaffected.

## NOT verified — same standing limitation as every step so far
Not compiled or run. Please Gradle-sync and test on a device:
1. From the native Dashboard, tap "Payment" — confirm Today's Collection
   summary numbers match what the WebView's own Payment screen shows for
   the same day.
2. Tap "Add Treatment Payment", search a mobile number with NO registered
   patient — confirm the correct "not found" message.
3. Search a real patient with no Bill set yet — confirm the Bill field is
   editable, save a first payment, confirm it's labeled "Advance".
4. Reopen Add Payment for the SAME patient — confirm the Bill field is now
   locked, tap it twice (confirm nothing unlocks), tap a 3rd time (confirm
   it unlocks with the "unlocked" message), and confirm the new payment is
   labeled "2nd Payment".
5. Log in as branch staff (not Master) — confirm Today's Collection only
   shows that staff's branch.

## Where this leaves the rebuild plan
The five originally-discussed modules are now all real native screens,
each with its own honestly-documented scope and remaining gaps (see each
step's own release note in this folder). None of this has been compiled
or run on a real device or emulator at any point in this rebuild — that
remains the essential next action before relying on any of it, and is
worth doing now, across all five steps together, rather than continuing
to add more native screens on top of an unverified foundation.
