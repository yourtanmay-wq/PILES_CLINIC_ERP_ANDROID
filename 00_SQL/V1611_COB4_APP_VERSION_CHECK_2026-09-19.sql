-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র রিপোর্ট (১৯.০৯.২০২৬): COB-4 বলছেন Chamber Date বন্ধ হয় না,
-- Treatment Progress লিখলে কাজ হয় না। এই একই উপসর্গ পুরনো ভার্সনে
-- (আজকের সারানো সমস্যা V439/V810/V938/V1157-এর আগে) হতো। আগে COB-4
-- পুরনো ভার্সনে ছিলেন (১২.০৯.২০২৬-এর তালিকায় ধরা পড়েছিল) — এখনো তাই
-- কিনা সরাসরি দেখা হচ্ছে।

select person_code, full_name, branch, app_version_code, app_seen_at
from hr.staff_profiles
where person_code ilike '%COB-4%' or person_code ilike '%COB4%';
