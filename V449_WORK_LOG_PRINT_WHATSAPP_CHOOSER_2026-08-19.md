# V449 Work Log — Print WhatsApp chooser
Date: 19.08.2026 (IST)

## TK-approved scope
Only Android Print Preview → WhatsApp PDF Share behavior.

## Verified V448 problem
`PrintPreviewActivity.sharePdf()` tried `com.whatsapp` first and only tried
`com.whatsapp.w4b` when Personal WhatsApp was unavailable. Therefore a phone
with both apps installed did not get a Personal/Business choice.

## V449 change
Before sending the already-generated PDF, a selection dialog now always shows:
1. WhatsApp
2. WhatsApp Business

The selected package alone is opened. If that selected app is not installed,
a safe message is shown; the app does not silently switch to the other WhatsApp.

## Explicitly unchanged
PDF contents/rendering, Save PDF, Print, patient data, Supabase/database, web app,
all other WhatsApp/message flows, UI outside this share selection.
