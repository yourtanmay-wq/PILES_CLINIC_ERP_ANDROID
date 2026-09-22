-- =====================================================================
-- V1298 (১০.০৯.২০২৬) — তালিকা ৪১৪: নোটিশ ঢোকামাত্র ফোনে push (SMS-এর মতো একবার শব্দ)
-- ১) device_tokens — কোন স্টাফের কোন ফোন (Firebase token); ফোন নিজে লেখে/হালনাগাদ করে
-- ২) pg_net চালু; briefings-এ নতুন সারি ঢুকলে trigger Netlify function-কে জানায় (HTTP POST)
-- ⛔ পুরনো কোনো টেবিল/সারি বদলায় না। ⛔ <<PUSH_SECRET>> জায়গায় গোপন শব্দটা বসাতে হবে (চ্যাটে দেওয়া সংস্করণে বসানো থাকে)।
-- =====================================================================
begin;

create table if not exists public.device_tokens (
  token text primary key,
  mobile text not null,
  name text,
  role text,
  branch text,
  platform text default 'android',
  "updatedAt" text
);
alter table public.device_tokens disable row level security;
create index if not exists device_tokens_mobile_idx on public.device_tokens (mobile);

create extension if not exists pg_net with schema extensions;

create or replace function public.tk_push_new_briefing() returns trigger
language plpgsql security definer as $$
begin
  begin
    perform net.http_post(
      url := 'https://maaayurvedpilesclinic.netlify.app/notify-push',
      headers := jsonb_build_object('content-type', 'application/json', 'x-push-secret', '<<PUSH_SECRET>>'),
      body := jsonb_build_object('record', to_jsonb(new)),
      timeout_milliseconds := 8000
    );
  exception when others then
    null;   -- push না গেলেও নোটিশ সেভ হবেই — কিছু আটকায় না
  end;
  return new;
end $$;

drop trigger if exists tk_push_new_briefing on public.briefings;
create trigger tk_push_new_briefing after insert on public.briefings
for each row execute function public.tk_push_new_briefing();

select 'device_tokens' as ki, count(*)::text as man from device_tokens
union all
select 'trigger', count(*)::text from pg_trigger where tgname = 'tk_push_new_briefing'
union all
select 'pg_net', count(*)::text from pg_extension where extname = 'pg_net';

commit;
