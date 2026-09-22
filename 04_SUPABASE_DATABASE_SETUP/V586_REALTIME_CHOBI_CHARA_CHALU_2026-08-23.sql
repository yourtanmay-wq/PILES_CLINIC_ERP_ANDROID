-- ═══════════════════════════════════════════════════════════════════════
-- V586 (২৩.০৮.২০২৬) — Realtime চালু, কিন্তু **ছবি বাদ দিয়ে**।
--
-- TK-এর সিদ্ধান্ত: *"ছবি বাদ দিয়ে চালু"*।
--
-- কী হবে: কেউ কিছু সেভ করলে **শুধু সেই একটা সারিই** সবার ব্রাউজারে পৌঁছাবে।
-- এখন প্রতিবার পুরো টেবিল আবার নামে — সেটাই egress-এর সবচেয়ে বড় খরচ।
-- ওয়েবের কোড আগে থেকেই তৈরি (`wireRealtime()`, B623) — সে ওই এক সারিটাই
-- ক্যাশে বসায়, নতুন কোনো ডাউনলোড করে না।
--
-- ⛔ **ছবি কেন বাদ:** ছবি ডেটাবেসে base64 লেখা হিসেবে থাকে (~৫৫–১২০ KB)।
--    ছবিসহ পাঠালে একজনের একটা এডিটে সবার ব্রাউজারে ছবিটা যেত — তাতে খরচ
--    উল্টো বাড়ত। তাই `patients.photo` · `followups.photo` · `medical.photos`
--    প্রকাশনার তালিকা থেকে বাদ।
-- ⛔ **জমা থাকা ছবি মুছবে না** — ওয়েবের `mergeById()` ঘর-ধরে জোড়ে
--    (`{...a,...r}`), তাই ছবির ঘরটা না এলে আগের ছবিটাই থেকে যায়
--    (app.js:591 ও 1051-এ আগে থেকেই লেখা আছে)।
-- ⛔ **REPLICA IDENTITY ছোঁয়া হয়নি** — ইচ্ছে করে। FULL করলে প্রতিটা
--    আপডেট/ডিলিটে **পুরনো গোটা সারিটাও (ছবিসহ)** যেত। ডিফল্টে শুধু `id` যায়,
--    আর ওয়েবের কোড ডিলিটে শুধু `payload.old.id`-ই পড়ে — তাই এটাই সঠিক।
-- ⛔ `trash` ইচ্ছে করে বাদ — ওটা শুধু দরকারে নামানো হয় (V509), স্ট্রিম হয় না।
--
-- ⚠️ পরে যদি কোনো টেবিলে **নতুন কলাম** যোগ হয়, সেটা নিজে থেকে এই তালিকায়
--    ঢুকবে না — তখন এই SQL আবার একবার চালালেই হবে (যতবার খুশি চালানো যায়,
--    কিছু নষ্ট হয় না)।
--
-- Supabase → SQL Editor-এ পুরোটা কপি করে Run করুন। শেষে একটা তালিকা দেখাবে।
-- ═══════════════════════════════════════════════════════════════════════

-- প্রকাশনাটা না থাকলে তৈরি (Supabase-এ সাধারণত আগে থেকেই থাকে)
do $$
begin
  if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    create publication supabase_realtime;
  end if;
end $$;

do $$
declare
  t     text;
  cols  text;
  excl  text[];
  tabs  text[] := array['enquiries','patients','payments','followups','medical',
                        'products','doctor_visits','briefings','address_tags'];
begin
  foreach t in array tabs loop
    -- টেবিলটা সত্যিই আছে কি না (না থাকলে চুপচাপ বাদ)
    if to_regclass('public.' || quote_ident(t)) is null then
      continue;
    end if;

    -- ছবি-বহনকারী ঘর — শুধু এগুলোই বাদ যাবে
    excl := case t
              when 'patients'  then array['photo']
              when 'followups' then array['photo']
              when 'medical'   then array['photos']
              else array[]::text[]
            end;

    -- বাকি সব ঘর, টেবিলের নিজের ক্রমেই (নাম আন্দাজে লেখা হয়নি —
    -- ডেটাবেস থেকেই পড়া, তাই কোনো ঘরের নাম ভুল হওয়ার পথ নেই)
    select string_agg(quote_ident(column_name), ', ' order by ordinal_position)
      into cols
      from information_schema.columns
     where table_schema = 'public'
       and table_name   = t
       and not (column_name = any(excl));

    if cols is null then
      continue;
    end if;

    -- আগে থেকে থাকলে আগে সরিয়ে নিই, নইলে কলাম-তালিকা বসানো যায় না
    if exists (select 1 from pg_publication_tables
                where pubname = 'supabase_realtime'
                  and schemaname = 'public'
                  and tablename = t) then
      execute format('alter publication supabase_realtime drop table public.%I', t);
    end if;

    execute format('alter publication supabase_realtime add table public.%I (%s)', t, cols);
  end loop;
end $$;

-- ── যাচাই · কোন টেবিলে কী চালু হলো, আর ছবি সত্যিই বাদ গেছে কি না ──
select
  pt.tablename                                      as "টেবিল",
  cardinality(pt.attnames)                          as "কতগুলো ঘর যাবে",
  case when 'photo'  = any(pt.attnames::text[])
         or 'photos' = any(pt.attnames::text[])
       then 'হ্যাঁ ⚠️' else 'না ✅' end              as "ছবি যাচ্ছে?"
from pg_publication_tables pt
where pt.pubname = 'supabase_realtime' and pt.schemaname = 'public'
order by pt.tablename;
