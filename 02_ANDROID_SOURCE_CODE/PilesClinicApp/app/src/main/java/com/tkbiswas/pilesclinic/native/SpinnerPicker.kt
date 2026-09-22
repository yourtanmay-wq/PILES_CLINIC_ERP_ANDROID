package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.view.MotionEvent
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * TK-REPORTED (2026-07-28, photo proof): "লিস্ট যখন ওপেন করা হয় মনে হচ্ছে একটা
 * আরেকটার গায়ে এসে গেছে" -- Android's own Spinner drop-down opens ON TOP of the
 * field it belongs to, so the list and the form text sit over each other while
 * it fades in. TK approved (proof 2) a clean centred popup instead.
 *
 * This attaches such a popup to an ordinary Spinner. The Spinner, its adapter,
 * its positions and its selectedItem are completely unchanged -- the popup only
 * calls setSelection(), exactly like tapping a row in the old drop-down did, so
 * every saved value stays byte-for-byte the same.
 *
 * [hidePlaceholder] leaves the first row ("Select Branch" / "Choose
 * Occupation") out of the list, matching what TK locked on 2026-07-27: the
 * placeholder belongs in the closed box only, never inside the opened list.
 *
 * [tapsToUnlock] keeps the existing 3-tap Branch lock for Staff and Doctor;
 * with the default of 1 the popup opens on a single tap.
 */
object SpinnerPicker {

    private const val WINDOW_MS = 1200L

    fun attach(
        sp: Spinner,
        title: String,
        hidePlaceholder: Boolean = false,
        tapsToUnlock: Int = 1,
        lockLabel: String = ""
    ) {
        var taps = 0
        var last = 0L
        sp.setOnTouchListener { v, e ->
            if (e.actionMasked == MotionEvent.ACTION_UP) {
                if (tapsToUnlock <= 1) {
                    show(sp, title, hidePlaceholder)
                } else {
                    val now = System.currentTimeMillis()
                    if (now - last > WINDOW_MS) taps = 0
                    last = now
                    taps++
                    if (taps >= tapsToUnlock) {
                        taps = 0
                        show(sp, title, hidePlaceholder)
                    } else {
                        Toast.makeText(
                            v.context,
                            "Tap 3 times to change " + lockLabel.ifBlank { title },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            // Swallow the touch so the old overlapping drop-down never opens.
            true
        }
    }

    /**
     * 🔵🔒 V540 (২২.০৮.২০২৬, TK-নির্দেশ: *"gradation — প্রফেশনাল লুক তৈরি করুন"*)
     *
     * এতদিন এই প্রিমিয়াম পিকারটা **শুধু Spinner-এ চাপ দিলেই** খুলত। CHECK-UP
     * পর্দায় Occupation ও Grade-এর Spinner এখন **লুকানো** (V539) — সেখানে চাপ
     * দেওয়া যায় না, তাই আমি নিজে দুটো সাদামাটা তালিকা বানিয়েছিলাম, যেগুলো
     * প্রজেক্টের বাকি পর্দার সঙ্গে মেলেনি।
     *
     * ⇒ এখন **এই প্রমাণিত পিকারটাই** সরাসরি খোলা যায় — একই প্রিমিয়াম হেডার,
     *   একই গোল-বোতামের তালিকা, একই রং (`PremiumAlert`)। **নতুন কোনো নকশা
     *   বানানো হয়নি।**
     * ⛔ নিচের `show()` এক অক্ষরও বদলায়নি; পুরোনো `attach()`-ও অপরিবর্তিত।
     */
    fun open(sp: Spinner, title: String, hidePlaceholder: Boolean = false) =
        show(sp, title, hidePlaceholder)

    private fun show(sp: Spinner, title: String, hidePlaceholder: Boolean) {
        val adapter = sp.adapter ?: return
        val ctx = sp.context
        if (ctx is Activity && (ctx.isFinishing || ctx.isDestroyed)) return

        val firstRow = if (hidePlaceholder && adapter.count > 0) 1 else 0
        if (adapter.count <= firstRow) return

        val labels = ArrayList<CharSequence>()
        val positions = ArrayList<Int>()
        for (i in firstRow until adapter.count) {
            labels.add(adapter.getItem(i)?.toString()?.uppercase() ?: "")
            positions.add(i)
        }

        val checked = positions.indexOf(sp.selectedItemPosition)
        AlertDialog.Builder(ctx)
            .setCustomTitle(PremiumAlert.header(ctx, title))
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                val target = positions.getOrNull(which)
                if (target != null) sp.setSelection(target)
                dialog.dismiss()
            }
            .show().also { PremiumAlert.paint(it) }
    }
}
