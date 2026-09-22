drop schema if exists wn cascade; drop schema if exists hr cascade;
create schema wn; create schema hr;
create table hr.staff_profiles(person_code text primary key, link_mobile text, role_kind text,
  branch text, active boolean not null default true, suspended_until date);
create table wn.notebook_days(id serial primary key, staff_code text not null, staff_mobile text,
  branch text, work_date date not null, check_in text, check_out text,
  created_at timestamptz default now(), updated_at timestamptz default now(),
  unique(staff_code, work_date));
create table wn.leave_requests(id serial primary key, staff_code text not null, leave_date date not null,
  status text not null default 'pending');
-- কে লগইন করা, সেটা পরীক্ষায় বদলানোর জন্য
create table hr._who(code text);
create or replace function hr.my_code() returns text language sql stable as $$ select coalesce((select code from hr._who limit 1),'') $$;
create or replace function hr.is_master() returns boolean language sql stable as $$ select false $$;
insert into hr.staff_profiles(person_code, link_mobile, role_kind, branch) values
  ('KNE-LAXMI','9800000001','staff','Kishanganj'),
  ('KNE-FIELD','9800000005','field','Kishanganj'),
  ('KNE-DOC',  '9800000002','doctor','Kishanganj'),
  ('MASTER-1', '9800000003','master','All'),
  ('KNE-OUT',  '9800000004','staff','Kishanganj'),
  ('KNE-SUSP', '9800000006','staff','Kishanganj'),
  ('KNE-LEAVE','9800000007','staff','Kishanganj');
update hr.staff_profiles set active=false where person_code='KNE-OUT';
update hr.staff_profiles set suspended_until=(now() at time zone 'Asia/Kolkata')::date + 3 where person_code='KNE-SUSP';
insert into wn.leave_requests(staff_code, leave_date, status)
  values('KNE-LEAVE',(now() at time zone 'Asia/Kolkata')::date,'approved');
