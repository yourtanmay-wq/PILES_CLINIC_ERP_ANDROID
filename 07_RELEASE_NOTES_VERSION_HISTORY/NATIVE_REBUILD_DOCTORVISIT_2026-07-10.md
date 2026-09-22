# Native Rebuild — Doctor Visit / RMP (2026-07-10)

## What this is
Doctor Visit / RMP is now a real native Kotlin screen
(`DoctorVisitActivity`), reached from the native Dashboard's "Doctor
Visit" tile. This is beyond the original five-module plan — continuing
per your instruction to keep going. Only "Menu" (Settings/other admin
screens) now still opens the WebView.

## What was built
- `DoctorVisitModel.kt` — matches `doctorDefaultNextDate()` (30-day
  default), `doctorDue()` (overdue-or-blank-date logic), and
  `saveVisit()`/`saveDoctorCall()`'s exact row shapes, including the
  call-history log (newest entry first, matching `hist.unshift(...)`).
- `DoctorVisitRepository.kt` — branch-scoped list fetch (same rule as
  Follow-up/Payment), duplicate-mobile check, add new contact, log a call.
- `DoctorVisitActivity.kt` + 2 new layouts — list (due-first sorted,
  matching `doctorSortList()`'s primary rule), Add Doctor dialog, Log Call
  dialog, Call/WhatsApp actions.

## Validation — matched to saveVisit()/saveDoctorCall()
- Add: Name, Mobile, Branch, Remarks all mandatory; Next Call Date can't
  be in the past; duplicate mobile is blocked (shows the existing
  doctor's name), not just warned.
- Log Call: Remarks mandatory; Next Call Date can't be in the past;
  correctly appends to (not replaces) the call history log.

## Scoped limitations for this module (disclosed clearly)
- Referral patient tracking and referral payment income is not shown —
  this screen covers the day-to-day call-tracking workflow.
- "This Month Called" and "Staff Call Summary" filtered views aren't
  included — only the full due-first sorted list.
- The cross-branch "needs approval" warning isn't shown — the branch
  filter already limits non-Master staff to their own branch's contacts,
  so this scenario shouldn't normally arise here.

## Extra-careful review applied (per your instruction)
Before packaging, all 4 new files were re-read line by line specifically
looking for the same class of nullable-comparison and leftover-code
mistakes found and fixed in the previous round (Follow-up/Payment). None
were found this time — but two accidental leftover code fragments (calling
`.let{}` on an unrelated value just to discard it, purely cosmetic
carelessness, not a bug) were caught and cleaned up before they were even
saved to disk, during first-draft writing rather than after.

## Verified in this sandbox
- All 85 Kotlin files: brace/parenthesis/string balance — clean.
- All 45 XML files: well-formed — clean.
- Every `binding.xxx`/`b.xxx` reference in the new Activity and Adapter
  cross-checked against actual layout IDs: 100% match on both files.
- Every `@drawable/...`/`@color/...` used in the two new layouts checked
  against what's defined: all exist (reused existing drawables/colors from
  Follow-up, no new ones needed).
- Full existing JS regression suite re-run — unaffected.

## NOT verified — same standing limitation as every module so far
Not compiled or run. This is now six native screens (Login, Dashboard,
Enquiry, Registration, Follow-up, Payment, Doctor Visit) built without a
single real compile check. The recommendation from the last update stands,
stronger now: an actual Android Studio Gradle sync is the one thing that
would meaningfully change confidence in all of this at once, more than any
further manual review can.
