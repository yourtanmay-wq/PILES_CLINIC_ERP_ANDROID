# V445 — Follow-up Rejected Duplicate Resurrection Fix

**Date/Time:** 19.08.2026 · 11:06 AM IST  
**Base:** V444 / 4.44 → **V445 / 4.45**

## Live problem
Owner's Follow-up screenshot showed old Enquiry numbers again even though they had been rejected long ago.

## Verified root cause
The active Follow-up query excluded the Cancelled row by its own ID, but an older duplicate row for the **same mobile + Inquiry stage** could still remain `Active`. Because the live list judged those rows independently, that stale Active sibling could reappear.

## Safe fix
- Android: build a terminal-mobile guard from already-loaded cloud terminal Inquiry rows, the already-loaded enquiry status, and this phone's local rejected rows. If a mobile has a terminal Inquiry sibling (`Cancelled/Incomplete/Rejected/Closed`), no Active duplicate for that mobile is shown in the live Enquiry Follow-up list.
- Web: same mobile-level guard in `isInquiryVisibleRow`; the terminal-mobile set is built once per render, not once per card. Today Pending also uses the same guard.
- Genuine Restore remains valid: existing Restore code reactivates **all** matching followup/enquiry rows. Once restored, no terminal sibling remains and the card is visible normally.
- No record is deleted. Payment, patient, call count, remark, branch, Doctor Queue and design are unchanged.

## Verification
- `node --check 03_NETLIFY_READY/app.js` — PASS.
- `python 00_GUARD/tk_guard.py` — all machine checks PASS, version V445 aligned.
- Targeted 4-case behavior test — PASS: Active-only visible; Active+Cancelled hidden; full Restore visible again; Patient-stage Cancel does not hide Inquiry.
- Android Gradle compile could not start because this environment cannot resolve `services.gradle.org` to download Gradle 8.5 (`UnknownHostException`). This is recorded honestly; no build-pass claim is made.

No SQL/table change is required.
