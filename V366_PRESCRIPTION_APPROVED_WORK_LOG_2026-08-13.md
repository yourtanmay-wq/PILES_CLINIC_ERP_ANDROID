# V366 — Prescription approved work log — 13.08.2026

- Android and Web parity completed for the approved Prescription feature.
- Fixed Common prescription: six approved medicines, exact dose/when, 5 days.
- Compact choices: Sitz Bath fixed at 2 Times; Diet optional; four default complaint/history fields plus Choose More.
- A4 Prescription: left Complaint/History, medicine table shifted right, Advice below medicine table.
- Prescription print output is English only; blank Diet is omitted.
- No Supabase schema/table or unrelated workflow was changed.
- Verification: Web JavaScript syntax passed. Android source/call sites were checked; Gradle build could not run because Gradle 8.5 download is blocked by this environment's network.
