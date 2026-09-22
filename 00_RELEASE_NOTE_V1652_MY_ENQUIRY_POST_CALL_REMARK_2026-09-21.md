# V1652 — My Enquiry Post-Call Remark Popup

Date: 2026-09-21

## Verified root cause
`DraftListActivity` rendered **My Enquiry (All Branch)** with `FollowUpAdapter`, but its Call callback only executed `dial(item.mobile)`. Unlike `FollowUpActivity`, it never registered the called row in `PendingRemarkStore`, and `DraftListActivity.onResume()` never checked for a post-call remark. Therefore there was no trigger that could show the Remark popup after returning from the phone dialer.

## Change
- Only `bucketKey == "received"` / My Enquiry staff calls register a `PendingRemarkStore` row with source `my_enquiry`.
- On return to My Enquiry, only that source is considered, so old/other Follow-up pending reminders are not mixed into this screen.
- The existing `CallRemarkActivity` is reused; no new remark persistence logic was invented.
- Before opening it, the real Follow-up row id is resolved with the existing `ensureFollowUpRowId()` safety path.
- Saving the Call Remark removes this local My Enquiry pending reminder; “Not now” keeps it and snoozes it.
- Cross-branch assignment is not blocked: a JPE staff member calling their own Falakata enquiry receives the same popup.

## Not touched
Payment, refund, partner share, income/expense, enquiry assignment, branch permission, WhatsApp, View/Timeline, Chamber logic, and database schema.
