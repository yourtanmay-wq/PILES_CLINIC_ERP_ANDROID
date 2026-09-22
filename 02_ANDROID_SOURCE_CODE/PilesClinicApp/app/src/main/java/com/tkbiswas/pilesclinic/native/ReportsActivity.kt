package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityReportsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Native rebuild -- Reports (Master only). Shows totals and this-month vs
 * last-month comparison, matching the headline numbers in reports().
 */
class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val repository = ReportsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        if (session.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_LONG).show()
            finish(); return
        }

        binding.btnBack.setOnClickListener { finish() }
        // 🔍 V1284 (তালিকা সারি ৪১০) — মাস্টারের Duplicate Check পর্দা (শুধু দেখা)
        binding.cardDupCheck.setOnClickListener {
            startActivity(android.content.Intent(this, DuplicateCheckActivity::class.java))
        }

        /* 🔴🔵🔒 V523 (২২.০৮.২০২৬, TK-নির্দেশ) — **উপরের তিনটে বাক্সে চাপ দিলে
           এতদিন কিছুই হত না।**
           TK-এর কথা: *"এখানে কোনোটাতেই চাপ দিলে কিছুই হয় না, তাহলে এগুলো
           দেখানোর কি দরকার?"* — যাচাই করে দেখলাম কথাটা ঠিক: layout-এ ওগুলো
           শুধু লেখা ছিল, বাক্সগুলোর কোনো id বা click ছিলই না।

           **এখন প্রতিটা বাক্স ঐ সংখ্যার আসল তালিকাটাই খোলে —**
             • Enquiry    → Follow-Up-এর **Enquiry** ট্যাব
             • Patients   → Follow-Up-এর **Patient** ট্যাব
             • Collection → **Payment Collection** পর্দা

           ⛔ **নতুন কোনো পর্দা বানানো হয়নি** — অ্যাপের চলতি, পরীক্ষিত
              পর্দাগুলোই খোলা হয়। তাই সংখ্যা দুই জায়গায় আলাদা হওয়ার ভয় নেই,
              আর ঐ পর্দাগুলোর ব্রাঞ্চ/অনুমতির নিয়মও নিজে থেকেই বহাল থাকে।
           ⛔ **Supabase-এ কোনো বাড়তি পড়া নেই** — TK যদি বাক্সে চাপই না দেন,
              আগের মতোই কিছুই হয় না; চাপ দিলে ঐ পর্দা নিজের স্বাভাবিক পড়াটাই
              করে (যা এমনিতেও করত)।
           ⛔ Reports-এর কোনো হিসাব/সংখ্যা এক অক্ষরও বদলায়নি। */
        binding.tileEnq.setOnClickListener { openFollowUpTab("Inquiry") }
        binding.tilePat.setOnClickListener { openFollowUpTab("Patient") }
        binding.tileColl.setOnClickListener {
            try {
                startActivity(android.content.Intent(this, CollectionListActivity::class.java))
            } catch (_: Throwable) { }
        }
        load()
    }

    private fun money(v: Double): String {
        val whole = if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format(Locale.US, "%.2f", v)
        return "₹$whole"
    }

    private fun arrow(a: Number, b: Number): String {
        val x = a.toDouble(); val y = b.toDouble()
        return if (x > y) "📈" else if (x < y) "📉" else "➡️"
    }

    /** A tappable summary row (title bold + subtitle) with a ›, used for the
     *  branch / staff lists so each opens its own detail. */
    private fun makeReportRow(title: String, subtitle: String, iconRes: Int? = null, badgeHex: String = "#F43F5E", onClick: () -> Unit): android.view.View {
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()
        // TK APPROVED (2026-07-15): premium card + green left accent stripe
        // instead of a flat list row -- title/subtitle text, click target and
        // the tap-to-open behaviour are all completely unchanged.
        val outer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE); cornerRadius = px(14).toFloat()
                setStroke(px(1), android.graphics.Color.parseColor("#E4E8ED"))
            }
            val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = px(8); layoutParams = lp
            isClickable = true
            setOnClickListener { onClick() }
        }
        outer.addView(android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(px(4), android.widget.LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#0EA25F"))
        })
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(px(12), px(12), px(12), px(12))
        }
        // TK-APPROVED (2026-07-17): small icon badge instead of a leading
        // emoji in the title text, for the Reports screen's proper-icon
        // pass. Purely visual -- click target, drill-down, and all data
        // above/below this are untouched.
        if (iconRes != null) {
            val badge = android.widget.FrameLayout(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(px(30), px(30)).apply { marginEnd = px(10) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(badgeHex)); cornerRadius = px(9).toFloat()
                }
            }
            badge.addView(android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(px(16), px(16)).apply { gravity = android.view.Gravity.CENTER }
                setImageResource(iconRes)
            })
            root.addView(badge)
        }
        val col = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
        }
        col.addView(android.widget.TextView(this).apply {
            text = title; textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF10223A.toInt())
        })
        col.addView(android.widget.TextView(this).apply {
            text = subtitle; textSize = 12.5f
            setTextColor(0xFF667085.toInt())
            setPadding(0, px(3), 0, 0)
        })
        root.addView(col)
        root.addView(android.widget.TextView(this).apply {
            text = "›"; textSize = 22f; setTextColor(0xFF9AA7B5.toInt())
        })
        outer.addView(root, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return outer
    }

    /** 🎨 TK-নির্দেশ (05.08.2026 — "প্রফেশনাল লুক আনতে হবে", ফিনটেক-স্টাইল
     *  transaction-list ওয়েব-সার্চ করে দেখার পরে) — Today's Collection-এর একটা
     *  পেমেন্ট-সারি: প্লেইন বুলেট-টেক্সটের বদলে এখন কার্ড-স্টাইলে দেখানো হয়। */
    private data class BranchPayRow(val name: String, val label: String, val amount: Double, val modeRaw: String)

    /** 🔵 V523: এক দিনের হিসাব — টাকা, সারি, আর "আজ আসবেন" বলা কতজন।
     *  (আগে `Triple` ছিল; চতুর্থ ঘরটার জন্য নাম-সহ ক্লাস পরিষ্কার।) */
    private data class BranchDayResult(
        val cash: Double, val online: Double,
        val rows: List<BranchPayRow>, val expectedCount: Int
    )

    /** Branch drill-down: today's cash / online / total + each payment. */
    private fun showBranchDetail(branch: String) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) {
                // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): this
                // used to download the WHOLE payments table (up to 5000 rows,
                // every column) and then throw away everything that was not
                // this branch and not today -- on TK's 0.16-2.00 KB/s line that
                // is most of the waiting on this popup.
                //
                // ⛔ THE NUMBERS CANNOT CHANGE. The cloud is now asked for
                // exactly the two things the loop below already insisted on:
                //   * branch  -- the loop keeps only branch == this branch
                //   * date    -- the loop keeps only rows whose date STARTS
                //               with today, so "like today*" matches the very
                //               same rows (date is a text column)
                // Both checks below are left in place word for word, so even
                // if the cloud ever returned something extra it would still be
                // dropped exactly as before. Only the columns nobody here reads
                // are no longer downloaded.
                val branchPart = "branch=eq." + java.net.URLEncoder.encode(branch, "UTF-8")
                val pay = SupabaseClient.fetchListSlim(
                    "payments", "$branchPart&date=like.$today*", 5000,
                    cols = SupabaseClient.PAYMENT_COLS_LIST
                )
                var cashSum = 0.0; var onlineSum = 0.0
                val rowList = ArrayList<BranchPayRow>()
                /* 🔴🔵🔒 V523 (২২.০৮.২০২৬, TK-নির্দেশ) — *"যখন কেউ কোন পেমেন্টই
                   করেনি, তাহলে এগুলো দেখানোর মানেটা কি?"*
                   এই তালিকায় আজকের **সব** payments-সারি ঢুকত — অঙ্ক শূন্য হলেও।
                   Chamber-এর "Mark Expected" বোতাম ঠিক ওই টেবিলেই ₹0-এর একটা
                   সারি লেখে, তাই কেউ টাকা না দিলেও তাঁর নাম Collection-এর
                   তালিকায় দেখা যেত (`₹0 · Marked Expected`)।
                   ⇒ এখন চিহ্ন-মাত্র সারিগুলো তালিকা থেকে বাদ, আর কতজনকে
                     "আজ আসবেন" বলা হয়েছে সেটা উপরে আলাদা করে লেখা হয়।
                   ⛔ **টাকার যোগফলে এক পয়সাও হাত পড়েনি** — cashSum/onlineSum
                      নিচে আলাদাভাবেই গোনা হয়, আর ₹0 যোগ হলে যোগফল বদলায়ও না।
                      Refund-এর বিয়োগ আগের মতোই অক্ষত।
                   ⛔ কোন সারি "চিহ্ন-মাত্র" তা ঠিক হয় প্রজেক্টের **চলতি
                      নিয়মেই** (`PaymentModel.isMarkerOnlyRow`) — নতুন কোনো
                      আন্দাজ নয়। */
                var expectedCount = 0
                // 🔴🔴 TK-অডিট-অনুরোধ (01.08.2026): Refund সারিও প্লেইন পজিটিভ
                // Collection হিসেবে যোগ হচ্ছিল — Chamber Board-এ আগেই লক করা
                // নিয়ম (B250/B251) এখানেও বসানো হলো (approved refund বিয়োগ,
                // pending/rejected কোনো প্রভাব নেই)। ⛔ এই হিসাব-লজিক এক অক্ষরও
                // বদলায়নি — শুধু ফলাফল এখন টেক্সটের বদলে সারি হিসেবে জমা হচ্ছে।
                for (i in 0 until pay.length()) {
                    val p = pay.getJSONObject(i)
                    if (p.s("branch") != branch) continue
                    if (p.s("date").take(10) != today) continue
                    val amt = when {
                        PaymentModel.isApprovedRefund(p) -> -p.optDouble("amount", 0.0)
                        PaymentModel.isRefundRow(p) -> 0.0
                        else -> p.optDouble("amount", 0.0)
                    }
                    val modeRaw = p.s("mode").ifBlank { "CASH" }
                    if (PaymentModel.isApprovedRefund(p)) {
                        val refundAmt = p.optDouble("amount", 0.0)
                        if (PaymentModel.normalizeMode(modeRaw) == "ONLINE") onlineSum -= refundAmt else cashSum -= refundAmt
                    } else if (!PaymentModel.isRefundRow(p)) {
                        val split = PaymentModel.paymentSplit(p)
                        cashSum += split.first
                        onlineSum += split.second
                    }
                    val label = p.s("payLabel").ifBlank { p.s("paymentLabel").ifBlank { "Payment" } }
                    val displayMode = if (p.s("payType").equals("treatment", true)) {
                        val split = PaymentModel.paymentSplit(p)
                        PaymentModel.splitMode(split.first, split.second)
                    } else modeRaw
                    if (PaymentModel.isMarkerOnlyRow(p.s("payType"))) {
                        // 🔵 V523: টাকার সারি নয় — গোনা হয়, তালিকায় বসে না।
                        if (p.s("payType").equals("chamber_expected", true)) expectedCount++
                    } else {
                        rowList.add(BranchPayRow(p.s("name").ifBlank { p.s("mobile").ifBlank { "-" } }, label, amt, displayMode))
                    }
                }
                BranchDayResult(cashSum, onlineSum, rowList, expectedCount)
            }
            showBranchCollectionDialog("$branch — Today's Collection", res.cash, res.online, res.rows, res.expectedCount)
        }
    }

    /** 🎨 TK-নির্দেশ (05.08.2026): showPremiumInfoDialog-এর হুবহু একই সবুজ
     *  হেডার+সাদা গোল কার্ড+Close — শুধু ভিতরের বডি এখন ফিনটেক-অ্যাপের মতো
     *  (সারাংশ Cash/Online/Total তিনটে চিপ পাশাপাশি, প্রতিটা পেমেন্ট নিজের
     *  আলাদা সাদা কার্ডে — নাম/লেবেল বাঁয়ে, টাকা+মোড ডানে) — প্লেইন বুলেট-লাইন
     *  আর না। ⛔ টাকার হিসাব/ডেটা এক অক্ষরও বদলায়নি, শুধু দেখানোর ধরন। */
    private fun showBranchCollectionDialog(title: String, cash: Double, online: Double, rows: List<BranchPayRow>, expectedCount: Int = 0) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE); cornerRadius = dp(20).toFloat()
            }
        }
        val header = android.widget.LinearLayout(this).apply {
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(android.graphics.Color.parseColor("#0A5428"), android.graphics.Color.parseColor("#0EA25F"))
                cornerRadii = floatArrayOf(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        header.addView(android.widget.TextView(this).apply {
            text = "📍 $title"; textSize = 15.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        root.addView(header)

        val body = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(4))
        }

        // ── সারাংশ: Cash/Online/Total — তিনটে চিপ পাশাপাশি ──
        fun chip(label: String, value: Double, bg: String): android.widget.LinearLayout =
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(bg)); cornerRadius = dp(12).toFloat()
                }
                val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = dp(6)
                layoutParams = lp
                addView(android.widget.TextView(this@ReportsActivity).apply {
                    text = label; textSize = 10.5f; setTextColor(android.graphics.Color.parseColor("#FFFFFF")); alpha = 0.9f
                    gravity = android.view.Gravity.CENTER
                })
                addView(android.widget.TextView(this@ReportsActivity).apply {
                    text = money(value); textSize = 14.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, dp(2), 0, 0)
                })
            }
        val summaryRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }
        summaryRow.addView(chip("CASH", cash, "#0B7A3C"))
        summaryRow.addView(chip("ONLINE", online, "#1565C0"))
        summaryRow.addView(chip("TOTAL", cash + online, "#10223A").apply {
            (layoutParams as android.widget.LinearLayout.LayoutParams).marginEnd = 0
        })
        body.addView(summaryRow)

        /* 🔵🔒 V523 (TK-নির্দেশ): "আজ আসবেন" বলে দাগ দেওয়া লোকজন আর
           Collection-এর তালিকায় ঢোকে না — কিন্তু তথ্যটা হারায়ও না, এখানে
           এক লাইনে থাকে। ⛔ শূন্য হলে লাইনটাই দেখায় না। */
        if (expectedCount > 0) {
            body.addView(android.widget.TextView(this).apply {
                text = "\u23F0 Expected today: $expectedCount  (no payment yet)"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#8A6D1F"))
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FFF8E6"))
                    cornerRadius = dp(10).toFloat()
                }
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dp(10)
                layoutParams = lp
            })
        }

        // ── প্রতিটা পেমেন্ট নিজের সাদা কার্ডে ──
        if (rows.isEmpty()) {
            body.addView(android.widget.TextView(this).apply {
                text = NoBengali.s("আজ কোনো লেনদেন নেই।"); textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#667085"))
                setPadding(dp(4), dp(10), dp(4), dp(10))
            })
        } else {
            rows.forEach { r ->
                val isOnline = r.modeRaw.uppercase().let { it.contains("UPI") || it.contains("ONLINE") }
                val accent = if (isOnline) "#1565C0" else "#0B7A3C"
                val card = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#F7F9FC"))
                        cornerRadius = dp(12).toFloat()
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = dp(8)
                    layoutParams = lp
                }
                // বাঁ-দিকে ছোট রঙিন আদ্যক্ষর-বৃত্ত
                card.addView(android.widget.TextView(this).apply {
                    text = r.name.trim().take(1).uppercase().ifBlank { "?" }
                    textSize = 13f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    gravity = android.view.Gravity.CENTER
                    val sz = dp(34)
                    layoutParams = android.widget.LinearLayout.LayoutParams(sz, sz)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(android.graphics.Color.parseColor(accent))
                    }
                })
                // মাঝে নাম + লেবেল
                val mid = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    lp.marginStart = dp(10)
                    layoutParams = lp
                }
                mid.addView(android.widget.TextView(this).apply {
                    text = r.name; textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                    maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                })
                mid.addView(android.widget.TextView(this).apply {
                    text = r.label; textSize = 11f
                    setTextColor(android.graphics.Color.parseColor("#667085"))
                })
                card.addView(mid)
                // ডানে টাকা + মোড-ব্যাজ
                val right = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    gravity = android.view.Gravity.END
                }
                right.addView(android.widget.TextView(this).apply {
                    text = money(r.amount); textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor(if (r.amount < 0) "#B42318" else accent))
                })
                right.addView(android.widget.TextView(this).apply {
                    text = r.modeRaw.uppercase(); textSize = 9.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor(accent))
                })
                card.addView(right)
                body.addView(card)
            }
        }

        val scroll = android.widget.ScrollView(this).apply {
            addView(body)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(420)
            )
        }
        root.addView(scroll)

        val close = android.widget.TextView(this).apply {
            text = "CLOSE"; gravity = android.view.Gravity.CENTER; textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(14), 0, dp(16)); isClickable = true; isFocusable = true
        }
        root.addView(close)

        UppercaseInputUtil.applyToAll(root)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).setView(root).setCancelable(true).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        close.setOnClickListener { dialog.dismiss() }
        dialog.show()
        PremiumAlert.paint(dialog)
    }

    /** Staff drill-down: their enquiries / patients / collection this list. */
    /** 🔵🔒 V523 (TK-নির্দেশ): পুরো Staff Performance পর্দা — যেখানে কল ·
     *  কালেকশন · RMP · রিপোর্ট · হাজিরা · Extra Income সবই সার্ভারের এক হিসাবে।
     *  ⛔ নতুন কোনো পর্দা বানানো হয়নি — **আসল পর্দাটাই** খোলা হয়, তাই
     *     সংখ্যা সবসময় এক জায়গা থেকেই আসে। */
    /** 🔵🔒 V523: Follow-Up পর্দার নির্দিষ্ট ট্যাব খোলে।
     *  ⛔ নতুন কিছু নয় — অ্যাপের চলতি পর্দাটাই, শুধু কোন ট্যাব দিয়ে শুরু
     *     হবে সেটা বলে দেওয়া (`startStage`)। */
    private fun openFollowUpTab(stage: String) {
        try {
            startActivity(
                android.content.Intent(this, FollowUpActivity::class.java)
                    .putExtra("startStage", stage)
            )
        } catch (_: Throwable) { }
    }

    private fun openStaffPerformance() {
        try {
            startActivity(
                android.content.Intent(this, com.tkbiswas.pilesclinic.modules.StaffProfileActivity::class.java)
                    .putExtra("openPerformance", true)
            )
        } catch (_: Throwable) { }
    }

    private fun showStaffDetail(name: String) {
        lifecycleScope.launch {
            // 🔴 TK-রিপোর্ট (02.08.2026 দুপুর ~১২.৩১ pm): "এখানে চাপ দিলে সেই
            // পেশেন্টের ডিটেলস খুলতে হবে, যে সেকশনে আছে সেখানে অটোমেটিক
            // রিডাইরেক্ট, ব্যাক করলে ঠিক রিপোর্টের মধ্যেই ফিরে আসবে।"
            // ⛔ আগে পুরো তালিকা একটাই TextView-এ প্লেইন লেখা ছিল (চাপ দেওয়ার
            // কিছু ছিল না) — এখন প্রতিটা সারি আলাদা, চাপ দিলে সেই মোবাইল ধরে
            // PatientTimelineActivity খোলে (ওই পর্দা নিজে থেকেই বুঝে নেয় রোগী
            // এখন Enquiry/Visit/Patient কোন ধাপে আছেন — আলাদা করে "কোন সেকশন"
            // বলার দরকার নেই)। Intent-এ কোনো বিশেষ flag নেই, তাই Android-এর
            // স্বাভাবিক Back-বাটনই সরাসরি এই Reports পর্দায় ফিরিয়ে আনে।
            val (header, rows) = withContext(Dispatchers.IO) {
                fun last10(s: String) = s.filter { it.isDigit() }.takeLast(10)
                val acc = StaffDirectory.allAccounts().find { it.name == name }
                val mob = last10(acc?.mobile ?: "")
                val mine = if (mob.isBlank()) null
                    else "or=(createdBy.like.*$mob,receivedBy.like.*$mob)"
                // V328: these three independent reads used to wait one after another.
                // They now travel together, so Staff Detail waits for the slowest one
                // instead of the sum of all three. Same three queries, same rows and
                // same calculations below; no extra Supabase request or design change.
                val (enq, pat, pay) = coroutineScope {
                    val enqDef = async {
                        if (mine == null) org.json.JSONArray()
                        else SupabaseClient.fetchListSlim("enquiries", mine, 5000, cols = "id,name,mobile,receivedBy,createdBy,updatedAt")
                    }
                    val patDef = async {
                        if (mob.isBlank()) org.json.JSONArray()
                        else SupabaseClient.fetchListSlim("patients", "createdBy=like.*$mob", 5000, cols = "id,name,mobile,createdBy,updatedAt")
                    }
                    val payDef = async {
                        if (mine == null) org.json.JSONArray()
                        else SupabaseClient.fetchListSlim("payments", mine, 5000, cols = "id,amount,payType,refundApprovalStatus,receivedBy,createdBy,updatedAt")
                    }
                    Triple(enqDef.await(), patDef.await(), payDef.await())
                }
                var en = 0; var pt = 0; var coll = 0.0
                val rowList = ArrayList<StaffEntryRow>()
                fun nameOrMobile(name: String, mobile: String) =
                    name.ifBlank { mobile.filter { it.isDigit() }.takeLast(10).ifBlank { "-" } }
                for (i in 0 until enq.length()) {
                    val r = enq.getJSONObject(i)
                    val who = last10(r.s("receivedBy").ifBlank { r.s("createdBy") })
                    if (who == mob && mob.isNotBlank()) {
                        en++
                        val rMobile = r.s("mobile")
                        rowList.add(StaffEntryRow(
                            "📥", nameOrMobile(r.s("name"), rMobile), "Enquiry",
                            DateUtil.display(r.s("updatedAt")), rMobile
                        ))
                    }
                }
                for (i in 0 until pat.length()) {
                    val r = pat.getJSONObject(i)
                    if (last10(r.s("createdBy")) == mob && mob.isNotBlank()) {
                        pt++
                        val rMobile = r.s("mobile")
                        rowList.add(StaffEntryRow(
                            "🧑", nameOrMobile(r.s("name"), rMobile), "Patient",
                            DateUtil.display(r.s("updatedAt")), rMobile
                        ))
                    }
                }
                for (i in 0 until pay.length()) {
                    val r = pay.getJSONObject(i)
                    if (last10(r.s("receivedBy").ifBlank { r.s("createdBy") }) == mob && mob.isNotBlank()) {
                        // 🔴🔴 TK-অডিট-অনুরোধ (01.08.2026): Refund ভুলভাবে যোগ হচ্ছিল,
                        // এখন approved refund বিয়োগ হয় (Chamber Board-এর B250/B251-এর
                        // একই নিয়ম), pending/rejected-এর কোনো প্রভাব নেই।
                        coll += when {
                            PaymentModel.isApprovedRefund(r) -> -r.optDouble("amount", 0.0)
                            PaymentModel.isRefundRow(r) -> 0.0
                            else -> r.optDouble("amount", 0.0)
                        }
                    }
                }
                "Enquiry: $en \u00b7 Patient: $pt \u00b7 Collection: ${money(coll)}" to rowList
            }
            showStaffEntryListDialog("\ud83d\udc64 $name \u2014 Report", header, rows)
        }
    }

    /** 🔴 B284 (02.08.2026): Staff Detail-এর একটা সারি — চাপ দিলে ওই মোবাইলের
     *  Patient Timeline খুলবে। শুধু এই ফাইলের ভিতরের ব্যবহারের জন্য। */
    private data class StaffEntryRow(val icon: String, val label: String, val kind: String, val dateText: String, val mobile: String)

    /** B284: showPremiumInfoDialog-এর হুবহু একই দৃশ্য (সবুজ হেডার, সাদা গোল
     *  বডি, Close) — শুধু বডি এখন প্লেইন টেক্সটের বদলে চাপ-যোগ্য সারি। */
    private fun showStaffEntryListDialog(title: String, headerLine: String, rows: List<StaffEntryRow>) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE); cornerRadius = dp(20).toFloat()
            }
        }
        val header = android.widget.LinearLayout(this).apply {
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(android.graphics.Color.parseColor("#0A5428"), android.graphics.Color.parseColor("#0EA25F"))
                cornerRadii = floatArrayOf(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        header.addView(android.widget.TextView(this).apply {
            text = title; textSize = 15.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        root.addView(header)

        val body = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(6))
        }
        body.addView(android.widget.TextView(this).apply {
            text = headerLine; textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#18251D"))
            setPadding(0, 0, 0, dp(10))
        })
        lateinit var dlg: androidx.appcompat.app.AlertDialog
        if (rows.isEmpty()) {
            body.addView(android.widget.TextView(this).apply {
                text = "\u0995\u09cb\u09a8\u09cb \u098f\u09a8\u09cd\u099f\u09cd\u09b0\u09bf \u09a8\u09c7\u0987\u0964"
                textSize = 13f; setTextColor(android.graphics.Color.parseColor("#18251D"))
            })
        } else {
            rows.forEach { row ->
                body.addView(android.widget.TextView(this).apply {
                    text = "${row.icon} ${row.label} \u2014 ${row.kind} \u00b7 ${row.dateText}" +
                        (if (row.mobile.isNotBlank()) "  \u203a" else "")
                    textSize = 13f
                    setTextColor(android.graphics.Color.parseColor(if (row.mobile.isNotBlank()) "#0B4F2A" else "#18251D"))
                    if (row.mobile.isNotBlank()) setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                    isClickable = row.mobile.isNotBlank()
                    isFocusable = row.mobile.isNotBlank()
                    if (row.mobile.isNotBlank()) {
                        setOnClickListener {
                            // 🔴 TK-REPORTED (04.08.2026, একই ক্লাসের বাগ): dlg.dismiss()
                            // করা হত, তাই Timeline থেকে Back করলে এই তালিকা আর দেখা
                            // যেত না। এখন পপ-আপ খোলা থাকে, Back করলে এখানেই ফেরে।
                            startActivity(
                                android.content.Intent(this@ReportsActivity, PatientTimelineActivity::class.java)
                                    .putExtra("mobile", row.mobile)
                            )
                        }
                    }
                })
            }
        }
        val scroll = android.widget.ScrollView(this).apply {
            addView(body)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(420)
            )
        }
        root.addView(scroll)

        val close = android.widget.TextView(this).apply {
            text = "CLOSE"; gravity = android.view.Gravity.CENTER; textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(14), 0, dp(16)); isClickable = true; isFocusable = true
        }
        root.addView(close)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).setView(root).setCancelable(true).create()
        dlg = dialog
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        close.setOnClickListener { dialog.dismiss() }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        // 🔒 খাতার সারি B181 (TK, 30.07.2026): এই পপ-আপে বাংলা লেখা ("কোনো
        // এন্ট্রি নেই" গোছের) আছে, কিন্তু পাহারা ছিল না।
        PremiumAlert.paint(dialog)
    }

    /** TK APPROVED (2026-07-15): shared premium dialog shell (green dual
     *  header + rounded white body + styled Close) for the Reports drill-down
     *  popups -- replacing the plain default AlertDialog title+message. Same
     *  exact text content as before, only the shell around it changed.
     *  ⛔ B284 (02.08.2026): এখনো Branch-এর "Today's Collection" drill-down
     *  (প্লেইন টেক্সট, চাপ-যোগ্য সারি দরকার নেই) এই আগের ফাংশনটাই ব্যবহার করে —
     *  তাই এটা সরানো হয়নি, শুধু Staff Detail-এর জন্য পাশে নতুন
     *  showStaffEntryListDialog() যোগ হয়েছে। */
    private fun showPremiumInfoDialog(title: String, message: String) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE); cornerRadius = dp(20).toFloat()
            }
        }
        val header = android.widget.LinearLayout(this).apply {
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(android.graphics.Color.parseColor("#0A5428"), android.graphics.Color.parseColor("#0EA25F"))
                cornerRadii = floatArrayOf(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        header.addView(android.widget.TextView(this).apply {
            text = title; textSize = 15.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        root.addView(header)

        val body = android.widget.TextView(this).apply {
            text = message; textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#18251D"))
            setPadding(dp(18), dp(16), dp(18), dp(6))
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        val scroll = android.widget.ScrollView(this).apply {
            addView(body)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(420)
            )
        }
        root.addView(scroll)

        val close = android.widget.TextView(this).apply {
            text = "CLOSE"; gravity = android.view.Gravity.CENTER; textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(14), 0, dp(16)); isClickable = true; isFocusable = true
        }
        root.addView(close)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog2 = androidx.appcompat.app.AlertDialog.Builder(this).setView(root).setCancelable(true).create()
        dialog2.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        close.setOnClickListener { dialog2.dismiss() }
        dialog2.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog2) } catch (_: Throwable) { }   // 🤫 V774
        // 🔒 খাতার সারি B181 (TK, 30.07.2026): এই পপ-আপে বাংলা লেখা ("কোনো
        // এন্ট্রি নেই" গোছের) আছে, কিন্তু পাহারা ছিল না।
        PremiumAlert.paint(dialog2)
    }

        private var firstResume = true
    override fun onResume() {
        super.onResume()
        if (firstResume) { firstResume = false; return }
        load()
    }

    private fun load() {
        binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        // 🔒🔒 B603 (10.08.2026, TK-নির্দেশ "cache-first, ঝুঁকি ছাড়া"): এই ফোনে জমানো
        // শেষ সারাংশ থাকলে সাথে সাথে দেখানো হয়, পিছনে ক্লাউড থেকে হালনাগাদ এলে ঠিক একই
        // renderSummary দিয়ে বদলে যায়। ⛔ repository.load()-এর হিসাব একটুও বদলায়নি; Reports
        // read-only (টাকার কোনো বোতাম নেই), তাই এক মুহূর্ত পুরনো সংখ্যা দেখালেও ঝুঁকি নেই।
        val me = NativeSession.current(this@ReportsActivity)
        val branchKey = if (me?.role == "master") "all" else (me?.branch ?: "-")
        loadCachedSummary(branchKey)?.let { renderSummary(it) }
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) {
                // TK-ORDER (2026-07-25): staff = own branch only; Master = all.
                repository.load(if (me?.role == "master") null else me?.branch)
            }
            saveCachedSummary(branchKey, r)
            renderSummary(r)
        }
    }

    /** 🔒 B603: সারাংশ দেখানোর অংশ (আগে load()-এর ভিতরে ছিল) — ক্যাশ ও আসল দুই
     *  ক্ষেত্রেই একই ফাংশন। কোনো হিসাব নেই, শুধু দেখানো (আগের কোড হুবহু)। */
    private fun renderSummary(r: ReportSummary) {
        binding.progressLoad.visibility = View.GONE

            binding.tvEnqTotal.text = r.totalEnquiries.toString()
            binding.tvPatTotal.text = r.totalPatients.toString()
            binding.tvCollTotal.text = money(r.totalCollection)

            val mc = StringBuilder()
            mc.append("Enquiry: ${r.enqThisMonth}  ${arrow(r.enqThisMonth, r.enqLastMonth)}  (গত মাসে ${r.enqLastMonth})\n")
            mc.append("Patient: ${r.patThisMonth}  ${arrow(r.patThisMonth, r.patLastMonth)}  (গত মাসে ${r.patLastMonth})\n")
            mc.append("Collection: ${money(r.collThisMonth)}  ${arrow(r.collThisMonth, r.collLastMonth)}  (গত মাসে ${money(r.collLastMonth)})")
            binding.tvMonthCompare.text = mc.toString()

            // Branch-wise summary — Master only.
            val role = NativeSession.current(this@ReportsActivity)?.role ?: ""
            if (role == "master") {
                binding.containerBranch.removeAllViews()
                r.branchRows.forEach { b ->
                    binding.containerBranch.addView(
                        makeReportRow(
                            b.branch,
                            "📋 ${b.enq} Enquiry   🧑‍⚕️ ${b.pat} Patient   💰 ${money(b.collection)}",
                            iconRes = com.tkbiswas.pilesclinic.R.drawable.ic_report_pin, badgeHex = "#F43F5E"
                        ) { showBranchDetail(b.branch) }
                    )
                }
                binding.tvBranchTitle.visibility = View.VISIBLE
                binding.cardBranch.visibility = View.VISIBLE

                if (r.staffRows.isNotEmpty()) {
                    binding.containerStaff.removeAllViews()
                    /* 🔴🔵🔒 V523 (২২.০৮.২০২৬, TK-নির্দেশ) — **দুই জায়গার দুই
                       হিসাব যেন বিভ্রান্ত না করে।**
                       TK-এর প্রশ্ন: *"এখানে যদি স্টাফ পারফরম্যান্স হয়ে থাকে,
                       তাহলে একই প্রজেক্টে দুই জায়গায় থাকার মানেটা কি?"*
                       **যাচাই করে যা পেয়েছি:** দুটো এক নয় —
                         • এখানকার হিসাব **ফোনেই গোনা** (`ReportsRepository`),
                           চেনে `createdBy`/`receivedBy` **মোবাইল** ধরে, আর
                           দেখায় শুধু **Enquiry ও Patient** সংখ্যা।
                         • Staff Profiles → 🏆 Staff Performance আসে **সার্ভারের
                           RPC** (`hr.staff_performance`) থেকে, চেনে
                           `staff_profiles`-এর **কোড** ধরে, আর দেখায় কল ·
                           কালেকশন · RMP · রিপোর্ট · হাজিরা · Extra Income।
                       ⇒ দুই নিয়মে গোনা, তাই **সংখ্যা মিলবে না** — এটাই আসল
                         বিপদ ছিল। TK-এর সিদ্ধান্ত: অংশটা **থাকবে** (ড্রিল-ডাউনটা
                         কাজের), শুধু **স্পষ্ট করে লেখা থাকবে** এটা কী গোনে।
                       ⛔ কোনো হিসাব · কোনো সারি · কোনো ড্রিল-ডাউন বদলায়নি —
                          শুধু একটা ব্যাখ্যার লাইন ও একটা বোতাম যোগ। */
                    // ⚠️ `dp()` এই ফাইলে শুধু পপ-আপ-ফাংশনগুলোর **ভিতরের** সহায়ক —
                    //    এখানে নেই। তাই এখানেই স্থানীয়ভাবে বানানো হলো (একই নিয়ম)।
                    val sd = resources.displayMetrics.density
                    fun sdp(v: Int) = (v * sd).toInt()
                    binding.containerStaff.addView(android.widget.TextView(this).apply {
                        text = "Counts Enquiry & Patient only, from this phone.\n" +
                            "Full performance (calls, collection, RMP, attendance) is in Staff Profiles \u2192 Staff Performance."
                        textSize = 11.5f
                        setTextColor(android.graphics.Color.parseColor("#8A6D1F"))
                        setPadding(sdp(10), sdp(9), sdp(10), sdp(9))
                        setLineSpacing(sdp(2).toFloat(), 1f)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#FFF8E6"))
                            cornerRadius = sdp(10).toFloat()
                        }
                        val lp = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.bottomMargin = sdp(10)
                        layoutParams = lp
                    })
                    r.staffRows.forEach { s ->
                        binding.containerStaff.addView(
                            makeReportRow(
                                s.name,
                                "📋 ${s.enq} Enquiry   🧑‍⚕️ ${s.pat} Patient",
                                iconRes = com.tkbiswas.pilesclinic.R.drawable.ic_report_person, badgeHex = "#6366F1"
                            ) { showStaffDetail(s.name) }
                        )
                    }
                    /* 🔵 V523: পুরো হিসাব যেখানে, সেখানে যাওয়ার সোজা পথ —
                       TK-কে আর খুঁজে বেড়াতে হবে না।
                       ⛔ শুধু Master-এর জন্য (এই অংশটাই master-only)। */
                    binding.containerStaff.addView(
                        makeReportRow(
                            "\uD83C\uDFC6 Staff Performance",
                            "Calls \u00b7 Collection \u00b7 RMP \u00b7 Reports \u00b7 Attendance \u00b7 Extra Income",
                            iconRes = com.tkbiswas.pilesclinic.R.drawable.ic_report_person, badgeHex = "#0EA25F"
                        ) { openStaffPerformance() }
                    )
                    binding.tvStaffTitle.visibility = View.VISIBLE
                    binding.cardStaff.visibility = View.VISIBLE
                }
            }
            binding.tvConversion.text = "Enquiry → Patient Conversion: ${r.conversionRate}%"
            binding.tvAnalytics.text = "💰 Payment Analytics\n" +
                "Today: ${money(r.todayCollection)}\n" +
                "Cash: ${money(r.cashTotal)}   Online: ${money(r.upiTotal)}\n" +
                "মোট বকেয়া (Outstanding Due): ${money(r.totalDue)}\n" +
                "Referral — Paid: ${money(r.referralPaidTotal)}   Due: ${money(r.referralDueTotal)}"
    }

    // 🔒 B603: ছোট ReportSummary ক্যাশ (স্কেলার + branch/staff তালিকা) — prefs-এ।
    private fun reportCachePrefs() = getSharedPreferences("reports_cache", MODE_PRIVATE)
    private fun saveCachedSummary(key: String, r: ReportSummary) {
        try {
            val br = org.json.JSONArray()
            for (b in r.branchRows) br.put(org.json.JSONObject().put("branch", b.branch).put("enq", b.enq).put("pat", b.pat).put("collection", b.collection))
            val st = org.json.JSONArray()
            for (s in r.staffRows) st.put(org.json.JSONObject().put("name", s.name).put("enq", s.enq).put("pat", s.pat))
            val o = org.json.JSONObject()
                .put("totalEnquiries", r.totalEnquiries).put("totalPatients", r.totalPatients).put("totalCollection", r.totalCollection)
                .put("enqThisMonth", r.enqThisMonth).put("enqLastMonth", r.enqLastMonth)
                .put("patThisMonth", r.patThisMonth).put("patLastMonth", r.patLastMonth)
                .put("collThisMonth", r.collThisMonth).put("collLastMonth", r.collLastMonth)
                .put("branchRows", br).put("conversionRate", r.conversionRate).put("staffRows", st)
                .put("cashTotal", r.cashTotal).put("upiTotal", r.upiTotal).put("todayCollection", r.todayCollection)
                .put("totalDue", r.totalDue).put("referralPaidTotal", r.referralPaidTotal).put("referralDueTotal", r.referralDueTotal)
            reportCachePrefs().edit().putString("sum_$key", o.toString()).apply()
        } catch (_: Throwable) {}
    }
    private fun loadCachedSummary(key: String): ReportSummary? {
        return try {
            val o = org.json.JSONObject(reportCachePrefs().getString("sum_$key", null) ?: return null)
            val br = ArrayList<BranchStat>()
            val ba = o.optJSONArray("branchRows") ?: org.json.JSONArray()
            for (i in 0 until ba.length()) { val x = ba.getJSONObject(i); br.add(BranchStat(x.optString("branch"), x.optInt("enq"), x.optInt("pat"), x.optDouble("collection"))) }
            val st = ArrayList<StaffStat>()
            val sa = o.optJSONArray("staffRows") ?: org.json.JSONArray()
            for (i in 0 until sa.length()) { val x = sa.getJSONObject(i); st.add(StaffStat(x.optString("name"), x.optInt("enq"), x.optInt("pat"))) }
            ReportSummary(
                o.optInt("totalEnquiries"), o.optInt("totalPatients"), o.optDouble("totalCollection"),
                o.optInt("enqThisMonth"), o.optInt("enqLastMonth"),
                o.optInt("patThisMonth"), o.optInt("patLastMonth"),
                o.optDouble("collThisMonth"), o.optDouble("collLastMonth"),
                br, o.optInt("conversionRate"), st,
                o.optDouble("cashTotal"), o.optDouble("upiTotal"), o.optDouble("todayCollection"),
                o.optDouble("totalDue"), o.optDouble("referralPaidTotal"), o.optDouble("referralDueTotal")
            )
        } catch (_: Throwable) { null }
    }
}
