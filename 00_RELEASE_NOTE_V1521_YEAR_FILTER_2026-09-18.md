# V1521 — Year Filter / History Navigation

Date: 18.09.2026

Scope only: historical/list/report navigation. Existing business, payment, RMP commission formula, restore/delete permission, live/pending workflow and Supabase financial rules are not changed.

## Android + Web parity
- Draft detail lists: My Enquiry, Unexpected Time Calls, Enquiry Reject, Visit Reject, Running Patient, Incomplete Patient, Complete Patient, Return Visit, Refunded — top-right Year menu. Existing Draft Home All / This Month / Custom Date stays unchanged; explicit Year/All Years temporarily overrides it only for the open detail list.
- Empty Draft bucket now opens so an older Year can still be selected.
- Collection History: Year / All Years. Monthly Collection remains month-driven.
- RMP Commission Sheet: Year from existing top-right menu; Month then stays inside the selected Year. Commission calculation is unchanged.
- Trash Bin: Deleted Year / All Years. Restore/Delete rules are unchanged.
- Work Notebook → My Reports: Year / All Years.

## UI
- Secondary history controls are kept in compact top-right ⋮ menus.
- Selected Year is shown where practical without changing existing card layouts.

## Verification
- Web app.js: node --check PASS.
- Web notebook.js: node --check PASS.
- Modified Android layout XML: parse PASS; all 68 layout XML files parse PASS.
- Kotlin parser pass: no syntax-like compiler errors detected with kotlinc; full Android Gradle build could not run in this environment because Gradle 8.5 wrapper download is blocked by network/DNS.
- Changed-file audit performed against V1520; only Year-filter/version/release-note files are intended to differ.
