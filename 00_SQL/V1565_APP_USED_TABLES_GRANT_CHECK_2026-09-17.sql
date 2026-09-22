-- শুধু দেখার জন্য (SELECT) -- V1564-এর ফলাফল মাঝপথে (বর্ণমালার 'd' পর্যন্ত)
-- কাটা পড়ে গিয়েছিল (সম্ভবত Supabase SQL Editor-এর নিজের সারি-সীমা), তাই
-- patients/payments/followups-এর মতো আসল টেবিলগুলোই দেখা যায়নি।
-- কোড খুঁজে (আন্দাজে নয়) বার করা হলো SupabaseClient.kt-এর মাধ্যমে অ্যাপ
-- আসলে ঠিক কোন ২১টা টেবিল পড়ে/লেখে -- শুধু সেগুলোই এখানে যাচাই হচ্ছে,
-- পুরনো/বাতিল ব্যাকআপ টেবিল (bak_*, demo_backup_* ইত্যাদি) বাদ দিয়ে।
-- কিচ্ছু বদলায় না, শুধু পড়া হয়।
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
  and table_name in (
    'activity_logs','backdate_payment_grants','briefings','call_remarks',
    'chamber_close','device_logins','device_tokens','dialer_calls',
    'doctor_visits','enquiries','followups','medical','message_log',
    'patients','payment_backdate_requests','payment_edit_requests',
    'payments','products','referral_edit_requests','trash','usercredentials'
  )
group by table_name, privilege_type
order by (case when bool_or(grantee='anon') != bool_or(grantee='authenticated') then 0 else 1 end),
         table_name, privilege_type;
