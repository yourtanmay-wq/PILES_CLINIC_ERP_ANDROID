-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- নতুন সন্দেহ (কোডে যাচাই করে): উত্তমার নতুন রিমার্ক/তারিখ লেখাটা `followups`
-- টেবিলের একটা নির্দিষ্ট `id`-তে PATCH করে বসে। কিন্তু Enquiry ট্যাবের কিছু
-- কার্ড সরাসরি `followups`-এর সারি নয় -- সেগুলো `enquiries` টেবিল থেকে
-- "জাল/নকল কার্ড" হিসেবে বানানো হয় (এখনো কোনো followups সারিই তৈরি হয়নি)।
-- এমন কার্ডে রিমার্ক লিখলে যে `id`-টা PATCH করতে যায়, সেটা `followups`-এ
-- আদৌ নেই -- সার্ভার "row_not_matched" বলে, আর TK-র নিজের আগের অনুমোদিত
-- নিয়মেই (১০.০৮.২০২৬) এই ধরনের ব্যর্থতা **কখনো রিট্রাই/সতর্কতা দেখায় না**
-- (চিরকালের জন্য নীরবে বাদ) -- তাই "Home-এ কিছু আসে না" অথচ তারিখও বদলায় না।
-- নিচের দুটো নম্বর সেই দুটো "UNKNOWN" কার্ডের -- এদের followups-এ
-- সত্যিকারের সারি আছে কিনা দেখা হচ্ছে।

select 'followups' as tbl, id, mobile, name, stage, status, "nextFollow", "updatedAt"
from public.followups
where mobile in ('+918670104488','+917002682284','8670104488','7002682284')
   or mobile like '%8670104488%' or mobile like '%7002682284%';

select 'enquiries' as tbl, id, mobile, name, stage, status, "nextFollow", "updatedAt"
from public.enquiries
where mobile in ('+918670104488','+917002682284','8670104488','7002682284')
   or mobile like '%8670104488%' or mobile like '%7002682284%';
