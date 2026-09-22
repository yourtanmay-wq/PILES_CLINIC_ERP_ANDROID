-- V247: one run after V246.
-- Makes Work Notebook/Profile/Finance open from the existing app session
-- without a second visible password screen. Idempotent.

do $$
declare
  rec record;
  v_uid uuid;
  v_pw text;
  has_provider_id boolean;
begin
  select exists(
    select 1 from information_schema.columns
    where table_schema='auth' and table_name='identities' and column_name='provider_id'
  ) into has_provider_id;

  for rec in
    select * from (values
      ('MASTER-TK','master-tk@staff.piles','8001080080','master',true,'ALL'),
      ('KNE-LAXMI','kne-laxmi@staff.piles','9883605917','staff',false,'KNE'),
      ('KNE-BRANCH','kne-branch@staff.piles','8676002200','staff',false,'KNE'),
      ('KNE-KISHAN5','kne-kishan5@staff.piles','6207841890','staff',false,'KNE'),
      ('JPE-CRP','jpe-crp@staff.piles','9647840067','staff',false,'JPE'),
      ('JPE-JALPAI-13','jpe-jalpai-13@staff.piles','8101397763','staff',false,'JPE'),
      ('JPE-RUPAM','jpe-rupam@staff.piles','8167096595','staff',false,'JPE'),
      ('JPE-BRANCH','jpe-branch@staff.piles','8436002200','staff',false,'JPE'),
      ('COB-UTTAMA','cob-uttama@staff.piles','7679751521','staff',false,'COB'),
      ('COB-4','cob-4@staff.piles','7501256248','staff',false,'COB'),
      ('COB-BRANCH','cob-branch@staff.piles','8514002200','staff',false,'COB'),
      ('FLK-1','flk-1@staff.piles','9883623823','staff',false,'FLK'),
      ('FLK-BRANCH','flk-branch@staff.piles','8514001100','staff',false,'FLK'),
      ('BIR-BRANCH','bir-branch@staff.piles','8538002200','staff',false,'BIR'),
      -- 🔴 V408 (17.08.2026) — TK-এর নির্দেশ: FALA-15 কাজ ছেড়ে দিয়েছেন, ওঁর সারিটি
      --    (মোবাইল নম্বর সহ) এখান থেকে বাদ দেওয়া হলো। এই ফাইল আবার চালালেও
      --    ওই লগইন আর তৈরি হবে না। ⛔ বাকি কারও কিছু বদলায়নি।
      ('DR-KH-MANDAL','dr-kh-mandal@staff.piles','7980993652','doctor',false,'COB'),
      ('DR-JAY-BANIK','dr-jay-banik@staff.piles','8001800148','doctor',false,'JPE'),
      ('DR-AMIT-GOLDAR','dr-amit-goldar@staff.piles','9046366596','doctor',false,'KNE'),
      ('DR-PK-ROY','dr-pk-roy@staff.piles','6297625447','doctor',false,'KNE'),
      ('FIELD-OFFICER','field-officer@staff.piles','9002003540','field',false,'ALL')
    ) as t(code,email,mobile,role,is_master,branch_code)
  loop
    v_pw := rec.mobile || '@' || rec.branch_code;
    select id into v_uid from auth.users where email = rec.email;
    if v_uid is null then
      v_uid := gen_random_uuid();
      insert into auth.users (
        instance_id,id,aud,role,email,encrypted_password,email_confirmed_at,
        created_at,updated_at,raw_app_meta_data,raw_user_meta_data,
        confirmation_token,recovery_token,email_change_token_new,email_change
      ) values (
        '00000000-0000-0000-0000-000000000000',v_uid,'authenticated','authenticated',
        rec.email,crypt(v_pw,gen_salt('bf')),now(),now(),now(),
        '{"provider":"email","providers":["email"]}'::jsonb,'{}'::jsonb,'','','',''
      );
      if has_provider_id then
        insert into auth.identities
          (id,user_id,provider_id,identity_data,provider,last_sign_in_at,created_at,updated_at)
        values
          (gen_random_uuid(),v_uid,v_uid::text,jsonb_build_object('sub',v_uid::text,'email',rec.email),'email',now(),now(),now());
      else
        insert into auth.identities
          (id,user_id,identity_data,provider,last_sign_in_at,created_at,updated_at)
        values
          (v_uid,v_uid,jsonb_build_object('sub',v_uid::text,'email',rec.email),'email',now(),now(),now());
      end if;
    else
      update auth.users
      set encrypted_password=crypt(v_pw,gen_salt('bf')), updated_at=now()
      where id=v_uid;
    end if;

    insert into hr.app_identity(uid,person_code,link_mobile,role_kind,is_master)
    values(v_uid,rec.code,rec.mobile,rec.role,rec.is_master)
    on conflict(uid) do update set
      person_code=excluded.person_code,link_mobile=excluded.link_mobile,
      role_kind=excluded.role_kind,is_master=excluded.is_master;
  end loop;
end $$;

notify pgrst, 'reload schema';
