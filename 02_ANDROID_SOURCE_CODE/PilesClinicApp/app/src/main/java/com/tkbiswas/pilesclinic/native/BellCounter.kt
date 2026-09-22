package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * TK-REQUESTED (2026-07-27): "ঘন্টায় যখন কিছু আসে চুপিসারে কেন আসে, notification
 * sound কেন হয় না?"
 *
 * The bell number was counted inside DashboardActivity only, so nothing could
 * reuse it. The counting code is moved here WORD FOR WORD -- same tables, same
 * order, same "a failure just adds 0" behaviour -- so that the Dashboard and the
 * background reminder worker always show the exact same number and can never
 * drift apart.
 *
 * Nothing new is fetched: exactly the same cloud reads the Dashboard already did.
 */
object BellCounter {

    /** Unseen briefings + (Master only) pending payment approvals & missing visit fees. */
    fun count(ctx: Context, session: NativeUser): Int {
        var c = 0
        try {
            // 🚨 TK-REPORTED, LIVE (29.07.2026 — TK-এর ছবি: ঘন্টায় ৫, ভিতরে
            // ফাঁকা) · খাতার সারি B60।
            // আগে এখানে নিজের আলাদা হিসাব ছিল: শুধু `targetsHit` + "দেখা
            // হয়েছে" দেখা হত, কিন্তু **"আমার জন্য মোছা" নোটিশ বাদ দেওয়া হত
            // না**। তাই TK নিজের পর্দা থেকে নোটিশ মুছে দিলে সেগুলো তালিকায়
            // আর দেখাত না, অথচ ঘন্টা গুনেই যেত — আর দেখা না গেলে "দেখা
            // হয়েছে" চিহ্নও বসানো যেত না, তাই সংখ্যাটা চিরকাল আটকে থাকত।
            // (এই আটকে থাকা সংখ্যার কারণেই নোটিফিকেশনের শব্দও আসত না —
            // শব্দ কেবল সংখ্যা **বাড়লে** বাজে।)
            //
            // এখন গোনাটা **পর্দার সেই একই নিয়ম** থেকেই আসে
            // (`BriefingRepository.unseenCount` → `visibleForUser`), তাই
            // ঘন্টার সংখ্যা আর ভিতরের তালিকা কখনো আলাদা হতে পারবে না।
            // ⛔ ক্লাউডে অনুরোধের সংখ্যা আগের মতোই একটাই।
            val repo = BriefingRepository()
            c += repo.unseenCount(repo.fetchRawForCount(ctx), session)   // 🔵 V405: শুধু গোনা ⇒ সরু পড়া
        } catch (_: Exception) {}
        // 🔒 খাতার সারি B51 (TK, 28.07.2026): *"যদি অ্যাপ্লিকেশনে না ফেরে তাহলে
        // Dashboard-এ যে ঘন্টা আছে সেখানে অবশ্যই নোটিফিকেশন আসতে হবে।"*
        // কল করার পরে যাঁদের রিমার্ক লেখা বাকি, তাঁদের সংখ্যা ঘন্টায় যোগ হয় —
        // সব রোলের জন্যই, কারণ কল স্টাফরাই করেন। ⛔ ক্লাউডে কোনো নতুন
        // অনুরোধ নেই, গোনাটা ফোনের নিজের ঘর থেকেই আসে।
        try { c += PendingRemarkStore.count(ctx, session.mobile) } catch (_: Exception) {}
        // 🔒 খাতার সারি B193 (TK, 30.07.2026 রাত): "EXPECTED" — কল করার সময়
        // ডাক্তার যদি একটা তারিখ বলেন যে তার কাছে একটা পেশেন্ট আসতে পারে,
        // সেই তারিখ আজ হলে স্টাফের ঘন্টায় "আজ এই ডাক্তারকে কল করুন"
        // মনে করিয়ে দিতে হবে (TK-এর স্পষ্ট নির্দেশ)। ⛔ সব রোলের জন্যই
        // (কল স্টাফরাই করেন, Follow-up-এর PendingRemarkStore ঠিক যেমন
        // সব রোলের জন্য উপরে গোনা হয়)। ⛔ স্টাফ/ফিল্ড অফিসার শুধু নিজের
        // ব্রাঞ্চ দেখেন (প্রজেক্টের সব জায়গার একই ব্রাঞ্চ-আলাদা-করার নিয়ম),
        // Master সব ব্রাঞ্চ একসাথে। ⛔ ক্লাউডে একটাই সস্তা count-only অনুরোধ।
        try {
            val branchFilter = if (session.role == "master") null else session.branch
            c += DoctorVisitRepository().fetchExpectedTodayCount(branchFilter)
        } catch (_: Exception) {}
        // 🔒 খাতার সারি B206 (TK, 30.07.2026 রাত — "তারিখ নির্ধারণ করলে সেই
        // উক্ত তারিখে সেই ডাক্তারকে কল করতে হবে, স্টাফের ঘন্টায় নোটিফিকেশন
        // আসবে সাউন্ড সহ"): "Next Call Date" আজ হলে স্টাফের ঘন্টায় "আজ এই
        // ডাক্তারকে কল করুন" মনে করিয়ে দিতে হবে — ঠিক EXPECTED-এর মতোই
        // একই ব্রাঞ্চ-নিয়ম ও একই সস্তা count-only অনুরোধ। ⛔ সাউন্ডের জন্য
        // আলাদা কোনো নতুন কোড লাগেনি — ঘন্টার মোট সংখ্যা (c) বাড়লেই
        // BellNotifier (CallReminderWorker-এর ভিতরে, দিনে ৩ বার চলে)
        // এমনিতেই ফোনের স্বাভাবিক নোটিফিকেশন-সাউন্ডসহ জানিয়ে দেয় — এটা
        // আগে থেকেই থাকা একই ব্যবস্থা, শুধু এই নতুন সংখ্যাটা তার মধ্যে যোগ হলো।
        /* 🔕🔒 V970 (০২.০৯.২০২৬, TK-নির্দেশ) — *"Today RMP Call Due নোটিফিকেশন
           হিসাবে দেখানোর দরকার নেই"*। ঘন্টার সংখ্যাতেও আর গোনা হয় না — নইলে
           ঘন্টায় সংখ্যা দেখাত অথচ তালিকায় কিছুই থাকত না।
           ⛔ `fetchNextCallDueTodayCount()` মোছা হয়নি, শুধু আর ডাকা হয় না —
              তাই ফ্রি প্ল্যানে দিনে কয়েকটা পড়াও কমল। */
        // 🆕🔒 খাতার সারি — Dialer → Missed কল-ব্যাক বাকি (TK-নির্দেশ,
        // 05.08.2026)। সম্পূর্ণ স্থানীয় (এই ফোনের Call Log), ক্লাউডে
        // কিছু যায় না — তাই নতুন কোনো Supabase-অনুরোধ নেই।
        try { c += BranchSimHelper.countPendingMissedCallbacks(ctx) } catch (_: Exception) {}
        if (session.role == "master") {
            try { c += PaymentRepository(ctx).fetchPendingBackdateCount() } catch (_: Exception) {}
            try { c += PaymentRepository(ctx).fetchPendingEditCount() } catch (_: Exception) {}
            // 🟢 B628 (11.08.2026): Referral Income এডিট/ডিলিটের pending অনুরোধও মাস্টারের bell-এ গোনা হয়।
            try { c += DoctorVisitRepository().fetchPendingReferralEditCount() } catch (_: Exception) {}
            // 🟢🆕 V401 (16.08.2026, TK-নির্দেশ): Staff/Doctor-এর পুরনো তারিখের আয়-খরচের
            //     অনুরোধও মাস্টারের ঘণ্টায় গোনা হয় (ঠিক উপরের তিনটার মতোই)।
            try { c += com.tkbiswas.pilesclinic.modules.IeRequests.pendingCount() } catch (_: Exception) {}
            // 🔒🔒 খাতার সারি (29.07.2026 রাত, TK বারবার বলেছিলেন — দেখুন
            // MissingFeeSeenGuard.kt-এর মাথার নোট): আগে এখানে কাঁচা সংখ্যা
            // গোনা হত (`fetchMissingVisitFeePatients().size`), অথচ Briefing
            // পর্দা আগে "দেখা হয়ে গেছে" মার্ক করা নামগুলো বাদ দিয়ে দেখাত —
            // তাই ঘন্টায় সংখ্যা থাকত, পর্দা খুললে ফাঁকা। এখন দুটোই ঠিক
            // একই ফাংশন (MissingFeeSeenGuard.trulyPending) ডাকে, তাই আর
            // কখনো আলাদা হতে পারবে না।
            try {
                val repo = PaymentRepository(ctx)
                val all = repo.fetchMissingVisitFeePatients()
                // 🔴💸 V509 (২১.০৮.২০২৬ · Egress) — `fetchFeeMissingSeenKeys()`
                //   `activity_logs` থেকে ৫০০০ পর্যন্ত সারি নামায়, আর এটা
                //   **প্রতিবার ঘন্টা গোনার সময়** চলত (মাস্টারের ফোনে দিনে বহুবার)।
                //   ⛔ তালিকাটাই ফাঁকা হলে ঐ চিহ্নগুলোর কোনো দরকারই নেই —
                //     `trulyPending(ctx, emptyList, …)` সবসময় ফাঁকাই ফেরে।
                //   তাই এখন **ফাঁকা না হলে তবেই** পড়া হয়। ফলাফল এক অক্ষরও
                //   বদলায় না, শুধু অকারণ পড়াটা বাদ যায়।
                if (all.isNotEmpty()) {
                    val cloudSeen = try { repo.fetchFeeMissingSeenKeys() } catch (_: Exception) { emptySet<String>() }
                    c += MissingFeeSeenGuard.trulyPending(ctx, all, cloudSeen).size
                }
            } catch (_: Exception) {}
        }
        /* 📌🔒 V1193 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"উপরের ঘন্টাতে
           নোটিফিকেশন আসুক"*। ঘন্টায় দুটো জিনিস যোগ হলো —
             ① যেগুলো **এই ব্যক্তির জন্য অপেক্ষা করছে** (Accept বাকি)
             ② তাঁর **নিজের পাঠানো** যেগুলো Accept হয়েছে, এখনো দেখা হয়নি
           ⛔ গোনা ও তালিকা **একই ফাংশন** থেকেই আসে (`waitingFor` /
              `acceptedNoticesFor`) — NotificationsActivity-ও ঠিক এগুলোই ডাকে,
              তাই ঘন্টায় সংখ্যা আছে অথচ ভিতরে ফাঁকা — এটা কখনো হতে পারবে না।
           ⛔ নিজের পাঠানো "অপেক্ষমাণ" রিমাইন্ডার কখনো নিজের ঘন্টায় আসে না। */
        try { c += DoctorReminderRepository.waitingFor(session).size } catch (_: Exception) {}
        try { c += DoctorReminderRepository.acceptedNoticesFor(session).size } catch (_: Exception) {}
        // 🟢 B629 (11.08.2026, TK-নির্দেশ): স্যালারির তারিখ পেরিয়ে গেছে অথচ এ মাসে
        //   দেওয়া হয়নি — এমন স্টাফ থাকলে **Master ও Doctor দুজনের** ঘণ্টাতেই গোনা হয়
        //   (দুজনকেই মনে করাতে)। ছোট hr-টেবিল পড়ে হিসাব; ব্যর্থ হলে ০ (কিছু ভাঙে না)।
        if (session.role == "master" || session.displayRole == "doctor") {
            try { c += com.tkbiswas.pilesclinic.modules.SalaryReminder.dueCount(ctx) } catch (_: Exception) {}
        }
        return c
    }
}
