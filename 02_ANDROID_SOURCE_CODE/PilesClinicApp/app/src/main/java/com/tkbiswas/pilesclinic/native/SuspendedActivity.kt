package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 🔴🔒 V1649 (২১.০৯.২০২৬, TK-নির্দেশ) — "সে বুঝতেই পারছে না যে সাসপেন্ড" ·
 * "সম্পূর্ণ ডিসপ্লে জুড়ে" — ডেমো ফটো-প্রুফ TK পাশ করেছেন।
 *
 * সাসপেন্ড/বাদ-দেওয়া হলে এতদিন শুধু একটা Toast (এক ঝলকের জন্য) বা লগইন-
 * পর্দার ছোট্ট লাল লেখা দেখাত — মিস হয়ে যেত। এখন পুরো পর্দা জুড়ে, স্পষ্ট।
 *
 * ⛔ এই পর্দা নিজে থেকে কিছুই লগআউট/লক করে না — যে কাজ আগে থেকেই করত
 *    (SessionGuardBridge.forceLogout / LoginActivity-র ব্লক), সেটা এক
 *    অক্ষরও বদলায়নি; এটা শুধু তার ওপর দেখানোর পর্দা।
 */
class SuspendedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STAFF_CODE = "staffCode"
        const val EXTRA_UNTIL = "until"        // "yyyy-MM-dd", অথবা ফাঁকা (removed হলে)
        const val EXTRA_REMOVED = "removed"    // true হলে "removed" বার্তা
        const val EXTRA_REASON = "reason"      // ফাঁকা হতে পারে
        /** true হলে "ঠিক আছে, বুঝেছি" (চলতে-চলতে ধরা পড়েছে, আগেই লগআউট হয়ে গেছে)।
         *  false হলে "Back" (লগইন-পর্দা থেকেই আটকানো হয়েছে, লগআউট লাগে না)। */
        const val EXTRA_ALREADY_LOGGED_OUT = "alreadyLoggedOut"

        fun launch(
            activity: Activity, staffCode: String, until: String, removed: Boolean,
            reason: String, alreadyLoggedOut: Boolean
        ) {
            val i = Intent(activity, SuspendedActivity::class.java).apply {
                putExtra(EXTRA_STAFF_CODE, staffCode)
                putExtra(EXTRA_UNTIL, until)
                putExtra(EXTRA_REMOVED, removed)
                putExtra(EXTRA_REASON, reason)
                putExtra(EXTRA_ALREADY_LOGGED_OUT, alreadyLoggedOut)
                if (alreadyLoggedOut) flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.startActivity(i)
            if (alreadyLoggedOut) activity.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val staffCode = intent.getStringExtra(EXTRA_STAFF_CODE).orEmpty()
        val until = intent.getStringExtra(EXTRA_UNTIL).orEmpty()
        val removed = intent.getBooleanExtra(EXTRA_REMOVED, false)
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
        val alreadyLoggedOut = intent.getBooleanExtra(EXTRA_ALREADY_LOGGED_OUT, true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#8A1810"), Color.parseColor("#C43325"))
            )
        }

        root.addView(TextView(this).apply {
            text = "⛔"; textSize = 56f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14); gravity = Gravity.CENTER_HORIZONTAL
            }
        })
        root.addView(TextView(this).apply {
            text = if (removed) "ACCOUNT REMOVED" else "ACCOUNT SUSPENDED"
            textSize = 22f; setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(16); gravity = Gravity.CENTER_HORIZONTAL
            }
        })
        root.addView(TextView(this).apply {
            text = NoBengali.s(if (removed)
                "আপনার অ্যাকাউন্ট মাস্টার বন্ধ করে দিয়েছেন। লগইন করা যাবে না।"
            else
                "আপনার অ্যাকাউন্ট মাস্টার সাময়িকভাবে বন্ধ করে দিয়েছেন। লগইন করা যাবে না।")
            textSize = 15f; setTextColor(Color.parseColor("#FFE5E1")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(24)
            }
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF")); cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(24)
            }
        }
        fun cardLine(text: String) {
            card.addView(TextView(this).apply {
                setText(NoBengali.s(text)); textSize = 13.5f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6)
                }
            })
        }
        if (staffCode.isNotBlank()) cardLine("স্টাফ কোড: $staffCode")
        if (!removed && until.isNotBlank()) cardLine("বন্ধ থাকবে: ${FollowUpModel.displayDate(until)} পর্যন্ত")
        card.addView(TextView(this).apply {
            text = NoBengali.s("কারণ: " + reason.ifBlank { "কিছু লেখা হয়নি" })
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        root.addView(card)

        root.addView(Button(this).apply {
            text = NoBengali.s("📞 সাহায্যের জন্য কল করুন")
            isAllCaps = false; textSize = 14.5f
            setTextColor(Color.parseColor("#8A1810"))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(14).toFloat() }
            setPadding(0, dp(14), 0, dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            setOnClickListener {
                try { CallChooser.open(this@SuspendedActivity, com.tkbiswas.pilesclinic.print.BranchCatalog.HELPLINE) } catch (_: Throwable) { }
            }
        })
        root.addView(Button(this).apply {
            text = if (alreadyLoggedOut) NoBengali.s("ঠিক আছে, বুঝেছি") else "Back"
            isAllCaps = false; textSize = 14f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor("#99FFFFFF")); cornerRadius = dp(14).toFloat()
            }
            setPadding(0, dp(13), 0, dp(13))
            setOnClickListener {
                if (alreadyLoggedOut) {
                    val i = Intent(this@SuspendedActivity, LoginActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(i)
                }
                finish()
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    // ⛔ এই পর্দা থেকে ফোনের Back বোতামে সাধারণ অ্যাপে ফেরা যাবে না —
    // suspend/removed হলে এটাই ঠেকানোর কথা।
    override fun onBackPressed() {
        if (!intent.getBooleanExtra(EXTRA_ALREADY_LOGGED_OUT, true)) {
            super.onBackPressed()
        }
        // alreadyLoggedOut হলে Back চাপলেও কিছু হবে না — বোতাম চেপেই বেরোতে হবে।
    }
}
