-- V1519 · 17.09.2026
-- Purpose: make the existing Android LiveRefresh delta/count queries use an index
-- instead of scanning the whole core table repeatedly.
--
-- SAFETY:
--   * indexes only; no row is inserted/updated/deleted
--   * no RLS/policy/schema-column change
--   * IF NOT EXISTS => safe to re-run
--
-- This exact change was applied to live project bcyeogjqtupbdyciqfmz and verified
-- with EXPLAIN ANALYZE before this file was packaged.

create index if not exists followups_updatedat_desc_idx
    on public.followups ("updatedAt" desc nulls last);

create index if not exists payments_updatedat_desc_idx
    on public.payments ("updatedAt" desc nulls last);

create index if not exists patients_updatedat_desc_idx
    on public.patients ("updatedAt" desc nulls last);

create index if not exists enquiries_updatedat_desc_idx
    on public.enquiries ("updatedAt" desc nulls last);
