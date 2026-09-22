# V364 Work Log — 2026-08-13 UTC

- Verified Chamber Patient ID tap reached the existing Patient box action, while Name's long-press listener retained the normal tap instead of passing it to the box.
- Arrived rows: tapping Name, Patient ID, or empty space in the Patient box now opens the same existing two choices: Patient Details and Report Card.
- Mobile normal tap remains Call and was not changed.
- Long-press Name copies Name; Mobile copies Mobile; Patient ID copies Patient ID; empty Patient-box space copies Name + Mobile + Patient ID together.
- Waiting/Expected rows: Name and Patient ID normal taps explicitly open the same two-choice menu; their existing row behavior remains intact.
- No patient data, Chamber totals, arrival status, treatment, fee, cash, online payment, Close Chamber, design, size, or navigation destination changed.
- Supabase Free Plan: no database, storage, SQL, network, or paid-feature impact.
- Android version 3.64 (versionCode 364); Web cache/version stamp v364.
