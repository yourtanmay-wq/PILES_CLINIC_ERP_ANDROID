-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- আগের SQL "branch = 'Cooch Behar'" ঠিক এই বানানেই খুঁজেছিল -- ১৭১টা পেয়েছিল।
-- কিন্তু ফোনে রিফ্রেশ করার পরেও ৩০০-ই দেখাচ্ছে (স্ক্রল করেও কার্ড আছে ২৯৯-৩০০
-- পর্যন্ত) -- তাই সন্দেহ: "Cooch Behar" ব্রাঞ্চের নাম টেবিলে অন্য বানানে/হরফে
-- (বড়/ছোট হাতের অক্ষর, বাড়তি স্পেস) একাধিকভাবে জমা থাকতে পারে, যেগুলো আগের
-- SQL-এর হুবহু-মিল খুঁজে পায়নি।

select branch, count(*) as total_rows
from public.followups
where stage = 'Patient'
  and status not in ('Cancelled','Incomplete','Rejected','Closed')
  and lower(trim(branch)) = lower(trim('Cooch Behar'))
group by branch
order by total_rows desc;
