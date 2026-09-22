-- V1497 (15.09.2026, TK-রিপোর্ট) — JPE-CRP সারাদিন এক চেম্বারেই বসে ছিলেন,
-- অথচ অ্যাপে "Distance 47.4 km" দেখাচ্ছিল। আসল কারণ: V1471 (14.09.2026)-এ
-- GPS-নিখুঁততার সীমা ঢিলা করাতে ঘরের ভিতরের স্বাভাবিক GPS-কাঁপুনিই কিলোমিটার
-- হিসেবে জমা হয়ে যাচ্ছিল (অ্যাপের কোডে V1497-এ ঠিক করা হয়েছে — এখন শুধু
-- RUPAM/ARMAN "Field Visit" বাছলেই কিলোমিটার গুনবে)। এই দোষ শুধু JPE-CRP-তে
-- নয়, RUPAM/ARMAN ছাড়া বাকি **সব স্টাফের** ১৪.০৯ ও ১৫.০৯-এর সারিতেই হতে
-- পারত (একই কারণ, একই কোড-পথ) — তাই একই ধাক্কায় সবার জন্য সারানো হলো।
-- ⛔ started_at/ended_at/last_lat/last_lng (হাজিরা ও শেষ-অবস্থান) কিছুই
--    ছোঁয়া হয়নি, শুধু distance_m ও route_points শূন্য/ফাঁকা করা হলো।
-- ⛔ RUPAM (JPE-RUPAM)/ARMAN (COB-ARMAN) — সত্যিকারের বাইক-স্টাফ — বাদ,
--    তাঁদের আসল কিলোমিটার এই SQL ছুঁচ্ছে না।

update wn.field_visit_days
set distance_m = 0,
    route_points = null
where staff_code not in ('JPE-RUPAM', 'COB-ARMAN')
  and work_date >= date '2026-09-14';
