-- V1070 · ০৪.০৯.২০২৬ — TK-নির্দেশ (TK নিজে চালাবেন)
--
-- কেন: ডাক্তার হোয়াটসঅ্যাপে রোগীর নম্বর পাঠান, ক্লিনিক All-Branch Enquiry
-- ভরে। রোগী না এলে রেজিস্ট্রেশনই হয় না, তাই কোন RMP পাঠিয়েছিলেন তা কোথাও
-- থাকে না — পরে রেফারেল টাকা নিয়ে দ্বন্দ্ব হয়।
--
-- ⛔ ঘরের নাম Registration-এর হুবহু একই (`refBy` · `refDoctor` ·
--    `refDoctorMobile`), তাই পরে রেজিস্ট্রেশনে মানটা নিজে থেকেই যেতে পারে।
-- ⛔ পুরনো সারিতে ঘরগুলো ফাঁকা থাকবে — আন্দাজে কিছু বসানো হয়নি।
-- ⛔ কোনো ঘর বাধ্যতামূলক নয়; না ভরলে এনকোয়ারি আগের মতোই সেভ হয়।
alter table public.enquiries add column if not exists "refBy" text;
alter table public.enquiries add column if not exists "refDoctor" text;
alter table public.enquiries add column if not exists "refDoctorMobile" text;
notify pgrst, 'reload schema';
