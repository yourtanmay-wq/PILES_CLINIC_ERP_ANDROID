-- ═══════════════════════════════════════════════════════════════════════════
-- 🕒🔒 V1243 (০৮.০৯.২০২৬) — IN TIME শুধু চেম্বারের সময়ে (সকাল ৯টা – সন্ধ্যা ৬টা)
--
-- TK-নির্দেশ (হুবহু): "এত রাত্রে একটা স্টাফ চেম্বারে আসলো কি করে · সকাল ৯টা
-- থেকে সন্ধ্যা ৬টা পর্যন্ত আমাদের চেম্বার খোলা থাকে · রাত ১১টা ২১-এ আপনি
-- কিভাবে তাকে এলাউ করলেন" ⇒ "সকাল ৯টা থেকে সন্ধ্যা ৬টার বাইরে একেবারে
-- আটকে দিন"।
--
-- 🔴 যা পাওয়া গেল: IN TIME-এ কোনো সময়ের সীমা কোথাও বসানোই ছিল না — না
--    অ্যাপে, না এই ফাংশনে। শুধু পর্দায় "(Late)" লেখা হত, আটকানো হত না।
--
-- ⛔ এই ফাইলে V496-এর ফাংশনটা **হুবহু** আছে — শুধু উপরে একটা নতুন যাচাই
--    যোগ হয়েছে (v_min)। প্রোফাইল · ভূমিকা · ছুটি · atomic insert ·
--    "দিনে একবার" · নিরাপত্তা — একটাও লাইন বদলানো হয়নি।
-- ⛔ কোনো টেবিল/ঘর তৈরি বা মোছা হয়নি; পুরনো একটাও সারি ছোঁয়া হয়নি।
-- ⛔ বার্তাটা ইংরেজিতে — স্টাফের পর্দার নিয়ম, আর পুরনো APK-ও হুবহু দেখাতে পারবে।
-- ═══════════════════════════════════════════════════════════════════════════
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
  v_min    int;                                  -- 🕒 V1243
begin
  -- ── ১) কে ডাকছে (ফোনের কথা নয়, টোকেন থেকে) ──────────────────────────────
  v_code := hr.my_code();
  if coalesce(v_code, '') = '' then
    raise exception 'Sign-in required';        -- anon বা অচেনা → এখানেই শেষ
  end if;

  -- ── 🕒🔒 V1243 (০৮.০৯.২০২৬, TK-নির্দেশ) — চেম্বারের সময়ের বাইরে হাজিরা নয় ──
  --    TK: "সকাল ৯টা থেকে সন্ধ্যা ৬টার বাইরে একেবারে আটকে দিন"।
  --    ⛔ এটাই একমাত্র নতুন যাচাই — নিচের প্রতিটা লাইন V496-এর হুবহু।
  --    ⛔ সময় সার্ভারের ঘড়িতে, ভারতীয় সময়ে — ফোনের সময় বদলে লাভ নেই।
  v_min := extract(hour   from (now() at time zone 'Asia/Kolkata'))::int * 60
         + extract(minute from (now() at time zone 'Asia/Kolkata'))::int;
  if v_min < 540 or v_min > 1080 then          -- 540 = 09:00, 1080 = 18:00
    return query select 'outside_hours'::text, ''::text, ''::text, null::date,
      'Attendance can only be marked between 9:00 AM and 6:00 PM.'::text;
    return;
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
  'V1243 (TK, 08.09.2026): V496-এর সব নিয়ম অপরিবর্তিত + IN TIME শুধু সকাল ৯টা – সন্ধ্যা ৬টা (Asia/Kolkata)।';

revoke all on function wn.mark_check_in() from public, anon;
grant execute on function wn.mark_check_in() to authenticated;

commit;
