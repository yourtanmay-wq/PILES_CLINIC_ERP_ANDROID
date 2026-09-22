-- V1401 (১২.০৯.২০২৬ সন্ধ্যা) — শুধু পড়া, কিছু বদলায় না।
-- TK: "প্রতিটা স্টাফ এর লোকেশন কেন দেখা যায় না — গভীরে গিয়ে যাচাই করুন"
-- Staff Profiles → Location বোতাম যে টেবিল থেকে দিন-তালিকা আঁকে (wn.field_visit_days),
-- গত ৭ দিনে কোন স্টাফের কোন দিনের সারি আছে, আর তাতে লোকেশন (last_lat/last_lng) এসেছে কিনা।
-- সারি না থাকা = ওই স্টাফের ফোন থেকে IN TIME-এর সঙ্গে লোকেশন-গোনা কখনো শুরুই হয়নি
-- (পুরনো ভার্সন, বা IN TIME নতুন ভার্সনে চাপা হয়নি)।
-- lat/lng ফাঁকা = সারি এসেছে কিন্তু ফোন একটাও GPS-অবস্থান দেয়নি (Precise location অনুমতি নেই / GPS বন্ধ)।

select
  staff_code,
  branch,
  work_date,
  started_at at time zone 'Asia/Kolkata' as in_time_ist,
  ended_at   at time zone 'Asia/Kolkata' as out_time_ist,
  auto_closed,
  distance_m,
  last_lat,
  last_lng,
  last_acc_m,
  last_seen_at at time zone 'Asia/Kolkata' as last_seen_ist,
  case
    when last_lat is null or last_lng is null or (last_lat = 0 and last_lng = 0) then 'NO LOCATION FROM PHONE'
    else 'LOCATION OK'
  end as location_status
from wn.field_visit_days
where work_date >= (current_date - interval '7 days')::date
order by work_date desc, staff_code;

-- দ্বিতীয় প্রশ্ন: আজ যাঁরা IN TIME চেপেছেন (notebook_days) অথচ উপরের টেবিলে সারিই নেই —
-- তাঁদের ফোনে নতুন ভার্সন (V1377+) নেই বা IN TIME নতুন ভার্সনে চাপা হয়নি।
select
  n.staff_code,
  n.work_date,
  n.check_in,
  case when f.staff_code is null then 'NO LOCATION ROW AT ALL' else 'ROW EXISTS' end as location_row
from wn.notebook_days n
left join wn.field_visit_days f
  on f.staff_code = n.staff_code and f.work_date = n.work_date
where n.work_date >= (current_date - interval '3 days')::date
  and n.check_in is not null
order by n.work_date desc, n.staff_code;
