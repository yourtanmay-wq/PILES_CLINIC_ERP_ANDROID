-- =====================================================================
-- V401 — STAFF & DOCTOR INCOME / EXPENSE  (TK-নির্দেশ, 16.08.2026)
-- =====================================================================
-- TK-এর নিয়ম (হুবহু):
--   "Doctor ও staff যেন আয় এবং খরচ তুলতে পারে / তবে সেটা দিনের দিন হতে হবে /
--    পুরাতন কোন হিসাব তুলতে গেলে অথবা Edit করতে গেলে Master এর অনুমতি লাগবে …
--    staff পূর্ববর্তী হিসাব দেখতে পারবেনা … Doctor পূর্ববর্তী হিসাব দেখতে পারবে
--    (কিন্তু কোন কিছু এডিট করতে পারবে না মাস্টারের অনুমতি ছাড়া) …
--    ডাক্তার এবং স্টাফ নিজস্ব ব্রাঞ্চ ছাড়া অন্য ব্রাঞ্চের কোন হিসাব সে দেখতে পাবে না"
--
-- TK-এর সিদ্ধান্ত: শুধু যাঁদের মাস্টার চালু করবেন · অনুরোধ→অনুমোদন ঘণ্টায় ·
--   Staff শুধু আজকের · Doctor পুরনো দেখবে বদলাবে না · মোছা শুধু মাস্টার ·
--   অংশীদার ডাক্তার আগের মতোই · চাবি "একজন মানুষ + একটা ব্রাঞ্চ" ধরে।
--
-- 🔒 নিরাপত্তা: V246-এর master নীতি ও V307-এর partner নীতি **এক অক্ষরও**
--    বদলানো হয়নি (একটা ছাড়া — নিচের ৪-নম্বর টীকা দেখুন)। Postgres নীতিগুলো
--    OR দিয়ে জোড়ে, তাই নিচেরগুলো শুধু সরু নতুন অধিকার **যোগ** করে।
-- 🔒 চালানোর পরেও **কারো কিছু বদলাবে না** — চাবির টেবিল ফাঁকা/বন্ধ অবস্থায়
--    শুরু হয়, মাস্টার নিজে চালু না করা পর্যন্ত কেউ ঢুকতে পারবে না।
-- 🔒 বারবার চালালেও ক্ষতি নেই (idempotent)।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- =====================================================================

begin;

-- ---------------------------------------------------------------------
-- 0) সহায়ক — আজকের তারিখ (ভারতীয় সময়)। এক জায়গায় লেখা, তাই কোথাও আলাদা
--    হয়ে যাওয়ার ভয় নেই।
-- ---------------------------------------------------------------------
create or replace function fin.ie_today() returns date
  language sql stable as $$ select (now() at time zone 'Asia/Kolkata')::date; $$;
revoke all on function fin.ie_today() from public, anon;
grant execute on function fin.ie_today() to authenticated;

-- ---------------------------------------------------------------------
-- 1) চাবির টেবিল — "একজন মানুষ + একটা ব্রাঞ্চ = এক চাবি"।
--    🔴 কেন ব্রাঞ্চ-ধরে: Dr. Saikat Roy দুই ব্রাঞ্চে (Falakata-তে তুলতে পারেন,
--       Birpara-তে পারেন না)। শুধু নাম ধরে চাবি হলে তিনি Birpara-তেও ঢুকে
--       পড়তেন — TK যা চাননি।
--    ⛔ hr.staff_profiles-এ **হাত দেওয়া হয়নি**, কারণ ওই টেবিল হাজিরা/বেতন/
--       রিপোর্ট সব জায়গায় ব্যবহার হয়। ব্রাঞ্চ-লগইনের ব্রাঞ্চ এই টেবিলেই থাকে।
-- ---------------------------------------------------------------------
create table if not exists fin.entry_permits (
  "person_code" text    not null,
  "branch"      text    not null,
  "can_entry"   boolean not null default false,
  "updated_by"  text,
  "updated_at"  timestamptz not null default now(),
  primary key ("person_code", "branch")
);
alter table fin.entry_permits enable row level security;
alter table fin.entry_permits force  row level security;

drop policy if exists ie_permits_master on fin.entry_permits;
create policy ie_permits_master on fin.entry_permits for all
  using ( hr.is_master() ) with check ( hr.is_master() );
-- নিজের চাবি নিজে **দেখতে** পারে (বদলাতে পারে না) — অ্যাপ যেন জানে কী করা যাবে।
drop policy if exists ie_permits_self_read on fin.entry_permits;
create policy ie_permits_self_read on fin.entry_permits for select
  using ( person_code = hr.my_code() and hr.my_code() <> '' );

-- ব্রাঞ্চ-লগইনগুলোর ব্রাঞ্চ বসানো (TK মিলিয়ে দিয়েছেন) — সব **বন্ধ** অবস্থায়,
-- তাই এতে কেউ কোনো নতুন অধিকার পাচ্ছে না; শুধু Entry Permission পর্দায়
-- ঠিক ব্রাঞ্চের নিচে নামগুলো দেখা যাবে।
insert into fin.entry_permits(person_code, branch, can_entry, updated_by)
values ('BIR-BRANCH','Birpara',     false,'V401'),
       ('COB-BRANCH','Cooch Behar', false,'V401'),
       ('FLK-BRANCH','Falakata',    false,'V401'),
       ('JPE-BRANCH','Jalpaiguri',  false,'V401'),
       ('KNE-BRANCH','Kishanganj',  false,'V401')
on conflict (person_code, branch) do nothing;

-- ---------------------------------------------------------------------
-- 2) সহায়ক ফাংশন — চাবি আছে কিনা, আর পড়ার অধিকার আছে কিনা।
--    (SECURITY DEFINER — forced RLS-এর নিচে পরিচয়-টেবিল পড়তে হয়।)
-- ---------------------------------------------------------------------

-- এই ব্রাঞ্চে কি আমার চাবি চালু আছে?
create or replace function fin.ie_has_permit(p_branch text) returns boolean
  language sql stable security definer set search_path = fin, hr, public as $$
  select hr.my_code() <> '' and exists(
    select 1 from fin.entry_permits e
    where e.person_code = hr.my_code()
      and lower(trim(e.branch)) = lower(trim(coalesce(p_branch,'')))
      and e.can_entry );
$$;

-- সারিটা কি আমারই তোলা? (ব্রাঞ্চ-লগইনে "আমি" = ওই লগইন — TK-অনুমোদিত:
--  "একই লগইনে তোলা আজকের যেকোনো সারি")
create or replace function fin.ie_is_mine(p_created_by text) returns boolean
  language sql stable security definer set search_path = fin, hr, public as $$
  select ( hr.my_code() <> '' and coalesce(p_created_by,'') = hr.my_code() )
      or ( fin.my_mobile() <> ''
           and right(regexp_replace(coalesce(p_created_by,''),'\D','','g'),10) = fin.my_mobile() );
$$;

-- এই ব্রাঞ্চের হিসাব আমি কি পড়তে পারি, আর কোন তারিখ পর্যন্ত?
--   master  → সব
--   doctor  → নিজের ব্রাঞ্চের সব তারিখ  (TK: "Doctor পূর্ববর্তী হিসাব দেখতে পারবে")
--   staff   → নিজের ব্রাঞ্চের **শুধু আজকের** (TK: "staff পূর্ববর্তী হিসাব দেখতে পারবেনা")
--   চাবি বন্ধ থাকলে কিছুই নয়।
create or replace function fin.ie_can_read_row(p_branch text, p_date date) returns boolean
  language sql stable security definer set search_path = fin, hr, public as $$
  select case
    when not fin.ie_has_permit(p_branch) then false
    when coalesce(hr.my_role(),'') = 'doctor' then true
    else p_date = fin.ie_today()
  end;
$$;

revoke all on function fin.ie_has_permit(text), fin.ie_is_mine(text),
                      fin.ie_can_read_row(text, date) from public, anon;
grant execute on function fin.ie_has_permit(text), fin.ie_is_mine(text),
                         fin.ie_can_read_row(text, date) to authenticated;

-- ---------------------------------------------------------------------
-- 3) fin.collections / fin.expenses — নতুন সরু নীতি (master ও partner নীতি অটুট)
--     পড়া   : উপরের নিয়ম
--     তোলা   : চাবি + নিজের ব্রাঞ্চ + **আজকের তারিখ** + নিজের নামে স্ট্যাম্প
--     বদলানো : তার উপরে — সারিটাও **আজকের** ও **আজই তোলা** ও **নিজের তোলা**
--     মোছা   : কোনো নীতি নেই ⇒ শুধু master (TK-সিদ্ধান্ত)
-- ---------------------------------------------------------------------
drop policy if exists coll_ie_read on fin.collections;
create policy coll_ie_read on fin.collections for select
  using ( fin.ie_can_read_row(branch, entry_date) );

drop policy if exists exp_ie_read on fin.expenses;
create policy exp_ie_read on fin.expenses for select
  using ( fin.ie_can_read_row(branch, entry_date) );

drop policy if exists coll_ie_insert on fin.collections;
create policy coll_ie_insert on fin.collections for insert
  with check ( fin.ie_has_permit(branch)
    and entry_date = fin.ie_today()
    and fin.ie_is_mine(created_by) );

drop policy if exists exp_ie_insert on fin.expenses;
create policy exp_ie_insert on fin.expenses for insert
  with check ( fin.ie_has_permit(branch)
    and entry_date = fin.ie_today()
    and fin.ie_is_mine(created_by) );

-- 🔴 `ignored` অবশ্যই false থাকতে হবে — নইলে "মোছা যাবে না" নিয়মটা ফাঁকি দেওয়া যেত:
--    সারিটা না মুছেও `ignored=true` বসিয়ে দিলে সেটা সব হিসাব থেকে উধাও হয়ে যায়,
--    অর্থাৎ কার্যত মোছা। পরীক্ষা করে এই ফাঁকটা ধরা পড়েছে, তাই দুই দিকেই আটকানো হলো।
drop policy if exists coll_ie_update on fin.collections;
create policy coll_ie_update on fin.collections for update
  using ( fin.ie_has_permit(branch)
    and entry_date = fin.ie_today()
    and (created_at at time zone 'Asia/Kolkata')::date = fin.ie_today()
    and coalesce(ignored,false) = false
    and fin.ie_is_mine(created_by) )
  with check ( fin.ie_has_permit(branch)
    and entry_date = fin.ie_today()
    and coalesce(ignored,false) = false
    and fin.ie_is_mine(created_by) );

drop policy if exists exp_ie_update on fin.expenses;
create policy exp_ie_update on fin.expenses for update
  using ( fin.ie_has_permit(branch)
    and entry_date = fin.ie_today()
    and (created_at at time zone 'Asia/Kolkata')::date = fin.ie_today()
    and coalesce(ignored,false) = false
    and fin.ie_is_mine(created_by) )
  with check ( fin.ie_has_permit(branch)
    and entry_date = fin.ie_today()
    and coalesce(ignored,false) = false
    and fin.ie_is_mine(created_by) );

-- ---------------------------------------------------------------------
-- 4) 🔴 অংশীদার ডাক্তারের **তোলার** নীতিতে একটাই বদল — তারিখ আজকের হতে হবে।
--    কেন: V307-এ অংশীদার পুরনো তারিখেও তুলতে পারতেন (বদলাতে পারতেন না)।
--    TK-এর নতুন নিয়ম — "পুরাতন কোন হিসাব তুলতে গেলে … Master এর অনুমতি লাগবে"।
--    ⛔ তাঁদের পড়া, বদলানো, ব্রাঞ্চ — সব আগের মতোই। শুধু পুরনো তারিখে নতুন
--       সারি বসানো এখন অনুরোধের পথে যাবে।
-- ---------------------------------------------------------------------
drop policy if exists coll_partner_insert on fin.collections;
create policy coll_partner_insert on fin.collections for insert
  with check ( fin.can_entry_branch(branch)
    and entry_date = fin.ie_today()
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile() );

drop policy if exists exp_partner_insert on fin.expenses;
create policy exp_partner_insert on fin.expenses for insert
  with check ( fin.can_entry_branch(branch)
    and entry_date = fin.ie_today()
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile() );

-- 🔴 অংশীদারের **বদলানোর** নীতিতেও একই `ignored` ফাঁক ছিল (V307)। TK-এর নিয়ম
--    "মোছা শুধু মাস্টার" — তাই এখানেও আটকানো হলো। ⛔ বাকি সব শর্ত হুবহু আগের মতোই।
drop policy if exists coll_partner_update on fin.collections;
create policy coll_partner_update on fin.collections for update
  using ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile()
    and (created_at at time zone 'Asia/Kolkata')::date = fin.ie_today()
    and coalesce(ignored,false) = false )
  with check ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile()
    and coalesce(ignored,false) = false );

drop policy if exists exp_partner_update on fin.expenses;
create policy exp_partner_update on fin.expenses for update
  using ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile()
    and (created_at at time zone 'Asia/Kolkata')::date = fin.ie_today()
    and coalesce(ignored,false) = false )
  with check ( fin.can_entry_branch(branch)
    and right(regexp_replace(coalesce(created_by,''),'\D','','g'),10) = fin.my_mobile()
    and coalesce(ignored,false) = false );

-- ---------------------------------------------------------------------
-- 5) অনুরোধের টেবিল — পুরনো তারিখে তুলতে/বদলাতে চাইলে এখানে জমা হয়,
--    মাস্টার ঘণ্টার পর্দা থেকে Approve/Reject করেন।
--    (fin.rmp_commission_requests-এর হুবহু একই প্রমাণিত ছাঁচ — V325)
-- ---------------------------------------------------------------------
create table if not exists fin.ie_requests (
  "id"                uuid primary key default gen_random_uuid(),
  "kind"              text not null
      check (kind in ('ADD_COLLECTION','ADD_EXPENSE','EDIT_COLLECTION','EDIT_EXPENSE')),
  "branch"            text not null,
  "entry_date"        date not null,
  "target_id"         uuid,                 -- EDIT_* হলে কোন সারি
  "payload"           jsonb not null,       -- যা বসাতে হবে
  "reason"            text,
  "status"            text not null default 'PENDING'
      check (status in ('PENDING','APPROVED','REJECTED')),
  "requested_by"      text,                 -- person_code
  "requested_by_name" text,
  "requested_at"      timestamptz not null default now(),
  "decided_by"        text,
  "decided_at"        timestamptz,
  "decide_note"       text
);
create index if not exists ie_requests_status_idx on fin.ie_requests(status, requested_at);
alter table fin.ie_requests enable row level security;
alter table fin.ie_requests force  row level security;

-- পড়া: মাস্টার সব, বাকিরা নিজেরটা। ⛔ কোনো সরাসরি লেখার নীতি নেই —
--      সব লেখা নিচের দুটো ফাংশনের মধ্য দিয়ে, তাই কেউ নিজের অনুরোধ
--      নিজে "Approve" করে দিতে পারবে না।
drop policy if exists ie_req_read on fin.ie_requests;
create policy ie_req_read on fin.ie_requests for select
  using ( hr.is_master() or (requested_by = hr.my_code() and hr.my_code() <> '') );

grant select, insert, update, delete on fin.entry_permits, fin.ie_requests to authenticated;

-- ---------------------------------------------------------------------
-- 5a) অনুরোধ পাঠানো — Staff/Doctor ডাকবে।
-- ---------------------------------------------------------------------
create or replace function fin.ie_request(
  p_kind       text,
  p_branch     text,
  p_entry_date date,
  p_target_id  uuid,
  p_payload    jsonb,
  p_reason     text
) returns uuid
  language plpgsql security definer set search_path = fin, hr, public as $$
declare v_id uuid;
begin
  if hr.is_master() then
    raise exception 'Master can do this directly — no request needed';
  end if;
  if not fin.ie_has_permit(p_branch) and not fin.can_entry_branch(p_branch) then
    raise exception 'You are not allowed to enter Income/Expense for this branch';
  end if;
  if p_kind not in ('ADD_COLLECTION','ADD_EXPENSE','EDIT_COLLECTION','EDIT_EXPENSE') then
    raise exception 'Unknown request type';
  end if;
  if p_entry_date > fin.ie_today() then
    raise exception 'A future date is not allowed';
  end if;
  if p_kind like 'EDIT%' and p_target_id is null then
    raise exception 'Nothing selected to edit';
  end if;

  insert into fin.ie_requests(kind, branch, entry_date, target_id, payload, reason,
                              requested_by, requested_by_name)
  values (p_kind, p_branch, p_entry_date, p_target_id, coalesce(p_payload,'{}'::jsonb),
          nullif(trim(coalesce(p_reason,'')),''),
          hr.my_code(),
          coalesce((select full_name from hr.staff_profiles where person_code = hr.my_code()), hr.my_code()))
  returning id into v_id;
  return v_id;
end $$;

-- ---------------------------------------------------------------------
-- 5b) অনুমোদন / নাকচ — শুধু মাস্টার। Approve হলে এই ফাংশনই আসল সারিটা
--     বসায়/বদলায় (SECURITY DEFINER, তাই RLS আটকায় না)।
-- ---------------------------------------------------------------------
create or replace function fin.ie_decide_request(p_id uuid, p_approve boolean, p_note text default null)
  returns text
  language plpgsql security definer set search_path = fin, hr, public as $$
declare r fin.ie_requests%rowtype;
begin
  if not hr.is_master() then
    raise exception 'Only Master can approve or reject';
  end if;
  select * into r from fin.ie_requests where id = p_id for update;
  if not found then raise exception 'Request not found'; end if;
  if r.status <> 'PENDING' then return r.status; end if;

  if not p_approve then
    update fin.ie_requests
       set status='REJECTED', decided_by=hr.my_code(), decided_at=now(), decide_note=p_note
     where id = p_id;
    return 'REJECTED';
  end if;

  if r.kind = 'ADD_COLLECTION' then
    insert into fin.collections(entry_date, branch, cash, online, expense_notes, expense_total, created_by)
    values (r.entry_date, r.branch,
            coalesce((r.payload->>'cash')::numeric, 0),
            coalesce((r.payload->>'online')::numeric, 0),
            coalesce(r.payload->>'expense_notes',''),
            coalesce((r.payload->>'expense_total')::numeric, 0),
            coalesce(r.requested_by,'staff'));

  elsif r.kind = 'ADD_EXPENSE' then
    insert into fin.expenses(entry_date, branch, category, paid_to, amount, mode, note, created_by)
    values (r.entry_date, r.branch,
            coalesce(r.payload->>'category',''),
            coalesce(r.payload->>'paid_to',''),
            coalesce((r.payload->>'amount')::numeric, 0),
            coalesce(r.payload->>'mode','Cash'),
            coalesce(r.payload->>'note',''),
            coalesce(r.requested_by,'staff'));

  elsif r.kind = 'EDIT_COLLECTION' then
    update fin.collections
       set entry_date    = coalesce((r.payload->>'entry_date')::date, entry_date),
           cash          = coalesce((r.payload->>'cash')::numeric, cash),
           online        = coalesce((r.payload->>'online')::numeric, online),
           expense_notes = coalesce(r.payload->>'expense_notes', expense_notes),
           expense_total = coalesce((r.payload->>'expense_total')::numeric, expense_total),
           updated_at    = now()
     where id = r.target_id;
    if not found then raise exception 'That ledger row no longer exists'; end if;

  elsif r.kind = 'EDIT_EXPENSE' then
    update fin.expenses
       set entry_date = coalesce((r.payload->>'entry_date')::date, entry_date),
           category   = coalesce(r.payload->>'category', category),
           paid_to    = coalesce(r.payload->>'paid_to', paid_to),
           amount     = coalesce((r.payload->>'amount')::numeric, amount),
           mode       = coalesce(r.payload->>'mode', mode),
           updated_at = now()
     where id = r.target_id;
    if not found then raise exception 'That expense no longer exists'; end if;
  end if;

  update fin.ie_requests
     set status='APPROVED', decided_by=hr.my_code(), decided_at=now(), decide_note=p_note
   where id = p_id;
  return 'APPROVED';
end $$;

revoke all on function fin.ie_request(text,text,date,uuid,jsonb,text) from public, anon;
revoke all on function fin.ie_decide_request(uuid, boolean, text)      from public, anon;
grant execute on function fin.ie_request(text,text,date,uuid,jsonb,text) to authenticated;
grant execute on function fin.ie_decide_request(uuid, boolean, text)     to authenticated;

-- ---------------------------------------------------------------------
-- 6) Entry Permission পর্দার তালিকা — এক ব্রাঞ্চের সম্ভাব্য সব মানুষ।
--    শুধু মাস্টার ডাকতে পারবেন।
-- ---------------------------------------------------------------------
create or replace function fin.ie_permit_candidates(p_branch text)
  returns table(person_code text, full_name text, role_kind text,
                is_partner boolean, partner_can_entry boolean, can_entry boolean)
  language sql stable security definer set search_path = fin, hr, public as $$
  select
    i.person_code,
    coalesce(nullif(trim(s.full_name),''), i.person_code) as full_name,
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

-- =====================================================================
-- চালানোর পরে "Success" দেখলেই হয়ে গেছে।
-- ⛔ এখনো কারো কিছু বদলায়নি — সব চাবি **বন্ধ**। অ্যাপে Entry Permission
--    পর্দা থেকে যাঁকে চালু করবেন, শুধু তিনিই তুলতে পারবেন।
-- =====================================================================
