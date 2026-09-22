package com.tkbiswas.pilesclinic.native

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.databinding.ActivityDashboardBinding
import com.tkbiswas.pilesclinic.databinding.ItemDashboardTileBinding

/**
 * Native Dashboard.
 *
 * Every module opens its own native screen. Enquiry, Registration, Follow Up,
 * Payment, Doctor Visit, Doctor Queue, Briefing, Calendar, Users, Photos, Print
 * and Menu (Draft/Reports/Backup/Trash/Settings via MoreMenuActivity) are all
 * native. The old WebView host has been removed from the app entirely.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        // 🟢🔒 B624 (Egress ফিক্স, 11.08.2026, TK-নির্দেশ): auto-backup আগে **প্রতিটা স্টাফের
        // ফোনে রোজ** পুরো ডেটাবেস (রোগীর ছবি base64-সহ) ক্লাউড থেকে নামাত — এটাই ছিল
        // Supabase Free-plan egress শেষ হওয়ার মূল কারণ। এখন শুধু **মাস্টারের ফোনে** চলে
        // (মাস্টার সব ব্রাঞ্চ দেখে, তাই তার ব্যাকআপই সম্পূর্ণ); স্টাফ-ফোনে আর পুরো-DB ডাউনলোড
        // নেই। V452 working rule: fresh install/reinstall-এ local JSON seed না থাকলে এখান থেকে
        // কোনো full-cloud backup শুরু হবে না; Settings → Backup Now একবার নেওয়ার পরেই weekly
        // controlled backup চালু হতে পারে। ⛔ ক্লাউডই মূল ভাণ্ডার — data delete/restore বদলায়নি।
        if (NativeSession.current(this)?.role == "master") {
            lifecycleScope.launch { CloudBackup.exportIfStale(this@DashboardActivity) }
        }

        // TK-REQUESTED ADDITION (2026-07-19): "Live colour" — a gentle
        // shifting gradient, but ONLY on this top header banner. The 15
        // module tiles below stay completely static/solid on purpose (TK's
        // own earlier decision: each module's fixed color is how staff
        // recognize it at a glance — animating those would work against
        // that). Uses a NEW drawable pair defined here in code, not the
        // shared bg_login_hero.xml, so Login and every other screen using
        // that same drawable are completely unaffected. Lightweight:
        // TransitionDrawable cross-fade, no continuous per-frame work, so
        // no meaningful battery/performance cost even on older phones.
        run {
            fun gradient(orient: android.graphics.drawable.GradientDrawable.Orientation): android.graphics.drawable.GradientDrawable {
                val navy = androidx.core.content.ContextCompat.getColor(this, com.tkbiswas.pilesclinic.R.color.brand_navy)
                val blue = androidx.core.content.ContextCompat.getColor(this, com.tkbiswas.pilesclinic.R.color.brand_blue)
                val green = androidx.core.content.ContextCompat.getColor(this, com.tkbiswas.pilesclinic.R.color.brand_green)
                return android.graphics.drawable.GradientDrawable(orient, intArrayOf(navy, blue, green)).apply {
                    gradientType = android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT
                }
            }
            val frameA = gradient(android.graphics.drawable.GradientDrawable.Orientation.TL_BR)
            val frameB = gradient(android.graphics.drawable.GradientDrawable.Orientation.BL_TR)
            val transition = android.graphics.drawable.TransitionDrawable(arrayOf(frameA, frameB))
            binding.dashboardHero.background = transition
            var reversed = false
            val handler = android.os.Handler(mainLooper)
            val cycle = object : Runnable {
                override fun run() {
                    if (isFinishing || isDestroyed) return
                    if (reversed) transition.reverseTransition(2600) else transition.startTransition(2600)
                    reversed = !reversed
                    handler.postDelayed(this, 2600)
                }
            }
            handler.post(cycle)
        }

        val user = NativeSession.current(this)
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        /* 👤🔒 V1168 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ "ক") — TK:
           *"কাকে ওয়েলকাম জানানো হচ্ছে তার নামটাই তো কেটে গেছে · এখানে স্টাফ
           কোড নাম্বার থাকবে না, এখানে স্টাফের সম্পূর্ণ নাম থাকবে"*, আর
           *"welcome এক লাইনে, নিচের লাইনে ফুল নাম"*।
           ⛔ V1138-এ TK নিজেই এক লাইন চেয়েছিলেন — এখন তিনিই দু'লাইন চাইলেন,
              তাই XML-এ `maxLines` ১ → ২; লেখাটা আর কাটে না।
           ⛔ নামটা আসে **ফোনে আগে থেকেই জমানো** স্টাফ-তালিকা থেকে
              (`CloudStaffDirectory` — দিনে একবার নবায়ন হয়); নতুন কোনো
              ক্লাউড-পড়া বানানো হয়নি, তাই ফ্রি প্ল্যানে বাড়তি খরচ নেই।
           ⛔ জমানো তালিকায় না পেলে আগের লেখাটাই (সেশনের নাম) বসে। */
        renderWelcome(welcomeFullName(user))
        /* 🧵 তালিকা পুরনো হলে (এক দিনের বেশি) নিঃশব্দে নবায়ন — নাম পাওয়া গেলে
           সঙ্গে সঙ্গে বসিয়ে দেওয়া হয়। ⛔ ব্যর্থ হলে কিছুই বদলায় না। */
        BackgroundWork.run {
            try {
                CloudStaffDirectory.refreshIfStale(applicationContext)
                val fresh = welcomeFullName(user)
                runOnUiThread { if (!isFinishing && !isDestroyed) renderWelcome(fresh) }
            } catch (_: Throwable) { }
        }
        val roleLabel = user.displayRole.replaceFirstChar { it.uppercase() }
        binding.tvRoleBranch.text = if (user.branch == "All") roleLabel else "$roleLabel · ${user.branch}"

        // 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬) — কিষাণগঞ্জের নাম এখানে "BISWAS
        // PILES CLINIC" লেখা ছিল, অথচ **ছাপা কাগজে ও কম্পিউটারে** সবসময়
        // "TK BISWAS PILES CLINIC" (BranchInfo.kt:21 KISHANGANJ_NAME)। TK
        // বললেন "TK BISWAS PILES CLINIC"-ই ঠিক, তাই এখানেও সেটাই বসল।
        // ⛔ শুধু ড্যাশবোর্ডের উপরের লেখা — কাগজ/রসিদ/বার্তা কিছুই ছোঁয়া হয়নি
        //    (ওগুলো আগে থেকেই ঠিক নামই ব্যবহার করে)।
        binding.tvClinicName.text =
            if (user.branch == "Kishanganj") "TK BISWAS PILES CLINIC" else "MAA AYURVED PILES CLINIC"
        binding.tvBranchSub.text = if (user.branch == "All") "All Branches" else user.branch

        // 🔒 B586 (TK-অনুমোদিত প্রুফ, 08.08.2026): ক্লিনিকের নামের পাশে আসল লোগো
        // (🌿 পাতার বদলে), ব্রাঞ্চ অনুযায়ী ও বৃত্তাকার — উপরের user-photo-র হুবহু
        // একই প্রমাণিত পদ্ধতি (RoundedBitmapDrawable, isCircular)। ⛔ লোগো ছাড়া
        // অন্য কিছু বদলায়নি; ব্যর্থ হলে XML-এর ডিফল্ট লোগোই থাকে (ক্ষতি নেই)।
        val clinicLogoRes = if (user.branch == "Kishanganj")
            com.tkbiswas.pilesclinic.R.drawable.ic_kishanganj_logo else com.tkbiswas.pilesclinic.R.drawable.ic_maa_ayurved_logo
        try {
            val logoBmp = android.graphics.BitmapFactory.decodeResource(resources, clinicLogoRes)
            if (logoBmp != null) {
                val circularLogo = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, logoBmp)
                circularLogo.isCircular = true
                binding.ivClinicLogo.setImageDrawable(circularLogo)
            }
        } catch (_: Throwable) { }

        // Logged-in user's photo (from My Photo), or their name's first letter.
        val photoData = UserPhotoStore.get(this, user.mobile)
        val bmp = PhotoUtils.decodeDataUrl(photoData)
        if (bmp != null) {
            val circular = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, bmp)
            circular.isCircular = true
            binding.ivUserPhoto.setImageDrawable(circular)
            binding.ivUserPhoto.visibility = android.view.View.VISIBLE
            binding.tvPhotoInitial.visibility = android.view.View.GONE
        } else {
            binding.tvPhotoInitial.text = user.name.trim().firstOrNull()?.uppercase() ?: "U"
            binding.ivUserPhoto.visibility = android.view.View.GONE
            binding.tvPhotoInitial.visibility = android.view.View.VISIBLE
        }

        val role = user.role
        // TK APPROVED (2026-07-15): dashboard tiles now each get their own soft
        // colour-coded gradient card + coloured icon circle (Option A, TK chose
        // this over the plain-white-card + left-stripe alternative) instead of
        // every tile looking identical. Grid, labels, icons, click targets and
        // role visibility are all completely unchanged -- only colour + a
        // little shadow depth were added.
        /* 🔔🔒 V1186 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"Check Up,
           Enquiry, follow Up এর মতই একটা icon থাকুক"* ও *"এটা প্রত্যেকের হোম
           স্ক্রিনে ই থাকবে · যে কোন staff, যে কোন ডাক্তার এবং মাস্টার এটা
           ক্রিয়েট করতে পারবে"* ⇒ তাই তিনটে role-এই দেখা যায়।
           ⛔ ঘড়ি/ক্যালেন্ডার/ঘণ্টার ইমোজি নয় (TK-র স্থায়ী নিয়ম) — 📌। */
        tile(binding.tileDoctorReminder, "📌", "Doctor Reminder", listOf("master", "staff", "doctor"), "#E8F4F1", "#C9E7E0", "#0F766E") { startActivity(Intent(this, DoctorReminderActivity::class.java)) }
        /* 📌🔒 V1212 (০৮.০৯.২০২৬, TK-নির্দেশ) — ঘরটা এখন ☰ মেনুতে
           (MoreMenuActivity.btnDocDoctorReminder)। Reports/Export-এর মতোই
           **ঘরের সেটআপ (role/রং/ক্লিক) হুবহু রেখে শুধু লুকানো** হলো, তাই
           ভবিষ্যতে ফেরাতে হলে এই একটা লাইন সরালেই হয়। */
        binding.tileDoctorReminder.root.visibility = android.view.View.GONE
        tile(binding.tileEnquiry, "📝", "Enquiry", listOf("master", "staff", "doctor"), "#FFF3E0", "#FFE0B2", "#FFB74D") { startActivity(Intent(this, EnquiryActivity::class.java)) }
        tile(binding.tileFollowUp, "🔁", "Follow-up", listOf("master", "staff", "doctor"), "#E8F5EE", "#C8E6D5", "#0EA25F") { startActivity(Intent(this, FollowUpActivity::class.java)) }
        tile(binding.tileRegistration, "🧾", "Registration", listOf("master", "staff", "doctor"), "#E7EEFB", "#C9D9F5", "#4A78D6") { startActivity(Intent(this, RegistrationActivity::class.java)) }
        // 🆕🔒 TK-নির্দেশ (05.08.2026): Dialer More মেনু থেকে সরিয়ে সরাসরি
        // Dashboard-এ — সবার জন্য (role-গেট নেই, More মেনুতেও তাই ছিল)।
        tile(binding.tileDialer, "📞", "Dialer", listOf("master", "staff", "doctor", "field"), "#E8F5EE", "#C8E6D5", "#0EA25F") { startActivity(Intent(this, DialerActivity::class.java)) }
        tile(binding.tileDoctorQueue, "🩺", "CHECK-UP", listOf("master", "doctor", "staff"), "#F1EAFB", "#DCC9F5", "#8E5FD6") { startActivity(Intent(this, DoctorQueueActivity::class.java)) }
        tile(binding.tilePayment, "💰", "Payment", listOf("master", "staff", "doctor"), "#E8F7F2", "#C9EEE0", "#0A9E8A") { startActivity(Intent(this, PaymentActivity::class.java)) }
        tile(binding.tilePrint, "🖨️", "Print", listOf("master", "staff", "doctor", "field"), "#FDEBEE", "#F7C9D2", "#D6577A") {
            com.tkbiswas.pilesclinic.clinical.RoleSession.currentPatientId = ""
            com.tkbiswas.pilesclinic.clinical.RoleSession.currentPatientName = ""
            com.tkbiswas.pilesclinic.clinical.RoleSession.currentPatientBranch = ""
            com.tkbiswas.pilesclinic.clinical.ClinicalRepository.resetForNewPatient()
            startActivity(Intent(this, com.tkbiswas.pilesclinic.print.PrintCenterActivity::class.java))
        }
        tile(binding.tileReports, "📊", "Reports", listOf("master"), "#FFF8E1", "#FCE9A8", "#C99A19") { startActivity(Intent(this, ReportsActivity::class.java)) }
        // TK-REQUESTED (2026-07-19): moved to the "More" menu (hamburger)
        // to keep the Dashboard focused on daily-use items -- these 5 are
        // occasional/admin-only. Tile setup (role/color/click) kept as-is,
        // just hidden here; MoreMenuActivity.kt now has the entry points.
        binding.tileReports.root.visibility = android.view.View.GONE
        // TK-REQUESTED NEW FEATURE (2026-07-16, Step 1 of a staged build --
        // see ChamberAttendanceActivity.kt / 00_PROJECT_STATE_MASTER_NOTE.md
        // for the full agreed plan). Master + Staff only for now (branch
        // collection/attendance tracking is not part of Doctor/Field's job).
        tile(binding.tileChamberAttendance, "📋", "Chamber Date", listOf("master", "staff"), "#FDEEE9", "#F8CFC0", "#D9612F") { startActivity(Intent(this, ChamberAttendanceActivity::class.java)) }
        tile(binding.tileDoctorVisit, "👨‍⚕️", "Dr. Visit", listOf("master", "field", "staff", "doctor"), "#E7F3FB", "#C4E4F7", "#2C9BD6") { startActivity(Intent(this, DoctorVisitActivity::class.java)) }
        tile(binding.tileDraft, "📂", "Draft", listOf("master", "staff", "doctor"), "#FBF3E7", "#F5E1C4", "#C9812F") { startActivity(Intent(this, DraftActivity::class.java)) }
        // 🔴 B339 (03.08.2026, TK-নির্দেশ): B311-এ Work Notebook ড্যাশবোর্ড থেকে
        // সরিয়ে শুধু More মেনুতে আনা হয়েছিল। TK এখন উল্টো নির্দেশ দিয়েছেন —
        // Work Notebook আবার Dashboard-এ থাকবে (Draft-এর পাশে), আর More মেনু
        // থেকে দুটোই (Draft + Work Notebook) সরিয়ে দেওয়া হয়েছে (MoreMenuActivity.kt দেখুন)।
        // 🧑‍⚕️🔒 V496 (২১.০৮.২০২৬, TK §২): **ডাক্তারের হাজিরা নেই।**
        // আগে এই টাইল `listOf("staff")`-এ ছিল, কিন্তু `permissionRole()`
        // ডাক্তারকেও "staff" বানায় (NativeSession.kt:25) — তাই **ডাক্তাররাও
        // Work Notebook (IN/OUT TIME) পেতেন**। এখন আসল ভূমিকা দেখে
        // (RoleRules → displayRole, প্রজেক্টের প্রমাণিত পথ) টাইলটাই লুকানো হয়।
        // ⛔ Staff ও Field-এর জন্য হুবহু আগের মতোই।
        if (com.tkbiswas.pilesclinic.native.RoleRules.usesAttendance(user)) {
            tile(binding.tileWorkNotebook, "🗒️", "Work Notebook", listOf("staff"), "#E8F5EE", "#C8E6D5", "#0EA25F") { startActivity(Intent(this, com.tkbiswas.pilesclinic.modules.WorkNotebookActivity::class.java)) }
        } else {
            // 🔴 V497 বিল্ড-ফিক্স (২১.০৮.২০২৬): `tileWorkNotebook` layout-এ
            //    `<include>` (activity_dashboard.xml:360) — তাই এটা View নয়,
            //    আরেকটা binding। লুকাতে হলে `.root` লাগে, ঠিক নিচের
            //    `binding.tileBriefing.root.visibility`-র মতোই।
            binding.tileWorkNotebook.root.visibility = android.view.View.GONE
        }
        tile(binding.tileBriefing, "💬", "Briefing", listOf("master", "staff", "doctor", "field"), "#F0F8FC", "#D6EAF7", "#3894C4") { startActivity(Intent(this, BriefingActivity::class.java)) }
        // 🔴🔒 B487 (06.08.2026) — Briefing-এর সবকিছুই (পড়া/রিপ্লাই/লেখা/
        // Master-অনুমোদন) এখন ঘন্টা → Notifications পাতার 📜/➕ দিয়ে
        // পৌঁছানো যায় (B486), তাই TK-এর নিজের সিদ্ধান্তে ড্যাশবোর্ড-গ্রিড
        // থেকে এই টাইলটা তুলে দেওয়া হলো। tile()-এর সেটআপ অক্ষত, শুধু
        // লুকানো হলো — কোনো ফাংশন হারায়নি।
        binding.tileBriefing.root.visibility = android.view.View.GONE
        tile(binding.tileBackup, "☁️", "Backup", listOf("master"), "#EEF3FB", "#D9E4F5", "#5B7FC7") { startActivity(Intent(this, com.tkbiswas.pilesclinic.security.SettingsActivity::class.java)) }
        binding.tileBackup.root.visibility = android.view.View.GONE
        tile(binding.tilePassword, "🔐", "Password Center", listOf("master"), "#FBEDED", "#F7D6D6", "#C44E4E") { startActivity(Intent(this, PasswordCenterActivity::class.java)) }
        binding.tilePassword.root.visibility = android.view.View.GONE
        tile(binding.tileTrash, "🗑️", "Trash Bin", listOf("master"), "#F3F5F7", "#E3E6EA", "#7A8699") { startActivity(Intent(this, TrashBinActivity::class.java)) }
        binding.tileTrash.root.visibility = android.view.View.GONE
        tile(binding.tileSearch, "🔍", "Search", listOf("master", "staff", "doctor", "field"), "#F3F5F7", "#E3E6EA", "#7A8699") {
            startActivity(Intent(this, GlobalSearchActivity::class.java))
        }
        // TK-REQUESTED ADDITION (2026-07-16): Export Data tile, Master-only.
        tile(binding.tileExportData, "📤", "Export Data", listOf("master"), "#EAF6F0", "#CDEAD9", "#0EA25F") {
            startActivity(Intent(this, ExportDataActivity::class.java))
        }
        // 🔴 B311 (03.08.2026, TK-নির্দেশ, ছবি দেখিয়ে): "এগুলি মেনুবারের মধ্যে
        // থাকবে, ড্যাশবোর্ডে ওপেন থাকবে না।" — ঠিক Export Data-র জন্য আগে যে
        // প্যাটার্ন TK অনুমোদন করেছিলেন (27.07.2026) তারই পুনরাবৃত্তি: Work
        // Notebook/Staff Profiles/Income & Expense — তিনটেই ড্যাশবোর্ড-গ্রিড
        // থেকে সম্পূর্ণ সরানো হলো (আগে যে `addModuleTile()` ব্লক এগুলো
        // প্রোগ্রাম্যাটিকভাবে গ্রিডে যোগ করত, সেটাই বাদ)। এখন থেকে এই তিনটে
        // মডিউল শুধু মেনুতে (MoreMenuActivity, নিচে দেখুন) পাওয়া যাবে — role-
        // চেক/টার্গেট-স্ক্রিন এক অক্ষরও বদলায়নি।
        // TK-REQUESTED (2026-07-25): Export Data was hidden; TK asked for it back.
        // tile(...) above already limits it to Master, so Staff/Doctor/Field
        // still never see it . the line that force hid it is simply removed.
        // TK-REQUESTED (2026-07-27, photo proof approved): Export Data is moved
        // off the Dashboard grid and lives only in the Menu (hamburger), where
        // MoreMenuActivity already has the very same Master-only entry point.
        // Same pattern as Reports/Backup/Password/Trash above -- the tile setup
        // stays exactly as it was, it is only hidden here. Nothing about the
        // Export screen itself changes.
        binding.tileExportData.root.visibility = android.view.View.GONE
        // Search now lives in the top Google-style capsule; hide the old bottom tile.
        binding.tileSearch.root.visibility = android.view.View.GONE
        binding.searchCapsule.setOnClickListener {
            startActivity(Intent(this, GlobalSearchActivity::class.java))
        }
        // TK request: remove the lower "Today Call" tile and the "Appointments"
        // tile from the dashboard grid. The top Today-Pending-Call banner
        // (tvCallBanner below) stays exactly as it was — only these two grid
        // tiles are hidden. No XML change: same pattern already used for
        // tileSearch above.
        binding.tileTodayCall.root.visibility = android.view.View.GONE
        binding.tileAppointment.root.visibility = android.view.View.GONE

        // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — ডাক্তার হলে উপরের ৪টা বাক্স পরিষ্কার
        // ২×২ ছকে সাজাও (নিচের arrangeDoctorGrid দেখুন)। অন্য role-এ কিছুই বদলায় না।
        arrangeDoctorGrid()

        binding.btnTopMenu.setOnClickListener {
            startActivity(Intent(this, MoreMenuActivity::class.java))
        }
        binding.photoFrame.setOnClickListener {
            startActivity(Intent(this, UserPhotoActivity::class.java))
        }

        // 🔒🔒 V1380 (১২.০৯.২০২৬, TK-নির্দেশ) — ARMAN HOQUE: "Dr. Visit" ছাড়া
        // আর কিছু না — তাই সার্চ বার ও ঘণ্টাও (রোগী/অনুস্মারক দেখা যেত) লুকানো।
        if (RoleRules.isDoctorVisitOnly(this)) {
            binding.searchCapsule.visibility = android.view.View.GONE
            binding.tvBell.visibility = android.view.View.GONE
            binding.tvBellBadge.visibility = android.view.View.GONE
        }

        showLastCrashIfAny()
    }

    /** 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — ডাক্তারের Dashboard-এ ঠিক ৪টা বাক্স
     *  পরিষ্কার ২×২ ছকে: উপরে **CHECK-UP · Print**, নিচে **Chamber Date · Payment**।
     *  আসল গ্রিড ৩-কলামের ও অনেক বাক্স `GONE`; তাই ডাক্তারের ৪ বাক্স আগে ফাঁক-সহ
     *  এলোমেলো দেখাত। এখানে **শুধু ডাক্তার হলে** গ্রিডটা ২-কলাম করা হয় এবং এই ৪টা
     *  বাক্সকে সরাসরি (0,0)(0,1)(1,0)(1,1) ঘরে বসানো হয়; বাকি সব `GONE` বাক্স অনেক
     *  নিচের সারিতে সরানো হয় (দেখা যায় না, তাই ওই সারিগুলো ০-উচ্চতা)।
     *  ⛔ master/staff/field-এ এই ফাংশন সঙ্গে সঙ্গে ফিরে আসে — তাদের গ্রিড আগের
     *     মতোই ৩-কলাম, এক চুলও বদলায়নি। XML-এ কোনো বদল নেই। */
    private fun arrangeDoctorGrid() {
        val realRole = NativeSession.current(this)?.displayRole
            ?: NativeSession.current(this)?.role ?: ""
        if (realRole != "doctor") return
        val grid = binding.tileDoctorQueue.root.parent as? android.widget.GridLayout ?: return
        val d = resources.displayMetrics.density
        val margin = (5 * d).toInt()
        val tileH = (100 * d).toInt()
        fun place(v: android.view.View, row: Int, col: Int) {
            val lp = v.layoutParams as android.widget.GridLayout.LayoutParams
            lp.rowSpec = android.widget.GridLayout.spec(row)
            lp.columnSpec = android.widget.GridLayout.spec(col, 1, android.widget.GridLayout.FILL, 1f)
            lp.width = 0
            lp.height = tileH
            lp.setMargins(margin, margin, margin, margin)
            v.layoutParams = lp
        }
        grid.columnCount = 2
        val four = listOf(
            binding.tileDoctorQueue.root,        // CHECK-UP → (0,0)
            binding.tilePrint.root,              // Print    → (0,1)
            binding.tileChamberAttendance.root,  // Chamber  → (1,0)
            binding.tilePayment.root             // Payment  → (1,1)
        )
        place(four[0], 0, 0)
        place(four[1], 0, 1)
        place(four[2], 1, 0)
        place(four[3], 1, 1)
        // বাকি সব বাক্স (সব GONE) অনেক নিচের আলাদা সারিতে সরিয়ে দাও, যাতে উপরের
        // দুই সারিতে ফাঁক তৈরি না করে।
        var farRow = 2
        for (i in 0 until grid.childCount) {
            val ch = grid.getChildAt(i)
            if (ch !in four) { place(ch, farRow, 0); farRow++ }
        }
    }

    /** If the app crashed last time (e.g. opening Follow-up), show the exact
     *  reason automatically here so it can be read/screenshotted, then clear it. */
    private fun showLastCrashIfAny() {
        try {
            val path = com.tkbiswas.pilesclinic.security.AppSettings.getLastCrashLogPath(this)
            if (path.isNullOrBlank()) return
            val f = java.io.File(path)
            if (!f.exists()) return
            val text = f.readText().take(1500)
            com.tkbiswas.pilesclinic.security.AppSettings.setLastCrashLogPath(this, "")
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Last crash reason"))
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show().also { PremiumAlert.paint(it) }
        } catch (_: Throwable) { }
    }

    override fun onResume() {
        try { PushTokenSync.syncOnLogin(this) } catch (_: Throwable) { }   // 🔔 V1298 push-ঠিকানা ক্লাউডে
        super.onResume()
        // Refresh the header photo after the user sets/changes it.
        val session = NativeSession.current(this) ?: return
        val bmp = PhotoUtils.decodeDataUrl(UserPhotoStore.get(this, session.mobile))
        if (bmp != null) {
            val circular = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, bmp)
            circular.isCircular = true
            binding.ivUserPhoto.setImageDrawable(circular)
            binding.ivUserPhoto.visibility = android.view.View.VISIBLE
            binding.tvPhotoInitial.visibility = android.view.View.GONE
        }
        refreshSyncStatus()
        refreshOldAppBanner()
        refreshUnclosedChambers(session)   // 🔒 খাতার সারি B36 → B46 (এখন শুধু লুকায়)
        // 🔒🔒 V1380 (১২.০৯.২০২৬, TK-নির্দেশ) — ARMAN HOQUE-এর হোম-পেজে ঘণ্টা,
        // Pending Calls, Doctor Reminder কার্ড, বা কল-এর পরে Remark-মনে-করানো
        // পপ-আপ — কোনোটাই আসবে না (এসবে রোগীর নাম/তথ্য থাকে)। বাকি সবার জন্য
        // এই পাঁচটাই আগের মতো অক্ষত।
        if (!RoleRules.isDoctorVisitOnly(this)) {
            refreshBell(session)
            refreshCallBanner(session)
            refreshReminderCard(session)    // 🟢 V1144
            paintDoctorReminders(session)   // 🔔 V1186 — Doctor Note & Reminder
            remindPendingRemarks(session)   // 🔒 খাতার সারি B51
        }
        requestNotificationPermissionIfNeeded()
        requestIgnoreBatteryOptimizationsIfNeeded()
        requestOverlayPermissionIfNeeded()   // 🪟 V845
        BranchSimHelper.resetSilentNoOnce(this)   // ☎️ V1429 — পুরনো চুপচাপ "সিম নেই" একবার মুছে আবার প্রশ্ন
        requestDoctorLocationPermissionIfNeeded()   // 🛰️ V1348
        DoctorLocation.captureIfPossible(this)             // 🛰️ V1348 (isDoctor না হলে নিজেই কিছু করে না)
    }

    /* 🛰️🔒 V1348 (১১.০৯.২০২৬, TK-নির্দেশ) — উপরের overlay/battery-অনুমতির
       ধাঁচেই "জীবনে একবারই জিজ্ঞেস, না দিলে আর জ্বালায় না"। ⛔ পার্থক্য:
       TK-র স্পষ্ট নিষেধ অনুযায়ী **কোনো AlertDialog/ব্যাখ্যা নেই** — সরাসরি
       ফোনের নিজের অনুমতি-বাক্স, তারপর সম্পূর্ণ চুপচাপ। শুধু role="doctor"।
       ⛔ ব্যর্থ/বাতিল হলেও কিছুই ভাঙে না — আগের মতোই কোনো লোকেশন জমা হবে না। */
    private fun requestDoctorLocationPermissionIfNeeded() {
        try {
            if (!RoleRules.isDoctor(this)) return
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
            val prefs = getSharedPreferences("piles_clinic_doctor_location_perm", android.content.Context.MODE_PRIVATE)
            if (prefs.getBoolean("asked_once", false)) return
            prefs.edit().putBoolean("asked_once", true).apply()
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                7341
            )
        } catch (_: Throwable) { }
    }

    /* ═══════════════════════════════════════════════════════════════════
       🟢🔒 V1144 (০৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ: *"হ্যাঁ পাশ, বসিয়ে দিন,
       সাবধানে"*) — হোম পেজেই **Reminders** কার্ড।

       TK-এর কথা: ডাক্তারবাবু যেন হোম পেজ থেকেই দেখতে পান কটা রিমাইন্ডার
       এসেছে, আর চাপ দিলেই ভিতরে ঢুকে যোগ করতে ও হিস্ট্রি দেখতে পারেন।
       স্টাফের পর্দাতেও একই কার্ড — ভিতরে "Sent by me" ঘরে নিজের পাঠানোগুলোর
       অবস্থা দেখা যায়।

       ⛔ XML-এ **এক অক্ষরও যোগ করা হয়নি** — কার্ডটা কোডে বানিয়ে কল-বাকির
          কার্ডের ঠিক নিচে বসানো হয়, তাই ডাক্তারের ২×২ ছক এক চুলও নড়ে না।
       ⛔ কিছু না থাকলে কার্ডটা **দেখাই যায় না** (খালি ঘর দেখানো হয় না)।
       ⛔ পড়াটা সরু — মাত্র তিনটে ঘর, আর পর্দা খোলার সময়েই একবার। */
    private var reminderCard: android.widget.LinearLayout? = null
    private var reminderTitle: android.widget.TextView? = null
    private var reminderLine: android.widget.TextView? = null

    private fun buildReminderCard(): android.widget.LinearLayout? {
        reminderCard?.let { return it }
        val anchorView = binding.callBannerCard
        val parent = anchorView.parent as? android.widget.LinearLayout ?: return null
        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()

        val card = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#FFF6E3"))
            setPadding(px(12), px(12), px(12), px(12))
            visibility = android.view.View.GONE
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(px(12), px(11), px(12), 0) }
        }
        card.addView(android.widget.TextView(this).apply {
            text = "\uD83D\uDD14"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#F7E4B8"))
            setPadding(px(10), px(8), px(10), px(8))
        })
        val mid = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(px(12), 0, px(8), 0) }
        }
        val title = android.widget.TextView(this).apply {
            textSize = 16.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#8A5B00"))
        }
        val line = android.widget.TextView(this).apply {
            textSize = 12.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(android.graphics.Color.parseColor("#9A7327"))
        }
        mid.addView(title); mid.addView(line)
        card.addView(mid)
        card.addView(android.widget.TextView(this).apply {
            text = "Open \u203A"
            textSize = 13.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#8A5B00"))
            setPadding(px(12), px(8), px(12), px(8))
        })
        card.setOnClickListener {
            startActivity(Intent(this@DashboardActivity, ReminderActivity::class.java))
        }
        parent.addView(card, parent.indexOfChild(anchorView) + 1)
        reminderCard = card; reminderTitle = title; reminderLine = line
        return card
    }

    /**
     * 🔔🔒 V1186 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — হোম পর্দায়
     * **Doctor Note & Reminder** কার্ড।
     *
     * TK: *"এটা প্রত্যেকের হোম স্ক্রিনে ই থাকবে · সেখানে কোন রুগীর জন্য কে
     * রিমাইনডার দিচ্ছে কার উদ্দেশ্যে দিচ্ছে সব যেন থাকে"* ও *"পাঠানোর পর
     * KH MANDAL কে Accept করতে হবে"*।
     *
     * ⛔ এটা V1144-এর "Reminders" কার্ড **নয়** — ওটা স্টাফের নিজের কাজের
     *    রিমাইন্ডার, আলাদা টেবিল, আলাদা কার্ড; দুটোই অটুট থাকে।
     * ⛔ কিছু না থাকলে কার্ডটা বসেই না (অন্য সব ব্যানারের মতোই)।
     * ⛔ একটাই সরু পড়া, আর সেটা ব্যাকগ্রাউন্ডে — পর্দা আঁকা থামে না।
     */
    private fun paintDoctorReminders(session: NativeUser) {
        val holder = binding.doctorReminderCard
        BackgroundWork.run {
            /* 🙈🔒 V1193 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"আমি তো পাঠালাম, তাহলে
               অল টাইম আমার হোম স্ক্রিনে কেন দেখাবে"* · *"এটা হাইড রাখার ব্যবস্থা
               তো রাখতে হবে"*।
               ⇒ হোম-কার্ডে এখন **শুধু যেগুলো এই ব্যক্তির জন্য অপেক্ষা করছে** —
                 নিজের পাঠানো নয় · Accept হয়ে গেলে নয় · নিজে Hide করলে নয়।
               ⛔ Doctor Reminder পর্দা ও History-তে সব আগের মতোই থাকে। */
            val rows = try { DoctorReminderRepository.waitingFor(session) } catch (_: Throwable) { emptyList() }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (isFinishing || isDestroyed) return@post
                holder.removeAllViews()
                if (rows.isEmpty()) { holder.visibility = android.view.View.GONE; return@post }
                holder.visibility = android.view.View.VISIBLE
                fun d(v: Int) = (v * resources.displayMetrics.density).toInt()
                fun bg(fill: String, stroke: String?, r: Int) =
                    android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = d(r).toFloat()
                        setColor(android.graphics.Color.parseColor(fill))
                        if (stroke != null) setStroke(d(1), android.graphics.Color.parseColor(stroke))
                    }
                fun t(text: String, size: Float, hex: String, bold: Boolean = false) =
                    android.widget.TextView(this).apply {
                        this.text = text; textSize = size
                        setTextColor(android.graphics.Color.parseColor(hex))
                        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }

                val card = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    background = bg("#FFFFFF", "#E7ECEA", 18)
                    clipToOutline = true
                }
                val head = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setBackgroundColor(android.graphics.Color.parseColor("#0B4F2A"))
                    setPadding(d(14), d(11), d(14), d(11))
                }
                head.addView(t("DOCTOR NOTE & REMINDER", 13.5f, "#FFFFFF", bold = true).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                /* 🙈 V1193 — "Hide": শুধু **এই ব্যক্তির** হোম ও ঘন্টা থেকে সরে যায়;
                   তালিকা ও History-তে সারিটা অটুট থাকে, কেউ কিছু হারায় না। */
                head.addView(t("Hide", 11.5f, "#FFFFFF", bold = true).apply {
                    background = bg("#0B4F2A", "#9FD3B6", 12)
                    setPadding(d(12), d(5), d(12), d(5))
                    isClickable = true
                    setOnClickListener {
                        BackgroundWork.run {
                            for (r in rows) try { DoctorReminderRepository.hide(r, session) } catch (_: Throwable) { }
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (isFinishing || isDestroyed) return@post
                                holder.removeAllViews()
                                holder.visibility = android.view.View.GONE
                            }
                        }
                    }
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = d(9) }
                })
                head.addView(t(rows.size.toString(), 12.5f, "#0B4F2A", bold = true).apply {
                    background = bg("#FFFFFF", null, 20)
                    setPadding(d(11), d(2), d(11), d(2))
                })
                card.addView(head)

                /* ⛔ হোম পর্দায় সর্বোচ্চ তিনটে — বাকিগুলো "View all"-এ। */
                for (r in rows.take(3)) {
                    val accepted = r.optString("acceptedAt", "").isNotBlank()
                    val row = android.widget.LinearLayout(this).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                    }
                    row.addView(android.view.View(this).apply {
                        setBackgroundColor(android.graphics.Color.parseColor(if (accepted) "#0F766E" else "#E0A800"))
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            d(5), android.widget.LinearLayout.LayoutParams.MATCH_PARENT)
                    })
                    val bd = android.widget.LinearLayout(this).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(d(13), d(11), d(13), d(12))
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    row.addView(bd)
                    /* 🩺 V1194 (TK: *"রোগের নাম দরকার তো"*) — নামের পাশে।
                       ⛔ পুরনো সারিতে ঘরটা ফাঁকা, তখন কিছুই দেখানো হয় না। */
                    val dis = r.optString("disease", "")
                    bd.addView(t(r.optString("patientName", "").ifBlank { "Patient" } +
                        "   " + r.optString("patientMobile", "") +
                        (if (dis.isNotBlank()) "   ·   $dis" else ""), 13.5f, "#0B2B1C", bold = true))
                    bd.addView(t(r.optString("note", ""), 12.5f, "#17212B").apply {
                        background = bg("#F6FAF7", "#E2EDE6", 10)
                        setPadding(d(10), d(8), d(10), d(8))
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = d(6) }
                    })
                    bd.addView(t("For  " + r.optString("forName", "").ifBlank { "All doctors" }, 11.5f, "#123E8C", bold = true)
                        .apply { setPadding(0, d(7), 0, 0) })
                    val byBranch = r.optString("byBranch", "")
                    bd.addView(t("By  " + r.optString("byName", "") +
                        (if (byBranch.isNotBlank()) " · $byBranch" else ""), 11.5f, "#8A5A00", bold = true))
                    if (accepted) {
                        bd.addView(t("Accepted", 11.5f, "#0A7C3F", bold = true).apply { setPadding(0, d(6), 0, 0) })
                    } else {
                        bd.addView(t("Not accepted yet", 11.5f, "#8A5A00", bold = true).apply { setPadding(0, d(6), 0, 0) })
                    }
                    card.addView(row)
                }
                card.addView(t("View all", 13f, "#0A5C33", bold = true).apply {
                    gravity = android.view.Gravity.CENTER
                    setPadding(d(10), d(12), d(10), d(12))
                })
                card.isClickable = true
                card.setOnClickListener {
                    startActivity(Intent(this@DashboardActivity, DoctorReminderActivity::class.java))
                }
                holder.addView(card)
            }
        }
    }

    private fun refreshReminderCard(session: NativeUser) {
        val card = buildReminderCard() ?: return
        val code = session.name
        if (code.isBlank()) { card.visibility = android.view.View.GONE; return }
        BackgroundWork.run {
            val count = ReminderRepository.openCountFor(code)
            val line = if (count > 0) ReminderRepository.topLineFor(code) else ""
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (count <= 0) { card.visibility = android.view.View.GONE; return@post }
                reminderTitle?.text = "Reminders " + count
                reminderLine?.text = line
                reminderLine?.visibility =
                    if (line.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
                card.visibility = android.view.View.VISIBLE
            }
        }
    }

    /*
     * ══════════════════════════════════════════════════════════════════════
     * 🔴🔴 V509 (২১.০৮.২০২৬) — **হোম পর্দায় ৩০ সেকেন্ডের "লাইভ" ব্যবস্থা
     * ইচ্ছে করে বসানো হয়নি।** কেন, সেটা এখানে লিখে রাখা হলো, যাতে ভবিষ্যতে
     * কেউ (আমি নিজেও) আবার একই ভুল না করি।
     *
     * TK-এর শর্ত ছিল: *"যার মোবাইল চালু থাকবে তাকে যেন সাথে সাথে দেখায়…
     * স্টাফ বাইরে পেমেন্ট নিলো, সেটা আমি যেন দেখতে পারি।"*
     *
     * প্রথমে হোম পর্দাতেও `LiveRefresh` বসানো হয়েছিল। যাচাই করে **তিনটে
     * গুরুতর দোষ** ধরা পড়ে, তাই পুরোটা তুলে নেওয়া হলো:
     *
     *  ১) **লাভ প্রায় কিছুই হত না।** হোম পর্দা পেমেন্ট/এনকোয়ারি/রেজিস্ট্রেশন
     *     **দেখায়ই না** — এখানে শুধু ঘণ্টার সংখ্যা আর "calls pending" পটি।
     *     TK যে পর্দাগুলোর কথা বলেছেন (Payment · Chamber · Follow-up ·
     *     Doctor Queue) — সেগুলোতে `LiveRefresh` **আগে থেকেই আছে**, ৩০
     *     সেকেন্ডে একবার। অর্থাৎ TK-এর শর্তটা আগে থেকেই মানা হচ্ছে।
     *
     *  ২) **খরচ কমার বদলে বাড়ত।** পাঁচটা টেবিলের পাহারা × ৩০ সেকেন্ড =
     *     ঘণ্টায় ৬০০টা প্রশ্ন, দিনে ১৬ ঘণ্টা, ১০টা ফোনে — Egress কমানোর
     *     কাজে এটা উল্টো দিকে টানত। মাস্টারের ফোনই সবচেয়ে বেশি সময় হোম
     *     পর্দায় খোলা থাকে, ক্ষতিও সেখানেই সবচেয়ে বেশি হত।
     *
     *  ৩) **খবর হারানোর ঝুঁকি ছিল।** `LiveRefresh.Watch.changed()` `true`
     *     ফেরার সঙ্গে সঙ্গেই নিজের ঘড়ি এগিয়ে দেয় (`LiveRefresh.kt`-এর
     *     ১৭৩ নম্বর লাইন)। তাই "বদলেছে" জেনেও যদি কোনো সময়-সীমার কারণে
     *     কাজটা না করা হয়, ঐ বদলটা **আর কোনোদিন ধরা পড়ে না**। প্রথম
     *     চেষ্টায় ঠিক এই ভুলটাই ছিল — একটা পেমেন্ট চিরকালের জন্য চাপা
     *     পড়ে যেতে পারত।
     *
     * ⇒ স্থায়ী নিয়ম: `changed()` **তখনই ডাকা হবে যখন `true` পেলে সত্যিই
     *   কাজটা করা হবে**। নিচের `refreshCallBanner()` ঠিক সেভাবেই লেখা —
     *   সময়ের সীমা `changed()`-এর **আগে**, পরে নয়।
     * ══════════════════════════════════════════════════════════════════════
     */

    /**
     * 🔒 খাতার সারি B51 (TK, 28.07.2026 রাত)
     *
     * TK-এর কথা: *"কল করার পরে সেই স্টাফ যখনই অ্যাপ্লিকেশনে ফিরবে তখনই তাকে
     * সেটা দেখাতে হবে।"* Follow-up পর্দায় না ফিরে সোজা হোম পেজে এলেও যেন
     * মনে পড়ে যায়, তাই এখানেও একই মনে-করানো।
     *
     * ⛔ **কিছু বাকি না থাকলে কোনো পপ-আপ নেই** — হোম পেজের চেহারা অক্ষত।
     * ⛔ **ক্লাউডে কোনো অনুরোধ নেই** — তালিকাটা ফোনের নিজের ঘরে।
     * ⛔ রিমার্ক লেখা হয়ে গেলে নামটা নিজে থেকেই উঠে যায়, তখন আর আসে না।
     */
    private fun remindPendingRemarks(session: NativeUser) {
        try {
            // 🆕 (03.08.2026, TK-নির্দেশ, B360) — "staff করবে শুধু মাত্র তাকেই
            // দেখাতে হবে। অন্য কোনো staff/Doctor/Master-কে যেন না দেখায়।"
            // ⛔ FollowUpActivity-তেও একই ভাবে role=="staff" না হলে কল-বোতাম
            // চাপলে এন্ট্রিই তৈরি হয় না (দ্বিতীয় স্তরের পাহারা)। এখানেও
            // আলাদা করে role চেক রাখা হলো, যাতে ভবিষ্যতে অন্য কোনো পর্দা
            // ভুল করে এন্ট্রি বানালেও Master/Doctor কখনো পপ-আপ না দেখেন।
            if (!session.role.equals("staff", ignoreCase = true)) return
            val pending = PendingRemarkStore.list(this, session.mobile)
            if (pending.isEmpty()) return
            val now = System.currentTimeMillis()
            // 🆕 B360: snooze না-থাকা ও ৩ বারের কম দেখানো প্রথম এন্ট্রিটাই বেছে
            // নেওয়া হয় — সবচেয়ে নতুনটা প্রথমে চেষ্টা হয়, ওটা snooze/৩-বার
            // হয়ে গেলে পরের এন্ট্রি দেখা হয়।
            val first = pending.firstOrNull { it.snoozeUntil <= now && it.shownCount < 3 } ?: return
            val more = pending.size - 1
            val who = first.name.ifBlank { first.mobile }
            val msg = if (more > 0) "Remark is still pending for $who and $more more after your calls."
            else "Remark is still pending for $who after that call."
            PendingRemarkStore.markShown(this, first.mobile)
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Remark pending"))
                .setMessage(msg)
                .setPositiveButton("Add Remark") { _, _ ->
                    startActivity(
                        Intent(this, FollowUpActivity::class.java)
                            .putExtra("remarkMobile", first.mobile)
                    )
                }
                .setNegativeButton("Not now") { _, _ -> PendingRemarkStore.snooze(this, first.mobile) }
                .setOnCancelListener { PendingRemarkStore.snooze(this, first.mobile) }
                .show().also { PremiumAlert.paint(it) }
        } catch (_: Throwable) { }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9201
                )
            }
        }
    }

    // 🔴🔴🔴 খাতার সারি [পরবর্তী] (TK-রিপোর্ট, 05.08.2026 — Call/Chamber
    // Close/Attendance রিমাইন্ডার নির্ভরযোগ্যভাবে আসে না, গভীরে যাচাই করে
    // আসল কারণের একটা অংশ পাওয়া গেছে)। **কারণ:** এই তিনটেই WorkManager
    // দিয়ে চলে, আর অ্যাপ ব্যাটারি-সেভিং থেকে বাদ রাখার অনুমতি কখনো চাওয়া
    // হয়নি — কিছু ফোন (Xiaomi/Vivo/Oppo-ঘরানার) অ্যাপ বন্ধ থাকলে এই
    // ব্যাকগ্রাউন্ড কাজ নিজে থেকেই আটকে দেয়। **সমাধান:** ঠিক উপরের
    // notification-অনুমতির প্যাটার্নেই — শুধু একবারই (SharedPreferences
    // ফ্ল্যাগ দিয়ে, বারবার পপ-আপ দেখাবে না) সিস্টেমের নিজস্ব পপ-আপ দেখানো
    // হয়, স্টাফ নিজে "Allow" বা "Deny" বেছে নেন — অ্যাপ জোর করে না। ইতিমধ্যে
    // অনুমতি থাকলে বা আগেই একবার জিজ্ঞাসা করা হয়ে থাকলে কিছুই হয় না। ⛔
    // ব্যর্থ হলেও (কোনো ফোনে এই সিস্টেম-স্ক্রিন না থাকলে) অ্যাপ ক্র্যাশ
    // করবে না — পুরো ফাংশন try/catch-এ ঢাকা।
    /* 🪟🔒 V845 (৩০.০৮.২০২৬, TK-নির্দেশ) — কল বাজার সময় কল-স্ক্রিনের উপরে
       কার্ড দেখাতে "Display over other apps" অনুমতি লাগে। এটা special
       অনুমতি — কোড থেকে দেওয়া যায় না, স্টাফকে **একবার হাতে** চালু করতে হয়।

       ⛔ উপরের `requestIgnoreBatteryOptimizationsIfNeeded()`-এর **হুবহু একই
          প্রমাণিত ধাঁচ**: জীবনে একবারই জিজ্ঞেস করে, না দিলে আর জ্বালায় না।
       ⛔ না দিলে **কিচ্ছু ভাঙে না** — আজকের মতোই নোটিফিকেশন আসবে।
       ⛔ পুরোটা try/catch-এ — কোনো ফোনে এই সিস্টেম-পর্দা না থাকলেও
          Dashboard কখনো ক্র্যাশ করবে না। */
    private fun requestOverlayPermissionIfNeeded() {
        try {
            if (CallOverlay.allowed(this)) return
            val prefs = getSharedPreferences("piles_clinic_overlay_perm", android.content.Context.MODE_PRIVATE)
            if (prefs.getBoolean("asked_once", false)) return
            prefs.edit().putBoolean("asked_once", true).apply()
            /* ⛔ প্রজেক্টের নিজের প্রমাণিত ধাঁচ (উপরের crash-log ডায়ালগের
               হুবহু একই): AlertDialog + PremiumAlert.header/paint। */
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, NoBengali.s("কল এলে রোগীর তথ্য পর্দায় দেখতে")))
                .setMessage(
                    NoBengali.s("কল-স্ক্রিনের উপরে রোগীর তথ্য দেখাতে একটা অনুমতি লাগে") +
                    " — \"Display over other apps\"\n\n" +
                    NoBengali.s("পরের পর্দায় এই অ্যাপটা বেছে চালু করে দিন") + "\n\n" +
                    NoBengali.s("না দিলেও কিছু নষ্ট হবে না — আগের মতোই নোটিফিকেশন আসবে।"))
                .setPositiveButton(NoBengali.s("চালু করি")) { _, _ ->
                    CallOverlay.openPermissionScreen(this)
                }
                .setNegativeButton(NoBengali.s("এখন নয়"), null)
                .show().also { PremiumAlert.paint(it) }
        } catch (_: Throwable) { }
    }

    private fun requestIgnoreBatteryOptimizationsIfNeeded() {
        try {
            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val pkg = packageName
            if (pm.isIgnoringBatteryOptimizations(pkg)) return
            val prefs = getSharedPreferences("piles_clinic_battery_opt", android.content.Context.MODE_PRIVATE)
            if (prefs.getBoolean("asked_once", false)) return
            prefs.edit().putBoolean("asked_once", true).apply()
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$pkg")
            startActivity(intent)
        } catch (_: Throwable) { }
    }

    /** Shows "📞 আজ N কল বাকি" on the dashboard and opens today's due list on tap.
     *
     * CHANGE-4 (TK, safe patch): this count must exactly match the number of
     * records the person sees after tapping the banner. Tapping opens
     * FollowUpActivity with todayOnly=true, which always lands on the
     * Inquiry (Enquiry) tab with the "Today" date filter applied — i.e.
     * FollowUpRepository.fetchTab("Inquiry", ...) filtered to nextFollow ==
     * today. The count here now runs that exact same repository call and
     * the exact same today-filter, instead of a separate hand-rolled query
     * over all three stages — so banner count and opened-list count can
     * never drift apart again. */
    /** ⛔ একবারে একটাই গোনা। এই ফাংশনটা পরপর দুবার আসতে পারে; দুটো ভারী পড়া
     *  তখন একসঙ্গে চলে যেত (ফাঁকের ঘড়িটা বসে ক্লাউডের উত্তরের **পরে**, তাই
     *  দ্বিতীয় ডাক সেটা দেখতেই পেত না)। এই চিহ্নটা মেইন থ্রেডে **সঙ্গে সঙ্গে**
     *  বসে, তাই ঐ দৌড় আর সম্ভব নয়।
     *  ⛔ এটা **এই পর্দার নিজের** (static নয়) — পর্দা মরে গেলে এমনিতেই মুছে
     *     যায়, তাই কখনো চিরকালের জন্য আটকে থাকতে পারে না।
     *  ⛔ এটা বসে **জমানো সংখ্যাটা পর্দায় দেখানোর পরে** — তাই ব্যস্ত থাকলেও
     *     ফোনের নিজের হিসাব দেখানো কখনো বাদ যায় না। */
    private var bannerBusy = false

    private fun refreshCallBanner(session: NativeUser) {
        lifecycleScope.launch {
          var held = false
          try {
            val repo = FollowUpRepository(this@DashboardActivity)
            // 🔴🔒 B500 (06.08.2026, TK-নির্দেশ — "যে কাজ করবে তার ফোনে
            // সাথে সাথে দেখাতে হবে, সব ব্রাঞ্চে সমানভাবে") — এর আগে এই
            // ব্যানার সরাসরি ক্লাউডের উত্তরের অপেক্ষা করত (কিছু সময় ফাঁকা/
            // দেরি দেখাত)। এখন `FollowUpActivity.kt`-এর মতোই — আগে ফোনের
            // জমানো তথ্য (`loadCachedTab`, তাৎক্ষণিক, নেটওয়ার্ক ছাড়া)
            // দিয়ে ব্যানার সঙ্গে সঙ্গে দেখানো হয়, তারপর নিঃশব্দে ক্লাউড
            // থেকে হালনাগাদ সংখ্যা এলে বদলে যায়। ⛔ গোনার নিয়ম/ব্রাঞ্চ-
            // ফিল্টার/টাকার হিসাব কিছুই বদলায়নি — শুধু কখন দেখানো হয়
            // সেটাই এগিয়ে আনা হলো।
            val today = FollowUpModel.today()
            // ══════════════════════════════════════════════════════════════
            // 🔴🔴🔒 V512 (২১.০৮.২০২৬) — TK-এর লক-করা দাবি: *"ব্যানারে চাপ দিলে
            //    যে কটা দেখাবে সে কটা ব্যক্তিকেই কল করা যাবে তার ব্যবস্থা রাখুন।"*
            //
            // V511-এ **তারিখের** নিয়মটা মেলানো হয়েছিল (`bannerCallsOnly`)।
            // কিন্তু **ব্রাঞ্চের** নিয়ম তখনো আলাদাই ছিল — কোড ধরে প্রমাণিত:
            //   • ব্যানার গুনত `session.branch` ধরে। মাস্টারের `session.branch`
            //     আক্ষরিক **"All"** (StaffDirectory.kt:26) → **পাঁচ ব্রাঞ্চ**।
            //   • কিন্তু ব্যানারে চাপ দিলে যে পর্দা খোলে
            //     (`FollowUpActivity.effectiveBranch()`) সেটা মাস্টারের জন্য
            //     **বেছে রাখা ব্রাঞ্চ** (`BranchFilterStore`) ধরে তালিকা আনে।
            //   ⇒ মাস্টার "Cooch Behar" বেছে রাখলে ব্যানারে পাঁচ ব্রাঞ্চের সংখ্যা,
            //     আর ভিতরে শুধু Cooch Behar-এর নাম — সংখ্যাটা কখনোই মিলত না।
            //   • JPE-CRP স্টাফের বেলায় উল্টোটা: ব্যানার শুধু নিজের ব্রাঞ্চ গুনত,
            //     ভিতরে Falakata+Birpara-ও দেখাত (V453) — ভিতরে **বেশি** নাম।
            //
            // ⇒ এখন দুই জায়গায় **হুবহু একই ব্রাঞ্চ-নিয়ম** ব্যবহার হয়।
            // ⛔ নতুন কোনো নিয়ম বানানো হয়নি — `FollowUpActivity.effectiveBranch()`
            //    যা করে, অক্ষরে অক্ষরে সেটাই এখানে ডাকা হলো।
            // ⛔ মাস্টার এখনো ব্রাঞ্চ না-বাছলে `BranchFilterStore.get()` ফাঁকা
            //    ফেরায়, আর `branchScopeFilter()` (লাইন ২১৩–২১৫) ফাঁকা মানে
            //    "ছাঁকনি নেই" ধরে — অর্থাৎ **আগের মতোই পাঁচ ব্রাঞ্চ**। তাই
            //    পুরোনো কোনো আচরণ হারায়নি।
            // ⛔ Egress বাড়ে না — বরং মাস্টার একটা ব্রাঞ্চ বেছে রাখলে সার্ভারেই
            //    ছাঁকা হয়, তাই কম ডেটা নামে। জমানো কপির চাবিও (`loadCachedTab`)
            //    এখন তালিকার চাবির সাথে এক, তাই দুটো আলাদা কপি আর জমে না।
            // ══════════════════════════════════════════════════════════════
            val bannerBranch = try {
                if (session.role == "master") BranchFilterStore.get(this@DashboardActivity)
                else CrossBranchStaffAccess.effectiveViewBranch(session)
            } catch (_: Throwable) { session.branch }
            /* 🟢🔒 V590 (TK-রিপোর্ট) — আগে শুধু **ঠিক আজকের** কল গোনা হত, তাই
               একদিন বাদ পড়া কল ব্যানার থেকে চিরতরে হারিয়ে যেত। এখন **আজকের ও
               বকেয়া** — দুটোই। ⛔ তারিখ ফাঁকা হলে (কল ঠিক করা নেই) গোনা হয় না। */
            /* 📵🔒 V1331 (১১.০৯.২০২৬, TK-নির্দেশ ও গভীর যাচাই — তালিকা সারি ৪৩৩):
               TK-রিপোর্ট: স্টাফ কল করে Remark লিখে "আর কল লাগবে না" বলার পরেও
               লোকটা ব্যানারের সংখ্যায় থেকে যাচ্ছিল। **আসল কারণ (কোডে ধরা):**
               এই `isDue()`/`overdueFrom()` কখনো `noMoreCalls` দেখত না — অথচ
               ব্যানারে চাপ দিয়ে যে তালিকা খোলে (`FollowUpActivity.kt:1059`)
               সেটা ঠিকই বাদ দেয়, আর "আর কল লাগবে না" বোতামের নিজের কথাই
               ("কল-তালিকা ও ব্যানার থেকে সরে যাবে") এটাই প্রতিশ্রুতি দেয়।
               ⇒ এখন গোনাতেও একই শর্ত — ব্যানার আর তালিকা সবসময় মিলবে।
               ⛔ বাকি নিয়ম (আজ + বকেয়া দুটোই গোনা, V590) এক অক্ষরও বদলায়নি। */
            // 📞 V1403 — কল হয়ে গেলে আর বাকি নয় (FollowUpModel.callPending — তালিকা/নোটিফিকেশনের একই নিয়ম)
            fun isDue(f: FollowUpItem): Boolean = FollowUpModel.callPending(f, today)
            fun countFrom(items: List<FollowUpItem>?): Int = items?.count { isDue(it) } ?: 0
            fun overdueFrom(items: List<FollowUpItem>?): Int =
                items?.count { FollowUpModel.callPending(it, today) && it.nextFollow < today } ?: 0   // 📞 V1403
            // 🟢🔒 V607 (২৪.০৮.২০২৬, TK-নির্দেশ) — একই তিনটে cache-পড়া থেকেই
            // (নতুন কোনো fetch নেই — V509-এর egress-সুরক্ষা অক্ষত) সব আইটেম
            // জমিয়ে রাখা হচ্ছে, যাতে নিচে ব্রাঞ্চ ধরে ভাঙা যায়।
            val allDueItems = mutableListOf<FollowUpItem>()
            val instant = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    listOf("Inquiry", "Patient", "Treatment").sumOf { stage ->
                        val items = repo.loadCachedTab(stage, bannerBranch)
                        items?.filter { isDue(it) }?.let { allDueItems.addAll(it) }
                        countFrom(items)
                    }
                } catch (_: Exception) { 0 }
            }
            /* 🟢 V590 — কতগুলো **বকেয়া** (আজকের নয়, আগের) সেটাও আলাদা করে
               দেখানো হয়, নইলে স্টাফ বুঝতেন না পুরনো কল জমে আছে। */
            var overdue = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    listOf("Inquiry", "Patient", "Treatment").sumOf { stage ->
                        overdueFrom(repo.loadCachedTab(stage, bannerBranch))
                    }
                } catch (_: Exception) { 0 }
            }
            fun render(count: Int) {
                // 🔵 B614 (10.08.2026, TK-নির্দেশ): ডাক্তার ফলো-আপ কল করেন না —
                // তাই তাঁর ড্যাশবোর্ডে "calls pending" ব্যানার দেখানো হয় না।
                if (count > 0 && session.displayRole != "doctor") {
                    binding.tvCallBanner.visibility = android.view.View.VISIBLE
                    // 🎨 V1084 — লেখাটা এখন গোলাপি কার্ডের ভিতরে, তাই কার্ডটাও
                    //    ঠিক একই সময়ে দেখা যায়/লুকায়। নিয়ম এক অক্ষরও বদলায়নি।
                    binding.callBannerCard.visibility = android.view.View.VISIBLE
                    /* ═══════════════════════════════════════════════════
                       📞🔒 V1115 (০৫.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ):
                       *"51 Calls Pending, 33 over Due তার পাশের ট্যাপ টু কল —
                       এটা রাখতে হবে না"* · *"Call icon 2 বার কেন? এখানে
                       Over due-ও লেখা থাকবে না"*
                       ⇒ পট্টিতে এখন শুধু **"51 calls pending"**।
                         · লেখার ভিতরের দ্বিতীয় 📞 বাদ (বাঁয়ে গোল আইকনটা
                           আগে থেকেই আছে, তাই দুবার দেখাত)
                         · *"— N overdue"* বাদ
                         · *"— tap to call"* বাদ (পাশেই **Call now ›** বোতাম)
                       ⛔ চাপ দিলে আগের মতোই আজকের কলের তালিকা খোলে — কাজের
                          কিছুই বদলায়নি, শুধু লেখা।
                       ⛔ `overdue` গোনাটা মোছা হয়নি — অন্য কাজে লাগে। */
                    // 🎨🔒 V1138 (TK-অনুমোদিত ফটো-প্রুফ) — TK: *"Pending Calls 49
                    //    এরকম হবে, একটু বোল্ড হবে"*। ⛔ সংখ্যা · চাপ · দেখা-না-দেখা
                    //    সবই আগের মতোই — শুধু লেখার ধরন বদলেছে।
                    binding.tvCallBanner.text = "Pending Calls $count"
                    binding.tvCallBanner.setOnClickListener {
                        startActivity(Intent(this@DashboardActivity, FollowUpActivity::class.java).putExtra("todayOnly", true))
                    }
                    // 🎨 V1084 — TK-এর নকশার "Call now ›" বোতাম, ঠিক একই জায়গায় নিয়ে যায়।
                    binding.btnCallNow.setOnClickListener {
                        startActivity(Intent(this@DashboardActivity, FollowUpActivity::class.java).putExtra("todayOnly", true))
                    }
                } else {
                    binding.tvCallBanner.visibility = android.view.View.GONE
                    binding.callBannerCard.visibility = android.view.View.GONE
                }
            }
            render(instant)

            // 🟢🔒 V607 (২৪.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — Master-only
            // ব্রাঞ্চ-ভিত্তিক ভাঙা। ⛔ নতুন fetch নেই — উপরের `allDueItems`
            // (একই cache-পড়া) থেকেই ব্রাঞ্চ ধরে গোনা হচ্ছে।
            // ⛔ সৎ সীমা: স্টাফ-ভিত্তিক ভাঙা সম্ভব না — FollowUpItem-এ কোন
            // ফলো-আপ কার দায়িত্বে তা রাখা হয় না, শুধু ব্রাঞ্চ আছে।
            if (session.role == "master" && instant > 0) {
                val byBranch = allDueItems.groupBy { it.branch.ifBlank { "—" } }
                    .mapValues { (_, items) ->
                        Pair(items.size, items.count { it.nextFollow.isNotBlank() && it.nextFollow < today })
                    }
                    .toList().sortedByDescending { it.second.first }
                if (byBranch.size > 1) {   // একটাই ব্রাঞ্চ হলে দেখানোর মানে নেই, ব্যানারই যথেষ্ট
                    binding.tvCallBreakdownLink.visibility = android.view.View.VISIBLE
                    var expanded = false
                    fun buildRows() {
                        binding.callBreakdownRows.removeAllViews()
                        val d = resources.displayMetrics.density
                        for ((branch, nums) in byBranch) {
                            val (pending, ov) = nums
                            val row = android.widget.TextView(this@DashboardActivity).apply {
                                text = "$branch — $pending pending" + (if (ov > 0) " · $ov overdue" else "")
                                textSize = 12.5f
                                setTextColor(android.graphics.Color.parseColor(if (ov > 0) "#D92D20" else "#374151"))
                                setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
                                setBackgroundColor(android.graphics.Color.WHITE)
                                isClickable = true; isFocusable = true
                                setOnClickListener {
                                    // 🔴🔒 ঠিক যা `showBranchPickerMenu()` (FollowUpActivity.kt) করে —
                                    // Master-এর ব্রাঞ্চ-বাছাই এই একটাই জায়গায় জমা থাকে
                                    // (BranchFilterStore), আলাদা কোনো intent-extra পড়া হয় না।
                                    BranchFilterStore.set(this@DashboardActivity, branch)
                                    startActivity(
                                        Intent(this@DashboardActivity, FollowUpActivity::class.java)
                                            .putExtra("todayOnly", true)
                                    )
                                }
                            }
                            binding.callBreakdownRows.addView(row)
                        }
                    }
                    binding.tvCallBreakdownLink.setOnClickListener {
                        expanded = !expanded
                        if (expanded) { buildRows(); binding.callBreakdownRows.visibility = android.view.View.VISIBLE }
                        else binding.callBreakdownRows.visibility = android.view.View.GONE
                        binding.tvCallBreakdownLink.text = if (expanded) "▲ Hide breakdown" else "👁 Breakdown by branch"
                    }
                } else {
                    binding.tvCallBreakdownLink.visibility = android.view.View.GONE
                    binding.callBreakdownRows.visibility = android.view.View.GONE
                }

                // 🟢🔒 V607 (২৪.০৮.২০২৬, TK-নির্দেশ — "৩+ দিন ওভারডিউ হলে
                // সরাসরি Master-কেও জানাতে হবে") — একই cache-পড়া ডেটা
                // পুনর্ব্যবহার (নতুন fetch নেই)। দিনে **একবারই** পাঠানো হয়
                // (SharedPreferences-এ আজকের তারিখ জমা রেখে) — নইলে Master
                // Dashboard-এ ফেরার সাথে সাথেই বারবার নোটিশ জমত।
                try {
                    val threeDaysAgo = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(java.util.Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000))
                    val badlyOverdue = allDueItems.filter { it.nextFollow.isNotBlank() && it.nextFollow <= threeDaysAgo }
                    if (badlyOverdue.isNotEmpty()) {
                        val alertPrefs = getSharedPreferences("piles_clinic_overdue_alert", android.content.Context.MODE_PRIVATE)
                        val lastSent = alertPrefs.getString("last_sent_date", "")
                        if (lastSent != today) {
                            val byBranch2 = badlyOverdue.groupBy { it.branch.ifBlank { "—" } }
                            val lines = byBranch2.entries.sortedByDescending { it.value.size }
                                .joinToString("\n") { (br, items) -> "$br — " + items.size + " calls overdue 3+ days" }
                            BriefingRepository().post(
                                this@DashboardActivity,
                                "⚠️ Overdue Follow-up Alert",
                                lines,
                                "individual", session.branch, "", session.mobile, session.mobile
                            )
                            alertPrefs.edit().putString("last_sent_date", today).apply()
                        }
                    }
                } catch (_: Throwable) { /* এই সতর্কতা কখনো ড্যাশবোর্ড আটকাতে পারবে না */ }
            } else {
                binding.tvCallBreakdownLink.visibility = android.view.View.GONE
                binding.callBreakdownRows.visibility = android.view.View.GONE
            }
            // ══════════════════════════════════════════════════════════════
            // 🔴🔴💸 V509 (২১.০৮.২০২৬, TK-নির্দেশ — Supabase Egress ১০০% ছুঁয়ে
            //   ফেলার পরে) — **এখানেই ছিল সবচেয়ে বড় ফুটো।**
            //
            // ─── মেপে দেখা হিসাব (আন্দাজ নয়) ─────────────────────────────
            //   নিচের কাজটা **তিনটে পূর্ণ `fetchTab()`** চালায়, আর একেকটা
            //   `fetchTab()` নামায় followups + patients + payments —
            //   TK-এর মাপা টেবিল-সাইজ অনুযায়ী মিলিয়ে ≈ **১ MB-র বেশি**।
            //   আর এটা চলত **হোম পর্দায় প্রতিবার ফিরলেই** (`onResume`) —
            //   অর্থাৎ অ্যাপের ভিতরে যে কোনো পর্দা থেকে ব্যাক চাপলেই।
            //   দিনে ৩০ বার × ১০ ফোন × ~১.৩ MB ≈ **৪০০ MB/দিন** — Supabase-এর
            //   দেখানো ৩৮১–৬৯৭ MB/দিনের সঙ্গে হুবহু মেলে।
            //
            // ─── এখন কী হয় ───────────────────────────────────────────────
            //   ফেরার আগে **একটা সস্তা প্রশ্ন**: "গতবার গোনার পরে followups ·
            //   enquiries · patients · payments-এর কোনোটায় কিছু বদলেছে?"
            //   ⛔ এই প্রশ্নে **একটাও সারি নামে না** (HEAD, কয়েকশো বাইট)।
            //   কিছু না বদলালে উপরের জমানো সংখ্যাটাই থাকে — যেটা ঠিকই আছে,
            //   কারণ কিছু বদলায়ইনি। বদলালে **আগের মতোই হুবহু একই তিনটে
            //   `fetchTab()`** চলে, তাই সংখ্যাটা ব্যানারে আর তালিকায় এক
            //   থাকার নিয়ম (CHANGE-4) এক অক্ষরও ভাঙে না।
            //
            // ─── ⛔ যেসব ভুল ইচ্ছে করে এড়ানো হয়েছে ──────────────────────
            //   • **প্রথমবার সবসময় সত্যিকারের গোনা** (`bannerCloudDone`) —
            //     নইলে অ্যাপ খোলার পরেই ব্যানার ফাঁকা থাকতে পারত।
            //   • পাহারায় **ব্রাঞ্চের ছাঁকনি বসানো হয়নি** (`null`) — কারণ
            //     V453-এর cross-branch স্টাফ নিজের ব্রাঞ্চের বাইরের সারিও
            //     দেখেন; ছাঁকনি বসালে ওদের বদল ধরা পড়ত না।
            //   • `fetchTabDelta()` **ব্যবহার করা হয়নি** — delta পূর্ণ-রিফ্রেশের
            //     ব্যবধান পর্যন্ত (V1258 থেকে ৩ ঘণ্টা) hard-delete মিস করতে
            //     পারে, তাতে ব্যানারের সংখ্যা
            //     আর তালিকার সংখ্যা আলাদা হয়ে যেত (TK-এর ৫৬ বনাম ৪৭-এর
            //     সমস্যার মতোই)। ঝুঁকি নেওয়া হয়নি।
            //   • অন্তত ৩ মিনিটের ফাঁক — পরপর ব্যাক চাপলে ঝড় ওঠে না।
            //
            // ─── ⛔ যাচাইয়ে ধরা পড়া পাঁচটা ফাঁদ, পাঁচটাই বন্ধ করা হয়েছে ────
            //   ক) **সময়ের সীমা `changed()`-এর আগে, পরে নয়।**
            //      `LiveRefresh.Watch.changed()` `true` ফেরার সঙ্গে সঙ্গেই
            //      নিজের ঘড়ি এগিয়ে দেয়। তাই "বদলেছে" জেনেও সময়-সীমার কারণে
            //      কাজ না করলে ঐ বদলটা **আর কোনোদিন ধরা পড়ত না**। এখন
            //      `changed()` ডাকাই হয় তখন, যখন `true` পেলে সত্যিই গোনা হবে।
            //   খ) **জমানো উত্তর দুই জায়গাতেই মোছা হয়।** `fetchTab()`-এর পড়া
            //      দুটো স্তরে জমা থাকে — `CloudReadCache` (২০ সেকেন্ড) **আর**
            //      `CloudReadDedupe` (৬০ সেকেন্ড, `SupabaseClient.fetchListOrNull`
            //      এর ভিতরে)। অন্য ফোনের লেখায় কোনোটাই মোছে না। শুধু প্রথমটা
            //      মুছলে ৬০ সেকেন্ডের পুরনো উত্তরই ফিরত আর নতুন সারিটা
            //      **চিরকালের জন্য** বাদ পড়ে যেত। প্রকল্পের সব জায়গায় (যেমন
            //      `SupabaseClient`-এর ৭টা জায়গা ও `SessionGuard`) দুটো একসাথেই
            //      মোছা হয় — এখানেও তাই।
            //   গ) **নির্দিষ্ট সময় পরপর জোর করে পূর্ণ গোনা** (`BANNER_FULL_GAP_MS` —
            //      V1258 থেকে ৩ ঘণ্টা, আগে ৩০ মিনিট)।
            //      `changed()` শুধু `updatedAt` দেখে, তাই **সত্যিকারের ডিলিট**
            //      (Trash → Delete Forever) সে কোনোদিন দেখতে পায় না; সংখ্যাটা
            //      বেশি দেখাতেই থাকত। এই জোর-করা গোনাটা প্রকল্পের প্রমাণিত
            //      নিয়ম (`FollowUpRepository.FU_FULL_REFRESH_INTERVAL_MS`-ও
            //      ঠিক একই মাপ, ঠিক এই কারণেই — দুটো সবসময় একসাথে বদলাবে)। এটা নেট-ব্যর্থতা বা মাঝপথে
            //      পর্দা বন্ধ হয়ে যাওয়ার ক্ষেত্রেও নিরাপত্তা-জাল।
            //   ঘ) **HEAD-প্রশ্নেরও নিজের ফাঁক** (`BANNER_PROBE_GAP_MS`)।
            //      নইলে কিছু না বদলালে প্রতিবার ব্যাক চাপলেই ৪টা করে প্রশ্ন যেত।
            //   ঙ) **ঘড়িগুলো পর্দার নয়, অ্যাপের** (companion) — ফোন ঘোরালে বা
            //      পর্দা নতুন করে তৈরি হলে ফাঁকটা মুছে গিয়ে পুরো ভারী গোনা
            //      আবার চলত। চাবিতে মোবাইল নম্বরও আছে, তাই অন্য কেউ লগইন করলে
            //      আগের হিসাব কখনো ব্যবহার হয় না।
            // ══════════════════════════════════════════════════════════════
            run {
                if (bannerBusy) return@launch
                // 🔴 V512: চাবিতেও **যে ব্রাঞ্চ ধরে গোনা হচ্ছে** সেটাই থাকবে —
                //   নইলে মাস্টার ব্রাঞ্চ বদলালেও পুরোনো ফাঁক-হিসাব ধরে নতুন
                //   গোনাটা আটকে যেত, আর ব্যানারে আগের ব্রাঞ্চের সংখ্যা বসে থাকত।
                val key = "dashbanner|" + session.mobile + "|" + bannerBranch
                val nowMs = System.currentTimeMillis()
                val firstEver = !bannerCloudDone || bannerKey != key
                val forced = firstEver || (nowMs - bannerCloudAt >= BANNER_FULL_GAP_MS)
                if (!forced) {
                    if (nowMs - bannerCloudAt < BANNER_MIN_GAP_MS) return@launch
                    if (nowMs - bannerProbeAt < BANNER_PROBE_GAP_MS) return@launch
                }
                bannerBusy = true
                held = true
                bannerProbeAt = nowMs
                // ⛔ জোর-করা গোনার সময়েও পাহারাটা **ডাকা হয়** (ফল ফেলে দেওয়া
                //   হয়) — শুধু তার "এই সময় পর্যন্ত দেখা হয়েছে" ঘড়িটা বসানোর
                //   জন্য। না ডাকলে এই গোনা আর পরের ডাকের মাঝের বদলগুলো হারাত।
                val moved = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    bannerWatch.changed(key, null)
                }
                if (!forced && !moved) return@launch
                try { CloudReadCache.clear() } catch (_: Throwable) { }    // ফাঁদ (খ)
                try { CloudReadDedupe.clear() } catch (_: Throwable) { }   // ফাঁদ (খ)
                bannerCloudDone = true
                bannerKey = key
                bannerCloudAt = System.currentTimeMillis()
            }
            // 🔒 খাতার সারি B61 (TK, 29.07.2026): *"টুডে পেন্ডিং কল — এখানে
            // শুধুমাত্র এনকোয়ারি সেকশনের নাম্বার আসলে হবে না। ভিজিট সেকশন এবং
            // পেসেন্ট সেকশন — অর্থাৎ ফলো আপের তিন জায়গা থেকেই এখানে আসতে হবে।"*
            //
            // Follow-up-এর তিনটে ভাগের ভিতরের নাম (কোড দেখে মিলিয়ে নেওয়া,
            // আন্দাজ নয় — `loadTabCounts()`-এ ঠিক এই তিনটেই ব্যবহার হয়):
            //   👥 Enquiry → "Inquiry"   ·   👣 Visit → "Patient"   ·   👤 Patient → "Treatment"
            // তিনটেই **একসঙ্গে (পাশাপাশি)** আনা হয়, তাই আগের চেয়ে দেরি হয় না।
            // ⛔ গোনার নিয়ম আগের মতোই — Next Follow-up তারিখ = আজ।
            val count = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    kotlinx.coroutines.coroutineScope {
                        val jobs = listOf("Inquiry", "Patient", "Treatment").map { stage ->
                            async {
                                try {
                                    repo.fetchTab(stage, bannerBranch, session.name, session.mobile)
                                        .count { isDue(it) }   // 🟢 V590: আজ + বকেয়া
                                } catch (_: Exception) { 0 }
                            }
                        }
                        var total = 0
                        for (j in jobs) total += j.await()
                        total
                    }
                } catch (_: Exception) { instant } // ব্যর্থ হলে তাৎক্ষণিক সংখ্যাই থাকুক, শূন্য না
            }
            /* 🟢 V590 — ক্লাউডের আসল তালিকা এলে বকেয়ার সংখ্যাটাও তখনই মিলিয়ে
               নেওয়া হয়, নইলে ব্যানারে পুরনো (জমানো) সংখ্যা থেকে যেত। */
            overdue = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    listOf("Inquiry", "Patient", "Treatment").sumOf { stage ->
                        overdueFrom(repo.loadCachedTab(stage, bannerBranch))
                    }
                } catch (_: Exception) { overdue }
            }
            render(count)
          } finally { if (held) bannerBusy = false }
        }
    }

    /**
     * 🔒 TK-APPROVED (28.07.2026, ফটো-প্রুফে লক · খাতার সারি B36).
     *
     * TK-এর প্রশ্ন ছিল: *"কোন স্টাফ যদি চেম্বার বন্ধ করতে ভুলে যায় তাহলে কী হবে?"*
     * স্টাফকে রাত ৭টা—১২টা তাগাদা দেওয়া হত, কিন্তু **মাস্টার কখনো জানতেই
     * পারতেন না**। এখন বিগত ৭ দিনের যে দিনগুলোর চেম্বার বন্ধ হয়নি, সেগুলো
     * মাস্টারের ড্যাশবোর্ডে দেখায়; সারিতে চাপ দিলে সোজা ওই দিনের চেম্বার খোলে।
     *
     * ⛔ **শুধু মাস্টার।** স্টাফ/ডাক্তারের পর্দায় এটা কখনো দেখাবে না।
     * ⛔ **কিছু না থাকলে পুরো কার্ডটাই লুকানো** — ড্যাশবোর্ডের আগের চেহারা অক্ষত।
     * ⛔ খোঁজাটা সম্পূর্ণ পিছনে হয় (মাত্র দুটো অনুরোধ), পর্দা কখনো অপেক্ষা করে না।
     */
    private fun refreshUnclosedChambers(session: NativeUser) {
        // 🔒 TK-APPROVED (28.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B46).
        //
        // TK-এর কথা: *"পুরো স্ক্রিন জুড়ে এইভাবে যেন ওপেন হয়ে না থাকে — এটা
        // দরকার, মডিউলের মধ্যে রাখেন... চেম্বার ক্লোজ একটা মেনু বারের মধ্যে
        // রাখুন।"* সাত দিনের হিসাবেই ২৪টা সারি হয়ে হোম পেজ অসম্ভব লম্বা হয়ে
        // যাচ্ছিল।
        //
        // তাই এই কার্ডটা হোম পেজে আর কখনো দেখাবে না। ঠিক একই তালিকা এখন
        // ☰ মেনুর **Chamber Close** ঘরে (ChamberCloseActivity), আর সেখান
        // থেকেই পুরনো দিনের চেম্বার বন্ধও করা যায়।
        //
        // ⛔ লেআউটের ঘরটা মোছা হয়নি (`unclosedCard`), শুধু চিরকাল লুকানো —
        //    দরকার হলে TK-এর কথায় এক লাইনে ফেরানো যাবে।
        binding.unclosedCard.visibility = android.view.View.GONE
    }

    /**
     * 🔴🆕🔒 V436 (TK-রিপোর্ট ১৮.০৮.২০২৬ — Dr. K.H MANDAL-এর ফোনে নতুন APK
     * বসানোর পরেও পুরনো **V259**-ই চলছিল, অথচ কেউ টেরই পাননি)।
     * এখন অ্যাপ নিজেই ওয়েবসাইটে রাখা ছোট্ট `version.json` দেখে বুঝে নেয় সে
     * পুরনো কিনা — পুরনো হলে ড্যাশবোর্ডে লাল পটি ওঠে।
     * ⛔ **সম্পূর্ণ নিরাপদ:** কাজটা পিছনের থ্রেডে ও `try/catch`-এ মোড়া; নেট না
     *    থাকলে বা ফাইল না পেলে **কিছুই হয় না**, পটিটা লুকানোই থাকে। কোনো
     *    ডেটা পড়া/লেখা নেই, Supabase-এ একটাও অনুরোধ যায় না (কোটায় প্রভাব শূন্য)।
     *    দিনে একবারের বেশি দেখা হয় না। নতুন ভার্সন **বেশি** হলে তবেই দেখায়।
     * ⛔ sync-এর পুরনো সতর্কবার্তার পটিটা আলাদাই আছে — সেটা এক চুলও বদলায়নি।
     */
    private fun refreshOldAppBanner() {
        try {
            val show = { newer: Int ->
                runOnUiThread {
                    try {
                        if (!isFinishing && !isDestroyed) {
                            if (newer > com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE) {
                                binding.tvOldAppText.text =
                                    NoBengali.s("Your app is old — please install the new version") +
                                        "  (V" + com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE +
                                        " → V" + newer + ")"
                                binding.oldAppBanner.visibility = android.view.View.VISIBLE
                            } else {
                                binding.oldAppBanner.visibility = android.view.View.GONE
                            }
                        }
                    } catch (_: Throwable) { }
                }
            }
            // আগেরবার যা জানা ছিল, সেটা সঙ্গে সঙ্গে দেখানো হয় (নেট লাগে না)
            show(com.tkbiswas.pilesclinic.native.AppVersionCheck.newerVersionOrZero(this))
            // তারপর পিছনে গিয়ে (দিনে একবার) আবার দেখে নেওয়া
            com.tkbiswas.pilesclinic.native.AppVersionCheck.refresh(this, false) { newer -> show(newer) }

            /* 📱🔒 V771 (২৮.০৮.২০২৬, TK-নির্দেশ: *"আমি কি করে জানবো — App থেকে
               দেখার ব্যবস্থা রাখুন"*) — এই ফোন নিজের ভার্সনটা মেঘকে জানিয়ে দেয়,
               যাতে মাস্টার এক পর্দাতেই দেখতে পান কোন ফোনে কোন ভার্সন চলছে।
               ⚡ দিনে **একবার** (ভার্সন বদলালে সঙ্গে সঙ্গে) — একটাই ছোট্ট ডাক।
               ⛔ 🧵 আলাদা থ্রেডে; ব্যর্থ হলে চুপচাপ ছেড়ে দেয়, কিছুই আটকায় না।
               ⛔ পুরনো ভার্সন-সতর্কবার্তার কোড (উপরে) এক অক্ষরও ছোঁয়া হয়নি। */
            try {
                /* 🔴 V775 — এখানে `user` নামে কিছু **নেই**। ওটা `onCreate()`-এর
                   ভিতরের নিজস্ব ভেরিয়েবল (line 79), এই ফাংশনের নয় — V771-এ
                   আমি ভুল করে ওটাই লিখে ফেলেছিলাম, তাই Android Studio-তে
                   বিল্ড ভেঙেছিল ("Unresolved reference: user")।
                   ⇒ এখন সরাসরি সেশন থেকেই নেওয়া হয়, যেভাবে এই ফাইলের
                     আরও ৪ জায়গায় নেওয়া হয় (line 39 · 258 · 315)। */
                val mob = NativeSession.current(this)?.mobile.orEmpty()
                if (mob.isNotBlank()) Thread {
                    com.tkbiswas.pilesclinic.native.AppVersionReporter
                        .reportIfDue(applicationContext, mob)
                }.start()
            } catch (_: Throwable) { }
        } catch (_: Throwable) { }
    }

    /** Sync indicator: reflects the offline pending-write queue (☁️ Synced / ⏳ N to sync). */
    private fun refreshSyncStatus() {
        // TK-APPROVED (2026-07-26, photo proof): this used to look at the
        // Registration queue ONLY, so anything stuck in the other eight
        // queues (Payment, Enquiry, Follow-up, Chamber, Prescription,
        // Briefing, corrections) was invisible. PendingSyncStatus counts all
        // of them; nothing is written or changed by the count itself.
        // 🚨 TK-REPORTED (28.07.2026, খাতার সারি B31): *"৫-৭ বার টাচ করার পরে
        // ওপেন হলো।"* এই গোনাটা **পর্দার নিজের থ্রেডে** হত — ন'টা অপেক্ষমাণ
        // তালিকার প্রতিটা পুরো পড়ে ফেলত। জমা কাজ বেশি থাকলে ওই সময়টুকু পর্দা
        // কোনো চাপই নিত না, তাই বারবার চাপতে হত।
        // এখন গোনাটা পিছনে হয়, আর শুধু সংখ্যাটা পর্দায় বসে।
        // ⛔ কী গোনা হয় বা কী দেখানো হয় — কিছুই বদলায়নি।
        lifecycleScope.launch {
            val s = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try { PendingSyncStatus.summary(this@DashboardActivity) }
                catch (_: Throwable) { PendingSyncStatus.Summary(0, "") }
            }
            if (isFinishing || isDestroyed) return@launch
            // 🔴🔴🔴 TK-ORDER (31.07.2026, tk_guard.py-এর [৯.৮] ধরে ফেলেছে):
            // এখানে "V210" হার্ডকোড করা ছিল — প্রতিটা নতুন ভার্সনে (V211,
            // V212...) হাতে করে বদলাতে ভুলে যাওয়ার ঝুঁকি ছিল, ঠিক যা TK-এর
            // D নিয়ম ("পুরনো/ভুল Version দেখা যাবে না") নিষেধ করে। এখন
            // MoreMenuActivity.kt-এর মতোই সরাসরি build.gradle.kts-এর
            // versionCode থেকে — ভবিষ্যতে কখনো হাতে বদলাতে হবে না।
            val vLabel = "V" + com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE
            binding.tvSyncStatus.text = if (s.total > 0) "⏳ ${s.total} to sync · $vLabel" else "☁️ Synced · $vLabel"
            if (s.total > 0) {
                binding.tvSyncWarnText.text =
                    "${s.total} item(s) not yet sent to the cloud" + if (s.detail.isNotBlank()) "\n(${s.detail})" else ""
                binding.syncWarnBanner.visibility = android.view.View.VISIBLE
            } else {
                binding.syncWarnBanner.visibility = android.view.View.GONE
            }
        }
        val retry = android.view.View.OnClickListener {
            binding.tvSyncWarnText.text = "Trying to send..."
            lifecycleScope.launch {
                val after = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    PendingSyncStatus.retryAll(this@DashboardActivity)
                    PendingSyncStatus.summary(this@DashboardActivity)
                }
                refreshSyncStatus()
                android.widget.Toast.makeText(
                    this@DashboardActivity,
                    if (after.total == 0) "All records are now saved in the cloud"
                    else "${after.total} still waiting — try again when the network is back",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.btnSyncRetry.setOnClickListener(retry)
        /* 🟢🔒 V706 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত) — TK: *"কোন
           পেশেন্ট এর পেমেন্ট আটকে রয়েছে সেটাই বা আমি জানবো কি করে"*।
           ⇒ লাল বাক্সে **চাপ দিলে এখন তালিকা** খোলে (নাম · রোগী নম্বর · টাকা ·
             তারিখ), আর তালিকার নিচেই "Send All" — সেটা হুবহু আগের `retry`-ই
             চালায়, নতুন কোনো পাঠানোর পথ বানানো হয়নি।
           ⛔ পাশের ছোট "send" বোতাম (`btnSyncRetry`) আগের মতোই সরাসরি পাঠায় —
              এক অক্ষরও বদলায়নি, তাই পুরোনো অভ্যাস অটুট।
           ⛔ দীর্ঘ-চাপের কাজটাও (নিচে, B274) অপরিবর্তিত।
           ⛔ TK-নির্দেশ: *"বাংলা হবে না, শুধুমাত্র ইংরেজিতে করুন"* ⇒ এই
              পপ-আপের প্রতিটা লেখা ইংরেজি। */
        binding.syncWarnBanner.setOnClickListener {
            lifecycleScope.launch {
                val items = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try { PendingSyncStatus.details(this@DashboardActivity) }
                    catch (_: Throwable) { emptyList<PendingSyncStatus.Item>() }
                }
                if (isFinishing || isDestroyed) return@launch
                // কিছু পড়া না গেলে আগের আচরণেই ফেরত — বোতামটা যেন কখনো "মরে" না যায়
                if (items.isEmpty()) { retry.onClick(binding.syncWarnBanner); return@launch }
                showPendingListDialog(items, retry)
            }
        }
        // 🔒🔒 B274 (02.08.2026, TK-অনুমোদিত): লাল বাক্সে **দীর্ঘ চাপ** দিলে
        // "যায়নি" (স্থায়ীভাবে ব্যর্থ) এন্ট্রিগুলো ছেড়ে দেওয়ার অপশন — শুধু তখনই
        // কাজ করে যখন সত্যিই কিছু "যায়নি" ঘরে আছে (`failedCount > 0`); সচল
        // "পাঠানো বাকি" (pending) এন্ট্রি এই পথে কখনো ছোঁয়া হয় না।
        // ⛔ ডিজাইন/লেআউটে কিছু যোগ হয়নি — শুধু আগে থেকে থাকা বাক্সে একটা
        // দীর্ঘ-চাপ আচরণ, TK নিজে জেনেশুনে চাইলে তবেই।
        binding.syncWarnBanner.setOnLongClickListener {
            val n = CloudWriteQueue.failedCount(this@DashboardActivity)
            if (n <= 0) return@setOnLongClickListener true
            AlertDialog.Builder(this@DashboardActivity)
                .setCustomTitle(PremiumAlert.header(this@DashboardActivity, NoBengali.s("Give up permanently?")))
                .setMessage(NoBengali.s("$n item(s) cannot be found on the server, so they can never be sent. They will not be shown or retried again. Records and money totals will not change."))
                .setPositiveButton(NoBengali.s("Yes, give up")) { _, _ ->
                    CloudWriteQueue.clearFailed(this@DashboardActivity)
                    refreshSyncStatus()
                    android.widget.Toast.makeText(this@DashboardActivity, NoBengali.s("$n item(s) given up"), android.widget.Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(NoBengali.s("Cancel"), null)
                .show().also { PremiumAlert.paint(it); try { NoBengali.installDialog(it) } catch (_: Throwable) { } }
            true
        }
    }

    /** 🟢🔒 V706 — "Not sent to the cloud" তালিকা। শুধু দেখায়; পাঠানোর কাজটা
     *  পুরোনো `retry` listener-ই করে। সব লেখা ইংরেজি (TK-নির্দেশ)। */
    private fun showPendingListDialog(
        items: List<PendingSyncStatus.Item>,
        retry: android.view.View.OnClickListener
    ) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(2))
        }
        for (it in items) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val left = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            left.addView(android.widget.TextView(this).apply {
                text = it.name
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#10223A"))
            })
            val sub = listOf(it.kind, it.code, it.date).filter { p -> p.isNotBlank() }.joinToString(" · ")
            left.addView(android.widget.TextView(this).apply {
                text = sub
                textSize = 11f
                setTextColor(android.graphics.Color.parseColor("#5A6B80"))
            })
            if (it.why.isNotBlank()) {
                left.addView(android.widget.TextView(this).apply {
                    text = it.why
                    textSize = 10.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    // টাকা পৌঁছে গেছে হলে হলুদ, টাকাই যায়নি হলে লাল
                    val sent = it.why.startsWith("Money sent")
                    setTextColor(android.graphics.Color.parseColor(if (sent) "#8A5A00" else "#A02A2A"))
                    setPadding(dp(7), dp(1), dp(7), dp(1))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(android.graphics.Color.parseColor(if (sent) "#FFF6E5" else "#FDECEA"))
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = dp(3)
                    layoutParams = lp
                })
            }
            row.addView(left)
            if (it.amount.isNotBlank()) {
                row.addView(android.widget.TextView(this).apply {
                    text = it.amount
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                })
            }
            box.addView(row)
            box.addView(android.view.View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(android.graphics.Color.parseColor("#EEF2F7"))
            })
        }
        val scroll = android.widget.ScrollView(this).apply { addView(box) }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "\u26A0 Not sent to the cloud - ${items.size}"))
            .setView(scroll)
            .setPositiveButton("Send All") { _, _ -> retry.onClick(binding.syncWarnBanner) }
            .setNegativeButton("Close", null)
            .show().also { dlg ->
                PremiumAlert.paint(dlg)
                try { NoBengali.installDialog(dlg) } catch (_: Throwable) { }
            }
    }

    /** Notification bell: shows unseen-notice count and today's pending-call count.
     *  🆕 (06.08.2026, TK-অনুমোদনে, খাতার সারি — "ঘন্টায় সংখ্যা আছে কিন্তু ভিতরে ফাঁকা"):
     *  আগে সরাসরি Briefing পাতা খুলত, যেখানে শুধু নোটিশ ও (Master-only)
     *  অনুমোদন দেখা যেত -- BellCounter-এর যোগ করা বাকি তিনটে (রিমার্ক বাকি,
     *  আজ ডাক্তার কল/EXPECTED, মিসড কল-ব্যাক) কোথাও দেখানোরই জায়গা ছিল না,
     *  তাই ঘন্টায় সংখ্যা থাকলেও পাতা ফাঁকা লাগত। এখন ঘন্টা চাপলে নতুন
     *  NotificationsActivity খোলে যেটা এই সবকটাই এক পাতায় দেখায়; প্রতিটা
     *  লাইনে চাপলে আসল পাতায় যায়, ব্যাক চাপলে আবার এই তালিকাতেই ফেরে। */
    private fun refreshBell(session: NativeUser) {
        binding.tvBell.setOnClickListener {
            startActivity(android.content.Intent(this, NotificationsActivity::class.java))
        }
        lifecycleScope.launch {
            // TK-REQUESTED (2026-07-27): the counting itself is unchanged, it
            // only moved into BellCounter so the background reminder can use
            // the very same number. BellNotifier then rings the phone's normal
            // notification sound when that number goes UP -- before this, a new
            // notice arrived completely silently.
            val count = withContext(kotlinx.coroutines.Dispatchers.IO) {
                BellCounter.count(this@DashboardActivity, session)
            }
            // 🎨 V1084 — সংখ্যাটা এখন ঘণ্টার পাশের লাল ব্যাজে (TK-এর নকশা)।
            //    ⛔ ০ হলে ব্যাজ লুকানো, তাই আগের মতোই শুধু "🔔" দেখা যায়।
            binding.tvBell.text = "🔔"
            binding.tvBellBadge.text = if (count > 99) "99+" else count.toString()
            binding.tvBellBadge.visibility =
                if (count > 0) android.view.View.VISIBLE else android.view.View.GONE
            BellNotifier.onCount(this@DashboardActivity, session, count)
        }
    }

    /** Shows a tile only for the given roles (matching the WebView dashboard's role list).
     *  TK APPROVED (2026-07-15): cardTopColor/cardBottomColor/iconColor give each
     *  module its own soft gradient card + coloured icon circle instead of the
     *  old identical-white-card look. Corner radii (20dp card / 14dp icon) and
     *  the light border/shadow match the original bg_dashboard_tile design --
     *  only the colours are now per-module instead of one fixed drawable. */
    private fun tile(
        tile: ItemDashboardTileBinding,
        icon: String,
        label: String,
        roles: List<String>,
        cardTopColor: String,
        cardBottomColor: String,
        iconColor: String,
        onClick: () -> Unit
    ) {
        val role = NativeSession.current(this)?.role ?: ""
        // 🔴🆕🔒 TK-নির্দেশ (07.08.2026, তালিকা দিয়ে স্পষ্ট করে) — *"Check Up ·
        // Print · Chamber Date · Payment · Dr. Visit — এই ৫টা শুধুমাত্র ডাক্তারের
        // Dashboard-এ থাকবে, বাকি সব কিছু মেনুবারে থাকবে।"*
        // 🔴🔴 UPDATE (08.08.2026) — TK এই ৫-এর মধ্যে **Dr. Visit-ও মেনুতে** পাঠাতে
        //    বলেছেন; তাই এখন Dashboard-এ শুধু ৪টা (CHECK-UP · Print · Chamber Date ·
        //    Payment), আর Dr. Visit Menu-এর Modules-এ। DOCTOR_DASHBOARD_TILES দেখুন।
        // **কীভাবে করা হলো (সবচেয়ে কম ঝুঁকির পথ):** প্রতিটা `tile(...)` ডাকের
        // role-তালিকা আলাদা করে না ঘেঁটে, এখানে **একটাই জায়গায়** সিদ্ধান্ত —
        // ডাক্তার হলে শুধু `DOCTOR_DASHBOARD_TILES`-এর নামগুলোই দেখা যায়।
        // ⛔ **অন্য কোনো role (master/staff/field) এক চুলও বদলায়নি** — তাদের
        //    জন্য আগের `role in roles` নিয়মই হুবহু চলে।
        // ⛔ "Chamber Date" টাইলের নিজস্ব তালিকায় ("master","staff") ডাক্তার ছিল
        //    না; এই নতুন নিয়মে ডাক্তার সেটাও পান — TK-এর নির্দেশ অনুযায়ীই।
        // ⛔ ডাক্তার যা হারালেন (Enquiry/Follow-up/Registration/Dialer/Draft/
        //    Briefing/Search) তার সবগুলোই Menu-তে যোগ করা হয়েছে (MoreMenuActivity,
        //    "Modules" সেকশন) — কিছুই দুর্গম হয়নি।
        // 🔴🔴 (08.08.2026, TK লাইভ টেস্টে ধরেছেন — "ডাক্তারের ড্যাশবোর্ডে এত কিছু
        // থাকবে না বলা হয়নি? ফাইল পাঠানোর পরেও কেন দেখছি?")। **আমার ভুল:** এই
        // অ্যাপে ডাক্তার/ফিল্ডের `role` ভিতরে **"staff"** করে রাখা হয়
        // (`NativeUser.permissionRole()` — সব অনুমতি-যাচাই এক রাখতে), আসল পরিচয়
        // থাকে `displayRole`-এ। আমি যাচাই না করে `role == "doctor"` লিখেছিলাম,
        // যেটা **কখনোই সত্যি হয় না** — তাই ডাক্তার আগের মতোই স্টাফের সব বাক্স
        // দেখতেন (Work Notebook-সহ)। এখন প্রজেক্টের প্রচলিত নিয়মেই `displayRole`
        // দেখা হয় (PrintCenterActivity/SecurityGuard ঠিক এভাবেই করে)।
        // ⛔ `roles`-তালিকা মেলানো আগের মতোই `role` (permission-role) দিয়েই —
        //    তাই Master/Staff/Field-এর ড্যাশবোর্ড এক চুলও বদলায়নি।
        val realRole = NativeSession.current(this)?.displayRole ?: role
        // 🔒🔒 V1380 (১২.০৯.২০২৬, TK-নির্দেশ) — ARMAN HOQUE-এর ফোনে শুধু
        // "Dr. Visit" বাক্সটাই, বাকি সব (role/roles যা-ই বলুক) লুকানো।
        // ⛔ অন্য কারো ড্যাশবোর্ড এক চুলও বদলায় না — উপরের ডাক্তারের নিয়মের
        //    (DOCTOR_DASHBOARD_TILES) হুবহু একই কম-ঝুঁকির প্যাটার্নে বসানো হলো।
        // 🔴 V1401 (১২.০৯.২০২৬ সন্ধ্যা, TK: *"শুধুমাত্র ডক্টর ভিজিটিং আর স্টাফ
        //    নোটবুক এই দুটি আসার কথা ছিল"*) — **আমার ভুল:** V1380-তে Work Notebook
        //    (IN TIME/OUT TIME) বাক্সটাও লুকিয়ে ফেলেছিলাম, অথচ TK-র নির্দেশ ছিল
        //    Dr. Visit + IN/OUT TIME (RoleRules-এর নোটেই লেখা)। এখন দুটোই আসে।
        val allowed = when {
            RoleRules.isDoctorVisitOnly(this) -> label == "Dr. Visit" || label == "Work Notebook"
            realRole == "doctor" -> label in DOCTOR_DASHBOARD_TILES
            else -> role in roles
        }
        if (allowed) {
            tile.tvIcon.text = icon
            tile.tvLabel.text = label
            tile.root.setOnClickListener { onClick() }
            tile.root.visibility = android.view.View.VISIBLE
            val density = resources.displayMetrics.density
            // TK APPROVED (2026-07-27, full-screen photo proof "Model 1"): TK
            // reported the module names looked blurry on the pastel gradient
            // cards. The card is now plain white with a hairline border and a
            // soft shadow, so the dark label sits on the highest possible
            // contrast. Icons, labels, roles, clicks, order and the grid itself
            // are all untouched -- only the card's colour/corner/shadow change.
            // cardTopColor / cardBottomColor are deliberately left in the
            // signature so that not one of the 18 call sites has to change.
            tile.root.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 10f * density
                setColor(android.graphics.Color.WHITE)
                setStroke((1 * density).toInt(), android.graphics.Color.parseColor("#E6EBF2"))
            }
            tile.root.elevation = 3f * density
            tile.tvIcon.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(android.graphics.Color.parseColor(iconColor))
            }
        } else {
            tile.root.visibility = android.view.View.GONE
        }
    }

    private fun setupTile(tile: ItemDashboardTileBinding, icon: String, label: String, onClick: () -> Unit) {
        tile.tvIcon.text = icon
        tile.tvLabel.text = label
        tile.root.setOnClickListener { onClick() }
    }

    /** ফোনে জমানো তালিকা থেকে সম্পূর্ণ নাম; না পেলে সেশনের লেখাটাই। */
    private fun welcomeFullName(user: NativeUser): String {
        val cached = try {
            CloudStaffDirectory.cachedNameFor(applicationContext, user.mobile)
        } catch (_: Throwable) { null }
        return cached?.takeIf { it.isNotBlank() } ?: user.name
    }

    /** TK-অনুমোদিত "ক": উপরে ছোট করে `Welcome,`, নিচের লাইনে বড় করে পুরো নাম।
     *  👤🔒 V1426 (১৩.০৯.২০২৬, TK: *"এক লাইনে হবে যত বড় নামই হোক না কেন"*, তালিকা ৫২৮) —
     *  নাম জায়গায় না আঁটলে (যেমন CHANDANA ROY PR…) "…" নয়: নামের অক্ষর ঠিক ততটুকু
     *  ছোট হয় যাতে পুরো নাম এক লাইনে আঁটে। ছোট নামে কিছুই বদলায় না। */
    private fun renderWelcome(fullName: String) {
        val head = "Welcome,\n"
        fun build(nameScale: Float): android.text.SpannableStringBuilder {
            val sp = android.text.SpannableStringBuilder(head + fullName)
            /* উপরের লাইন — ছোট, হালকা রং, মোটা নয় (XML-এ পুরোটা bold, তাই খোলা হয়)। */
            sp.setSpan(android.text.style.RelativeSizeSpan(11f / 14f), 0, head.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#CFE8D8")),
                0, head.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), 0, head.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            /* নিচের লাইন — নামটাই বড়, সাদা ও মোটা। */
            sp.setSpan(android.text.style.RelativeSizeSpan(nameScale), head.length, sp.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return sp
        }
        val normalScale = 15f / 14f
        binding.tvWelcome.text = build(normalScale)
        binding.tvWelcome.post {
            val avail = binding.tvWelcome.width - binding.tvWelcome.paddingLeft - binding.tvWelcome.paddingRight
            if (avail <= 0 || fullName.isBlank()) return@post
            val paint = android.text.TextPaint(binding.tvWelcome.paint)
            paint.textSize = binding.tvWelcome.textSize * normalScale
            val needed = paint.measureText(fullName)
            if (needed > avail) {
                val shrunk = (normalScale * avail / needed) * 0.97f
                binding.tvWelcome.text = build(shrunk.coerceAtLeast(8f / 14f))
            }
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Logout"))
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                NativeSession.clear(this)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    companion object {
        /** 🔴💸 V509 — হোম পর্দায় ফিরলে দুটো "সত্যিকারের গোনা"-র মধ্যে সবচেয়ে
         *  কম ফাঁক। এর ভিতরে ফিরলে জমানো সংখ্যাই দেখানো হয় (ক্লাউডে কিছু যায় না)। */
        private const val BANNER_MIN_GAP_MS = 3L * 60L * 1000L
        /** 🔴💸 V509 — দুটো সস্তা HEAD-প্রশ্নের মধ্যে সবচেয়ে কম ফাঁক।
         *  এর ভিতরে ব্যাক চাপলে ক্লাউডে একটাও প্রশ্ন যায় না। */
        private const val BANNER_PROBE_GAP_MS = 45L * 1000L
        /** 🔴🔒 V509 — এত সময় পার হলে **জোর করে** পূর্ণ গোনা, পাহারা যাই বলুক।
         *  কারণ: পাহারা শুধু `updatedAt` দেখে, তাই সত্যিকারের ডিলিট সে ধরতে
         *  পারে না; আর নেট-ব্যর্থতা/মাঝপথে পর্দা বন্ধের ক্ষেত্রেও এটা জাল।
         *  ⛔ প্রকল্পের প্রমাণিত মাপ — `FollowUpRepository`-র delta-জালের সঙ্গে
         *     **সবসময় এক** রাখতে হবে; নইলে ব্যানারের সংখ্যা আর তালিকার সংখ্যা
         *     বেমানান হয় (TK-এর "৫৬ বনাম ৪৭"-এর সমস্যা)।
         *
         *  📉🔒 V1258 (০৯.০৯.২০২৬, TK-অনুমোদিত Egress-প্লানের **ধাপ ৩**) —
         *     ৩০ মিনিট → **৩ ঘণ্টা**। এই জোর-করা গোনাটা তিনটে পূর্ণ `fetchTab()`
         *     চালায় (ব্রাঞ্চের ছাঁকনি ছাড়া) — অ্যাপের অন্যতম ভারী পড়া। ব্যবধান
         *     ৬ গুণ হলে ওই খরচ ৬ ভাগের এক।
         *  ⛔ সস্তা HEAD-পাহারা (`bannerWatch.changed`) আগের মতোই প্রতিবার চলে —
         *     তাই নতুন/বদলানো সারি ব্যানারে আগের মতোই দ্রুত ধরা পড়ে; শুধু
         *     **সত্যিকারের ডিলিট** সরতে সর্বোচ্চ ৩০ মিনিটের বদলে ৩ ঘণ্টা লাগতে
         *     পারে (TK-কে কাজের আগেই বলা হয়েছে)।
         *  ⛔ `FollowUpRepository.FU_FULL_REFRESH_INTERVAL_MS`-ও একই টার্নে ৩ ঘণ্টা। */
        private const val BANNER_FULL_GAP_MS = 3L * 60L * 60L * 1000L
        /** 🔴💸 V509 — ব্যানারের পাহারা ও ঘড়িগুলো **অ্যাপের**, পর্দার নয়:
         *  ফোন ঘোরালে বা পর্দা নতুন করে তৈরি হলেও ফাঁকটা মুছে যায় না।
         *  ⛔ চাবিতে মোবাইল নম্বর + ব্রাঞ্চ দুটোই আছে, তাই অন্য কেউ লগইন করলে
         *     আগের হিসাব কখনো ব্যবহার হয় না (নতুন করে পূর্ণ গোনা হয়)।
         *  ⛔ শুধু "কখন আবার গুনব" — কোনো তথ্য/টাকার হিসাব এখানে জমে না। */
        private val bannerWatch = LiveRefresh.Watch("followups", "enquiries", "patients", "payments")
        private var bannerCloudDone = false
        private var bannerKey = ""
        private var bannerCloudAt = 0L
        private var bannerProbeAt = 0L
        /** 🔴🆕🔒 TK-নির্দেশ (07.08.2026) — ডাক্তারের Dashboard-এ **শুধু এই কয়টা**
         *  বাক্স থাকবে; বাকি সব Menu-তে। নামগুলো `tile(...)` ডাকের `label`-এর
         *  সাথে **হুবহু** মিলতে হবে (তাই এখানে ঠিক সেই বানানই রাখা হলো)।
         *
         *  🔴🔴 UPDATE (08.08.2026, TK লাইভ টেস্টে ছবি+তালিকা দিয়ে) — *"এই চারটা
         *  পাশাপাশি থাকবে: CHECK-UP, Print / Chamber Date, Payment। বাকি Dr. Visit
         *  মেনু বারে থাকবে।"* তাই "Dr. Visit" এই তালিকা থেকে বাদ (এখন ৪টা), আর
         *  Dr. Visit ডাক্তারের Menu-এর "Modules" সেকশনে যোগ করা হলো
         *  (MoreMenuActivity → btnDocDoctorVisit)। ⛔ পুরনো ৫-টার লাইন মোছা হয়নি,
         *  উপরে ইতিহাস রাখা হলো; শুধু কার্যকরী তালিকা ৪টায় নামানো হলো। */
        val DOCTOR_DASHBOARD_TILES = setOf(
            "CHECK-UP",       // Check Up  (উপরে-বাঁয়ে)
            "Print",          //           (উপরে-ডানে)
            "Chamber Date",   //           (নিচে-বাঁয়ে)
            "Payment",        //           (নিচে-ডানে)
            // 🔔 V1186 (TK-নির্দেশ): *"এটা প্রত্যেকের হোম স্ক্রিনে ই থাকবে"* —
            //    তাই ডাক্তারের ছোট তালিকাতেও এটা যোগ হলো।
            "Doctor Reminder"
            // "Dr. Visit" — 08.08.2026-এ সরানো, এখন Menu-তে (উপরের নোট দেখুন)
        )
    }
}
