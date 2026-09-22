package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * ⏰🔒 V990 (০৩.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
 * **MY UNEXPECTED ENQUIRIES — স্টাফ নিজেই দেখবেন কে কোন ধাপে।**
 *
 * TK-এর কথা: *"তারা যদি নাই জানতে পারে যে সেই পেশেন্টটা ট্রিটমেন্ট চালু
 * করেছে কিনা, তাহলে তারা হিসাবটা পাবে কি করে"*।
 *
 * ⛔ উপরের লাল লাইনে **কল কখন এসেছিল** — TK-এর কথায় এটাই টাকার একমাত্র শর্ত
 *    (*"1.02 PM এটা তো আনএক্সপেক্টেড টাইম নয়"*)। শুধু অসময়ের এনকোয়ারিই
 *    এই তালিকায় আসে।
 * ⛔ টাকার নিয়ম নতুন করে বানানো হয়নি — ডেটাবেসের চালু নিয়মই দেখানো হয়
 *    (ভিজিট ₹১০০ · চিকিৎসা শুরু হলে আরও ₹৪০০ ⇒ ₹৫০০)।
 * ⛔ একটাও সারি লেখা হয় না — শুধু পড়া।
 */
class UnexpectedEnquiryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOBILE = "staff_mobile"
        const val EXTRA_NAME = "staff_name"
    }

    private lateinit var listBox: LinearLayout
    private lateinit var sumBar: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun box(fill: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fill)); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val who = intent?.getStringExtra(EXTRA_MOBILE).orEmpty()
            .ifBlank { NativeSession.current(this)?.mobile.orEmpty() }
        val whoName = intent?.getStringExtra(EXTRA_NAME).orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF3F1"))
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#123E8C"))
            setPadding(dp(14), dp(13), dp(16), dp(13))
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = "◀"; textSize = 16f
                setTextColor(Color.WHITE); setPadding(dp(2), 0, dp(12), 0)
                setOnClickListener { finish() }
            })
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = if (whoName.isBlank()) "MY UNEXPECTED ENQUIRIES"
                       else whoName.uppercase() + " · UNEXPECTED"
                textSize = 14.5f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
        })
        sumBar = TextView(this).apply {
            text = "Loading…"
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0B4F2A"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        root.addView(sumBar)
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            addView(listBox)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        })
        setContentView(root)

        Thread {
            val rows = try { UnexpectedIncentive.forStaff(who) } catch (_: Throwable) { emptyList() }
            runOnUiThread { if (!isFinishing && !isDestroyed) render(rows) }
        }.start()
    }

    private fun render(rows: List<UnexpectedIncentive.Row>) {
        listBox.removeAllViews()
        val total = UnexpectedIncentive.thisMonthTotal(rows)
        sumBar.text = "THIS MONTH · EARNED        " + UnexpectedIncentive.money(total)
        if (rows.isEmpty()) {
            sumBar.visibility = View.GONE
            listBox.addView(TextView(this).apply {
                text = "No unexpected-time enquiry found yet."
                textSize = 13f
                setTextColor(Color.parseColor("#7A8794"))
                setPadding(dp(18), dp(20), dp(18), dp(20))
            })
            return
        }
        for (r in rows) listBox.addView(cardFor(r))
    }

    /* 👆 V1029 — কার্ডে চাপ দিলে ওই রোগীর পুরো ইতিহাস।
       ⛔ `GlobalSearchActivity.openTimeline()`-এর হুবহু একই ডাক (একই package)। */
    private fun openTimeline(v: View, mobile: String) {
        val d = mobile.filter { it.isDigit() }.takeLast(10)
        if (d.length != 10) return
        try {
            val ctx: android.content.Context = v.context
            val i = Intent(ctx, PatientTimelineActivity::class.java)
            i.putExtra("mobile", d)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (_: Throwable) { }
    }

    private fun cardFor(r: UnexpectedIncentive.Row): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#FFFFFF", 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(10), dp(8), dp(10), 0) }
        }
        // নাম · মোবাইল · ব্রাঞ্চ
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = r.name; textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#16232E"))
            })
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = "  " + r.mobile; textSize = 12f
                setTextColor(Color.parseColor("#1667D8"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = r.branch; textSize = 11.5f
                setTextColor(Color.parseColor("#7A8794"))
            })
        })
        // ⏰ কল কখন এসেছিল — এটাই টাকার শর্ত
        card.addView(TextView(this).apply {
            text = "⏰ Call: " + UnexpectedIncentive.dateTime(r.callAt) + "  ·  UNEXPECTED"
            textSize = 11.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#8A1810"))
            setPadding(0, dp(7), 0, 0)
        })
        // এখন কোন ধাপে
        val (line, ink, fill) = when (r.stage) {
            "treatment" -> Triple(
                "✓ Treatment started  ·  " + UnexpectedIncentive.dateTime(r.stageAt),
                "#0B5B2F", "#EAF7F0")
            "visit" -> Triple(
                "⌛ Visit given  ·  " + UnexpectedIncentive.dateTime(r.stageAt),
                "#8A5A00", "#FFF6E6")
            else -> Triple("— Not come to the branch yet", "#5B6B81", "#F3F5F7")
        }
        /* 👆🔒 V1029 (০৩.০৯.২০২৬, TK-রিপোর্ট: *"এখানে চাপ দিলেও কিছু কাজই হয় না"*)
           — সত্যিই কার্ডে চাপার কোনো ব্যবস্থাই ছিল না। এখন চাপলে ওই রোগীর পুরো
           ইতিহাস (Patient Timeline) খোলে, তাই স্টাফ দেখেই বুঝতে পারেন কেন এখনো
           টাকা হয়নি বা কোন ধাপে আটকে আছে।
           ⛔ টাকার কোনো অঙ্ক এখান থেকে বদলায় না — শুধু দেখা। */
        card.isClickable = true
        card.isFocusable = true
        card.isFocusable = true
        card.setOnClickListener { v -> openTimeline(v, r.mobile) }
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = box(fill, 8)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = line; textSize = 12f
                setTextColor(Color.parseColor(ink))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@UnexpectedEnquiryActivity).apply {
                text = UnexpectedIncentive.money(r.earned); textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(ink))
            })
        })
        return card
    }
}
