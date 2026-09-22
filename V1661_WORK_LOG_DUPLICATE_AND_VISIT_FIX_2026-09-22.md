# V1661 — 22 Sep 2026

## 1. Visit → Treatment stale-row fix
- Root cause: some treated patients had both an Active `Patient` follow-up row and an Active `Treatment` row.
- Production fix applied at database level.
- After a genuine positive treatment payment/advance, the old `Patient`/Visit follow-up is automatically closed.
- A guard also prevents stale Visit rows from becoming Active again after treatment has started.
- Historical correction was performed only for rows proven to have a positive treatment payment.
- Verification after backfill: stale Visit rows for treated patients = 0.
- Every historical row changed by the backfill was logged to `hr.visit_treatment_transition_audit`.

## 2. AJIJAR RAHAMAN duplicate correction
- Verified two patient rows represented the same person: same name, same branch, same registration/visit date and same bill amount.
- Canonical record retained: `COB-30052026-002` / `pat_7001794843`.
- Later duplicate record removed from live workflow: `COB-30052026-003` / `pat_9064422429`.
- The later duplicate Visit Fee row and duplicate follow-up rows were removed from live calculations/workflow only after a complete JSON snapshot was written to `hr.patient_merge_audit`.
- Canonical treatment/payment history was not changed.
- Duplicate data is recoverable from the merge audit and normal deletion/tombstone logs.

## 3. Version
- Android `versionCode`: 1661
- Android `versionName`: 16.61

## Safety
- No patient bill amount was changed.
- No canonical treatment payment was changed.
- No RMP commission logic was changed.
- No salary, attendance, enquiry, prescription or print logic was changed.
