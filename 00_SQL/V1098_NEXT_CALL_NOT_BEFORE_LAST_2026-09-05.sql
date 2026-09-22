-- ═══════════════════════════════════════════════════════════════════════
-- V1098 (০৫.০৯.২০২৬) — NEXT CALL কখনো LAST CALL-এর আগে থাকবে না
--
-- TK তৃতীয়বার একই কথা বলেছেন (সারি ৯১ · ১৭১ · এখন NOOR ALAM আবার)।
-- 🔴 আমার ফাঁক: আগে শুধু **নতুন করে যাতে না হয়** সেই কোড-দরজাগুলো বন্ধ
--    করেছিলাম (V998 · V1065 · V1066), কিন্তু **পুরনো সারিগুলো সারাইনি**।
--    মেপে দেখা: এমন সারি **৫৩টা**, সবচেয়ে পুরনো nextFollow ২৮.০৭.২০২৬।
--
-- ① পুরনো ৫৩টা সারানো — NEXT CALL বসে ঠিক LAST CALL-এর দিনেই (তার আগে নয়)।
--    ⛔ কেউ তালিকা থেকে হারায় না (তারিখ পিছিয়ে নয়, এগিয়ে যাচ্ছে)।
--    ⛔ শুধু `nextFollow` — নাম · নম্বর · স্টেজ · হিস্ট্রি · টাকা কিছুই ছোঁয়া হয় না।
--    ⛔ তারিখের ধাঁচ ঠিক (YYYY-MM-DD) না হলে ওই সারিতে হাত পড়ে না।
-- ② স্থায়ী নিয়ম সার্ভারে — ফোন হোক বা কম্পিউটার, কোনো পথেই আর
--    NEXT CALL < LAST CALL বসতে পারবে না। ⇒ নতুন APK লাগে না।
-- ═══════════════════════════════════════════════════════════════════════

-- ② আগে নিয়মটা বসাই, তাতে ①-এর সময়ও নিয়ম মেনেই লেখা হয়
create or replace function public.tk_next_call_not_before_last()
returns trigger
language plpgsql
as $fn$
begin
  if coalesce(NEW."nextFollow",'')   ~ '^\d{4}-\d{2}-\d{2}$'
     and coalesce(NEW."lastCallDate",'') ~ '^\d{4}-\d{2}-\d{2}$'
     and NEW."nextFollow" < NEW."lastCallDate" then
    NEW."nextFollow" := NEW."lastCallDate";
  end if;
  return NEW;
end;
$fn$;

drop trigger if exists tk_next_call_not_before_last on public.followups;
create trigger tk_next_call_not_before_last
before insert or update on public.followups
for each row execute function public.tk_next_call_not_before_last();

-- ① পুরনো সারিগুলো সারানো
update public.followups
set "nextFollow" = "lastCallDate",
    "updatedAt"  = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where coalesce("nextFollow",'')     ~ '^\d{4}-\d{2}-\d{2}$'
  and coalesce("lastCallDate",'')   ~ '^\d{4}-\d{2}-\d{2}$'
  and "nextFollow" < "lastCallDate";

-- ③ যাচাই — baki অবশ্যই 0 হবে
select count(*) as baki
from public.followups
where coalesce("nextFollow",'')   ~ '^\d{4}-\d{2}-\d{2}$'
  and coalesce("lastCallDate",'') ~ '^\d{4}-\d{2}-\d{2}$'
  and "nextFollow" < "lastCallDate";
