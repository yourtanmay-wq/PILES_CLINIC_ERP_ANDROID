-- ============================================================================
-- V770 — 📱 কোন ফোনে কোন ভার্সন চলছে (অ্যাপ থেকেই দেখা যাবে)
--
-- TK-নির্দেশ (২৮.০৮.২০২৬): *"আমি কি করে জানবো — App থেকে দেখার ব্যবস্থা রাখুন"*
--
-- ❗ কেন দরকার (প্রমাণসহ)
--   Supabase লগে দেখা গেছে একটা পড়া বারবার **400 ভুল** দিচ্ছে —
--   `deleted_records?select=*&order=updatedAt…`। এই ডাকটা **আজকের কোডে নেই**
--   (V451-এ ঠিক করা)। অর্থাৎ কোনো ফোনে এখনো **পুরনো ভার্সন** চলছে, আর সেটাই
--   অকারণে Egress খরচ করছে। কোন ফোন — সেটা না জানলে ঠিক করা যায় না।
--
-- 🔒 নিরাপত্তা
--   · নতুন কোনো টেবিল নেই — `hr.staff_profiles`-এ মাত্র **২টি ঘর** যোগ।
--   · ফোন যা লিখতে পারে: **শুধু নিজের ভার্সন ও সময়** — আর কিচ্ছু নয়।
--   · নাম · মোবাইল · বেতন · ভূমিকা — কিছুই ছোঁয়া যায় না।
--   · তালিকা দেখা **শুধু মাস্টার** (`hr.is_master()`)।
--   · কোনো নতুন সারি তৈরি হয় না — আগে থেকে থাকা মোবাইল হলে তবেই বসে।
--
-- ⛔ পুরনো কোনো তথ্য/নিয়ম বদলায় না। দুবার চালালেও ক্ষতি নেই।
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================

-- ── ১. দুটো ঘর যোগ (থাকলে কিছুই হয় না) ────────────────────────────────────
alter table hr.staff_profiles
  add column if not exists app_version_code int,
  add column if not exists app_seen_at timestamptz;

-- ── ২. ফোন নিজের ভার্সন জানায় (লগইনের আগেও চলে, তাই anon) ────────────────
--    ⛔ শুধু ওই দুটো ঘর, শুধু আগে থেকে থাকা মোবাইলে। নতুন সারি বানায় না।
create or replace function public.report_app_version(p_mobile text, p_version int)
returns void
language plpgsql security definer set search_path = hr, public as $$
declare v_mob text := regexp_replace(coalesce(p_mobile,''), '[^0-9]', '', 'g');
begin
  if length(v_mob) <> 10 or coalesce(p_version,0) <= 0 then return; end if;
  update hr.staff_profiles
     set app_version_code = p_version,
         app_seen_at = now()
   where link_mobile = v_mob;
end $$;
revoke all on function public.report_app_version(text,int) from public;
grant execute on function public.report_app_version(text,int) to anon, authenticated;

-- ── ৩. মাস্টারের তালিকা (কে কোন ভার্সনে, শেষ কবে খুলেছে) ──────────────────
create or replace function hr.app_devices_list()
returns table(person_code text, full_name text, branch text,
              role_kind text, app_version_code int, app_seen_at timestamptz)
language sql stable security definer set search_path = hr, public as $$
  select person_code, full_name, branch, role_kind, app_version_code, app_seen_at
    from hr.staff_profiles
   where hr.is_master()
     and coalesce(active, true)
     and coalesce(role_kind,'') in ('staff','doctor','field','master')
   order by app_version_code nulls first, branch, full_name
$$;
revoke all on function hr.app_devices_list() from public, anon;
grant execute on function hr.app_devices_list() to authenticated;

notify pgrst, 'reload schema';
