-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V1504-stage-ডায়াগনস্টিক (১৫.০৯.২০২৬) — টাকা মুছে যাওয়ার পর এই ৫ জনের
-- patients ও followups সারিতে এখন কী আছে দেখার জন্য (stage/bill ঠিক করার
-- আগে আসল অবস্থা যাচাই করা, আন্দাজে UPDATE না করা)। ⛔ শুধু পড়া।
-- ═══════════════════════════════════════════════════════════════════════════
select 'PATIENT' as tag, p.id, p.name, p.mobile, p.stage, p.bill, p."registrationDate", p."visitDate", p."date"
  from public.patients p
 where p.id in (
   'pat_b217ab7c7ff1481ea9b23999ccec1ab1', -- BABLI MISRA
   'pat_8eff3a010cee47e2a6f43564f43381d9', -- BIBHASH BARMON
   'pat_1d1ca86e57aa4ed58fc6065f17da2513', -- DHANANJOY ROY
   'pat_bee111b4f8df484bb0d63e4f68be477a', -- KAMAL ROY
   'pat_2fbbfbfda1e14421ad7c5e73326f937b'  -- SNIGDHYA DUTTA
 )
union all
select 'FOLLOWUP' as tag, f.id, f.name, f.mobile, f.stage, null, f."registrationDate", f."visitDate", f."date"
  from public.followups f
 where right(regexp_replace(coalesce(f.mobile,''),'\D','','g'),10) in (
   '8617763108','9932921397','9593295190','9933157070','9474628935'
 )
order by tag, name;
