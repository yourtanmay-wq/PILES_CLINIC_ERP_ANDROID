# V246 — CHANGED FILES (vs V245_FINAL_2 base) — additive only

## Changed (minimal, additive)
| File | Change |
|------|--------|
| `02_ANDROID_SOURCE_CODE/.../app/build.gradle.kts` | versionCode 245→246, versionName 2.45→2.46 |
| `04_SUPABASE_DATABASE_SETUP/V246_ONE_RUN_SETUP_2026-08-02.sql` | **NEW** single-run SQL with the security fix (replaces the V245 module SQL) |
| `04_SUPABASE_DATABASE_SETUP/V246_SQL_RUN_STEPS.md` | **NEW** simple run steps |
| `00_LOCK_NOTE_SESSION_2026-08-02_V246.md` | **NEW** V246 lock note |
| `V246_CHANGED_FILES.md`, `V246_LIVE_TEST_CHECKLIST.md`, `V246_FILE_MANIFEST_SHA256.json` | **NEW** |

## Removed (superseded, my own V245-turn additions — NOT original project files)
- `04_SUPABASE_DATABASE_SETUP/V245_MODULES_HR_WN_FIN_2026-08-02.sql`  → replaced by the V246 single-run SQL
- `04_SUPABASE_DATABASE_SETUP/V245_SETUP_STEPS_STHAYI.md`  → replaced by V246 run steps
- `V245_CHANGED_FILES.md`, `V245_LIVE_TEST_CHECKLIST.md`  → replaced by V246 versions
- (`00_LOCK_NOTE_...V245.md` is KEPT as history; the staging native folder was already removed in V245_FINAL_2.)

## Unchanged from V245_FINAL_2 (still fully present + working)
- Android module source: `.../modules/ModuleAuth.kt · ModuleUi.kt · StaffProfileActivity.kt · WorkNotebookActivity.kt · IncomeExpenseActivity.kt`
- Android additive hooks: DashboardActivity.kt, MoreMenuActivity.kt, CallChooser.kt, AndroidManifest.xml
- Web modules: `03_NETLIFY_READY/module_core.js · profile.js · notebook.js · finance.js` + index.html/app.js hooks
- All existing Patient/Payment/Refund/Follow-up/Briefing/Login/Print/Sync/Search/Design/Dashboard/Guard/Rollback/.git — untouched.

## The security fix (what changed in the DB design)
- Deleted the unsafe `hr.map_identity()` (was callable by any authenticated user).
- `hr.app_identity` has **no write policy** for normal users (RLS blocks self-write).
- Added `hr.admin_set_identity()` which raises unless the caller is already Master.
- First Master is created only by the SQL (service role). No callable path grants Master.
- Proven on real Postgres (see Security Test Report in the lock note / delivery message).
