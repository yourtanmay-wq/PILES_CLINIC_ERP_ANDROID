# V1654 — Cooch Behar Partner Share correction

Date: 21.09.2026

Approved rule:
- 01 Jan–31 Jul 2026: TK BISWAS 50%, Dr. K.H MANDAL 30%, J.H MANDAL 10%, GOKUL SARKAR 10%.
- From 01 Aug 2026: TK 40%, K.H 40%, J.H 10%, Gokul 10%.
- Jan–Jul J.H + Gokul shares are taken from K.H Mandal's original 50% share.

Scope:
- Cloud: fin.partner_pct_history only, Cooch Behar four partners.
- Android/Web calculation already uses partner_pct_history segment-by-segment, so no formula fork was added.
- Old V309 migration text/data seed corrected to prevent accidental future rollback.
- No collections, expenses, partner drawings, payments, patients, RMP, salary, attendance or other branch data changed.
