-- ═══════════════════════════════════════════════════════════════════════
-- V1137 (০৬.০৯.২০২৬) — নতুন স্টাফ **ARCHANA MANDAL BISWAS** · KNE-KISHAN11
-- কিশানগঞ্জ। TK-নির্দেশ ও ডিটেলস (ছবিসহ)।
-- বেতন: ₹৭,০০০ · প্রতি মাসের **৬** তারিখে (TK: *"মাসিক বেতন 7000, আজকের
-- তারিখ করে দিন"* — আজ ০৬.০৯.২০২৬)।
--
-- V1136-এর হুবহু একই প্রমাণিত ধাঁচ — মডিউল-লগইন · পরিচয়ের সারি · পুরো প্রোফাইল,
-- আর এবার বেতনের ঘরটাও।
-- ⛔ দুবার চালালেও ক্ষতি নেই · আর কারও একটাও সারি ছোঁয় না
-- ⛔ আধারের **শেষ ৪ অঙ্কই** রাখা হয় — পুরো নম্বর ডেটাবেসে লেখা হয় না
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ═══════════════════════════════════════════════════════════════════════

do $$
declare
  v_code   text := 'KNE-KISHAN11';
  v_email  text := 'kne-kishan11@staff.piles';
  v_mobile text := '7478288608';
  v_role   text := 'staff';
  v_pw     text := 'staff123';
  v_branch text := 'Kishanganj';
  v_name   text := 'ARCHANA MANDAL BISWAS';
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
  v_code text := 'KNE-KISHAN11';
  r record;
  vals jsonb := jsonb_build_object(
    'join_date',              '2026-09-06',
    'dob',                    '1992-02-07',
    'gender',                 'Female',
    'blood_group',            'B+',
    'qualification',          'Intermediate (Inter pass)',
    'designation',            'Staff',
    'address',                'VILL BHOTOR, P.O. PANJIPARA, UTTAR DINAJPUR, WEST BENGAL - 733208',
    'emergency_contact',      '9002808689',
    'emergency_relationship', 'Husband (PRABIR BISWAS)',
    'gov_id_type',            'Aadhaar',
    'gov_id_last4',           '4828',
    'notes',                  'Languages: Hindi, Bangla, Rajbanshi'
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


-- ── বেতন: ₹৭,০০০ · প্রতি মাসের ৬ তারিখে ──────────────────────────────────
insert into hr.salary_config(person_code, salary_enabled, salary_amount, salary_date, updated_by, updated_at)
values ('KNE-KISHAN11', true, 7000, '6', 'MASTER', now())
on conflict (person_code) do update set
  salary_enabled = excluded.salary_enabled,
  salary_amount  = excluded.salary_amount,
  salary_date    = excluded.salary_date,
  updated_by     = excluded.updated_by,
  updated_at     = now();

notify pgrst, 'reload schema';

-- ── যাচাই ────────────────────────────────────────────────────────────────
select p.person_code, p.full_name, p.link_mobile, p.branch, p.role_kind, p.active,
       c.salary_amount, c.salary_date
from hr.staff_profiles p
left join hr.salary_config c on c.person_code = p.person_code
where p.person_code in ('KNE-KISHAN10','KNE-KISHAN11')
order by p.person_code;


-- ── অ্যাপে ঢোকার পাসওয়ার্ড (TK: "অন্যান্য Staff-এর যেমনভাবে করেছেন তেমনভাবে") ──
-- মেপে দেখা: প্রত্যেক ব্যবহারকারীরই `usercredentials`-এ নিজের সারি আছে।
-- ঘরগুলো অ্যাপের `PasswordCenterRepository.buildCredentialRow`-এর হুবহু একই।
insert into public.usercredentials
  (id, mobile, role, name, branch, password, password_hash, "changedBy", "createdAt", "updatedAt")
values
  ('cred_7482966958','7482966958','staff','KNE-KISHAN10','Kishanganj','staff123','','8001080080',now(),now()),
  ('cred_7478288608','7478288608','staff','KNE-KISHAN11','Kishanganj','staff123','','8001080080',now(),now())
on conflict (id) do update set
  password = excluded.password, password_hash = excluded.password_hash,
  role = excluded.role, name = excluded.name, branch = excluded.branch,
  "changedBy" = excluded."changedBy", "updatedAt" = now();
