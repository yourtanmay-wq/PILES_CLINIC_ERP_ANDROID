# V365 Work Log — 2026-08-13 UTC

- Audited every V355–V364 session change across Android and Web.
- Already matched: Login, Staff Profile/Photo/Salary, Master branch RMP count, RMP Remarks/counting, and Follow-up Remarks/counting.
- Fixed Web Chamber parity: Name, Patient ID, and Patient box open Patient Details / Report Card; Mobile still calls; long-press/right-click copies the relevant value.
- Fixed Web Referral parity: second tap prompts and third tap opens Edit; single tap keeps Report Card when a linked mobile exists.
- A legacy Referral id is added only when exactly one entry matches date + amount + status + patient; ambiguous data is untouched.
- Master direct edit and Staff-to-Master approval remain unchanged.
- No new SQL, table, column, storage, or paid Supabase feature; no unrelated workflow changed.
- Android version 3.65 (versionCode 365); Web cache/version stamp v365.
