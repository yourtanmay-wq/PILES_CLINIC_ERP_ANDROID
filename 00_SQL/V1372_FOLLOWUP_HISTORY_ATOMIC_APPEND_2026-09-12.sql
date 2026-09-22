-- ═══════════════════════════════════════════════════════════════════════════
-- V1372 (১২.০৯.২০২৬) — TK-নির্দেশে গভীরে যাচাই (তালিকা ৪৬২-চ): দুই ফোনে
-- একসাথে একই রোগীর ফোন-কল/চেম্বার রিমার্ক লেখা হলে একজনের লেখা হারিয়ে
-- যাওয়ার ঝুঁকি — কারণ ফোন আগে পুরো history পড়ে, নিজের নতুন লাইনটা জুড়ে,
-- তারপর পুরো array-টাই ফিরিয়ে লেখে। দুটো ফোন কাছাকাছি সময়ে এটা করলে যেটা
-- পরে লেখে সেটাই জেতে, আগের ফোনের লাইনটা কোনো চিহ্ন না রেখেই হারিয়ে যায়।
--
-- এই ফাংশন একটাই নতুন history-এন্ট্রি **সরাসরি ডেটাবেসের ভিতরেই** জুড়ে দেয়
-- (single UPDATE, jsonb ||) — তাই দুটো ফোন কাছাকাছি সময়ে ডাকলেও PostgreSQL
-- নিজেই সারিটা একে একে লক করে, দুজনেরই এন্ট্রি শেষে ঠিকই জমা থাকে, কেউ
-- কারো লেখা মুছে দেয় না। ⛔ history ছাড়া আর কোনো ঘর এই ফাংশন ছোঁয় না —
-- lastRemark/callCount/lastCallDate ইত্যাদির পুরনো (ভালোভাবে যাচাই করা)
-- নিয়ম ফোনের কোডেই অক্ষত থাকে, শুধু history-টাই সরাসরি সার্ভারে জোড়া হবে।
-- ⛔ চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ═══════════════════════════════════════════════════════════════════════════

create or replace function public.tk_append_followup_history(p_id text, p_entry jsonb)
returns jsonb
language plpgsql
as $$
declare v_history jsonb;
begin
  if p_id is null or trim(p_id) = '' then
    raise exception 'p_id required';
  end if;
  update public.followups
  set history = coalesce(history, '[]'::jsonb) || jsonb_build_array(p_entry),
      "updatedAt" = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
  where id = p_id
  returning history into v_history;
  return v_history;
end;
$$;

grant execute on function public.tk_append_followup_history(text, jsonb) to anon, authenticated;
