-- =====================================================================
-- V1295 (১০.০৯.২০২৬) — তালিকা ৪১১-⑭ (ক): patients · followups · medical-এ "সার্ভারে শেষ কখন বদলাল" ঘর
--   কম্পিউটার এই তিন টেবিলে লাইভ-সংযোগের বদলে প্রতি মিনিটে শুধু "এর পরে বদলানো সারি" (ছবি ছাড়া) পড়বে।
--   ফোন/কম্পিউটারের নিজের ঘড়ির updatedAt-এর বদলে সার্ভারের ঘড়ি — তাই কোনো সারি বাদ পড়ে না।
-- ⛔ শুধু ঘর + trigger (প্রতিটা insert/update-এ সার্ভার নিজে সময় বসায়); কোনো সারি/মান বদলায় না।
-- ⛔ অ্যাপ এই ঘর লেখে না; লিখলেও trigger সার্ভারের সময়ই বসায়।
-- =====================================================================
begin;

create or replace function public.tk_touch_server_updated_at() returns trigger
language plpgsql as $$
begin
  new.server_updated_at := now();
  return new;
end $$;

alter table public.patients  add column if not exists server_updated_at timestamptz not null default now();
alter table public.followups add column if not exists server_updated_at timestamptz not null default now();
alter table public.medical   add column if not exists server_updated_at timestamptz not null default now();

drop trigger if exists tk_touch_server_updated_at on public.patients;
create trigger tk_touch_server_updated_at before insert or update on public.patients  for each row execute function public.tk_touch_server_updated_at();
drop trigger if exists tk_touch_server_updated_at on public.followups;
create trigger tk_touch_server_updated_at before insert or update on public.followups for each row execute function public.tk_touch_server_updated_at();
drop trigger if exists tk_touch_server_updated_at on public.medical;
create trigger tk_touch_server_updated_at before insert or update on public.medical   for each row execute function public.tk_touch_server_updated_at();

create index if not exists patients_server_updated_at_idx  on public.patients  (server_updated_at);
create index if not exists followups_server_updated_at_idx on public.followups (server_updated_at);
create index if not exists medical_server_updated_at_idx   on public.medical   (server_updated_at);

-- যাচাই: ৩ টেবিলে ঘর + trigger
select c.table_name, c.column_default, (select count(*) from pg_trigger tg where tg.tgname = 'tk_touch_server_updated_at' and tg.tgrelid = ('public.' || c.table_name)::regclass) as trigger_ache
from information_schema.columns c
where c.table_schema = 'public' and c.column_name = 'server_updated_at'
order by 1;

commit;
