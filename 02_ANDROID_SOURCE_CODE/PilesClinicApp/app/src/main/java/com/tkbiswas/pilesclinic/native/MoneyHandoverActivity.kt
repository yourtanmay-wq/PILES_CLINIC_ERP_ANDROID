package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * 💰🔒 V984 (০২.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
 * **MONEY HANDOVER — কোন দিনের টাকা কে বুঝে নিলেন, তার পুরো ইতিহাস।**
 *
 * TK-এর কথা:
 *  • *"চেম্বার বন্ধ করার history — কবে কোন চেম্বার বন্ধ করে মাস্টারকে বুঝিয়ে
 *    দেয়া হয়েছে, টাকার পরিমাণ, ডাক্তারকে — সেটাই বা স্টাফ কি করে বুঝতে পারবে"*
 *  • *"সেদিন টাকাটা স্টাফের কাছে থেকে গেল… পরে স্টাফ যেন ডাক্তারকে বা
 *    মাস্টারকে টাকাটা বুঝে দিতে পারে তার ব্যবস্থা"*
 *  • *"কে রিসিভ করল তার নাম এবং তারিখ এবং সময় এক লাইনে রাখুন"* — আর তারিখ
 *    উপরে একবারই (*"তারিখ দুই জায়গায় কেন"*)।
 *
 * ⛔ কোনো টাকা তৈরি বা বদল হয় না — শুধু কে বুঝে নিলেন সেটা লেখা ও দেখা।
 * ⛔ যিনি নিচ্ছেন তিনি নিজের পাসওয়ার্ড দিলে তবেই "বুঝে নেওয়া" ধরা হয়।
 */
class MoneyHandoverActivity : AppCompatActivity() {

    private lateinit var listBox: LinearLayout
    private lateinit var sumCard: LinearLayout
    private lateinit var sumMoney: TextView
    private lateinit var sumDays: TextView
    private var historyMode = false        // ⋮ V1196 — বুঝে নেওয়া দিনগুলো
    private var days: List<MoneyHandover.Day> = emptyList()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun box(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor(stroke))
        cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF3F1"))
        }
        /* 🎨🔒 V1196 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — প্রফেশনাল সাজ।
           ⛔ টাকার কোনো হিসাব · পাসওয়ার্ড যাচাইয়ের নিয়ম কিছুই বদলায়নি, শুধু
              পর্দার চেহারা আর কার কাছে নোটিশ যাবে সেটা। */
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0B4F2A"))
            setPadding(dp(14), dp(13), dp(10), dp(13))
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = "◀"; textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(dp(2), 0, dp(12), 0)
                setOnClickListener { finish() }
            })
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = "MONEY HANDOVER"; textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            /* ⋮ V1196 (TK: *"বিগত দিনের হিস্টোরি কোথায় পাবো"*) — বুঝে নেওয়া
               দিনগুলো এখন এই মেনুর "Handover History"-তে। */
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = "⋮"; textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(16), dp(2), dp(12), dp(2))   // 🔎 V1254
                isClickable = true
                setOnClickListener { v ->
                    try {
                        val pm = android.widget.PopupMenu(this@MoneyHandoverActivity, v)
                        pm.menu.add(0, 0, 0, if (historyMode) "Pending handovers" else "Handover History")
                        pm.setOnMenuItemClickListener { historyMode = !historyMode; render(); true }
                        pm.show()
                    } catch (_: Throwable) { historyMode = !historyMode; render() }
                }
            })
        })
        /* 💵 V1196 — লাল পট্টির বদলে একটাই সাদা কার্ড: কত বাকি · কয় দিন। */
        sumMoney = TextView(this).apply {
            text = "…"; textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#C0392B"))
        }
        sumDays = TextView(this).apply {
            text = "…"; textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#16232E"))
        }
        fun capt(t: String) = TextView(this).apply {
            text = t; textSize = 10.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#8B98A9"))
            letterSpacing = 0.06f
        }
        sumCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = box("#FFFFFF", "#E1E8E4", 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(10), dp(10), dp(10), dp(2)) }
            addView(LinearLayout(this@MoneyHandoverActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(10), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(capt("STILL WITH YOU")); addView(sumMoney)
            })
            addView(View(this@MoneyHandoverActivity).apply {
                setBackgroundColor(Color.parseColor("#EDF2EF"))
                layoutParams = LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT)
                    .apply { topMargin = dp(10); bottomMargin = dp(10) }
            })
            addView(LinearLayout(this@MoneyHandoverActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(10), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(capt("DAYS PENDING")); addView(sumDays)
            })
        }
        root.addView(sumCard)
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            addView(listBox)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        })
        setContentView(root)
        load()
    }

    private fun branchOfMe(): String {
        val u = NativeSession.current(this) ?: return ""
        return if (u.role == "master") BranchFilterStore.get(this) else u.branch
    }

    private fun load() {
        Thread {
            val me = NativeSession.current(this)
            val mine = branchOfMe()
            val rows = if (me?.role == "doctor")
                /* ডাক্তারের পর্দায় শুধু তাঁর নিজের বুঝে নেওয়ার কাজগুলো ও
                   তাঁর ব্রাঞ্চের ইতিহাস। */
                (MoneyHandover.waitingFor(me.mobile) + MoneyHandover.fetchDays(mine))
                    .distinctBy { it.id }
            else MoneyHandover.fetchDays(mine)
            runOnUiThread { days = rows.sortedByDescending { it.date }; render() }
        }.start()
    }

    private fun render() {
        listBox.removeAllViews()
        /* 💵🔒 V1037 (০৪.০৯.২০২৬, TK-নির্দেশ) — TK: *"অনলাইনে টাকা ডাইরেক্ট আমাদের
           কাছে চলে আসে, শুধু ক্যাশ টাকা স্টাফরা আমাদেরকে বুঝিয়ে দেয়"*।
           ⇒ হাতে বুঝিয়ে দেওয়ার অঙ্ক **শুধু ক্যাশ** (`cashTotal`), দিনের মোট নয়। */
        /* 💰 V1308 (তালিকা ৪২১): যে দিনে ক্যাশ ₹0 আর কিছু হয়নি — বুঝিয়ে দেওয়ার কিছু নেই ⇒ তালিকায়
           বা গোনায় নয় (কিষানগঞ্জে এমন ৩৫ দিন জমে স্টাফ বিভ্রান্ত হচ্ছিলেন)। WAITING/RECEIVED আগের মতোই। */
        val pendingDays = days.filter { it.stillWithStaff && it.cash > 0.0 }
        val pending = pendingDays.sumOf { it.cash }
        sumMoney.text = MoneyHandover.money(pending)
        sumDays.text = pendingDays.size.toString()
        sumCard.visibility = if (days.isEmpty()) View.GONE else View.VISIBLE

        /* ⋮ V1196 — মূল তালিকায় যেগুলো এখনো মেটেনি (এখনো আপনার কাছে · স্বীকার
           বাকি); "Handover History"-তে বুঝে নেওয়া দিনগুলো। */
        val shown = if (historyMode) days.filter { it.status == "received" }
                    else days.filter { it.status != "received" && !(it.stillWithStaff && it.cash <= 0.0) }   // 💰 V1308
        if (shown.isEmpty()) {
            listBox.addView(TextView(this).apply {
                text = if (historyMode) "No handover yet." else "Nothing pending."
                textSize = 13f
                setTextColor(Color.parseColor("#7A8794"))
                setPadding(dp(18), dp(20), dp(18), dp(20))
            })
            return
        }
        for (d in shown) listBox.addView(cardFor(d))
    }

    /** এক দিন = এক বাক্স (TK-র পাশ-করা প্রুফের হুবহু সাজ)। */
    private fun cardFor(d: MoneyHandover.Day): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = box("#FFFFFF", "#E7ECEA", 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(10), dp(8), dp(10), 0) }
        }
        val rail = when (d.status) {
            "received" -> "#0F766E"
            "waiting" -> "#E0A800"
            else -> "#C0392B"
        }
        wrap.addView(View(this).apply {
            setBackgroundColor(Color.parseColor(rail))
            layoutParams = LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT)
        })
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        wrap.addView(card)

        // ── উপরের সারি: তারিখ · ব্রাঞ্চ · টাকা (তারিখ শুধু এখানেই — TK-নির্দেশ) ──
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = MoneyHandover.dotDate(d.date); textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#16232E"))
            })
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = d.branch; textSize = 10.5f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#5B6B82"))
                background = box("#F2F6F4", "#F2F6F4", 8)
                setPadding(dp(9), dp(4), dp(9), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(8) }
            })
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = MoneyHandover.money(d.cash); textSize = 15f   // 💵 V1037 — শুধু ক্যাশ
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F5132"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        })

        // ── অবস্থার ছোট ব্যাজ + এক লাইনে কে · কখন (TK-নির্দেশ) ──
        val time = MoneyHandover.timeOf(d.receivedAt)
        val (label, sub, ink, fill, stroke) = when (d.status) {
            "received" -> Quint("RECEIVED",
                d.receiverName + (if (time.isBlank()) "" else "  ·  " + MoneyHandover.dotDate(d.receivedAt) + "  ·  " + time),
                "#0A7C3F", "#E8F6ED", "#BFE3CD")
            "waiting" -> Quint("WAITING",
                "Sent to " + d.receiverName + (if (time.isBlank()) "" else "  ·  " + time),
                "#8A5A00", "#FFF6E6", "#F0DCA8")
            else -> Quint("NOT HANDED OVER", "nobody has received it", "#C0392B", "#FDECEA", "#F3C4BE")
        }
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        statusRow.addView(TextView(this).apply {
            text = label; textSize = 10.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(ink))
            background = box(fill, stroke, 9)
            setPadding(dp(10), dp(5), dp(10), dp(5))
        })
        statusRow.addView(TextView(this).apply {
            text = "  " + sub; textSize = 11.5f
            setTextColor(Color.parseColor("#7A8794"))
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val me = NativeSession.current(this)
        val meMobile = StaffDirectory.normalizeMobile(me?.mobile.orEmpty())

        // ── স্টাফের কাছে থেকে গেলে পরে যেকোনো দিন বুঝিয়ে দেওয়া যায় ──
        if (d.stillWithStaff) statusRow.addView(smallButton("Hand over", "#0B8A3E") { handOver(d) })
        // ── যাঁকে দেওয়া হয়েছে, তিনি নিজের ফোনে স্বীকার করবেন ──
        if (d.status == "waiting" && meMobile.isNotBlank() && meMobile == d.receiverMobile)
            statusRow.addView(smallButton("I received it", "#0F3D6B") { acknowledge(d) })
        card.addView(statusRow)

        if (d.online > 0.0) card.addView(TextView(this).apply {
            text = "Online " + MoneyHandover.money(d.online) + " — came to you directly"
            textSize = 10.5f
            setTextColor(Color.parseColor("#8B98A9"))
            setPadding(0, dp(8), 0, 0)
        })
        return wrap
    }

    /** পাঁচটা লেখা একসাথে ফেরানোর ছোট্ট ঘর (Kotlin-এ Triple-এর পরেরটা নেই)। */
    private data class Quint(
        val a: String, val b: String, val c: String, val d: String, val e: String
    )

    private fun smallButton(label: String, colour: String, run: () -> Unit) =
        TextView(this).apply {
            text = label; textSize = 11.5f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = box(colour, colour, 11)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
            setOnClickListener { run() }
        }

    private fun actionButton(label: String, colour: String, run: () -> Unit) =
        TextView(this).apply {
            text = label; textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = box(colour, colour, 10)
            setPadding(dp(6), dp(13), dp(6), dp(13))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(9) }
            setOnClickListener { run() }
        }

    private fun passwordField(): EditText = EditText(this).apply {
        hint = "Password"
        textSize = 13.5f
        setTextColor(Color.parseColor("#101C2E"))
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        background = box("#F8FBFE", "#D6E1EE", 9)
        setPadding(dp(12), dp(11), dp(12), dp(11))
        tag = "nocaps"
    }

    /* 🔔🔒 V1196 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু):
         *"যে স্টাফ হ্যান্ডওভার করে দেবে, এখানে J.H MANDAL-এর পাসওয়ার্ড সেই
          staff-এর ফোনে কেন দেখাবে? তার জন্য তো একটা নোটিফিকেশন যাওয়া উচিত —
          শুধুমাত্র যাকে টাকাটা বুঝে দেবে তার কাছে আর মাস্টারের কাছে"*
       ⇒ স্টাফের ফোনে **আর কারো পাসওয়ার্ড লাগে না**। কাকে দিচ্ছেন সেটা বেছে
         "Send" — নোটিশ যায় শুধু তাঁর ও মাস্টারের কাছে, তিনি নিজের ফোনে
         স্বীকার করলে তবেই "received"।
       ⚠️ ততক্ষণ দিনটা **WAITING** থাকে — মাস্টার দেখতে পান (TK-কে জানানো)।
       ⛔ টাকার অঙ্ক · কোথায় জমা · পাসওয়ার্ড যাচাইয়ের কোড কিছুই বদলায়নি;
          যিনি নিচ্ছেন তিনি নিজের ফোনে নিজের পাসওয়ার্ডেই স্বীকার করেন। */
    private fun handOver(d: MoneyHandover.Day) {
        val receivers = MoneyHandover.receiversFor(d.branch)
        if (receivers.isEmpty()) {
            Toast.makeText(this, "No doctor is listed for this branch", Toast.LENGTH_LONG).show(); return
        }
        val names = receivers.map { it.name + "   ·   " + it.role }.toTypedArray()
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💰 Hand over " + MoneyHandover.money(d.cash)))   // 💵 V1037
            .setItems(names) { _, which -> askConfirm(d, receivers[which]) }
            .setNegativeButton("Cancel", null)
            .create().also { it.show(); try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun askConfirm(d: MoneyHandover.Day, who: MoneyHandover.Receiver) {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💰 Hand over " + MoneyHandover.money(d.cash)))
            .setMessage(
                "To : " + who.name + "  ·  " + who.role + "\n\n" +
                "He will get a notification on his own phone and confirm there.\n" +
                "Until then this day stays as WAITING."
            )
            .setPositiveButton("Send") { _, _ -> doHandOver(d, who) }
            .setNegativeButton("Cancel", null)
            .create().also { it.show(); try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun doHandOver(d: MoneyHandover.Day, who: MoneyHandover.Receiver) {
        val me = NativeSession.current(this)
        val myName = me?.name.orEmpty().ifBlank { me?.mobile.orEmpty() }
        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show()
        Thread {
            val ok = MoneyHandover.saveHandover(
                this, d.branch, d.date, d.cash, who, false, myName, me?.mobile.orEmpty()
            )
            runOnUiThread {
                if (ok) {
                    Toast.makeText(this, "Sent to " + who.name + " for confirmation", Toast.LENGTH_LONG).show()
                    load()
                } else {
                    Toast.makeText(this, "Could not save — please try again", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** ডাক্তার/মাস্টার নিজের ফোনে স্বীকার করছেন। */
    private fun acknowledge(d: MoneyHandover.Day) {
        val me = NativeSession.current(this) ?: return
        val field = passwordField()
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
            addView(TextView(this@MoneyHandoverActivity).apply {
                text = "YOUR PASSWORD"
                textSize = 9.5f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#8B98A9"))
                letterSpacing = 0.09f
                setPadding(0, 0, 0, dp(5))
            })
            addView(field)
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "✅ " + MoneyHandover.money(d.cash)))   // 💵 V1037
            .setView(body)
            .setPositiveButton("I received this money") { _, _ ->
                val typed = field.text?.toString().orEmpty()
                Thread {
                    val v = MoneyHandover.verifyPassword(me.mobile, me.role, typed)
                    val ok = v == MoneyHandover.Verify.OK &&
                        MoneyHandover.acknowledge(this, d, me.name.ifBlank { me.mobile })
                    runOnUiThread {
                        when {
                            v == MoneyHandover.Verify.NO_NETWORK ->
                                Toast.makeText(this, "Network problem — could not verify. Please try again.", Toast.LENGTH_LONG).show()
                            v == MoneyHandover.Verify.WRONG ->
                                Toast.makeText(this, "Wrong password", Toast.LENGTH_LONG).show()
                            ok -> { Toast.makeText(this, "Confirmed", Toast.LENGTH_LONG).show(); load() }
                            else -> Toast.makeText(this, "Could not save — please try again", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .create().also { it.show(); try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }
}
