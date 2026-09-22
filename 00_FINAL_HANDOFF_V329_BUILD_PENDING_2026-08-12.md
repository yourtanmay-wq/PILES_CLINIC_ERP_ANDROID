# PILES CLINIC APP V329 — COMPLETE PROJECT HANDOFF

## Identity

- Android versionCode: 329
- Android versionName: 3.29
- Status: FINAL_BUILD_PENDING

## Owner-approved work

- Five exact, owner-confirmed Demo mobiles were backed up, globally tombstoned and removed from live workflow/financial data.
- Live SQL proof returned zero remaining rows; Android proof showed PP/GST/7777777777 absent.
- Deleted source data can no longer be recreated by the Follow-up background self-heal path.
- Incomplete/Reject now selects the active follow-up linked to the current patient, not an older same-mobile record.

## Scope lock

- No design, label, button, Branch rule, role permission, normal Registration/Payment flow, RMP or Commission calculation was changed.
- No other mobile number was included in cleanup.
- Backup identity: `backup_demo_cleanup_20260812_five_numbers`.

## Honest build status

- Static project guard and XML/source checks must pass before packaging.
- Full Gradle build cannot be claimed in this environment because Gradle 8.5 download is unavailable. Android Studio must perform the final build.
