# V238 — Refund Paid/Due Fix

- Base: V237.
- Fixed only the Follow-up Paid/Due calculation.
- Approved refund now subtracts from treatment payments.
- Pending or rejected refund does not change Paid/Due.
- Original payment and refund rows remain unchanged.
- Android and Web follow-up calculations use the same rule.
- Version: `versionCode 238`, `versionName 2.38`.

Changed functional files:

1. `native/FollowUpRepository.kt`
2. `03_NETLIFY_READY/app.js`
3. Android web mirror `assets/www/app.js`

Only version/cache identifiers and this note were additionally changed.
