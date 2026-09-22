-- =====================================================================
-- PILES CLINIC — V308 · "সব অংশীদারই ডাক্তার" (TK-নির্দেশ, 10.08.2026 IST)
-- The 4 partners set up in V307 as role 'partner' are actually DOCTORS.
-- This file converts their LOGIN IDENTITY from partner → doctor.
--
-- HOW TO RUN: Supabase → SQL Editor → New query → paste WHOLE file → Run.
--   Run AFTER V306 and V307. Idempotent (safe to run again).
--
-- WHAT CHANGES: ONLY these 4 people's login identity (auth user + identity
--   map) is switched from the temporary 'partner' identity (ptr-…@staff.piles,
--   password partner123) to a normal DOCTOR identity (dr-…@staff.piles,
--   password doctor123, role_kind 'doctor'). Their profit-share data in
--   fin.partners / drawings / history is keyed by MOBILE and is NOT touched —
--   so their "My Share Ledger" keeps working exactly the same.
--
-- 🔒 SAFETY: touches ONLY the 4 named accounts. No other user, no fin.* data,
--   no RLS policy is changed. The partner RLS from V307 stays (it matches by
--   mobile, not role — so a doctor whose mobile is a partner still gets their
--   own ledger, just like Dr. K.H. Mandal and Dr. Jay Banik already do).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Remove the temporary 'partner' identities created in V307 (the 4).
--    Delete the identity-map row and the auth user; auth.identities rows
--    cascade with the auth user. Nothing else references these.
-- ---------------------------------------------------------------------
delete from hr.app_identity
  where person_code in ('PTR-JH-MANDAL','PTR-GOKUL','PTR-SAIKAT-ROY','PTR-PRANAB-BISWAS');

delete from auth.users
  where email in ('ptr-jh-mandal@staff.piles','ptr-gokul@staff.piles',
                  'ptr-saikat-roy@staff.piles','ptr-pranab-biswas@staff.piles');

-- ---------------------------------------------------------------------
-- 2. Create the 4 as DOCTOR identities (password 'doctor123', same as the
--    other doctors; master can change each in Supabase → Auth → Users).
-- ---------------------------------------------------------------------
do $$
declare
  rec record; v_uid uuid; temp_pw text := 'doctor123'; has_provider_id boolean;
begin
  select exists(
    select 1 from information_schema.columns
    where table_schema='auth' and table_name='identities' and column_name='provider_id'
  ) into has_provider_id;

  for rec in
    select * from (values
      ('DR-JH-MANDAL',     'dr-jh-mandal@staff.piles',     '7479173399'),
      ('DR-GOKUL',         'dr-gokul@staff.piles',         '9002610352'),
      ('DR-SAIKAT-ROY',    'dr-saikat-roy@staff.piles',    '7810907954'),
      ('DR-PRANAB-BISWAS', 'dr-pranab-biswas@staff.piles', '9242009205')
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
    values (v_uid, rec.code, rec.mobile, 'doctor', false)
    on conflict (uid) do update
       set person_code = excluded.person_code, link_mobile = excluded.link_mobile,
           role_kind = excluded.role_kind, is_master = excluded.is_master;
  end loop;
end $$;

-- =====================================================================
-- DONE. The 4 now log in as DOCTORS (full doctor access) and still see
-- their own "My Share Ledger" (matched by mobile). App files: config.js
-- users.doctor, module_core SPECIAL_CODE, StaffDirectory, ModuleAuth all
-- updated to DR-… codes — upload the web + build V306 once.
-- =====================================================================
