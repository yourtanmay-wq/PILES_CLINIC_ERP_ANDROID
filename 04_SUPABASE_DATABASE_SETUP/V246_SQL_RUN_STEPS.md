# V246 — how to run the SQL (this is ALL you do)

1. Open Supabase → **SQL Editor** → **New query**.
2. Open `V246_ONE_RUN_SETUP_2026-08-02.sql`, **copy the whole file**, paste it in.
3. Press **Run**. Done.

That one run creates everything:
- the three private schemas `hr`, `wn`, `fin` and all their tables + security (RLS);
- the security fix (no one can make themselves Master);
- the **15 secure logins** — Master + 9 staff + 4 doctors + 1 field officer — and maps each to its person automatically (you do NOT copy any UID);
- the two **private** storage buckets `hr-private`, `fin-private` and their policies;
- exposes the new schemas to the API (keeps your existing ones).

You do **not** need to create users, copy UIDs, edit any line, make buckets, add
policies, or expose schemas by hand. It is safe to run again (idempotent).

## After the run — the only manual thing (Master control of passwords)
Every login is created with the temporary password **`Change#2026`**.
Go to Supabase → **Authentication → Users** and change each password (this keeps
password control in your hands). Staff then log in from the app with their
**Staff Code + the new password** you set.

## ⚠️ One honest note (please read)
Section 8 of the SQL creates the 15 logins by writing to Supabase's internal
auth tables. On a normal Supabase project the SQL Editor is allowed to do this.
If your project restricts direct auth writes, **only section 8 will error** —
everything else (schemas, tables, security, buckets, exposure) is already done.
In that one case, create the 15 users once in Authentication → Users using the
emails listed in section 8 (e.g. `kne-laxmi@staff.piles`) and re-run the file;
the re-run maps them automatically. Nothing is guessed or unsafe.
