package com.tkbiswas.pilesclinic.native

import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.util.Locale

/**
 * 🟢🔒 B671 (15.08.2026, TK-অনুমোদিত ফটো-প্রুফ) — **সেভ করা Doctor/RMP বেছে নেওয়ার
 * তালিকা**, নাম · মোবাইল · বিকল্প নম্বর · জায়গা · ব্রাঞ্চ — পাঁচটা ধরেই খোঁজা যায়।
 *
 * TK-এর নির্দেশ (হুবহু): *"রেজিস্ট্রেশন ফর্মে কি রেখেছেন সেরকম ব্যবস্থা রাখতে হবে,
 * যাতে ডাক্তারের নাম হোক বা নাম্বার হোক বা জায়গার নাম দিয়ে সার্চ করলে চলে আসবে।"*
 *
 * ⛔ এই ফাইলটা `RegistrationActivity`-র ইতিমধ্যে **TK-অনুমোদিত ও চলমান** picker-এর
 *    হুবহু নকল (লাইন 118-312 থেকে তোলা) — শুধু ক্লাস-নির্ভর অংশগুলো (`this`,
 *    `binding`, `resources`) সাধারণ `act`/callback-এ বদলানো হয়েছে।
 *    ⛔ **`RegistrationActivity`-তে এক অক্ষরও হাত দেওয়া হয়নি** — চালু পর্দাটা যেন
 *       কোনোভাবেই না ভাঙে (TK-এর নিয়ম: "কোনো ভাল কাজ যেন খারাপ না হয়")।
 *
 * ⚡ **Free plan নিশ্চয়তা:** এই ফাইল **কখনো Supabase-এ যায় না** — তালিকাটা ফোনে
 *    আগে থেকে জমানো ঘর (`doctor_visit_cache`) থেকে পড়ে, ঠিক Registration-এর মতোই।
 *    ⇒ নতুন কোনো egress খরচ নেই, এক বাইটও নয়।
 */
object RmpPicker {

    private val BRANCHES = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")

    data class RmpChoice(
        val id: String,
        val name: String,
        val mobile: String,
        val altMobiles: String,
        val area: String,
        val branch: String
    ) {
        fun searchText(): String = "$name $mobile $altMobiles $area $branch".lowercase(Locale.US)
        /** 🟢 B672: টাইপ করতে করতে যে তালিকা নামে, তার এক-লাইনের লেখা।
         *  নাম · মোবাইল · জায়গা · ব্রাঞ্চ — চারটেই এক লাইনে, তাই যেটা দিয়েই টাইপ
         *  করুন (নাম / নম্বর / জায়গা) Android-এর নিজের শব্দ-ধরে-মেলানো ছাঁকনি ধরতে পারে।
         *  ⛔ নিচের পুরনো `label()` এক অক্ষরও বদলানো হয়নি। */
        fun flatLabel(): String =
            listOf(name, mobile, area, branch).filter { it.isNotBlank() }.joinToString("  ·  ")

        fun label(): String {
            val line2 = listOf(mobile, area, branch).filter { it.isNotBlank() }.joinToString("  ·  ")
            return if (line2.isBlank()) name else "$name\n$line2"
        }
    }

    /**
     * 🟢🔒 V1132 (০৬.০৯.২০২৬, TK-অনুমোদিত) — TK: *"জলপাইগুড়ির স্টাফ All Branch
     * Enquiry Form-এ কোচবিহারের RMP বাছতে চাইছে, নাম-নম্বর সম্পূর্ণ দিয়েও
     * সাজেস্ট করছে না।"*
     *
     * 🔴 **কারণ (কোডে মেপে পাওয়া):** তালিকাটা সবসময় **লগ-ইন করা স্টাফের নিজের
     *    ব্রাঞ্চ** ধরে ছাঁকা হত — ফর্মে যে ব্রাঞ্চ বাছা হয়েছে সেটা ধরে নয়।
     *
     * ⇒ এখন ডাকার সময় চাইলে **অন্য একটা ব্রাঞ্চ** পাঠানো যায় (`branchScope`)।
     * ⛔ কেউ না পাঠালে আচরণ **এক অক্ষরও বদলায় না** — আগের মতোই নিজের ব্রাঞ্চ।
     * ⛔ Registration-এর নিজের তালিকা এই ফাইলটা ব্যবহারই করে না — সেখানে হাত পড়েনি।
     */
    fun cachedRmpChoices(
        act: android.app.Activity,
        user: NativeUser,
        branchScope: String = ""
    ): List<RmpChoice> {
        val scope = branchScope.trim().ifBlank { user.branch }
        return try {
            val prefs = act.getSharedPreferences("doctor_visit_cache", android.content.Context.MODE_PRIVATE)
            // Staff/Doctor only ever need their own branch cache. Master may
            // have opened either All or an individual branch, so all existing
            // cache buckets are safely combined without any network request.
            val keys = if (scope.equals("All", ignoreCase = true)) {
                listOf("All") + BRANCHES
            } else listOf(scope)
            val combined = org.json.JSONArray()
            for (branch in keys.distinct()) {
                val raw = prefs.getString("cache_${branch.ifBlank { "All" }}", null) ?: continue
                val arr = try { org.json.JSONArray(raw) } catch (_: Throwable) { continue }
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { combined.put(it) }
            }
            // 🟢🔒 V802 — Doctor Visit পর্দার জমানো ঘরের সঙ্গে RMP-বাছার নিজস্ব
            // হালকা ঘরটাও যোগ (নিচের `seen` ইতিমধ্যেই ডুপ্লিকেট বাদ দেয়)।
            run {
                val extra = RmpDirectory.cachedRows(act, scope)
                for (i in 0 until extra.length()) extra.optJSONObject(i)?.let { combined.put(it) }
            }
            // Include a doctor/RMP just added on this phone even if its cloud
            // copy has not yet appeared in the saved cache.
            val rows = MyPhoneWrites.overlay(act, "doctor_visits", combined)
            val seen = HashSet<String>()
            val out = ArrayList<RmpChoice>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.optString("id").trim()
                val name = row.optString("name").trim()
                val mobile = row.optString("mobile").filter { it.isDigit() }.takeLast(10)
                val branch = row.optString("branch").trim()
                val status = row.optString("status").trim()
                if (name.isBlank()) continue
                if (status.isNotBlank() && !status.equals("Active", ignoreCase = true)) continue
                if (!scope.equals("All", ignoreCase = true) &&
                    branch.isNotBlank() && !branch.equals(scope, ignoreCase = true)) continue
                if (id.isNotBlank() && try { DeletedGuard.isDeleted("doctor_visits", id, act) } catch (_: Throwable) { false }) continue
                val unique = id.ifBlank { "$mobile|${name.lowercase(Locale.US)}" }
                if (!seen.add(unique)) continue
                out.add(RmpChoice(
                    id = id,
                    name = name,
                    mobile = mobile,
                    altMobiles = row.optString("altMobiles"),
                    area = row.optString("area").trim(),
                    branch = branch
                ))
            }
            out.sortedBy { it.name.lowercase(Locale.US) }
        } catch (_: Throwable) { emptyList() }
    }

    fun show(act: android.app.Activity, user: NativeUser, onPick: (RmpChoice) -> Unit) {
        val all = cachedRmpChoices(act, user)
        /* 🟢🔒 V802 (২৮.০৮.২০২৬) — TK: "RMP পাঠিয়েছে, তাহলে এখন কেন দেখাচ্ছে না
           আরএমপি লিস্ট?" ─ কারণ ছিল: তালিকাটা শুধু `doctor_visit_cache` থেকে পড়ত,
           আর ওই ঘরটা ভরে **একমাত্র Doctor Visit পর্দা খুললে**। যে ফোনে কেউ ওই
           পর্দা কখনো খোলেনি, সেখানে তালিকা চিরকাল ফাঁকাই থাকত (পুরনো লেখাতেই
           ছিল — "No cloud search was made")।
           এখন ফাঁকা হলে **একবার** হালকা পড়া হয় (৮টা ঘর, ভারী `callHistory` ও
           `referralPayments` ছাড়া ⇒ কয়েক KB), তারপর ফোনে জমা থাকে — পরের বার
           এক বাইটও খরচ নেই। নেট না থাকলে আগের মতোই হাতে লেখা যায়। */
        if (all.isEmpty() && !RmpDirectory.hasDoctorVisitCache(act, user.branch) &&
            RmpDirectory.cachedRows(act, user.branch).length() == 0) {   // ⛔ একবারই — নামানো হয়ে গেলে আর নয়
            android.widget.Toast.makeText(act, "Loading saved RMP list…", android.widget.Toast.LENGTH_SHORT).show()
            Thread {
                val got = RmpDirectory.refreshFromCloud(act.applicationContext, user.branch)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (act.isFinishing || act.isDestroyed) return@post
                    if (got) show(act, user, onPick) else AlertDialog.Builder(act)
                        .setCustomTitle(PremiumAlert.header(act, "Saved RMP list not available"))
                        .setMessage("Could not load the saved RMP list. Check the internet connection and try again, or enter the Doctor / RMP name and mobile manually below.")
                        .setPositiveButton("OK", null)
                        .show().also { PremiumAlert.paint(it) }
                }
            }.start()
            return
        }
        if (all.isEmpty()) {
            AlertDialog.Builder(act)
                .setCustomTitle(PremiumAlert.header(act, "Saved RMP list not available"))
                .setMessage("No saved RMP list is available on this phone yet. You can enter the Doctor / RMP name and mobile manually below. No cloud search was made.")
                .setPositiveButton("OK", null)
                .show().also { PremiumAlert.paint(it) }
            return
        }

        val pad = (16 * act.resources.displayMetrics.density).toInt()
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val search = EditText(act).apply {
            hint = "Search by name, mobile or area"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(pad, pad / 2, pad, pad / 2)
        }
        val status = TextView(act).apply {
            textSize = 13f
            setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#147A45"))
            setPadding(6, pad / 2, 4, pad / 2)
        }
        val list = ListView(act).apply {
            divider = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            dividerHeight = (8 * act.resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, pad / 2)
            clipToPadding = false
            setBackgroundColor(android.graphics.Color.WHITE)
            isFastScrollEnabled = all.size > 30
        }
        box.addView(search, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        box.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        box.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (360 * act.resources.displayMetrics.density).toInt()))

        var shown = all
        fun render(query: String = "") {
            val q = query.trim().lowercase(Locale.US)
            shown = if (q.isBlank()) all else all.filter { it.searchText().contains(q) }
            status.text = if (shown.isEmpty()) "No matching saved RMP — use manual entry" else "${shown.size} saved RMP found"
            list.adapter = object : android.widget.BaseAdapter() {
                override fun getCount(): Int = shown.size
                override fun getItem(position: Int): RmpChoice = shown[position]
                override fun getItemId(position: Int): Long = position.toLong()

                private fun branchTag(text: String): TextView = TextView(act).apply {
                    this.text = text
                    textSize = 11f
                    setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#15549B"))
                    setPadding(pad / 2, pad / 4, pad / 2, pad / 4)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 18 * act.resources.displayMetrics.density
                        setColor(android.graphics.Color.parseColor("#E8F2FF"))
                    }
                }

                override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
                    val item = getItem(position)
                    return LinearLayout(act).apply {
                        orientation = LinearLayout.HORIZONTAL
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 14 * act.resources.displayMetrics.density
                            setColor(android.graphics.Color.WHITE)
                            setStroke((1 * act.resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#DBE8E2"))
                        }
                        elevation = 2f * act.resources.displayMetrics.density
                        addView(android.view.View(act).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#118452"))
                        }, LinearLayout.LayoutParams((4 * act.resources.displayMetrics.density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT))
                        addView(LinearLayout(act).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(pad * 3 / 4, pad * 3 / 4, pad * 3 / 4, pad * 3 / 4)
                            addView(LinearLayout(act).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                addView(TextView(act).apply {
                                    // 👁️ V752 (২৭.০৮.২০২৬, TK-রিপোর্ট ছবিসহ:
                                    //    *"একই যায়গায় ২ রকম — সবাই আমরা বিভ্রান্ত হয়ে যাচ্ছি"*)
                                    //    স্টাফেরা কেউ ছোট হাতে, কেউ বড় হাতে নাম লিখে সেভ
                                    //    করেছেন ("amit goldar" বনাম "AMIT LAL BARMAN"), তাই এক
                                    //    তালিকাতেই দু'রকম দেখাত। ⛔ ডেটাবেসের লেখা বদলানো হয়নি —
                                    //    শুধু **দেখানোর সময়** এক রকম করা হলো।
                                    text = item.name.trim().uppercase()
                                    textSize = 17f
                                    setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                    setTextColor(android.graphics.Color.parseColor("#17312A"))
                                    maxLines = 2
                                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = pad / 2 })
                                if (item.branch.isNotBlank()) addView(branchTag(item.branch))
                            })
                            addView(TextView(act).apply {
                                text = if (item.mobile.isBlank()) "Mobile not saved" else item.mobile
                                textSize = 14f
                                setTextColor(android.graphics.Color.parseColor("#29483C"))
                                setPadding(0, pad / 3, 0, 0)
                            })
                            if (item.area.isNotBlank()) addView(TextView(act).apply {
                                // 👁️ V752 — এলাকার নামও একই কারণে এক রকম।
                                text = item.area.trim().uppercase()
                                textSize = 13f
                                setTextColor(android.graphics.Color.parseColor("#60766D"))
                                setPadding(0, pad / 5, 0, 0)
                                maxLines = 2
                            })
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    }
                }
            }
        }
        render()

        val dialog = AlertDialog.Builder(act)
            .setCustomTitle(PremiumAlert.header(act, "Select Saved RMP / Doctor"))
            .setView(box)
            .setNegativeButton("Manual Entry", null)
            .create()
        list.setOnItemClickListener { _, _, position, _ ->
            val selected = shown.getOrNull(position) ?: return@setOnItemClickListener
            onPick(selected)
            dialog.dismiss()
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        dialog.setOnShowListener { PremiumAlert.paint(dialog) }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }
}
