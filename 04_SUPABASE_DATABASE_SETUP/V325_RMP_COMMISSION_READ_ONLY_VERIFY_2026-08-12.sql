-- V325 RMP Commission — READ-ONLY verification. This file changes no data.

-- 1) All five isolated tables must appear and have RLS + FORCE RLS enabled.
select n.nspname as schema_name, c.relname as table_name,
       c.relrowsecurity as rls_enabled, c.relforcerowsecurity as force_rls
from pg_class c join pg_namespace n on n.oid=c.relnamespace
where n.nspname='fin' and c.relname in (
  'rmp_commission_defaults','rmp_patient_commissions','rmp_commission_payments',
  'rmp_commission_requests','rmp_commission_audit')
order by c.relname;

-- 2) Expected RLS policies. No INSERT/UPDATE/DELETE table policy should exist.
select schemaname,tablename,policyname,cmd,roles,qual
from pg_policies
where schemaname='fin' and tablename like 'rmp_%'
order by tablename,policyname;

-- 3) Anonymous must have no table privilege.
select table_name,privilege_type
from information_schema.role_table_grants
where table_schema='fin' and table_name like 'rmp_%' and grantee in ('anon','PUBLIC')
order by table_name,privilege_type;

-- 4) Owner-provided formula proofs (expected: 2500, 500, 2400, 0, 400).
select
  round(least(27000::numeric,25000)*10/100,2) as percent_cap_2500,
  round(3000::numeric*least(5000::numeric,30000)/30000,2) as amount_partial_500,
  round(3000::numeric*least(8000::numeric,10000)/10000,2) as amount_refund_2400,
  greatest(500::numeric-900,0) as due_zero,
  greatest(900::numeric-500,0) as more_paid_400;

-- 4b) Discount proof: Bill 30,000 - Discount 5,000 = Final Bill 25,000.
select greatest(0,30000::numeric-5000::numeric) as final_bill_after_discount_25000;

-- 5) Treatment classification proofs (expected: true, false, false, false).
select
  fin.rmp_is_treatment('treatment','Advance') as treatment_true,
  fin.rmp_is_treatment('registration','') as registration_false,
  fin.rmp_is_treatment('visitfee','Visit Fee') as visit_false,
  fin.rmp_is_treatment('medicine','') as medicine_false;

-- 6) Linked Expense category/name is fixed in the protected function source.
select p.proname, pg_get_functiondef(p.oid) like '%RMP Commission Payment%' as expense_name_present
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='fin' and p.proname='rmp_record_payment';

-- 7) Free-plan lookup indexes must all appear.
select schemaname,tablename,indexname
from pg_indexes
where indexname in (
  'rmp_public_payments_patient_idx','rmp_pay_rmp_idx','rmp_req_pending_idx'
)
order by indexname;

-- 8) Authenticated has the API doorway; RLS still decides every visible row.
select has_schema_privilege('authenticated','fin','USAGE') as authenticated_fin_usage;
