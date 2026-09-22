# V1516 — Automatic Field Tracking Fix (17/09/2026)

Scope: Android only. No patient/payment/leave/report/database schema changes.

Verified root cause:
- IN TIME already called `FieldVisit.startDay()`, but distance/route saving still depended on the obsolete manual `chosenMode == FIELD`. The picker is no longer invoked, so RUPAM/ARMAN could run location service while route/distance stayed zero.
- The daily battery-optimization protection was documented for IN TIME but actually ran only after pressing FIX LOCATION; automatic IN flow skipped it.
- If phone Location was OFF during automatic IN, the flow silently returned.

Fixes:
1. RUPAM/ARMAN are automatically marked as FIELD when IN TIME starts; all other attendance users remain CHAMBER for distance purposes.
2. `onLocation()` determines field-distance/route eligibility from the stored staff identity, not a manual selector.
3. IN TIME with valid permission/location now re-starts the foreground tracking service idempotently and runs the once-per-day battery protection prompt.
4. IN TIME with phone Location OFF now tells the staff to turn it on (attendance remains saved).
5. Route storage remains capped at 20 points, but when full it compacts old points instead of discarding the rest of the day, so the route continues to cover first-to-last movement without increasing cloud row size.
6. Stale last-known GPS positions older than 10 minutes are no longer accepted as a fresh starting point.
7. Android version bumped 1515/15.15 -> 1516/15.16.

Expected rule:
IN TIME -> automatic location foreground service -> RUPAM/ARMAN distance + route + last location -> cloud updates -> OUT TIME stops service and final-pushes the day.

Verification performed in this environment:
- Static flow checks: 16/16 PASS (IN start, field identity, permission, Location-OFF handling, battery prompt, foreground-service declaration, OUT stop, route compaction, version).
- Kotlin parser pass had no syntax/"expecting" diagnostics attributable to the edit; full standalone kotlinc resolution is not possible without Android dependencies.
- Full Gradle APK build was attempted, but this environment has no Gradle 8.5 distribution cached and network download is blocked (`UnknownHostException: services.gradle.org`). Therefore no false claim of a completed APK compile is made here. Android Studio/normal internet-connected Gradle should perform the final compile check.
