# V1522 — Egress-safe pagination + Realtime fallback

Date: 18 Sep 2026

## Why
Supabase Usage showed 1.84 GB PostgREST egress on 17 Sep. That daily total is dashboard evidence; pg_stat_statements call counts are cumulative since 13 Jul and are not treated as 17-Sep-only counts. Read-only audit found a concrete Android source leak in V1519+ pagination: for lists over 1000 rows, offset 0 used CloudListRevalidate but offset 1000/2000/... bypassed it and were downloaded again after the 60-second dedupe expired. This affects large followups/patients/payments reads.

Live read-only database measurements on 18 Sep show the row payloads used by the heavy no-photo/slim list paths are roughly below the existing 2 MB single-entry cap: followups ~0.63–1.23 MB per 1000 rows, patients ~0.27–1.45 MB, payments ~0.47–1.26 MB. These are database-side payload estimates, not a byte-for-byte PostgREST network measurement. If any specific response exceeds the 2 MB guard, CloudListRevalidate safely refuses to cache that page and falls back to the old network behavior; it does not truncate data.

## Changes
1. SupabaseClient.fetchListPagedOrNull(): every non-direct page now passes through the existing CloudListRevalidate guard. No filter/order/select/offset/data rule changed. Add/delete changes row count; edit advances updatedAt; either invalidates the cached pages and forces fresh reads.
2. RealtimeRefresh/LiveRefresh: the old 30-second count poll slows to a 5-minute safety poll only after the actual `postgres_changes` channel join is acknowledged and server acknowledgements remain recent. A heartbeat reply by itself cannot mark the subscription healthy. If the channel is not joined, becomes stale, or the socket fails, the original 30-second fallback remains unchanged.
3. Version parity: Android 1522 / 15.22 and Web version.json 1522 / 15.22. V1521 ZIP was found to have Android 1521 while Web version.json still said 1520; corrected here.

## Not changed
No patient/payment/commission/attendance/leave data, DB schema, UI design, branch rules, roles, calculations, print rules, or live Supabase rows were changed by this source patch.

## Verification
- Android resource guard: PASS.
- Kotlin parser smoke check: no syntax/"expecting" diagnostics in the three edited Kotlin files; full type compile could not run because android.jar download is blocked in this environment. This is NOT claimed as a full Gradle compile.
- Live DB checks used read-only SQL only.


## Rollout note
The source fix cannot reduce traffic from phones that continue running older APKs. Live read-only version data on 17–18 Sep still showed active devices on V1488/V1508/V1514. After V1522 is built and installed, all active clinic phones should be updated before judging the next day's egress. Exact GB savings must be verified from Supabase Usage after real deployment; no precise reduction is claimed in advance.
