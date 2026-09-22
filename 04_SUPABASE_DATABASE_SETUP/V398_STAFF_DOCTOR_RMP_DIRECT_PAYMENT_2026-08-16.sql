-- =====================================================================
-- V398 (16.08.2026) — Staff / Doctor নিজের ব্রাঞ্চের RMP-কে রোগী না বেছেও
-- টাকা দিতে পারবেন।  TK-অনুমোদিত (16.08.2026): "হ্যাঁ, নিজের ব্রাঞ্চের RMP-কে পারবেন"
--
-- আগে কী ছিল (V383 ফাইলে):
--     if not hr.is_master() then
--       raise exception 'Only Master can record an unallocated RMP payment';
--     end if;
-- অর্থাৎ Staff/Doctor অ্যাপে বোতাম পেলেও ডেটাবেস তাঁদের আটকে দিত।
--
-- এখন কী হবে:
--   ১. Master · Staff · Doctor — তিনজনই পারবেন (`fin.rmp_can_use()`)।
--   ২. তবে **শুধু নিজের ব্রাঞ্চের** RMP-কে (`fin.rmp_can_write_branch()`) —
--      প্রজেক্টের আগে থেকেই থাকা ও পরীক্ষিত সেই একই নিয়ম, নতুন কিছু নয়।
--   ৩. Ref. Due-র চেয়ে **বেশি** টাকা দিতে হলে আগের মতোই **শুধু Master** —
--      হুবহু সেই নিয়ম যেটা `fin.rmp_pay_commission`-এ (V325, লাইন ৩০৯)
--      ইতিমধ্যেই চলছে।
--
-- ⛔ টাকার কোনো পুরোনো সারি ছোঁয়া হয় না — এটি শুধু একটি function-এর নিয়ম বদল।
-- ⛔ হিসাবের সূত্র (`v_paid + v_unallocated`, Due, Overpaid) একটুও বদলায়নি।
-- ⛔ `fin.rmp_allocate_advance` (রোগীর সঙ্গে মেলানো) আগের মতোই **Master-only**
--    রইল — TK আলাদা করে না বললে ওটা বদলানো হবে না।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → পুরোটা পেস্ট করে Run।
-- একবারের বেশি চালালেও ক্ষতি নেই (create or replace)।
-- =====================================================================

-- =====================================================================
-- ধাপ ০ (নতুন, 16.08.2026 বিকেল) — "Ref. Paid সাথে সাথে আপডেট হচ্ছে না"
--
-- TK-এর পরীক্ষার ফল: পর্দা বন্ধ করে আবার খুললেও Ref. Paid ₹4,300-ই থাকে,
-- অথচ RMP Performance ₹7,000 দেখায়।
--
-- কারণ (কোড মিলিয়ে): দুই জায়গা দুই ফাংশন ব্যবহার করে —
--   • RMP Performance → fin.rmp_legacy_performance  (V382 ফাইলে) — এটি
--     unallocated advance (`amount - allocated_amount`) যোগ করে → ₹7,000 ✔
--   • ডাক্তারের পর্দা  → fin.rmp_rmp_summary
--       - V325-এর পুরোনো রূপে: paid_to_this_rmp = শুধু commission payments
--         (unallocated advance **বাদ**) → ₹4,300 ✘
--       - V383-এর নতুন রূপে : paid_to_this_rmp = commission + unallocated
--         → ₹7,000 ✔
--
-- অর্থাৎ **V383 ফাইলটি Supabase-এ চালানো হয়নি** — এটাই সবচেয়ে সম্ভাব্য কারণ।
--
-- আগে শুধু এই একটি লাইন চালিয়ে দেখুন (কিছুই বদলায় না, শুধু পড়া):
--   select pg_get_functiondef(oid) like '%v_unallocated%' as v383_installed
--     from pg_proc where proname = 'rmp_rmp_summary';
--   → false এলে নিচের ধাপ ০ ঠিক এই সমস্যাটাই সারাবে।
--   → true এলে আমাকে জানাবেন — তখন অন্য কারণ খুঁজতে হবে।
--
-- নিচেরটা V383 ফাইলের হুবহু একই সংজ্ঞা, শুধু একবারে চালানোর সুবিধার জন্য
-- এখানে আবার রাখা হলো। ⛔ টাকার কোনো সারি ছোঁয়া হয় না।
-- =====================================================================

create or replace function fin.rmp_rmp_summary(p_rmp_id text)
returns table(patient_count bigint, earned numeric, paid_to_this_rmp numeric,
              previous_rmp_paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare c record; s record; v_count bigint:=0; v_earned numeric:=0; v_due numeric:=0;
        v_over numeric:=0; v_paid numeric:=0; v_previous numeric:=0;
        v_unallocated numeric:=0; v_branch text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;
  for c in select patient_row_id from fin.rmp_patient_commissions where rmp_id=p_rmp_id loop
    select * into s from fin.rmp_summary(c.patient_row_id);
    v_count:=v_count+1; v_earned:=v_earned+coalesce(s.earned,0);
    v_due:=v_due+coalesce(s.due,0); v_over:=v_over+coalesce(s.overpaid,0);
  end loop;
  select coalesce(sum(x.amount),0) into v_paid
    from fin.rmp_commission_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount-x.allocated_amount),0) into v_unallocated
    from fin.rmp_advance_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount),0) into v_previous
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions pc on pc.id=x.patient_commission_id
   where pc.rmp_id=p_rmp_id and x.rmp_id<>p_rmp_id;
  return query select v_count,round(v_earned,2),round(v_paid+v_unallocated,2),round(v_previous,2),
    round(greatest(0,v_due-v_unallocated),2),round(v_over+greatest(0,v_unallocated-v_due),2);
end $$;

revoke all on function fin.rmp_rmp_summary(text) from public,anon;
grant execute on function fin.rmp_rmp_summary(text) to authenticated;

create or replace function fin.rmp_record_advance(
  p_rmp_id text, p_amount numeric, p_paid_on date, p_mode text, p_reference_no text default null)
returns uuid language plpgsql security definer set search_path=fin,public,hr as $$
declare d record; s record; v_expense uuid; v_id uuid;
begin
  -- 🟢 V398: Master + Staff + Doctor (আগে শুধু Master ছিল)
  if not fin.rmp_can_use() then
    raise exception 'Master, Staff or Doctor identity required';
  end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  if p_paid_on is null or p_paid_on>(now() at time zone 'Asia/Kolkata')::date then
    raise exception 'Future payment date is not allowed';
  end if;
  if upper(coalesce(p_mode,'')) not in ('CASH','ONLINE') then raise exception 'Payment mode must be CASH or ONLINE'; end if;

  select id,name,coalesce(branch,'') branch into d from public.doctor_visits where id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;

  -- 🟢 V398: নিজের ব্রাঞ্চ ছাড়া অন্য ব্রাঞ্চের RMP-কে টাকা দেওয়া যাবে না।
  --    Master-এর ক্ষেত্রে এই ফাংশন সবসময় true ফেরে, তাই Master-এর কিছুই বদলায়নি।
  if not fin.rmp_can_write_branch(d.branch) then
    raise exception 'Not allowed for this RMP branch';
  end if;

  -- 🟢 V398: Ref. Due-র চেয়ে বেশি হলে শুধু Master। (rmp_pay_commission-এর একই নিয়ম।)
  if not hr.is_master() then
    select * into s from fin.rmp_rmp_summary(p_rmp_id);
    if p_amount > coalesce(s.due,0) then
      raise exception 'Payment is higher than Ref. Due — only Master can approve this';
    end if;
  end if;

  insert into fin.expenses(entry_date,branch,category,paid_to,amount,mode,note,ignored,created_by)
  values(p_paid_on,d.branch,'RMP Commission Advance',d.name,p_amount,upper(p_mode),
    nullif(trim(coalesce(p_reference_no,'')),''),false,hr.my_code()) returning id into v_expense;

  insert into fin.rmp_advance_payments(rmp_id,rmp_name,branch,paid_on,amount,mode,reference_no,expense_id,recorded_by)
  values(d.id,d.name,d.branch,p_paid_on,p_amount,upper(p_mode),nullif(trim(coalesce(p_reference_no,'')),''),v_expense,hr.my_code())
  returning id into v_id;

  insert into fin.rmp_commission_audit(action,entity_id,new_value,changed_by)
  values('RMP_ADVANCE_PAYMENT',v_id::text,
         jsonb_build_object('rmp_id',d.id,'amount',p_amount,'paid_on',p_paid_on,'v398_role','staff_doctor_allowed'),
         hr.my_code());
  return v_id;
end $$;

revoke all on function fin.rmp_record_advance(text,numeric,date,text,text) from public,anon;
grant execute on function fin.rmp_record_advance(text,numeric,date,text,text) to authenticated;
notify pgrst,'reload schema';

-- =====================================================================
-- যাচাই (শুধু পড়া — কিছুই বদলায় না):
--   select proname, pg_get_functiondef(oid) like '%rmp_can_use%' as staff_allowed
--     from pg_proc where proname='rmp_record_advance';
-- `staff_allowed` = true এলে বুঝবেন নতুন নিয়ম বসে গেছে।
-- =====================================================================
