-- V1429 (১৩.০৯.২০২৬ দুপুর, তালিকা ৫৪২) — শুধু পড়া, কিছু বদলায় না।
-- TK: "RUPAM & ARMAN সহ কোন staff-এর লোকেশন দেখা যাচ্ছে না কেন?"
-- আজ ও গতকাল যাঁরা IN TIME চেপেছেন, তাঁদের ফোন থেকে GPS-সেবা ৩ মিনিট পরপর যে ping পাঠায়
-- (updated_at বদলায়), সেটা আদৌ আসছে কিনা — এটাই আসল প্রশ্ন:
--   · updated_at ≈ IN-এর সময়েই আটকে  ⇒ GPS-সেবা IN-এর পরে আর চলেনি/মরে গেছে (যেমন নতুন ফাইল ইনস্টলে অ্যাপ বন্ধ)
--   · updated_at টাটকা, অথচ lat/lng নেই ⇒ সেবা চলছে, কিন্তু ফোন একটাও অবস্থান দেয়নি (Location/Precise বন্ধ)
--   · lat/lng আছে                        ⇒ LOCATION OK (Master-এর পর্দায় Google Maps বোতাম ওঠার কথা)
select
  staff_code,
  branch,
  work_date,
  started_at   at time zone 'Asia/Kolkata' as in_time_ist,
  updated_at   at time zone 'Asia/Kolkata' as last_ping_ist,
  last_seen_at at time zone 'Asia/Kolkata' as last_gps_ist,
  ended_at     at time zone 'Asia/Kolkata' as out_time_ist,
  distance_m,
  last_lat, last_lng, last_acc_m,
  case
    when last_lat is not null and last_lng is not null and not (last_lat = 0 and last_lng = 0) then 'LOCATION OK'
    when updated_at is null or started_at is null then 'NO PING INFO'
    when updated_at - started_at < interval '5 minutes' then 'SERVICE NEVER PINGED AFTER IN (app closed/killed?)'
    else 'SERVICE PINGING BUT NO GPS FIX (Location/Precise off?)'
  end as diagnosis
from wn.field_visit_days
where work_date >= (current_date - interval '1 day')::date
order by work_date desc, staff_code;
