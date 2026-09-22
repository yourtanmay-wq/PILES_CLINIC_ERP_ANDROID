# V1523 — RMP One Patient = One Line

- Keeps all V1522 egress hardening.
- Android RMP View All: when server patient-wise ledger is available, each referred patient is rendered once.
- The separate referred row + income row duplication is removed.
- Zero-treatment referred patients still remain visible as one line.
- Treatment deposit is taken from the patient-wise server breakdown (`net_paid`) and commission is auto-calculated by the stored RMP patient rate.
- Web RMP detail mirrors the same one-patient-one-line presentation.
- No patient/payment/referral history rows are deleted by this source change.
- Live data correction on 2026-09-18: Kishanganj AMIT GOLDAR / NOOR ALAM patient commission corrected from 10% to 50%, matching the approved Kishanganj branch rule. Jalpaiguri AMIT GOLDAR remains 30%.
