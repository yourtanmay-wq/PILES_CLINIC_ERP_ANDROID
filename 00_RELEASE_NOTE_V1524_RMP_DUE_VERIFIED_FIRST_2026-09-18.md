# V1524 — RMP Due List verified-cloud-first

- RMP Due List no longer flashes legacy/local due amounts before cloud verification.
- With a branch selected, the screen shows CHECKING CLOUD first, then one verified final list.
- When `fin.rmp_branch_due` succeeds, its branch result is the sole final source for the Due List; legacy rows are not merged back.
- Android and Web now call branch due first. Up to 25 per-RMP summary calls are used only as fallback if the branch summary is unavailable.
- No patient/payment/referral history is deleted or rewritten by this UI fix.
- Based on user video showing Jalpaiguri briefly as 2 RMP / ₹5,251 before switching to a different verified list.
