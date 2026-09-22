-- ============================================================================
-- V739 — স্টাফের ছুটির ব্যবস্থা (Staff Leave)
-- TK-নির্দেশ, ২৭.০৮.২০২৬ (আলোচনা করে এক এক করে ঠিক করা):
--   · শুধু Staff — Doctor / Field Officer / Master নয়
--   · মাসে ৪ দিন — অ্যাপ নিজেই মঞ্জুর করবে
--   · ৪ দিনের বেশি → মাস্টারের অনুমতি
--   · চেম্বারের দিনে ছুটি নেই → আটকাবে, তবে মাস্টারের অনুমতি চাওয়া যাবে
--   · একই ব্রাঞ্চে একই দিনে দুজন নয় → আটকাবে, অনুমতি চাওয়া যাবে
--   · ১০ দিন পর্যন্ত আগাম আবেদন করা যাবে
--   · নিজের ছুটি নিজে বাতিল করা যাবে (দিনটা খালি হবে, হিসাবও ফেরত আসবে)
--
-- 🔒 নিরাপদ ও একবার চালানোর কোড:
--   · পুরনো কোনো টেবিল/ঘর/তথ্য **ছোঁয় না** — শুধু নতুন জিনিস যোগ করে
--   · "if not exists" আছে, ভুল করে দুবার চালালেও কোনো ক্ষতি নেই
--   · সব নিয়ম **সার্ভারেই** যাচাই হয় — ফোন থেকে ফাঁকি দেওয়ার পথ নেই
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================


-- ── ১. চেম্বারের দিন (ডাক্তার বসেন) ─────────────────────────────────────────
--    ⛔ অ্যাপের ভিতরের `ChamberDays` (রোগী কবে আসতে পারে) এটা **নয়** —
--       ওটা আলাদা জিনিস, অক্ষত থাকছে।
--    রবি=0 · সোম=1 · মঙ্গল=2 · বুধ=3 · বৃহ=4 · শুক্র=5 · শনি=6
create table if not exists hr.branch_chamber_days (
  "branch"   text primary key,
  "weekdays" int[] not null,
  "updated_at" timestamptz not null default now()
);

insert into hr.branch_chamber_days(branch, weekdays) values
  ('Jalpaiguri',  array[6,2]),      -- শনি + মঙ্গল
  ('Cooch Behar', array[1,5]),      -- সোম + শুক্র
  ('Birpara',     array[3,0]),      -- বুধ + রবি
  ('Falakata',    array[2,4,6]),    -- মঙ্গল + বৃহ + শনি
  ('Kishanganj',  array[3,6])       -- বুধ + শনি
on conflict (branch) do nothing;


-- ── ২. ছুটির খাতা ───────────────────────────────────────────────────────────
create table if not exists hr.staff_leave (
  "id"            uuid primary key default gen_random_uuid(),
  "person_code"   text not null,
  "staff_name"    text,
  "link_mobile"   text,
  "branch"        text not null,
  "leave_date"    date not null,
  -- approved = মঞ্জুর · pending = মাস্টারের অনুমতির অপেক্ষায়
  -- rejected = নামঞ্জুর · cancelled = স্টাফ নিজে বাতিল করেছে
  "status"        text not null default 'pending',
  -- auto = অ্যাপ নিজে দিয়েছে · master = মাস্টার দিয়েছেন
  "granted_by_kind" text,
  "reason"        text,          -- স্টাফ কেন ছুটি চাইছেন
  "block_reason"  text,          -- কেন আটকেছিল (chamber_day / another_staff / over_limit)
  "month_key"     text not null, -- 'YYYY-MM' — মাসের হিসাব সহজ করতে
  "requested_at"  timestamptz not null default now(),
  "decided_by"    text,
  "decided_at"    timestamptz,
  "decision_note" text,
  "cancelled_at"  timestamptz
);

create index if not exists staff_leave_person_month_idx
  on hr.staff_leave(person_code, month_key);
create index if not exists staff_leave_branch_date_idx
  on hr.staff_leave(branch, leave_date);

-- একই স্টাফ একই দিনে দুবার আবেদন করতে পারবে না (বাতিল/নামঞ্জুর হলে আবার পারবে)
create unique index if not exists staff_leave_one_live_per_day
  on hr.staff_leave(person_code, leave_date)
  where status in ('approved','pending');


-- ── ৩. নিরাপত্তা (RLS) — প্রজেক্টের চালু নিয়মেই ─────────────────────────────
alter table hr.staff_leave enable row level security;
alter table hr.staff_leave force row level security;
alter table hr.branch_chamber_days enable row level security;
alter table hr.branch_chamber_days force row level security;

-- স্টাফ **শুধু নিজের** ছুটি দেখতে পাবে; মাস্টার সব দেখবেন।
drop policy if exists sl_read on hr.staff_leave;
create policy sl_read on hr.staff_leave for select
  using ( hr.is_master() or person_code = hr.my_code() );

-- ⛔ সরাসরি লেখা/বদলানো **কেউ পারবে না** (মাস্টারও না) — সব নিচের
--    ফাংশনগুলোর মধ্য দিয়ে যাবে, তাই নিয়ম কখনো ফাঁকি দেওয়া যাবে না।
drop policy if exists sl_write on hr.staff_leave;

drop policy if exists bcd_read on hr.branch_chamber_days;
create policy bcd_read on hr.branch_chamber_days for select using ( true );
drop policy if exists bcd_write on hr.branch_chamber_days;
create policy bcd_write on hr.branch_chamber_days for all
  using ( hr.is_master() ) with check ( hr.is_master() );


-- ── ৪. আজকের তারিখ (ভারতের সময়ে) ───────────────────────────────────────────
create or replace function hr.today_ist() returns date
  language sql stable as $$ select (now() at time zone 'Asia/Kolkata')::date $$;


-- ── ৫. ছুটির আবেদন ──────────────────────────────────────────────────────────
--     ফেরত দেয়: {ok, status, block_reason, used, limit, message}
create or replace function hr.leave_apply(p_date date, p_reason text default '')
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare
  me       text := hr.my_code();
  prof     hr.staff_profiles%rowtype;
  mkey     text;
  dow      int;
  chamber  int[];
  blocks   text[] := array[]::text[];
  used     int;
  st       text;
  newid    uuid;
begin
  if me is null or me = '' then
    return jsonb_build_object('ok', false, 'message', 'Not signed in');
  end if;

  select * into prof from hr.staff_profiles where person_code = me;
  if not found then
    return jsonb_build_object('ok', false, 'message', 'Profile not found');
  end if;
  if coalesce(prof.role_kind,'') <> 'staff' then
    return jsonb_build_object('ok', false, 'message', 'Leave is for staff only');
  end if;
  if coalesce(prof.branch,'') = '' then
    return jsonb_build_object('ok', false, 'message', 'Branch not set in profile');
  end if;

  if p_date < hr.today_ist() then
    return jsonb_build_object('ok', false, 'message', 'Past date is not allowed');
  end if;
  if p_date > hr.today_ist() + 10 then
    return jsonb_build_object('ok', false, 'message', 'You can apply up to 10 days ahead');
  end if;

  -- 🔒 একই ব্রাঞ্চ+দিনে দুজন যেন এক সেকেন্ডে ঢুকে না পড়ে
  perform pg_advisory_xact_lock(hashtext(prof.branch || '|' || p_date::text));

  if exists (select 1 from hr.staff_leave
             where person_code = me and leave_date = p_date
               and status in ('approved','pending')) then
    return jsonb_build_object('ok', false, 'message', 'You already have leave on this date');
  end if;

  mkey := to_char(p_date, 'YYYY-MM');
  dow  := extract(dow from p_date)::int;

  -- (ক) চেম্বারের দিন?
  select weekdays into chamber from hr.branch_chamber_days where branch = prof.branch;
  if chamber is not null and dow = any(chamber) then
    blocks := blocks || 'chamber_day'::text;
  end if;

  -- (খ) একই ব্রাঞ্চে ওইদিন আর কারও ছুটি মঞ্জুর আছে?
  if exists (select 1 from hr.staff_leave
             where branch = prof.branch and leave_date = p_date
               and status = 'approved' and person_code <> me) then
    blocks := blocks || 'another_staff'::text;
  end if;

  -- (গ) এই মাসে ৪ দিন হয়ে গেছে?
  select count(*) into used from hr.staff_leave
   where person_code = me and month_key = mkey and status = 'approved';
  if used >= 4 then
    blocks := blocks || 'over_limit'::text;
  end if;

  st := case when array_length(blocks,1) is null then 'approved' else 'pending' end;

  insert into hr.staff_leave(person_code, staff_name, link_mobile, branch, leave_date,
                             status, granted_by_kind, reason, block_reason, month_key)
  values (me, prof.full_name, prof.link_mobile, prof.branch, p_date,
          st, case when st='approved' then 'auto' else null end,
          nullif(btrim(coalesce(p_reason,'')),''),
          nullif(array_to_string(blocks, ','), ''), mkey)
  returning id into newid;

  return jsonb_build_object(
    'ok', true, 'id', newid, 'status', st,
    'block_reason', nullif(array_to_string(blocks, ','), ''),
    'used', used, 'limit', 4,
    'message', case when st = 'approved'
                    then 'Leave approved'
                    else 'Sent to Master for permission' end);
end $$;


-- ── ৬. নিজের ছুটি বাতিল ─────────────────────────────────────────────────────
create or replace function hr.leave_cancel(p_id uuid)
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare me text := hr.my_code(); row hr.staff_leave%rowtype;
begin
  if me is null or me = '' then
    return jsonb_build_object('ok', false, 'message', 'Not signed in');
  end if;
  select * into row from hr.staff_leave where id = p_id;
  if not found then return jsonb_build_object('ok', false, 'message', 'Not found'); end if;
  if row.person_code <> me and not hr.is_master() then
    return jsonb_build_object('ok', false, 'message', 'This is not your leave');
  end if;
  if row.status not in ('approved','pending') then
    return jsonb_build_object('ok', false, 'message', 'Already closed');
  end if;
  if row.leave_date < hr.today_ist() then
    return jsonb_build_object('ok', false, 'message', 'Past leave cannot be cancelled');
  end if;
  update hr.staff_leave
     set status = 'cancelled', cancelled_at = now(), decided_by = me
   where id = p_id;
  return jsonb_build_object('ok', true, 'message', 'Leave cancelled');
end $$;


-- ── ৭. মাস্টারের সিদ্ধান্ত ──────────────────────────────────────────────────
create or replace function hr.leave_decide(p_id uuid, p_approve boolean, p_note text default '')
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare me text := hr.my_code(); row hr.staff_leave%rowtype;
begin
  if not hr.is_master() then
    return jsonb_build_object('ok', false, 'message', 'Only Master can decide');
  end if;
  select * into row from hr.staff_leave where id = p_id;
  if not found then return jsonb_build_object('ok', false, 'message', 'Not found'); end if;
  if row.status <> 'pending' then
    return jsonb_build_object('ok', false, 'message', 'This request is already closed');
  end if;
  update hr.staff_leave
     set status = case when p_approve then 'approved' else 'rejected' end,
         granted_by_kind = case when p_approve then 'master' else null end,
         decided_by = me, decided_at = now(),
         decision_note = nullif(btrim(coalesce(p_note,'')),'')
   where id = p_id;
  return jsonb_build_object('ok', true,
    'message', case when p_approve then 'Approved' else 'Rejected' end);
end $$;


-- ── ৮. স্টাফ নিজের মাস দেখবে ────────────────────────────────────────────────
create or replace function hr.leave_my_month(p_month text default null)
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare me text := hr.my_code(); mkey text; used int; rows jsonb;
begin
  if me is null or me = '' then
    return jsonb_build_object('ok', false, 'message', 'Not signed in');
  end if;
  mkey := coalesce(nullif(p_month,''), to_char(hr.today_ist(), 'YYYY-MM'));
  select count(*) into used from hr.staff_leave
   where person_code = me and month_key = mkey and status = 'approved';
  select coalesce(jsonb_agg(to_jsonb(t) order by t.leave_date), '[]'::jsonb) into rows
    from (select id, leave_date, status, block_reason, reason, decision_note,
                 granted_by_kind
            from hr.staff_leave
           where person_code = me and month_key = mkey
             and status in ('approved','pending','rejected')) t;
  return jsonb_build_object('ok', true, 'month', mkey,
                            'used', used, 'limit', 4, 'list', rows);
end $$;


-- ── ৯. কোন দিনগুলো নেওয়া যাবে না (ক্যালেন্ডারের জন্য) ──────────────────────
--     ⛔ সহকর্মীর **নাম দেখায় না** — শুধু "ওইদিন হবে না", এটুকুই।
create or replace function hr.leave_blocked_days()
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare me text := hr.my_code(); prof hr.staff_profiles%rowtype;
        chamber int[]; d date; out jsonb := '[]'::jsonb; why text[];
begin
  if me is null or me = '' then
    return jsonb_build_object('ok', false, 'message', 'Not signed in');
  end if;
  select * into prof from hr.staff_profiles where person_code = me;
  if not found then return jsonb_build_object('ok', false, 'message', 'Profile not found'); end if;
  select weekdays into chamber from hr.branch_chamber_days where branch = prof.branch;

  d := hr.today_ist();
  while d <= hr.today_ist() + 10 loop
    why := array[]::text[];
    if chamber is not null and extract(dow from d)::int = any(chamber) then
      why := why || 'chamber_day'::text;
    end if;
    if exists (select 1 from hr.staff_leave
               where branch = prof.branch and leave_date = d
                 and status = 'approved' and person_code <> me) then
      why := why || 'another_staff'::text;
    end if;
    if array_length(why,1) is not null then
      out := out || jsonb_build_object('date', d, 'why', array_to_string(why, ','));
    end if;
    d := d + 1;
  end loop;
  return jsonb_build_object('ok', true, 'days', out);
end $$;


-- ── ১০. মাস্টার অপেক্ষমাণ অনুরোধ দেখবেন ─────────────────────────────────────
create or replace function hr.leave_pending()
returns jsonb
language plpgsql security definer set search_path = hr, public as $$
declare rows jsonb;
begin
  if not hr.is_master() then
    return jsonb_build_object('ok', false, 'message', 'Only Master');
  end if;
  select coalesce(jsonb_agg(to_jsonb(t) order by t.leave_date), '[]'::jsonb) into rows
    from (select id, person_code, staff_name, branch, leave_date,
                 block_reason, reason, requested_at
            from hr.staff_leave where status = 'pending') t;
  return jsonb_build_object('ok', true, 'list', rows);
end $$;


-- ── ১১. অনুমতি ──────────────────────────────────────────────────────────────
grant usage on schema hr to authenticated;
grant select on hr.staff_leave, hr.branch_chamber_days to authenticated;
grant insert, update, delete on hr.branch_chamber_days to authenticated;
grant execute on function
  hr.today_ist(), hr.leave_apply(date, text), hr.leave_cancel(uuid),
  hr.leave_decide(uuid, boolean, text), hr.leave_my_month(text),
  hr.leave_blocked_days(), hr.leave_pending()
  to authenticated;
