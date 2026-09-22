# Native Rebuild — Step 4: Follow-up (2026-07-10)

## What this is
Follow-up is now a real native Kotlin screen (`FollowUpActivity`), reached
from the native Dashboard's "Follow Up" tile. Login/Dashboard (Step 1),
Enquiry (Step 2), and Registration (Step 3) are unchanged. Payment, Print,
and everything else are still reached via the WebView as before.

## What was built
- **Three tabs**, same as the WebView: Enquiry / Visit / Patient (internal
  stage values `Inquiry` / `Patient` / `Treatment` — same naming the
  WebView already uses, kept identical rather than renamed without being
  asked).
- **Today Due / Overdue / X Days Due badge** on the Enquiry tab, same
  color-coding (red for Overdue/Today, orange for future) fixed earlier
  this session in the WebView redesign.
- **Call** (opens the phone dialer) and **WhatsApp** (opens `wa.me/91...`)
  — same behavior as the WebView's `contact()`.
- **Update Remark** and **Set Next Follow-up Date** — the two most common
  day-to-day actions, both write straight to Supabase.
- **Branch-based visibility is correctly applied**: staff only see their
  own branch's records, Master sees all — verified by directly checking the
  filter logic (see below), not just assumed.

## A mistake I caught and corrected while writing this
My first draft of this screen's own code comments incorrectly said branch
filtering "is not yet applied here." That was wrong — the filter was
already written and wired correctly; the comment just hadn't been updated
to match. Since this is exactly the kind of thing you've asked me to be
careful and honest about, I want to be upfront that I caught this myself
while double-checking rather than assuming my own comment was accurate,
and corrected it before packaging this. The one real remaining gap is
noted below (creator override).

## Scoped limitations for this step (real, disclosed clearly)
- **"View All" full timeline**: not in this native screen yet. Remark
  editing covers the everyday case; the richer premium timeline view
  (built earlier in the WebView redesign) is a candidate for a later
  refinement, not blocking day-to-day use.
- **Patient tab's payment circle**: not shown here yet — Payment is its
  own not-yet-rebuilt module. This tab still correctly lists every
  Patient-stage (Treatment) record with name/mobile/branch/disease/remark.
- **Triple-tap Continue/Cancelled menu**: not yet in this native screen.
- **"Creator override" edge case**: in the WebView, a staff member keeps
  seeing a record they personally created even while viewing a different
  branch context. That one specific edge case is not replicated here yet
  — the main rule (staff sees only their branch, Master sees all) is.

## Verified in this sandbox
- All 77 Kotlin files: brace/parenthesis/string balance check passed.
- All 41 XML files: well-formed.
- Every `binding.xxx` reference in both `FollowUpActivity.kt` and
  `FollowUpAdapter.kt` cross-checked one-by-one against the actual
  `android:id`s in `activity_followup.xml` and `item_followup_card.xml`:
  100% match on both files.
- Every `@drawable/...`/`@color/...` used in the two new layouts
  cross-checked against what's defined: all exist.
- Full existing JS regression suite re-run — unaffected (no `app.js` code
  touched this step).

## NOT verified — same standing limitation as every step so far
Not compiled or run. Please Gradle-sync and test on a device:
1. From the native Dashboard, tap "Follow Up" — confirm the Enquiry tab
   loads with correct badges (test with a record whose Next Follow-up is
   today, one that's overdue, and one a few days ahead).
2. Switch to Visit and Patient tabs — confirm they load the right records.
3. Log in as a branch staff account (not Master) — confirm only that
   staff's own branch shows up, on all three tabs.
4. Tap Call/WhatsApp on a real record — confirm the dialer/WhatsApp opens
   with the right number.
5. Update a Remark and set a Next Follow-up date — confirm both changes
   are visible immediately in the WebView's own Follow-up screen too.

## Next step
Payment, as previously discussed — the last of the four modules named at
the very start of this rebuild plan.
