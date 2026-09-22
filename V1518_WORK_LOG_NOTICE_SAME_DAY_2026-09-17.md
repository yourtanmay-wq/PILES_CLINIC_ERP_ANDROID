# V1518 — Notice Board same-day rule

Date: 17/09/2026

## User rule
Ordinary Notice Board messages must remain visible only on their own date. After midnight, yesterday’s ordinary messages must not be shown.

## Minimal change
- Android: all ordinary notices, including New Enquiry / New Registration / Advance auto notices, require `row.date == today`.
- Web: `briefingDateOk()` uses the same same-day rule.
- Master-approval requests (Refund / Delete / Reopen / Leave) are unchanged and may remain until a decision is made.
- No database schema, patient, payment, attendance, salary, tracking, or leave-count rule changed.

## Leave verification
- The observed Leave request was not caused by the monthly-5th-leave threshold; the existing Chamber-day / conflict approval paths remain unchanged.

## Version
- versionCode: 1518
- versionName: 15.18
