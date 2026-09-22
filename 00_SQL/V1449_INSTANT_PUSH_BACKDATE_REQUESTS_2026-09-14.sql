-- ═══════════════════════════════════════════════════════════════════════════
-- V1449 (১৪.০৯.২০২৬, TK-নির্দেশ, খাতার সারি ৪৪০) — Backdate Payment/Refund-এর
-- নতুন অনুরোধ তৈরি হলে Master-এর ফোনে তাৎক্ষণিক শব্দ-নোটিফিকেশন যেত না।
--
-- আসল কারণ: V1298-এ বসানো তাৎক্ষণিক-push ব্যবস্থা (device_tokens + pg_net trigger)
-- শুধু `public.briefings` টেবিলে নতুন সারি ঢুকলেই চালু হয়। কিন্তু Backdate Payment/
-- Refund-এর অনুরোধ কখনো `briefings`-এ কোনো সারি বসায় না — শুধু `payment_backdate_
-- requests`-এ বসে (PaymentModel.buildBackdateRequestRow); তাই সেই trigger কখনো
-- চালুই হত না, আর Master শুধু ~১০ মিনিট পরপর চলা ব্যাকগ্রাউন্ড-চেকে (BriefingReminder
-- Worker) দেরিতে জানতে পারতেন।
--
-- সমাধান: একই, অপরিবর্তিত push-ফাংশন (public.tk_push_new_briefing — গোপন শব্দ
-- সহ, এখানে ছোঁয়া হয়নি) এখন `payment_backdate_requests`-এও AFTER INSERT বসানো
-- হলো। Netlify function (notify-push.mjs, V1449) নিজে থেকেই এই সারিটা চিনে নেয়
-- (এতে `targets` কলাম নেই, `briefings`-এ থাকে) — শুধু role="master" ডিভাইসেই পাঠায়,
-- কোনো `briefings` সারি তৈরি হয় না, তাই নোটিশ-বোর্ডে কিছুই দুবার দেখাবে না
-- (TK-নির্দেশ: "একবারই ইস্যু ঠিক করে দিন")।
--
-- ⛔ payment_backdate_requests টেবিলের কোনো কলাম/WHERE/অনুমতি বদলায়নি — শুধু
-- একটা নতুন trigger যোগ হলো। Approve/Reject workflow (BriefingActivity) এক
-- অক্ষরও বদলায়নি।
-- ═══════════════════════════════════════════════════════════════════════════

drop trigger if exists tk_push_new_backdate_request on public.payment_backdate_requests;
create trigger tk_push_new_backdate_request after insert on public.payment_backdate_requests
for each row execute function public.tk_push_new_briefing();

select 'trigger' as ki, count(*)::text as man from pg_trigger where tgname = 'tk_push_new_backdate_request';
