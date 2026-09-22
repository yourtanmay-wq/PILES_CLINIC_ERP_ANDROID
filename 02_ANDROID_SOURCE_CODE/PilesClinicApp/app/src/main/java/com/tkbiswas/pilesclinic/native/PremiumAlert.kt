package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * TK-APPROVED (2026-07-25, via photo proof) — ONE shared professional look
 * for every remaining plain popup in the app.
 *
 * WHY THIS EXISTS: the app already had a premium popup look (dark-green
 * header strip + white rounded card + coloured action buttons) on the
 * dialogs TK finalised earlier (Add/Edit Doctor, Add Treatment Payment,
 * etc.). The rest were still the plain default white popups. TK approved
 * giving those the SAME look. Instead of rewriting 60 popups by hand (high
 * risk), every one of them keeps its own existing code exactly as it is and
 * only gains two things:
 *
 *   .setTitle(x)  ->  .setCustomTitle(PremiumAlert.header(ctx, x))
 *   .show()       ->  .show().also { PremiumAlert.paint(it) }
 *
 * NOTHING about what any popup asks, saves, deletes or shows changes — this
 * file only paints. Only documented public APIs are used (setCustomTitle,
 * Window.setBackgroundDrawable, AlertDialog.getButton), so no popup can
 * break because of a hidden/internal id.
 *
 * DO NOT change these colours without TK's permission — they are the same
 * values as the already-approved premium dialogs.
 */
object PremiumAlert {

    private const val HEADER_GREEN = "#145A32"
    private const val POSITIVE_GREEN = "#0A7C3F"
    private const val NEUTRAL_BG = "#EEF2F7"
    private const val NEUTRAL_TEXT = "#41506A"
    private const val CARD_RADIUS_DP = 20f

    // TK-INSTRUCTION (2026-07-25): a popup must NOT always be green. Popups
    // that warn get a warning colour — RED where the action is serious or
    // cannot be undone (delete, delete-forever, reject, restore-cloud-data,
    // final confirmation), YELLOW where it is only a "please look before you
    // tap" caution (duplicate number, sending a request, paying more than the
    // bill, cancelling an expected visit). Everything else stays green.
    private const val HEADER_RED = "#B42318"
    private const val POSITIVE_RED = "#B42318"
    private const val HEADER_YELLOW = "#E8A100"
    private const val YELLOW_TEXT = "#3A2600"   // dark text: readable on yellow

    private const val SEV_GREEN = "piles_sev_green"
    private const val SEV_YELLOW = "piles_sev_yellow"
    private const val SEV_RED = "piles_sev_red"

    /** Decides green / yellow / red from what the popup is about. */
    private fun severityOf(title: CharSequence): String {
        val t = title.toString()
        val low = t.lowercase()
        // Checked FIRST: these only SEND a request / are reversible, so even
        // when the word "delete" is in them they are a caution, not a danger.
        val cautionFirst = low.contains("request") || t.contains("অনুরোধ")
        if (cautionFirst) return SEV_YELLOW
        val red = listOf(
            "delete", "delete forever", "permanently", "reject", "remove",
            "final confirmation", "restore cloud", "sure?"
        ).any { low.contains(it) } || t.contains("মুছ") || t.contains("ধ্বংস")
        if (red) return SEV_RED
        val yellow = listOf(
            // 🟡🔒 V814 (২৮.০৮.২০২৬, TK-নির্দেশ, ছবিসহ: *"সবুজ কালারের বদলে
            //    হলুদ কালার দিলে ভালো হয় যাতে সহজেই বুঝতে পারি"*) —
            //    "already paid"/"duplicate" এখন সতর্কতার হলুদ, সবুজ নয়।
            "already paid", "duplicate",
            "already exists", "already in the system", "confirm", "logout",
            "restore", "problem", "crash", "error", "warning", "overpay"
        ).any { low.contains(it) } ||
            t.contains("বেশি হয়ে যাচ্ছে") || t.contains("বাতিল") ||
            t.startsWith("⚠")
        if (yellow) return SEV_YELLOW
        return SEV_GREEN
    }

    /** Picks a small icon from what the popup is about, so the header never
     *  looks bare. If the title already begins with its own emoji (several
     *  do), that emoji is kept and nothing is added. */
    private fun iconFor(title: CharSequence): String {
        val t = title.toString()
        // Titles that ALREADY start with their own emoji keep it (e.g. "💊 Type",
        // "⚠️ Restore cloud data?"). Checked by real emoji ranges only, so a
        // Bengali title (e.g. "আসার কথা বাতিল") is NOT mistaken for an emoji.
        if (t.isNotBlank()) {
            val c0 = t[0]
            val emojiStart = c0.isHighSurrogate() ||
                c0.code in 0x2190..0x21FF ||
                c0.code in 0x2600..0x27BF ||
                c0.code in 0x2B00..0x2BFF
            if (emojiStart) return ""
        }
        val low = t.lowercase()
        return when {
            low.contains("password") -> "🔑"
            low.contains("delete") || t.contains("মুছ") -> "🗑️"
            low.contains("restore") -> "♻️"
            low.contains("logout") -> "🚪"
            low.contains("photo") -> "📷"
            low.contains("remark") || low.contains("note") -> "📝"
            low.contains("payment") || low.contains("income") || t.contains("পেমেন্ট") -> "💰"
            low.contains("arrived") || low.contains("approve") || low.contains("confirm") -> "✅"
            // 🟡🔒 V814 — "already paid"-এর আইকন ⚠️, নইলে "payment" ধরে 💰 বসত
            //    আর একই দিনে দ্বিতীয়বার টাকা নেওয়ার সতর্কতাটা চোখেই পড়ত না।
            low.contains("already paid") || low.contains("duplicate") -> "⚠️"
            low.contains("crash") || low.contains("error") || low.contains("problem") -> "⚠️"
            low.contains("branch") || low.contains("select") -> "📋"
            low.contains("call") -> "📞"
            low.contains("document") -> "📄"
            else -> "ℹ️"
        }
    }

    /** The coloured header strip used in place of the plain popup title.
     *  Green = normal, Yellow = caution, Red = serious/undoable (TK's rule).
     *  Its top corners are rounded to the same radius as the card below, so
     *  the two line up exactly. The chosen severity is stored on the view's
     *  tag so paint() below can colour the buttons to match. */
    fun header(ctx: Context, title: CharSequence): View {
        val d = ctx.resources.displayMetrics.density
        val icon = iconFor(title)
        val sev = severityOf(title)
        // 🎨 TK-APPROVED (2026-08-06, দল ১ · পথ ক): হেডার এখন গ্র্যাডিয়েন্ট সবুজ
        // (গাঢ়→উজ্জ্বল); জরুরি হলে লাল/হলুদ গ্র্যাডিয়েন্ট। এক জায়গায় বদল বলে
        // প্রজেক্টের সব PremiumAlert পপ-আপের হেডার একসাথে গ্র্যাডিয়েন্ট হয়।
        val gradColors = when (sev) {
            SEV_RED -> intArrayOf(Color.parseColor("#8A1810"), Color.parseColor("#C43325"))
            SEV_YELLOW -> intArrayOf(Color.parseColor("#C98A00"), Color.parseColor("#F0B520"))
            else -> intArrayOf(Color.parseColor("#0B5E34"), Color.parseColor("#1F9D55"))
        }
        val fg = if (sev == SEV_YELLOW) YELLOW_TEXT else "#FFFFFF"
        return TextView(ctx).apply {
            tag = sev
            text = if (icon.isEmpty()) title else "$icon  $title"
            textSize = 16.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(fg))
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradColors).apply {
                cornerRadii = floatArrayOf(
                    CARD_RADIUS_DP * d, CARD_RADIUS_DP * d,   // top-left
                    CARD_RADIUS_DP * d, CARD_RADIUS_DP * d,   // top-right
                    0f, 0f,                                    // bottom-right
                    0f, 0f                                     // bottom-left
                )
            }
        }
    }

    /** Finds the severity tag put on the header view above, by walking the
     *  popup's own view tree. Uses no hidden Android ids — if nothing is
     *  found (a popup with no title) it simply reports green. */
    private fun severityInside(v: View?): String {
        if (v == null) return SEV_GREEN
        (v.tag as? String)?.let { if (it.startsWith("piles_sev_")) return it }
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) {
                val found = severityInside(v.getChildAt(i))
                if (found != SEV_GREEN) return found
            }
        }
        return SEV_GREEN
    }

    /** Called right after show(): rounds the card and colours the buttons.
     *  Every step is null-safe, so a popup with no buttons (or no window)
     *  simply keeps its normal look instead of failing. */
    fun paint(dialog: AlertDialog) {
        try {
            // 🔒 খাতার সারি B158 (TK, 30.07.2026): যে স্টাফের ফোনে বাংলা বন্ধ,
            // তাঁর জন্য এই পপ-আপের লেখাও বাংলা-মুক্ত করা হয়। পপ-আপের নিজের
            // আলাদা উইন্ডো, তাই পর্দার পাহারা এখানে পৌঁছায় না — এই এক লাইনেই
            // প্রজেক্টের সব PremiumAlert পপ-আপ ঢেকে যায়।
            // ⛔ বাংলা বন্ধ না থাকলে এটা কিছুই করে না।
            try { NoBengali.installDialog(dialog) } catch (_: Throwable) { }
            // 🔴🔒 V752 (TK-রিপোর্ট ছবিসহ: "এরকম যেন সাজেস্ট না করে") — পপ-আপের
            //    নিজের আলাদা উইন্ডো, তাই পর্দার Autofill-পাহারা এখানে পৌঁছায় না।
            //    এই এক লাইনেই প্রজেক্টের সব PremiumAlert পপ-আপ ঢেকে যায়
            //    (ঠিক উপরের NoBengali-র প্রমাণিত পথেই)।
            try { NoAutofill.scrubDialogWindow(dialog.window) } catch (_: Throwable) { }
            val ctx = dialog.context
            val d = ctx.resources.displayMetrics.density
            dialog.window?.setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = CARD_RADIUS_DP * d
            })
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { b ->
                val sev = severityInside(dialog.window?.decorView)
                val btnBg = when (sev) {
                    SEV_RED -> POSITIVE_RED
                    SEV_YELLOW -> HEADER_YELLOW
                    else -> POSITIVE_GREEN
                }
                b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(btnBg))
                b.setTextColor(Color.parseColor(if (sev == SEV_YELLOW) YELLOW_TEXT else "#FFFFFF"))
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { b ->
                b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(NEUTRAL_BG))
                b.setTextColor(Color.parseColor(NEUTRAL_TEXT))
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.let { b ->
                b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(NEUTRAL_BG))
                b.setTextColor(Color.parseColor(NEUTRAL_TEXT))
            }
            // 🎨 TK-APPROVED (2026-08-06, দল ১ · পথ ক): অপশন-লিস্ট পপ-আপের
            // (setItems / single-choice) সারির মাঝে হালকা দাগ + একটু ফাঁক —
            // সব ৪৫টায় একসাথে, কোনো স্ক্রিনে হাত না দিয়ে। সারির লেখা theme
            // থেকে গাঢ় (textColorAlertDialogListItem)। null-safe — লিস্ট না
            // থাকলে (সাধারণ পপ-আপ) কিছুই হয় না।
            dialog.listView?.let { lv ->
                try {
                    lv.divider = GradientDrawable().apply { setColor(Color.parseColor("#EEF1F5")) }
                    lv.dividerHeight = (1 * d).toInt()
                    lv.setPadding(lv.paddingLeft, (4 * d).toInt(), lv.paddingRight, (4 * d).toInt())
                } catch (_: Throwable) { }
            }
        } catch (e: Exception) {
            // Painting must never be able to break a working popup.
        }
    }

    /** Two popups in LoginActivity ("Forgot Password") are built with the
     *  older framework android.app.AlertDialog instead of the AndroidX one.
     *  That is a different class, so it needs its own overload — the paint
     *  work is identical. */
    fun paint(dialog: android.app.AlertDialog) {
        try {
            // 🔒 খাতার সারি B158 (TK, 30.07.2026): যে স্টাফের ফোনে বাংলা বন্ধ,
            // তাঁর জন্য এই পপ-আপের লেখাও বাংলা-মুক্ত করা হয়। পপ-আপের নিজের
            // আলাদা উইন্ডো, তাই পর্দার পাহারা এখানে পৌঁছায় না — এই এক লাইনেই
            // প্রজেক্টের সব PremiumAlert পপ-আপ ঢেকে যায়।
            // ⛔ বাংলা বন্ধ না থাকলে এটা কিছুই করে না।
            try { NoBengali.installDialog(dialog) } catch (_: Throwable) { }
            // 🔴🔒 V752 (TK-রিপোর্ট ছবিসহ: "এরকম যেন সাজেস্ট না করে") — পপ-আপের
            //    নিজের আলাদা উইন্ডো, তাই পর্দার Autofill-পাহারা এখানে পৌঁছায় না।
            //    এই এক লাইনেই প্রজেক্টের সব PremiumAlert পপ-আপ ঢেকে যায়
            //    (ঠিক উপরের NoBengali-র প্রমাণিত পথেই)।
            try { NoAutofill.scrubDialogWindow(dialog.window) } catch (_: Throwable) { }
            val d = dialog.context.resources.displayMetrics.density
            dialog.window?.setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = CARD_RADIUS_DP * d
            })
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.let { b ->
                val sev = severityInside(dialog.window?.decorView)
                val btnBg = when (sev) {
                    SEV_RED -> POSITIVE_RED
                    SEV_YELLOW -> HEADER_YELLOW
                    else -> POSITIVE_GREEN
                }
                b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(btnBg))
                b.setTextColor(Color.parseColor(if (sev == SEV_YELLOW) YELLOW_TEXT else "#FFFFFF"))
            }
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE)?.let { b ->
                b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(NEUTRAL_BG))
                b.setTextColor(Color.parseColor(NEUTRAL_TEXT))
            }
            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL)?.let { b ->
                b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(NEUTRAL_BG))
                b.setTextColor(Color.parseColor(NEUTRAL_TEXT))
            }
        } catch (e: Exception) {
            // Painting must never be able to break a working popup.
        }
    }
}
