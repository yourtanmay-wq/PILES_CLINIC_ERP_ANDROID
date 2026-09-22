-- V1520 — One Leave Request = one active Leave Notice
-- Application code uses a deterministic ID; this database guard is the final
-- backstop for retries, old clients and simultaneous submissions.

create or replace function wn.leave_notice_staff(p_message text)
returns text
language plpgsql
immutable
as $function$
declare m text[];
begin
  m := regexp_match(coalesce(p_message,''), 'Staff[[:space:]]*:[[:space:]]*([^\r\n]+)', 'i');
  if m is null or array_length(m,1) is null then return null; end if;
  return nullif(trim(m[1]),'');
end
$function$;

create or replace function wn.leave_notice_date(p_message text)
returns date
language plpgsql
immutable
as $function$
declare m text[]; v text;
begin
  m := regexp_match(coalesce(p_message,''), 'Leave date[[:space:]]*:[[:space:]]*([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}[./-][0-9]{2}[./-][0-9]{4})', 'i');
  if m is null or array_length(m,1) is null then return null; end if;
  v := trim(m[1]);
  if v ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then return v::date; end if;
  v := replace(replace(v,'.','/'),'-','/');
  return to_date(v,'DD/MM/YYYY');
exception when others then
  return null;
end
$function$;

create or replace function wn.guard_leave_briefing_insert()
returns trigger
language plpgsql
security definer
set search_path = wn, public, pg_temp
as $function$
declare
  v_staff text;
  v_leave date;
  v_branch text;
  v_status text;
  v_existing text;
  v_now text := to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
begin
  if lower(trim(coalesce(new.title,''))) <> 'leave request' then return new; end if;

  v_staff := wn.leave_notice_staff(new.message);
  v_leave := wn.leave_notice_date(new.message);
  v_branch := trim(coalesce(new.branch,''));
  if v_staff is null or v_leave is null or v_branch='' then return new; end if;

  select lower(trim(coalesce(l.status,''))) into v_status
    from wn.leave_requests l
   where l.staff_code = v_staff
     and l.leave_date = v_leave
     and lower(trim(coalesce(l.branch,''))) = lower(v_branch)
   order by l.updated_at desc nulls last
   limit 1;

  if found and v_status <> 'pending' then
    update public.briefings b
       set "deletedAt" = coalesce(nullif(b."deletedAt",''), v_now),
           "deletedBy" = case when coalesce(b."deletedBy",'')='' then 'SYSTEM-LEAVE-CLOSED' else b."deletedBy" end,
           "updatedAt" = v_now
     where lower(trim(coalesce(b.title,'')))='leave request'
       and coalesce(b."deletedAt",'')=''
       and lower(trim(coalesce(b.branch,'')))=lower(v_branch)
       and wn.leave_notice_staff(b.message)=v_staff
       and wn.leave_notice_date(b.message)=v_leave;
    return null;
  end if;

  select b.id into v_existing
    from public.briefings b
   where lower(trim(coalesce(b.title,'')))='leave request'
     and coalesce(b."deletedAt",'')=''
     and lower(trim(coalesce(b.branch,'')))=lower(v_branch)
     and wn.leave_notice_staff(b.message)=v_staff
     and wn.leave_notice_date(b.message)=v_leave
   order by b."createdAt" asc nulls last, b.id
   limit 1;

  if v_existing is not null then
    update public.briefings b
       set message = new.message,
           targets = coalesce(new.targets,b.targets),
           "updatedAt" = coalesce(nullif(new."updatedAt",''),v_now)
     where b.id=v_existing;
    return null;
  end if;

  return new;
end
$function$;

revoke all on function wn.guard_leave_briefing_insert() from public, anon;
drop trigger if exists trg_leave_briefing_dedupe on public.briefings;
create trigger trg_leave_briefing_dedupe
before insert on public.briefings
for each row execute function wn.guard_leave_briefing_insert();

create or replace function wn.close_leave_briefings_on_decision()
returns trigger
language plpgsql
security definer
set search_path = wn, public, pg_temp
as $function$
declare
  v_now text := to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
begin
  if lower(trim(coalesce(new.status,''))) <> 'pending'
     and lower(trim(coalesce(old.status,''))) = 'pending' then
    update public.briefings b
       set "deletedAt" = coalesce(nullif(b."deletedAt",''),v_now),
           "deletedBy" = case when coalesce(b."deletedBy",'')='' then 'SYSTEM-LEAVE-'||upper(coalesce(new.status,'CLOSED')) else b."deletedBy" end,
           "updatedAt" = v_now
     where lower(trim(coalesce(b.title,'')))='leave request'
       and coalesce(b."deletedAt",'')=''
       and lower(trim(coalesce(b.branch,'')))=lower(trim(coalesce(new.branch,'')))
       and wn.leave_notice_staff(b.message)=new.staff_code
       and wn.leave_notice_date(b.message)=new.leave_date;
  end if;
  return new;
end
$function$;

revoke all on function wn.close_leave_briefings_on_decision() from public, anon;
drop trigger if exists trg_close_leave_briefings_on_decision on wn.leave_requests;
create trigger trg_close_leave_briefings_on_decision
after update of status on wn.leave_requests
for each row execute function wn.close_leave_briefings_on_decision();

-- Safe cleanup before the unique guard: history is retained via soft-delete.
with closed as (
  select b.id
    from public.briefings b
    join wn.leave_requests l
      on l.staff_code = wn.leave_notice_staff(b.message)
     and l.leave_date = wn.leave_notice_date(b.message)
     and lower(trim(coalesce(l.branch,''))) = lower(trim(coalesce(b.branch,'')))
   where lower(trim(coalesce(b.title,'')))='leave request'
     and coalesce(b."deletedAt",'')=''
     and lower(trim(coalesce(l.status,''))) <> 'pending'
)
update public.briefings b
   set "deletedAt"=to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
       "deletedBy"='SYSTEM-V1520-CLOSED',
       "updatedAt"=to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
 where b.id in (select id from closed);

with ranked as (
  select b.id,
         row_number() over (
           partition by lower(trim(coalesce(b.branch,''))), wn.leave_notice_staff(b.message), wn.leave_notice_date(b.message)
           order by b."createdAt" asc nulls last, b.id
         ) as rn
    from public.briefings b
   where lower(trim(coalesce(b.title,'')))='leave request'
     and coalesce(b."deletedAt",'')=''
     and wn.leave_notice_staff(b.message) is not null
     and wn.leave_notice_date(b.message) is not null
)
update public.briefings b
   set "deletedAt"=to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
       "deletedBy"='SYSTEM-V1520-DUPLICATE',
       "updatedAt"=to_char(clock_timestamp() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
 where b.id in (select id from ranked where rn>1);

create unique index if not exists briefings_one_active_leave_notice_uq
on public.briefings (
  (lower(trim(coalesce(branch,'')))),
  (wn.leave_notice_staff(message)),
  (wn.leave_notice_date(message))
)
where lower(trim(coalesce(title,'')))='leave request'
  and coalesce("deletedAt",'')=''
  and wn.leave_notice_staff(message) is not null
  and wn.leave_notice_date(message) is not null;

notify pgrst, 'reload schema';
