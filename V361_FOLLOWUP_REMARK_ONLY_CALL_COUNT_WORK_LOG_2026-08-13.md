# V361 Work Log — 2026-08-13 UTC

- Owner rule extended to the full Follow-up section: dialing or returning from the phone app does not count a call.
- Main Android Follow-up verified: its Remark field already starts blank and blocks empty Save before call counting.
- Web Follow-up verified: its New Remark field already starts blank and blocks empty Save before call counting.
- Android Follow-up Calendar defect fixed: the old Remark is no longer prefilled, and blank Save exits before history, Last Call, Next Follow-up prompt, or call-count updates.
- Android repository final safety gate also blocks every empty-Remark path from changing Last Call or Call Count.
- A newly written Remark still follows the existing safe save, history, daily de-duplication, 5-call guard, and mandatory Next Follow-up workflow.
- Supabase Free Plan: no new table, column, storage, SQL, function, or paid feature.
- No unrelated design, patient data, branch rule, payment, RMP, salary, photo, or login workflow changed.
- Android version 3.61 (versionCode 361); Web cache/version stamp v361.
