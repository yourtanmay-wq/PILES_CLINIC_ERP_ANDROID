-- শুধু দেখার জন্য (SELECT) -- যাদের পাসওয়ার্ড এখনো নিরাপদ (hash) হয়নি, তাদের নাম-তালিকা
select mobile, role, name, branch
from public.usercredentials
where password_hash is null or password_hash = ''
order by mobile;
