-- ============================================================================
-- V1388 — ISHRAT PARWEEN (KNE-KISHAN12, কিশানগঞ্জ) — মডিউল-লগইন তৈরি
-- TK নিজে জিজ্ঞাসা করলেন "KISHAN-12-র কী সমস্যা" — যাচাই করে দেখা গেল ARMAN-এর
-- হুবহু একই ভুল: রেজিস্ট্রেশনের সময় (সারি ৪৮৫) StaffDirectory.kt/config.js-এ
-- যোগ হয়েছিল, কিন্তু Work Notebook-এর দ্বিতীয় (মডিউল) লগইনটা তখন বাদ পড়ে
-- গিয়েছিল — KISHAN10 (V1136) ও KISHAN11 (V1137)-এর বেলায় করা হয়েছিল,
-- KISHAN12-র বেলায় হয়নি (00_SQL ফোল্ডারে খুঁজে নিশ্চিত হওয়া, আন্দাজ নয়)।
--
-- 🔒 নিরাপদ ও একবার চালানোর কোড (V744/V1136/V1137-এর হুবহু প্রমাণিত ধাঁচ)
--   · আগে থেকে থাকলে নতুন করে বানায় না, শুধু পাসওয়ার্ড মিলিয়ে দেয়
--   · আর কারও কিছু ছোঁয় না · দুবার চালালেও কোনো ক্ষতি নেই
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================

do $$
declare
  v_code   text := 'KNE-KISHAN12';
  v_email  text := 'kne-kishan12@staff.piles';
  v_mobile text := '9679319516';
  v_role   text := 'staff';
  v_pw     text := 'staff123';
  v_branch text := 'Kishanganj';
  v_name   text := 'ISHRAT PARWEEN';
  v_uid    uuid;
  has_provider_id boolean;
begin
  select exists(
    select 1 from information_schema.columns
    where table_schema='auth' and table_name='identities' and column_name='provider_id'
  ) into has_provider_id;

  select id into v_uid from auth.users where email = v_email;

  if v_uid is null then
    v_uid := gen_random_uuid();
    insert into auth.users (
      instance_id,id,aud,role,email,encrypted_password,email_confirmed_at,
      created_at,updated_at,raw_app_meta_data,raw_user_meta_data,
      confirmation_token,recovery_token,email_change_token_new,email_change
    ) values (
      '00000000-0000-0000-0000-000000000000',v_uid,'authenticated','authenticated',
      v_email,crypt(v_pw,gen_salt('bf')),now(),now(),now(),
      '{"provider":"email","providers":["email"]}'::jsonb,'{}'::jsonb,'','','',''
    );
    if has_provider_id then
      insert into auth.identities
        (id,user_id,provider_id,identity_data,provider,last_sign_in_at,created_at,updated_at)
      values
        (gen_random_uuid(),v_uid,v_uid::text,
         jsonb_build_object('sub',v_uid::text,'email',v_email),'email',now(),now(),now());
    else
      insert into auth.identities
        (id,user_id,identity_data,provider,last_sign_in_at,created_at,updated_at)
      values
        (v_uid,v_uid,jsonb_build_object('sub',v_uid::text,'email',v_email),'email',now(),now(),now());
    end if;
  else
    update auth.users
       set encrypted_password = crypt(v_pw, gen_salt('bf')), updated_at = now()
     where id = v_uid;
  end if;

  insert into hr.app_identity(uid, person_code, link_mobile, role_kind, is_master)
  values (v_uid, v_code, v_mobile, v_role, false)
  on conflict (uid) do update set
    person_code = excluded.person_code, link_mobile = excluded.link_mobile,
    role_kind   = excluded.role_kind,   is_master   = excluded.is_master;

  insert into hr.staff_profiles(person_code, link_mobile, full_name, role_kind, branch)
  values (v_code, v_mobile, v_name, v_role, v_branch)
  on conflict (person_code) do nothing;
end $$;

notify pgrst, 'reload schema';
