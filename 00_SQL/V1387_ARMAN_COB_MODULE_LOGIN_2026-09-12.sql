-- ============================================================================
-- V1387 — ARMAN HOQUE (COB-ARMAN, কোচবিহার) — মডিউল-লগইন তৈরি
-- TK-রিপোর্ট (ভিডিও, ১২.০৯.২০২৬): Work Notebook খুলতে গেলে
--   "Could not open — Step 1 (login) — server said HTTP 400: Invalid login
--    credentials, Code: COB-ARMAN"।
--
-- 🐞 আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয় — হুবহু V744/ROHINI-র একই ধরনের ঘটনা):
--   অ্যাপে দুটো আলাদা লগইন — ১) সাধারণ লগইন (মোবাইল+পাসওয়ার্ড, StaffDirectory.kt) —
--   V1377-এ ARMAN-কে এখানে যোগ করা হয়েছিল, এটা কাজ করছে। ২) মডিউল-লগইন
--   (Work Notebook/Profile/Finance) — এটা Supabase-এর নিজের auth ব্যবহার করে,
--   ইমেল `cob-arman@staff.piles` আর পাসওয়ার্ড `staff123` দিয়ে। V1377-এ এই
--   দ্বিতীয় লগইনটা তৈরিই হয়নি — তাই ব্যর্থ হচ্ছে।
--
-- 🔒 নিরাপদ ও একবার চালানোর কোড (V744-এর হুবহু প্রমাণিত ধাঁচ, শুধু ARMAN-এর জন্য)
--   · আগে থেকে থাকলে নতুন করে বানায় না, শুধু পাসওয়ার্ড মিলিয়ে দেয়
--   · আর কারও কিছু ছোঁয় না · দুবার চালালেও কোনো ক্ষতি নেই
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================

do $$
declare
  v_code   text := 'COB-ARMAN';
  v_email  text := 'cob-arman@staff.piles';   -- codeToEmail() যা বানায়, হুবহু তাই
  v_mobile text := '9883884394';
  v_role   text := 'staff';
  v_pw     text := 'staff123';
  v_branch text := 'Cooch Behar';
  v_name   text := 'ARMAN HOQUE';
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

  -- প্রোফাইলের সারি (না থাকলে বানাও; থাকলে ছুঁয়ো না)
  insert into hr.staff_profiles(person_code, link_mobile, full_name, role_kind, branch)
  values (v_code, v_mobile, v_name, v_role, v_branch)
  on conflict (person_code) do nothing;
end $$;

notify pgrst, 'reload schema';
