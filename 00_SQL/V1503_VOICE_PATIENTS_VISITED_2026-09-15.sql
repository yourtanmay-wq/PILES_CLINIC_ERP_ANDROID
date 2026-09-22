-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1503 (১৫.০৯.২০২৬, TK-নির্দেশ) — "[তারিখ] [ব্রাঞ্চ]-এ কতজন পেশেন্ট
-- এসেছিল" প্রশ্নটা V1415-এ শুধু "নতুন রেজিস্ট্রেশন" গুনত (TK-র নিজের প্রথম
-- উদাহরণ প্রশ্ন ধরেই বানানো হয়েছিল)। TK এখন ধরলেন — গতকাল কোচবিহারে অনেক
-- পুরনো রোগীও এসেছিলেন (ফলো-আপ/ট্রিটমেন্টে), সেগুলো ধরা পড়ছে না।
-- TK: "হ্যাঁ, সব রোগী ধরে গুনুন।"
--
-- ⛔ পুরনো `patients_registered_count/list` (শুধু রেজিস্ট্রেশন) এক অক্ষরও
--    বদলানো হয়নি — অন্য জায়গায় (Yearly Registration ইত্যাদি) এই একই
--    নামে "শুধু রেজিস্ট্রেশন" অর্থেই ব্যবহার হতে পারে। এটা সম্পূর্ণ নতুন,
--    আলাদা ফাংশন — voice-প্রশ্নের ম্যাপিং বদলাবে অ্যাপের কোডে (V1503,
--    Kotlin+web), এই SQL শুধু নতুন ফাংশন যোগ করে।
--
-- "এসেছিল" = সেদিন (ক) নতুন রেজিস্ট্রেশন হয়েছে, বা (খ) কোনো পেমেন্ট-সারি
-- আছে যেটা সত্যিকারের ভিজিটের প্রমাণ (attendance_mark/treatment/medicine/
-- saline/ইত্যাদি) — শুধু বাদ: chamber_expected (আসার কথা, এখনো আসেননি)
-- আর refund (টাকা ফেরত, নতুন ভিজিটের প্রমাণ না)। দুটো মিলিয়ে ডুপ্লিকেট
-- বাদ দিয়ে (distinct patient) গোনা হয়।
-- ⛔ শুধু পড়া, কোনো টেবিল বদলায় না।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

create or replace function reports.patients_visited_count(p_branch text, p_from date, p_to date)
returns int language plpgsql stable security definer set search_path = hr, public as $$
declare n int;
begin
  if not reports.can_access_branch(p_branch) then return null; end if;
  select count(distinct pid) into n from (
    select p.id as pid
      from public.patients p
     where p."branch" = p_branch
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
    union
    select y."patientId" as pid
      from public.payments y
     where y."branch" = p_branch
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
       and lower(coalesce(y."payType",'')) not in ('chamber_expected','refund')
       and coalesce(y."patientId",'') <> ''
  ) both_sources;
  return n;
end $$;
revoke all on function reports.patients_visited_count(text, date, date) from public, anon;
grant execute on function reports.patients_visited_count(text, date, date) to authenticated;

create or replace function reports.patients_visited_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, visit_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           min(v.visited_on)
      from (
        select p2.id as pid,
               left(coalesce(nullif(p2."registrationDate",''), p2."date", ''), 10) as visited_on
          from public.patients p2
         where p2."branch" = p_branch
           and left(coalesce(nullif(p2."registrationDate",''), p2."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
           and left(coalesce(nullif(p2."registrationDate",''), p2."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
        union
        select y."patientId" as pid, left(coalesce(y."date",''),10) as visited_on
          from public.payments y
         where y."branch" = p_branch
           and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
           and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
           and lower(coalesce(y."payType",'')) not in ('chamber_expected','refund')
           and coalesce(y."patientId",'') <> ''
      ) v
      join public.patients p on p.id = v.pid
     group by p.id, p."patientId", p.name, p.mobile
     order by min(v.visited_on) desc, p.name
     limit 500;
end $$;
revoke all on function reports.patients_visited_list(text, date, date) from public, anon;
grant execute on function reports.patients_visited_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';

commit;
