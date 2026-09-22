# V1657 — Partner Settlement Safety

- Android + Web: Settlement first opens a review screen; no money action happens on first tap.
- Remarks / Note is mandatory.
- Explicit confirmation checkbox is mandatory.
- Confirm button is disabled until both are provided.
- Double-tap guard while settlement is running.
- Settlement writes moved to `fin.partner_settle_atomic` RPC so all partner drawings + settlement records commit together or all roll back together.
- Server validates Master role, branch, partner, amount/direction and mandatory note.
- No patient/payment/income/expense calculations changed.
