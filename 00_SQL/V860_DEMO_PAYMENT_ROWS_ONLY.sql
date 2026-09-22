-- 🔍 V860 (৩০.০৮.২০২৬) — শুধু **একটাই** query, তাই Supabase ফলটা দেখাবে।
-- ⛔ কিছুই মোছে না।
-- (V859-এ দুটো query ছিল, Supabase শেষেরটার ফলই দেখায় — সেটা আমার ভুল।)

select y."mobile"::text                         as "নম্বর",
       coalesce(y."date"::text,'')              as "তারিখ",
       coalesce(y."name"::text,'')              as "নাম",
       coalesce(y."branch"::text,'')            as "ব্রাঞ্চ",
       coalesce(y."amount"::text,'')            as "টাকা",
       coalesce(y."payType"::text,'')           as "ধরন",
       coalesce(y."mode"::text,'')              as "মোড",
       coalesce(y."receivedBy"::text,'')        as "কে নিল",
       coalesce(y."patientId"::text,'')         as "রোগীর আইডি"
  from public.payments y
 where y."mobile" like '%6207841890' or y."mobile" like '%7321960416'
    or y."mobile" like '%7583973566' or y."mobile" like '%7679751521'
    or y."mobile" like '%8001080080' or y."mobile" like '%8101397763'
    or y."mobile" like '%8167096595' or y."mobile" like '%8210342405'
    or y."mobile" like '%8676002200' or y."mobile" like '%9002003540'
    or y."mobile" like '%9647840067' or y."mobile" like '%9883605917'
    or y."mobile" like '%9883623823'
 order by 1, 2;
