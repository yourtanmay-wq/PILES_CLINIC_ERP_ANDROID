package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ) — Incoming ও Outgoing দুই ধরনের
 * কলেই রিমার্কস লেখার ছোট, স্বাধীন পর্দা।
 *
 * TK-এর নির্দেশ (হুবহু): *"কল চলাকালীন ও কল শেষে দুটোতেই Remarks ...
 * লেখার ব্যবস্থা থাকবে ... নাম্বার সেভ না থাকলে New Enquiry Form Fillup
 * এর ব্যবস্থা করতে হবে।"*
 *
 * এই পর্দাটা নোটিফিকেশনের "📝 Add Remark" বোতাম চাপলে খোলে —
 * `CallNotifyManager`-এ যে নম্বর/দিক/রোগীর id সেই মুহূর্তে জানা ছিল,
 * সেটাই সাথে করে আসে (extras)।
 *
 * ⛔ নতুন কোনো XML লেআউট বানানো হয়নি — সম্পূর্ণ কোড থেকে বানানো (প্রজেক্টের
 *    অনেক ছোট utility পর্দার মতোই), ভুল হওয়ার সুযোগ কম।
 * ⛔ `call_remarks` (নতুন, স্বাধীন টেবিল) ছাড়া আর কিছু ছোঁয়া হয়নি।
 */
class CallRemarkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mobile = intent.getStringExtra("mobile").orEmpty()
        val direction = intent.getStringExtra("direction").orEmpty().ifBlank { "incoming" }
        val patientId = intent.getStringExtra("patientId").orEmpty()
        val patientName = intent.getStringExtra("patientName").orEmpty()
        val branch = intent.getStringExtra("branch").orEmpty()
        val calledAt = intent.getStringExtra("calledAt").orEmpty()
        val existingRemark = intent.getStringExtra("existingRemark").orEmpty()
        // 🟢🔒 V634 (২৪.০৮.২০২৬, TK-রিপোর্ট — "২ বার কল করা হয়েছে তাও Wifi
        // signal কেন ১ টা দেখাচ্ছে") — followups সারির আসল id, যাতে রিমার্কস
        // সেভের সময় সিগন্যাল-আইকনের callCount-ও ঠিকভাবে বাড়ে (RMP-মিলে
        // এটা ফাঁকা থাকে, CallNotifyManager নিজেই পাঠায় না)।
        val followupId = intent.getStringExtra("followupId").orEmpty()
        // 🩺🔒 V836 (২৯.০৮.২০২৬, TK-নির্দেশ, ডেমো-ফটো পাশ) — RMP-র নিজের
        // `doctor_visits` সারির id। ফাঁকা মানে এটা RMP নয় ⇒ তারিখের ঘরটাও
        // দেখানো হয় না (পর্দা তখন হুবহু আগের মতোই থাকে)।
        val rmpId = intent.getStringExtra("rmpId").orEmpty()
        // V1652 — My Enquiry post-call flow-তে Save সত্যিই চাপা হলে তবেই
        // local pending reminder সরবে। অন্য CallRemark caller-এ আচরণ অপরিবর্তিত।
        val clearPendingOnSave = intent.getBooleanExtra("clearPendingOnSave", false)

        val user = NativeSession.current(this)
        if (mobile.isBlank() || user == null) { finish(); return }

        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(android.graphics.Color.parseColor("#F7F9FC"))
        }

        root.addView(TextView(this).apply {
            text = "📝 Call Remark"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#101828"))
        })
        root.addView(TextView(this).apply {
            text = patientName.ifBlank { mobile } + (if (patientName.isNotBlank()) "  ·  $mobile" else "")
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(6), 0, dp(2))
        })
        root.addView(TextView(this).apply {
            text = (if (direction == "outgoing") "Outgoing call" else "Incoming call") +
                (if (branch.isNotBlank()) " · $branch" else "")
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#8A93A0"))
            setPadding(0, 0, 0, dp(16))
        })

        val input = EditText(this).apply {
            hint = NoBengali.s("কল নিয়ে যা বললেন লিখুন…")
            setText(existingRemark)
            minLines = 4
            gravity = android.view.Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#DBE2EA"))
            }
        }
        root.addView(input)

        /* 📅🔒 V836 (২৯.০৮.২০২৬, TK-নির্দেশ — *"পপ-আপে Next Call তারিখ বাছার
           ঘরও থাকবে, তবে বাধ্যতামূলক নয়"*; ডেমো-ফটো দেখিয়ে অনুমতি নেওয়া)।
           ⛔ **শুধু RMP-র বেলায়** দেখানো হয় — Enquiry/Visit/Patient-এর পর্দা
              এক অক্ষরও বদলায়নি।
           ⛔ ফাঁকা রাখলে আগের Next Call তারিখ অক্ষত থাকে
              (`DoctorVisitRepository.logCallKeepingDates` সেটাই করে)। */
        var pickedNextCall = ""
        val dateLine = TextView(this)
        /* 🔴🔒🔒 V873 (৩০.০৮.২০২৬, TK-রিপোর্ট + অনুমতি — BHUDEV CHANDRA ROY):
           *"২৯.৮ এই ব্যক্তি তো চলে এসেছিল, তাহলে কেন বাকির খাতায় দেখাচ্ছে"*

           **আসল দোষ (কোড ধরে যাচাই):** "টুডে পেন্ডিং" ঠিক হয় **শুধু**
           `nextFollow` (পরের কলের তারিখ) দেখে। Follow-up পর্দায় রিমার্ক সেভ
           করলে তারিখ বাধ্যতামূলকভাবে চাওয়া হয় — কিন্তু **এই পর্দায় (Dialer-এর
           "📝 Add / Edit Remark" ও কল আসার সময়ের রিমার্ক) তারিখের ঘরটা শুধু
           RMP ডাক্তারের বেলায় দেখাত**, রোগীর বেলায় নয়। তাই রোগীর তারিখ
           পুরোনোই থেকে যেত আর নাম পেন্ডিং তালিকায় আটকে থাকত।

           **এখন (TK-অনুমোদিত):** রোগীর বেলাতেও ঘরটা দেখায়, আর Save চাপলে
           তারিখ না দেওয়া থাকলে **ক্যালেন্ডার নিজে খোলে** — Follow-up পর্দার
           হুবহু একই আচরণ (নতুন কিছু বানানো হয়নি, একই `PilesDatePicker`)।
           ⛔ RMP-র বেলায় আচরণ এক অক্ষরও বদলায়নি। */
        if (rmpId.isNotBlank() || followupId.isNotBlank()) {
            root.addView(TextView(this).apply {
                text = "NEXT CALL DATE"
                textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                setPadding(0, dp(16), 0, dp(6))
            })
            val dateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            dateLine.apply {
                text = "Tap to choose date"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#101828"))
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(android.graphics.Color.WHITE)
                    setStroke(dp(1), android.graphics.Color.parseColor("#DBE2EA"))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true; isFocusable = true
                setOnClickListener {
                    /* 🔴🔒 V843 (২৯.০৮.২০২৬, TK-রিপোর্ট ছবিসহ: *"Next Follow Up
                       Call এ কেন Old Date দেখাবে"*) — **আমার V836-এর বাদ পড়া**:
                       এখানে `minDate` বসানোই হয়নি, তাই **পুরনো তারিখ বাছা যেত**
                       আর ক্যালেন্ডার আজকের দিনে না-ও খুলতে পারত।
                       ⇒ ডাক্তার-রিমাইন্ডারের (V671) **হুবহু প্রমাণিত নিয়মই**
                         এখানে বসানো হলো — নতুন কিছু বানানো হয়নি:
                         · প্রজেক্টের নিজের `PilesDatePicker` চেহারা
                         · শুরু সবসময় **আজকের দিনে** (আগে বাছা থাকলে সেটিতে)
                         · `minDate` = আজ ⇒ **অতীতের তারিখ ধূসর, বাছাই বন্ধ**
                       ⛔ Next Call তারিখ সবসময় ভবিষ্যতের — অতীতের তারিখ
                          বসালে RMP সেকশনে ভুল তথ্য যেত। */
                    val cal = java.util.Calendar.getInstance()
                    if (pickedNextCall.isNotBlank()) {
                        try {
                            val parts = pickedNextCall.split("-").map { it.toInt() }
                            cal.set(parts[0], parts[1] - 1, parts[2])
                        } catch (_: Throwable) { }
                    }
                    android.app.DatePickerDialog(
                        this@CallRemarkActivity,
                        com.tkbiswas.pilesclinic.R.style.PilesDatePicker,
                        { _, y, m, d ->
                            pickedNextCall = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                            text = FollowUpModel.displayDate(pickedNextCall)
                        },
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH),
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    ).apply {
                        // ⛔ অতীতের তারিখ বাছা যাবে না (V671-এর হুবহু একই লাইন)
                        datePicker.minDate = System.currentTimeMillis() - 1000L
                    }.show()
                }
            }
            dateRow.addView(dateLine)
            dateRow.addView(TextView(this).apply {
                text = "Clear"
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(android.graphics.Color.parseColor("#EEF2F7"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#DBE2EA"))
                }
                isClickable = true; isFocusable = true
                setOnClickListener {
                    pickedNextCall = ""
                    dateLine.text = "Tap to choose date"
                }
            }.also { it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) } })
            root.addView(dateRow)
            root.addView(TextView(this).apply {
                text = if (followupId.isNotBlank())
                    "Not given? The calendar opens on Save - this decides today's pending call list."
                else "Leave it empty - the old Next Call date stays as it is."
                textSize = 11.5f
                setTextColor(android.graphics.Color.parseColor("#8A93A0"))
                setPadding(0, dp(6), 0, 0)
            })
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }
        val save = TextView(this).apply {
            text = "Save Remark"
            textSize = 14.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            setPadding(dp(10), dp(13), dp(10), dp(13))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#16A36D"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true; isFocusable = true
            setOnClickListener {
                val text = input.text?.toString().orEmpty().trim()
                if (text.isBlank()) { Toast.makeText(this@CallRemarkActivity, "Write something first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                /* 🟢 V873 — রোগীর বেলায় তারিখ না দিলে আগে ক্যালেন্ডার।
                   ⛔ RMP-র বেলায় আগের মতোই ঐচ্ছিক, কিছু বদলায়নি। */
                if (followupId.isNotBlank() && pickedNextCall.isBlank()) {
                    Toast.makeText(this@CallRemarkActivity, "Now give the next call date", Toast.LENGTH_SHORT).show()
                    dateLine.performClick()
                    return@setOnClickListener
                }
                DialerRepository.saveCallRemark(
                    ctx = applicationContext,
                    mobile = mobile, direction = direction, remark = text, patientId = patientId,
                    staffMobile = user.mobile, staffName = user.name, branch = user.branch,
                    calledAtIso = calledAt, followupId = followupId,
                    rmpId = rmpId, nextCallDate = pickedNextCall   /* 🩺 V836 */
                )
                if (clearPendingOnSave) {
                    try { PendingRemarkStore.remove(this@CallRemarkActivity, mobile) } catch (_: Throwable) { }
                }
                Toast.makeText(this@CallRemarkActivity, "Remark saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        btnRow.addView(save)
        root.addView(btnRow)

        setContentView(root)
    }
}
