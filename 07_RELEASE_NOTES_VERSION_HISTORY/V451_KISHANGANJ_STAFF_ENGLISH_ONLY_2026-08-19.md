# V451 — Kishanganj Staff / Branch Number: English-only display

Date: 19.08.2026 18:23 IST

TK-approved scope:
- Android: Kishanganj `staff` role is now the No-Bengali branch rule, not a single mobile/code rule.
- `KNE-BRANCH` is covered automatically because it is a Kishanganj staff account.
- Web: same branch+role rule.
- Legacy `KNE-KISHAN5` mobile/code fallback retained for safety.
- Doctor/Master/Field and staff of other branches are unchanged.
- No database, RLS, Supabase read/write, payment, Reject, Loading, Print/WhatsApp, or patient-message data logic changed.
- No new Supabase call was added.

Release discipline:
- Version metadata advanced to V451 / 4.51.
- ZIP must not be sent until TK explicitly asks for the file.
