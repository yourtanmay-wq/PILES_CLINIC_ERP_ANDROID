package com.tkbiswas.pilesclinic.native

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityFollowCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Native rebuild -- Follow-up Calendar.
 *
 * Mirrors openRealFollowCalendar()/openFollowCalendarDate(): a month grid where
 * each date shows how many follow-ups are due that day (by nextFollow date);
 * tapping a date lists those follow-ups. Reuses the existing native
 * FollowUpRepository (same branch-scoping and data as the Follow-up screen), so
 * no follow-up fetching logic is duplicated.
 *
 * SCOPED LIMITATION (honest): this screen shows the "Inquiry" stage follow-ups
 * (the default the WebView opens the calendar on). Other stages use the same
 * mechanism and can be added with a stage selector later.
 */
class FollowCalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowCalendarBinding
    private lateinit var repository: FollowUpRepository
    private lateinit var user: NativeUser

    private val stage = "Inquiry"
    private val cal = Calendar.getInstance()
    private var itemsByDate: Map<String, List<FollowUpItem>> = emptyMap()

    // TK-REQUESTED (2026-07-17): "did you write a remark after that call?"
    // reminder. No READ_PHONE_STATE / call-listening permission used or
    // needed -- we simply remember which patient the call button was tapped
    // for, and check it in onResume(), which Android already calls every
    // time this screen comes back to the foreground (e.g. after the dialer
    // closes). If the app is killed in the background this won't fire; that
    // is the known, accepted trade-off of the no-permission approach TK
    // approved (2026-07-17) over the heavier call-listening-service approach.
    private var pendingCallItem: FollowUpItem? = null

    override fun onResume() {
        super.onResume()
        val item = pendingCallItem
        if (item != null) {
            pendingCallItem = null
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Update remark?"))
                .setMessage("Add a remark for ${item.name.ifBlank { item.mobile }} after that call?")
                .setPositiveButton("Add Remark") { _, _ -> editRemark(item) {} }
                .setNegativeButton("Not now", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = FollowUpRepository(this)

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPrevMonth.setOnClickListener { cal.add(Calendar.MONTH, -1); render() }
        binding.btnNextMonth.setOnClickListener { cal.add(Calendar.MONTH, 1); render() }

        setupBranchPicker { loadThenRender() }   // খাতার সারি B43

        loadThenRender()
    }


    /**
     * 🔒 TK-REQUESTED (28.07.2026, খাতার সারি B43): *"আমি মাস্টার এডমিন — সমস্ত
     * প্রজেক্টের সব জায়গায় আমি চাইলে ব্রাঞ্চ চুজ করতে পারি, চাইলে অল ব্রাঞ্চ
     * রাখতে পারি।"*
     *
     * ⛔ Follow-up পর্দায় TK-এর আগেই পাশ করা **হুবহু একই বাক্স ও একই নিয়ম** —
     * নতুন কোনো ডিজাইন তৈরি করা হয়নি।
     * ⛔ **স্টাফ ও ডাক্তারের জন্য এক অক্ষরও বদলায়নি** — বাক্সটাই তাঁদের পর্দায়
     * থাকে না, তাঁরা আগের মতোই শুধু নিজের ব্রাঞ্চ দেখেন।
     */
    private var pickedBranch: String = ""

    /** কোন ব্রাঞ্চের তথ্য দেখানো হবে — একটাই জায়গা থেকে সিদ্ধান্ত, তাই জমানো ও
     *  নতুন তালিকা কখনো আলাদা ব্রাঞ্চ দেখাতে পারবে না। */
    /** 🟢🔒 V398: মাস্টার কিছু না-বাছলে "" ফেরে — তখন কিচ্ছু আনা হয় না। */
    private fun shownBranch(): String =
        if (user.role == "master") pickedBranch else user.branch

    private fun setupBranchPicker(onPicked: () -> Unit) {
        if (user.role != "master") {
            binding.branchPicker.visibility = View.GONE
            return
        }
        // 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): বাছাইটা আর এই পর্দার নিজের নয় —
        //   পুরো অ্যাপের একটাই জায়গা `BranchFilterStore`। তাই একবার বাছলেই সব
        //   পর্দায় সেটাই থাকে, বারবার বাছতে হয় না।
        pickedBranch = BranchFilterStore.get(this)
        binding.branchPicker.visibility = View.VISIBLE
        binding.branchPicker.text = BranchFilterStore.pillText(this)
        binding.branchPicker.setOnClickListener {
            val branches = BranchFilterStore.choices()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                    pickedBranch = branches[which]
                    BranchFilterStore.set(this@FollowCalendarActivity, pickedBranch)
                    binding.branchPicker.text = BranchFilterStore.pillText(this@FollowCalendarActivity)
                    dialog.dismiss()
                    onPicked()
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    private fun loadThenRender() {
        binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        // 🟢🔒 V398: মাস্টার এখনো ব্রাঞ্চ না-বাছলে **একটাও ক্লাউড-অনুরোধ যাবে না**।
        //   ⚠️ এখানে গার্ডটা জরুরি — FollowUpRepository.branchScopeFilter() (লাইন ১১৫)
        //   ফাঁকা নামকে "ছাঁকনি নেই" ধরে, ফলে গার্ড ছাড়া সব ব্রাঞ্চ নেমে যেত।
        if (BranchFilterStore.notChosen(this, user)) {
            itemsByDate = emptyMap()
            binding.calendarGrid.removeAllViews()
            binding.calendarGrid.addView(TextView(this).apply {
                text = BranchFilterStore.ASK_TEXT
                textSize = 13f
                setTextColor(Color.parseColor("#0A5C33"))
            })
            return
        }
        // TK-REQUESTED (2026-07-24): real cache-first, reusing the SAME
        // already-tested loadCachedTab()/fetchTab() pair FollowUpActivity
        // already uses for this exact stage/branch -- no new cache storage,
        // no change to render()'s logic, only WHEN it first runs.
        val cached = repository.loadCachedTab(stage, shownBranch())   // খাতার সারি B43
        if (!cached.isNullOrEmpty()) {
            itemsByDate = cached.filter { it.nextFollow.isNotBlank() }.groupBy { it.nextFollow.take(10) }
            render()
        } else {
            binding.calendarGrid.removeAllViews()
            binding.calendarGrid.addView(TextView(this).apply {
                text = "Loading..."
                textSize = 13f
                setTextColor(Color.parseColor("#8A93A6"))
            })
        }
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { repository.fetchTab(stage, shownBranch(), user.name, user.mobile) }   // খাতার সারি B43
            itemsByDate = items
                .filter { it.nextFollow.isNotBlank() }
                .groupBy { it.nextFollow.take(10) }
            binding.progressLoad.visibility = View.GONE
            render()
        }
    }

    private fun render() {
        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
        binding.tvMonth.text = monthFmt.format(cal.time)

        val grid = binding.calendarGrid
        grid.removeAllViews()

        // Weekday headers (Su..Sa)
        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { addHeaderCell(grid, it) }

        val monthCal = cal.clone() as Calendar
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstWeekday = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday=0
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val year = monthCal.get(Calendar.YEAR)
        val month = monthCal.get(Calendar.MONTH) + 1

        repeat(firstWeekday) { addEmptyCell(grid) }
        for (d in 1..daysInMonth) {
            val iso = String.format(Locale.US, "%04d-%02d-%02d", year, month, d)
            val count = itemsByDate[iso]?.size ?: 0
            addDayCell(grid, d, count, iso)
        }

        // Today Pending Call banner — direct shortcut to today's due follow-ups.
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val todayCount = itemsByDate[todayIso]?.size ?: 0
        if (todayCount > 0) {
            binding.tvTodayPending.visibility = View.VISIBLE
            binding.tvTodayPending.text = "📞 Today Pending Call — $todayCount"
            binding.tvTodayPending.setOnClickListener { showDayList(todayIso) }
        } else {
            binding.tvTodayPending.visibility = View.GONE
        }
    }

    private fun cellWidth(): Int =
        (resources.displayMetrics.widthPixels - (16 * resources.displayMetrics.density).toInt()) / 7

    private fun addHeaderCell(grid: GridLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#6B7280"))
            textSize = 12f
            setPadding(0, 8, 0, 8)
        }
        val params = GridLayout.LayoutParams().apply { width = cellWidth() }
        grid.addView(tv, params)
    }

    private fun addEmptyCell(grid: GridLayout) {
        val v = View(this)
        val params = GridLayout.LayoutParams().apply { width = cellWidth(); height = (52 * resources.displayMetrics.density).toInt() }
        grid.addView(v, params)
    }

    private fun addDayCell(grid: GridLayout, day: Int, count: Int, iso: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                cornerRadius = 8f * resources.displayMetrics.density
                setColor(if (count > 0) Color.parseColor("#FDE1E1") else Color.TRANSPARENT)
            }
            background = bg
        }
        val dayText = TextView(this).apply {
            text = day.toString()
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.parseColor("#111827"))
        }
        container.addView(dayText)
        if (count > 0) {
            val badge = TextView(this).apply {
                text = count.toString()
                gravity = Gravity.CENTER
                textSize = 10.5f
                setTextColor(Color.parseColor("#E5484D"))
            }
            container.addView(badge)
            container.setOnClickListener { showDayList(iso) }
        }
        val params = GridLayout.LayoutParams().apply {
            width = cellWidth()
            height = (52 * resources.displayMetrics.density).toInt()
            setMargins(2, 2, 2, 2)
        }
        grid.addView(container, params)
    }

    // TK-APPROVED (2026-07-17): call button + tappable/editable remark
    // (with the date it was written) added to each card in this popup.
    // Same green header/card shell as before (TK-approved 2026-07-15) --
    // only what's INSIDE each card changed: added branch+disease tags,
    // a call button, and made the remark line editable (reusing the same
    // proven FollowUpRepository.updateRemark()/5-call-guard logic already
    // used on the main Follow-up screen -- nothing new invented here).
    private fun showDayList(iso: String) {
        val items = itemsByDate[iso].orEmpty()
        if (items.isEmpty()) return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(20).toFloat()
            }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(Color.parseColor("#0A5428"), Color.parseColor("#0EA25F"))
                cornerRadii = floatArrayOf(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        header.addView(TextView(this).apply {
            text = "Follow-ups on ${FollowUpModel.displayDate(iso)}"
            textSize = 15.5f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.WHITE)
        })
        header.addView(TextView(this).apply {
            text = "${items.size} scheduled"
            textSize = 11.5f; setTextColor(Color.parseColor("#DCF3E6"))
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(2); layoutParams = p
        })
        root.addView(header)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(4))
        }

        lateinit var dialog: AlertDialog

        fun tagView(text: String, bgColor: String): TextView = TextView(this).apply {
            this.text = text; textSize = 9f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.WHITE)
            setPadding(dp(7), dp(3), dp(7), dp(3))
            background = GradientDrawable().apply { setColor(Color.parseColor(bgColor)); cornerRadius = dp(6).toFloat() }
        }

        items.forEach { i ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F7F9FA")); cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#E4E8ED"))
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(8); layoutParams = lp
            }
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(10), dp(12), dp(6))
                gravity = Gravity.CENTER_VERTICAL
            }
            val stripe = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(44)).apply { marginEnd = dp(9) }
                background = GradientDrawable().apply { setColor(Color.parseColor("#0EA25F")); cornerRadius = dp(2).toFloat() }
            }
            val who = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            who.addView(TextView(this).apply {
                text = i.name.ifBlank { i.mobile }; textSize = 14f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#10223A"))
            })
            who.addView(TextView(this).apply {
                text = i.mobile; textSize = 11.5f; setTextColor(Color.parseColor("#5B6B81"))
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = dp(1); layoutParams = p
            })
            val tagRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = dp(4); layoutParams = p
            }
            val branchTag = tagView(i.branch.uppercase(Locale.US), "#1067D8")
            val branchTagLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            branchTagLp.marginEnd = dp(5)
            tagRow.addView(branchTag, branchTagLp)
            if (i.disease.isNotBlank()) tagRow.addView(tagView(i.disease.uppercase(Locale.US), "#0EA25F"))
            who.addView(tagRow)

            val callBtn = TextView(this).apply {
                text = "\uD83D\uDCDE" // 📞
                textSize = 15f; gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(Color.parseColor("#078C18")); cornerRadius = dp(9).toFloat() }
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                isClickable = true; isFocusable = true
                setOnClickListener { pendingCallItem = i; callNumber(i.mobile) }
            }

            topRow.addView(stripe); topRow.addView(who); topRow.addView(callBtn)
            card.addView(topRow)

            // Divider
            card.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                    leftMargin = dp(12); rightMargin = dp(12)
                }
                setBackgroundColor(Color.parseColor("#EEF1F5"))
            })

            val remarkRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(8), dp(12), dp(10))
                gravity = Gravity.TOP
            }
            val remarkTextCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            remarkTextCol.addView(TextView(this).apply {
                text = i.lastRemark.ifBlank { "No remark" }; textSize = 11.5f
                setTextColor(Color.parseColor("#18251D"))
            })
            val whenText = FollowUpModel.displayShort(i.updatedAt)
            if (whenText.isNotBlank()) {
                remarkTextCol.addView(TextView(this).apply {
                    text = NoBengali.s("লেখা হয়েছিল: $whenText"); textSize = 9.5f
                    setTextColor(Color.parseColor("#98A2B3"))
                    val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    p.topMargin = dp(2); layoutParams = p
                })
            }
            val editLink = TextView(this).apply {
                text = "Edit"; textSize = 10.5f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#1067D8"))
                isClickable = true; isFocusable = true
                setOnClickListener { editRemark(i) { dialog.dismiss() } }
            }
            remarkRow.addView(remarkTextCol); remarkRow.addView(editLink)
            card.addView(remarkRow)

            col.addView(card)
        }
        val scroll = android.widget.ScrollView(this).apply {
            addView(col)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(460))
        }
        root.addView(scroll)

        val close = TextView(this).apply {
            text = "CLOSE"; gravity = Gravity.CENTER; textSize = 13f
            setTypeface(typeface, Typeface.BOLD); setTextColor(Color.parseColor("#5B6B81"))
            setPadding(0, dp(14), 0, dp(16)); isClickable = true; isFocusable = true
        }
        root.addView(close)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        dialog = AlertDialog.Builder(this).setView(root).setCancelable(true).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        close.setOnClickListener { dialog.dismiss() }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    // TK-REQUESTED (2026-07-24): "everywhere calling is possible in the
    // project" -- now uses the shared CallChooser.kt (Phone/Superfone/etc.
    // picker, Truecaller excluded) instead of opening the OS default
    // dialer directly.
    private fun callNumber(mobile: String) {
        try {
            CallChooser.open(this, mobile)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No phone app found", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔒 খাতার সারি B78 (TK, 29.07.2026): নিয়মটা এখন **এক জায়গায়** —
    // `FollowUpRepository.ensureFollowUpRowId()`। আগে এই ফাইলে আলাদা কপি ছিল,
    // ফলে দুই পর্দায় দুই রকম আচরণের ভয় ছিল। ওই ফাংশন মোবাইল ধরে আসল সারিটা
    // খোঁজে, **আর সারিটা সত্যিই না থাকলে তৈরি করে দেয়** — নইলে বদলটা এমন
    // আইডিতে যেত যার কোনো সারিই নেই, আর Supabase তাতেও "200 OK" বলে বলে
    // অ্যাপ ভুল করে "হয়ে গেছে" দেখাত।
    // ⛔ নেট খারাপ হলে নতুন সারি তৈরি হয় না — পুরনো আচরণেই ফিরে যায়।
    private fun resolveFollowUpId(item: FollowUpItem): String =
        repository.ensureFollowUpRowId(item)

    // Reused logic from FollowUpActivity.showRemarkDialog()/showFiveCallDecision()
    // -- same 5-call guard, same repository calls -- just triggered from this
    // popup instead of the main list, so the business rule can't be skipped
    // by editing a remark from the Calendar screen.
    private fun editRemark(item: FollowUpItem, onDone: () -> Unit) {
        if (item.stage == "Inquiry" && item.callCount >= 5) {
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "5 calls completed"))
                .setMessage("${item.name.ifBlank { item.mobile }} — called 5 times. Continue this enquiry or close it?")
                .setPositiveButton("Continue (reset)") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { repository.resetCallCount(resolveFollowUpId(item)) }
                        onDone(); loadThenRender()
                    }
                }
                .setNegativeButton("Cancel Entry") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { repository.updateStatus(
                            resolveFollowUpId(item), "Cancelled", "5-call limit — closed", user.mobile,
                            mobileHint = item.mobile, stageHint = item.stage
                        ) }
                        onDone(); loadThenRender()
                    }
                }
                .show().also { PremiumAlert.paint(it) }
            return
        }
        // TK-REQUESTED (2026-08-13): every counted follow-up call needs a
        // newly written discussion remark. Never prefill the previous remark,
        // because tapping Save on old text must not create another call count.
        val input = EditText(this).apply { hint = "Write the new call discussion" }
        UppercaseInputUtil.applyToAll(input)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Update Remark — ${item.name.ifBlank { item.mobile }}"))
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val remark = input.text.toString().trim()
                // No remark = no history entry, no Last Call update and no
                // call-count increase. This matches the main Follow-up screen
                // and the Web Follow-up form.
                if (remark.isBlank()) {
                    Toast.makeText(this@FollowCalendarActivity, "Remarks required — call not counted", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // V215 (§17, 31.07.2026): সব stage-এ post-call remark = একটা
                // Completed Call (Last Call Date/Time আপডেট, Signal +১, দিনে-একবার
                // de-dup)। আগে শুধু Inquiry-তে গোনা হত।
                val isEnquiryCall = true
                // 🔒 TK-এর নিয়ম (28.07.2026): Save চাপার সঙ্গে সঙ্গে ক্যালেন্ডার
                // খুলবে — ক্লাউডের উত্তরের জন্য স্টাফকে বসিয়ে রাখা যাবে না।
                // Follow-up পর্দার মতোই একই ব্যবস্থা।
                Toast.makeText(this@FollowCalendarActivity, "Remark updated", Toast.LENGTH_SHORT).show()
                showMandatoryNextFollowPrompt(item)
                BackgroundWork.run {
                    val ok = repository.updateRemark(resolveFollowUpId(item), remark, user.name, isEnquiryCall)
                    if (ok && !isFinishing && !isDestroyed) {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) { onDone(); loadThenRender() }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            // 🔒 খাতার সারি B181: এই ডায়ালগে এখন সরাসরি বাংলা নেই, তবু
            // ধারাবাহিকতার জন্য পাহারা বসানো হলো।
            .show().also { PremiumAlert.paint(it) }
    }

    // TK-REQUESTED (2026-07-24, simplified per TK's follow-up instruction
    // "সাথে সাথে Automatic Calendar Popup"): same as FollowUpActivity's own
    // showMandatoryNextFollowPrompt() -- the date-picker opens DIRECTLY and
    // immediately after Remark save, no intermediate button-tap step. Still
    // fully MANDATORY -- setCancelable(false), and the DatePicker's own
    // built-in Cancel button is hidden after it shows.
    private fun showMandatoryNextFollowPrompt(item: FollowUpItem) {
        Toast.makeText(this, NoBengali.s("এখন পরের Follow-up Call তারিখ দিন"), Toast.LENGTH_SHORT).show()
        val cal = java.util.Calendar.getInstance()
        // 🔴🔴🟢 FollowUpActivity.kt-এর showMandatoryNextFollowPrompt()-এ যে
        // একই বাগ ঠিক হলো (TK-রিপোর্ট, ছবিসহ — বিগত তারিখ ক্যালেন্ডারে
        // ডিফল্ট বসত), ঠিক এই ফাইলের নিজস্ব কপিতেও একই বাগ ছিল (TK-এর
        // নিয়ম ৬.২, প্রজেক্ট-জোড়া খোঁজা)। বিগত `item.nextFollow` আর
        // ডিফল্ট হবে না, আজকের তারিখই থাকবে।
        val todayIso = FollowUpModel.today()
        try {
            if (item.nextFollow.isNotBlank() && item.nextFollow >= todayIso) {
                val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(item.nextFollow)
                if (parsed != null) cal.time = parsed
            }
        } catch (_: Exception) { }
        val picker = android.app.DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, day ->
            val iso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, day)
            /* 🔴🔒 V1222 ③ (০৮.০৯.২০২৬) — FollowUpActivity-র হুবহু একই সুরক্ষা
               (নিয়ম ৭)। সঙ্গে সঙ্গের বার্তা অটুট; নিচে কাজটা সত্যিই না বসলে
               পরে একটা সৎ খবর যায়। */
            Toast.makeText(this@FollowCalendarActivity, "Next follow-up set", Toast.LENGTH_SHORT).show()
            BackgroundWork.run {
                val ok = repository.updateNextFollowSaved(resolveFollowUpId(item), iso).also { s2 ->
                    if (!s2) runOnUiThread {
                        if (!isFinishing && !isDestroyed) Toast.makeText(
                            this@FollowCalendarActivity,
                            "Not sent yet — saved on this phone, will send when online",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } || true
                if (ok && item.stage != "Inquiry") {
                    try {
                        ChamberAttendanceRepository.markExpected(this@FollowCalendarActivity, item.mobile, item.name, item.branch, iso, user.mobile)
                    } catch (_: Throwable) { }
                }
                if (ok && !isFinishing && !isDestroyed) {
                    runOnUiThread { if (!isFinishing && !isDestroyed) loadThenRender() }
                }
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
        picker.setTitle(NoBengali.s("⏰ Next Follow-up Call — required"))
        picker.setCancelable(false)
        picker.setCanceledOnTouchOutside(false)
        picker.setOnShowListener {
            // 🔒 খাতার সারি B158: এই তারিখ-বাছাই পপ-আপের শিরোনামে বাংলা আছে
            // আর এটা PremiumAlert.paint দিয়ে যায় না — তাই এখানেই ঢাকা হলো।
            try { NoBengali.installDialog(picker) } catch (_: Throwable) { }
            picker.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
        }
        picker.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(picker) } catch (_: Throwable) { }   // 🤫 V774
    }
}
