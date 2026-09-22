# V1651 — Partner Shares AUTO Income parity

Date: 21/09/2026 (IST)

## Problem verified
Partner Shares calculated income only from `fin.collections`. Income–Expense, however, has a locked rule from 01/09/2026: when a date has no real `fin.collections` row, Visit Fee / Treatment / Medicine collection is shown as AUTO Income. This made Partner Shares lower than Income–Expense.

## Fix
- Android `PartnerSharesActivity`: builds an effective collection list = real `fin.collections` + missing-day AUTO Income from the same `PaymentRepository.fetchCollectionRange()` source used by Income–Expense.
- Existing manual row wins for that date; AUTO never double-counts it.
- The existing old/backdated-entry exclusion, refunded/cancelled mobile exclusion, approved-refund handling from collection rows, and daily non-negative clamp are preserved.
- The same effective rows feed Total Income, Net Profit, percentage-history segment accrual, Due/Balance, Settlement, Withdraw/Return and Print/Export.
- Web `partners.js`: mirrors the Income–Expense AUTO Income helper and applies it both to Master Partner Shares and the partner's own ledger.
- No database writes, schema changes, partner percentages, effective dates, drawings, expenses, or UI layout changes.

## Live Cooch Behar verification snapshot (01/01/2026–21/09/2026)
- Jan–Jul net: ₹1,50,324
- Aug–21 Sep net: ₹1,76,065
- Total net: ₹3,26,389
- Expected shares: TK BISWAS ₹1,45,588; Dr. K.H. MANDAL ₹1,45,588; GOKUL SARKAR ₹17,606.50; J.H. MANDAL ₹17,606.50.

## Checks
- `partners.js`: `node --check` PASS.
- Kotlin patch structural checks PASS.
- Full Gradle compile could not run in this isolated environment because the Gradle 8.5 distribution is not cached and outbound download is unavailable; no compile PASS is claimed.
