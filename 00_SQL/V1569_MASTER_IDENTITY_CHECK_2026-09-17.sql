-- শুধু দেখার জন্য (SELECT) -- TK নিজেই নিশ্চিত করেছেন এটা মাস্টারের ফোন,
-- অথচ Staff Profiles খুললে মাস্টারের বদলে COB-4 (BULTI SINGHA)-র নিজের
-- পাতা দেখাচ্ছে বারবার। তার মানে মাস্টারের ফোনটা module-এ (hr/wn/fin
-- স্কিমা) মাস্টার হিসেবে না ঢুকে অন্য কোনো পরিচয়ে ঢুকছে -- এটা কেন হচ্ছে
-- বোঝার জন্য মাস্টারের নিজের identity সারিটা যাচাই করা হচ্ছে।
-- কিচ্ছু বদলায় না, শুধু পড়া হয়।
select uid, person_code, link_mobile, role_kind, is_master, created_at
from hr.app_identity
where is_master = true
   or link_mobile = '8001080080'
   or person_code ilike '%master%'
   or person_code ilike '%tk%biswas%'
order by created_at;
