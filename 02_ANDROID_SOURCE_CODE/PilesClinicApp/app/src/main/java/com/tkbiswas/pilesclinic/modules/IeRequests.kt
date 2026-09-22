package com.tkbiswas.pilesclinic.modules

import org.json.JSONArray
import org.json.JSONObject
import com.tkbiswas.pilesclinic.native.s   // 🔴 V819 — SQL NULL-নিরাপদ পড়া

/* =====================================================================
   🟢🆕🔒 V401 (16.08.2026) — Staff/Doctor-এর পাঠানো "পুরনো তারিখের আয়-খরচ"
   অনুরোধ পড়া ও মাস্টারের অনুমোদন/নাকচ। ঘণ্টার পর্দা (BriefingActivity) এটাই ডাকে।

   TK-নির্দেশ: *"পুরাতন কোন হিসাব তুলতে গেলে অথবা Edit করতে গেলে Master এর
   অনুমতি লাগবে"* · সিদ্ধান্ত: *"অনুরোধ পাঠাবে, আপনি ঘণ্টায় দেখে Approve করবেন"*

   ⛔ Payment Backdate / Payment Edit / Referral Edit — এই তিনটের হুবহু একই
      প্রমাণিত ছাঁচ। নতুন কোনো পদ্ধতি বানানো হয়নি।
   ⛔ অনুমোদনের আসল কাজটা ডেটাবেসের `fin.ie_decide_request()` করে (master-only),
      তাই অ্যাপের কোনো ফাঁকি দিয়ে কেউ নিজের অনুরোধ পাশ করাতে পারবে না।
   ===================================================================== */
object IeRequests {

    /** ঘণ্টার সংখ্যা — শুধু pending কটা আছে। ⚠️ থ্রেড থেকে ডাকুন। */
    fun pendingCount(): Int = try {
        val r = ModuleAuth.getRowsChecked("fin", "ie_requests", "select=id&status=eq.PENDING")
        if (r.ok) r.rows.length() else 0
    } catch (_: Throwable) { 0 }

    /** মাস্টারের পর্দার জন্য পুরো তালিকা (পুরনো আগে)। ⚠️ থ্রেড থেকে ডাকুন। */
    fun fetchPending(): JSONArray = try {
        val r = ModuleAuth.getRowsChecked(
            "fin", "ie_requests",
            "select=*&status=eq.PENDING&order=requested_at.asc&limit=100"
        )
        if (r.ok) r.rows else JSONArray()
    } catch (_: Throwable) { JSONArray() }

    /** Approve / Reject — ডেটাবেসের master-only ফাংশন। ⚠️ থ্রেড থেকে ডাকুন। */
    fun decide(id: String, approve: Boolean, note: String = ""): ModuleAuth.RpcResult = try {
        ModuleAuth.rpc(
            "fin", "ie_decide_request",
            JSONObject().put("p_id", id).put("p_approve", approve)
                .put("p_note", if (note.isBlank()) JSONObject.NULL else note)
        )
    } catch (_: Throwable) {
        ModuleAuth.RpcResult(false, "", "Could not reach the server — try again")
    }

    /** এক লাইনের সহজ বর্ণনা — ঘণ্টার কার্ডে দেখানোর জন্য (ইংরেজি, TK-নির্দেশ)। */
    fun describe(r: JSONObject, money: (Double) -> String, slashDate: (String) -> String): String {
        val p = r.optJSONObject("payload") ?: JSONObject()
        val what = when (r.optString("kind")) {
            "ADD_COLLECTION"  -> "Add Collection — Cash " + money(p.optDouble("cash", 0.0)) +
                                 " · Online " + money(p.optDouble("online", 0.0))
            "EDIT_COLLECTION" -> "Edit Collection — Cash " + money(p.optDouble("cash", 0.0)) +
                                 " · Online " + money(p.optDouble("online", 0.0))
            "ADD_EXPENSE"     -> "Add Expense — " + p.optString("category", "") +
                                 (p.optString("paid_to", "").let { if (it.isBlank()) "" else " · $it" }) +
                                 " · " + money(p.optDouble("amount", 0.0))
            "EDIT_EXPENSE"    -> "Edit Expense — " + p.optString("category", "") +
                                 (p.optString("paid_to", "").let { if (it.isBlank()) "" else " · $it" }) +
                                 " · " + money(p.optDouble("amount", 0.0))
            else              -> r.optString("kind")
        }
        val who = r.s("requested_by_name").ifBlank { r.s("requested_by") }   // 🔴 V819 — `optString` SQL NULL-এ আক্ষরিক "null" ফেরায় (V696/V812-এর ফাঁদ); `s()` সেটা ফাঁকা ধরে
        val why = r.optString("reason", "").let { if (it.isBlank() || it == "null") "" else "\nReason: $it" }
        return slashDate(r.optString("entry_date", "")) + " · " + r.optString("branch", "") +
               "\n" + what + "\nBy: " + who + why
    }
}
