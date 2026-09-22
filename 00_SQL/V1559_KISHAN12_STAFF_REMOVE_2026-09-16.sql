-- ISHRAT PARWEEN (KNE-KISHAN12, মোবাইল 9679319516) কাজ ছেড়ে দিয়েছেন (TK-নির্দেশ ১৬.০৯.২০২৬)।
-- অ্যাপের বাঁধা তালিকা (StaffDirectory.kt/config.js) থেকে ইতিমধ্যে সরানো হয়েছে।
-- এখন ক্লাউডেও (hr.staff_profiles) বন্ধ করা হচ্ছে, নইলে suspended_until_for()/
-- staff_login_list() এখনো তাকে সচল হিসেবে দেখাতে/লগইন করাতে পারত।
-- ⛔ পুরনো কাজ/হাজিরা/বেতনের রেকর্ড কিছুই মোছা হয় না -- শুধু active=false।
update hr.staff_profiles
set active = false, updated_at = now()
where person_code = 'KNE-KISHAN12' and link_mobile = '9679319516';
