# V325 RMP Commission — Controlled Test Checklist

Status legend: `[S]` static/formula verified · `[T]` disposable PostgreSQL function test passed · `[B]` Android build required · `[L]` controlled live test required.

## Calculation

- [S] Registration Fee does not increase commission.
- [S] Visit Fee does not increase commission.
- [S] Medicine Payment does not increase commission.
- [T] 10% treatment calculation uses the protected summary function.
- [T] Final Bill ₹25,000, paid ₹27,000, 10% caps at ₹2,500.
- [T] Fixed ₹3,000, Final Bill ₹30,000, paid ₹5,000 creates ₹500.
- [T] Fixed ₹3,000, net paid after refund ₹8,000 / Final Bill ₹10,000 creates ₹2,400.
- [T] Approved linked treatment refund reduces earned commission.
- [T] Approved refund linked to Visit Fee does not reduce treatment commission.
- [T] Pending/rejected refund does not reduce earned commission.
- [T] Final Bill edit immediately recalculates fixed Amount commission.

## Default and patient snapshot

- [T] RMP Default and patient snapshots support Percent and Fixed Amount.
- [S] Pay form is locked when the selected patient belongs to another RMP.
- [L] First treatment payment copies current Default to patient.
- [S] Missing Default/commission failure is isolated after Payment save and cannot return Payment to Pending.
- [L] Missing Default warning appears correctly on the real Android/Web screen.
- [T] Late commission setting includes all earlier treatment payments.
- [T] Changing Default does not change existing patient snapshot; new patient receives new Default.
- [T] Patient-specific override does not change RMP Default.

## Commission payment / Expense

- [T] Partial payment reduces Unpaid; multiple payments work.
- [T] Cash/Online saves; Online reference remains optional.
- [T] Expense category is exactly `RMP Commission Payment` and amount matches Commission Payment.
- [T] Expense uses patient's treatment Branch.
- [T] Staff/Doctor cannot pay above Unpaid.
- [S] Staff/Doctor cannot exceed the allowed remaining amount through Payment Edit.
- [S] Staff/Doctor cannot create an above-Unpaid backdate/edit approval request.
- [T] Master can pay above Unpaid; More Paid appears separately and linked Expenses still total Paid.
- [T] Staff same-day edit updates Commission and linked Expense together.
- [T] A failed linked Expense insert leaves no Commission Payment or payment-audit row.
- [T] A failed linked Expense update rolls back the Commission edit and preserves both old values.

## Approval and visibility

- [T] Old-date Staff/Doctor payment remains pending with no money change until Master approves.
- [T] Master approval applies exactly once; a second approval is rejected.
- [T] Approval recalculates current Unpaid and rejects a request made stale by later payments.
- [T] Approval rejects an old commission-change request after Master has changed the commission.
- [T] Approval rejects an RMP-reassignment request after the patient's RMP has already changed.
- [S] Staff/Doctor edit validation locks the patient commission against simultaneous overpayment.
- [S] Future commission/payment dates are rejected at database level.
- [T] Rejection changes no Commission or Expense amount.
- [T] Old payment edit by Staff/Doctor creates a Pending request; approval updates Commission and Expense together.
- [S] Staff/Doctor cannot change today's payment date to an old date through direct edit.
- [S] Earlier patient commission change is blocked from direct Staff/Doctor save.
- [T] Earlier patient commission change stays unchanged until Master approves it.
- [S] Commission requests verify the patient's Branch before creating a request.
- [T] Normal payment rows are visible to authenticated Staff in their Branch and Master.
- [S] Web contains the same normal History and correction entry points as Android.
- [T] Web Referral menu, Patient form, Pay form, History and Master-private Edit render without a runtime error in a disposable DOM.
- [S] Payment Edit request verifies that the payment belongs to the selected patient.
- [T] Field Officer cannot use protected commission functions.
- [T] Non-Master cannot write another Branch's patient commission.
- [T] Patient/RMP summary functions reject another Branch for Staff/Doctor.
- [T] Patient commission, payment history and RMP Default read policies are Branch-limited.
- [T] Master Private edit marks the payment hidden and is filtered from Staff direct reads.
- [T] Staff/Doctor summary still reflects the Master-private amount while history hides that row.
- [T] Private audit rows are visible to Master and filtered to zero for Staff.

## RMP reassignment — owner option 1

- [T] Master reassigns directly; Staff/Doctor request remains pending until approval.
- [T] Old paid rows keep old RMP id/name.
- [T] Old Paid still reduces patient Unpaid; duplicate Due is not created.
- [T] Remaining and future Commission Payment uses new RMP id/name.
- [T] New RMP Summary shows `Previous RMP Paid` separately.
- [T] Old RMP Summary retains the amount actually paid to old RMP.

## Regression / design

- [L] Owner proof: Android V325 built, installed and opened the synced Master dashboard at 13:20 IST.
- [L] Owner proof: Master Referral Income menu opened all five intended routes at 13:22 IST.
- [L] Owner proof: RMP Default screen opened and live read returned `No Default set yet` at 13:25 IST.
- [L] Owner proof: Master saved Dr. JAFAR Default as 10% and received success at 13:26 IST.
- [L] Owner proof: reopening returned Dr. JAFAR Percent 10.0 with `Current Default loaded` at 13:27 IST.
- [L] Owner proof: Patient Commission, zero Summary, preserved Previous Records and empty Pending
  Approval routes opened correctly at 13:29 IST.
- [L] Owner proof: Registration cached RMP selector opened, name search narrowed 1000 entries to three,
  and selection auto-filled the saved RMP name/mobile at 13:32–13:34 IST without saving a patient.
- [L] Owner proof: exact mobile search returned only the matching Dr. JAFAR saved record at 13:36 IST.
- [S] Every Web JavaScript file passes `node --check`.
- [B] Existing Registration, Treatment Payment, Refund, Receipt and Previous Records open normally.
- [B] No text/button overlaps in small Android screen.
- [S] Web and Android amount displays call the same protected `rmp_summary` / `rmp_rmp_summary` functions.
- [S] Commission sources contain no photo/full-table download, timer or background polling.
- [L] Supabase request count remains small during controlled real-device use.
- [T] Patient Payment, RMP payment-total and Pending-request lookup indexes create successfully.
- [S] Android `Previous Records` still opens the preserved legacy function; Web still opens existing `viewDoctorVisit`.
- [B] Existing old Referral Income history opens correctly in the built app.

## Release gate

- [T] Running the migration a second time preserves existing commission/payment/Expense rows and functions.
- [L] Owner screenshot: live foundation SQL completed successfully at 12:55 IST on 2026-08-12.
- [L] Owner screenshot: read-only verification completed without error and returned
  `authenticated_fin_usage = true` at 12:57 IST on 2026-08-12.

- Do not mark complete until all `[B]` and `[L]` items are tested.
- Do not package or deliver without the owner's explicit request.
- Before delivery, outer ZIP, inner folder, Android version, Web version, nested package names,
  report and manifest must all use the same final version.
