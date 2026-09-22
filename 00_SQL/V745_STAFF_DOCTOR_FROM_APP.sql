-- ============================================================================
-- V745 — অ্যাপ থেকেই স্টাফ ও ডাক্তার যোগ / বাদ / ফেরানো
-- TK-নির্দেশ (২৭.০৮.২০২৬): *"শুধু staff কেন ডাক্তারও যোগ বিয়োগ করা যাবে তার
--   ব্যবস্থা করুন — তবে খুব সাবধান এবং নিরাপদে।"*
--
-- 🎯 এটা একবার চালালে **ভবিষ্যতে আর কোনোদিন SQL লাগবে না** —
--    মাস্টার অ্যাপ থেকেই সব করতে পারবেন।
--
-- 🔒 নিরাপত্তা (প্রতিটা যাচাই করা হয়েছে):
--   · **শুধু মাস্টার** ডাকতে পারেন — সার্ভারেই আটকানো
--   · অ্যাপে কোনো গোপন চাবি লাগে না (anon চাবিতেই চলে)
--   · **মাস্টারকে বাদ দেওয়া যায় না** — ডেটাবেসেই আটকানো
--   · **কাউকে সত্যিকারের মোছা হয় না** — শুধু নিষ্ক্রিয়, তাই পুরনো
--     রেকর্ডে নাম চিরকাল থেকে যায়
--   · টাকার কোনো হিসাব (fin.*) ছোঁয়া হয় না — সেসব মোবাইল ধরে চলে (V308)
--
-- ⛔ পুরনো কোনো টেবিল/তথ্য/নিয়ম বদলায় না — শুধু নতুন ৪টে ফাংশন যোগ।
-- ⛔ দুবার চালালেও কোনো ক্ষতি নেই।
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================


-- ── ১. লগইনের তালিকা (লগইনের **আগেই** লাগে, তাই security definer) ──────────
--    ⛔ শুধু যেটুকু লগইনে দরকার — নাম · মোবাইল · ব্রাঞ্চ · ভূমিকা।
--       ঠিকানা · আধার · বেতন — কিচ্ছু আসে না।
create or replace function public.staff_login_list()
returns table(mobile text, person_code text, full_name text, branch text, role_kind text)
language sql stable security definer set search_path = hr, public as $$
  select link_mobile, person_code, full_name, branch, role_kind
    from hr.staff_profiles
   where coalesce(active, true)
     and coalesce(link_mobile,'') <> ''
     and coalesce(role_kind,'') in ('staff','doctor','field')
$$;
revoke all on function public.staff_login_list() from public;
grant execute on function public.staff_login_list() to anon, authenticated;


-- ── ২. নতুন স্টাফ / ডাক্তার যোগ (মাস্টার-গেটেড) ────────────────────────────
create or replace function hr.admin_create_person(
  p_code text, p_mobile text, p_name text, p_branch text, p_role text)
returns jsonb
language plpgsql security definer set search_path = hr, auth, public as $$
declare
  v_code text := upper(btrim(coalesce(p_code,'')));
  v_mob  text := regexp_replace(coalesce(p_mobile,''), '[^0-9]', '', 'g');
  v_role text := lower(btrim(coalesce(p_role,'')));
  v_email text; v_pw text; v_uid uuid; v_has_provider boolean;
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


-- ── ৩. বাদ দেওয়া / ফেরানো (মাস্টার-গেটেড) ─────────────────────────────────
--    ⛔ মোছা হয় না — শুধু active চালু/বন্ধ। পুরনো রেকর্ডে নাম অটুট থাকে,
--       আর বাদ দিলে লগইন নিজে থেকেই বন্ধ হয় (V403-এর প্রমাণিত নিয়ম)।
create or replace function hr.admin_set_person_active(p_code text, p_active boolean)
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare v_code text := upper(btrim(coalesce(p_code,''))); v_role text; v_n int;
begin
  if not hr.is_master() then
    return jsonb_build_object('ok', false, 'message', 'Only Master can do this');
  end if;

  -- 🔒 পাহারা — মাস্টারকে কখনো বাদ দেওয়া যাবে না (নইলে কেউ ঢুকতেই পারবে না)।
  --    ⚠️ এই যাচাইটা **সবার আগে** — পরীক্ষায় দেখা গেল মাস্টারের
  --       staff_profiles সারি না থাকলে নিচের যাচাই "Not found" বলে থেমে যেত,
  --       ফলে আসল কারণটা বোঝাই যেত না।
  if exists (select 1 from hr.app_identity
              where person_code = v_code and is_master) then
    return jsonb_build_object('ok', false, 'message', 'Master cannot be removed');
  end if;

  select role_kind into v_role from hr.staff_profiles where person_code = v_code;
  if v_role is null then
    return jsonb_build_object('ok', false, 'message', 'Not found');
  end if;

  update hr.staff_profiles set active = coalesce(p_active, true)
   where person_code = v_code;
  get diagnostics v_n = row_count;

  return jsonb_build_object('ok', v_n > 0,
    'message', case when p_active then 'Restored' else 'Removed — login is now off' end);
end $$;
revoke all on function hr.admin_set_person_active(text, boolean) from public, anon;
grant execute on function hr.admin_set_person_active(text, boolean) to authenticated;


-- ── ৪. মাস্টারের দেখার তালিকা (বাদ-দেওয়া লোকও দেখায়, ফেরাতে পারেন) ────────
create or replace function hr.admin_people_list()
returns table(person_code text, full_name text, link_mobile text,
              branch text, role_kind text, active boolean)
language sql stable security definer set search_path = hr, public as $$
  select person_code, full_name, link_mobile, branch, role_kind, coalesce(active,true)
    from hr.staff_profiles
   where hr.is_master()
     and coalesce(role_kind,'') in ('staff','doctor','field')
   order by coalesce(active,true) desc, branch, full_name
$$;
revoke all on function hr.admin_people_list() from public, anon;
grant execute on function hr.admin_people_list() to authenticated;

notify pgrst, 'reload schema';
