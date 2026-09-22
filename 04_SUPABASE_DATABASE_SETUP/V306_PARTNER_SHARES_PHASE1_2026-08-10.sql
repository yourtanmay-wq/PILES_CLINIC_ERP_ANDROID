-- =====================================================================
-- PILES CLINIC — V306 · PARTNER SHARES (অংশীদারি ভাগ) · PHASE 1 FOUNDATION
-- Owner: TK BISWAS · 10.08.2026 (IST).
--
-- HOW TO RUN (that is all you do):
--   Supabase → SQL Editor → New query → paste this WHOLE file → Run.
--   Safe to run again (idempotent). Creates ONLY new tables inside the
--   existing `fin` schema. Never alters/drops/touches any old table or its
--   security. RLS = MASTER-ONLY in this phase (same as Income & Expense).
--
-- WHAT THIS IS:
--   The data foundation for the branch-wise partner profit-share book.
--   Net profit = fin.collections − fin.expenses (already master-only), from
--   1 January of the current year to today. Each partner's share = their %
--   of that net (loss shared too). Withdrawals ("তোলা") are tracked here,
--   separate from expenses (they never reduce Net Profit). Balance =
--   opening + accrued share − withdrawn. Carries forward year to year.
--
-- PHASE 1 (this file): tables + master-only security + audit.
-- PHASE 2 (later file): partner login read-access + same-day edit lock +
--   the 4 new partner identities. NOT in this file, so nothing a partner
--   could touch is opened yet — zero risk to the live master data.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. PARTNERS  (one row = one person's share in one branch)
--    A person can be a partner in more than one branch (multiple rows).
--    `mobile` is the join key to the app user (auto-match by mobile).
-- ---------------------------------------------------------------------
create table if not exists fin.partners (
  "id"          uuid primary key default gen_random_uuid(),
  "branch"      text not null,
  "mobile"      text not null,                 -- 10-digit, matches app user
  "name"        text,
  "pct"         numeric not null default 0,    -- current share %  (0–100)
  "opening"     numeric not null default 0,    -- opening balance this year (+owed to / −already over-taken)
  "can_entry"   boolean not null default false,-- may this partner add Income/Expense?
  "in_app"      boolean not null default false,-- true = already an app user (auto-match); false = new Partner login
  "active"      boolean not null default true, -- false = removed (kept for history, share stops)
  "created_by"  text,
  "created_at"  timestamptz not null default now(),
  "updated_at"  timestamptz not null default now()
);
create unique index if not exists fin_partners_branch_mobile_uidx
  on fin.partners(branch, mobile);
create index if not exists fin_partners_mobile_idx on fin.partners(mobile);

-- ---------------------------------------------------------------------
-- 2. PARTNER % HISTORY  (effective-dated — % change applies FORWARD only)
--    Every % set/change writes a row here with the date it takes effect.
--    Share is computed period-by-period using the % in force at the time,
--    so a later change never rewrites past accrued share.
-- ---------------------------------------------------------------------
create table if not exists fin.partner_pct_history (
  "id"             uuid primary key default gen_random_uuid(),
  "partner_id"     uuid not null,
  "branch"         text not null,
  "mobile"         text not null,
  "pct"            numeric not null,
  "effective_from" date not null,              -- new % applies from this date onward
  "created_by"     text,
  "created_at"     timestamptz not null default now()
);
create index if not exists fin_pcthist_partner_idx on fin.partner_pct_history(partner_id, effective_from);

-- ---------------------------------------------------------------------
-- 3. PARTNER DRAWINGS  (তোলা / ফেরত — separate from expenses)
--    kind = 'withdraw' (money out) | 'return' (money back in).
--    mode = 'cash' | 'online'. Reduces branch CASH balance, never Net Profit.
--    Entered by MASTER only (partners cannot insert drawings).
-- ---------------------------------------------------------------------
create table if not exists fin.partner_drawings (
  "id"          uuid primary key default gen_random_uuid(),
  "branch"      text not null,
  "mobile"      text not null,                 -- which partner
  "entry_date"  date not null,
  "amount"      numeric not null default 0,
  "kind"        text not null default 'withdraw', -- withdraw | return
  "mode"        text,                          -- cash | online
  "note"        text,
  "ignored"     boolean not null default false,
  "created_by"  text,
  "created_at"  timestamptz not null default now(),
  "updated_at"  timestamptz not null default now()
);
create index if not exists fin_draw_branch_mobile_idx on fin.partner_drawings(branch, mobile, entry_date);

-- ---------------------------------------------------------------------
-- 4. SETTLEMENTS  (year-end or on-demand "zero everyone out" record)
-- ---------------------------------------------------------------------
create table if not exists fin.partner_settlements (
  "id"          uuid primary key default gen_random_uuid(),
  "branch"      text not null,
  "mobile"      text not null,
  "settled_on"  date not null,
  "balance_before" numeric,                    -- what was paid out (+) / collected (−) to reach zero
  "note"        text,
  "created_by"  text,
  "created_at"  timestamptz not null default now()
);
create index if not exists fin_settle_branch_idx on fin.partner_settlements(branch, mobile);

-- ---------------------------------------------------------------------
-- 5. SECURITY — MASTER ONLY (this phase). force RLS, master full access.
-- ---------------------------------------------------------------------
do $$ declare t text; begin
  foreach t in array array['partners','partner_pct_history','partner_drawings','partner_settlements'] loop
    execute format('alter table fin.%I enable row level security;', t);
    execute format('alter table fin.%I force row level security;', t);
  end loop; end $$;

drop policy if exists partners_master        on fin.partners;
create policy partners_master        on fin.partners        for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists pcthist_master         on fin.partner_pct_history;
create policy pcthist_master         on fin.partner_pct_history for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists drawings_master        on fin.partner_drawings;
create policy drawings_master        on fin.partner_drawings for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists settlements_master     on fin.partner_settlements;
create policy settlements_master     on fin.partner_settlements for all using ( hr.is_master() ) with check ( hr.is_master() );

-- ---------------------------------------------------------------------
-- 6. AUDIT — reuse fin.audit (master-only view, already exists). Every
--    change to a partner row or a drawing is logged with who + when.
-- ---------------------------------------------------------------------
drop trigger if exists trg_partners_audit on fin.partners;
create trigger trg_partners_audit before update on fin.partners
  for each row execute function fin.fn_audit('partner');
drop trigger if exists trg_drawings_audit on fin.partner_drawings;
create trigger trg_drawings_audit before update on fin.partner_drawings
  for each row execute function fin.fn_audit('partner_drawing');

-- ---------------------------------------------------------------------
-- 7. GRANTS — RLS still restricts every row; grant only opens the API path.
--    (fin schema is already granted to authenticated in V246; re-assert for
--    the new tables so they are reachable once RLS allows.)
-- ---------------------------------------------------------------------
grant select, insert, update, delete on all tables in schema fin to authenticated;

-- =====================================================================
-- DONE. Master can now read/write the partner tables from the app.
-- Partner login access (read-only own ledger + same-day-locked entry) and
-- the 4 new partner identities come in PHASE 2 — a separate one-run file.
-- =====================================================================
