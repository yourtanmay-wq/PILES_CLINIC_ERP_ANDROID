-- ============================================================================
-- V747 — একই Staff Code দ্বিতীয়বার দিলে পুরনো লোকের তথ্য যেন না মোছে
--
-- TK-নির্দেশ (২৭.০৮.২০২৬): *"আমি একজন সাধারণ ব্যবহারকারী, আমার সামনে যেন
--   কোনো রকম সমস্যা না থাকে।"*
--
-- ❗ কী সমস্যা ছিল (V745-এ)
--   `admin_create_person` শেষে `on conflict (person_code) do update` করত।
--   ফলে ভুল করে **আগে থেকে থাকা** একটা কোড (যেমন KNE-LAXMI) আবার লিখে
--   নতুন নাম-নম্বর দিলে — পুরনো লোকের নাম ও মোবাইল **চুপচাপ মুছে** নতুনটা
--   বসে যেত। কোনো সতর্কবার্তা আসত না।
--
-- ✅ এখন কী হয়
--   কোডটা আগে থেকেই থাকলে **আর মোবাইল আলাদা হলে** সার্ভার সাফ বলে দেয়:
--     "This code already belongs to LAXMI. Please use a different code."
--   আর কিচ্ছু বদলায় না — এক অক্ষরও না।
--
--   ⛔ একই কোড + **একই মোবাইল** হলে আগের মতোই চলে (নাম/ব্রাঞ্চ ঠিক করা যায়) —
--      কারণ ওখানে কারও তথ্য হারানোর ভয় নেই, লোকটা একই।
--
-- ⛔ পুরনো কোনো টেবিল/তথ্য বদলায় না — শুধু ওই একটা ফাংশন নতুন করে লেখা।
-- ⛔ দুবার চালালেও কোনো ক্ষতি নেই।
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================

create or replace function hr.admin_create_person(
  p_code text, p_mobile text, p_name text, p_branch text, p_role text)
returns jsonb
language plpgsql security definer set search_path = hr, auth, public as $$
declare
  v_code text := upper(btrim(coalesce(p_code,'')));
  v_mob  text := regexp_replace(coalesce(p_mobile,''), '[^0-9]', '', 'g');
  v_role text := lower(btrim(coalesce(p_role,'')));
  v_email text; v_pw text; v_uid uuid; v_has_provider boolean;
  v_found boolean; v_old_name text; v_old_mob text;
begin
  -- 🔒 পাহারা ১ — শুধু মাস্টার
  if not hr.is_master() then
    return jsonb_build_object('ok', false, 'message', 'Only Master can add people');
  end if;

  -- 🔒 পাহারা ২ — ভূমিকা শুধু এই তিনটে। master এখান থেকে বানানো যায় না।
  if v_role not in ('staff','doctor','field') then
    return jsonb_build_object('ok', false, 'message', 'Role must be staff, doctor or field');
  end if;

  -- 🔒 পাহারা ৩ — সব ঘর ভরা চাই
  if v_code = '' or coalesce(btrim(p_name),'') = '' or coalesce(btrim(p_branch),'') = '' then
    return jsonb_build_object('ok', false, 'message', 'Code, name and branch are required');
  end if;
  if length(v_mob) <> 10 then
    return jsonb_build_object('ok', false, 'message', 'Mobile must be 10 digits');
  end if;

  -- 🔒 পাহারা ৪ — একই মোবাইল অন্য কারও নামে থাকলে চলবে না
  if exists (select 1 from hr.staff_profiles
              where link_mobile = v_mob and person_code <> v_code) then
    return jsonb_build_object('ok', false, 'message', 'This mobile is already used by someone else');
  end if;

  -- 🔒 পাহারা ৫ (V747, নতুন) — কোডটা আগে থেকেই অন্য কারও কিনা।
  --    ⚠️ `v_old_name` ফাঁকা থাকতে পারে, তাই আলাদা `v_found` পতাকা দিয়ে
  --       দেখা হয় সারিটা সত্যিই আছে কিনা — নইলে নাম না থাকা সারি ফাঁকি দিত।
  select true, full_name, coalesce(link_mobile,'')
    into v_found, v_old_name, v_old_mob
    from hr.staff_profiles where person_code = v_code;
  if coalesce(v_found, false) and v_old_mob <> v_mob then
    return jsonb_build_object('ok', false,
      'message', 'This code already belongs to '
                 || coalesce(nullif(btrim(v_old_name), ''), v_code)
                 || '. Please use a different code.');
  end if;

  v_email := lower(regexp_replace(v_code, '[^a-zA-Z0-9]+', '-', 'g')) || '@staff.piles';
  v_pw := case v_role when 'doctor' then 'doctor123'
                      when 'field'  then 'field123'
                      else 'staff123' end;

  select exists(select 1 from information_schema.columns
                 where table_schema='auth' and table_name='identities'
                   and column_name='provider_id') into v_has_provider;

  select id into v_uid from auth.users where email = v_email;
  if v_uid is null then
    v_uid := gen_random_uuid();
    insert into auth.users(instance_id,id,aud,role,email,encrypted_password,
      email_confirmed_at,created_at,updated_at,raw_app_meta_data,raw_user_meta_data,
      confirmation_token,recovery_token,email_change_token_new,email_change)
    values('00000000-0000-0000-0000-000000000000',v_uid,'authenticated','authenticated',
      v_email,crypt(v_pw,gen_salt('bf')),now(),now(),now(),
      '{"provider":"email","providers":["email"]}'::jsonb,'{}'::jsonb,'','','','');
    if v_has_provider then
      insert into auth.identities(id,user_id,provider_id,identity_data,provider,
        last_sign_in_at,created_at,updated_at)
      values(gen_random_uuid(),v_uid,v_uid::text,
        jsonb_build_object('sub',v_uid::text,'email',v_email),'email',now(),now(),now());
    else
      insert into auth.identities(id,user_id,identity_data,provider,
        last_sign_in_at,created_at,updated_at)
      values(v_uid,v_uid,jsonb_build_object('sub',v_uid::text,'email',v_email),
        'email',now(),now(),now());
    end if;
  end if;

  insert into hr.app_identity(uid, person_code, link_mobile, role_kind, is_master)
  values(v_uid, v_code, v_mob, v_role, false)
  on conflict (uid) do update set person_code = excluded.person_code,
    link_mobile = excluded.link_mobile, role_kind = excluded.role_kind;

  insert into hr.staff_profiles(person_code, link_mobile, full_name, role_kind, branch, active)
  values(v_code, v_mob, btrim(p_name), v_role, btrim(p_branch), true)
  on conflict (person_code) do update set link_mobile = excluded.link_mobile,
    full_name = excluded.full_name, role_kind = excluded.role_kind,
    branch = excluded.branch, active = true;

  return jsonb_build_object('ok', true, 'code', v_code,
    'message', 'Added — password is ' || v_pw);
end $$;
revoke all on function hr.admin_create_person(text,text,text,text,text) from public, anon;
grant execute on function hr.admin_create_person(text,text,text,text,text) to authenticated;

notify pgrst, 'reload schema';
