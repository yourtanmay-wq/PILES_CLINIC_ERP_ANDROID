package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.R

/**
 * Patient-facing landing page (launcher). Professional layout: gradient banner with a
 * tappable "TK BISWAS" login chip, floating appointment card, stat ribbon, treatment
 * rows (each opens its educational detail page) and branch cards. Data is unchanged.
 */
class PublicSiteActivity : AppCompatActivity() {

    private data class Dz(val name: String, val sub: String, val icon: Int, val bg: Int, val key: String?)

    private val diseases = listOf(
        Dz("Piles", "অর্শ · बवासीर", R.drawable.ic_pd_piles, 0xFFFDECEC.toInt(), "Piles"),
        Dz("Fissure", "ফিশার · फिशर", R.drawable.ic_pd_fissure, 0xFFFFF2E2.toInt(), "Fissure"),
        Dz("Fistula", "ভগন্দর · भगंदर", R.drawable.ic_pd_fistula, 0xFFEFEAFC.toInt(), "Fistula"),
        Dz("Hydrocele", "একশিরা · हाइड्रोसील", R.drawable.ic_pd_hydrocele, 0xFFE5F3FB.toInt(), "Hydrocele"),
        Dz("Gupt Rog", "গুপ্ত রোগ · गुप्त रोग", R.drawable.ic_pd_guptrog, 0xFFE7F6F0.toInt(), null)
    )

    // Branches, ported from config.js (name, address, phone, map).
    private val branches = listOf(
        Branch("Kishanganj", "Caltex Chowk, Modi Gola, Kishanganj", "8676002200", "https://www.google.com/maps/search/?api=1&query=Biswas+Piles+Clinic+Kishanganj"),
        Branch("Jalpaiguri", "Raikatpara, Opp. Sports Complex, Jalpaiguri", "8436002200", "https://maps.app.goo.gl/fRjsuxhoXq9efBtv9"),
        // 🔒 খাতার সারি B33: কোচবিহারের ঘরে আগে ভুল করে ফালাকাটার নম্বর (8514001100) বসানো ছিল।
        // StaffDirectory-র COB-BRANCH অনুযায়ী সঠিক নম্বর 8514002200।
        Branch("Cooch Behar", "Opp. Mini Bus Stand, Sengupta Complex 1st Floor, Cooch Behar", "8514002200", "https://www.google.com/maps/search/?api=1&query=Maa+Ayurved+Piles+Clinic+Cooch+Behar"),
        Branch("Falakata", "BDO Office Road, near Hotel Nandonik, Falakata", "8514001100", "https://www.google.com/maps/search/?api=1&query=Maa+Ayurved+Piles+Clinic+Falakata"),
        Branch("Birpara", "MG Road, near Axis Bank, Birpara", "8538002200", "https://www.google.com/maps/search/?api=1&query=Maa+Ayurved+Piles+Clinic+Birpara")
    )

    data class Branch(val name: String, val address: String, val phone: String, val map: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_public_site)

        val dc = findViewById<LinearLayout>(R.id.diseaseContainer)
        diseases.forEach { dc.addView(diseaseRow(it)) }

        val bc = findViewById<LinearLayout>(R.id.branchContainer)
        branches.forEach { bc.addView(branchCard(it)) }

        /* 🎨🔒 V829 (২৯.০৮.২০২৬, TK-অনুমোদিত ফটো-প্রুফ: *"হ্যাঁ করুন, তবে সাবধানে"*)
           — অ্যাপের থিমে XML-এর সাদামাটা `<Button>` আপনা-আপনি **MaterialButton**
           হয়ে যায়, আর সেটা `android:background` **অগ্রাহ্য করে** নিজের গাঢ় নীল
           `backgroundTint` বসিয়ে দেয়। ফলে XML-এ লেখা রংটা ফোনে কখনো দেখা যেত না
           (কম্পিউটারে ঠিকই দেখা যেত)। `backgroundTintList = null` বসালে তবেই
           XML-এর drawable-টা দেখা যায় — প্রজেক্টের নিজেরই প্রমাণিত ওষুধ
           (`DoctorQueueAdapter` · `DraftCardAdapter`-এ আগে থেকেই চলছে, পাহারা ৯.৩২)।
           ⛔ শুধু চেহারা — বোতামের কাজ · জায়গা · লেখা কিচ্ছু বদলায়নি। */
        val btnBook = findViewById<Button>(R.id.btnBook)
        val btnCallTop = findViewById<Button>(R.id.btnCallTop)
        val btnBranchTop = findViewById<Button>(R.id.btnBranchTop)
        val btnStaffLogin = findViewById<Button>(R.id.btnStaffLogin)
        btnBook.backgroundTintList = null
        btnCallTop.backgroundTintList = null
        btnBranchTop.backgroundTintList = null
        btnStaffLogin.backgroundTintList = null
        btnBook.setOnClickListener {
            startActivity(Intent(this, AppointmentActivity::class.java))
        }
        btnStaffLogin.setOnClickListener { openLogin() }
        // Tapping "TK BISWAS" opens the Staff Login window.
        findViewById<View>(R.id.tkLoginChip).setOnClickListener { openLogin() }

        btnCallTop.setOnClickListener {
            // TK-REQUESTED (2026-07-24): "everywhere calling is possible in
            // the project" -- shared CallChooser.kt (Phone/Superfone/etc.
            // picker, Truecaller excluded).
            CallChooser.open(this, "+91${branches.first().phone}")
        }
        btnBranchTop.setOnClickListener {
            findViewById<LinearLayout>(R.id.branchContainer).requestFocus()
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(branches.first().map))) } catch (_: Exception) {}
        }
    }

    private fun openLogin() = startActivity(Intent(this, LoginActivity::class.java))

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun iconTile(iconRes: Int, bg: Int): ImageView = ImageView(this).apply {
        setImageResource(iconRes)
        val d = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(bg) }
        background = d
        setPadding(dp(11), dp(11), dp(11), dp(11))
        layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
    }

    private fun rowCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = androidx.core.content.ContextCompat.getDrawable(this@PublicSiteActivity, R.drawable.bg_pub_card)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dp(9))
        layoutParams = lp
    }

    private fun diseaseRow(d: Dz): LinearLayout = rowCard().apply {
        addView(iconTile(d.icon, d.bg))
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(13) }
        }
        col.addView(TextView(context).apply { text = d.name; textSize = 14.5f; setTextColor(0xFF0F2431.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        col.addView(TextView(context).apply { text = d.sub; textSize = 11.5f; setTextColor(0xFF0D5C63.toInt()); setPadding(0, dp(1), 0, 0) })
        if (d.key != null) {
            col.addView(TextView(context).apply {
                text = "বিস্তারিত দেখুন › Learn more"; textSize = 11f
                setTextColor(0xFF12A37A.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dp(4), 0, 0)
            })
        } else {
            col.addView(TextView(context).apply { text = "Confidential private consultation."; textSize = 11f; setTextColor(0xFF6B7A80.toInt()); setPadding(0, dp(3), 0, 0) })
        }
        addView(col)
        addView(TextView(context).apply { text = "›"; textSize = 18f; setTextColor(0xFFC2D0CF.toInt()) })
        if (d.key != null) {
            isClickable = true
            setOnClickListener {
                startActivity(Intent(this@PublicSiteActivity, DiseaseDetailActivity::class.java).putExtra("disease", d.key))
            }
        }
    }

    private fun branchCard(b: Branch): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = androidx.core.content.ContextCompat.getDrawable(this@PublicSiteActivity, R.drawable.bg_pub_card)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dp(9)); layoutParams = lp

        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(iconTile(R.drawable.ic_pd_pin, 0xFFE7F6F0.toInt()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)); setPadding(dp(9), dp(9), dp(9), dp(9))
        })
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(11) }
        }
        col.addView(TextView(context).apply { text = b.name; textSize = 15f; setTextColor(0xFF0D5C63.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        col.addView(TextView(context).apply { text = b.address; textSize = 11f; setTextColor(0xFF6B7A80.toInt()); setPadding(0, dp(2), 0, 0) })
        top.addView(col)
        addView(top)

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(11), 0, 0) }
        row.addView(Button(context).apply {
            text = "📞 Call"; isAllCaps = false; setTextColor(Color.WHITE); textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = androidx.core.content.ContextCompat.getDrawable(this@PublicSiteActivity, R.drawable.bg_pub_book)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) }
            // TK-REQUESTED (2026-07-24): "everywhere calling is possible in
            // the project" -- shared CallChooser.kt (Phone/Superfone/etc.
            // picker, Truecaller excluded).
            setOnClickListener { CallChooser.open(this@PublicSiteActivity, "+91${b.phone}") }
        })
        row.addView(Button(context).apply {
            text = "📍 Map"; isAllCaps = false; setTextColor(0xFF0D5C63.toInt()); textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = androidx.core.content.ContextCompat.getDrawable(this@PublicSiteActivity, R.drawable.bg_pub_outline)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) }
            setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.map))) } catch (_: Exception) {} }
        })
        addView(row)
    }
}
