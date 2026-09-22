package com.tkbiswas.pilesclinic.native

import android.content.Context
import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/* =====================================================================
   🔵 B618 (11.08.2026, TK-নির্দেশ, ধাপে-ধাপে আলোচনা করে ফাইনাল) —
   ছুটির অনুরোধ অনুমোদন/নামঞ্জুর। কাজ করে `wn.leave_requests`-এ (module-লগইন
   লাগে — RLS: master বা doctor আপডেট করতে পারে; V311)। তাই ডাকার আগে
   BriefingActivity-তে ModuleUi.ensureSignedIn() করা থাকতে হবে।
   ⛔ পুরনো কোনো টেবিল/হিসাব ছোঁয় না — শুধু নতুন wn.leave_requests +
   confirmed হলে wn.notebook_days-এ is_leave (পুরনো হাজিরা/রিপোর্ট অটুট) +
   briefings-এ নোটিশ (আগের প্রমাণিত পথ)।
   ===================================================================== */
object LeaveRepository {

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date())
    }
    private fun enc(s: String) = try { java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20") } catch (_: Throwable) { s }
    private fun dotDate(iso: String): String = try { val p = iso.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (_: Throwable) { iso }

    /** V1528: prior-day pending leave is no longer actionable. On opening the
     *  approval list, close stale rows in Cloud first; the existing V1520 DB
     *  trigger then soft-closes the matching Leave Request notice. Only today's
     *  or future pending requests are returned. */
    fun fetchPending(user: NativeUser): JSONArray {
        val isMaster = user.role == "master"
        val todayIst = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(java.util.Date())
        try {
            ModuleAuth.update(
                "wn", "leave_requests",
                "status=eq.pending&leave_date=lt.${enc(todayIst)}",
                JSONObject().put("status", "expired").put("updated_at", nowIso())
            )
        } catch (_: Throwable) { }
        val q = if (isMaster)
            "select=*&status=eq.pending&leave_date=gte.${enc(todayIst)}&order=leave_date.asc"
        else
            "select=*&status=eq.pending&leave_date=gte.${enc(todayIst)}&branch=eq.${enc(user.branch)}&order=leave_date.asc"
        return try { ModuleAuth.getRows("wn", "leave_requests", q) } catch (_: Throwable) { JSONArray() }
    }

    /** Approve: status=confirmed + ওই দিনের notebook_days-এ is_leave + ব্রাঞ্চে নোটিশ। */
    fun approve(context: Context, req: JSONObject, approverMobile: String): Boolean {
        val id = req.optString("id"); if (id.isBlank()) return false
        val staffCode = req.optString("staff_code")
        val staffMobile = req.optString("staff_mobile")
        val branch = req.optString("branch")
        val date = req.optString("leave_date")
        val reason = req.optString("reason")
        val patch = JSONObject().put("status", "confirmed").put("decided_by", approverMobile)
            .put("decided_at", nowIso()).put("updated_at", nowIso())
        val ok = try { ModuleAuth.update("wn", "leave_requests", "id=eq.${enc(id)}", patch) } catch (_: Throwable) { false }
        if (!ok) return false
        try {
            val nd = JSONObject().put("staff_code", staffCode).put("staff_mobile", staffMobile)
                .put("work_date", date).put("is_leave", true).put("leave_reason", reason).put("updated_at", nowIso())
            ModuleAuth.upsertOnConflict("wn", "notebook_days", nd, "staff_code,work_date")
        } catch (_: Throwable) { }
        try {
            val msg = "👤 Staff : ${staffCode.ifBlank { staffMobile }}\n🏥 Branch : $branch\n🏖️ Leave : " +
                dotDate(date) + "\nReason : " + reason + "\n✅ Approved"
            BriefingRepository().post(context, "Staff Leave", msg, "branch", branch, "", approverMobile)
        } catch (_: Throwable) { }
        return true
    }

    /** Reject: status=rejected + স্টাফকে ব্যক্তিগত নোটিশ (কাজে আসবেন)। */
    fun reject(context: Context, req: JSONObject, approverMobile: String): Boolean {
        val id = req.optString("id"); if (id.isBlank()) return false
        val staffMobile = req.optString("staff_mobile")
        val branch = req.optString("branch")
        val date = req.optString("leave_date")
        val patch = JSONObject().put("status", "rejected").put("decided_by", approverMobile)
            .put("decided_at", nowIso()).put("updated_at", nowIso())
        val ok = try { ModuleAuth.update("wn", "leave_requests", "id=eq.${enc(id)}", patch) } catch (_: Throwable) { false }
        if (!ok) return false
        try {
            val msg = "🏖️ Leave request rejected\nDate : " + dotDate(date) + "\nBranch : $branch\nPlease come to work."
            BriefingRepository().post(context, "Leave rejected", msg, "individual", branch, "", approverMobile, staffMobile)
        } catch (_: Throwable) { }
        return true
    }
}
