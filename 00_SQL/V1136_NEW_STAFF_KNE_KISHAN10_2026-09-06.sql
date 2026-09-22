-- ═══════════════════════════════════════════════════════════════════════
-- V1136 (০৬.০৯.২০২৬) — নতুন স্টাফ **MANISHA LAKRA** · KNE-KISHAN10 · কিশানগঞ্জ
-- TK-নির্দেশ ও ডিটেলস (ছবিসহ), কোড TK নিজে চূড়ান্ত করেছেন।
--
-- এই একটা ফাইল চালালেই তিনটে কাজ হয়:
--   ১) মডিউল-লগইন (Work Notebook · Profile · Finance) — V744-এর প্রমাণিত ধাঁচ
--   ২) `hr.app_identity` — কোন লগইন কোন কোড
--   ৩) `hr.staff_profiles` — পুরো প্রোফাইল (জন্ম · ঠিকানা · জরুরি নম্বর …)
--
-- 🔒 নিরাপদ:
--   · আগে থেকে থাকলে নতুন বানায় না, শুধু মিলিয়ে দেয় — দুবার চালালেও ক্ষতি নেই
--   · আর কারও একটাও সারি ছোঁয় না
--   · প্রোফাইলের বাড়তি ঘরগুলো **থাকলে তবেই** ভরা হয় (কলাম না থাকলে চুপচাপ বাদ)
--   · আধারের **শেষ ৪ অঙ্কই** রাখা হয় — পুরো নম্বর ডেটাবেসে লেখা হয় না
--
-- ⚠️ বেতন এখানে বসানো হয়নি — TK অঙ্ক ও তারিখ দিলে আলাদা করে বসবে।
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ═══════════════════════════════════════════════════════════════════════

do $$
declare
  v_code   text := 'KNE-KISHAN10';
  v_email  text := 'kne-kishan10@staff.piles';   -- codeToEmail() যা বানায়, হুবহু তাই
  v_mobile text := '7482966958';
  v_role   text := 'staff';
  v_pw     text := 'staff123';
  v_branch text := 'Kishanganj';
  v_name   text := 'MANISHA LAKRA';
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

  insert into hr.staff_profiles(person_code, link_mobile, full_name, role_kind, branch, active)
  values (v_code, v_mobile, v_name, v_role, v_branch, true)
  on conflict (person_code) do update set
    link_mobile = excluded.link_mobile, full_name = excluded.full_name,
    role_kind = excluded.role_kind, branch = excluded.branch, active = true;
end $$;


-- ── প্রোফাইলের বাকি ঘর — যে কলামগুলো সত্যিই আছে, শুধু সেগুলোই ভরা হয় ──────
do $$
declare
  v_code text := 'KNE-KISHAN10';
  r record;
  vals jsonb := jsonb_build_object(
    'join_date',              '2026-09-03',
    'dob',                    '2005-11-20',
    'gender',                 'Female',
    'blood_group',            'B+',
    'qualification',          '12th (1st Division)',
    'designation',            'Staff',
    'address',                'WARD NO. 20, KADAM RASUL COLONY, HALIM CHOWK, KISHANGANJ, BIHAR - 855108',
    'emergency_contact',      '6206164604',
    'emergency_relationship', 'Mother (GAYA DEVI)',
    'gov_id_type',            'Aadhaar',
    'gov_id_last4',           '2766',
    'notes',                  'Languages: Hindi, Rajbanshi'
  );
begin
  for r in select key, value from jsonb_each_text(vals) loop
    if exists (select 1 from information_schema.columns
                where table_schema='hr' and table_name='staff_profiles'
                  and column_name = r.key) then
      execute format('update hr.staff_profiles set %I = $1 where person_code = $2', r.key)
        using r.value, v_code;
    end if;
  end loop;
  if exists (select 1 from information_schema.columns
              where table_schema='hr' and table_name='staff_profiles'
                and column_name = 'updated_at') then
    update hr.staff_profiles set updated_at = now() where person_code = v_code;
  end if;
end $$;

notify pgrst, 'reload schema';

-- ── যাচাই — সারিটা ঠিকঠাক বসল কিনা ───────────────────────────────────────
select person_code, full_name, link_mobile, branch, role_kind, active
from hr.staff_profiles where person_code = 'KNE-KISHAN10';


-- ── বেতন (TK-নিশ্চিত ০৬.০৯.২০২৬): ₹৭,০০০ · প্রতি মাসের ৬ তারিখে ──────────
insert into hr.salary_config(person_code, salary_enabled, salary_amount, salary_date, updated_by, updated_at)
values ('KNE-KISHAN10', true, 7000, '6', 'MASTER', now())
on conflict (person_code) do update set
  salary_enabled = excluded.salary_enabled,
  salary_amount  = excluded.salary_amount,
  salary_date    = excluded.salary_date,
  updated_by     = excluded.updated_by,
  updated_at     = now();

notify pgrst, 'reload schema';
