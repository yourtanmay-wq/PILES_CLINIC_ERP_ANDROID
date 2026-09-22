-- শুধু দেখার জন্য (SELECT) -- patients/payments টেবিলে "anon" আর "authenticated"
-- দুই role-এর অনুমতি এখন হুবহু একই কিনা যাচাই। patients/payments-এর কোডে
-- আসল JWT (লগইনের) বসানোর আগে এটা নিশ্চিত হওয়া দরকার -- না মিললে সেই বদলটাই
-- হঠাৎ সবার পড়া/লেখা আটকে দিতে পারে (RLS চালু হওয়ার অনেক আগেই)।
-- কিচ্ছু বদলায় না, শুধু পড়া হয়।
select table_name, grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public'
  and table_name in ('patients', 'payments')
  and grantee in ('anon', 'authenticated')
order by table_name, privilege_type, grantee;
