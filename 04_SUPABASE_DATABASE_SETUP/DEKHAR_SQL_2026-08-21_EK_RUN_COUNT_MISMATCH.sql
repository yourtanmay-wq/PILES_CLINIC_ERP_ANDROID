select "কী দেখাচ্ছে", "ট্যাব / ব্রাঞ্চ", "কতগুলো" from (

  -- ১ · কোন ট্যাবে কোন ব্রাঞ্চের কতগুলো
  select 1 as ord,
    '1) ট্যাব অনুযায়ী ব্রাঞ্চ'                             as "কী দেখাচ্ছে",
    (case coalesce(trim("stage"),'')
       when 'Inquiry'   then 'Enquiry ট্যাব'
       when 'Patient'   then 'Visit ট্যাব'
       when 'Treatment' then 'Patient ট্যাব'
       else '(অন্য)' end)
      || '  →  ' || coalesce(nullif(trim("branch"),''),'** ব্রাঞ্চ ফাঁকা **') as "ট্যাব / ব্রাঞ্চ",
    count(*)                                              as "কতগুলো"
  from public.followups
  where coalesce(trim("stage"),'') in ('Inquiry','Patient','Treatment')
    and coalesce(trim("status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
  group by 1,2,3

  union all

  -- ২ · ব্রাঞ্চের ঘরে হুবহু কী লেখা আছে (বানান / বাড়তি ফাঁকা জায়গা ধরার জন্য)
  select 2,
    '2) ব্রাঞ্চে হুবহু যা লেখা',
    '[' || coalesce("branch",'(খালি)') || ']',
    count(*)
  from public.followups
  where coalesce(trim("stage"),'') in ('Inquiry','Patient','Treatment')
    and coalesce(trim("status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
  group by 1,2,3

  union all

  -- ৩ · ব্রাঞ্চ ফাঁকা সারিগুলোর আসল ব্রাঞ্চ (patients টেবিল মিলিয়ে)
  select 3,
    '3) ব্রাঞ্চ ফাঁকা — আসলে কার',
    coalesce(nullif(trim(p."branch"),''),'** patients-এও ফাঁকা **'),
    count(*)
  from public.followups f
  left join public.patients p
    on right(regexp_replace(coalesce(p."mobile",''),'\D','','g'),10)
     = right(regexp_replace(coalesce(f."mobile",''),'\D','','g'),10)
  where coalesce(trim(f."stage"),'') in ('Inquiry','Patient','Treatment')
    and coalesce(trim(f."status"),'') not in ('Cancelled','Incomplete','Rejected','Closed')
    and coalesce(trim(f."branch"),'') = ''
  group by 1,2,3

) x
order by ord, "কতগুলো" desc, "ট্যাব / ব্রাঞ্চ";
