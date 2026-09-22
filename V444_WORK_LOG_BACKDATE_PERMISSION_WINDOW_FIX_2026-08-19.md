# V444 — Backdate Permission Window Fix

**Base:** V443 / 4.43 → **V444 / 4.44**

## Live issue
Master granted COB-4 permission for 07.08.2026–31.08.2026, but Staff still could not enter older payment dates.

## Verified root cause
`backdate_payment_grants.startDate/endDate` are the temporary **permission-active period**. V443 Android/Web incorrectly compared those dates with the historical payment date itself. Therefore a grant active in August 2026 failed for a 2024/2025 payment.

## Fix
- Android `BackdatePaymentGrant.isGrantedNow(...)`: checks whether **today** is within the grant period.
- Web `wlv1IsBackdateGranted(...)`: same rule.
- The historical transaction date remains passed/validated by the caller, but it is no longer incorrectly used as the permission period.
- Existing branch ownership, money validation, Master-request fallback, audit trail, and Revoke rules are unchanged.

## Expected example
On 19.08.2026, grant 07.08.2026–31.08.2026 ⇒ historical payment dated 18.03.2024 is allowed for that Staff, subject to existing branch/payment rules.

No SQL/table change is required.
