package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityReportCardBinding
import com.tkbiswas.pilesclinic.print.BranchInfo
import com.tkbiswas.pilesclinic.print.BranchCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TK-APPROVED (2026-07-20): "Report Card" — the paper "Patient Progress
 * Report of Movement History", digital. Opened from the Take Action menu.
 *
 * Layout mirrors TK's own register: a branch-specific clinic header
 * (name/address/phone/logo watermark — all from BranchInfo, so it changes
 * per branch), patient details, three totals (Bill / Paid / Due), then a
 * Visit | Date | Progress | Paid | Due table.
 *
 * Edit rules (TK-specified, 3-tap on a cell):
 *   • Progress  — ANYONE, ANY time. Saved to that visit's own payment row
 *                 `remarks`, so it auto-syncs to Timeline/Payment too.
 *   • Paid      — staff only on the SAME day the payment was made; otherwise
 *                 "মাস্টারের অনুমতি লাগবে" and edit is blocked. Master any time.
 *   • Estimated — master only.
 *   • Due       — auto-derived (Bill − Paid); not directly editable.
 */
class ReportCardActivity : AppCompatActivity() {

    // TK-REQUESTED PROACTIVE FIX (2026-07-25): the same overlapping.refresh
    // guard already proven on Follow-up and Chamber Attendance. Two loads can
    // overlap (screen reopened, an action refreshing, a slow first fetch
    // finishing late) . without this the OLDER result could land last and
    // overwrite fresh data on screen. Only the newest load may paint now.
    private var loadGuardToken = 0

    private lateinit var binding: ActivityReportCardBinding
    private lateinit var user: NativeUser
    private var mobile: String = ""

    /**
     * 🔴🔵🔒 V522 (২২.০৮.২০২৬, TK-নির্দেশ) — **এই Report কোন রোগীর।**
     *
     * **সমস্যা যেটা ছিল:** এই পর্দা **শুধু মোবাইল নম্বর** নিয়ে খুলত।
     * V516-এর পরে এক নম্বরে স্বামী ও স্ত্রী দুজন আলাদা রোগী থাকতে পারেন —
     * তখন কার Report দেখাবে, অ্যাপ নিজে বেছে নিত (`pickPatientRow`)। ফলে
     * Patient Timeline-এ স্ত্রীকে দেখে "Report Card" চাপলে **স্বামীর
     * রিপোর্ট** খুলে যেতে পারত, আর ছাপাও হত তাঁরই নামে।
     *
     * **এখন:** ডাকা পর্দা জানলে রোগীর সারির আইডি সঙ্গে পাঠায়, আর সেটাই
     * পুরো পর্দা · জমানো কপি · ছাপা — তিন জায়গাতেই ব্যবহার হয়।
     *
     * ⛔ **ফাঁকা রাখলে আচরণ হুবহু আগের মতোই** (`pickPatientRow`) — তাই যেসব
     *    পুরোনো ডাকার জায়গা এটা পাঠায় না, সেগুলো এক অক্ষরও বদলাতে হয়নি।
     * ⛔ কোনো বাড়তি cloud-read নেই — শুধু আগের পড়াগুলোকেই ঠিক রোগীর দিকে
     *    তাক করানো হয়।
     */
    private var preferRowId: String = ""

    /** 🔵 V522: কিছু পর্দা সারির আইডি জানে না, শুধু Official Patient ID জানে
     *  (Chamber Attendance) — সেটাও রোগী-প্রতি অনন্য, তাই সেটাও চলে। */
    private var preferPatientCode: String = ""

    /** 🔵 V522: জমানো কপির চাবিতে যেটা বসবে — যা জানা আছে সেটাই।
     *  দুটোই ফাঁকা হলে ফাঁকা, অর্থাৎ চাবিটা **অবিকল আগের মতোই**। */
    private val cacheToken: String get() = preferRowId.ifBlank { preferPatientCode }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun isMaster(): Boolean = user.role == "master"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        try {
            binding = ActivityReportCardBinding.inflate(layoutInflater)
            setContentView(binding.root)
            UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

            val u = NativeSession.current(this)
            if (u == null) { finish(); return }
            user = u

            mobile = intent.getStringExtra("mobile")?.filter { it.isDigit() }?.takeLast(10) ?: ""
            preferRowId = intent.getStringExtra("patientRowId").orEmpty()          // 🔵 V522
            preferPatientCode = intent.getStringExtra("patientCode").orEmpty()      // 🔵 V522
            if (mobile.length != 10) {
                android.widget.Toast.makeText(this, "No valid mobile", android.widget.Toast.LENGTH_SHORT).show()
                finish(); return
            }

            binding.btnBack.setOnClickListener { finish() }
            binding.btnPrint.setOnClickListener { printReport() }

            load()
        } catch (e: Throwable) {
            android.widget.Toast.makeText(this, "Could not open Report Card", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun load() {
        val myLoadToken = ++loadGuardToken
        // 🔒 V216 (§10, 31.07.2026): "Report খুললে আলাদা Loading Screen দেখা যাচ্ছে।"
        // এখন cache-first — এই ফোনে এই রোগীর Timeline আগে দেখা থাকলে (TimelineCache)
        // সঙ্গে সঙ্গে সেই তথ্য দিয়ে Report আঁকা হয়, "Loading..." দেখাতে হয় না;
        // পিছনে আসল fetch চলে ও নতুন তথ্য এলে আবার আঁকে (loadGuardToken শুধু নতুনটাকে
        // আঁকতে দেয়)। ⛔ cache না থাকলে আগের মতোই "Loading..." — কোনো regression নেই।
        // ⛔ age/address/sex cache-এ নেই বলে ওই তিনটে full load-এ ভরে; হিসাব বদলায় না।
        val cached = try { TimelineCache.load(this, mobile, cacheToken) } catch (_: Throwable) { null }
        binding.reportContainer.removeAllViews()
        if (cached != null) {
            try { render(cached, "", "", "", BranchCatalog.byName(cached.branch)) } catch (_: Throwable) { }
        } else {
            // TK-REQUESTED (2026-07-24): plain "Loading..." text immediately so
            // this screen is never blank on slow network -- reportContainer
            // is cleared and rebuilt by render() the instant data arrives.
            binding.reportContainer.addView(TextView(this).apply {
                text = "Loading..."
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#8A93A6"))
                setPadding(0, (24 * resources.displayMetrics.density).toInt(), 0, 0)
            })
        }
        lifecycleScope.launch {
            val guardAtStart = myLoadToken
            try {
                val data = withContext(Dispatchers.IO) {
                    PatientTimelineRepository.build(mobile, null, this@ReportCardActivity,
                        preferRowId = preferRowId, preferPatientCode = preferPatientCode)
                }
                if (guardAtStart != loadGuardToken) return@launch
                val patientRow = withContext(Dispatchers.IO) {
                    try {
                        // TK-REQUESTED (2026-07-27), ধাপ ৩: this used to ask for
                        // ONE row by mobile and take whatever came back -- with a
                        // duplicate registration that could be the empty row, so
                        // Age/Address/Sex went blank while the rest of the app
                        // used the real row. The Timeline just above has ALREADY
                        // resolved this patient's real row, so we simply read
                        // that same row by its id -- one row, one request, and
                        // the screen, the print and the Timeline can never
                        // disagree. Falls back to the old lookup when there is no
                        // patients row yet (enquiry-only), exactly as before.
                        // TK-REQUESTED (2026-07-27), ধাপ ৩খ: the fallback below
                        // (used only when the Timeline found no patients row of
                        // its own) still asked for ONE row and took whatever came
                        // back, so on a duplicate it could pick a different row
                        // than every other screen. It now reads the same way and
                        // applies the ONE shared rule.
                        val rid = data.rowId
                        /* 🔴🔒 V794 — এই সারিটা থেকে শুধু বয়স/ঠিকানা/লিঙ্গ নেওয়া হয়
                           (যাচাই করা); ছবিটা আসে `PatientTimelineRepository.build`
                           থেকে। তাই এখানে ছবি ছাড়া পড়া হয়। */
                        val arr = if (rid.isNotBlank()) SupabaseClient.fetchListSlim("patients",
                                "id=eq.$rid", 1, SupabaseClient.PATIENT_NO_PHOTO_COLS)
                            else SupabaseClient.findByMobile("patients", "+91$mobile",
                                SupabaseClient.PATIENT_NO_PHOTO_COLS, 50)
                        val ownBranch = NativeSession.current(this@ReportCardActivity)?.branch.orEmpty()
                        PatientIdentity.pickPatientRow(arr, ownBranch)
                    } catch (_: Throwable) { null }
                }
                // 🆕 (03.08.2026, TK-অনুমোদনে) — গভীর অডিট: optString(key, "")-ও কলাম
                // সত্যিই NULL হলে "null" শব্দ ফেরত দেয় (B286/B327-এর একই বাগ) — তাই
                // এখানে প্রমাণিত নিরাপদ `.s()` হেল্পার বসানো হলো।
                val age = patientRow?.s("age") ?: ""
                val address = patientRow?.s("address") ?: ""
                // TK-APPROVED (2026-07-22): Sex fetched from the same patients
                // row already being queried above (no extra network call) so
                // the patient header can show it, matching the app-wide
                // Name/Age/ID/Mobile + Date/Sex/Diseases/Address format.
                val sex = patientRow?.s("sex") ?: ""
                val branch = BranchCatalog.byName(data.branch)
                render(data, age, address, sex, branch)
                // 🔒 V216 (§10): পরের বার এই Report যেন সঙ্গে সঙ্গে খোলে, তাই এই
                // ফোনে Timeline cache-এ সেভ করে রাখা হয় (PatientTimeline পর্দাও
                // একই cache ব্যবহার করে, তাই দুই পর্দা এক তথ্যই দেখায়)।
                try { TimelineCache.save(this@ReportCardActivity, mobile, data, cacheToken) } catch (_: Throwable) { }
            } catch (e: Exception) {
                binding.reportContainer.removeAllViews()
                binding.reportContainer.addView(TextView(this@ReportCardActivity).apply {
                    text = "Could not load — check connection"
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(android.graphics.Color.parseColor("#B42318"))
                    setPadding(0, (24 * resources.displayMetrics.density).toInt(), 0, 0)
                })
                android.widget.Toast.makeText(this@ReportCardActivity, "Could not load — check connection", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val d get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()
    private fun money(v: Double) = "\u20B9" + "%,.0f".format(v)

    private fun render(data: TimelineData, age: String, address: String, sex: String, branch: BranchInfo) {
        val box = binding.reportContainer
        box.removeAllViews()

        // ---- Clinic + patient header (white bg, print-friendly) ----
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(2), android.graphics.Color.parseColor("#0B2B59"))
                cornerRadius = dp(8).toFloat()
            }
        }
        // clinic strip
        header.addView(TextView(this).apply {
            text = "${branch.clinicName}\n${branch.addressLine} · \uD83D\uDCDE ${branch.phoneLine}".uppercase()
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(android.graphics.Color.parseColor("#0B2B59"), android.graphics.Color.parseColor("#0e7c7b"))
            )
        })
        // patient row: photo | gap | left(name/mob/id) | right(age/disease/address)
        val prow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val photo = android.widget.ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(78), dp(78))
            val bmp = PhotoUtils.decodeDataUrl(data.photo)
            if (bmp != null) {
                val c = RoundedBitmapDrawableFactory.create(resources, bmp); c.isCircular = true
                setImageDrawable(c)
            } else { setBackgroundColor(android.graphics.Color.parseColor("#E2E8EE")) }
        }
        prow.addView(photo)
        prow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(14), 1) }) // gap: photo -> info
        val leftCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        leftCol.addView(TextView(this).apply {
            // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে নিচের MOB লাইনের সাথে মিলে মোবাইল দুইবার দেখাত।
            text = data.name.ifBlank { "UNKNOWN" }.uppercase(); textSize = 12.5f
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.parseColor("#0B2B59"))
        })
        // TK-APPROVED (2026-07-22): field order/content changed to the
        // app-wide standard — LEFT: Name/Age/ID/Mobile, RIGHT: Date/Sex/
        // Diseases/Address. Same box, same photo, same smallLine() style —
        // nothing visual besides the field order/content changed.
        /* 🎨🔒 V894 (৩১.০৮.২০২৬, TK ডেমো ফটো দেখে **"হ্যাঁ পাশ, বসিয়ে দিন"**) —
           TK: *"Age 30 MALE — অন্যান্য জায়গায় যেমন পাশাপাশি ছিল সে রকম থাকবে"*।
           বয়স ও লিঙ্গ এখন এক লাইনেই ("30 / MALE"), ঠিক যেমন ছাপার কাগজে
           (`PrintMappers`-এ "Age / Gender: 30 / MALE") আগে থেকেই আছে।
           ⛔ ডানের কলাম থেকে SEX লাইনটা বাদ, তাই কিছু দুবার দেখায় না; আর
              দু-কলামের লাইন সমান হওয়ায় DATE · DISEASE · ADDRESS নিজে থেকেই
              একটু উপরে উঠে নামের সারি বরাবর বসে (TK-এর দ্বিতীয় নির্দেশ)। */
        leftCol.addView(smallLine("AGE / SEX",
            age.ifBlank { "—" } + " / " + sex.ifBlank { "—" }))
        leftCol.addView(smallLine("ID", data.patientId.ifBlank { "—" }))
        leftCol.addView(smallLine("MOB", "+91${data.mobile}"))
        val rightCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        rightCol.addView(smallLine("DATE", formatDisplayDate(today())))
        rightCol.addView(smallLine("DISEASE", data.disease.ifBlank { "—" }))
        rightCol.addView(smallLine("ADDRESS", address.ifBlank { "—" }))
        prow.addView(leftCol)
        prow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(14), 1) }) // gap: left col -> right col
        prow.addView(rightCol)
        header.addView(prow)
        box.addView(header)
        (header.layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = dp(10)

        // ---- three total boxes ----
        val bill = data.billTotal
        // 🔒 V217 (§B216, 31.07.2026): `paidEffect` ব্যবহার — approved refund
        // এখন Report Card-এর PAID থেকেও সত্যিই বিয়োগ হয় (আগে যোগ হয়ে যেত,
        // ভুল দিকে); pending/rejected refund কোনো প্রভাব ফেলে না।
        val paidTotal = data.entries.filter {
            it.paymentId != null && (it.payType.equals("treatment", true) || it.payType.equals("refund", true))
        }.sumOf { it.paidEffect }
        val dueTotal = if (bill > 0.0) (bill - paidTotal).coerceAtLeast(0.0) else 0.0
        val totals = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(10)) }
        totals.addView(totalBox("TOTAL BILL", money(bill), "#EEF3FB", "#3f6fb0", "#1c3d6e") {
            if (isMaster()) editEstimated(data) else denyMaster()
        })
        totals.addView(spacer())
        totals.addView(totalBox("PAID", money(paidTotal), "#E9F8F0", "#16a36d", "#0c7a45", null))
        totals.addView(spacer())
        totals.addView(totalBox("DUE", money(dueTotal), "#FDECEC", "#e5484d", "#b02525", null))
        box.addView(totals)

        // ---- table ----
        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#0B2B59"))
            }
        }
        table.addView(tableHeaderRow())
        // TK-CLARIFIED (2026-07-20): one row per VISIT (one date). A day's
        // treatment payments — cash AND online — are summed into a single
        // "Paid" here (mode is never split on the Report Card). Progress is
        // that day's remark(s). Oldest visit first, like the paper register.
        // TK-REQUESTED (2026-07-24): a day with ONLY an Attendance Mark (no
        // money that day) now still gets its own row here too -- Paid stays
        // ₹0 for that day exactly as before (attendance_mark rows always
        // carry amount=0.0), only its Progress/remark now has somewhere to
        // show. visit_fee/bill_edit/chamber_expected stay excluded, same as
        // before -- this only removes the attendance_mark exclusion.
        // 🔴🔒 V1636 (TK-রিপোর্ট, ছবিসহ, ২০.০৯.২০২৬ — "তারিখ উল্টোপাল্টা সাজানো
        // কেন?") — CHAYAN ROY-র কার্ডে ১ম ভিজিট ০৮.০৮, ২য় ১৮.০৮, ৩য় ২০.০৮,
        // ৪র্থ আবার ০৪.০৮ — এলোমেলো। আসল কারণ: এতদিন `it.sortKey` দিয়ে সাজানো
        // হতো, যেটা পেমেন্টের ক্ষেত্রে সবসময় `createdAt` (স্টাফ **কবে এন্ট্রি
        // করেছেন**) — ভিজিটের আসল তারিখ নয়। পুরনো/বাকি টাকা পরে ব্যাচ করে
        // এন্ট্রি করলে এন্ট্রির ক্রম আর ভিজিটের তারিখের ক্রম আলাদা হয়ে যায়,
        // তাই সারি এলোমেলো দেখায় — ঠিক এই একই বাগ আগে সাধারণ Timeline-এ
        // ধরা পড়েছিল (B679, ১৫.০৮.২০২৬) ও `orderKey()` দিয়ে সারানো হয়েছিল,
        // কিন্তু Report Card তখন সেই সারানো তালিকা আবার নিজের মতো করে
        // `sortKey` দিয়ে re-sort করে ফেলত — তাই এই পর্দাতেই বাগটা রয়ে গিয়েছিল।
        // ⛔ Report Card-এর এই তালিকায় `chamber_expected` (ভবিষ্যতের-তারিখ
        // ধরে রাখা সারি, যেটার জন্য orderKey()-তে sortKey-প্রাধান্য বসানো
        // হয়েছিল) আদৌ আসে না (উপরের filter-এই বাদ) — তাই এখানে নিরাপদে
        // সরাসরি **ভিজিটের নিজের তারিখ** (`date`, সবসময় yyyy-MM-dd) দিয়েই
        // সাজানো যায়, ঠিক এই পর্দার নিজের পুরনো মন্তব্যেরই ("Oldest visit
        // first") কথামতো। একই দিনের একাধিক এন্ট্রি এমনিতেই একটাই ভিজিট-সারিতে
        // জমা হয় (নিচের `byDate`), তাই ক্রম-বদলে টাকার হিসাব ছোঁয় না।
        val payEntries = data.entries
            .filter {
                it.paymentId != null && (
                    it.payType.equals("treatment", true) ||
                    it.payType.equals("attendance_mark", true) ||
                    it.payType.equals("refund", true)
                )
            }
            .sortedBy { it.date }
        val byDate = LinkedHashMap<String, MutableList<TimelineEntry>>()
        for (e in payEntries) byDate.getOrPut(e.date) { mutableListOf() }.add(e)
        var run = 0.0
        var idx = 0
        for ((visitDate, allDayEntries) in byDate) {
            val financialEntries = allDayEntries.filter {
                it.payType.equals("treatment", true) || it.payType.equals("refund", true)
            }
            val paidThisDay = financialEntries.sumOf { it.paidEffect }
            // V1653: money always affects running Paid/Due, even when the day is payment-only.
            // VISIT rows from 22 Aug onward need real chamber/clinical evidence. Older history is preserved.
            run += paidThisDay
            val legacyVisit = visitDate < "2026-08-22" && allDayEntries.any {
                it.payType.equals("treatment", true) || it.payType.equals("attendance_mark", true)
            }
            val strongVisit = allDayEntries.any {
                it.payType.equals("attendance_mark", true) ||
                    (it.payType.equals("treatment", true) && (it.typedRemark.isNotBlank() || it.isChamberVisitAnchor))
            }
            if (!(legacyVisit || strongVisit)) continue
            idx++
            val displayPaidThisDay = allDayEntries
                .filter { it.payType.equals("treatment", true) }
                .sumOf { it.paidEffect.coerceAtLeast(0.0) }
            val hasRealTreatmentPayment = allDayEntries.any {
                it.payType.equals("treatment", true) && it.paymentAmount > 0.0
            }
            val due = if (bill > 0.0) (bill - run).coerceAtLeast(0.0) else 0.0
            val dayList = allDayEntries
            // TK-REPORTED BUG FIX (2026-07-25, from TK's own screenshot):
            // this used to join EVERY same-day payment's raw .note
            // together -- including the auto-generated "₹amount · MODE"
            // fallback (when no remark was typed) and any accumulated
            // "| Audit: ..." edit-trail text from 3-tap amount corrections
            // -- producing an unreadable wall of payment/audit text in
            // Progress instead of the actual treatment description. The
            // amount already has its own Paid column; Progress now shows
            // ONLY genuine typed clinical remarks (auto-syncs from
            // whichever screen last wrote one -- Chamber Attendance's
            // Treatment Progress, or this same box's own 3-tap edit below
            // -- exactly like TK asked).
            // 🚨 TK-REPORTED, LIVE (27.07.2026, SADDAM / KNE-15072026-002 — TK's
            // photo showed this box three lines tall with "Advance Payment —
            // ₹10,000 · CASH · ₹1,000 · CASH | ₹400 · CASH", while the next visit
            // correctly showed "—"). This used to try to WORK OUT which part of
            // the finished note was a real remark by pattern-matching the text,
            // and that guess failed. It now reads the value the Timeline itself marks
            // as genuinely typed by a person (typedRemark), so the app's own
            // payment text and audit lines can never appear here again.
            // V1526: Progress belongs to the visit itself, never to a refund/audit row.
            // A same-day refund may change summary Paid/Due, but its reason (e.g. DEMO)
            // must not replace the treatment progress shown to the patient.
            val progress = dayList
                .filter { it.payType.equals("treatment", true) || it.payType.equals("attendance_mark", true) }
                .map { it.typedRemark }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")
            table.addView(tableDataRow(idx, visitDate, progress, displayPaidThisDay, due, dayList, hasRealTreatmentPayment))
        }
        box.addView(table)
    }

    private fun smallLine(label: String, value: String): TextView = TextView(this).apply {
        text = android.text.SpannableStringBuilder().apply {
            val b = "$label: "
            append(b); setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#0e7c7b")), 0, b.length, 0)
            append(value.uppercase())
        }
        textSize = 10.5f; setTextColor(android.graphics.Color.parseColor("#10223A"))
        setPadding(0, dp(1), 0, dp(1))
    }

    private fun spacer(): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 1) }

    private fun totalBox(label: String, value: String, bg: String, border: String, textColor: String, onEdit: (() -> Unit)?): View {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            /* 🎨🔒 V894 — TK: *"total bill due paid — এই ঘরগুলো একটু
               উচ্চতায় কম হবে"*। উপর-নিচের ফাঁক ৮ → ৫/৬। বাঁ-ডানের ফাঁক · রং ·
               বর্ডার · টাকার অঙ্ক — কিছুই বদলায়নি। */
            setPadding(dp(8), dp(5), dp(8), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor(bg)); cornerRadius = dp(9).toFloat()
                setStroke(dp(1), android.graphics.Color.parseColor(border))
            }
        }
        v.addView(TextView(this).apply { text = label; textSize = 9.5f; setTextColor(android.graphics.Color.parseColor(border)); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        v.addView(TextView(this).apply { text = value; textSize = 14f; setTextColor(android.graphics.Color.parseColor(textColor)); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        if (onEdit != null) TripleTapEdit.attach(v) { onEdit() }
        return v
    }

    private fun tableHeaderRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#0e7c7b"))
        }
        // TK-REQUESTED (2026-07-20): DUE column not shown on screen — only
        // in the printed report. TOTAL DUE box above still shows it live.
        row.addView(headCell("VISIT", dp(36), 0f))
        row.addView(headCell("DATE", dp(78), 0f))
        row.addView(headCell("PROGRESS", 0, 1f))
        row.addView(headCell("PAID", dp(64), 0f))
        return row
    }

    private fun headCell(t: String, w: Int, weight: Float): TextView = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.MATCH_PARENT, weight)
        text = t; gravity = Gravity.CENTER; textSize = 10f
        setTextColor(android.graphics.Color.WHITE); setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(3), dp(8), dp(3), dp(8))
    }

    private fun formatDisplayDate(iso: String): String {
        // yyyy-MM-dd -> 31.12.2026 for screen + print display only (stored
        // data / sortKey / DB format is untouched).
        // BUG FIX (2026-07-26, full-project audit): this still emitted
        // slashes, against the locked 2026-07-24 global dot-date rule.
        val parts = iso.take(10).split("-")
        return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
    }

    private fun tableDataRow(visitNo: Int, date: String, progress: String, paid: Double, due: Double, dayList: List<TimelineEntry>, hasRealTreatmentPayment: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(if (visitNo % 2 == 0) android.graphics.Color.parseColor("#F6F9FB") else android.graphics.Color.WHITE)
        }
        row.addView(cell(ordinal(visitNo), dp(36), 0f, "#0e7c7b", true))
        row.addView(cell(formatDisplayDate(date), dp(78), 0f, "#10223A", false))
        val prog = cell(progress.ifBlank { "—" }, 0, 1f, "#334155", false, left = true)
        row.addView(prog)
        val paidCell = cell(if (hasRealTreatmentPayment) money(paid) else "—", dp(64), 0f, "#0c8a4e", true)
        row.addView(paidCell)
        // TK-REQUESTED (2026-07-20): DUE column not shown on screen.

        // Progress edit — anyone, any time (saved on this day's first payment row)
        TripleTapEdit.attach(prog) { editProgressGroup(dayList) }
        // Paid edit — staff same-day only; master any time. If the day has
        // more than one payment (e.g. part cash + part online), pick which.
        if (hasRealTreatmentPayment) TripleTapEdit.attach(paidCell) { editPaidGroup(dayList) }
        return row
    }

    private fun editProgressGroup(dayList: List<TimelineEntry>) {
        // V1526: never save a Progress edit into a refund row just because it
        // happened earlier on the same date. Prefer the actual visit/progress holder.
        val target = dayList.firstOrNull {
            (it.payType.equals("treatment", true) || it.payType.equals("attendance_mark", true)) &&
                it.typedRemark.isNotBlank() && it.paymentId != null
        } ?: dayList.firstOrNull {
            it.payType.equals("treatment", true) && it.paymentId != null
        } ?: dayList.firstOrNull {
            it.payType.equals("attendance_mark", true) && it.paymentId != null
        } ?: return
        editProgress(target)
    }

    private fun editPaidGroup(dayList: List<TimelineEntry>) {
        val editablePayments = dayList.filter { it.payType.equals("treatment", true) && it.paymentId != null }
        if (editablePayments.isEmpty()) return
        if (editablePayments.size == 1) { editPaid(editablePayments[0]); return }
        val labels = editablePayments.map { "${money(it.paymentAmount)} · ${it.paymentMode}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Which payment do you want to Edit?"))
            .setItems(labels) { _, which -> editPaid(editablePayments[which]) }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun ordinal(n: Int): String {
        val s = when {
            n % 100 in 11..13 -> "th"
            n % 10 == 1 -> "st"; n % 10 == 2 -> "nd"; n % 10 == 3 -> "rd"; else -> "th"
        }
        return "$n$s".uppercase()
    }

    // TK-REQUESTED GLOBAL RULE (2026-07-24): every row's boxes must stretch
    // to equal height together (like Google Sheets) when one cell's text
    // wraps taller (e.g. a long Progress remark) -- same fix as Patient
    // Timeline's table.
    // TK-REPORTED BUG FIX (2026-07-25): MATCH_PARENT here was the WRONG way
    // to achieve that -- inside a WRAP_CONTENT row it can under-measure a
    // cell's own multi-line content and CLIP it instead of stretching
    // (exactly the cut-off text TK's photo-proof caught in Patient
    // Timeline's identical table, fixed there the same way). The Progress
    // column here has no maxLines limit at all, so a long remark was even
    // more likely to get clipped. WRAP_CONTENT sizes every cell to its own
    // real content -- never clips, whatever else is in the row.
    /* 🎨🔒 V894 (৩১.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"নিচের বক্সগুলি দেখুন"*):
       প্রতিটা ঘরের উচ্চতা আলাদাভাবে মাপা হতো (`WRAP_CONTENT`)। বাংলা লেখাওয়ালা
       PROGRESS ঘরটা একটু লম্বা হয়ে যেত, ফলে **শেষ সারিতে দাগগুলো মিলত না** —
       ঘরের নিচের রেখা এক জায়গায়, পাশেরটার আরেক জায়গায়।
       এখন এক সারির সব ঘর **একই উচ্চতার** (`MATCH_PARENT`) — এই ফাইলেরই
       `headCell()` আগে থেকে এটাই ব্যবহার করে, নতুন কিছু নয়।
       ⛔ লেখা · টাকা · তারিখ · এডিট (ট্রিপল-ট্যাপ) — কিচ্ছু বদলায়নি, শুধু মাপ। */
    private fun cell(t: String, w: Int, weight: Float, color: String, bold: Boolean, left: Boolean = false): TextView = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.MATCH_PARENT, weight)
        text = t; gravity = if (left) Gravity.CENTER_VERTICAL else Gravity.CENTER
        textSize = 12f; setTextColor(android.graphics.Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(6), dp(9), dp(6), dp(9))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT); setStroke(dp(1), android.graphics.Color.parseColor("#0B2B59"))
        }
    }

    // ---------------- edits ----------------

    private fun denyMaster() {
        android.widget.Toast.makeText(this, NoBengali.s("মাস্টারের অনুমতি লাগবে"), android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun editProgress(e: TimelineEntry) {
        val pid = e.paymentId ?: return
        val input = android.widget.EditText(this).apply {
            // 🚨 TK-REPORTED (27.07.2026): this pre-filled the edit box with the
            // finished note, so opening it on a payment with no real remark
            // dropped the app's own "₹10,000 · CASH" text into the box and saved
            // it back as if a person had written it. It now starts from what a
            // person actually typed -- blank when nothing was typed, exactly what
            // the staff expects to see before writing the day's Progress.
            setText(e.typedRemark); hint = NoBengali.s("আজ কী হলো — নিজে লিখুন বা নিচের চিপ চাপুন")
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p); minLines = 2; gravity = Gravity.TOP
        }
        /* 🟢🔒 V588 (23.08.2026) — ৯টা সাজেশন-চিপ এখন একটাই জায়গা থেকে
           (`TreatmentQuickNotes`), যাতে চারটে বাক্সেই হুবহু এক তালিকা।
           ⛔ লেখা ও আচরণ এক অক্ষরও বদলায়নি। */
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), 0); addView(input)
        }
        TreatmentQuickNotes.attach(this, container, input)
        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "🩺 Progress"))
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                // 🔵🔒 V533: Report Card-এর নিজের Progress লেখাও এখন `progress` ঘরে।
                savePayment(pid, JSONObject().put("progress", text)) { load() }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun editPaid(e: TimelineEntry) {
        val pid = e.paymentId ?: return
        if (!isMaster()) {
            // TK-REQUESTED (2026-07-25): free window widened from
            // "same-day only" to "the payment's own day OR the very next
            // day" -- staff can still correct a mistyped amount without
            // needing Master at all within that window. Beyond it, instead
            // of an outright denial, staff can send a Request that Master
            // approves/rejects from the Dashboard bell -- same pattern as
            // Backdate Payment requests.
            lifecycleScope.launch {
                val payDate = withContext(Dispatchers.IO) {
                    try {
                        // TK-REQUESTED (2026-07-27), ধাপ ২: this used to pull
                        // up to 5000 rows by mobile and hunt for one id. If the
                        // patient's number had since changed, the row was not in
                        // that list at all and the date came back blank -- the
                        // staff was then wrongly pushed into "ask Master" even
                        // inside their own free correction window. Asking for
                        // that one row by its own id is exact, and costs the
                        // cloud far less.
                        val arr = SupabaseClient.fetchList("payments", "id=eq.$pid", 1)
                        if (arr.length() > 0) arr.getJSONObject(0).optString("date", "") else ""
                    } catch (_: Throwable) { "" }
                }
                if (payDate.isNotBlank() && PaymentModel.withinFreeEditWindow(payDate)) {
                    paidAmountDialog(pid, e.paymentAmount)
                } else {
                    requestPaidEditDialog(pid, e.paymentAmount, payDate, e.paymentMode)
                }
            }
        } else paidAmountDialog(pid, e.paymentAmount)
    }

    // TK-REQUESTED ADDITION (2026-07-25): staff-side request when the free
    // 2-day window has passed -- writes a pending request only, the real
    // payment amount is untouched until Master approves it.
    private fun requestPaidEditDialog(pid: String, current: Double, payDate: String, mode: String) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
            hint = "Correct amount"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p)
        }
        val reason = android.widget.EditText(this).apply {
            hint = NoBengali.s("কেন বদলাতে হচ্ছে (Reason)")
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), 0)
            addView(android.widget.TextView(this@ReportCardActivity).apply {
                text = NoBengali.s("⚠️ পেমেন্টের দিন পার হয়ে গেছে (2 দিনের বেশি) — এখন থেকে Amount বদলাতে Master-এর অনুমতি লাগবে। এখানে অনুরোধ পাঠান।")
                textSize = 11f; setTextColor(android.graphics.Color.parseColor("#B45309"))
                setPadding(0, 0, 0, dp(8))
            })
            addView(input); addView(reason)
        }
        UppercaseInputUtil.applyToAll(box)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "🔒 Request Edit — Master Approval"))
            .setView(LinearLayout(this).apply { setPadding(0, 0, 0, 0); addView(box) })
            .setPositiveButton("Send Request") { _, _ ->
                val v = input.text.toString().trim().toDoubleOrNull()
                if (v == null || v <= 0) { android.widget.Toast.makeText(this, "Enter a valid amount", android.widget.Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            val repo = PaymentRepository(this@ReportCardActivity)
                            /* 🔵🔒 V522: টাকার সংশোধনের অনুরোধও **এই** রোগীর নামেই
                               যেতে হবে — নইলে এক নম্বরে দুজন থাকলে অন্যজনের নামে
                               অনুরোধ চলে যেত। ⛔ ফাঁকা হলে হুবহু আগের পথ। */
                            val patient = repo.findPatientByMobile(
                                mobile, preferPatientCode = preferPatientCode, preferRowId = preferRowId)
                                ?: return@withContext false
                            repo.requestPaymentEdit(
                                pid, patient, current, v, mode, payDate,
                                reason.text.toString().trim(), user.mobile, user.name.ifBlank { user.mobile }
                            )
                        } catch (_: Throwable) { false }
                    }
                    android.widget.Toast.makeText(
                        this@ReportCardActivity, NoBengali.s(if (ok) "Master-এর কাছে অনুরোধ পাঠানো হয়েছে ✅" else "পাঠানো যায়নি — আবার চেষ্টা করুন"),
                        android.widget.Toast.LENGTH_LONG
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                }
            }
            .setNegativeButton("Cancel", null)
            // 🔒 খাতার সারি B181 (TK, 30.07.2026): এই ডায়ালগের নিজের টাইটেলের পাহারা ছিল না।
            .show().also { PremiumAlert.paint(it) }
    }

    private fun paidAmountDialog(pid: String, current: Double) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
            setText(if (current > 0) "%.0f".format(current) else "")
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p)
        }
        UppercaseInputUtil.applyToAll(input)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "✏️ Edit Paid"))
            .setView(LinearLayout(this).apply { setPadding(dp(20), dp(12), dp(20), 0); addView(input) })
            .setPositiveButton("Update") { _, _ ->
                val v = input.text.toString().trim().toDoubleOrNull()
                if (v == null || v < 0) { android.widget.Toast.makeText(this, "Enter a valid amount", android.widget.Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                savePayment(pid, JSONObject().put("amount", v)) { load() }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun editEstimated(data: TimelineData) {
        val rowId = data.rowId
        if (rowId.isBlank()) { android.widget.Toast.makeText(this, "No registration record to set the bill on", android.widget.Toast.LENGTH_SHORT).show(); return }
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
            setText(if (data.billTotal > 0) "%.0f".format(data.billTotal) else "")
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p)
        }
        UppercaseInputUtil.applyToAll(input)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "✏️ Estimated Amount (Total Bill)"))
            .setView(LinearLayout(this).apply { setPadding(dp(20), dp(12), dp(20), 0); addView(input) })
            .setPositiveButton("Update") { _, _ ->
                val v = input.text.toString().trim().toDoubleOrNull()
                if (v == null || v < 0) { android.widget.Toast.makeText(this, "Enter a valid amount", android.widget.Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch {
                    val fields = JSONObject().put("bill", v)
                    val ok = withContext(Dispatchers.IO) {
                        try { SupabaseClient.updateById("patients", rowId, fields) } catch (_: Throwable) { false }
                    }
                    if (!ok) GenericUpdateQueue.queue(this@ReportCardActivity, "patients", rowId, fields)
                    android.widget.Toast.makeText(this@ReportCardActivity, if (ok) "Updated ✅" else "Failed — retry", android.widget.Toast.LENGTH_SHORT).show()
                    if (ok) load()
                }
            }
            .setNegativeButton("Cancel", null)
            // 🔒 খাতার সারি B181: এই ডায়ালগে এখন সরাসরি বাংলা নেই, তবু
            // ধারাবাহিকতার জন্য পাহারা বসানো হলো।
            .show().also { PremiumAlert.paint(it) }
    }

    private fun savePayment(pid: String, fields: JSONObject, onDone: () -> Unit) {
        val stamped = JSONObject(fields.toString()).put("updatedAt", SupabaseClient.localWriteStamp())
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { SupabaseClient.updateById("payments", pid, stamped) } catch (_: Throwable) { false }
            }
            if (!ok) GenericUpdateQueue.queue(this@ReportCardActivity, "payments", pid, stamped)
            android.widget.Toast.makeText(this@ReportCardActivity, if (ok) "Saved ✅" else "Failed — retry", android.widget.Toast.LENGTH_SHORT).show()
            if (ok) onDone()
        }
    }

    // ---------------- print ----------------

    private fun printReport() {
        android.widget.Toast.makeText(this, NoBengali.s("প্রিন্ট তৈরি হচ্ছে…"), android.widget.Toast.LENGTH_SHORT).show()
        ReportCardPrinter.print(this, mobile, user, preferRowId, preferPatientCode)   // 🔵 V522: ছাপাও এই রোগীরই
    }
}
