package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 🟢🔒 V1144 (০৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — "Reminders" পর্দা।
 *
 * দুটো ঘর: **For me** (আমার কাছে এসেছে) ও **Sent by me** (আমি পাঠিয়েছি)।
 * নিজের ঘরে Accept ও Done, আর History চাপলে ওই রোগীর আগের সব রিমাইন্ডার।
 * পাঠানোর ঘরে অবস্থা — Sent → Seen → Accepted → Done, আর ২৪ ঘণ্টায় Accept
 * না হলে লাল "Not accepted yet"।
 *
 * ⛔ ExpectedTomorrowActivity-র প্রমাণিত ধাঁচেই পুরোটা **কোডে** বানানো — নতুন
 *    কোনো layout XML নেই, তাই resource ভাঙার ঝুঁকি নেই।
 * ⛔ পর্দা খুললে **একটাই** সরু পড়া হয় (যে ঘরটা দেখা হচ্ছে শুধু সেটার)।
 */
class ReminderActivity : AppCompatActivity() {

    private lateinit var listHolder: LinearLayout
    private lateinit var tabMine: TextView
    private lateinit var tabSent: TextView
    private lateinit var emptyView: TextView

    private var showingSent = false
    private var myCode: String = ""
    private val openHistory = HashSet<String>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = NativeSession.current(this)
        if (user == null) { finish(); return }
        myCode = user.name

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF3F8"))
        }

        // ── উপরের পট্টি: অ্যাপের নিজের ঢাল (TK: "নেভি ব্লু কালার থাকবে না")।
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_login_hero)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(TextView(this@ReminderActivity).apply {
                text = "←"
                textSize = 20f
                setTextColor(Color.WHITE)
                setPadding(dp(4), 0, dp(14), 0)
                setOnClickListener { finish() }
            })
            addView(TextView(this@ReminderActivity).apply {
                text = "Reminders"
                textSize = 19f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
        })

        // ── দুটো ঘর (tab)।
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E2E9F2"))
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(12), dp(12), 0) }
        }
        tabMine = makeTab("For me") { if (showingSent) { showingSent = false; paintTabs(); load() } }
        tabSent = makeTab("Sent by me") { if (!showingSent) { showingSent = true; paintTabs(); load() } }
        tabs.addView(tabMine); tabs.addView(tabSent)
        root.addView(tabs)

        // ── নতুন রিমাইন্ডার।
        root.addView(TextView(this).apply {
            text = "+  Add Reminder"
            textSize = 17f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0EA57A"))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(12), dp(12), 0) }
            setOnClickListener {
                startActivity(android.content.Intent(this@ReminderActivity, ReminderAddActivity::class.java))
            }
        })

        emptyView = TextView(this).apply {
            text = "Nothing here yet."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#8B97A6"))
            setPadding(dp(16), dp(28), dp(16), dp(28))
            visibility = android.view.View.GONE
        }

        listHolder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(20))
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(emptyView)
            addView(listHolder)
        }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(inner)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        })

        setContentView(root)
        paintTabs()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun makeTab(label: String, onTap: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(10), dp(10), dp(10), dp(10))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onTap() }
    }

    private fun paintTabs() {
        fun paint(t: TextView, on: Boolean) {
            t.setBackgroundColor(if (on) Color.WHITE else Color.TRANSPARENT)
            t.setTextColor(Color.parseColor(if (on) "#15213A" else "#6B7A8C"))
        }
        paint(tabMine, !showingSent)
        paint(tabSent, showingSent)
    }

    private fun ui(block: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { block() }
    }

    private fun load() {
        val sent = showingSent
        val code = myCode
        BackgroundWork.run {
            val rows = if (sent) ReminderRepository.sentByMe(code) else ReminderRepository.forMe(code)
            ui { render(rows, sent) }
        }
    }

    private fun render(rows: List<ReminderItem>, sent: Boolean) {
        listHolder.removeAllViews()
        if (rows.isEmpty()) {
            emptyView.visibility = android.view.View.VISIBLE
            return
        }
        emptyView.visibility = android.view.View.GONE
        val live = rows.filter { !ReminderModel.isDone(it) }
        val done = rows.filter { ReminderModel.isDone(it) }
        if (live.isNotEmpty()) {
            listHolder.addView(sectionLabel(if (sent) "WAITING" else "TODO"))
            live.forEach { listHolder.addView(card(it, sent)) }
        }
        if (done.isNotEmpty()) {
            listHolder.addView(sectionLabel("EARLIER"))
            done.forEach { listHolder.addView(card(it, sent)) }
        }
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#6B7A8C"))
        setPadding(dp(16), dp(14), dp(16), dp(6))
    }

    private fun card(item: ReminderItem, sent: Boolean): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(6), dp(12), dp(6)) }
            alpha = if (ReminderModel.isDone(item)) 0.65f else 1f
        }

        // ── উপরের সারি: চিহ্ন + নাম/ব্রাঞ্চ + ধরন ও তারিখ।
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@ReminderActivity).apply {
                text = ReminderModel.icon(item.type)
                textSize = 20f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor(ReminderModel.tint(item.type)))
                setPadding(dp(10), dp(8), dp(10), dp(8))
            })
            addView(LinearLayout(this@ReminderActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(dp(10), 0, 0, 0) }
                addView(TextView(this@ReminderActivity).apply {
                    text = item.patientName.ifBlank { item.patientMobile }
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor("#15213A"))
                })
                addView(TextView(this@ReminderActivity).apply {
                    val bits = ArrayList<String>()
                    if (item.branch.isNotBlank()) bits.add(item.branch)
                    if (item.type.isNotBlank()) bits.add(item.type)
                    if (item.remindOn.isNotBlank()) bits.add(FollowUpModel.displayDate(item.remindOn))
                    text = bits.joinToString(" · ")
                    textSize = 12.5f
                    setTextColor(Color.parseColor("#7B8899"))
                })
            })
        })

        if (item.disease.isNotBlank() || item.patientMobile.isNotBlank()) {
            card.addView(TextView(this).apply {
                val bits = ArrayList<String>()
                if (item.patientMobile.isNotBlank()) bits.add(item.patientMobile)
                if (item.disease.isNotBlank()) bits.add(item.disease)
                text = bits.joinToString(" · ")
                textSize = 12.5f
                setTextColor(Color.parseColor("#7B8899"))
                setPadding(0, dp(8), 0, 0)
            })
        }

        card.addView(TextView(this).apply {
            text = item.details
            textSize = 15f
            setTextColor(Color.parseColor("#28374A"))
            setPadding(0, dp(8), 0, 0)
        })

        card.addView(TextView(this).apply {
            text = if (sent) ("To " + item.toName.ifBlank { item.toCode })
                   else ("Sent by " + item.fromName.ifBlank { item.fromCode })
            textSize = 12f
            setTextColor(Color.parseColor("#8B97A6"))
            setPadding(0, dp(6), 0, 0)
        })

        if (sent) {
            card.addView(trail(item))
            if (ReminderModel.overdueForAccept(item, System.currentTimeMillis())) {
                card.addView(TextView(this).apply {
                    text = "Not accepted yet — 24 hours passed"
                    textSize = 13.5f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor("#C81E2B"))
                    setBackgroundColor(Color.parseColor("#FDECEC"))
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, dp(8), 0, 0) }
                })
            }
        } else {
            card.addView(actions(item))
            if (openHistory.contains(item.id)) card.addView(historyBox(item))
        }
        return card
    }

    /** Sent → Seen → Accepted → Done, প্রতিটার নিচে সময়। */
    private fun trail(item: ReminderItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        fun col(label: String, at: String, on: Boolean) {
            row.addView(LinearLayout(this@ReminderActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@ReminderActivity).apply {
                    text = label
                    textSize = 12.5f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (on) "#0E8F63" else "#B9C3CF"))
                })
                addView(TextView(this@ReminderActivity).apply {
                    text = if (on) ReminderModel.stamp(at) else "—"
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#8B97A6"))
                })
            })
        }
        col("Sent", item.sentAt, item.sentAt.isNotBlank())
        col("Seen", item.seenAt, item.seenAt.isNotBlank())
        col("Accepted", item.acceptedAt, item.acceptedAt.isNotBlank())
        col("Done", item.doneAt, item.doneAt.isNotBlank())
        return row
    }

    /** নিজের ঘরের বোতাম — Accept (একবার) · Done · History। */
    private fun actions(item: ReminderItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        fun button(label: String, bg: String, fg: String, onTap: () -> Unit) {
            row.addView(TextView(this@ReminderActivity).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(fg))
                setBackgroundColor(Color.parseColor(bg))
                setPadding(dp(8), dp(10), dp(8), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(dp(3), 0, dp(3), 0) }
                setOnClickListener { onTap() }
            })
        }
        if (item.status == ReminderModel.ST_SENT || item.status == ReminderModel.ST_SEEN) {
            button("Accept", "#0EA57A", "#FFFFFF") { advance(item, ReminderModel.ST_ACCEPTED) }
        }
        if (!ReminderModel.isDone(item)) {
            button("Done", "#12805F", "#FFFFFF") { advance(item, ReminderModel.ST_DONE) }
        }
        button("History", "#FFFFFF", "#3A4A5E") {
            if (openHistory.contains(item.id)) openHistory.remove(item.id) else openHistory.add(item.id)
            load()
        }
        return row
    }

    private fun advance(item: ReminderItem, to: String) {
        BackgroundWork.run {
            val ok = ReminderRepository.advance(item, to)
            ui {
                toast(if (ok) "Saved" else "Could not save — check internet")
                if (ok) load()
            }
        }
    }

    /** ওই রোগীর আগের রিমাইন্ডারগুলো — মেডিসিন/ট্রিটমেন্টের হিসেব একনজরে। */
    private fun historyBox(item: ReminderItem): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        box.addView(TextView(this).apply {
            text = "Loading history…"
            textSize = 13f
            setTextColor(Color.parseColor("#8B97A6"))
        })
        val mobile = item.patientMobile
        val skipId = item.id
        BackgroundWork.run {
            val rows = ReminderRepository.historyFor(mobile).filter { it.id != skipId }
            ui {
                box.removeAllViews()
                if (rows.isEmpty()) {
                    box.addView(TextView(this).apply {
                        text = "No earlier record for this patient."
                        textSize = 13f
                        setTextColor(Color.parseColor("#8B97A6"))
                    })
                    return@ui
                }
                rows.forEach { h ->
                    box.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(5), 0, dp(5))
                        addView(TextView(this@ReminderActivity).apply {
                            text = if (h.remindOn.isBlank()) "—" else FollowUpModel.displayDate(h.remindOn)
                            textSize = 13f
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(Color.parseColor("#7B8899"))
                            width = dp(96)
                        })
                        addView(TextView(this@ReminderActivity).apply {
                            text = h.type + " — " + h.details
                            textSize = 13f
                            setTextColor(Color.parseColor("#3A4A5E"))
                            layoutParams = LinearLayout.LayoutParams(
                                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                            )
                        })
                    })
                }
            }
        }
        return box
    }

    private fun toast(msg: String) {
        try {
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT)
                .also { try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }
                .show()
        } catch (_: Throwable) { }
    }
}
