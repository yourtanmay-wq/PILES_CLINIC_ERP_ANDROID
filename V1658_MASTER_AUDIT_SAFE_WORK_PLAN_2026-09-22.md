# V1658 Master Audit — Safe Work Plan
Date: 22.09.2026 08:10 IST
Baseline SHA-256: c980331c5df2e67afb525bbf842a677d3f954fd78a345d82206f6e93f43709b3

Rule: One approved change at a time. Explain benefit, risk and rollback before each change. No live DB or release change without owner approval.

Order:
1. Web login security regression — local copy only first.
2. Credential security migration — safe verification path first; plaintext/RLS later.
3. Public data RLS — table-by-table pilot only.
4. Anonymous privileged RPC lock — only after token coverage proof.
5. Partner settlement current-balance server recheck.
6. Partner setup / withdraw / return safety guards.

No existing patient/payment/partner financial data is to be rewritten as part of these security fixes.
