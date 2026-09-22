# V372 — Registration Form Approved Work Log

**Date/Time:** 14.08.2026, 09:51 AM IST  
**Owner approval:** Photo proof approved before implementation.

## Approved work completed

- Android and Web registration forms kept on the existing workflow and design.
- Mandatory Branch selector moved from the form body to the top-right area.
- Staff/Doctor automatic branch lock and Master/Field mandatory selection remain unchanged.
- Occupation and Age are now side by side.
- Duration of Problem now saves number + selected Days/Months/Years.
- Removed the old duplicate previous-treatment question.
- Previous Treatment History retains the owner-approved visible wording, except:
  - Previous Medication
  - Previous Ayurvedic/Herbal Treatment
- Optional Other Treatment History box placed below the treatment-history choices.
- Selected history plus optional typed history now both reach Doctor Check-up and print.
- Print/save wording mappings:
  - Massa Bara Hua → Prolapsed Lump
  - Previous Doctor Treatment → Previous Medical Treatment
  - Previous Operation History → Previous Surgical History
- Ref By → Referred By.
- Fees Amount → Fee Amount.

## Safety lock

- No database column, role permission, branch rule, payment rule, or Supabase setup changed.
- Existing IDs were preserved wherever possible; Branch uses the same Spinner and lock logic.
- No unrelated design or module was changed.

## Verification — 14.08.2026, 09:54 AM IST

- Web code syntax: passed.
- Android XML and all changed view connections: passed.
- Guard checks for brackets, XML, bindings, columns, version consistency and project completeness: passed.
- Full guard did not give final clearance because it found older, unrelated warnings in PatientMessage, DoctorMessage, Briefing and Chamber files. Those files were not changed in this work.
- Android build could not run in this environment because the required Gradle file was not already available and network download was blocked. No build-success claim has been made.
