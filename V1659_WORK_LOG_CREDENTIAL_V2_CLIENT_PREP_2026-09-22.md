# V1659 Work Log — Credential V2 Client Preparation

Date: 22.09.2026
Scope approved by TK: prepare the modified Web/Android build for real-use testing without risking existing working flows.

## Runtime changes retained
- Web Login: safe pre-V265 login path restored; V265 double-tap button lock retained.
- Android + Web Login: V2 credential verification first; old proven path remains fallback if V2 is unavailable.
- Android + Web Money Handover: V2 verification first; old proven path remains fallback.
- Android + Web Password Center: Master-authenticated V2 list/set first; old proven path remains fallback.
- Web Password Center writes a PBKDF2-SHA256 hash in the same format used by Android.

## Live DB additive preparation already applied
- `public.tk_credential_verify_v2(text,text)`
- `public.tk_password_center_list_v2()`
- `public.tk_password_center_set_v2(...)`

The set RPC was corrected before real use so it updates an existing credential row by normalized mobile, inserts only when none exists, and refuses duplicate-mobile state. This avoids duplicate rows for legacy credential IDs.

## Verification before handoff
- Login targeted behavior: 9/9 PASS.
- Password Center + Money Handover targeted safety: 12/12 PASS.
- All Web JS syntax checks: PASS.
- Master-only V2 list/set rollback test: PASS; no test row persisted.
- Non-Master V2 Password Center access: blocked as intended.
- Duplicate credential mobile groups after rollback tests: 0.
- Full Android Gradle compile: NOT CLAIMED because Gradle distribution is unavailable in this environment and network download is blocked.

## Intentionally NOT done yet
- `usercredentials` RLS/GRANT lock is not enabled yet.
- Plaintext passwords are not removed yet.
- Old credential table/RPC compatibility paths are not removed yet.

These remain pending until V1659 is installed/deployed and Login + Password Center + Money Handover are checked in real use.
