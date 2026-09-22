package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.tkbiswas.pilesclinic.native.MoneyHandover
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.StaffDirectory

/**
 * 💰🔒 V984 (০২.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
 * **Chamber Register পর্দার "MONEY HANDOVER" ঘর।**
 *
 * TK: *"টাকা বুঝিয়ে দেয়ার সিস্টেমটা এই পর্দাতে রাখুন"*।
 *
 * ⛔ এই ঘরটা **শুধু তখনই** বসে যখন Chamber Register এই পর্দায় আসে
 *    (`PrintDataHolder.handoverBranch` ভরা থাকে)। বাকি প্রতিটা কাগজে
 *    ঘরটা লুকানোই থাকে — এক পিক্সেলও বদলায় না।
 * ⛔ পাসওয়ার্ড যাচাই লগইনের হুবহু প্রমাণিত পথে (`MoneyHandover.verifyPassword`),
 *    নেট না পেলে কখনো "ঠিক আছে" ধরা হয় না।
 */
object MoneyHandoverCard {

    private fun dp(a: Activity, v: Int) = (v * a.resources.displayMetrics.density).toInt()

    private fun box(a: Activity, fill: String, stroke: String, radius: Int, strokeDp: Int = 1) =
        GradientDrawable().apply {
            setColor(Color.parseColor(fill))
            setStroke(dp(a, strokeDp), Color.parseColor(stroke))
            cornerRadius = dp(a, radius).toFloat()
        }

    /** ঘরটা ভরে দেয়। কিছু বসানোর মতো না থাকলে চুপচাপ ফিরে যায়। */
    fun attach(activity: Activity, slot: LinearLayout, onHandled: (() -> Unit)? = null) {
        val branch = PrintDataHolder.handoverBranch
        val date = PrintDataHolder.handoverDate
        if (branch.isBlank() || date.isBlank()) { slot.visibility = View.GONE; return }

        val fees = PrintDataHolder.handoverFees
        val cash = PrintDataHolder.handoverCash
        val online = PrintDataHolder.handoverOnline
        val refund = PrintDataHolder.handoverRefund
        val total = PrintDataHolder.handoverTotal
        PrintDataHolder.clearHandover()

        val receivers = MoneyHandover.receiversFor(branch)
        if (receivers.isEmpty()) { slot.visibility = View.GONE; return }

        val me = NativeSession.current(activity)
        val myName = me?.name.orEmpty().ifBlank { me?.mobile.orEmpty() }

        slot.removeAllViews()
        slot.visibility = View.VISIBLE

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = box(activity, "#FFFFFF", "#0B4F2A", 14, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(activity, 8) }
        }
        slot.addView(card)

        card.addView(TextView(activity).apply {
            text = "💰  MONEY HANDOVER"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0B4F2A"))
            setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10))
        })

        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 12))
        }
        card.addView(body)

        // ── TOTAL ──
        body.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(activity).apply {
                text = "CASH TO HAND OVER"; textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F5132"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(activity).apply {
                text = MoneyHandover.money(cash); textSize = 15f   // 💵 V1038 — শুধু ক্যাশ
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F5132"))
            })
        })

        // 💵 V1038 — অনলাইনের টাকা মালিকের কাছে সরাসরি যায়, তাই শুধু জানানো হয়, যোগ হয় না।
        if (online > 0.0) {
            body.addView(TextView(activity).apply {
                text = "Online " + MoneyHandover.money(online) + " — came to you directly"
                textSize = 11f
                setTextColor(Color.parseColor("#8B98A9"))
                setPadding(0, dp(activity, 4), 0, 0)
            })
        }

        body.addView(TextView(activity).apply {
            text = "RECEIVED BY"
            textSize = 9.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#8B98A9"))
            letterSpacing = 0.09f
            setPadding(0, dp(activity, 12), 0, dp(activity, 5))
        })

        // ── কে নিচ্ছেন ──
        var chosen = receivers.first()
        val rows = ArrayList<LinearLayout>()
        val pick = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = box(activity, "#FFFFFF", "#DDE5EC", 10)
        }
        fun paintPick() {
            for ((i, r) in rows.withIndex()) {
                val on = receivers[i].mobile == chosen.mobile
                r.setBackgroundColor(Color.parseColor(if (on) "#F2FBF6" else "#FFFFFF"))
                (r.getChildAt(0) as TextView).text = if (on) "◉" else "○"
            }
        }
        for (r in receivers) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(activity, 12), dp(activity, 11), dp(activity, 12), dp(activity, 11))
                addView(TextView(activity).apply {
                    text = "○"; textSize = 15f
                    setTextColor(Color.parseColor("#0B4F2A"))
                    setPadding(0, 0, dp(activity, 10), 0)
                })
                addView(TextView(activity).apply {
                    text = r.name; textSize = 13.5f
                    setTextColor(Color.parseColor("#101C2E"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(activity).apply {
                    text = r.role; textSize = 11.5f
                    setTextColor(Color.parseColor("#7A8794"))
                })
                setOnClickListener { chosen = r; paintPick() }
            }
            rows.add(row); pick.addView(row)
        }
        body.addView(pick)
        paintPick()

        /* 🔔🔒 V1196 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"যে স্টাফ হ্যান্ডওভার করে
           দেবে, এখানে অন্যের পাসওয়ার্ড সেই staff-এর ফোনে কেন দেখাবে? তার জন্য
           তো একটা নোটিফিকেশন যাওয়া উচিত"* — নিয়ম ৭ মেনে **এখানেও** একই বদল।
           ⇒ পাসওয়ার্ডের ঘরটা বাদ; নোটিশ যায় শুধু যাঁকে দেওয়া হচ্ছে তাঁর ও
             মাস্টারের কাছে, তিনি নিজের ফোনে স্বীকার করলে তবেই "received"। */
        body.addView(TextView(activity).apply {
            text = "He will get a notification on his own phone and confirm there.\n" +
                "Until then this day stays as WAITING."
            textSize = 11.5f
            setTextColor(Color.parseColor("#7A8794"))
            setPadding(0, dp(activity, 10), 0, 0)
        })

        // ── দুটো বোতাম ──
        val btns = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(activity, 10) }
        }
        body.addView(btns)

        val done = TextView(activity).apply {
            text = "Send"
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = box(activity, "#0B4F2A", "#0B4F2A", 11)
            setPadding(dp(activity, 6), dp(activity, 13), dp(activity, 6), dp(activity, 13))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { leftMargin = dp(activity, 4) }
        }
        val later = TextView(activity).apply {
            text = "Not handed over yet"
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#41506A"))
            background = box(activity, "#EEF2F7", "#E2E9F2", 11)
            setPadding(dp(activity, 6), dp(activity, 13), dp(activity, 6), dp(activity, 13))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { rightMargin = dp(activity, 4) }
        }
        btns.addView(later); btns.addView(done)

        fun lock(on: Boolean) {
            done.isEnabled = !on; later.isEnabled = !on
            done.alpha = if (on) 0.5f else 1f
            later.alpha = if (on) 0.5f else 1f
        }

        fun finishCard(line: String, colour: String) {
            body.removeAllViews()
            body.addView(TextView(activity).apply {
                text = line
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(colour))
                setPadding(0, dp(activity, 4), 0, dp(activity, 4))
            })
        }

        // 🔒 টাকার অঙ্কগুলো আগে সারিতে বসে — এটাই পরে প্রমাণের ভিত্তি।
        Thread {
            try { MoneyHandover.saveTotals(branch, date, fees, cash, online, refund, total) }
            catch (_: Throwable) { }
        }.start()

        done.setOnClickListener {
            lock(true)
            val who = chosen
            val myMobile = try { NativeSession.current(activity)?.mobile.orEmpty() } catch (_: Throwable) { "" }
            Thread {
                val ok = MoneyHandover.saveHandover(activity, branch, date, cash, who, false, myName, myMobile)
                activity.runOnUiThread {
                    lock(false)
                    if (ok) { finishCard("⌛  Sent to " + who.name + "  ·  " +
                        MoneyHandover.money(cash) + "  ·  waiting for confirmation", "#8A5A00"); try { onHandled?.invoke() } catch (_: Throwable) { } }
                    else Toast.makeText(activity, "Could not save — please try again", Toast.LENGTH_LONG).show()
                }
            }.start()
        }

        later.setOnClickListener {
            lock(true)
            Thread {
                val ok = MoneyHandover.markPending(activity, branch, date, cash, myName)   // 💵 V1038
                activity.runOnUiThread {
                    lock(false)
                    if (ok) { finishCard("⚠️  Money is still with you — the master has been informed", "#8A1810"); try { onHandled?.invoke() } catch (_: Throwable) { } }
                    else Toast.makeText(activity, "Could not save — please try again", Toast.LENGTH_LONG).show()
                }
            }.start()
        }
    }
}
