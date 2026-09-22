# V325 RMP Commission — Owner-Controlled Work Log

Owner: Master Admin (TK)
Rule: No unrecorded change. No final package/file delivery without the owner's explicit request.

## 2026-08-12 10:47 IST — Safe foundation (work in progress)

- Confirmed the existing authenticated identities for Master, Staff and Doctor.
- Kept every existing table, policy, design and workflow unchanged.
- Added an isolated RMP commission database foundation; it is not run on any live database.
- Kept `fin.expenses` Master-only. Added a protected database transaction so an approved
  commission payment and its `RMP Commission Payment` expense succeed or fail together.
- Added one shared Android calculation rule:
  - excludes Registration Fee, Visit Fee and Medicine;
  - includes treatment payments only;
  - subtracts approved refunds linked to treatment payments;
  - supports Percent and proportional fixed Amount;
  - caps earning at Final Bill / fixed Amount;
  - shows excess payment separately and never makes Due negative.
- Added authenticated RPC support without changing existing read/write methods.
- Added tests for owner-provided examples: ₹500, ₹2,400, ₹2,500 maximum, ₹400 excess.
- Verification limitation recorded honestly: Android tests could not start because Gradle 8.5
  was not present locally and this workspace cannot reach the Gradle download server. This is
  not recorded as a pass. Full build/test remains mandatory before completion.

## Lock / accountability

- This log is append-only during this work. Corrections must be added as a new dated entry.
- Database mutations are written to `fin.rmp_commission_audit` with actor and timestamp.
- Anonymous/public users receive no commission write permission.
- Work status: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Android authenticated repository

- Added a separate Android repository for RMP Default, patient commission snapshot,
  live summary and partial commission payment.
- It uses only the authenticated protected functions; it has no direct Expense write access.
- It does not write or delete old `doctor_visits.referralPayments` records.
- Network/read failure is returned as failure; it is never shown as a genuine zero.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Discount, refund and all payment-entry parity

- Proven from current source that `bill` is saved/displayed as Total Bill and new Web payments set
  separate discount to zero; older rows may still carry discount.
- Final Bill calculation now safely uses `bill - discount` (minimum zero).
- Found that the current general Patient Refund flow usually has no `refundOfPaymentId`.
- Corrected refund logic: approved unlinked Patient Refund reduces treatment commission; a linked
  refund reduces it only when its original row is a treatment payment.
- Pending/rejected refunds do not reduce commission.
- During static review, found and corrected a Kotlin parenthesis error immediately; no build pass claimed.
- Centralized Android commission activation/warning across all nine treatment-payment callers and
  offline retry success; same patient is warned at most once per day while Default is missing.
- Added Web activation for both Visit Advance and normal Treatment Payment paths.
- Web JavaScript syntax passed again after both hooks.
- Confirmed no commission source downloads photos/full Patient/full Payment tables and no direct
  commission table INSERT/UPDATE/UPSERT exists outside protected RPC functions.
- Removed an unnecessary function-drop statement; migration remains data-additive.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — RMP totals and compile-risk reduction

- Added RMP-level Summary: patient count, earned, paid to this RMP, previous RMP paid,
  Unpaid and More Paid.
- Previous-RMP credit is displayed separately so reassignment arithmetic is transparent.
- Existing legacy Referral totals are not overwritten; new verified summary is a separate path.
- Added 43 controlled test scenarios with static/build/live status labels.
- Compared working tree with pristine source: differences are limited to approved RMP selection
  and RMP commission scope; no Medicine/Prescription/Follow-up/Report/shared-CSS file changed.
- Removed two mixed generic-result expressions from Android UI and normalized them to simple
  success/message pairs, reducing Kotlin compile uncertainty without changing behavior.
- Web JavaScript syntax still passes after Summary changes.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Owner decision: RMP reassignment option 1

- Owner selected option 1.
- Existing paid commission rows permanently keep the old RMP id/name.
- The same paid amount remains included in total Paid, so reassignment cannot create duplicate Due.
- Only remaining and future commission belongs to the newly assigned RMP.
- Master can reassign directly; Staff/Doctor creates a pending request.
- Direct patient-commission save is blocked from bypassing the protected reassignment path.
- Android and Web use the same reassignment RPC and approval rule.
- Re-ran Web JavaScript syntax validation after the change; all files passed.
- SQL structural check: all 12 function bodies have balanced dollar blocks and BEGIN/END pairs.
- Full PostgreSQL execution still requires the controlled test database and is not claimed as passed.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Commission history and correction workflow

- Added Android Commission Payment History for normal records.
- Staff/Doctor do not receive Master-private rows because database RLS filters them.
- Master sees private rows marked `Master Private` and Master-only audit retains before/after values.
- Same-day normal correction updates Commission Payment and linked Expense in one transaction.
- Old-day correction by Staff/Doctor creates a PAYMENT_EDIT approval request; no amount changes first.
- Master Approve applies the correction once; Reject leaves Commission and Expense unchanged.
- Optional reason is preserved; it is not mandatory.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Web parity, isolated implementation

- Added a separate `rmp_commission.js`; existing Referral records/functions and shared CSS were not rewritten.
- Added one Referral Income entry button to existing RMP cards.
- Web now has RMP Default, patient commission snapshot, verified summary, Cash/Online payment,
  optional reference number, backdate request and Master approval paths.
- Previous Records still opens the existing `viewDoctorVisit` history.
- All Web commission writes use the same authenticated `fin` RPCs as Android; no anon money writes.
- Ran `node --check` on every Web JavaScript file; all passed syntax validation.
- Browser/live Supabase testing is still pending and is not claimed as passed.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Approval, branch and private-adjustment foundation

- Added pending approval records for backdated commission payments and past commission changes.
- Staff/Doctor requests do not affect commission or Expense before Master approval.
- Master approval applies once; an already-decided request cannot be applied twice.
- Added Android date selection: today saves normally; an old date from Staff/Doctor sends a request.
- Added a Master-only Pending Commission Approvals list in Referral Income.
- Restricted commission access to Master, Staff and Doctor; Field Officer is denied at database level.
- Restricted non-Master commission writes to the patient's treatment branch.
- Added atomic Commission Payment edit: linked Expense amount/date/mode always changes with it.
- Added optional Master-private adjustment: the payment row is hidden from non-Master history,
  but summary still uses the adjusted amount; private audit history is Master-only.
- RMP reassignment is not auto-approved because the owner decided that already-paid money requires
  a Master decision at reassignment time. No money moves without those decision details.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Recalculation and owner-example verification

- Confirmed Refund and Final Bill changes need no destructive rewrite: summary is recalculated
  from the latest Final Bill, treatment payments and approved linked refunds whenever loaded.
- Re-ran the owner's numeric examples against the implemented formula:
  - Final Bill ₹25,000, collection ₹27,000, 10% => maximum ₹2,500.
  - Final Bill ₹30,000, collection ₹5,000, fixed ₹3,000 => ₹500 earned.
  - Final Bill ₹10,000, net collection after refund ₹8,000, fixed ₹3,000 => ₹2,400 earned.
  - Earned ₹500 and paid ₹900 => Unpaid ₹0 and More Paid ₹400.
- Registration Fee, Visit Fee and Medicine remain outside the accepted treatment types.
- These were formula/static checks, not a substitute for the still-pending Android build/live test.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Treatment-payment activation safety

- Audited all treatment-payment callers; nine call sites share `saveTreatmentPayment`.
- Did not alter payment amount, bill, local cache, pending queue, cloud write, receipt or design logic.
- Added commission activation only after the payment is confirmed in cloud.
- If a patient already has commission, it remains unchanged.
- If no patient commission exists and the RMP has a Default, that Default is snapshotted to the patient.
- If the RMP Default is missing, Payment still saves and the Payment screen shows a warning.
- Commission check exceptions are isolated and cannot fail or roll back payment.
- Registration, Visit Fee and Medicine save paths are untouched.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 — Android Referral Income UI (first controlled pass)

- Kept the visible name `Referral Income` unchanged.
- Added separate choices for RMP Default, Patient Commission / Payment, and Previous Records.
- Kept all previous Referral Income records accessible; no migration or deletion is attempted.
- Patient selection verifies the real saved patient by mobile before enabling commission work.
- Added Percent, proportional Fixed Amount, and "Use RMP Default" choices.
- Commission payment stays locked until the cloud returns a verified Earned/Paid/Unpaid summary.
- Staff/Doctor overpayment is blocked in both Android and database validation; Master remains allowed.
- Cash/Online and optional transaction/reference number are included.
- Existing premium dialog helpers are reused; no shared layout/resource was changed.
- Full Android build is still pending because the required Gradle distribution is unavailable here.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 11:36 IST — Approval-bypass and Branch audit correction

- During the second static audit, found that Staff/Doctor could attempt to replace an earlier
  patient's commission through the normal save path. Closed that path at the database level.
- Android and Web now send an earlier commission change to Master Approval; the saved commission
  remains unchanged until Master approves it.
- When "Use RMP Default" is chosen for that request, the exact current Default is copied into the
  request, so a later Default change cannot silently alter what Master is approving.
- Closed a date-check gap: Staff/Doctor can no longer edit today's Commission Payment and change its
  date to an older date without Master Approval.
- Approval requests now verify the patient's Branch. RMP Default changes also verify the RMP's Branch;
  Master remains unrestricted and Staff/Doctor remain limited to their existing Branch.
- Added explicit validation for missing Payment date and malformed past-commission/reassignment requests.
- Re-ran Web JavaScript syntax checks after these corrections; all files passed.
- SQL structural check now covers 13 functions; all dollar blocks and BEGIN/END bodies are balanced.
- These are static safeguards. Controlled database and Android build tests are still pending and no
  package/file has been delivered.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 11:37 IST — Supabase Free Plan request reduction

- Reduced Android's normal post-payment commission check to one small indexed patient-commission
  lookup when that patient's commission is already set.
- Patient and RMP details are now fetched only while creating the first snapshot or showing the
  missing-Default warning; they are not downloaded again after every later treatment payment.
- No photo, medical record, full patient list or full payment list is fetched by this check.
- This optimization changes no Payment, commission amount, screen design or approval rule.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 11:40 IST — Web History parity and edit-overpay protection

- Found during Android/Web comparison that Android had Commission Payment History and correction,
  but Web did not. Added the missing Web path without changing shared CSS or old Referral records.
- Web Master/Staff/Doctor can now see normal Commission Payment history. Database RLS continues to
  hide Master-private rows from Staff/Doctor.
- Web correction now follows the same rule as Android: old/originally-old date by Staff/Doctor creates
  a Master request; Master can edit directly and may mark the change private; reason remains optional.
- Commission Payment and its linked Expense still update together through the single protected function.
- Found and closed a second overpayment route: Staff/Doctor could try to exceed Unpaid while editing an
  existing payment. Direct edit and approval-request creation now both enforce the allowed remaining amount.
- Payment-edit requests now verify that the selected Commission Payment actually belongs to that patient.
- Re-ran syntax checks on every Web JavaScript file; all passed. SQL's 13 function bodies remain balanced.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 11:42 IST — Branch read protection

- Found that Branch limits were enforced on commission writes, but the first read policy was broader
  than the existing app rule. A Staff/Doctor who somehow knew another row id could attempt a direct read.
- Closed that database-level path: patient commission, payment history, patient summary, RMP summary
  and RMP Default reads now enforce the current user's Branch; Master remains all-Branch.
- This does not change the visible Branch filter or any screen design. It makes the database follow
  the same Branch rule even when someone tries to bypass the screen.
- Master-private rows remain hidden from non-Master in addition to the Branch restriction.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 11:44 IST — Wrong-RMP payment prevention

- Found a user-error risk: while viewing one RMP, someone could enter a patient already assigned to
  another RMP and reach the Pay form. The database would correctly credit the assigned RMP, but the
  screen context could mislead the user.
- Android and Web now lock that Pay form and clearly show the patient's actual RMP. RMP must first be
  changed through the approved reassignment workflow.
- This prevents accidental payment under the wrong visible RMP without changing any saved data.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## Verification note — SQL parser limitation

- Attempted an additional independent PostgreSQL parser check.
- That parser does not support PostgreSQL's `FORCE ROW LEVEL SECURITY` statement and stopped there;
  therefore this attempt is **not** recorded as a SQL pass or as a product failure.
- The migration's own structural checks still pass, but actual execution remains reserved for the
  controlled Supabase test step.

## 2026-08-12 11:48 IST — Stale approval and future-date protection

- Found a timing risk: a Staff/Doctor request could be valid when sent, but another commission payment
  could reduce Unpaid before Master approved it. Approval now recalculates the latest balance under a
  database lock and refuses the stale request if it would exceed the current allowed amount.
- The same approval-time recheck now protects both backdated payments and payment corrections.
- Payment correction locks the patient's commission row while checking the allowed amount, preventing
  two simultaneous Staff/Doctor corrections from jointly exceeding Unpaid.
- Added database-level rejection of future commission/payment dates. The approved rule supports today
  and controlled old dates, not future-dated money records.
- These safeguards do not alter an already-saved patient Payment or existing Referral record.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 11:58 IST — Stale RMP/commission request protection

- Commission-change requests now carry the exact old mode, value and date seen by Staff/Doctor.
  Master approval first verifies those values are still current; otherwise the old request stops.
- RMP-reassignment requests now carry the old RMP id. Approval verifies the patient still belongs
  to that RMP before moving the remaining/future commission.
- This prevents a delayed approval from silently overwriting a newer Master decision.
- Android, Web and the protected database functions use the same old-state checks.
- Time-log correction: the preceding entry labelled `11:48 IST` was actually verified at
  `11:57 IST`. Per the append-only rule, the original line was not silently rewritten.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:00 IST — RMP reassignment arithmetic proof

- Re-ran the selected option-1 example: earned ₹1,000, already paid ₹600 to old RMP.
- After reassignment, old RMP retains ₹600 actually received; new RMP shows ₹600 as Previous RMP Paid;
  patient Unpaid remains ₹400 and does not become ₹1,000 again.
- This was a deterministic arithmetic/static proof. Controlled database execution is still pending.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:01 IST — Android verification retry (not passed)

- Re-attempted the real Android unit-test task from the project itself.
- Gradle 8.5 is not installed in this workspace and its official download address is unreachable
  from this environment, so the task stopped before compilation. This is **not** recorded as a pass.
- Also attempted a separate Kotlin parser; its native component could not be installed in this
  restricted environment. That attempt is likewise not recorded as a pass or as an app failure.
- No project file was changed by either failed verification attempt.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:03 IST — Temporary PostgreSQL execution proof

- Built a disposable in-memory PostgreSQL-compatible test database with only empty test tables;
  no live Supabase connection or owner data was used.
- The full V325 migration executed successfully there. Only the environment-specific `pgcrypto`
  installation line was skipped because the temporary engine does not ship that extension;
  its built-in UUID generator was present. All V325 tables, policies and functions were executed.
- Executed the actual protected summary/payment/edit functions, not a copied formula:
  - Final Bill ₹25,000, treatment paid ₹27,000, 10% => ₹2,500; Visit Fee/Medicine excluded.
  - Fixed ₹3,000, Final Bill ₹30,000, paid ₹5,000 => ₹500.
  - Fixed ₹3,000, Final Bill ₹10,000, approved refund leaves ₹8,000 => ₹2,400.
  - Commission Payment ₹900 created linked `RMP Commission Payment` Expense ₹900.
  - Master-private correction to ₹800 changed both Commission and Expense to ₹800 and marked
    the Commission Payment private.
- These temporary database proofs passed. They strengthen static verification but do not replace
  controlled Supabase execution, Android Build or real-device UI testing.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:06 IST — Approval SQL defect found, corrected and re-proved

- The temporary role/approval test found a real pre-release SQL defect: a temporary Payment record
  variable and a Patient query alias used the same short name, which stopped approval-request creation.
- No live migration has been run, so no owner data was affected.
- Renamed the two values clearly and found/removed the same ambiguity in the RMP Summary query.
- Recreated the disposable database from zero and repeated the affected tests:
  - stale approval was rejected using the latest Unpaid;
  - the failed approval rolled back and left the request Pending for Master review;
  - Master Reject changed it to Rejected without adding money;
  - Staff own-Branch summary worked; another Branch read/write was rejected;
  - Staff above-Unpaid payment was rejected; Field Officer access was rejected;
  - functional RMP reassignment returned old RMP paid ₹600, new RMP previous-paid ₹600,
    new RMP direct-paid ₹0 and patient Unpaid ₹400.
- All repeated temporary database tests above passed after the correction.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:07 IST — Refund classification and Final Bill function proofs

- Ran actual temporary database summaries with four refund types together.
- Approved refund linked to Treatment reduced net treatment paid.
- Approved refund linked to Visit Fee did not reduce treatment commission.
- Pending and Rejected refunds did not reduce treatment commission.
- With Treatment ₹10,000 and approved linked Treatment Refund ₹2,000, the protected summary returned
  net treatment paid ₹8,000 and 10% earned ₹800.
- Tested fixed Amount recalculation directly: Final Bill ₹10,000, paid ₹5,000, fixed ₹3,000 returned
  ₹1,500; after Final Bill changed to ₹20,000, the next summary automatically returned ₹750.
- Both temporary function tests passed; live Supabase verification remains pending.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:08 IST — Newer Master decision preservation proof

- Created real temporary Staff requests for an earlier commission change and an RMP reassignment.
- Master then changed the patient's commission from 10% to 20% and reassigned the patient from RMP-1
  to RMP-3 before deciding those requests.
- Both stale requests were rejected by the protected approval function.
- The latest Master decision remained exactly RMP-3 and 20%; neither old request overwrote it.
- Temporary function test passed. Live Supabase verification remains pending.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:11 IST — Direct RLS and private-history visibility proof

- The first direct-table test exposed two self-containment dependencies: explicit authenticated use
  of the `fin` schema and direct policy reading of the old Doctor Visit table.
- Added explicit authenticated schema use and moved RMP Branch lookup into a protected helper;
  anonymous/public table access remains revoked.
- Recreated the disposable database and tested while acting as the actual `authenticated` role:
  - Kishanganj Staff saw only Kishanganj patient commission and RMP Default;
  - Jalpaiguri commission rows were filtered out;
  - Staff saw the normal Kishanganj Commission Payment but not the Master-private payment;
  - Staff saw zero private audit rows;
  - Master saw both Branches, all three test Commission Payments and all eight audit rows.
- The repeated direct RLS/private visibility test passed.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:12 IST — Temporary Web workflow render proof

- Loaded the isolated Web commission script in a disposable browser-like DOM with fake patient/RMP
  and protected-function responses; no live website or Supabase data was used.
- Referral Income menu, Patient Commission form, verified Pay form, Payment History and
  Master-private Edit form all rendered with their required controls and values.
- Pay form displayed Earned ₹500, Paid ₹100 and Unpaid ₹400 from the simulated protected summary.
- History displayed the normal ₹100 payment and opened its edit/private/reason controls.
- Every Web JavaScript file passed syntax validation again after the test.
- This proves the main Web event/render path does not immediately crash; real mobile layout and live
  network behavior still require controlled testing.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:13 IST — Safe second-run migration proof

- Executed the V325 migration twice in the same disposable database after creating one patient
  commission, one Commission Payment and its linked Expense.
- Before and after the second run, row counts remained exactly 1 patient commission, 1 commission
  payment and 1 Expense; no duplicate and no deletion occurred.
- The protected summary still returned ₹400 Unpaid after the second run.
- Temporary second-run test passed. The migration remains additive and data-preserving in this test.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:14 IST — Supabase Free Plan lookup protection

- Confirmed the existing public Payment ledger had no patient-row lookup index, while every commission
  summary filters that ledger for exactly one patient.
- Added small, additive lookup indexes for public Payment by patient, Commission Payment by RMP and
  Pending Approval status/date. No data row, formula, download or screen was changed.
- This avoids repeated full-ledger scans as Payment history grows and reduces database work for the
  Free Plan; it does not add photo/full-list downloads or background polling.
- Re-executed the migration in a disposable database and verified all three indexes were created.
- SQL now contains 14 balanced functions after the protected RMP-Branch helper addition.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:28 IST — Atomic Commission/Expense failure proofs

- Added temporary test-only database triggers that deliberately failed Expense insert and Expense update.
- When Expense insert failed, the protected Commission Payment call rolled back fully: zero Commission
  Payment and zero payment-audit row remained.
- After a valid ₹100 Cash Commission/Expense pair was created, Expense update was deliberately failed
  during an attempted change to ₹200 Online.
- The whole edit rolled back: Commission remained ₹100 Cash and Expense remained ₹100 Cash.
- No test trigger or temporary row exists in the project migration; these were disposable test fixtures only.
- Atomic failure tests passed. Live Supabase verification remains pending.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:29 IST — Default snapshot and late-setting proof

- Created two test patients who had already deposited ₹5,000 treatment money before commission setup.
- RMP Default 10% was snapshotted to the first patient; changing the Default to 20% did not alter
  that existing patient, whose earned commission remained ₹500.
- The second patient then copied the new 20% Default and earned ₹1,000.
- Both patients included the treatment deposit made before commission was set.
- Temporary protected-function test passed; live activation/warning UI remains pending.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:29 IST — Partial payment and separate-overpaid proof

- For ₹500 earned commission, recorded two partial payments ₹200 Cash and ₹100 Online with optional
  reference: summary returned Paid ₹300, Unpaid ₹200 and Overpaid ₹0.
- Master then recorded ₹600 more: summary returned total Paid ₹900, Unpaid ₹0 and Overpaid ₹400.
- Three linked `RMP Commission Payment` Expenses totalled the same ₹900.
- Temporary protected-function test passed; no Settle action was added because the owner left that
  decision for after real implementation/testing.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:30 IST — Clear Pending Approval labels

- Found a display-only ambiguity: non-payment requests in Pending Approval were shown using the
  payment-style ₹0/date label.
- Android and Web now label each request by its real meaning: Backdate Payment, Payment Correction,
  Percent/Fixed Amount Change or Patient RMP Change.
- No approval, money, role, database row or shared design was changed.
- All Web JavaScript syntax checks and Android source brace checks passed after the label correction.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:38 IST — Comprehensive Staff/Master workflow proof

- Ran one connected temporary database workflow as Master and Kishanganj Staff.
- Staff another-Branch patient change and above-Unpaid payment were rejected.
- Staff same-day correction changed both Commission and linked Expense to ₹150 Online in Kishanganj.
- A backdated Staff payment stayed Pending with no money change; Master approval added it once and a
  second approval was rejected. A separate rejected request changed no money.
- After Master-private edit, Staff history exposed only the normal row while Staff summary still used
  the private-adjusted total Paid ₹300 and returned Unpaid ₹200.
- Earlier commission stayed 10% until Master approved the Staff request, then became 20%.
- Staff RMP reassignment stayed a request; after Master approval, the next Commission Payment stored
  new RMP id `R2` and name `RMP TWO`.
- The comprehensive temporary protected-function workflow passed.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:39 IST — Old payment correction and patient override proof

- Set patient-specific commission to 15% while RMP Default remained exactly 10%.
- Created a ₹100 Cash Commission/Expense pair on an old date.
- Staff direct old-date edit was rejected; Staff PAYMENT_EDIT request remained Pending and left both
  Commission and Expense unchanged at ₹100 Cash.
- Master approval changed both together to ₹150 Online.
- Temporary protected-function test passed.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:41 IST — Remaining live-test classification audit

- Confirmed Android activates commission only after cloud Payment success and catches every commission
  failure separately; a missing Default cannot put the already-saved Payment back into Pending.
- Confirmed Android and Web display patient/RMP amounts from the same protected database summary
  functions rather than maintaining two independent formulas.
- Confirmed commission sources contain no photo/full-table download, timer or background polling.
- Confirmed both platforms retain an explicit Previous Records route to the unchanged legacy Referral
  workflow. Actual opening remains an Android build/device test and is not marked passed.
- Split code-proven checks from genuine live/build checks in the checklist without overstating either.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:41 IST — Internal version alignment

- Aligned the Android release identity with the current approved work: `versionCode 325` and
  `versionName 3.25`.
- Rechecked version declarations; there is one active Android version source and it now reports V325.
- Kept `V324_RMP_SELECTION_REPORT_BANGLA.md` unchanged because it is the historical record of the
  earlier RMP-selection work; relabeling it would make the audit history inaccurate.
- Outer folder/ZIP naming was not changed and no package was created or delivered.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:42 IST — Version and change-boundary recheck

- Web JavaScript syntax passed again using the correct `03_NETLIFY_READY` paths.
- The first attempted Web syntax command used a non-existent `03_WEB_APP` path; this was a test-command
  path mistake, not an application failure. It made no file change.
- SQL foundation still has 28 balanced dollar-quote markers.
- Compared the working tree with the pristine V323 copy. Existing-file changes remain confined to the
  approved RMP selection/commission integration points plus Android version identity.
- Refund, Receipt and unrelated layout source files were not directly modified.
- Android build/device checks and controlled live Supabase checks remain genuinely outstanding; they
  are still marked `[B]` and `[L]` and have not been represented as passed.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:44 IST — Old-work preservation and label correction

- Reviewed every removed/replaced line in the existing integration files.
- Confirmed Payment Pending remains queued on failure; commission activation runs only after confirmed
  cloud success and remains isolated from the original payment/retry result.
- Confirmed the former Referral Income form was retained as `Previous Records`; it was not deleted.
- Corrected two comment-only version labels from V323 to V324 so they match the historical RMP-selection
  report. No behavior, design, database action or user-visible text changed.
- Web JavaScript syntax passed after the comment correction.
- Work status remains: **NOT COMPLETE — DO NOT PACKAGE OR DELIVER**.

## 2026-08-12 12:49 IST — Final locally-possible verification and handoff state

- Android Gradle Build was attempted again. It stopped before compilation because this environment
  could not download the required official Gradle 8.5 distribution and had no cached/system Gradle.
  This is recorded as an environment blocker, not represented as an app build pass or code failure.
- Parsed all 278 Android resource XML files successfully.
- Lexical delimiter check passed for all 7 changed Kotlin integration files.
- Web JavaScript and SQL structure checks passed again.
- Added a Bengali read-first handoff note stating the exact remaining owner steps and prohibiting
  guessed/repeated live changes.
- Source work is ready for controlled Build and Live Test. Package must be named `BUILD_PENDING`,
  never `FINAL`, until those two external proofs pass.

## 2026-08-12 12:55 IST — Live Supabase foundation installed

- Owner supplied screenshot proof that the complete V325 foundation SQL reached line 580 and finished
  with `Success. No rows returned`.
- No SQL error was shown. The required commission schema, protected functions, grants and RLS foundation
  are therefore installed in the owner's Supabase project.

## 2026-08-12 12:57 IST — Read-only Supabase verification completed

- Owner supplied screenshot proof that the read-only verification query completed without an error.
- Its final permission result was `authenticated_fin_usage = true`.
- No more SQL is required for this V325 installation. Android Build and controlled mobile Live Test
  remain pending and must not be represented as passed before the owner performs them.

## 2026-08-12 12:58 IST — Owner-authorized Android Studio handoff

- Owner explicitly requested the complete project package after the chronological work log was updated.
- Package remains honestly labelled `BUILD_PENDING` because this environment could not perform the
  Gradle compilation. All locally possible structural checks passed before packaging.

## 2026-08-12 13:20 IST — Owner Build/install/dashboard proof

- Owner supplied a real-device screenshot of the native Android home screen showing `Synced · V325`.
- This proves the V325 project built into an installable app, launched successfully and reached the
  Master dashboard on the owner's phone.
- Visible existing dashboard cards and header remained present in the supplied screenshot.
- RMP Commission workflow, role/Branch behavior, calculation, Refund and linked Expense still require
  controlled one-at-a-time Live Test before the release can be called final.

## 2026-08-12 13:22 IST — Referral Income menu live proof

- Owner supplied a real-device Master screenshot for Dr. JAFAR in Kishanganj.
- The V325 Referral Income menu opened without a visible error and displayed all five intended routes:
  RMP Default Commission, Patient Commission / Payment, Commission Summary, Previous Records and
  Pending Commission Approvals.
- The preserved Previous Records route remains visibly available.
- No financial Save was requested or performed during this screen-only check.

## 2026-08-12 13:25 IST — RMP Default screen live proof

- Owner supplied a real-device Master screenshot of `RMP Default Commission — Dr. JAFAR`.
- The screen opened without a visible error and showed Commission Type, Default Value, Cancel and
  Save Default controls.
- Live cloud read completed and correctly displayed `No Default set yet`.
- Nothing was saved during this screen-only check; the Default type/value remains an owner decision.

## 2026-08-12 13:26 IST — RMP Default 10% live save proof

- Owner entered Percent mode with Default Value `10` for Dr. JAFAR and supplied before/after screenshots.
- The Android app closed the form and displayed `Default commission saved` with no visible error.
- This proves the Master save action returned success. A reopen/read check is required next to prove the
  saved 10% value is returned from cloud rather than relying only on the success message.

## 2026-08-12 13:27 IST — RMP Default cloud persistence proof

- Owner reopened Dr. JAFAR's RMP Default screen on the real Android app.
- The screen returned Percent mode, `10.0` and `Current Default loaded` from the live system.
- Dr. JAFAR's 10% Default is therefore proven saved and readable from cloud, not merely acknowledged by
  a temporary success message.

## 2026-08-12 13:29 IST — Remaining Referral routes screen proof

- Owner supplied four real-device Master screenshots for Dr. JAFAR.
- Patient Commission / Payment opened with `Use RMP Default`, patient mobile lookup, Pay and Save
  Commission controls; no patient was selected or saved.
- Commission Summary correctly returned Patients 0, Earned ₹0, Paid ₹0 and Unpaid ₹0 before any patient
  commission existed.
- Previous Records opened the preserved legacy `Add Referral Income` form with its former Unpaid and
  Payment Mode fields; no legacy record was saved.
- Pending Commission Approvals correctly returned `No pending commission approval`.
- These were read/screen checks only and created no financial or approval row.

## 2026-08-12 13:31 IST — No referred patient available for money test

- Owner confirmed Dr. JAFAR has not yet sent any patient.
- Therefore the live Summary result Patients 0 / Earned ₹0 / Paid ₹0 / Unpaid ₹0 is consistent with
  the real business state.
- No fake patient, fake treatment payment, fake commission or fake expense will be created merely to
  force a live test. Patient/payment/refund calculations remain pending until a genuine case exists.

## 2026-08-12 13:32–13:34 IST — Registration saved-RMP picker live proof

- Owner opened native Patient Registration, selected `DR. VISIT` and opened `Select Saved RMP / Doctor`.
- The cached selector opened without a visible error and reported 1000 locally saved RMP entries for
  the Master/All-Branches context; this selector made no extra live cloud search.
- Searching `jafar` reduced the list to three matching saved RMPs.
- Selecting the first result populated `DR MD JAFAR ALAM` and mobile `9614608531` into the existing
  registration fields automatically.
- This selection was not Dr. JAFAR (the RMP whose 10% Default was tested). No patient was saved, so no
  patient/referral/fee record was created. Owner was instructed to cancel the temporary `TEST FOR RMP` form.

## 2026-08-12 13:36 IST — Exact mobile RMP search live proof

- Owner searched saved RMPs by the exact mobile `8617597893` on the real Android Registration screen.
- The local selector returned exactly one result: Dr. JAFAR, GODASIMAL, Kishanganj, with the matching
  saved mobile number.
- This proves both approved search routes—name and mobile—work from the phone's saved RMP cache without
  creating a patient or making a commission/payment change.

## 2026-08-12 13:39 IST — Owner-approved saved-RMP visual refinement

- Owner reported that the working saved-RMP list looked too plain/white and that its combined text was
  visually confusing, then explicitly requested a more professional design.
- Limited the change strictly to the native Registration saved-RMP popup list.
- Each RMP is now presented as a separate soft white/green card: bold name, separate mobile line, green
  Area tag and blue Branch tag, on a lightly tinted background with clearer spacing.
- Search, local-cache behavior, Manual Entry, selection, auto-fill, Registration fields, cloud requests,
  commission logic and every other screen remain unchanged.
- Because the already-built V325 was materially changed after owner device testing, advanced the Android
  identity to `versionCode 326` / `versionName 3.26` to prevent two different builds sharing V325.

## 2026-08-12 13:41 IST — Small-screen RMP card safety check

- Changed Area and Branch tags from side-by-side to separate lines so long saved areas/branches cannot
  collide or disappear on the owner's phone width.
- Android resource parsing, Registration Kotlin structure, Web JavaScript and V326 identity checks passed.
- No SQL, Supabase function, commission calculation, Registration save rule or other screen was changed.

## 2026-08-12 13:55 IST — Owner approved white professional proof

- Owner rejected the first two visual proofs as insufficiently professional, accepted the third layout,
  then requested that the same layout remain white instead of black/dark.
- Owner approved the revised white proof before project delivery.
- Applied that approved structure to the native picker: white popup/list, separate white cards, green
  left accent, bold name, blue Branch label, then mobile and area on their own clear lines.
- No project file was delivered before this approval. Search, selection and all business logic remain unchanged.

## 2026-08-12 14:12 IST — Owner-approved Prescription Details boxes

- Owner identified that tapping a Prescription timeline Note opened a confusing `Note / Remark` popup
  with all medicines in one paragraph.
- After four visual proofs, owner approved the white boxed layout and explicitly removed the labels
  `How often`, `Use`, `Dose`, `Direction` and `When` while keeping the information boxes.
- Applied the approved display only to timeline rows whose type is Prescription. Each semicolon-separated
  medicine is a separate numbered white card; its saved directions are shown in plain green/blue boxes.
- Stored Prescription text, Prescription creation/edit/print, Payment, Registration, other timeline Note
  types and database/cloud behavior remain unchanged.
- Advanced Android identity to `versionCode 327` / `versionName 3.27` because V326 was already delivered.

## 2026-08-12 14:15 IST — Payment summary dummy hints removed

- Owner explicitly ordered removal of the visible helper text `3-tap: edit`, `tap: history` and
  `tap: refund` beneath Bill, Paid and Due on Add Treatment Payment.
- Removed only those three visible hints from native Android and the matching Web payment form.
- Bill edit, Paid history and Due refund tap actions remain wired exactly as before; amounts, payment
  calculation, Refund, buttons and layout structure were not changed.
## 2026-08-12 14:51 IST — Dr. Visit/RMP Branch নির্দেশনার ডেমি লেখা বাদ

- Branch নির্বাচন না-করা অবস্থায় দেখা যেত: `উপরে ব্রাঞ্চ বাছাই করুন RMP লিস্ট দেখতে`।
- শুধু এই নির্দেশনাটি লুকানো হয়েছে।
- Branch selector, Search, চারটি হিসাবের কার্ড, RMP Performance Report, RMP list এবং অন্য সব ডিজাইন/কাজ অপরিবর্তিত রাখা হয়েছে।
- Branch বাছার পরে তালিকা সত্যিই খালি হলে আগের `No doctors found` বার্তা অপরিবর্তিত থাকবে।

## 2026-08-12 14:56 IST — V327 final handoff verification

- Active Android identity rechecked: `versionCode 327` / `versionName 3.27`.
- 278 Android XML resources parsed successfully, 223 Kotlin sources passed the local structural scan, and Web JavaScript syntax checks passed.
- A full Gradle build was attempted. It could not start because this controlled environment was unable to download Gradle 8.5; no project compilation error was reported before that network stop.
- Final package must therefore remain honestly named `V327_FINAL_BUILD_PENDING` until Android Studio completes the build on the owner's computer.
- Owner's instruction remains locked: no other design, workflow or existing function was authorized for modification.

## 2026-08-12 16:37 IST — V328 cautious loading investigation and safe Reports improvement

- Owner authorized loading improvements only after a no-guesswork investigation, with every existing design, workflow, calculation and Supabase Free Plan safety preserved.
- Follow-up was inspected first. Its older duplicate-download problem is already protected by the existing 20-second `CloudReadCache`; concurrent identical reads share one cloud answer, failures are never cached, and every successful write clears that cache. No Follow-up code was changed because changing its exact-count/Branch behavior without live comparison would add risk.
- Briefing startup sections were verified to run independently; they do not block the cached main notice list. No Briefing workflow or approval visibility was changed.
- Patient Timeline already starts its independent reads in parallel and uses its existing saved screen cache. No medical/financial query was removed.
- RMP's active heavy patient/payment detail path was already parallel. The separate sequential RMP block found later is documented dead code and was deliberately not changed.
- Two active, proven sequential waits remained in Reports and were safely parallelized:
  1. Staff Detail now starts its existing Enquiry, Patient and Payment reads together.
  2. Main Report now starts its existing RMP summary read together with the existing Enquiry, Patient and Payment reads.
- Query count, filters, limits, selected columns, Refund arithmetic, Branch rules, role visibility, UI and returned calculations remain unchanged. This adds no Supabase request, table, SQL, RPC, Realtime or Edge Function.
- Android identity advanced to `versionCode 328` / `versionName 3.28` because V327 had already been delivered.
- Reports Kotlin structural checks and all 278 Android XML resource parses passed. Full Android Gradle compilation still requires Android Studio because this environment cannot download Gradle.

## 2026-08-12 16:52 IST — RMP safe-speed investigation: live-data proof gate

- Owner authorized cautious RMP safety/performance work but prohibited guesses, design changes and risk to any existing workflow.
- Confirmed the new Patient Commission, Commission Payment and Commission Summary screens already use small authenticated `fin` lookups/functions rather than downloading the full patient/payment ledgers.
- Confirmed the remaining heavy legacy paths: the RMP card count can download up to 5,000 patient rows, while View All / Performance detail can download up to 5,000 patient plus 5,000 payment rows.
- Found an important compatibility point that must be proved against live data before filtering: current Registration stores `refBy = Dr. Visit` and stores the selected RMP separately in `refDoctor` / `refDoctorMobile`; the legacy RMP list primarily succeeds through the saved RMP mobile. Old or incomplete rows must therefore be measured before any matching rule is replaced.
- Added `V328_RMP_READ_ONLY_DATA_MATCH_AUDIT_2026-08-12.sql`. It contains SELECT statements only and cannot add, edit or delete a patient, payment, RMP, commission or permission.
- No Android screen, design, calculation, cloud request, SQL function or live database was changed at this stage. The audit result is the mandatory gate before choosing a filtered read or server summary.

## 2026-08-12 17:00 IST — First read-only audit copy corrected

- Owner's first SELECT-only audit attempt returned PostgreSQL error `42703: column ref_name does not exist` and changed no data.
- Cause was in the supplied audit query: the intermediate match result did not carry `ref_name` and `ref_mobile` into the final count.
- Corrected only that read-only query projection/grouping. No database or Android behavior was changed.

## 2026-08-12 17:01 IST — Second owner run still rejected; simplified audit required

- Owner's second read-only run again returned `42703: column ref_name does not exist`; no data was changed.
- To remove this failure point completely, the owner-facing audit was rewritten so the final result never references the intermediate `ref_name` or `ref_mobile` columns. Those checks are now counted inside their source CTE/subqueries.
- This remains SELECT-only. No application, table, function, permission or business record was changed.

## 2026-08-12 17:03 IST — Live RMP matching audit result received

- Owner exported the successful Supabase read-only result as CSV.
- Live counts: 34 RMP-referred patients; 30 have an RMP name; 31 have a valid 10-digit RMP mobile; 23 match exactly one saved RMP; 10 match none; 1 matches multiple saved RMPs.
- Current table sizes reported by the same read-only result: 1,886 saved RMP rows, 208 patient rows and 738 payment rows.
- Safety decision: a direct filtered replacement is blocked because it could omit or misassign 11 existing RMP patients. No app query or calculation was changed.
- Next gate is a SELECT-only detail report for those 11 rows, followed by an owner decision on any data correction; no automatic cleanup is allowed.

## 2026-08-12 17:05 IST — Eleven RMP identity exceptions classified

- Owner supplied the SELECT-only problem-row CSV; it contains 11 rows exactly.
- Ten patients have no exact saved RMP match. Two of those have neither RMP name nor mobile; the other eight contain a manually/previously entered name or mobile that is absent from the current saved RMP list. No automatic reassignment is safe.
- One patient (`COB-07082026-003`) has referring name `JAKIR HOSSAIN`, shared by four saved RMP rows, but also has exact mobile `7407407675` matching the Cooch Behar RMP.
- Project code was rechecked: commission activation tries exact 10-digit mobile first and only falls back to name when mobile finds nothing. Therefore that patient's current commission activation selects the exact mobile row, not an arbitrary same-name row.
- The current legacy RMP card count also matches that row by exact mobile. The ten unmatched patients are already not attributable to a saved RMP card; changing them requires an owner/business decision, not a performance optimization.
- Safe optimization direction remains: reproduce the current mobile-priority result in a separate server-side read summary, compare it with the unchanged old result, and retain the old path as fallback until proven equal.

## 2026-08-12 17:10 IST — Current RMP card rule isolated for proof

- Deeper code comparison found that two existing workflows intentionally use different historical fields: RMP list cards use `patients.refBy` or `patients.refDoctorMobile`, while new commission activation uses exact RMP mobile first and then `patients.refDoctor` name.
- Combining these into one guessed matcher would change old counts, so that approach was rejected.
- Added `V328_RMP_LEGACY_COUNT_READ_ONLY_PROOF_2026-08-12.sql`, a SELECT-only server calculation that reproduces the current RMP card count rule without altering the new commission workflow.
- It creates no function/view/table/index, writes no data and is not wired into Android. Its result must be checked before any permanent Server Summary SQL is proposed.

## 2026-08-12 17:18 IST — RMP card-count proof passed; additive foundation prepared

- Owner returned the read-only server-count CSV. It contains 12 RMP rows with a combined 24 referred patients: JH MANDAL 7, PK 5, PKB 2, PRANAB GHOSH 2 and eight RMPs with 1 each.
- JAKIR HOSSAIN correctly appears once for exact mobile `7407407675`; the same-name duplicates did not inflate the current legacy card result.
- Prepared `V328_RMP_SAFE_CARD_COUNTS_FOUNDATION_2026-08-12.sql`. It adds one authenticated, read-only-in-effect function returning only RMP id + count; it changes no business row and is not connected to Android yet.
- Master preserves current all-patient counting. Non-Master identity derives its active branch from the protected staff profile and fails closed if no active branch exists; blank legacy patient branch remains included to match current Android.
- Public/anon execution is explicitly revoked. Android wiring remains blocked until the function is run, privilege proof passes and authenticated live output is compared.

## 2026-08-12 17:58–18:00 IST — Secure function proof passed and fail-safe Android wiring

- Owner ran and saved the additive function SQL. Supabase proof returned `authenticated_can_call = true` and `anon_can_call = false` exactly as required.
- Android RMP card counts now first request the tiny authenticated `rmp_legacy_card_counts` result (RMP id + positive count only) instead of normally downloading up to 5,000 patient rows.
- Fail-safe preservation: if identity, authentication, network, permission, server response or JSON validation fails, the unchanged old patient-row count runs automatically. No guessed zero replaces an unverified result.
- This phone's not-yet-cloud RMP overlay is also protected: if such an RMP exists, the old patient read is retained for that local row while verified cloud RMPs use the server result.
- New commission Summary/Payment matching, old View All/Performance detail, Branch picker, list cards, designs and every count formula remain unchanged.
- No new Realtime, background refresh, table, trigger or repeated polling was added. One normal patient-ledger download is replaced by one tiny authenticated function response in the successful path.
- Android identity remains `versionCode 328` / `versionName 3.28`; V328 had not been delivered, so no duplicate version identity was created.
- Full Gradle test could not start because the controlled environment cannot download Gradle 8.5. No compilation error was reached. Both edited Kotlin files passed structural balance checks and all 278 Android XML resources parsed successfully.

## 2026-08-12 18:05 IST — View All/Performance payment-link risk found before implementation

- Investigation of the still-heavy View All/Performance detail found that its current Paid calculation groups every payment by normalized patient mobile, not by internal Patient ID.
- Therefore duplicate patient mobiles or blank/invalid mobiles can mix payment totals between patients. Replacing this path without measuring those live exceptions could preserve or amplify a wrong result.
- The app also caps both patient and payment downloads at the newest 5,000 rows. An uncapped server query would silently change future totals, so any proof must mirror those caps first.
- Added SELECT-only `V328_RMP_VIEW_ALL_MOBILE_RISK_READ_ONLY_2026-08-12.sql` to count duplicate/blank referred-patient mobiles, invalid/blank payment mobiles, invalid amounts and Refund states. No app or live database was changed.

## 2026-08-12 18:10 IST — View All mobile/refund risk gate passed

- Owner returned the live risk CSV: 24 referred patients; zero invalid referred mobiles; zero duplicate referred mobiles; zero invalid payment mobiles; zero invalid amounts; 10 approved Refund rows; zero pending/rejected Refund rows.
- This proves the current mobile-grouped Paid calculation has no collision/blank-key exception in today's live data. It does not authorize silently changing the historical mobile-based rule.
- Active Performance Report was rechecked separately: it downloads patient rows only; the later patient+payment block is dead code and remains untouched.
- Prepared SELECT-only `V328_RMP_VIEW_ALL_DETAILS_READ_ONLY_PROOF_2026-08-12.sql`. It mirrors the current 5,000-row caps, RMP match, mobile-based Paid and approved-Refund subtraction and returns the 24 detailed rows for final comparison before any function/app change.

## 2026-08-12 18:15 IST — Detailed View All proof received; four legacy values escalated

- Owner returned all 24 detailed referred-patient rows. RMP assignment totals reconcile with the earlier 12-RMP count proof.
- Four current legacy View All values require owner awareness before activation: SUJIT DEBNATH Bill ₹0 / Paid ₹5,000; SUSANTO SARKAR Bill ₹0 / Paid ₹5,000; CHAYAN ROY Bill ₹0 / Paid ₹2,000; Biplob Bonik Bill ₹23,500 / Paid ₹33,500.
- These values were produced by the existing View All rule and faithfully reproduced by the read-only server proof; they are not introduced by the proposed optimization.
- No amount or patient row was changed. Permanent View All server function and Android wiring are paused until the owner decides whether to preserve the current displayed values or investigate those four records first.

## 2026-08-12 18:18 IST — Owner ordered four-record investigation before decision

- Owner explicitly chose read-only investigation first and prohibited any correction/implementation without a later permission.
- Prepared `V328_FOUR_RMP_PATIENTS_PAYMENT_READ_ONLY_2026-08-12.sql`, limited to the four identified Patient Codes and the same newest-5,000 Payment cap used by current View All.
- It reports every mobile-matched payment/refund, its type/label/amount/mode/status/original link, whether Payment patientId matches the internal row or official code, and its exact current View All effect.
- SELECT only; no project/live data change was made. Optimization remains paused.

## 2026-08-12 18:22 IST — Four-record payment investigation result

- Owner returned the full Payment CSV. Every listed Payment has `INTERNAL ID MATCH`; no payment from another patient was mixed by the mobile lookup in these four cases.
- SUJIT DEBNATH: Bill ₹0; Visit Fee ₹500 correctly excluded; linked Treatment Advance ₹5,000 produces current Paid ₹5,000.
- SUSANTO SARKAR: Bill ₹0; Visit Fee ₹400 correctly excluded; linked Treatment Advance ₹5,000 produces current Paid ₹5,000.
- CHAYAN ROY: Bill ₹0; Visit Fee ₹400 correctly excluded; linked Treatment Advance ₹2,000 produces current Paid ₹2,000.
- Biplob Bonik: Bill ₹23,500; Visit Fee ₹400 correctly excluded; twelve linked Treatment payments total ₹33,500; no Refund row exists in the returned history.
- Conclusion: these are not cross-patient matching errors introduced by the proof. They reflect missing/outdated Bill values versus real linked treatment collections. No Bill, Payment or Refund was edited. Owner decision is required before optimization resumes.

## 2026-08-12 18:25 IST — Owner approved preserving actual collections

- Owner accepted the safe recommendation to preserve the four patients' real linked collection values exactly as currently recorded/displayed. No Bill or Payment correction was authorized.
- Prepared additive `V328_RMP_SAFE_VIEW_ALL_FOUNDATION_2026-08-12.sql`. It returns only the selected RMP's referred patient details while reproducing the current newest-5,000 caps, mobile-based Paid, Visit Fee exclusion and approved Refund subtraction.
- It explicitly allows Paid above Bill and Bill ₹0 because changing those values would violate the owner's preservation decision.
- Authenticated role and branch are validated inside the function; public/anon execution is revoked. It changes no business row and is not wired into Android until live privilege proof passes.

## 2026-08-12 18:19–18:21 IST — View All security proof passed and fail-safe Android wiring

- Owner's Supabase screenshot proved `authenticated_can_call = true` and `anon_can_call = false` for `fin.rmp_legacy_view_all(text)`.
- Android View All now normally receives only the selected RMP's verified patient details from that function instead of downloading up to 5,000 patients plus 5,000 payments.
- Any identity, login, network, permission, server or parsing failure automatically runs the untouched old two-download calculation. Successful empty data remains distinguishable from failure, so no unverified empty/zero is shown.
- The function result is rejected if Patient ID is blank/duplicated, Bill is invalid/negative, or Paid is invalid. Paid above Bill remains valid by the owner's explicit decision.
- Call History and old Referral Income still come from the selected doctor row exactly as before. View All layout, buttons, navigation, messages, ordering, Bill/Paid values and Refund treatment are unchanged.
- All 278 Android XML resources parsed and both edited Kotlin files passed structural checks. Gradle compilation could not start because this environment cannot download Gradle 8.5; no source compilation error was reached. Android identity remains V328 because it has not been delivered.

## 2026-08-12 18:30 IST — Master Performance investigation found branch-edge mismatch

- Confirmed the Performance button is Master-only and its active repository downloads only doctor rows + patient detail rows; it does not download the full Payment ledger. Referral Paid comes from each existing doctor row.
- Found a pre-existing branch edge: the main RMP card count includes legacy patients whose branch is blank, but Performance's branch-specific PostgREST filter fetches only exact-branch patients, so blank-branch referrals can be omitted after Master selects a branch.
- No code was changed for Performance. Prepared SELECT-only `V328_RMP_PERFORMANCE_READ_ONLY_PROOF_2026-08-12.sql` to reproduce All-branch month/all-time/latest/referral-paid values and expose blank-branch matches before choosing exact preservation behavior.

## 2026-08-12 18:35 IST — Master Performance read-only proof passed

- Owner returned the live CSV. Twelve active RMPs have referrals; every `blank_branch_patients` value is zero, so the identified branch-edge mismatch has no current live affected patient.
- Proved representative current values: JH MANDAL 6 this month / 7 all-time / ₹11,500 Referral Paid; JAKIR HOSSAIN 1 / 1 / ₹5,000; PRANAB GHOSH 0 / 2 / ₹1,500.
- Prepared additive `V328_RMP_SAFE_PERFORMANCE_FOUNDATION_2026-08-12.sql`. It is Master-gated inside the function, validates the five approved Branch names, applies branch filtering before the same newest-5,000 limit, and returns only RMP id + four metrics.
- No Android Performance code is connected until the function's execution doorway and anonymous denial are proved live. Existing design/data remain unchanged.

## 2026-08-12 18:29–18:31 IST — Wrong saved SQL tab was run; no deletion performed

- The Supabase screenshot showed line 142 text from `V306_PARTNER_SHARES_PHASE1_2026-08-10.sql`, proving the old Partner Phase 1 query was run instead of the prepared 64-line RMP Performance function.
- The V306 file is idempotent: it creates/refreshes Partner foundation tables, their Master-only RLS policies, audit triggers and authenticated API grants. It contains no patient/payment/commission deletion and inserts no Partner data.
- No rollback/drop was attempted because removing those existing Partner objects would create unnecessary risk. The RMP Performance function remained unproved and Android Performance wiring remained paused.
- Prevention: owner was instructed to use a fresh Supabase query tab and paste only the separately supplied 64-line RMP Performance SQL.

## 2026-08-12 18:32–18:38 IST — Master Performance security proof passed and Android wiring added

- Owner ran the correct Performance function in a fresh query. Live proof returned `authenticated_has_doorway = true` and `anon_can_call = false`.
- Android Master Performance now normally downloads only the protected per-RMP metrics instead of up to 5,000 patient-detail rows.
- Strict result checks reject blank/duplicate RMP ids, impossible counts, more than the preserved 5,000 cap, invalid paid values and unknown doctor ids.
- On any login, permission, network, server, parsing or identity mismatch, the unchanged old patient-download calculation runs automatically; no guessed empty/zero report replaces a failure.
- Existing Performance dialog, branch picker, cards, ordering, labels, View All taps and every other module/design remain unchanged.

## 2026-08-12 18:38–18:45 IST — V328 final local verification

- Compared the current project with the uploaded V323 base. The remaining Guard warnings about locked message text and Bengali-disabled translations already existed in V323; they were not caused by the RMP work and were left untouched under the owner's no-unrelated-change rule.
- Renamed only two internal generic RMP result-holder class names to `RepoResult` and `ActivationResult`. This removes a Guard name collision with Android WorkManager `Result.success()`; values, call sites and behavior remain identical.
- Guard passed Kotlin-aware balance for 223 main Kotlin files, all 279 XML resources parsed, binding/drawable and Supabase-column checks passed, and Web JavaScript/JSON syntax checks passed.
- Confirmed Android identity `versionCode 328` / `versionName 3.28`.
- Full Gradle build remains unavailable because Gradle 8.5 cannot be downloaded in this environment. Final package must therefore remain honestly marked `BUILD_PENDING` until Android Studio builds it.
- Complete project package created as `PILES_CLINIC_APP_V328_FINAL_BUILD_PENDING.zip`. Its internal root folder has the same V328 name; ZIP integrity test reported no compressed-data errors, and the packaged Gradle file was reopened to confirm `328 / 3.28`.
## 2026-08-12 19:04 IST — Web RMP cache identity aligned with V328

- Owner authorized only the Web RMP version/cache identity correction.
- `03_NETLIFY_READY/index.html` now loads `rmp_commission.js?v=v328`, forcing browsers to fetch the current RMP commission file instead of retaining the V325 cached copy.
- The RMP file header now records the truthful history: introduced in V325, cache identity refreshed in V328.
- No RMP calculation, design, workflow, role, Branch rule, Supabase function, Android source or other Web module was changed.
- JavaScript syntax passed after the change; comparison against the previously delivered V328 package confirmed only these two approved text lines changed.

## 2026-08-12 19:10 IST — Permanent AI version/cache mistake-prevention lock

- Owner ordered a permanent simple-language rule so no future AI makes the owner responsible for detecting technical version/cache mistakes.
- The mandatory rule was added identically to all three required copies of `00_FIRST_OPEN_OWNER_RED_ALERT.md`: Project root, `00_READ_ME_FIRST`, and Android project root.
- It requires pre-release Android/Web/ZIP/cache/work-log comparison, truthful history, changed-file-only cache updates, proof before Final claims, and forbids touching unrelated working features.
- No application code, design, workflow, calculation, Database, Role or Branch rule was changed by this documentation-only lock.

## 2026-08-12 19:12–19:35 IST — Owner-approved Android/Web RMP parity completion

Owner approved all six audited parity items together, with no unrelated change and no Supabase Free Plan risk.

### Completed

1. Web Doctor role can now enter the existing Doctor Visit/RMP screen, matching its existing menu visibility and the approved Master/Staff/Doctor commission rule.
2. Web RMP card counts, selected RMP patient details and Master Performance normally use the already-live protected V328 small-result functions. Each result is kept for two minutes; the old local calculation remains automatic fallback on any unavailable/invalid response.
3. Master Web Performance now has All/Branch selection and uses the protected Master-only branch result.
4. Commission patient lookup checks local data first. Only after a complete 10-digit mobile is present and local data has no match does it make one exact authenticated patient lookup; the result is remembered for two minutes. No per-keystroke cloud search and no large patient download was added.
5. Web Registration Saved RMP picker now follows the already owner-approved Android V326 white-card design: separate name, mobile, area and Branch tag. Search, selection, branch filter and Manual Entry behavior are unchanged.
6. Web Prescription Details now follows the already owner-approved Android V327 white medicine cards and unlabeled direction boxes. Saved prescription text is display-only and unchanged; forbidden How often/Use/Dose/Direction/When dummy labels were not added.

### Changed files

- `03_NETLIFY_READY/app.js`
- `03_NETLIFY_READY/rmp_commission.js`
- `03_NETLIFY_READY/styles.css`
- `03_NETLIFY_READY/index.html` (changed-file cache refresh only: styles V328, app v351, RMP V328 update 2)

### Safety and verification

- No Android source, SQL, table, policy, patient/payment/refund/commission/expense row, financial formula or other screen was changed.
- No new SQL is required; these are the same three V328 protected functions already run and proved authenticated=true / anon=false by the Owner.
- Both Web JavaScript files passed syntax checking; every referenced local index file exists.
- Targeted checks passed for Doctor access, all three RPCs, complete-number patient lookup, two-minute cache, all three legacy fallbacks, professional RMP cards, Prescription boxes and absence of forbidden dummy labels.
- Existing project guard again reported only its previously recorded unrelated locked-message/translation warnings; they were not changed without Owner permission.
- Real browser/phone live test remains pending and must not be falsely called passed.

## 2026-08-12 19:38 IST — Final Android build attempt before package

- Command attempted: Gradle `:app:assembleDebug` from the included Android Studio project.
- Result: Gradle wrapper started with Java 17 but could not download Gradle 8.5 because this environment has no access to the Gradle server (`Network is unreachable`).
- This is an environment/download block, not evidence of an Android source error; equally, it is not a Build Pass.
- Package must remain named `BUILD_PENDING` until Android Studio completes Gradle Sync/Build on the Owner's computer.
