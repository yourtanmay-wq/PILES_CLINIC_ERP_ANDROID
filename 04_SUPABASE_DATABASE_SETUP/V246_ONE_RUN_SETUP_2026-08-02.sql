-- =====================================================================
-- PILES CLINIC — V246  ·  ONE-RUN SECURE SETUP  (copy ALL, run ONCE)
-- Three isolated modules: Profile & Salary (hr) · Work Notebook (wn) ·
-- Income & Expense (fin). Owner: TK BISWAS · 02.08.2026 (IST).
--
-- HOW TO RUN (that is all you do):
--   Supabase → SQL Editor → New query → paste this WHOLE file → Run.
--   Nothing else. It creates schemas, tables, security, the 15 secure
--   identities (Master + 9 staff + 4 doctors + 1 field), exposes the API
--   schemas, and makes the two private storage buckets + policies.
--   Safe to run again (idempotent).
--
-- ⚠️ ONE THING TO KNOW (temporary passwords):
--   Every module login is created with the SAME temporary password:
--        Change#2026
--   After the run, YOU (Master) change each password in
--   Supabase → Authentication → Users. Password control stays with Master.
--
-- SAFETY: creates ONLY new schemas hr/wn/fin. Never alters/drops/【touches】
--   any existing public.* table, and never enables RLS on any old table.
--   Branch-login accounts are NEVER given an identity (they are not people).
-- =====================================================================

create extension if not exists pgcrypto;

create schema if not exists hr;
create schema if not exists wn;
create schema if not exists fin;

-- =====================================================================
-- 1. IDENTITY MAP  (Auth user  ↔  person). No personal email required.
-- =====================================================================
create table if not exists hr.app_identity (
  "uid"          uuid primary key,
  "person_code"  text unique,
  "link_mobile"  text,
  "role_kind"    text,                       -- master / staff / doctor / field
  "is_master"    boolean not null default false,
  "created_at"   timestamptz not null default now()
);
alter table hr.app_identity enable row level security;
alter table hr.app_identity force row level security;

-- Helpers (SECURITY DEFINER so they can read the identity map under forced RLS).
create or replace function hr.is_master() returns boolean
  language sql stable security definer set search_path = hr, public as $$
  select coalesce((select is_master from hr.app_identity where uid = auth.uid()), false);
$$;
create or replace function hr.my_code() returns text
  language sql stable security definer set search_path = hr, public as $$
  select coalesce((select person_code from hr.app_identity where uid = auth.uid()), '');
$$;

-- RLS: a person may read ONLY their own mapping row; Master may read all.
-- 🔒 NO write policy for normal users — the identity map can be written ONLY
--    by the service role (this SQL) or by the Master-gated function below.
--    This closes the reported hole (no one can map themselves as Master).
drop policy if exists app_identity_read on hr.app_identity;
create policy app_identity_read on hr.app_identity for select
  using ( hr.is_master() or uid = auth.uid() );

-- =====================================================================
-- 2. SECURITY FIX — remove the old, unsafe map_identity(); replace with a
--    Master-gated admin function. The FIRST master is created only by this
--    SQL (service role, section 8). No callable path lets a normal user
--    become Master.
-- =====================================================================
drop function if exists hr.map_identity(uuid, text, text, text, boolean);

create or replace function hr.admin_set_identity(
  p_uid uuid, p_code text, p_mobile text, p_role text, p_is_master boolean)
  returns void
  language plpgsql
  security definer
  set search_path = hr, public
as $$
begin
  -- Only an already-authenticated MASTER may add or change any identity.
  if not hr.is_master() then
    raise exception 'Only Master can manage identities';
  end if;
  insert into hr.app_identity(uid, person_code, link_mobile, role_kind, is_master)
  values (p_uid, p_code, p_mobile, p_role, p_is_master)
  on conflict (uid) do update
     set person_code = excluded.person_code,
         link_mobile = excluded.link_mobile,
         role_kind   = excluded.role_kind,
         is_master   = excluded.is_master;
end;
$$;
revoke all on function hr.admin_set_identity(uuid, text, text, text, boolean) from public, anon;
grant execute on function hr.admin_set_identity(uuid, text, text, text, boolean) to authenticated;

-- =====================================================================
-- 3. MODULE 1 TABLES — PROFILE & SALARY (schema hr)
-- =====================================================================
create table if not exists hr.staff_profiles (
  "id" uuid primary key default gen_random_uuid(),
  "person_code" text unique not null, "link_mobile" text, "full_name" text,
  "role_kind" text, "branch" text, "join_date" text, "dob" text, "address" text,
  "emergency_contact" text, "gov_id_type" text, "gov_id_last4" text,
  "gov_id_full_enc" text, "notes" text, "photo_path" text,
  "active" boolean not null default true, "created_by" text,
  "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now()
);
create table if not exists hr.salary_config (
  "id" uuid primary key default gen_random_uuid(),
  "person_code" text unique not null, "salary_enabled" boolean not null default false,
  "salary_amount" numeric, "salary_date" text, "updated_by" text,
  "updated_at" timestamptz not null default now()
);
create table if not exists hr.salary_payments (
  "id" uuid primary key default gen_random_uuid(), "person_code" text not null,
  "paid_on" date, "amount" numeric, "mode" text, "paid_by" text, "remark" text,
  "created_at" timestamptz not null default now()
);
create index if not exists salary_payments_person_idx on hr.salary_payments(person_code);
create table if not exists hr.staff_documents (
  "id" uuid primary key default gen_random_uuid(), "person_code" text not null,
  "doc_type" text, "storage_path" text, "masked_number" text, "uploaded_by" text,
  "uploaded_at" timestamptz not null default now()
);
create index if not exists staff_documents_person_idx on hr.staff_documents(person_code);
create table if not exists hr.profile_audit (
  "id" uuid primary key default gen_random_uuid(), "entity" text, "person_code" text,
  "field" text, "old_value" jsonb, "new_value" jsonb, "changed_by" text,
  "changed_at" timestamptz not null default now()
);
create index if not exists profile_audit_person_idx on hr.profile_audit(person_code);

do $$ declare t text; begin
  foreach t in array array['staff_profiles','salary_config','salary_payments','staff_documents','profile_audit'] loop
    execute format('alter table hr.%I enable row level security;', t);
    execute format('alter table hr.%I force row level security;', t);
  end loop; end $$;

drop policy if exists sp_read on hr.staff_profiles;
create policy sp_read on hr.staff_profiles for select using ( hr.is_master() or person_code = hr.my_code() );
drop policy if exists sp_write on hr.staff_profiles;
create policy sp_write on hr.staff_profiles for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists sc_read on hr.salary_config;
create policy sc_read on hr.salary_config for select using ( hr.is_master() or person_code = hr.my_code() );
drop policy if exists sc_write on hr.salary_config;
create policy sc_write on hr.salary_config for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists spay_read on hr.salary_payments;
create policy spay_read on hr.salary_payments for select using ( hr.is_master() or person_code = hr.my_code() );
drop policy if exists spay_write on hr.salary_payments;
create policy spay_write on hr.salary_payments for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists sd_read on hr.staff_documents;
create policy sd_read on hr.staff_documents for select using ( hr.is_master() or person_code = hr.my_code() );
drop policy if exists sd_write on hr.staff_documents;
create policy sd_write on hr.staff_documents for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists pa_read on hr.profile_audit;
create policy pa_read on hr.profile_audit for select using ( hr.is_master() or person_code = hr.my_code() );

create or replace function hr.fn_profile_audit() returns trigger
  language plpgsql security definer set search_path = hr, public as $$
begin
  insert into hr.profile_audit(entity, person_code, field, old_value, new_value, changed_by)
  values (tg_argv[0], coalesce(new.person_code, old.person_code), 'row', to_jsonb(old), to_jsonb(new), coalesce(hr.my_code(),'unknown'));
  return new;
end $$;
drop trigger if exists trg_sp_audit on hr.staff_profiles;
create trigger trg_sp_audit before update on hr.staff_profiles for each row execute function hr.fn_profile_audit('profile');
drop trigger if exists trg_sc_audit on hr.salary_config;
create trigger trg_sc_audit before update on hr.salary_config for each row execute function hr.fn_profile_audit('salary_config');
drop trigger if exists trg_spay_audit on hr.salary_payments;
create trigger trg_spay_audit before update on hr.salary_payments for each row execute function hr.fn_profile_audit('salary_payment');

-- =====================================================================
-- 4. MODULE 2 TABLES — WORK NOTEBOOK / REPORTS / OUTSIDE CALLS (schema wn)
-- =====================================================================
create table if not exists wn.notebook_days (
  "id" uuid primary key default gen_random_uuid(), "staff_code" text not null,
  "staff_mobile" text, "branch" text, "work_date" date not null, "check_in" text,
  "check_out" text, "notes" text, "carry_forward" text, "problem_help" text,
  "calc_table" jsonb, "manual_entries" jsonb,
  "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now(),
  unique ("staff_code","work_date")
);
create table if not exists wn.outside_calls (
  "id" uuid primary key default gen_random_uuid(), "staff_code" text not null, "branch" text,
  "call_date" date not null, "call_time" text not null, "target_mobile" text not null,
  "remark" text, "created_at" timestamptz not null default now(),
  unique ("staff_code","call_date","call_time","target_mobile")
);
create table if not exists wn.call_taps (
  "id" uuid primary key default gen_random_uuid(), "staff_code" text not null,
  "target_mobile_mask" text, "tapped_at" timestamptz not null default now(),
  "call_date" date not null default (now() at time zone 'Asia/Kolkata')::date
);
create index if not exists call_taps_staff_date_idx on wn.call_taps(staff_code, call_date);
create table if not exists wn.work_reports (
  "id" uuid primary key default gen_random_uuid(), "staff_code" text not null, "branch" text,
  "period_type" text not null, "period_key" text not null, "auto_stats" jsonb,
  "manual_summary" text, "status" text not null default 'draft', "version" integer not null default 1,
  "superseded_by" uuid, "submitted_at" timestamptz, "seen_by" text, "seen_at" timestamptz,
  "accepted" boolean not null default false,
  "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now()
);
create index if not exists work_reports_staff_idx on wn.work_reports(staff_code, period_type, period_key);
create table if not exists wn.report_audit (
  "id" uuid primary key default gen_random_uuid(), "report_id" uuid, "old_value" jsonb,
  "new_value" jsonb, "changed_by" text, "changed_at" timestamptz not null default now()
);

do $$ declare t text; begin
  foreach t in array array['notebook_days','outside_calls','call_taps','work_reports','report_audit'] loop
    execute format('alter table wn.%I enable row level security;', t);
    execute format('alter table wn.%I force row level security;', t);
  end loop; end $$;

drop policy if exists nd_all on wn.notebook_days;
create policy nd_all on wn.notebook_days for all using ( hr.is_master() or staff_code = hr.my_code() ) with check ( staff_code = hr.my_code() or hr.is_master() );
drop policy if exists oc_all on wn.outside_calls;
create policy oc_all on wn.outside_calls for all using ( hr.is_master() or staff_code = hr.my_code() ) with check ( staff_code = hr.my_code() or hr.is_master() );
drop policy if exists ct_all on wn.call_taps;
create policy ct_all on wn.call_taps for all using ( hr.is_master() or staff_code = hr.my_code() ) with check ( staff_code = hr.my_code() or hr.is_master() );
drop policy if exists wr_read on wn.work_reports;
create policy wr_read on wn.work_reports for select using ( hr.is_master() or staff_code = hr.my_code() );
drop policy if exists wr_insert on wn.work_reports;
create policy wr_insert on wn.work_reports for insert with check ( staff_code = hr.my_code() or hr.is_master() );
drop policy if exists wr_update on wn.work_reports;
create policy wr_update on wn.work_reports for update using ( hr.is_master() or (staff_code = hr.my_code() and status = 'draft') ) with check ( hr.is_master() or staff_code = hr.my_code() );
drop policy if exists ra_read on wn.report_audit;
create policy ra_read on wn.report_audit for select using ( hr.is_master() or exists ( select 1 from wn.work_reports r where r.id = report_audit.report_id and r.staff_code = hr.my_code() ) );

create or replace function wn.fn_report_audit() returns trigger
  language plpgsql security definer set search_path = wn, hr, public as $$
begin
  if old.status is distinct from 'draft' then
    insert into wn.report_audit(report_id, old_value, new_value, changed_by)
    values (old.id, to_jsonb(old), to_jsonb(new), coalesce(hr.my_code(),'unknown'));
  end if;
  return new;
end $$;
drop trigger if exists trg_wr_audit on wn.work_reports;
create trigger trg_wr_audit before update on wn.work_reports for each row execute function wn.fn_report_audit();

-- =====================================================================
-- 5. MODULE 3 TABLES — INCOME & EXPENSE (schema fin, MASTER ONLY)
-- =====================================================================
create table if not exists fin.collections (
  "id" uuid primary key default gen_random_uuid(), "entry_date" date not null, "branch" text,
  "cash" numeric not null default 0, "online" numeric not null default 0, "note" text,
  "ignored" boolean not null default false, "created_by" text,
  "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now()
);
create index if not exists fin_coll_date_idx on fin.collections(entry_date, branch);
create table if not exists fin.expenses (
  "id" uuid primary key default gen_random_uuid(), "entry_date" date not null, "branch" text,
  "category" text, "paid_to" text, "amount" numeric not null default 0, "mode" text, "note" text,
  "receipt_path" text, "ignored" boolean not null default false, "created_by" text,
  "created_at" timestamptz not null default now(), "updated_at" timestamptz not null default now()
);
create index if not exists fin_exp_date_idx on fin.expenses(entry_date, branch, category);
create table if not exists fin.audit (
  "id" uuid primary key default gen_random_uuid(), "entity" text, "row_id" uuid,
  "old_value" jsonb, "new_value" jsonb, "changed_by" text, "changed_at" timestamptz not null default now()
);

do $$ declare t text; begin
  foreach t in array array['collections','expenses','audit'] loop
    execute format('alter table fin.%I enable row level security;', t);
    execute format('alter table fin.%I force row level security;', t);
  end loop; end $$;

drop policy if exists coll_master on fin.collections;
create policy coll_master on fin.collections for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists exp_master on fin.expenses;
create policy exp_master on fin.expenses for all using ( hr.is_master() ) with check ( hr.is_master() );
drop policy if exists finaudit_master on fin.audit;
create policy finaudit_master on fin.audit for select using ( hr.is_master() );

create or replace function fin.fn_audit() returns trigger
  language plpgsql security definer set search_path = fin, hr, public as $$
begin
  insert into fin.audit(entity, row_id, old_value, new_value, changed_by)
  values (tg_argv[0], old.id, to_jsonb(old), to_jsonb(new), coalesce(hr.my_code(),'master'));
  return new;
end $$;
drop trigger if exists trg_coll_audit on fin.collections;
create trigger trg_coll_audit before update on fin.collections for each row execute function fin.fn_audit('collection');
drop trigger if exists trg_exp_audit on fin.expenses;
create trigger trg_exp_audit before update on fin.expenses for each row execute function fin.fn_audit('expense');

-- =====================================================================
-- 6. GRANTS (RLS still restricts every row; grants only open the API path)
-- =====================================================================
grant usage on schema hr, wn, fin to authenticated;
grant select, insert, update, delete on all tables in schema hr  to authenticated;
grant select, insert, update, delete on all tables in schema wn  to authenticated;
grant select, insert, update, delete on all tables in schema fin to authenticated;
grant execute on function hr.is_master(), hr.my_code() to authenticated;
alter default privileges in schema hr  grant select, insert, update, delete on tables to authenticated;
alter default privileges in schema wn  grant select, insert, update, delete on tables to authenticated;
alter default privileges in schema fin grant select, insert, update, delete on tables to authenticated;

-- =====================================================================
-- 7. PROFILE SKELETON SEED (identity-safe; NO personal data). Branch-login
--    accounts are deliberately EXCLUDED. Master fills personal fields later.
-- =====================================================================
insert into hr.staff_profiles (person_code, link_mobile, role_kind, branch) values
  ('KNE-LAXMI','9883605917','staff','Kishanganj'),
  ('KNE-KISHAN5','6207841890','staff','Kishanganj'),
  ('JPE-CRP','9647840067','staff','Jalpaiguri'),
  ('JPE-JALPAI-13','8101397763','staff','Jalpaiguri'),
  ('JPE-RUPAM','8167096595','staff','Jalpaiguri'),
  ('COB-UTTAMA','7679751521','staff','Cooch Behar'),
  ('COB-4','7501256248','staff','Cooch Behar'),
  ('FLK-1','9883623823','staff','Falakata'),
  -- 🔴 V408 (17.08.2026) — TK-এর নির্দেশ: FALA-15 কাজ ছেড়ে দিয়েছেন, ওঁর সারিটি
  --    (মোবাইল নম্বর সহ) এখান থেকে বাদ দেওয়া হলো। এই ফাইল আবার চালালেও
  --    ওই লগইন আর তৈরি হবে না। ⛔ বাকি কারও কিছু বদলায়নি।
  ('DR-KH-MANDAL','7980993652','doctor','Cooch Behar'),
  ('DR-JAY-BANIK','8001800148','doctor','Jalpaiguri'),
  ('DR-AMIT-GOLDAR','9046366596','doctor','Kishanganj'),
  ('DR-PK-ROY','6297625447','doctor','Kishanganj'),
  ('FIELD-OFFICER','9002003540','field','All')
on conflict (person_code) do nothing;
insert into hr.salary_config (person_code, salary_enabled)
  select person_code, false from hr.staff_profiles on conflict (person_code) do nothing;

-- =====================================================================
-- 8. SECURE IDENTITIES — creates the 15 Auth logins (Master + 9 staff +
--    4 doctors + 1 field) AND maps each to its person, in ONE step, so you
--    never copy a UID by hand. Idempotent. Temporary password: Change#2026
--    (change each in Authentication → Users after the run).
--
--    This block writes to Supabase's internal auth tables. On a standard
--    Supabase project the SQL Editor is allowed to do this. If your project
--    restricts direct auth writes, ONLY this block will error (everything
--    above is already done) — see the delivery note for the 1-time fallback.
-- =====================================================================
do $$
declare
  rec record;
  v_uid uuid;
  temp_pw text := 'Change#2026';
  has_provider_id boolean;
begin
  select exists(
    select 1 from information_schema.columns
    where table_schema='auth' and table_name='identities' and column_name='provider_id'
  ) into has_provider_id;

  for rec in
    select * from (values
      ('MASTER-TK','master-tk@staff.piles','8001080080','master', true),
      ('KNE-LAXMI','kne-laxmi@staff.piles','9883605917','staff', false),
      ('KNE-KISHAN5','kne-kishan5@staff.piles','6207841890','staff', false),
      ('JPE-CRP','jpe-crp@staff.piles','9647840067','staff', false),
      ('JPE-JALPAI-13','jpe-jalpai-13@staff.piles','8101397763','staff', false),
      ('JPE-RUPAM','jpe-rupam@staff.piles','8167096595','staff', false),
      ('COB-UTTAMA','cob-uttama@staff.piles','7679751521','staff', false),
      ('COB-4','cob-4@staff.piles','7501256248','staff', false),
      ('FLK-1','flk-1@staff.piles','9883623823','staff', false),
      -- 🔴 V408 (17.08.2026) — TK-এর নির্দেশ: FALA-15 কাজ ছেড়ে দিয়েছেন, ওঁর সারিটি
      --    (মোবাইল নম্বর সহ) এখান থেকে বাদ দেওয়া হলো। এই ফাইল আবার চালালেও
      --    ওই লগইন আর তৈরি হবে না। ⛔ বাকি কারও কিছু বদলায়নি।
      ('DR-KH-MANDAL','dr-kh-mandal@staff.piles','7980993652','doctor', false),
      ('DR-JAY-BANIK','dr-jay-banik@staff.piles','8001800148','doctor', false),
      ('DR-AMIT-GOLDAR','dr-amit-goldar@staff.piles','9046366596','doctor', false),
      ('DR-PK-ROY','dr-pk-roy@staff.piles','6297625447','doctor', false),
      ('FIELD-OFFICER','field-officer@staff.piles','9002003540','field', false)
    ) as t(code, email, mobile, role, is_master)
  loop
    select id into v_uid from auth.users where email = rec.email;
    if v_uid is null then
      v_uid := gen_random_uuid();
      insert into auth.users (
        instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, created_at, updated_at,
        raw_app_meta_data, raw_user_meta_data,
        confirmation_token, recovery_token, email_change_token_new, email_change
      ) values (
        '00000000-0000-0000-0000-000000000000', v_uid, 'authenticated', 'authenticated',
        rec.email, crypt(temp_pw, gen_salt('bf')),
        now(), now(), now(),
        '{"provider":"email","providers":["email"]}'::jsonb, '{}'::jsonb,
        '', '', '', ''
      );
      if has_provider_id then
        insert into auth.identities (id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
        values (gen_random_uuid(), v_uid, v_uid::text, jsonb_build_object('sub', v_uid::text, 'email', rec.email), 'email', now(), now(), now());
      else
        insert into auth.identities (id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
        values (v_uid, v_uid, jsonb_build_object('sub', v_uid::text, 'email', rec.email), 'email', now(), now(), now());
      end if;
    end if;
    -- map identity (runs as service role here → bypasses RLS; safe bootstrap)
    insert into hr.app_identity (uid, person_code, link_mobile, role_kind, is_master)
    values (v_uid, rec.code, rec.mobile, rec.role, rec.is_master)
    on conflict (uid) do update
       set person_code = excluded.person_code, link_mobile = excluded.link_mobile,
           role_kind = excluded.role_kind, is_master = excluded.is_master;
  end loop;
end $$;

-- =====================================================================
-- 9. PRIVATE STORAGE BUCKETS + POLICIES (documents / receipts)
-- =====================================================================
insert into storage.buckets (id, name, public)
values ('hr-private','hr-private', false), ('fin-private','fin-private', false)
on conflict (id) do nothing;

-- hr-private: Master full access; each staff may read only their own folder
--   (folder name = their person_code). Uploads are Master-only.
drop policy if exists hr_priv_master on storage.objects;
create policy hr_priv_master on storage.objects for all to authenticated
  using ( bucket_id = 'hr-private' and hr.is_master() )
  with check ( bucket_id = 'hr-private' and hr.is_master() );
drop policy if exists hr_priv_self_read on storage.objects;
create policy hr_priv_self_read on storage.objects for select to authenticated
  using ( bucket_id = 'hr-private' and (storage.foldername(name))[1] = hr.my_code() );

-- fin-private: Master only (no staff access at all).
drop policy if exists fin_priv_master on storage.objects;
create policy fin_priv_master on storage.objects for all to authenticated
  using ( bucket_id = 'fin-private' and hr.is_master() )
  with check ( bucket_id = 'fin-private' and hr.is_master() );

-- =====================================================================
-- 10. EXPOSE the new schemas to the API (PostgREST) WITHOUT removing any
--     schema you already expose. Appends hr, wn, fin only if missing.
-- =====================================================================
do $$
declare cur text; want text;
begin
  select regexp_replace(setting, '^pgrst\.db_schemas=', '')
    into cur
  from pg_db_role_setting drs
  join pg_roles r on r.oid = drs.setrole
  cross join lateral unnest(drs.setconfig) as setting
  where r.rolname = 'authenticator' and setting like 'pgrst.db_schemas=%'
  limit 1;

  if cur is null or length(trim(cur)) = 0 then
    cur := 'public, graphql_public';
  end if;
  want := cur;
  if position('hr' in want) = 0 or want !~ '(^|[, ])hr([, ]|$)' then want := want || ', hr'; end if;
  if want !~ '(^|[, ])wn([, ]|$)' then want := want || ', wn'; end if;
  if want !~ '(^|[, ])fin([, ]|$)' then want := want || ', fin'; end if;

  execute format('alter role authenticator set pgrst.db_schemas = %L', want);
end $$;
notify pgrst, 'reload config';

-- =====================================================================
-- DONE. Log in from the app with a Staff Code + the temporary password
-- Change#2026, then change every password in Authentication → Users.
-- =====================================================================
