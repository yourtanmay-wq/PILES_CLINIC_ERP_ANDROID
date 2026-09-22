-- =====================================================================
-- V403 — বাদ-দেওয়া কর্মীর চাবি নিজে থেকেই বন্ধ + নাম ঠিক দেখানো (16.08.2026)
-- =====================================================================
-- 🔴 কেন দরকার (TK "Swapna কে বাদ দিয়ে দিয়েছি" বলার পরে যাচাই করে ধরা পড়ল):
--    কাউকে বাদ দিলে (staff_profiles-এ active = false) তার আয়-খরচের চাবি
--    **আপনা থেকে বন্ধ হত না** — চাবি চালু থাকলে সে তখনো আজকের আয়-খরচ
--    তুলতে পারত। এখন বাদ-দেওয়া মানেই চাবিও বন্ধ।
--    আর Entry Permission-এর তালিকাতেও বাদ-দেওয়া কেউ আর দেখাবে না।
--
-- এই ফাইলেই V402-এর নাম-দেখানোর সংশোধনটাও আছে।
--   ⇒ **V402 চালিয়ে থাকলেও কিছু যায় আসে না — এটাই চালান, এতে দুটোই আছে।**
--
-- ⛔ কোনো টেবিল, কোনো তথ্য, কারো চাবির মান বদলায় না — শুধু দুটো ফাংশন
--    নতুন করে লেখা হয়। বারবার চালালেও ক্ষতি নেই।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- =====================================================================

begin;

-- ---------------------------------------------------------------------
-- ১) চাবি খাটে কিনা — এখন "সচল (active)" কিনা সেটাও দেখা হয়।
--    ⛔ ব্রাঞ্চ-লগইনের (BIR-BRANCH ইত্যাদি) staff_profiles সারি নেই, তাই
--       coalesce(active, true) — ওদের কিছু ভাঙবে না।
--    ⛔ অংশীদার ডাক্তারের পুরনো পথ (fin.can_entry_branch) ছোঁয়া হয়নি।
-- ---------------------------------------------------------------------
create or replace function fin.ie_has_permit(p_branch text) returns boolean
  language sql stable security definer set search_path = fin, hr, public as $$
  select hr.my_code() <> '' and exists(
    select 1 from fin.entry_permits e
    left join hr.staff_profiles s on s.person_code = e.person_code
    where e.person_code = hr.my_code()
      and lower(trim(e.branch)) = lower(trim(coalesce(p_branch,'')))
      and e.can_entry
      and coalesce(s.active, true) = true );
$$;
revoke all on function fin.ie_has_permit(text) from public, anon;
grant execute on function fin.ie_has_permit(text) to authenticated;

-- ---------------------------------------------------------------------
-- ২) Entry Permission-এর তালিকা — নাম ঠিক (V402) + বাদ-দেওয়া কেউ নয়।
-- ---------------------------------------------------------------------
create or replace function fin.ie_permit_candidates(p_branch text)
  returns table(person_code text, full_name text, role_kind text,
                is_partner boolean, partner_can_entry boolean, can_entry boolean)
  language sql stable security definer set search_path = fin, hr, public as $$
  select
    i.person_code,
    -- নাম খোঁজার ক্রম: staff_profiles → fin.partners → শেষে কোডটাই
    coalesce(
      nullif(trim(s.full_name),''),
      nullif(trim((select p2.name from fin.partners p2
                    where p2.active
                      and right(regexp_replace(coalesce(p2.mobile,''),'\D','','g'),10)
                        = right(regexp_replace(coalesce(i.link_mobile,''),'\D','','g'),10)
                      and coalesce(i.link_mobile,'') <> ''
                    order by (lower(trim(p2.branch)) = lower(trim(p_branch))) desc
                    limit 1)),''),
      i.person_code
    ) as full_name,
    coalesce(i.role_kind,'') as role_kind,
    exists(select 1 from fin.partners p
            where p.active and lower(trim(p.branch)) = lower(trim(p_branch))
              and right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10)
                = right(regexp_replace(coalesce(i.link_mobile,''),'\D','','g'),10)
              and coalesce(i.link_mobile,'') <> '') as is_partner,
    exists(select 1 from fin.partners p
            where p.active and p.can_entry and lower(trim(p.branch)) = lower(trim(p_branch))
              and right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10)
                = right(regexp_replace(coalesce(i.link_mobile,''),'\D','','g'),10)
              and coalesce(i.link_mobile,'') <> '') as partner_can_entry,
    coalesce(e.can_entry, false) as can_entry
  from hr.app_identity i
  left join hr.staff_profiles s on s.person_code = i.person_code
  left join fin.entry_permits  e on e.person_code = i.person_code
                                and lower(trim(e.branch)) = lower(trim(p_branch))
  where hr.is_master()
    and coalesce(i.is_master,false) = false
    and coalesce(i.role_kind,'') in ('staff','doctor')
    -- 🔴 V403: বাদ-দেওয়া (inactive) কেউ আর তালিকায় আসবে না।
    --    ব্রাঞ্চ-লগইনগুলোর staff_profiles সারিই নেই (s.active = null) — তাই
    --    coalesce(...,true), নইলে ওরাও উধাও হয়ে যেত।
    and coalesce(s.active, true) = true
    and (
         lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
      or e.person_code is not null
      or exists(select 1 from fin.partners p
                 where p.active and lower(trim(p.branch)) = lower(trim(p_branch))
                   and right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10)
                     = right(regexp_replace(coalesce(i.link_mobile,''),'\D','','g'),10)
                   and coalesce(i.link_mobile,'') <> '')
    )
  order by coalesce(i.role_kind,''), 2;
$$;
revoke all on function fin.ie_permit_candidates(text) from public, anon;
grant execute on function fin.ie_permit_candidates(text) to authenticated;


commit;

-- "Success. No rows returned" দেখলেই হয়ে গেছে।
