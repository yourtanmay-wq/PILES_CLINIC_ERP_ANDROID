-- ============================================================================
-- V744 — ROHINI KUMARI (KNE-KISHAN8) — মডিউল-লগইন তৈরি
-- TK-রিপোর্ট (২৭.০৮.২০২৬, ৩টে ছবিসহ): অ্যাপে লগইন হচ্ছে ("Welcome,
--   KNE-KISHAN8"), কিন্তু ভিতরে ঢুকতে গেলে **"Could not open — Sign-in failed"**।
--
-- 🐞 আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয়):
--   অ্যাপে **দুটো আলাদা লগইন** আছে —
--     ১) সাধারণ লগইন (মোবাইল + পাসওয়ার্ড) → এটা কাজ করছে ✅
--     ২) **মডিউল-লগইন** (Work Notebook · Profile · Finance-এর জন্য) — এটা
--        Supabase-এর নিজের auth ব্যবহার করে, ইমেল `<code>@staff.piles` আর
--        স্টাফের পাসওয়ার্ড `staff123` দিয়ে।
--   V735-এ ROHINI-কে **অ্যাপে** যোগ করা হয়েছিল, কিন্তু **ডেটাবেসের এই
--   দ্বিতীয় লগইনটা তৈরি হয়নি** — তাই ২ নম্বরটা ব্যর্থ হচ্ছিল।
--
-- 🔒 নিরাপদ ও একবার চালানোর কোড:
--   · হুবহু V249-এর প্রমাণিত ধাঁচ, শুধু **একজনের** জন্য
--   · আগে থেকে থাকলে নতুন করে বানায় না, শুধু পাসওয়ার্ড মিলিয়ে দেয়
--   · **আর কারও কিছু ছোঁয় না** — অন্য ২০ জনের একটাও সারি বদলায় না
--   · দুবার চালালেও কোনো ক্ষতি নেই
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================

do $$
declare
  v_code   text := 'KNE-KISHAN8';
  v_email  text := 'kne-kishan8@staff.piles';   -- codeToEmail() যা বানায়, হুবহু তাই
  v_mobile text := '7321960416';
  v_role   text := 'staff';
  v_pw     text := 'staff123';                  -- ModuleAuth-এ স্টাফের পাসওয়ার্ড
  v_branch text := 'Kishanganj';
  v_name   text := 'ROHINI KUMARI';
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
    -- আগে থেকেই আছে — শুধু পাসওয়ার্ডটা মিলিয়ে দিই
    update auth.users
       set encrypted_password = crypt(v_pw, gen_salt('bf')), updated_at = now()
     where id = v_uid;
  end if;

  -- কে কোন কোড — মডিউল এই সারিটা দেখেই চেনে
  insert into hr.app_identity(uid, person_code, link_mobile, role_kind, is_master)
  values (v_uid, v_code, v_mobile, v_role, false)
  on conflict (uid) do update set
    person_code = excluded.person_code, link_mobile = excluded.link_mobile,
    role_kind   = excluded.role_kind,   is_master   = excluded.is_master;

  -- প্রোফাইলের সারি (না থাকলে বানাও; থাকলে ছুঁয়ো না — TK নিজে ভরেছেন)
  insert into hr.staff_profiles(person_code, link_mobile, full_name, role_kind, branch)
  values (v_code, v_mobile, v_name, v_role, v_branch)
  on conflict (person_code) do nothing;
end $$;

notify pgrst, 'reload schema';
