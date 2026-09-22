package com.tkbiswas.pilesclinic.modules

import android.content.Context
import org.json.JSONObject

/* =====================================================================
   🟢🆕🔒 V401 (16.08.2026) — "কার আয়-খরচ তোলার চাবি চালু আছে" জানার একটাই জায়গা।

   TK-নির্দেশ: *"Doctor ও staff যেন আয় এবং খরচ তুলতে পারে … শুধু যাঁদের মাস্টার
   চালু করবেন … নিজস্ব ব্রাঞ্চ ছাড়া অন্য ব্রাঞ্চের কোন হিসাব সে দেখতে পাবে না"*

   আসল নিরাপত্তা **ডেটাবেসেই** (fin.entry_permits + RLS, V401 SQL) — এই ফাইল
   শুধু অ্যাপকে জানায় কোন পর্দা দেখাতে হবে, যাতে ব্যবহারকারীর সামনে অকারণ
   ভুল-বার্তা না আসে। এখানে কিছু ফাঁকি দিলেও ডেটাবেস আটকাবে।

   ⛔ মাস্টার ও অংশীদার-ডাক্তারের পুরনো পথ এক অক্ষরও বদলায়নি — এটা তাঁদের
      ছাড়া বাকিদের জন্য বাড়তি একটা পথ।
   ===================================================================== */
object IePermit {

    private const val PREF = "piles_ie_permit"
    private const val KEY_BRANCHES = "branches"     // কমা দিয়ে আলাদা
    private const val KEY_LOADED = "loaded"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** এই ফোনে শেষবার জানা চাবি-চালু ব্রাঞ্চের তালিকা (নেট ছাড়াও কাজ করে)। */
    fun cached(ctx: Context): List<String> = try {
        prefs(ctx).getString(KEY_BRANCHES, "").orEmpty()
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
    } catch (_: Throwable) { emptyList() }

    /** একবারও ক্লাউড থেকে জানা হয়েছে কিনা (প্রথমবার "চাবি নেই" ভেবে ভুল বার্তা ঠেকাতে)। */
    fun everLoaded(ctx: Context): Boolean = try { prefs(ctx).getBoolean(KEY_LOADED, false) } catch (_: Throwable) { false }

    fun has(ctx: Context): Boolean = cached(ctx).isNotEmpty()

    fun isMyBranch(ctx: Context, branch: String): Boolean =
        cached(ctx).any { it.equals(branch.trim(), ignoreCase = true) }

    /** ক্লাউড থেকে নিজের চাবি টেনে এনে জমা রাখে। **শুধু নিজেরটাই** আসে —
     *  ডেটাবেসের নিয়মেই (ie_permits_self_read) অন্যেরটা দেখা যায় না।
     *  ⚠️ এটা একটা নেটওয়ার্ক কল — তাই আলাদা থ্রেড থেকে ডাকতে হবে। */
    fun refresh(ctx: Context): List<String> {
        return try {
            /* 🔴 জরুরি: মেনু-পর্দা থেকে ডাকলে Module-সেশন এখনো খোলা নাও থাকতে পারে
               (ওটা খোলে ModuleUi.ensureSignedIn, শুধু মডিউলে ঢোকার সময়)। সেশন ছাড়া
               এই পড়া ব্যর্থ হত আর চাবি-থাকা স্টাফও বোতাম দেখতে পেত না।
               তাই দরকার হলে এখানেই চুপচাপ সেশন খোলা হয় — ব্যবহারকারীর সামনে
               কোনো পাসওয়ার্ড পর্দা আসে না (V247-এর সেই প্রমাণিত পথ)। */
            if (!ModuleAuth.isSignedIn) {
                try { ModuleAuth.signInCurrentSession(ctx.applicationContext) } catch (_: Throwable) { }
            }
            if (!ModuleAuth.isSignedIn) return cached(ctx)
            val r = ModuleAuth.getRowsChecked(
                "fin", "entry_permits", "select=branch,can_entry&can_entry=is.true"
            )
            if (!r.ok) return cached(ctx)          // নেট খারাপ — শেষ জানা তথ্যই থাক
            val list = ArrayList<String>()
            for (i in 0 until r.rows.length()) {
                val b = r.rows.getJSONObject(i).optString("branch", "").trim()
                if (b.isNotBlank() && !list.any { it.equals(b, true) }) list.add(b)
            }
            prefs(ctx).edit()
                .putString(KEY_BRANCHES, list.joinToString(","))
                .putBoolean(KEY_LOADED, true)
                .apply()
            list
        } catch (_: Throwable) { cached(ctx) }
    }

    /** লগ-আউট / ব্যবহারকারী বদলালে পুরনো চাবি যেন থেকে না যায়। */
    fun clear(ctx: Context) {
        try { prefs(ctx).edit().clear().apply() } catch (_: Throwable) { }
    }

    // -----------------------------------------------------------------
    // অনুরোধ পাঠানো — পুরনো তারিখে তুলতে/বদলাতে চাইলে।
    // (ডেটাবেসের fin.ie_request(...) ফাংশন, V401)
    // -----------------------------------------------------------------
    const val ADD_COLLECTION  = "ADD_COLLECTION"
    const val ADD_EXPENSE     = "ADD_EXPENSE"
    const val EDIT_COLLECTION = "EDIT_COLLECTION"
    const val EDIT_EXPENSE    = "EDIT_EXPENSE"

    /** সফল হলে ok=true; না হলে ডেটাবেসের নিজের বার্তা ফেরে (ব্যবহারকারীকে দেখানোর জন্য)।
     *  ⚠️ নেটওয়ার্ক কল — থ্রেড থেকে ডাকুন। */
    fun sendRequest(
        kind: String, branch: String, entryDateIso: String,
        targetId: String?, payload: JSONObject, reason: String
    ): ModuleAuth.RpcResult = try {
        val args = JSONObject()
            .put("p_kind", kind)
            .put("p_branch", branch)
            .put("p_entry_date", entryDateIso)
            .put("p_target_id", if (targetId.isNullOrBlank()) JSONObject.NULL else targetId)
            .put("p_payload", payload)
            .put("p_reason", reason)
        ModuleAuth.rpc("fin", "ie_request", args)
    } catch (t: Throwable) {
        ModuleAuth.RpcResult(false, "", "Could not send the request — try again")
    }
}
