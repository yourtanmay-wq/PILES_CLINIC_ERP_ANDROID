-- শুধু দেখার জন্য (SELECT) -- KNE-KISHAN6 (9162625854) ক্লাউডেও (hr.staff_profiles) বন্ধ আছে কিনা যাচাই
-- (এই একটা টেবিলে আমার নিজের নকল-ডেটাবেস-পাহারা যাচাই করতে পারে না -- তাই এখানে
--  বাড়তি সাবধানে, কলাম-নাম আগের আসল কোড থেকে মিলিয়ে হাতে লেখা হলো।)
select person_code, "link_mobile" as mobile, active, updated_at
from hr.staff_profiles
where "link_mobile" = '9162625854' or person_code ilike '%KISHAN6%';
