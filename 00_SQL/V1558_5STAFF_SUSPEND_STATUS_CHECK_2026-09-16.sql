-- শুধু দেখার জন্য (SELECT) -- ৫ জনের Suspend সত্যিই উঠেছে কিনা যাচাই (বার্তা না দেখানোয়)
select person_code, "link_mobile" as mobile, active, suspended_until
from hr.staff_profiles
where "link_mobile" in ('8167096595','8436002200','8514002200','9002003540','9002610352');
