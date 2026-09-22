# V1527 — Take Action first/second tap consistency fix

## Verified problem
The patient Timeline paints cached totals immediately, while the fresh cloud read finishes afterward. The Take Action button was clickable during that gap. Its options depend on live fields such as bill, due, paid entries, stage, registration, referring doctor, and completion state. Therefore the first tap could build a shorter menu, while a second tap after cloud refresh built the full menu.

## Fix
- Cached Timeline stays display-only.
- Take Action cannot open until the fresh cloud Timeline has populated all action-condition fields.
- A first tap during loading is queued; once verified data arrives, the final menu opens automatically.
- Queue auto-action uses the same gate.
- If live verification fails, no stale action menu is shown.

No patient/payment/chamber/Supabase data is changed by this source fix.
