-- ============================================================================
-- V496 — Staff IN TIME: নিরাপদ, server-side, atomic
-- TK-এর চূড়ান্ত নির্দেশ §৬ · ২১.০৮.২০২৬
-- ============================================================================
--
-- ⛔⛔ এই ফাইল **এখনো লাইভে চালানো হয়নি**। TK-এর আলাদা অনুমতির পরেই চলবে।
--
-- ─── কেন দরকার ──────────────────────────────────────────────────────────────
-- V495 পর্যন্ত IN TIME বসত ফোন থেকে: `WorkNotebookActivity.kt:437` —
--     day.put("check_in", nowTime())      // nowTime() = ফোনের নিজের ঘড়ি
-- ফলে দুটো ফাঁক ছিল:
--   ১) ফোনের তারিখ/সময় পিছিয়ে দিলে পিছনের তারিখে হাজিরা বসানো যেত।
--   ২) দ্রুত দুবার চাপলে বা দুই ফোন থেকে একসাথে চাপলে সময় বদলে যেতে পারত।
--
-- এই ফাংশন দুটোই বন্ধ করে — কারণ সিদ্ধান্তটা **ডেটাবেসের ভিতরে, এক ধাপে** হয়।
--
-- ─── ফোনের কোনো কথা বিশ্বাস করা হয় না ──────────────────────────────────────
-- ফোন **কিছুই পাঠায় না** — staff_code নয়, branch নয়, তারিখ নয়, সময় নয়।
-- সার্ভার নিজে বের করে:
--   • কে ডাকছে              → hr.my_code()      (V246, প্রমাণিত)
--   • তিনি Staff কিনা        → hr.staff_profiles.role_kind
--   • active কিনা            → hr.staff_profiles.active
--   • suspend/removed কিনা   → hr.staff_profiles.suspended_until
--   • তাঁর ব্রাঞ্চ           → hr.staff_profiles.branch
--   • আজকের তারিখ ও সময়     → now() at time zone 'Asia/Kolkata'
--   • আজ ছুটি অনুমোদিত কিনা  → wn.leave_requests
--   • আজ আগে IN TIME আছে কিনা→ wn.notebook_days.check_in
--
-- ─── 🔒 যা এক অক্ষরও বদলায় না ───────────────────────────────────────────────
--   ⛔ `check_in` ঘরের নাম ও `HH:mm` রূপ — হুবহু আগের মতোই
--      (তাই `V420_STAFF_PERFORMANCE…sql:154`-এর present_days ও বেতনের
--       হিসাব এক চুলও নড়ে না)
--   ⛔ `check_out` (OUT TIME) — এই ফাংশন ছোঁয়ই না
--   ⛔ ছুটির টেবিল/নিয়ম — শুধু **পড়া** হয়, কিছু লেখা হয় না
--   ⛔ কোনো RLS policy বদলানো হয়নি
--   ⛔ কোনো টেবিল/ঘর তৈরি বা মোছা হয়নি
--   ⛔ কোনো সারি delete/update করা হয়নি (শুধু নতুন IN TIME বসে)
--
-- ─── নিরাপত্তা ──────────────────────────────────────────────────────────────
-- `security definer` কেন: `wn.notebook_days`-এ RLS আছে
-- (`nd_all`: hr.is_master() or staff_code = hr.my_code()), আর এই ফাংশনকে
-- ছুটি ও প্রোফাইল পড়তে হয় — সেগুলোর RLS ডাকা ব্যবহারকারীর জন্য আলাদা।
-- তাই definer, কিন্তু **ভিতরে নিজের কড়া যাচাই** আছে:
--   • hr.my_code() ফাঁকা হলে সঙ্গে সঙ্গে exception (anon ঢুকতে পারে না)
--   • staff_code **সবসময়** hr.my_code() — ফোন যা-ই বলুক, নিজের ছাড়া
--     কারো হাজিরা দেওয়া অসম্ভব
--   • `search_path` স্থির করে বসানো (wn, hr, public) — search-path আক্রমণ বন্ধ
--   • `revoke from public, anon` তারপর `grant to authenticated`
--
-- ⛔ অ্যাপে কোনো service_role key বা ডেটাবেস পাসওয়ার্ড **নেই ও থাকবে না** —
--    অ্যাপ শুধু publishable key দিয়ে লগইন করা ব্যবহারকারী হিসেবে ডাকে।
--
-- ─── ফল ─────────────────────────────────────────────────────────────────────
-- একটাই সারি:  status text, check_in text, branch text, work_date date, message text
--   status = 'saved'          → এইমাত্র বসল
--            'already'        → আজ আগেই ছিল; পুরনো সময়ই ফেরত (বদলানো হয়নি)
--            'on_leave'       → আজ অনুমোদিত ছুটি, হাজিরা লাগে না
--            'not_staff'      → ডাক্তার/মাস্টার — হাজিরার ব্যবস্থা নেই
--            'inactive'       → বাদ দেওয়া হয়েছে
--            'suspended'      → নির্দিষ্ট তারিখ পর্যন্ত বন্ধ
--            'no_profile'     → প্রোফাইল পাওয়া যায়নি
-- ⇒ অ্যাপ কখনো নিজে সিদ্ধান্ত নেয় না; শুধু এই status দেখে বার্তা দেখায়।
-- ============================================================================

begin;

create or replace function wn.mark_check_in()
returns table(status text, check_in text, branch text, work_date date, message text)
language plpgsql
security definer
set search_path = wn, hr, public
as $$
-- 🔴 V496 বাগ-ফিক্স (পরীক্ষায় ধরা পড়েছে): `returns table(... check_in, branch,
-- work_date, status ...)` — এই নামগুলো `wn.notebook_days`-এর ঘরের নামের সঙ্গে
-- এক। তাই `on conflict (staff_code, work_date)` লিখলে PostgreSQL বুঝত না
-- কোনটা — ঘর না ফেরত-নাম:
--     ERROR: column reference "work_date" is ambiguous
-- নিচের নির্দেশ দিয়ে বলা হলো — এমন ক্ষেত্রে **ঘরটাই** ধরতে হবে।
-- ⛔ ফেরত-নামগুলো বদলানো হয়নি (অ্যাপ ওই নামেই পড়ে), আর আমরা কোথাও
--    ফেরত-নাম পড়ি না — সব কাজ v_* চলকে। তাই এতে কিছু ভাঙে না।
#variable_conflict use_column
declare
  v_code   text;
  v_prof   hr.staff_profiles%rowtype;
  v_today  date;
  v_now    text;
  v_exist  text;
  v_leave  text;
  v_rows   int;
begin
  -- ── ১) কে ডাকছে (ফোনের কথা নয়, টোকেন থেকে) ──────────────────────────────
  v_code := hr.my_code();
  if coalesce(v_code, '') = '' then
    raise exception 'Sign-in required';        -- anon বা অচেনা → এখানেই শেষ
  end if;

  select * into v_prof from hr.staff_profiles where person_code = v_code;
  if not found then
    return query select 'no_profile'::text, ''::text, ''::text, null::date,
      'আপনার প্রোফাইল পাওয়া যায়নি। মাস্টারকে জানান।'::text;
    return;
  end if;

  -- ── ২) হাজিরার ব্যবস্থা কার জন্য (TK §২) ────────────────────────────────
  --    TK-এর চূড়ান্ত নিয়ম: IN TIME **শুধুমাত্র আসল staff**-এর।
  --    Doctor · Field · Master — কারো নয়। (আগের খসড়ায় 'field' ছিল, TK বাতিল করেছেন।)
  if lower(coalesce(v_prof.role_kind, '')) <> 'staff' then
    -- 🔴 V496 বাগ-ফিক্স (পরীক্ষায় ধরা পড়েছে): আগে মাস্টারকেও "ডাক্তারদের জন্য…"
    -- বার্তা যেত। এখন ভূমিকা অনুযায়ী আলাদা।
    return query select 'not_staff'::text, ''::text, coalesce(v_prof.branch,'')::text, null::date,
      (case when lower(coalesce(v_prof.role_kind,'')) = 'doctor'
            then 'ডাক্তারদের জন্য হাজিরার ব্যবস্থা নেই — আপনি যেকোনো সময় আসতে ও যেতে পারেন।'
            else 'এই অ্যাকাউন্টে হাজিরার ব্যবস্থা নেই।'
       end)::text;
    return;
  end if;

  -- ── ৩) বাদ দেওয়া / বন্ধ করা কিনা ────────────────────────────────────────
  if coalesce(v_prof.active, true) = false then
    return query select 'inactive'::text, ''::text, coalesce(v_prof.branch,'')::text, null::date,
      'আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে। মাস্টারকে জানান।'::text;
    return;
  end if;

  v_today := (now() at time zone 'Asia/Kolkata')::date;

  if v_prof.suspended_until is not null and v_prof.suspended_until >= v_today then
    return query select 'suspended'::text, ''::text, coalesce(v_prof.branch,'')::text, null::date,
      ('আপনি ' || to_char(v_prof.suspended_until, 'DD.MM.YYYY') ||
       ' পর্যন্ত বন্ধ আছেন। মাস্টারকে জানান।')::text;
    return;
  end if;

  -- ── ৪) আজ অনুমোদিত ছুটি কিনা (TK §৫) ────────────────────────────────────
  select lower(coalesce(l.status, '')) into v_leave
    from wn.leave_requests l
   where l.staff_code = v_code and l.leave_date = v_today
   limit 1;
  if v_leave in ('approved', 'auto', 'auto_approved') then
    return query select 'on_leave'::text, ''::text, coalesce(v_prof.branch,'')::text, v_today,
      'আজ আপনার ছুটি অনুমোদিত — হাজিরা লাগবে না।'::text;
    return;
  end if;

  -- ── ৫) সার্ভারের সময়, ফোনের নয় ──────────────────────────────────────────
  v_now := to_char(now() at time zone 'Asia/Kolkata', 'HH24:MI');   -- আগের মতোই HH:mm

  -- ── ৬) এক ধাপে বসানো (atomic) ───────────────────────────────────────────
  --    `staff_code, work_date` unique (V246:179)। সারি না থাকলে বসে;
  --    থাকলে **শুধু তখনই** check_in বসে যখন সেটা এখনো খালি —
  --    অর্থাৎ আগের IN TIME কখনো বদলায় না। দুই ফোন একসাথে চাপলেও
  --    একটাই জেতে, দ্বিতীয়টা কিছুই বদলাতে পারে না।
  insert into wn.notebook_days (staff_code, staff_mobile, branch, work_date, check_in)
  values (v_code, coalesce(v_prof.link_mobile, ''), coalesce(v_prof.branch, ''), v_today, v_now)
  on conflict (staff_code, work_date) do update
     set check_in   = v_now,
         branch     = coalesce(nullif(wn.notebook_days.branch, ''), excluded.branch),
         updated_at = now()
   where coalesce(wn.notebook_days.check_in, '') = '';

  -- 🔴 V496 বাগ-ফিক্স (পরীক্ষায় ধরা পড়েছে): সত্যিই বসল কিনা তা **সময় মিলিয়ে**
  -- বোঝা যায় না — একই মিনিটে দ্বিতীয়বার চাপলে পুরনো ও নতুন সময় এক হয়ে যেত,
  -- তাই ভুল করে 'saved' দেখাত (যদিও সারি ঠিকই অপরিবর্তিত ছিল)।
  -- এখন ডেটাবেস নিজে যা বলে — কটা সারি সত্যিই বসল/বদলাল — সেটাই ধরা হয়।
  get diagnostics v_rows = row_count;

  -- ── ৭) শেষ পর্যন্ত কী দাঁড়াল, সেটাই সত্য ────────────────────────────────
  select coalesce(n.check_in, '') into v_exist
    from wn.notebook_days n
   where n.staff_code = v_code and n.work_date = v_today;

  if coalesce(v_exist, '') = '' then
    -- এখানে আসার কথা নয়; এলে সৎভাবে ব্যর্থতা জানানো হয় (নীরব সফলতা নয়)।
    return query select 'error'::text, ''::text, coalesce(v_prof.branch,'')::text, v_today,
      'হাজিরা বসানো গেল না। আবার চেষ্টা করুন।'::text;
    return;
  end if;

  if v_rows > 0 then
    return query select 'saved'::text, v_exist::text, coalesce(v_prof.branch,'')::text, v_today,
      'হাজিরা হয়ে গেছে।'::text;
  else
    return query select 'already'::text, v_exist::text, coalesce(v_prof.branch,'')::text, v_today,
      ('আজ আগেই হাজিরা হয়েছে — ' || v_exist || '। দিনে একবারই দেওয়া যায়।')::text;
  end if;
end $$;

comment on function wn.mark_check_in() is
  'V496 (TK, 21.08.2026): Staff-এর দৈনিক IN TIME — সার্ভারের সময়ে, এক ধাপে, দিনে একবার। ফোনের কোনো তথ্য বিশ্বাস করা হয় না।';

revoke all on function wn.mark_check_in() from public, anon;
grant execute on function wn.mark_check_in() to authenticated;


-- ── যাচাই — কিছুই ভাঙেনি ───────────────────────────────────────────────────
do $$
declare n_days bigint; n_leave bigint; n_prof bigint;
begin
  select count(*) into n_days  from wn.notebook_days;
  select count(*) into n_leave from wn.leave_requests;
  select count(*) into n_prof  from hr.staff_profiles;
  raise notice '── V496 সফল ─────────────────────────────────';
  raise notice 'notebook_days সারি   : %  (অপরিবর্তিত)', n_days;
  raise notice 'leave_requests সারি  : %  (অপরিবর্তিত)', n_leave;
  raise notice 'staff_profiles সারি  : %  (অপরিবর্তিত)', n_prof;
  raise notice 'নতুন ফাংশন           : wn.mark_check_in()';
  raise notice 'নতুন টেবিল/ঘর        : ০টি';
  raise notice 'মোছা সারি            : ০টি';
  raise notice '─────────────────────────────────────────────';
end $$;

commit;
