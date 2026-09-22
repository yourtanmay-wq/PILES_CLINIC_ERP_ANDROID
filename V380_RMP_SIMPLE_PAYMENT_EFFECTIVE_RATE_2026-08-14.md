# V380 — RMP Simple Payment + Effective Rate + Controlled Correction

- Ref. Paid -> select the referred patient -> enter payment amount/mode.
- Ref. Due comes from the verified commission summary.
- RMP Default changes apply from the change date; old treatment collections retain the old rate.
- A patient-specific commission remains independent of later RMP Default changes.
- Master can edit/delete any dated commission payment.
- Staff/Doctor can edit/delete only today's commission payment.
- Payment edit keeps its linked Expense synchronized; delete removes both and keeps the audit trail.
- Existing legacy Referral Income records and unrelated design/workflows are unchanged.
- Required Cloud migration: `04_SUPABASE_DATABASE_SETUP/V380_RMP_PAYMENT_EDIT_DELETE_RULE_2026-08-14.sql`.
- Verification: Web JavaScript syntax passed. Gradle compile could not start because Gradle 8.5 download is network-blocked in this environment.
