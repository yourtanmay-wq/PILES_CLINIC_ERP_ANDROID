-- ═══════════════════════════════════════════════════════════════════════════
-- 📱🔒 V889 (৩০.০৮.২০২৬) — "কোন ফোনে কে লগইন" জমা রাখার টেবিল
-- TK-নির্দেশ। ⛔ কোনো পুরোনো টেবিল ছোঁয়া হয় না, শুধু নতুন একটা।
-- ═══════════════════════════════════════════════════════════════════════════

create table if not exists public.device_logins (
  "id"          text primary key,          -- ফোনের স্থায়ী নম্বর
  "staffMobile" text,
  "staffName"   text,
  "branch"      text,
  "role"        text,
  "phoneModel"  text,
  "appVersion"  text,
  "loggedInAt"  text
);

alter table public.device_logins disable row level security;
notify pgrst, 'reload schema';

-- ─── দেখার জন্য ─────────────────────────────────────────────────────────
select "staffName" as "কে", "staffMobile" as "নম্বর", "branch" as "ব্রাঞ্চ",
       "role" as "ধরন", "phoneModel" as "ফোন", "appVersion" as "ভার্সন",
       "loggedInAt" as "শেষ লগইন"
  from public.device_logins
 order by "loggedInAt" desc;
