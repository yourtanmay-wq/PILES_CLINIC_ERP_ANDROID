-- V1393: শুধু দেখার জন্য (read-only) — সার্ভারে **সত্যিই** এখন এই মুহূর্তে
-- চলা fin.rmp_autolink_refdoctor ফাংশনের আসল কোড বের করা। ফাইলে যা লেখা
-- আছে তার সাথে লাইভ সার্ভার হুবহু মেলে কিনা এটা দিয়েই নিশ্চিত হওয়া যাবে —
-- আন্দাজ নয়। কিছুই বদলায় না।

select pg_get_functiondef('fin.rmp_autolink_refdoctor(text, boolean)'::regprocedure);
