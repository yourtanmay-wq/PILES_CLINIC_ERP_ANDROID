# V367 — Verified correction — 13.08.2026

- Re-audited the V366 work after the owner's live report that the changes were not effective.
- Confirmed and corrected two real defects: Prescription did not reliably read the patient's latest saved Doctor Check-up, and Rx was not placed above the SL column.
- Android now reads only the current patient's latest Doctor Check-up before opening Prescription; a failed read keeps the patient-bound phone copy and never substitutes another patient's data.
- Disease, mobile and patient information are now passed through the Doctor Queue route too.
- Default Complaint/History choices always print; a genuinely missing value says `Not Recorded` instead of leaving a blank or inventing data.
- Web and Android both place Rx directly above SL; medicine table remains shifted right; Advice contains Sitz Bath and optional Diet.
- The approved six-medicine Common prescription and exact dose/when/5-day rules remain unchanged.
- No table, SQL, storage bucket, payment, registration, salary, RMP, Follow-up or unrelated design was changed.
