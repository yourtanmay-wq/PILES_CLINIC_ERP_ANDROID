package com.tkbiswas.pilesclinic.native

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Native rebuild of the WebView apptForm()/saveAppt(): staff can log an
 * appointment (stored as an Inquiry-stage enquiry with an appointmentDate and
 * disease "Public Appointment", exactly like the public site does) and see the
 * list of upcoming appointments. Publicly-booked appointments also land here
 * because they are the same enquiry rows.
 */
class AppointmentActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etMobile: EditText
    private lateinit var spBranch: Spinner
    private lateinit var tvDate: TextView
    private lateinit var etRemarks: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var progressLoad: ProgressBar
    private lateinit var tvEmpty: TextView

    private val branches = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
    private var pickedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etName = findViewById(R.id.etName)
        etMobile = findViewById(R.id.etMobile)
        spBranch = findViewById(R.id.spBranch)
        tvDate = findViewById(R.id.tvDate)
        etRemarks = findViewById(R.id.etRemarks)
        listContainer = findViewById(R.id.listContainer)
        progressLoad = findViewById(R.id.progressLoad)
        tvEmpty = findViewById(R.id.tvEmpty)

        MobileInput.attach(etMobile)
        spBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        val user = NativeSession.current(this)
        if (user != null && user.branch != "All") {
            val idx = branches.indexOf(user.branch)
            if (idx >= 0) spBranch.setSelection(idx)
        }

        tvDate.setOnClickListener { pickDate() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save(user?.mobile ?: "") }

        loadUpcoming(user?.branch ?: "All")
    }

    private fun pickDate() {
        val c = Calendar.getInstance()
        val dlg = DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
            pickedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            tvDate.text = DateUtil.display(pickedDate)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        dlg.datePicker.minDate = System.currentTimeMillis() - 1000
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
    }

    private fun save(staffMobile: String) {
        val name = etName.text?.toString()?.trim().orEmpty()
        val digits = MobileInput.digits(etMobile)
        val branch = spBranch.selectedItem?.toString() ?: branches.first()
        val remarks = etRemarks.text?.toString()?.trim().orEmpty()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        // TK-DECISION (2026-07-22): app-wide standard -- mark the exact wrong
        // field red + move the cursor there, with that field's own message.
        val msg = FieldError.validate(listOf<Triple<View, Boolean, String>>(
            Triple(etName, name.isNotBlank(), "Name দিন"),
            Triple(etMobile, digits.length == 10, "সঠিক 10 ডিজিট মোবাইল দিন"),
            Triple(spBranch, branch.isNotBlank(), "Branch বাছুন"),
            Triple(tvDate, pickedDate.isNotBlank(), "তারিখ বাছুন"),
            Triple(etRemarks, remarks.isNotBlank(), "Remarks দিন")
        ))
        if (msg != null) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return
        }
        if (pickedDate < today) {
            Toast.makeText(this, "Past appointment not allowed", Toast.LENGTH_SHORT).show()
            return
        }
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        lifecycleScope.launch {
            // 🔒🔒 TK-ORDER (30.07.2026 রাত — "একই নম্বর বারবার বুক করলে প্রতিবার
            // নতুন রেকর্ড তৈরি হয়" ঝুঁকি, TK অনুমতি দিয়েছেন সাবধানে ঠিক করতে):
            //
            // আগে এই ফর্ম প্রতিবার একটা **নতুন এনকোয়ারি সারি** বানাত, একই
            // মোবাইল দিয়ে বারবার আসলেও — TK-এর স্থায়ী নিয়ম ("এক মোবাইল =
            // এক রেকর্ড") ভাঙছিল। এখন Registration/Enquiry-র মতোই আগে
            // যাচাই করা হয় — একই ফাংশন (`EnquiryRepository.checkDuplicate`)
            // যেটা আগে থেকেই ব্যবহার হয়, কোনো নতুন যাচাই-পদ্ধতি বানানো হয়নি।
            // ⛔ নতুন কোনো ডিজাইন/পপ-আপ নয় — নম্বর আগে থেকে থাকলে চুপচাপ
            //    সেই পুরনো রেকর্ডেই আসার তারিখ/রিমার্ক বসে, শুধু Toast দিয়ে
            //    স্টাফকে জানানো হয়। নম্বর সত্যিই নতুন হলে আগের মতোই কাজ করে,
            //    এক অক্ষরও বদলায়নি।
            val duplicate = withContext(Dispatchers.IO) { EnquiryRepository(this@AppointmentActivity).checkDuplicate(digits) }
            val ok = withContext(Dispatchers.IO) {
                if (duplicate.found) {
                    val fuId = FollowUpRepository(this@AppointmentActivity).ensureFollowUpRowIdFor(
                        mobile = "+91$digits",
                        stage = duplicate.stage.ifBlank { "Inquiry" },
                        name = duplicate.name.ifBlank { name },
                        branch = duplicate.branch.ifBlank { branch },
                        recordDate = today
                    )
                    if (fuId.isBlank()) return@withContext false
                    val a = FollowUpRepository(this@AppointmentActivity).updateNextFollow(fuId, pickedDate)
                    val b = FollowUpRepository(this@AppointmentActivity).updateRemark(fuId, remarks, staffMobile.ifBlank { "Public Website" })
                    a && b
                } else {
                    val row = JSONObject()
                        .put("id", "enq_" + UUID.randomUUID().toString().replace("-", ""))
                        .put("name", name)
                        .put("mobile", "+91$digits")
                        .put("branch", branch)
                        .put("date", today)
                        .put("appointmentDate", pickedDate)
                        .put("disease", "Public Appointment")
                        .put("remarks", remarks)
                        .put("status", "Active")
                        .put("stage", "Inquiry")
                        .put("callCount", 0)
                        .put("receivedBy", staffMobile.ifBlank { "Public Website" })
                        .put("createdBy", staffMobile)
                        .put("createdAt", now)
                        .put("updatedAt", now)
                    val a = SupabaseClient.upsert("enquiries", row)
                    // Also create the Inquiry follow-up row so it shows in the pipeline.
                    // 🔴 B376 (TK-নির্দেশ, ধাপে-ধাপে যাচাই করে) — এই "followups" সারিও
                    // ঠিক EnquiryModel.buildFollowUpRow()-এর (B215, 31.07.2026) একই
                    // নিয়ম মেনে চলবে: Inquiry stage-এ প্রথম থেকেই callCount=1, নইলে
                    // এই স্ক্রিন থেকে বানানো অ্যাপয়েন্টমেন্টে 📶 সিগন্যাল সবসময় Nill
                    // দেখাত (ওয়েবের একই ফিচারে (app.js, ensureFollow()) আগে থেকেই ১
                    // বসে — এখানে শুধু বাদ পড়ে গিয়েছিল)।
                    val fu = JSONObject()
                        /* 🔗 V1005 — এনকোয়ারির সারির মতোই নির্দিষ্ট id, তাই
                           কম্পিউটারের self-heal আর দ্বিতীয় সারি বানায় না। */
                        .put("id", "fu_inq_" + row.getString("id"))
                        .put("refId", row.getString("id"))
                        .put("name", name).put("mobile", "+91$digits").put("branch", branch)
                        .put("stage", "Inquiry").put("status", "Active")
                        .put("nextFollow", pickedDate).put("callCount", 1)
                        .put("lastRemark", remarks)
                        .put("createdBy", staffMobile).put("createdAt", now).put("updatedAt", now)
                    val b = SupabaseClient.upsert("followups", fu)
                    a && b
                }
            }
            progressLoad.visibility = View.GONE
            val doneMsg = when {
                !ok -> "Save failed — check connection"
                duplicate.found -> "Already registered — appointment date updated on the existing record"
                else -> "Appointment saved"
            }
            Toast.makeText(this@AppointmentActivity, doneMsg, Toast.LENGTH_SHORT).show()
            if (ok) {
                etName.setText(""); etMobile.setText(""); etRemarks.setText(""); pickedDate = ""; tvDate.text = ""
                loadUpcoming(NativeSession.current(this@AppointmentActivity)?.branch ?: "All")
            }
        }
    }

    // 🔴🔒 B501 (06.08.2026, TK-নির্দেশ — "সাথে সাথে দেখাতে হবে, সব
    // ব্রাঞ্চে সমানভাবে") — Appointment পাতা আগে সরাসরি ক্লাউডের উত্তরের
    // অপেক্ষা করত, ফোনের জমানো তথ্য দেখানোর কোনো ব্যবস্থাই ছিল না। এখন
    // FollowUpRepository.loadCachedTab()-এর একই কৌশলে — শেষবার সফলভাবে
    // আনা তালিকা এই স্ক্রিনের নিজের SharedPreferences-এ জমা থাকে, পাতা
    // খোলার সাথে সাথেই সেটা দেখানো হয়, তারপর নিঃশব্দে ক্লাউড থেকে
    // হালনাগাদ তালিকা এলে বদলে যায়। ⛔ ছাঁকনি/সাজানো/ব্রাঞ্চের নিয়ম কিছুই
    // বদলায়নি — শুধু কখন প্রথম দেখানো হয় সেটাই এগিয়ে আনা হলো।
    private fun apptCachePrefs() = getSharedPreferences("appointment_cache", MODE_PRIVATE)
    private fun apptCacheKey(userBranch: String) = "appts_$userBranch"

    private fun loadCachedAppointments(userBranch: String): List<JSONObject>? {
        return try {
            val json = apptCachePrefs().getString(apptCacheKey(userBranch), null) ?: return null
            val arr = JSONArray(json)
            val list = ArrayList<JSONObject>()
            for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
            list.ifEmpty { null }
        } catch (_: Throwable) { null }
    }

    private fun saveCachedAppointments(userBranch: String, appts: List<JSONObject>) {
        try {
            val arr = JSONArray()
            for (r in appts) arr.put(r)
            apptCachePrefs().edit().putString(apptCacheKey(userBranch), arr.toString()).apply()
        } catch (_: Throwable) { }
    }

    private fun renderAppointments(appts: List<JSONObject>) {
        /* 🔴🔒 V509 (TK-রিপোর্ট): ক্যাশ ও ক্লাউডের তালিকা হুবহু এক হলে আর
           মুছে-আঁকা হয় না — ঝিলিক বন্ধ। ⛔ আলাদা হলেই আগের মতোই পুরো আঁকে। */
        if (com.tkbiswas.pilesclinic.native.RedrawGuard.alreadyShowing(
                listContainer, appts.joinToString("|") { it.toString() })) return
        listContainer.removeAllViews()
        for (r in appts) listContainer.addView(card(r))
        tvEmpty.visibility = if (appts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadUpcoming(userBranch: String) {
        progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        tvEmpty.visibility = View.GONE
        listContainer.removeAllViews()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        // 🔴🔒 B501 — ফোনের জমানো তালিকা থাকলে সাথে সাথেই দেখানো হয়।
        val cachedAppts = loadCachedAppointments(userBranch)
        if (!cachedAppts.isNullOrEmpty()) renderAppointments(cachedAppts)
        lifecycleScope.launch {
            // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): this used
            // to download up to 3,000 enquiries -- almost all of which have no
            // appointment at all -- and then drop them in the loop below.
            // ⛔ THE LIST CANNOT CHANGE: the loop kept a row only when its
            // appointmentDate was neither blank nor earlier than today, and
            // "gte.today" is exactly that same test done by the cloud (the
            // dates are plain yyyy-MM-dd text, and a blank or missing date is
            // left out by gte just as the loop left it out). Both checks below
            // are still there, word for word, and the sorting is unchanged.
            // ⛔ The branch rule is deliberately NOT sent to the cloud: this
            // screen keeps a row whose branch is blank, and only the loop below
            // knows that. So branch filtering stays exactly where it was.
            // 🔒 কোটা-সংশোধন (29.07.2026, খাতার সারি B103): এই পর্দাটা এতদিন
            // এনকোয়ারির **প্রতিটা ঘর** নামাত (`select=*`) — ছবি-সহ ভারী ঘরগুলোও,
            // অথচ নিচে মাত্র ছ'টা ঘর পড়া হয়। এখন শুধু ওই ছ'টা (+ `updatedAt`,
            // যেটা সাজানোর কাজে লাগে) নামে।
            // ⛔ তালিকা এক চুলও বদলায় না: সারি · ছাঁকনি · সাজানো · ব্রাঞ্চের নিয়ম —
            //    সব হুবহু আগের মতোই। `fetchListSlim` নিজেই পাহারা দেয় — সরু
            //    অনুরোধ কোনো কারণে না চললে সে আপনা থেকেই আগের মতো পুরো সারি চায়।
            // 🔵 TK-ORDER (07.08.2026): fetchListSlimOrNull — পড়া ব্যর্থ হলে null।
            // আগে fetchListSlim ব্যর্থে খালি ফেরাত → saveCachedAppointments(খালি)
            // ভালো cache মুছে দিত ও খালি তালিকা দেখাত (সব appointment হারিয়ে
            // যেত)। এখন ব্যর্থ (null) হলে cache ছোঁব না, খালি দেখাব না — শেষ-জানা
            // তালিকাই থাকে। ⛔ একই একটাই cloud-read; সারি/ছাঁকনি/সাজানো আগের মতোই।
            val rows = withContext(Dispatchers.IO) {
                SupabaseClient.fetchListSlimOrNull(
                    "enquiries", "appointmentDate=gte.$today", 3000,
                    "id,name,mobile,branch,remarks,appointmentDate,updatedAt"
                )
            }
            progressLoad.visibility = View.GONE
            if (rows == null) {
                if (cachedAppts.isNullOrEmpty()) {
                    tvEmpty.text = "Could not load — check connection and try again"
                    tvEmpty.visibility = View.VISIBLE
                }
                return@launch   // ব্যর্থ পড়া — ভালো cache/তালিকা অক্ষত
            }
            val appts = mutableListOf<JSONObject>()
            for (i in 0 until rows.length()) {
                val r = rows.getJSONObject(i)
                val ad = r.s("appointmentDate")
                if (ad.isBlank() || ad < today) continue
                val br = r.s("branch")
                if (userBranch != "All" && br.isNotBlank() && br != userBranch) continue
                appts.add(r)
            }
            appts.sortBy { it.s("appointmentDate") }
            saveCachedAppointments(userBranch, appts)
            renderAppointments(appts)
        }
    }

    private fun card(r: JSONObject): View {
        val tv = TextView(this).apply {
            text = "⏰ ${DateUtil.display(r.s("appointmentDate"))} · ${r.s("name")}\n" +
                "${r.s("mobile")} · ${r.s("branch")}\n${r.s("remarks")}"
            textSize = 13f
            setPadding(24, 20, 24, 20)
            setTextColor(0xFF10223A.toInt())
            setBackgroundColor(0xFFF4F8FC.toInt())
        }
        tv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 12) }
        TripleTapEdit.attach(tv) { showApptEdit(r) }
        return tv
    }

    /** Global 3-tap edit for an appointment (stored as an enquiry row). */
    private fun showApptEdit(r: JSONObject) {
        val id = r.s("id")
        if (id.isBlank()) return
        val pad = (16 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad * 3, pad, pad * 3, 0) }
        fun lbl(t: String) = TextView(this).apply { text = t; setPadding(0, pad, 0, 0) }
        val name = EditText(this).apply { setText(r.s("name")); hint = "Name" }
        val mobile = EditText(this).apply { setText(r.s("mobile")); hint = "Mobile"; inputType = android.text.InputType.TYPE_CLASS_PHONE }
        val remarks = EditText(this).apply { setText(r.s("remarks")); hint = "Remarks" }
        box.addView(lbl("Name")); box.addView(name)
        box.addView(lbl("Mobile")); box.addView(mobile)
        box.addView(lbl("Remarks")); box.addView(remarks)
        UppercaseInputUtil.applyToAll(box)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Edit Appointment"))
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val fields = JSONObject()
                    .put("name", name.text.toString().trim())
                    .put("mobile", mobile.text.toString().filter { it.isDigit() }.takeLast(10))
                    .put("remarks", remarks.text.toString().trim())
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { SupabaseClient.updateById("enquiries", id, fields) }
                    Toast.makeText(this@AppointmentActivity, if (ok) "Updated" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                    if (ok) loadUpcoming(NativeSession.current(this@AppointmentActivity)?.branch ?: "All")
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }
}
