-- শুধু দেখা (তালিকা ৪১৭): ফোন payments তালিকা "updatedAt নতুন→পুরনো" ক্রমে টানে; Supabase একবারে সর্বোচ্চ ১০০০ সারি দেয়।
-- Rehana-র ₹1,500 সারি ওই ১০০০-র ভিতরে না বাইরে — মেপে দেখা।
with kne as (
  select id, mobile, "updatedAt", row_number() over (order by "updatedAt" desc nulls last) as rnk
  from public.payments where branch = 'Kishanganj' or branch is null
), alls as (
  select id, mobile, "updatedAt", row_number() over (order by "updatedAt" desc nulls last) as rnk
  from public.payments
)
select 'kishanganj_scope_rows' as ki, count(*)::text as man from kne
union all
select 'rehana_1500_rank_in_kishanganj', rnk::text from kne where id = 'pay_38fcb701faea48759d885eaf3f3a0618'
union all
select 'khusbu_ranks_in_kishanganj', string_agg(rnk::text, ',' order by rnk) from kne where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = '8436214614'
union all
select 'kishanganj_rows_beyond_1000', count(*)::text from kne where rnk > 1000
union all
select 'all_rows', count(*)::text from alls
union all
select 'all_rows_beyond_1000', count(*)::text from alls where rnk > 1000
union all
select 'rehana_1500_rank_in_all', rnk::text from alls where id = 'pay_38fcb701faea48759d885eaf3f3a0618';
