# V1526 — Report Card Visit-Day Permanent Fix

Verified from MANIK ROY live data on 18 Sep 2026.

- One real chamber visit date = one Report Card row.
- attendance_mark / progress-holder can anchor a no-payment visit, but PAID shows “—”, never ₹0.
- refund-only, chamber_expected and bill-edit events never create a visit row.
- Same-day refunds still affect the SUMMARY Paid/Due accounting, but are not shown as a negative visit payment.
- Chamber Treatment Progress saved in payments.progress is preserved even when the row is attendance_mark.
- If payments.progress is absent, the same-day source=treat Follow-up/Chamber note is used.
- Android screen + Android print + Web Report Card + Web text share use the same visit-day rule.
- No patient/payment/chamber rows were deleted or rewritten by this source change.
- Historical blank data is not invented. If an old date has no saved progress in any source, it remains “—”.
