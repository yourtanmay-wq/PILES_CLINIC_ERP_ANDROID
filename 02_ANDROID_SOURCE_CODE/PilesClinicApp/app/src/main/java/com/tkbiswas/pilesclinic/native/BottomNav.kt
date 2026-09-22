package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.TextView
import com.tkbiswas.pilesclinic.R

/**
 * Shared web-parity bottom navigation bar (bottom_nav_bar.xml).
 * Call BottomNav.wire(this) in onCreate of any screen that includes the bar.
 * Tabs are role-based, mirroring the WebView bottomNav():
 *   master/staff : Home · Enquiry · Follow Up · Registration · Menu
 *   doctor       : Home · Queue · Follow-up · Search · Menu
 *   field        : Home · Follow Up · Doctor Visit · Search · Menu
 */
object BottomNav {

    private data class Tab(val icon: String, val label: String, val target: Class<*>)


    /**
     * 🚨 TK-REPORTED (2026-07-27): a save that could not reach the cloud is
     * retried only when a screen that has the bottom bar is opened. Nine
     * screens do NOT have that bar . including Patient Details (View All),
     * Draft, Report Card and Collection . so a staff member could work in
     * those for a long time while a registration or a payment sat unsent,
     * with nothing trying again.
     *
     * The retry work is unchanged; it is only lifted out into its own
     * function so those screens can call it too. wire() still calls exactly
     * this, so every screen that already retried behaves identically.
     */
    /**
     * 🚨 খাতার সারি B145 (TK, 30.07.2026): *"প্রতিটি Screen খুললেই অনেক Retry
     * একসঙ্গে শুরু হয়।"* — সত্যি ছিল। প্রতিবার পর্দা খুললেই একটা **নতুন thread**
     * খুলত, কোনো তালা বা বিরতি ছাড়াই; দ্রুত দশটা পর্দা খুললে দশটা thread একসাথে
     * চলতে পারত (অকারণ নেট · ব্যাটারি · দুর্বল লাইনে অ্যাপ ধীর)।
     *
     * এখন দুটো পাহারা:
     *  ১. **তালা** — একটা দফা চলার সময় দ্বিতীয়টা শুরুই হয় না।
     *  ২. **২ মিনিটের বিরতি** — কিন্তু ⛔ **শুধু তখনই, যখন পাঠানোর মতো কিছুই
     *     নেই**। কিছু আটকে থাকলে বিরতি মানা হয় না, সঙ্গে সঙ্গে চেষ্টা হয় —
     *     নইলে TK-এর লক করা নিয়ম *"আমার ফোনে যা করলাম তা হারাবে না"* দুর্বল
     *     হয়ে যেত।
     * ⛔ পাঠানোর কাজ, তার ক্রম, কোনো নিয়ম — এক অক্ষরও বদলানো হয়নি।
     */
    // 🔒 খাতার সারি B169 (TK-এর ৬ নম্বর সন্দেহ, 30.07.2026): তালাটা আগে **শুধু
    // এখানেই** ছিল, তাই পিছনের দুটো worker আর "পাঠান" বোতাম একই সময়ে আলাদা
    // দফা চালাতে পারত। এখন চারটে জায়গাই **একটাই দরজা** (`SyncGate`) দিয়ে ঢোকে।
    // ⛔ ২ মিনিটের বিরতির নিয়মটা এখানেই আগের মতো রইল — ওটা শুধু এই পর্দা-খোলা
    //    পথের জন্য, অন্য তিনটের নিজের নিজের নিয়ম আছে।
    @Volatile private var lastRunAt = 0L
    private const val QUIET_GAP_MS = 2L * 60L * 1000L

    /** পাঠানোর মতো কিছু আছে কি? ⛔ পুরোটাই এই ফোনের ভিতরে দেখা হয় — ক্লাউডে
     *  একটাও অনুরোধ যায় না, তাই Supabase-এর কোটায় কোনো প্রভাব নেই। */
    private fun anythingWaiting(activity: Activity): Boolean {
        val a = try { PendingSyncStatus.summary(activity).total } catch (_: Throwable) { 0 }
        if (a > 0) return true
        return try { CloudWriteQueue.pendingCount(activity) > 0 } catch (_: Throwable) { false }
    }

    fun retryStuckSaves(activity: Activity) {
        // ⛔ মেইন থ্রেডে একটাও ফাইল পড়া হয় না — সবই নিচের thread-এর ভিতরে,
        //    নইলে পর্দা খোলার সময় এক পলকের জন্য আটকে যেতে পারত।
        Thread {
            // 🔴🔴 TK-ORDER (31.07.2026, Loading Speed Audit-এর প্রস্তাবিত
            // সমাধান — "ঝুঁকিহীনভাবে কাজটা করে শেষ করুন"): এই burst-এ ১১টা
            // repository flush আছে, যেগুলো স্ক্রিন খোলার ঠিক মুহূর্তেই শুরু
            // হয়ে যেত — সেই স্ক্রিনের **নিজের** ডেটা-লোডের সাথে একই দুর্বল
            // লাইনে ভাগ বসাত। তাই এখন এই burst শুরুর আগে **দেড় সেকেন্ড**
            // অপেক্ষা করে — এই সময়টায় স্ক্রিনের নিজের প্রথম fetch-টা এগিয়ে
            // যাওয়ার সুযোগ পায়। ⛔ কাজের ক্রম/নিয়ম/নিরাপত্তা-জাল (কী কী
            // flush হয়, কী ক্রমে, ২-মিনিটের বিরতি) এক অক্ষরও বদলায়নি —
            // শুধু পুরো burst-টা দেড় সেকেন্ড পরে শুরু হয়। দেড় সেকেন্ডে কোনো
            // pending কাজ হারায় না, শুধু কয়েক মুহূর্ত পরে পাঠানো হয়।
            try { Thread.sleep(1500L) } catch (_: InterruptedException) { }
            // 🔒 খাতার সারি B169: একজন ভিতরে থাকলে দ্বিতীয় দফা শুরুই হয় না।
            SyncGate.tryRun {
            // ⛔ আগের নিয়ম হুবহু: যে দফা সত্যিই চলেনি, সে বিরতির ঘড়িটা
            //    পিছিয়ে দেবে না (নইলে বারবার পর্দা খুললে ঘড়িটা সরতেই থাকত)।
            var didRun = false
            try {
                val now = System.currentTimeMillis()
                // ২ মিনিটের বিরতি — শুধু তখনই, যখন পাঠানোর মতো কিছুই নেই।
                if (lastRunAt > 0L && now - lastRunAt < QUIET_GAP_MS &&
                    !anythingWaiting(activity)
                ) return@tryRun
                didRun = true
            // 🚨 TK'S ORDER (2026-07-28): the safety net first -- anything that
            // failed to reach the cloud earlier (a medicine sale, a bill
            // correction, an approval, a Trash restore...) is sent again the
            // moment the app is used, before the older queues run.
            try { CloudWriteQueue.attach(activity) } catch (_: Throwable) { }
            // 🔒 খাতার সারি B138 (TK, 29.07.2026 রাত ১০.২০): *"ডিলিট করলে যেন
            // সেটা আর ফিরে না আসে।"* — অন্য ফোনে যা যা ডিলিট হয়েছে তার
            // তালিকাটা এখানে একবার নামিয়ে নেওয়া হয়, যাতে এই ফোনের কোনো
            // অপেক্ষমাণ পুরনো কপি সেগুলোকে ক্লাউডে ফেরত পাঠাতে না পারে।
            // ⛔ **১ ঘণ্টায়** একবারের বেশি নয় (খাতার সারি B145, 30.07.2026 — আগে
            //    ছিল ৬ ঘণ্টা) · মাত্র একটা ঘর নামে · ব্যর্থ হলে
            //    চুপচাপ ফিরে যায়, অ্যাপের কিছুই বদলায় না।
            try { DeletedGuard.syncFromCloud(activity) } catch (_: Throwable) { }
            try { CloudWriteQueue.flush(activity) } catch (_: Throwable) { }
            try { EnquiryRepository(activity).flushPending() } catch (_: Throwable) { }
            try { RegistrationRepository(activity).flushPending() } catch (_: Throwable) { }
            // TK APPROVED (2026-07-16): Advance/Treatment Payment now has
            // the SAME retry-queue as Enquiry/Registration above (see
            // PaymentRepository.kt) -- same reasoning, same fix.
            try { PaymentRepository(activity).flushPending() } catch (_: Throwable) { }
            // TK-REPORTED BUG FIX (2026-07-16): Follow-up's own remark/
            // status/next-follow-date updates (updateRemark, updateStatus,
            // updateNextFollow, logEnquiryCall, resetCallCount) had this
            // SAME silent-failure problem, with no retry at all before now
            // -- see FollowUpRepository.kt. Follow-up is the most-used
            // screen in the app, so this is likely the single biggest
            // contributor to "I updated it but no one else sees it".
            try { FollowUpRepository(activity).flushPending() } catch (_: Throwable) { }
            // TK-REQUESTED ADDITION (2026-07-18): Chamber Attendance's
            // "Mark Arrived" now has the SAME retry-queue pattern as
            // everything else above (see ChamberAttendanceRepository.kt) --
            // same reasoning, same fix.
            try { ChamberAttendanceRepository.flushPending(activity) } catch (_: Throwable) { }
            // TK-REQUESTED ADDITION (2026-07-18): Doctor Checkup/Prescription/
            // Diet Chart/Investigation now have the same retry-queue pattern
            // (see ClinicalCloudRepository.kt) -- same reasoning, same fix.
            try { com.tkbiswas.pilesclinic.clinical.ClinicalCloudRepository.flushPending(activity) } catch (_: Throwable) { }
            // TK-REQUESTED ADDITION (2026-07-18): Briefing posts now retry
            // too (see BriefingRepository.kt) -- same reasoning, same fix.
            try { BriefingRepository().flushPending(activity) } catch (_: Throwable) { }
            // TK-REQUESTED ADDITION (2026-07-22): several smaller edit
            // dialogs (Bill correction, Paid/Estimated Amount, payment
            // amount/mode edits, follow-up remark edits) previously had no
            // retry at all -- same reasoning, same fix, see
            // GenericUpdateQueue.kt. Does not change any UI/toast text.
            try { GenericUpdateQueue.flushPending(activity) } catch (_: Throwable) { }
            // 🚨🚨 খাতার সারি B170 (TK-এর ৭ নম্বর সন্দেহ): পর্দা খোলার এই দফাতেও
            // Chamber Close-এর অপেক্ষমাণ কাজ পাঠানো হত না — এখন হয়।
            try { ChamberCloseRepository.flushPending(activity) } catch (_: Throwable) { }
            // 🔴🔒 V1338 (১১.০৯.২০২৬, TK-নির্দেশ, JPE-CRP ১৭-বনাম-৪ কল-গোনার
            // সমস্যা) — Call বোতাম চাপার সময় ব্যর্থ হয়ে যাওয়া App Call লগ
            // (call_taps) এখানেও বাকি সবার মতোই আবার চেষ্টা হয়।
            try { com.tkbiswas.pilesclinic.modules.ModuleAuth.flushPendingCallTaps(activity) } catch (_: Throwable) { }
            } finally {
                // ⛔ যা-ই ঘটুক দরজা খুলে যাবে (`SyncGate`-এর নিজের `finally`),
                //    নইলে একবার আটকে গেলে পাঠানো চিরকাল বন্ধ হয়ে যেত।
                if (didRun) lastRunAt = System.currentTimeMillis()
            }
            }
        }.start()
    }

    fun wire(activity: Activity) {
        // TK-REPORTED BUG FIX (2026-07-16): ROOT CAUSE of "Enquiry/
        // Registration doesn't show up for other branches/staff/Master".
        // EnquiryRepository.save() and RegistrationRepository.save() save
        // to THIS device instantly (by design, TK-approved, so staff never
        // waits on the network to see their own save succeed), then push
        // to Supabase with a SINGLE background attempt and return success
        // regardless of whether that attempt actually reached the cloud.
        // If that one attempt fails (weak signal, screen closed mid-
        // upload, app backgrounded, etc.), the record was stuck in this
        // device's local-only queue FOREVER -- invisible to every other
        // staff/branch/Master -- because nothing else in the app retried
        // it, except reopening that exact same Enquiry/Registration screen
        // again on that exact same device. Retrying here instead -- once
        // per screen open, from EVERY screen in the app (this function
        // already runs on every screen's onCreate) -- means a stuck record
        // gets pushed again the very next time this staff opens anything,
        // not just if they happen to revisit the original form. Nothing
        // about what gets saved, or the instant-local-save behavior,
        // changes -- this only adds more frequent retries of the SAME
        // existing, already-safe flushPending() functions.
        retryStuckSaves(activity)

        val role = NativeSession.current(activity)?.role ?: "staff"
        val tabs = when (role) {
            "doctor" -> listOf(
                Tab("🏠", "Home", DashboardActivity::class.java),
                Tab("🩺", "Queue", DoctorQueueActivity::class.java),
                Tab("🔁", "Follow Up", FollowUpActivity::class.java),
                Tab("🔍", "Search", GlobalSearchActivity::class.java),
                Tab("☰", "Menu", MoreMenuActivity::class.java)
            )
            "field" -> listOf(
                Tab("🏠", "Home", DashboardActivity::class.java),
                Tab("🔁", "Follow Up", FollowUpActivity::class.java),
                Tab("👨‍⚕️", "Doctor Visit", DoctorVisitActivity::class.java),
                Tab("🔍", "Search", GlobalSearchActivity::class.java),
                Tab("☰", "Menu", MoreMenuActivity::class.java)
            )
            else -> listOf(
                Tab("🏠", "Home", DashboardActivity::class.java),
                Tab("📝", "Enquiry", EnquiryActivity::class.java),
                Tab("🔁", "Follow Up", FollowUpActivity::class.java),
                Tab("🧾", "Registration", RegistrationActivity::class.java),
                Tab("☰", "Menu", MoreMenuActivity::class.java)
            )
        }
        val slots = listOf(R.id.navHome, R.id.navEnquiry, R.id.navFollow, R.id.navReg, R.id.navMenu)
        slots.forEachIndexed { i, id ->
            val v = activity.findViewById<LinearLayout?>(id) ?: return@forEachIndexed
            val tab = tabs[i]
            (v.getChildAt(0) as? TextView)?.text = tab.icon
            (v.getChildAt(1) as? TextView)?.text = tab.label
            v.setOnClickListener {
                if (activity.javaClass != tab.target) activity.startActivity(Intent(activity, tab.target))
            }
        }
        // TK-REQUESTED (2026-07-20): the bottom bar must not appear on ANY
        // screen. Belt-and-suspenders with the layout's own visibility="gone":
        // hide the bar's root (parent of navHome) here too, in case an
        // <include> ever ignores the included root's visibility. Runs on every
        // screen (wire is called everywhere); null-safe, so no crash. The
        // pending-sync queue drain above is unaffected.
        (activity.findViewById<LinearLayout?>(R.id.navHome)?.parent as? android.view.View)?.visibility = android.view.View.GONE
    }
}
