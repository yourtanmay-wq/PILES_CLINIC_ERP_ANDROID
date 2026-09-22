-- =====================================================================
-- PILES CLINIC — V307 · PARTNER SHARES · PHASE 2 · PARTNER LOGIN ACCESS
-- Owner: TK BISWAS · 10.08.2026 (IST).
--
-- HOW TO RUN: Supabase → SQL Editor → New query → paste WHOLE file → Run.
--   Idempotent (safe to run again). RUN V306 FIRST (needs its tables).
--
-- WHAT THIS DOES (all ADDITIVE — no existing MASTER policy is changed/dropped):
--   1) A helper fin.my_mobile() — the logged-in person's own 10-digit mobile.
--   2) Extra RLS so a PARTNER can READ only their own branch's figures and
--      their own share/drawings — and (if the master turned their toggle on)
--      ADD Income/Expense, editable ONLY on the day it was entered (IST).
--   3) Creates the 4 NEW partner login identities. The module signs in
--      silently with the role password 'partner123' (same design as
--      staff123/doctor123), so this identity password is set to 'partner123'
--      to match. The MAIN-APP password (what the partner types on the login
--      screen) is controlled by the master in Password Center as usual. The
--      partners who are already app users (TK master, K.H Mandal, Jay Banik)
--      need NO new identity — they are matched by mobile automatically.
--
-- 🔒 SAFETY: Every MASTER policy from V306 stays exactly as it was. Postgres
--   combines policies with OR, so these partner policies only GRANT narrow
--   extra rights; the master keeps full access, and a partner can never see
--   another branch or another person's row.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. HELPER — the caller's own mobile (from the identity map). SECURITY
--    DEFINER so it can read hr.app_identity under forced RLS. 10 digits.
-- ---------------------------------------------------------------------
create or replace function fin.my_mobile() returns text
  language sql stable security definer set search_path = hr, public as $$
  select right(regexp_replace(coalesce(
      (select link_mobile from hr.app_identity where uid = auth.uid()), ''), '\D', '', 'g'), 10);
$$;
revoke all on function fin.my_mobile() from public, anon;
grant execute on function fin.my_mobile() to authenticated;

-- Which branches is the caller an ACTIVE partner of? (helper for readability)
create or replace function fin.is_my_partner_branch(p_branch text) returns boolean
  language sql stable security definer set search_path = fin, hr, public as $$
  select exists(
    select 1 from fin.partners p
    where p.branch = p_branch and p.active
      and right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10) = fin.my_mobile()
      and fin.my_mobile() <> '');
$$;
create or replace function fin.can_entry_branch(p_branch text) returns boolean
  language sql stable security definer set search_path = fin, hr, public as $$
  select exists(
    select 1 from fin.partners p
    where p.branch = p_branch and p.active and p.can_entry
      and right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10) = fin.my_mobile()
      and fin.my_mobile() <> '');
$$;
revoke all on function fin.is_my_partner_branch(text) from public, anon;
revoke all on function fin.can_entry_branch(text) from public, anon;
grant execute on function fin.is_my_partner_branch(text), fin.can_entry_branch(text) to authenticated;

-- ---------------------------------------------------------------------
-- 2a. PARTNER can READ only their OWN partner / drawing / history rows.
--     (Master policies from V306 are untouched.)
-- ---------------------------------------------------------------------
drop policy if exists partners_self_read on fin.partners;
create policy partners_self_read on fin.partners for select
  using ( right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = fin.my_mobile() and fin.my_mobile() <> '' );

drop policy if exists drawings_self_read on fin.partner_drawings;
create policy drawings_self_read on fin.partner_drawings for select
  using ( right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = fin.my_mobile() and fin.my_mobile() <> '' );

drop policy if exists pcthist_self_read on fin.partner_pct_history;
create policy pcthist_self_read on fin.partner_pct_history for select
  using ( right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = fin.my_mobile() and fin.my_mobile() <> '' );

drop policy if exists settlements_self_read on fin.partner_settlements;
create policy settlements_self_read on fin.partner_settlements for select
  using ( right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = fin.my_mobile() and fin.my_mobile() <> '' );

-- ---------------------------------------------------------------------
-- 2b. PARTNER can READ their branch's collections & expenses (to see the
--     branch Income/Expense/Net). Master policy untouched.
-- ---------------------------------------------------------------------
drop policy if exists coll_partner_read on fin.collections;
create policy coll_partner_read on fin.collections for select
  using ( fin.is_my_partner_branch(branch) );
drop policy if exists exp_partner_read on fin.expenses;
create policy exp_partner_read on fin.expenses for select
  using ( fin.is_my_partner_branch(branch) );

-- ---------------------------------------------------------------------
-- 2c. PARTNER (only if master turned their toggle ON) can ADD Income/
--     Expense for their branch, stamped with their own mobile, and EDIT it
--     ONLY on the same IST day it was entered. From the next day it is
--     locked for the partner (master can still edit — master policy).
-- ---------------------------------------------------------------------
drop policy if exists coll_partner_insert on fin.collections;
create policy coll_partner_insert on fin.collections for insert
  with check ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile() );
drop policy if exists coll_partner_update on fin.collections;
create policy coll_partner_update on fin.collections for update
  using ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile()
    and (created_at at time zone 'Asia/Kolkata')::date = (now() at time zone 'Asia/Kolkata')::date )
  with check ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile() );

drop policy if exists exp_partner_insert on fin.expenses;
create policy exp_partner_insert on fin.expenses for insert
  with check ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile() );
drop policy if exists exp_partner_update on fin.expenses;
create policy exp_partner_update on fin.expenses for update
  using ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile()
    and (created_at at time zone 'Asia/Kolkata')::date = (now() at time zone 'Asia/Kolkata')::date )
  with check ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile() );

-- ---------------------------------------------------------------------
-- 3. THE 4 NEW PARTNER LOGIN IDENTITIES (module/Supabase auth). Temp
--    password Change#2026 — master changes each in Supabase → Auth → Users.
--    (Same safe pattern as V246. Existing app users are NOT re-created.)
-- ---------------------------------------------------------------------
do $$
declare
  rec record; v_uid uuid; temp_pw text := 'partner123'; has_provider_id boolean;
begin
  select exists(
    select 1 from information_schema.columns
    where table_schema='auth' and table_name='identities' and column_name='provider_id'
  ) into has_provider_id;

  for rec in
    select * from (values
      ('PTR-JH-MANDAL',     'ptr-jh-mandal@staff.piles',     '7479173399'),
      ('PTR-GOKUL',         'ptr-gokul@staff.piles',         '9002610352'),
      ('PTR-SAIKAT-ROY',    'ptr-saikat-roy@staff.piles',    '7810907954'),
      ('PTR-PRANAB-BISWAS', 'ptr-pranab-biswas@staff.piles', '9242009205')
    ) as t(code, email, mobile)
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
    insert into hr.app_identity (uid, person_code, link_mobile, role_kind, is_master)
    values (v_uid, rec.code, rec.mobile, 'partner', false)
    on conflict (uid) do update
       set person_code = excluded.person_code, link_mobile = excluded.link_mobile,
           role_kind = excluded.role_kind, is_master = excluded.is_master;
  end loop;
end $$;

-- =====================================================================
-- DONE. Partners can now (once the app's partner view + config roster are
-- deployed) log in and see ONLY their own branch ledger, and — if their
-- toggle is on — add same-day-editable Income/Expense. Master unaffected.
-- =====================================================================
