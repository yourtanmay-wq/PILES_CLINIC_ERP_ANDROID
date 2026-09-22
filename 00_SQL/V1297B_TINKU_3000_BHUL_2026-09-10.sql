-- (খ) ব্রাঞ্চ বললে "₹3,000 নেওয়া হয়নি, ভুলে বসেছে": চিহ্ন-সারির টাকা আবার ০ (সারি থাকে, নথি remarks-এ থাকে)
begin;
update payments
set amount = '0',
    remarks = coalesce(remarks,'') || ' | V1297: ভুলে বসানো ₹3,000 → ₹0 (ব্রাঞ্চ নিশ্চিত করেছে) ' || to_char(now() at time zone 'Asia/Kolkata','DD.MM.YYYY HH24:MI'),
    "updatedAt" = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where id = 'pay_4a81fb54f47e4ea893875ab2d4ba95e2' and "payType" = 'bill_edit' and amount = '3000';
select id, "payType", amount, "payLabel", date from payments where id = 'pay_4a81fb54f47e4ea893875ab2d4ba95e2';
commit;
