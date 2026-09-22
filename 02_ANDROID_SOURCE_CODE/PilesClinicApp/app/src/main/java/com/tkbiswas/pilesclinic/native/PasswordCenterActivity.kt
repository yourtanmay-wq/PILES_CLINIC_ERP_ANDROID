package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityPasswordCenterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native rebuild -- Password Center (User management). Master-only.
 *
 * Lists every configured user and lets the Master change any user's login
 * password, saved to the live Supabase "usercredentials" table (see
 * PasswordCenterRepository). Mirrors passwordCenter()/saveUserPassword().
 */
class PasswordCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordCenterBinding
    private lateinit var repository: PasswordCenterRepository
    private lateinit var adapter: PasswordCenterAdapter
    private lateinit var user: NativeUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = PasswordCenterRepository()

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        if (user.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = PasswordCenterAdapter(this, emptyList(), onChange = { showChangeDialog(it) })
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        loadList()
    }

    private fun loadList() {
        binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        binding.tvEmpty.text = "Loading..."
        binding.tvEmpty.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                try { com.tkbiswas.pilesclinic.modules.ModuleAuth.signInCurrentSession(this@PasswordCenterActivity) } catch (_: Throwable) { }
                repository.loadCredentials()
            }
            binding.progressLoad.visibility = View.GONE
            if (items.isEmpty()) {
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.text = "No users found"
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.progressLoad.visibility = View.GONE
                // 🔴🔒 V1602 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ) — তালিকা সফলভাবে
                // এলেও "Loading..." লেখাটা (tvEmpty) কখনো লুকানো হতো না, তাই
                // তালিকার উপরে চিরকাল আটকে থাকত। "কোনো ইউজার নেই" শাখায় এটা
                // আগে থেকেই লুকানো হতো, শুধু এই সফল-শাখাতেই বাদ পড়েছিল।
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.updateItems(items)
            }
        }
    }

    private fun showChangeDialog(item: UserCredential) {
        val input = EditText(this).apply {
            setText(item.password)
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Password: ${item.account.name.ifBlank { item.account.mobile }}"))
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pw = input.text.toString().trim()
                if (pw.isBlank()) {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    input.requestFocus()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try { com.tkbiswas.pilesclinic.modules.ModuleAuth.signInCurrentSession(this@PasswordCenterActivity) } catch (_: Throwable) { }
                        repository.savePassword(item.account, pw, user.mobile)
                    }
                    Toast.makeText(
                        this@PasswordCenterActivity,
                        if (ok) "Password saved" else "Failed — check connection",
                        Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) loadList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }
}
