package com.tkbiswas.pilesclinic.clinical

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/**
 * TK APPROVED (2026-07-15): shared premium "choose medicines" dialog for both
 * Prescription (Ayurvedic) and Medicine Slip (Allopathic). Same structure and
 * behaviour for both, only the accent colour + list type differ:
 *   - Default (no search) list is EXACTLY the existing reference list — never
 *     changed, never guessed at, nothing added to it automatically.
 *   - Typing in the search box also searches previously "learned" names (ones
 *     TK/staff actually typed and added before) — these never show before a
 *     search, only when searched for.
 *   - If the typed name has never been seen before, an "Add '<name>' as new"
 *     row appears; tapping it adds that exact typed name (no invented names)
 *     and remembers it for future searches only (not the default list).
 *   - A always-visible bottom bar shows the selected count + Cancel/Add,
 *     removing the need to scroll down to save.
 */
object MedicinePickerDialog {

    data class Accent(val main: String, val light: String, val border: String)

    val GREEN_AYURVEDIC = Accent(main = "#0B4F2A", light = "#EAFBF0", border = "#BFE9CE")
    val BLUE_ALLOPATHIC = Accent(main = "#1067D8", light = "#EAF1FB", border = "#BFD6F2")

    private fun dp(activity: Activity, v: Int): Int = (v * activity.resources.displayMetrics.density).toInt()

    private fun rounded(activity: Activity, colorHex: String, strokeHex: String?, radiusDp: Int, strokeWidthDp: Int = 2): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(activity, radiusDp).toFloat()
            setColor(Color.parseColor(colorHex))
            if (strokeHex != null) setStroke(dp(activity, strokeWidthDp), Color.parseColor(strokeHex))
        }

    /**
     * @param baseList the unchanged, existing reference list (commonMedicines or slipMedicines)
     * @param listType  "ayurvedic" or "allopathic" — used for the learned-name memory bucket
     * @param showExtraFields when true, also shows Frequency + Days inline edit fields
     *   under each selected medicine (used by Prescription's direct-add flow, TK approved
     *   2026-07-15, so nothing is lost by skipping the old separate review screen).
     *   Medicine Slip keeps its original Dose-only picker (showExtraFields stays false there).
     */
    /** TK-DECISION (2026-07-22): pick the medicine Type from a list (all 12
     *  dosage forms). Used both for the first-time set (single tap) and for a
     *  3-tap change. The caller remembers the choice forever per medicine. */
    private fun showTypePicker(activity: Activity, name: String, current: String, onPicked: (String) -> Unit) {
        val types = ClinicalRepository.MEDICINE_TYPES.toTypedArray()
        AlertDialog.Builder(activity)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(activity, "💊 Type — $name"))
            .setItems(types) { _, which -> onPicked(types[which]) }
            .setNegativeButton("Cancel", null)
            .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }

    fun showPicker(
        activity: Activity,
        listType: String,
        baseList: List<String>,
        title: String,
        subtitle: String,
        accent: Accent,
        showExtraFields: Boolean = false,
        // TK FIX (2026-07-15): optional override for where picked medicines are
        // added. Existing callers (Patient card Prescription/Medicine Slip)
        // don't pass this, so they keep working exactly as before, writing to
        // ClinicalRepository.currentPrescription/currentSlip. Walk-in (Print
        // Center, no real patient session) passes its own private list here so
        // it can reuse this exact same screen without ever touching a real
        // patient's in-progress prescription.
        targetList: MutableList<MedicineEntry>? = null,
        // TK-DECISION (2026-07-22): optional "⭐ Common" chip beside the search
        // box. When a caller supplies this, tapping the chip ticks the caller's
        // saved common-medicine set right here (each with its own remembered
        // dose/type/days). Callers that don't pass it (e.g. Print Center) simply
        // don't show the chip. This replaces the separate "Apply Common
        // Prescription" button that used to sit on the Prescription screen.
        commonProvider: (() -> Set<String>)? = null,
        // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B38):
        // *"যেখান থেকে ঢুকেছিলাম, ব্যাক করলে আবার সেখানেই থাকতে হবে। মাঝপথে এই
        // দ্বিতীয় পর্দাটা কোথা থেকে আসছে? এটা আসা বন্ধ করুন।"*
        // কিছু না বেছে বেরিয়ে গেলে ডাকা পর্দা যেন নিজেকেও বন্ধ করে দিতে পারে,
        // সেজন্য এই ঐচ্ছিক খবরটা। যারা এটা পাঠায় না, তাদের কিছুই বদলায় না।
        onCancelled: (() -> Unit)? = null,
        /* 🔵 V488 (20.08.2026, TK-নির্দেশ): নিচের বারে "WhatsApp" বোতাম। এটা
           **ঐচ্ছিক** — যে পর্দা এটা পাঠায় না (Medicine Slip · Print Center …)
           তাদের বার আগের মতোই দুটো বোতামেই থাকে, কিছুই বদলায় না।
           যে পাঠায়, তার জন্য: বাছা ওষুধ আগে commit হয়, তারপর এই কাজটা ডাকা হয়। */
        onWhatsApp: (() -> Unit)? = null,
        onAdded: () -> Unit
    ) {
        val d = activity.resources.displayMetrics.density
        fun px(v: Int) = dp(activity, v)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        // ---- TK-DECISION (2026-07-22): full-screen (not a floating pop-up).
        // Header shows the PATIENT (no "Add from Reference List" title) so staff
        // can't make a prescription/slip for the wrong patient. Below it, three
        // equal boxes: ⭐ Common | 🔍 Search | ＋ Add. ----
        val patName = RoleSession.currentPatientName.trim()
        val hasPatient = patName.isNotBlank()
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(14), px(14), px(14), px(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#0B2B59"), Color.parseColor("#123F34"))
            )
        }
        val backArrow = TextView(activity).apply {
            text = "←"; textSize = 20f; setTextColor(Color.parseColor("#CDD8E6"))
            setPadding(0, 0, px(12), 0); isClickable = true; isFocusable = true
        }
        header.addView(backArrow)
        val headerInfo = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        if (hasPatient) {
            headerInfo.addView(TextView(activity).apply {
                text = "👤  $patName"; textSize = 16f; setTextColor(Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            // 🔒🔒 খাতার সারি B175 (TK, 30.07.2026 — ছবি-প্রুফে সরাসরি এই লাইনটাই
            // দাগ দিয়ে দেখিয়েছেন: "pat_9711468691 · Piles")। **আসল কারণ:**
            // এই লাইনটা raw `currentPatientId` পড়ত, অথচ মানুষ-পড়া-যায় কোড
            // (`currentPatientDisplayId`, যেমন "KNE-30072026-001") পাঠানোর
            // ব্যবস্থা RoleSession-এ আগে থেকেই ছিল — শুধু এই পর্দাটাই সেটা
            // পড়ত না। এখন `displayId()` (থাকলে সেটাই, নইলে raw — আগের মতো)।
            val sub = listOf(
                RoleSession.displayId(),
                RoleSession.currentPatientDisease,
                RoleSession.currentPatientAddress
            ).map { it.trim() }.filter { it.isNotBlank() }.joinToString("  ·  ")
            /* 🔵 V488 (20.08.2026, TK-নির্দেশ): *"উপরে রোগের নামটা একটু বোল্ড হোক"*।
               লাইনটা "ID · রোগ · ঠিকানা" — শুধু **রোগের অংশটুকু** মোটা ও একটু
               সাদা করা হল, বাকি লেখা আগের মতোই। ⛔ কোনো তথ্য যোগ/বাদ যায়নি। */
            val diseaseTxt = RoleSession.currentPatientDisease.trim()
            val subStyled: CharSequence = if (diseaseTxt.isNotBlank() && sub.contains(diseaseTxt)) {
                android.text.SpannableString(sub).apply {
                    val at = sub.indexOf(diseaseTxt)
                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        at, at + diseaseTxt.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        android.text.style.ForegroundColorSpan(Color.parseColor("#FFFFFF")),
                        at, at + diseaseTxt.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else sub
            if (sub.isNotBlank()) headerInfo.addView(TextView(activity).apply {
                text = subStyled; textSize = 11f; setTextColor(Color.parseColor("#C3D1E4"))
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = px(2); layoutParams = p
            })
        } else {
            headerInfo.addView(TextView(activity).apply {
                text = title; textSize = 16f; setTextColor(Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            headerInfo.addView(TextView(activity).apply {
                text = subtitle; textSize = 11f; setTextColor(Color.parseColor("#C3D1E4"))
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = px(2); layoutParams = p
            })
        }
        header.addView(headerInfo)
        val docTag = subtitle.substringAfterLast("·").trim()
        if (docTag.isNotBlank()) header.addView(TextView(activity).apply {
            text = docTag; textSize = 10f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0B2B59"))
            background = rounded(activity, "#EAF7EF", "#CFE6D8", 20)
            setPadding(px(9), px(4), px(9), px(4))
        })
        root.addView(header)

        // Owner-approved compact Prescription-only options. Medicine Slip and
        // every other caller skip this block, so their working flow is untouched.
        var dietInput: EditText? = null
        // 🖥️🔵 B669 (১৫.০৮.২০২৬, TK-অনুমোদিত প্রুফ "খ"): কার্ডটা এখন আর সরাসরি `root`-এ
        //   বসে না — নিচে ScrollView-এর ভিতরে বসে, তাই স্ক্রোল করলে উপরে উঠে যায়।
        var topCard: LinearLayout? = null
        if (showExtraFields && listType == "ayurvedic" && hasPatient) {
            val chosen = PrescriptionOptionsStore.selectedFields(activity)
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(px(12), px(9), px(12), px(9))
                background = rounded(activity, "#FFFFFF", "#D8E0E9", 14, 1)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(px(12), px(10), px(12), 0)
                }
            }
            val first = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            /* 🔵 V488 (20.08.2026, TK-নির্দেশ): আগে `isEnabled = false` ছিল — টিক
               সবসময় বসানো থাকত, তোলার কোনো উপায়ই ছিল না। TK-এর সিদ্ধান্ত: তোলা
               যাবে, আর তুললে ছাপা কাগজ থেকেও "ADVICE: Sitz Bath — 2 Times Daily"
               লাইনটা উঠে যাবে।
               ⛔ ডিফল্ট আগের মতোই টিক-দেওয়া (store-এর default true) — কেউ হাত না
                  দিলে কাগজ হুবহু আগের মতোই ছাপে। রোগী ধরে ধরে মনে রাখা হয়। */
            first.addView(CheckBox(activity).apply {
                text = "Sitz Bath — 2 Times"
                isChecked = PrescriptionOptionsStore.sitzBath(activity)
                isEnabled = true
                textSize = 12f
                setTextColor(Color.parseColor("#10223A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnCheckedChangeListener { _, checked ->
                    PrescriptionOptionsStore.saveSitzBath(activity, checked)
                }
            })
            first.addView(TextView(activity).apply { text = "Diet"; textSize = 12f; setPadding(px(6), 0, px(6), 0) })
            dietInput = EditText(activity).apply {
                hint = "Optional"; textSize = 12f; minHeight = 0; setSingleLine(false); maxLines = 2
                setText(PrescriptionOptionsStore.diet(activity))
                background = rounded(activity, "#FFFFFF", "#D8E0E9", 9, 1)
                setPadding(px(9), px(6), px(9), px(6))
                layoutParams = LinearLayout.LayoutParams(px(145), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            first.addView(dietInput); card.addView(first)

            val grid = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            fun optionRow(a: Pair<String,String>, b: Pair<String,String>): LinearLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                listOf(a,b).forEach { item ->
                    addView(CheckBox(activity).apply {
                        text = item.second; textSize = 11.5f; isChecked = item.first in chosen
                        setTextColor(Color.parseColor("#10223A"))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setOnCheckedChangeListener { _, checked ->
                            if (checked) chosen.add(item.first) else chosen.remove(item.first)
                            PrescriptionOptionsStore.saveSelectedFields(activity, chosen)
                        }
                    })
                }
            }
            grid.addView(optionRow("disease" to "Disease Name", "symptoms" to "Symptoms"))
            grid.addView(optionRow("since" to "Since When", "complaint" to "Chief Complaint"))
            card.addView(grid)
            card.addView(TextView(activity).apply {
                text = "+  Choose More Information"; gravity = Gravity.CENTER; textSize = 11.5f
                setTextColor(Color.parseColor(accent.main)); background = rounded(activity, "#FFFFFF", accent.main, 9, 1)
                setPadding(px(8), px(7), px(8), px(7)); isClickable = true
                setOnClickListener {
                    val keys = PrescriptionOptionsStore.moreFields.keys.toList()
                    val labels = PrescriptionOptionsStore.moreFields.values.toTypedArray()
                    val checks = BooleanArray(keys.size) { keys[it] in chosen }
                    AlertDialog.Builder(activity).setTitle("Choose More Information")
                        .setMultiChoiceItems(labels, checks) { _, i, checked -> if (checked) chosen.add(keys[i]) else chosen.remove(keys[i]) }
                        .setPositiveButton("Apply") { _, _ -> PrescriptionOptionsStore.saveSelectedFields(activity, chosen) }
                        .setNegativeButton("Cancel", null).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                }
            })
            card.addView(TextView(activity).apply {
                text = ""; layoutParams = LinearLayout.LayoutParams(1, 1)
            })
            dietInput?.setOnFocusChangeListener { _, focused -> if (!focused) PrescriptionOptionsStore.saveDiet(activity, dietInput?.text?.toString().orEmpty()) }
            topCard = card          // 🔵 B669: root-এ নয় — নিচে scroll-এর ভিতরে
        }

        // ---- Three equal boxes: Common | Search | Add ----
        // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B37):
        // *"Common Search Add — এই তিনটে বোতাম একই সাইজের হতে হবে, পাশাপাশি
        // থাকতে হবে, একই সারিতে। কোনটা নিচে কোনটা উপরে কেন?"*
        //
        // **আসল কারণ:** তিনটেরই মাপ এক দেওয়া ছিল, কিন্তু —
        //  • সারিটার নিজের কোনো খাড়া-বরাবর মিল (gravity) বসানো ছিল না,
        //  • মাঝেরটা `EditText`, যার নিজের ভিতরের মাপ ও প্যাডিং আলাদা,
        //  • আর ওটার লেখা উপরের দিকে বসানো ছিল (CENTER_VERTICAL)।
        // তাই মাঝের বাক্সটা একটু উপরে উঠে ছোট দেখাত।
        //
        // এখন তিনটেই এক উচ্চতা · এক চওড়া · এক ফাঁক · এক বরাবর।
        val arow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(12), px(6), px(12), px(4))   // 🔵 V488: উপরের ফাঁক ১২→৬
        }
        /* 🔵 V488 (20.08.2026, TK-নির্দেশ): *"Common Search Add — এই তিনটে বোতামের
           উচ্চতা কমাতে হবে, প্রয়োজনে আইকন রাখা যাবে না।"*
           কেন এত উঁচু ছিল: লেখা **দু-লাইনে** বসানো ছিল (`"⭐\nCommon"`) — আইকন
           উপরে, নাম নিচে। তাই ৫২dp লাগত। এখন এক লাইনে (আইকন লেখার পাশেই),
           উচ্চতা ৫২ → ৩৬dp (৩১% কম) — ওষুধের তালিকা তত বেশি দেখা যায়।
           ⛔ তিনটে এখনো এক মাপ · এক ফাঁক · এক বরাবর (উপরের B37 নিয়ম অটুট)। */
        val boxH = px(36)
        val gap = px(8)
        fun boxParams(withStartGap: Boolean) =
            LinearLayout.LayoutParams(0, boxH, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
                if (withStartGap) marginStart = gap
            }
        val commonChip: TextView? = if (commonProvider != null) TextView(activity).apply {
            text = "⭐ Common"; textSize = 12.5f; gravity = Gravity.CENTER   // 🔵 V488: এক লাইনে
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor(accent.main))
            background = rounded(activity, accent.light, accent.main, 12)
            isClickable = true; isFocusable = true
            includeFontPadding = false
            setPadding(px(4), 0, px(4), 0)
            layoutParams = boxParams(false)
        } else null
        val searchBox = EditText(activity).apply {
            hint = "\uD83D\uDD0D Medicine"   /* 🔵 V545 (২২.০৮.২০২৬, TK-নির্দেশ: "মেডিসিন বাংলায় লেখা থাকবে না, ওটা ইংরেজিতে হবে")। ⛔ NoBengali-র পুরোনো ম্যাপিং অক্ষত। */
            textSize = 13f
            gravity = Gravity.CENTER          // ঠিক মাঝখানে, বাকি দুটোর মতোই
            background = rounded(activity, "#F5F7FA", "#D3DBE6", 12)
            includeFontPadding = false
            minHeight = 0; minimumHeight = 0  // EditText-এর নিজের সর্বনিম্ন উচ্চতা যেন মাপ না বদলায়
            setPadding(px(10), 0, px(10), 0)
            layoutParams = boxParams(commonChip != null)
        }
        val btnOutside = TextView(activity).apply {
            text = "＋ Add"; textSize = 12.5f; gravity = Gravity.CENTER   // 🔵 V488: এক লাইনে
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#9A6A12"))
            background = rounded(activity, "#FBF1DD", "#C98A1E", 12)
            isClickable = true; isFocusable = true
            includeFontPadding = false
            setPadding(px(4), 0, px(4), 0)
            layoutParams = boxParams(true)
        }
        if (commonChip != null) arow.addView(commonChip)
        arow.addView(searchBox)
        arow.addView(btnOutside)
        // 🔵 B669: এই তিনটে বোতামও (Common · Search · Add) এখন scroll-এর ভিতরে

        // ---- Scrollable rows (fills the rest of the full screen) ----
        val rowsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(14), px(8), px(14), px(10))
        }
        /**
         * 🖥️🔵 B669 (১৫.০৮.২০২৬) — TK-এর রিপোর্ট (ছবিসহ): *"স্ক্রোল যখন করা হচ্ছে
         * উপরের সবগুলি স্টিল কেন থাকছে"* — পর্দার প্রায় অর্ধেক উপরের কার্ড ও তিনটে
         * বোতামের দখলে থাকায় একসাথে মাত্র ~৫টা ওষুধ দেখা যেত।
         * TK-অনুমোদিত প্রুফ **"খ"**: কার্ড **ও** তিনটে বোতাম — দুটোই তালিকার সঙ্গে
         * উপরে উঠে যাবে। মাপা হিসাব: ওষুধের জায়গা **1236px → 2004px (+৬২%)**,
         * অর্থাৎ ~৫টার বদলে ~৯টা ওষুধ একসাথে।
         *
         * ⚠️ কেন আলাদা `scrollInner` মোড়ক লাগল (গুরুত্বপূর্ণ):
         *   `rebuildRows()` প্রতিবার খোঁজার সময় `rowsContainer.removeAllViews()` করে
         *   (লাইন 634)। কার্ড/বোতাম সোজা `rowsContainer`-এ বসালে **খুঁজতে গেলেই ওগুলো
         *   মুছে যেত**। তাই মোড়কের ভিতরে তিনটে আলাদা অংশ — মোছা হয় শুধু তালিকাটাই।
         * ⛔ হেডার (রোগীর নাম) ও নিচের বার (0 selected · Cancel · Save) আগের মতোই স্থির।
         * ⛔ কার্ড/বোতাম/সারির নিজস্ব ডিজাইন · মাপ · রং — এক অক্ষরও বদলায়নি,
         *    শুধু কে কার ভিতরে বসবে সেটুকু বদলেছে।
         */
        val scrollInner = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        topCard?.let { scrollInner.addView(it) }
        scrollInner.addView(arow)
        scrollInner.addView(rowsContainer)
        val scroll = ScrollView(activity).apply {
            addView(scrollInner)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)

        /* ═══════════════════════════════════════════════════════════════════
           🔵 V488 (20.08.2026, TK-নির্দেশ): নিচের বার নতুন সাজে —
           **WhatsApp · Cancel · Save — তিনটে পাশাপাশি, উচ্চতায় যতটা কম সম্ভব।**

           আগে ছিল: "0 selected" (পুরো বাকি জায়গা নিত) · Cancel · Save।
           TK-এর বাছাই (খ): "0 selected" লেখাটা তুলে দিয়ে সংখ্যাটা **Save বোতামের
           ভিতরেই** — অর্থাৎ দুটো ওষুধ বাছলে বোতামে লেখা উঠবে "Save (2)"।
           তাতে তিনটে বোতামের জন্য পুরো চওড়া জায়গা পাওয়া যায়।

           উচ্চতা: উপর-নিচের ফাঁক ১৪ → ৮dp, বোতামের নিজের উচ্চতা ৪২dp বাঁধা।
           ⛔ Cancel/Save-এর কাজ · রং · আচরণ — এক অক্ষরও বদলায়নি।
           ⛔ WhatsApp বোতাম শুধু তখনই দেখায় যখন ডাকা পর্দা সেটা চেয়েছে
              (onWhatsApp দিয়েছে) — Medicine Slip ও অন্য সব পর্দা অপরিবর্তিত।
           ═══════════════════════════════════════════════════════════════ */
        val bottomBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(12), px(8), px(12), px(8))
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams = p
        }
        val barBtnH = px(42)
        fun barParams(withStartGap: Boolean) =
            LinearLayout.LayoutParams(0, barBtnH, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
                if (withStartGap) marginStart = px(8)
            }
        val btnWhatsApp: TextView? = if (onWhatsApp != null) TextView(activity).apply {
            text = "WhatsApp"; textSize = 14f; gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#FFFFFF"))
            background = rounded(activity, "#1FA855", null, 12)
            includeFontPadding = false
            isClickable = true; isFocusable = true
            setPadding(px(4), 0, px(4), 0)
            layoutParams = barParams(false)
        } else null
        val btnCancel = TextView(activity).apply {
            text = "Cancel"; textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#5B6B81"))
            background = rounded(activity, "#F5F7FA", "#E1E6ED", 12)
            includeFontPadding = false
            setPadding(px(4), 0, px(4), 0)
            layoutParams = barParams(btnWhatsApp != null)
        }
        val btnAdd = TextView(activity).apply {
            text = "Save"; textSize = 14f; gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(activity, accent.main, null, 12)
            includeFontPadding = false
            setPadding(px(4), 0, px(4), 0)
            layoutParams = barParams(true)
        }
        btnWhatsApp?.let { bottomBar.addView(it) }
        bottomBar.addView(btnCancel); bottomBar.addView(btnAdd)
        root.addView(bottomBar)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(activity).setView(root).setCancelable(true).create()
        // খাতার সারি B38: কিছু যোগ না করে বেরিয়ে গেলে ডাকা পর্দাকে জানানো হয়।
        var anythingAdded = false
        dialog.setOnDismissListener {
            if (!anythingAdded) try { onCancelled?.invoke() } catch (_: Throwable) { }
        }
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.WHITE))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        backArrow.setOnClickListener { dialog.dismiss() }

        // name -> dose, insertion order preserved
        val selected = LinkedHashMap<String, String>()
        // only populated/used when showExtraFields is true (Prescription direct-add flow)
        val selectedFreq = LinkedHashMap<String, String>()
        val selectedDays = LinkedHashMap<String, String>()
        val selectedNote = LinkedHashMap<String, String>()
        // TK APPROVED (2026-07-15): medicine Type (Tab/Cap/Syp/...) — tap to cycle,
        // remembered per medicine name forever via ClinicalRepository.rememberRxType.
        val selectedType = LinkedHashMap<String, String>()

        fun refreshCount() {
            // 🔵 V488 (TK-বাছাই "খ"): আলাদা "0 selected" লেখাটা আর নেই — সংখ্যাটা
            // এখন Save বোতামের ভিতরেই ("Save (2)"), তাই তিনটে বোতামের জায়গা হয়।
            btnAdd.text = if (selected.isEmpty()) "Save" else "Save (${selected.size})"
        }

        fun buildRow(name: String): LinearLayout {
            val isChecked = selected.containsKey(name)
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(px(14), px(12), px(14), px(12))
                background = if (isChecked) rounded(activity, accent.light, accent.main, 14)
                             else rounded(activity, "#FFFFFF", "#E1E6ED", 14)
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.bottomMargin = px(10); layoutParams = p
            }
            val nameRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val circle = TextView(activity).apply {
                text = if (isChecked) "✓" else ""
                textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                background = if (isChecked) rounded(activity, accent.main, null, 20)
                             else rounded(activity, "#FFFFFF", "#B7C0CE", 20)
                layoutParams = LinearLayout.LayoutParams(px(28), px(28))
            }
            // TK-DECISION (2026-07-22): compact 2-line card, SAME for Prescription
            // AND Medicine Slip (this one shared picker). Line 1 = ✓  [Type]  Name
            // with a clear gap on each side of the Type badge. The Type is picked
            // once from a list, remembered forever per medicine (rememberRxType),
            // and changing it later needs a 3-tap safety. Dose + duration (days)
            // move to line 2; Frequency and Instructions fields are removed.
            val curType = (selectedType[name]?.ifBlank { null }) ?: ClinicalRepository.rxTypeFor(name)
            val typeBadge = TextView(activity).apply {
                text = curType.ifBlank { "Type" }
                textSize = 12f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = rounded(activity, if (curType.isBlank()) "#8A97A8" else accent.main, null, 6)
                setPadding(px(9), px(3), px(9), px(3))
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.marginStart = px(10); p.marginEnd = px(10); layoutParams = p
                isClickable = true; isFocusable = true
            }
            val openTypePicker = {
                showTypePicker(activity, name, curType) { picked ->
                    selectedType[name] = picked
                    rebuildRows(activity, rowsContainer, baseList, listType, searchBox.text.toString(), selected, accent, ::buildRow)
                }
            }
            if (curType.isBlank()) typeBadge.setOnClickListener { openTypePicker() }
            else com.tkbiswas.pilesclinic.native.TripleTapEdit.attach(typeBadge) { openTypePicker() }
            val nameView = TextView(activity).apply {
                text = name; textSize = 16f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(if (isChecked) Color.parseColor(accent.main) else Color.parseColor("#10223A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            nameRow.addView(circle); nameRow.addView(typeBadge); nameRow.addView(nameView)
            card.addView(nameRow)

            val doseRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                visibility = if (isChecked) View.VISIBLE else View.GONE
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = px(8); layoutParams = p
                background = rounded(activity, "#FFFFFF", accent.border, 8)
                setPadding(px(10), px(6), px(10), px(6))
            }
            // 🔴 V425 (TK-নির্দেশ ১৭.০৮.২০২৬): *"Dose এর আগে icon থাকবে না"* ⇒ 💊 তোলা হলো।
            doseRow.addView(TextView(activity).apply {
                text = "Dose:"; textSize = 13f; setTextColor(Color.parseColor("#5B6B81"))
                // 🔴 V425 (TK-নির্দেশ): *"Dose লেখার পরে একটু ব্যবধান রাখুন"*
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = px(8) }
            })
            // 🔴 V425 (TK-নির্দেশ ১৭.০৮.২০২৬: *"When এর যায়গা টা নেই কেন"*) —
            //   ছাপা কাগজে DOSE · WHEN · DURATION তিনটে আলাদা ঘর, অথচ এই তালিকায়
            //   **When লেখার কোনো জায়গাই ছিল না** — তাই ছাপায় WHEN কলামটা ফাঁকা
            //   পড়ে থাকত (একমাত্র যে ওষুধের সেভ করা ডোজের ভিতরেই "After Food"
            //   লেখা ছিল, সেটাতেই দেখাত)। এখন Dose-এর পাশেই When-এর ঘর।
            //   ⛔ সেভ করার নিয়ম আগেরটাই — `selectedFreq` আগে থেকেই ছিল, কেবল
            //      ভরার উপায় ছিল না; তাই পুরনো কোনো হিসাব/ডিফল্ট বদলায়নি।
            // 🍯 V956: Dose ও When এখন আলাদা ঘর থেকে আসে, তাই "After Food with
            //   Honey"-এর মতো লেখা হুবহু ফিরে আসে (আগে ভেঙে যেত)।
            val liveEdit = selected[name]
            val (preDose, preWhen) = if (liveEdit != null) {
                val split = ClinicalRepository.splitDoseAndFrequency(liveEdit)
                Pair(split.first, split.second.ifBlank { ClinicalRepository.rxWhenFor(name) })
            } else ClinicalRepository.rxDoseWhenFor(name)
            val doseInput = EditText(activity).apply {
                setText(preDose)
                textSize = 13f; background = null; setPadding(0, 0, 0, 0)
                // 🔴 V425 (TK-নির্দেশ): *"2- 0- 2 এই গুলির মধ্যে একটু ব্যবধান রাখুন"*
                //   ⛔ লেখাটা বদলানো হয়নি — সেভ হওয়া মান হুবহু "2-0-2"-ই থাকে,
                //      শুধু দেখতে অক্ষরগুলোর মাঝে একটু ফাঁক পড়ে। তাই ছাপা কাগজ,
                //      পুরনো হিসাব বা ডিফল্ট — কিছুই বদলায় না।
                try { letterSpacing = 0.12f } catch (_: Throwable) { }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            doseRow.addView(doseInput)
            doseRow.addView(TextView(activity).apply {
                text = "When:"; textSize = 13f; setTextColor(Color.parseColor("#5B6B81"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginStart = px(10); it.marginEnd = px(8) }
            })
            val whenInput = EditText(activity).apply {
                setText(selectedFreq[name]?.ifBlank { preWhen } ?: preWhen)
                textSize = 13f; background = null; setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            doseRow.addView(whenInput)
            whenInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (selected.containsKey(name)) selectedFreq[name] = s?.toString().orEmpty()
                }
            })
            val daysInput = EditText(activity).apply {
                setText(selectedDays[name] ?: ClinicalRepository.rxDaysFor(name))
                textSize = 13f; gravity = Gravity.CENTER
                background = rounded(activity, "#F1F6FB", accent.border, 7)
                setPadding(px(10), px(4), px(10), px(4))
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.marginStart = px(8); minWidth = px(64); layoutParams = p
            }
            doseRow.addView(daysInput)
            card.addView(doseRow)
            daysInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (selected.containsKey(name)) selectedDays[name] = s?.toString().orEmpty()
                }
            })

            nameRow.setOnClickListener {
                if (selected.containsKey(name)) {
                    selected.remove(name)
                    selectedFreq.remove(name)
                    selectedDays.remove(name)
                    selectedNote.remove(name)
                    selectedType.remove(name)
                } else {
                    selected[name] = doseInput.text.toString().trim().ifBlank { ClinicalRepository.rxDoseWhenFor(name).first }  /* 🍯 V956: শুধু ডোজটুকু — জোড়া লেখা (মধু/মাখনসহ When) যেন ডোজের ঘরে না ঢোকে */
                    selectedType[name] = ClinicalRepository.rxTypeFor(name)
                    if (showExtraFields) {
                        // V425: টিক দিলে When-এর ঘরে সেভ করা ডিফল্টটাই বসে।
                        selectedFreq[name] = selectedFreq[name]?.ifBlank { preWhen } ?: preWhen
                        selectedDays[name] = selectedDays[name] ?: ClinicalRepository.rxDaysFor(name)
                        selectedNote[name] = selectedNote[name] ?: ""
                    }
                }
                refreshCount()
                rebuildRows(activity, rowsContainer, baseList, listType, searchBox.text.toString(), selected, accent, ::buildRow)
            }
            doseInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (selected.containsKey(name)) selected[name] = s?.toString().orEmpty()
                }
            })
            return card
        }

        fun rebuild() {
            rebuildRows(activity, rowsContainer, baseList, listType, searchBox.text.toString(), selected, accent, ::buildRow)
        }

        /* ════════════════════════════════════════════════════════════════════
           🔵 V546 (২২.০৮.২০২৬, TK-নির্দেশ ফটো-প্রুফসহ):
           *"যখন মেডিসিন সেখানে কিছু লিখব তখন উপরের এত কিছু বিস্তারিত থাকার তো কোনো
             দরকার নেই / যাতে মেডিসিনের নাম ভালোভাবে টাইপ করা যায় তখন শুধুমাত্র
             সেটুকুই থাকবে / সেখান থেকে ব্যাক করলে আবার এই স্ক্রিনে আসবে"*

           নিয়ম (হুবহু TK-এর কথা মতো):
             • খোঁজার ঘরে **কিছু লেখা থাকলে** উপরের কার্ডটা (Sitz Bath · Diet ·
               Disease Name · Symptoms · Since When · Chief Complaint ·
               "+ Choose More Information") লুকিয়ে যায় — খোঁজার তিনটে বাক্স ও
               ওষুধের তালিকা থাকে।
             • ঘর ফাঁকা হলেই কার্ডটা **হুবহু আগের অবস্থায়** ফিরে আসে।
             • **ব্যাক** (ফোনের বোতাম বা উপরের ←) চাপলে আগে খোঁজাটুকু মুছে এই
               পুরো পর্দাতেই ফেরে; পর্দা বন্ধ হয় তার পরের ব্যাক-এ।

           ⛔ কেন লেখা-ভিত্তিক, focus-ভিত্তিক নয়: পর্দা খোলার সময় EditText নিজেই
              focus নিলে কার্ডটা না-চাইতেই লুকিয়ে যেত। লেখা থাকলে-লুকাও
              নিয়মে সেই ঝুঁকি নেই।
           ⛔ কার্ডের কোনো টিক/লেখা হারায় না — টিক সঙ্গে সঙ্গেই PrescriptionOptionsStore-এ
              জমা হয়, আর Diet-এর লেখা GONE হলেও EditText-এর ভিতরেই থাকে
              (Save-এর সময় আগের মতোই dietInput পড়া হয়)।
           ⛔ Medicine Slip ও অন্য সব ডাক: ওখানে topCard = null, তাই এক বিন্দুও
              বদলায়নি — ব্যাক আগের মতোই সরাসরি বন্ধ করে।
           ═════════════════════════════════════════════════════════════ */
        fun applySearchOnlyMode() {
            val card = topCard ?: return
            val want = if (searchBox.text.toString().isNotEmpty()) View.GONE else View.VISIBLE
            if (card.visibility != want) card.visibility = want
        }
        /** ব্যাক-এ আগে খোঁজাটুকু মোছা হয়; মোছার কিছু না থাকলে false — তখন আগের মতোই বন্ধ। */
        fun leaveSearchOnlyMode(): Boolean {
            if (topCard == null) return false
            if (searchBox.text.toString().isEmpty()) return false
            searchBox.setText("")            // watcher → applySearchOnlyMode() + rebuild()
            searchBox.clearFocus()
            try {
                (activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager)
                    ?.hideSoftInputFromWindow(searchBox.windowToken, 0)
            } catch (_: Throwable) { }
            return true
        }
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { applySearchOnlyMode(); rebuild() }
        })
        if (topCard != null) {
            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    event.action == android.view.KeyEvent.ACTION_UP) leaveSearchOnlyMode() else false
            }
            backArrow.setOnClickListener { if (!leaveSearchOnlyMode()) dialog.dismiss() }
        }

        // TK-DECISION (2026-07-22): "⭐ Common" ticks the caller's saved common
        // set right here — each medicine with its own remembered dose/type/days,
        // skipping any already selected. Replaces the old separate Prescription
        // button. Names not in the reference list still get added on Save (same
        // as any selected item), even if not shown as a visible row.
        commonChip?.setOnClickListener {
            val names = if (showExtraFields && listType == "ayurvedic")
                ClinicalRepository.fixedCommonPrescription else (commonProvider?.invoke() ?: emptySet())
            if (names.isEmpty()) {
                Toast.makeText(activity, "No common set saved yet — add medicines and Save once first.", Toast.LENGTH_SHORT).show()
            } else {
                names.forEach { nm ->
                    if (!selected.containsKey(nm)) {
                        selected[nm] = ClinicalRepository.rxDoseWhenFor(nm).first  /* 🍯 V956: শুধু ডোজটুকু — জোড়া লেখা (মধু/মাখনসহ When) যেন ডোজের ঘরে না ঢোকে */
                        selectedType[nm] = ClinicalRepository.rxTypeFor(nm)
                        if (showExtraFields) selectedDays[nm] = ClinicalRepository.rxDaysFor(nm)
                    }
                }
                refreshCount()
                rebuild()
                if (showExtraFields && listType == "ayurvedic") {
                    PrescriptionOptionsStore.saveDiet(activity, dietInput?.text?.toString().orEmpty())
                    // Common is the owner's one-tap prescription: commit, save
                    // and open print through the caller's existing proven path.
                    btnAdd.post { btnAdd.performClick() }
                }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        // 🔒🔒 খাতার সারি B176 (TK, 30.07.2026 — "লিস্ট থেকে মেডিসিন চুস করার
        // পর যদি আলাদা কিছু টাইপ করে লিখতে চাই, তখন প্রেসক্রিপশনে সেগুলো যোগ
        // হচ্ছে না, শুধুমাত্র যেটা টাইপ করলাম সেটাই যোগ হচ্ছে")।
        //
        // **আসল কারণ (কোড ধরে, আন্দাজ নয়):** চেকবক্স-তালিকার বাছাই (`selected`)
        // ততক্ষণ `currentPrescription`-এ লেখা হত **না**, যতক্ষণ না এই পর্দার
        // নিজের **"Save"** বোতাম চাপা হত। কিন্তু "＋ Add" (বাইরের ওষুধ) নিজের
        // মতো **সঙ্গে সঙ্গেই** লিখে ফেলত — দুটো ভিন্ন "কমিট" নিয়ম একই পর্দায়।
        // তাই স্টাফ বক্স টিক দিয়ে "＋ Add"-এ গিয়ে একটা ওষুধ টাইপ করে "Add"
        // চাপলে (এটাই সম্পূর্ণ কাজ মনে হয়), আসলে চেকবক্সের বাছাইগুলো তখনও
        // অপেক্ষমাণ — বাইরের এই পর্দার নিজের "Save" চাপা না হলে সেগুলো কখনো
        // লেখা হয়ই না। **সমাধান:** "＋ Add" চাপার **সঙ্গে সঙ্গেই**, বাইরের
        // ডায়ালগ খোলার আগে, বর্তমানে টিক দেওয়া সবকিছু আগে থেকেই commit করে
        // ফেলা হচ্ছে — তাই স্টাফ এরপর যা-ই করুন (Save চাপুন বা না চাপুন),
        // তালিকার বাছাই আর হারায় না। ⛔ Type (Tab/Cap/...) না বসানো থাকলে
        // আগের মতোই আটকানো হয় (কোনো নীরব ভুল বসানো হয় না)।
        fun commitSelectedToList(): Boolean {
            if (selected.isEmpty()) return true
            val missingType = selected.keys.filter { selectedType[it].isNullOrBlank() }
            if (missingType.isNotEmpty()) {
                Toast.makeText(activity, "Please set Type (Tab/Cap/Syp/...) for: ${missingType.joinToString(", ")}", Toast.LENGTH_LONG).show()
                return false
            }
            selected.forEach { (name, dose) ->
                val finalDose = dose.trim().ifBlank { ClinicalRepository.rxDoseWhenFor(name).first }  /* 🍯 V956: শুধু ডোজটুকু — জোড়া লেখা (মধু/মাখনসহ When) যেন ডোজের ঘরে না ঢোকে */
                val finalType = selectedType[name].orEmpty()
                val finalDays = if (showExtraFields) (selectedDays[name]?.trim()?.ifBlank { ClinicalRepository.rxDaysFor(name) } ?: ClinicalRepository.rxDaysFor(name)) else ClinicalRepository.DEFAULT_DURATION
                val effectiveList = targetList ?: (if (listType == "allopathic") ClinicalRepository.currentSlip else ClinicalRepository.currentPrescription)
                // TK-REPORTED BUG FIX (2026-07-22): a medicine NAME must appear
                // only ONCE on a prescription/slip. Adding the same name again
                // (across re-opens, or via "⭐ Common") used to append a second
                // row. Now, if the name is already on the list, update that
                // existing entry instead of adding a duplicate.
                val typedFreq = if (showExtraFields) selectedFreq[name].orEmpty() else ""
                val (dosePart, autoFreqSaved) = if (typedFreq.isBlank())
                    ClinicalRepository.splitDoseAndFrequency(finalDose) else Pair(finalDose, "")
                // 🔵 V548: সেভের সময়ও একই নিয়ম — When যেন ফাঁকা না ছাপে
                // 🍯 V956: আগে মনে-রাখা When (মধু/মাখনসহ) অগ্রাধিকার পায়।
                val autoFreq = autoFreqSaved.ifBlank { ClinicalRepository.rxDoseWhenFor(name).second }
                ClinicalRepository.rememberPermanentDefault(
                    activity.applicationContext,
                    name,
                    finalType,
                    dosePart,
                    typedFreq.ifBlank { autoFreq },
                    finalDays
                )
                val entry = MedicineEntry(
                    name = name,
                    dosage = dosePart,
                    frequency = typedFreq.ifBlank { autoFreq },
                    duration = finalDays,
                    instructions = if (showExtraFields) selectedNote[name].orEmpty() else "",
                    medicineType = finalType
                )
                val dupIdx = effectiveList.indexOfFirst { it.name.trim().equals(name.trim(), ignoreCase = true) }
                if (dupIdx >= 0) effectiveList[dupIdx] = entry else effectiveList.add(entry)
            }
            anythingAdded = true
            return true
        }

        btnAdd.setOnClickListener {
            if (showExtraFields && listType == "ayurvedic")
                PrescriptionOptionsStore.saveDiet(activity, dietInput?.text?.toString().orEmpty())
            val ok = commitSelectedToList()
            if (!ok) return@setOnClickListener
            if (selected.isNotEmpty()) onAdded()
            dialog.dismiss()
        }
        /* 🔵 V488 (20.08.2026, TK-নির্দেশ): WhatsApp বোতাম। Save-এর হুবহু একই
           প্রস্তুতি (Diet সেভ + বাছা ওষুধ commit) — শুধু শেষে ছাপার পর্দার বদলে
           WhatsApp-এ PDF পাঠানোর কাজটা ডাকা হয়। ⛔ Save-এর নিজের পথ অটুট। */
        btnWhatsApp?.setOnClickListener {
            if (showExtraFields && listType == "ayurvedic")
                PrescriptionOptionsStore.saveDiet(activity, dietInput?.text?.toString().orEmpty())
            if (!commitSelectedToList()) return@setOnClickListener
            val effective = targetList
                ?: (if (listType == "allopathic") ClinicalRepository.currentSlip else ClinicalRepository.currentPrescription)
            if (effective.isEmpty()) {
                Toast.makeText(activity, "Add at least one medicine first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            onWhatsApp?.invoke()
        }
        btnOutside.setOnClickListener {
            // 🔒 খাতার সারি B176: বাইরের ওষুধ যোগ করার ডায়ালগ খোলার **আগেই**
            // এতক্ষণ টিক দেওয়া সবকিছু commit করে ফেলা হচ্ছে — তাই এরপর যেই
            // ওষুধটা টাইপ করে "Add" চাপা হোক, সেটা তালিকার বাছাইয়ের **উপরে
            // যোগ** হবে, তার জায়গা দখল করবে না।
            if (!commitSelectedToList()) return@setOnClickListener
            // "＋ Add" দিয়ে বাইরের ওষুধ যোগ করা হলেও সেটা যোগ করাই — তখন ডাকা
            // পর্দাকে বন্ধ করার খবর পাঠানো যাবে না।
            showOutsideDialog(activity, listType, accent, targetList) { anythingAdded = true; onAdded() }
        }

        rebuild()
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        // Same screen/design: if a newer permanent default exists (for example
        // after reinstall or a change on another phone), quietly repaint only
        // the existing rows when the small cloud refresh completes.
        MedicineDefaultsCloudRepository.refreshIfNeeded(activity.applicationContext) {
            if (dialog.isShowing) rebuild()
        }
    }

    /** Rebuilds the visible row list: base list (unchanged) when search is
     *  empty, else base+learned names matching the query, always keeping any
     *  already-selected medicine visible so a choice is never hidden. */
    private fun rebuildRows(
        activity: Activity,
        container: LinearLayout,
        baseList: List<String>,
        listType: String,
        query: String,
        selected: LinkedHashMap<String, String>,
        accent: Accent,
        buildRow: (String) -> LinearLayout
    ) {
        container.removeAllViews()
        val trimmed = query.trim()
        val pool = ClinicalRepository.searchableMedicines(listType)
        val display = LinkedHashSet<String>()
        if (trimmed.isBlank()) {
            display.addAll(baseList)
        } else {
            display.addAll(pool.filter { it.contains(trimmed, ignoreCase = true) })
        }
        // never hide an already-selected medicine, even if it no longer matches the query
        display.addAll(selected.keys)

        display.forEach { name -> container.addView(buildRow(name)) }

        val exactMatch = pool.any { it.equals(trimmed, ignoreCase = true) }
        if (trimmed.isNotEmpty() && !exactMatch) {
            val d = activity.resources.displayMetrics.density
            fun px(v: Int) = (v * d).toInt()
            val addNewCard = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(14), px(14), px(14), px(14))
                background = GradientDrawable().apply {
                    cornerRadius = px(14).toFloat()
                    setColor(Color.parseColor("#FFF8E8"))
                    setStroke(px(2), Color.parseColor("#E9C879"))
                }
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.bottomMargin = px(10); layoutParams = p
            }
            addNewCard.addView(TextView(activity).apply {
                text = "➕  Add \"$trimmed\" as new medicine"
                textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#8A6116"))
            })
            addNewCard.setOnClickListener {
                ClinicalRepository.learnMedicine(trimmed, listType)
                selected[trimmed] = ClinicalRepository.rxDoseWhenFor(trimmed).first  /* 🍯 V956: শুধু ডোজটুকু — জোড়া লেখা (মধু/মাখনসহ When) যেন ডোজের ঘরে না ঢোকে */
                rebuildRows(activity, container, baseList, listType, query, selected, accent, buildRow)
            }
            container.addView(addNewCard)
        }
    }

    /** The detailed "outside" dialog (name/dose/frequency/days/instruction),
     *  shared by both Prescription and Medicine Slip. Also remembers the name
     *  for future searches (never added to the default list). */
    fun showOutsideDialog(
        activity: Activity,
        listType: String,
        accent: Accent,
        targetListOverride: MutableList<MedicineEntry>? = null,
        onAdded: () -> Unit
    ) {
        val d = activity.resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(16), px(16), 0)
        }
        fun field(hintText: String): EditText = EditText(activity).apply {
            hint = hintText
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(px(16), px(12), px(16), px(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(8) }
        }
        val name = field("Medicine name")
        /* 🔵🔒 V545 (২২.০৮.২০২৬, TK-নির্দেশ) — *"dose when days — এগুলো তিনটে
           কলমে থাকবে না, একটা কলমে থাকবে, প্রথম ফটোর মতন।"*
           ⇒ তালিকার কার্ডে যে সারিটা আগে থেকেই চলছে (`Dose: … When: … [৫ days]`)
             **হুবহু সেই চেহারাই** এখানে — একই রং, একই মাপ, একই ধরন।
           ⛔ ঘর তিনটে **মোছা হয়নি** — শুধু এক সারিতে বসেছে। নিচের সেভের
              কোড (`dose`/`frequency`/`days` পড়া) **এক অক্ষরও বদলায়নি**। */
        val dose = EditText(activity).apply {
            hint = "Dose"; textSize = 13f; background = null; setPadding(0, 0, 0, 0)
            try { letterSpacing = 0.12f } catch (_: Throwable) { }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val frequency = EditText(activity).apply {
            hint = "When"; textSize = 13f; background = null; setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val days = EditText(activity).apply {
            hint = "Days"; textSize = 13f; gravity = Gravity.CENTER
            background = rounded(activity, "#F1F6FB", accent.border, 7)
            setPadding(px(10), px(4), px(10), px(4))
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.marginStart = px(8); minWidth = px(64); layoutParams = p
        }
        fun tinyLabel(t: String, startPx: Int) = TextView(activity).apply {
            text = t; textSize = 13f; setTextColor(Color.parseColor("#5B6B81"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = startPx; it.marginEnd = px(8) }
        }
        val doseRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(activity, "#FFFFFF", accent.border, 8)
            setPadding(px(10), px(6), px(10), px(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(8) }
            addView(tinyLabel("Dose:", 0))
            addView(dose)
            addView(tinyLabel("When:", px(10)))
            addView(frequency)
            addView(days)
        }
        val note = field("Instruction (optional)")

        // TK APPROVED (2026-07-15): Type (Tab/Cap/Syp/...) chip — tap to cycle,
        // same remember-forever behaviour as the reference-list picker.
        var currentType = ""
        val typeChip = TextView(activity).apply {
            text = "Type: Tap to set"; textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(activity, accent.main, null, 8)
            setPadding(px(14), px(10), px(14), px(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(8) }
            isClickable = true; isFocusable = true
            setOnClickListener {
                val types = ClinicalRepository.MEDICINE_TYPES
                val nextIdx = (types.indexOf(currentType) + 1).let { if (it >= types.size) 0 else it }
                currentType = types[nextIdx]
                text = "Type: $currentType"
            }
        }
        box.addView(typeChip)
        box.addView(name)
        box.addView(doseRow)
        box.addView(note)
        // 🟢🔒🔒 V662 (২৫.০৮.২০২৬, TK-নির্দেশ, রেগে গিয়ে রিপোর্ট — "একটা মেডিসিন
        // Add করার পরে আরেকটা লেখার অপশন নেই কেন... তিনি চাইলে দশটা মেডিসিনের
        // জন্য লিখতে পারে") — কতগুলো যোগ হলো, সেটা দেখানোর ছোট লেখা।
        val addedLabel = TextView(activity).apply {
            text = ""; textSize = 12.5f
            setTextColor(Color.parseColor("#0A7C3F"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            visibility = android.view.View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(8) }
        }
        box.addView(addedLabel)

        /* 🔵🔒 V545 (TK-নির্দেশ) — *"কোন মেডিসিনের নাম যখন টাইপ করে লিখব,
           ডোজের ঘর অটো ডিফল্ট থাকবে তো? আমি যদি চাই সেই ক্ষেত্রেই পরিবর্তন
           হবে। তাছাড়া কত দিনের জন্য সেটাও ডিফল্ট ওখানে লেখা থাকবে।"*

           নাম লেখা হলে **প্রজেক্টের নিজের মনে-রাখা ডিফল্টই** বসে —
           `rxDoseFor` · `splitDoseAndFrequency` · `rxDaysFor` · `rxTypeFor`
           (তালিকার কার্ডে ঠিক এগুলোই ব্যবহার হয়, নতুন কিছু বানানো হয়নি)।

           ⛔ **স্টাফ নিজে একবার কিছু লিখলে সেই ঘরে আর কখনো হাত পড়ে না** —
              নিচের `touched*` পাহারা। তাই টাইপ করা লেখা মুছে যাওয়ার ভয় নেই।
           ⛔ ডিফল্ট না জানা থাকলে ঘর ফাঁকাই থাকে — আগের মতোই। */
        var touchedDose = false; var touchedWhen = false; var touchedDays = false
        var fillingDefaults = false
        fun watch(e: EditText, mark: () -> Unit) {
            e.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { if (!fillingDefaults) mark() }
            })
        }
        watch(dose) { touchedDose = true }
        watch(frequency) { touchedWhen = true }
        watch(days) { touchedDays = true }

        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val nm = s?.toString()?.trim().orEmpty()
                if (nm.isEmpty()) return
                fillingDefaults = true
                try {
                    // 🍯 V956: Dose ও When নিজের নিজের ঘর থেকে — মধু/মাখনের লেখাও টেকে।
                    val (dDose, dWhen) = ClinicalRepository.rxDoseWhenFor(nm)
                    if (!touchedDose) dose.setText(dDose)
                    if (!touchedWhen) frequency.setText(dWhen)
                    if (!touchedDays) days.setText(ClinicalRepository.rxDaysFor(nm))
                    if (currentType.isBlank()) {
                        val t = ClinicalRepository.rxTypeFor(nm)
                        if (t.isNotBlank()) { currentType = t; typeChip.text = "Type: $t" }
                    }
                } catch (_: Throwable) { }
                fillingDefaults = false
            }
        })

        UppercaseInputUtil.applyToAll(box)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(activity)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(activity, "💊 Add Medicine (Outside List)"))
            .setView(box)
            .setPositiveButton("Add", null)
            // 🟢🔒 V662 (২৫.০৮.২০২৬) — "Cancel" থেকে "Done"-এ বদলানো হলো,
            // কারণ এখন প্রতিটা "Add"-এই সাথে সাথে তালিকায় বসে যায় (আগের
            // মতো পুরো পপ-আপ বাতিল করার কিছু নেই) — এই বোতাম এখন শুধু
            // বন্ধ করে। ⛔ id/আচরণ (বন্ধ করা) এক অক্ষরও বদলায়নি, শুধু লেখা।
            .setNegativeButton("Done", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val medName = name.text.toString().trim()
                if (medName.isBlank()) { name.error = "Medicine name required"; name.requestFocus(); return@setOnClickListener }
                // TK FIX (2026-07-15): same mandatory Type rule as the list picker.
                if (currentType.isBlank()) {
                    Toast.makeText(activity, "Please tap Type chip and set Tab/Cap/Syp/...", Toast.LENGTH_LONG).show()
                    typeChip.requestFocus()
                    return@setOnClickListener
                }
                val medDose = dose.text.toString().trim().ifBlank { ClinicalRepository.rxDoseWhenFor(medName).first }  /* 🍯 V956: শুধু ডোজটুকু — জোড়া লেখা (মধু/মাখনসহ When) যেন ডোজের ঘরে না ঢোকে */
                ClinicalRepository.learnMedicine(medName, listType)
                val finalDays = days.text.toString().trim().ifBlank { ClinicalRepository.rxDaysFor(medName) }
                val targetList = targetListOverride ?: (if (listType == "allopathic") ClinicalRepository.currentSlip else ClinicalRepository.currentPrescription)
                // TK-REPORTED BUG FIX (2026-07-16): if the When/frequency field
                // was left blank, split the auto-filled dose so WHEN isn't empty.
                val typedFreq = frequency.text.toString().trim()
                val (dosePart, autoFreqSaved) = if (typedFreq.isBlank())
                    ClinicalRepository.splitDoseAndFrequency(medDose) else Pair(medDose, "")
                // 🍯 V956: When ফাঁকা রাখলে ওষুধটার **মনে-রাখা** When-ই বসে
                //   (মধু/মাখনসহ), শুধু কিছুই জমা না থাকলে আদত তালিকার লেখা।
                val autoFreq = autoFreqSaved.ifBlank { ClinicalRepository.rxDoseWhenFor(medName).second }
                ClinicalRepository.rememberPermanentDefault(
                    activity.applicationContext,
                    medName,
                    currentType,
                    dosePart,
                    typedFreq.ifBlank { autoFreq },
                    finalDays
                )
                // TK-REPORTED BUG FIX (2026-07-22): one medicine NAME appears
                // only once -- update an existing same-name row instead of
                // adding a duplicate.
                val outEntry = MedicineEntry(
                    name = medName,
                    dosage = dosePart,
                    frequency = typedFreq.ifBlank { autoFreq },
                    duration = finalDays,
                    instructions = note.text.toString().trim(),
                    medicineType = currentType
                )
                val outDup = targetList.indexOfFirst { it.name.trim().equals(medName.trim(), ignoreCase = true) }
                val wasUpdate = outDup >= 0
                if (outDup >= 0) targetList[outDup] = outEntry else targetList.add(outEntry)
                onAdded()
                // 🟢🔒🔒 V662 (২৫.০৮.২০২৬, TK-নির্দেশ) — আগে এখানে `dialog.dismiss()`
                // ছিল — একটা ওষুধ Add করলেই পপ-আপ বন্ধ হয়ে যেত, পরের ওষুধ
                // লিখতে হলে আবার নতুন করে "Add Medicine (Outside List)"
                // খুলতে হতো। TK: *"তিনি চাইলে দশটা মেডিসিনের জন্য লিখতে
                // পারে"* — এখন পপ-আপ **খোলাই থাকে**, ঘরগুলো ফাঁকা হয়ে পরের
                // ওষুধ লেখার জন্য প্রস্তুত হয়, ডাক্তার যতগুলো লাগবে
                // একটার-পর-একটা লিখতে পারবেন। "Cancel"-এ চাপলে বন্ধ হয় —
                // ততক্ষণে যা যোগ হয়েছে তা সবই থেকে যায় (প্রতিটা Add-এই
                // তালিকায় বসে যাচ্ছে, তাই কিছু হারানোর ঝুঁকি নেই)।
                name.setText(""); dose.setText(""); frequency.setText(""); days.setText(""); note.setText("")
                touchedDose = false; touchedWhen = false; touchedDays = false
                currentType = ""; typeChip.text = "Type: Tap to set"
                addedLabel.visibility = android.view.View.VISIBLE
                addedLabel.text = "✓ Added: $medName" + (if (wasUpdate) " (updated)" else "") + "  ·  ${targetList.size} medicine(s) so far"
                name.requestFocus()
            }
        }
        dialog.show()
        com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dialog)
    }
}
