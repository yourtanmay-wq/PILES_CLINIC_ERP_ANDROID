package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONObject
import com.tkbiswas.pilesclinic.native.s

/**
 * 👥🔒 V746 (২৭.০৮.২০২৬, TK-অনুমোদিত) — **মাস্টার নিজে স্টাফ ও ডাক্তার
 * যোগ / বাদ / ফেরাতে পারবেন** — SQL ছাড়া, নতুন APK ছাড়া।
 *
 * TK-এর কথা: *"আপনি তো আর আমার সাথে সারা জীবন থাকবেন না।"*
 *
 * ─────────────────────────────────────────────────────────────────────
 * ⛔ **এই ফাইলটা নিজে কোনো নিয়ম যাচাই করে না** — একটাও না।
 *    সব পাহারা **সার্ভারে** (`00_SQL/V745_STAFF_DOCTOR_FROM_APP.sql`):
 *      · শুধু মাস্টার ডাকতে পারেন
 *      · master ভূমিকা এখান থেকে বানানো যায় না
 *      · মাস্টারকে বাদ দেওয়া যায় না
 *      · মোবাইল ১০ অঙ্ক, আর অন্য কারও নামে থাকলে চলবে না
 *      · নাম · কোড · ব্রাঞ্চ — ফাঁকা চলবে না
 *    ⇒ ফোনের অ্যাপ বদলে ফেললেও এই নিয়মগুলো ফাঁকি দেওয়ার পথ নেই।
 *
 * ⛔ **কাউকে কখনো মোছা হয় না** — শুধু "নিষ্ক্রিয়" করা হয়। তাই পুরনো
 *    রেকর্ডে নাম চিরকাল থেকে যায়, আর লগইন নিজে থেকেই বন্ধ হয় (V403)।
 * ⛔ টাকার কোনো হিসাব ছোঁয়া হয় না — সেসব মোবাইল ধরে চলে (V308)।
 * ⚡ প্রতিটা কাজ **একটাই ছোট RPC**, চাপ দিলে তবেই যায়।
 */
object PeopleAdminRepository {

    /** ⚠️ নাম `Result` রাখা যায়নি — Android-এর নিজের `Result` ক্লাসের সঙ্গে
     *  গুলিয়ে গিয়ে গার্ড অন্য ফাইলে ভুয়া ভুল দেখাচ্ছিল (যাচাই করে ধরা)। */
    data class AdminResult(val ok: Boolean, val message: String)

    data class Person(
        val code: String,
        val name: String,
        val mobile: String,
        val branch: String,
        val role: String,
        val active: Boolean
    )

    private const val SCHEMA = "hr"

    /** ⛔ নেট/লগইন ব্যর্থ হলেও অ্যাপ থামে না — সৎ বার্তা নিয়ে ফেরে। */
    private fun call(fn: String, args: JSONObject): AdminResult {
        val r = try { ModuleAuth.rpc(SCHEMA, fn, args) } catch (_: Throwable) { null }
            ?: return AdminResult(false, "Could not reach the server. Check internet.")
        if (!r.ok) {
            return AdminResult(false, r.message.ifBlank { "Could not reach the server. Check internet." })
        }
        val o = try { JSONObject(r.body) } catch (_: Throwable) { JSONObject() }
        val ok = o.optBoolean("ok", false)
        return AdminResult(ok, o.optString("message", if (ok) "Done" else "Could not do it"))
    }

    /** নতুন স্টাফ বা ডাক্তার। `role` = "staff" বা "doctor"। */
    fun add(code: String, mobile: String, name: String, branch: String, role: String): AdminResult =
        call(
            "admin_create_person",
            JSONObject()
                .put("p_code", code)
                .put("p_mobile", mobile)
                .put("p_name", name)
                .put("p_branch", branch)
                .put("p_role", role)
        )

    // 🧹 V761 — `setActive()` ও `list()` মুছে দেওয়া হলো।
    //    TK ধরিয়ে দিয়েছেন: Remove · Restore · Suspend মূল Staff Profiles
    //    পর্দায় **আগে থেকেই** আছে (V404/V603)। ওগুলো আবার বানানো ছিল
    //    নিছক ডুপ্লিকেট — তাই কোডও রাখা হলো না, শুধু `add()` থাকল।

    /** পর্দায় দেখানোর জন্য — "Staff" / "Doctor" / "Field Officer"। */
    fun roleLabel(role: String): String = when (role.trim().lowercase()) {
        "doctor" -> "Doctor"
        "field" -> "Field Officer"
        else -> "Staff"
    }
}
