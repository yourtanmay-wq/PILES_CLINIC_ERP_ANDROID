-- শুধু দেখার জন্য (SELECT) -- V1562-এ শুধু patients/payments যাচাই করা হয়েছিল,
-- কিন্তু কোড ধরে দেখা গেছে যে টোকেন-বদলটা আসলে এই একই ফাইলের (SupabaseClient.kt)
-- সব টেবিলের কলেই একসাথে চলে (followups, medical, enquiries, usercredentials
-- ইত্যাদি সবগুলোই একই সাধারণ ফাংশন ব্যবহার করে) -- শুধু patients/payments নয়।
-- তাই এখন public স্কিমার **প্রতিটা** টেবিলে "anon" আর "authenticated"-এর
-- অনুমতি মেলে কিনা একসাথে যাচাই করা হচ্ছে -- কোথাও না মিললে নিচের তালিকায়
-- সেই টেবিলটা "MISMATCH" হয়ে উঠে আসবে। কিচ্ছু বদলায় না, শুধু পড়া হয়।
select
  table_name,
  privilege_type,
  bool_or(grantee = 'anon') as anon_has_it,
  bool_or(grantee = 'authenticated') as authenticated_has_it,
  case when bool_or(grantee = 'anon') = bool_or(grantee = 'authenticated')
       then 'OK' else 'MISMATCH' end as status
from information_schema.role_table_grants
where table_schema = 'public'
  and grantee in ('anon', 'authenticated')
group by table_name, privilege_type
order by status desc, table_name, privilege_type;
