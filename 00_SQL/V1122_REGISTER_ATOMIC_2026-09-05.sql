-- ═══════════════════════════════════════════════════════════════════════
-- V1122 (০৫.০৯.২০২৬) — রেজিস্ট্রেশনের তিনটে সারি **একটাই লেনদেনে**
--
-- TK: *"ভিজিট ফি যখন বাধ্যতামূলক, তখন আমি এই ধরনের নোটিফিকেশন কেন মেনে নেব"*
--
-- 🔴 প্রমাণিত কারণ: রেজিস্ট্রেশনে তিনটে সারি (followups · patients · payments)
--    **আলাদা আলাদা তিনটে অনুরোধে** যেত, payments সবার শেষে। মাঝপথে লাইন কাটলে
--    প্রথম দুটো বসে যেত, ফি-র সারিটা ফোনের জমা-ঘরে পড়ে থাকত — ওই ফোন পরে
--    ফ্লাশ না করলে সারিটা চিরতরে হারাত, অথচ রোগী ক্লাউডে বসেই থাকত।
--
-- ⇒ এই ফাংশনটা তিনটে সারি **একসাথে** বসায়। যেকোনো একটায় গোলমাল হলে
--    **তিনটেরই কিছু বসে না** (Postgres-এর নিজের লেনদেন) — তাই "রোগী আছে,
--    ফি নেই" অবস্থাটা আর তৈরিই হতে পারে না।
--
-- ⛔ কোনো টেবিলের গড়ন বদলায় না · কোনো সারি মোছা হয় না · কোনো ট্রিগার ছোঁয়া হয় না।
-- ⛔ রোগীর সারি **আগে থেকে থাকলে** ফাংশনটা কিছুই করে না, ব্যর্থ বলে ফেরে —
--    তখন অ্যাপ তার পুরনো প্রমাণিত পথেই সেভ করে (আচরণ হুবহু আগের মতোই)।
-- ⛔ একই আইডিতে দ্বিতীয়বার ডাকলে কিছুই দুবার বসে না।
-- ⛔ এটা না চালালেও অ্যাপ আগের মতোই চলবে — শুধু পুরনো পথটা ব্যবহার হবে।
-- ═══════════════════════════════════════════════════════════════════════

create or replace function public.tk_register_patient(
  p_patient  jsonb,
  p_followup jsonb,
  p_payment  jsonb default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $fn$
declare
  v_pid text := coalesce(p_patient ->> 'id', '');
  v_fid text := coalesce(p_followup->> 'id', '');
  v_yid text := coalesce(p_payment ->> 'id', '');
begin
  if v_pid = '' or v_fid = '' then
    raise exception 'patient and followup id required';
  end if;

  -- রোগীর সারি আগে থেকে থাকলে এই পথ নয় — অ্যাপ নিজের পুরনো পথে যাবে।
  if exists (select 1 from public.patients where id = v_pid) then
    raise exception 'patient already exists';
  end if;

  insert into public.patients
  select r.* from jsonb_populate_record(null::public.patients, p_patient) as r;

  if not exists (select 1 from public.followups where id = v_fid) then
    insert into public.followups
    select r.* from jsonb_populate_record(null::public.followups, p_followup) as r;
  end if;

  if p_payment is not null and v_yid <> ''
     and not exists (select 1 from public.payments where id = v_yid) then
    insert into public.payments
    select r.* from jsonb_populate_record(null::public.payments, p_payment) as r;
  end if;

  return jsonb_build_object(
    'patient',  v_pid,
    'followup', v_fid,
    'payment',  v_yid,
    'feeOk',    (v_yid = '' or exists (select 1 from public.payments where id = v_yid))
  );
end;
$fn$;

revoke all on function public.tk_register_patient(jsonb, jsonb, jsonb) from public;
grant execute on function public.tk_register_patient(jsonb, jsonb, jsonb) to anon, authenticated;
