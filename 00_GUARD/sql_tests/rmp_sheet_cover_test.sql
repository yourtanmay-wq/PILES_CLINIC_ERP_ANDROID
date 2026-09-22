-- 📒 V1309 — fin.rmp_sheet_cover-এর নমুনা-পরীক্ষা (নকল DB): ২ রোগী, ২ RMP-পেমেন্ট ⇒ FIFO 1000+500, তারপর 1500 · অগাস্টে ০
-- চালানো: python3 00_GUARD/sql_local_check.py 00_GUARD/sql_tests/rmp_sheet_cover_test.sql  (ফল: cover-সারি ৩টা, all-branches 3, aug 0)
begin;
create or replace function fin.rmp_sheet_cover(p_from date, p_to date, p_branch text default null)
returns table(advance_id uuid, patient_row_id text, patient_name text, patient_mobile text, amount numeric, kind text)
language plpgsql security definer set search_path = fin, public, hr as $$
declare r record; a record; c record; v_rem numeric; v_take numeric; v_due numeric;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  -- (ক) হাতে adjust করা (আসল, জমা আছে)
  return query
    select al.advance_id, pc.patient_row_id, pc.patient_name, pc.patient_mobile, al.amount, 'allocated'::text
    from fin.rmp_advance_allocations al
    join fin.rmp_advance_payments ap on ap.id = al.advance_id
    join fin.rmp_patient_commissions pc on pc.id = al.patient_commission_id
    where ap.paid_on between p_from and p_to
      and (p_branch is null or p_branch = '' or ap.branch = p_branch);
  -- (খ) বাকি টাকা: ওই RMP-র রোগীদের বকেয়া (earned − paid) পুরনো আগে
  create temp table if not exists _tk_cov(seq serial, pcid uuid, prow text, pname text, pmob text, remaining numeric) on commit drop;
  for r in select distinct ap.rmp_id from fin.rmp_advance_payments ap
           where ap.paid_on between p_from and p_to
             and (p_branch is null or p_branch = '' or ap.branch = p_branch)
  loop
    truncate _tk_cov;
    for c in select pc.id, pc.patient_row_id, pc.patient_name, pc.patient_mobile
             from fin.rmp_patient_commissions pc where pc.rmp_id = r.rmp_id
             order by pc.set_on, pc.id
    loop
      select greatest(coalesce(s.earned,0) - coalesce(s.paid,0), 0) into v_due from fin.rmp_summary(c.patient_row_id) s;
      if v_due > 0 then
        insert into _tk_cov(pcid, prow, pname, pmob, remaining) values (c.id, c.patient_row_id, c.patient_name, c.patient_mobile, v_due);
      end if;
    end loop;
    -- ওই RMP-র সব RMP-পেমেন্ট (তারিখ ধরে পুরনো আগে) — আগের মাসেরটা আগের রোগীদের ঢাকে, তারপর এই সীমার
    for a in select ap.id, ap.paid_on, ap.branch,
                    greatest(ap.amount - ap.allocated_amount - ap.legacy_covered_amount, 0) as unalloc,
                    (ap.paid_on between p_from and p_to and (p_branch is null or p_branch = '' or ap.branch = p_branch)) as in_range
             from fin.rmp_advance_payments ap where ap.rmp_id = r.rmp_id
             order by ap.paid_on, ap.recorded_at, ap.id
    loop
      v_rem := a.unalloc;
      for c in select * from _tk_cov where remaining > 0 order by seq loop
        exit when v_rem <= 0;
        v_take := least(v_rem, c.remaining);
        update _tk_cov set remaining = remaining - v_take where seq = c.seq;
        v_rem := v_rem - v_take;
        if a.in_range then
          advance_id := a.id; patient_row_id := c.prow; patient_name := c.pname; patient_mobile := c.pmob;
          amount := round(v_take, 2); kind := 'fifo';
          return next;
        end if;
      end loop;
    end loop;
  end loop;
  drop table if exists _tk_cov;
  return;
end $$;
insert into public.doctor_visits(id,name,branch,mobile) values ('rmp1','PKB','Cooch Behar','9000000001');
insert into public.patients(id,"patientId",name,mobile,branch,bill,discount,"registrationDate","createdAt") values
 ('p1','COB-01082026-001','RATAN DAS','+919100000001','Cooch Behar','10000','0','2026-08-01','2026-08-01T10:00:00Z'),
 ('p2','COB-15082026-001','SUMI KHATUN','+919100000002','Cooch Behar','20000','0','2026-08-15','2026-08-15T10:00:00Z');
insert into public.payments(id,"patientId",mobile,branch,amount,"payType",date,"createdAt") values
 ('pay1','p1','+919100000001','Cooch Behar','10000','treatment','2026-08-05','2026-08-05T10:00:00Z'),
 ('pay2','p2','+919100000002','Cooch Behar','20000','treatment','2026-08-20','2026-08-20T10:00:00Z');
insert into fin.rmp_patient_commissions(patient_row_id,patient_code,patient_name,patient_mobile,treatment_branch,rmp_id,rmp_name,commission_mode,commission_value,set_on,set_by,use_rmp_default) values
 ('p1','COB-01082026-001','RATAN DAS','9100000001','Cooch Behar','rmp1','PKB','AMOUNT',1000,'2026-08-01','TEST',false),
 ('p2','COB-15082026-001','SUMI KHATUN','9100000002','Cooch Behar','rmp1','PKB','PERCENT',10,'2026-08-15','TEST',false);
select 'due p1', due from fin.rmp_summary('p1'); select 'due p2', due from fin.rmp_summary('p2');
select fin.rmp_record_advance('rmp1',1500,'2026-09-04','CASH',null);
select fin.rmp_record_advance('rmp1',2000,'2026-09-07','CASH',null);
select 'cover', ap.paid_on, c.patient_name, c.amount, c.kind from fin.rmp_sheet_cover('2026-09-01','2026-09-30','Cooch Behar') c join fin.rmp_advance_payments ap on ap.id=c.advance_id order by ap.paid_on, c.patient_name;
select 'cover-all-branches', count(*) from fin.rmp_sheet_cover('2026-09-01','2026-09-30',null);
select 'cover-aug(empty)', count(*) from fin.rmp_sheet_cover('2026-08-01','2026-08-31','Cooch Behar');
rollback;
