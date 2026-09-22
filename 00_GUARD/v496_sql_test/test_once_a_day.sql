create or replace function pg_temp.as_user(c text) returns void language sql as $$ delete from hr._who; insert into hr._who values(c); $$;
delete from wn.notebook_days;
select pg_temp.as_user('KNE-LAXMI');
\echo '── প্রথমবার ──'
select status, check_in from wn.mark_check_in();
\echo '── সঙ্গে সঙ্গে দ্বিতীয়বার (একই মিনিট) ──'
select status, check_in from wn.mark_check_in();
\echo '── তৃতীয়বার ──'
select status, check_in from wn.mark_check_in();
\echo '── পুরনো সময় (07:05) বসিয়ে আবার চাপা ──'
update wn.notebook_days set check_in='07:05' where staff_code='KNE-LAXMI';
select status, check_in, message from wn.mark_check_in();
\echo '── ডেটাবেসে সত্যিই কী আছে ──'
select staff_code, work_date, check_in, check_out, branch from wn.notebook_days;
