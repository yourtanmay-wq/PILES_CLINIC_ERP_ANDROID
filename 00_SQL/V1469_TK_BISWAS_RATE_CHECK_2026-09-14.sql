-- V1469 (১৪.০৯.২০২৬) — শুধুমাত্র পড়া। ফালাকাটার "TK BISWAS"
-- (dv_8ad730969a2542f2bef8e26b97c6cbc7)-এর জন্য ব্রাঞ্চ-ভিত্তিক হার
-- সিস্টেমে আদৌ জমা আছে কিনা — V1468 আটকে গেছে ("rate not found"),
-- তাই আন্দাজে না বসিয়ে আগে সরাসরি দেখে নেওয়া।

select 'branch-specific' as source, rmp_id, branch, commission_mode, commission_value, updated_by, updated_at
from fin.rmp_commission_branch_defaults
where rmp_id = 'dv_8ad730969a2542f2bef8e26b97c6cbc7'
union all
select 'global default' as source, rmp_id, null as branch, commission_mode, commission_value, updated_by, updated_at
from fin.rmp_commission_defaults
where rmp_id = 'dv_8ad730969a2542f2bef8e26b97c6cbc7';
