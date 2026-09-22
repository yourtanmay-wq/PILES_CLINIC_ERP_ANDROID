package com.tkbiswas.pilesclinic.clinical

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.NoBengali
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

class PatientClinicalHistoryActivity : AppCompatActivity() {

    /* 🔴🔒 V786 (২৮.০৮.২০২৬, TK-রিপোর্ট: হেডারে "Patient / - / -") —
       ফোনে কল এলে বা মেমরি কম পড়লে Android অ্যাপের প্রসেস বন্ধ করে দেয়;
       পরে এই পর্দাটা আবার খোলে, কিন্তু মেমরির `RoleSession` ততক্ষণে ফাঁকা।
       তাই রোগীর পরিচয় এই পর্দার নিজের Bundle-এও রাখা হয় — Bundle প্রসেস
       মরলেও বাঁচে, আর V721-এর ৩০ মিনিটের সীমাও এতে লাগে না।
       ⛔ মেমরিতে রোগী থাকলে `restoreFrom()` কিচ্ছু করে না (RoleSession.kt)। */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        RoleSession.saveTo(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoleSession.restoreFrom(savedInstanceState)   // 🔴🔒 V786 — কল/মেমরির কারণে হারানো রোগী ফেরানো
        setContentView(R.layout.activity_patient_history)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyState)
        rv.layoutManager = LinearLayoutManager(this)

        // Load the saved records from Supabase for THIS patient so history is
        // there after every app restart (not just the current session). Falls
        // back to the in-memory session list if the cloud returns nothing.
        // TK-REQUESTED (2026-07-24): real cache-first, keyed per patientId
        // (SharedPreferences, same pattern as Trash/Briefing) -- raw rows
        // saved after every successful fetch, shown instantly next time.
        val patientId = RoleSession.currentPatientId
        val cachePrefs = getSharedPreferences("piles_clinic_clinical_history_cache", MODE_PRIVATE)
        val cacheKey = "rows_$patientId"
        var hasCache = false
        try {
            val json = cachePrefs.getString(cacheKey, null)
            if (!json.isNullOrBlank()) {
                val cachedList = ClinicalCloudRepository.buildFromRows(org.json.JSONArray(json), patientId)
                if (cachedList.isNotEmpty()) {
                    hasCache = true
                    rv.adapter = HistoryAdapter(cachedList)
                    rv.visibility = android.view.View.VISIBLE
                    tvEmpty.visibility = android.view.View.GONE
                }
            }
        } catch (_: Throwable) { }
        if (!hasCache) {
            tvEmpty.text = "Loading..."
            tvEmpty.visibility = android.view.View.VISIBLE
            rv.visibility = android.view.View.GONE
        }
        lifecycleScope.launch {
            // 🔵 TK-ORDER (07.08.2026): loadMedicalRawOrNull() — পড়া ব্যর্থ হলে null।
            // আগে loadMedicalRaw() ব্যর্থে খালি ফেরাত → ওই খালিটা cache-এ বসে যেত →
            // পরের বার খুললেও "No visit history"। এখন ব্যর্থ (null) হলে cache ছোঁব না,
            // "No history"ও দেখাব না — শেষ-জানা তথ্যই থাকে।
            // ⛔ একই একটাই cloud-read; পুরনো loadMedicalRaw অন্য জায়গায় অক্ষত।
            val rawRows = try {
                withContext(Dispatchers.IO) { ClinicalCloudRepository.loadMedicalRawOrNull(patientId) }
            } catch (_: Throwable) { null }
            if (rawRows == null) {
                if (!hasCache) {
                    tvEmpty.text = NoBengali.s("লোড করা গেল না — একটু পরে আবার দেখুন")
                    tvEmpty.visibility = android.view.View.VISIBLE
                    rv.visibility = android.view.View.GONE
                }
                return@launch   // ব্যর্থ পড়া — ভালো cache/তালিকা অক্ষত
            }
            try { cachePrefs.edit().putString(cacheKey, rawRows.toString()).apply() } catch (_: Throwable) { }
            val cloud = ClinicalCloudRepository.buildFromRows(rawRows, patientId, this@PatientClinicalHistoryActivity)
            val list = if (cloud.isNotEmpty()) cloud else ClinicalRepository.visitHistory
            if (list.isEmpty()) {
                tvEmpty.text = "No visit history yet"
                tvEmpty.visibility = android.view.View.VISIBLE
                rv.visibility = android.view.View.GONE
            } else {
                tvEmpty.visibility = android.view.View.GONE
                rv.visibility = android.view.View.VISIBLE
                rv.adapter = HistoryAdapter(list)
            }
        }
    }
}
