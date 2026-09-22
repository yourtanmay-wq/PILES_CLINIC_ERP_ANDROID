-- hr.salary_deleted_log টেবিলটা V1195 (07.09.2026)-এ বানানো হয়েছিল, কিন্তু ওই সময়
-- বাকি hr-টেবিলগুলোর মতো RLS বসানো হয়নি -- এটাই Supabase-এর নিরাপত্তা-সতর্কতার
-- একটা অংশ। এই SQL হুবহু V246-এর প্রমাণিত (আগে থেকেই চালু) নিয়ম কপি করছে --
-- নতুন কোনো নিয়ম নয়, শুধু বাকি hr-টেবিলগুলোর সাথে মিলিয়ে দেওয়া।
-- ⛔ কোনো ডেটা বদলায়/মোছে না -- শুধু কে পড়তে/লিখতে পারবে সেই নিয়ম বসছে।
-- read: মাস্টার সব দেখবেন, বাকিরা শুধু নিজের নাম মিললে (আগের বেতন-হিসাবের মতোই)।
-- write: শুধু মাস্টার -- অ্যাপেও ভুল সারি মোছা শুধু মাস্টারের কাজ (StaffProfileActivity-তে লেখা আছে)।

alter table hr.salary_deleted_log enable row level security;
alter table hr.salary_deleted_log force row level security;

drop policy if exists sdl_read on hr.salary_deleted_log;
create policy sdl_read on hr.salary_deleted_log for select using ( hr.is_master() or person_code = hr.my_code() );
drop policy if exists sdl_write on hr.salary_deleted_log;
create policy sdl_write on hr.salary_deleted_log for all using ( hr.is_master() ) with check ( hr.is_master() );
