package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🆕 (06.08.2026, TK-অনুমোদনে — খাতার সারি "ঘন্টায় সংখ্যা আছে কিন্তু ভিতরে ফাঁকা")
 *
 * **কেন বানানো হলো:** Dashboard-এর ঘন্টা (`BellCounter.count`) পাঁচ-ছয় রকম
 * জিনিস একসাথে গোনে — অদেখা নোটিশ, রিমার্ক বাকি, আজ ডাক্তার কল/EXPECTED,
 * মিসড কল-ব্যাক, আর (Master-only) পেমেন্ট-অনুমোদন। কিন্তু ঘন্টা চাপলে আগে
 * সরাসরি Briefing পাতা খুলত, যেখানে শুধু নোটিশ আর Master-এর অনুমোদন
 * দেখানোর ব্যবস্থা ছিল — বাকিগুলো (রিমার্ক/ডাক্তার-কল/মিসড-কল) দেখানোর
 * কোনো জায়গাই ছিল না। তাই TK-এর স্ক্রিনে ঘন্টায় সংখ্যা থাকলেও পাতা ফাঁকা লাগত।
 *
 * **এখন:** ঘন্টা চাপলে এই পাতা খোলে, যেটা সবগুলো এক জায়গায় তালিকা করে
 * দেখায়। প্রতিটা লাইনে চাপলে আসল পাতায় যায় (এই পাতা `finish()` করা হয় না,
 * তাই ব্যাক চাপলে TK আবার এই তালিকাতেই ফেরেন — TK-এর স্পষ্ট নির্দেশ)।
 *
 * ⛔ **কোনো পুরনো গণনা বদলানো হয়নি** — এই পাতা `BellCounter.count()`-এর
 *    ভিতরে যে ফাংশনগুলো আগে থেকেই আছে (বা তাদের হুবহু "তালিকা" সংস্করণ,
 *    যেমন `fetchExpectedTodayList` — নতুন, কিন্তু `fetchExpectedTodayCount`-এর
 *    সাথে একদম একই filter) সেগুলোই আবার ডাকে, তাই ঘন্টার সংখ্যা আর এই
 *    পাতার তালিকা কখনো আলাদা হতে পারবে না।
 * ⛔ **Master-only পেমেন্ট-অনুমোদন এখানে আলাদা করে তৈরি করা হয়নি** — সেগুলো
 *    আগে থেকেই BriefingActivity-এর ভিতরে ঠিকঠাক দেখানো হয়, তাই "নোটিশ"
 *    লাইনে চাপলে সেই একই Briefing পাতা খোলে যেখানে সবকিছু আছে।
 */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var user: NativeUser
    private lateinit var sectionsContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var tvEmpty: TextView
    private lateinit var progressLoad: ProgressBar

    /* 🟢🔒 V410 (TK-নির্দেশ, ১৭.০৮.২০২৬) — **লম্বা তালিকা গুটিয়ে রাখা।**
       TK-এর কথা: আজ ১৩৮ জন, ২২ অগাস্ট ৩০৮ জন — এতগুলো নাম একসাথে দেখালে
       স্টাফ তালিকাটাই এড়িয়ে যান, তখন ঘণ্টার মানেই নষ্ট হয়।
       ⇒ ১০ জনের বেশি হলে **এক লাইনে** দেখাবে ("138 doctors to call today"),
         চাপলে পুরো তালিকা খুলবে, আবার চাপলে গুটিয়ে যাবে।
       ⛔ গোনা এক অক্ষরও বদলায়নি — উপরের "Notifications (138)" আগের মতোই।
       ⛔ খোলা/গোটানোর সময় **ক্লাউড থেকে নতুন করে কিছু আনা হয় না** (মেমরিতে
          রাখা তালিকাই আবার আঁকা হয়) — তাই Egress-এ এক বাইটও বাড়ে না। */
    private val COLLAPSE_OVER = 10
    private var expandCallList = false
    private var expandExpectedList = false

    private var lastNotices: Int = 0
    private var lastRemarks: List<PendingRemark> = emptyList()
    private var lastExpected: List<DoctorVisitItem> = emptyList()
    private var lastCallDue: List<DoctorVisitItem> = emptyList()
    private var lastMissed: List<BranchSimHelper.CallLogRow> = emptyList()
    // 📌 V1193 — Doctor Reminder (অপেক্ষমাণ ও "Accepted" খবর)
    private var lastRemWaiting: List<org.json.JSONObject> = emptyList()
    private var lastRemAccepted: List<org.json.JSONObject> = emptyList()

    /** মেমরিতে রাখা একই তালিকা দিয়ে আবার আঁকে — নতুন কোনো ডাউনলোড নয়। */
    private fun renderStored() {
        render(lastNotices, lastRemarks, lastExpected, lastCallDue, lastMissed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        UppercaseInputUtil.applyToAll(findViewById(android.R.id.content))

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        // 🆕🔒 B486 (06.08.2026) — 📜 সরাসরি Briefing খোলে, ➕ Briefing-এর
        // ভিতরের প্রমাণিত Post Notice ডায়ালগ সরাসরি খোলে (extra
        // "openCompose")। দুটোই আগে-থেকে-থাকা BriefingActivity পুনর্ব্যবহার।
        findViewById<TextView>(R.id.btnBrowseNotices).setOnClickListener {
            startActivity(Intent(this, BriefingActivity::class.java))
        }
        findViewById<TextView>(R.id.btnAddNotice).setOnClickListener {
            startActivity(Intent(this, BriefingActivity::class.java).putExtra("openCompose", true))
        }
        sectionsContainer = findViewById(R.id.sectionsContainer)
        scrollView = findViewById(R.id.scrollView)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressLoad = findViewById(R.id.progressLoad)

        loadAll()
    }

    // পাতায় ফিরলে (কোনো একটা লাইন থেকে কাজ সেরে ব্যাক করলে) তালিকা নতুন করে আনে,
    // যাতে যেটার কাজ হয়ে গেছে সেটা আর দেখাবে না -- Follow-up/Doctor Visit-এর
    // onResume রিফ্রেশের একই ধাঁচ।
    override fun onResume() {
        super.onResume()
        loadAll()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun loadAll() {
        progressLoad.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val branchFilter = if (user.role == "master") null else user.branch

            val unseenNotices = try {
                withContext(Dispatchers.IO) {
                    val repo = BriefingRepository()
                    repo.unseenCount(repo.fetchRawForCount(this@NotificationsActivity), user)   // 🔵 V405: শুধু গোনা ⇒ সরু পড়া
                }
            } catch (_: Throwable) { 0 }

            val pendingRemarks = try {
                withContext(Dispatchers.IO) { PendingRemarkStore.list(this@NotificationsActivity, user.mobile) }
            } catch (_: Throwable) { emptyList() }

            val expectedToday = try {
                withContext(Dispatchers.IO) { DoctorVisitRepository().fetchExpectedTodayList(branchFilter) }
            } catch (_: Throwable) { emptyList() }

            /* 🔕🔒 V970 (০২.০৯.২০২৬, TK-নির্দেশ) — *"Today RMP Call Due
               নোটিফিকেশন হিসাবে দেখানোর দরকার নেই, শুধুমাত্র RMP সেকশন খুললে
               সেখানে দেখাক"*। তাই এখানে ক্লাউডে **অনুরোধই যায় না** (ফ্রি
               প্ল্যানে একটা পড়া কমল), তালিকাও খালি।
               ⛔ `DoctorVisitRepository.fetchNextCallDueTodayList()` মোছা হয়নি —
                  RMP পর্দা ওটাই ব্যবহার করে। */
            val callDueToday = emptyList<DoctorVisitItem>()

            val missedCallbacks = try {
                withContext(Dispatchers.IO) { BranchSimHelper.pendingMissedCallbackNumbers(this@NotificationsActivity) }
            } catch (_: Throwable) { emptyList() }

            /* 📌🔒 V1193 (TK-নির্দেশ): *"উপরের ঘন্টাতে নোটিফিকেশন আসুক"*।
               ⛔ ঠিক সেই দুটো ফাংশনই ডাকা হয় যেগুলো `BellCounter` গোনে,
                  তাই ঘন্টার সংখ্যা আর এই তালিকা কখনো আলাদা হতে পারে না। */
            lastRemWaiting = try {
                withContext(Dispatchers.IO) { DoctorReminderRepository.waitingFor(user) }
            } catch (_: Throwable) { emptyList() }
            lastRemAccepted = try {
                withContext(Dispatchers.IO) { DoctorReminderRepository.acceptedNoticesFor(user) }
            } catch (_: Throwable) { emptyList() }

            progressLoad.visibility = View.GONE
            render(unseenNotices, pendingRemarks, expectedToday, callDueToday, missedCallbacks)
        }
    }

    private fun render(
        unseenNotices: Int,
        pendingRemarks: List<PendingRemark>,
        expectedToday: List<DoctorVisitItem>,
        callDueToday: List<DoctorVisitItem>,
        missedCallbacks: List<BranchSimHelper.CallLogRow>
    ) {
        // 🟢 V410: এই তালিকাটাই মেমরিতে রাখা হয়, যাতে "খুলুন/গুটিয়ে নিন" চাপলে
        //    ক্লাউডে আবার না যেতে হয়।
        lastNotices = unseenNotices
        lastRemarks = pendingRemarks
        lastExpected = expectedToday
        lastCallDue = callDueToday
        lastMissed = missedCallbacks

        sectionsContainer.removeAllViews()
        val totalCount = unseenNotices + pendingRemarks.size + expectedToday.size + callDueToday.size +
            missedCallbacks.size + lastRemWaiting.size + lastRemAccepted.size

        findViewById<TextView>(R.id.tvHeaderTitle).text =
            if (totalCount > 0) "Notifications ($totalCount)" else "Notifications"

        if (totalCount == 0) {
            tvEmpty.text = "No pending items"
            tvEmpty.visibility = View.VISIBLE
            scrollView.visibility = View.GONE
            return
        }
        tvEmpty.visibility = View.GONE
        scrollView.visibility = View.VISIBLE

        /* 📌🔒 V1193 — সবচেয়ে উপরে, কারণ এগুলোতে কারো কাজ আটকে থাকে। */
        if (lastRemWaiting.isNotEmpty()) {
            addSectionHeader("📌 Doctor Reminder", "#0F766E")
            for (r in lastRemWaiting) {
                val nm = r.optString("patientName", "").ifBlank { "Patient" }
                val mb = r.optString("patientMobile", "")
                val by = r.optString("byName", "")
                val rd = r.optString("remindDate", "")
                val rt = r.optString("remindTime", "")
                val dis = r.optString("disease", "")   // 🩺 V1194
                addRow(
                    icon = "📌",
                    iconColor = "#0F766E",
                    title = nm + (if (mb.isNotBlank()) "   $mb" else "") + (if (dis.isNotBlank()) "   ·   $dis" else ""),
                    subtitle = r.optString("note", "") +
                        (if (by.isNotBlank()) "  ·  By $by" else "") +
                        (if (rd.isNotBlank()) "  ·  " + dmy(rd) + (if (rt.isNotBlank()) " · " + time12(rt) else "") else "")
                ) { startActivity(Intent(this, DoctorReminderActivity::class.java)) }
            }
        }

        /* ✅ V1193 — পাঠানো ব্যক্তির ঘন্টায় "Accept হয়েছে" খবরটা। চাপলে
           পর্দা খোলে আর খবরটা দেখা-হয়েছে হিসেবে চিহ্নিত হয় (আর আসে না);
           ⛔ History-তে সারিটা চিরকাল থাকে, কিছুই মোছে না। */
        if (lastRemAccepted.isNotEmpty()) {
            addSectionHeader("✅ Reminder Accepted", "#0A7C3F")
            for (r in lastRemAccepted) {
                val nm = r.optString("patientName", "").ifBlank { "Patient" }
                val mb = r.optString("patientMobile", "")
                addRow(
                    icon = "✅",
                    iconColor = "#0A7C3F",
                    title = nm + (if (mb.isNotBlank()) "   $mb" else "") +
                        (if (r.optString("disease", "").isNotBlank()) "   ·   " + r.optString("disease", "") else ""),
                    subtitle = "Accepted by " + r.optString("acceptedByName", "") + "  ·  " + stamp(r.optString("acceptedAt", ""))
                ) {
                    lifecycleScope.launch {
                        try { withContext(Dispatchers.IO) { DoctorReminderRepository.ack(r, user) } } catch (_: Throwable) { }
                        startActivity(Intent(this@NotificationsActivity, DoctorReminderActivity::class.java))
                    }
                }
            }
        }

        if (unseenNotices > 0) {
            addSectionHeader("🔔 Notices", "#3b82f6")
            addRow(
                icon = "📋",
                iconColor = "#3b82f6",
                title = if (unseenNotices == 1) "1 new notice" else "$unseenNotices new notices",
                subtitle = "Tap to view"
            ) { startActivity(Intent(this, BriefingActivity::class.java)) }
        }

        if (pendingRemarks.isNotEmpty()) {
            addSectionHeader("📝 Remark Pending", "#F59E0B")
            for (p in pendingRemarks) {
                addRow(
                    icon = "📝",
                    iconColor = "#F59E0B",
                    title = p.name.ifBlank { "UNKNOWN" },
                    subtitle = "${p.mobile} · Remark Pending after the call"
                ) {
                    startActivity(
                        Intent(this, FollowUpActivity::class.java)
                            .putExtra("remarkMobile", p.mobile)
                    )
                }
            }
        }

        /* 🔕 V970 (TK-নির্দেশ) — "📞 Call Doctor Today" সেকশনটা এখান থেকে
           তুলে দেওয়া হলো; ওটা এখন শুধু RMP পর্দায়। ⛔ উপরের `callDueToday`
           সবসময় খালি, তাই ঘন্টার সংখ্যাতেও আর গোনা হয় না। */

        if (expectedToday.isNotEmpty()) {
            addSectionHeader("🧑\u200d⚕️ Patient Expected Today", "#6941C6")
            // 🟢 V410: এখানেও একই নিয়ম — বেশি হলে এক লাইনে।
            if (expectedToday.size > COLLAPSE_OVER && !expandExpectedList) {
                addRow(
                    icon = "🧑\u200d⚕️",
                    iconColor = "#6941C6",
                    title = "${expectedToday.size} doctors expecting a patient today",
                    subtitle = "Tap to open the full list"
                ) { expandExpectedList = true; renderStored() }
            } else {
                for (d in expectedToday) {
                    addRow(
                        icon = "🧑\u200d⚕️",
                        iconColor = "#6941C6",
                        title = d.name.ifBlank { "UNKNOWN" },
                        subtitle = "${d.mobile} \u00b7 Expected patient today"
                    ) {
                        startActivity(
                            Intent(this, DoctorVisitActivity::class.java)
                                .putExtra("searchMobile", d.mobile)
                                .putExtra("searchBranch", d.branch)
                        )
                    }
                }
                if (expectedToday.size > COLLAPSE_OVER) {
                    addRow(
                        icon = "\u25b2",
                        iconColor = "#6b7280",
                        title = "Hide this list",
                        subtitle = "Show it as one line again"
                    ) { expandExpectedList = false; renderStored() }
                }
            }
        }

        if (missedCallbacks.isNotEmpty()) {
            addSectionHeader("☎️ Callback Pending", "#D92D20")
            for (m in missedCallbacks) {
                addRow(
                    icon = "↙",
                    iconColor = "#D92D20",
                    title = m.number,
                    subtitle = "Missed call, callback not done yet"
                ) {
                    startActivity(
                        Intent(this, DialerActivity::class.java)
                            .putExtra("openTab", "missed")
                    )
                }
            }
        }
    }

    private fun dmy(iso: String): String = try {
        val p = iso.take(10).split("-"); p[2] + "/" + p[1] + "/" + p[0]
    } catch (_: Throwable) { iso }

    private fun time12(hm: String): String {
        val p = hm.trim().split(":")
        if (p.size < 2) return hm
        val h = p[0].toIntOrNull() ?: return hm
        val ap = if (h >= 12) "PM" else "AM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return "$h12.${p[1]} $ap"
    }

    /** "2026-09-07T18:42:03Z" → "07/09/2026 · 6.42 PM"। চেনা না গেলে যা আছে তাই। */
    private fun stamp(raw: String): String {
        val t = raw.trim()
        if (t.length < 10) return ""
        val d = dmy(t)
        if (t.length < 16) return d
        return try {
            val hh = t.substring(11, 13).toInt()
            val mm = t.substring(14, 16)
            val ap = if (hh >= 12) "PM" else "AM"
            val h12 = when { hh == 0 -> 12; hh > 12 -> hh - 12; else -> hh }
            "$d  ·  $h12.$mm $ap"
        } catch (_: Throwable) { d }
    }

    private fun addSectionHeader(text: String, colorHex: String) {
        sectionsContainer.addView(TextView(this).apply {
            this.text = text
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(colorHex))
            setPadding(dp(16), dp(14), dp(16), dp(4))
        })
    }

    private fun addRow(icon: String, iconColor: String, title: String, subtitle: String, onClick: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(this@NotificationsActivity, outValue.resourceId)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(12), dp(4), dp(12), dp(0))
            }
        }

        row.addView(TextView(this).apply {
            text = icon
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor(iconColor))
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(12) }
        })

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        content.addView(TextView(this).apply {
            text = title
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#10223A"))
        })
        content.addView(TextView(this).apply {
            text = subtitle
            textSize = 11.5f
            setTextColor(Color.parseColor("#5b6b81"))
            setPadding(0, dp(2), 0, 0)
        })
        row.addView(content)

        row.addView(TextView(this).apply {
            text = "›"
            textSize = 18f
            setTextColor(Color.parseColor("#9ca3af"))
        })

        sectionsContainer.addView(row)
    }
}
