# V248 LOCK NOTE

- Date: 02.08.2026
- Exact time: 05:35:09 PM IST (12:05:09 UTC)
- Base: V247 correction source; release renamed because V247 had already been recorded.
- Exact owner-approved change: Work Notebook and the other new private modules
  do not show a second Staff Code/password screen. They reuse the currently
  logged-in app identity and authenticate silently.
- Required database step: run `V248_AUTO_MODULE_LOGIN_ONE_RUN.sql` once.
- Unrelated Patient, Payment, Refund, Follow-up, design, permissions and
  workflows remain unchanged.

## Verification at 05:35 PM IST

- TK Guard machine checks: PASS.
- Web JavaScript syntax: PASS.
- Android XML/static Kotlin checks: PASS.
- ZIP integrity and SHA-256 manifest: must pass before delivery.
- Honest limitation: APK/device test is for the owner after Android Studio build.
