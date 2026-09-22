# V246 — LIVE TEST CHECKLIST (laptop + phone)

Run the one SQL first (`V246_SQL_RUN_STEPS.md`), then change passwords.

## A. Security (the reason for V246)
- [ ] Sign in as a normal Staff → they CANNOT see any "make me Master" option and
      cannot see anyone else's profile/salary/notebook.
- [ ] Sign in as Master → can see all profiles, salaries, staff reports, finance.
- [ ] A signed-out / public browser sees NONE of hr/wn/fin data.
- [ ] (Already proven in DB) staff cannot self-promote; only Master manages identities.

## B. Old app unchanged (most important)
- [ ] Existing login, Dashboard design/order, Briefing, Enquiry, Registration,
      Follow-up, Payment, Refund, Print, Doctor Visit, Chamber Date, Draft, Sync,
      Search — all behave exactly as before. Only the new cards are added at the end.

## C. Modules (web now; Android after you build the APK)
- [ ] Master: Staff Profiles (edit + salary + record payment + history) and
      Income & Expense (collection/expense, daily ledger, monthly branch-wise,
      Final Balance = Collection − Expense) work.
- [ ] Staff: My Work Notebook (check-in/out, entries, calculator, sheet totals,
      outside calls with same mobile+time blocked, auto counts, daily/monthly
      report → submit; re-submit asks for correction, old version kept) and
      My Profile (own only, masked numbers).
- [ ] In-app Call button press increments App Calls; report shows App | Outside | Total.
- [ ] Income & Expense never pulls any Patient Payment amount (all manual).

## D. Robustness
- [ ] Run the SQL a second time → no error, no duplicate users/rows.
- [ ] Weak/no internet: notebook entries stay on screen and sync later without duplicates.

## E. Android (after you build in Android Studio)
- [ ] Project builds; Guard `--release` all green; same behaviours as web.
