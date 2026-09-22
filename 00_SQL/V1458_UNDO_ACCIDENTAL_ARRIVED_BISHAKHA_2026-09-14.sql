-- V1458 (14.09.2026, TK-রিপোর্ট) — ভুল করে "Arrived" মার্ক হয়ে যাওয়া একজন রোগীর
-- সেই মার্কটা মোছা। এই রোগী চেম্বারে আজ আসেননি, ভুল করে ডান-দিকের তীর-বোতামে
-- চাপ পড়ে যায়।
--
-- ⛔ নিরাপদ কেন: "Marked Arrived" একটা ফাঁকা (₹০) সারি — অ্যাপের নিজের
-- `undoAttendanceMark()`-ও ঠিক এই একই ধরনের সারি মোছে। এই DELETE-ও একইভাবে
-- payType = 'attendance_mark' দিয়ে বাঁধা, তাই কোনো আসল টাকার সারি এতে কখনো
-- মোছা যাবে না — শুধু এই খালি "এসেছেন" চিহ্নটাই সরে।

delete from public.payments
where mobile = '+918670104488'
  and branch = 'Cooch Behar'
  and date = '2026-09-14'
  and "payType" = 'attendance_mark'
returning id, name, mobile, "payType", date;
