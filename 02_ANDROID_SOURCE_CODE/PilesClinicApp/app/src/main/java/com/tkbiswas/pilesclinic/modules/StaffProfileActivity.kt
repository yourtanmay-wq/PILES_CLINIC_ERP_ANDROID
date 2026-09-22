/* =====================================================================
   V245 — MODULE 1 : PROFESSIONAL PROFILE & SALARY.
   Master: view/edit every profile + salary + record payments.
   Staff/Doctor/Field: view ONLY their own profile + salary history.
   Data in schema `hr` (RLS). Numbers shown masked. English UI.
   ===================================================================== */
package com.tkbiswas.pilesclinic.modules

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.ImageView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.native.HourSalary   // ⏱️ V1166
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.PhotoUtils
import com.tkbiswas.pilesclinic.native.RmpCallingAccessRepository
import com.tkbiswas.pilesclinic.native.TripleTapEdit
import com.tkbiswas.pilesclinic.native.UserPhotoStore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
// 🅰️ V1057 — কার্ডের ছয়টা আইকনের জন্য (নিয়ম ৯.৩৮ মেনে খালি লাইনে)
import com.tkbiswas.pilesclinic.R

class StaffProfileActivity : AppCompatActivity() {

    // 🔴 বাগ-ফিক্স (02.08.2026, TK-রিপোর্ট Income & Expense-এ, একই কারণ এখানেও
    // ছিল বলে খুঁজে ঠিক করা হলো): সিস্টেম Back আগে সরাসরি হোমে চলে যেত।
    private var backAction: () -> Unit = { finish() }

    /**
     * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — *"Calls From App"* পর্দা):
     * *"এখান থেকে ব্যাক বাটনে কাজ করে না। মোবাইলের ব্যাক বাটন দিয়ে চাপ দিলেও
     * কোনো কাজ হয় না।"*
     *
     * ─── আসল কারণ (কোড ধরে বার করা, আন্দাজ নয়) ────────────────────────────
     * `perfListScreen()` আগে লিখত:
     *      val prevBack = backAction
     *      backAction = { prevBack() }
     * অর্থাৎ **তখন যা-ই `backAction` থাকুক, সেটাই** ধরে নিত। সাধারণ পথে ঠিকই
     * চলত, কিন্তু এই পথে ভেঙে যেত —
     *   ১. Performance → "Calls from app" → তালিকা খুলল (Back = Performance ✅)
     *   ২. তালিকার একটা সারিতে চাপ → Detail খুলল। `perfDetailScreen` বসাল
     *      `backAction = { perfShowCallsList(...) }` (অর্থাৎ "তালিকায় ফেরো")।
     *   ৩. Detail থেকে Back → তালিকা আবার আঁকা হলো। কিন্তু এইবার
     *      `prevBack` = **"তালিকায় ফেরো"**, তাই `backAction` দাঁড়াল
     *      "তালিকায় ফেরো" — অর্থাৎ Back চাপলে **একই তালিকাই আবার আঁকে**।
     *   ⇒ পর্দায় কিচ্ছু বদলায় না। TK-এর দেখা "ব্যাক কাজ করে না" ঠিক এটাই।
     *      (সিস্টেম Back-ও একই `backAction()` ডাকে, তাই সেটাও আটকে যেত।)
     *
     * ─── সমাধান ───────────────────────────────────────────────────────────
     * তালিকা-পর্দার ফেরার ঠিকানা আর "তখন যা ছিল" থেকে আন্দাজ করা হয় না —
     * `performanceOne()` নিজে একবার এখানে লিখে রাখে "আমার কাছে ফিরবে", আর
     * `perfListScreen()` সেটাই ব্যবহার করে। ডিটেল থেকে যতবারই ফিরুক, ঠিকানাটা
     * বদলায় না, তাই লুপ তৈরি হতে পারে না।
     * ⛔ কোনো তথ্য/হিসাব/ডিজাইন ছোঁয়া হয়নি — শুধু "Back চাপলে কোথায় যাব"।
     */
    private var perfListBack: (() -> Unit)? = null

    /* 🎨 V417গ (মডেল ৩): স্যালারি পর্দার আলাদা দুটো কার্ড চিনে রাখার নাম।
       ⛔ পর্দা আবার আঁকা হলে পুরনোটা এই নাম ধরে সরানো হয়, তাই কার্ড জমে না। */
    private val SAL_TAG_EXTRA = "salExtraCard"
    private val SAL_TAG_CFG = "salCfgCard"
    override fun onBackPressed() { backAction() }

    /* ═══════════════════════════════════════════════════════════════════════
       🔧 V486 (20.08.2026, TK-রিপোর্ট): *"যে স্টাফের Performance খুললাম, Back
       করলে আবার সেখানেই আসার কথা — কিন্তু পুরো উপরে চলে যাচ্ছে"*।

       আসল কারণ: এই পর্দাগুলো আলাদা Activity নয় — একই Activity বারবার নতুন
       করে আঁকা হয় (renderList / performanceOne …)। Back মানে renderList()
       আবার ডাকা, অর্থাৎ **সম্পূর্ণ নতুন ScrollView** — আর নতুন ScrollView
       সবসময় একদম উপর থেকে শুরু হয়। তাই তালিকার নিচে থাকা স্টাফ দেখতে হলে
       প্রতিবার আবার নিচে নামতে হত।

       সমাধান: তালিকায় থাকা অবস্থায় কতটা নিচে নামা হয়েছে সেটা মনে রাখা হয়,
       আর তালিকা আবার আঁকা হলে ঠিক সেই জায়গাতেই ফিরিয়ে দেওয়া হয়।
       ⛔ কোনো হিসাব · ডেটা · ডিজাইন · বোতাম কিছুই বদলায়নি — শুধু তালিকা
          কোথায় দাঁড়াবে সেটুকু।
       ═══════════════════════════════════════════════════════════════════ */
    private var listScrollY = 0
    private var listScroll: android.widget.ScrollView? = null
    private var trackListScroll = true

    /** তালিকা আঁকা শেষে — মনে রাখা জায়গায় ফিরিয়ে দাও। */
    private fun restoreListScroll() {
        val sv = listScroll ?: return
        val want = listScrollY
        if (want <= 0) { trackListScroll = true; return }
        // আঁকার সময় উচ্চতা ক্ষণিকের জন্য ০ হয়ে যায় — তখন ScrollView নিজে
        // ০-তে নেমে আসে। সেই ভুল মানটা যেন মনে না থাকে, তাই মাপা বন্ধ রাখি।
        trackListScroll = false
        sv.post {
            sv.scrollTo(0, want)
            sv.post { trackListScroll = true }
        }
    }

    // ---------- Photo (V252, TK-অনুরোধে) ----------
    // patients.photo-এর হুবহু একই প্রমাণিত পথ (PhotoUtils) — নতুন কোনো Storage
    // bucket/জটিলতা যোগ হয়নি। "Change Photo"-তে ৩-ট্যাপ লক (TripleTapEdit, ঠিক
    // ব্রাঞ্চ-লকের মতোই) — ভুল করে চাপলে ছবি বদলাবে না।
    private var pendingPhotoDataUrl: String? = null   // নতুন বাছা ছবি (এখনো সেভ হয়নি)
    private var currentPhotoDataUrl: String? = null   // আগে থেকে সেভ করা ছবি
    private var photoPreview: ImageView? = null
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val dataUrl = PhotoUtils.encodeResized(this, uri)
            if (dataUrl != null) {
                pendingPhotoDataUrl = dataUrl
                photoPreview?.setImageBitmap(PhotoUtils.decodeDataUrl(dataUrl))
                ModuleUi.toast(this, "Photo picked — press Save to keep it")
            } else ModuleUi.toast(this, "Could not read image")
        }
    }

    /** 🗓️ V1248 — "2026-08-28" → "28/08/2026"। চেনা না গেলে যা আছে তাই। */
    private fun salDmy(iso: String): String = try {
        val p = iso.take(10).split("-"); p[2] + "/" + p[1] + "/" + p[0]
    } catch (_: Throwable) { iso }

    private fun todayIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US); f.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }
    /* 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬: "₹2,10,850 — ভারতীয় ভাগ") — এই একটা
       জায়গায় `Locale.US` বসানো ছিল, তাই ২,১০,৮৫০-এর বদলে ২১০,৮৫০ দেখাত —
       অ্যাপের বাকি সব পর্দার (ও কম্পিউটারের) সঙ্গে মিলত না। এখন ভারতীয়
       ভাগেই দেখাবে। ⛔ অঙ্ক একটুও বদলায়নি, শুধু কমা বসার জায়গা। */
    private fun money(n: Double): String = "₹" + com.tkbiswas.pilesclinic.native.MoneyFormat.inr(n)

    // 🔴 বাগ-ফিক্স (02.08.2026): Android-এর org.json-এ কোনো ঘর ডেটাবেসে SQL NULL হলে
    // optString(key) আসলে খালি "" ফেরত দেয় না — সাক্ষাৎ শব্দ "null" ফেরত দেয় (Android-এর
    // JSON.toString(JSONObject.NULL) == "null")। তাই সব জায়গায় সরাসরি optString() না ডেকে
    // এই ns() ব্যবহার করা হচ্ছে, যেটা "null" শব্দটাকেও খালি ধরে।
    private fun ns(o: JSONObject, key: String): String {
        val v = o.optString(key)
        return if (v.isBlank() || v == "null") "" else v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = NativeSession.current(this)?.name ?: ""
        // 🟢 B629 (11.08.2026): "Salary Due" reminder থেকে সরাসরি এই স্টাফের Salary পর্দা
        //   খোলার জন্য (শুধু Master; Master ছাড়া কেউ অন্যের Salary খুলতে পারে না)।
        val salaryFor = intent.getStringExtra("salaryFor")
        /* 🔵🔒 V523 (২২.০৮.২০২৬, TK-নির্দেশ): Reports-এর Staff-wise অংশ থেকে
           সরাসরি **পুরো** Staff Performance পর্দায় আসার পথ — TK-কে আর খুঁজে
           বেড়াতে হবে না, আর দুই জায়গার সংখ্যা নিয়ে বিভ্রান্তিও থাকবে না।
           ⛔ `salaryFor`-এর হুবহু একই প্যাটার্ন। ⛔ শুধু Master (নিচের শর্তেই)।
           ⛔ ঘরটা না এলে (পুরোনো সব ডাক) আচরণ অবিকল আগের মতোই। */
        val openPerf = intent.getBooleanExtra("openPerformance", false)
        ModuleUi.ensureSignedIn(this, code) {
            if (!salaryFor.isNullOrBlank() && ModuleAuth.isMaster) salary(salaryFor)
            else if (openPerf && ModuleAuth.isMaster) performanceList("")
            else if (ModuleAuth.isMaster) renderList() else renderSelf()
        }
    }

    // ---------- MASTER: list all ----------
    private fun renderList() {
        backAction = { finish() }
        /* 🎨🔒 V764 (২৭.০৮.২০২৬, TK-অনুমোদিত **ডিজাইন A**, ডেমো ফটো দেখে বাছা)
           TK: *"Add staff & Doctor উপরে হেডারে রাখুন, একটা icon সহ"*।
           ⛔ `ModuleUi.screen()`-কে **খালি শিরোনাম** দেওয়া হলো, কারণ ওটা নিজে
              শুধু একটা লেখা বসায় — পাশে বোতাম বসানোর জায়গা নেই। শিরোনাম ও
              গোল ➕ বোতাম এখানে নিজেই এক সারিতে বসানো হলো।
           ⛔ `ModuleUi.screen()` **ছোঁয়া হয়নি** — নইলে প্রজেক্টের অন্য সব
              Module-পর্দার শিরোনাম বদলে যেত। */
        val root = ModuleUi.screen(this, "")
        run {
            val d = resources.displayMetrics.density
            val head = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, (10 * d).toInt())
            }
            head.addView(ModuleUi.heading(this, "🧑\u200d💼 Staff Profiles").apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            // ⋮ শুধু মাস্টার দেখবেন — সার্ভারেও একই পাহারা আছে।
            /* 📋🔒 V1321 (TK-নির্দেশ ১০.০৯.২০২৬, ডেমো-প্রুফ পাশ) — আলাদা 📱
               ও ➕ গোল বোতাম দুটো, আর নিচের "🏆 Staff Performance" বোতাম —
               তিনটেই এখন এই একটা ⋮ মেনুর ভিতরে। TK: *"এগুলি উপরে ডান সাইড
               3 Dot এর মধ্যে থাকবে"*।
               ⛔ কাজ/পাহারা/অ্যাকশন তিনটেই আগের মতোই (phoneVersionsScreen,
                  addPersonDialog, performanceList) — শুধু বসার জায়গা বদলাল। */
            if (ModuleAuth.isMaster) {
                head.addView(android.widget.TextView(this).apply {
                    text = "\u22EE"
                    textSize = 20f
                    setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0B7A3E"))
                    gravity = android.view.Gravity.CENTER
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 10f * d
                        setColor(android.graphics.Color.WHITE)
                        setStroke((1 * d).toInt(), android.graphics.Color.parseColor("#CFE3D8"))
                    }
                    elevation = 3f * d
                    isClickable = true; isFocusable = true
                    contentDescription = "More"
                    setOnClickListener { v ->
                        val items: List<Pair<String, () -> Unit>> = listOf(
                            "\uD83C\uDFC6 Staff Performance" to ({ performanceList("") }),
                            "\uD83D\uDCF1 Mobile Version" to ({ phoneVersionsScreen() }),
                            "\u2795 Add Staff / Doctor" to ({ addPersonDialog() })
                        )
                        try {
                            val pm = android.widget.PopupMenu(this@StaffProfileActivity, v)
                            items.forEachIndexed { i, (label, _) -> pm.menu.add(0, i, i, label) }
                            pm.setOnMenuItemClickListener { mi ->
                                items.getOrNull(mi.itemId)?.second?.invoke(); true
                            }
                            pm.show()
                        } catch (_: Throwable) { }
                    }
                    layoutParams = LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt())
                })
            }
            root.addView(head)
        }
        // 🔧 V486: এই তালিকার ScrollView চিনে রাখি + কতটা নিচে নামা হচ্ছে মাপি।
        listScroll = (root.parent as? android.widget.ScrollView)?.also { sv ->
            sv.setOnScrollChangeListener { _, _, y, _, _ -> if (trackListScroll) listScrollY = y }
        }
        // 🔴 B315 (03.08.2026, TK-নির্দেশ): আগে "Loading..."-এর জন্য আলাদা `box`
        // কার্ড ছিল, কিন্তু আসল স্টাফ-কার্ডগুলো সরাসরি `root`-এ যোগ হতো — তাই
        // লোড হওয়ার পর `box` খালি (অপ্রয়োজনীয়) কার্ড হয়ে থেকে যেত, আর Back
        // বোতাম (তখন box-এর ঠিক পরেই যোগ করা হতো) তালিকার মাঝে/আগে দেখাত,
        // শেষে না। এখন একটাই `listBox` কনটেইনার — শুরুতে "Loading...", পরে
        // ঠিক ওই একই জায়গায় আসল কার্ড বা "No profiles." বসে — কোনো খালি বক্স
        // থাকে না। Back বোতাম `listBox`-এর পরে (তাই সবসময় সম্পূর্ণ তালিকার
        // নিচে) একবারই যোগ করা হয়, তালিকা পরে আপডেট হলেও Back-এর অবস্থান
        // বদলায় না (ভেতরের কনটেন্ট বদলায়, `listBox`-এর নিজের অবস্থান না)।
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // 🔴🔒 B505 (06.08.2026, TK-নির্দেশ, সাবধানে করার শর্তে) — এই ফোনে
        // জমানো শেষ সফল স্টাফ-তালিকা থাকলে সাথে সাথেই দেখানো হয় (Loading...
        // এর বদলে), তারপর ক্লাউড থেকে হালনাগাদ তালিকা এলে বদলে যায়। ⛔
        // তালিকা-বাছাই/বেতনের হিসাব-নিয়ম একটুও বদলায়নি — একই রেন্ডার-
        // ফাংশন (renderStaffList) দুইবার (প্রথমে ক্যাশ, পরে আসল) ডাকা হয়।
        /* 🏆🔒 V419 (TK-নির্দেশ, ১৭.০৮.২০২৬): সবার পারফরম্যান্স এক পর্দায় —
           তালিকার উপরে একটাই বোতাম। ⛔ শুধু Master দেখতে পাবেন। */
        if (ModuleAuth.isMaster) {
            // ⛔ V1321 — বোতামটা এখন উপরের ⋮ মেনুতে ("🏆 Staff Performance"), তাই এখানে আর নেই।
            /* 📱🔒 V813 (২৮.০৮.২০২৬, TK-নির্দেশ ও অনুমোদিত ডেমো-প্রুফ:
               *"phone Version আলাদা থাকবে না … স্টাফের পারফরম্যান্স যেখানে,
               তার পাশে একটুখানি অপশন থাকলেই তো চলে"*) —
               V771-এর আলাদা "📱 Phone Versions" **বোতাম ও পর্দা দুটোই উঠে গেল**।
               কোন ফোনে কোন ভার্সন চলছে সেটা এখন **Staff Performance-এর ভিতরেই**
               প্রত্যেকের নামের নিচে ছোট ট্যাগ হয়ে দেখায় (performanceList)।
               ⛔ কোনো তথ্য · পাহারা · SQL কিছুই বদলায়নি — একই `hr.app_devices_list`,
                  একই মাস্টার-যাচাই; শুধু দেখার জায়গাটা বদলাল। */
            /* 👥🔒 V746 (২৭.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) —
               TK: *"আপনি তো আর আমার সাথে সারা জীবন থাকবেন না... আমি
               অ্যাপ্লিকেশন থেকে কোন স্টাফ যোগ বা বিয়োগ করতে পারব কিনা।"*
               ⛔ শুধু মাস্টার — সার্ভারেও একই পাহারা, তাই ফোন থেকে ফাঁকি নেই।
               ⛔ পুরনো তালিকা/বেতন/পারফরম্যান্স — কিচ্ছু ছোঁয়া হয়নি,
                  শুধু একটা নতুন বোতাম যোগ। */
            /* 🧹🔒 V761 (২৭.০৮.২০২৬, TK: *"Remove করার অপশন আগেই ছিল, আবার কেন
   ডুপ্লিকেট বানালেন"*) — TK সম্পূর্ণ ঠিক বলেছেন।
   এই পর্দার **প্রতিটা কার্ডেই** আগে থেকে Remove · Restore · Suspend আছে
   (V404/V603, ডাক্তার ও ফিল্ড-অফিসার সহ সবার জন্য), আর নিচে আলাদা
   "Removed" ভাগও আছে। V746-এ আমি ভুল করে ওই একই তালিকা আবার বানিয়ে
   ফেলেছিলাম — সেটা মুছে দেওয়া হলো।
   ⇒ সত্যিই যেটা ছিল না তা হলো **যোগ করা**। তাই বোতামটা এখন সরাসরি
     যোগ করার ঘরটাই খোলে — বাড়তি কোনো পর্দা নেই। */
            // ⛔ V764 — বোতামটা এখন উপরে হেডারে (গোল ➕), তাই এখানে আর নেই।
        }
        val cachedNow = loadCachedStaffList()
        if (cachedNow != null) {
            // 🔴 V509 নিজের যাচাইয়ে ধরা পড়েছে (২১.০৮.২০২৬): নিচের ক্লাউড-আঁকায়
            // ঝিলিক-পাহারা বসানো হয়েছিল, কিন্তু **ক্যাশের এই প্রথম আঁকাটা
            // পাহারার ভিতর দিয়ে যেত না** — তাই পাহারার কাছে "আগে কী আঁকা
            // হয়েছিল" তথ্যটাই থাকত না, আর সে কখনোই কাজ করত না (এই পর্দায়
            // ঝিলিক আগের মতোই থাকত)। এখানে একবার ডেকে নেওয়ায় চিহ্নটা জমা
            // থাকে। ⛔ ঘর ফাঁকা বলে এটা কখনো আঁকা আটকায় না — শুধু মনে রাখে।
            com.tkbiswas.pilesclinic.native.RedrawGuard.alreadyShowing(
                listBox, cachedNow.first.toString() + "|" + cachedNow.second.toString())
            renderStaffList(listBox, cachedNow.first, cachedNow.second)
        } else listBox.addView(ModuleUi.body(this, "Loading..."))
        root.addView(listBox)
        root.addView(ModuleUi.button(this, "Back") { finish() })
        restoreListScroll()   // 🔧 V486: আগের জায়গায় ফিরে যাও (নতুন হলে ০ = উপরে)
        Thread {
            // 🔴 বাগ-ফিক্স (02.08.2026, TK-রিপোর্ট Supabase খরচ বেশি দেখে ধরা পড়েছে):
            // এই তালিকায় ছবি দেখানোই হয় না (শুধু Edit-এ একজনের ছবি দেখা যায়), অথচ
            // আগে select=* দিয়ে **সবার ছবিসহ (photo_data, ৫০-১০০ KB প্রতিটা)** পুরো
            // রেকর্ড টানা হতো — তালিকা একবার খুললেই অকারণে অনেক ডেটা খরচ হতো।
            // এখন শুধু তালিকায় সত্যিই যা দেখানো হয় সেই কলামগুলোই টানা হচ্ছে।
            // 🔵 TK-ORDER (07.08.2026): getRowsChecked — পড়া ব্যর্থ ও "সত্যিই খালি"
            // আলাদা। আগে getRows ব্যর্থে খালি ফেরাত → saveCachedStaffList(খালি) ভালো
            // cache মুছত → Master পরের বারও "No profiles." দেখত। এখন ব্যর্থ হলে
            // cache/তালিকা ছোঁব না। ⛔ একই দুটো cloud-read (free-plan-এ বাড়তি নয়)।
            // 🔁 পুরনো: ModuleAuth.getRows("hr","staff_profiles",…) ও ("hr","salary_config","select=*")
            val rowsR = ModuleAuth.getRowsChecked(
                "hr", "staff_profiles",
                // 🔴 V404 (16.08.2026): `active` ঘরটা যোগ করা হলো — বাদ-দেওয়া কর্মী
                //    আলাদা করতে। ⛔ পুরনো cache-এ ঘরটা নেই; optBoolean("active", true)
                //    ⇒ সচল ধরা হয়, তাই পুরনো cache-এ কেউ হঠাৎ উধাও হবে না।
                // 🔴 V1510 (TK-রিপোর্ট ১৬.০৯.২০২৬ — "সাসপেন্ড করলেও কাজ হচ্ছে না
                //    বোঝার উপায় নেই") — `suspended_until` যোগ করা হলো, শুধু কার্ডে
                //    বর্তমান অবস্থা দেখানোর জন্য (একটা বাড়তি টেক্সট ঘর, egress নগণ্য)।
                "select=person_code,designation,role_kind,branch,full_name,link_mobile,active,suspended_until&order=person_code"
            )
            val cfgR = ModuleAuth.getRowsChecked("hr", "salary_config", "select=*")
            /* 🟣🔒 V961 (০১.০৯.২০২৬, TK-নির্দেশ) — TK: *"এখানে extra income নেই"*।
               সত্যিই ছিল না — কার্ডে শুধু Salary লেখা হত। এখন প্রতিটা কার্ডে
               `Extra: ₹… paid · ₹… due` বসে।
               ⛔ **একজন-একজন করে নয় — গোটা তালিকার জন্য একটাই পড়া**, আর মাত্র
                  তিনটে ঘর (`person_code,kind,amount,status`)। Free Plan-এ
                  Egress-এ প্রভাব নগণ্য।
               ⛔ ব্যর্থ হলে ম্যাপ ফাঁকা থাকে ⇒ লাইনটা বসে না, কার্ড হুবহু আগের
                  মতোই — একটাও নাম/তথ্য হারায় না। */
            try {
                val exR = ModuleAuth.getRowsChecked(
                    "hr", "salary_payments", "select=person_code,kind,amount,status&kind=eq.EXTRA")
                if (exR.ok) {
                    val paidM = HashMap<String, Double>(); val dueM = HashMap<String, Double>()
                    for (i in 0 until exR.rows.length()) {
                        val r = exR.rows.optJSONObject(i) ?: continue
                        val c = ns(r, "person_code").trim()
                        if (c.isBlank()) continue
                        val a = r.optDouble("amount", 0.0)
                        if (payStatus(r) == "DUE") dueM[c] = (dueM[c] ?: 0.0) + a
                        else paidM[c] = (paidM[c] ?: 0.0) + a
                    }
                    extraPaidByStaff = paidM; extraDueByStaff = dueM
                }
            } catch (_: Throwable) { }
            /* 📱🔒 V822 — কার সাথে কোন ভার্সন, সেটাও একই থ্রেডে আনা হয়।
               ⛔ একটাই ছোট RPC (~৩০ সারি) — Egress-এ প্রভাব নগণ্য।
               ⛔ ব্যর্থ হলে ম্যাপ ফাঁকা থাকে ⇒ কোনো ট্যাগ বসে না, আর
                  তালিকাটা আগের মতোই পুরোপুরি চলে (একটাও নাম হারায় না)। */
            try {
                val vr = ModuleAuth.rpc("hr", "app_devices_list", JSONObject())
                if (vr.ok) {
                    val va = JSONArray(vr.body)
                    val m = HashMap<String, Int>()
                    for (i in 0 until va.length()) {
                        val o = va.optJSONObject(i) ?: continue
                        val code = ns(o, "person_code").trim().uppercase(Locale.US)
                        if (code.isNotBlank()) m[code] = o.optInt("app_version_code", 0)
                    }
                    verListMap = m
                    verListLatest = maxOf(
                        com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE,
                        com.tkbiswas.pilesclinic.native.AppVersionCheck.newerVersionOrZero(this)
                    )
                }
            } catch (_: Throwable) { }
            if (!rowsR.ok || !cfgR.ok) {
                runOnUiThread {
                    if (cachedNow == null) {
                        listBox.removeAllViews()
                        listBox.addView(ModuleUi.body(this, "Could not load. Please try again."))
                    }
                }
                return@Thread   // ব্যর্থ পড়া — ভালো cache/তালিকা অক্ষত
            }
            /* ⛔🔒 V890 (৩০.০৮.২০২৬, TK-নির্দেশ) — বাদ দেওয়া স্টাফের একটাও
               তথ্য কোথাও দেখাবে না। এই **একটাই জায়গায়** ছেঁকে দেওয়া হয়, তাই
               স্টাফ-তালিকা · Performance · Phone Versions · বেতন — সব পর্দাতেই
               একসাথে বাদ পড়ে, কোনো জায়গা ভুলে বাদ যায় না।
               তালিকা: `BlockedStaff`। ⛔ রোগীর তথ্য/টাকা কিছুই মোছে না। */
            val rows = com.tkbiswas.pilesclinic.native.BlockedStaff.let { blk ->
                val keep = JSONArray()
                for (i in 0 until rowsR.rows.length()) {
                    val r = rowsR.rows.optJSONObject(i) ?: continue
                    if (blk.isBlocked(ns(r, "link_mobile"), ns(r, "person_code"))) continue
                    keep.put(r)
                }
                keep
            }
            val cfg = cfgR.rows
            saveCachedStaffList(rows, cfg)
            val cfgMap = HashMap<String, JSONObject>()
            for (i in 0 until cfg.length()) cfgMap[cfg.getJSONObject(i).optString("person_code")] = cfg.getJSONObject(i)
            runOnUiThread {
                /* 🔴🔒 V509 (TK-রিপোর্ট ২১.০৮.২০২৬ — "স্ক্রিন কম্পন দিচ্ছে"):
                   এই পর্দা আগে **দুবার** আঁকত — প্রথমে ফোনে জমানো তালিকা, তারপর
                   ক্লাউড থেকে এসে পুরোটা মুছে আবার। বেশিরভাগ সময় দুটো হুবহু
                   এক, তাই দ্বিতীয়বার মুছে-আঁকাটাই চোখে **ঝিলিক** লাগত।
                   এখন হুবহু এক হলে আর আঁকা হয় না।
                   ⛔ এক চুল আলাদা হলেই আগের মতোই পুরো আঁকে — কিছু চাপা পড়ে না।
                   ⛔ ক্লাউড-কল · cache সেভ · তালিকার নিয়ম — কিছুই বদলায়নি। */
                if (!com.tkbiswas.pilesclinic.native.RedrawGuard.alreadyShowing(
                        listBox, rows.toString() + "|" + cfg.toString())) {
                    trackListScroll = false   // 🔧 V486: আঁকার সময়ের ভুল মান মনে রেখো না
                    listBox.removeAllViews()
                    renderStaffList(listBox, rows, cfg)
                    restoreListScroll()       // 🔧 V486: ক্লাউড থেকে আসার পরেও একই জায়গা
                }
            }
        }.start()
    }

    private fun staffListCachePrefs() = getSharedPreferences("staff_profile_cache", MODE_PRIVATE)
    private fun loadCachedStaffList(): Pair<JSONArray, JSONArray>? {
        return try {
            val p = staffListCachePrefs()
            val rowsJson = p.getString("rows", null) ?: return null
            val cfgJson = p.getString("cfg", null) ?: return null
            Pair(JSONArray(rowsJson), JSONArray(cfgJson))
        } catch (_: Throwable) { null }
    }
    private fun saveCachedStaffList(rows: JSONArray, cfg: JSONArray) {
        try { staffListCachePrefs().edit().putString("rows", rows.toString()).putString("cfg", cfg.toString()).apply() } catch (_: Throwable) { }
    }

    private fun renderStaffList(listBox: LinearLayout, rows: JSONArray, cfg: JSONArray) {
        val cfgMap = HashMap<String, JSONObject>()
        for (i in 0 until cfg.length()) cfgMap[cfg.getJSONObject(i).optString("person_code")] = cfg.getJSONObject(i)
        // V308-এ এই চার Doctor-এর login identity তৈরি হয়েছিল, কিন্তু
        // staff_profiles সারি তৈরি হয়নি। তাই তালিকা থেকে অদৃশ্য ছিলেন। live
        // directory-এর একই code/mobile/name/branch দিয়ে শুধু অনুপস্থিত চারজনকে
        // তালিকায় পূরণ করা হয়; DB-তে থাকা কাউকে কখনো overwrite করা হয় না।
        val allRows = ArrayList<JSONObject>()
        val presentCodes = HashSet<String>()
        for (i in 0 until rows.length()) {
            val p = rows.getJSONObject(i); allRows.add(p)
            presentCodes.add(ns(p, "person_code").uppercase(Locale.US))
        }
        configuredMissingDoctors().forEach { p ->
            if (!presentCodes.contains(ns(p, "person_code").uppercase(Locale.US))) allRows.add(p)
        }

        val activeStaff = ArrayList<JSONObject>()
        val activeDoctors = ArrayList<JSONObject>()
        val activeField = ArrayList<JSONObject>()
        // 🔴🔒 V1517 (17.09.2026, TK-নির্দেশ) — Permanent Remove মানে profile
        // আর Staff Profile-এ কোথাও দেখাবে না এবং app থেকে Restore-এর পথ থাকবে না।
        // ⛔ database row delete করা হচ্ছে না: `active=false`-ই login বন্ধ রাখে,
        //    আর পুরনো Attendance/Salary/Extra Income/history অক্ষত থাকে।
        for (p in allRows) {
            val roleKind = ns(p, "role_kind")
            // পুরনো cache হলেও active=false থাকলে এখানেই বাদ — তাই cloud refresh-এর
            // আগেও Removed card এক ঝলক দেখা যাবে না।
            if (!p.optBoolean("active", true) && (
                    roleKind.equals("staff", true) || roleKind.equals("doctor", true) || roleKind.equals("field", true))) {
                continue
            }
            // 🟢🔒 V603 (২৪.০৮.২০২৬, TK-স্পষ্ট নির্দেশ, ছবি-প্রুফ পাশ) —
            // B306 (03.08.2026)-এ ইচ্ছে করে এই তালিকা শুধু staff-এ সীমিত
            // করা হয়েছিল। TK এখন বলেছেন: *"সবাইকে তো সেখানে দেখায় না...
            // যে কোনো ব্যক্তির ফোনে অ্যাপ চলবে না সেরকম ব্যবস্থা"* — অর্থাৎ
            // Doctor/Field-কেও Suspend করার সুযোগ দরকার। ⇒ B306-এর সিদ্ধান্ত
            // আংশিক উল্টে staff-এর সাথে doctor ও field-ও এখন এই তালিকায়
            // আসবে (backend-এর suspend-যাচাই আগে থেকেই সবার জন্য কাজ করে —
            // SessionGuard.kt-এ শুধু Master ছাড়া বাকি সবাইকে ধরে, তাই এটা
            // নতুন কোনো ঝুঁকি নয়, শুধু আগে থেকে-কাজ-করা জিনিসটা এখানে
            // দেখানো হলো)।
            // ⛔ Master কখনো এখানে আসবে না/suspend হবে না — RoleRules ও
            //    SessionGuard দুটোতেই আগে থেকেই আটকানো, এখানেও বাদ রাখা হলো।
            val visibleRole = roleKind.equals("staff", true) ||
                roleKind.equals("doctor", true) || roleKind.equals("field", true)
            if (!visibleRole) continue
            when {
                roleKind.equals("staff", true) -> activeStaff.add(p)
                roleKind.equals("doctor", true) -> activeDoctors.add(p)
                else -> activeField.add(p)
            }
        }

        fun heading(label: String, top: Int = 14) = TextView(this).apply {
            text = label; textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setPadding(dp(4), dp(top), dp(4), dp(6))
        }
        fun addCard(p: JSONObject, removed: Boolean = false) {
            val pc = ns(p, "person_code")
            val sc = cfgMap[pc]
            val directory = com.tkbiswas.pilesclinic.native.StaffDirectory.findAccount(ns(p, "link_mobile"))
            val roleKind = ns(p, "role_kind")
            val desig = ns(p, "designation").ifBlank { roleKind }
            val knownDoctorName = mapOf(
                "DR-KH-MANDAL" to "Dr. K.H MANDAL",
                "DR-JAY-BANIK" to "Dr. JAY BANIK",
                "DR-AMIT-GOLDAR" to "AMIT GOLDAR",
                "DR-PK-ROY" to "P.K ROY",
                "DR-JH-MANDAL" to "J.H MANDAL",
                "DR-GOKUL" to "GOKUL",
                "DR-SAIKAT-ROY" to "Dr. SAIKAT ROY",
                "DR-PRANAB-BISWAS" to "Dr. PRANAB BISWAS"
            )[pc.uppercase(Locale.US)]
            val fullName = ns(p, "full_name").ifBlank { directory?.name ?: knownDoctorName ?: "(name not set)" }
            val branch = ns(p, "branch").ifBlank { directory?.branch ?: "" }
            val salTxt = if (sc != null && sc.optBoolean("salary_enabled", false))
                "Salary: " + money(sc.optDouble("salary_amount", 0.0)) + " (day " + ns(sc, "salary_date") + ")" else "Salary: disabled"
            /* 🟣 V961 — কোনো এক্সট্রা না থাকলে লাইনটাই বসে না (কার্ড আগের মতো)। */
            val exPaid = extraPaidByStaff[pc] ?: 0.0
            val exDue = extraDueByStaff[pc] ?: 0.0
            /* 🎨🔒 V1146 (০৬.০৯.২০২৬, TK-রিপোর্ট ছবিসহ: *"Extra 8100 Paid তাহলে
               লাল কালার কেন? তাছাড়া 8100 এখানে দেখাবে না, শুধুমাত্র Due দেখাবে"*)
               ⇒ লাইনটায় এখন **শুধু বাকিটা** — দেওয়া হয়ে যাওয়া টাকা আর ওঠে না,
                 আর বাকি না থাকলে লাইনটাই বসে না (তাই লাল রংও আর ভুল বোঝায় না)।
               ⛔ টাকার কোনো অঙ্ক/নিয়ম ছোঁয়া হয়নি — শুধু কার্ডে দেখানোর লেখা।
               ⛔ `exPaid` মোছা হয়নি — Extra Income পর্দায় ওটা আগের মতোই লাগে। */
            val extraTxt = if (exDue > 0.0) "Extra: " + money(exDue) + " due" else ""
            listBox.addView(staffCard(pc, desig, roleKind, branch, fullName, ns(p, "link_mobile"), salTxt,
                onView = { editProfile(pc) }, onSalary = { salary(pc) }, isRemoved = removed,
                extraText = extraTxt, suspendedUntil = ns(p, "suspended_until")))
        }

        var shown = 0
        if (activeStaff.isNotEmpty()) {
            listBox.addView(heading("STAFF"))
            val branchOrder = com.tkbiswas.pilesclinic.native.BranchFilterStore.BRANCHES
            val branches = activeStaff.map { ns(it, "branch") }.distinct()
                .sortedWith(compareBy<String> { val n = branchOrder.indexOf(it); if (n < 0) Int.MAX_VALUE else n }.thenBy { it })
            for (branch in branches) {
                listBox.addView(heading(branch.ifBlank { "Branch not set" }, 7))
                activeStaff.filter { ns(it, "branch") == branch }
                    .sortedBy { ns(it, "full_name").ifBlank { ns(it, "person_code") } }
                    .forEach { addCard(it); shown++ }
            }
        }
        if (activeDoctors.isNotEmpty()) {
            listBox.addView(heading("DOCTORS"))
            activeDoctors.sortedWith(compareBy<JSONObject>({ ns(it, "branch") }, { ns(it, "full_name").ifBlank { ns(it, "person_code") } }))
                .forEach { addCard(it); shown++ }
        }
        if (activeField.isNotEmpty()) {
            listBox.addView(heading("FIELD OFFICER"))
            activeField.sortedBy { ns(it, "full_name").ifBlank { ns(it, "person_code") } }
                .forEach { addCard(it); shown++ }
        }
        if (shown == 0) listBox.addView(ModuleUi.body(this, "No profiles."))
        // 🔴🔒 V1517 — active=false profile-এর জন্য ইচ্ছাকৃতভাবে কোনো
        // REMOVED/Restore section নেই। পুরনো record cloud-এ অক্ষত থাকে।
    }

    private fun configuredMissingDoctors(): List<JSONObject> = listOf(
        JSONObject().put("person_code", "DR-JH-MANDAL").put("link_mobile", "7479173399").put("full_name", "J.H MANDAL").put("role_kind", "doctor").put("branch", "Cooch Behar").put("active", true),
        JSONObject().put("person_code", "DR-GOKUL").put("link_mobile", "9002610352").put("full_name", "GOKUL").put("role_kind", "doctor").put("branch", "Cooch Behar").put("active", true),
        JSONObject().put("person_code", "DR-SAIKAT-ROY").put("link_mobile", "7810907954").put("full_name", "Dr. SAIKAT ROY").put("role_kind", "doctor").put("branch", "Falakata").put("active", true),
        JSONObject().put("person_code", "DR-PRANAB-BISWAS").put("link_mobile", "9242009205").put("full_name", "Dr. PRANAB BISWAS").put("role_kind", "doctor").put("branch", "Birpara").put("active", true)
    )

    // 🔴 লক করা ডিজাইন (03.08.2026, B304 মকআপ অনুমোদন, B307-এ সংশোধিত) —
    // কম্প্যাক্ট কার্ড: নাম+ব্যাজ + View/Salary ছোট বোতাম ডানপাশে। TK পরে
    // অ্যাভাটার-আইকন (রঙিন বাক্স/👤) বাদ দিতে বলেছেন — সরানো হলো, শুধু নাম-ই
    // যথেষ্ট। শুধু এই ফাইলেই ব্যবহৃত, ModuleUi.kt ছোঁয়া হয়নি।
    /* 📱🔒 V822 (২৯.০৮.২০২৬, TK-নির্দেশ) — তালিকার কার্ডে **শুধু পুরনো ভার্সন**
       দেখানোর জন্য। TK: *"যাদের পুরনো ভার্সন চলছে তাদেরটাই শো করবে; লেটেস্ট
       চললে আর শো করার দরকার নেই।"*
       ⚠️ TK-কে আগেই জানানো ঝুঁকি: ট্যাগ না থাকা মানে "হালনাগাদ" — কিন্তু
          ভার্সনের তালিকা আনতে **ব্যর্থ** হলেও ট্যাগ বসে না। তাই আসল উত্তর
          সবসময় 📱 পর্দাটাই দেয় (ব্যর্থ হলে সেখানে "Could not load" লেখে)। */
    private var verListMap: Map<String, Int> = emptyMap()
    private var verListLatest = 0

    private fun dp(v: Int) = ModuleUi.dp(this, v)

    /* 🔴🔒 V442 (TK-নির্দেশ ১৮.০৮.২০২৬, ফটো-প্রুফ অনুমোদিত: "প্রফেশনাল বানানো
       যায় কি") — আগে View/Salary/Performance/Suspend/Remove একটার নিচে
       একটা লম্বা কলামে ডানপাশে সরু জায়গায় গাদাগাদি ছিল (কার্ড অনেক লম্বা
       দেখাত)। এখন card উপরে-নিচে (তথ্য পুরো চওড়া জুড়ে, তার নিচে বোতাম দুই
       সারিতে পাশাপাশি — View·Salary·Performance / Suspend·Remove)।
       ⛔ কোনো বোতামের কাজ/রং/লেবেল বদলায়নি — শুধু জায়গা। */
    /* ⋮🔒 V1058 (TK-নির্দেশ: *"এই থ্রি ডটে চাপ দিলে fix attendance, suspend,
       remove আসবে এবং সেটা কার্যকারী হতে হবে"*) — তিনটেই **আসল কাজ** করে,
       আগের সেই একই ফাংশনগুলোই ডাকা হয়; নতুন কিছু বানানো হয়নি।
       ⛔ বাদ-দেওয়া স্টাফের কার্ডে শুধু Restore — আগের নিয়মই।
       ⛔ Master ছাড়া Fix Attendance আসে না (আগেও আসত না)। */
    private fun staffDotsMenu(pc: String, fullName: String, mobile: String, isRemoved: Boolean, onView: () -> Unit, suspendedUntil: String = "") {
        val labels = ArrayList<String>()
        val acts = ArrayList<() -> Unit>()
        labels.add("View profile"); acts.add { onView() }
        // 🏆 V1091 (TK: *"ডানদিকে থ্রি ডটের মধ্যে থাকবে"*) — কার্ড থেকে তুলে আনা
        //    বোতামটার কাজ এক অক্ষরও বদলায়নি, একই `performanceOne()` ডাকা হয়।
        if (ModuleAuth.isMaster) { labels.add("Performance"); acts.add { performanceOne(pc, "") } }
        if (isRemoved) {
            labels.add("Restore"); acts.add { restoreStaffDialog(pc, fullName) }
        } else {
            if (ModuleAuth.isMaster) {
                labels.add("Fix Attendance"); acts.add { fixAttendanceDialog(pc, fullName, mobile) }
            }
            labels.add("Suspend"); acts.add { suspendStaffDialog(pc, fullName, suspendedUntil) }
            labels.add("Remove");  acts.add { removeStaffDialog(pc, fullName) }
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, fullName))
            .setItems(labels.toTypedArray()) { _, which -> acts.getOrNull(which)?.invoke() }
            .setNegativeButton("Close", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun staffCard(
        pc: String, desig: String, roleKind: String, branch: String, fullName: String,
        mobile: String, salaryText: String, onView: () -> Unit, onSalary: () -> Unit,
        // 🔴 V404 (16.08.2026): বাদ-দেওয়া কর্মীর কার্ডে Suspend/Remove-এর বদলে
        //    শুধু Restore থাকবে। ডিফল্ট false ⇒ পুরনো সব ডাক অবিকল আগের মতোই চলে।
        isRemoved: Boolean = false,
        extraText: String = "",     // 🟣 V961 — ফাঁকা হলে লাইনটা বসে না
        suspendedUntil: String = "" // 🔴 V1510 — ফাঁকা হলে লাইনটা বসে না
    ): LinearLayout {
        val card = ModuleUi.card(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        /* 🎨🔒 V1057 (০৪.০৯.২০২৬ — **TK-এর নিজের পাঠানো ছবি হুবহু**) — TK: *"অ্যান্ড্রয়েড
           ফোনে চেহারা এরকম বানান… যা ফটো পাঠিয়েছে হুবহু একই বানাবেন"*।
           ⇒ বাঁয়ে নামের আদ্যক্ষরের গোল ব্যাজ, ডানে ⋮ (কার্ডের View-ই খোলে),
             বেতনের লাইনে `•` বিভাজক ও "Salary day:", আর ছয়টা বোতামেই আইকন।
           ⛔ **কোনো বোতাম যোগ/বাদ/অদলবদল হয়নি** — ছবির মতোই ৩+৩; আগের সেশনে
              ওয়েবে যে "danger row" করেছিলাম সেটাও ফিরিয়ে নেওয়া হলো, কারণ
              TK-এর ছবিতে ওরকম নেই।
           ⛔ কোনো বোতামের কাজ এক অক্ষরও বদলায়নি। */
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        /* 🗑️🔒 V1059 (TK-নির্দেশ ০৪.০৯.২০২৬: *"নামের short থাকবে না, মানে
           LAXMI GUPTA (LG)"*) — V1057-এর গোল আদ্যক্ষর-ব্যাজটা তুলে দেওয়া হলো।
           ⛔ বাকি সব (⋮ · বেতনের লাইন · তিনটে বোতাম) আগের মতোই। */
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        /* 🎨🔒 V1091 (TK-রিপোর্ট ছবিসহ — CHANDANA ROY PRADHAN: *"mobile version
           কেন এরকম ব্রেক হবে"*) — লম্বা নাম গোটা সারিটা দখল করে নিত, তাই
           ডানের ভার্সন-ব্যাজটা চেপে গিয়ে **তিন লাইনে** ভেঙে যেত।
           ⇒ এখন নামটাই একমাত্র ঘর যেটা ছোট হয় (`weight = 1f`), আর ব্যাজ দুটো
             নিজের মাপেই থাকে। নাম না ধরলে শেষে "…" বসে — ভাঙে না।
           ⛔ লেখা · রং · মাপ কিছুই বদলায়নি, শুধু জায়গা ভাগের নিয়ম। */
        topRow.addView(TextView(this).apply {
            text = fullName; textSize = 14.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1C2B22"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val isDoc = roleKind.equals("doctor", true)
        topRow.addView(TextView(this).apply {
            text = desig.ifBlank { if (isDoc) "Doctor" else "Staff" }; textSize = 9.5f
            maxLines = 1   // 🎨 V1091 — চিপ কখনো ভাঙবে না
            setTextColor(android.graphics.Color.parseColor(if (isDoc) "#6A3FCB" else "#0B8A3E"))
            setPadding(dp(7), dp(2), dp(7), dp(2))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(android.graphics.Color.parseColor(if (isDoc) "#EFEAFB" else "#EAF6EE"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(6) }
        })
        /* 📱🔒 V822 — নামের পাশে ভার্সনের ট্যাগ, কিন্তু **শুধু পুরনো হলে**
           (TK: *"লেটেস্ট ভার্সন চললে আর শো করার দরকার নেই"*)।
           ⛔ জানা না থাকলে বা হালনাগাদ হলে কিছুই বসে না — কার্ড আগের মতোই। */
        if (verListLatest > 0) {
            val v = verListMap[pc.trim().uppercase(Locale.US)]
            if (v != null && v < verListLatest) {
                topRow.addView(pvChip(
                    if (v <= 0) "No app yet" else "V" + v + " · old",
                    "#B3261E", "#FDECEA"
                ))
            }
        }
        info.addView(topRow)
        info.addView(TextView(this).apply {
            text = pc + " · " + branch + " · " + ModuleUi.fullMobile(mobile)   // 🔵 V521 (TK): পুরো নম্বর
            textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#6B7A72"))
            setPadding(0, dp(2), 0, 0)
        })
        /* \uD83D\uDD12\uD83D\uDC41\uFE0F V1321 (TK-\u09a8\u09bf\u09b0\u09cd\u09a6\u09c7\u09b6 \u09e7\u09e6.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec, \u09a1\u09c7\u09ae\u09cb-\u09aa\u09cd\u09b0\u09c1\u09ab \u09aa\u09be\u09b6) \u2014 "Salary: \u09f3\u09ee,\u09e6\u09e6\u09e6"
           \u09a1\u09bf\u09ab\u09b2\u09cd\u099f\u09c7 \u09a1\u099f \u09a6\u09bf\u09af\u09bc\u09c7 \u09a2\u09be\u0995\u09be, \u09aa\u09be\u09b6\u09c7\u09b0 \uD83D\uDC41 \u099a\u09be\u09aa\u09b2\u09c7 \u09a6\u09c7\u0996\u09be \u09af\u09be\u09af\u09bc\u0964 "Salary day: N" \u0993
           "Extra: \u2026 due" \u0986\u0997\u09c7\u09b0 \u09ae\u09a4\u09cb\u0987 \u0996\u09cb\u09b2\u09be\u0964 \u09aa\u09cd\u09b0\u09a4\u09bf\u099f\u09be \u0995\u09be\u09b0\u09cd\u09a1 \u0986\u09b2\u09be\u09a6\u09be \u099f\u0997\u09b2 \u2014 \u098f\u0995\u099f\u09be
           \u0996\u09c1\u09b2\u09b2\u09c7 \u0985\u09a8\u09cd\u09af\u0997\u09c1\u09b2\u09cb \u099b\u09cb\u0981\u09af\u09bc \u09a8\u09be\u0964 \u2b1b Salary disabled \u09b9\u09b2\u09c7 \u09ae\u09be\u09b8\u09cd\u0995 \u09a6\u09b0\u0995\u09be\u09b0 \u09a8\u09c7\u0987\u0964 */
        if (salaryText.startsWith("Salary: disabled")) {
            info.addView(TextView(this).apply {
                text = salaryText
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#A7B0AB"))
                setPadding(0, dp(5), 0, if (extraText.isBlank()) dp(10) else dp(2))
            })
        } else {
            val m = Regex("^Salary:\\s*(.+?)\\s*\\(day\\s*([^)]*)\\)").find(salaryText)
            val amountStr = m?.groupValues?.get(1) ?: salaryText.removePrefix("Salary: ")
            val dayStr = m?.groupValues?.getOrNull(2) ?: ""
            var salaryRowMasked = true
            val salRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, if (extraText.isBlank()) dp(10) else dp(2))
            }
            val amtTv = TextView(this).apply {
                text = "Salary: \u2022\u2022\u2022\u2022\u2022\u2022"
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0B8A3E"))
            }
            salRow.addView(amtTv)
            salRow.addView(TextView(this).apply {
                text = "\uD83D\uDC41"
                textSize = 9.5f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(4), dp(2), dp(4), dp(2))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(5).toFloat()
                    setColor(android.graphics.Color.parseColor("#EEF3F0"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#DCE6E0"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(6) }
                isClickable = true; isFocusable = true
                setOnClickListener {
                    salaryRowMasked = !salaryRowMasked
                    amtTv.text = "Salary: " + (if (salaryRowMasked) "\u2022\u2022\u2022\u2022\u2022\u2022" else amountStr)
                }
            })
            if (dayStr.isNotBlank()) {
                salRow.addView(TextView(this).apply {
                    text = "  \u2022  Salary day: $dayStr"
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0B8A3E"))
                })
            }
            info.addView(salRow)
        }
        if (extraText.isNotBlank()) {   // 🟣 V961
            info.addView(TextView(this).apply {
                text = extraText; textSize = 10.5f
                setTextColor(android.graphics.Color.parseColor("#B45309"))
                setPadding(0, 0, 0, dp(8))
            })
        }
        /* 🔴🔒 V1510 (TK-রিপোর্ট ১৬.০৯.২০২৬ — "সাসপেন্ড করলেও কাজ হচ্ছে না,
           বোঝার উপায় নেই · বার্তা মিস হয়") — এতদিন Suspend-এর ফলাফল শুধু
           এক ঝলক টোস্টে দেখাত, কার্ডে কোথাও স্থায়ীভাবে বসত না। এখন
           suspended_until থাকলে কার্ডেই স্পষ্ট লেখা থাকবে — কোনো মেনু
           খোলা বা টোস্ট ধরার দরকার নেই। ⛔ ফাঁকা হলে লাইনটা বসে না, বাকি
           কার্ড আগের মতোই। */
        if (suspendedUntil.isNotBlank()) {
            info.addView(TextView(this).apply {
                text = "🔴 Suspended till " + dmy(suspendedUntil)
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#B3261E"))
                setPadding(0, 0, 0, dp(8))
            })
        }
        // 🔴 V442 — দুই সারির অনুভূমিক বোতাম-বার (প্রতিটা বোতাম সমান চওড়া, `weight=1f`)।
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(6) }
        }
        fun rowBtnParams(first: Boolean, last: Boolean) =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = if (first) 0 else dp(4)
                marginEnd = if (last) 0 else dp(4)
            }
        /* 🎨🔒 V1091 (০৫.০৯.২০২৬, TK-রিপোর্ট ছবিসহ: *"সব লেখাগুলো ব্রেক হয়ে
           উপর-নিচে হয়ে গেছে… সাইজ একটু ছোট করুন যাতে উপর নিচে না হয়"* —
           বিশেষ করে RUPAM-এর কার্ড, যেখানে বোতাম একটা বেশি)।
           ⇒ তিনটে পাহারা একসাথে:
             ① `maxLines = 1` — লেখা কখনো দ্বিতীয় লাইনে নামবে না
             ② লেখা **নিজে থেকে ছোট হয়ে** ঘরে বসে (autosize 8→11.5sp),
                তাই বোতাম কটা আছে বা নাম কত লম্বা — কিছুতেই ভাঙবে না
             ③ উচ্চতা নির্দিষ্ট (dp 40) — autosize-এর জন্য উচ্চতা wrap হলে
                Android-এর ফল অনিশ্চিত হয়ে যায় (গুগলের নিজের সতর্কতা), আর
                সব বোতাম সমান উঁচু দেখায়
           ⛔ কোনো বোতামের কাজ · রং · কে দেখতে পায় — কিছুই বদলায়নি। */
        // 🎨🔒 V1342 (১১.০৯.২০২৬, TK-নির্দেশ ও ডেমো-প্রুফ পাশ: "উচ্চতা এত কম কেন,
        // প্রফেশনাল বলে মনে হচ্ছে না") — 46dp → 56dp, লেখাও 11.5sp → 13sp।
        // ⛔ শুধু এই বোতাম আর নিচের dangerBtn (Suspend/Remove/Restore, একই
        // উচ্চতা রাখতেই হয়) — বাকি প্রজেক্টের অন্য কোনো বোতাম/পর্দা ছোঁয়া হয়নি।
        fun smallBtn(text: String, filled: Boolean, icon: Int = 0, onClick: () -> Unit) = TextView(this).apply {
            this.text = text; textSize = 13f
            maxLines = 1
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            /* 🎨 V1057 — লেখার বাঁয়ে আইকন (TK-এর ছবির মতো)। ⛔ আইকন না দিলে
               বোতামটা আগের মতোই শুধু লেখা — অন্য কোথাও কিছু ভাঙে না। */
            if (icon != 0) {
                setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
                compoundDrawablePadding = dp(4)   // 🎨 V1091 — লেখার জায়গা বাড়াতে
                // ভরাট বোতামে আইকনটাও সাদা, নইলে সবুজের উপর সবুজ মিলিয়ে যেত
                if (filled) androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(
                    this, android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE))
            }
            setPadding(dp(4), 0, dp(4), 0)
            // 🎨 V1146 (TK: *"তিনটা লাইন উচ্চতা এত কম"*) — 40 → 46dp, সব বোতামেই।
            // 🎨 V1342 (১১.০৯.২০২৬, TK-নির্দেশ) — 46 → 56dp, আরো প্রফেশনাল দেখাতে।
            /* 🔴🔒 V1401 (১২.০৯.২০২৬ সন্ধ্যা, TK: *"এই বক্সগুলোর উচ্চতা সামান্য একটু
               বড় করুন"*, ডেমো-প্রুফ পাশ) — **আমার ভুল ধরা পড়ল:** উপরের ৪৬/৫৬dp
               কোনোদিন কার্যকর হয়নি। `height = dp(..)` লেখার **পরে** নিচের
               `setSingleLine(true)` চলত, আর Android-এ setSingleLine ভিতরে
               setLines(1) ডাকে — যা আগের নির্দিষ্ট উচ্চতা বাতিল করে দেয়। তাই বোতাম
               আসলে লেখা/আইকনের মাপেই (~১৬-২৪dp) ছিল — TK-র ছবিতেও তাই দেখা যায়।
               ⇒ উচ্চতা এখন setSingleLine-এর **পরে** বসে (XML-এ singleLine+height
                  যেভাবে কাজ করে, হুবহু সেই ক্রম), মাপ TK-র পাশ করা ২৬dp —
                  এখনকার আসল উচ্চতার থেকে সামান্য বেশি (৫৬ দিলে অনেক বেশি হত,
                  TK ডেমো দেখে বাতিল করেছেন)।
               ⛔ নিচের dangerBtn-এও একই ক্রম, একই উচ্চতা (একই দোষ, নিয়ম ৭)। */
            /* 🎨🔒 V1092 (০৫.০৯.২০২৬ — TK-এর বিল্ড-করা ছবিতে ধরা পড়ল: RUPAM-এর
               "Extra Income" তখনো দুই লাইনে)। **আসল কারণ মেপে পাওয়া:** বাঁয়ের
               আইকনটা (২৪dp + ফাঁক) বোতামের চওড়ার একটা বড় অংশ নিয়ে নেয়, আর
               Android-এর নিজে-থেকে-ছোট-হওয়া (autosize) মাপার সময় **আইকনের
               জায়গাটা বাদ দেয় না** — তাই লেখা ঠিক আছে ভেবে ছোট করত না, অথচ
               আঁকার সময় জায়গা কম পড়ে ভেঙে যেত। ⇒ autosize-এর উপর ভরসা না করে
               এখন নিশ্চিত নিয়ম: এক লাইনে বাঁধা, আর সারিতে ৩টে বা বেশি বোতাম
               থাকলে নিচে (`row1Btns` বানানোর পরে) আইকন বাদ ও লেখা ছোট। */
            setSingleLine(true)
            height = dp(26)   // 🔴 V1401 — setSingleLine-এর পরে, নইলে বাতিল হয়ে যায়
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(android.graphics.Color.parseColor(if (filled) "#FFFFFF" else "#0B4F2A"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                if (filled) setColor(android.graphics.Color.parseColor("#0B8A3E"))
                else { setColor(android.graphics.Color.parseColor("#EAF6EE")); setStroke(dp(1), android.graphics.Color.parseColor("#CFE9D8")) }
            }
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        // 🔴 V404: লাল বোতাম বানানোর একটাই জায়গা (Suspend ও Remove একই চেহারার)।
        fun dangerBtn(label: String, icon: Int = 0, onClick: () -> Unit) = TextView(this).apply {
            text = label; textSize = 13f   // 🎨 V1342 — smallBtn-এর হুবহু একই মাপ
            maxLines = 1   // 🎨 V1091 — উপরের smallBtn-এর হুবহু একই নিয়ম
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            if (icon != 0) {
                setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
                compoundDrawablePadding = dp(4)   // 🎨 V1091 — লেখার জায়গা বাড়াতে
            }
            setPadding(dp(4), 0, dp(4), 0)
            setSingleLine(true)   // 🎨 V1092 — উপরের smallBtn-এর হুবহু একই নিয়ম
            height = dp(26)       // 🔴 V1401 — smallBtn-এর হুবহু একই উচ্চতা, setSingleLine-এর পরে
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(android.graphics.Color.parseColor("#B0392B"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(android.graphics.Color.parseColor("#FDECEA")); setStroke(dp(1), android.graphics.Color.parseColor("#F2C6C0"))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        val row1Btns = mutableListOf<TextView>()
        /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"এই কার্ডের মধ্যে ভিউ থাকবে না,
           কিন্তু কার্ডে চাপ দিলে ভিউ হবে। তাহলে Salary · Performance ·
           Fix Attendance — এই তিনটা পাশাপাশি থাকবে।"*
           ⇒ "View" বোতামটা সরানো হলো; তার কাজটা এখন **পুরো কার্ডে চাপ** দিলেই হয়।
           ⛔ `onView` ফাংশনটা এক অক্ষরও বদলায়নি — শুধু কোথা থেকে ডাকা হচ্ছে সেটা।
           ⛔ ভিতরের বোতামগুলো নিজের কাজই করে (Android-এ ভিতরের ক্লিক আগে চলে ও
              সেখানেই থেমে যায়), তাই Salary চাপলে ভুল করে View খুলবে না। */
        /* 🏆🔒 V1091 (০৫.০৯.২০২৬, TK-নির্দেশ: *"কার্ডে চাপ দিলে পারফরমেন্স খুলবে…
           সামনে Performance লেখা না থাকলেও চলবে, ডানদিকে থ্রি ডটের মধ্যে থাকবে"*)
           ⇒ ① কার্ডে চাপ = **Performance** (আগে প্রোফাইল খুলত)
             ② কার্ডের "Performance" বোতামটা তুলে দেওয়া হলো — তাই বোতাম একটা কমল,
                লেখা ভাঙার চাপও কমল
             ③ প্রোফাইল হারায়নি — ⋮ মেনুতে "View profile" আগে থেকেই আছে, আর
                সেখানে "Performance"-ও বসানো হলো
           ⛔ Master ছাড়া কারো জন্য Performance আগেও ছিল না, এখনো নেই — তাই
              তাঁদের কার্ডে চাপ দিলে আগের মতোই প্রোফাইলই খোলে। */
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener { if (ModuleAuth.isMaster) performanceOne(pc, "") else onView() }
        row1Btns.add(smallBtn("Salary", true, R.drawable.ic_sp_wallet, onSalary))
        // 💰 V1058 — TK: "salary performance extra income এই তিনটাই পাশাপাশি"
        row1Btns.add(smallBtn("Extra Income", false, R.drawable.ic_sp_hand_rupee) { salaryExtra(pc) })
        // 🔴🔴🔒 V477 (20.08.2026, TK-জরুরি নির্দেশ — "সমস্ত স্টাফের একই সমস্যা,
        // OUT TIME দেখাচ্ছে না") — আসল কারণ (যাচাই করা): আজ সকালে JWT/reAuth
        // বাগ (V465-এ ঠিক করা) থাকাকালীন যাদের IN TIME নিঃশব্দে ক্লাউডে সেভ
        // হয়নি, তাদের জন্য এখন (সন্ধ্যা, দুপুর ১২টার সীমা পার) নিজে থেকে আর
        // IN TIME বসানোর উপায় নেই। শুধু Master-এর জন্য — যেকোনো সময়ে সেই
        // স্টাফের আজকের IN/OUT TIME সরাসরি বসানোর/ঠিক করার সুযোগ।
        // ⛔ স্টাফের নিজের Work Notebook স্ক্রিন/নিয়ম এক অক্ষরও বদলায়নি —
        //    এটা সম্পূর্ণ নতুন, আলাদা Master-only পথ, একই টেবিলে লেখে।
        /* 🎨🔒 V1058 (TK-নির্দেশ ০৪.০৯.২০২৬: *"এই থ্রি ডটে চাপ দিলে fix attendance,
           suspend, remove আসবে… salary performance extra income এই তিনটাই কার্ডে
           পাশাপাশি থাকবে"*) — Fix Attendance এখন কার্ডে নয়, ⋮ মেনুতে।
           ⛔ কাজটা এক অক্ষরও বদলায়নি — একই `fixAttendanceDialog()` ডাকা হয়। */
        /* 🏍️🔒 V978 (০২.০৯.২০২৬, TK-নির্দেশ: *"হ্যাঁ, ওই সারিতেই বসিয়ে দিন"*) —
           বাইরে ঘোরা স্টাফের (এখন শুধু RUPAM) কার্ডেই **Field Visit** বোতাম,
           এই একই সারিতে। আগে এটা Salary পর্দার ভিতরে ছিল, TK খুঁজে পাচ্ছিলেন না।
           🛰️🔒 V1346 (১১.০৯.২০২৬, TK-নির্দেশ) — এখন **সব স্টাফের** কার্ডেই এই
           বোতাম আসে (আগে শুধু isFieldStaffCode হলে) — কারণ IN-OUT TIME
           হাজিরা-লোকেশন এখন সবার জন্য চালু। ফিল্ড-স্টাফের বেলায় লেবেল আগের
           মতোই "Field Visit" (বাইক/কিমি-হিসাব), বাকি সবার বেলায় "Location"
           (শুধু হাজিরার সময়টুকু কোথায় ছিলেন) — একই পর্দা (`FieldVisitActivity`)
           খোলে, ভিতরের `readDays()`-এর তথ্য একই টেবিল থেকেই আসে। */
        if (ModuleAuth.isMaster) {
            val isFieldCode = com.tkbiswas.pilesclinic.native.FieldVisit.isFieldStaffCode(pc)
            row1Btns.add(smallBtn(if (isFieldCode) "Field Visit" else "Location", false, R.drawable.ic_sp_pin) {
                startActivity(android.content.Intent(this, FieldVisitActivity::class.java)
                    .putExtra(FieldVisitActivity.EXTRA_OWNER, true)
                    .putExtra(FieldVisitActivity.EXTRA_STAFF_CODE, pc)
                    .putExtra(FieldVisitActivity.EXTRA_STAFF_MOBILE,
                        com.tkbiswas.pilesclinic.native.FieldVisit.mobileForCode(pc)))
            })
        }
        /* 🎨🔒 V1092 — ৩টে বা বেশি বোতাম হলে (এখন শুধু RUPAM-এর কার্ড) প্রতিটা
           বোতাম সরু হয়ে যায়। তখন আইকন তুলে দিয়ে ও লেখা একটু ছোট করে দেওয়া হয়,
           তাই "Extra Income" এক লাইনেই ধরে। ⛔ দুটো বোতামের কার্ড (বাকি সবার)
           হুবহু আগের মতোই — আইকনসহ, একই মাপ। */
        /* 🎨🔒 V1128 (০৫.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — *"সবার মতো এক রকম করুন…
           বোতামগুলোর উচ্চতা সবগুলোর ক্ষেত্রে একই থাকবে, যাতে দেখলে প্রফেশনাল
           বলে মনে হয়"*) — আগে তিনটে বোতাম হলে **আইকন তুলে দেওয়া হত**, তাই
           RUPAM-এর কার্ডটা বাকি সবার থেকে আলাদা দেখাত (TK ধরেছেন)।
           ⇒ এখন **আইকন থাকে**, শুধু লেখা এক ধাপ ছোট (১১.৫ → ১০) — ফোনের আসল
             চওড়ায় (৩৬০) মেপে দেখা হয়েছে, "Extra Income" ঠিকঠাক ধরে, কিছুই
             কাটে না বা ভাঙে না।
           ⛔ উচ্চতা প্রতিটা বোতামেই আগের মতোই স্থির (dp 40) — কার্ড কটা বোতাম
              ধরে তাতে কিছু বদলায় না।
           ⛔ দুটো বোতামের কার্ড (বাকি সবার) এক অক্ষরও বদলায়নি। */
        /* 🎨🔒 V1146 (০৬.০৯.২০২৬, TK-রিপোর্ট ছবিসহ: *"লেখা ব্রেক হয়ে গেছে"* —
           RUPAM-এর কার্ডে "Extra Inc…")। **কারণ মেপে পাওয়া:** ৩৬০dp ফোনে
           তিনটে বোতামের প্রতিটা ≈৯৭dp; তার থেকে আইকন ২৪dp + ফাঁক ৪ + প্যাডিং ৮
           বাদ গেলে লেখার থাকে ≈৬১dp, আর "Extra Income" ১০sp-এ ≈৬২dp — ঠিক
           ওইটুকুতেই কেটে যেত।
           ⇒ আইকনটা **১৬dp**-তে ছোট করা হলো (ফাঁকও ৩dp) ⇒ লেখার জায়গা ≈৭০dp,
             তাই পুরো "Extra Income" এক লাইনেই ধরে।
           ⛔ V1128-এর TK-নির্দেশ অটুট: **আইকন থাকছে** (তুলে দেওয়া হয়নি) আর
              সব বোতামের উচ্চতা এক — কার্ড কটা বোতাম ধরে তাতে কিছু বদলায় না।
           ⛔ দুটো বোতামের কার্ড (বাকি সবার) এক অক্ষরও বদলায়নি। */
        if (row1Btns.size >= 3) {
            val ic = dp(16)
            row1Btns.forEach { b ->
                b.textSize = 10f
                b.compoundDrawablePadding = dp(3)
                b.compoundDrawablesRelative.getOrNull(0)?.let { d ->
                    d.setBounds(0, 0, ic, ic)
                    b.setCompoundDrawablesRelative(d, null, null, null)
                }
            }
        }
        row1Btns.forEachIndexed { i, b -> b.layoutParams = rowBtnParams(i == 0, i == row1Btns.size - 1); row1.addView(b) }
        info.addView(row1)
        if (isRemoved) {
            // 🔴 V404: ভুল করে বাদ দিলে ফিরিয়ে আনার পথ।
            val restore = smallBtn("Restore", false) { restoreStaffDialog(pc, fullName) }
            restore.layoutParams = rowBtnParams(true, true)
            row2.addView(restore)
        } else {
            /* 💰🔒 V1029 (০৩.০৯.২০২৬, TK-নির্দেশ: *"salary সহ যে পাঁচটা বটম আছে
               সেখানেই এক্সট্রা ইনকামটা রাখতে বলা হয়েছিল"*) — এতদিন এক্সট্রা ইনকাম
               শুধু বেতন-পর্দার **ভিতরে** ছিল, কার্ডে শুধু লেখার একটা লাইন (V961)।
               এখন এই সারিতেই বোতাম — চাপলে সোজা সেই স্টাফের এক্সট্রা ইনকামে।
               ⛔ V978-এর Field Visit-এর মতোই একই ধরন; বাকি বোতাম অপরিবর্তিত।
               ⛔ টাকার কোনো অঙ্ক/নিয়ম ছোঁয়া হয়নি — শুধু পৌঁছনোর পথ। */

            // 🔵🔒 B618 (11.08.2026, TK-নির্দেশ): master স্টাফকে কয়েকদিন Suspend করতে
            // পারবেন — সাসপেন্ড থাকাকালীন সে লগইন করতে পারবে না (LoginActivity গেট)।
            // ⛔ শুধু স্টাফ-তালিকায় (এই পর্দা master-only, role_kind=staff ফিল্টার করা)।

            // 🔴 V404 (16.08.2026, TK-নির্দেশ "কর্মী বাদ দিন বোতাম বসান"):
            //    আগে অ্যাপে বাদ দেওয়ার কোনো পথই ছিল না — শুধু Suspend ছিল।

        }
        info.addView(row2)
        /* 🎨 V1057 — গোল ব্যাজ · তথ্য · ⋮ এক সারিতে; ⋮ চাপলে কার্ডে চাপ দিলে যা
           হয় ঠিক তাই (View) — নতুন কোনো কাজ বানানো হয়নি। */
        headRow.addView(info)
        headRow.addView(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_sp_more)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginStart = dp(6) }
            isClickable = true; isFocusable = true
            setOnClickListener { staffDotsMenu(pc, fullName, mobile, isRemoved, onView, suspendedUntil) }   // ⋮ V1058
        })
        card.addView(headRow)
        return card
    }

    // =====================================================================
    // 🔴 V404 (16.08.2026, TK-নির্দেশ) — কর্মী বাদ দেওয়া ও ফিরিয়ে আনা
    // ---------------------------------------------------------------------
    // বাদ দিলে একটাই বোতামে তিনটে কাজ হয়:
    //   ১) hr.staff_profiles.active = false  ⇒ লগইন বন্ধ
    //      (V404 SQL-এর `suspended_until_for` তখন 2999-12-31 ফেরায় — ওয়েব ও
    //       ফোন দুটোর লগইন-গেটই এই একটা ফাংশনই ডাকে, তাই লগইন-কোডে হাত পড়েনি)
    //   ২) hr.salary_config.salary_enabled = false ⇒ আর "Salary Due"-তে নাম নয়
    //   ৩) fin.entry_permits.can_entry = false ⇒ আয়-খরচের চাবি বন্ধ
    // ⛔ রোগী · ফলোআপ · মাইনের রসিদ · ছুটি — একটাও সারি ছোঁয়া হয় না
    //    (TK-সিদ্ধান্ত: "রেকর্ড অটুট থাক")।
    // ⛔ খাতার ফাঁদ ১: RLS আটকালে update চুপচাপ ০ সারিতে চলে ও true ফেরায় —
    //    তাই এই পর্দা master-only, আর ব্যর্থ হলে সৎ বার্তা দেখানো হয়।
    // =====================================================================
    private fun removeStaffDialog(pc: String, fullName: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Permanent Remove — $fullName"))
            .setMessage(
                "$pc — permanently remove this staff from the app?\n\n" +
                "• Login stops\n" +
                "• Staff Profile will no longer show this person\n" +
                "• Removed from the salary list\n" +
                "• Income/Expense key turned off\n\n" +
                "Past Attendance/Salary/Income records are NOT deleted."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove Permanently") { _, _ -> doSetActive(pc, false) }
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    private fun restoreStaffDialog(pc: String, fullName: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Restore — $fullName"))
            .setMessage(
                "$pc — restore this staff?\n\n" +
                "Login will work again.\n" +
                "Salary must be turned on separately from the Salary screen."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore") { _, _ -> doSetActive(pc, true) }
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    private fun doSetActive(pc: String, active: Boolean) {
        Thread {
            val enc = java.net.URLEncoder.encode(pc, "UTF-8")
            val patch = JSONObject().put("active", active)
            var ok = try { ModuleAuth.updateAtLeastOne("hr", "staff_profiles", "person_code=eq.$enc", patch) } catch (_: Throwable) { false }
            // চার V308 Doctor-এর profile আগে DB-তে ছিল না। তাঁদের Remove করলে
            // একটি inactive profile তৈরি করাই login বন্ধ করার নিরাপদ চিহ্ন।
            if (!ok && !active) {
                val seed = configuredMissingDoctors().firstOrNull { ns(it, "person_code") == pc }
                if (seed != null) {
                    val inactiveSeed = JSONObject(seed.toString()).put("active", false)
                    ok = try { ModuleAuth.upsertOnConflict("hr", "staff_profiles", inactiveSeed, "person_code") } catch (_: Throwable) { false }
                    if (ok) {
                        val verify = try { ModuleAuth.getRowsChecked("hr", "staff_profiles", "select=active&person_code=eq.$enc&active=eq.false&limit=1") } catch (_: Throwable) { null }
                        ok = verify?.ok == true && verify.rows.length() == 1
                    }
                }
            }
            // বাদ দিলে মাইনে ও চাবিও বন্ধ। ⛔ ফেরানোর সময় মাইনে নিজে থেকে চালু
            //    হয় না — টাকার ব্যাপার, মাস্টার নিজে Salary পর্দায় গিয়ে করবেন।
            var salOk = true; var permitOk = true
            if (ok && !active) {
                salOk = try {
                    ModuleAuth.update("hr", "salary_config", "person_code=eq.$enc",
                        JSONObject().put("salary_enabled", false))
                } catch (_: Throwable) { false }
                permitOk = try {
                    ModuleAuth.update("fin", "entry_permits", "person_code=eq.$enc",
                        JSONObject().put("can_entry", false))
                } catch (_: Throwable) { false }
            }
            runOnUiThread {
                // ⛔ সৎ বার্তা — অর্ধেক হলে "হয়ে গেছে" বলা হয় না।
                val msg = when {
                    !ok -> "Failed — check net"
                    !active && (!salOk || !permitOk) -> "Partly done. Please try again."
                    active -> "$pc restored"
                    else -> "$pc removed"
                }
                ModuleUi.toast(this, msg)
                if (ok) {
                    // পুরনো cached card এক মুহূর্তও আবার দেখাব না। নতুন সত্য
                    // সরাসরি cloud থেকে এনে তারপর তালিকা আঁকা হবে।
                    try { staffListCachePrefs().edit().clear().apply() } catch (_: Throwable) { }
                    renderList()
                }
            }
        }.start()
    }

    // 🔵🔒 B618: Suspend ডায়ালগ — কত দিন (3/7/নিজে) অথবা Remove।
    // 🔴🔒 V1510 (TK-রিপোর্ট ১৬.০৯.২০২৬ — "যাকে সাসপেন্ড করিনি তার ক্ষেত্রেও
    //    Remove suspend দেখায় কেন") — এখন শিরোনামেই বর্তমান অবস্থা স্পষ্ট
    //    লেখা থাকে, তাই TK নিজে বুঝে নিতে পারবেন Remove suspend চাপার
    //    দরকার আছে কিনা। ⛔ অপশন চারটেই আগের মতো থাকছে (Remove suspend
    //    চাপলে এমনিতেও ক্ষতি নেই, ইতিমধ্যে ফাঁকা থাকা ঘর ফাঁকাই থাকে)।
    private fun suspendStaffDialog(pc: String, fullName: String, suspendedUntil: String = "") {
        val opts = arrayOf("3 days", "7 days", "Custom days", "Remove suspend")
        val status = if (suspendedUntil.isBlank()) "Currently Active (not suspended)" else "Currently Suspended till ${dmy(suspendedUntil)}"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Suspend — $fullName\n($status)"))
            .setItems(opts) { _, which ->
                when (which) {
                    0 -> askSuspendReason(pc, 3)
                    1 -> askSuspendReason(pc, 7)
                    2 -> askCustomSuspendDays(pc)
                    else -> doSuspend(pc, -1)   // Remove suspend — কারণ জিজ্ঞাসা করার দরকার নেই, ফাঁকাই হবে
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }
    private fun askCustomSuspendDays(pc: String) {
        val input = ModuleUi.numberInput(this, "Days")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Suspend how many days?"))
            .setView(input)
            .setPositiveButton("OK") { _, _ -> val n = input.text.toString().toIntOrNull() ?: 0; if (n > 0) askSuspendReason(pc, n) else ModuleUi.toast(this, "Enter days") }
            .setNegativeButton("Cancel", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }
    /* 🔴🔒 V1649 (২১.০৯.২০২৬, TK-নির্দেশ) — "সাসপেন্ড কি কারনে করা হয়েছে সেটার
       জন্য সে বুঝতে পারে" · "ফাঁকা রাখলেও চলবে, তবে স্টাফ বার্তা দেখবে কিছু
       লেখা হয়নি"। তাই এই ঘরটা বাধ্যতামূলক নয় — ফাঁকা রেখেই OK চাপা যায়। */
    private fun askSuspendReason(pc: String, days: Int) {
        val input = android.widget.EditText(this).apply {
            hint = "Reason (optional) — staff will see this"
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Reason for suspending?"))
            .setView(input)
            .setPositiveButton("OK") { _, _ -> doSuspend(pc, days, input.text.toString().trim()) }
            .setNegativeButton("Cancel", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }
    // days < 0 = Remove suspend (null বসে, কারণও ফাঁকা হয়ে যায়)।
    private fun doSuspend(pc: String, days: Int, reason: String = "") {
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val until: String? = if (days < 0) null else {
            val cal = java.util.Calendar.getInstance(tz); cal.add(java.util.Calendar.DAY_OF_MONTH, days)
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = tz }.format(cal.time)
        }
        ModuleUi.toast(this, "Saving...")
        // 🔴🔒 V1649 — Remove suspend (days<0) হলে কারণও ফাঁকা হয়ে যায়।
        val reasonForSave = if (days < 0) "" else reason
        Thread {
            val patch = JSONObject().put("suspended_until", until ?: JSONObject.NULL)
                .put("suspend_reason", if (reasonForSave.isBlank()) JSONObject.NULL else reasonForSave)
            val enc = try { java.net.URLEncoder.encode(pc, "UTF-8").replace("+", "%20") } catch (_: Throwable) { pc }
            // 🔴🔒 V1645 (TK-রিপোর্ট ২০.০৯.২০২৬ — "সাসপেন্ড করলেও বাস্তবে কাজ
            // করে না") — চার V308 Doctor-এর (J.H MANDAL/GOKUL/SAIKAT ROY/
            // PRANAB BISWAS) কোনো staff_profiles সারিই DB-তে ছিল না (doSetActive-
            // এর মতোই একই ফাঁক, ⛔ কমেন্ট-লাইন ৪২২-৪২৫ দ্রষ্টব্য), তাই এই PATCH
            // ০ সারিতে চলত। পুরনো `update()` ০ সারিতেও `true` ফেরাত — তাই
            // "Suspended till …" দেখালেও আসলে কিছুই সেভ হতো না। এখন
            // `updateAtLeastOne()` দিয়ে সত্যিই সারি বদলেছে কিনা যাচাই হয়, আর
            // সারি না থাকলে (Remove-এর মতোই) seed দিয়ে নতুন সারি বসানো হয়।
            var ok = try { ModuleAuth.updateAtLeastOne("hr", "staff_profiles", "person_code=eq.$enc", patch) } catch (_: Throwable) { false }
            if (!ok) {
                val seed = configuredMissingDoctors().firstOrNull { ns(it, "person_code") == pc }
                if (seed != null) {
                    val withSuspend = JSONObject(seed.toString()).put("suspended_until", until ?: JSONObject.NULL)
                        .put("suspend_reason", if (reasonForSave.isBlank()) JSONObject.NULL else reasonForSave)
                    ok = try { ModuleAuth.upsertOnConflict("hr", "staff_profiles", withSuspend, "person_code") } catch (_: Throwable) { false }
                }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                ModuleUi.toast(this, if (ok) (if (days < 0) "Suspend removed" else "Suspended till $until") else "Failed — check net")
                /* 🔴🔒 V1510 (TK-রিপোর্ট ১৬.০৯.২০২৬ — "কোনো বার্তা আসে না বুঝতে
                   পারছি না কাজ হয়েছে কিনা") — টোস্ট এক ঝলকের জন্য আসে, মিস
                   হতেই পারে। Restore/Remove-এর মতোই এখন তালিকা নিজে থেকে
                   আবার আঁকা হয় (cache মুছে, টাটকা ক্লাউড-তথ্য দিয়ে) — কার্ডে
                   স্থায়ীভাবে "🔴 Suspended till …" দেখা যাবে, বার্তা ধরার
                   দরকার নেই। */
                if (ok) {
                    try { staffListCachePrefs().edit().clear().apply() } catch (_: Throwable) { }
                    renderList()
                }
            }
        }.start()
    }

    private fun manageRmpCallingAccess(personCode: String, homeBranch: String) {
        ModuleUi.toast(this, "Loading RMP Calling Access...")
        Thread {
            val result = try { RmpCallingAccessRepository.adminAccessList(this, personCode) }
                         catch (_: Throwable) { RmpCallingAccessRepository.Result<List<RmpCallingAccessRepository.AccessRow>>(false, message = "Could not load access") }
            runOnUiThread {
                if (!result.ok) {
                    ModuleUi.toast(this, result.message.ifBlank { "Could not load access" })
                    return@runOnUiThread
                }
                val rows = (result.value ?: emptyList()).filter { !it.branch.equals(homeBranch.trim(), true) }
                if (rows.isEmpty()) {
                    ModuleUi.toast(this, "No other branch available")
                    return@runOnUiThread
                }
                val labels = rows.map { r ->
                    (if (r.enabled) "✓ " else "○ ") + r.branch + (if (r.enabled) " — ALLOWED" else " — OFF")
                }.toTypedArray()
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "RMP Calling Access — $personCode"))
                    .setItems(labels) { _, which ->
                        val row = rows[which]
                        val newValue = !row.enabled
                        val reason = android.widget.EditText(this).apply {
                            hint = "Mandatory reason / note"
                            minLines = 2
                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        }
                        val verb = if (newValue) "ENABLE" else "DISABLE"
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "$verb ${row.branch} RMP Calling"))
                            .setMessage("$personCode will ${if (newValue) "be able" else "no longer be able"} to open ${row.branch} RMP Call + Remark only. Patient, payment, commission, edit and delete access are not granted.")
                            .setView(reason)
                            .setPositiveButton("Confirm", null)
                            .setNegativeButton("Cancel", null)
                            .create()
                        dialog.setOnShowListener {
                            com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dialog)
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                val note = reason.text.toString().trim()
                                if (note.isBlank()) {
                                    reason.error = "Reason is required"
                                    reason.requestFocus()
                                    return@setOnClickListener
                                }
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                                Thread {
                                    val saved = try { RmpCallingAccessRepository.adminSetAccess(this, personCode, row.branch, newValue, note) }
                                                catch (_: Throwable) { RmpCallingAccessRepository.Result<Unit>(false, message = "Could not save") }
                                    runOnUiThread {
                                        if (saved.ok) {
                                            dialog.dismiss()
                                            ModuleUi.toast(this, if (newValue) "RMP Calling Access enabled" else "RMP Calling Access disabled")
                                            manageRmpCallingAccess(personCode, homeBranch)
                                        } else {
                                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                            ModuleUi.toast(this, saved.message.ifBlank { "Could not save" })
                                        }
                                    }
                                }.start()
                            }
                        }
                        dialog.show()
                    }
                    .setNegativeButton("Close", null)
                    .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
            }
        }.start()
    }

    // 🔴 লক করা নিয়ম (03.08.2026, TK-নির্দেশ স্পষ্ট): "Edit" বোতাম আর নেই, নাম
    // "View" — ফিল্ড ডিফল্টে শুধু দেখা যাবে (টাইপ করা যাবে না, কীবোর্ড খুলবে
    // না), ৩-বার চাপলে তবেই সেই একটা ফিল্ড এডিটযোগ্য হবে — ঠিক ছবির মতোই
    // (TripleTapEdit পুনর্ব্যবহার, নতুন কিছু আবিষ্কার করা হয়নি)।
    private fun lockField(f: android.widget.EditText) {
        f.isFocusable = false
        f.isFocusableInTouchMode = false
        f.isCursorVisible = false
        TripleTapEdit.attach(f) {
            f.isFocusable = true
            f.isFocusableInTouchMode = true
            f.isCursorVisible = true
            f.requestFocus()
            f.setSelection(f.text.length)
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(f, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun unlockField(f: android.widget.EditText) {
        f.isFocusable = true
        f.isFocusableInTouchMode = true
        f.isCursorVisible = true
    }

    // ---------- MASTER: edit ----------
    private fun editProfile(code: String) {
        backAction = { renderList() }
        pendingPhotoDataUrl = null
        currentPhotoDataUrl = null
        // 🔴 B308 (03.08.2026, TK-অনুমোদিত "মডেল ২" মকআপ) — সবুজ গ্রেডিয়েন্ট হিরো
        // হেডার (নাম+কোড+ফটো) + আইকন-সহ ফ্ল্যাট ফিল্ড-লিস্ট (প্রতিটা সারিতে
        // আইকন+লেবেল+মান+🔒) + নিচে পাশাপাশি Back/Save। ফিল্ড-লক (B304, TripleTapEdit)
        // এক অক্ষরও বদলায়নি, শুধু চেহারা।
        val col = ModuleUi.screen(this, "")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(android.graphics.Color.parseColor("#0B4F2A"), android.graphics.Color.parseColor("#0B8A3E"))
            ).apply { cornerRadius = dp(16).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(12) }
        }
        /* 🎨🔒 V1456 (১৪.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ পাশ — "হ্যাঁ পাশ, এবার
           বসিয়ে দিন") — TK: *"Card গুলি আরো প্রফেশনাল লুক বানাতে হবে তাছাড়া
           Edit এর অপশন ও রাখতে হবে"*। আগে "Edit Profile"/"Editing Enabled"
           পুরো-চওড়া একটা লেখা-বোতাম নিচে আলাদা লাইনে থাকত, যেটা বোতাম বলে
           বোঝা কঠিন ছিল (TK-এর প্রশ্ন: "কারো প্রোফাইল Edit/update কেন করতে
           পারবো না")। এখন শিরোনামের ঠিক পাশেই ছোট, স্পষ্ট "✏️ Edit" বোতাম। */
        val heroTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
        }
        heroTop.addView(TextView(this).apply {
            text = "View $code"; textSize = 19f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val editAllBtn = TextView(this).apply {
            text = "✏️ Edit"; textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            setPadding(dp(11), dp(5), dp(11), dp(5))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(android.graphics.Color.parseColor("#26FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#4DFFFFFF"))
            }
            isClickable = true; isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
        }
        heroTop.addView(editAllBtn)
        hero.addView(heroTop)
        val photoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val img = ModuleUi.image(this); photoPreview = img
        img.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(android.graphics.Color.parseColor("#26FFFFFF"))
            setStroke(dp(1), android.graphics.Color.parseColor("#4DFFFFFF"))
        }
        // V356: Master-এর জন্য ছবি আর গোপন ৩-ট্যাপ নয়—Profile-এর ভিতরেই
        // সরাসরি Add/Change Photo বোতাম। পুরনো আলাদা Staff Photos menu বাদ।
        val changeLabel = TextView(this).apply {
            text = "📷 Add / Change Photo"; textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#E8F5EC"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#26FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#4DFFFFFF"))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { pickPhoto.launch("image/*") }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(12) }
        }
        photoRow.addView(img); photoRow.addView(changeLabel)
        hero.addView(photoRow)
        col.addView(hero)

        val name = ModuleUi.input(this, "Full Name")
        val branch = ModuleUi.input(this, "Branch")
        val designation = ModuleUi.input(this, "Designation (e.g. Staff/Receptionist)")
        val join = ModuleUi.input(this, "Join Date")
        val dob = ModuleUi.input(this, "Date of Birth")
        val gender = ModuleUi.input(this, "Gender")
        val bloodGroup = ModuleUi.input(this, "Blood Group")
        val qualification = ModuleUi.input(this, "Qualification")
        val addr = ModuleUi.input(this, "Address")
        val altMobile = ModuleUi.input(this, "Alternate Mobile")
        val emg = ModuleUi.input(this, "Emergency Contact (Name + Mobile)")
        val emgRel = ModuleUi.input(this, "Emergency Contact Relationship")
        val idType = ModuleUi.input(this, "ID Type (Aadhaar/PAN/Voter)")
        val idNum = ModuleUi.input(this, "ID Number (stored masked)")
        val notes = ModuleUi.input(this, "Notes")
        val fieldList = listOf(
            Triple("👤", "Full Name", name), Triple("🏢", "Branch", branch), Triple("💼", "Designation", designation),
            Triple("📌", "Join Date", join), Triple("🎂", "Date of Birth", dob), Triple("⚧", "Gender", gender),
            Triple("🩸", "Blood Group", bloodGroup), Triple("🎓", "Qualification", qualification),
            Triple("🏠", "Address", addr), Triple("📱", "Alternate Mobile", altMobile),
            Triple("🚨", "Emergency Contact", emg), Triple("👪", "Emergency Relationship", emgRel),
            Triple("🪪", "ID Type", idType), Triple("🔢", "ID Number", idNum), Triple("📝", "Notes", notes)
        )
        editAllBtn.setOnClickListener {
            fieldList.forEach { unlockField(it.third) }
            editAllBtn.text = "✏️ Editing"
            ModuleUi.toast(this@StaffProfileActivity, "Profile can now be edited — press Save after changes")
        }
        /* 🎨🔒 V1456 (১৪.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — আগে সবকটা ঘর একটাই
           লম্বা কার্ডে মেশানো থাকত। TK: *"Card গুলি আরো প্রফেশনাল লুক বানাতে
           হবে"* — এখন বিষয়ভিত্তিক পাঁচটা ছোট কার্ডে ভাগ, প্রতিটার উপরে ছোট
           শিরোনাম (Personal Details/Work/Contact/Emergency Contact/ID & Notes)
           — ওয়েবের "My Profile"-এ TK আগেই পাশ করা প্যানেল-ডিজাইনের ফোন-সংস্করণ।
           ⛔ প্রতিটা ঘরে lockField()-এর ৩-ট্যাপ এডিট ও দুই-কলাম/পুরো-চওড়া সাজ
           (fullWidth সেট) অক্ষত — শুধু কার্ড আলাদা-আলাদা হলো, ঘরের সংখ্যা/নাম/
           সেভ-লজিক এক অক্ষরও বদলায়নি। */
        val fullWidth = setOf("Full Name", "Address", "Emergency Contact", "Notes")
        // 🔗 সেকশনের নাম ইচ্ছে করে ওয়েবের profile.js-এর profEdit()-এর panel() নামগুলোর
        // সাথে হুবহু মেলানো (Personal Details/Employment/Contact/Emergency Contact/
        // Identification/Notes) — TK-এরই আগে পাশ করা সেই ডিজাইন, দুই প্ল্যাটফর্মে
        // একই ভাগ যাতে কখনো গুলিয়ে না যায়।
        val sections = listOf(
            "Personal Details" to listOf(fieldList[0], fieldList[4], fieldList[5], fieldList[6], fieldList[7]),
            "Employment" to listOf(fieldList[1], fieldList[2], fieldList[3]),
            "Contact" to listOf(fieldList[8], fieldList[9]),
            "Emergency Contact" to listOf(fieldList[10], fieldList[11]),
            "Identification" to listOf(fieldList[12], fieldList[13]),
            "Notes" to listOf(fieldList[14])
        )
        fun freshRow(): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        fun spacerCell() = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
        }
        for ((sectionTitle, sectionFields) in sections) {
            col.addView(TextView(this).apply {
                text = sectionTitle.uppercase(Locale.US); textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#6B7A70"))
                setPadding(dp(4), dp(10), dp(4), dp(5))
            })
            val sheet = ModuleUi.card(this)
            col.addView(sheet)
            var pendingRow: LinearLayout? = null
            for (trip in sectionFields) {
                val (icon, label, field) = trip
                lockField(field)
                val cellView = fieldRow(icon, label, field)
                cellView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (label in fullWidth) {
                    pendingRow?.let { pr -> pr.addView(spacerCell()); sheet.addView(pr) }
                    pendingRow = null
                    val r = freshRow(); r.addView(cellView); sheet.addView(r)
                } else if (pendingRow == null) {
                    pendingRow = freshRow()
                    pendingRow!!.addView(cellView)
                } else {
                    pendingRow!!.addView(cellView)
                    sheet.addView(pendingRow!!)
                    pendingRow = null
                }
            }
            pendingRow?.let { pr -> pr.addView(spacerCell()); sheet.addView(pr) }
        }

        // V1660 — Master-only, narrowly scoped cross-branch RMP calling permission.
        // This does NOT change the staff's home branch or grant patient/payment/
        // commission/edit/delete access. Every permission change needs confirmation
        // plus a mandatory reason and is audited server-side.
        val accessCard = ModuleUi.card(this)
        accessCard.addView(TextView(this).apply {
            text = "RMP CALLING ACCESS"
            textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#6B7A70"))
            setPadding(dp(4), dp(4), dp(4), dp(8))
        })
        accessCard.addView(ModuleUi.button(this, "Manage RMP Calling Access") {
            manageRmpCallingAccess(code, branch.text.toString())
        })
        col.addView(accessCard)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(14) }
        }
        val backBtn = ModuleUi.button(this, "Back") { renderList() }
        backBtn.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
        backBtn.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.WHITE)
            setStroke(dp(1), android.graphics.Color.parseColor("#CFE9D8"))
        }
        backBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
        // 🔴 B316 (03.08.2026, TK-নির্দেশ — "Save ফাঁকা ডেটা দিয়ে প্রোফাইল
        // ওভাররাইট করতে পারে"): আগে Save বোতাম শুরু থেকেই সক্রিয় থাকত, অথচ
        // নিচের প্রিফিল-Thread আলাদাভাবে, ধীরে, নেটওয়ার্কে চলত — কেউ দ্রুত Save
        // চাপলে সব ফিল্ড তখনো ফাঁকা থাকত, আর সেই ফাঁকা মানই আসল প্রোফাইলের
        // উপর সেভ হয়ে যেত। এখন Save ডিফল্টে বন্ধ (ধূসর), প্রোফাইল সফলভাবে
        // লোড হওয়ার পরেই চালু হয়। লোড ব্যর্থ হলে Save বন্ধই থাকে, বদলে
        // "⟳ Retry" দেখায়।
        val saveBtn = ModuleUi.button(this, "Loading...") {}
        saveBtn.isEnabled = false
        saveBtn.alpha = 0.5f
        fun paintSaveReady() {
            saveBtn.text = "Save"; saveBtn.isEnabled = true; saveBtn.alpha = 1f
        }
        fun doSave() {
            val row = JSONObject().put("person_code", code)
                .put("full_name", name.text.toString()).put("branch", branch.text.toString())
                .put("designation", designation.text.toString())
                .put("join_date", join.text.toString()).put("dob", dob.text.toString())
                .put("gender", gender.text.toString()).put("blood_group", bloodGroup.text.toString())
                .put("qualification", qualification.text.toString())
                .put("address", addr.text.toString()).put("alt_mobile", altMobile.text.toString())
                .put("emergency_contact", emg.text.toString()).put("emergency_relationship", emgRel.text.toString())
                .put("gov_id_type", idType.text.toString()).put("notes", notes.text.toString())
                .put("updated_at", nowIso())
            val photoToSave = pendingPhotoDataUrl ?: currentPhotoDataUrl
            if (photoToSave != null) row.put("photo_data", photoToSave)
            val idv = idNum.text.toString().filter { !it.isWhitespace() }
            if (idv.length >= 4) row.put("gov_id_last4", idv.takeLast(4))
            ModuleUi.toast(this, "Saving...")
            Thread {
                val ok = ModuleAuth.upsert("hr", "staff_profiles", row)
                runOnUiThread { ModuleUi.toast(this, if (ok) "Saved" else "Retry"); renderList() }
            }.start()
        }
        saveBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) }
        footer.addView(backBtn); footer.addView(saveBtn)
        col.addView(footer)
        // prefill existing — Save stays disabled until this genuinely succeeds
        fun loadProfile() {
            saveBtn.text = "Loading..."; saveBtn.isEnabled = false; saveBtn.alpha = 0.5f
            Thread {
                val result = ModuleAuth.getRowsChecked("hr", "staff_profiles", "select=*&person_code=eq.$code&limit=1")
                runOnUiThread {
                    if (!result.ok) {
                        // লোড ব্যর্থ — Save কখনো চালু হবে না, শুধু Retry দেখাবে
                        saveBtn.text = "⟳ Retry"; saveBtn.isEnabled = true; saveBtn.alpha = 1f
                        saveBtn.setOnClickListener { loadProfile() }
                        ModuleUi.toast(this, "Could not load profile — tap Retry")
                        return@runOnUiThread
                    }
                    if (result.rows.length() > 0) {
                        val p = result.rows.getJSONObject(0)
                        name.setText(ns(p, "full_name")); branch.setText(ns(p, "branch"))
                        designation.setText(ns(p, "designation"))
                        join.setText(ns(p, "join_date")); dob.setText(ns(p, "dob"))
                        gender.setText(ns(p, "gender")); bloodGroup.setText(ns(p, "blood_group"))
                        qualification.setText(ns(p, "qualification"))
                        addr.setText(ns(p, "address")); altMobile.setText(ns(p, "alt_mobile"))
                        emg.setText(ns(p, "emergency_contact")); emgRel.setText(ns(p, "emergency_relationship"))
                        idType.setText(ns(p, "gov_id_type")); notes.setText(ns(p, "notes"))
                        val cloudPhoto = ns(p, "photo_data")
                        // পুরনো আলাদা Staff Photos-এ এই ফোনে রাখা ছবি থাকলে তা হারাবে না:
                        // cloud profile photo না থাকলেই শুধু mobile-keyed পুরনো ছবি fallback।
                        val oldLocalPhoto = UserPhotoStore.get(this, ns(p, "link_mobile")) ?: ""
                        val ph = cloudPhoto.ifBlank { oldLocalPhoto }
                        if (ph.isNotBlank()) {
                            currentPhotoDataUrl = ph
                            photoPreview?.setImageBitmap(PhotoUtils.decodeDataUrl(ph))
                        }
                    }
                    // result.ok == true মানে সার্ভার সত্যিই উত্তর দিয়েছে — সারি
                    // থাকুক (বিদ্যমান প্রোফাইল) বা না থাকুক (নতুন/প্রথমবার), দুটোই
                    // বৈধ, নিরাপদে Save চালু করা যায়।
                    saveBtn.setOnClickListener { doSave() }
                    paintSaveReady()
                }
            }.start()
        }
        loadProfile()
    }

    // ফ্ল্যাট আইকন-সারি: আইকন-বাক্স + লেবেল/মান + স্থায়ী 🔒 (ফিল্ড সবসময় লক
    // দিয়ে শুরু হয় বলে আইকনও স্থির — B304-এর lockField()-এর সাথে মেলানো)।
    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026, ফটো-প্রুফে লক) — View পর্দা এক স্ক্রিনে সব
    // দেখাতে কম্প্যাক্ট করা হলো: প্রতিটা সারি ছোট (কম প্যাডিং), 🔒 তালা-আইকন বাদ
    // ("যেকোনো তথ্য বদলাতে ৩ বার চাপ" ধরনের নির্দেশ-লেখা TK চাননি; ৩-ট্যাপ এডিট
    // আগের মতোই lockField()-এ বহাল)। ⛔ EditText/এডিট/সেভ লজিক একটুও বদলায়নি।
    private fun fieldRow(icon: String, label: String, field: android.widget.EditText): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(9), dp(7), dp(6), dp(7))
        }
        val iconBox = TextView(this).apply {
            text = icon; textSize = 13f; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat(); setColor(android.graphics.Color.parseColor("#EAF6EE"))
            }
        }
        val col2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(9); marginEnd = dp(2) }
        }
        col2.addView(TextView(this).apply {
            text = label; textSize = 9.5f; setTextColor(android.graphics.Color.parseColor("#8A9A90"))
        })
        field.apply {
            textSize = 13.5f
            setTextColor(android.graphics.Color.parseColor("#1C2B22"))
            background = null
            setPadding(0, dp(1), 0, 0)
            minHeight = 0
        }
        col2.addView(field)
        row.addView(iconBox); row.addView(col2)
        return row
    }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date())
    }

    // ---------- MASTER: salary ----------
    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026, ধাপে ধাপে ফটো-প্রুফে লক) — Salary পর্দা সহজ করা:
    // উপরে মাসিক বেতন (৩-ট্যাপে এডিট), তারপর "কোন মাস পর্যন্ত দেওয়া / এই মাসে বাকি",
    // "এই মাসের বেতন দিন" বোতাম, সাম্প্রতিক পেমেন্ট ও "See all payments"। মাসের নাম
    // ইংরেজিতে; কোনো নির্দেশ-লেখা নয়।
    // ⛔ টাকা সেভ/রেকর্ডের আসল কল আগেরটাই — salary_config upsert · salary_payments
    //    insert (হুবহু), শুধু পেমেন্টে নতুন `for_month` ট্যাগ যোগ। নতুন যা দেখানো হয়
    //    (paid up to / this-month due) তা শুধু **পড়ার হিসাব** — ভুল হলেও টাকা/রেকর্ড ভাঙে না।
    // হিসাব (আন্দাজ নয়): প্রতিটা পেমেন্ট "কোন মাসের" = `for_month` থাকলে সেটা, নইলে
    //    `paid_on`-এর মাস (তাই পুরনো ট্যাগ-বিহীন পেমেন্টও ধরা পড়ে, TK: লাস্ট মাস পর্যন্ত
    //    সবার দেওয়া আছে)। "Paid up to" = পেমেন্টগুলোর সবচেয়ে সাম্প্রতিক ঐ মাস।
    /* 💰 V1029 — `openExtra=true` হলে বেতন-পর্দা খোলার সঙ্গে সঙ্গেই
       এক্সট্রা ইনকামের তালিকাটা দেখানো হয় (কার্ডের নতুন বোতামের জন্য)।
       ⛔ ডিফল্ট `false` — পুরনো সব ডাক হুবহু আগের মতোই চলে। */
    /* 💰 V1029 — কার্ডের "Extra Income" বোতামের জন্য: বেতন-পর্দা খুলে
       সঙ্গে সঙ্গেই এক্সট্রা ইনকামের তালিকাটা দেখায়। */
    private var openExtraOnce = false
    private fun salaryExtra(code: String) { openExtraOnce = true; salary(code) }

    /* 🎨🔒 V1181 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"Salary History,
       Salary Statement উপরে ৩ ডট থাকবে তার মধ্যে থাকবে · Total Paid Joining Date
       না থাকলেও চলবে এখানে"* + *"salary Setting টা ও ৩ ডটে রাখুন"*।
       ⇒ পর্দার উপরে ডান দিকে একটা ⋮ — তার ভিতরে পাঁচটা: Salary History ·
         Statement · Salary Settings · Total paid · Joining date।
       ⛔ কোনো বোতামের **কাজ** বদলায়নি — একই ফাংশনই ডাকা হয়, শুধু জায়গা।
       ⛔ টাকার একটাও হিসাব ছোঁয়া হয়নি। */
    private var salMenuAction: (() -> Unit)? = null

    private fun salary(code: String) {
        backAction = { renderList() }
        salMenuAction = null
        val col = ModuleUi.screen(this, "")
        col.addView(salHeaderRow("Salary — " + code))
        val box = ModuleUi.card(this)
        col.addView(box)
        /* 🔵🔒 V521 — জমানো তথ্য থাকলে **সঙ্গে সঙ্গে** পর্দা; "Loading..." নয়।
           ⛔ প্রথমবার (বা ১০ মিনিটের পুরনো হলে) আগের মতোই "Loading..."। */
        val cached = salaryCacheLoad(code)
        /* 🔵🔒 V1250 (০৯.০৯.২০২৬, TK-রিপোর্ট ছবিসহ: *"এই স্ক্রিনে ঢুকলেই স্ক্রিনটা
           কাঁপে কেন"*, খাতার সারি ৩৭০) — জমানো তথ্য দিয়ে যেটা আঁকা হলো, তার
           **হুবহু ছাপ** এখানে রাখা হয়। ক্লাউডের উত্তর এসে যদি ঠিক এই তথ্যই দেয়
           (বেশির ভাগ সময় তাই হয়), তখন পর্দাটা **আর দ্বিতীয়বার আঁকা হয় না** —
           ওই দ্বিতীয় আঁকাটাই TK-র চোখে "কাঁপা" হয়ে ধরা পড়ছিল।
           ⛔ ছাপ না মিললে (সত্যিই কিছু বদলেছে) আগের মতোই সঙ্গে সঙ্গে নতুন করে
              আঁকা হয় — বাসি তথ্য এক মুহূর্তও থাকে না। */
        var cacheSig = ""
        if (cached != null) {
            try {
                val cfg = cached.optJSONObject("cfg") ?: JSONObject()
                val cPays = cached.optJSONArray("pays") ?: JSONArray()
                val cJoin = cached.optString("joinDate", "")
                renderSalary(code, box,
                    cfg.optBoolean("salary_enabled", false),
                    cfg.optDouble("salary_amount", 0.0),
                    ns(cfg, "salary_date"),
                    cPays, cJoin)
                // ⛔ ছাপটা **আঁকা সফল হওয়ার পরেই** রাখা হয় — নইলে "Loading..."
                //    লেখাটাই পর্দায় থেকে যেত।
                cacheSig = salSignature(cfg, cPays, cJoin)
            } catch (_: Throwable) {
                box.removeAllViews()
                box.addView(ModuleUi.body(this, "Loading..."))
            }
        } else {
            box.addView(ModuleUi.body(this, "Loading..."))
        }
        // 🔵 V416 (TK-নির্দেশ, ১৭.০৮.২০২৬): "Back বটম নিচে বসবে"।
        //    পর্দাটা ScrollView-এর ভিতরে, তাই শুধু ওজন দিলে হত না — `fillViewport`
        //    চালু করে দিলে ভিতরের কলাম অন্তত পর্দার সমান উঁচু হয়, তখন ওজনওয়ালা
        //    ফাঁকা জায়গাটা Back-কে একদম নিচে ঠেলে দেয়।
        //    ⛔ শুধু এই পর্দায় — `ModuleUi.screen()` (সব পর্দার শেয়ার করা) ছোঁয়া হয়নি।
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.button(this, "Back") { renderList() })
        loadSalary(code, box, cached != null, cacheSig)
    }

    /** 🔵 V416 (TK-নির্দেশ): তারিখ সবসময় 31/12/2026 ধাঁচে।
     *  ⛔ ডেটাবেসে তারিখ আগের মতোই (YYYY-MM-DD) থাকে — শুধু দেখানোর সময় বদলায়। */
    private fun dmy(iso: String): String {
        val t = iso.trim()
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(t) ?: return t
        // 🔴🔒 V936 (TK-নির্দেশ — এক ফরম্যাট): স্ল্যাশ ছিল, এখন প্রজেক্টের বিন্দু।
        return m.groupValues[3] + "/" + m.groupValues[2] + "/" + m.groupValues[1]   // 🔴 V1158
    }

    /** একটা সারি এখনো "বাকি" না "দেওয়া হয়েছে"। পুরনো সারিতে ঘরটা নেই ⇒ দেওয়া হয়েছে। */
    private fun payStatus(p: JSONObject): String =
        ns(p, "status").trim().uppercase(Locale.US).ifBlank { "PAID" }

    /** একটা সারি বেতন না বাড়তি টাকা। পুরনো সারিতে ঘরটা নেই ⇒ বেতন ধরা হয়। */
    private fun payKind(p: JSONObject): String =
        ns(p, "kind").trim().uppercase(Locale.US).ifBlank { "SALARY" }

    private fun salaryCurrentMonth(): String {
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return String.format(Locale.US, "%04d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1)
    }
    private fun salaryMonthLabel(ym: String): String = try {
        val p = ym.split("-"); val m = p[1].toInt()
        arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")[m - 1] + " " + p[0]
    } catch (_: Throwable) { ym }
    /** একটা পেমেন্ট কোন মাসের বেতন: `for_month` থাকলে সেটা, নইলে `paid_on`-এর মাস। */
    private fun salaryPayMonth(p: JSONObject): String {
        val fm = ns(p, "for_month")
        if (fm.isNotBlank()) return fm
        val paid = ns(p, "paid_on")
        return if (paid.length >= 7) paid.substring(0, 7) else salaryCurrentMonth()
    }

    /** 🔵🔒 V418 (TK-অনুমোদিত নিয়ম, ১৭.০৮.২০২৬) — Extra Income আপনা থেকে।
     *  Master স্যালারি পর্দা খুললেই একবার হিসাবটা মিলিয়ে নেওয়া হয়:
     *    • Unexpected Time-এ রেজিস্ট্রেশন + Fee জমা ⇒ ₹১০০ (৫০-৫০ ভাগ)
     *    • ওই রোগীর প্রথম Advance জমা ⇒ আরও ₹৪০০ ⇒ মোট ₹৫০০
     *  ⛔ পুরো হিসাবটা ডেটাবেসের ভিতরে (`hr.incentive_sync`) — অ্যাপ শুধু ডাক দেয়,
     *     তাই ফোন আর ওয়েবে নিয়ম আলাদা হয়ে যাওয়ার সুযোগ নেই।
     *  ⛔ রোগী/এনকোয়ারি/পেমেন্ট — একটাও টেবিলে লেখা হয় না, শুধু পড়া হয়।
     *  ⛔ একবার "দেওয়া হয়েছে" হয়ে গেলে সেই সারি আর কখনো বদলায় না।
     *  ⛔ ডাকটা ব্যর্থ হলেও পর্দা আগের মতোই খোলে — কিছুই ভাঙে না। */
    private fun incentiveSync() {
        try {
            if (!ModuleAuth.isMaster) return
            ModuleAuth.rpc("hr", "incentive_sync", JSONObject())
        } catch (_: Throwable) {}
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — **"একই স্টাফের বারবার কেন লোডিং
    //    দেখাবে? যতবারই ওপেন করি ততবারই লোডিং কেন দেখাবে?"**
    //
    // **আসল কারণ (কোডে যাচাই করা, আন্দাজ নয়):** `loadSalary()` প্রতিবার
    // পর্দা খোলার সময় **চারটে** নেট-কাজ করত —
    //   ১. `incentive_sync` RPC (শুধু Master)
    //   ২. `salary_config` পড়া
    //   ৩. `salary_payments` পড়া
    //   ৪. `staff_profiles` পড়া
    // আর **কোথাও কিছু জমা রাখা হত না**। তাই একই স্টাফ দশবার খুললে দশবারই
    // "Loading..." আর দশবারই চারটে করে নেট-কাজ।
    //
    // **সমাধান — cache-first**, ঠিক যে পদ্ধতি এই প্রজেক্টে আগেই পাশ হয়েছে
    // (`TimelineCache`, V216 §10 — "Report খুললে আলাদা Loading Screen"):
    //   • আগে দেখা তথ্য থাকলে **সঙ্গে সঙ্গে** পর্দা আঁকা হয় — "Loading..." নয়।
    //   • পিছনে আসল পড়া চলে; নতুন তথ্য এলে পর্দা নিজে থেকেই হালনাগাদ হয়।
    //   • জমানো তথ্য না থাকলে (প্রথমবার) আগের মতোই "Loading..."।
    //
    // ⛔ **টাকার হিসাব এক পয়সাও বদলায়নি** — জমানো তথ্য শুধু **দেখানোর** জন্য;
    //    Add/Cancel/সেভ সব আগের মতোই আসল পড়ার উপরেই চলে।
    // ⛔ **বাসি তথ্য দেখানোর ভয় নেই**: জমানো তথ্য সর্বোচ্চ ১০ মিনিট পুরনো
    //    হতে পারে, আর আসল পড়া শেষ হলেই সেটা মুছে গিয়ে নতুনটা বসে।
    // ⛔ পড়া **ব্যর্থ** হলে জমানো তথ্যই থাকে (আগে পুরো পর্দা ফাঁকা হয়ে
    //    "weak internet" দেখাত) — TK-র কাজ থামে না।
    // ⛔ Supabase-এ **বাড়তি একটাও query নেই** — বরং কম, কারণ `incentive_sync`
    //    RPC এখন প্রতি ৫ মিনিটে একবারের বেশি চলে না।
    // ══════════════════════════════════════════════════════════════════════
    private fun salaryCachePrefs() =
        getSharedPreferences("piles_clinic_salary_cache_v1", MODE_PRIVATE)

    private fun salaryCacheSave(code: String, cfg: JSONObject, pays: JSONArray, joinDate: String) {
        try {
            val root = JSONObject()
                .put("savedAt", System.currentTimeMillis())
                .put("cfg", cfg)
                .put("pays", pays)
                .put("joinDate", joinDate)
            salaryCachePrefs().edit().putString("sal_$code", root.toString()).apply()
        } catch (_: Throwable) { /* জমানো শুধু সুবিধার জন্য — ব্যর্থ হলে কিছুই যায় আসে না */ }
    }

    /** জমানো তথ্য, নইলে null। ১০ মিনিটের পুরনো হলে ব্যবহার করা হয় না। */
    private fun salaryCacheLoad(code: String): JSONObject? = try {
        val raw = salaryCachePrefs().getString("sal_$code", null)
        val root = if (raw.isNullOrBlank()) null else JSONObject(raw)
        val savedAt = root?.optLong("savedAt", 0L) ?: 0L
        if (root == null || savedAt <= 0L ||
            System.currentTimeMillis() - savedAt > 10L * 60L * 1000L) null else root
    } catch (_: Throwable) { null }

    /** 🔵🔒 V521: এই স্টাফের কিছু লেখা হলো (বেতন যোগ / সেটিং বদল / Extra) —
     *  জমানো তথ্য এখনই মুছে ফেলা হয়, নইলে পরেরবার পর্দা খুললে **পুরনো**
     *  তথ্য দেখিয়ে দিত আর TK ভাবতেন কাজটা হয়নি।
     *  ⛔ মুছে দিলে পরের বার শুধু একবার "Loading..." দেখায় — কিন্তু তথ্য
     *     সবসময় ঠিক থাকে। সঠিকতা আগে, গতি তার পরে। */
    private fun salaryCacheClear(code: String) {
        try { salaryCachePrefs().edit().remove("sal_$code").apply() } catch (_: Throwable) {}
    }

    /** `incentive_sync` একটা **লেখার** RPC — প্রতিবার পর্দা খুললে চালানোর
     *  দরকার নেই। ৫ মিনিটে একবারই যথেষ্ট (পাওনা টাকা এর মধ্যে বদলায় না)।
     *  ⛔ টাকার নিয়ম বদলায়নি — শুধু কত ঘন ঘন হিসাব মেলানো হয়। */
    private fun incentiveSyncThrottled() {
        try {
            if (!ModuleAuth.isMaster) return
            val prefs = salaryCachePrefs()
            val last = prefs.getLong("incSyncAt", 0L)
            val now = System.currentTimeMillis()
            if (now - last < 5L * 60L * 1000L) return
            prefs.edit().putLong("incSyncAt", now).apply()
            // ⛔ RPC-টা আগের ফাংশনই চালায় — টাকার হিসাব মেলানোর কোড একটাই জায়গায়।
            incentiveSync()
        } catch (_: Throwable) {}
    }

    /** 🔵🔒 V1250 — পর্দায় এখন যা আঁকা আছে তার হুবহু ছাপ। দুটো ছাপ এক হলে
     *  আর আঁকার দরকার নেই (পর্দা কাঁপে না)। ⛔ শুধু মেলানোর কাজ — কোনো
     *  টাকার হিসাব বা সাজ এতে বদলায় না। */
    private fun salSignature(cfg: JSONObject, pays: JSONArray, joinDate: String): String =
        cfg.toString() + "\u0001" + pays.toString() + "\u0001" + joinDate

    private fun loadSalary(code: String, box: LinearLayout, hadCache: Boolean = false, shownSig: String = "") {
        Thread {
            incentiveSyncThrottled()
            val cfgR = ModuleAuth.getRowsChecked("hr", "salary_config", "select=*&person_code=eq.$code&limit=1")
            val payR = ModuleAuth.getRowsChecked("hr", "salary_payments", "select=*&person_code=eq.$code&order=paid_on.desc")
            // 🟢 B629: "মাস বেছে স্যালারি যোগ"-এর মাস-তালিকা জয়েনিং ডেট থেকে শুরু হবে।
            //   পড়া ব্যর্থ হলে join_date খালি থাকবে (ফলে ২৪ মাসের ফলব্যাক) — মূল Salary
            //   লোডিং কখনো এর জন্য আটকায় না।
            // 🔴🔒 V496 (TK §৩): একই পড়াতেই `role_kind`-ও আনা হচ্ছে (বাড়তি
            //    cloud-read নয়) — ডাক্তার/ফিল্ড-এর বেতনের পর্দা খুলবেই না।
            val profR = ModuleAuth.getRowsChecked("hr", "staff_profiles", "select=join_date,role_kind&person_code=eq.$code&limit=1")
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                /* 🔴🔒 V496 (TK §৩) — শেষ পাহারা। তালিকায় আগে থেকেই শুধু staff
                   দেখানো হয়, তবু অন্য কোনো পথ (যেমন "Salary Due" reminder-এর
                   `salaryFor`) দিয়ে ডাক্তার/ফিল্ড-এর কোড এলে এখানেই থেমে যাবে।
                   ⛔ পড়া ব্যর্থ হলে আটকানো হয় না (নইলে দুর্বল ইন্টারনেটে
                      আসল staff-ও বেতন দেখতে পেতেন না) — তখন উপরের
                      "weak internet" বার্তাই দেখা যাবে। */
                if (profR.ok && profR.rows.length() > 0 &&
                    !com.tkbiswas.pilesclinic.native.RoleRules.salaryAppliesToRoleKind(
                        ns(profR.rows.getJSONObject(0), "role_kind"))) {
                    box.removeAllViews()
                    box.addView(ModuleUi.body(this, com.tkbiswas.pilesclinic.native.NoBengali.s(
                        com.tkbiswas.pilesclinic.native.RoleRules.NO_SALARY_MSG)))
                    return@runOnUiThread
                }
                if (!cfgR.ok || !payR.ok) {
                    /* 🔵🔒 V521: আগে পর্দায় কিছু দেখানো থাকলে সেটা **মোছা হবে না** —
                       নইলে জমানো ঠিক তথ্যটা মুছে গিয়ে শুধু ভুলের বার্তা থাকত।
                       ⛔ কিছুই দেখানো না থাকলে আগের মতোই বার্তাটাই দেখায়। */
                    if (hadCache) {
                        ModuleUi.toast(this, "Could not refresh now — showing last known.")
                        return@runOnUiThread
                    }
                    box.removeAllViews()
                    box.addView(ModuleUi.body(this, "⚠️ Could not load now — weak internet. Data is safe; open again when online."))
                    return@runOnUiThread
                }
                val cfg = if (cfgR.rows.length() > 0) cfgR.rows.getJSONObject(0) else JSONObject()
                val joinDate = if (profR.ok && profR.rows.length() > 0) ns(profR.rows.getJSONObject(0), "join_date") else ""
                // 🔵 V521: পরেরবার যেন সঙ্গে সঙ্গে দেখানো যায়
                salaryCacheSave(code, cfg, payR.rows, joinDate)
                /* 🔵🔒 V1250 — ক্লাউড ঠিক সেই তথ্যই দিল যা পর্দায় আঁকা আছে
                   ⇒ আবার আঁকা মানে শুধু একটা ঝাঁকুনি, আর কিছু নয়। থামা হলো।
                   ⛔ এক অক্ষর আলাদা হলেই নিচের আঁকাটা আগের মতোই চলে। */
                if (shownSig.isNotEmpty() &&
                    salSignature(cfg, payR.rows, joinDate) == shownSig) return@runOnUiThread
                renderSalary(code, box,
                    cfg.optBoolean("salary_enabled", false),
                    cfg.optDouble("salary_amount", 0.0),
                    ns(cfg, "salary_date"),
                    payR.rows, joinDate)
            }
        }.start()
    }

    /**
     * ⏱️ V1166 — ওই স্টাফের ওই মাসের হাজিরা পড়ে **ঘণ্টা হিসাবে বেতন কত হত**
     * সেটা লাইনে বসায়। ⛔ শুধু দেখানো — কোনো টাকা লেখা/বদলানো হয় না।
     */
    /* 💰🔒 V1178 (০৭.০৯.২০২৬, TK-অনুমোদিত ধাপ ২) — TK নিজে সেপ্টেম্বরের
       সংখ্যাটা মিলিয়ে দেখে (COB-UTTAMA — ৪৪ঘ ৫২মি · ₹১,৪৯৬, হাতে গুনে হুবহু
       মিলেছে) বলেছেন: *"হ্যাঁ, সেপ্টেম্বর থেকেই চালু হবে"*।

       ⇒ এখন **"এই মাসে বাকি" ঘণ্টা হিসাবেই** আসে (সেপ্টেম্বর ২০২৬ থেকে):
          বাকি = ঘণ্টার হিসাবে প্রাপ্য − ওই মাসে ইতিমধ্যে দেওয়া।

       ⛔ **শেষ-ভরসা:** হাজিরা আনা না গেলে (নেট/অনুমতি) সংখ্যাটা **ছোঁয়াই হয় না** —
          তখন আগের নিয়মেই সেট করা বেতন ধরে বাকি দেখায়। ভুল অঙ্ক কখনো বসে না।
       ⛔ সেপ্টেম্বরের আগের কোনো মাস ছোঁয়া হয় না।
       ⛔ দেওয়া টাকার সারি (`hr.salary_payments`) এক অক্ষরও বদলায় না — শুধু
          "বাকি কত" দেখানোর অঙ্কটা। */
    private var hourPayThisMonth: Double? = null
    private var salaryDueRow: LinearLayout? = null
    private var salaryPaidThisMonth: Double = 0.0

    private fun loadHourSalaryInto(code: String, amount: Double, ym: String, row: LinearLayout) {
        if (amount <= 0.0) { row.visibility = android.view.View.GONE; return }
        Thread {
            val from = "$ym-01"
            val end = try {
                val p = ym.split("-"); val y = p[0].toInt(); val m = p[1].toInt()
                if (m >= 12) String.format(Locale.US, "%04d-01-01", y + 1)
                else String.format(Locale.US, "%04d-%02d-01", y, m + 1)
            } catch (_: Throwable) { "$ym-31" }
            val r = try {
                ModuleAuth.getRowsChecked(
                    "wn", "notebook_days",
                    /* 🏠 V1180 — Work From Home দিনও চেনা দরকার (৭ ঘণ্টা ধরা হয়)। */
                    "select=work_date,check_in,check_out,is_leave,is_wfh&staff_code=eq.$code" +
                        "&work_date=gte.$from&work_date=lt.$end"
                )
            } catch (_: Throwable) { null }
            val res = HourSalary.compute(if (r != null && r.ok) r.rows else null, amount, ym)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (r == null || !r.ok) { row.visibility = android.view.View.GONE; return@runOnUiThread }
                /* ⛔ নিজে মেপে ধরা: নিয়মটা চালু **আগামী মাস থেকে**, অথচ পর্দায়
                   দেখানো হচ্ছে **এই মাসের** হিসাব (TK যেন আগে থেকে মিলিয়ে
                   দেখতে পারেন)। তাই চালু হওয়ার আগে লেবেলে স্পষ্ট লেখা থাকে
                   যে এটা শুধু আগাম দেখা — নইলে TK ভাবতেন সংখ্যাটা ওই মাসের। */
                val started = ym >= HourSalary.startsFrom()
                (row.getChildAt(0) as? TextView)?.text =
                    if (started) "By hours \u00b7 " + salaryMonthLabel(ym)
                    else "By hours \u00b7 " + salaryMonthLabel(ym) +
                        " (preview \u2014 counts from " + salaryMonthLabel(HourSalary.startsFrom()) + ")"
                (row.getChildAt(1) as? TextView)?.apply {
                    text = money(res.payable) + "  \u00b7  " +
                        HourSalary.hoursText(res.workedMinutes) + " of " + res.monthHours.toInt() + "h"
                    setTextColor(android.graphics.Color.parseColor("#0E6E8C"))
                }
                /* 💰 V1178 — নিয়ম চালু হওয়া মাস থেকে "এই মাসে বাকি"-ও
                   ঘণ্টা হিসাবেই। ⛔ এর আগের মাসে হাত পড়ে না। */
                if (started) {
                    hourPayThisMonth = res.payable
                    val dueNow = maxOf(0.0, res.payable - salaryPaidThisMonth)
                    (salaryDueRow?.getChildAt(1) as? TextView)?.apply {
                        text = if (dueNow <= 0.0) "Paid" else "Due " + money(dueNow)
                        setTextColor(android.graphics.Color.parseColor(
                            if (dueNow <= 0.0) "#0A7C3F" else "#B42318"))
                    }
                }
            }
        }.start()
    }

    private fun renderSalary(code: String, box: LinearLayout, enabled: Boolean, amount: Double, salaryDate: String, pays: JSONArray, joinDate: String) {
        box.removeAllViews()
        val cur = salaryCurrentMonth()
        var latestMonth = ""
        var paidThisMonth = 0.0
        var extraTotal = 0.0
        var extraDue = 0.0
        var salaryTotal = 0.0
        for (i in 0 until pays.length()) {
            val p = pays.getJSONObject(i)
            // 🔵 V416: বাড়তি টাকা (EXTRA) বেতনের হিসাবে ধরা হয় না — নইলে
            //    "এই মাসে বাকি কত" ভুল দেখাত। TK-কে আগেই জানানো হয়েছিল।
            if (payKind(p) == "EXTRA") {
                // 🔵 V417: "বাকি" (DUE) টাকা এখনো দেওয়া হয়নি — তাই "দেওয়া হয়েছে"-তে ধরা যাবে না।
                if (payStatus(p) == "DUE") extraDue += p.optDouble("amount", 0.0)
                else extraTotal += p.optDouble("amount", 0.0)
                continue
            }
            // ⛔ বেতনের সারি সবসময় দেওয়া-হয়েছে ধরা হয় (DUE শুধু Extra-তেই ব্যবহার হয়)।
            salaryTotal += p.optDouble("amount", 0.0)
            val mth = salaryPayMonth(p)
            if (mth > latestMonth) latestMonth = mth
            if (mth == cur) paidThisMonth += p.optDouble("amount", 0.0)
        }
        val active = enabled && amount > 0
        val due = if (active) maxOf(0.0, amount - paidThisMonth) else 0.0

        /* 🎨 V417গ (TK-অনুমোদিত "মডেল ৩", ১৭.০৮.২০২৬) — *"৩ নম্বর বসান · সব ইংরেজিতে
           হবে বাংলা লেখা থাকবে না"*। পর্দাটা এখন তিনটে বাক্সে:
             বাক্স ১ = Salary          (মাসিক · এই মাস · মোট দেওয়া · দুটো বোতাম)
             বাক্স ২ = Extra Income    (দেওয়া · বাকি · দুটো বোতাম)
             বাক্স ৩ = Salary Settings (একটাই বোতাম)
           ⛔ একটাও হিসাব বদলায়নি — উপরের গণনা হুবহু আগেরটাই।
           ⛔ পর্দায় বাংলা লেখা নেই, নির্দেশ/সাহায্য-লাইনও নেই (TK-এর স্থায়ী নিয়ম)।
           ⛔ ওয়েবেও হুবহু একই সাজ। */

        // ───────── বাক্স ১ · Salary ─────────
        box.addView(salSectionTitle("Salary", "#0A5C33"))
        if (active) {
            box.addView(salaryStatusRow("Monthly", money(amount) + (if (salaryDate.isNotBlank()) " · day $salaryDate" else ""), "#0A5C33"))
            /* 💰 V1178 — এই সারিটাই পরে ঘণ্টার হিসাবে বদলে যায় (উপরে দেখুন)।
               ⛔ প্রথমে আগের নিয়মেই বসে, তাই হাজিরা না এলে কিছুই খারাপ হয় না। */
            val dueRow = salaryStatusRow(salaryMonthLabel(cur),
                if (due <= 0.0) "Paid" else "Due " + money(due),
                if (due <= 0.0) "#0A7C3F" else "#B42318")
            salaryDueRow = dueRow
            salaryPaidThisMonth = paidThisMonth
            hourPayThisMonth = null
            box.addView(dueRow)
            box.addView(salaryStatusRow("Paid up to", if (latestMonth.isNotBlank()) salaryMonthLabel(latestMonth) else "—", "#0A7C3F"))
            /* ⏱️🔒 V1166 (০৭.০৯.২০২৬, TK-র সঙ্গে পুরো আলোচনা করে ঠিক হওয়া নিয়ম —
               খাতার সারি ২৭০) — **ঘণ্টা হিসাবে বেতন কত হত**, এখানে দেখানো হয়।
               ⛔ TK-এর স্পষ্ট নির্দেশ: *"প্রথমে শুধু দেখানো, টাকা কাটা নয়"* ⇒
                  এই লাইনটা **কোনো টাকা বদলায় না** — উপরের Due · Paid · সব
                  হিসাব হুবহু আগের নিয়মেই চলে, এক পয়সাও নড়ে না।
               ⛔ TK: *"বিগত দিনের হিসাব ধরবেন না, আগামী মাস থেকে হবে"* ⇒ লাইনেই
                  লেখা থাকে কোন মাস থেকে নিয়মটা চালু হবে।
               ⛔ একটাই ছোট পড়া (ওই স্টাফের ওই মাসের হাজিরা, তিনটে ঘর) — পর্দা
                  আঁকা এর জন্য থামে না, ব্যর্থ হলে লাইনটা শুধু বসে না। */
            val hourRow = salaryStatusRow("By hours", "…", "#5B6B81")
            box.addView(hourRow)
            loadHourSalaryInto(code, amount, cur, hourRow)
        } else {
            box.addView(salaryStatusRow("Monthly", "Not set", "#B42318"))
        }
        /* 🎨 V1181 — "Total paid" ও "Joining date" এখন ⋮-এর ভিতরে (TK: *"না
           থাকলেও চলবে এখানে"*) — মুছে ফেলা হয়নি, শুধু সরানো হয়েছে। */

        /* 🔵 V417খ (TK-নির্দেশ): *"Add Salary & Payment History এক লাইনে থাকবে পাশাপাশি
           বক্স"* ⇒ দুটো সমান বাক্স এক লাইনে।
           ⛔ স্যালারি চালু না থাকলে "Add Salary" আসে না — তখন "Payment History"
              নিজেই পুরো লাইন নেয়, ফাঁকা বাক্স বসে না।
           🟢 B629: যেকোনো (পুরনো) মাসের স্যালারি যোগ — Master নিজে জয়েনিং ডেট থেকে
              History ভরতে পারবেন।
           🔵 V417: Payment History নিজে থেকে খোলা থাকে না — চাপলে Statement খোলে। */
        val btnAddSalary = if (active) salOutlineButton("Add Salary", "#0A5C33", "#0A5C33") {
            addSalaryAnyMonth(code, amount, joinDate, pays)
        } else null
        /* 🟣🔒 V961 — TK-নির্দেশ: বেতন ও এক্সট্রার হিস্ট্রি আলাদা। এই বোতামটা
           এখন **শুধু বেতনের** সারিগুলো দেখায়; এক্সট্রার নিজের বোতাম নিচের
           Extra Income বাক্সে বসে। ⛔ কোনো সারি হারায় না — দুটো মিলিয়ে আগের
           সেই একই তালিকা। */
        var salaryCount = 0
        for (i in 0 until pays.length()) if (payKind(pays.getJSONObject(i)) != "EXTRA") salaryCount++
        val btnHistory = salOutlineButton("Salary History (" + salaryCount + ")", "#0A5C33", "#0A5C33") {
            showAllPayments(code, pays, "SALARY")
        }
        /* 🎨 V1181 — "Add Salary" একাই পুরো লাইন; "Salary History" ও
           "🧾 Statement (date to date)" এখন ⋮-এর ভিতরে (নিচে দেখুন)।
           ⛔ `btnHistory` বানানোই থাকল — গোনার হিসাবটা (salaryCount) ওখান
              থেকেই আসে, আর ⋮-এর সারিতেও ঠিক ওই লেখাটাই বসে। */
        if (btnAddSalary != null) box.addView(salPairRow(btnAddSalary, null))
        /* 🗑️🔒 V1051 (TK-নির্দেশ, ০৪.০৯.২০২৬: *"Add salary আর Pay September 2026
           salary — ২টা একই জিনিস, তাহলে এটা বাদ দিন"*) — TK ঠিক বলেছেন: "Add
           Salary" দিয়ে **যেকোনো মাসের** বেতন দেওয়া যায়, তাই এই বোতামটা বাড়তি।
           ⛔ `payForMonth()` মোছা হয়নি — অন্য জায়গা থেকে ওটা ব্যবহার হয়;
              শুধু এই বোতামটা তোলা হলো, টাকার কোনো হিসাব বদলায়নি। */

        // ───────── বাক্স ২ ও ৩ · আলাদা সাদা কার্ডে (মডেল ৩) ─────────
        // ⛔ আগের কার্ডগুলো tag দিয়ে চিনে সরিয়ে তবেই নতুন বসে ⇒ বারবার আঁকলেও
        //    কার্ড জমতে থাকে না। col না পেলে সবটা এই বাক্সেই বসে (কিছু হারায় না)।
        /* 🎨 V1181 — "Salary Settings"-এর নিজের বাক্সটা আর নেই (TK-নির্দেশে
           বোতামটা ⋮-এ গেছে), তাই সেই কার্ডটা এখন **বানানোই হয় না** — নইলে
           একটা ফাঁকা সাদা বাক্স পড়ে থাকত। ⛔ পুরনো কার্ড থাকলে আগের মতোই
           সরিয়ে দেওয়া হয় (বারবার আঁকলে জমে না)। */
        val col = box.parent as? LinearLayout
        val extraBox: LinearLayout
        if (col != null) {
            col.findViewWithTag<android.view.View>(SAL_TAG_EXTRA)?.let { col.removeView(it) }
            col.findViewWithTag<android.view.View>(SAL_TAG_CFG)?.let { col.removeView(it) }
            extraBox = ModuleUi.card(this).apply { tag = SAL_TAG_EXTRA }
            col.addView(extraBox, col.indexOfChild(box) + 1)
        } else {
            extraBox = box
        }

        // 🔵 V416: বেতন ছাড়াও বাড়তি টাকা। ⛔ `kind='EXTRA'` হয়ে জমা হয়, তাই বেতনের
        //    বাকি-হিসাব ছোঁয় না। 🔵 V417: এখনো না-দেওয়া টাকা লাল "Due" হয়ে থাকে।
        /* 💰 V991 (TK-নির্দেশ, ফটো-প্রুফ পাশ) — সোনালি পট্টি ও দুটো টালি।
           ⛔ শুধু সাজ; `extraTotal` ও `extraDue` ঠিক আগের হিসাবেই আসে। */
        extraBox.addView(salGoldHeader("EXTRA INCOME", salMonthName(salaryCurrentMonth())))
        extraBox.addView(salMoneyTiles(money(extraTotal), money(extraDue), extraDue > 0.0))
        val btnAddExtra = salOutlineButton("Add Extra", "#B45309", "#E0A800") { addExtraIncome(code) }
        val btnPayExtra = if (extraDue > 0.0) salOutlineButton("Pay " + money(extraDue), "#0A5C33", "#0A5C33") {
            payExtraDue(code, pays)
        } else null
        extraBox.addView(salPairRow(btnAddExtra, btnPayExtra))
        /* 🟣🔒 V961 — TK: *"এক্সট্রা ইনকামের টাকার উপরে চাপ দিলে কোনো হিস্ট্রি
           দেখতে পাচ্ছি না"*। সত্যিই দেখা যেত না — Paid/Due দুটোই স্রেফ লেখা
           ছিল, চাপা যেত না, আর হিস্ট্রির একটাই বোতামে বেতনের সাথে মেশানো ছিল।
           এখন এখানেই নিজের বোতাম। ⛔ টাকার কোনো অঙ্ক ছোঁয়া হয়নি। */
        var extraCount = 0
        for (i in 0 until pays.length()) if (payKind(pays.getJSONObject(i)) == "EXTRA") extraCount++
        val btnExtraHistory = salOutlineButton("Extra Income History (" + extraCount + ")", "#B45309", "#E0A800") {
            showAllPayments(code, pays, "EXTRA")
        }
        // 💰 V1029 — কার্ডের "Extra Income" বোতাম থেকে এলে তালিকাটা নিজেই খোলে
        if (openExtraOnce) { openExtraOnce = false; extraBox.post { try { showAllPayments(code, pays, "EXTRA") } catch (_: Throwable) {} } }
        /* ⏰🔒 V990 (০৩.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK: *"তারা যদি নাই
           জানতে পারে যে সেই পেশেন্টটা ট্রিটমেন্ট চালু করেছে কিনা, তাহলে তারা
           হিসাবটা পাবে কি করে"*। এই বোতামে স্টাফ নিজের অসময়ের এনকোয়ারিগুলো ও
           প্রতিটার এখনকার ধাপ দেখতে পান; মাস্টার যেকোনো স্টাফেরটা।
           ⛔ টাকার কোনো অঙ্ক এখান থেকে বদলায় না — শুধু দেখা। */
        /* 🎨🔒 V1181 (TK-নির্দেশ, হুবহু): *"Extra income History, My Unexpected
           Enquiry এগুলি পাশাপাশি থাকতে হবে ( তাছাড়া My Unexpected Enquiry এটা
           staff এর তাহলে মাস্টারের ডিসপ্লে তে এরকম নাম কেন থাকবে)"*।
           ⇒ দুটো বোতাম এক লাইনে, আর মাস্টারের পর্দায় "My" থাকে না — কারণ
             মাস্টার তখন **অন্য একজন স্টাফের** এনকোয়ারি দেখছেন, নিজের নয়।
           ⛔ বোতামের কাজ ও রং অপরিবর্তিত; স্টাফের নিজের পর্দায় লেখাটাও হুবহু আগের। */
        val btnUnexpected = salOutlineButton(
            if (ModuleAuth.isMaster) "Unexpected Enquiries" else "My Unexpected Enquiries",
            "#123E8C", "#123E8C") {
            try {
                startActivity(
                    android.content.Intent(this, com.tkbiswas.pilesclinic.native.UnexpectedEnquiryActivity::class.java)
                        .putExtra(
                            com.tkbiswas.pilesclinic.native.UnexpectedEnquiryActivity.EXTRA_MOBILE,
                            com.tkbiswas.pilesclinic.native.UnexpectedIncentive.mobileForCode(code)
                        )
                        .putExtra(
                            com.tkbiswas.pilesclinic.native.UnexpectedEnquiryActivity.EXTRA_NAME,
                            if (ModuleAuth.isMaster) code else ""
                        )
                )
            } catch (_: Throwable) { }
        }
        extraBox.addView(salPairRow(btnExtraHistory, btnUnexpected))

        /* 🎨🔒 V1181 — ⋮-এর তালিকা এখানেই তৈরি হয়, কারণ সংখ্যা ও তারিখগুলো
           (History-র গোনা · মোট দেওয়া · জয়েনিং ডেট) এই জায়গাতেই জানা যায়।
           ⛔ প্রতিটা সারি ঠিক আগের বোতামটাই ডাকে — কাজ এক অক্ষরও বদলায়নি।
           ⛔ "Total paid" ও "Joining date" শুধু দেখার সারি, চাপলে কিছু হয় না। */
        salMenuAction = {
            val items: List<Pair<String, () -> Unit>> = listOf(
                ("Salary History (" + salaryCount + ")") to ({ showAllPayments(code, pays, "SALARY") }),
                "🧾 Statement (date to date)" to ({ statement(code, pays) }),
                "Salary Settings" to ({ editSalaryConfig(code, enabled, amount, salaryDate) }),
                /* 🗓️ V1199 (TK-নির্দেশ) — কত তারিখে কত ঘণ্টা, আলাদা পর্দায়। */
                "Performance Sheet" to ({ attendanceSheet(code, salaryCurrentMonth()) }),
                ("Total paid  ·  " + money(salaryTotal)) to ({ }),
                ("Joining date  ·  " + (if (joinDate.isBlank()) "Not recorded" else dmy(joinDate))) to ({ })
            )
            try {
                val pm = android.widget.PopupMenu(this, salMenuAnchor ?: box)
                items.forEachIndexed { i, (label, _) -> pm.menu.add(0, i, i, label) }
                pm.setOnMenuItemClickListener { mi ->
                    items.getOrNull(mi.itemId)?.second?.invoke(); true
                }
                pm.show()
            } catch (_: Throwable) { }
        }

        /* 🏍️ V978 (TK-নির্দেশ) — বোতামটা এখন **স্টাফ-কার্ডের সারিতেই**
           (Salary · Performance · Fix Attendance-এর পাশে), তাই বেতন-পর্দার
           ভিতরের এই দ্বিতীয় বোতামটা তুলে দেওয়া হলো — একই জিনিস দুই জায়গায়
           থাকলে বিভ্রান্তি হত। ⛔ বেতনের একটাও হিসাব ছোঁয়া হয়নি। */
    }

    /* =====================================================================
       🏆🔒 V419 — STAFF PERFORMANCE (TK-নির্দেশ, ১৭.০৮.২০২৬)
       TK চারটেই দেখতে চেয়েছেন — রোগী আনার কাজ · ফলোআপ ও কল · টাকা আদায় ·
       হাজিরা ও রিপোর্ট। পর্দাও "দুটোই" — সবার তালিকা, আর একজনের পুরো হিসাব।
       ⛔ পুরো গণনা ডেটাবেসের ভিতরে (`hr.staff_performance`) ⇒ ফোন ও ওয়েবে
          সংখ্যা আলাদা হওয়ার সুযোগ নেই, আর এক ডাকে ছোট্ট উত্তর আসে।
       ⛔ একটাও সারি লেখা/বদলানো হয় না — কেবল পড়া। ⛔ শুধু Master।
       ⛔ পর্দায় বাংলা লেখা নেই, নির্দেশ/সাহায্য-লাইনও নেই। */
    private fun perfMonthNow(): String {
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return String.format(Locale.US, "%04d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1)
    }

    /* 🔵 V420 (TK-নির্দেশ: *"daily performance দেখার ব্যবস্থা রাখতে হবে"*) —
       একই পর্দায় দুটোই: `2026-08` = গোটা মাস · `2026-08-17` = শুধু ওই দিন।
       ⛔ ডেটাবেসের একই ফাংশন, একই সংখ্যা-নিয়ম — শুধু কতটুকু সময় সেটা বদলায়। */
    private fun perfIsDay(k: String): Boolean = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(k.trim())

    private fun perfDayNow(): String = todayIso()

    /** দেখানোর জন্য: দিন হলে 17/08/2026, মাস হলে August 2026। */
    private fun perfLabel(k: String): String =
        if (perfIsDay(k)) dmy(k) else salaryMonthLabel(k)

    /** এক ডাকে সবার হিসাব। ব্যর্থ হলে `null` — তখন পর্দা সৎ বার্তা দেখায়।
     *  🔴 TK-নির্দেশ (১৭.০৮.২০২৬): *"ডাক্তারদের বাদ দিয়ে দিন"* — এই তালিকা শুধু
     *  কর্মীদের। ডেটাবেসেও একই ছাঁকনি বসানো আছে; এটা দ্বিতীয় স্তর, যাতে পুরনো
     *  ডেটাবেসেও (নতুন SQL না চালালেও) ডাক্তার আর তালিকায় না ওঠেন। */
    /* ═══════════════════════════════════════════════════════════════
       ⚡🔒 V1092 (০৫.০৯.২০২৬, TK: *"যতবার চাপ দেই ততবার লোডিং নেয়"*)
       V1091-এ কার্ডে চাপ দিলেই Performance খোলে, তাই TK এখন বারবার খোলেন —
       আর প্রতিবার সার্ভারে **দুটো ডাক** (`staff_performance` +
       `branch_performance`) যেত, তাই প্রতিবার "Loading..."।

       ⇒ একই মাসের উত্তরটা **৬০ সেকেন্ড** ফোনের মধ্যেই রাখা হয়। ওই সময়ের
         ভিতরে আবার খুললে সঙ্গে সঙ্গে দেখায়, কোনো ডাক যায় না।
       ✅ Egress-এও লাভ — বারবার খুললে আর বারবার নামে না।
       ⛔ ৬০ সেকেন্ড পেরোলে আগের মতোই তাজা হিসাব নামে, তাই সংখ্যা কখনো
          পুরনো হয়ে থাকে না। ⛔ অ্যাপ বন্ধ করলে জমানো কিছুই থাকে না।
       ⛔ গণনার নিয়ম · সংখ্যা · পর্দা — কিছুই বদলায়নি, শুধু বারবার না নামানো।
       ═══════════════════════════════════════════════════════════════ */
    private val perfCache = java.util.HashMap<String, JSONArray>()
    private val perfCacheAt = java.util.HashMap<String, Long>()

    private fun perfFetch(month: String): JSONArray? {
        perfCache[month]?.let { hit ->
            if (System.currentTimeMillis() - (perfCacheAt[month] ?: 0L) < 60_000L) return hit
        }
        return try {
            val r = ModuleAuth.rpc("hr", "staff_performance", JSONObject().put("p_month", month))
            if (!r.ok) return null
            val all = JSONArray(r.body)
            val out = JSONArray()
            for (i in 0 until all.length()) {
                val x = all.optJSONObject(i) ?: continue
                if (ns(x, "person_code").uppercase(Locale.US).startsWith("DR-")) continue
                out.put(x)
            }
            /* 🔴🔒 V428 (TK-নির্দেশ ১৭.০৮.২০২৬: *"8514002200 — ওটা ব্রাঞ্চ হিসাবেই
               গন্য হোক"*) — চেম্বারের সাধারণ নম্বরে করা কাজ কোনো staff-এর নামে ওঠে
               না, তাই Collection কম দেখাত (আজ কোচবিহারে ₹৭,৫০০ বাদ পড়ছিল)। এখন
               সেগুলো **ব্রাঞ্চের নিজের সারি** হয়ে তালিকার শেষে আসে, ফলে যোগফল মেলে।
               ⛔ পুরনো `staff_performance`-এ হাত দেওয়া হয়নি — এটা আলাদা ফাংশন।
               ⛔ ডাক ব্যর্থ হলে বা সব শূন্য হলে কিছুই যোগ হয় না, তালিকা আগের মতোই। */
            try {
                val rb = ModuleAuth.rpc("hr", "branch_performance", JSONObject().put("p_month", month))
                if (rb.ok) {
                    val bAll = JSONArray(rb.body)
                    for (i in 0 until bAll.length()) {
                        val b = bAll.optJSONObject(i) ?: continue
                        val busy = perfInt(b, "enquiry_count") + perfInt(b, "registration_count") +
                            perfInt(b, "treatment_count") + perfInt(b, "rmp_added")
                        val money = perfDbl(b, "cash_collected") + perfDbl(b, "online_collected")
                        if (busy > 0 || money > 0.0) out.put(b)
                    }
                }
            } catch (_: Throwable) { }
            // ⚡ V1092 — সফল উত্তরটাই শুধু জমা থাকে (ব্যর্থ হলে কিছুই জমে না)
            perfCache[month] = out
            perfCacheAt[month] = System.currentTimeMillis()
            out
        } catch (_: Throwable) { null }
    }

    private fun perfInt(o: JSONObject, k: String): Int = try { o.optInt(k, 0) } catch (_: Throwable) { 0 }
    private fun perfDbl(o: JSONObject, k: String): Double = try { o.optDouble(k, 0.0) } catch (_: Throwable) { 0.0 }

    /* 🔧 V421খ (TK-নির্দেশ, ১৭.০৮.২০২৬): *"Day month আবার ক্যালেন্ডার — তিনটে
       রাখার দরকার নেই · Month & calendar থাকবে · ক্যালেন্ডারে চাপ দিলে pop up
       ক্যালেন্ডার খুলবে · তারিখ পছন্দ করলে অটোমেটিক সেই তারিখের পারফরম্যান্স ·
       অন্যথায় ডিফল্ট আজকের · আর এগুলো হেডারে থাকবে"*
       ⇒ শিরোনামের **একই লাইনে** দুটোই — [Month] আর তারিখের বাক্স। তারিখে চাপলে
         ফোনের নিজের পপ-আপ ক্যালেন্ডার (DatePickerDialog) খোলে, তারিখ বাছলেই
         সঙ্গে সঙ্গে ওই দিনের হিসাব।
       ⛔ ক্যালেন্ডার-ইমোজি কোথাও নেই (TK-এর স্থায়ী নিয়ম)। ⛔ ডিফল্ট = আজকের দিন। */
    private fun perfHeader(title: String, curKey: String, onPick: (String) -> Unit): LinearLayout {
        val isDay = perfIsDay(curKey)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        fun chip(text: String, on: Boolean, onClick: () -> Unit) = TextView(this).apply {
            this.text = text; textSize = 12.5f
            gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(11), dp(7), dp(11), dp(7))
            setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#0A5C33"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(android.graphics.Color.parseColor(if (on) "#0A5C33" else "#FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#0A5C33"))
            }
            isClickable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(6) }
        }
        row.addView(chip("Month", !isDay) { onPick(perfMonthNow()) })
        row.addView(chip(perfLabel(curKey), isDay) {
            // ফোনের নিজের পপ-আপ ক্যালেন্ডার — অ্যাপে আগে থেকেই এই প্রমাণিত পথ ব্যবহার হয়।
            val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            try {
                val d = if (isDay) curKey else todayIso()
                val q = d.split("-")
                cal.set(q[0].toInt(), q[1].toInt() - 1, q[2].toInt())
            } catch (_: Throwable) {}
            android.app.DatePickerDialog(this, { _, y, mth, day ->
                onPick(String.format(Locale.US, "%04d-%02d-%02d", y, mth + 1, day))
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
               cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        })
        return row
    }

    /** ---- ১) সবার তালিকা ---- */
    // =====================================================================
    /* 📱🔒 V813 — ছোট রঙিন ট্যাগ (শুধু দেখানোর, কোনো কাজ করে না)।
       V771-এ এটা আলাদা "Phone Versions" পর্দায় ছিল; TK-র নির্দেশে সেই পর্দা
       উঠে গেছে, তাই ট্যাগটাই এখন Performance-এর সারিতে বসে। */
    private fun pvChip(text: String, textHex: String, bgHex: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(textHex))
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(android.graphics.Color.parseColor(bgHex))
                setStroke(dp(1), android.graphics.Color.parseColor(textHex))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(7) }
        }


    /* 📱🔒 V822 (২৯.০৮.২০২৬) — **V813-এ মুছে ফেলা এই পর্দাটাই হুবহু ফিরিয়ে আনা হলো**
       (git ইতিহাস থেকে, নতুন করে লেখা হয়নি — তাই আচরণ এক অক্ষরও বদলায়নি)।
       কেন: V813-এর পরে **ডাক্তারদের ভার্সন আর কোথাও দেখা যেত না** —
       `hr.staff_performance()` সার্ভারেই ডাক্তার বাদ দেয়। এই পর্দা
       `hr.app_devices_list` পড়ে, যা staff · doctor · field · master **সবাইকেই**
       দেয়। ⛔ শিরোনাম-লেখা আর বোতাম নয় — এখন উপরের 📱 চিহ্ন থেকে খোলে। */
    private fun phoneVersionsScreen() {
        backAction = { renderList() }
        val col = ModuleUi.screen(this, "")
        col.addView(ModuleUi.heading(this, "📱 Phone Versions"))
        col.addView(TextView(this).apply {
            text = "Which phone is running which app version. " +
                "An old version keeps using extra internet, so it must be updated."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(2), 0, dp(8))
        })
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listBox.addView(ModuleUi.body(this, "Loading..."))
        col.addView(listBox)
        col.addView(ModuleUi.button(this, "Back") { renderList() })
        Thread {
            val rows = try {
                val r = ModuleAuth.rpc("hr", "app_devices_list", JSONObject())
                if (r.ok) JSONArray(r.body) else null
            } catch (_: Throwable) { null }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                listBox.removeAllViews()
                if (rows == null) {
                    listBox.addView(ModuleUi.body(this, "Could not load. Please try again."))
                    return@runOnUiThread
                }
                if (rows.length() == 0) {
                    listBox.addView(ModuleUi.body(this, "No one yet."))
                    return@runOnUiThread
                }
                /* সর্বশেষ ভার্সন কোনটা?
                   ⛔ শুধু নিজের বিল্ডের সংখ্যা ধরলে ভুল হতো — মাস্টারের ফোনটাই
                      যদি পুরনো হয়, তবে সবাইকে "Latest" দেখাত। তাই ওয়েবসাইটের
                      `version.json` থেকে জানা সংখ্যাও (থাকলে) ধরা হয়। */
                val latest = maxOf(
                    com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE,
                    com.tkbiswas.pilesclinic.native.AppVersionCheck.newerVersionOrZero(this)
                )
                var old = 0
                var never = 0
                for (i in 0 until rows.length()) {
                    val v = rows.optJSONObject(i)?.optInt("app_version_code", 0) ?: 0
                    if (v <= 0) never++ else if (v < latest) old++
                }
                if (old > 0 || never > 0) {
                    val warn = ModuleUi.card(this)
                    warn.addView(TextView(this).apply {
                        text = "⚠️ " + (old + never) + " phone" + (if (old + never == 1) "" else "s") +
                            " not on the latest version"
                        textSize = 14f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#B3261E"))
                    })
                    warn.addView(TextView(this).apply {
                        text = "Old: " + old + "   ·   Never opened: " + never +
                            "\nPlease install V" + latest + " on these phones."
                        textSize = 12f
                        setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                        setPadding(0, dp(3), 0, 0)
                    })
                    listBox.addView(warn)
                }
                for (i in 0 until rows.length()) {
                    val x = rows.optJSONObject(i) ?: continue
                    val v = x.optInt("app_version_code", 0)
                    val card = ModuleUi.card(this)
                    card.addView(TextView(this).apply {
                        text = ns(x, "full_name").ifBlank { ns(x, "person_code") }
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                    })
                    card.addView(TextView(this).apply {
                        // ⛔ `PeopleAdminRepository.roleLabel()` "master" চেনে না
                        //    (ওখানে master যোগ করাই যায় না), তাই এখানে আলাদা।
                        text = ns(x, "person_code") + " · " + ns(x, "branch") + " · " +
                            (if (ns(x, "role_kind").trim().lowercase() == "master") "Master"
                             else com.tkbiswas.pilesclinic.native.PeopleAdminRepository
                                 .roleLabel(ns(x, "role_kind")))
                        textSize = 12f
                        setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                        setPadding(0, dp(2), 0, dp(6))
                    })
                    card.addView(when {
                        v <= 0 -> pvChip("Never opened the new app", "#5B6B81", "#EEF1F5")
                        v < latest -> pvChip("V" + v + "  ·  OLD — must update", "#B3261E", "#FDECEA")
                        else -> pvChip("V" + v + "  ·  Latest", "#0A5C33", "#E9F7EE")
                    })
                    val seen = pvSeen(ns(x, "app_seen_at"))
                    if (seen.isNotBlank()) {
                        card.addView(TextView(this).apply {
                            text = "Last seen: " + seen
                            textSize = 11.5f
                            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                            setPadding(0, dp(5), 0, 0)
                        })
                    }
                    listBox.addView(card)
                }
            }
        }.start()
    }

    /* ⏰ PostgREST-এর timestamptz আসে `2026-08-28T05:12:33.123456+00:00` রূপে।
       `DateUtil` এই রূপটা চেনে না (মাইক্রোসেকেন্ড + অফসেট), আর অফসেট না ধরলে
       সময় সাড়ে ৫ ঘণ্টা ভুল দেখাত। তাই এখানে নিজেই ঠিকভাবে পড়া হলো, তারপর
       ফোনের নিজের সময়-অঞ্চলে (IST) দেখানো — খাতার লক-করা ছাঁদেই (B76)।
       ⛔ চিনতে না পারলে ফাঁকা ফেরে — পর্দা কখনো ভাঙে না। */
    private fun pvSeen(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            var body = raw.trim()
            var off = "+0000"
            val dot = body.indexOf('.')
            if (dot > 0) {
                var end = dot + 1
                while (end < body.length && body[end].isDigit()) end++
                off = pvOffset(body.substring(end))
                body = body.substring(0, dot)
            } else if (body.length > 19) {
                off = pvOffset(body.substring(19))
                body = body.substring(0, 19)
            }
            val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            val d = f.parse(body + off) ?: return ""
            com.tkbiswas.pilesclinic.native.DateUtil.displayWithTime(d)
        } catch (_: Throwable) { "" }
    }

    private fun pvOffset(tail: String): String {
        val t = tail.trim()
        if (t.isEmpty() || t.equals("Z", true)) return "+0000"
        return t.replace(":", "")
    }

    private fun performanceList(month: String) {
        backAction = { renderList() }
        // 🔧 V421খ: ডিফল্ট **আজকের দিন** (আগে মাস ছিল)।
        val ym = month.ifBlank { perfDayNow() }
        val col = ModuleUi.screen(this, "")
        col.addView(perfHeader("Performance", ym) { k -> performanceList(k) })
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listBox.addView(ModuleUi.body(this, "Loading..."))
        col.addView(listBox)
        col.addView(ModuleUi.button(this, "Back") { renderList() })
        Thread {
            val rows = perfFetch(ym)
            /* 📱🔒 V813 — একই থ্রেডে ভার্সনের তালিকাও আনা হয় (person_code → version)।
               ⛔ ব্যর্থ হলে ম্যাপ ফাঁকা থাকে — পারফরম্যান্সের পর্দা আগের মতোই
                  পুরোপুরি চলে, একটাও সংখ্যা আটকায় না। মাস্টার না হলে সার্ভার
                  নিজেই খালি তালিকা ফেরায়, তাই কোনো ট্যাগ দেখায় না। */
            val verMap = HashMap<String, Int>()
            var verLatest = 0
            try {
                val vr = ModuleAuth.rpc("hr", "app_devices_list", JSONObject())
                if (vr.ok) {
                    val va = JSONArray(vr.body)
                    for (i in 0 until va.length()) {
                        val o = va.optJSONObject(i) ?: continue
                        val code = ns(o, "person_code").trim().uppercase(Locale.US)
                        if (code.isNotBlank()) verMap[code] = o.optInt("app_version_code", 0)
                    }
                    verLatest = maxOf(
                        com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE,
                        com.tkbiswas.pilesclinic.native.AppVersionCheck.newerVersionOrZero(this)
                    )
                }
            } catch (_: Throwable) { }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                listBox.removeAllViews()
                if (rows == null) { listBox.addView(ModuleUi.body(this, "Could not load. Please try again.")); return@runOnUiThread }
                if (rows.length() == 0) { listBox.addView(ModuleUi.body(this, "No staff yet.")); return@runOnUiThread }
                // 🔴 V427 (TK-নির্দেশ ১৭.০৮.২০২৬: *"কোচবিহার এর সমস্ত staff &
                //    Branch এর নম্বর একের পর এক থাকতে হবে"*) — তালিকা এখন
                //    **ব্রাঞ্চ ধরে সাজানো**, এক ব্রাঞ্চের সবাই পরপর; ভিতরে নাম
                //    অনুসারে। ⛔ শুধু ক্রম — একটাও সংখ্যা বদলায় না। ওয়েবেও একই ক্রম।
                val perfSorted = ArrayList<org.json.JSONObject>(rows.length())
                for (i in 0 until rows.length()) rows.optJSONObject(i)?.let { perfSorted.add(it) }
                // ক্রম: আগে ব্রাঞ্চ, ভিতরে staff-রা নাম অনুসারে, আর ওই ব্রাঞ্চের
                // নিজের সারিটা সবার **শেষে** — ওয়েবেও হুবহু একই ক্রম।
                perfSorted.sortWith(compareBy(
                    { ns(it, "branch").uppercase() },
                    { if (ns(it, "person_code").uppercase(Locale.US).startsWith("BRANCH-")) 1 else 0 },
                    { ns(it, "full_name").ifBlank { ns(it, "person_code") }.uppercase() }
                ))
                for (i in perfSorted.indices) {
                    val x = perfSorted[i]
                    val card = ModuleUi.card(this)
                    card.addView(TextView(this).apply {
                        text = ns(x, "full_name").ifBlank { ns(x, "person_code") }
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                    })
                    /* 📱🔒 V813 — কোড · ব্রাঞ্চ-এর পাশেই ফোনের ভার্সনের ট্যাগ।
                       ⛔ পুরনো লাইনটা হুবহু আগের মতোই আছে, শুধু তার পাশে
                          একটা ট্যাগ যোগ হলো (জানা না থাকলে কিছুই বসে না)। */
                    val idRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(0, dp(2), 0, dp(2))
                    }
                    idRow.addView(TextView(this).apply {
                        text = ns(x, "person_code") + " · " + ns(x, "branch")
                        textSize = 12f
                        setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                    })
                    if (verLatest > 0) {
                        val v = verMap[ns(x, "person_code").trim().uppercase(Locale.US)]
                        if (v != null) {
                            idRow.addView(when {
                                v <= 0 -> pvChip("No app yet", "#B3261E", "#FDECEA")
                                v < verLatest -> pvChip("V" + v + " · old", "#B3261E", "#FDECEA")
                                else -> pvChip("V" + v, "#0A5C33", "#E9F7EE")
                            })
                        }
                    }
                    card.addView(idRow)
                    val tiles = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(7), 0, dp(2))
                    }
                    tiles.addView(perfTile("Enquiry", perfInt(x, "enquiry_count").toString(), false))
                    tiles.addView(perfTile("Regist.", perfInt(x, "registration_count").toString(), false))
                    tiles.addView(perfTile("Treat.", perfInt(x, "treatment_count").toString(), false))
                    /* 🟠🔒 V1077 (০৪.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) — TK:
                       *"আজকের সারাদিনে কে কতগুলো RMP-র নাম এন্ট্রি করল সেটাও যেন
                       বোঝা যায়, বিশেষ করে নতুন নাম"*।
                       ⛔ নতুন কোনো গোনা বানানো হয়নি — `hr.staff_performance`-এর
                          **আগে থেকে থাকা** `rmp_added` ঘরটাই দেখানো হচ্ছে; ওটা
                          গোনে ওই সময়ে ওই স্টাফের `createdBy`-তে বসানো নতুন
                          `doctor_visits` সারি। ভিতরের পর্দায় "RMP added"-এ
                          ঠিক এই সংখ্যাটাই আগে থেকেই দেখানো হয়, তাই দুই পর্দায়
                          কখনো দুরকম উত্তর হবে না।
                       ⛔ SQL লাগেনি · বাকি চারটে ঘরের সংখ্যা/রং এক অক্ষরও বদলায়নি। */
                    tiles.addView(perfTile("New RMP", perfInt(x, "rmp_added").toString(), false, warm = true))
                    tiles.addView(perfTile("Collected",
                        money(perfDbl(x, "cash_collected") + perfDbl(x, "online_collected")), true))
                    card.addView(tiles)
                    val pc = ns(x, "person_code")
                    card.isClickable = true
                    card.setOnClickListener { performanceOne(pc, ym) }
                    listBox.addView(card)
                }
            }
        }.start()
    }

    /** তালিকার ছোট বাক্স — Salary পর্দার টাইলের মতোই। */
    /* 🟠 V1077 — `warm` শুধু নতুন "New RMP" ঘরের জন্য (হলুদ), TK-এর পাশ-করা
       ফটোর হুবহু রং। ডিফল্ট false, তাই পুরনো চারটে ডাক আগের মতোই সবুজ। */
    private fun perfTile(caption: String, value: String, last: Boolean, warm: Boolean = false): LinearLayout {
        val fill = if (warm) "#FFF4E5" else "#F2FBF5"
        val edge = if (warm) "#F3D9AE" else "#D8ECDF"
        val capCol = if (warm) "#8A5A00" else "#5B6B81"
        val valCol = if (warm) "#B45309" else "#0A5C33"
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(3), dp(8), dp(3), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor(fill))
                setStroke(dp(1), android.graphics.Color.parseColor(edge))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { if (!last) rightMargin = dp(6) }
        }
        t.addView(TextView(this@StaffProfileActivity).apply {
            text = caption; textSize = 10f
            gravity = android.view.Gravity.CENTER
            maxLines = 1
            setTextColor(android.graphics.Color.parseColor(capCol))
        })
        t.addView(TextView(this@StaffProfileActivity).apply {
            text = value; textSize = 13.5f
            gravity = android.view.Gravity.CENTER
            maxLines = 1
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(valCol))
            setPadding(0, dp(2), 0, 0)
        })
        return t
    }

    /** ---- ২) একজনের পুরো হিসাব ---- */
    /* 🔧 V421 (TK-রিপোর্ট: *"staff এর Daily Performance কেন দেখা যাচ্ছে না"*) —
       Day/Month বোতাম দুটো আগে **শুধু তালিকার** পর্দায় ছিল। Staff Profiles থেকে
       সরাসরি কারও Performance-এ ঢুকলে দিনের হিসাব দেখার উপায়ই ছিল না।
       এখন এই পর্দাতেও একই দুটো বোতাম। ⛔ গোনার নিয়ম কিছুই বদলায়নি।
       ⛔ `fromList` দিয়ে মনে রাখা হয় Back কোথায় ফিরবে — তালিকা থেকে এলে
          তালিকায়, Staff Profiles থেকে এলে Staff Profiles-এ। */
    // =====================================================================
    // 🔴🔴🔒 V477 (20.08.2026, TK-জরুরি নির্দেশ) — Master-only: যেকোনো
    // স্টাফের আজকের IN/OUT TIME সরাসরি বসানো/ঠিক করা — দুপুর ১২টার সীমা
    // ছাড়াই। আজ সকালের JWT বাগে (V465-এ ঠিক করা) যাদের IN TIME নিঃশব্দে
    // ক্লাউডে সেভ হয়নি, সন্ধ্যায় তাদের হাজিরা হারিয়ে যাওয়া ঠেকাতে।
    // ⛔ একই টেবিলে (wn.notebook_days), স্টাফের নিজের Work Notebook যে
    //    upsert পথ ব্যবহার করে ঠিক সেই একই প্যাটার্নে — নতুন কোনো নিয়ম নয়।
    // =====================================================================
    private fun fixAttendanceDialog(pc: String, fullName: String, mobile: String) {
        val d = resources.displayMetrics.density
        fun dp2(v: Int) = (v * d).toInt()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }.format(java.util.Date())
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp2(20), dp2(10), dp2(20), dp2(4))
        }
        box.addView(TextView(this).apply {
            text = "$fullName ($pc) — $today"
            textSize = 13f; setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, 0, 0, dp2(10))
        })
        box.addView(TextView(this).apply { text = "IN TIME (e.g. 09:15 AM)"; textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#5B6B81")) })
        val inInput = android.widget.EditText(this).apply {
            hint = "Tap to select time — leave blank if not changing"
            val pad = dp2(11); setPadding(pad, pad, pad, pad)
            isFocusable = false
            isCursorVisible = false
            isClickable = true
        }
        box.addView(inInput)
        box.addView(TextView(this).apply { text = "OUT TIME (e.g. 06:30 PM)"; textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#5B6B81")); setPadding(0, dp2(10), 0, 0) })
        val outInput = android.widget.EditText(this).apply {
            hint = "Tap to select time — leave blank if not changing"
            val pad = dp2(11); setPadding(pad, pad, pad, pad)
            isFocusable = false
            isCursorVisible = false
            isClickable = true
        }
        box.addView(outInput)
        // Fix Attendance-এ সময় টাইপ করতে হবে না — ঘড়ি থেকে বেছে নেওয়া হবে।
        fun openTimePicker(target: android.widget.EditText) {
            val now = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            android.app.TimePickerDialog(
                this,
                { _, hour, minute ->
                    val picked = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                    }
                    target.setText(SimpleDateFormat("hh.mm a", Locale.US).format(picked.time))   // 🔴 V1158
                },
                now.get(java.util.Calendar.HOUR_OF_DAY),
                now.get(java.util.Calendar.MINUTE),
                false
            ).show()
        }
        inInput.setOnClickListener { openTimePicker(inInput) }
        outInput.setOnClickListener { openTimePicker(outInput) }
        // hh:mm AM/PM → "HH:mm:ss" (24-ঘণ্টা, notebook_days-এ যেভাবে জমা থাকে)।
        fun to24(raw: String): String? {
            val t = raw.trim().uppercase(Locale.US)
            if (t.isBlank()) return null
            /* 🔴🔒 V1158 — লেখাটা এখন `03.15 PM` ধাঁচে বসে, কিন্তু স্টাফ হাতে
               `3:15 PM`-ও লিখতে পারেন, আর পুরনো বিল্ডের লেখাও ওরকমই।
               ⇒ চারটে ধাঁচই চেষ্টা করা হয়, একটাও হারায় না। */
            for (pat in listOf("hh.mm a", "h.mm a", "hh:mm a", "h:mm a")) {
                try {
                    val parsed = SimpleDateFormat(pat, Locale.US).parse(t) ?: continue
                    return SimpleDateFormat("HH:mm:ss", Locale.US).format(parsed)
                } catch (_: Throwable) { }
            }
            return null
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Fix Attendance"))
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val inTxt = inInput.text.toString().trim()
                val outTxt = outInput.text.toString().trim()
                if (inTxt.isBlank() && outTxt.isBlank()) {
                    android.widget.Toast.makeText(this, "Enter at least one time", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val in24 = if (inTxt.isNotBlank()) to24(inTxt) else null
                val out24 = if (outTxt.isNotBlank()) to24(outTxt) else null
                if ((inTxt.isNotBlank() && in24 == null) || (outTxt.isNotBlank() && out24 == null)) {
                    android.widget.Toast.makeText(this, "Use format hh:mm AM/PM, e.g. 09:15 AM", android.widget.Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                Thread {
                    val existing = try {
                        val r = ModuleAuth.getRowsChecked("wn", "notebook_days", "select=*&staff_code=eq.$pc&work_date=eq.$today&limit=1")
                        if (r.ok && r.rows.length() > 0) r.rows.getJSONObject(0) else null
                    } catch (_: Throwable) { null }
                    val row = existing ?: JSONObject()
                        .put("staff_code", pc).put("staff_mobile", mobile).put("work_date", today)
                        .put("manual_entries", JSONArray())
                    if (!row.has("manual_entries") || row.isNull("manual_entries")) row.put("manual_entries", JSONArray())
                    if (in24 != null) row.put("check_in", in24)
                    if (out24 != null) row.put("check_out", out24)
                    row.put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
                    var ok = try { ModuleAuth.upsertOnConflict("wn", "notebook_days", row, "staff_code,work_date") } catch (_: Throwable) { false }
                    if (!ok) ok = try { ModuleAuth.upsert("wn", "notebook_days", row) } catch (_: Throwable) { false }
                    runOnUiThread {
                        android.widget.Toast.makeText(this, if (ok) "Attendance updated for $fullName" else "Could not save — check connection and try again", android.widget.Toast.LENGTH_LONG).show()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) {} }
    }

    private fun performanceOne(code: String, month: String, fromList: Boolean = month.isNotBlank()) {
        // 🔧 V421খ: ডিফল্ট **আজকের দিন**।
        val ym = month.ifBlank { perfDayNow() }
        backAction = { if (fromList) performanceList(ym) else renderList() }
        // 🔴 V511 (উপরের `perfListBack`-এর বড় নোট দ্রষ্টব্য): এই পর্দা থেকে যত
        //    তালিকা খোলা হয় (Enquiry · Calls · Collection · Reports · RMP),
        //    সবগুলোর Back-এর ঠিকানা **এখানেই** — ডিটেল থেকে যতবারই ফিরুক।
        perfListBack = { performanceOne(code, ym, fromList) }
        val col = ModuleUi.screen(this, "")
        col.addView(perfHeader(code, ym) { k -> performanceOne(code, k, fromList) })
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val first = ModuleUi.card(this)
        first.addView(ModuleUi.body(this, "Loading..."))
        box.addView(first)
        col.addView(box)
        col.addView(ModuleUi.button(this, "Back") { backAction() })
        Thread {
            val rows = perfFetch(ym)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                box.removeAllViews()
                if (rows == null) {
                    box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "Could not load. Please try again.")) })
                    return@runOnUiThread
                }
                var x: JSONObject? = null
                for (i in 0 until rows.length()) {
                    val r = rows.optJSONObject(i) ?: continue
                    if (ns(r, "person_code") == code) { x = r; break }
                }
                val row = x
                if (row == null) {
                    box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "No record for this month.")) })
                    return@runOnUiThread
                }
                val head = ModuleUi.card(this)
                head.addView(TextView(this).apply {
                    text = ns(row, "full_name").ifBlank { code }
                    textSize = 17f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                })
                head.addView(TextView(this).apply {
                    text = code + " · " + ns(row, "branch") + " · " + perfLabel(ym)
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                    setPadding(0, dp(2), 0, 0)
                })
                box.addView(head)

                val c1 = ModuleUi.card(this)
                c1.addView(salSectionTitle("Patient Work", "#0A5C33"))
                c1.addView(salaryStatusRow("Enquiry forms", perfInt(row, "enquiry_count").toString(), "#123A26") { perfShowEnquiryList(code, ym) })
                c1.addView(salaryStatusRow("Registrations", perfInt(row, "registration_count").toString(), "#123A26") { perfShowRegistrationList(code, ym, treatmentOnly = false) })
                c1.addView(salaryStatusRow("Started treatment", perfInt(row, "treatment_count").toString(), "#0A7C3F") { perfShowRegistrationList(code, ym, treatmentOnly = true) })
                box.addView(c1)

                val c2 = ModuleUi.card(this)
                c2.addView(salSectionTitle("Calls", "#0A5C33"))
                c2.addView(salaryStatusRow("Calls from app", perfInt(row, "app_calls").toString(), "#123A26") { perfShowCallsList(code, ym, outside = false) })
                c2.addView(salaryStatusRow("Outside calls", perfInt(row, "outside_calls").toString(), "#123A26") { perfShowCallsList(code, ym, outside = true) })
                c2.addView(salaryStatusRow("RMP added", perfInt(row, "rmp_added").toString(), "#123A26") { perfShowRmpList(code, ym) })
                box.addView(c2)

                val c3 = ModuleUi.card(this)
                c3.addView(salSectionTitle("Money Collected", "#0A5C33"))
                c3.addView(salaryStatusRow("Cash", money(perfDbl(row, "cash_collected")), "#123A26") { perfShowPaymentList(code, ym, online = false) })
                c3.addView(salaryStatusRow("Online", money(perfDbl(row, "online_collected")), "#123A26") { perfShowPaymentList(code, ym, online = true) })
                c3.addView(salaryStatusRow("Total", money(perfDbl(row, "cash_collected") + perfDbl(row, "online_collected")), "#0A7C3F"))
                box.addView(c3)

                val c4 = ModuleUi.card(this)
                c4.addView(salSectionTitle("Attendance & Reports", "#B45309"))
                c4.addView(salaryStatusRow("Days present", perfInt(row, "present_days").toString(), "#123A26") { perfShowAttendanceSheet(code, ns(row, "full_name").ifBlank { code }, ym, fromPerf = true) })
                c4.addView(salaryStatusRow("Daily reports sent", perfInt(row, "reports_sent").toString(), "#123A26") { perfShowReportsList(code, ym) })
                val lv = perfInt(row, "leave_days")
                c4.addView(salaryStatusRow("Leave days", lv.toString(), if (lv > 0) "#B42318" else "#5B6B81") { perfShowAttendanceSheet(code, ns(row, "full_name").ifBlank { code }, ym, fromPerf = true) })
                box.addView(c4)
            }
        }.start()
    }

    /** মডেল ৩-এর কার্ড-শিরোনাম (Salary / Extra Income)। */
    /* 🎨🔒 V1181 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — বেতন-পর্দার উপরের
       সারি: বাঁয়ে শিরোনাম, ডানে ⋮।
       ⛔ শিরোনামের লেখা · মাপ · রং · নিচের ফাঁক — সব হুবহু `ModuleUi.screen()`-এর
          মতোই রাখা হলো, যাতে দেখতে এক চুলও আলাদা না লাগে।
       ⛔ তথ্য আসার আগে ⋮ চাপলে কিছু ভাঙে না — তখন শুধু একটা ছোট বার্তা। */
    private var salMenuAnchor: android.view.View? = null

    private fun salHeaderRow(title: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val dots = TextView(this).apply {
            text = "⋮"
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            setPadding(dp(16), dp(4), dp(12), dp(4))   // 🔎 V1254
            isClickable = true
            setOnClickListener {
                val a = salMenuAction
                if (a == null) ModuleUi.toast(this@StaffProfileActivity, "Loading...")
                else a()
            }
        }
        salMenuAnchor = dots
        row.addView(dots)
        return row
    }

    private fun salSectionTitle(text: String, hex: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(android.graphics.Color.parseColor(hex))
        setPadding(dp(2), dp(2), dp(2), dp(6))
    }

    /** 🔵 V417খ (TK-নির্দেশ): দুটো বোতাম **এক লাইনে পাশাপাশি**, দুটোই সমান চওড়া।
     *  ডান দিকেরটা না থাকলে বাঁ দিকেরটাই পুরো লাইন নেয় — ফাঁকা বাক্স বসে না।
     *  ⛔ বোতামের কাজ · রং · লেখা কিছুই এখানে বদলায় না, শুধু বসার জায়গা। */
    private fun salPairRow(left: android.view.View, right: android.view.View?): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(5), 0, dp(5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        left.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            .apply { if (right != null) rightMargin = dp(9) }
        row.addView(left)
        if (right != null) {
            right.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            row.addView(right)
        }
        return row
    }

    /* 🎨 V416 (TK-অনুমোদিত মডেল ২ + বোতাম মডেল ৩, ১৭.০৮.২০২৬) — শুধু **সাজ**।
       ⛔ কোনো হিসাব · সংখ্যা · সেভ/পড়ার নিয়ম এক অক্ষরও বদলায়নি।
       ⛔ নেভি ব্লু কোথাও নেই (TK-নির্দেশ)। */
    /** 🔵 V417 (TK-অনুমোদিত): ঠিক করে রাখা বাড়তি টাকা এখন দেওয়া হলো।
     *  ⛔ নতুন সারি বানানো হয় না — যে সারিটা "বাকি" ছিল সেটাই "দেওয়া হয়েছে" হয়,
     *     তাই একই টাকা দুবার গোনা হওয়ার সুযোগ নেই।
     *  ⛔ শুধু `status` · `mode` · `paid_on` বদলায়; টাকার অঙ্ক ও কারণ অটুট। */
    /**
     * 🎨🔒 V1183 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু):
     * *"এই ২ টা তে চেহারা একই রকম করতে হবে, রোগের নামের পাশে থাকবে Unexpected Time"*
     * · *"একটা একটা আলদা আলদা ও Paid করা যায় তার ব্যাবস্থা রাখতে হবে"*
     *
     * ⇒ এই পর্দাটা এখন Extra Income History-র **হুবহু একই বাক্স** আঁকে —
     *   এক রোগী = এক বাক্স (নাম · মোবাইল · রোগ · Unexpected Time), নিচে ধাপগুলো
     *   তারিখ-সময় সহ, তারপর Total ও অবস্থা। প্রতিটা বাক্সে নিজের একটা **Pay**
     *   বোতাম — শুধু ওই রোগীর বাকি টাকাটাই মেটে।
     * ⛔ নিচের "Mark as Paid" আগের মতোই **সবগুলো একসাথে** মেটায় — তুলে দেওয়া হয়নি।
     * ⛔ টাকার কোনো অঙ্ক · নিয়ম · কোন সারি কোথায় জমা — কিচ্ছু বদলায়নি; যা সেভ হয়
     *    তা ঠিক আগের মতোই (`status=PAID` · `mode` · `paid_on`)।
     * ⛔ বাক্সগুলোর তথ্য আসে ঠিক ওই একই ব্যাচ-পড়া থেকে (`fillExtraPatientNames`) —
     *    নতুন কোনো cloud-read যোগ হয়নি।
     */
    private fun payExtraDue(code: String, pays: JSONArray) {
        backAction = { salary(code) }
        val col = ModuleUi.screen(this, "Pay Extra Income")
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true

        val dueRows = ArrayList<JSONObject>()
        var dueSum = 0.0
        for (i in 0 until pays.length()) {
            val p = pays.getJSONObject(i)
            if (payKind(p) == "EXTRA" && payStatus(p) == "DUE") {
                dueRows.add(p); dueSum += p.optDouble("amount", 0.0)
            }
        }
        val head = ModuleUi.card(this)
        col.addView(head)
        head.addView(salaryStatusRow("Total to pay now", money(dueSum), "#B42318"))

        val md = spinner(listOf("Cash", "Online"))

        /* ⛔ এই পর্দার নিজের ছোট সহায়ক — History-র বাক্সের হুবহু মাপ ও রং। */
        fun boxBg(fill: String, stroke: String): android.graphics.drawable.GradientDrawable =
            android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.parseColor(fill))
                setStroke(dp(1), android.graphics.Color.parseColor(stroke))
            }

        // ⬇ এক রোগী = এক বাক্স (History-র মতোই), ধাপ ধরে টাকার যোগ
        xStepBoxes.clear(); xGroupSums.clear(); xGroupDue.clear(); xGroupState.clear(); xGroupOther.clear()
        val byPatient = LinkedHashMap<String, MutableList<JSONObject>>()
        val loose = ArrayList<JSONObject>()
        for (p in dueRows) {
            val pid = extraPatientId(p)
            if (pid.isBlank()) loose.add(p) else byPatient.getOrPut(pid) { mutableListOf() }.add(p)
        }
        for ((pid, list) in byPatient) {
            val arr = doubleArrayOf(0.0, 0.0, 0.0)
            for (q in list) {
                val amt = q.optDouble("amount", 0.0)
                val h = cleanWhy(ns(q, "extra_reason")).trim().substringBefore("·").trim().lowercase()
                when {
                    h.startsWith("registration") -> arr[0] += amt
                    h.startsWith("treatment") -> arr[1] += amt
                    else -> {
                        arr[2] += amt
                        val txt = cleanWhy(ns(q, "extra_reason")).trim()
                        if (txt.isNotBlank()) xGroupOther.getOrPut(pid) { mutableListOf() }.add(Pair(txt, amt))
                    }
                }
            }
            xGroupSums[pid] = arr
            xGroupDue[pid] = true
            xGroupState[pid] = "DUE"
        }

        val payRows = ArrayList<Triple<String, TextView?, JSONObject>>()
        val nameViews = ArrayList<Pair<String, TextView>>()

        for ((pid, list) in byPatient) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(9))
                background = boxBg("#FFF7F7", "#F2C8C8")
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            }
            val nameView = TextView(this).apply {
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0F5132"))
                setPadding(dp(3), dp(3), dp(2), 0)
                visibility = android.view.View.GONE
            }
            card.addView(nameView)
            nameViews.add(Pair(pid, nameView))

            val stepBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            card.addView(stepBox)
            xStepBoxes.add(Pair(pid, stepBox))

            var mine = 0.0
            for (q in list) mine += q.optDouble("amount", 0.0)

            val foot = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                /* 📏🔒 V1640 (20.09.2026, TK photo review): keep the Total/DUE row
                   clearly separated from the last step date/time row. Presentation only. */
                setPadding(dp(13), dp(12), dp(2), dp(3))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(5) }
            }
            foot.addView(TextView(this).apply {
                text = "Total " + money(mine)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#C62828"))
            })
            foot.addView(TextView(this).apply {
                text = "DUE"
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#C62828"))
                gravity = android.view.Gravity.CENTER
                setPadding(dp(7), dp(4), dp(7), dp(4))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(android.graphics.Color.parseColor("#FDE9EA"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(12) }
            })
            /* 💸 V1183 (TK-নির্দেশ) — শুধু **এই রোগীর** টাকা মেটানোর বোতাম। */
            foot.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
            })
            foot.addView(salOutlineButton("Pay " + money(mine), "#0A5C33", "#0A5C33") {
                markExtraPaid(code, list, md.selectedItem?.toString().orEmpty())
            })
            card.addView(foot)

            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { openPatientHistory(pid, extraPatientCache[pid]?.second.orEmpty()) }
            payRows.add(Triple(pid, null, list[0]))
            col.addView(card)
        }

        /* ⛔ রোগী চেনা যায়নি এমন সারি (পুরনো তথ্য) — আগের মতোই সাদামাটা লাইন,
           যাতে একটাও টাকা পর্দা থেকে হারিয়ে না যায়। */
        for (p in loose) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(9))
                background = boxBg("#FFF7F7", "#F2C8C8")
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            }
            row.addView(TextView(this).apply {
                text = money(p.optDouble("amount", 0.0)) + "  ·  " + cleanWhy(ns(p, "extra_reason"))
                textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor("#17212B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(salOutlineButton("Pay " + money(p.optDouble("amount", 0.0)), "#0A5C33", "#0A5C33") {
                markExtraPaid(code, listOf(p), md.selectedItem?.toString().orEmpty())
            })
            col.addView(row)
        }

        if (payRows.isNotEmpty() || nameViews.isNotEmpty()) fillExtraPatientNames(payRows, nameViews = nameViews)

        col.addView(ModuleUi.label(this, "Mode")); col.addView(md)
        col.addView(ModuleUi.button(this, "✅ Mark as Paid") {
            if (dueRows.isEmpty()) { ModuleUi.toast(this, "Nothing due"); return@button }
            markExtraPaid(code, dueRows, md.selectedItem?.toString().orEmpty())
        })
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.button(this, "Back") { salary(code) })
    }

    /** 💸🔒 V1183 — বাকি টাকা মেটানোর **একটাই** পথ (একটা সারি হোক বা সবগুলো),
     *  তাই দুই বোতামে কখনো দুরকম কিছু ঘটতে পারে না।
     *  ⛔ যা লেখা হয় তা হুবহু আগের মতোই: `status=PAID` · `mode` · `paid_on`। */
    private fun markExtraPaid(code: String, rows: List<JSONObject>, mode: String) {
        if (rows.isEmpty()) { ModuleUi.toast(this, "Nothing due"); return }
        Thread {
            var okAll = true
            for (p in rows) {
                val id = ns(p, "id")
                if (id.isBlank()) { okAll = false; continue }
                val patch = JSONObject().put("status", "PAID").put("mode", mode).put("paid_on", todayIso())
                if (!ModuleAuth.update("hr", "salary_payments", "id=eq.$id", patch)) okAll = false
            }
            runOnUiThread {
                salaryCacheClear(code)
                ModuleUi.toast(this, if (okAll) "Paid" else "Some entries did not save — try again")
                salary(code)
            }
        }.start()
    }


    /* 🧾🔒 V1055 (TK-নির্দেশ ০৪.০৯.২০২৬) — **তারিখ-থেকে-তারিখ স্টেটমেন্ট**,
       কম্পিউটারের V1052–V1054-এর হুবহু যমজ (নিয়ম ৬.৬)।
       ⛔ **নতুন কোনো হিসাব বানানো হয়নি** — যে সারিগুলো এমনিতেই আনা হয়েছে
          সেগুলোই `paid_on` ধরে ছেঁকে মাস ধরে যোগ করা হয়, তাই এই পাতার সংখ্যা
          আর অন্য পর্দার সংখ্যা কখনো আলাদা হবে না (নিয়ম ৭ক-এর ২)।
       ⛔ তারিখ-ফাঁকা সারি চুপচাপ হারায় না — কতগুলো বাদ পড়ল নিচে লেখা থাকে।
       ⛔ মাসের লেখা `Sep-26` ধাঁচে — TK নিজে তালিকা দিয়েছেন। */
    private var stFrom: String = ""
    private var stTo: String = ""

    /* 📅 V1055 — "From / To" এক-একটা সারি; চাপ দিলে ফোনের নিজের ক্যালেন্ডার খোলে
       (প্রকল্পে আগে থেকেই এই প্রমাণিত পথ — `perfHeader`-এ একই DatePickerDialog)। */
    private fun dateRow(label: String, value: String, onPick: (String) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(6), dp(2), dp(6))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 13.5f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            layoutParams = LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = dmy(value); textSize = 14.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F5132"))
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#DDE5EC"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
                try {
                    val q = value.split("-")
                    cal.set(q[0].toInt(), q[1].toInt() - 1, q[2].toInt())
                } catch (_: Throwable) { }
                android.app.DatePickerDialog(this@StaffProfileActivity, { _, y, mth, day ->
                    onPick(String.format(Locale.US, "%04d-%02d-%02d", y, mth + 1, day))
                }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
                   cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
            }
        })
        return row
    }

    private fun stMonthLabel(ym: String): String {
        val N = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return try {
            val q = ym.split("-")
            (N.getOrNull(q[1].toInt() - 1) ?: q[1]) + "-" + q[0].takeLast(2)
        } catch (_: Throwable) { ym }
    }

    private fun statement(code: String, pays: JSONArray) {
        backAction = { salary(code) }
        if (stTo.isBlank()) stTo = todayIso()
        if (stFrom.isBlank()) {
            stFrom = try {
                val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
                c.add(java.util.Calendar.MONTH, -11); c.set(java.util.Calendar.DAY_OF_MONTH, 1)
                String.format(Locale.US, "%04d-%02d-%02d",
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, 1)
            } catch (_: Throwable) { todayIso() }
        }
        val col = ModuleUi.screen(this, "Statement — $code")

        val pick = ModuleUi.card(this)
        col.addView(pick)
        pick.addView(dateRow("From", stFrom) { v -> stFrom = v; statement(code, pays) })
        pick.addView(dateRow("To", stTo) { v -> stTo = v; statement(code, pays) })

        // ── মাস ধরে যোগ ──
        val sal = HashMap<String, Double>(); val exP = HashMap<String, Double>()
        val exD = HashMap<String, Double>(); var noDate = 0
        for (i in 0 until pays.length()) {
            val r = pays.optJSONObject(i) ?: continue
            val d = ns(r, "paid_on").take(10)
            if (d.isBlank()) { noDate++; continue }
            if (d < stFrom || d > stTo) continue
            val amt = r.optDouble("amount", 0.0)
            if (payKind(r) == "EXTRA") {
                val ym = d.take(7)   // বাড়তি টাকা = যেদিন দেওয়া সেই মাস (for_month ফাঁকা থাকে)
                if (payStatus(r) == "DUE") exD[ym] = (exD[ym] ?: 0.0) + amt
                else exP[ym] = (exP[ym] ?: 0.0) + amt
            } else {
                /* 🔴🔒 V1432 (১৩.০৯.২০২৬, TK: "২ পর্দায় দুই রকম কেন — LAXMI-র স্যালারি", তালিকা ৫৪৯) —
                   আগে এখানে বেতন **যেদিন দেওয়া** সেই মাসে বসত (paid_on), অথচ Salary History
                   পর্দা **কোন মাসের বেতন** (for_month) দেখায় — তাই জুনের বেতন ৪ জুলাই দিলে
                   Statement-এ Jul, History-তে June: দুই পর্দায় দুরকম (নিয়ম ৭ক-২)। এখন Statement-ও
                   History-র হুবহু একই নিয়ম `salaryPayMonth()` (for_month, নইলে paid_on-এর মাস)।
                   From/To ছাঁকনি আগের মতোই paid_on-এ। */
                val ym = salaryPayMonth(r).take(7)
                sal[ym] = (sal[ym] ?: 0.0) + amt
            }
        }
        val yms = (sal.keys + exP.keys + exD.keys).distinct().sorted()

        val sheet = ModuleUi.card(this)
        col.addView(sheet)
        sheet.addView(TextView(this).apply {
            text = "SALARY & EXTRA INCOME STATEMENT"
            textSize = 14f; gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F5132"))
        })
        sheet.addView(TextView(this).apply {
            text = code + "  ·  " + dmy(stFrom) + "  to  " + dmy(stTo)
            textSize = 11.5f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(3), 0, dp(10))
        })

        fun cell(t: String, w: Float, hex: String, bold: Boolean, right: Boolean): TextView =
            TextView(this).apply {
                text = t; textSize = 11.5f; maxLines = 1
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(hex))
                gravity = if (right) android.view.Gravity.END else android.view.Gravity.START
                setPadding(dp(3), dp(6), dp(3), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            }
        fun tableRow(a: String, b: String, c: String, d: String, e: String,
                     head: Boolean = false, foot: Boolean = false, dueRed: Boolean = false): LinearLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                if (head) setBackgroundColor(android.graphics.Color.parseColor("#0B4F2A"))
                else if (foot) setBackgroundColor(android.graphics.Color.parseColor("#F4F9F6"))
                val ink = if (head) "#FFFFFF" else "#1C2A33"
                addView(cell(a, 1.15f, ink, head || foot, false))
                addView(cell(b, 1f, ink, head || foot, true))
                addView(cell(c, 0.95f, ink, head || foot, true))
                addView(cell(d, 0.9f, if (!head && dueRed) "#C62828" else ink, head || foot || dueRed, true))
                addView(cell(e, 1.05f, ink, true, true))
            }

        sheet.addView(tableRow("Month", "Salary", "Extra", "Due", "Total", head = true))
        var tS = 0.0; var tP = 0.0; var tD = 0.0
        val printRows = ArrayList<com.tkbiswas.pilesclinic.print.SalaryStatementHtmlPrint.Row>()
        for (ym in yms) {
            val a = sal[ym] ?: 0.0; val b = exP[ym] ?: 0.0; val c = exD[ym] ?: 0.0
            tS += a; tP += b; tD += c
            printRows.add(com.tkbiswas.pilesclinic.print.SalaryStatementHtmlPrint.Row(
                stMonthLabel(ym), a, b, c))
            sheet.addView(tableRow(stMonthLabel(ym), money(a), money(b), money(c), money(a + b),
                dueRed = c > 0.0))
        }
        if (yms.isEmpty()) sheet.addView(ModuleUi.body(this, "No payments in this period."))
        sheet.addView(tableRow("TOTAL", money(tS), money(tP), money(tD), money(tS + tP),
            foot = true, dueRed = tD > 0.0))
        if (noDate > 0) sheet.addView(TextView(this).apply {
            text = "$noDate entry(ies) have no date and are not counted here."
            textSize = 10.5f
            setTextColor(android.graphics.Color.parseColor("#8B98A9"))
            setPadding(0, dp(8), 0, 0)
        })

        /* 🎨🔒 V1104 (০৫.০৯.২০২৬, TK: *"Print, Back-এ দুটো পাশাপাশি রাখা যায় তো"*)
           — দুটো বোতাম এখন এক সারিতে সমান চওড়ায়। ⛔ কাজ, রং, লেখা কিছুই
           বদলায়নি; শুধু জায়গা। ⛔ ছোট পর্দাতেও লেখা ভাঙে না (এক লাইনে বাঁধা)। */
        col.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val bPrint = ModuleUi.button(this@StaffProfileActivity, "🖨 Print / PDF") {
                com.tkbiswas.pilesclinic.print.SalaryStatementHtmlPrint.print(
                    this@StaffProfileActivity, code, dmy(stFrom), dmy(stTo), printRows)
            }
            val bBack = ModuleUi.button(this@StaffProfileActivity, "Back") { salary(code) }
            for (b in listOf(bPrint, bBack)) {
                (b as? android.widget.TextView)?.let {
                    it.setSingleLine(true)
                    it.ellipsize = android.text.TextUtils.TruncateAt.END
                }
                b.minimumWidth = 0
            }
            bPrint.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = dp(5) }
            bBack.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(5) }
            addView(bPrint); addView(bBack)
        })
    }

    private fun salOutlineButton(text: String, textHex: String, borderHex: String, onClick: () -> Unit): android.widget.Button =
        android.widget.Button(this).apply {
            this.text = text
            isAllCaps = false
            // 🔵 V417খ: এখন দুটো বোতাম পাশাপাশি বসে (অর্ধেক চওড়া), তাই লেখা একটু
            //    ছোট আর দু'লাইনে ভেঙে বসার ব্যবস্থা — নইলে লেখা কেটে যেত।
            //    ⛔ Button-এর নিজের সবচেয়ে-কম চওড়া ০ করা হলো, নইলে weight মানত না।
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            minWidth = 0
            minimumWidth = 0
            // 🟣🔒 V961 (TK: *"প্রতিটা বক্সের সাইজ এত বড় বড় কেন থাকবে"*) —
            //    উপরে-নিচে ১৩ → ৯dp। ⛔ লেখা · রং · কাজ কিছুই বদলায়নি।
            setPadding(dp(8), dp(9), dp(8), dp(9))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(textHex))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(2), android.graphics.Color.parseColor(borderHex))
            }
            setOnClickListener { onClick() }
        }

    /** উপরের তিনটে ছোট বাক্স — বেতন · বাড়তি · সব মিলিয়ে (TK-বাছা মডেল ২)। */
    private fun salTotalTiles(paidSalary: Double, extra: Double): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, dp(4))
        }
        fun tile(caption: String, value: String, last: Boolean): LinearLayout {
            val t = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(dp(6), dp(9), dp(6), dp(9))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.parseColor("#F2FBF5"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#D8ECDF"))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (!last) rightMargin = dp(8) }
            }
            t.addView(TextView(this@StaffProfileActivity).apply {
                text = caption; textSize = 11f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            })
            t.addView(TextView(this@StaffProfileActivity).apply {
                text = value; textSize = 16f
                gravity = android.view.Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                setPadding(0, dp(3), 0, 0)
            })
            return t
        }
        row.addView(tile("Salary paid", money(paidSalary), false))
        row.addView(tile("Extra", money(extra), false))
        row.addView(tile("Total", money(paidSalary + extra), true))
        return row
    }

    /* 🔴🔴🔒 V440 (TK-নির্দেশ ১৮.০৮.২০২৬) — `onClick` দিলে সারিটা চাপ-যোগ্য হয়
       (হালকা ripple + ডান পাশে ">") — নাহলে আগের মতোই স্থির। ⛔ পুরনো সব ডাক
       (Salary ইত্যাদি, onClick ছাড়া) হুবহু আগের মতোই দেখাবে/আচরণ করবে। */
    /* 💰🔒 V991 (০৩.০৯.২০২৬, TK-নির্দেশ: *"ডিজাইনটা আরো প্রফেশনাল লুক বানাতে
       হবে"*, ফটো-প্রুফ পাশ) — Extra Income-এর মাথায় সোনালি পট্টি, আর নিচে
       "PAID" ও "DUE" দুটো আলাদা রঙিন টালিতে।
       ⛔ শুধু **সাজ** — টাকার অঙ্ক · হিসাব · সেভ/পড়ার নিয়ম এক অক্ষরও বদলায়নি।
       ⛔ বেতনের বাক্সে হাত পড়েনি, তাই ওই অংশ হুবহু আগের মতোই। */
    /** "2026-09" → "September 2026" (শুধু দেখানোর জন্য)। */
    private fun salMonthName(ym: String): String = try {
        val p = ym.split("-")
        val names = listOf("January","February","March","April","May","June",
            "July","August","September","October","November","December")
        names[p[1].toInt() - 1] + " " + p[0]
    } catch (_: Throwable) { ym }

    private fun salGoldHeader(title: String, right: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    android.graphics.Color.parseColor("#B45309"),
                    android.graphics.Color.parseColor("#E0A800")
                )
            ).apply { cornerRadius = dp(12).toFloat() }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            addView(TextView(this@StaffProfileActivity).apply {
                text = title; textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                letterSpacing = 0.05f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@StaffProfileActivity).apply {
                text = right; textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#FFF3D6"))
            })
        }

    /** "PAID" ও "DUE" — পাশাপাশি দুটো রঙিন টালি। */
    private fun salMoneyTiles(paid: String, due: String, dueRed: Boolean): LinearLayout {
        fun tile(cap: String, value: String, fill: String, ink: String) =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(fill))
                    cornerRadius = dp(12).toFloat()
                }
                setPadding(dp(14), dp(11), dp(14), dp(12))
                addView(TextView(this@StaffProfileActivity).apply {
                    text = cap; textSize = 10f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#6B7A83"))
                    letterSpacing = 0.12f
                })
                addView(TextView(this@StaffProfileActivity).apply {
                    text = value; textSize = 19f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor(ink))
                    setPadding(0, dp(3), 0, 0)
                })
            }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        val a = tile("PAID", paid, "#EAF7F0", "#0B5B2F")
        val b = tile("DUE", due, if (dueRed) "#FDEDEC" else "#F3F5F7", if (dueRed) "#B42318" else "#5B6B81")
        a.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { rightMargin = dp(8) }
        b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(a); row.addView(b)
        return row
    }

    private fun salaryStatusRow(label: String, value: String, valueColor: String, onClick: (() -> Unit)? = null): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(4))   // 🟣 V961: ৬ → ৪dp
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 13.5f; setTextColor(android.graphics.Color.parseColor("#3B5A49"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 13.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(valueColor))
            gravity = android.view.Gravity.END
        })
        if (onClick != null) {
            row.addView(TextView(this).apply {
                text = "  \u203A"; textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#8AA79A"))
            })
            row.isClickable = true; row.isFocusable = true
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            row.setBackgroundResource(outValue.resourceId)
            row.setOnClickListener { onClick() }
        }
        return row
    }

    /* ── V440: এক-একটা সংখ্যায় চাপ দিলে আসল তালিকা/হাজিরা-খাতা ───────────── */

    /** সাধারণ তালিকা-পর্দা — শিরোনাম + লোডিং + সারিগুলো নিজের মতো করে বসানো
     *  (renderRow), না পেলে সৎ বার্তা। কোনো লেখা/বদল নেই — শুধু দেখা। */
    private fun perfListScreen(
        title: String, code: String, month: String,
        fetch: () -> org.json.JSONArray?,
        emptyMsg: String,
        cachedRows: org.json.JSONArray? = null,
        onRowClick: ((org.json.JSONObject, org.json.JSONArray) -> Unit)? = null,
        renderRow: (LinearLayout, org.json.JSONObject) -> Unit
    ) {
        // 🔴🔴 V511 (২১.০৮.২০২৬, TK-রিপোর্ট — "Calls From App"-এ Back কাজ করে না)।
        //   আগে এখানে `backAction`-এর তখনকার মানটাই ধরে নেওয়া হত, আর ডিটেল
        //   থেকে ফিরলে সেই মানটা হয়ে যেত **"এই তালিকাটাই আবার খোলো"** —
        //   ফলে Back চাপলে একই পর্দা আবার আঁকা হত, কিছুই বদলাত না।
        //   এখন ফেরার ঠিকানা `performanceOne()` আগেই লিখে রাখে (`perfListBack`),
        //   তাই লুপ আর সম্ভব নয়। ⛔ `perfListBack` কোনো কারণে ফাঁকা থাকলে
        //   আগের আচরণই চলে — কিছু ভাঙে না।
        val prevBack = perfListBack ?: backAction
        backAction = { prevBack() }
        val col = ModuleUi.screen(this, title)
        col.addView(TextView(this).apply {
            text = perfLabel(month); textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(dp(2), 0, dp(2), dp(8))
        })
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val loading = ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "Loading...")) }
        box.addView(loading)
        col.addView(box)
        col.addView(ModuleUi.button(this, "Back") { backAction() })
        Thread {
            // 🔴 V452: detail থেকে Back করলে একই already-loaded list আবার দেখাই;
            // Cloud-এ অকারণে দ্বিতীয় read করি না। Fresh open-এ আগের fetch-ই চলে।
            val rows = cachedRows ?: try { fetch() } catch (_: Throwable) { null }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                box.removeAllViews()
                if (rows == null) {
                    box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "Could not load. Please try again.")) })
                    return@runOnUiThread
                }
                if (rows.length() == 0) {
                    box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, emptyMsg)) })
                    return@runOnUiThread
                }
                val card = ModuleUi.card(this)
                for (i in 0 until rows.length()) {
                    val r = rows.optJSONObject(i) ?: continue
                    val line = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(2), dp(8), dp(2), dp(8))
                    }
                    renderRow(line, r)
                    if (onRowClick != null) {
                        line.isClickable = true
                        line.isFocusable = true
                        val outValue = android.util.TypedValue()
                        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                        if (outValue.resourceId != 0) line.setBackgroundResource(outValue.resourceId)
                        line.setOnClickListener { onRowClick(r, rows) }
                    }
                    card.addView(line)
                    if (i < rows.length() - 1) card.addView(android.view.View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                        setBackgroundColor(android.graphics.Color.parseColor("#E3ECE7"))
                    })
                }
                box.addView(card)
            }
        }.start()
    }

    /**
     * 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — **নম্বরে/সারিতে চাপ দিলে সেই
     * নম্বরের রেকর্ডে চলে যাওয়া।**
     *
     * TK-এর কথা: *"এই নাম্বারের উপর চাপ দিলে যেন রিডাইরেক্ট হয় — এই
     * নাম্বারের অবস্থান যেখানে সেখানে চলে যেতে হবে।"* এবং *"সেই ব্যক্তি
     * রেজিস্ট্রেশন যেখানে হয়েছে সেখানে যেন রিডাইরেক্ট হয়ে যায়।"*
     *
     * ⛔ **বাড়তি কোনো cloud-read নেই** — নম্বরটা তালিকার সারিতেই আছে।
     * ⛔ `PatientTimelineActivity` **শুধু মোবাইল নম্বর** নেয় (ঐ ফাইলের
     *    ১৫৪ নম্বর লাইন), তাই সেটাই পাঠানো হয় — এই ফাইলের
     *    `openPatientHistory()`-র হুবহু একই পথ।
     * ⛔ নম্বরটা পুরো না হলে (নিচের নোট দেখুন) কিছুই খোলে না, স্পষ্ট বার্তা যায়।
     */
    private fun perfOpenNumber(rawMobile: String): Boolean {
        val digits = rawMobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            ModuleUi.toast(this, "Full number not available for this row")
            return false
        }
        return try {
            startActivity(android.content.Intent(
                this, com.tkbiswas.pilesclinic.native.PatientTimelineActivity::class.java)
                .putExtra("mobile", digits))
            true
        } catch (_: Throwable) { false }
    }

    private fun perfRowTitle(text: String): TextView = TextView(this).apply {
        this.text = text; textSize = 14f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(android.graphics.Color.parseColor("#123A26"))
    }
    private fun perfRowSub(text: String): TextView = TextView(this).apply {
        this.text = text; textSize = 12.5f
        setTextColor(android.graphics.Color.parseColor("#5B6B81"))
        setPadding(0, dp(2), 0, 0)
    }

    /* 🔴 V452 (19.08.2026, TK-অনুমোদিত): Staff Performance-এর দ্বিতীয় স্তরের
       Enquiry / Calls / Collection row চাপলে exact read-only detail। নতুন Cloud
       read হয় না — list RPC-তেই detail fields এসেছে; Back-এ cached rows ফেরে। */
    private fun perfDetailField(card: LinearLayout, label: String, value: String) {
        if (value.isBlank()) return
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(7), dp(2), dp(7))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#123A26"))
            setPadding(0, dp(2), 0, 0)
        })
        card.addView(row)
    }

    private fun perfDetailScreen(
        title: String,
        period: String,
        fields: List<Pair<String, String>>,
        onBack: () -> Unit
    ) {
        backAction = onBack
        val col = ModuleUi.screen(this, title)
        if (period.isNotBlank()) col.addView(TextView(this).apply {
            text = period; textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(dp(2), 0, dp(2), dp(8))
        })
        val card = ModuleUi.card(this)
        var shown = 0
        for ((label, value) in fields) {
            if (value.isBlank()) continue
            if (shown > 0) card.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(android.graphics.Color.parseColor("#E3ECE7"))
            })
            perfDetailField(card, label, value)
            shown++
        }
        if (shown == 0) card.addView(ModuleUi.body(this, "No detail available."))
        col.addView(card)
        col.addView(ModuleUi.button(this, "Back") { onBack() })
    }

    private fun perfShowEnquiryList(code: String, month: String, cachedRows: JSONArray? = null) {
        perfListScreen("Enquiry Forms", code, month,
            fetch = {
                val r = ModuleAuth.rpc("hr", "perf_enquiry_list_v2", JSONObject().put("p_month", month).put("p_code", code))
                if (r.ok) JSONArray(r.body) else null
            }, emptyMsg = "No enquiry forms this period.", cachedRows = cachedRows,
            onRowClick = { r, rows ->
                perfDetailScreen(
                    "Enquiry Detail", perfLabel(month),
                    listOf(
                        "Name" to ns(r, "name").ifBlank { "Unknown" },
                        "Mobile" to ns(r, "mobile"),
                        "Date" to dmy(ns(r, "enq_date")),
                        "Branch" to ns(r, "branch"),
                        "Disease" to ns(r, "disease"),
                        "Address" to ns(r, "address"),
                        "Remarks" to ns(r, "remarks"),
                        "Status" to ns(r, "status"),
                        "Stage" to ns(r, "stage"),
                        "Received By" to ns(r, "received_by"),
                        "Created By" to ns(r, "created_by"),
                        "Created At" to ns(r, "created_at"),
                        "Record ID" to ns(r, "id")
                    )
                ) { perfShowEnquiryList(code, month, rows) }
            }) { line, r ->
            line.addView(perfRowTitle(ns(r, "name").ifBlank { "Unknown" }))
            line.addView(perfRowSub(dmy(ns(r, "enq_date")) + "  ·  " + ns(r, "mobile") + "  ·  " + ns(r, "branch")))
            if (ns(r, "disease").isNotBlank()) line.addView(perfRowSub(ns(r, "disease")))
        }
    }

    private fun perfShowRegistrationList(code: String, month: String, treatmentOnly: Boolean) {
        val title = if (treatmentOnly) "Started Treatment" else "Registrations"
        perfListScreen(title, code, month,
            fetch = {
                val fn = if (treatmentOnly) "perf_treatment_list" else "perf_registration_list"
                val r = ModuleAuth.rpc("hr", fn, JSONObject().put("p_month", month).put("p_code", code))
                if (r.ok) JSONArray(r.body) else null
            }, emptyMsg = "None this period.",
            /* 🔵🔒 V521 (TK-নির্দেশ): সারিতে চাপ দিলে **ঐ রোগীর রেকর্ডেই**
               চলে যাওয়া হয় — আগে চাপ দিলে কিছুই হত না।
               ⛔ নম্বর পুরো না থাকলে কিছুই খোলে না, স্পষ্ট বার্তা যায়। */
            onRowClick = { r, _ -> perfOpenNumber(ns(r, "mobile")) }) { line, r ->
            line.addView(perfRowTitle(ns(r, "name").ifBlank { "Unknown" }))
            line.addView(perfRowSub(dmy(ns(r, "reg_date")) + "  \u00b7  " + ns(r, "mobile") + "  \u00b7  " + ns(r, "branch")))
            if (ns(r, "patient_id").isNotBlank()) line.addView(perfRowSub(ns(r, "patient_id")))
        }
    }

    private fun perfShowRmpList(code: String, month: String) {
        perfListScreen("RMP Added", code, month,
            fetch = {
                val r = ModuleAuth.rpc("hr", "perf_rmp_list", JSONObject().put("p_month", month).put("p_code", code))
                if (r.ok) JSONArray(r.body) else null
            }, emptyMsg = "None this period.",
            // 🔵 V521 (TK-নির্দেশ): সারিতে চাপ = ঐ নম্বরের রেকর্ড।
            onRowClick = { r, _ -> perfOpenNumber(ns(r, "mobile")) }) { line, r ->
            line.addView(perfRowTitle(ns(r, "name").ifBlank { "Unknown" }))
            line.addView(perfRowSub(dmy(ns(r, "added_date").take(10)) + "  \u00b7  " + ns(r, "mobile") + "  \u00b7  " + ns(r, "area")))
        }
    }

    private fun perfShowCallsList(code: String, month: String, outside: Boolean, cachedRows: JSONArray? = null) {
        perfListScreen(if (outside) "Outside Calls" else "Calls From App", code, month,
            fetch = {
                val r = ModuleAuth.rpc("hr", "perf_calls_list_v2", JSONObject().put("p_month", month).put("p_code", code).put("p_kind", if (outside) "outside" else "app"))
                if (r.ok) JSONArray(r.body) else null
            }, emptyMsg = "No calls this period.", cachedRows = cachedRows,
            onRowClick = { r, rows ->
                val fullOk = try { r.optBoolean("full_number_available", false) } catch (_: Throwable) { false }
                /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ — *"এই নাম্বারের উপর চাপ দিলে
                   যেন রিডাইরেক্ট হয়"*): নম্বরটা **পুরো জানা থাকলে** সোজা ঐ
                   নম্বরের রেকর্ডে চলে যায়।
                   ⚠️ **সৎ কথা:** পুরনো কিছু কলের পুরো নম্বর **কখনো সেভই হয়নি**
                      (সার্ভারে `target_mobile` ফাঁকা, তাই মুখোশটাই ফেরে —
                      `V452_STAFF_PERFORMANCE_EXACT_DETAIL` SQL, লাইন ১০২)।
                      অ্যাপ সেটা লুকোচ্ছে না — নেই বলেই দেখাতে পারে না।
                      ঐ সারিগুলোয় চাপ দিলে আগের মতোই ডিটেল পর্দা খোলে, যেখানে
                      কারণটা লেখা আছে। নতুন কলে পুরো নম্বরই সেভ হয় ও দেখায়। */
                val opened = fullOk && perfOpenNumber(ns(r, "target"))
                val note = if (!outside && !fullOk) "This is an older call. The full number was not stored at that time." else ""
                if (!opened) perfDetailScreen(
                    if (outside) "Outside Call Detail" else "App Call Detail", perfLabel(month),
                    listOf(
                        "Number" to ns(r, "target").ifBlank { "—" },
                        "Date" to dmy(ns(r, "call_date")),
                        "Time" to ns(r, "call_time"),
                        "Call Type" to if (outside) "Outside call" else "Call from app",
                        "Remark" to ns(r, "remark"),
                        "Note" to note,
                        "Record ID" to ns(r, "id")
                    )
                ) { perfShowCallsList(code, month, outside, rows) }
            }) { line, r ->
            line.addView(perfRowTitle(ns(r, "target").ifBlank { "—" }))
            line.addView(perfRowSub(dmy(ns(r, "call_date")) + "  ·  " + ns(r, "call_time")))
            if (ns(r, "remark").isNotBlank()) line.addView(perfRowSub(ns(r, "remark")))
        }
    }

    private fun perfShowPaymentList(code: String, month: String, online: Boolean, cachedRows: JSONArray? = null) {
        perfListScreen(if (online) "Online Collection" else "Cash Collection", code, month,
            fetch = {
                val r = ModuleAuth.rpc("hr", "perf_payment_list_v2", JSONObject().put("p_month", month).put("p_code", code).put("p_mode", if (online) "online" else "cash"))
                if (r.ok) JSONArray(r.body) else null
            }, emptyMsg = "No payments this period.", cachedRows = cachedRows,
            onRowClick = { r, rows ->
                perfDetailScreen(
                    if (online) "Online Collection Detail" else "Cash Collection Detail", perfLabel(month),
                    listOf(
                        "Patient" to ns(r, "name").ifBlank { "Unknown" },
                        "Mobile" to ns(r, "mobile"),
                        "Amount" to money(perfDbl(r, "amount")),
                        "Date" to dmy(ns(r, "pay_date")),
                        "Mode" to ns(r, "mode").ifBlank { if (online) "Online" else "Cash" },
                        "Payment" to ns(r, "pay_label"),
                        "Payment Type" to ns(r, "pay_type"),
                        "Branch" to ns(r, "branch"),
                        "Remarks" to ns(r, "remarks"),
                        "Patient ID" to ns(r, "patient_id"),
                        "Patient Code" to ns(r, "patient_code"),
                        "Received By" to ns(r, "received_by"),
                        "Created By" to ns(r, "created_by"),
                        "Created At" to ns(r, "created_at"),
                        "Status" to ns(r, "status"),
                        "Record ID" to ns(r, "id")
                    )
                ) { perfShowPaymentList(code, month, online, rows) }
            }) { line, r ->
            line.addView(perfRowTitle(ns(r, "name").ifBlank { "Unknown" } + "  —  " + money(perfDbl(r, "amount"))))
            line.addView(perfRowSub(dmy(ns(r, "pay_date")) + "  ·  " + ns(r, "mobile") + "  ·  " + ns(r, "branch")))
            if (ns(r, "pay_label").isNotBlank()) line.addView(perfRowSub(ns(r, "pay_label")))
            else if (ns(r, "remarks").isNotBlank()) line.addView(perfRowSub(ns(r, "remarks")))
        }
    }

    private fun perfShowReportsList(code: String, month: String) {
        perfListScreen("Daily Reports Sent", code, month,
            fetch = {
                val r = ModuleAuth.rpc("hr", "perf_reports_list", JSONObject().put("p_month", month).put("p_code", code))
                if (r.ok) JSONArray(r.body) else null
            }, emptyMsg = "No reports sent this period.") { line, r ->
            line.addView(perfRowTitle(dmy(ns(r, "report_date"))))
            val acc = try { r.optBoolean("accepted", false) } catch (_: Throwable) { false }
            line.addView(perfRowSub(ns(r, "status").ifBlank { "sent" } + if (acc) "  \u00b7  seen \u2705" else ""))
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       🗓️🔒 V1199 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **ATTENDANCE SHEET**
       (কত তারিখে কত ঘণ্টা, আলাদা পর্দায়)।

       TK-এর কথা (হুবহু): *"কত ঘন্টা কাজ করেছে, কত তারিখে কত ঘন্টা তার হিস্ট্রি
       যেন আমি আলাদাভাবে দেখতে পারি"* · *"গুগল শিটের মতন চারি সাইডে বক্স"* ·
       *"ছুটি · ওয়ার্ক ফ্রম হোম · অন্য ব্রাঞ্চে ডিউটি — ঘন্টার ঘরে থাকবে"* ·
       *"আউট টাইম মিসিং এই লেখাটা আউট টাইমের ঘরে থাকবে"* · *"মাস্টার যেন এখান
       থেকেই ইন টাইম আউট টাইম এডিট করতে পারে"* · *"WhatsApp-এ শেয়ার ও A4
       পিডিএফ/প্রিন্ট"*।

       ⛔ ঘণ্টার হিসাব **হুবহু `HourSalary`-রই** — এখানে নতুন কোনো নিয়ম নেই
          (ছুটি ও Work From Home = ৭ ঘণ্টা · IN/OUT-এর একটা না থাকলে ০)।
       ⛔ পুরনো `perfShowAttendanceSheet` পর্দাটা **ছোঁয়া হয়নি** — সেটা আগের
          মতোই চলে; এটা তার পাশে নতুন পর্দা।
       ⛔ মাস্টারের এডিট ঠিক সেই ঘরেই লেখে যেখানে Fix Attendance লেখে
          (`wn.notebook_days` · staff_code,work_date) — নতুন কোনো ঘর নয়।
       ═══════════════════════════════════════════════════════════════════ */
    private var attSheetRows: List<AttRow> = emptyList()
    private var attSheetMonth: String = ""
    /* 🎨🔒 V1457 (১৪.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ — "হ্যাঁ পাশ, বসিয়ে দিন") —
       TK: *"print Whatsapp chang Month এগুলি উপরে ডান সাইডে LG 3 dot এর মধ্যে
       থাকবে"*। Salary Statement-এ TK আগেই পাশ করা একই ⋮ মেনু-প্যাটার্ন (V1181,
       `salMenuAnchor`/`salMenuAction`) — এখানে Performance Sheet-এর জন্য
       আলাদা নাম দিয়ে হুবহু পুনর্ব্যবহার। */
    private var perfMenuAnchor: android.view.View? = null
    private var perfMenuAction: (() -> Unit)? = null

    private data class AttRow(
        val dateIso: String, val date: String, val inTime: String, val outTime: String,
        val outMissing: Boolean, val minutes: Int, val hours: String,
        val tag: String, val tagKind: String
    )

    /** "10:05" / "10:05:00" → "10.05 AM"; ফাঁকা হলে "—"। */
    private fun attT12(raw: String): String {
        val t = raw.trim()
        if (t.length < 4) return "\u2014"
        return try {
            val hh = t.substring(0, 2).toInt(); val mm = t.substring(3, 5)
            val ap = if (hh >= 12) "PM" else "AM"
            val h12 = when { hh == 0 -> 12; hh > 12 -> hh - 12; else -> hh }
            "$h12.$mm $ap"
        } catch (_: Throwable) { t }
    }

    private fun attendanceSheet(code: String, ym: String) {
        val prevBack = backAction
        backAction = { prevBack() }
        attSheetMonth = ym
        val col = ModuleUi.screen(this, "")   // V1457 — শিরোনাম এখন নিজের হেডার-সারিতে
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true
        perfMenuAction = null
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        val headTextCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headTextCol.addView(TextView(this).apply {
            text = "Performance Sheet"; textSize = 19f
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
        })
        headTextCol.addView(TextView(this).apply {
            text = code + "  \u00b7  " + salaryMonthLabel(ym)
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(2), 0, 0)
        })
        headRow.addView(headTextCol)
        val perfDots = TextView(this).apply {
            text = "⋮"; textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            setPadding(dp(16), dp(4), dp(4), dp(4))
            isClickable = true
            setOnClickListener {
                val a = perfMenuAction
                if (a == null) ModuleUi.toast(this@StaffProfileActivity, "Loading...") else a()
            }
        }
        perfMenuAnchor = perfDots
        headRow.addView(perfDots)
        col.addView(headRow)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(box)
        box.addView(ModuleUi.body(this, "Loading..."))

        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.buttonSoft(this, "Back") { backAction() }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        })

        Thread {
            val from = "$ym-01"
            val end = try {
                val p = ym.split("-"); val y = p[0].toInt(); val m = p[1].toInt()
                if (m >= 12) String.format(Locale.US, "%04d-01-01", y + 1)
                else String.format(Locale.US, "%04d-%02d-01", y, m + 1)
            } catch (_: Throwable) { "$ym-31" }
            val days = try {
                ModuleAuth.getRowsChecked(
                    "wn", "notebook_days",
                    // 📊 V1204 — `outside_calls_manual`-ও এই একই পড়াতেই আসে, নতুন কোনো নেট-কল নয়।
                    "select=work_date,check_in,check_out,is_leave,leave_reason,is_wfh,is_other_branch,branch,outside_calls_manual" +
                        "&staff_code=eq.$code&work_date=gte.$from&work_date=lt.$end&order=work_date.asc"
                )
            } catch (_: Throwable) { null }
            val cfg = try {
                ModuleAuth.getRows("hr", "salary_config", "select=salary_amount&person_code=eq.$code&limit=1")
            } catch (_: Throwable) { JSONArray() }
            val prof = try {
                ModuleAuth.getRows("hr", "staff_profiles",
                    "select=full_name,branch,link_mobile,address&person_code=eq.$code&limit=1")
            } catch (_: Throwable) { JSONArray() }
            val amount = if (cfg.length() > 0) cfg.getJSONObject(0).optDouble("salary_amount", 0.0) else 0.0
            val pr = if (prof.length() > 0) prof.getJSONObject(0) else JSONObject()
            val homeBranch = ns(pr, "branch")

            val rows = ArrayList<AttRow>()
            var worked = 0
            val arr = if (days != null && days.ok) days.rows else JSONArray()
            for (i in 0 until arr.length()) {
                val d = arr.optJSONObject(i) ?: continue
                val iso = ns(d, "work_date").take(10)
                val ci = ns(d, "check_in"); val co = ns(d, "check_out")
                var tag = ""; var kind = ""
                var mins: Int
                var outMissing = false
                when {
                    d.optBoolean("is_leave", false) -> {
                        mins = (HourSalary.DAY_HOURS * 60).toInt()
                        val why = ns(d, "leave_reason")
                        tag = "LEAVE" + (if (why.isNotBlank()) " ($why)" else ""); kind = "lv"
                    }
                    d.optBoolean("is_wfh", false) -> {
                        mins = (HourSalary.DAY_HOURS * 60).toInt()
                        tag = "WORK FROM HOME"; kind = "wf"
                    }
                    /* 🚌 V1200 — আগে থেকে জানানো "অন্য ব্রাঞ্চে ডিউটি" ⇒ ৭ ঘণ্টা
                       (হিসাবের নিয়ম হুবহু `HourSalary`-রই)। */
                    d.optBoolean("is_other_branch", false) -> {
                        mins = (HourSalary.DAY_HOURS * 60).toInt()
                        tag = "DUTY \u00b7 " + ns(d, "branch").trim().uppercase(); kind = "br"
                    }
                    else -> {
                        val a = HourSalary.minutesOf(ci); val b = HourSalary.minutesOf(co)
                        /* ⏰ V1200 (TK-সিদ্ধান্ত) — IN আছে অথচ OUT নেই ⇒ **৭ ঘণ্টা**
                           (আগে ০ ছিল)। IN-ই না থাকলে আগের মতোই ০।
                           🐞🔒 V1455 (১৪.০৯.২০২৬, TK-রিপোর্ট — "আজকের তারিখে এখনও staff
                           চেম্বারে আছে, আর আপনি বানিয়েছেন Missing 7hr এটা তো ঠিক না") —
                           TK-র নিজের কথা ছিল ("চেম্বারে আসার পরে in time চাপবে, কিন্তু
                           সেই দিন যদি আউট টাইম চাপতে **ভুলে যায়**...") — অর্থাৎ এই নিয়ম
                           **দিন শেষ হয়ে যাওয়ার পরেও ভুলে-যাওয়া** দিনের জন্য, আজকের এখনো
                           চলতে-থাকা দিনের জন্য নয়। আজকের তারিখে এখনো OUT না চাপা মানে
                           "ভুলে গেছেন" নয় — দিনটাই শেষ হয়নি। ⇒ আজকের সারিতে এখন লাল
                           "MISSING"/আন্দাজি ৭ ঘণ্টা বসে না — এখন পর্যন্ত IN থেকে সত্যিকারের
                           যত সময় কেটেছে সেটাই দেখায় (OUT না দেওয়া পর্যন্ত), "—" (OUT TIME
                           ফাঁকা)। আজকের আগের যেকোনো দিনে (সত্যিই OUT ভুলে যাওয়া) আগের
                           নিয়ম (৭ ঘণ্টা, লাল MISSING) অটুট। */
                        val isTodayRow = iso == todayIso()
                        if (a != null && b == null && isTodayRow) {
                            val nowMin = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let {
                                it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE)
                            }
                            mins = if (nowMin > a) nowMin - a else 0
                        }
                        else if (a != null && b == null) { mins = (HourSalary.DAY_HOURS * 60).toInt(); outMissing = true }
                        else if (a == null || b == null || b <= a) { mins = 0 }
                        else mins = b - a
                        val br = ns(d, "branch")
                        if (tag.isBlank() && br.isNotBlank() && homeBranch.isNotBlank() &&
                            !br.trim().equals(homeBranch.trim(), ignoreCase = true)) {
                            tag = br.trim().uppercase(); kind = "br"
                        }
                    }
                }
                worked += mins
                rows.add(AttRow(
                    iso, dmy(iso),
                    if (d.optBoolean("is_leave", false) || d.optBoolean("is_wfh", false)) "\u2014" else attT12(ci),
                    if (d.optBoolean("is_leave", false) || d.optBoolean("is_wfh", false)) "\u2014" else attT12(co),
                    outMissing, mins, HourSalary.hoursText(mins), tag, kind
                ))
            }
            /* 📊🔒 V1204 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — মাসের পারফরম্যান্স।
               ⛔ গোনার নিয়ম **হুবহু WorkNotebook-এর `fetchStats("month")`-এর মতোই**
                 (শেষ ১০ অঙ্ক ধরে মেলানো, তিনটে তারিখ-ঘরের যেকোনোটা) — নতুন কোনো
                 নিয়ম বানানো হয়নি, তাই স্টাফের পর্দা ও এই কাগজ কখনো আলাদা হবে না।
               ⛔ পড়া ব্যর্থ হলে "…" বসে, মিথ্যা ০ নয়।
               ⛔ egress: মোট তিনটে ছোট গোনা-কল (count/HEAD), সারি টেনে আনা হয় না। */
            val perfObj: com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.Perf = run {
                val linkMobileRaw = ns(pr, "link_mobile")
                val mob10 = linkMobileRaw.filter { it.isDigit() }.takeLast(10)
                val monthOr = "or(createdAt.gte.$ym-01,date.gte.$ym-01,registrationDate.gte.$ym-01)"
                /* 🐞🔒 V1454 (১৪.০৯.২০২৬, TK-রিপোর্ট — "Enquiry কতগুলি করেছে সেটা কেন
                   দেখাচ্ছে না", COB-UTTAMA-র ছবিতে REGISTRATION ১৮ ঠিকই এসেছিল কিন্তু
                   NEW ENQUIRY "…" রয়ে গেছিল) — V1204-এর মন্তব্যে দাবি ছিল এই গোনা
                   "হুবহু WorkNotebook-এর fetchStats(month)-এর মতোই", কিন্তু আসল কোডে
                   ফাঁক ছিল: fetchStats()-এর নিজস্ব `countBoth()` প্রথম (উদার, `.like.*`)
                   ছাঁকনি ব্যর্থ হলে **পুরনো নির্ভুল (`.eq.`) ছাঁকনিতে আবার একবার**
                   চেষ্টা করে (দুটো সুযোগ) — এই পর্দায় সেই দ্বিতীয় সুযোগটাই বসানো হয়নি,
                   প্রথমবার ব্যর্থ হলেই সরাসরি "…"। এখন এখানেও ঠিক সেই একই দুই-ধাপের
                   পড়া — Registration-এর মতোই Enquiry-ও দ্বিতীয় সুযোগ পাবে। */
                val monthOld = "createdAt=gte.$ym-01&createdAt=lt.$ym-32"
                fun cnt(table: String, filter: String, oldFilter: String? = null): String = try {
                    val r = ModuleAuth.countPublicChecked(table, filter)
                    if (r.ok) r.count.toString()
                    else if (oldFilter != null) {
                        val r2 = ModuleAuth.countPublicChecked(table, oldFilter)
                        if (r2.ok) r2.count.toString() else "…"
                    } else "…"
                } catch (_: Throwable) { "…" }
                val enqTxt = if (mob10.length != 10) "…"
                    else cnt("enquiries", "and=(or(createdBy.like.*$mob10,receivedBy.like.*$mob10),$monthOr)",
                        "or=(createdBy.eq.$linkMobileRaw,receivedBy.eq.$linkMobileRaw)&$monthOld")
                val regTxt = if (mob10.length != 10) "…"
                    else cnt("patients", "and=(or(registeredBy.like.*$mob10,createdBy.like.*$mob10),$monthOr)",
                        "or=(registeredBy.eq.$linkMobileRaw,createdBy.eq.$linkMobileRaw)&$monthOld")
                val appTxt = try {
                    val r = ModuleAuth.getRowsChecked("wn", "call_taps",
                        "select=id&staff_code=eq.$code&call_date=gte.$from&call_date=lt.$end")
                    if (r.ok) r.rows.length().toString() else "…"
                } catch (_: Throwable) { "…" }
                var outSum = 0
                for (i in 0 until arr.length()) outSum += arr.optJSONObject(i)?.optInt("outside_calls_manual", 0) ?: 0
                var lv = 0
                for (i in 0 until arr.length()) if (arr.optJSONObject(i)?.optBoolean("is_leave", false) == true) lv++
                val totTxt = if (appTxt == "…") "…" else (appTxt.toInt() + outSum).toString()
                com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.Perf(
                    enqTxt, regTxt, appTxt, outSum.toString(), totTxt, lv.toString()
                )
            }
            val res = HourSalary.compute(arr, amount, ym)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                attSheetRows = rows
                attSheetPerf = perfObj   // 📊 V1204
                box.removeAllViews()
                box.addView(attPerfStrip(perfObj))   // 📊 V1204 — কাগজে যা, পর্দাতেও তাই
                box.addView(attSummary(res, worked))
                box.addView(attTable(code, rows))
                /* 🎨🔒 V1457 (TK-অনুমোদিত ফটো-প্রুফ) — TK: "print Whatsapp
                   chang Month এগুলি উপরে ডান সাইডে LG 3 dot এর মধ্যে থাকবে"।
                   আগের তিনটে বড় বোতাম বাদ, এখন হেডারের ⋮-এ (Salary Statement-এর
                   V1181-এর হুবহু একই প্যাটার্ন)। ⛔ প্রতিটা বোতামের কাজ এক
                   অক্ষরও বদলায়নি — একই ফাংশনই ডাকা হয়, শুধু জায়গা। */
                perfMenuAction = {
                    val items: List<Pair<String, () -> Unit>> = listOf(
                        "Print / PDF" to ({ attPrint(code, ym, pr, amount, res, worked) }),
                        "WhatsApp" to ({ attWhatsApp(code, ym, pr, res) }),
                        "Change month" to ({ attPickMonth(code) })
                    )
                    try {
                        val pm = android.widget.PopupMenu(this, perfMenuAnchor ?: box)
                        items.forEachIndexed { i, (label, _) -> pm.menu.add(0, i, i, label) }
                        pm.setOnMenuItemClickListener { mi -> items.getOrNull(mi.itemId)?.second?.invoke(); true }
                        pm.show()
                    } catch (_: Throwable) { }
                }
            }
        }.start()
    }

    private fun attSummary(res: HourSalary.HourPay, worked: Int): LinearLayout {
        fun cell(label: String, value: String, hex: String) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(11), dp(10), dp(11))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@StaffProfileActivity).apply {
                text = label; textSize = 10.5f
                setTextColor(android.graphics.Color.parseColor("#8B98A9"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@StaffProfileActivity).apply {
                text = value; textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(hex))
                setPadding(0, dp(4), 0, 0)
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#D9E8DF"))
            }
            addView(cell("WORKED", HourSalary.hoursText(worked), "#0A7C3F"))
            addView(cell("MONTH HOURS", res.monthHours.toInt().toString() + "h", "#16232E"))
            addView(cell("SALARY", money(res.payable), "#0E6E8C"))
        }
    }

    /** Google Sheet-এর মতো — প্রতিটা ঘরের চার দিকেই দাগ (TK-নির্দেশ)। */
    private fun attTable(code: String, rows: List<AttRow>): LinearLayout {
        val line = android.graphics.Color.parseColor("#C9D8CF")
        fun cellBox(text: String, weight: Float, bold: Boolean, hex: String, size: Float): TextView =
            TextView(this).apply {
                this.text = text; textSize = size
                gravity = android.view.Gravity.CENTER
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(hex))
                setPadding(dp(6), dp(8), dp(6), dp(8))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.TRANSPARENT); setStroke(dp(1), line)
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
            }
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        wrap.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#0B4F2A"))
            addView(cellBox("DATE", 1.15f, true, "#FFFFFF", 10.5f))
            addView(cellBox("IN TIME", 1f, true, "#FFFFFF", 10.5f))
            addView(cellBox("OUT TIME", 1f, true, "#FFFFFF", 10.5f))
            addView(cellBox("HOURS", 1.25f, true, "#FFFFFF", 10.5f))
        })
        if (rows.isEmpty()) {
            wrap.addView(ModuleUi.body(this, "No attendance in this month."))
            return wrap
        }
        for (r in rows) {
            val tr = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(android.graphics.Color.WHITE)
            }
            tr.addView(cellBox(r.date, 1.15f, true, "#17212B", 12f))
            tr.addView(cellBox(r.inTime, 1f, false, "#17212B", 12f))
            tr.addView(
                if (r.outMissing) cellBox("MISSING", 1f, true, "#C62828", 11.5f)
                else cellBox(r.outTime, 1f, false, "#17212B", 12f)
            )
            val hx = when (r.tagKind) { "lv" -> "#123E8C"; "wf" -> "#8A5A00"; "br" -> "#0A7C3F"; else -> "#17212B" }
            tr.addView(cellBox(r.hours + (if (r.tag.isBlank()) "" else "\n" + r.tag), 1.25f, true, hx,
                if (r.tag.isBlank()) 12f else 11f))
            /* ✏️ V1199 — মাস্টার সারিতে চাপলে ওই দিনের IN/OUT বদলাতে পারেন। */
            if (ModuleAuth.isMaster) {
                tr.isClickable = true
                tr.setOnClickListener { attEditDay(code, r) }
            }
            wrap.addView(tr)
        }
        return wrap
    }

    private fun attPickMonth(code: String) {
        val months = ArrayList<String>()
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        for (i in 0 until 12) {
            months.add(String.format(Locale.US, "%04d-%02d",
                c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1))
            c.add(java.util.Calendar.MONTH, -1)
        }
        val labels = months.map { salaryMonthLabel(it) }.toTypedArray()
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Choose month"))
            .setItems(labels) { _, which -> attendanceSheet(code, months[which]) }
            .setNegativeButton("Close", null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    /** ✏️ মাস্টার ওই দিনের IN / OUT বদলান — ঠিক সেই ঘরেই লেখে যেখানে
     *  Fix Attendance লেখে (`wn.notebook_days`)। ঘণ্টা নিজে থেকেই ঠিক হয়। */
    private fun attEditDay(code: String, r: AttRow) {
        if (!ModuleAuth.isMaster) return
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(4))
        }
        fun timeField(label: String, initial: String): Pair<TextView, android.widget.EditText> {
            val cap = TextView(this).apply {
                text = label; textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#8B98A9"))
                setPadding(0, dp(8), 0, dp(4))
            }
            val f = android.widget.EditText(this).apply {
                setText(if (initial == "\u2014") "" else initial)
                hint = "Tap to select time"
                isFocusable = false; isCursorVisible = false
                textSize = 14f
                setPadding(dp(12), dp(11), dp(12), dp(11))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(11).toFloat()
                    setColor(android.graphics.Color.parseColor("#FBFDFC"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#E7ECEA"))
                }
                setOnClickListener {
                    val c = java.util.Calendar.getInstance()
                    android.app.TimePickerDialog(this@StaffProfileActivity, { _, h, mi ->
                        val ap = if (h >= 12) "PM" else "AM"
                        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
                        setText(String.format(Locale.US, "%d.%02d %s", h12, mi, ap))
                    }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), false).show()
                }
            }
            return Pair(cap, f)
        }
        val (inCap, inF) = timeField("IN TIME", r.inTime)
        val (outCap, outF) = timeField("OUT TIME", r.outTime)
        box.addView(inCap); box.addView(inF); box.addView(outCap); box.addView(outF)
        box.addView(TextView(this).apply {
            text = "Hours will be counted from these two times."
            textSize = 11.5f
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setPadding(dp(2), dp(12), 0, 0)
        })
        /** "9.15 AM" → "09:15:00"; ফাঁকা হলে null (বদলাবে না)। */
        fun to24(v: String): String? {
            val t = v.trim().uppercase(Locale.US)
            if (t.isBlank()) return null
            return try {
                val pm = t.contains("PM")
                val core = t.replace("AM", "").replace("PM", "").trim().replace(".", ":")
                val p = core.split(":")
                var h = p[0].trim().toInt(); val mi = p[1].trim().toInt()
                if (pm && h < 12) h += 12
                if (!pm && h == 12) h = 0
                String.format(Locale.US, "%02d:%02d:00", h, mi)
            } catch (_: Throwable) { null }
        }
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Edit hours  \u00b7  " + r.date))
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val in24 = to24(inF.text.toString())
                val out24 = to24(outF.text.toString())
                if (in24 == null && out24 == null) {
                    ModuleUi.toast(this, "Nothing to save"); return@setPositiveButton
                }
                ModuleUi.toast(this, "Saving...")
                Thread {
                    val existing = try {
                        val rr = ModuleAuth.getRowsChecked("wn", "notebook_days",
                            "select=*&staff_code=eq.$code&work_date=eq." + r.dateIso + "&limit=1")
                        if (rr.ok && rr.rows.length() > 0) rr.rows.getJSONObject(0) else null
                    } catch (_: Throwable) { null }
                    val row = existing ?: JSONObject()
                        .put("staff_code", code).put("work_date", r.dateIso)
                        .put("manual_entries", JSONArray())
                    if (!row.has("manual_entries") || row.isNull("manual_entries")) row.put("manual_entries", JSONArray())
                    if (in24 != null) row.put("check_in", in24)
                    if (out24 != null) row.put("check_out", out24)
                    row.put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
                    var ok = try { ModuleAuth.upsertOnConflict("wn", "notebook_days", row, "staff_code,work_date") } catch (_: Throwable) { false }
                    if (!ok) ok = try { ModuleAuth.upsert("wn", "notebook_days", row) } catch (_: Throwable) { false }
                    runOnUiThread {
                        ModuleUi.toast(this, if (ok) "Saved" else "Could not save — try again")
                        if (ok) attendanceSheet(code, attSheetMonth)
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    /* 📊 V1204 — পর্দার উপরে পারফরম্যান্সের ছোট পট্টি। কাগজের বাক্সটার হুবহু
       একই ছয়টা ঘর, একই সংখ্যা — তাই পর্দা ও কাগজ কখনো আলাদা হতে পারে না। */
    private fun attPerfStrip(p: com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.Perf): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#F4F9F6"))
                setStroke(dp(1), android.graphics.Color.parseColor("#CFE3D4"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        wrap.addView(TextView(this).apply {
            text = "MONTHLY PERFORMANCE"; textSize = 9.5f
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        })
        fun cell(label: String, v: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@StaffProfileActivity).apply {
                text = label; textSize = 8.5f
                setTextColor(android.graphics.Color.parseColor("#6B7280"))
            })
            addView(TextView(this@StaffProfileActivity).apply {
                text = v.ifBlank { "-" }; textSize = 15f
                val bright = (v.toIntOrNull() ?: 0) > 0
                setTextColor(android.graphics.Color.parseColor(if (bright) "#0B2B59" else "#B9C0C8"))
                setTypeface(typeface, if (bright) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            })
        }
        val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        r1.addView(cell("NEW ENQUIRY", p.enquiries)); r1.addView(cell("REGISTRATION", p.registrations))
        r1.addView(cell("APP CALLS", p.appCalls))
        val r2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        r2.addView(cell("OUTSIDE CALLS", p.outsideCalls)); r2.addView(cell("TOTAL CALLS", p.totalCalls))
        r2.addView(cell("LEAVE DAYS", p.leaveDays))
        wrap.addView(r1); wrap.addView(r2)
        return wrap
    }

    /** 📊 V1204 — এই মাসের পারফরম্যান্স, ছাপা ও WhatsApp দুটোতেই একই সংখ্যা যায়। */
    private var attSheetPerf: com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.Perf? = null

    private fun attPrintRows(): List<com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.Row> =
        attSheetRows.map {
            com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.Row(
                it.date, it.inTime, it.outTime, it.outMissing, it.hours, it.tag, it.tagKind
            )
        }

    private fun attPrint(code: String, ym: String, pr: JSONObject, amount: Double, res: HourSalary.HourPay, worked: Int) {
        val html = com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.build(
            branchName = ns(pr, "branch"),
            staffName = ns(pr, "full_name").ifBlank { code },
            staffCode = code,
            mobile = ns(pr, "link_mobile"),
            address = ns(pr, "address"),
            monthLabel = salaryMonthLabel(ym),
            printedOn = dmy(todayIso()),
            rows = attPrintRows(),
            totalHours = HourSalary.hoursText(worked),
            monthHoursText = res.monthHours.toInt().toString() + "h 00m",
            salaryText = money(amount),
            rateText = money(res.ratePerHour),
            payableText = money(res.payable),
            perf = attSheetPerf                       // 📊 V1204
        )
        com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.print(this, code, salaryMonthLabel(ym), html)
    }

    private fun attWhatsApp(code: String, ym: String, pr: JSONObject, res: HourSalary.HourPay) {
        val txt = com.tkbiswas.pilesclinic.print.AttendanceSheetHtmlPrint.whatsAppText(
            ns(pr, "full_name").ifBlank { code }, code, ns(pr, "branch"),
            salaryMonthLabel(ym), attPrintRows(),
            HourSalary.hoursText(attSheetRows.sumOf { it.minutes }), money(res.payable),
            attSheetPerf                              // 📊 V1204
        )
        try {
            val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, txt)
                setPackage("com.whatsapp")
            }
            startActivity(i)
        } catch (_: Throwable) {
            try {
                startActivity(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, txt)
                })
            } catch (_: Throwable) { ModuleUi.toast(this, "WhatsApp not found") }
        }
    }

    /** 🗓️ Google Sheet-এর মতো হাজিরা-খাতা — Date · IN · OUT · Leave, একটা
     *  টেবিলে (TK-নির্দেশ ১৮.০৮.২০২৬)। "Days present"/"Leave days" দুটো
     *  থেকেই এটাই খোলে। */
    private fun perfShowAttendanceSheet(code: String, fullName: String, month: String, fromPerf: Boolean = false) {
        // 🔴 V511: মাস্টারের Performance পর্দা থেকে খুললে (`fromPerf`) Back-এ
        //   **ঐ পর্দাতেই** ফিরবে — আগে এক ধাপ বেশি পিছিয়ে যেত।
        //   ⛔ স্টাফ নিজের পর্দা থেকে খুললে `fromPerf` false, তাই আগের আচরণ
        //      এক অক্ষরও বদলায়নি (V509-এ যেভাবে ছিল, ঠিক সেভাবেই)।
        val prevBack = (if (fromPerf) perfListBack else null) ?: backAction
        backAction = { prevBack() }
        val col = ModuleUi.screen(this, "Performance Sheet")   // 📊 V1204
        col.addView(TextView(this).apply {
            text = fullName + "  \u00b7  " + perfLabel(month); textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(dp(2), 0, dp(2), dp(8))
        })
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "Loading...")) })
        col.addView(box)
        col.addView(ModuleUi.button(this, "Back") { backAction() })
        Thread {
            val r = try { ModuleAuth.rpc("hr", "perf_attendance_sheet", JSONObject().put("p_month", month).put("p_code", code)) } catch (_: Throwable) { null }
            val rows = if (r != null && r.ok) try { JSONArray(r.body) } catch (_: Throwable) { null } else null
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                box.removeAllViews()
                if (rows == null) {
                    box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "Could not load. Please try again.")) })
                    return@runOnUiThread
                }
                if (rows.length() == 0) {
                    box.addView(ModuleUi.card(this).apply { addView(ModuleUi.body(this@StaffProfileActivity, "No attendance record this period.")) })
                    return@runOnUiThread
                }
                val sheet = ModuleUi.card(this)
                val head = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(2), dp(4), dp(2), dp(8))
                }
                fun headCell(t: String, w: Float) = TextView(this).apply {
                    text = t; textSize = 11.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
                }
                head.addView(headCell("DATE", 1.1f))
                head.addView(headCell("IN", 1f))
                head.addView(headCell("OUT", 1f))
                head.addView(headCell("LEAVE", 0.8f))
                sheet.addView(head)
                sheet.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#D8ECDF"))
                })
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val isLeave = try { row.optBoolean("is_leave", false) } catch (_: Throwable) { false }
                    val tr = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(2), dp(7), dp(2), dp(7))
                        if (isLeave) setBackgroundColor(android.graphics.Color.parseColor("#FDF1E7"))
                    }
                    fun cell(t: String, w: Float, color: String) = TextView(this).apply {
                        text = t; textSize = 12.5f; setTextColor(android.graphics.Color.parseColor(color))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
                    }
                    /* 🔴🔒 V1162 (০৭.০৯.২০২৬, TK-নির্দেশ) — হাজিরার এই তালিকায়
                       সময় দুটো **কাঁচা ২৪-ঘণ্টার লেখাই** বসত (`09:15` / `09:15:00`),
                       অথচ প্রকল্পের বাকি সব জায়গায় `9.15 AM`। ⇒ এখন এখানেও এক নিয়ম।
                       ⛔ ডেটাবেসে আগের মতোই ২৪-ঘণ্টা জমা থাকে — শুধু দেখানোটা বদলাল। */
                    tr.addView(cell(dmy(ns(row, "work_date")), 1.1f, "#123A26"))
                    tr.addView(cell(attTime12(ns(row, "check_in")).ifBlank { "\u2014" }, 1f, "#0A7C3F"))
                    tr.addView(cell(attTime12(ns(row, "check_out")).ifBlank { "\u2014" }, 1f, "#B42318"))
                    tr.addView(cell(if (isLeave) "\u2713" else "\u2014", 0.8f, if (isLeave) "#B45309" else "#5B6B81"))
                    sheet.addView(tr)
                    if (i < rows.length() - 1) sheet.addView(android.view.View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                        setBackgroundColor(android.graphics.Color.parseColor("#EEF4F1"))
                    })
                }
                box.addView(sheet)
            }
        }.start()
    }

    /* ═══════════════════════════════════════════════════════════════
       💰🔒 V1195 (০৭.০৯.২০২৬, TK-রিপোর্ট ও ফটো-প্রুফ পাশ, হুবহু):
         *"September আমি 2044 দেই নাই একবারও, তাহলে এখানে ৩ বার কেন দেখাচ্ছে"* ·
         *"একই পেমেন্ট আমি যখন তিনবার দিব তাহলে আমাকে আটকাবে না কেন?"* ·
         *"ভুল করে দিয়ে ফেললে সেটা ডিলিট করতে পারছি না কেন?"*

       কোডে মেপে দেখা গিয়েছিল — ঢোকানোর সময় **কোনো ডুপ্লিকেট-পাহারা ছিল না**,
       আর অঙ্কটা অ্যাপ নিজেই ঘরে বসিয়ে রাখত (V1178-এর ঘণ্টা-হিসাব), তাই না
       দেখে চাপ পড়লেই সেটা সেভ হয়ে যেত। তিনটেই এখানে সারানো হলো।
       ⛔ টাকার **হিসাবের নিয়ম** এক অক্ষরও বদলায়নি — শুধু পাহারা যোগ হলো। */
    private fun salaryAlreadyPaidRow(pays: JSONArray, ym: String): JSONObject? {
        for (i in 0 until pays.length()) {
            val p = pays.optJSONObject(i) ?: continue
            if (payKind(p) == "EXTRA") continue
            if (salaryPayMonth(p) == ym) return p
        }
        return null
    }

    /** ওই মাস আগে দেওয়া থাকলে **আটকে জিজ্ঞাসা করে**; নইলে সোজা কাজটা করে। */
    private fun confirmIfMonthPaid(pays: JSONArray, ym: String, go: () -> Unit) {
        val old = salaryAlreadyPaidRow(pays, ym)
        if (old == null) { go(); return }
        val msg = money(old.optDouble("amount", 0.0)) + "  ·  " +
            ns(old, "mode").ifBlank { "—" } + "  ·  " + dmy(ns(old, "paid_on")) +
            "\nAlready recorded for this month.\n\nAdd one more payment for " +
            salaryMonthLabel(ym) + "?"
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, salaryMonthLabel(ym) + " already paid"))
            .setMessage(msg)
            .setPositiveButton("Yes, add") { _, _ -> go() }
            .setNegativeButton("No", null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    /** ভুল সারি মোছা — আগে খাতায় নকল, তারপরই আসল সারি। ⛔ শুধু মাস্টার। */
    private fun deleteSalaryRow(code: String, row: JSONObject, after: () -> Unit) {
        val id = ns(row, "id")
        if (id.isBlank()) { ModuleUi.toast(this, "This entry has no id"); return }
        val title = if (payKind(row) == "EXTRA") "Delete this extra income?" else "Delete this salary payment?"
        val body = (if (payKind(row) == "EXTRA") "Extra" else salaryMonthLabel(salaryPayMonth(row))) +
            "  ·  " + money(row.optDouble("amount", 0.0)) + "  ·  " + ns(row, "mode").ifBlank { "—" } +
            "  ·  " + dmy(ns(row, "paid_on")) +
            "\nIt will be removed from the total, and who deleted it will be recorded."
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
            .setMessage(body)
            .setPositiveButton("Yes, delete") { _, _ ->
                ModuleUi.toast(this, "Deleting...")
                Thread {
                    val log = JSONObject()
                        .put("id", "sdl_" + System.currentTimeMillis() + "_" + (0..999).random())
                        .put("original_id", id)
                        .put("person_code", ns(row, "person_code").ifBlank { code })
                        .put("for_month", salaryPayMonth(row))
                        .put("amount", row.optDouble("amount", 0.0))
                        .put("mode", ns(row, "mode"))
                        .put("kind", payKind(row))
                        .put("paid_on", ns(row, "paid_on"))
                        .put("paid_by", ns(row, "paid_by"))
                        .put("remark", ns(row, "remark"))
                        .put("deleted_by", ModuleAuth.personCode)
                        .put("deleted_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
                    /* ⛔ খাতায় লেখা না গেলে **কিছুই মোছা হয় না** — নইলে টাকা
                       হারিয়ে যেত আর কে মুছল তার কোনো প্রমাণ থাকত না। */
                    val logged = ModuleAuth.insert("hr", "salary_deleted_log", log)
                    if (!logged) {
                        runOnUiThread { ModuleUi.toast(this, "Not deleted — run the V1195 SQL patch first") }
                        return@Thread
                    }
                    val gone = ModuleAuth.deleteRows("hr", "salary_payments", "id=eq." + id)
                    runOnUiThread {
                        salaryCacheClear(code)
                        ModuleUi.toast(this, if (gone) "Deleted" else "Could not delete — try again")
                        if (gone) after()
                    }
                }.start()
            }
            .setNegativeButton("No", null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    /** "এই মাসের বেতন দিন" — ছোট পূর্ণ-স্ক্রিন ফর্ম: Amount (prefilled=due) · Mode; for_month=এই মাস।
     *  ⛔ salary_payments-এ insert-এর কল আগের মতোই, শুধু `for_month` যোগ। */
    private fun payForMonth(code: String, monthYm: String, due: Double) {
        backAction = { salary(code) }
        val col = ModuleUi.screen(this, "Pay " + salaryMonthLabel(monthYm))
        val pamt = ModuleUi.numberInput(this, "Amount", allowDecimal = true).apply { if (due > 0) setText(due.toLong().toString()) }
        val pmode = spinner(listOf("Cash", "Online"))
        col.addView(ModuleUi.label(this, "Amount")); col.addView(pamt)
        col.addView(ModuleUi.label(this, "Mode")); col.addView(pmode)
        /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"Add Payment · Cancel এই দুইটা
           পাশাপাশি থাকবে, একটা যেন আরেকটার গায়ে ঘেঁষে না যায়। 'Add Payment'
           লেখা থাকবে না — 'Salary Payment' হবে।"*
           আগে দুটো বোতাম একটার নিচে একটা, গায়ে-গায়ে লেগে ছিল (দুটোই সবুজ বলে
           একটাই মোটা সবুজ চাকতির মতো দেখাত — ভুল বোতামে চাপ পড়ার ভয়)।
           এখন এক সারিতে সমান দুই ভাগ, মাঝে ফাঁক — ঠিক যে সাজ `editSalaryConfig`-এ
           TK আগেই পাশ করেছেন (Cancel · Save), হুবহু সেই একই নিয়ম।
           ⛔ সেভের নিয়ম · টাকার হিসাব · database — এক অক্ষরও বদলায়নি। */
        val payRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        val payCancel = ModuleUi.button(this, "Cancel") { salary(code) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        }
        val paySave = ModuleUi.button(this, "Salary Payment") {
            val row = JSONObject().put("person_code", code).put("paid_on", todayIso())
                .put("amount", pamt.text.toString().toDoubleOrNull() ?: 0.0)
                .put("mode", pmode.selectedItem.toString()).put("paid_by", ModuleAuth.personCode)
                .put("remark", "").put("for_month", monthYm)
            Thread {
                val ok = ModuleAuth.insert("hr", "salary_payments", row)
                runOnUiThread { salaryCacheClear(code); ModuleUi.toast(this, if (ok) "Payment added" else "Retry"); salary(code) }
            }.start()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            }
        }
        payRow.addView(payCancel)
        payRow.addView(paySave)
        col.addView(payRow)
    }

    /** 🟢 B629 (11.08.2026, TK-নির্দেশ): যেকোনো মাসের স্যালারি রেকর্ড — Master নিজে
     *  জয়েনিং ডেট থেকে History ভরতে পারবেন। ⛔ salary_payments-এ insert-এর কল
     *  payForMonth-এর হুবহু একই, শুধু মাসটা spinner থেকে বেছে নেওয়া। ডিজাইন/লেবেল
     *  বিদ্যমান ফর্মের মতোই (প্লেইন, emoji ছাড়া)। */
    private fun addSalaryAnyMonth(code: String, amount: Double, joinDate: String, pays: JSONArray) {
        backAction = { salary(code) }
        val col = ModuleUi.screen(this, "Add Salary — $code")
        val months = monthsFromJoin(joinDate)
        val paidSet = HashSet<String>()
        for (i in 0 until pays.length()) paidSet.add(salaryPayMonth(pays.getJSONObject(i)))
        val labels = months.map { salaryMonthLabel(it) + (if (paidSet.contains(it)) "  (Paid)" else "  (Due)") }
        val monthSpinner = spinner(labels)
        /* 💰 V1178 — চলতি মাসের ঘণ্টা-হিসাব জানা থাকলে **সেটাই** আগে বসে
           (TK-র নিয়ম: কম কাজ = কম বেতন)। না জানলে আগের মতোই সেট করা বেতন।
           ⛔ অঙ্কটা বদলানো যায় — মাস্টারের হাতেই শেষ সিদ্ধান্ত। */
        /* 💰🔒 V1195 (TK: *"September আমি 2044 দেই নাই একবারও"*) — অঙ্কটা আর
           **নিজে থেকে বসে না**; ঘরটা ফাঁকাই থাকে, প্রস্তাবটা শুধু নিচে হালকা
           লেখায় দেখানো হয়। ⇒ না দেখে চাপ পড়লে আর কোনো অঙ্ক সেভ হবে না। */
        val suggested = hourPayThisMonth ?: amount
        val pamt = ModuleUi.numberInput(this, "Amount", allowDecimal = true)
        val pmode = spinner(listOf("Cash", "Online"))
        /* 🗓️ V1248 — "কবে দিয়েছি"। ডিফল্ট আজ; চাপ দিলে ক্যালেন্ডার,
           সর্বোচ্চ আজ পর্যন্ত (ভবিষ্যতের তারিখ নয়)। */
        var paidOnIso = todayIso()
        val paidOnBtn = TextView(this).apply {
            text = salDmy(paidOnIso)
            textSize = 15.5f
            setTextColor(android.graphics.Color.parseColor("#101C2E"))
            isClickable = true
            setOnClickListener {
                val c = java.util.Calendar.getInstance()
                try {
                    c.set(paidOnIso.substring(0, 4).toInt(),
                        paidOnIso.substring(5, 7).toInt() - 1,
                        paidOnIso.substring(8, 10).toInt())
                } catch (_: Throwable) { }
                val dlg = android.app.DatePickerDialog(this@StaffProfileActivity, { _, y, m, d ->
                    paidOnIso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    text = salDmy(paidOnIso)
                }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                    c.get(java.util.Calendar.DAY_OF_MONTH))
                try { dlg.datePicker.maxDate = System.currentTimeMillis() } catch (_: Throwable) { }
                dlg.show()
            }
        }
        /* 🗓️🔒 V1248 (০৯.০৯.২০২৬, TK-রিপোর্ট ও ফটো-প্রুফ পাশ — খাতার সারি ৩৬৯):
           TK: *"সেলারিটা তাকে আমি আগেই দিয়েছি কিন্তু লিখতে ভুলে গেছিলাম — কিন্তু
           এখানে আগের ডেট দিয়ে গেল না, কেন এরকম হলো"*।
           **কারণ (কোডে মেপে):** এই ফর্মে **তারিখের কোনো ঘরই ছিল না**; সেভের সময়
           সবসময় `paid_on = আজ` বসত। শুধু "কোন মাসের বেতন" বাছা যেত, "কবে দিয়েছি"
           বাছার উপায় ছিল না। ⇒ এখন **Paid on** ঘরটা বসল — ডিফল্ট আজ, চাপ দিলে
           পিছনের যেকোনো দিন; **ভবিষ্যতের তারিখ বাছা যায় না**।
           🎨 সঙ্গে TK-র পাশ-করা প্রুফ অনুযায়ী সাজটাও Extra Income-এর মতো —
              সোনালি পট্টি · একটাই সাদা কার্ড · ঘরগুলো বক্সে · Paid on ও Mode
              পাশাপাশি। ⛔ সবগুলোই **আগে থেকে থাকা** সহায়ক (`salGoldHeader` ·
              `salBoxed` · `salPairRow`) — নতুন কিছু বানানো হয়নি।
           ⛔ কোন মাস বাছা যাবে · প্রস্তাবিত অঙ্ক · Mode · "আগে দেওয়া মাসে আবার
              দিলে জিজ্ঞাসা" — কিচ্ছু বদলায়নি; শুধু তারিখটা এখন হাতে বাছা যায়। */
        val card = ModuleUi.card(this)
        col.addView(card)
        card.addView(salGoldHeader("SALARY PAYMENT", ""))
        card.addView(ModuleUi.label(this, "Month")); card.addView(salBoxed(monthSpinner))
        card.addView(ModuleUi.label(this, "Amount")); card.addView(salBoxed(pamt))
        if (suggested > 0) card.addView(TextView(this).apply {
            text = "Suggested from this month's hours : " + money(suggested)
            textSize = 11.5f
            setTextColor(android.graphics.Color.parseColor("#8B98A9"))
            setPadding(dp(2), dp(6), 0, 0)
        })
        val paidOnCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        paidOnCol.addView(ModuleUi.label(this, "Paid on"))
        paidOnCol.addView(salBoxed(paidOnBtn))
        val modeCol2 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modeCol2.addView(ModuleUi.label(this, "Mode")); modeCol2.addView(salBoxed(pmode))
        card.addView(salPairRow(paidOnCol, modeCol2))
        /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"Add Payment · Cancel এই দুইটা
           পাশাপাশি থাকবে, একটা যেন আরেকটার গায়ে ঘেঁষে না যায়। 'Add Payment'
           লেখা থাকবে না — 'Salary Payment' হবে।"*
           আগে দুটো বোতাম একটার নিচে একটা, গায়ে-গায়ে লেগে ছিল (দুটোই সবুজ বলে
           একটাই মোটা সবুজ চাকতির মতো দেখাত — ভুল বোতামে চাপ পড়ার ভয়)।
           এখন এক সারিতে সমান দুই ভাগ, মাঝে ফাঁক — ঠিক যে সাজ `editSalaryConfig`-এ
           TK আগেই পাশ করেছেন (Cancel · Save), হুবহু সেই একই নিয়ম।
           ⛔ সেভের নিয়ম · টাকার হিসাব · database — এক অক্ষরও বদলায়নি। */
        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        val addCancel = ModuleUi.button(this, "Cancel") { salary(code) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        }
        val addSave = ModuleUi.button(this, "Salary Payment") {
            val idx = monthSpinner.selectedItemPosition
            if (idx < 0 || idx >= months.size) { ModuleUi.toast(this, "Choose a month"); return@button }
            val amt = pamt.text.toString().toDoubleOrNull() ?: 0.0
            if (amt <= 0) { ModuleUi.toast(this, "Enter amount"); return@button }
            val ym = months[idx]
            /* 🛡️ V1195 — ওই মাস আগে দেওয়া থাকলে **আগে জিজ্ঞাসা**, তারপরই সেভ। */
            confirmIfMonthPaid(pays, ym) {
                // 🗓️ V1248 — আজকের তারিখের বদলে স্টাফের বাছা তারিখ।
                val row = JSONObject().put("person_code", code).put("paid_on", paidOnIso)
                    .put("amount", amt).put("mode", pmode.selectedItem.toString())
                    .put("paid_by", ModuleAuth.personCode).put("remark", "").put("for_month", ym)
                Thread {
                    val ok = ModuleAuth.insert("hr", "salary_payments", row)
                    runOnUiThread { salaryCacheClear(code); ModuleUi.toast(this, if (ok) "Payment added" else "Retry"); salary(code) }
                }.start()
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            }
        }
        addRow.addView(addCancel)
        addRow.addView(addSave)
        card.addView(addRow)   // 🎨 V1248 — বোতাম দুটোও একই কার্ডের ভিতরে
    }

    /** জয়েনিং মাস থেকে চলতি মাস পর্যন্ত YYYY-MM তালিকা (নতুন-আগে)। join_date না
     *  থাকলে/ভুল হলে শেষ ২৪ মাস ধরা হয় — কখনো ফাঁকা রাখে না। */
    private fun monthsFromJoin(joinIso: String): List<String> {
        val out = ArrayList<String>()
        try {
            val cal = java.util.Calendar.getInstance()
            val cy = cal.get(java.util.Calendar.YEAR)
            val cm = cal.get(java.util.Calendar.MONTH) + 1
            val curIdx = cy * 12 + (cm - 1)
            val startIdx = try {
                val jy = joinIso.substring(0, 4).toInt()
                val jm = joinIso.substring(5, 7).toInt()
                (jy * 12 + (jm - 1)).coerceIn(curIdx - 240, curIdx)
            } catch (_: Exception) { curIdx - 23 }
            var idx = curIdx
            while (idx >= startIdx) {
                val y = idx / 12; val m = (idx % 12) + 1
                out.add("%04d-%02d".format(y, m))
                idx--
            }
        } catch (_: Exception) {}
        return out
    }

    /** ৩-ট্যাপে মাসিক বেতন/তারিখ এডিট — পূর্ণ-স্ক্রিন ফর্ম। ⛔ config upsert আগের মতোই। */
    private fun editSalaryConfig(code: String, enabled: Boolean, amount: Double, salaryDate: String) {
        backAction = { salary(code) }
        val col = ModuleUi.screen(this, "Edit Salary — $code")
        val en = spinner(listOf("disabled", "enabled")).apply { setSelection(if (enabled) 1 else 0) }
        val amt = ModuleUi.numberInput(this, "Salary Amount", allowDecimal = true).apply { if (amount > 0) setText(amount.toLong().toString()) }
        val sd = ModuleUi.input(this, "Salary Date (day of month)").apply { setText(salaryDate) }
        col.addView(ModuleUi.label(this, "Salary")); col.addView(en)
        col.addView(ModuleUi.label(this, "Amount")); col.addView(amt)
        col.addView(ModuleUi.label(this, "Salary Date")); col.addView(sd)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        val cancelButton = ModuleUi.button(this, "Cancel") { salary(code) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        }
        val saveButton = ModuleUi.button(this, "Save") {
            val row = JSONObject().put("person_code", code)
                .put("salary_enabled", en.selectedItem.toString() == "enabled")
                .put("salary_amount", amt.text.toString().toDoubleOrNull() ?: 0.0)
                .put("salary_date", sd.text.toString())
                .put("updated_by", ModuleAuth.personCode).put("updated_at", nowIso())
            Thread {
                /* 🔴🔒 V1432 (১৩.০৯.২০২৬, TK: "COB-ARMAN — যতবারই চেষ্টা করি Retry", তালিকা ৫৫০) —
                   আগে `upsert()` (PK `id` ধরে merge) ছিল, অথচ এই সারিতে id পাঠানো হয় না ⇒ প্রথমবার
                   INSERT হয়, **দ্বিতীয়বার থেকে** `person_code` unique-এ 409 ⇒ প্রতিবার "Retry"।
                   অর্থাৎ যে স্টাফের config একবার সেভ হয়ে গেছে, ফোনে তার বেতন আর এডিট করা যেত না
                   (সব স্টাফের বেলায় একই, নিয়ম ৭)। ওয়েব আগে থেকেই id খুঁজে update করে। এখন
                   `on_conflict=person_code` — থাকলে update, না থাকলে insert। */
                val ok = ModuleAuth.upsertOnConflict("hr", "salary_config", row, "person_code")
                runOnUiThread { salaryCacheClear(code); ModuleUi.toast(this, if (ok) "Saved" else "Retry"); salary(code) }
            }.start()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            }
        }
        actionRow.addView(cancelButton)
        actionRow.addView(saveButton)
        col.addView(actionRow)
    }

    /** See all payments — পূর্ণ তালিকা (পূর্ণ-স্ক্রিন)। */
    /** 🔵 V416 (TK-নির্দেশ): "কার কবে কত বেতন দেওয়া হয়েছে" দেখার পূর্ণ Statement।
     *  উপরে দুটো মোট (বেতন · বাড়তি), তারপর সব সারি নতুন থেকে পুরনো।
     *  ⛔ কোনো সারি বদলানো/মোছার পথ এখানে নেই — শুধু দেখা। */
    /* 🎨🔒 V443 (TK-অনুমোদিত 19.08.2026) — Salary Statement professional UI.
     *  ⛔ শুধু presentation বদলেছে: salary/extra/due হিসাব, sort order, database,
     *     save/edit/payment rule একটুও বদলায়নি।
     *  ✅ TK-র শেষ photo-proof অনুযায়ী Mode/HISTORICAL + Date ডানদিকে fixed
     *     column — সব মাসে একই সোজা রেখা বরাবর। */
    /**
     * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — Extra সারির পিছনের **রোগীর আইডি**।
     *
     * V418-এর SQL প্রতিটা স্বয়ংক্রিয় Extra সারিতে `src_key` লিখে রাখে —
     *      INC:<REG|TRT>:<patients.id>:<staff-code>
     * অর্থাৎ রোগীর আসল সারির আইডি ওখানেই আছে। তাই কোড/নাম মিলিয়ে আন্দাজ
     * করার দরকার নেই — সরাসরি ঠিক রোগীতে যাওয়া যায়।
     *
     * ⛔ শেষ `:`-এর পরেরটা staff-code, আর `INC:` ও stage বাদ দিলে মাঝেরটাই
     *    রোগীর আইডি — তাই আইডিতে `:` থাকলেও ভুল হয় না।
     * ⛔ হাতে বসানো Extra (`src_key` ফাঁকা) হলে ফাঁকা ফেরে — তখন কার্ডে চাপ
     *    দিলে কিছুই হয় না, আগের মতোই।
     */
    /* 🐞🔒 V1029 (০৩.০৯.২০২৬, TK-রিপোর্ট: *"কিসের জন্য পেল সেখানে ক্লিক করলে
       কোন কাজই হয় না"*) — কারণ (যাচাই করা): চাপার ব্যবস্থাটা কেবল তখনই বসত যখন
       সারিতে রোগীর সূত্র (`src_key`) লেখা থাকে। যে সারিগুলো হাতে ডেটাবেসে বসানো
       হয়েছিল, তাদের ওই ঘরটা ফাঁকা — তাই চাপ দিলে কিছুই হত না।
       ⇒ এখন ঘরটা ফাঁকা হলে **কারণের লেখা থেকেই রোগীর কোড** নেওয়া হয়
         (`Registration · KNE-22082026-001` → `KNE-22082026-001`), আর সেই কোড
         দিয়ে রোগীটা খুঁজে নিয়ে চাপার ব্যবস্থা বসে।
       ⛔ আগের পথটা এক অক্ষরও বদলায়নি — সূত্র থাকলে আগের মতোই সরাসরি চলে। */
    /* 🧹🔒 V1041 (TK: *"Manually approved by TK এর মানেটা আগে আমাকে একটু বোঝান তো"*)।
       ⚠️ **দোষ আমার** — ওই লেখাটা আমারই দেওয়া SQL থেকে ডেটাবেসে বসেছিল, TK-এর
       কাছে ওটার কোনো মানে ছিল না। ⇒ পর্দায় দেখানোর সময় ওটা সোজা ইংরেজিতে
       বদলে যায়: `Added by hand`।
       ⛔ ডেটাবেসের একটা অক্ষরও বদলানো হয় না (TK-কে কোনো SQL চালাতে হবে না) —
          শুধু **দেখানোর সময়** লেখাটা পরিষ্কার করা হয়।
       ⛔ কোড-খোঁজা (`extraPatientCodeFromReason`) মূল লেখাটাই পড়ে, তাই রোগী
          চেনার কাজে এটার কোনো প্রভাব নেই। */
    /* 🔴 V1043 (TK: *"auto unexpected লেখা আছে এক জায়গায় আবার লেখা added by hand"*)
       — TK ঠিক ধরেছেন: `⏰ AUTO UNEXPECTED · … · Added by hand` এক লাইনে দুটো
       উল্টো কথা হয়ে যাচ্ছিল (একটা বলে সময়টা অ্যাপ নিজে বুঝেছে, আরেকটা বলে
       হাতে বসানো)। তাই লেখাটা আর বদলে নয়, **একদম তুলে** দেওয়া হয় — সারিটা
       তখন অ্যাপের নিজের সারির মতোই দেখায়। */
    private fun cleanWhy(text: String): String =
        text.replace(Regex("\\s*[·|-]\\s*Manually approved by TK\\s*", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s*Manually approved by TK\\s*", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .trim('·')
            .trim()

    private fun extraPatientCodeFromReason(p: JSONObject): String {
        val why = ns(p, "extra_reason").trim()
        if (why.isBlank()) return ""
        for (raw in why.split("·")) {
            val t = raw.trim()
            // রোগীর কোড: BRANCH-DDMMYYYY-NNN  (যেমন KNE-22082026-001)
            if (Regex("^[A-Za-z]{2,4}-\\d{6,8}-\\d{2,4}$").matches(t)) return t
        }
        return ""
    }

    private fun extraPatientId(p: JSONObject): String {
        val key = ns(p, "src_key").trim()
        if (!key.startsWith("INC:")) return ""
        val rest = key.removePrefix("INC:")
        val firstColon = rest.indexOf(':')
        val lastColon = rest.lastIndexOf(':')
        if (firstColon < 0 || lastColon <= firstColon) return ""
        return rest.substring(firstColon + 1, lastColon).trim()
    }

    /**
     * 🔴 V511 — Extra সারিগুলোর রোগীর **নাম** এনে লাইনে বসানো (TK: *"হ্যাঁ, নাম
     * দেখান"*)। একটাই ছোট পড়া — যতগুলো আলাদা রোগী, তাদের `id,name,mobile`।
     * ⛔ ব্যর্থ হলে কিছুই বদলায় না — আগের মতো শুধু কোডই থাকে।
     * ⛔ নাম/মোবাইল পরে পপ-আপেও ব্যবহার হয়, তাই এখানেই জমা রাখা হয়।
     */
    // 🟣 V961 — তালিকার কার্ডে দেখানোর জন্য প্রতিটা স্টাফের এক্সট্রা ইনকাম।
    private var extraPaidByStaff: Map<String, Double> = emptyMap()
    private var extraDueByStaff: Map<String, Double> = emptyMap()

    private val extraPatientCache = HashMap<String, Pair<String, String>>()   // id → (name, mobile)

    /**
     * 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — **"কী কারণে টাকা দিচ্ছি, সেটা তো
     * বোঝা যাচ্ছে না।"**
     *
     * **আসল নিয়মটা (কোডে প্রমাণিত — `V418_INCENTIVE_AUTO_2026-08-17.sql`):**
     * Extra income তখনই তৈরি হয় যখন রোগীর সারিতে
     * `timeType = 'Unexpected Time'` — অর্থাৎ **অসময়ে আসা এনকোয়ারি**।
     * তারপর Registration Fee জমা পড়লে ₹১০০, আর প্রথম Advance/Treatment টাকা
     * জমা পড়লে আরও ₹৪০০।
     *
     * **সমস্যা যেটা ছিল:** SQL শুধু `"Registration · <কোড>"` লিখত — *Unexpected
     * Time* কথাটা **কোথাও লেখাই হত না**। তাই পপ-আপ খুলেও TK বুঝতে পারতেন না
     * কেন টাকাটা পাওনা।
     *
     * **এখন:** রোগীর `timeType` ঘরটা এখানে জমা রাখা হয় ও পপ-আপে দেখানো হয়।
     * ⛔ **একটাও বাড়তি cloud-read নয়** — উপরের একই ব্যাচ-পড়াতেই শুধু একটা
     *    সরু কলাম যোগ হয়েছে (`timeType`)। Free Plan-এ egress প্রায় শূন্য বাড়ে।
     * ⛔ কোনো SQL চালাতে হবে না · database-এ কিছু বদলায় না · টাকার অঙ্ক
     *    এক পয়সাও বদলায় না — এটা শুধু **দেখানোর** কাজ।
     */
    private val extraPatientTiming = HashMap<String, String>()               // id → timeType
    /* 🕐🔒 V1042 — id → timeSource ("auto" = অ্যাপ কল-তালিকা দেখে নিজে বুঝেছে,
       "hand" = স্টাফ হাতে বেছেছেন, ফাঁকা = পুরনো সারি, জানা নেই)। */
    private val extraPatientSrc = HashMap<String, String>()
    /* 🩺🔒 V1045 (TK: *"নাম মোবাইল নাম্বার এবং রোগের নাম থাকবে"*) — id → রোগ। */
    private val extraPatientDisease = HashMap<String, String>()

    /* 🕐🔒 V1042 (TK-নির্দেশ) — সময়ের ব্যাজ এক জায়গায়, তাই সব পর্দায় এক লেখা।
       ⛔ ঘরটা ফাঁকা হলে ব্যাজ হুবহু আগের মতোই থাকে। */
    private fun timeBadge(timing: String, src: String, longForm: Boolean = false): String {
        val t = timing.trim()
        if (!t.equals("Unexpected Time", ignoreCase = true))
            return if (t.isBlank()) "" else "\uD83D\uDD50 " + t.uppercase()
        return when (src.trim().lowercase()) {
            "auto" -> "\u23F0 AUTO UNEXPECTED" + (if (longForm) " TIME" else "")
            "hand" -> "\u270D\uFE0F UNEXPECTED (BY HAND)"
            else   -> "\u23F0 UNEXPECTED" + (if (longForm) " TIME" else "")
        }
    }

    /* 👤🔒 V1040 — `nameOnly = true` হলে লাইনে শুধু **👤 নাম · মোবাইল** বসে
       (Pay Extra Income পর্দার জন্য, যেখানে কারণটা উপরের লাইনেই আছে)।
       ⛔ default `false`, তাই Extra Income History-র লাইন এক অক্ষরও বদলায়নি। */
    /* 📅🔒 V1049 (TK ডেমো-"ক" পাশ করেছেন, ০৪.০৯.২০২৬) — রোগীর তিনটে ধাপের
       তারিখ ও সময়: এনকোয়ারি · রেজিস্ট্রেশন · ট্রিটমেন্টের টাকা।
       ⛔ রেজিস্ট্রেশনের সময় `patients.createdAt` — সেটা আগের পড়াতেই চলে আসে।
          এনকোয়ারি ও ট্রিটমেন্টের টাকার জন্য **দুটো ছোট পড়া** লাগে, তাই সেগুলো
          একবারেই (`id=in.(…)`) আনা হয়, প্রতি সারিতে নয়।
       ⛔ ব্যর্থ হলে কিছুই ভাঙে না — ঐ লাইনটা শুধু বসে না। */
    private val extraPatientReg = HashMap<String, String>()   // id → registration createdAt
    private val extraPatientEnq = HashMap<String, String>()   // id → enquiry createdAt
    private val extraPatientTrt = HashMap<String, String>()   // id → treatment payment createdAt
    /* 💰 V1050 — রোগী ধরে ধরে টাকার যোগ: [০]=Registration, [১]=Treatment, [২]=অন্য।
       ⛔ **Total সবসময় এই তিনটের যোগফলই** — উপরে দেখানো অঙ্ক আর নিচের Total
          কখনো আলাদা হবে না (কম্পিউটারে ঠিক এই নিয়মই, নিয়ম ৬.৬)। */
    private val xGroupSums = HashMap<String, DoubleArray>()
    private val xStepBoxes = mutableListOf<Pair<String, LinearLayout>>()
    /* 🧾🔒 V1155 (TK: *"১৫৬ আর ১৫৭ ফোনে করুন"*) — হাতে-লেখা কারণগুলো রোগী ধরে
       জমা থাকে, যাতে উপরের লাইন থেকে লেখাটা তুলে দিলেও **কিছু হারিয়ে না যায়** —
       ওটা নিচে "Other" ধাপের পাশেই বসে (কম্পিউটারে ঠিক এই নিয়মই)। */
    private val xGroupOther = HashMap<String, MutableList<Pair<String, Double>>>()
    private val xGroupDue = HashMap<String, Boolean>()      // 💰 V1050 — কিছু বাকি আছে কি
    private val xGroupState = HashMap<String, String>()     // DUE · PAID · PART DUE

    /** 🔴 V1162 — `"09:15"` বা `"09:15:00"` → `"9.15 AM"`। চেনা না গেলে যা এসেছে
     *  তাই ফেরে (কখনো ফাঁকা নয়)। ⛔ WorkNotebook-এর `displayTime12()`-এর একই নিয়ম। */
    private fun attTime12(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return ""
        val p = t.split(":")
        if (p.size != 2 && p.size != 3) return t
        val h24 = p[0].toIntOrNull() ?: return t
        val mi = p[1]
        val ampm = if (h24 < 12) "AM" else "PM"
        val h12 = when { h24 == 0 -> 12; h24 > 12 -> h24 - 12; else -> h24 }
        return "$h12.$mi $ampm"
    }

    /** `2026-08-22T21:14:00Z` → `22/08/2026 : 9.14 PM` (সময় না থাকলে শুধু তারিখ)। */
    private fun whenText(iso: String): String {
        val t = iso.trim()
        if (t.length < 10) return ""
        val d = t.substring(0, 10).split("-")
        if (d.size != 3) return ""
        val date = d[2] + "/" + d[1] + "/" + d[0]
        if (t.length < 16) return date
        val hh = t.substring(11, 13).toIntOrNull() ?: return date
        val mi = t.substring(14, 16)
        val ap = if (hh < 12) "AM" else "PM"
        var h12 = hh % 12
        if (h12 == 0) h12 = 12
        return date + " : " + h12 + "." + mi + " " + ap   // 🔴 V1158
    }

    /* 👤🔒 V1044 (TK: *"আমার মনে হয় পেশেন্ট এর নাম দরকার এখানে"*) — `nameViews`
       দিলে রোগীর নাম **নিজের আলাদা সারিতে** বসে, আর তখন নিচের লাইনে নামটা আর
       দ্বিতীয়বার জুড়ে দেওয়া হয় না। ⛔ না দিলে আচরণ হুবহু আগের মতোই। */
    private fun fillExtraPatientNames(
        rows: List<Triple<String, TextView?, JSONObject>>,
        nameOnly: Boolean = false,
        nameViews: List<Pair<String, TextView>> = emptyList()
    ) {
        if (rows.isEmpty() && nameViews.isEmpty()) return
        val ids = (rows.map { it.first } + nameViews.map { it.first })
            .filter { it.isNotBlank() }.distinct()
        val need = ids.filter { !extraPatientCache.containsKey(it) }
        /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ): লাইনটা এখন **প্রতিবার নতুন করে
           বানানো হয়** (আগে শেষে জুড়ে দেওয়া হত)। কারণ এখন সামনে ⏰ চিহ্নও বসে,
           আর একই লাইন দুবার আঁকা হলে চিহ্ন/নাম দুবার বসে যেত।
           ফল: `⏰ UNEXPECTED  ·  Registration · COB-…  ·  NUR ALAM MIYA`
           ⛔ তথ্য সবই আগে থেকেই আনা — নতুন কোনো cloud-read নেই। */
        fun paint() {
            // 👤 V1044 — নাম নিজের সারিতে
            for ((pid, v) in nameViews) {
                val nm = extraPatientCache[pid]?.first.orEmpty().trim()
                if (nm.isBlank()) continue
                /* 🩺🔒 V1045 (TK-নির্দেশ) — নাম · মোবাইল · রোগ একসাথে।
                   ⛔ যেটা জানা নেই সেটা বসেই না, আন্দাজে কিছু লেখা হয় না। */
                val mb = extraPatientCache[pid]?.second.orEmpty().trim()
                val ds = extraPatientDisease[pid].orEmpty().trim()
                /* 🎨🔒 V1183 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু):
                   *"রোগের নামের পাশে থাকবে Unexpected Time"* ⇒ সময়ের ব্যাজটা
                   এখন রোগের ঠিক পাশেই বসে। ⛔ নিচের লাইন থেকে ওটা তুলে দেওয়া
                   হয়েছে (একই কথা দুবার লেখা হত)। ⛔ কোনো নতুন পড়া লাগেনি —
                   `timeType` আগের ব্যাচ-পড়াতেই আসে। */
                val tb = timeBadge(
                    extraPatientTiming[pid].orEmpty(), extraPatientSrc[pid].orEmpty(), longForm = true
                )
                /* 📏🔒 V1639 (20.09.2026, TK): একই লাইনের লেখা যেন একে অন্যের
                   গায়ে না লাগে। নাম · মোবাইল · রোগ · Unexpected Time-এর মধ্যে
                   পরিষ্কার separator রাখা হলো এবং wrap হলে line spacing দেওয়া হলো।
                   ⛔ কোনো তথ্য/হিসাব/ক্লিকের কাজ বদলায়নি। */
                val infoParts = mutableListOf<String>()
                infoParts.add("\uD83D\uDC64 " + nm)
                if (mb.isNotBlank()) infoParts.add("\uD83D\uDCDE " + mb)
                if (ds.isNotBlank()) infoParts.add("\uD83E\uDE7A " + ds)
                if (tb.isNotBlank()) infoParts.add(tb)
                val one = infoParts.joinToString("   \u00b7   ")
                if (v.text?.toString() != one) v.text = one
                v.setLineSpacing(dp(3).toFloat(), 1.0f)
                v.maxLines = 3
                v.visibility = android.view.View.VISIBLE
                /* 👤 V1045 — নামের সারিতে চাপ ⇒ ঐ রোগীর পুরো ডিটেলস।
                   ⛔ কার্ডের নিজের চাপ (ছোট পপ-আপ) আগের মতোই আছে। */
                v.isClickable = true
                v.setOnClickListener { openPatientHistory(pid, mb) }
            }
            /* 📅 V1050 — ধাপগুলোর তারিখ · সময় · অঙ্ক। */
            for ((pid, box) in xStepBoxes) {
                box.removeAllViews()
                val sums = xGroupSums[pid] ?: doubleArrayOf(0.0, 0.0, 0.0)
                /* ⛔ `tv()` তালিকা-আঁকার ভিতরের নিজস্ব সহায়ক, এখান থেকে পাওয়া যায় না
                   (পাহারা ধরিয়ে দিল) — তাই এখানে সোজা TextView বানানো হয়। */
                fun cell(text: String, size: Float, hex: String, widthDp: Int): TextView =
                    TextView(this).apply {
                        this.text = text
                        textSize = size
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor(hex))
                        if (widthDp > 0) layoutParams =
                            LinearLayout.LayoutParams(dp(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)
                    }
                fun stepRow(label: String, whenIso: String, amt: Double) {
                    if (whenIso.isBlank() && amt <= 0.0) return
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(13), dp(5), dp(7), dp(5))
                    }
                    row.addView(cell(label, 11.8f, "#8B98A9", 104).apply {
                        setPadding(0, 0, dp(8), 0)
                    })
                    row.addView(cell(whenText(whenIso).ifBlank { "\u2014" }, 11.8f, "#33404F", 0).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setPadding(0, 0, dp(10), 0)
                        maxLines = 2
                    })
                    if (amt > 0.0) row.addView(cell(money(amt), 12f, "#C62828", 0).apply {
                        setPadding(dp(8), 0, 0, 0)
                    })
                    box.addView(row)
                }
                stepRow("Enquiry", extraPatientEnq[pid].orEmpty(), 0.0)
                stepRow("Registration", extraPatientReg[pid].orEmpty(), sums[0])
                stepRow("Treatment paid", extraPatientTrt[pid].orEmpty(), sums[1])
                /* 🧾 V1155 — হাতে-লেখা কারণ: প্রত্যেকটা নিজের সারিতে, লেখা সহ।
                   ⛔ লেখা জানা না গেলে আগের মতোই শুধু যোগফলের একটা সারি। */
                val others = xGroupOther[pid].orEmpty()
                if (others.isNotEmpty()) {
                    for ((txt, amt) in others) {
                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            setPadding(dp(13), dp(5), dp(7), dp(5))
                        }
                        row.addView(cell("Other", 11.8f, "#8B98A9", 104).apply {
                            setPadding(0, 0, dp(8), 0)
                        })
                        row.addView(cell(txt, 11.8f, "#33404F", 0).apply {
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            setPadding(0, 0, dp(10), 0)
                            maxLines = 2
                        })
                        if (amt > 0.0) row.addView(cell(money(amt), 12f, "#C62828", 0).apply {
                            setPadding(dp(8), 0, 0, 0)
                        })
                        box.addView(row)
                    }
                } else if (sums[2] > 0.0) stepRow("Other", "", sums[2])
            }
            val shownSeparately = nameViews.map { it.first }.toSet()   // 👤 V1044
            /* 🧾 V1155 — যে রোগীর ধাপ-বাক্স আঁকা হয়েছে। ⛔ নামের তালিকা ছাড়াও
               ডাকা হতে পারে (কোড ধরে দেরিতে জোড়া লাগার পথ), তাই দুটোই মেলানো হয়। */
            val xGrouped = xStepBoxes.map { it.first }.toSet()
            for ((pid, view, row) in rows) {
                if (view == null) continue
                val nm = extraPatientCache[pid]?.first.orEmpty().trim()
                val tt = extraPatientTiming[pid].orEmpty().trim()
                val why = cleanWhy(ns(row, "extra_reason").trim())
                if (nameOnly) {
                    if (nm.isBlank()) continue
                    val mb = extraPatientCache[pid]?.second.orEmpty().trim()
                    val one = "\uD83D\uDC64 " + nm + (if (mb.isNotBlank()) "  \u00B7  " + mb else "")
                    if (view.text?.toString() != one) view.text = one
                    view.visibility = android.view.View.VISIBLE
                    continue
                }
                /* 🧾🔒 V1155 (TK-নির্দেশ ১৫৬: *"patient ID লাগবে না"* · ১৫৭:
                   *"unexpected এর আগে আবার Registration কেন থাকবে?"*) — যে বাক্সে
                   ধাপগুলো নিচে তারিখ-সময় সহ বসে, সেখানে এই লাইনে এখন **শুধু সময়ের
                   ব্যাজ**। ধাপের নাম ও রোগীর কোড দুটোই নিচে/উপরে আগে থেকেই আছে,
                   তাই এখানে থাকলে দুবার হত। ⛔ কম্পিউটারে হুবহু এই নিয়মই (V1047·V1048)।
                   ⛔ হাতে-লেখা কারণ হারায় না — সেটা নিচে "Other" ধাপে বসে।
                   ⛔ রোগী চেনা না গেলে (বাক্স হয়নি) লাইনটা আগের মতোই থাকে। */
                /* 🎨 V1183 (TK-নির্দেশ) — সময়ের ব্যাজ এখন **উপরের নামের সারিতে**,
                   রোগের ঠিক পাশে। তাই এই লাইনটা এখানে আর কিছুই দেখায় না
                   (নইলে একই কথা দুবার লেখা হত)।
                   ⛔ লাইনটা মোছা হয়নি — শুধু লুকানো, তাই বাকি সব পথ অটুট। */
                if (pid in shownSeparately || pid in xGrouped) {
                    view.text = ""
                    view.visibility = android.view.View.GONE
                    continue
                }
                if (nm.isBlank() && tt.isBlank()) continue          // এখনো কিছুই আসেনি
                val parts = mutableListOf<String>()
                /* 🧾 V1046 (TK: *"Registration  UNEXPECTED"*) — নাম যখন নিজের
                   সারিতে বসে, তখন এই লাইনে আগে **কী কারণে**, তারপর সময়ের ব্যাজ।
                   ⛔ বাকি সব জায়গায় ক্রমটা আগের মতোই (ব্যাজ আগে)। */
                val newOrder = pid in shownSeparately
                if (!newOrder && tt.isNotBlank()) parts.add(timeBadge(tt, extraPatientSrc[pid].orEmpty()))
                if (why.isNotBlank()) parts.add(why)
                // ⛔ হাতে-লেখা মন্তব্য থাকলে সেটাও যেন হারিয়ে না যায় (আগের লাইনে ছিল)
                ns(row, "remark").trim().takeIf { it.isNotBlank() }?.let { parts.add(it) }
                if (newOrder && tt.isNotBlank()) parts.add(timeBadge(tt, extraPatientSrc[pid].orEmpty()))
                if (nm.isNotBlank() && pid !in shownSeparately) parts.add(nm)   // 👤 V1044
                val line = parts.joinToString("  ·  ")
                if (line.isNotBlank() && view.text?.toString() != line) view.text = line
            }
        }
        if (need.isEmpty()) { paint(); return }
        Thread {
            try {
                val list = need.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
                val rows2 = com.tkbiswas.pilesclinic.native.SupabaseClient.fetchListSlimOrNull(
                    "patients", "id=in.($list)", 500, "id,name,mobile,timeType,timeSource,disease,createdAt", order = "id.asc"
                )
                if (rows2 != null) {
                    for (i in 0 until rows2.length()) {
                        val o = rows2.optJSONObject(i) ?: continue
                        val id = o.optString("id", "")
                        if (id.isBlank()) continue
                        extraPatientTiming[id] = o.optString("timeType", "").trim()
                        extraPatientSrc[id] = o.optString("timeSource", "").trim()   // 🕐 V1042
                        extraPatientDisease[id] = o.optString("disease", "").trim()   // 🩺 V1045
                        extraPatientReg[id] = o.optString("createdAt", "").trim()     // 📅 V1050
                        extraPatientCache[id] = Pair(
                            o.optString("name", "").trim(),
                            o.optString("mobile", "").trim()
                        )
                    }
                }
            } catch (_: Throwable) { }
            /* 📅🔒 V1050 (TK ডেমো-"ক" পাশ) — এনকোয়ারির সময় ও ট্রিটমেন্টের টাকার
               সময়। দুটোই **একবারেই** আনা হয় (`in.(…)`), প্রতি সারিতে নয়।
               ⛔ ব্যর্থ হলে কিছুই ভাঙে না — ঐ লাইনটা শুধু বসে না।
               ⛔ ট্রিটমেন্টের শর্ত V418-এর SQL-এর সাথে হুবহু এক
                  (`payType='treatment'` ও অঙ্ক > ০), তাই টাকার নিয়ম আর পর্দার
                  লেখা কখনো আলাদা হবে না। */
            try {
                val mobs = ids.mapNotNull { extraPatientCache[it]?.second?.trim() }
                    .filter { it.length >= 10 }.distinct()
                if (mobs.isNotEmpty()) {
                    val inList = mobs.joinToString(",") { java.net.URLEncoder.encode("+91" + it.takeLast(10), "UTF-8") }
                    val eq = com.tkbiswas.pilesclinic.native.SupabaseClient.fetchListSlimOrNull(
                        "enquiries", "mobile=in.($inList)", 500, "id,mobile,date,createdAt", order = "createdAt.asc")
                    if (eq != null) {
                        for (i in 0 until eq.length()) {
                            val o = eq.optJSONObject(i) ?: continue
                            val m10 = o.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
                            if (m10.length != 10) continue
                            val pid2 = ids.firstOrNull {
                                extraPatientCache[it]?.second.orEmpty().filter { c -> c.isDigit() }.takeLast(10) == m10
                            } ?: continue
                            // সবচেয়ে পুরনোটাই প্রথম কল — তালিকা createdAt.asc, তাই একবারই বসে
                            if (extraPatientEnq[pid2].isNullOrBlank())
                                extraPatientEnq[pid2] = o.optString("createdAt", "").ifBlank { o.optString("date", "") }.trim()
                        }
                    }
                }
            } catch (_: Throwable) { }
            try {
                val inIds = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
                val py = com.tkbiswas.pilesclinic.native.SupabaseClient.fetchListSlimOrNull(
                    "payments", "patientId=in.($inIds)&payType=eq.treatment", 500,
                    "id,patientId,amount,date,createdAt", order = "createdAt.asc")
                if (py != null) {
                    for (i in 0 until py.length()) {
                        val o = py.optJSONObject(i) ?: continue
                        val pid2 = o.optString("patientId", "").trim()
                        if (pid2.isBlank()) continue
                        val amt = o.optString("amount", "").filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) continue
                        if (extraPatientTrt[pid2].isNullOrBlank())
                            extraPatientTrt[pid2] = o.optString("createdAt", "").ifBlank { o.optString("date", "") }.trim()
                    }
                }
            } catch (_: Throwable) { }
            runOnUiThread { if (!isFinishing && !isDestroyed) paint() }
        }.start()
    }

    /**
     * 🔴 V511 — Extra সারিতে চাপ দিলে ছোট পপ-আপ (TK-এর বাছা পথ)।
     * দেখায় — রোগীর নাম · মোবাইল · কেন এই টাকা · কত · কবে · অবস্থা।
     * নিচে **Open History** — চাপলে ঐ রোগীর পুরো Timeline খোলে।
     * ⛔ নাম এখনো না এলে (নেট) শুধু কোড দেখায়, তবু History খোলা যায়।
     */
    /**
     * 🔴 V511 — ঐ রোগীর পুরো History খোলা।
     * ⛔ `PatientTimelineActivity` **শুধু মোবাইল নম্বর** নেয় (ঐ ফাইলের ১৫৪ নম্বর
     *    লাইন — `patientRowId` বলে কিছু নেই)। তাই নম্বর জানা না থাকলে আগে
     *    একটাই ছোট পড়ায় নম্বরটা এনে **তারপর** খোলা হয় — নইলে ফাঁকা পর্দা খুলত।
     * ⛔ নম্বর না পাওয়া গেলে পরিষ্কার করে বলা হয়, চুপচাপ ফাঁকা পর্দা নয়।
     */
    private fun openPatientHistory(pid: String, knownMobile: String) {
        fun go(mobile: String) {
            if (mobile.isBlank()) {
                ModuleUi.toast(this, "Patient mobile not found")
                return
            }
            try {
                startActivity(android.content.Intent(
                    this, com.tkbiswas.pilesclinic.native.PatientTimelineActivity::class.java)
                    .putExtra("mobile", mobile))
            } catch (_: Throwable) { }
        }
        if (knownMobile.isNotBlank()) { go(knownMobile); return }
        Thread {
            var m = ""
            try {
                val enc = java.net.URLEncoder.encode(pid, "UTF-8")
                val rows = com.tkbiswas.pilesclinic.native.SupabaseClient.fetchListSlimOrNull(
                    "patients", "id=eq.$enc", 1, "id,name,mobile,timeType,timeSource,disease,createdAt", order = "id.asc")
                val o = if (rows != null && rows.length() > 0) rows.optJSONObject(0) else null
                if (o != null) {
                    m = o.optString("mobile", "").trim()
                    extraPatientTiming[pid] = o.optString("timeType", "").trim()
                    extraPatientSrc[pid] = o.optString("timeSource", "").trim()      // 🕐 V1042
                    extraPatientDisease[pid] = o.optString("disease", "").trim()     // 🩺 V1045
                    extraPatientReg[pid] = o.optString("createdAt", "").trim()       // 📅 V1050
                    extraPatientCache[pid] = Pair(o.optString("name", "").trim(), m)
                }
            } catch (_: Throwable) { }
            val mm = m
            runOnUiThread { if (!isFinishing && !isDestroyed) go(mm) }
        }.start()
    }

    private fun showExtraPatientPopup(p: JSONObject, pid: String) {
        val cached = extraPatientCache[pid]
        val name = cached?.first ?: ""
        val mob = cached?.second ?: ""
        val why = cleanWhy(ns(p, "extra_reason"))
        val amt = money(p.optDouble("amount", 0.0))
        val on = dmy(ns(p, "paid_on"))
        val status = if (payStatus(p) == "DUE") "DUE (not paid yet)" else "PAID"

        /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"কী কারণে দিচ্ছি সেটা তো বোঝা
           যাচ্ছে না… আমি বুঝবো কী করে যে স্টাফটা কী কারণে টাকা নিচ্ছে।"*
           নিচের তিনটে নতুন লাইন সেটাই বলে দেয় — Timing · কোন ধাপ · নিয়মটা।
           ⛔ সবই ইতিমধ্যেই আনা তথ্য থেকে; নতুন কোনো cloud-read নেই। */
        val timing = extraPatientTiming[pid].orEmpty().trim()
        val isUnexpected = timing.equals("Unexpected Time", ignoreCase = true)
        // `extra_reason` SQL-এ লেখা হয় `Registration · <কোড>` বা `Treatment · <কোড>`
        val stage = why.substringBefore("·").trim()
        val stageLine = when {
            stage.equals("Registration", true) ->
                "Registration Fee received  →  ₹100"
            stage.equals("Treatment", true) ->
                "First Advance / Treatment payment received  →  ₹400"
            else -> ""
        }

        val sb = StringBuilder()
        if (name.isNotBlank()) sb.append("Patient:  ").append(name).append("\n\n")
        if (mob.isNotBlank()) sb.append("Mobile:  ").append(mob).append("\n\n")
        // ⏰ সবচেয়ে জরুরি লাইন — এটাই না থাকায় TK কিছু বুঝতে পারতেন না
        if (timing.isNotBlank()) {
            val shown = timeBadge(timing, extraPatientSrc[pid].orEmpty(), longForm = true)   // 🕐 V1042
            sb.append("Timing:  ").append(shown).append("\n\n")
        }
        if (why.isNotBlank()) sb.append("For:  ").append(why).append("\n\n")
        if (stageLine.isNotBlank()) sb.append("Step:  ").append(stageLine).append("\n\n")
        sb.append("Amount:  ").append(amt).append("\n\n")
        sb.append("Date:  ").append(on).append("\n\n")
        sb.append("Status:  ").append(status)
        // নিয়মটা এক নজরে — TK যেন প্রতিবার মনে করার চেষ্টা না করেন
        if (isUnexpected) {
            sb.append("\n\n────────────\n")
            sb.append("Rule: only an UNEXPECTED TIME enquiry earns extra.\n")
            sb.append("₹100 when that number registers and pays the fee,\n")
            sb.append("₹400 more when the same patient pays an advance.\n")
            /* 🔵🔒 V532 (২২.০৮.২০২৬, TK-নির্দেশ) — **ভাগের হিসাবটা এখন সত্যি।**
               এতদিন এখানে সবসময় লেখা থাকত *"Shared 50-50"* — কিন্তু ডেটাবেসের
               আসল নিয়ম (`hr.incentive_wanted()`, V418 SQL) তা নয়:
                 • এনকোয়ারি ও রেজিস্ট্রেশন **দুজন আলাদা** স্টাফ ⇒ টাকা **দু'ভাগ**
                 • **একই লোক**, বা দু'জনের একজনকেই চেনা গেল ⇒ **পুরো টাকাটাই একজনের**
               (SQL-এ: `round(st.amt / s.n, 2)`, যেখানে `n` = আলাদা স্টাফ-কোডের সংখ্যা।)
               ⇒ n=1 হলে "50-50" লেখাটা **ভুল তথ্য** দিত।
               ⛔ নতুন কোনো cloud-read নেই — অঙ্কটা (`amount`) আগে থেকেই হাতে আছে,
                  আর ধাপ থেকে পুরো অঙ্ক (₹100 / ₹400) জানা। ভাগ = অঙ্ক ÷ পুরো অঙ্ক। */
            val fullAmt = when {
                stage.equals("Registration", true) -> 100.0
                stage.equals("Treatment", true) -> 400.0
                else -> 0.0
            }
            val got = p.optDouble("amount", 0.0)
            if (fullAmt > 0.0 && got > 0.0) {
                if (got >= fullAmt - 0.01) {
                    sb.append("\nThis entry: the FULL ").append(money(fullAmt))
                        .append(" — enquiry and registration by the same staff\n(or only one of the two could be identified).")
                } else {
                    sb.append("\nThis entry: ").append(money(got)).append(" of ").append(money(fullAmt))
                        .append(" — the rest goes to the other staff\n(enquiry and registration were done by two different people).")
                }
            } else {
                sb.append("\nShared between the enquiry staff and the registering staff\nwhen they are two different people.")
            }
        } else if (timing.isNotBlank()) {
            // এটা কখনো হওয়ার কথা নয় — হলে TK-কে জানানোই ঠিক, চুপ করে থাকা নয়
            sb.append("\n\n⚠️ This patient is not marked UNEXPECTED TIME.")
            sb.append("\nExtra income is only for unexpected-time enquiries —")
            sb.append("\nplease check this entry.")
        }
        /* ═══════════════════════════════════════════════════════════════
           🎨🔒 V1104 (০৫.০৯.২০২৬, TK: *"এটা আরো প্রফেশনাল লুক বানান"* —
           ফটো-প্রুফ দেখে পাশ) — লেখাগুলো এখন সাজানো কার্ডে: উপরে **টাকার
           অঙ্ক বড় করে** ও পাশে DUE/PAID ব্যাজ, নিচে সারি ধরে তথ্য, শেষে
           হলুদ বাক্সে নিয়মটা। TK-এর নির্দেশে **তারিখের পাশে সময়ও**।
           ⛔ একটাও তথ্য বাদ যায়নি — উপরের `sb`-তে যা যা ছিল সবই আছে।
           ⛔ দুটো বোতামের কাজ এক অক্ষরও বদলায়নি (Close · Open History)।
           ⛔ সময় জানা না গেলে শুধু তারিখই বসে (নতুন কোনো ডাক নেই)।
           ═══════════════════════════════════════════════════════════════ */
        val isDue = payStatus(p) == "DUE"
        /* 🕐 V1104 — TK: *"তারিখের পাশে সময় লাগবে"*। সময়টা সারির নিজের
           `createdAt` থেকে (আগে থেকেই আনা), তাই নতুন কোনো ডাক নেই।
           ⛔ সময় জানা না গেলে শুধু তারিখই বসে — আগের মতোই। */
        val dateLine = run {
            val raw = listOf(p.optString("createdAt", ""), p.optString("created_at", ""))
                .firstOrNull { it.trim().length >= 16 }.orEmpty().trim()
            val full = if (raw.length >= 16) whenText(raw) else ""
            if (full.isNotBlank()) full else on
        }

        fun xiRow(k: String, v: String, color: String = "#12271E"): View =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(9), 0, dp(9))
                addView(TextView(this@StaffProfileActivity).apply {
                    text = k; textSize = 12.5f
                    setTextColor(android.graphics.Color.parseColor("#8494A0"))
                    layoutParams = LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT)
                })
                addView(TextView(this@StaffProfileActivity).apply {
                    text = v; textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor(color))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(6), dp(16), 0)
        }
        // টাকার বাক্স
        body.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(android.graphics.Color.parseColor("#F1F8F4"))
                setStroke(dp(1), android.graphics.Color.parseColor("#DCEDE3"))
            }
            addView(LinearLayout(this@StaffProfileActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@StaffProfileActivity).apply {
                    text = "AMOUNT"; textSize = 11f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#5C7568"))
                })
                addView(TextView(this@StaffProfileActivity).apply {
                    text = amt; textSize = 25f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0B6B34"))
                })
            })
            addView(TextView(this@StaffProfileActivity).apply {
                text = if (isDue) "DUE" else "PAID"
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setTextColor(android.graphics.Color.parseColor(if (isDue) "#B0392B" else "#0B6B34"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(android.graphics.Color.parseColor(if (isDue) "#FDECEA" else "#E7F4EC"))
                    setStroke(dp(1), android.graphics.Color.parseColor(if (isDue) "#F2C6C0" else "#CFE9D8"))
                }
            })
        })
        // সারিগুলো
        if (name.isNotBlank()) body.addView(xiRow("Patient", name))
        if (mob.isNotBlank()) body.addView(xiRow("Mobile", mob, "#1A73E8"))
        if (timing.isNotBlank()) body.addView(xiRow("Timing",
            timeBadge(timing, extraPatientSrc[pid].orEmpty(), longForm = true),
            if (isUnexpected) "#B45309" else "#12271E"))
        if (why.isNotBlank()) body.addView(xiRow("For", why))
        if (stageLine.isNotBlank()) body.addView(xiRow("Step", stageLine))
        body.addView(xiRow("Date", dateLine))
        // নিয়ম ও টীকা — উপরের `sb`-র শেষ অংশটাই, হুবহু একই লেখা
        run {
            val tail = sb.toString().substringAfter("────────────", "").trim()
            if (tail.isNotBlank()) body.addView(TextView(this).apply {
                text = tail; textSize = 12.5f
                setLineSpacing(0f, 1.35f)
                setTextColor(android.graphics.Color.parseColor("#5B5233"))
                setPadding(dp(13), dp(11), dp(13), dp(11))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.parseColor("#FBF8EF"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#EFE4C7"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(8); bottomMargin = dp(4) }
            })
        }

        val d = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(
                this, "💰 Extra Income — why?"))
            .setView(android.widget.ScrollView(this).apply { addView(body) })
            .setNegativeButton("Close", null)
        d.setPositiveButton("Open History") { _, _ -> openPatientHistory(pid, mob) }
        d.show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    /* 🟣🔒 V961 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK: *"salary history ·
       extra income history — এগুলো আলাদা আলাদা রাখতে হবে"*।
       আগে একটাই বোতাম ছিল আর তালিকায় বেতন ও এক্সট্রা **মেশানো** থাকত
       ("All Entries")। এখন `only` দিয়ে বাছা যায় — "SALARY" · "EXTRA" · ""(সব)।
       ⛔ উপরের Summary-র তিনটে সংখ্যা **সবসময় গোটা তালিকা** থেকেই গোনা হয়
          (`pays`), তাই কোনো অঙ্ক বদলায় না — শুধু নিচের সারিগুলো ছাঁকা হয়। */
    private fun showAllPayments(code: String, pays: JSONArray, only: String = "") {
        val shownPays = if (only.isBlank()) pays else org.json.JSONArray().also { out ->
            for (i in 0 until pays.length()) {
                val r = pays.optJSONObject(i) ?: continue
                if (payKind(r) == only) out.put(r)
            }
        }
        backAction = { salary(code) }
        val col = ModuleUi.screen(this, "")
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true

        val green = android.graphics.Color.parseColor("#075B32")
        val green2 = android.graphics.Color.parseColor("#0A7C3F")
        val ink = android.graphics.Color.parseColor("#17212B")
        val muted = android.graphics.Color.parseColor("#5B6B81")
        val line = android.graphics.Color.parseColor("#DDE8E1")
        val danger = android.graphics.Color.parseColor("#C62828")

        fun bg(fill: String, stroke: String? = null, radius: Int = 14): android.graphics.drawable.GradientDrawable =
            android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(radius).toFloat()
                setColor(android.graphics.Color.parseColor(fill))
                if (stroke != null) setStroke(dp(1), android.graphics.Color.parseColor(stroke))
            }

        fun tv(
            textValue: String, size: Float, color: Int = ink,
            bold: Boolean = false, gravityValue: Int = android.view.Gravity.START
        ) = TextView(this@StaffProfileActivity).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            includeFontPadding = false
        }

        fun divider(vertical: Boolean = false): android.view.View = android.view.View(this).apply {
            setBackgroundColor(line)
            layoutParams = if (vertical)
                LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    topMargin = dp(8); bottomMargin = dp(8)
                }
            else LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(8); bottomMargin = dp(8)
            }
        }

        fun pill(textValue: String, kind: String): TextView {
            val fill: String
            val color: Int
            when (kind) {
                "DUE" -> { fill = "#FDE9EA"; color = danger }
                "PAID" -> { fill = "#E8F6ED"; color = green2 }
                else -> { fill = "#F0F2F4"; color = android.graphics.Color.parseColor("#49545E") }
            }
            return tv(textValue, 10.5f, color, bold = true, gravityValue = android.view.Gravity.CENTER).apply {
                setPadding(dp(7), dp(4), dp(7), dp(4))
                background = bg(fill, null, 6)
            }
        }

        fun friendlyDate(iso: String): String {
            return try {
                val src = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }
                // 🔴 V1158 — TK: সব জায়গায় একই চেহারা (31/12/2026)।
                val out = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }
                out.format(src.parse(iso.take(10)) ?: return dmy(iso))
            } catch (_: Throwable) { dmy(iso) }
        }

        fun shortMonth(ym: String): String {
            return try {
                val p = ym.split("-")
                val names = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                names[p[1].toInt() - 1] + " " + p[0]
            } catch (_: Throwable) { ym }
        }

        // ── totals: exact same rule as V442 ────────────────────────────────
        var totSalary = 0.0
        var totExtra = 0.0
        var totDue = 0.0
        var latestPaidOn = ""
        var newestSalaryMonth = ""
        var oldestSalaryMonth = ""
        for (i in 0 until pays.length()) {
            val p = pays.getJSONObject(i)
            val paidOn = ns(p, "paid_on").take(10)
            if (paidOn > latestPaidOn) latestPaidOn = paidOn
            if (payKind(p) == "EXTRA") {
                if (payStatus(p) == "DUE") totDue += p.optDouble("amount", 0.0)
                else totExtra += p.optDouble("amount", 0.0)
            } else {
                totSalary += p.optDouble("amount", 0.0)
                val ym = salaryPayMonth(p)
                if (newestSalaryMonth.isBlank() || ym > newestSalaryMonth) newestSalaryMonth = ym
                if (oldestSalaryMonth.isBlank() || ym < oldestSalaryMonth) oldestSalaryMonth = ym
            }
        }

        /* 📅🔒 V1641 (20.09.2026, TK review): “Up to” তারিখ এখন
           যে history খোলা আছে সেই filtered list থেকেই আসে। Extra Income
           History-তে কোনো Salary payment-এর পরের তারিখ আর ঢুকবে না। */
        var latestShownPaidOn = ""
        for (i in 0 until shownPays.length()) {
            val q = shownPays.optJSONObject(i) ?: continue
            val d = ns(q, "paid_on").take(10)
            if (d > latestShownPaidOn) latestShownPaidOn = d
        }

        // ── Header ──────────────────────────────────────────────────────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(10))
        }
        val back = tv("‹", 34f, green, bold = false, gravityValue = android.view.Gravity.CENTER).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(44))
            isClickable = true; isFocusable = true
            setOnClickListener { salary(code) }
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(tv("Salary Statement", 21f, green, bold = true))
        titleCol.addView(tv(code, 17f, green, bold = true).apply { setPadding(0, dp(2), 0, 0) })
        header.addView(back)
        header.addView(titleCol)
        col.addView(header)

        // ── Summary card ────────────────────────────────────────────────────
        /* 🔒👁️ V1321 (TK-নির্দেশ ১০.০৯.২০২৬, ডেমো-প্রুফ "B" পাশ) — Salary
           paid (total) ও Grand total paid ডিফল্টে ডট দিয়ে ঢাকা থাকে, পাশের
           👁 চাপলে দেখা যায় (দুটোই একসাথে খোলে/বন্ধ হয়)। Extra income
           paid/due অপরিবর্তিত — সবসময় খোলা। স্ক্রিন নতুন করে খুললে আবার
           ঢাকা অবস্থা থেকেই শুরু হয় (কোথাও জমা থাকে না)। */
        var moneyMasked = true
        val maskedValueViews = mutableListOf<TextView>()
        val maskedAmounts = mutableListOf<() -> Double>()
        fun maskText(amt: Double) = if (moneyMasked) "••••••" else money(amt)
        fun refreshMasked() {
            for (i in maskedValueViews.indices) maskedValueViews[i].text = maskText(maskedAmounts[i]())
        }
        fun eyeToggle(): TextView = tv("👁", 10.5f, muted, gravityValue = android.view.Gravity.CENTER).apply {
            setPadding(dp(5), dp(3), dp(5), dp(3))
            background = bg("#EEF3F0", "#DCE6E0", 6)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(6) }
            isClickable = true; isFocusable = true
            setOnClickListener { moneyMasked = !moneyMasked; refreshMasked() }
        }
        fun hiddenPill(): TextView = tv("🔒 hidden", 8.3f, android.graphics.Color.parseColor("#9A6B00")).apply {
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = bg("#FFF4E5", "#F0D6A6", 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(3) }
        }
        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg("#FFFFFF", "#CFE3D8", 15)
            elevation = 3f * resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        val summaryHead = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(11), dp(14), dp(11))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(android.graphics.Color.parseColor("#075B32"), android.graphics.Color.parseColor("#0A7C3F"))
            ).apply { cornerRadius = dp(12).toFloat() }
        }
        summaryHead.addView(tv("Summary", 17f, android.graphics.Color.WHITE, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val upto = if (latestShownPaidOn.isNotBlank()) "Up to " + friendlyDate(latestShownPaidOn) else "Current statement"
        summaryHead.addView(tv(upto, 10.5f, android.graphics.Color.WHITE, false, android.view.Gravity.CENTER).apply {
            setPadding(dp(9), dp(5), dp(9), dp(5))
            background = bg("#0B7040", "#33A36A", 8)
        })
        summary.addView(summaryHead)

        fun metric(label: String, value: String, valueColor: Int): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(12), dp(8), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(tv(label, 10.3f, ink))
            addView(tv(value, 16.5f, valueColor, bold = true).apply { setPadding(0, dp(5), 0, 0) })
        }
        /* 20.09.2026 — TK final instruction: Extra Income History খুললে Summary-তে
           শুধু Extra income paid + Extra income due থাকবে। Salary total, Grand total
           এবং Extra income total — কোনোটাই এই filtered screen-এ দেখানো হবে না।
           Salary History/All Entries-এর পুরনো Summary এক অক্ষরও বদলানো হয়নি। */
        if (only == "EXTRA") {
            val metrics = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(6), dp(4), dp(6), dp(8))
            }
            metrics.addView(metric("Extra income paid", money(totExtra), green2))
            metrics.addView(divider(vertical = true))
            metrics.addView(metric("Extra income due", money(totDue), if (totDue > 0.0) danger else muted))
            summary.addView(metrics)
        } else {
            // 🔒👁️ V1321 — "Salary paid (total)"-এর নিজস্ব বাক্স, ডট + 👁 + hidden ট্যাগসহ
            val salaryMetricBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(12), dp(8), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            salaryMetricBox.addView(tv("Salary paid (total)", 10.3f, ink))
            salaryMetricBox.addView(hiddenPill())
            val salaryValueRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, 0)
            }
            val salaryValueTv = tv(maskText(totSalary), 16.5f, green2, bold = true)
            maskedValueViews.add(salaryValueTv); maskedAmounts.add({ totSalary })
            salaryValueRow.addView(salaryValueTv)
            salaryValueRow.addView(eyeToggle())
            salaryMetricBox.addView(salaryValueRow)
            val metrics = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(6), dp(4), dp(6), 0)
            }
            metrics.addView(salaryMetricBox)
            metrics.addView(divider(vertical = true))
            metrics.addView(metric("Extra income paid", money(totExtra), green2))
            metrics.addView(divider(vertical = true))
            metrics.addView(metric("Extra income due", money(totDue), if (totDue > 0.0) danger else muted))
            summary.addView(metrics)
            summary.addView(divider().apply {
                (layoutParams as LinearLayout.LayoutParams).apply { leftMargin = dp(12); rightMargin = dp(12) }
            })
            val grand = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(14))
            }
            val grandLabelCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            grandLabelCol.addView(tv("Grand total paid", 14f, ink, bold = true))
            grandLabelCol.addView(hiddenPill())
            grand.addView(grandLabelCol)
            val grandValueTv = tv(maskText(totSalary + totExtra), 21f, green, bold = true, gravityValue = android.view.Gravity.END)
            maskedValueViews.add(grandValueTv); maskedAmounts.add({ totSalary + totExtra })
            val grandValueRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            grandValueRow.addView(grandValueTv)
            grandValueRow.addView(eyeToggle())
            grand.addView(grandValueRow)
            summary.addView(grand)
        }
        col.addView(summary)

        // ── All entries title ────────────────────────────────────────────────
        val entriesHead = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(7))
        }
        val entriesTitle = when (only) {          // 🟣 V961
            "SALARY" -> "Salary History"
            "EXTRA" -> "Extra Income History"
            else -> "All Entries"
        }
        entriesHead.addView(tv("$entriesTitle  (${shownPays.length()})", 16f, ink, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        entriesHead.addView(tv("Most recent", 12f, green, false, android.view.Gravity.END))
        col.addView(entriesHead)

        if (shownPays.length() == 0) {          // 🟣 V961
            col.addView(tv("No payments.", 14f, muted).apply { setPadding(dp(4), dp(18), dp(4), dp(18)) })
        }

        // 🔴 V511 — কোন সারিতে কোন রোগী; নাম এলে ঐ লাইনগুলোই হালনাগাদ হয়।
        /* 💰🔒 V1050 (TK ডেমো-"ক") — আঁকার **আগেই** রোগী ধরে ধরে টাকার যোগ।
           ⛔ ধাপ চেনা হয় `extra_reason`-এর প্রথম শব্দ থেকে — V418-এর SQL ওখানে
              `Registration` বা `Treatment` লেখে, তাই দুই জায়গার নিয়ম মেলে।
           ⛔ এক ধাপে একাধিক সারি থাকলে সেগুলো **যোগ** হয়, তাই Total আর উপরের
              অঙ্ক কখনো আলাদা হবে না (কম্পিউটারে ধরা দোষটা এখানেও ঠিক)। */
        xStepBoxes.clear(); xGroupSums.clear(); xGroupDue.clear(); xGroupState.clear()
        xGroupOther.clear()                                   // 🧾 V1155
        run {
            val seenCount = HashMap<String, Int>()
            val dueCount = HashMap<String, Int>()
            for (k in 0 until shownPays.length()) {
                val q = shownPays.optJSONObject(k) ?: continue
                if (payKind(q) != "EXTRA") continue
                val qp = extraPatientId(q); if (qp.isBlank()) continue
                val amt = q.optDouble("amount", 0.0)
                val head = cleanWhy(ns(q, "extra_reason")).trim().substringBefore("·").trim().lowercase()
                val arr = xGroupSums.getOrPut(qp) { doubleArrayOf(0.0, 0.0, 0.0) }
                when {
                    head.startsWith("registration") -> arr[0] += amt
                    head.startsWith("treatment") -> arr[1] += amt
                    else -> {
                        arr[2] += amt
                        // 🧾 V1155 — লেখাটাও রাখা হয়, নিচে "Other" ধাপে দেখানোর জন্য
                        val txt = cleanWhy(ns(q, "extra_reason")).trim()
                        if (txt.isNotBlank())
                            xGroupOther.getOrPut(qp) { mutableListOf() }.add(Pair(txt, amt))
                    }
                }
                seenCount[qp] = (seenCount[qp] ?: 0) + 1
                if (payStatus(q) == "DUE") dueCount[qp] = (dueCount[qp] ?: 0) + 1
            }
            for ((qp, n) in seenCount) {
                val d = dueCount[qp] ?: 0
                xGroupDue[qp] = d > 0
                xGroupState[qp] = if (d == 0) "PAID" else if (d == n) "DUE" else "PART DUE"
            }
        }
        // 🧾 V1050 — একই রোগীর বাক্স একবারই আঁকা হয়
        val xDrawn = HashSet<String>()
        val extraRows = mutableListOf<Triple<String, TextView?, JSONObject>>()
        val extraNameViews = mutableListOf<Pair<String, TextView>>()   // 👤 V1044
        // 🐞 V1029 — যাদের সূত্র ফাঁকা, শুধু রোগীর কোড আছে
        val pendingCodeRows = mutableListOf<Triple<String, TextView?, Pair<JSONObject, LinearLayout>>>()

        // ── Entry cards. Fixed MODE + DATE columns = one straight line. ────
        for (i in 0 until shownPays.length()) {          // 🟣 V961
            val p = shownPays.getJSONObject(i)
            // 🧾 V1050 — এই রোগীর বাক্স আগেই আঁকা হয়েছে
            if (payKind(p) == "EXTRA") {
                val dupPid = extraPatientId(p)
                if (dupPid.isNotBlank()) { if (!xDrawn.add(dupPid)) continue }
            }
            val isExtra = payKind(p) == "EXTRA"
            val isDue = isExtra && payStatus(p) == "DUE"
            val amountText = money(p.optDouble("amount", 0.0))
            val modeRaw = ns(p, "mode").trim()
            val modeText = when {
                isDue -> "DUE"
                modeRaw.isBlank() -> "—"
                else -> modeRaw
            }
            val dateText = dmy(ns(p, "paid_on"))
            val leftTitle = if (isExtra) "Extra" else salaryMonthLabel(salaryPayMonth(p))
            val why = if (isExtra) cleanWhy(ns(p, "extra_reason")) else ns(p, "remark")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(9))
                background = bg(if (isDue) "#FFF7F7" else "#FFFFFF", if (isDue) "#F2C8C8" else "#E0E8E3", 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6) }
            }
            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            // slim professional accent — no emoji/icon, keeps more width for aligned columns
            top.addView(android.view.View(this).apply {
                background = bg(if (isDue) "#D83A3A" else "#1B8A50", null, 2)
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(34)).apply { rightMargin = dp(9) }
            })

            val left = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            left.addView(tv(leftTitle, 12.7f, ink, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
            })
            left.addView(tv(amountText, 12.5f, if (isDue) danger else ink, bold = isDue, gravityValue = android.view.Gravity.END).apply {
                setPadding(dp(5), 0, dp(4), 0)
            })
            top.addView(left)

            // Fixed MODE column — all rows start/end at the same x position.
            val modeBox = LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(78), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = dp(2)
                }
            }
            modeBox.addView(pill(
                modeText,
                when {
                    isDue -> "DUE"
                    modeText.equals("Cash", true) || modeText.equals("Online", true) -> "PAID"
                    else -> "HIST"
                }
            ))
            top.addView(modeBox)

            // Fixed DATE column — exactly aligned in one straight vertical line.
            top.addView(tv(dateText, 11.7f, muted, false, android.view.Gravity.END).apply {
                layoutParams = LinearLayout.LayoutParams(dp(86), LinearLayout.LayoutParams.WRAP_CONTENT)
                maxLines = 1
            })
            /* 🧾🔒 V1046 (TK-নির্দেশ: *"আগে নাম, মোবাইল, রোগ · তারপর এর লাইনে
               Registration UNEXPECTED · তারপর কত টাকা পাবে"*) — Extra সারিতে
               টাকার সারিটা (`top`) এখন **সবার শেষে** বসে, তাই নিচেই নামানো হলো।
               ⛔ স্যালারির সারি হুবহু আগের মতোই — সেখানে এখনই বসে। */
            if (!isExtra) card.addView(top)

            /* 🗑️🔒 V1195 (TK-নির্দেশ ও ফটো-প্রুফ পাশ) — ভুল করে বসে যাওয়া সারি
               **মাস্টার** মুছতে পারেন। ⛔ নিশ্চিত করার প্রশ্নের পরেই · মোছার
               আগে খাতায় (hr.salary_deleted_log) কে-কখন-কী মুছল লেখা হয়।
               ⛔ স্টাফ/ডাক্তারের পর্দায় বোতামটা দেখাই যায় না। */
            if (!isExtra && ModuleAuth.isMaster && ns(p, "id").isNotBlank()) {
                val delRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.END
                    setPadding(0, dp(6), 0, 0)
                }
                delRow.addView(TextView(this@StaffProfileActivity).apply {
                    text = "Delete"
                    textSize = 11.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(danger)
                    setPadding(dp(14), dp(6), dp(14), dp(6))
                    background = bg("#FFFFFF", "#E8B4B4", 10)
                    isClickable = true
                    setOnClickListener {
                        deleteSalaryRow(code, p) { salary(code) }
                    }
                })
                card.addView(delRow)
            }

            val detail = when {
                isExtra && why.isNotBlank() -> why + (ns(p, "remark").takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
                !isExtra && why.isNotBlank() -> why
                !isExtra && modeText.equals("HISTORICAL", true) -> "Salary paid - confirmed by Master"
                else -> ""
            }
            /* 👤🔒 V1044 (TK-নির্দেশ) — রোগীর নাম এতদিন লাইনের একদম শেষে কোডের
               পরে বসত, চোখেই পড়ত না। এখন নিজের সারিতে, মোটা সবুজ লেখায়।
               ⛔ নাম না জানা গেলে সারিটা দেখাই যায় না (আগের মতোই)। */
            var nameView: TextView? = null
            if (isExtra) {
                nameView = tv("", 13.5f, android.graphics.Color.parseColor("#0F5132"), bold = true).apply {
                    setPadding(dp(13), dp(5), dp(2), 0)
                    visibility = android.view.View.GONE
                }
                card.addView(nameView)
            }
            var detailView: TextView? = null
            if (detail.isNotBlank()) {
                detailView = tv(detail, 10.8f, muted).apply {
                    setPadding(dp(13), dp(5), dp(2), 0)
                    maxLines = 2
                }
                card.addView(detailView)
            }
            /* 🧾🔒 V1050 (TK ডেমো-"ক" পাশ, ০৪.০৯.২০২৬) — **এক রোগী = এক বাক্স**।
               ধাপগুলো তারিখ-সময় সহ, যে ধাপের জন্য টাকা তার পাশেই অঙ্ক, নিচে Total।
               ⛔ রোগী চেনা না গেলে সারিটা আগের মতোই একা আঁকা হয় (নিচের `else`)।
               ⛔ উপরের Summary ও footer ছোঁয়া হয়নি — টাকার হিসাব অপরিবর্তিত। */
            val gPid = if (isExtra) extraPatientId(p) else ""
            if (isExtra && gPid.isNotBlank()) {
                val box = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    // V1641: last date/time row-এর নিচে স্থায়ী breathing space
                    setPadding(0, 0, 0, dp(8))
                }
                card.addView(box)
                xStepBoxes.add(Pair(gPid, box))
                val mineTot = xGroupSums[gPid]?.sum() ?: p.optDouble("amount", 0.0)
                val foot = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    /* 📏🔒 V1641 (20.09.2026, TK photo review): last date/time row
                       থেকে Total/DUE-কে আরও নিচে নামিয়ে পরিষ্কার vertical gap। */
                    setPadding(dp(13), dp(15), dp(2), dp(4))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(10) }
                }
                foot.addView(tv("Total " + money(mineTot), 15f,
                    if (xGroupDue[gPid] == true) android.graphics.Color.parseColor("#C62828")
                    else android.graphics.Color.parseColor("#0A7C3F"), bold = true))
                val statePill = pill(xGroupState[gPid] ?: modeText,
                    if (xGroupDue[gPid] == true) "DUE" else "PAID")
                foot.addView(statePill, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(12) })
                card.addView(foot)
            } else if (isExtra) card.addView(top)   // 🧾 V1046 — টাকা সবার শেষে
            /* 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — **কোন রোগীর জন্য এই টাকা।**
               TK-এর কথা: *"staff কিসের জন্য পেমেন্ট পাবে আমি কেন বুঝতে পারছি না।
               যেখানে ডিউ লেখা রয়েছে সেখানে চাপ দিলে যেন আমি বুঝতে পারি, এটা কোন
               পেশেন্টের জন্য সে এক্সট্রা পেমেন্ট পাবে। তার হিস্টরি যেন আমি সেখান
               থেকে একবারেই ক্লিক করলে রিডাইরেক্ট হতে পারি।"*
               ⛔ রোগীর আসল আইডি **আগে থেকেই জমা আছে** — `src_key` ঘরে
                  (`INC:REG:<patients.id>:<staff-code>`, V418-এর SQL)। তাই আন্দাজ
                  করতে হয় না, ঠিক ঐ রোগীতেই যাওয়া যায়।
               ⛔ TK-এর বাছা পথ (২১.০৮.২০২৬): আগে ছোট পপ-আপ (নাম · মোবাইল · কেন ·
                  কত · কবে), তারপর "Open History" বোতাম — ভুল রোগীতে চলে যাওয়ার ভয় নেই।
               ⛔ নাম দেখানোর জন্য একটাই ছোট পড়া (নিচে `fillExtraPatientNames`),
                  Egress-এ প্রভাব নগণ্য। */
            val pid = extraPatientId(p)
            if (isExtra && pid.isNotBlank()) {
                extraRows.add(Triple(pid, detailView, p))
                nameView?.let { extraNameViews.add(Pair(pid, it)) }   // 👤 V1044
                card.isClickable = true
                card.isFocusable = true
                card.setOnClickListener { showExtraPatientPopup(p, pid) }
            } else if (isExtra) {
                // 🐞 V1029 — সূত্র ফাঁকা: কোড দিয়ে পরে জোড়া লাগানো হবে
                val cd = extraPatientCodeFromReason(p)
                if (cd.isNotBlank()) pendingCodeRows.add(Triple(cd, detailView, Pair(p, card)))
            }
            col.addView(card)
        }

        // 🔴 V511 — সব সারি আঁকা হয়ে গেছে; এবার রোগীর নামগুলো এনে বসানো হয়
        //   (একটাই ছোট পড়া, ব্যর্থ হলে আগের মতোই শুধু কোড থাকে)।
        fillExtraPatientNames(extraRows, nameViews = extraNameViews)   // 👤 V1044

        /* 🐞🔒 V1029 — যে সারিগুলোর সূত্র ফাঁকা, তাদের রোগীর কোড দিয়ে একবারেই
           রোগীগুলো খুঁজে নেওয়া হয়; পাওয়া গেলে চাপার ব্যবস্থা বসে ও নামও আসে।
           ⛔ একটাই ছোট পড়া, শুধু দরকার হলে; ব্যর্থ হলে আগের মতোই কিছু বদলায় না। */
        if (pendingCodeRows.isNotEmpty()) {
            val codes = pendingCodeRows.map { it.first }.distinct()
            Thread {
                val found = try {
                    val list = codes.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
                    com.tkbiswas.pilesclinic.native.SupabaseClient.fetchListSlimOrNull(
                        "patients", "patientId=in.($list)", 500, "id,patientId,name,mobile,timeType",
                        order = "id.asc"
                    )
                } catch (_: Throwable) { null }
                val byCode = HashMap<String, String>()
                if (found != null) {
                    for (i in 0 until found.length()) {
                        val r = found.optJSONObject(i) ?: continue
                        val cd = ns(r, "patientId").trim()
                        val id = ns(r, "id").trim()
                        if (cd.isBlank() || id.isBlank()) continue
                        byCode[cd] = id
                        extraPatientCache[id] = Pair(ns(r, "name").trim(), ns(r, "mobile").trim())
                        extraPatientTiming[id] = ns(r, "timeType").trim()
                        extraPatientSrc[id] = ns(r, "timeSource").trim()             // 🕐 V1042
                        extraPatientDisease[id] = ns(r, "disease").trim()            // 🩺 V1045
                        extraPatientReg[id] = ns(r, "createdAt").trim()              // 📅 V1050
                    }
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val ready = mutableListOf<Triple<String, TextView?, JSONObject>>()
                    for ((cd, view, pair) in pendingCodeRows) {
                        val id = byCode[cd] ?: continue
                        val row = pair.first
                        val card = pair.second
                        card.isClickable = true
                        card.isFocusable = true
                        card.setOnClickListener { showExtraPatientPopup(row, id) }
                        ready.add(Triple(id, view, row))
                    }
                    if (ready.isNotEmpty()) fillExtraPatientNames(ready)
                }
            }.start()
        }

        // ── Footer summary ──────────────────────────────────────────────────
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = bg("#075B32", null, 13)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6); bottomMargin = dp(8) }
        }
        fun footerTile(label: String, value: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(tv(label, 10.5f, android.graphics.Color.parseColor("#D5EEE0")))
            addView(tv(value, 14.5f, android.graphics.Color.WHITE, bold = true).apply { setPadding(0, dp(4), 0, 0) })
        }
        footer.addView(footerTile("Total Entries", shownPays.length().toString()))   // 🟣 V961
        footer.addView(android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#75A98B"))
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(42)).apply { leftMargin = dp(7); rightMargin = dp(7) }
        })
        val period = if (oldestSalaryMonth.isNotBlank() && newestSalaryMonth.isNotBlank())
            shortMonth(oldestSalaryMonth) + " – " + shortMonth(newestSalaryMonth) else "—"
        footer.addView(footerTile("Period", period))
        footer.addView(android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#75A98B"))
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(42)).apply { leftMargin = dp(7); rightMargin = dp(7) }
        })
        /* 🐞🔒 V1453 (১৪.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — "Grand Total একই স্ক্রিনে
           উপর নিচে ২ যায়গায় কেন") — আসল কারণ (কোডে মিলিয়ে): "Total Entries"
           উপরের ফিল্টার (`only`) মেনে শুধু দেখানো তালিকাটাই গোনে (SALARY
           বাছলে ৮টা), কিন্তু "Net Paid" সবসময় **অফিল্টার্ড গোটা তালিকার**
           (totSalary + totExtra) যোগফল দেখাত — উপরের "Grand total paid"
           কার্ডের হুবহু কপি, আর "৮টা এন্ট্রি"-র পাশে যেটা মেলেই না। এখন
           "Net Paid" **ওই একই ফিল্টার** মেনে শুধু দেখানো তালিকার যোগফল
           দেখায় — SALARY-তে শুধু বেতন, EXTRA-তে শুধু দেওয়া এক্সট্রা, "All
           Entries"-এ আগের মতোই দুটো মিলিয়ে। উপরের Summary কার্ড (V961-এ
           TK-লক করা "সবসময় গোটা তালিকা") এক অক্ষরও বদলায়নি। */
        val netPaidValue = when (only) {
            "SALARY" -> totSalary
            "EXTRA" -> totExtra
            else -> totSalary + totExtra
        }
        footer.addView(footerTile("Net Paid", money(netPaidValue)))
        col.addView(footer)

        col.addView(ModuleUi.button(this, "Back") { salary(code) }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })
    }

    /** 🔵 V416 (TK-নির্দেশ): বেতনের বাইরে দেওয়া বাড়তি টাকা।
     *  ⛔ `kind='EXTRA'` হয়ে জমা হয় ⇒ বেতনের "বাকি কত" হিসাবে কখনো ঢোকে না।
     *  ⛔ `for_month` ফাঁকা রাখা হয় — বাড়তি টাকা কোনো মাসের বেতন নয়। */
    /* 🎨🔒 V1182 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — Extra Income
       ফর্মের চেহারা। উপরে সোনালি পট্টি, সবকিছু একটাই সাদা কার্ডে, ঘরগুলো
       বক্সের ভিতরে, আর "When" ও "Mode" পাশাপাশি।
       ⛔ যা সেভ হয় তার এক অক্ষরও বদলায়নি — একই সারি, একই ঘর, একই নিয়ম
          (`Paying now` = PAID, নইলে DUE)। শুধু বসার জায়গা ও সাজ। */
    private fun salBoxed(v: android.view.View): android.view.View {
        v.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(11).toFloat()
            setColor(android.graphics.Color.parseColor("#FBFDFC"))
            setStroke(dp(1), android.graphics.Color.parseColor("#D6DEE6"))
        }
        /* 📏🔒 V1195 (TK-নির্দেশ ও ফটো-প্রুফ পাশ: *"বক্সের উচ্চতা কিছু কিছু এত
           উচ্চতা কেন করেছেন?"*) — উপরে-নিচের ফাঁক ১২ → ৬dp, আর ঘরের নিজের
           সবচেয়ে-কম উচ্চতাও কমানো হলো। ⛔ লেখা · রং · কাজ কিছুই বদলায়নি। */
        v.setPadding(dp(13), dp(6), dp(13), dp(6))
        if (v is android.widget.EditText) {
            v.minHeight = dp(34)
            v.minimumHeight = dp(34)
        }
        /* 📏🔒 V1247 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — খাতার সারি ৩৬৮):
           TK: *"Paying now & Cash — ওই বক্সগুলির উচ্চতা কম করুন"*।
           **কারণ (মেপে দেখা):** এই দুটো ঘর Spinner। Android-এর নিজের
           `simple_spinner_dropdown_item` সাজে বন্ধ অবস্থাতেও ভিতরে অনেকটা
           ফাঁক রাখে, তাই বাক্স দুটো Amount/Reason-এর চেয়ে অনেক লম্বা দেখাত।
           ⇒ বাইরের উপর-নিচের ফাঁক ৬ → ২dp, আর নিজের সবচেয়ে-কম উচ্চতা ৩৪dp —
             অর্থাৎ Amount/Reason-এর সমান মাপে নামল।
           ⛔ **Amount ও Reason-এর বাক্স এক চুলও বদলায়নি** (TK নিজে ধরেছেন যে
              ওগুলো ঠিকই ছিল — আমার প্রুফেই ভুল আঁকা হয়েছিল, কোডে নয়)।
           ⛔ লেখা · রং · তালিকা · কী সেভ হয় — কিচ্ছু বদলায়নি; শুধু উচ্চতা।
           ⛔ `salBoxed` শুধু এই Extra Income ফর্মেই ব্যবহার হয় (মেপে দেখা),
              তাই অন্য কোনো পর্দার চেহারা ছোঁয়া হয়নি। */
        if (v is android.widget.Spinner) {
            v.setPadding(dp(13), dp(2), dp(13), dp(2))
            v.minimumHeight = dp(34)
        }
        return v
    }

    private fun addExtraIncome(code: String) {
        backAction = { salary(code) }
        val col = ModuleUi.screen(this, "Extra Income — $code")
        val card = ModuleUi.card(this)
        col.addView(card)
        val amt = ModuleUi.numberInput(this, "Amount", allowDecimal = true)
        val why = ModuleUi.input(this, "Reason (Bonus / Festival / Overtime)")
        val md = spinner(listOf("Cash", "Online"))
        // 🔵 V417 (TK-অনুমোদিত): এখনই দিচ্ছি, নাকি ঠিক করে রাখছি (পরে দেব)।
        val whenSpin = spinner(listOf("Paying now", "Pay later (Due)"))
        /* 🗓️🔒 V1273 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — খাতার সারি ৩৯৫):
           TK: *"Extra income কত তারিখে দেয়া হলো, তারিখ তো এখানে রাখতে হবে ·
           প্রকৃত টাকা দেয়ার তারিখ · আজকের তারিখ হতে পারে পূর্ববর্তী তারিখও হতে পারে"*।
           **কারণ (কোডে মেপে):** এই ফর্মে **তারিখের কোনো ঘরই ছিল না** — সেভের
           সময় সবসময় `paid_on = আজ` বসত। ⇒ আগে দেওয়া বাড়তি টাকা লিখতে গেলে
           তারিখ আজকেরই হয়ে যেত। হুবহু এই দোষটাই Add Salary ফর্মে ছিল, V1248-এ
           সারানো হয়েছে — এটা একই দোষের বাকি অর্ধেক (নিয়ম ৭)।
           ⇒ এখন **Paid on** ঘরটা বসল — ডিফল্ট আজ, চাপ দিলে পিছনের যেকোনো দিন;
             **ভবিষ্যতের তারিখ বাছা যায় না** (V1248-এর হুবহু একই নিয়ম)।
           🎨 সাজ Add Salary-র মতোই — "Paid on | Mode" পাশাপাশি, "When" উপরে
              পুরো চওড়ায় ⇒ দুই পর্দার নিচের সারি এক দেখায় (নিয়ম ৭ক-এর ২)।
           ⛔ "Pay later (Due)" বাছলে তারিখের ঘরটা লুকোয় — তখনো টাকা দেওয়াই
              হয়নি, তাই তারিখের কোনো মানে নেই; পরে "দেওয়া হলো" করলে আগের
              মতোই ওই দিনের তারিখ বসে (`payExtraDue` এক অক্ষরও বদলায়নি)।
           ⛔ কী সেভ হয় (`kind='EXTRA'` · `for_month` ফাঁকা · status · mode) —
              কিচ্ছু বদলায়নি, শুধু তারিখটা এখন হাতে বাছা যায়। */
        var exPaidOnIso = todayIso()
        val exPaidOnBtn = TextView(this).apply {
            text = salDmy(exPaidOnIso)
            textSize = 15.5f
            setTextColor(android.graphics.Color.parseColor("#101C2E"))
            isClickable = true
            setOnClickListener {
                val c = java.util.Calendar.getInstance()
                try {
                    c.set(exPaidOnIso.substring(0, 4).toInt(),
                        exPaidOnIso.substring(5, 7).toInt() - 1,
                        exPaidOnIso.substring(8, 10).toInt())
                } catch (_: Throwable) { }
                val dlg = android.app.DatePickerDialog(this@StaffProfileActivity, { _, y, mth, d ->
                    exPaidOnIso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, mth + 1, d)
                    text = salDmy(exPaidOnIso)
                }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                    c.get(java.util.Calendar.DAY_OF_MONTH))
                try { dlg.datePicker.maxDate = System.currentTimeMillis() } catch (_: Throwable) { }
                dlg.show()
            }
        }
        card.addView(salGoldHeader("EXTRA INCOME", ""))
        card.addView(ModuleUi.label(this, "Amount")); card.addView(salBoxed(amt))
        card.addView(ModuleUi.label(this, "Reason")); card.addView(salBoxed(why))
        // 🗓️ V1273 — "When" এখন উপরে পুরো চওড়ায় (আগে Mode-এর পাশে ছিল)।
        card.addView(ModuleUi.label(this, "When")); card.addView(salBoxed(whenSpin))
        /* 🎨 V1182 + V1273 — নিচের সারিতে দুটো বাছাই এক লাইনে (Add Salary-র মতো)। */
        val exPaidOnCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        exPaidOnCol.addView(ModuleUi.label(this, "Paid on")); exPaidOnCol.addView(salBoxed(exPaidOnBtn))
        val modeCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modeCol.addView(ModuleUi.label(this, "Mode")); modeCol.addView(salBoxed(md))
        card.addView(salPairRow(exPaidOnCol, modeCol))
        /* 🗓️ V1273 — "Pay later (Due)" বাছলে তারিখের ঘরটা লুকোয়। ⛔ ফর্ম খোলার
           সময় "Paying now"-ই বাছা থাকে, তাই শুরুতে ঘরটা দেখাই যায়। */
        whenSpin.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                exPaidOnCol.visibility = if (position == 0) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) { }
        }
        card.addView(ModuleUi.button(this, "Save Extra Income") {
            val v = amt.text.toString().toDoubleOrNull() ?: 0.0
            if (v <= 0.0) { ModuleUi.toast(this, "Enter an amount"); return@button }
            val r = why.text.toString().trim()
            if (r.isEmpty()) { ModuleUi.toast(this, "Enter a reason"); return@button }
            val payingNow = whenSpin.selectedItemPosition == 0
            /* 🗓️ V1273 — "এখনই দিচ্ছি" হলে মাস্টারের বাছা তারিখ; "পরে দেব" হলে
               আগের মতোই আজ (তখনো টাকা দেওয়াই হয়নি)। */
            val row = JSONObject().put("person_code", code)
                .put("paid_on", if (payingNow) exPaidOnIso else todayIso())
                .put("amount", v).put("mode", if (payingNow) md.selectedItem.toString() else "")
                .put("paid_by", ModuleAuth.personCode).put("remark", "")
                .put("for_month", "").put("kind", "EXTRA").put("extra_reason", r)
                .put("status", if (payingNow) "PAID" else "DUE")
            Thread {
                val ok = ModuleAuth.insert("hr", "salary_payments", row)
                runOnUiThread { salaryCacheClear(code); ModuleUi.toast(this, if (ok) "Extra income added" else "Retry"); salary(code) }
            }.start()
        })
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.button(this, "Back") { salary(code) })
    }

    private fun loadConfig(code: String, enabled: Spinner, amount: android.widget.EditText, sdate: android.widget.EditText) {
        Thread {
            val r = ModuleAuth.getRows("hr", "salary_config", "select=*&person_code=eq.$code&limit=1")
            if (r.length() > 0) {
                val c = r.getJSONObject(0)
                runOnUiThread {
                    enabled.setSelection(if (c.optBoolean("salary_enabled", false)) 1 else 0)
                    if (c.has("salary_amount")) amount.setText(c.optDouble("salary_amount", 0.0).toInt().toString())
                    sdate.setText(ns(c, "salary_date"))
                }
            }
        }.start()
    }

    private fun loadHistory(code: String, hist: LinearLayout) {
        Thread {
            val r = ModuleAuth.getRows("hr", "salary_payments", "select=*&person_code=eq.$code&order=paid_on.desc")
            runOnUiThread {
                hist.removeAllViews()
                if (r.length() == 0) hist.addView(ModuleUi.body(this, "No payments."))
                for (i in 0 until r.length()) {
                    val p = r.getJSONObject(i)
                    hist.addView(ModuleUi.body(this, ns(p, "paid_on") + " · " + money(p.optDouble("amount", 0.0)) + " · " + ns(p, "mode") + " · " + ns(p, "paid_by")))
                }
            }
        }.start()
    }

    // ---------- SELF ----------
    private fun renderSelf() {
        backAction = { finish() }
        val col = ModuleUi.screen(this, "My Profile")
        val box = ModuleUi.card(this); col.addView(box); box.addView(ModuleUi.body(this, "Loading..."))
        col.addView(ModuleUi.button(this, "Back") { finish() })
        Thread {
            // 🔴🔴🔴🔒 V1570 (১৭.০৯.২০২৬, TK-ভিডিওতে ধরা পড়া আসল বাগ) — এই পড়াটা
            // আগে `select=*&limit=1` ছিল, নিজের `person_code` দিয়ে ছাঁকা ছাড়াই —
            // শুধু RLS-এর "নিজেরটাই দেখা যায়" নিয়মের ভরসায়। মাস্টারের RLS
            // **সবার** সারি দেখতে দেয় বলে (ভুল করে "My Profile" খুললে) এই
            // `limit=1` এলোমেলো যেকোনো একজন স্টাফের সারি দিয়ে দিত (ORDER BY
            // নেই, আর ২৩ জনের `created_at`-ও হুবহু এক)। ModuleAuth.signIn()-এর
            // নিজের `uid`-ফিল্টার ফিক্সের পর এখন এটা ঘটার কথা না, কিন্তু এই
            // ঘরটাও নিজের person_code দিয়ে সরাসরি ছেঁকে দেওয়া হলো — RLS যতই
            // চওড়া হোক, এখন থেকে ভুল করে অন্য কারো সারি কখনো আসবে না।
            val myCodeFilter = ModuleAuth.personCode?.takeIf { it.isNotBlank() }
                ?.let { "&person_code=eq." + java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") } ?: ""
            val r = ModuleAuth.getRows("hr", "staff_profiles", "select=*$myCodeFilter&limit=1")
            /* 🔴🔒 V496 (২১.০৮.২০২৬, TK §৩) — **বেতন শুধু আসল staff-এর।**
               সার্ভারের `role_kind` দেখেই ঠিক হয় (ফোনের কথা নয়)। ডাক্তার ও
               ফিল্ড অফিসারের পর্দায় "My Salary" ভাগটাই আর আসবে না, এবং
               বেতনের দুটো cloud-read-ও করা হয় না (Egress-ও বাঁচে)।
               ⛔ ডেটাবেসের পুরোনো কোনো সারি মোছা হয়নি — শুধু দেখানো বন্ধ। */
            val myRoleKind = if (r.length() > 0) ns(r.getJSONObject(0), "role_kind") else ""
            val salaryAllowed = com.tkbiswas.pilesclinic.native.RoleRules.salaryAppliesToRoleKind(myRoleKind)
            val sc = if (salaryAllowed) ModuleAuth.getRows("hr", "salary_config", "select=*$myCodeFilter&limit=1") else JSONArray()
            /* 🔵🔒 V818 (২৯.০৮.২০২৬, TK-নির্দেশে Egress-এর পূর্ণ যাচাই) —
               এটা **"My Profile"** পর্দা, অর্থাৎ শুধু নিজের বেতনের হিসাব।
               কিন্তু পড়াটা ছিল `select=*&order=paid_on.desc` — **কোনো ছাঁকনি
               নেই, কোনো সীমা নেই**। সার্ভারের নিয়ম (`spay_read`) বলে
               মাস্টার **সবার** সারি দেখতে পান — তাই মাস্টার নিজের প্রোফাইল
               খুললেই **সব কর্মীর জীবনের সব বেতন-লেনদেন** নেমে আসত। প্রতি
               মাসে এটা বাড়তেই থাকত।
               ⛔ এখন নিজের `person_code` দিয়ে ছাঁকা, সর্বোচ্চ ৩০০ সারি
                  (২৫ বছরের মাসিক বেতনও ধরে যায়)।
               ⛔ ঘরগুলো `select=*`-ই রইল — ইচ্ছে করেই। এই সারির অনেকগুলো ঘর
                  (amount · paid_on · for_month · mode · paid_by · remark ·
                  extra_reason · kind) নানা জায়গায় পড়া হয়; ঘর ছেঁটে দিলে
                  কোথাও একটা ফাঁকা দেখানোর ঝুঁকি ছিল। ছাঁকনি + সীমাতেই
                  আসল সাশ্রয়টা হয়ে যায়, আর কোনো ভালো কাজ নষ্ট হয় না।
               ⛔ কর্মীর নিজের পর্দায় আচরণ হুবহু আগের মতোই (সার্ভার এমনিতেই
                  তাঁকে নিজের সারিই দিত)। */
            val myCode = if (r.length() > 0) ns(r.getJSONObject(0), "person_code") else ""
            val payScope = if (myCode.isNotBlank()) "&person_code=eq." + (try { java.net.URLEncoder.encode(myCode, "UTF-8").replace("+", "%20") } catch (_: Throwable) { myCode }) else ""
            val pays = if (salaryAllowed) ModuleAuth.getRows(
                "hr", "salary_payments", "select=*" + payScope + "&order=paid_on.desc&limit=300"
            ) else JSONArray()
            runOnUiThread {
                box.removeAllViews()
                val p = if (r.length() > 0) r.getJSONObject(0) else JSONObject()
                val desig = ns(p, "designation").ifBlank { ns(p, "role_kind") }
                val ph = ns(p, "photo_data")
                if (ph.isNotBlank()) {
                    val img = ModuleUi.image(this)
                    img.setImageBitmap(PhotoUtils.decodeDataUrl(ph))
                    box.addView(img)
                }
                box.addView(ModuleUi.body(this, ns(p, "person_code") + " · " + desig + " · " + ns(p, "branch")))
                box.addView(ModuleUi.body(this, ns(p, "full_name").ifBlank { "(name not set by Master yet)" }))
                box.addView(ModuleUi.body(this, "Mobile: " + ModuleUi.fullMobile(ns(p, "link_mobile"))))   // 🔵 V521 (TK)
                val altM = ns(p, "alt_mobile")
                if (altM.isNotBlank())
                    box.addView(ModuleUi.body(this, "Alternate Mobile: " + (if (altM.any { it.isDigit() }) ModuleUi.fullMobile(altM) else altM)))   // 🔵 V521 (TK)
                if (ns(p, "gender").isNotBlank() || ns(p, "blood_group").isNotBlank())
                    box.addView(ModuleUi.body(this, listOf(ns(p, "gender"), ns(p, "blood_group")).filter { it.isNotBlank() }.joinToString(" · ")))
                if (ns(p, "qualification").isNotBlank())
                    box.addView(ModuleUi.body(this, "Qualification: " + ns(p, "qualification")))
                if (ns(p, "gov_id_type").isNotBlank())
                    box.addView(ModuleUi.body(this, ns(p, "gov_id_type") + ": " + ModuleUi.maskIdLast4(ns(p, "gov_id_last4"))))

                /* 🗓️🔴 V509 (২১.০৮.২০২৬, TK-নির্দেশ — কাগজের হাজিরা-খাতার ছবিসহ):
                 *   *"staff এর এখানে attendance sheet এরকম থাকবে, যাতে সে দেখতে
                 *    পারে সারা মাসে কোন সময় এসেছে এবং কোন সময় ক্লিনিক থেকে গেছে,
                 *    কবে সে ছুটি নিয়েছিল।"*
                 *
                 * ─── ⛔ নতুন কিছু বানানো হয়নি ───────────────────────────────
                 * হাজিরা-খাতার পর্দাটা (`perfShowAttendanceSheet`) **আগে থেকেই
                 * তৈরি ও পরীক্ষিত** — DATE · IN · OUT · LEAVE, ঠিক TK-এর কাগজের
                 * খাতার মতোই ছক। এতদিন সেটা খুলত **শুধু মাস্টারের** "Staff
                 * Performance" পথ থেকে; স্টাফের নিজের পর্দায় ঢোকার দরজাই ছিল না।
                 * এখানে শুধু **সেই দরজাটা** বসানো হলো।
                 *
                 * ⛔ স্টাফ **শুধু নিজের** খাতা দেখেন — নিজের `person_code` ছাড়া
                 *    কিছু পাঠানোই হয় না, আর সার্ভারের নিয়মও (সঙ্গের SQL প্যাচ
                 *    V509_MY_ATTENDANCE_SHEET) নিজের কোড ছাড়া অন্য কারও সারি
                 *    ফেরত দেয় না। মাস্টারের ক্ষমতা এক অক্ষরও বদলায়নি।
                 * ⛔ বেতন · Fix Attendance · অন্য স্টাফের তথ্য — কিছুই খোলে না।
                 * ⚡ Egress: একটা মাস দেখলে **একটাই ছোট RPC** (সর্বোচ্চ ৩১ সারি,
                 *    ৪টে ঘর) — খরচ নগণ্য, চাপ দিলে তবেই যায়।
                 */
                val myCode = ns(p, "person_code")
                if (myCode.isNotBlank()) {
                    box.addView(ModuleUi.button(this, "My Attendance Sheet") {
                        val ym = SimpleDateFormat("yyyy-MM", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
                            .format(java.util.Date())
                        perfShowAttendanceSheet(myCode, ns(p, "full_name").ifBlank { myCode }, ym)
                    })
                }
                // 🔴🔒 V496 (TK §৩): ডাক্তার/ফিল্ড হলে বেতনের ভাগটাই বসে না।
                if (salaryAllowed) {
                    val sbox = ModuleUi.card(this); col.addView(sbox)
                    sbox.addView(ModuleUi.heading(this, "My Salary"))
                    if (sc.length() > 0 && sc.getJSONObject(0).optBoolean("salary_enabled", false)) {
                        val c = sc.getJSONObject(0)
                        sbox.addView(ModuleUi.body(this, "Salary: " + money(c.optDouble("salary_amount", 0.0)) + " (day " + ns(c, "salary_date") + ")"))
                    } else sbox.addView(ModuleUi.body(this, "Salary not enabled."))
                    sbox.addView(ModuleUi.heading(this, "History"))
                    if (pays.length() == 0) sbox.addView(ModuleUi.body(this, "No payments."))
                    for (i in 0 until pays.length()) {
                        val pp = pays.getJSONObject(i)
                        sbox.addView(ModuleUi.body(this, ns(pp, "paid_on") + " · " + money(pp.optDouble("amount", 0.0)) + " · " + ns(pp, "mode")))
                    }
                }
            }
        }.start()
    }

    /* ═══════════════════════════════════════════════════════════════════
       👥🔒 V746 (২৭.০৮.২০২৬) — **মাস্টার নিজে স্টাফ ও ডাক্তার যোগ / বাদ /
       ফেরাতে পারবেন** — SQL ছাড়া, নতুন APK ছাড়া।
       —————————————————————————————————————————————————————————————————
       ⛔ **একটাও নিয়ম এখানে যাচাই হয় না** — সব সার্ভারে
          (`00_SQL/V745_STAFF_DOCTOR_FROM_APP.sql`)। ফোনের অ্যাপ বদলে
          ফেললেও নিয়ম ফাঁকি দেওয়ার পথ নেই।
       ⛔ **কাউকে কখনো মোছা হয় না** — শুধু নিষ্ক্রিয়, তাই পুরনো রেকর্ডে
          নাম চিরকাল থাকে আর লগইন নিজে থেকেই বন্ধ হয় (V403)।
       ⛔ টাকার হিসাব ছোঁয়া হয় না — সেসব মোবাইল ধরে চলে (V308)।
       ⛔ TK-নির্দেশ: নতুন লেখা ইংরেজিতে।
       ═══════════════════════════════════════════════════════════════════ */
    /** নতুন স্টাফ/ডাক্তার — ⛔ সব যাচাই সার্ভারে, এখানে শুধু ঘরগুলো। */
    private fun addPersonDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(6), dp(18), dp(6))
        }
        var role = "staff"
        /* 🟢🔒 V821 (২৯.০৮.২০২৬, TK-নির্দেশ ও অনুমোদিত ডেমো-প্রুফ:
           *"staff doctor পাশাপাশি থাকতে হবে, যেটা সিলেক্ট করবো সেটা হাইলাইট হবে"*)

           **আগে:** দুটো বোতাম উপরে-নিচে, দুটোই একই হালকা সবুজ — কোনটা বাছা
           হয়েছে বোঝার একমাত্র উপায় ছিল উপরের "Type:  Staff" লেখাটা।
           **এখন:** পাশাপাশি, আর বাছা বোতামটাই **গাঢ় সবুজ + সাদা লেখা**।
           ⇒ লেখাটার আর দরকার নেই, তাই তুলে দেওয়া হলো।

           ⛔ রংগুলো নতুন নয় — `MedicinePaymentActivity.setupSaleTypeButtons()`-এর
              (V805/V806, TK-অনুমোদিত) হুবহু একই দুটো রং, তাই অ্যাপ জুড়ে
              বাছাই দেখানোর চেহারা একই থাকে।
           ⛔ `role`-এর মান ("staff" / "doctor") ও নিচের সেভের যুক্তি
              **এক অক্ষরও বদলায়নি** — শুধু দেখার ভঙ্গি বদলাল। */
        val pickStaff = ModuleUi.buttonSoft(this, "Staff") { }
        val pickDoctor = ModuleUi.buttonSoft(this, "Doctor") { }
        val roleOn = android.graphics.Color.parseColor("#0B6E33")
        val roleOff = android.graphics.Color.parseColor("#E6F4EC")
        fun paintRole() {
            val isStaff = role == "staff"
            pickStaff.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(if (isStaff) roleOn else roleOff)
                setStroke(dp(1), roleOn)
            }
            pickStaff.setTextColor(if (isStaff) android.graphics.Color.WHITE else roleOn)
            pickDoctor.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(if (!isStaff) roleOn else roleOff)
                setStroke(dp(1), roleOn)
            }
            pickDoctor.setTextColor(if (!isStaff) android.graphics.Color.WHITE else roleOn)
        }
        pickStaff.setOnClickListener { role = "staff"; paintRole() }
        pickDoctor.setOnClickListener { role = "doctor"; paintRole() }
        // পাশাপাশি বসানো — দুজনেই সমান চওড়া (weight 1f), মাঝে একটু ফাঁক।
        val roleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(6))
        }
        roleRow.addView(pickStaff, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        roleRow.addView(pickDoctor, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(10)
        })
        box.addView(roleRow)
        paintRole()

        box.addView(ModuleUi.body(this, "Full Name"))
        val etName = ModuleUi.input(this, "Full name")
        box.addView(etName)
        box.addView(ModuleUi.body(this, "Mobile (10 digits)"))
        val etMobile = ModuleUi.input(this, "10-digit mobile")
        box.addView(etMobile)
        box.addView(ModuleUi.body(this, "Staff Code"))
        val etCode = ModuleUi.input(this, "e.g. KNE-KISHAN9")
        box.addView(etCode)
        box.addView(ModuleUi.body(this, "Branch"))
        /* 🔒 V747 (২৭.০৮.২০২৬, TK-অনুমোদিত) — **ব্রাঞ্চ আর হাতে লেখা যাবে না।**
           আগে বানান একটু এদিক-ওদিক হলে (যেমন "Kisanganj") `BranchCatalog.byName()`
           চুপচাপ **কিশানগঞ্জ** ধরে নিত — লোকটা ভুল ব্রাঞ্চে বসে যেত, কেউ টেরও
           পেত না। এখন তালিকা থেকে বেছে নিতে হয়, তাই ভুল বানানের পথই নেই।
           ⛔ নামগুলো `BranchCatalog` থেকেই আসে — দুই জায়গায় আলাদা হওয়ার ভয় নেই। */
        val branchNames = com.tkbiswas.pilesclinic.print.BranchCatalog.all.map { it.displayName }
        val spBranch = spinner(branchNames)
        box.addView(spBranch)

        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Add Staff or Doctor"))
            .setView(android.widget.ScrollView(this).apply { addView(box) })
            .setPositiveButton("Save", null)   // ⛔ null — নিজেরাই বন্ধ করব
            .setNegativeButton("Close", null)
            .create()
        dlg.show()
        com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg)
        dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val name = etName.text?.toString().orEmpty().trim()
            val mob = etMobile.text?.toString().orEmpty().filter { c -> c.isDigit() }.takeLast(10)
            val code = etCode.text?.toString().orEmpty().trim().uppercase()
            val branch = branchNames.getOrNull(spBranch.selectedItemPosition).orEmpty()
            // ⛔ এটুকু শুধু স্টাফকে বাঁচাতে — আসল পাহারা সার্ভারেই।
            if (name.isBlank() || code.isBlank() || branch.isBlank() || mob.length != 10) {
                android.widget.Toast.makeText(this,
                    "Please fill name, mobile and code (mobile must be 10 digits)",
                    android.widget.Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            dlg.dismiss()
            savePerson(code, mob, name, branch, role)
        }
    }

    private fun savePerson(code: String, mobile: String, name: String, branch: String, role: String) {
        android.widget.Toast.makeText(this, "Saving...", android.widget.Toast.LENGTH_SHORT).show()
        Thread {
            val res = com.tkbiswas.pilesclinic.native.PeopleAdminRepository
                .add(code, mobile, name, branch, role)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val title = if (res.ok) "Added" else "Not done"
                val body = if (res.ok)
                    name + "\n" + code + " · " + branch + " · " +
                        com.tkbiswas.pilesclinic.native.PeopleAdminRepository.roleLabel(role) +
                        "\n\nThis person can log in right now." +
                        "\nMobile: " + ModuleUi.fullMobile(mobile) +
                        "\n" + res.message
                else res.message
                val d = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
                    .setMessage(body)
                    .setPositiveButton("OK") { _, _ -> if (res.ok) renderList() }
                    .create()
                d.show()
                com.tkbiswas.pilesclinic.native.PremiumAlert.paint(d)
            }
        }.start()
    }

    private fun spinner(items: List<String>): Spinner {
        val sp = Spinner(this)
        /* 📏🔒 V1247 — বন্ধ অবস্থার সাজ এখন `simple_spinner_item` (ছোট),
           আর খোলা তালিকা আগের মতোই `simple_spinner_dropdown_item`।
           এটাই Android-এর নিজের স্বাভাবিক ধাঁচ — বাক্সের উচ্চতা কমে, কিন্তু
           বেছে নেওয়ার তালিকা হুবহু আগের মতোই দেখায়।
           ⛔ কোন কোন বিকল্প থাকবে · কোনটা বাছা আছে · কী সেভ হয় — কিচ্ছু
              বদলায়নি (`selectedItemPosition` আগের মতোই কাজ করে)। */
        val ad = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sp.adapter = ad
        return sp
    }
}
