# V1519 — Realtime Payment + Loading Performance Fix
Date: 17.09.2026

## Scope
Only the reported Realtime/payment refresh and loading bottlenecks were changed.
No payment arithmetic, branch rules, RLS, report formula, print layout, Web UI, or patient workflow was redesigned.

## Android Realtime
- Added one foreground-only Supabase Realtime websocket helper for `payments`, `patients`, `followups`, `enquiries`.
- Realtime payload is restricted to identifiers (`id`, `branch`, `mobile`, `patientId`, `updatedAt` where available); photos/full rows are not streamed.
- Payment, Collection, Follow-up, Chamber Attendance, Doctor Queue, and the currently-open Patient Timeline refresh on a matching change.
- Existing 30-second LiveRefresh HEAD/count check remains unchanged as fallback.
- Websocket stops when no subscribed screen is foreground and respects the existing 06:00–22:00 LiveRefresh window.
- Heartbeat uses the Supabase/Phoenix 25-second requirement.
- On a remote event, stale read caches for that table are invalidated before the existing screen loader runs.

## Payment edit freshness
All direct Android `payments` UPDATE paths were audited. Every one now updates `updatedAt`, including amount/mode edits, progress edits, name sync, and Report Card payment edits. This prevents a successful edit from being invisible to delta refresh/cache invalidation.

## Loading
`fetchListPagedOrNull()` keeps V1303/V1304 correctness (1000-row paging, exact-count completeness guard, id de-duplication, null on partial failure) but downloads the required 1000-row pages concurrently through the existing bounded `ParallelCloud` helper. The query, filter, sort, selected columns and result order are unchanged.

## Live database
Added `updatedAt DESC NULLS LAST` indexes for the four core refresh tables. Measured examples:
- followups delta: ~3.18 ms sequential scan -> ~0.47 ms index scan
- payments delta: ~3.82 ms sequential scan -> ~0.27 ms index scan

## Verification
- Live `supabase_realtime` publication verified to include all four core tables.
- Current Supabase Realtime protocol/heartbeat requirements checked against official documentation.
- Realtime core + callback call-pattern compiled with local Kotlin compiler and stubs.
- Full Gradle build could not run in the execution environment because Gradle 8.5 wrapper distribution was not cached and outbound download to services.gradle.org was blocked. This is an environment limitation, not a reported Kotlin compiler error.
