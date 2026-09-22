-- ⛔ শুধু দেখার SQL — একটাও সারি লেখে/বদলায়/মোছে না।
-- JPE-CRP শেষ ১৫ দিনে কোন দিন কতগুলো কল করেছে।
SELECT call_date, count(*) AS koyta
FROM wn.call_taps
WHERE staff_code = 'JPE-CRP'
GROUP BY call_date
ORDER BY call_date DESC
LIMIT 15;
