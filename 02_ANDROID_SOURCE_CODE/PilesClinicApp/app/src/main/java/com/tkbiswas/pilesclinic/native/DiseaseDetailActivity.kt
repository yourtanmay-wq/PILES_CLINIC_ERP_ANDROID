package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.R

/**
 * Educational disease-detail screen. Opened from the Public Site treatment cards.
 * All Bangla / Hindi / English text comes VERBATIM from DiseaseCatalog (ported from
 * the website's diseaseData), so nothing is hand-written here — no spelling drift.
 */
class DiseaseDetailActivity : AppCompatActivity() {

    private var currentKey = "Piles"
    private var currentLang = "bn" // bn | hi | en

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        setContentView(R.layout.activity_disease_detail)

        currentKey = intent.getStringExtra("disease") ?: "Piles"

        findViewById<TextView>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<TextView>(R.id.langBn).setOnClickListener { currentLang = "bn"; render() }
        findViewById<TextView>(R.id.langHi).setOnClickListener { currentLang = "hi"; render() }
        findViewById<TextView>(R.id.langEn).setOnClickListener { currentLang = "en"; render() }

        buildChips()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildChips() {
        val c = findViewById<LinearLayout>(R.id.chipContainer)
        c.removeAllViews()
        DiseaseCatalog.list.forEach { d ->
            val chip = TextView(this).apply {
                text = d.nameEn
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.rightMargin = dp(8)
                layoutParams = lp
                setOnClickListener { currentKey = d.key; render() }
            }
            c.addView(chip)
        }
    }

    private fun render() {
        val d = DiseaseCatalog.byKey(currentKey)

        // language toggle highlight
        highlightLang()
        // chip highlight
        val c = findViewById<LinearLayout>(R.id.chipContainer)
        for (i in 0 until c.childCount) {
            val chip = c.getChildAt(i) as TextView
            val on = chip.text == DiseaseCatalog.list[i].nameEn && DiseaseCatalog.list[i].key == currentKey
            chip.setBackgroundColor(if (on) 0xFF0B7A3A.toInt() else 0xFFFFFFFF.toInt())
            chip.setTextColor(if (on) 0xFFFFFFFF.toInt() else 0xFF0B3B66.toInt())
        }

        findViewById<TextView>(R.id.tvTitle).text = when (currentLang) {
            "hi" -> d.nameHi; "en" -> d.nameEn; else -> d.nameBn
        }
        findViewById<TextView>(R.id.tvDesc).text = when (currentLang) {
            "hi" -> d.descHi; "en" -> d.descEn; else -> d.descBn
        }

        val sc = findViewById<LinearLayout>(R.id.symptomsContainer)
        sc.removeAllViews()
        d.symptoms.forEach { s ->
            sc.addView(TextView(this).apply {
                text = "•  $s"
                textSize = 13f
                setTextColor(0xFF37485C.toInt())
                setPadding(0, dp(3), 0, dp(3))
            })
        }
        findViewById<TextView>(R.id.tvCause).text = d.cause
        findViewById<TextView>(R.id.tvTreat).text = d.treat
    }

    private fun highlightLang() {
        val map = mapOf("bn" to R.id.langBn, "hi" to R.id.langHi, "en" to R.id.langEn)
        map.forEach { (lang, id) ->
            val tv = findViewById<TextView>(id)
            val on = lang == currentLang
            tv.setBackgroundColor(if (on) 0xFF0B7A3A.toInt() else 0xFFFFFFFF.toInt())
            tv.setTextColor(if (on) 0xFFFFFFFF.toInt() else 0xFF5A6B7D.toInt())
        }
    }
}
