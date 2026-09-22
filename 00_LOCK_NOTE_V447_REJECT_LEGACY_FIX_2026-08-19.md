# V447 — Old Reject resurfacing fix — 19.08.2026

Scope: only Follow-up Inquiry visibility guard.

Verified root cause in V446:
- V445 read terminal `followups` correctly.
- But the companion `enquiries` read was limited to `stage=Inquiry`.
- Older reject paths can have `enquiries.stage=Cancelled` + `status=Cancelled`.
- Therefore that durable reject marker was invisible to the V445 guard and an older Active duplicate follow-up could show again.

V447:
- Android also reads slim non-Active enquiry statuses independent of stage and adds only Cancelled/Incomplete/Rejected/Closed mobiles to the reject guard.
- Web uses the already-loaded enquiries list with the same terminal-status rule.
- Restore/Continue Entry remains valid because it sets matching enquiry/follow-up status back to Active.
- No records are deleted. No payment, patient, medical, salary, design, permission or Supabase schema logic changed.
