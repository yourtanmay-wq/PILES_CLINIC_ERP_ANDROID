package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText

/**
 * TK-DECISION (2026-07-22): ONE app-wide standard for form validation, so the
 * same behaviour applies everywhere without re-specifying it per screen.
 *
 * When the user misses/mis-fills a mandatory field, that EXACT field turns
 * red and the cursor jumps to it -- never a single generic "everything
 * mandatory" message on the wrong field. The red box clears the moment the
 * user starts fixing that field.
 *
 * Purely visual + focus: no field, save, or flow logic is changed. Any screen
 * calls FieldError.mark(view) for a bad field, or FieldError.validate(list)
 * to check several fields at once (marks all bad ones, focuses the first,
 * returns its message).
 */
object FieldError {

    private val RED = Color.parseColor("#E23B3B")
    private val RED_BG = Color.parseColor("#FFF4F4")

    private data class Orig(val bg: Drawable?, val l: Int, val t: Int, val r: Int, val b: Int)
    private val originals = java.util.WeakHashMap<View, Orig>()

    /** Turn a field's box red. Remembers its original background + padding the
     *  first time, and (for an EditText) auto-clears the red the moment the
     *  user edits it. Safe to call repeatedly. */
    fun mark(v: View) {
        if (!originals.containsKey(v)) {
            originals[v] = Orig(v.background, v.paddingLeft, v.paddingTop, v.paddingRight, v.paddingBottom)
            if (v is EditText) {
                v.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) { clear(v) }
                })
            }
        }
        val o = originals[v]!!
        val d = v.resources.displayMetrics.density
        v.background = GradientDrawable().apply {
            cornerRadius = 12f * d
            setColor(RED_BG)
            setStroke((1.7f * d).toInt(), RED)
        }
        v.setPadding(o.l, o.t, o.r, o.b)
    }

    /** Restore a field to its original look. */
    fun clear(v: View) {
        val o = originals[v] ?: return
        v.background = o.bg
        v.setPadding(o.l, o.t, o.r, o.b)
    }

    /** Check several fields at once. Each Triple is (view, isValid, message).
     *  Marks every invalid field red, clears valid ones, moves the cursor to
     *  the FIRST invalid field, and returns that field's message (or null if
     *  all valid, meaning: go ahead and save). */
    fun validate(checks: List<Triple<View, Boolean, String>>): String? {
        var firstMsg: String? = null
        var firstBad: View? = null
        for ((v, ok, msg) in checks) {
            if (ok) {
                clear(v)
            } else {
                mark(v)
                if (firstMsg == null) { firstMsg = msg; firstBad = v }
            }
        }
        firstBad?.let { v ->
            v.requestFocus()
            if (v is EditText) v.setSelection(v.text?.length ?: 0)
        }
        // 🔒🔒 খাতার সারি B181 (TK, 30.07.2026 — "সম্পূর্ণ প্রজেক্ট ব্যবহার
        // করার সময় কেন এখনো অনেক জায়গায় বাংলা আসছে?")। **আসল কারণ:** এই
        // ফাংশনের ফেরত-দেওয়া বার্তাটা (যেমন "Title দিন", "Patient নাম দিন")
        // সরাসরি `Toast.makeText()`-এ যেত — Toast-এর নিজের আলাদা উইন্ডো
        // থাকে বলে পর্দার সাধারণ পাহারা (`NoBengali.install()`) সেখানে
        // পৌঁছায় না, আর এই একটাই ফাংশন **প্রজেক্টের বহু জায়গায়** ব্যবহার হয়
        // (BriefingActivity, PatientTimelineActivity, EnquiryActivity...) —
        // তাই একই ফাঁক অনেকবার দেখা যেত। **এখানে, একটাই জায়গায়, `NoBengali.
        // s()` দিয়ে মুড়ে দেওয়া হলো** — যাঁর ফোনে বাংলা বন্ধ নয়, তাঁর জন্য
        // `NoBengali.s()` কিছুই বদলায় না (আগের মতোই বাংলা দেখেন)।
        // ⛔ `firstMsg` যদি `null` হয় (মানে যাচাই পাশ করেছে), সেটা `null`ই
        // থাকতে হবে — `NoBengali.s(null)` খালি স্ট্রিং ("") ফেরায়, `null` নয়।
        // সব ব্যবহারকারী `if (vmsg != null)` দিয়ে চেক করে; `""` ফেরত গেলে
        // সেটাও `!= null` সত্যি হয়ে যেত, তাই **সফল সেভও ভুল করে আটকে যেত।**
        // তাই শুধু সত্যিকারের বার্তা থাকলেই মোড়ানো হচ্ছে।
        return firstMsg?.let { NoBengali.s(it) }
    }
}
