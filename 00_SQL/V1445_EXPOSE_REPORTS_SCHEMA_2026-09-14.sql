-- ============================================================================
-- V1445 (১৪.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — Search-এ প্রশ্ন লিখলেই "Invalid schema:
-- reports" / "?" দেখাচ্ছে) — আসল কারণ (কোডে মেপে ধরা, আন্দাজ নয়):
--
-- GlobalSearchActivity.kt-এর ভয়েস-প্রশ্ন ফিচার (V1415-V1428) সার্ভারের
-- `reports` স্কিমার RPC ফাংশন কল করে (VoiceReportRepository.kt →
-- ModuleAuth.rpc("reports", ...))। V1415/VOICE_ALL SQL চালিয়ে `reports`
-- স্কিমা ও তার ফাংশনগুলো (enquiry_count, patients_registered_count ইত্যাদি)
-- আগেই বসানো হয়েছিল — কিন্তু PostgREST-কে বলা হয়নি যে `reports` স্কিমাটাও
-- API-তে **প্রকাশ (expose)** করতে হবে। ফলে প্রতিটা প্রশ্নেই PostgREST নিজে
-- থেকে ফিরিয়ে দিচ্ছিল ঠিক এই বার্তা: "Invalid schema: reports" (কোড এটা
-- বানায়নি, সার্ভারের নিজের জবাব হুবহু দেখানো হয় — ModuleAuth.kt লাইন ৫১৯-৫২১)।
--
-- এই একই কাজ আগে একবার (V246, ০২.০৮.২০২৬) `hr`/`wn`/`fin`-এর জন্য করা
-- হয়েছিল, ঠিক এই নিয়মেই — এখানে সেই একই নিরাপদ, **শুধু-যোগ-করা** পদ্ধতি,
-- `reports`-এর জন্য। ⛔ এই স্টেটমেন্ট আগে থেকে প্রকাশিত কোনো স্কিমা
-- (public, hr, wn, fin) সরায় না — শুধু তালিকায় `reports` না থাকলে যোগ করে।
-- ⛔ `reports`-এর কোনো ফাংশন/টেবিলের সংজ্ঞা এখানে বদলানো হয়নি — শুধু
-- API-এক্সেস চালু করা হলো, যা আগেই বসানো ছিল তার জন্য।
-- ============================================================================

do $$
declare cur text; want text;
begin
  select regexp_replace(setting, '^pgrst\.db_schemas=', '')
    into cur
  from pg_db_role_setting drs
  join pg_roles r on r.oid = drs.setrole
  cross join lateral unnest(drs.setconfig) as setting
  where r.rolname = 'authenticator' and setting like 'pgrst.db_schemas=%'
  limit 1;

  if cur is null or length(trim(cur)) = 0 then
    cur := 'public, graphql_public';
  end if;
  want := cur;
  if want !~ '(^|[, ])reports([, ]|$)' then want := want || ', reports'; end if;

  execute format('alter role authenticator set pgrst.db_schemas = %L', want);
end $$;
notify pgrst, 'reload config';

-- যাচাই: এই SELECT-এ 'reports' শব্দটা তালিকায় দেখা উচিত।
select setting from pg_db_role_setting drs
join pg_roles r on r.oid = drs.setrole
cross join lateral unnest(drs.setconfig) as setting
where r.rolname = 'authenticator' and setting like 'pgrst.db_schemas=%';
