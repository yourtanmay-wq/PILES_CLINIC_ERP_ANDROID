# V1653 — Treatment Progress / Report Card safety

Date: 2026-09-21

## Verified scope
- V1652 My Enquiry → Call → post-call Remark popup fix retained.
- Chamber Treatment Progress is successful only after the Follow-up write and Report Card `payments.progress` write are both confirmed from cloud.
- No-payment Progress holder ID is unique by patient identity + Chamber Date, so two patients sharing one mobile cannot overwrite each other.
- Same-mobile patient fallback is identity-safe in Android Timeline/Report Card and Web Report Card fallback.
- Android Report Card screen and Print/PDF use the same visit rule. Web Report Card uses the matching rule.
- Web Chamber Register direct Print / Share PDF now uses the same mandatory Treatment Progress guard as Chamber Close; it can no longer close/print an open chamber with missing Progress. Already-closed historical registers remain printable.
- From 2026-08-22 onward, a generic treatment payment by itself does not create a VISIT row; real attendance / Chamber payment / clinical Progress is required. Hidden payment-only dates still affect Paid/Due correctly.
- Old history before 2026-08-22 keeps the legacy visit display rule to avoid deleting historical rows.

## Safety boundaries
- No payment amount, mode, refund arithmetic, Partner Share, Income–Expense, RMP commission, or database schema/data was changed.
- No guessed historical Treatment Progress was written to live data. Existing same-day evidence is only read as fallback.
- Full Android Gradle compile is recorded separately; source/web/ZIP guards must pass before delivery.
