-- V1660 (2026-09-22) — Safe cross-branch RMP Call + Mandatory Remark only.
-- Applied to production Supabase during this release.
-- Existing patient/payment/commission/business tables are NOT altered here.

create table if not exists hr.rmp_call_access (
  person_code text not null,
  branch text not null,
  active boolean not null default true,
  granted_by text not null default '',
  granted_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (person_code, branch),
  constraint rmp_call_access_branch_chk check (branch in
    ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara'))
);

create table if not exists hr.rmp_call_access_audit (
  id uuid primary key default gen_random_uuid(),
  person_code text not null,
  branch text not null,
  enabled boolean not null,
  remark text not null,
  changed_by text not null,
  changed_at timestamptz not null default now()
);

create table if not exists hr.rmp_call_audit (
  id uuid primary key default gen_random_uuid(),
  person_code text not null,
  staff_mobile text not null default '',
  home_branch text not null default '',
  target_branch text not null,
  rmp_id text not null,
  rmp_name text not null default '',
  rmp_mobile text not null default '',
  remark text not null,
  next_call_date text not null default '',
  expected_patient_date text not null default '',
  called_on date not null,
  created_at timestamptz not null default now()
);

alter table hr.rmp_call_access enable row level security;
alter table hr.rmp_call_access_audit enable row level security;
alter table hr.rmp_call_audit enable row level security;

revoke all on hr.rmp_call_access from anon, authenticated;
revoke all on hr.rmp_call_access_audit from anon, authenticated;
revoke all on hr.rmp_call_audit from anon, authenticated;

create or replace function hr.can_call_rmp_branch(p_branch text)
returns boolean
language sql stable security definer
set search_path = hr, public, auth
as $$
  select exists (
    select 1
      from hr.app_identity ai
      left join hr.staff_profiles sp on sp.person_code = ai.person_code
     where ai.uid = auth.uid()
       and (
         coalesce(ai.is_master,false)
         or (
           sp.person_code is not null
           and coalesce(sp.active,false)
           and (sp.suspended_until is null or sp.suspended_until < current_date)
           and (
             coalesce(ai.branch, sp.branch, '') = coalesce(p_branch,'')
             or exists (
               select 1 from hr.rmp_call_access a
                where a.person_code = ai.person_code
                  and a.branch = p_branch
                  and a.active
             )
           )
         )
       )
  );
$$;
revoke all on function hr.can_call_rmp_branch(text) from public, anon;
grant execute on function hr.can_call_rmp_branch(text) to authenticated;

create or replace function hr.rmp_call_allowed_branches()
returns table(branch text)
language sql stable security definer
set search_path = hr, public, auth
as $$
  select a.branch
    from hr.rmp_call_access a
    join hr.app_identity ai on ai.uid = auth.uid() and ai.person_code = a.person_code
    join hr.staff_profiles sp on sp.person_code = ai.person_code
   where a.active
     and coalesce(sp.active,false)
     and (sp.suspended_until is null or sp.suspended_until < current_date)
     and a.branch <> coalesce(ai.branch, sp.branch, '')
   order by a.branch;
$$;
revoke all on function hr.rmp_call_allowed_branches() from public, anon;
grant execute on function hr.rmp_call_allowed_branches() to authenticated;

create or replace function hr.admin_rmp_call_access_list(p_person_code text)
returns table(branch text, enabled boolean)
language sql stable security definer
set search_path = hr, public, auth
as $$
  with b(branch) as (
    values ('Kishanganj'::text),('Jalpaiguri'),('Cooch Behar'),('Falakata'),('Birpara')
  )
  select b.branch,
         coalesce((select a.active from hr.rmp_call_access a
                    where a.person_code = upper(btrim(coalesce(p_person_code,'')))
                      and a.branch = b.branch), false) as enabled
    from b
   where hr.is_master()
   order by case b.branch
      when 'Kishanganj' then 1 when 'Jalpaiguri' then 2 when 'Cooch Behar' then 3
      when 'Falakata' then 4 when 'Birpara' then 5 else 99 end;
$$;
revoke all on function hr.admin_rmp_call_access_list(text) from public, anon;
grant execute on function hr.admin_rmp_call_access_list(text) to authenticated;

create or replace function hr.admin_set_rmp_call_access(
  p_person_code text, p_branch text, p_enabled boolean, p_remark text)
returns jsonb
language plpgsql security definer
set search_path = hr, public, auth
as $$
declare
  v_code text := upper(btrim(coalesce(p_person_code,'')));
  v_branch text := btrim(coalesce(p_branch,''));
  v_remark text := btrim(coalesce(p_remark,''));
  v_home text;
  v_role text;
  v_active boolean;
  v_by text := coalesce(hr.my_code(),'');
begin
  if not hr.is_master() then
    return jsonb_build_object('ok',false,'message','Only Master can change RMP Calling Access');
  end if;
  if v_remark = '' then
    return jsonb_build_object('ok',false,'message','Remark is required');
  end if;
  if v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    return jsonb_build_object('ok',false,'message','Invalid branch');
  end if;

  select coalesce(branch,''), coalesce(role_kind,''), coalesce(active,true)
    into v_home, v_role, v_active
    from hr.staff_profiles where person_code = v_code;
  if not found then
    return jsonb_build_object('ok',false,'message','Staff profile not found');
  end if;
  if v_role <> 'staff' then
    return jsonb_build_object('ok',false,'message','RMP Calling Access can be granted to staff only');
  end if;
  if not v_active then
    return jsonb_build_object('ok',false,'message','This staff member is not active');
  end if;
  if v_branch = v_home then
    return jsonb_build_object('ok',false,'message','Own branch already has normal access');
  end if;

  insert into hr.rmp_call_access(person_code,branch,active,granted_by,granted_at,updated_at)
  values(v_code,v_branch,coalesce(p_enabled,false),v_by,now(),now())
  on conflict(person_code,branch) do update set
    active=excluded.active, granted_by=excluded.granted_by, updated_at=now();

  insert into hr.rmp_call_access_audit(person_code,branch,enabled,remark,changed_by)
  values(v_code,v_branch,coalesce(p_enabled,false),v_remark,v_by);

  return jsonb_build_object('ok',true,'message',
    case when coalesce(p_enabled,false) then 'RMP Calling Access enabled' else 'RMP Calling Access disabled' end);
end;
$$;
revoke all on function hr.admin_set_rmp_call_access(text,text,boolean,text) from public, anon;
grant execute on function hr.admin_set_rmp_call_access(text,text,boolean,text) to authenticated;

create or replace function hr.rmp_call_directory(p_branch text)
returns table(
  id text, name text, mobile text, alt_mobiles text, area text, police_station text,
  branch text, remarks text, last_call_date text, next_call_date text,
  call_status text, expected_patient_date text, call_count integer,
  last_call_by text, last_call_time text
)
language plpgsql stable security definer
set search_path = hr, public, auth
as $$
begin
  if not hr.can_call_rmp_branch(p_branch) then
    raise exception 'RMP Calling Access not allowed for this branch' using errcode='42501';
  end if;
  return query
  select d.id, coalesce(d.name,''), coalesce(d.mobile,''), coalesce(d."altMobiles",''),
         coalesce(d.area,''), coalesce(d."policeStation",''), coalesce(d.branch,''),
         coalesce(d.remarks,''), coalesce(d."lastCallDate",''), coalesce(d."nextCallDate",''),
         coalesce(d."callStatus",''), coalesce(d."expectedPatientDate",''),
         jsonb_array_length(coalesce(d."callHistory",'[]'::jsonb))::integer,
         coalesce(d."callHistory"->0->>'by',''),
         coalesce(d."callHistory"->0->>'createdAt','')
    from public.doctor_visits d
   where d.branch = p_branch
     and coalesce(d.status,'Active') = 'Active'
   order by
     case when coalesce(d."nextCallDate",'')='' or d."nextCallDate" <= to_char(current_date,'YYYY-MM-DD') then 0 else 1 end,
     coalesce(d."nextCallDate",''), upper(coalesce(d.name,''));
end;
$$;
revoke all on function hr.rmp_call_directory(text) from public, anon;
grant execute on function hr.rmp_call_directory(text) to authenticated;

create or replace function hr.rmp_log_cross_branch_call(
  p_rmp_id text, p_note text, p_next_call_date text, p_expected_patient_date text)
returns jsonb
language plpgsql security definer
set search_path = hr, public, auth
as $$
declare
  v_code text;
  v_mobile text;
  v_home text;
  v_branch text;
  v_name text;
  v_rmp_mobile text;
  v_note text := btrim(coalesce(p_note,''));
  v_next text := btrim(coalesce(p_next_call_date,''));
  v_expected text := btrim(coalesce(p_expected_patient_date,''));
  v_today text := to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM-DD');
  v_now_text text := to_char(now() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
  v_entry jsonb;
begin
  select ai.person_code, coalesce(ai.link_mobile,''), coalesce(ai.branch,sp.branch,'')
    into v_code,v_mobile,v_home
    from hr.app_identity ai
    left join hr.staff_profiles sp on sp.person_code=ai.person_code
   where ai.uid=auth.uid()
     and (
       coalesce(ai.is_master,false)
       or (
         sp.person_code is not null
         and coalesce(sp.active,false)
         and (sp.suspended_until is null or sp.suspended_until < current_date)
       )
     );
  if v_code is null then
    return jsonb_build_object('ok',false,'message','Active staff login not verified');
  end if;
  if v_note='' then
    return jsonb_build_object('ok',false,'message','Remarks required');
  end if;
  if length(v_note) > 2000 then
    return jsonb_build_object('ok',false,'message','Remark is too long');
  end if;
  if v_next<>'' and v_next !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then
    return jsonb_build_object('ok',false,'message','Invalid next-call date');
  end if;
  if v_expected<>'' and v_expected !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then
    return jsonb_build_object('ok',false,'message','Invalid expected-patient date');
  end if;

  select d.branch, coalesce(d.name,''), coalesce(d.mobile,'')
    into v_branch,v_name,v_rmp_mobile
    from public.doctor_visits d
   where d.id=p_rmp_id and coalesce(d.status,'Active')='Active'
   for update;
  if not found then
    return jsonb_build_object('ok',false,'message','RMP not found');
  end if;
  if not hr.can_call_rmp_branch(v_branch) then
    return jsonb_build_object('ok',false,'message','RMP Calling Access is not allowed or was revoked');
  end if;

  v_entry := jsonb_build_object(
    'date',v_today,'note',v_note,'nextCallDate',v_next,
    'by',v_mobile,'byCode',v_code,'createdAt',v_now_text
  );

  update public.doctor_visits
     set "lastCallDate"=v_today,
         "nextCallDate"=v_next,
         "callStatus"='Called',
         remarks=v_note,
         "callHistory"=jsonb_build_array(v_entry) || coalesce("callHistory",'[]'::jsonb),
         "expectedPatientDate"=v_expected,
         "updatedAt"=v_now_text
   where id=p_rmp_id;

  insert into hr.rmp_call_audit(person_code,staff_mobile,home_branch,target_branch,
    rmp_id,rmp_name,rmp_mobile,remark,next_call_date,expected_patient_date,called_on)
  values(v_code,v_mobile,v_home,v_branch,p_rmp_id,v_name,v_rmp_mobile,v_note,v_next,v_expected,v_today::date);

  return jsonb_build_object('ok',true,'message','Doctor call updated');
end;
$$;
revoke all on function hr.rmp_log_cross_branch_call(text,text,text,text) from public, anon;
grant execute on function hr.rmp_log_cross_branch_call(text,text,text,text) to authenticated;

notify pgrst, 'reload schema';
