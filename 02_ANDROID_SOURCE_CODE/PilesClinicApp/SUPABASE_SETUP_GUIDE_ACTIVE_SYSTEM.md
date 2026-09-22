# Supabase Setup Guide — for the ACTIVE system (WebView ERP)

## ⚠️ Important correction first
This project ships with an existing `SUPABASE_SETUP.md` that documents a
**different, unused native Room/Retrofit Supabase layer** (Kotlin
`data/repository/*`) — that layer exists in the codebase but is never
launched from any screen (flagged in this session's Step 1 report). The
**real, active ERP** is the WebView app (`assets/www/app.js` +
`config.js`), which talks to Supabase completely differently. This guide
covers the real, active path. Keep `SUPABASE_SETUP.md` for reference if you
ever wire up the native layer, but use **this** file for the app you're
actually shipping.

## 1. How the active app connects to Supabase
- Credentials live in `app/src/main/assets/www/config.js` as
  `window.RK_CONFIG.supabaseUrl` / `.supabaseKey`. These are bundled into
  the APK as plain static JS — **there is no build-time secret injection
  for this layer** (unlike the native layer's `local.properties` approach).
  Treat this key the way you'd treat any public/anon key: it should be a
  Supabase **anon** key with Row Level Security policies restricting what
  it can do, never a service-role key.
- The Supabase JS SDK itself is **not bundled** — it's fetched at runtime
  from `https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2` (see
  `ensureSupabaseSdk()` in `app.js`). This means:
  - The device needs real internet access for cloud sync to work at all.
  - If `cdn.jsdelivr.net` is blocked (corporate firewall, some rural ISPs,
    China-region restrictions, etc.), the SDK never loads and the app
    **silently falls back to local-only storage** — it does not crash or
    show an error, it just queues everything as "pending cloud sync"
    forever. This is a real operational risk to be aware of (see Remaining
    Risks in the final report).
- All reads/writes are offline-first: every save goes to `localStorage`
  immediately and successfully regardless of network; cloud sync is a
  best-effort background layer on top (`directCloudUpsertRow`,
  `pullCloudAllForVisibility`, `queueCloudTableSave`).

## 2. Tables the active app actually uses
Confirmed by scanning every `load(...)`/`save(...)`/`sb.from(...)` call in
`app.js`:
```
enquiries, followups, patients, payments, medical, doctor_visits,
briefings, products, backuprecords, usercredentials, activity_logs,
download, trash
```
These are **not** the same table/column names as `SUPABASE_SETUP.md`'s SQL
schema (that schema is for the unused native layer, with different column
naming — e.g. `phone` instead of `mobile`, `patient_name` instead of
`name`). If your Supabase project doesn't already have these tables, you'll
need to create them matching the field names each JS row object actually
uses (visible directly in `app.js` wherever `add('tablename', {...})` is
called).

## 3. Row Level Security
Since the anon key ships inside the APK, **RLS policies on every table
above are not optional** — without them, anyone who extracts the key from
the APK (trivial — it's a plain string in an asset file) has the same
access as your app. At minimum:
- Enable RLS on every table listed above.
- Scope policies by an authenticated user/branch claim if your Supabase
  Auth setup supports it, or at minimum restrict to authenticated sessions
  only (no anonymous public write access).

## 4. What I could not verify in this sandbox
No network access here means none of the following were tested end-to-end:
login against your real Supabase project, an actual row save reaching your
database, pull-sync merging remote+local correctly, or RLS policies
behaving as expected. All of the logic paths were reviewed and are
internally consistent, but "should work" is not the same as "verified
against your live project" — that step is yours to do on a real device.
