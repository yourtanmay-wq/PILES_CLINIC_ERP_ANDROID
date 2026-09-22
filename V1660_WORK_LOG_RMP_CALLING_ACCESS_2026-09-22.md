# V1660 — 22 Sep 2026

## Approved UI
- More screen: **Reports + Password** side-by-side.

## Safe cross-branch RMP Calling Access
- Master opens Staff Profile → **Manage RMP Calling Access**.
- Grant/revoke requires **confirmation + mandatory reason**.
- Permission is per staff + per target branch; home branch is not changed.
- Staff sees remote branch in Doctor Visit/RMP branch picker as **CALL ONLY**.
- Remote branch exposes only **Call + Doctor Call Remarks**.
- Hidden/blocked in remote mode: WhatsApp shortcut, View All, referral/patient details, commission/payment, edit, delete, add RMP.
- Remote list is fetched through authenticated `hr.rmp_call_directory()` with limited columns only.
- Remote remark save uses authenticated atomic `hr.rmp_log_cross_branch_call()`.
- Server re-checks permission at save time, so revoked access cannot keep saving from an already-open screen.
- Permission changes and call saves are separately audited in `hr.rmp_call_access_audit` and `hr.rmp_call_audit`.
- No existing patient/payment/finance/commission table or business rule was modified by the V1660 migration.

## Cloud
- Production Supabase migration applied: `v1660_rmp_calling_access` + hardening.
- New HR tables use RLS and direct anon/authenticated table access is revoked; app uses controlled authenticated RPCs.

## Build note
- Full Android Gradle build must be run in Android Studio if this environment cannot download missing Gradle dependencies.
