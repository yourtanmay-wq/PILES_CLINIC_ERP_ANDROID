package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.databinding.ActivityMoreMenuBinding
import com.tkbiswas.pilesclinic.security.SettingsActivity

/**
 * Native rebuild -- "More" menu. Replaces the old WebView Menu: every item here
 * is a native screen. Reports / Backup / Trash are Master-only (matching menu()'s
 * role list); Draft is available to Master and Staff.
 *
 * 🔒 TK-LOCKED DESIGN (04.08.2026, "Premium" ফটো-প্রুফে "লক করুন" — ধাপে
 * ধাপে অনুমোদিত): পুরো "More" স্ক্রিন নতুন ডিজাইনে (activity_more_menu.xml
 * দেখুন — সবুজ গ্রেডিয়েন্ট হেডার, "Management"/"Security & Data" সেকশন,
 * সাদা কার্ড + আউটলাইন আইকন-ব্যাজ + সেরিফ শিরোনাম)। প্রতিটা বোতাম এখন আগের
 * মতো `Button` নয়, `LinearLayout` (আইকন+লেখা+চেভরন একসাথে) — তাই এই পুরো
 * ফাইলটাই নতুন করে লেখা হলো।
 *
 * ⛔ **যা এক অক্ষরও বদলায়নি:** প্রতিটা বোতামের ক্লিক-গন্তব্য (কোন Activity
 * খোলে), role-ভিত্তিক দেখা/লুকানোর নিয়ম (Master-only vs সবার), Logout-এর
 * confirm-ডায়ালগ, Staff Photo picker, Chamber Close-এর বাকি-দিন গোনা।
 *
 * 🔒 **পুরনো দুটো আসল বাগের (B374, B345) ঝুঁকি এখন থেকে কম, বেশি নয়:**
 * আগে GridLayout-এর ভিতরে জোর করে ভিউ ঢোকানো/সরানো হতো (ভাঙনের কারণ)।
 * এখন প্রতিটা "সারি" নিজেই একটা ছোট, স্বাধীন LinearLayout (rowManagement1/
 * rowManagement2/rowSecurity1/rowSecurity2) — কোনো বোতাম লুকালে
 * `hideItem()` শুধু সেই একটা সারি থেকেই ভিউ সরায় (বাকি ৩টা সারি অস্পৃশ্য),
 * আর দুটোই লুকালে পুরো সারিটা GONE হয়ে যায় (ফাঁকা গর্ত থাকে না)। "My
 * Profile"/"Staff Profiles"/"Income & Expense" এখন স্বাধীন, পূর্ণ-প্রস্থ
 * কার্ড (আগের মতো রানটাইমে GridLayout-এর ভিতরে ভুলভাবে addView() করা হয়
 * না) — শুধু সাধারণ visibility=GONE/VISIBLE, যেটা vertical LinearLayout-এ
 * সবসময় নিরাপদ।
 */
class MoreMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoreMenuBinding
    private lateinit var user: NativeUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        binding.btnBack.setOnClickListener { finish() }
        // 🆕🔒 TK-নির্দেশ (05.08.2026): Dialer এখন Dashboard-এর টাইল থেকেই
        // খোলে (দেখুন DashboardActivity.kt) — এখান থেকে বাটন-ওয়্যারিং
        // সরানো হয়েছে, কারণ XML থেকেই কার্ডটা তুলে দেওয়া হয়েছে।

        // TK APPROVED (2026-07-16): shows the current app version so a
        // branch running an older APK can be spotted at a glance. Reads
        // BuildConfig.VERSION_NAME (set from build.gradle.kts's
        // versionName) -- nothing here needs to change on future
        // releases, only versionName in build.gradle.kts.
        binding.tvAppVersion.text = "App Version: ${com.tkbiswas.pilesclinic.BuildConfig.VERSION_NAME}"

        val isMaster = user.role == "master"

        // 🔒 নতুন হেল্পার (04.08.2026) — B374-এর সমাধানের সেই একই চিন্তা,
        // এখন ২-কলাম "সারি" LinearLayout-এর জন্য: item লুকিয়ে row থেকে
        // সরিয়ে দেয় (বাকি item weight=1 থাকায় নিজে থেকে পুরো সারি নিয়ে
        // নেয়), আর row-টা সম্পূর্ণ ফাঁকা হয়ে গেলে (দুটো item-ই লুকানো)
        // পুরো row-টাও GONE করে দেয়, যাতে ফাঁকা জায়গা/মার্জিন না থাকে।
        // 📌🔒 V1572 (১৭.০৯.২০২৬) — এই ফাংশনটা এখন Doctor Reminder/Staff
        // Profiles-এর সারিতেও লাগে, তাই সেই ব্যবহারের আগেই এখানে আনা হলো।
        fun hideItem(row: LinearLayout, item: View) {
            item.visibility = View.GONE
            row.removeView(item)
            if (row.childCount == 0) row.visibility = View.GONE
        }

        /* 📌🔒 V1212 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"Doctor reminder icon টা উপরে
           ডান সাইডে যা থ্রি ডট, তার মধ্যে ক্লিক করলে যে মেনুগুলো আসে সেখানে থাকবে"*।
           ⇒ Dashboard-এর ঘরটা লুকানো হলো (DashboardActivity), আর এখানে এলো।
           ⛔ কারা দেখবেন সেই নিয়ম **হুবহু আগের মতোই** — master · staff · doctor
              (Dashboard-এর ঘরটায় ঠিক এই তিনজনই ছিল)। Field-এর পর্দায় বসে না।
           ⛔ Doctor Reminder পর্দাটা এক অক্ষরও বদলায়নি — শুধু পৌঁছানোর পথ বদলাল।
           📌🔒 V1572 (১৭.০৯.২০২৬, TK-নির্দেশ: *"doctor reminder, staff profile
           পাশাপাশি রাখুন"*) — rowDocModules5 এখন Staff Profiles-এরও ঘর, তাই
           পুরো সারি VISIBLE/GONE না করে rowManagement1-এর মতোই শুধু নিজের
           আইটেমটা hideItem() দিয়ে সরানো হয় — বাকিটা (isMaster ব্লকে) ঠিক
           করবে Staff Profiles আসলে দেখা যাবে কিনা। */
        run {
            val who = user.displayRole
            // 🔒🔒 V1380 (১২.০৯.২০২৬, TK-নির্দেশ) — ARMAN HOQUE: Doctor Reminder-ও
            // এখান থেকে বাদ (এতে রোগী/ডাক্তারের নোট থাকে) — Menu-তে শুধু
            // Logout + App Version থাকবে, আর কিছু না।
            val allowed = (isMaster || who == "staff" || who == "doctor") &&
                !RoleRules.isDoctorVisitOnly(this)
            if (allowed) {
                binding.btnDocDoctorReminder.setOnClickListener {
                    startActivity(Intent(this, DoctorReminderActivity::class.java))
                }
            } else {
                hideItem(binding.rowDocModules5, binding.btnDocDoctorReminder)
            }
        }

        // 🔴🔒 V1603 (১৯.০৯.২০২৬) — Backup ও Export এখন গ্রিডে নেই, উপরের ⋮
        // মেনুতে চলে গেছে — তাই দুটো রোলেই (master/non-master) গ্রিড থেকে
        // সরানো হলো। ⛔ ক্লিক-গন্তব্য/অনুমতি একটুও বদলায়নি, শুধু জায়গা।
        hideItem(binding.rowSecurity1, binding.btnBackup)
        hideItem(binding.rowSecurity1, binding.btnExportData)
        // V1660: Reports + Password now sit together; Backup/Export live in ⋮.
        binding.securityHeader.visibility = View.GONE

        // Reports / Backup&Settings / Trash / Password / Export / Staff
        // Photos / Chamber Close: master only (অপরিবর্তিত নিয়ম)
        if (isMaster) {
            binding.btnReports.setOnClickListener { startActivity(Intent(this, ReportsActivity::class.java)) }
            binding.btnTrash.setOnClickListener { startActivity(Intent(this, TrashBinActivity::class.java)) }
            binding.btnPasswordCenter.setOnClickListener { startActivity(Intent(this, PasswordCenterActivity::class.java)) }
            // 🔒 TK-APPROVED (28.07.2026 · খাতার সারি B46): "চেম্বার বন্ধ করুন"।
            binding.btnChamberClose.setOnClickListener {
                startActivity(Intent(this, ChamberCloseActivity::class.java))
            }
            binding.btnStaffProfiles.visibility = View.VISIBLE
            binding.btnStaffProfiles.setOnClickListener {
                startActivity(Intent(this, com.tkbiswas.pilesclinic.modules.StaffProfileActivity::class.java))
            }
            binding.btnIncomeExpense.visibility = View.VISIBLE
            binding.btnIncomeExpense.setOnClickListener {
                startActivity(Intent(this, com.tkbiswas.pilesclinic.modules.IncomeExpenseActivity::class.java))
            }
            loadUnclosedCount()
        } else {
            hideItem(binding.rowManagement1, binding.btnReports)
            hideItem(binding.rowManagement2, binding.btnChamberClose)
            hideItem(binding.rowManagement2, binding.btnTrash)
            hideItem(binding.rowSecurity1, binding.btnPasswordCenter)
            // 🔵 (07.08.2026, নিজের অডিটে ধরা) — Logout কার্ডটা হেডারে সরানোর পরে
            // non-master-এর পর্দায় এই সেকশনের দুটো সারিই খালি হয়ে লুকিয়ে যায়,
            // অথচ "🛡️ Security & Data" শিরোনামটা একা দাঁড়িয়ে থাকত। এখন সেটাও লুকায়।
            binding.securityHeader.visibility = View.GONE
            // 📌🔒 V1572 (১৭.০৯.২০২৬) — Staff Profiles এখন rowDocModules5-এর
            // ভিতরে (Doctor Reminder-এর পাশে), তাই XML-এর ডিফল্ট ভিজিবল —
            // non-master-দের জন্য hideItem()-দিয়ে সরিয়ে নিতে হয় (আগে XML
            // ডিফল্ট gone-ই যথেষ্ট ছিল, যখন এটা নিজের আলাদা পূর্ণ-প্রস্থ card
            // ছিল)। ⛔ কারা দেখবেন সেই নিয়ম (শুধু master) বদলায়নি।
            hideItem(binding.rowDocModules5, binding.btnStaffProfiles)
            // Income & Expense (btnIncomeExpense) থেকেই যায় visibility=gone
            // (XML-এর ডিফল্ট, নিজের আলাদা full-width card) — master-এর
            // branch-এই শুধু VISIBLE করা হয়েছে।

            // 🔒🔒 B605 (10.08.2026, TK-নির্দেশ): "My Profile" এখন শুধু Master-এর জন্য
            // (Master-এর "Staff Profiles" থেকেই সব)। staff/doctor/field-এর পর্দা থেকে
            // বাদ — আগে এখানে VISIBLE করা হত, এখন করা হয় না (XML ডিফল্টই gone)।
            // ⛔ StaffProfileActivity/renderSelf কোড অক্ষত — শুধু আর এখান থেকে ডাকা হয় না।
            binding.btnMyProfile.visibility = View.GONE

            /* 🟢🆕🔒 V401 (16.08.2026, TK-নির্দেশ): মাস্টার যাঁর চাবি চালু করেছেন, সেই
               staff-ও এখন "Income & Expense" দেখতে পাবেন। চাবি বন্ধ থাকলে বোতামই আসে না।
               ⛔ চাবি না থাকলে পর্দা হুবহু আগের মতোই — কিছু বদলায়নি।
               ⛔ আসল সুরক্ষা ডেটাবেসের RLS-এ (V401); এটা শুধু বোতাম দেখানো/না-দেখানো।
               একটা ছোট পড়া (নিজের এক-দুই সারি) — প্রতিবার মেনু খুললে, তাই মাস্টার
               চাবি চালু/বন্ধ করলে সঙ্গে সঙ্গেই কাজে লাগে। */
            if (com.tkbiswas.pilesclinic.modules.IePermit.has(this)) {
                binding.btnIncomeExpense.visibility = View.VISIBLE
                binding.btnIncomeExpense.setOnClickListener {
                    startActivity(Intent(this, com.tkbiswas.pilesclinic.modules.IncomeExpenseActivity::class.java))
                }
            }
            // ⛔ শুধু staff ও doctor-এর জন্য — Field Officer-এর পর্দায় অকারণ কোনো
            //    ক্লাউড-অনুরোধ যায় না (Supabase free-plan-এ প্রতিটা পড়াই হিসাবে আসে)।
            if (user.displayRole == "staff" || user.displayRole == "doctor") Thread {
                val allowed = com.tkbiswas.pilesclinic.modules.IePermit.refresh(this).isNotEmpty()
                runOnUiThread {
                    try {
                        if (allowed) {
                            binding.btnIncomeExpense.visibility = View.VISIBLE
                            binding.btnIncomeExpense.setOnClickListener {
                                startActivity(Intent(this, com.tkbiswas.pilesclinic.modules.IncomeExpenseActivity::class.java))
                            }
                        } else binding.btnIncomeExpense.visibility = View.GONE
                    } catch (_: Throwable) { }
                }
            }.start()
        }

        // 🔴🆕🔒 TK-নির্দেশ (07.08.2026) — *"Check Up · Print · Chamber Date ·
        // Payment · Dr. Visit — এই ৫টা শুধুমাত্র ডাক্তারের Dashboard-এ থাকবে,
        // বাকি সব কিছু মেনুবারে থাকবে।"* Dashboard থেকে ডাক্তারের যেগুলো সরানো
        // হয়েছে, ঠিক সেগুলোই এখানে "Modules" সেকশনে — একই Activity, একই কাজ;
        // তাই ডাক্তারের কোনো সুবিধাই হারায়নি (বিশেষ করে Briefing = নোটিশ, ও Search)।
        // ⛔ পুরো সেকশনটা XML-এ ডিফল্ট gone; শুধু ডাক্তার হলে VISIBLE — তাই
        //    Master/Staff/Field-এর Menu পর্দা এক চুলও বদলায়নি।
        // 🔴🔴 (08.08.2026, TK লাইভ টেস্টে ধরেছেন) — এখানেও একই ভুল ছিল:
        // ডাক্তারের `role` ভিতরে "staff" (permissionRole), আসল পরিচয় `displayRole`-এ।
        // `user.role == "doctor"` কখনো সত্যি হত না, তাই এই "Modules" সেকশনটাই
        // ডাক্তারের পর্দায় দেখাত না। এখন `displayRole` দেখা হয়।
        if (user.displayRole == "doctor") {
            binding.docModulesSection.visibility = View.VISIBLE
            binding.btnDocEnquiry.setOnClickListener { startActivity(Intent(this, EnquiryActivity::class.java)) }
            binding.btnDocFollowUp.setOnClickListener { startActivity(Intent(this, FollowUpActivity::class.java)) }
            binding.btnDocRegistration.setOnClickListener { startActivity(Intent(this, RegistrationActivity::class.java)) }
            binding.btnDocDialer.setOnClickListener { startActivity(Intent(this, DialerActivity::class.java)) }
            binding.btnDocDraft.setOnClickListener { startActivity(Intent(this, DraftActivity::class.java)) }
            binding.btnDocBriefing.setOnClickListener { startActivity(Intent(this, BriefingActivity::class.java)) }
            binding.btnDocSearch.setOnClickListener { startActivity(Intent(this, GlobalSearchActivity::class.java)) }
            // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — Dr. Visit এখন Dashboard-এ নেই, তাই
            // ডাক্তারের Modules-এ যোগ করা হলো (Dashboard থেকে যা সরে, তা এখানেই আসে)।
            binding.btnDocDoctorVisit.setOnClickListener { startActivity(Intent(this, DoctorVisitActivity::class.java)) }
            // 🔵🔒 B617 (11.08.2026, TK-নির্দেশ, প্রুফ-অনুমোদিত): "My Share Ledger"-এর
            // জায়গায় "আয় ও ব্যয়" (Income & Expense) — অংশীদার-ডাক্তার নিজের ব্রাঞ্চের
            // আয়-ব্যয় লিখতে পারবেন; অংশীদারি ভাগ ওই পর্দার ভেতরেই আছে (হারায় না)।
            // Amit Goldar · P.K Roy অংশীদার নন (শুধু রোগী দেখেন) — তাঁদের বোতাম নয়।
            // ⛔ প্রোগ্রাম্যাটিক (XML/binding অটুট); আসল সুরক্ষা DB-র RLS।
            run {
                val mob10 = user.mobile.filter { it.isDigit() }.takeLast(10)
                /* 🟢🔒 V401 (TK-সিদ্ধান্ত: "চাবির অধীনে আসুক"): Amit Goldar ও P.K Roy
                   এতদিন হাতে-লেখা করে বাদ ছিলেন। এখন তাঁরাও দেখতে পাবেন — তবে **শুধু
                   যদি মাস্টার তাঁদের চাবি চালু করে থাকেন**। চাবি না থাকলে আগের মতোই নয়। */
                val patientOnly = (mob10 == "9046366596" || mob10 == "6297625447") &&
                    !com.tkbiswas.pilesclinic.modules.IePermit.has(this@MoreMenuActivity)
                if (!patientOnly) {
                    val dm = resources.displayMetrics.density
                    val ieBtn = android.widget.TextView(this).apply {
                        text = NoBengali.s("💵  আয় ও ব্যয়")
                        textSize = 15f
                        setTextColor(android.graphics.Color.WHITE)
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        gravity = android.view.Gravity.CENTER
                        setPadding((14 * dm).toInt(), (14 * dm).toInt(), (14 * dm).toInt(), (14 * dm).toInt())
                        background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = 12 * dm
                            setColor(android.graphics.Color.parseColor("#0A5C33"))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = (10 * dm).toInt() }
                        isClickable = true
                        setOnClickListener { startActivity(Intent(this@MoreMenuActivity, com.tkbiswas.pilesclinic.modules.IncomeExpenseActivity::class.java)) }
                    }
                    binding.docModulesSection.addView(ieBtn)
                }
            }
        }

        // Logout: সবার জন্য (অপরিবর্তিত) — 🔴 (07.08.2026) বোতামটা এখন নিচের
        // তালিকায় নেই, হেডারের ডান পাশে; id ও এই কোড অপরিবর্তিত।
        binding.btnLogout.setOnClickListener { confirmLogout() }

        // 🔴🔒 V1603 (১৯.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — Call ID Banner ·
        // Backup · Export এখন হেডারের ⋮-এর ভিতরে (আগে আলাদা কার্ড ছিল)।
        // ⛔ Call ID Banner সব রোলে (V1427-এর আসল নিয়ম অটুট); Backup/Export
        // শুধু Master (গ্রিডে যেমন ছিল, তার হুবহু একই ক্লিক-গন্তব্য)।
        binding.btnMoreMenu.setOnClickListener { v ->
            val pm = android.widget.PopupMenu(this, v)
            var idx = 0
            pm.menu.add(0, idx++, idx, "Call ID Banner")
            if (isMaster) {
                pm.menu.add(0, idx++, idx, "Backup")
                pm.menu.add(0, idx++, idx, "Export")
            }
            pm.setOnMenuItemClickListener { mi ->
                when (mi.title?.toString()) {
                    "Call ID Banner" -> startActivity(Intent(this, CallIdSetupActivity::class.java))
                    "Backup" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Export" -> startActivity(Intent(this, ExportDataActivity::class.java))
                }
                true
            }
            pm.show()
        }
    }

    /**
     * 🔒 খাতার সারি B46 — মেনুর "Chamber Close" ঘরে কত দিনের চেম্বার বন্ধ হয়নি
     * সেই সংখ্যাটা বসায়। নতুন ডিজাইনে সংখ্যাটা শিরোনামের ভিতরে "\n"-জোড়া না
     * হয়ে আলাদা লাল ব্যাজে (badgeChamberClose) ও উপ-লেখায় (tvChamberCloseSub)
     * বসে।
     *
     * ⛔ সম্পূর্ণ পিছনে হয় — পর্দা এক মুহূর্তও অপেক্ষা করে না, আর সংখ্যা না
     * এলে বোতামের লেখা আগের মতোই থাকে ("All closed")। দু'মিনিটের স্মৃতি
     * (findUnclosedCached) থাকায় মেনু থেকে পর্দায় গেলে আবার ক্লাউডে যেতে হয় না।
     */
    private fun loadUnclosedCount() {
        Thread {
            val n = try {
                ChamberUnclosedRepository.findUnclosedCached(this, "All", 30).size
            } catch (_: Throwable) { 0 }
            if (n <= 0) return@Thread
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.tvChamberCloseSub.text = "$n pending days"
                binding.badgeChamberClose.text = "$n"
                binding.badgeChamberClose.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Logout"))
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes, Logout") { _, _ ->
                NativeSession.clear(this)
                // 🔴 B315 (03.08.2026, TK-নির্দেশ): মূল অ্যাপ লগআউটের সাথে সাথে
                // Staff Profile/Work Notebook/Income-Expense-এর Module সেশনও
                // সম্পূর্ণ শেষ হওয়া দরকার — নইলে এই একই ফোনে পরের কেউ লগইন
                // করলে আগের ব্যক্তির Module-পরিচয় (cached accessToken) থেকে
                // যেতে পারত (B317-এর সেফটি-নেট থাকলেও, লগআউটেই সরাসরি সাফ
                // করাটা বেশি নিরাপদ ও দ্রুত)।
                // 🔴🔒 V453: context দেওয়ায় SharedPreferences-এ জমানো module
                // টোকেনও মুছে যায় — এই ফোনে পরের কেউ লগইন করলে আগের কারো
                // cached session কখনো ফিরে আসবে না।
                com.tkbiswas.pilesclinic.modules.ModuleAuth.signOut(this)
                // 🟢🔒 V401: একই ফোনে পরের কেউ লগইন করলে আগের ব্যক্তির আয়-খরচের
                // চাবি যেন থেকে না যায় — তাই লগআউটেই মুছে ফেলা হয়।
                com.tkbiswas.pilesclinic.modules.IePermit.clear(this)
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

}
