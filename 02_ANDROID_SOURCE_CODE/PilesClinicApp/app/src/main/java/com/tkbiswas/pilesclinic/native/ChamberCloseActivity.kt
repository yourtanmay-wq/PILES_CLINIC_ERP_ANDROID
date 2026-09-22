package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityChamberCloseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🔒 TK-APPROVED (28.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B46) — "CHAMBER
 * CLOSE / চেম্বার বন্ধ করুন"।
 *
 * TK-এর কথা: *"তাহলে সারা জীবন কীভাবে খোলা থাকবে... ওখানে বন্ধ করার ব্যবস্থা
 * করুন। তাছাড়া পুরো স্ক্রিন জুড়ে এইভাবে যেন ওপেন হয়ে না থাকে — এটা দরকার,
 * মডিউলের মধ্যে রাখেন... চেম্বার ক্লোজ একটা মেনু বারের মধ্যে রাখুন, আর সেখানে
 * পুরনো দিনের চেম্বার বন্ধ না করলে সেটা যেন বন্ধ করা যায়।"*
 *
 * যা হলো:
 *  ১. ড্যাশবোর্ডের লম্বা লাল তালিকাটা সেখান থেকে **তুলে দেওয়া হয়েছে** (ঘরটা
 *     মোছা হয়নি, শুধু চিরকাল লুকানো — দরকারে ফেরানো যায়)।
 *  ২. সেই তালিকা এখন এই পর্দায়, ☰ মেনুর "Chamber Close" ঘর থেকে খোলে।
 *  ৩. প্রতিটা দিনের পাশে **বন্ধ করুন** — চাপলে ওই দিনের ও ওই ব্রাঞ্চের
 *     Chamber Date পর্দা খোলে এবং **আগের সেই একই নিয়মে** (রিভিউ · ৩-ট্যাপ ·
 *     Confirm & Print) চেম্বার বন্ধ হয়।
 *
 * ⛔ এখানে টাকার কোনো হিসাব নেই · কোনো সারি লেখা হয় না · কোনো পুরনো নিয়ম
 *    বদলায়নি। শুধু পড়া হয় (দুটো অনুরোধ, দু'মিনিটের স্মৃতি সহ)।
 */
class ChamberCloseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChamberCloseBinding
    private lateinit var user: NativeUser

    private val branches = BranchFilterStore.choices()   // 🟢🔒 V398: একটাই তালিকা
    private var selectedBranch = ""                     // 🟢🔒 V398: মনে-রাখা মানের প্রতিচ্ছবি

    /** কত দিন পিছনে দেখা হবে। পুরনো দিন যেন কখনো আটকে না থাকে, তাই এক মাস। */
    private val lookBackDays = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChamberCloseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.wire(this)

        val session = NativeSession.current(this)
        if (session == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish(); return
        }
        user = session
        // 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): আগে জোর করে "All" বসত; এখন শেষবার
        //   বাছা ব্রাঞ্চটাই বসে (পুরো অ্যাপে একটাই জায়গা — BranchFilterStore)।
        selectedBranch = if (user.role == "master") BranchFilterStore.get(this) else user.branch

        binding.btnBack.setOnClickListener { finish() }

        if (user.role == "master") {
            // 🔒 TK-APPROVED (29.07.2026, খাতার সারি B84): ব্রাঞ্চ বাছার ঘর এখন
            // হেডারের ডানের পিলে — সব পর্দার এক মডেল। পুরনো Spinner লুকানো
            // আছে, মোছা হয়নি; নিয়ম সেটাই চালায়, পিল শুধু তাকে বেছে দেয়।
            binding.branchPicker.visibility = View.VISIBLE
            binding.spBranch.adapter = android.widget.ArrayAdapter(
                this, com.tkbiswas.pilesclinic.R.layout.item_branch_spinner, branches
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            binding.branchPicker.text = BranchFilterStore.pillText(this)
            binding.spBranch.setSelection(branches.indexOf(selectedBranch).coerceAtLeast(0))
            binding.spBranch.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val picked = branches.getOrNull(position) ?: BranchFilterStore.ALL
                    if (picked != selectedBranch) {
                        selectedBranch = BranchFilterStore.set(this@ChamberCloseActivity, picked)   // 🟢 V398
                        load()
                    }
                    binding.branchPicker.text = BranchFilterStore.pillText(this@ChamberCloseActivity)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
            // 🔒 খাতার সারি B84 — Follow-up-এর হুবহু একই পপ-আপ।
            binding.branchPicker.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "Branch"))
                    .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                        binding.spBranch.setSelection(which)
                        // 🚨 খাতার সারি B126 (TK, 29.07.2026 সন্ধ্যা ৬.২৩): লুকানো
                        // (`gone`) Spinner layout হয় না বলে তার `onItemSelected`
                        // চলত না — বাছাই হারিয়ে যেত। কাজটা এখন এখানেই সরাসরি হয়।
                        val picked = branches.getOrNull(which) ?: BranchFilterStore.ALL
                        if (picked != selectedBranch) {
                            selectedBranch = BranchFilterStore.set(this@ChamberCloseActivity, picked)   // 🟢 V398
                            load()
                        }
                        binding.branchPicker.text = BranchFilterStore.pillText(this@ChamberCloseActivity)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show().also { PremiumAlert.paint(it) }
            }
        } else {
            binding.branchPicker.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // একটা দিন বন্ধ করে ফিরে এলে তালিকা যেন সঙ্গে সঙ্গে ছোট হয়।
        ChamberUnclosedRepository.clearCache()
        load()
    }

    private fun load() {
        // 🟢🔒 V398: ব্রাঞ্চ না-বাছা থাকলে কোনো অনুরোধ যাবে না।
        //   ⚠️ গার্ডটা জরুরি — ChamberUnclosedRepository (লাইন ৭৮) ফাঁকা নামকে
        //   "ছাঁকনি নেই" ধরে, তাই গার্ড ছাড়া সব ব্রাঞ্চ নেমে যেত।
        if (BranchFilterStore.notChosen(this, user)) {
            binding.tvNote.text = BranchFilterStore.ASK_TEXT
            binding.listBox.removeAllViews()
            return
        }
        // 🟢🔒 V604 (২৪.০৮.২০২৬, TK-রিপোর্ট — "পুরনো ডেটা থাকলেও Loading
        // কেন দেখায়?") — যাচাই করে ধরা পড়ল: এই স্ক্রিন সবসময়ই "Loading..."
        // লিখে দিত, নেটে যাওয়ার আগে মেমরির ক্যাশ (নিচের কল নিজেই ব্যবহার
        // করে) একবারও দেখত না। এখন ক্যাশ তাজা থাকলে "Loading..." একবারও
        // দেখানো হয় না — সরাসরি তালিকা বসে যায়, নেট-কলও বাদ যায় না
        // (নিচের কল এখনো হয়, পিছনে গিয়ে হালনাগাদ করে)।
        val cached = ChamberUnclosedRepository.peekCached(selectedBranch, lookBackDays)
        if (cached != null) render(cached) else binding.tvNote.text = "Loading..."
        lifecycleScope.launch {
            val days = withContext(Dispatchers.IO) {
                try { ChamberUnclosedRepository.findUnclosedCached(this@ChamberCloseActivity, selectedBranch, lookBackDays) }
                catch (_: Throwable) { emptyList() }
            }
            if (isFinishing || isDestroyed) return@launch
            render(days)
        }
    }

    private fun render(days: List<ChamberUnclosedRepository.UnclosedDay>) {
        val box = binding.listBox
        box.removeAllViews()

        if (days.isEmpty()) {
            binding.tvNote.text = "✅ All days' chambers are closed — nothing pending"
            return
        }
        binding.tvNote.text = "⚠️ These days' chambers are not closed yet — ${days.size}"

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        for (u in days) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = androidx.core.content.ContextCompat.getDrawable(
                    this@ChamberCloseActivity, com.tkbiswas.pilesclinic.R.drawable.bg_unclosed_card
                )
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isClickable = true
                isFocusable = true
                layoutParams = android.widget.LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(dp(10), 0, dp(10), dp(8))
                }
                setOnClickListener { openDay(u, autoClose = false) }
            }

            val left = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
            }
            left.addView(android.widget.TextView(this).apply {
                text = DateUtil.display(u.date)
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#10223A"))
            })
            left.addView(android.widget.TextView(this).apply {
                text = u.branch
                textSize = 11f
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            })
            row.addView(left)

            row.addView(android.widget.TextView(this).apply {
                text = NoBengali.s("${u.arrived} জন\n₹" + "%,.0f".format(u.money))
                textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0F7A3D"))
                gravity = android.view.Gravity.END
                setPadding(dp(8), 0, dp(8), 0)
            })

            row.addView(android.widget.TextView(this).apply {
                // 🔤🔒 V832 — একই কারণ (উপরে WorkNotebookActivity-তে বিস্তারিত)।
                //    `NoBengali`-র নিজের অনুবাদই বসানো হলো ⇒ কিশানগঞ্জে বদল নেই।
                text = NoBengali.s("Close")
                textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                background = androidx.core.content.ContextCompat.getDrawable(
                    this@ChamberCloseActivity, com.tkbiswas.pilesclinic.R.drawable.bg_btn_choco
                )
                setPadding(dp(11), dp(8), dp(11), dp(8))
                isClickable = true
                isFocusable = true
                setOnClickListener { openDay(u, autoClose = true) }
            })

            box.addView(row)
        }
    }

    /**
     * ⛔ বন্ধ করার কাজটা এখানে করা হয় না — ইচ্ছে করেই। ওই দিনের Chamber Date
     * পর্দাটাই খোলা হয়, কারণ রিভিউ · সংশোধন · Confirm & Print · বন্ধের চিহ্ন —
     * সব ওখানে আগে থেকেই প্রমাণিত অবস্থায় আছে। তাই টাকার হিসাবে হাত পড়ে না।
     */
    private fun openDay(u: ChamberUnclosedRepository.UnclosedDay, autoClose: Boolean) {
        startActivity(
            Intent(this, ChamberAttendanceActivity::class.java)
                .putExtra("openDate", u.date)
                .putExtra("openBranch", u.branch)
                .putExtra("autoClose", autoClose)
        )
    }
}
