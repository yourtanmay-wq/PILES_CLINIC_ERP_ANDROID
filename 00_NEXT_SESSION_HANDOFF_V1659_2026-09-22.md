# READ FIRST — NEXT SESSION HANDOFF — V1659 — 22.09.2026

এই ফাইলটি নতুন সেশনে সবার আগে পড়বেন। বিস্তারিত পূর্ণ ইতিহাস `00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`-এর একেবারে শেষে **“MASTER AUDIT SESSION HANDOFF (V1658 → V1659)”** অংশে আছে।

## Current baseline
- Project: `PILES_CLINIC_APP_V1659_FINAL`
- App version: V1659 / 16.59
- Original audited baseline: V1658 (kept untouched during work)
- Full Android Gradle compile: NOT PROVEN in prior session due network/dependency limitation
- Targeted Login tests: 9/9 PASS
- Targeted Password Center + Money Handover tests: 12/12 PASS
- Web JS syntax: PASS

## Live Supabase changes already done
1. Added transitional credential V2 RPCs (`tk_credential_verify_v2`, `tk_password_center_list_v2`, `tk_password_center_set_v2`).
2. Fixed V2 save to update existing legacy credential row by normalized mobile; duplicate groups verified 0; rollback tests passed.
3. Revoked direct PUBLIC/anon/authenticated EXECUTE from four trigger-only SECURITY DEFINER functions; DB triggers preserved.
4. Enabled RLS on 24 archive/demo/backup tables only; no data deleted/updated. Public RLS-disabled warning 54 → 30.
5. Secured `tk_demo_suspects` view (`security_invoker=true`, anon/auth read revoked).
6. Added 2 missing FK indexes on empty `fin.rmp_advance_allocations`; unindexed-FK warning 2 → 0.
7. Guard-log tables `rk_blocked_lastcall` / `rk_blocked_dup_followup`: anon/auth SELECT/UPDATE/DELETE revoked; INSERT preserved.
8. Optimized `hr.app_identity` read policy with cached auth checks; before/after behavior Master=29 rows, non-Master=1 row; initPlan warning now 0.

## DO NOT do yet
- Do NOT final-lock `usercredentials` / remove plaintext until V1659 real-device/Web test passes.
- Do NOT blanket-enable RLS on live patients/payments/enquiries/followups until Web main data client is authenticated-ready.
- Do NOT revoke patient/payment RPCs (`tk_register_patient`, `tk_record_treatment_payment`) without caller mapping and replacement auth path.
- Do NOT rewrite Partner settlement finance formula without source-of-truth balance audit.

## Remaining priority
1. Real V1659 test: Login, Password Center, Money Handover, suspend/removed behavior, Web cache/version.
2. Password table final RLS/lock + staged plaintext removal.
3. Web authenticated data client, then table-by-table core RLS.
4. Secure patient/payment privileged RPCs.
5. Partner Settlement server fresh-balance + idempotency.
6. Partner Setup atomic/verified save Android+Web.
7. Withdraw/Return mandatory note + confirmation + duplicate protection.
8. Later: mutable search_path functions, permissive-policy cleanup, legacy no-PK classification, leaked-password protection review.

## Current advisor snapshot
- Security: RLS disabled in public = 30; anon SECURITY DEFINER executable = 6; authenticated SECURITY DEFINER executable = 9; mutable search_path = 26; leaked-password protection warning = 1.
- Performance: unindexed FK = 0; auth RLS initPlan = 0; multiple permissive policies = 90; no-PK info = 17.

## One-line state
**V1659 is a transition-safe credential build; low-risk live security cleanup is complete, but big password/core-RLS/RPC/Partner-finance changes are deliberately pending real V1659 testing.**
