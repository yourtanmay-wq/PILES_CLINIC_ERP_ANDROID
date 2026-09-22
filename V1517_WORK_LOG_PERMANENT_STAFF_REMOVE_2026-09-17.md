# V1517 — Permanent Staff Remove

Date: 17/09/2026

## User rule
Permanent Remove must hide the person from the app and remove Restore access, while historical Attendance/Salary/Extra Income records remain intact.

## Minimal change
- `hr.staff_profiles.active=false` remains the source of truth. No profile row is deleted.
- Staff Profile rendering now skips inactive staff/doctor/field profiles completely, including cached rows.
- The REMOVED/Restore section is no longer rendered.
- Remove confirmation now clearly says Permanent Remove and confirms historical records are retained.
- Existing login block, salary disable, and Income/Expense permit disable are unchanged.

## Version
- versionCode: 1517
- versionName: 15.17
