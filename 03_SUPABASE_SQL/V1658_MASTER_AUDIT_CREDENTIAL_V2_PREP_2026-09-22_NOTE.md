# V1658 Master Audit — Credential V2 Preparation

Applied live on 22.09.2026 with TK approval as an additive compatibility step.

Functions added:
- `public.tk_credential_verify_v2(text,text)` — returns only OK/WRONG/NO_CUSTOM; anon/authenticated execute.
- `public.tk_password_center_list_v2()` — authenticated only and checks `hr.is_master()`.
- `public.tk_password_center_set_v2(...)` — authenticated only and checks `hr.is_master()`.

No RLS/GRANT on `public.usercredentials` was changed in this step. No plaintext password was removed. Existing paths remain as compatibility fallback until V2 clients are proven in real use.
