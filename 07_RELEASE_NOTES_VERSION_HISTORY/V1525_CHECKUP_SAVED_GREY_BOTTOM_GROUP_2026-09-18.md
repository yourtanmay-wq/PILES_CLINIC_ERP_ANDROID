# V1525 — Doctor Check-up saved sections

- Fixed Doctor Check-up UI where a saved/restored lifetime section could remain bright in the active area while expanded.
- After save/restore, completed lifetime sections stay inside the bottom SAVED group regardless of expanded/collapsed state.
- Completed sections remain grey (alpha 0.55) so active vs completed is visually obvious.
- If a completed section becomes genuinely empty, it can return to the active area.
- No patient/payment/Supabase data logic changed.
- Includes all V1524 RMP Due List and prior fixes.
