package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityExportDataBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TK-REQUESTED ADDITION (2026-07-16): "Export to Excel" (CSV) feature,
 * Master-only, matching the same access-restriction pattern as Trash Bin /
 * Password Center. Isolated screen -- reads existing tables via the
 * existing SupabaseClient.fetchList (no new query logic on the server
 * side, no change to any other screen/repository).
 */
class ExportDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExportDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        if (session.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_LONG).show()
            finish(); return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnExport.setOnClickListener { confirmThenExport() }
    }

    /**
     * 🔵 V405 (16.08.2026, TK-অনুমোদিত — Egress) — চাপার আগে একবার জিজ্ঞাসা।
     *
     * এই পর্দাটা প্রতিটি টেবিলের **সব ঘর সহ ১০০০০০ পর্যন্ত সারি** নামায়
     * (`select=*` — রোগীর ছবি base64-সহ), কোনো বিরতি বা সীমা ছাড়া। একবার
     * চাপলেই প্রায় **পুরো ডেটাবেস** নেমে আসে। কৌতূহলে বা ভুল করে কয়েকবার
     * চাপলে Supabase-এর মাসিক কোটার বড় অংশ একদিনেই শেষ হয়ে যেতে পারে —
     * TK-এর ১৫ GB Egress-এর সময় এটাও একটা সন্দেহভাজন ছিল।
     *
     * ⛔ Export-এর কাজ **এক অক্ষরও বদলানো হয়নি** — সব সারি, সব ঘর আগের মতোই
     *    নামে (CSV অসম্পূর্ণ হলে TK-এর কাজে লাগত না)। শুধু ভুল করে বা বারবার
     *    চাপা আটকাতে একটা প্রশ্ন যোগ হলো।
     * ⛔ পর্দাটা আগে থেকেই master-only (MoreMenu ও Dashboard দুটোতেই)।
     */
    private fun confirmThenExport() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Export Data"))
            .setMessage(
                "এটা চাপলে বাছাই করা টেবিলের **সব তথ্য** ইন্টারনেট থেকে নামবে " +
                "(রোগীর ছবিসহ) — প্রায় পুরো ডেটাবেস।\n\n" +
                "এতে মাসিক ডেটার হিসাব থেকে অনেকটা খরচ হয়। সত্যিই দরকার হলে " +
                "তবেই চালান, বারবার নয়।\n\nএখন নামাব?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes, download") { _, _ -> runExport() }
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    private fun runExport() {
        val tables = mutableListOf<String>()
        if (binding.cbPatients.isChecked) tables.add("patients")
        if (binding.cbEnquiries.isChecked) tables.add("enquiries")
        if (binding.cbPayments.isChecked) tables.add("payments")
        if (binding.cbFollowups.isChecked) tables.add("followups")

        if (tables.isEmpty()) {
            Toast.makeText(this, "Select at least one table first.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressExport.visibility = View.VISIBLE
        binding.tvStatus.text = "Fetching data…"
        binding.btnExport.isEnabled = false

        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                val out = mutableListOf<File>()
                for (table in tables) {
                    try {
                        // AUDIT FIX (2026-08-06, TK approved): raised from 5000
                        // to 100000 so a large table's OLDEST rows are no longer
                        // silently dropped from the CSV export. Matches the same
                        // limit the daily JSON cloud backup already uses
                        // (CloudBackup.kt), so Export and Backup now cover the
                        // full history. No other behaviour changed.
                        val rows = SupabaseClient.fetchList(table, limit = 100000)
                        out.add(CsvExportHelper.writeCsv(this@ExportDataActivity, table, rows))
                    } catch (_: Exception) { /* skip this table, continue with the rest */ }
                }
                out
            }

            binding.progressExport.visibility = View.GONE
            binding.btnExport.isEnabled = true

            if (files.isEmpty()) {
                binding.tvStatus.text = "Export failed — check your internet connection and try again."
                return@launch
            }

            binding.tvStatus.text = "${files.size} file(s) ready — choose where to save/send them."
            shareFiles(files)
        }
    }

    private fun shareFiles(files: List<File>) {
        val uris = files.map { f ->
            FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Save/Share CSV export"))
    }
}
