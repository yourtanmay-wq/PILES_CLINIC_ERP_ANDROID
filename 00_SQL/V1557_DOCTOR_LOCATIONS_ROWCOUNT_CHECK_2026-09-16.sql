-- শুধু দেখার জন্য (SELECT) -- এই টেবিলে আদৌ কোনো ডেটা আছে কিনা যাচাই
-- (দুইজন ডাক্তারের "No field visit recorded yet" দেখাচ্ছে -- এটা RLS-এর জন্য
--  নাকি এমনিতেই কোনো লোকেশন কখনো জমা হয়নি, সেটা বোঝার জন্য)
select mobile, updated_at from wn.doctor_locations order by updated_at desc;
