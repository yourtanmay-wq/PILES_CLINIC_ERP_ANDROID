-- V1659 credential V2 safe-path record, 22.09.2026.
-- Additive compatibility layer. Does NOT enable RLS or remove plaintext yet.

create or replace function public.tk_credential_verify_v2(
  p_mobile text,
  p_password text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_mobile text;
  v_password text;
begin
  v_mobile := right(regexp_replace(coalesce(p_mobile,''), '\D', '', 'g'), 10);
  if length(v_mobile) <> 10 or coalesce(p_password,'') = '' or length(p_password) > 256 then
    return jsonb_build_object('status','WRONG');
  end if;

  select uc.password into v_password
  from public.usercredentials uc
  where right(regexp_replace(coalesce(uc.mobile,''), '\D', '', 'g'), 10) = v_mobile
  limit 1;

  if not found or coalesce(v_password,'') = '' then
    return jsonb_build_object('status','NO_CUSTOM');
  end if;
  if p_password = v_password then
    return jsonb_build_object('status','OK');
  end if;
  return jsonb_build_object('status','WRONG');
end;
$$;

revoke all on function public.tk_credential_verify_v2(text,text) from public, anon, authenticated;
grant execute on function public.tk_credential_verify_v2(text,text) to anon, authenticated;

create or replace function public.tk_password_center_list_v2()
returns table(
  mobile text, role text, name text, branch text, password text,
  changed_by text, created_at text, updated_at text
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not hr.is_master() then
    raise exception 'Master Admin only' using errcode = '42501';
  end if;
  return query
  select uc.mobile, uc.role, uc.name, uc.branch, uc.password,
         uc."changedBy", uc."createdAt", uc."updatedAt"
  from public.usercredentials uc
  order by uc.mobile;
end;
$$;

revoke all on function public.tk_password_center_list_v2() from public, anon, authenticated;
grant execute on function public.tk_password_center_list_v2() to authenticated;

create or replace function public.tk_password_center_set_v2(
  p_mobile text,
  p_role text,
  p_name text,
  p_branch text,
  p_password text,
  p_password_hash text,
  p_changed_by text
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_mobile text;
  v_now text;
  v_match_count integer;
  v_existing_id text;
begin
  if not hr.is_master() then
    raise exception 'Master Admin only' using errcode = '42501';
  end if;

  v_mobile := right(regexp_replace(coalesce(p_mobile,''), '\D', '', 'g'), 10);
  if length(v_mobile) <> 10 then raise exception 'Invalid mobile' using errcode = '22023'; end if;
  if coalesce(p_password,'') = '' or length(p_password) > 256 then raise exception 'Invalid password' using errcode = '22023'; end if;
  if coalesce(p_password_hash,'') <> '' and p_password_hash not like 'pbkdf2_sha256$%' then
    raise exception 'Invalid password hash' using errcode = '22023';
  end if;

  select count(*)::integer, min(uc.id)
    into v_match_count, v_existing_id
  from public.usercredentials uc
  where right(regexp_replace(coalesce(uc.mobile,''), '\D', '', 'g'), 10) = v_mobile;

  if v_match_count > 1 then
    raise exception 'Duplicate credential rows for mobile' using errcode = '23505';
  end if;

  v_now := to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');

  if v_match_count = 1 then
    update public.usercredentials
       set mobile = v_mobile,
           role = coalesce(p_role,''),
           name = coalesce(p_name,''),
           branch = coalesce(p_branch,''),
           password = p_password,
           "changedBy" = coalesce(p_changed_by,''),
           "updatedAt" = v_now,
           password_hash = coalesce(p_password_hash,''),
           password_algo = case when coalesce(p_password_hash,'') like 'pbkdf2_sha256$%' then 'pbkdf2_sha256' else '' end
     where id = v_existing_id;
  else
    insert into public.usercredentials(
      id, mobile, role, name, branch, password,
      "changedBy", "createdAt", "updatedAt", password_hash, password_algo
    ) values(
      'cred_' || v_mobile, v_mobile, coalesce(p_role,''), coalesce(p_name,''),
      coalesce(p_branch,''), p_password, coalesce(p_changed_by,''), v_now, v_now,
      coalesce(p_password_hash,''),
      case when coalesce(p_password_hash,'') like 'pbkdf2_sha256$%' then 'pbkdf2_sha256' else '' end
    );
  end if;
  return true;
end;
$$;

revoke all on function public.tk_password_center_set_v2(text,text,text,text,text,text,text) from public, anon, authenticated;
grant execute on function public.tk_password_center_set_v2(text,text,text,text,text,text,text) to authenticated;
