-- ═══════════════════════════════════════════════════════════════════════════
-- V1149 (০৬.০৯.২০২৬) — কল-গোনা আর কল-তালিকা এক করা
-- TK-সিদ্ধান্ত: "শুধু রিমার্ক লিখলে তবেই সেটা কল বলে গণ্য করা হয়" · "খ করুন"
--
-- TK-এর নিজের SQL-এ মাপা: ১৫০২টা সারির মধ্যে ১৩৯৩টায় গোনা ও তালিকা আলাদা
-- (১৩৯০টায় গোনা কম, ৩টায় বেশি)। কারণ কোডে — গোনা বাড়ত কেবল কিছু পথে,
-- অথচ রিমার্ক লেখা হলেই হিস্ট্রিতে সারি বসত। কোডটা V1149-এ সারানো হয়েছে;
-- এই SQL পুরনো সারিগুলোকে ওই একই নিয়মে বসিয়ে দেয়।
--
-- নিয়ম: গোনা = **যত দিনে রিমার্ক লেখা হয়েছে** (একই দিনে একাধিক = ১), সর্বোচ্চ ৫।
-- ⛔ হিস্ট্রি · রিমার্ক · তারিখ · টাকা — কিচ্ছু ছোঁয়া হয় না, শুধু `callCount`।
-- ⚠️ ফল: Inquiry ধাপের ২৪টা সারি নতুন করে ৫-এ পৌঁছবে, তাই সেখানে রিমার্ক
--    লিখতে গেলে "৫ কল হয়ে গেছে — সিদ্ধান্ত নিন" পর্দা আসবে (TK-র নিয়মই)।
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── ১) আগে কী অবস্থা (কিছু বদলায় না) ──────────────────────────────────
select count(*) as mot_sari,
       count(*) filter (where cc <> hist_days) as omil_sari
from (
  select coalesce(nullif(regexp_replace(coalesce("callCount",'')::text,'\D','','g'),''),'0')::int as cc,
         least(5, (select count(distinct h->>'date')
                     from jsonb_array_elements(coalesce("history",'[]')::jsonb) h
                    where coalesce(h->>'remark','') <> '')) as hist_days
    from public.followups
) t;

-- ─── ২) শুধরানো ────────────────────────────────────────────────────────
with calc as (
  select f."id",
         least(5, (select count(distinct h->>'date')
                     from jsonb_array_elements(coalesce(f."history",'[]')::jsonb) h
                    where coalesce(h->>'remark','') <> '')) as thik
    from public.followups f
)
update public.followups f
   set "callCount" = calc.thik::text,
       "updatedAt" = now()::text
  from calc
 where calc."id" = f."id"
   and coalesce(nullif(regexp_replace(coalesce(f."callCount",'')::text,'\D','','g'),''),'0')::int
       <> calc.thik;

-- ─── ৩) এখন কী অবস্থা — `omil_sari` ০ হওয়ার কথা ────────────────────────
select count(*) as mot_sari,
       count(*) filter (where cc <> hist_days) as omil_sari
from (
  select coalesce(nullif(regexp_replace(coalesce("callCount",'')::text,'\D','','g'),''),'0')::int as cc,
         least(5, (select count(distinct h->>'date')
                     from jsonb_array_elements(coalesce("history",'[]')::jsonb) h
                    where coalesce(h->>'remark','') <> '')) as hist_days
    from public.followups
) t;
