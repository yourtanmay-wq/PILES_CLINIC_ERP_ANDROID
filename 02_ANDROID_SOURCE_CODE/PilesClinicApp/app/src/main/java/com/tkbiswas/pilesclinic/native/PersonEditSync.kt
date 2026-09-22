package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.print.BranchCatalog
import org.json.JSONObject

/**
 * 🟢🔒 V1139 (০৬.০৯.২০২৬, TK-নির্দেশ) — **এক জায়গায় নাম/ঠিকানা ঠিক করলে
 * সেটা পুরো প্রজেক্টে বসবে।**
 *
 * TK-এর কথা (হুবহু): *"আমি এত কিছু বুঝি না, এডিট করবো — এখানে এডিট হবে,
 * এবং সম্পূর্ণ প্রজেক্টে এই পেশেন্টের ডিটেলস আপডেট হয়ে যাবে।"*
 *
 * ─── 🔴 আগে কেন হতো না (কোডে মেপে পাওয়া, আন্দাজ নয়) ─────────────────────
 * ① "কাল আসার কথা" পর্দায় **এডিটের কোনো পথই ছিল না** — শুধু পড়া যেত।
 * ② Follow-up-এর এডিট ঠিকানা লিখত শুধু `patients`/`enquiries`-এ; `followups`-এ
 *    নয় (ওখানকার পুরনো মন্তব্যে ভুল লেখা ছিল "followups-এ address নেই",
 *    অথচ ঘরটা আছে আর ওই পর্দা ঠিকানা **ওখান থেকেই** পড়ে)।
 * ③ "কাল আসার কথা" কার্ডের **নাম** আসে `payments`-এর `chamber_expected` সারি
 *    থেকে, আর কোনো এডিটই ওই সারিটা ছুঁত না।
 * ⇒ ফল: নাম বা ঠিকানা ঠিক করলেও ওই কার্ডে পুরনোটাই থেকে যেত।
 *
 * ─── এখন কী হয় ───────────────────────────────────────────────────────────
 * একই নম্বরের **চারটে ঘরেই** এক সঙ্গে বসে — `followups` · `patients` ·
 * `enquiries` · `payments`-এর `chamber_expected` সারি।
 *
 * ⛔ **টাকার কোনো হিসাব ছোঁয়া হয় না** — payments-এ কেবল `name` ঘরটা,
 *    আর সেটাও শুধু `chamber_expected` ধরনের সারিতে (অঙ্ক · তারিখ · ধরন অটুট)।
 * ⛔ **ফাঁকা মানে "বদলিও না"** — নাম বা ঠিকানা ফাঁকা পাঠালে ওই ঘরটা ছোঁয়াই হয় না।
 * ⛔ ব্রাঞ্চ দেওয়া থাকলে **শুধু সেই ব্রাঞ্চের** সারিই বদলায় — এক নম্বরে
 *    একাধিক ব্রাঞ্চে রোগী থাকলে অন্যজনের সারি নিরাপদ থাকে।
 * ⛔ নেট খারাপ হলে যেটুকু বসেনি সেটুকুই বাদ — কিছু ভাঙে না, ভুল কিছু লেখেও না।
 */
object PersonEditSync {

    /**
     * 🔴🔒 V1145 (০৬.০৯.২০২৬, TK-নির্দেশ) — সময়ের ধরন ("Official Time" /
     * "Unexpected Time") এক নম্বরের **তিন টেবিলেই** বসায়।
     *
     * TK-এর কথা: *"অফিসিয়াল টাইমে কল এসেছিল, স্টাফ ভুল করে আনএক্সপেক্টেড করে
     * দিয়েছিল … এগুলো তো টাকা পয়সার হিসাব"*।
     *
     * ⚠️ **ব্রাঞ্চ মিলিয়ে দেখা হয় না — ইচ্ছে করেই।** KASHAB MANDAL-এর ডেটায়
     *    মেপে দেখা গেছে: রোগীর সারি **Jalpaiguri**, অথচ এনকোয়ারির সারি
     *    **Cooch Behar** (কল অন্য ব্রাঞ্চের ফোনে এসেছিল)। ব্রাঞ্চ মিলিয়ে
     *    বদলালে ঠিক ওই এনকোয়ারির সারিটাই বাদ পড়ত — আর **টাকার হিসেব ওটাই
     *    পড়ে** (`UnexpectedIncentive` ⇒ `enquiries.timeType`)। তাই এখানে
     *    নম্বরই শেষ কথা।
     * ⛔ ফাঁকা পাঠালে কিছুই বদলায় না। ⛔ টাকার কোনো সারি ছোঁয়া হয় না।
     */
    fun updateTimeType(mobile: String, timeType: String): Boolean {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        val t = timeType.trim()
        if (digits.length != 10 || t.isBlank()) return false
        var any = false
        for (table in listOf("patients", "enquiries", "followups")) {
            try {
                val rows = SupabaseClient.findByMobile(table, digits, "id,timeType", 50)
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val id = row.optString("id", "")
                    if (id.isBlank()) continue
                    if (row.optString("timeType", "").trim().equals(t, ignoreCase = true)) continue
                    val ok = SupabaseClient.updateById(table, id, JSONObject().put("timeType", t))
                    if (ok) any = true
                }
            } catch (_: Throwable) { }
        }
        return any
    }

    /** এক নম্বরের সব সারিতে নাম ও ঠিকানা বসায়। অন্তত একটা সারি বসলে `true`। */
    fun updateNameAddress(
        mobile: String,
        branch: String,
        newName: String,
        newAddress: String
    ): Boolean {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return false
        val name = newName.trim()
        val address = newAddress.trim()
        if (name.isBlank() && address.isBlank()) return false

        var any = false
        // ── ① followups · ② patients · ③ enquiries ───────────────────────
        for (table in listOf("followups", "patients", "enquiries")) {
            try {
                val rows = SupabaseClient.findByMobile(table, digits, "id,branch", 50)
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val id = row.optString("id", "")
                    if (id.isBlank()) continue
                    if (!sameBranch(row.optString("branch", ""), branch)) continue
                    val fields = JSONObject()
                    if (name.isNotBlank()) fields.put("name", name)
                    if (address.isNotBlank()) fields.put("address", address)
                    if (SupabaseClient.updateById(table, id, fields)) any = true
                }
            } catch (_: Throwable) { }
        }

        // ── ④ "কাল আসার কথা" কার্ড যে সারিটা থেকে নাম পড়ে ────────────────
        //    ⛔ শুধু `chamber_expected` ধরনের সারি, আর শুধু `name` ঘরটা।
        if (name.isNotBlank()) {
            try {
                val rows = SupabaseClient.fetchListSlim(
                    "payments", "payType=eq.chamber_expected&mobile=like.*$digits", 50, "id,branch"
                )
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val id = row.optString("id", "")
                    if (id.isBlank()) continue
                    if (!sameBranch(row.optString("branch", ""), branch)) continue
                    val fields = JSONObject()
                        .put("name", name)
                        .put("updatedAt", SupabaseClient.localWriteStamp())
                    if (SupabaseClient.updateById("payments", id, fields)) any = true
                }
            } catch (_: Throwable) { }
        }
        return any
    }

    /** ব্রাঞ্চ না জানা থাকলে (দু'দিকের কোনোটা ফাঁকা) সারিটা ধরা হয়। */
    private fun sameBranch(rowBranch: String, want: String): Boolean {
        val a = rowBranch.trim()
        val b = want.trim()
        if (a.isBlank() || b.isBlank()) return true
        return BranchCatalog.byName(a).id == BranchCatalog.byName(b).id
    }
}
