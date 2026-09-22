# V1520 — RMP Financial + Leave Notice Hardening

Date: 17-09-2026

## RMP financial safety
- Patient branch and RMP branch must match. Android, Web and Database all enforce this.
- Removed the old Web fallback that could choose the first same-name/mobile RMP from another branch.
- Removed the Android/Web confirmation override that allowed saving commission on a mismatched branch.
- RMP patient lookup now prefers the open RMP card's branch.
- Old-date commission-change requests now use branch-specific default priority on Android.
- Database backfill/matching is branch-aware and table triggers block cross-branch commission rows or later branch changes.

## Leave notice safety
- Web uses a deterministic Leave Notice ID: the same staff/date/branch updates the same notice instead of creating another row.
- Database deduplicates old-client/retry inserts and automatically closes pending Leave Notices after approval/rejection.
- A partial unique index guarantees only one active Leave Notice even under simultaneous requests.

## Live data correction completed before this release
- Cross-branch RMP commission rows were corrected and verified to zero mismatch.
- Duplicate Leave Notices were soft-closed (history retained); only real pending notice remains active.

## Scope
- No patient/payment/treatment data is deleted by V1520 application code.
- No design/theme change.
