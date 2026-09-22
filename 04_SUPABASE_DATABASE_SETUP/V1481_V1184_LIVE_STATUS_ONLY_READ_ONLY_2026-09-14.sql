-- ⛔ শুধু দেখার SQL — একটাও সারি লেখে/বদলায়/মোছে না।
-- এটা একাই Run করুন (আলাদা প্রশ্ন, তাই একবারেই ফলাফল দেখাবে)।

SELECT
  CASE WHEN pg_get_functiondef('hr.incentive_wanted()'::regprocedure) LIKE '%new_who%'
       THEN '✅ V1184 (নতুন নিয়ম) লাইভ আছে'
       ELSE '⛔ V1184 এখনো Run করা হয়নি — পুরনো নিয়মেই চলছে' END AS v1184_status;
