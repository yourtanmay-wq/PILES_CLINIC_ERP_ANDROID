-- V331 — ONE-TIME MEDICINE DEFAULT STORAGE
-- Stores no patient, money, photo or prescription-history data.

begin;

create table if not exists public.medicine_defaults (
    id text primary key,
    name text not null,
    "medicineType" text not null default '',
    dose text not null default '',
    "whenText" text not null default '',
    days text not null default '',
    "updatedAt" timestamptz not null default now()
);

-- Owner-confirmed existing medicine. No other medicine type is guessed.
insert into public.medicine_defaults
    (id, name, "medicineType", dose, "whenText", days, "updatedAt")
values
    ('rx_e60d63662fe806ca', 'Arshakuthar Rasa', 'Tab', '1-0-1', 'After Food', '5 days', now())
on conflict (id) do nothing;

-- A delayed/offline retry must never overwrite a newer manual change.
create or replace function public.keep_newest_medicine_default()
returns trigger
language plpgsql
as $$
begin
    if old."updatedAt" > new."updatedAt" then
        return old;
    end if;
    return new;
end;
$$;

drop trigger if exists medicine_defaults_keep_newest on public.medicine_defaults;
create trigger medicine_defaults_keep_newest
before update on public.medicine_defaults
for each row execute function public.keep_newest_medicine_default();

alter table public.medicine_defaults enable row level security;

drop policy if exists medicine_defaults_read_app on public.medicine_defaults;
create policy medicine_defaults_read_app on public.medicine_defaults
for select to anon, authenticated using (true);

drop policy if exists medicine_defaults_add_app on public.medicine_defaults;
create policy medicine_defaults_add_app on public.medicine_defaults
for insert to anon, authenticated with check (true);

drop policy if exists medicine_defaults_change_app on public.medicine_defaults;
create policy medicine_defaults_change_app on public.medicine_defaults
for update to anon, authenticated using (true) with check (true);

revoke all on public.medicine_defaults from public;
grant select, insert, update on public.medicine_defaults to anon, authenticated;

notify pgrst, 'reload schema';
commit;
