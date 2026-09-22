package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.modules.ModuleUi
import org.json.JSONObject

/**
 * 🔔🔒 V1186 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **Doctor Note & Reminder**
 * পর্দা: উপরে চলতি রিমাইন্ডারগুলো (Accept সহ), নিচে নতুন পাঠানোর বোতাম ও
 * **Reminder History**।
 *
 * ⛔ পর্দা আঁকার ধরন প্রকল্পের নিজের `ModuleUi` দিয়েই — নতুন কোনো ছাঁদ নয়।
 * ⛔ টাকার কোনো হিসাব এখানে নেই।
 */
class DoctorReminderActivity : AppCompatActivity() {

    private var user: NativeUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = NativeSession.current(this)
        renderList()
    }

    private fun dp(v: Int) = ModuleUi.dp(this, v)

    private fun box(fill: String, stroke: String, radius: Int = 14) =
        android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(android.graphics.Color.parseColor(fill))
            setStroke(dp(1), android.graphics.Color.parseColor(stroke))
        }

    private fun tv(text: String, size: Float, hex: String, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(android.graphics.Color.parseColor(hex))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun dmy(iso: String): String = try {
        val p = iso.take(10).split("-"); p[2] + "/" + p[1] + "/" + p[0]
    } catch (_: Throwable) { iso }

    /* ⏰🔒 V1241 (০৮.০৯.২০২৬, TK-রিপোর্ট ছবিসহ, খাতার সারি ৩৬১) — TK:
       *"রাত ১১টা ১৪-১৫ মিনিটে Accept করলাম, কিন্তু এখানে দেখাচ্ছে ৫.৪৫ PM"*।
       🔴 **আসল কারণ (মেপে দেখা, দোষটা আমারই):** সময়টা ডেটাবেসে **UTC**-তে
          জমা হয় (`...THH:mm:ssZ` — TK-র লক করা নিয়ম, ছোঁয়া হয়নি), কিন্তু
          দেখানোর সময় ওই ঘণ্টাটাই **সোজা ছাপা হত**, ভারতীয় সময়ে বদলানো হত না।
          ভারত UTC-র চেয়ে ৫ ঘণ্টা ৩০ মিনিট এগিয়ে ⇒ ১১.১৫ PM লেখা হত ৫.৪৫ PM।
       ⇒ এখন সময়টা ভারতীয় সময়ে বদলে দেখানো হয়। ⛔ **জমা থাকা লেখাটা এক
         অক্ষরও বদলায়নি** — শুধু পর্দায় পড়াটা ঠিক হলো; তাই পুরনো সব সারিও
         এখন থেকে সঠিক সময় দেখাবে। */
    /** "2026-09-07T18:42:03Z" → ভারতীয় সময়ে "08/09/2026 · 12.12 AM"। */
    private fun stamp(raw: String): String {
        val t = raw.trim()
        if (t.length < 10) return ""
        if (t.length >= 19 && t.endsWith("Z")) {
            try {
                val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                inFmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val outFmt = java.text.SimpleDateFormat("dd/MM/yyyy'  ·  'h.mm a", java.util.Locale.US)
                outFmt.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                val parsed = inFmt.parse(t)
                if (parsed != null) return outFmt.format(parsed)
            } catch (_: Throwable) { }
        }
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

    private fun time12(hm: String): String {
        val p = hm.trim().split(":")
        if (p.size < 2) return hm
        val h = p[0].toIntOrNull() ?: return hm
        val ap = if (h >= 12) "PM" else "AM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return "$h12.${p[1]} $ap"
    }

    /* ═══════════════ 🎨 V1193 — TK-র পাশ-করা প্রুফের সাজ ═══════════════
       TK (০৭.০৯.২০২৬): *"এগুলি প্রফেশনাল লুক আনতে হবে"* ও
       *"Back একদম ডিসপ্লের নিচে থাকবে"*।
       ⇒ Extra Income ফর্মের (V1182, TK-পাশ) হুবহু একই ভাষা — একটাই সাদা
         কার্ড, উপরে রঙিন পট্টি, প্রতিটা ঘর নিজের বাক্সে, ছোট আউটলাইন
         বোতাম, আর Back কার্ডের বাইরে পর্দার একদম নিচে।
       ⛔ কী সেভ হয় · কে দেখেন · কে Accept করেন — কিচ্ছু বদলায়নি, শুধু সাজ। */

    private fun cardBox(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(11), dp(11), dp(11), dp(12))
        background = box("#FFFFFF", "#CFE9D8", 18)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun stripHeader(title: String, right: String, from: String, to: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(android.graphics.Color.parseColor(from), android.graphics.Color.parseColor(to))
            ).apply { cornerRadius = dp(14).toFloat() }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            addView(TextView(this@DoctorReminderActivity).apply {
                text = title; textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                letterSpacing = 0.05f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (right.isNotBlank()) addView(TextView(this@DoctorReminderActivity).apply {
                text = right; textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#EAF7F0"))
            })
        }

    private fun cellBox(fill: String = "#FBFDFC", stroke: String = "#E7ECEA"): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(fill, stroke, 13)
            /* 📏 V1202 (TK-নির্দেশ ও ফটো-প্রুফ পাশ: *"প্রতিটা ঘর এবং প্রতিটা
               বক্সের উচ্চতা আরো কম হবে"*) — ভিতরের উপর-নিচের ফাঁক ১০ → ৬dp। */
            setPadding(dp(12), dp(6), dp(12), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

    private fun capLabel(text: String): TextView = tv(text, 10.5f, "#8B98A9", bold = true).apply {
        letterSpacing = 0.06f
    }

    private fun miniBtn(text: String, textHex: String, borderHex: String, onClick: () -> Unit): android.widget.Button =
        android.widget.Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 12.5f
            minWidth = 0; minimumWidth = 0
            gravity = android.view.Gravity.CENTER
            setPadding(dp(14), dp(5), dp(14), dp(5))   // 📏 V1202 — পাতলা বোতাম
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(textHex))
            background = box("#FFFFFF", borderHex, 12).apply {
                setStroke(dp(2), android.graphics.Color.parseColor(borderHex))
            }
            var lastTap = 0L
            setOnClickListener {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastTap < 900L) return@setOnClickListener
                lastTap = now
                onClick()
            }
        }

    private fun rowOf(vararg views: android.view.View): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            for (v in views) addView(v)
        }

    /** কার্ডের বাইরে, পর্দার একদম নিচে Back (TK-নির্দেশ)। */
    private fun bottomBack(col: LinearLayout, onClick: () -> Unit) {
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.buttonSoft(this, "Back", onClick).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        })
    }

    // ───────────────────────── চলতি তালিকা ─────────────────────────
    private fun renderList() {
        val col = ModuleUi.screen(this, "")
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true

        // উপরে শিরোনাম + ছোট "+ New" (আগের বড় সবুজ বার নয় — TK-র পাশ-করা প্রুফ)
        col.addView(rowOf(
            /* 📏 V1202 (TK: *"এই লেখাটা আরও ছোট হবে"*) — ১৯ → ১৫.৫sp। */
            tv("Doctor Note & Reminder", 15.5f, "#0B4F2A").apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
            android.widget.Button(this).apply {
                text = "+ New"; isAllCaps = false; textSize = 12.5f
                minWidth = 0; minimumWidth = 0
                /* 📏 V1202 (TK: *"+ New উচ্চতা আরো কম হবে"*) — ৯ → ৫dp।
                   📏 V1571 (১৭.০৯.২০২৬, TK-নির্দেশ, Artifact ডেমো "A" পাশ) — ৫ → ৩dp। */
                setPadding(dp(15), dp(3), dp(15), dp(3))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.parseColor("#0B8A3E"))
                }
                setOnClickListener { renderCreate() }
            },
            /* ⋮🔒 V1194 (TK-নির্দেশ, ফটো-প্রুফ পাশ, হুবহু): *"রিমাইন্ডার হিস্টরি
               উপরে ডান সাইডে ৩ ডট থাকবে তার মধ্যে থাকতে হবে"* — বেতন-পর্দার
               হুবহু একই ধাঁচ (`PopupMenu`)। ⛔ History-র পর্দা ও তার সব সারি
               এক অক্ষরও বদলায়নি, শুধু বোতামটা এখানে এলো। */
            TextView(this).apply {
                text = "⋮"
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                /* 📏 V1202 (TK: *"+New ও ৩ ডটের মধ্যে গ্যাপ থাকবে আরো"*) — ১৪ → ৩০dp। */
                setPadding(dp(30), dp(4), dp(12), dp(4))   // 🔎 V1254
                isClickable = true
                setOnClickListener { v ->
                    try {
                        val pm = android.widget.PopupMenu(this@DoctorReminderActivity, v)
                        pm.menu.add(0, 0, 0, "Reminder History")
                        pm.setOnMenuItemClickListener { renderHistory(); true }
                        pm.show()
                    } catch (_: Throwable) { renderHistory() }
                }
            }
        ).apply { setPadding(0, 0, 0, dp(10)) })

        val sheet = cardBox()
        col.addView(sheet)
        val strip = stripHeader("WAITING NOW", "", "#0B4F2A", "#0F766E")
        sheet.addView(strip)
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sheet.addView(holder)
        holder.addView(ModuleUi.body(this, "Loading..."))
        Thread {
            val rows = DoctorReminderRepository.visibleFor(user)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                holder.removeAllViews()
                (strip.getChildAt(0) as? TextView)?.text =
                    if (rows.isEmpty()) "WAITING NOW" else "WAITING NOW   (" + rows.size + ")"
                if (rows.isEmpty()) {
                    holder.addView(ModuleUi.body(this, "No reminder right now."))
                } else {
                    for (r in rows) holder.addView(card(r, showAccept = true))
                }
            }
        }.start()
        bottomBack(col) { finish() }
    }

    /** এক রোগী = এক বাক্স (TK-র পাশ-করা প্রুফের হুবহু সাজ)। */
    private fun card(r: JSONObject, showAccept: Boolean): LinearLayout {
        val accepted = r.optString("acceptedAt", "").isNotBlank()
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = box("#FFFFFF", "#E7ECEA", 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        wrap.addView(android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor(if (accepted) "#0F766E" else "#E0A800"))
            layoutParams = LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT)
        })
        val bd = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        wrap.addView(bd)

        val nm = r.optString("patientName", "").ifBlank { "Patient" }
        val mb = r.optString("patientMobile", "")
        /* 🩺 V1194 (TK: *"রোগের নাম দরকার তো"*) — নামের ঠিক পাশে।
           ⛔ পুরনো সারিতে ঘরটা ফাঁকা, তখন কিছুই দেখানো হয় না। */
        val dis = r.optString("disease", "")
        bd.addView(rowOf(
            tv(nm + (if (mb.isNotBlank()) "   $mb" else ""), 14.5f, "#0B2B1C", bold = true),
            tv(if (dis.isBlank()) "" else dis, 11.5f, "#123E8C", bold = true).apply {
                if (dis.isNotBlank()) {
                    background = box("#EEF4FF", "#D6E2FB", 10)
                    setPadding(dp(9), dp(3), dp(9), dp(3))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(8) }
            }
        ))
        bd.addView(tv(r.optString("note", ""), 13.5f, "#17212B").apply {
            background = box("#F6FAF7", "#E2EDE6", 11)
            setPadding(dp(11), dp(9), dp(11), dp(9))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(7) }
        })

        // FOR ও BY পাশাপাশি দুটো ঘরে (প্রুফের মতো)
        fun cell(label: String, value: String, last: Boolean): LinearLayout = cellBox().apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { topMargin = dp(8); if (!last) rightMargin = dp(8) }
            addView(capLabel(label))
            addView(tv(value.ifBlank { "—" }, 12.5f, "#0B2B1C", bold = true))
        }
        val forName = r.optString("forName", "").ifBlank { "All doctors" }
        val byName = r.optString("byName", "")
        val byBranch = r.optString("byBranch", "")
        bd.addView(rowOf(
            cell("FOR", forName, false),
            cell("BY", byName + (if (byBranch.isNotBlank()) " · $byBranch" else ""), true)
        ))

        val rd = r.optString("remindDate", "")
        val rt = r.optString("remindTime", "")
        if (rd.isNotBlank()) {
            bd.addView(cellBox("#FFFBF0", "#F0E0BC").apply {
                addView(tv("Remind on  " + dmy(rd) + (if (rt.isNotBlank()) "  ·  " + time12(rt) else ""),
                    12.5f, "#B45309", bold = true))
            })
        }

        val acts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        if (accepted) {
            acts.addView(tv("Accepted  ·  " + r.optString("acceptedByName", "") + "  ·  " + stamp(r.optString("acceptedAt", "")),
                12f, "#0A7C3F", bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        } else if (showAccept && DoctorReminderRepository.canAccept(r, user)) {
            acts.addView(miniBtn("Accept", "#0A5C33", "#0A5C33") {
                val id = r.optString("id", "")
                Thread {
                    val ok = DoctorReminderRepository.accept(id, user)
                    runOnUiThread {
                        ModuleUi.toast(this, if (ok) "Accepted" else "Failed — check the network")
                        if (ok) renderList()
                    }
                }.start()
            })
        } else if (showAccept && DoctorReminderRepository.canCancel(r, user)) {
            /* 🚫 V1194 — ভুল করে পাঠানো হলে **যিনি পাঠিয়েছেন** বাতিল করতে পারেন
               (Accept হওয়ার আগে পর্যন্ত)। ⛔ সারিটা মোছে না — History-তে থাকে। */
            acts.addView(miniBtn("Cancel", "#C0392B", "#C0392B") {
                val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "Cancel this reminder?"))
                    .setMessage("It was sent by you and is not accepted yet.\nHistory will still show it as CANCELLED.")
                    .setPositiveButton("Yes, cancel") { _, _ ->
                        Thread {
                            val ok = DoctorReminderRepository.cancel(r, user)
                            runOnUiThread {
                                ModuleUi.toast(this, if (ok) "Cancelled" else "Failed — check the network")
                                if (ok) renderList()
                            }
                        }.start()
                    }
                    .setNegativeButton("No", null)
                    .create()
                dlg.show()
                try { PremiumAlert.paint(dlg) } catch (_: Throwable) { }
            })
        }
        /* 🙈 V1193 (TK-নির্দেশ) — শুধু **এই ব্যক্তির** হোম ও ঘন্টা থেকে সরে;
           তালিকা ও History-তে সারিটা আগের মতোই থাকে, কেউ কিছু হারায় না। */
        if (showAccept) {
            acts.addView(miniBtn("Hide", "#5B6B82", "#C9D6CE") {
                Thread {
                    val ok = DoctorReminderRepository.hide(r, user)
                    runOnUiThread {
                        ModuleUi.toast(this, if (ok) "Hidden from your Home & bell" else "Failed — check the network")
                    }
                }.start()
            })
        }
        /* ↔️ V1202 (TK-নির্দেশ) — বোতামগুলো **পাশাপাশি সমান চওড়া**।
           লেখা-ঘর (Accepted ...) থাকলে সেটাই জায়গা নেয়, বোতাম নয়। */
        run {
            var onlyBtn = acts.childCount > 0
            for (i in 0 until acts.childCount) if (acts.getChildAt(i) !is android.widget.Button) onlyBtn = false
            if (onlyBtn) for (i in 0 until acts.childCount) {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.leftMargin = if (i == 0) 0 else dp(8)
                acts.getChildAt(i).layoutParams = lp
            }
        }
        bd.addView(acts)
        return wrap
    }

    // ───────────────────────── History ─────────────────────────
    private fun renderHistory() {
        val col = ModuleUi.screen(this, "Reminder History")
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(holder)
        holder.addView(ModuleUi.body(this, "Loading..."))
        Thread {
            val rows = DoctorReminderRepository.visibleFor(user, historyMode = true)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                holder.removeAllViews()
                if (rows.isEmpty()) holder.addView(ModuleUi.body(this, "Nothing yet."))
                else for (r in rows) holder.addView(historyCard(r))
            }
        }.start()
        bottomBack(col) { renderList() }
    }

    /** TK-র পাশ-করা প্রুফ — কে পাঠাল · কবে · কাকে · কবে Accept, প্রতিটা নিজের ঘরে। */
    private fun historyCard(r: JSONObject): LinearLayout {
        val accepted = r.optString("acceptedAt", "").isNotBlank()
        val cancelled = r.optString("cancelledAt", "").isNotBlank()   // 🚫 V1194
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = box("#FFFFFF", "#E7ECEA", 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        wrap.addView(android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor(
                if (cancelled) "#C0392B" else if (accepted) "#0F766E" else "#E0A800"))
            layoutParams = LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT)
        })
        val bd = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        wrap.addView(bd)
        val hDis = r.optString("disease", "")
        bd.addView(rowOf(
            tv(r.optString("patientName", "").ifBlank { "Patient" } + "   " + r.optString("patientMobile", ""),
                14f, "#0B2B1C", bold = true),
            tv(if (hDis.isBlank()) "" else hDis, 11.5f, "#123E8C", bold = true).apply {
                if (hDis.isNotBlank()) {
                    background = box("#EEF4FF", "#D6E2FB", 10)
                    setPadding(dp(9), dp(3), dp(9), dp(3))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(8) }
            }
        ))
        bd.addView(tv(r.optString("note", ""), 13f, "#17212B").apply {
            background = box("#F6FAF7", "#E2EDE6", 11)
            setPadding(dp(11), dp(9), dp(11), dp(9))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(7) }
        })
        fun cell(label: String, value: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#FBFDFC", "#EDF2EF", 11)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { topMargin = dp(7); rightMargin = dp(7) }
            addView(tv(label, 10.5f, "#8B98A9"))
            addView(tv(value.ifBlank { "—" }, 12.5f, "#0B2B1C", bold = true))
        }
        fun pair(l1: String, v1: String, l2: String, v2: String) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(cell(l1, v1)); row.addView(cell(l2, v2))
            bd.addView(row)
        }
        val byName = r.optString("byName", "")
        val byBranch = r.optString("byBranch", "")
        pair("SENT BY", byName + (if (byBranch.isNotBlank()) " · $byBranch" else ""),
            "SENT ON", stamp(r.optString("createdAt", "")))
        if (cancelled) {
            pair("SENT TO", r.optString("forName", "").ifBlank { "All doctors" },
                "CANCELLED BY", r.optString("cancelledByName", "") + " · " + stamp(r.optString("cancelledAt", "")))
        } else {
            pair("SENT TO", r.optString("forName", "").ifBlank { "All doctors" },
                "ACCEPTED", if (accepted) (r.optString("acceptedByName", "") + " · " + stamp(r.optString("acceptedAt", ""))) else "Not yet")
        }
        val rd = r.optString("remindDate", "")
        val rt = r.optString("remindTime", "")
        bd.addView(tv("Remind on  " + dmy(rd) + (if (rt.isNotBlank()) "  ·  " + time12(rt) else "") +
            "        " + (if (cancelled) "CANCELLED" else if (accepted) "ACCEPTED" else "WAITING"),
            12.5f, if (cancelled) "#C0392B" else if (accepted) "#0A7C3F" else "#8A5A00", bold = true)
            .apply { setPadding(0, dp(9), 0, 0) })
        return wrap
    }

    // ───────────────────────── নতুন পাঠানো ─────────────────────────
    private var pickedPatient: JSONObject? = null
    private var forMobile = ""
    private var forName = ""
    private var remindDate = ""
    private var remindTime = ""

    private fun renderCreate() {
        pickedPatient = null; forMobile = ""; forName = ""; remindDate = ""; remindTime = ""
        val col = ModuleUi.screen(this, "New Reminder")
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true

        val sheet = cardBox()
        col.addView(sheet)
        sheet.addView(stripHeader("NEW REMINDER", dmy(DoctorReminderRepository.todayIso()), "#B45309", "#E0A800"))

        // ── রোগী ──
        sheet.addView(capLabel("PATIENT"))
        val search = ModuleUi.input(this, "Patient name or mobile")
        val patLine = tv("No patient chosen", 13.5f, "#8A93A0")
        val sugHolder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val findCell = cellBox()
        findCell.addView(rowOf(
            search.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
            miniBtn("Find", "#0A5C33", "#0A5C33") {
                val q = search.text.toString().trim()
                if (q.length < 3) { ModuleUi.toast(this, "Type at least 3 letters or digits"); return@miniBtn }
                findPatient(q) { p -> choosePatient(p, patLine, sugHolder) }
            }
        ))
        sheet.addView(findCell)
        sheet.addView(sugHolder)
        sheet.addView(cellBox().apply { addView(patLine) })

        /* 🔎🔒 V1193 (TK-নির্দেশ, হুবহু): *"পেশেন্ট এর নাম 4 সংখ্যা দিলে সাজেশন
           কেন করে না"* — যাচাই করে দেখা গেল টাইপ করার সঙ্গে সঙ্গে সাজেশন
           দেখানোর ব্যবস্থা **কোনোদিনই ছিল না**, "Find Patient" চাপতেই হত।
           এখন ৩ অক্ষর/সংখ্যা লিখলেই নিজে থেকে তালিকা নামে।
           ⛔ টাইপ থামার ~০.৫ সেকেন্ড পরে **একটাই সরু পড়া** (৮টা সারি,
              ৪টে ঘর) — তাই ফ্রি প্ল্যানে বাড়তি চাপ পড়ে না।
           ⛔ "Find" বোতামটা আগের মতোই আছে, কিছু কেড়ে নেওয়া হয়নি। */
        val sugHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var sugTask: Runnable? = null
        var lastQuery = ""
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            override fun afterTextChanged(e: android.text.Editable?) {
                val q = (e?.toString() ?: "").trim()
                sugTask?.let { sugHandler.removeCallbacks(it) }
                if (q.length < 3) { sugHolder.removeAllViews(); return }
                val t = Runnable {
                    if (q == lastQuery) return@Runnable
                    lastQuery = q
                    Thread {
                        val rows = DoctorReminderRepository.searchPatients(q)
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            if (search.text.toString().trim() != q) return@runOnUiThread
                            sugHolder.removeAllViews()
                            if (rows == null || rows.length() == 0) return@runOnUiThread
                            for (i in 0 until rows.length()) {
                                val pt = rows.optJSONObject(i) ?: continue
                                sugHolder.addView(cellBox("#F2FBF5", "#D8ECDF").apply {
                                    isClickable = true
                                    addView(tv(pt.optString("name", ""), 13.5f, "#0B2B1C", bold = true))
                                    addView(tv(pt.optString("mobile", "") + "  ·  " + pt.optString("branch", "") +
                                        (if (pt.optString("disease", "").isNotBlank()) "  ·  " + pt.optString("disease", "") else ""),
                                        11.5f, "#4A6B58"))
                                    setOnClickListener { choosePatient(pt, patLine, sugHolder) }
                                })
                            }
                        }
                    }.start()
                }
                sugTask = t
                sugHandler.postDelayed(t, 500L)
            }
        })

        // ── নোট ──
        val note = ModuleUi.input(this, "What to give / what to do")
        sheet.addView(cellBox().apply { addView(capLabel("NOTE")); addView(note) })

        // ── কোন ডাক্তার ──
        val whoLine = tv("All doctors", 14f, "#0B2B1C", bold = true)
        sheet.addView(cellBox().apply {
            addView(rowOf(
                LinearLayout(this@DoctorReminderActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(capLabel("SEND TO"))
                    addView(whoLine)
                },
                miniBtn("Choose", "#123E8C", "#123E8C") {
                    val branch = (pickedPatient?.optString("branch", "") ?: "").ifBlank { user?.branch ?: "" }
                    val docs = DoctorReminderRepository.doctorsForBranch(branch)
                    val labels = (listOf("All doctors") + docs.map { it.first }).toTypedArray()
                    val mobiles = listOf("") + docs.map { it.second }
                    val names = listOf("") + docs.map { it.first }
                    /* ⚠️ এখানে `this` = উপরের `cellBox()` (LinearLayout), Activity নয় —
                       তাই স্পষ্ট করে Activity-টাই দিতে হয়। */
                    val act = this@DoctorReminderActivity
                    val dlg = androidx.appcompat.app.AlertDialog.Builder(act)
                        .setCustomTitle(PremiumAlert.header(act, "Send to which doctor?"))
                        .setItems(labels) { _, which ->
                            forMobile = mobiles[which]; forName = names[which]
                            whoLine.text = labels[which]
                        }
                        .setNegativeButton("Close", null)
                        .create()
                    dlg.show()
                    try { PremiumAlert.paint(dlg) } catch (_: Throwable) { }
                }
            ))
        })

        /* ✂️ V1202 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"reminder day not choosen /
           time not choosen — এই ডেমি লেখাগুলো থাকবে না"* ⇒ আলাদা "Not chosen"
           লাইনটা তুলে দেওয়া হলো; **বাছা দিন/সময় বোতামের গায়েই** লেখা ওঠে।
           ঘরের উচ্চতাও তাই কমে। ⛔ কী সেভ হয় — এক অক্ষরও বদলায়নি। */
        val dateBtn = miniBtn("Pick Date", "#B45309", "#E0A800") { }
        val timeBtn = miniBtn("Pick Time", "#B45309", "#E0A800") { }
        fun pickCell(label: String, b: android.widget.Button, onPick: () -> Unit, last: Boolean): LinearLayout =
            cellBox().apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { topMargin = dp(8); if (!last) rightMargin = dp(8) }
                addView(capLabel(label))
                addView(b.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(5) }
                    var lastTap = 0L
                    setOnClickListener {
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastTap < 900L) return@setOnClickListener
                        lastTap = now
                        onPick()
                    }
                })
            }
        sheet.addView(rowOf(
            pickCell("REMIND DAY", dateBtn, {
                val c = java.util.Calendar.getInstance()
                val dp = android.app.DatePickerDialog(this, { _, y, m, d ->
                    remindDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    dateBtn.text = dmy(remindDate)
                }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                    c.get(java.util.Calendar.DAY_OF_MONTH))
                /* 📅🔒 V1201 (০৮.০৯.২০২৬, TK-রিপোর্ট, হুবহু): *"রিমাইন্ডার আবার
                   অতীত কাল কি করে নির্বাচন করা হয়"* — কোডে মেপে দেখা গেল তারিখ
                   বাছার ঘরে **সর্বনিম্ন তারিখ বসানোই ছিল না**, তাই গত মাসের দিনও
                   বেছে ফেলা যেত (ওই রিমাইন্ডার কখনো বাজত না)।
                   ⇒ এখন **আজকের আগের কোনো দিন বাছাই করা যায় না**।
                   ⛔ বাকি সব — কার কাছে যাবে · কী সেভ হয় — এক অক্ষরও বদলায়নি। */
                try { dp.datePicker.minDate = System.currentTimeMillis() - 1000 } catch (_: Throwable) { }
                dp.show()
            }, false),
            pickCell("TIME", timeBtn, {
                val c = java.util.Calendar.getInstance()
                android.app.TimePickerDialog(this, { _, h, mi ->
                    remindTime = String.format(java.util.Locale.US, "%02d:%02d", h, mi)
                    timeBtn.text = time12(remindTime)
                }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), false).show()
            }, true)
        ))
        // 🔔 V1241 — এখন আজকের দিনের রিমাইন্ডারও বাজে, তাই লেখাটা সত্যি করা হলো।
        sheet.addView(tv("Reminds the doctor the day before, and again at the chosen time.", 11.5f, "#8B98A9").apply {
            setPadding(dp(2), dp(8), 0, 0)
        })

        // ── কে পাঠাচ্ছেন ──
        sheet.addView(cellBox("#FFFBF0", "#F0E0BC").apply {
            addView(capLabel("REMINDER BY"))
            addView(tv((user?.name ?: "") + (if ((user?.branch ?: "").isNotBlank()) "  ·  " + user?.branch else "") + "   (you)",
                13.5f, "#8A5A00", bold = true))
        })

        sheet.addView(ModuleUi.button(this, "Send Reminder") {
            val p = pickedPatient
            if (p == null) { ModuleUi.toast(this, "Choose a patient first"); return@button }
            val n = note.text.toString().trim()
            if (n.isBlank()) { ModuleUi.toast(this, "Write the note"); return@button }
            if (remindDate.isBlank()) { ModuleUi.toast(this, "Pick the date"); return@button }
            /* 📅 V1201 — দ্বিতীয় স্তরের পাহারা: পুরনো তারিখ কোনোভাবেই সেভ হবে না। */
            if (remindDate < DoctorReminderRepository.todayIso()) {
                ModuleUi.toast(this, "Past date cannot be chosen"); return@button
            }
            ModuleUi.toast(this, "Sending...")
            Thread {
                val ok = DoctorReminderRepository.send(
                    p.optString("id", ""), p.optString("name", ""), p.optString("mobile", ""),
                    p.optString("branch", "").ifBlank { user?.branch ?: "" },
                    n, forMobile, forName, remindDate, remindTime, user,
                    p.optString("disease", "")
                )
                runOnUiThread {
                    ModuleUi.toast(this, if (ok) "Sent" else "Failed — check the network")
                    if (ok) {
                        /* ⏰🔒 V1241 — পাঠানোর **সঙ্গে সঙ্গেই** বাছা সময়ের জন্য ফোনের
                           অ্যালার্ম ঘড়ি বসে; পিছনের ১৫-মিনিটের কাজের অপেক্ষা করতে
                           হয় না। ⛔ ব্যর্থ হলেও কিছু ভাঙে না — ওই কাজটা আগের মতোই
                           পরে নিজে থেকে বসিয়ে নেয়। */
                        /* ⏰🔒 V1277 (০৯.০৯.২০২৬, TK-রিপোর্ট: *"এখানে সময় মতো
                           নোটিফিকেশন আসেই না … একই সমস্যার কথা বেশ কয়েকবার বলা
                           হয়ে গেছে"* — তালিকা সারি ৪০০, আগেরটা সারি ৩৬১)।
                           🔴 **এখানে দুটো দোষ ছিল, দুটোই আমার:**
                           ① এখানে অ্যালার্ম বসত **`remindDate`-এর দিনে** — অথচ
                              বাজার নিয়মই হলো **তার আগের দিন** ওই সময়ে
                              (`DoctorReminderWorker` আজ+কাল দুদিনের সারি পড়ে,
                              বাজায় **আজ**)। ⇒ ঘড়িটা এক দিন পরে বসত, তাই ঠিক
                              সময়ে কিছুই বাজত না।
                           ② অ্যালার্মের ঘর **একটাই**। নতুন রিমাইন্ডারের (দেরির)
                              সময় বসিয়ে দিলে **আগের কাছের অ্যালার্মটা মুছে যেত**
                              ⇒ ওটাও আর ঠিক সময়ে বাজত না।
                           ⇒ এখন নিজে হিসাব না করে কাজটাকেই সঙ্গে সঙ্গে চালানো হয়;
                             সে **সব সারি দেখে সবচেয়ে কাছের** সময়ের জন্য অ্যালার্ম
                             বসায় (`DoctorReminderWorker`-এর ২০৩ নম্বর লাইন) —
                             এক জায়গাতেই নিয়ম, দুরকম হওয়ার পথ নেই।
                           ⛔ বাজার নিয়ম · লেখা · শব্দ · "দিনে একবার" — কিচ্ছু বদলায়নি। */
                        try { DoctorReminderScheduler.runNow(this) } catch (_: Throwable) { }
                        renderList()
                    }
                }
            }.start()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        })

        bottomBack(col) { renderList() }
    }

    /** বেছে নেওয়া রোগী পর্দায় বসে, সাজেশনের তালিকা গুটিয়ে যায়। */
    private fun choosePatient(p: JSONObject, patLine: TextView, sugHolder: LinearLayout) {
        pickedPatient = p
        patLine.text = p.optString("name", "") + "   " + p.optString("mobile", "") +
            (if (p.optString("branch", "").isNotBlank()) "   ·   " + p.optString("branch", "") else "") +
            (if (p.optString("disease", "").isNotBlank()) "   ·   " + p.optString("disease", "") else "")
        patLine.setTextColor(android.graphics.Color.parseColor("#0B2B1C"))
        sugHolder.removeAllViews()
    }

    /** নাম বা নম্বর ধরে রোগী খোঁজা — একটাই সরু পড়া, তারপর বেছে নেওয়া। */
    private fun findPatient(q: String, then: (JSONObject) -> Unit) {
        ModuleUi.toast(this, "Searching...")
        Thread {
            val digitsOnly = q.filter { it.isDigit() }
            val enc = java.net.URLEncoder.encode("*$q*", "UTF-8")
            val filter = if (digitsOnly.length >= 4) "mobile=like.*$digitsOnly*" else "name=ilike.$enc"
            val rows = try {
                SupabaseClient.fetchListSlimOrNull(
                    "patients", filter, 25, "id,name,mobile,branch,disease", order = "name.asc"
                )
            } catch (_: Throwable) { null }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (rows == null || rows.length() == 0) { ModuleUi.toast(this, "No patient found"); return@runOnUiThread }
                val list = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
                val labels = list.map {
                    it.optString("name", "") + "  ·  " + it.optString("mobile", "") + "  ·  " + it.optString("branch", "") +
                        (if (it.optString("disease", "").isNotBlank()) "  ·  " + it.optString("disease", "") else "")
                }.toTypedArray()
                val pick = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "Choose patient"))
                    .setItems(labels) { _, which -> then(list[which]) }
                    .setNegativeButton("Close", null)
                    .create()
                pick.show()
                try { PremiumAlert.paint(pick) } catch (_: Throwable) { }
            }
        }.start()
    }
}
