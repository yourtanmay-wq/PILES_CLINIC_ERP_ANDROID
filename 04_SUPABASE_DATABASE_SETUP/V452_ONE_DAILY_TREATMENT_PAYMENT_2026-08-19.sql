-- V452 · 19.08.2026 · TK-approved option A
-- ONE PATIENT + ONE CALENDAR DAY = ONE TREATMENT PAYMENT going forward.
-- A genuine second same-day collection is added to that day's canonical row.
-- CASH and ONLINE stay separately preserved, and every actual collection event
-- (amount/mode/staff/time/remark) remains in dailyEvents for audit/performance.
-- IMPORTANT: historical duplicate payment rows are NOT deleted or rewritten.

alter table public.payments add column if not exists "cashAmount" numeric;
alter table public.payments add column if not exists "onlineAmount" numeric;
alter table public.payments add column if not exists "dailyEvents" jsonb;

create or replace function public.tk_merge_daily_treatment_payment()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $fn$
declare
  v_day text := left(coalesce(new."date", ''), 10);
  v_type text := lower(coalesce(new."payType", ''));
  v_rem text := lower(coalesce(new."remarks", ''));
  v_owner text;
  v_old public.payments%rowtype;
  v_have_old boolean := false;
  v_old_amount numeric := 0;
  v_old_cash numeric := 0;
  v_old_online numeric := 0;
  v_new_amount numeric := 0;
  v_new_cash numeric := 0;
  v_new_online numeric := 0;
  v_delta_amount numeric := 0;
  v_delta_cash numeric := 0;
  v_delta_online numeric := 0;
  v_old_events jsonb := '[]'::jsonb;
  v_new_events jsonb := '[]'::jsonb;
  v_unique_events jsonb := '[]'::jsonb;
  v_ev jsonb;
  v_eid text;
  v_eamt numeric;
  v_emode text;
  v_mode text;
  v_new_remark text;
  v_updated integer := 0;
  v_exact_id boolean := false;
  v_old_stamp text := '';
  v_new_stamp text := '';
begin
  begin v_new_amount := coalesce(nullif(new."amount", ''), '0')::numeric;
  exception when others then v_new_amount := 0; end;

  -- Only real Treatment money belongs to this rule. Visit Fee, Medicine,
  -- Refund, bill-edit, attendance marks and every other existing workflow pass unchanged.
  if not (v_type in ('', 'treatment'))
     or v_rem like '%visit fee%'
     or v_rem like '%registration fee%'
     or v_day = ''
     or v_new_amount <= 0 then
    return new;
  end if;

  v_owner := case
    when nullif(new."patientId", '') is not null then 'pid:' || new."patientId"
    when nullif(new."patientCode", '') is not null then 'code:' || new."patientCode"
    else 'mob:' || right(regexp_replace(coalesce(new."mobile", ''), '\D', '', 'g'), 10)
         || '|br:' || lower(coalesce(new."branch", ''))
  end;

  -- Two phones saving at the same moment cannot both create separate daily rows.
  perform pg_advisory_xact_lock(hashtext(v_owner || '|' || v_day)::bigint);

  -- SAFETY 1: if this physical id ALREADY exists, this is an upsert/retry of
  -- that exact row. Keep that exact row as the owner. This prevents an old
  -- device re-uploading a historical duplicate from adding its old money again
  -- into another same-day row.
  select p.* into v_old from public.payments p where p.id = new.id limit 1 for update;
  v_have_old := found;
  v_exact_id := v_have_old;

  -- SAFETY 2: genuinely NEW id -> find that patient's existing row for the day.
  if not v_have_old then
    select p.* into v_old
    from public.payments p
    where left(coalesce(p."date", ''), 10) = v_day
      and lower(coalesce(p."payType", '')) in ('', 'treatment')
      and lower(coalesce(p."remarks", '')) not like '%visit fee%'
      and lower(coalesce(p."remarks", '')) not like '%registration fee%'
      and (
        (nullif(new."patientId", '') is not null and p."patientId" = new."patientId")
        or (nullif(new."patientCode", '') is not null and p."patientCode" = new."patientCode")
        or (
          nullif(new."patientId", '') is null and nullif(new."patientCode", '') is null
          and right(regexp_replace(coalesce(p."mobile", ''), '\D', '', 'g'), 10)
              = right(regexp_replace(coalesce(new."mobile", ''), '\D', '', 'g'), 10)
          and lower(coalesce(p."branch", '')) = lower(coalesce(new."branch", ''))
        )
      )
    order by coalesce(p."createdAt", '') asc, p.id asc
    limit 1
    for update;
    v_have_old := found;
  end if;

  begin v_new_cash := coalesce(new."cashAmount", 0);
  exception when others then v_new_cash := 0; end;
  begin v_new_online := coalesce(new."onlineAmount", 0);
  exception when others then v_new_online := 0; end;
  if v_new_cash <= 0 and v_new_online <= 0 and v_new_amount > 0 then
    if upper(coalesce(new."mode", 'CASH')) in ('ONLINE', 'UPI') then
      v_new_online := v_new_amount;
    else
      v_new_cash := v_new_amount;
    end if;
  end if;

  if jsonb_typeof(new."dailyEvents") = 'array' and jsonb_array_length(new."dailyEvents") > 0 then
    v_new_events := new."dailyEvents";
  else
    v_new_events := jsonb_build_array(jsonb_build_object(
      'eventId', new.id,
      'amount', v_new_amount,
      'mode', case when upper(coalesce(new."mode", 'CASH')) in ('ONLINE','UPI') then 'ONLINE' else 'CASH' end,
      'receivedBy', coalesce(new."receivedBy", ''),
      'createdBy', coalesce(new."createdBy", ''),
      'createdAt', coalesce(new."createdAt", new."updatedAt", ''),
      'remarks', coalesce(new."remarks", '')
    ));
    new."dailyEvents" := v_new_events;
  end if;

  -- First genuine Treatment payment on that day: normal insert, with split normalized.
  if not v_have_old then
    new."cashAmount" := v_new_cash;
    new."onlineAmount" := v_new_online;
    new."mode" := case
      when v_new_cash > 0 and v_new_online > 0 then 'MIXED'
      when v_new_online > 0 then 'ONLINE'
      else 'CASH'
    end;
    return new;
  end if;

  begin v_old_amount := coalesce(nullif(v_old."amount", ''), '0')::numeric;
  exception when others then v_old_amount := 0; end;
  begin v_old_cash := coalesce(v_old."cashAmount", 0);
  exception when others then v_old_cash := 0; end;
  begin v_old_online := coalesce(v_old."onlineAmount", 0);
  exception when others then v_old_online := 0; end;
  if v_old_cash <= 0 and v_old_online <= 0 and v_old_amount > 0 then
    if upper(coalesce(v_old."mode", 'CASH')) in ('ONLINE', 'UPI') then
      v_old_online := v_old_amount;
    else
      v_old_cash := v_old_amount;
    end if;
  end if;

  if jsonb_typeof(v_old."dailyEvents") = 'array' and jsonb_array_length(v_old."dailyEvents") > 0 then
    v_old_events := v_old."dailyEvents";
    -- A single-event row may have had its amount corrected later by the
    -- existing Payment Edit workflow. Make its one audit event reflect the
    -- current authoritative row before a second same-day event is appended.
    if jsonb_array_length(v_old_events) = 1 then
      v_ev := v_old_events->0;
      v_old_events := jsonb_build_array(
        v_ev || jsonb_build_object(
          'eventId', coalesce(nullif(v_ev->>'eventId',''), v_old.id),
          'amount', v_old_amount,
          'mode', case when v_old_online > 0 and v_old_cash <= 0 then 'ONLINE' else 'CASH' end,
          'receivedBy', coalesce(nullif(v_ev->>'receivedBy',''), v_old."receivedBy", ''),
          'createdBy', coalesce(nullif(v_ev->>'createdBy',''), v_old."createdBy", ''),
          'createdAt', coalesce(nullif(v_ev->>'createdAt',''), v_old."createdAt", ''),
          'remarks', coalesce(nullif(v_ev->>'remarks',''), v_old."remarks", '')
        )
      );
    end if;
  else
    v_old_events := jsonb_build_array(jsonb_build_object(
      'eventId', v_old.id,
      'amount', v_old_amount,
      'mode', case when v_old_online > 0 and v_old_cash <= 0 then 'ONLINE' else 'CASH' end,
      'receivedBy', coalesce(v_old."receivedBy", ''),
      'createdBy', coalesce(v_old."createdBy", ''),
      'createdAt', coalesce(v_old."createdAt", v_old."updatedAt", ''),
      'remarks', coalesce(v_old."remarks", '')
    ));
  end if;

  -- Exact-id UPSERT is also the existing Web/offline correction path. A one-event
  -- daily row may be legitimately corrected (amount/mode) and must therefore be
  -- allowed to reach ON CONFLICT UPDATE. A stale older copy is rejected by its
  -- timestamp. Multi-event rows do NOT take this shortcut: they continue through
  -- the event-id merge below, so a stale browser can never flatten a CASH+ONLINE
  -- day or add the same event twice.
  if v_exact_id
     and jsonb_array_length(v_old_events) <= 1
     and jsonb_array_length(v_new_events) <= 1 then
    v_old_stamp := coalesce(nullif(v_old."updatedAt",''), nullif(v_old."createdAt",''), '');
    v_new_stamp := coalesce(nullif(new."updatedAt",''), nullif(new."createdAt",''), '');
    if v_old_stamp <> '' and v_new_stamp <> '' and v_new_stamp < v_old_stamp then
      return null;
    end if;
    new."cashAmount" := v_new_cash;
    new."onlineAmount" := v_new_online;
    new."mode" := case
      when v_new_cash > 0 and v_new_online > 0 then 'MIXED'
      when v_new_online > 0 then 'ONLINE'
      else 'CASH'
    end;
    return new;
  end if;

  -- Retry-safe: only eventIds that are not already inside the physical owner row
  -- contribute money. Re-sending the same queued event therefore adds ₹0.
  for v_ev in select value from jsonb_array_elements(v_new_events)
  loop
    v_eid := coalesce(v_ev->>'eventId', '');
    if v_eid <> '' and exists (
      select 1 from jsonb_array_elements(v_old_events) e
      where coalesce(e->>'eventId','') = v_eid
    ) then
      continue;
    end if;
    begin v_eamt := coalesce(nullif(v_ev->>'amount',''), '0')::numeric;
    exception when others then v_eamt := 0; end;
    if v_eamt <= 0 then continue; end if;
    v_emode := upper(coalesce(v_ev->>'mode', new."mode", 'CASH'));
    v_delta_amount := v_delta_amount + v_eamt;
    if v_emode in ('ONLINE', 'UPI') then v_delta_online := v_delta_online + v_eamt;
    else v_delta_cash := v_delta_cash + v_eamt;
    end if;
    v_unique_events := v_unique_events || jsonb_build_array(v_ev);
  end loop;

  -- Exact retry / stale full-row sync: do not overwrite a newer combined row.
  if jsonb_array_length(v_unique_events) = 0 then
    return null;
  end if;

  v_mode := case
    when (v_old_cash + v_delta_cash) > 0 and (v_old_online + v_delta_online) > 0 then 'MIXED'
    when (v_old_online + v_delta_online) > 0 then 'ONLINE'
    else 'CASH'
  end;
  v_new_remark := trim(coalesce(new."remarks", ''));

  update public.payments
  set "amount" = trim(to_char(v_old_amount + v_delta_amount, 'FM999999999999990.################')),
      "cashAmount" = v_old_cash + v_delta_cash,
      "onlineAmount" = v_old_online + v_delta_online,
      "mode" = v_mode,
      "dailyEvents" = v_old_events || v_unique_events,
      "remarks" = case
        when v_new_remark = '' then v_old."remarks"
        when trim(coalesce(v_old."remarks", '')) = '' then v_new_remark
        when trim(coalesce(v_old."remarks", '')) = v_new_remark then v_old."remarks"
        else trim(coalesce(v_old."remarks", '')) || ' | ' || v_new_remark
      end,
      "updatedAt" = coalesce(nullif(new."updatedAt", ''), nullif(new."createdAt", ''), v_old."updatedAt")
  where id = v_old.id;
  get diagnostics v_updated = row_count;

  -- If another existing safety trigger blocks that owner row update, never lose
  -- genuine money silently: allow the new physical row instead.
  if v_updated = 0 then return new; end if;
  return null;
end;
$fn$;

revoke all on function public.tk_merge_daily_treatment_payment() from public, anon, authenticated;
drop trigger if exists tk_merge_daily_treatment_payment on public.payments;
create trigger tk_merge_daily_treatment_payment
before insert on public.payments
for each row execute function public.tk_merge_daily_treatment_payment();

-- Payment Edit safety. Existing old clients still PATCH amount/mode directly.
-- For a single-event Treatment row, keep the Cash/Online split and audit event
-- synchronized automatically. A combined multi-event day cannot be flattened
-- by an old one-amount/one-mode editor; only a split-aware update that also
-- changes dailyEvents may alter it.
create or replace function public.tk_normalize_daily_treatment_edit()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $fn$
declare
  v_type text := lower(coalesce(new."payType",''));
  v_rem text := lower(coalesce(new."remarks",''));
  v_amt numeric := 0;
  v_mode text := upper(coalesce(new."mode",'CASH'));
  v_count integer := 0;
  v_event jsonb;
begin
  if not (v_type in ('','treatment'))
     or v_rem like '%visit fee%'
     or v_rem like '%registration fee%' then
    return new;
  end if;

  if new."amount" is not distinct from old."amount"
     and new."mode" is not distinct from old."mode" then
    return new;
  end if;

  begin v_amt := coalesce(nullif(new."amount",''),'0')::numeric;
  exception when others then v_amt := 0; end;
  if v_amt <= 0 then return new; end if;

  if jsonb_typeof(old."dailyEvents")='array' then
    v_count := jsonb_array_length(old."dailyEvents");
  end if;

  if v_count > 1 and new."dailyEvents" is not distinct from old."dailyEvents" then
    raise exception 'combined daily payment requires split-safe correction';
  end if;

  -- A legitimate split-aware merge/correction supplied its own changed events.
  if v_count > 1 then return new; end if;
  if jsonb_typeof(new."dailyEvents")='array'
     and jsonb_array_length(new."dailyEvents") > 1 then return new; end if;

  v_mode := case when v_mode in ('ONLINE','UPI') then 'ONLINE' else 'CASH' end;
  new."cashAmount" := case when v_mode='CASH' then v_amt else 0 end;
  new."onlineAmount" := case when v_mode='ONLINE' then v_amt else 0 end;
  new."mode" := v_mode;

  if jsonb_typeof(old."dailyEvents")='array' and jsonb_array_length(old."dailyEvents")=1 then
    v_event := old."dailyEvents"->0;
  else
    v_event := jsonb_build_object(
      'eventId', old.id,
      'receivedBy', coalesce(old."receivedBy",''),
      'createdBy', coalesce(old."createdBy",''),
      'createdAt', coalesce(old."createdAt",''),
      'remarks', coalesce(old."remarks",'')
    );
  end if;
  v_event := v_event || jsonb_build_object('amount',v_amt,'mode',v_mode);
  new."dailyEvents" := jsonb_build_array(v_event);
  return new;
end;
$fn$;

revoke all on function public.tk_normalize_daily_treatment_edit() from public, anon, authenticated;
drop trigger if exists tk_normalize_daily_treatment_edit on public.payments;
create trigger tk_normalize_daily_treatment_edit
before update of "amount","mode","dailyEvents" on public.payments
for each row execute function public.tk_normalize_daily_treatment_edit();

-- New clients use this RPC so they receive the REAL canonical row even when the
-- INSERT was merged into an older same-day row. Old clients are still protected
-- by the trigger above.
create or replace function public.tk_record_treatment_payment(p_row jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $fn$
declare
  v_id text := coalesce(p_row->>'id','');
  v_type text := lower(coalesce(p_row->>'payType',''));
  v_amount numeric := 0;
  v_confirm public.payments%rowtype;
begin
  if v_id = '' then raise exception 'payment id required'; end if;
  begin v_amount := coalesce(nullif(p_row->>'amount',''),'0')::numeric;
  exception when others then v_amount := 0; end;
  if not (v_type in ('','treatment')) or v_amount <= 0 then
    raise exception 'treatment payment required';
  end if;

  insert into public.payments
  select r.* from jsonb_populate_record(null::public.payments, p_row) as r;

  select p.* into v_confirm
  from public.payments p
  where p.id = v_id
     or (jsonb_typeof(p."dailyEvents")='array'
         and p."dailyEvents" @> jsonb_build_array(jsonb_build_object('eventId',v_id)))
  order by case when p.id=v_id then 0 else 1 end, coalesce(p."createdAt",'') asc, p.id asc
  limit 1;

  if not found then raise exception 'payment not confirmed'; end if;
  return to_jsonb(v_confirm);
end;
$fn$;
revoke all on function public.tk_record_treatment_payment(jsonb) from public;
grant execute on function public.tk_record_treatment_payment(jsonb) to anon, authenticated;

-- Expand only multi-event daily rows for Staff Performance. Single-event rows
-- stay row-authoritative so the existing Payment Edit workflow remains exact.
create or replace function hr.perf_payment_events()
returns table(
  payment_id text, event_id text, pay_date text, name text, mobile text, branch text,
  amount numeric, mode text, pay_type text, pay_label text, remarks text,
  patient_id text, patient_code text, received_by text, created_by text,
  created_at text, status text
)
language sql stable security definer
set search_path = hr, public, wn
as $fn$
  with p as (
    select y.*,
      (lower(coalesce(y."payType",''))='treatment'
       and jsonb_typeof(y."dailyEvents")='array'
       and jsonb_array_length(y."dailyEvents") > 1) as expand_events
    from public.payments y
  )
  select p.id, p.id, p."date", p."name", p."mobile", p."branch",
         hr.perf_num(p."amount"),
         case when upper(coalesce(p."mode",'CASH')) in ('ONLINE','UPI') then 'ONLINE' else 'CASH' end,
         p."payType", coalesce(nullif(p."payLabel",''), nullif(p."paymentLabel",''), p."payType"),
         p."remarks", p."patientId", p."patientCode", p."receivedBy", p."createdBy", p."createdAt", p."status"
  from p where not p.expand_events
  union all
  select p.id,
         coalesce(nullif(ev.e->>'eventId',''), p.id || ':' || ev.ord::text),
         p."date", p."name", p."mobile", p."branch",
         hr.perf_num(ev.e->>'amount'),
         case when upper(coalesce(ev.e->>'mode','CASH')) in ('ONLINE','UPI') then 'ONLINE' else 'CASH' end,
         p."payType", coalesce(nullif(p."payLabel",''), nullif(p."paymentLabel",''), p."payType"),
         coalesce(nullif(ev.e->>'remarks',''), p."remarks"), p."patientId", p."patientCode",
         coalesce(nullif(ev.e->>'receivedBy',''), p."receivedBy"),
         coalesce(nullif(ev.e->>'createdBy',''), p."createdBy"),
         coalesce(nullif(ev.e->>'createdAt',''), p."createdAt"), p."status"
  from p
  cross join lateral jsonb_array_elements(p."dailyEvents") with ordinality ev(e,ord)
  where p.expand_events;
$fn$;
revoke all on function hr.perf_payment_events() from public, anon, authenticated;

-- Existing Staff Performance shape stays identical; only Cash/Online attribution
-- reads the actual money event when a daily row contains both modes/staff.
create or replace function hr.staff_performance(p_month text)
returns table(person_code text, full_name text, branch text, enquiry_count integer,
  registration_count integer, treatment_count integer, rmp_added integer,
  app_calls integer, outside_calls integer, cash_collected numeric,
  online_collected numeric, present_days integer, reports_sent integer, leave_days integer)
language sql stable security definer
set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (select k as key,length(k) as klen from (select case when coalesce(p_month,'')~'^\d{4}-\d{2}-\d{2}$' then p_month when coalesce(p_month,'')~'^\d{4}-\d{2}$' then p_month else to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM') end as k) q),
  staff as (select s.person_code,s.full_name,s.branch,hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s where s.active is not false and lower(coalesce(s.role_kind,''))<>'doctor' and upper(coalesce(s.person_code,'')) not like 'DR-%')
  select st.person_code,coalesce(nullif(st.full_name,''),st.person_code),coalesce(st.branch,''),
    (select count(*)::int from public.enquiries e,mon where left(coalesce(e."date",''),mon.klen)=mon.key and length(st.m10)=10 and hr.perf_m10(coalesce(nullif(e."receivedBy",''),e."createdBy"))=st.m10),
    (select count(*)::int from public.patients p,mon where left(coalesce(nullif(p."registrationDate",''),p."date",''),mon.klen)=mon.key and length(st.m10)=10 and hr.perf_m10(coalesce(nullif(p."registeredBy",''),p."createdBy"))=st.m10),
    (select count(*)::int from public.patients p,mon where left(coalesce(nullif(p."registrationDate",''),p."date",''),mon.klen)=mon.key and length(st.m10)=10 and hr.perf_m10(coalesce(nullif(p."registeredBy",''),p."createdBy"))=st.m10 and exists(select 1 from public.payments y where y."patientId"=p.id and lower(coalesce(y."payType",''))='treatment' and hr.perf_num(y."amount")>0)),
    (select count(*)::int from public.doctor_visits d,mon where left(coalesce(d."createdAt",d."date",''),mon.klen)=mon.key and length(st.m10)=10 and hr.perf_m10(d."createdBy")=st.m10),
    (select count(*)::int from wn.call_taps c,mon where left(to_char(c.call_date,'YYYY-MM-DD'),mon.klen)=mon.key and c.staff_code=st.person_code),
    (select count(*)::int from wn.outside_calls o,mon where left(to_char(o.call_date,'YYYY-MM-DD'),mon.klen)=mon.key and o.staff_code=st.person_code),
    (select coalesce(sum(y.amount),0) from hr.perf_payment_events() y,mon where left(coalesce(y.pay_date,''),mon.klen)=mon.key and length(st.m10)=10 and hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by))=st.m10 and upper(coalesce(y.mode,'CASH'))<>'ONLINE' and lower(coalesce(y.pay_type,''))<>'refund'),
    (select coalesce(sum(y.amount),0) from hr.perf_payment_events() y,mon where left(coalesce(y.pay_date,''),mon.klen)=mon.key and length(st.m10)=10 and hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by))=st.m10 and upper(coalesce(y.mode,'CASH'))='ONLINE' and lower(coalesce(y.pay_type,''))<>'refund'),
    (select count(*)::int from wn.notebook_days n,mon where left(to_char(n.work_date,'YYYY-MM-DD'),mon.klen)=mon.key and n.staff_code=st.person_code and coalesce(n.check_in,'')<>''),
    (select count(distinct w.period_key)::int from wn.work_reports w,mon where w.period_type='daily' and left(coalesce(w.period_key,''),mon.klen)=mon.key and w.staff_code=st.person_code),
    (select count(*)::int from wn.leave_requests l,mon where left(to_char(l.leave_date,'YYYY-MM-DD'),mon.klen)=mon.key and l.staff_code=st.person_code and lower(coalesce(l.status,'')) in ('approved','accepted'))
  from staff st,guard order by 4 desc,5 desc,2;
$fn$;
revoke all on function hr.staff_performance(text) from public, anon;
grant execute on function hr.staff_performance(text) to authenticated;

create or replace function hr.branch_performance(p_month text)
returns table(person_code text, full_name text, branch text, enquiry_count integer,
  registration_count integer, treatment_count integer, rmp_added integer,
  app_calls integer, outside_calls integer, cash_collected numeric,
  online_collected numeric, present_days integer, reports_sent integer, leave_days integer)
language sql stable security definer
set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (select k as key,length(k) as klen from (select case when coalesce(p_month,'')~'^\d{4}-\d{2}-\d{2}$' then p_month when coalesce(p_month,'')~'^\d{4}-\d{2}$' then p_month else to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM') end as k) q),
  known as (select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s where s.active is not false and lower(coalesce(s.role_kind,''))<>'doctor' and upper(coalesce(s.person_code,'')) not like 'DR-%' and length(hr.perf_m10(s.link_mobile))=10),
  br as (select unnest(array['Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara']) as b)
  select ('BRANCH-'||br.b),(upper(br.b)||' (BRANCH)'),br.b,
    (select count(*)::int from public.enquiries e,mon where left(coalesce(e."date",''),mon.klen)=mon.key and e."branch"=br.b and length(hr.perf_m10(coalesce(nullif(e."receivedBy",''),e."createdBy")))=10 and hr.perf_m10(coalesce(nullif(e."receivedBy",''),e."createdBy")) not in(select k.m10 from known k)),
    (select count(*)::int from public.patients p,mon where left(coalesce(nullif(p."registrationDate",''),p."date",''),mon.klen)=mon.key and p."branch"=br.b and length(hr.perf_m10(coalesce(nullif(p."registeredBy",''),p."createdBy")))=10 and hr.perf_m10(coalesce(nullif(p."registeredBy",''),p."createdBy")) not in(select k.m10 from known k)),
    (select count(*)::int from public.patients p,mon where left(coalesce(nullif(p."registrationDate",''),p."date",''),mon.klen)=mon.key and p."branch"=br.b and length(hr.perf_m10(coalesce(nullif(p."registeredBy",''),p."createdBy")))=10 and hr.perf_m10(coalesce(nullif(p."registeredBy",''),p."createdBy")) not in(select k.m10 from known k) and exists(select 1 from public.payments y where y."patientId"=p.id and lower(coalesce(y."payType",''))='treatment' and hr.perf_num(y."amount")>0)),
    (select count(*)::int from public.doctor_visits d,mon where left(coalesce(d."createdAt",d."date",''),mon.klen)=mon.key and coalesce(d."branch",'')=br.b and length(hr.perf_m10(d."createdBy"))=10 and hr.perf_m10(d."createdBy") not in(select k.m10 from known k)),
    0,0,
    (select coalesce(sum(y.amount),0) from hr.perf_payment_events() y,mon where left(coalesce(y.pay_date,''),mon.klen)=mon.key and y.branch=br.b and upper(coalesce(y.mode,'CASH'))<>'ONLINE' and lower(coalesce(y.pay_type,''))<>'refund' and length(hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by)))=10 and hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by)) not in(select k.m10 from known k)),
    (select coalesce(sum(y.amount),0) from hr.perf_payment_events() y,mon where left(coalesce(y.pay_date,''),mon.klen)=mon.key and y.branch=br.b and upper(coalesce(y.mode,'CASH'))='ONLINE' and lower(coalesce(y.pay_type,''))<>'refund' and length(hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by)))=10 and hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by)) not in(select k.m10 from known k)),
    0,0,0
  from br,guard;
$fn$;
revoke all on function hr.branch_performance(text) from public, anon;
grant execute on function hr.branch_performance(text) to authenticated;

create or replace function hr.perf_payment_list_v2(p_month text, p_code text, p_mode text)
returns table(id text,pay_date text,name text,mobile text,branch text,amount numeric,mode text,pay_type text,pay_label text,remarks text,patient_id text,patient_code text,received_by text,created_by text,created_at text,status text)
language sql stable security definer
set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (select k as key,length(k) as klen from (select case when coalesce(p_month,'')~'^\d{4}-\d{2}-\d{2}$' then p_month when coalesce(p_month,'')~'^\d{4}-\d{2}$' then p_month else to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM') end as k) q),
  st as (select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s where s.person_code=p_code),
  known as (select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s where s.active is not false and lower(coalesce(s.role_kind,''))<>'doctor' and upper(coalesce(s.person_code,'')) not like 'DR-%' and length(hr.perf_m10(s.link_mobile))=10)
  select y.event_id,y.pay_date,y.name,y.mobile,y.branch,y.amount,y.mode,y.pay_type,y.pay_label,y.remarks,y.patient_id,y.patient_code,y.received_by,y.created_by,y.created_at,y.status
  from hr.perf_payment_events() y,mon,guard
  where left(coalesce(y.pay_date,''),mon.klen)=mon.key
    and (case when lower(coalesce(p_mode,'cash'))='online' then upper(coalesce(y.mode,'CASH'))='ONLINE' else upper(coalesce(y.mode,'CASH'))<>'ONLINE' end)
    and lower(coalesce(y.pay_type,''))<>'refund'
    and ((p_code like 'BRANCH-%' and y.branch=substring(p_code from 8) and length(hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by)))=10 and hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by)) not in(select m10 from known))
      or (p_code not like 'BRANCH-%' and exists(select 1 from st where length(st.m10)=10) and hr.perf_m10(coalesce(nullif(y.received_by,''),y.created_by))=(select m10 from st)))
  order by y.pay_date desc,y.created_at desc nulls last,y.event_id;
$fn$;
revoke all on function hr.perf_payment_list_v2(text,text,text) from public, anon;
grant execute on function hr.perf_payment_list_v2(text,text,text) to authenticated;

notify pgrst, 'reload schema';
