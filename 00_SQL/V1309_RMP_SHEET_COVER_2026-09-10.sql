-- V1309 (তালিকা ৪১৮, TK: "৩ পাশ") — RMP COMMISSION SHEET: RMP-কে দেওয়া টাকা **কোন রোগীর জন্য** —
-- (ক) হাতে adjust করা থাকলে সেটা (rmp_advance_allocations), (খ) বাকিটা পুরনো বকেয়া আগে (FIFO) ধরে —
-- সব একটা ডাকে (রোগীপ্রতি আলাদা ডাক নয়, egress নেই)। ⛔ শুধু পড়া — কিছু লেখে/বদলায় না।
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
revoke all on function fin.rmp_sheet_cover(date, date, text) from public, anon;
grant execute on function fin.rmp_sheet_cover(date, date, text) to authenticated;
notify pgrst, 'reload schema';
-- যাচাই (শুধু পড়া): ফাংশন বসেছে কিনা
select proname, pg_get_function_identity_arguments(oid) as args from pg_proc where proname = 'rmp_sheet_cover';
