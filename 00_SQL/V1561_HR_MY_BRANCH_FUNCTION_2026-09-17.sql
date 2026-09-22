-- RLS বড় কাজের ধাপ ২ (শুধু প্রস্তুতি, আচরণ বদলায় না)
-- V1560-এ যোগ করা branch ঘর থেকে "আমার নিজের ব্রাঞ্চ কী" বলার একটা ছোট
-- ফাংশন -- হুবহু hr.is_master()/hr.my_code()/fin.my_mobile()-এর একই
-- প্রমাণিত ধাঁচ (আগে থেকে production-এ চালু, নতুন কিছু আবিষ্কার নয়)।
-- ⛔ এখনো কোনো নীতি (policy) এই ফাংশন ডাকছে না -- তাই এই মুহূর্তে অ্যাপের
-- কোনো আচরণ বদলাবে না। শুধু পরের ধাপে ব্যবহারের জন্য তৈরি রাখা হলো।

create or replace function hr.my_branch() returns text
  language sql stable security definer set search_path = hr, public as $$
  select coalesce((select branch from hr.app_identity where uid = auth.uid()), '');
$$;
revoke all on function hr.my_branch() from public, anon;
grant execute on function hr.my_branch() to authenticated;

-- যাচাই (শুধু দেখার জন্য, কিছু বদলায় না) -- ফাংশনটা ঠিকভাবে বসেছে কিনা
select proname from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'hr' and p.proname = 'my_branch';
