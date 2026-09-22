-- আজকের ইমপোর্টে পাওয়া তারিখ-টাইপো সংশোধন (প্রতিটা কিস্তির ক্রম মিলিয়ে যাচাই করে)
-- এগুলো ইতিমধ্যে লাইভ ডেটাবেসে বসে গেছে, তাই UPDATE দিয়ে ঠিক করা হচ্ছে।

-- ১. BISWAJIT MAHATO (কিশানগঞ্জ, নন-বিলড ব্যাচ) -- সিটে "23.4.55" লেখা ছিল,
--    ২০৫৫ সাল ভুলবশত বসে গিয়েছিল, আসল তারিখ ২৩.৪.২০২৫।
--    patientId-এর সিরিয়াল নম্বর নিজে থেকে হিসাব করে বসানো হচ্ছে (KNE-23042025-তে যতগুলো আগে থেকে আছে, তার পরেরটা)।
update public.patients
set "patientId" = 'KNE-23042025-' || lpad((
      (select count(*) from public.patients where "patientId" like 'KNE-23042025-%') + 1
    )::text, 3, '0'),
    date = '2025-04-23', "registrationDate" = '2025-04-23', "visitDate" = '2025-04-23',
    "createdAt" = '2025-04-23T10:00:00.000Z', "updatedAt" = '2025-04-23T10:00:00.000Z'
where id = 'histkne25nb_6f3b388611756871' and mobile = '8016527704' and name = 'BISWAJIT MAHATO';

-- ২-৪. জলপাইগুড়ির পেমেন্ট-তারিখ টাইপো (বছর ভুল বসেছিল, কিস্তির ক্রম দিয়ে মিলিয়ে দেখা):
update public.payments set date = '2025-06-21', "createdAt" = '2025-06-21T10:15:00.000Z', "updatedAt" = '2025-06-21T10:15:00.000Z'
where id = 'histjpe25_pay_3b90d3e71e46dd2f'; -- JOYDIP MANDAL, Advance: 2057→2025
update public.payments set date = '2025-08-19', "createdAt" = '2025-08-19T10:20:00.000Z', "updatedAt" = '2025-08-19T10:20:00.000Z'
where id = 'histjpe25_pay_5b9a33b6f1991c46'; -- SANTASH RAM BHAGAT, 6th: 2028→2025
update public.payments set date = '2025-10-04', "createdAt" = '2025-10-04T10:18:00.000Z', "updatedAt" = '2025-10-04T10:18:00.000Z'
where id = 'histjpe25_pay_d3e5b8942739ba94'; -- PURNADEB BISWAS, 4th: 2002→2025

-- ৫-৯. কোচবিহারের পেমেন্ট-তারিখ টাইপো (বছর ভুল বসেছিল, কিস্তির ক্রম দিয়ে মিলিয়ে দেখা):
update public.payments set date = '2025-02-28', "createdAt" = '2025-02-28T10:16:00.000Z', "updatedAt" = '2025-02-28T10:16:00.000Z'
where id = 'histcob_pay_bf72ffc791a961b6'; -- SOUMEN SARKAR, 2nd: 2028→2025
update public.payments set date = '2025-05-09', "createdAt" = '2025-05-09T10:17:00.000Z', "updatedAt" = '2025-05-09T10:17:00.000Z'
where id = 'histcob_pay_177c05613ec813c0'; -- UTTAM DEY, 3rd: 2035→2025
update public.payments set date = '2025-05-12', "createdAt" = '2025-05-12T10:21:00.000Z', "updatedAt" = '2025-05-12T10:21:00.000Z'
where id = 'histcob_pay_740b8b01cd7cffda'; -- BASANTI ROY, 7th: 2015→2025
update public.payments set date = '2025-08-02', "createdAt" = '2025-08-02T10:17:00.000Z', "updatedAt" = '2025-08-02T10:17:00.000Z'
where id = 'histcob_pay_a6504fbb05485341'; -- NOSMINA KHATUN, 3rd: 2028→2025
update public.payments set date = '2025-08-08', "createdAt" = '2025-08-08T10:18:00.000Z', "updatedAt" = '2025-08-08T10:18:00.000Z'
where id = 'histcob_pay_67d40569ac510aac'; -- NOSMINA KHATUN, 4th: 2028→2025
update public.payments set date = '2026-01-12', "createdAt" = '2026-01-12T10:19:00.000Z', "updatedAt" = '2026-01-12T10:19:00.000Z'
where id = 'histcob_pay_285225a9847debfe'; -- BADAL SAHA, 6th: 2027→2026
