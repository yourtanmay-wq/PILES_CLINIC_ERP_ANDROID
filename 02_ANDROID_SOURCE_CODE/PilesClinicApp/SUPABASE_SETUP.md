# Phase 5 — Supabase Integration + Offline-First: Setup Guide

## 1. Why raw REST instead of the supabase-kt SDK
This build talks to Supabase's stable HTTP APIs directly (GoTrue for auth,
PostgREST for CRUD) via Retrofit + OkHttp, instead of the official
multiplatform `supabase-kt` SDK. Reason: this project was built in a sandbox
with no Android SDK / Gradle / internet access to actually compile and verify
exact library APIs. Supabase's REST contract (documented, versioned HTTP
endpoints) is something hand-written Retrofit code can target correctly with
high confidence; a fast-moving multiplatform Kotlin SDK is not, without being
able to compile against it. If you'd rather use the official SDK later, the
`data/repository` layer is the only place that would need to change — the
Room/offline-first design underneath does not depend on this choice.

## 2. Configure your Supabase credentials (never hardcoded)
1. Copy `local.properties.example` to `local.properties` (same folder, project root).
2. Fill in real values from your Supabase project (**Project Settings → API**):
   ```
   SUPABASE_URL=https://YOUR-PROJECT-REF.supabase.co
   SUPABASE_ANON_KEY=your-anon-public-key
   ```
3. `local.properties` is already in `.gitignore` — it will never be committed.
4. Rebuild. `SupabaseConfig.isConfigured` becomes true and the Sync & Account
   screen's red "not configured" banner disappears.

CI alternative: set the `SUPABASE_URL` / `SUPABASE_ANON_KEY` environment
variables instead of using `local.properties`.

## 3. Supabase SQL schema (run this once in the Supabase SQL editor)

```sql
create table if not exists enquiries (
  id text primary key,
  patient_name text not null,
  phone text not null,
  address text,
  enquiry_date bigint not null,
  source text,
  notes text,
  created_at bigint not null,
  updated_at bigint not null,
  is_deleted boolean not null default false
);

create table if not exists registrations (
  id text primary key,
  enquiry_id text,
  reg_no text not null,
  patient_name text not null,
  age integer,
  gender text,
  phone text not null,
  address text,
  registration_date bigint not null,
  referred_by text,
  notes text,
  created_at bigint not null,
  updated_at bigint not null,
  is_deleted boolean not null default false
);

create table if not exists follow_ups (
  id text primary key,
  patient_id text not null,
  follow_up_date bigint not null,
  notes text,
  next_visit_date bigint,
  done_by_role text not null default 'STAFF',
  created_at bigint not null,
  updated_at bigint not null,
  is_deleted boolean not null default false
);

create table if not exists payments (
  id text primary key,
  patient_id text not null,
  amount double precision not null,
  payment_date bigint not null,
  mode text not null default 'Cash',
  purpose text,
  received_by_role text not null default 'STAFF',
  created_at bigint not null,
  updated_at bigint not null,
  is_deleted boolean not null default false
);

-- Row Level Security: enable + a simple "any authenticated user of this
-- clinic app can read/write" policy. Tighten later per-role if needed.
alter table enquiries enable row level security;
alter table registrations enable row level security;
alter table follow_ups enable row level security;
alter table payments enable row level security;

create policy "authenticated full access" on enquiries
  for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');
create policy "authenticated full access" on registrations
  for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');
create policy "authenticated full access" on follow_ups
  for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');
create policy "authenticated full access" on payments
  for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');
```

Dates are stored as `bigint` epoch-milliseconds (not `timestamptz`) to keep
the client-server date math trivial and timezone-proof; the app always reads
them back as `Long` on the Kotlin side.

Create at least one Supabase Auth user (Authentication → Users → Add User)
to sign in from the app's Sync & Account screen.

## 4. Conflict-safe sync strategy (as implemented)
- Every row has `updated_at`, stamped on every local edit.
- **Push**: `POST .../rest/v1/<table>?on_conflict=id` with
  `Prefer: resolution=merge-duplicates` — a Postgres upsert keyed by `id`.
  The latest push for a given id always wins on the server.
- **Pull**: only overwrite a local row if the server's `updated_at` is newer
  than the local one, so a not-yet-synced local edit is never clobbered by
  stale server data.
- **Deletes are soft** (`is_deleted` flag), so a delete is just another
  synced field — never a destructive drop before the delete itself is synced.
- **Never lose local data**: Room is the single offline source of truth for
  the UI. A push failure only sets `syncStatus = FAILED` on that row (with
  the error message) and leaves the row fully intact for a retry — nothing
  is ever deleted locally because of a sync failure.

## 5. Where things live
- `data/local/` — Room entities, DAOs, `AppDatabase`.
- `data/remote/` — Retrofit interfaces + DTOs + `SupabaseConfig`.
- `data/session/` — encrypted on-device token storage (`SessionManager`).
- `data/repository/` — `AuthRepository`, one repository per entity,
  `SyncManager` (the actual push/pull engine), `SyncStatusHolder` (observable
  UI state).
- `data/sync/` — `SyncWorker` (WorkManager job), `SyncScheduler` (periodic +
  on-demand), `ConnectivityObserver` (auto-sync when internet returns).
- `sync/SyncStatusActivity` — standalone test/status screen (Account sign
  in/out, Sync Now, per-table Pending/Synced/Failed counts + quick "add test
  record" buttons for all 4 tables).
