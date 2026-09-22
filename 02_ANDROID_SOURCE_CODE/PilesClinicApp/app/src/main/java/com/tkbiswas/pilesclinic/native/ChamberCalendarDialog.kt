package com.tkbiswas.pilesclinic.native

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * NEW (2026-08-07, TK-approved). A small self-contained calendar popup used
 * by the Follow-up "next date" flow. It replaces the plain Android
 * DatePickerDialog ONLY at the call-sites that opt in — every other date
 * picker in the app is untouched.
 *
 * What it adds over the stock picker:
 *   • Chamber-days for the branch are LIGHTLY tinted (হালকা সবুজ) so staff can
 *     see them at a glance; the selected day turns solid green (গাঢ়).
 *   • Past days can never be chosen.
 *   • `chamberOnly = true` (used only for an Enquiry "আসবে" pick) allows
 *     selecting ONLY a real chamber-day; otherwise ANY future day is allowed
 *     and chamber-days are merely highlighted (TK: "যেকোনো দিন বাঁচতে দেবে,
 *     চেম্বারের দিন হালকা হাইলাইট")।
 *
 * It reads nothing from the cloud (no free-plan cost) and writes nothing —
 * it just returns the chosen yyyy-MM-dd string to `onPicked`.
 */
object ChamberCalendarDialog {

    /**
     * @param initialIso  yyyy-MM-dd to preselect, or null/blank for none.
     * @param mandatory   when true the popup cannot be dismissed without
     *                    choosing a date (used by the post-remark flow that
     *                    was already mandatory before this feature).
     */
    fun show(
        context: Context,
        branch: String?,
        title: String,
        chamberOnly: Boolean,
        initialIso: String?,
        mandatory: Boolean,
        /* 📵🔒 V1332 (১১.০৯.২০২৬, TK-নির্দেশ ও গভীর যাচাই — তালিকা সারি ৪৩৩) —
           TK-রিপোর্ট: রিমার্ক লিখেই স্টাফ ক্যালেন্ডারে **আজকের** তারিখই বেছে
           নিতে পারতেন, ফলে "পরের কল" আসলে আজই বকেয়া থেকে যেত — স্টাফ কাজ
           করেও ব্যানারে থেকে যাওয়ার একটা বড় কারণ এটাই। ⛔ ডিফল্ট `false`,
           তাই প্রকল্পের বাকি সব ডাক (ম্যানুয়াল এডিট, Enquiry-র "আসবে"
           চেম্বার-তারিখ ইত্যাদি) এক অক্ষরও বদলায়নি — শুধু বাধ্যতামূলক
           (post-remark) ক্যালেন্ডারেই সত্যিকারের আগামীকাল বা তার পরের
           তারিখ বাছতে হবে (`startNextFollowDate`-এ `blockToday = mandatory`)। */
        blockToday: Boolean = false,
        /* 📵🔒 V711 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত) — TK: *"কোন
           পেশেন্ট যখন কন্টিনিউ পেশেন্ট অথবা কন্টিনিউ ট্রিটমেন্ট করাচ্ছে, তাদেরকে
           আর ফোন না করলেও চলে"*। এই ঘরটা দেওয়া থাকলে ক্যালেন্ডারের নিচে একটা
           বাড়তি বোতাম আসে — "📵 No more calls needed"।
           ⛔ ডিফল্ট `null`, তাই প্রজেক্টের **বাকি সব ডাক এক অক্ষরও বদলায়নি** —
              যেখানে দেওয়া হয়নি সেখানে বোতামটা বসেই না। */
        onNoMoreCalls: (() -> Unit)? = null,
        onPicked: (String) -> Unit
    ) {
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // Today at midnight — the earliest selectable day.
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayKey = iso.format(today.time)

        // Month currently drawn (1st of that month).
        val shown = Calendar.getInstance().apply {
            time = today.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // If a valid future preselect was given, open on its month.
        var selectedKey: String? = null
        if (!initialIso.isNullOrBlank() && initialIso >= todayKey && !(blockToday && initialIso == todayKey)) {
            try {
                val d = iso.parse(initialIso)
                if (d != null) {
                    shown.time = d
                    shown.set(Calendar.DAY_OF_MONTH, 1)
                    selectedKey = initialIso
                }
            } catch (_: Exception) {}
        }

        val dialog = Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(!mandatory)
        dialog.setCanceledOnTouchOutside(!mandatory)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFFFFF"))
                cornerRadius = dp(16).toFloat()
            }
        }

        // ── Header: title + chamber-day caption ─────────────────────────────
        root.addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#101828"))
        })
        val caption = TextView(context).apply {
            val bn = ChamberDays.labelFor(branch)
            text = "${com.tkbiswas.pilesclinic.print.BranchCatalog.byName(branch).displayName} — Chamber: $bn"
            textSize = 12f
            setTextColor(Color.parseColor("#0F766E"))
            setPadding(0, dp(3), 0, dp(10))
        }
        root.addView(caption)

        // ── Month navigation row ────────────────────────────────────────────
        val monthLabel = TextView(context).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B3B73"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val prev = TextView(context).apply {
            text = "‹"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0B3B73"))
            setPadding(dp(14), 0, dp(14), 0)
            isClickable = true; isFocusable = true
        }
        val next = TextView(context).apply {
            text = "›"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0B3B73"))
            setPadding(dp(14), 0, dp(14), 0)
            isClickable = true; isFocusable = true
        }
        val navRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(prev); addView(monthLabel); addView(next)
        }
        root.addView(navRow)

        // ── Weekday header (Sunday-first) ───────────
        // 🔒 B605 (TK-নির্দেশ 09.08.2026): বারের নাম ইংরেজিতে (বাংলা সংক্ষেপে বিভ্রান্ত হচ্ছিল)।
        val weekNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(4))
            for (w in weekNames) addView(TextView(context).apply {
                text = w; textSize = 11f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#98A2B3"))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        })

        // Grid container (rebuilt whenever the month changes).
        val gridHolder = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(gridHolder)

        // Footer confirm/back.
        // 🔒 B606 (TK-নির্দেশ 09.08.2026): বোতাম "Set" (তারিখ কার্যকর)।
        val confirm = TextView(context).apply {
            text = "Set"
            textSize = 14f; gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#12805C")); cornerRadius = dp(11).toFloat()
            }
            isClickable = true; isFocusable = true
            alpha = if (selectedKey != null) 1f else 0.45f
        }

        // cellFor draws one day. Returns the TextView so the caller can restyle.
        fun styleCell(cell: TextView, dayKey: String?, isChamber: Boolean, selectable: Boolean) {
            if (dayKey == null) { // blank pad
                cell.text = ""; cell.background = null; cell.isClickable = false
                return
            }
            val isSelected = dayKey == selectedKey
            val bg = GradientDrawable().apply { cornerRadius = dp(10).toFloat() }
            when {
                isSelected -> { bg.setColor(Color.parseColor("#12805C")); cell.setTextColor(Color.WHITE) }
                !selectable -> { bg.setColor(Color.TRANSPARENT); cell.setTextColor(Color.parseColor("#C3CBD4")) }
                isChamber -> { bg.setColor(Color.parseColor("#F1FAF5")); cell.setTextColor(Color.parseColor("#3F9B74")) }
                else -> { bg.setColor(Color.TRANSPARENT); cell.setTextColor(Color.parseColor("#344054")) }
            }
            cell.background = bg
            cell.isClickable = selectable
        }

        // rebuild() redraws the whole month grid + labels.
        fun rebuild() {
            monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.US).format(shown.time)
            // Prev disabled if we're already on (or before) the current month.
            val onCurrentMonth = shown.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                shown.get(Calendar.MONTH) == today.get(Calendar.MONTH)
            prev.alpha = if (onCurrentMonth) 0.25f else 1f
            prev.isClickable = !onCurrentMonth

            gridHolder.removeAllViews()
            val first = Calendar.getInstance().apply {
                time = shown.time; set(Calendar.DAY_OF_MONTH, 1)
            }
            val lead = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0..6
            val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)

            var dayNum = 1
            var placed = 0
            val totalCells = lead + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                val rowLl = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(3), 0, dp(3))
                }
                for (c in 0 until 7) {
                    val cell = TextView(context).apply {
                        textSize = 14f; gravity = Gravity.CENTER
                        setTypeface(typeface, Typeface.BOLD)
                        val lp = LinearLayout.LayoutParams(0, dp(40), 1f)
                        lp.setMargins(dp(3), 0, dp(3), 0)
                        layoutParams = lp
                    }
                    if (placed < lead || dayNum > daysInMonth) {
                        styleCell(cell, null, false, false)
                    } else {
                        val dCal = Calendar.getInstance().apply {
                            time = first.time; set(Calendar.DAY_OF_MONTH, dayNum)
                        }
                        val key = iso.format(dCal.time)
                        val dow = dCal.get(Calendar.DAY_OF_WEEK)
                        val isChamber = ChamberDays.isChamberWeekday(branch, dow)
                        val isPast = if (blockToday) key <= todayKey else key < todayKey
                        val selectable = !isPast && (!chamberOnly || isChamber)
                        cell.text = dayNum.toString()
                        styleCell(cell, key, isChamber, selectable)
                        if (selectable) {
                            val label = SimpleDateFormat("EEE dd.MM", Locale.US).format(dCal.time)
                            cell.setOnClickListener {
                                selectedKey = key
                                confirm.alpha = 1f
                                confirm.text = "Set · $label"
                                rebuild() // repaint selection
                            }
                        }
                        dayNum++
                    }
                    placed++
                    rowLl.addView(cell)
                }
                gridHolder.addView(rowLl)
            }
        }

        prev.setOnClickListener {
            val onCurrentMonth = shown.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                shown.get(Calendar.MONTH) == today.get(Calendar.MONTH)
            if (!onCurrentMonth) { shown.add(Calendar.MONTH, -1); rebuild() }
        }
        next.setOnClickListener { shown.add(Calendar.MONTH, 1); rebuild() }

        // Legend.
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(10))
            fun swatch(colorHex: String, label: String) {
                addView(View(context).apply {
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(colorHex)); cornerRadius = dp(4).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).also { it.setMargins(0, dp(2), dp(5), 0) }
                })
                addView(TextView(context).apply {
                    text = label; textSize = 11f; setTextColor(Color.parseColor("#5A6B76"))
                    setPadding(0, 0, dp(14), 0)
                })
            }
            swatch("#F1FAF5", "Chamber Day")
            swatch("#12805C", "Selected")
        })

        // Footer row — Cancel + Set (দুটোই সবসময়)।
        // 🔒 B606 (TK-নির্দেশ 09.08.2026): "Cancel" বোতাম সবসময় থাকবে (আগে শুধু
        // non-mandatory হলে "Back" আসত) — Set চাপলে তারিখ কার্যকর, Cancel চাপলে বাতিল/ব্যাক।
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        footer.addView(TextView(context).apply {
            text = "Cancel"; textSize = 14f; gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#5B6470"))
            setPadding(0, dp(12), 0, dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(11).toFloat()
                setStroke(dp(2), Color.parseColor("#C6CFD8"))
                setColor(Color.WHITE)
            }
            isClickable = true; isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.setMargins(0, 0, dp(10), 0) }
            setOnClickListener { dialog.dismiss() }
        })
        confirm.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        confirm.setOnClickListener {
            val chosen = selectedKey
            if (chosen == null) {
                android.widget.Toast.makeText(context, "Please select a date", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            onPicked(chosen)
        }
        footer.addView(confirm)
        root.addView(footer)

        /* 📵🔒 V711 — "আর কল লাগবে না" (শুধু যেখানে চাওয়া হয়েছে)।
           ⛔ ক্যালেন্ডারের বাকি সব — তারিখ বাছা · Cancel · Set — এক অক্ষরও
              বদলায়নি; এটা তাদের **নিচে** একটা আলাদা বোতাম। */
        if (onNoMoreCalls != null) {
            root.addView(TextView(context).apply {
                text = "\uD83D\uDCF5  No more calls needed"
                textSize = 13.5f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#5B3A9E"))
                setPadding(0, dp(12), 0, dp(12))
                background = GradientDrawable().apply {
                    cornerRadius = dp(11).toFloat()
                    setStroke(dp(2), Color.parseColor("#C9B8F0"))
                    setColor(Color.parseColor("#F6F2FE"))
                }
                isClickable = true; isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dp(10) }
                setOnClickListener { dialog.dismiss(); onNoMoreCalls.invoke() }
            })
        }

        rebuild()

        val scroll = android.widget.ScrollView(context).apply { addView(root) }
        dialog.setContentView(scroll)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        try { NoBengali.installDialog(dialog) } catch (_: Throwable) {}
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }
}
