# Piles Clinic ERP

Clinic management system for TK Biswas Piles Clinic — Android app + web app (PWA)
on a shared Supabase backend.

Current version: **V690 / versionName 6.90** (imported from `PILES_CLINIC_APP_V690_FINAL_2.zip`).

## Read these first

| Order | File |
|---|---|
| 1 | [`00000_SOBAR_AGE_EITAI_PORUN.md`](00000_SOBAR_AGE_EITAI_PORUN.md) — সবার আগে এটাই পড়ুন |
| 2 | [`00_FIRST_OPEN_OWNER_RED_ALERT.md`](00_FIRST_OPEN_OWNER_RED_ALERT.md) — owner's red-alert rules |
| 3 | [`00_CLAUDE_STHAYEE_NOTE_PACKAGING_RULES.md`](00_CLAUDE_STHAYEE_NOTE_PACKAGING_RULES.md) — permanent packaging / working rules |
| 4 | [`01_MASTER_LOCK_BOOK_SOURCE_OF_TRUTH/`](01_MASTER_LOCK_BOOK_SOURCE_OF_TRUTH/) — the source of truth |
| 5 | [`00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md`](00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md) — the work ledger (খাতা) |

## Layout

```
00_AUDIT/ 00_GUARD/ 00_READ_ME_FIRST/ 00_SQL/   audits, guard scripts, first-read docs
01_MASTER_LOCK_BOOK_SOURCE_OF_TRUTH/            locked spec — source of truth
02_ANDROID_SOURCE_CODE/PilesClinicApp/          Android Studio opens THIS folder
03_NETLIFY_READY/                               web app / PWA — deploy this folder to Netlify
03_SUPABASE_SQL/ 04_SUPABASE_DATABASE_SETUP/    database schema + migrations
05_APK_AAB_BUILD_NOTES/                         build & release notes
06_TEST_CHECKLISTS/ 11_V223_TESTS/              test checklists
07_RELEASE_NOTES_VERSION_HISTORY/               release notes
08_ASSETS_BACKUP/                               logos and icons
10_FUTURE_PLANS/                                not-yet-built features
ROLLBACK_V448/                                  kept-back copies for rollback
```

The folder structure above is **locked** — see the packaging rules note. Do not move
`PilesClinicApp` to the top level, and do not rename folders.

## Build / deploy

- **Android:** open `02_ANDROID_SOURCE_CODE/PilesClinicApp/` in Android Studio.
  Copy `local.properties.example` → `local.properties` and fill in your SDK path
  (and release-signing values, when producing a signed build).
- **Web:** upload the contents of `03_NETLIFY_READY/` to Netlify.
- **Database:** run the SQL in `04_SUPABASE_DATABASE_SETUP/` against the Supabase project.

## Notes on secrets

- `local.properties` and any release keystore are git-ignored and must never be committed.
- The Supabase **publishable (anon)** key is present in the client code by design — it is
  the public client key. No `service_role` key exists anywhere in this repository.
- `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/permanent-debug-key/` holds a **debug-only**
  keystore that is intentionally version-controlled so debug builds keep the same signature
  across machines. It is not used for release builds.

## Packaging a release zip

The zip handed to the owner keeps the historical shape: copy the repository contents
(excluding `.git/`, `build/`, `.gradle/`, `.idea/`, `node_modules/`) into a folder named
`PILES_CLINIC_APP_V{number}_FINAL/` and zip that folder as
`PILES_CLINIC_APP_V{number}_FINAL.zip`, incrementing the number each time.
