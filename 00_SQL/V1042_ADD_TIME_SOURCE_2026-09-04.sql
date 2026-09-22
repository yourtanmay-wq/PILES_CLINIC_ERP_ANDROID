-- V1042 · ০৪.০৯.২০২৬ — TK-নির্দেশ (TK নিজে চালিয়েছেন, "Success. No rows returned")
--
-- কেন: কলটা চেম্বারের ফোনে এলে অ্যাপ কল-লগ দেখে নিজেই সময় বুঝে নেয় (auto)।
-- কিন্তু কলটা স্টাফের নিজের ফোনে এলে অ্যাপ কিছুই জানে না — স্টাফ হাতে ফর্ম ভরে
-- বেছে দেয় (hand)। এতদিন শুধু `timeType` জমা হত, কোনটা কোন পথে এসেছে তা নয়।
--
-- ⛔ পুরনো সারিতে ঘরটা ফাঁকা থাকবে — অ্যাপ কোনোদিনই জানবে না ওগুলো কোন পথে
--    এসেছিল, তাই আন্দাজে কিছু বসানো হয়নি।
alter table public.enquiries add column if not exists "timeSource" text;
alter table public.patients  add column if not exists "timeSource" text;
alter table public.followups add column if not exists "timeSource" text;
notify pgrst, 'reload schema';
