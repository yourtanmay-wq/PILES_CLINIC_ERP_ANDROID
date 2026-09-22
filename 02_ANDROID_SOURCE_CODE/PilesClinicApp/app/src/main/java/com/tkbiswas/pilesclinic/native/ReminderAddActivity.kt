package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 🟢🔒 V1144 (০৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — "Add Reminder" ফর্ম।
 *
 * TK-নির্দেশ (ফটো-প্রুফ দেখে):
 *   · **ব্রাঞ্চ যিনি পাঠাচ্ছেন তাঁর নিজের ব্রাঞ্চেই তালাবন্ধ** থাকবে
 *   · **পেশেন্ট আইডি লাগবে না** — নম্বর দিলে **নাম · মোবাইল · রোগ** উঠে আসবে
 *   · কীসের রিমাইন্ডার (Medicine · Treatment · Other) · বিবরণ · কোন দিন · কাকে
 *
 * ⛔ পুরোটা কোডে বানানো (নতুন layout XML নেই)।
 * ⛔ রোগীর সারিতে **এক অক্ষরও লেখা হয় না** — শুধু পড়ে নাম/রোগ দেখানো হয়।
 */
class ReminderAddActivity : AppCompatActivity() {

    private lateinit var mobileInput: EditText
    private lateinit var detailsInput: EditText
    private lateinit var foundBox: TextView
    private lateinit var dateBtn: TextView
    private lateinit var toBtn: TextView
    private lateinit var errorView: TextView
    private val typeButtons = ArrayList<TextView>()

    private var branch: String = ""
    private var pickedType: String = ReminderModel.TYPE_MEDICINE
    private var pickedDate: String = ""
    private var toCode: String = ""
    private var toName: String = ""
    private var foundName: String = ""
    private var foundDisease: String = ""
    private var lastLookedUp: String = ""
    private var sending = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = NativeSession.current(this)
        if (user == null) { finish(); return }
        branch = user.branch

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF3F8"))
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_login_hero)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(TextView(this@ReminderAddActivity).apply {
                text = "←"
                textSize = 20f
                setTextColor(Color.WHITE)
                setPadding(dp(4), 0, dp(14), 0)
                setOnClickListener { finish() }
            })
            addView(TextView(this@ReminderAddActivity).apply {
                text = "Add Reminder"
                textSize = 19f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
        })

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14), dp(14), dp(14), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(12), dp(12), dp(12)) }
        }

        // ── ব্রাঞ্চ (তালাবন্ধ — নিজের ব্রাঞ্চ)।
        form.addView(label("Branch"))
        form.addView(TextView(this).apply {
            text = branch.ifBlank { "—" } + "   🔒"
            textSize = 15f
            setTextColor(Color.parseColor("#4A5A6D"))
            setBackgroundColor(Color.parseColor("#F2F5F9"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        })

        // ── রোগীর মোবাইল।
        form.addView(label("Patient mobile *"))
        mobileInput = EditText(this).apply {
            hint = "10-digit mobile"
            textSize = 15f
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            /* 🔒 পাহারা [৯.১৭]: শুধু-সংখ্যার কীবোর্ড কিছু ফোনে খোলে না — তাই টেক্সট + শুধু অঙ্ক (B411)। */
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
        }
        try { MobileInput.attach(mobileInput) } catch (_: Throwable) { }
        form.addView(mobileInput)
        foundBox = TextView(this).apply {
            textSize = 13.5f
            setTextColor(Color.parseColor("#15213A"))
            setBackgroundColor(Color.parseColor("#F1F6FF"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = android.view.View.GONE
        }
        form.addView(foundBox)
        mobileInput.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) lookUp() }
        mobileInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (StaffDirectory.normalizeMobile(s?.toString() ?: "").length == 10) lookUp()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { }
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { }
        })

        // ── ধরন।
        form.addView(label("Reminder for *"))
        val typeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, 0)
        }
        listOf(ReminderModel.TYPE_MEDICINE, ReminderModel.TYPE_TREATMENT, ReminderModel.TYPE_OTHER)
            .forEach { t ->
                val b = TextView(this).apply {
                    text = t
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(dp(6), dp(11), dp(6), dp(11))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { setMargins(dp(3), 0, dp(3), 0) }
                    setOnClickListener { pickedType = t; paintTypes() }
                }
                typeButtons.add(b); typeRow.addView(b)
            }
        form.addView(typeRow)
        paintTypes()

        // ── বিবরণ।
        form.addView(label("Details *"))
        detailsInput = EditText(this).apply {
            hint = "What has to be done"
            textSize = 15f
            gravity = Gravity.TOP or Gravity.START
            minLines = 3
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        form.addView(detailsInput)

        // ── কোন দিন মনে করাবে (V1143-এর নিয়মেই — নিজে বাছতে হয়)।
        form.addView(label("Remind on *"))
        dateBtn = TextView(this).apply {
            text = "Tap to select"
            textSize = 15f
            setTextColor(Color.parseColor("#15213A"))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { pickDate() }
        }
        form.addView(dateBtn)

        // ── কাকে।
        form.addView(label("Send to *"))
        toBtn = TextView(this).apply {
            text = "Tap to select"
            textSize = 15f
            setTextColor(Color.parseColor("#15213A"))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { pickRecipient() }
        }
        form.addView(toBtn)

        errorView = TextView(this).apply {
            textSize = 13.5f
            setTextColor(Color.parseColor("#C81E2B"))
            setPadding(0, dp(10), 0, 0)
            visibility = android.view.View.GONE
        }
        form.addView(errorView)

        val send = TextView(this).apply {
            text = "Send Reminder"
            textSize = 17f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0EA57A"))
            setPadding(dp(14), dp(15), dp(14), dp(15))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), 0, dp(12), dp(20)) }
            setOnClickListener { trySend() }
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(form)
            addView(send)
        }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(inner)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(root)
        try { UppercaseInputUtil.applyToAll(root) } catch (_: Throwable) { }
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13.5f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#5C6B7D"))
        setPadding(0, dp(14), 0, dp(5))
    }

    private fun paintTypes() {
        typeButtons.forEach { b ->
            val on = b.text.toString() == pickedType
            b.setBackgroundColor(Color.parseColor(if (on) "#16A36D" else "#FFFFFF"))
            b.setTextColor(Color.parseColor(if (on) "#FFFFFF" else "#5C6B7D"))
        }
    }

    /** নম্বর ধরে রোগীর নাম ও রোগ — শুধু পড়া, কিছু লেখা হয় না। */
    private fun lookUp() {
        val m = StaffDirectory.normalizeMobile(mobileInput.text.toString())
        if (m.length != 10 || m == lastLookedUp) return
        lastLookedUp = m
        val br = branch
        BackgroundWork.run {
            val rows = SupabaseClient.findByMobileOrNull(
                "patients", m, "name,mobile,branch,disease", 5
            )
            var nm = ""; var ds = ""
            if (rows != null) {
                for (i in 0 until rows.length()) {
                    val o = rows.optJSONObject(i) ?: continue
                    val rowBranch = o.optString("branch", "")
                    if (nm.isBlank() || rowBranch.equals(br, ignoreCase = true)) {
                        nm = o.optString("name", "")
                        ds = o.optString("disease", "")
                        if (rowBranch.equals(br, ignoreCase = true)) break
                    }
                }
            }
            val fname = nm; val fdis = ds
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                foundName = fname; foundDisease = fdis
                if (fname.isBlank()) {
                    foundBox.text = "No patient found with this number."
                    foundBox.setTextColor(Color.parseColor("#8B97A6"))
                } else {
                    val bits = ArrayList<String>()
                    bits.add(m)
                    if (fdis.isNotBlank()) bits.add(fdis)
                    foundBox.text = fname + "\n" + bits.joinToString(" · ")
                    foundBox.setTextColor(Color.parseColor("#15213A"))
                }
                foundBox.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun pickDate() {
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker,
            { _, y, mo, d ->
                val c2 = java.util.Calendar.getInstance().apply { set(y, mo, d) }
                pickedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(c2.time)
                dateBtn.text = FollowUpModel.displayDate(pickedDate)
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).also { try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }.show()
    }

    /** কাকে পাঠানো যাবে — ডাক্তার ও মাস্টার (নিজেকে বাদ দিয়ে)। */
    private fun pickRecipient() {
        val me = NativeSession.current(this)
        val people = StaffDirectory.allAccounts()
            .filter { it.role == "doctor" || it.role == "master" }
            .filter { !it.name.equals(me?.name ?: "", ignoreCase = true) }
        if (people.isEmpty()) { showError("No doctor found to send to"); return }
        val labels = people.map {
            it.name + (if (it.branch.isNotBlank() && it.branch != "All") " · " + it.branch else "")
        }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Send to")
            .setItems(labels) { _, which ->
                toCode = people[which].name
                toName = people[which].name
                toBtn.text = labels[which]
            }
            .create()
            .also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
            .show()
    }

    private fun showError(msg: String) {
        errorView.text = msg
        errorView.visibility = android.view.View.VISIBLE
    }

    private fun trySend() {
        if (sending) return
        errorView.visibility = android.view.View.GONE
        val user = NativeSession.current(this) ?: return
        val m = StaffDirectory.normalizeMobile(mobileInput.text.toString())
        val details = detailsInput.text.toString().trim()
        if (m.length != 10) { showError("Valid patient mobile mandatory"); return }
        if (details.isBlank()) { showError("Details mandatory"); return }
        if (pickedDate.isBlank()) { showError("Remind date mandatory — tap to select"); return }
        if (toCode.isBlank()) { showError("Choose who this reminder is for"); return }

        sending = true
        val br = branch
        val nm = foundName.ifBlank { m }
        val ds = foundDisease
        val ty = pickedType
        val on = pickedDate
        val tc = toCode; val tn = toName
        val fc = user.name; val fn = user.name
        BackgroundWork.run {
            val ok = ReminderRepository.send(br, nm, m, ds, ty, details, on, tc, tn, fc, fn)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                sending = false
                if (ok) { toast("Reminder sent"); finish() }
                else showError("Could not send — check internet and try again")
            }
        }
    }

    private fun toast(msg: String) {
        try {
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT)
                .also { try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }
                .show()
        } catch (_: Throwable) { }
    }
}
