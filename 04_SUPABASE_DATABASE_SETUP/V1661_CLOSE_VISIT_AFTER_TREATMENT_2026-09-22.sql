-- V1661 — 22 Sep 2026
-- PURPOSE: Once a real treatment payment/advance is recorded, the old Visit/Patient
-- follow-up row must not remain Active. This prevents treated patients from appearing
-- again in the Visit section.
-- Production migration already applied as: v1661_close_stale_visit_after_treatment

create table if not exists hr.visit_treatment_transition_audit (
  id uuid primary key default gen_random_uuid(),
  followup_id text not null,
  patient_row_id text not null,
  official_patient_id text not null default '',
  branch text not null default '',
  old_stage text not null default '',
  old_status text not null default '',
  old_next_follow text not null default '',
  payment_id text not null default '',
  source text not null,
  changed_at timestamptz not null default now()
);

alter table hr.visit_treatment_transition_audit enable row level security;
revoke all on hr.visit_treatment_transition_audit from anon, authenticated;

create or replace function public.tk_close_visit_after_treatment_payment()
returns trigger
language plpgsql
security definer
set search_path = public, hr, fin, pg_temp
as $$
declare
  v_patient public.patients%rowtype;
  v_now text := to_char(now() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
begin
  if not fin.rmp_is_treatment(new."payType", new.remarks) then return new; end if;
  if fin.rmp_safe_number(new.amount) <= 0 then return new; end if;
  if coalesce(btrim(new."patientId"),'') = '' then return new; end if;

  select p.* into v_patient
  from public.patients p
  where p.id = new."patientId" or p."patientId" = new."patientId"
  order by case when p.id = new."patientId" then 0 else 1 end
  limit 1;
  if not found then return new; end if;

  insert into hr.visit_treatment_transition_audit
    (followup_id,patient_row_id,official_patient_id,branch,old_stage,old_status,old_next_follow,payment_id,source)
  select f.id, v_patient.id, coalesce(v_patient."patientId",''), coalesce(v_patient.branch,''),
         coalesce(f.stage,''), coalesce(f.status,''), coalesce(f."nextFollow",''),
         coalesce(new.id,''), 'payment_trigger'
  from public.followups f
  where lower(coalesce(f.stage,''))='patient'
    and lower(coalesce(f.status,'active'))='active'
    and (f."refId" = v_patient.id or
         (coalesce(v_patient."patientId",'') <> '' and f."patientId" = v_patient."patientId"));

  update public.followups f
     set status='Closed',
         "nextFollow"='',
         "convertedPatientId"=case when coalesce(f."convertedPatientId",'')=''
                                   then coalesce(v_patient."patientId",'')
                                   else f."convertedPatientId" end,
         "updatedAt"=v_now
   where lower(coalesce(f.stage,''))='patient'
     and lower(coalesce(f.status,'active'))='active'
     and (f."refId" = v_patient.id or
          (coalesce(v_patient."patientId",'') <> '' and f."patientId" = v_patient."patientId"));
  return new;
end;
$$;

drop trigger if exists tk_close_visit_after_treatment_payment on public.payments;
create trigger tk_close_visit_after_treatment_payment
after insert or update of amount, "payType", "patientId", remarks
on public.payments for each row
execute function public.tk_close_visit_after_treatment_payment();

create or replace function public.tk_block_stale_visit_after_treatment()
returns trigger
language plpgsql
set search_path = public, fin, pg_temp
as $$
declare
  v_patient_row_id text;
  v_official_patient_id text;
begin
  if lower(coalesce(new.stage,'')) <> 'patient'
     or lower(coalesce(new.status,'active')) <> 'active' then return new; end if;

  select p.id, coalesce(p."patientId",'')
    into v_patient_row_id, v_official_patient_id
  from public.patients p
  where (coalesce(new."refId",'') <> '' and p.id = new."refId")
     or (coalesce(new."patientId",'') <> '' and p."patientId" = new."patientId")
  order by case when p.id = new."refId" then 0 else 1 end
  limit 1;
  if not found then return new; end if;

  if exists (
    select 1 from public.payments pay
    where (pay."patientId" = v_patient_row_id
           or (v_official_patient_id <> '' and pay."patientId" = v_official_patient_id))
      and fin.rmp_is_treatment(pay."payType", pay.remarks)
      and fin.rmp_safe_number(pay.amount) > 0
  ) then
    new.status := 'Closed';
    new."nextFollow" := '';
    if coalesce(new."convertedPatientId",'') = '' then
      new."convertedPatientId" := v_official_patient_id;
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists zz_tk_block_stale_visit_after_treatment on public.followups;
create trigger zz_tk_block_stale_visit_after_treatment
before insert or update of stage, status, "refId", "patientId"
on public.followups for each row
execute function public.tk_block_stale_visit_after_treatment();

notify pgrst, 'reload schema';
