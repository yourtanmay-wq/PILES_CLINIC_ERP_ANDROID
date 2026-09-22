-- V1657 · Partner settlement safety
-- Atomic server-side settlement: mandatory note, master-only, one DB transaction.
create or replace function fin.partner_settle_atomic(
  p_branch text,
  p_note text,
  p_rows jsonb
) returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_branch text := trim(coalesce(p_branch,''));
  v_note text := trim(coalesce(p_note,''));
  v_row jsonb;
  v_mobile text;
  v_kind text;
  v_amount numeric;
  v_balance numeric;
  v_count integer := 0;
  v_today date := (now() at time zone 'Asia/Kolkata')::date;
begin
  if not hr.is_master() then
    raise exception 'Master only';
  end if;
  if v_branch = '' then
    raise exception 'Branch is required';
  end if;
  if length(v_note) < 1 then
    raise exception 'Remarks / Note is mandatory';
  end if;
  if length(v_note) > 200 then
    raise exception 'Remarks / Note is too long';
  end if;
  if p_rows is null or jsonb_typeof(p_rows) <> 'array' or jsonb_array_length(p_rows) < 1 then
    raise exception 'No settlement rows supplied';
  end if;
  if jsonb_array_length(p_rows) > 20 then
    raise exception 'Too many settlement rows';
  end if;

  -- One settlement per branch at a time; concurrent/double RPC calls serialize here.
  perform pg_advisory_xact_lock(hashtext('fin.partner_settle_atomic'), hashtext(v_branch));

  for v_row in select value from jsonb_array_elements(p_rows)
  loop
    v_mobile := right(regexp_replace(coalesce(v_row->>'mobile',''), '\\D', '', 'g'), 10);
    v_kind := lower(trim(coalesce(v_row->>'kind','')));
    begin
      v_amount := (v_row->>'amount')::numeric;
      v_balance := (v_row->>'balance_before')::numeric;
    exception when others then
      raise exception 'Invalid settlement amount';
    end;

    if length(v_mobile) <> 10 then raise exception 'Invalid partner mobile'; end if;
    if v_kind not in ('withdraw','return') then raise exception 'Invalid settlement type'; end if;
    if v_amount <= 0 then raise exception 'Settlement amount must be positive'; end if;
    if abs(abs(v_balance) - v_amount) > 0.01 then raise exception 'Settlement amount does not match balance'; end if;
    if (v_balance > 0 and v_kind <> 'withdraw') or (v_balance < 0 and v_kind <> 'return') then
      raise exception 'Settlement direction does not match balance';
    end if;
    if not exists (
      select 1 from fin.partners p
      where p.branch = v_branch and right(regexp_replace(p.mobile, '\\D', '', 'g'),10) = v_mobile and p.active = true
    ) then
      raise exception 'Active partner not found for branch';
    end if;

    insert into fin.partner_drawings(branch,mobile,entry_date,amount,kind,mode,note,ignored,created_by)
    values(v_branch,v_mobile,v_today,v_amount,v_kind,'cash',v_note,false,hr.my_code());

    insert into fin.partner_settlements(branch,mobile,settled_on,balance_before,note,created_by)
    values(v_branch,v_mobile,v_today,v_balance,v_note,hr.my_code());

    v_count := v_count + 1;
  end loop;

  return jsonb_build_object('ok',true,'count',v_count,'branch',v_branch,'settled_on',v_today);
end;
$$;

revoke all on function fin.partner_settle_atomic(text,text,jsonb) from public, anon;
grant execute on function fin.partner_settle_atomic(text,text,jsonb) to authenticated;
