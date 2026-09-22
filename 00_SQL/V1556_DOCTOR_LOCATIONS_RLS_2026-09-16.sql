-- wn.doctor_locations-এ RLS বসানো হলো (TK-র নতুন সিদ্ধান্ত, ১৬.০৯.২০২৬) --
-- আগে ইচ্ছে করেই খোলা রাখা হয়েছিল, এখন বাকি hr/wn/fin টেবিলের মতোই বন্ধ করা হচ্ছে।
-- ⛔ কোনো ডেটা বদলায়/মোছে না -- শুধু কে পড়তে/লিখতে পারবে সেই নিয়ম বসছে।
-- এই টেবিলের একমাত্র চাবি "mobile" (person_code নয়), তাই আগে থেকে চালু
-- fin.my_mobile()-ই ব্যবহার করা হলো (fin.partners-এর জন্য বানানো, কিন্তু
-- এটা শুধু "আমার নিজের মোবাইল কী" বলে -- যেকোনো schema-র টেবিলে ব্যবহার করা যায়,
-- নতুন করে বানানোর দরকার নেই)।
-- read: মাস্টার সবার লোকেশন দেখবেন (Partner Shares-এর "Location" বোতাম),
--       ডাক্তার শুধু নিজেরটা।
-- write: ডাক্তারের ফোন থেকে নিজের মোবাইল-নম্বরের সারিটাই স্বয়ংক্রিয়ভাবে
--        বসে (captureIfPossible, DoctorLocation.kt) -- তাই ডাক্তারকে নিজের
--        সারি লিখতে দেওয়া হলো, মাস্টারকেও (দরকার হলে) সব সারি।

alter table wn.doctor_locations enable row level security;
alter table wn.doctor_locations force row level security;

drop policy if exists dl_all on wn.doctor_locations;
create policy dl_all on wn.doctor_locations for all
  using ( hr.is_master() or mobile = fin.my_mobile() )
  with check ( hr.is_master() or mobile = fin.my_mobile() );
