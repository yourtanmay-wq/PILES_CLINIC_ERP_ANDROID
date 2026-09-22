# V240 Changed Files

Date: 01.08.2026
Worker: ChatGPT

- Payment header: All Branch made compact and moved beside a real date selector.
- Removed the misleading fixed Calendar emoji from Payment Collection labels.
- Selected date shows that day's combined collection using the existing payment/product range calculation.
- Master can edit selected-date payment entries; every non-Master role is Read Only in selected-date view.
- Web Payment Collection also receives a real date input and no fixed Calendar emoji.
- No payment/refund arithmetic, save, approval, branch permission, Supabase schema or unrelated design changed.
- Rollback copies: `ROLLBACK_V240/`.
