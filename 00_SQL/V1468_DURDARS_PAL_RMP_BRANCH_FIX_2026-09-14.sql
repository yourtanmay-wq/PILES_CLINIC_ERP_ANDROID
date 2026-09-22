-- ═══════════════════════════════════════════════════════════════════════════
-- V1468 (১৪.০৯.২০২৬) — DURDARS PAL (pat_9635099181, FLK-12092026-001)-এর
-- কমিশন সংশোধন — TK-অনুমোদিত ("হ্যাঁ, দুটোতেই করুন")।
--
-- আসল কারণ (ডেটা দেখে নিশ্চিত করা, আন্দাজ নয়): TK কোচবিহারের "TK BISWAS"
-- (dv_c1fe1542581143e4b5accc02dcb5222a) কার্ড খোলা অবস্থায় ফালাকাটার এই
-- রোগীর কমিশন বসিয়েছিলেন — রোগী সত্যিই ফালাকাটায় চিকিৎসা করিয়েছেন ও
-- ১২.০৯-এ সেখানেই ₹৫০০ জমা দিয়েছেন (V1466-এর ফলে যাচাই হয়েছে)।
--
-- এখন ঠিক ফালাকাটার "TK BISWAS" (dv_8ad730969a2542f2bef8e26b97c6cbc7)-এ
-- সরানো হচ্ছে। হার — V1469-এর ফলে যাচাই হয়েছে: এই rmp_id-র জন্য
-- fin.rmp_commission_branch_defaults-এ আলাদা ব্রাঞ্চ-সারি নেই, কিন্তু
-- fin.rmp_commission_defaults-এ এই rmp_id-র নিজের "global default" ৬০%
-- (FLK-1 স্টাফ, ১৪.০৮.২০২৬-এ বসানো) — যেহেতু এই RMP-রেকর্ডটা নিজেই শুধু
-- ফালাকাটার জন্য, তার "global" মানেই ফালাকাটার হার। সার্ভারের নিজের নিয়মের
-- (rmp_set_patient_commission) হুবহু একই দুই-ধাপের অগ্রাধিকার এখানেও —
-- ব্রাঞ্চ-নির্দিষ্ট থাকলে সেটা, নইলে এই rmp_id-র global default — আন্দাজে
-- কিছু বসানো হয়নি।
--
-- ⛔ শুধু এই একজন রোগীর কমিশন-সারি ছোঁয়া হচ্ছে (patient_row_id দিয়ে বাঁধা)।
-- ⛔ পেমেন্ট/বিল/রোগীর কোনো সারি ছোঁয়া হয় না।
-- ⛔ পুরনো (ভুল) মান audit trail-এ old_value হিসেবে থেকেই যাবে — মোছা হয় না।
-- ═══════════════════════════════════════════════════════════════════════════

begin;

do $$
declare
  v_patient_row_id text := 'pat_9635099181';
  v_correct_rmp_id text := 'dv_8ad730969a2542f2bef8e26b97c6cbc7';   -- TK BISWAS · Falakata
  v_rmp_name text := 'TK BISWAS';
  v_rmp_mobile text := '+918001080080';
  v_branch text := 'Falakata';
  v_rate numeric;
  v_old jsonb;
  v_comm_id uuid;
begin
  -- আসল জমা-করা হার খুঁজে বার করা (আন্দাজ নয়) — সার্ভারের নিজের নিয়মের
  -- মতোই দুই ধাপ: ① এই rmp_id-র ব্রাঞ্চ-নির্দিষ্ট সারি থাকলে সেটা আগে।
  select commission_value into v_rate
    from fin.rmp_commission_branch_defaults
   where rmp_id = v_correct_rmp_id and branch = v_branch;
  -- ② না থাকলে, এই rmp_id-র নিজের global default (V1469-এ যাচাই করা ৬০%)।
  if v_rate is null then
    select commission_value into v_rate
      from fin.rmp_commission_defaults
     where rmp_id = v_correct_rmp_id;
  end if;
  if v_rate is null then
    raise exception 'No rate on file for this RMP (branch or global) — stopping, nothing changed';
  end if;

  select id, to_jsonb(c) into v_comm_id, v_old
    from fin.rmp_patient_commissions c
   where patient_row_id = v_patient_row_id;
  if v_comm_id is null then
    raise exception 'DURDARS PAL commission row not found — stopping, nothing changed';
  end if;

  update fin.rmp_patient_commissions
     set rmp_id = v_correct_rmp_id,
         rmp_name = v_rmp_name,
         rmp_mobile = v_rmp_mobile,
         commission_mode = 'PERCENT',
         commission_value = v_rate,
         set_by = 'MASTER-TK-CORRECTION',
         updated_at = now()
   where id = v_comm_id;

  insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
  select 'RMP_BRANCH_CORRECTION', c.id::text, v_old, to_jsonb(c),
         'DURDARS PAL was treated at Falakata (₹500 paid there 12.09.2026); commission had been set against the Cooch Behar TK BISWAS card by mistake — corrected to the Falakata TK BISWAS card at its own branch rate.',
         'MASTER-TK-CORRECTION'
    from fin.rmp_patient_commissions c
   where c.id = v_comm_id;

  raise notice 'Corrected: rmp_id=% rate=%', v_correct_rmp_id, v_rate;
end $$;

commit;

-- ফল যাচাই করার জন্য (এই একই query আবার আলাদা করে চালালে বসা মান দেখা যাবে)।
select id, patient_row_id, patient_name, treatment_branch, rmp_id, rmp_name,
       commission_mode, commission_value, set_by, updated_at
from fin.rmp_patient_commissions
where patient_row_id = 'pat_9635099181';
