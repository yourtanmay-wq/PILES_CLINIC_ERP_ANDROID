\pset border 2
\pset format aligned
create or replace function pg_temp.as_user(c text) returns void language sql as $$ delete from hr._who; insert into hr._who values(c); $$;

\echo '── ১. সাধারণ Staff — প্রথমবার ──'
select pg_temp.as_user('KNE-LAXMI');
select status, check_in, branch, message from wn.mark_check_in();

\echo '── ২. একই Staff — দ্বিতীয়বার (সময় বদলায় কি?) ──'
select status, check_in, message from wn.mark_check_in();

\echo '── ৩. ডাক্তার ──'
select pg_temp.as_user('KNE-DOC');
select status, check_in, message from wn.mark_check_in();

\echo '── ৪. মাস্টার ──'
select pg_temp.as_user('MASTER-1');
select status, message from wn.mark_check_in();

\echo '── ৫. বাদ দেওয়া (active=false) ──'
select pg_temp.as_user('KNE-OUT');
select status, message from wn.mark_check_in();

\echo '── ৬. Suspend করা ──'
select pg_temp.as_user('KNE-SUSP');
select status, message from wn.mark_check_in();

\echo '── ৭. আজ অনুমোদিত ছুটি ──'
select pg_temp.as_user('KNE-LEAVE');
select status, message from wn.mark_check_in();

\echo '── ৮. Field অফিসার (TK-এর নির্দেশে এখন not_staff হওয়ার কথা) ──'
select pg_temp.as_user('KNE-FIELD');
select status, check_in, message from wn.mark_check_in();

\echo '── ৯. লগইন ছাড়া (anon) ──'
delete from hr._who;
do $$ begin perform * from wn.mark_check_in(); raise notice 'FAIL — anon ঢুকে গেছে'; exception when others then raise notice 'PASS — আটকেছে: %', sqlerrm; end $$;
