package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.util.Base64
import org.json.JSONObject

/**
 * Holds the current logged-in session for the native screens (Step 1: Login +
 * Dashboard), persisted in SharedPreferences so staff stay logged in across
 * app restarts -- same intent as the WebView's own localStorage 'rk_session'.
 *
 * TK-REQUESTED (2026-07-19): Doctor and Field Officer now have the exact
 * same permissions/access as Staff -- only the on-screen label differs
 * ("Doctor Dashboard" / "Field Officer Dashboard" vs "Staff Dashboard").
 * `role` is the PERMISSION value every existing check in the app already
 * uses (listOf("master","staff",...) etc.) -- for doctor/field accounts
 * this is now "staff" too, so every one of those checks across the whole
 * app works correctly with ZERO changes needed anywhere else.
 * `displayRole` keeps the real original role ("doctor"/"field"/"staff"/
 * "master") purely for showing the correct label on screen.
 */
data class NativeUser(val mobile: String, val name: String, val branch: String, val role: String, val displayRole: String = role) {
    companion object {
        /** doctor/field -> staff for every permission check; master/staff pass through unchanged. */
        fun permissionRole(actualRole: String): String =
            if (actualRole == "doctor" || actualRole == "field") "staff" else actualRole
    }
}

object NativeSession {
    private const val PREFS = "piles_clinic_native_session"
    private const val KEY_MOBILE = "mobile"
    private const val KEY_NAME = "name"
    private const val KEY_BRANCH = "branch"
    private const val KEY_ROLE = "role"
    private const val KEY_DISPLAY_ROLE = "displayRole"

    fun save(context: Context, user: NativeUser) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MOBILE, user.mobile)
            .putString(KEY_NAME, user.name)
            .putString(KEY_BRANCH, user.branch)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_DISPLAY_ROLE, user.displayRole)
            .commit()
        // 🔐🔒 V494 (TK-যাচাই ২): লগইন / ব্যবহারকারী বদল — জমানো cloud-পড়া
        // সঙ্গে সঙ্গে মুছে যায় ও নতুন পরিচয় বসে, তাই আগের জনের তালিকা
        // কখনো নতুন জনের পর্দায় আসতে পারে না।
        try { CloudReadDedupe.setSession(user.mobile) } catch (_: Throwable) { }
        // 🟢🔒 V601 (২৪.০৮.২০২৬) — সদ্য পাসওয়ার্ড দিয়ে ঢুকলেন মানেই পরিচয়
        // প্রমাণিত হলো, তাই ২৪-ঘণ্টার ঘড়ি এখান থেকেই শুরু — লগইনের পরপরই
        // আবার আঙুল চাইবে না।
        try { AppLock.recordLoginUnlock(context) } catch (_: Throwable) { }
        /* 📱🔒 V889 (৩০.০৮.২০২৬, TK-নির্দেশ) — *"কোন ফোনে কে লগইন জমা
           রাখুন"*। শুধু লগইনের সময় একটাই সারি বসে (deviceId-ই আইডি), তাই
           Egress প্রায় শূন্য। ব্যর্থ হলেও লগইন কখনো আটকায় না। */
        try { DeviceLoginLog.record(context, user.mobile, user.name, user.branch, user.role) } catch (_: Throwable) { }
    }

    fun current(context: Context): NativeUser? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedRole = p.getString(KEY_ROLE, null) ?: return null
        val mobile = p.getString(KEY_MOBILE, null) ?: return null
        val name = p.getString(KEY_NAME, "") ?: ""
        val branch = p.getString(KEY_BRANCH, "") ?: ""
        // TK-REQUESTED (2026-07-19): displayRole falls back to the stored
        // role for sessions saved before this change (so an already
        // logged-in doctor/field account doesn't get logged out or break
        // -- it just won't show the special label until their next login,
        // completely harmless).
        val displayRole = p.getString(KEY_DISPLAY_ROLE, storedRole) ?: storedRole
        // 🔐🔒 V494 (TK-যাচাই ২) — তৃতীয় সুরক্ষা-জাল। উপরের save()/clear()
        // ছাড়াও অ্যাপের যেকোনো পথে (জোর করে সাইন-আউট, অন্য কোড থেকে
        // SharedPreferences বদল) পরিচয় বদলালে সেটা এখানেই ধরা পড়ে —
        // কারণ প্রতিটা পর্দা কাজ শুরুর আগে current() ডাকে।
        // ⛔ পরিচয় একই থাকলে setSession() কিছুই করে না (সস্তা)।
        try { CloudReadDedupe.setSession(mobile) } catch (_: Throwable) { }
        return NativeUser(mobile, name, branch, NativeUser.permissionRole(storedRole), displayRole)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        // 🔐🔒 V494 (TK-যাচাই ২): লগআউট — সব জমানো cloud-পড়া মুছে যায়।
        try { CloudReadDedupe.setSession(null) } catch (_: Throwable) { }
        /* 🔴🔒 V721 — লগ-আউটে রোগীর জমানো প্রসঙ্গও মুছে যায়, যাতে অন্য কেউ
           লগইন করলে আগের রোগীর নাম/ব্রাঞ্চ কিছুতেই ফিরে না আসে। */
        try { com.tkbiswas.pilesclinic.clinical.RoleSession.clearPersisted() } catch (_: Throwable) { }
    }

    /** Builds the same {mobile,name,branch,role} JSON shape app.js's own login()
     * already stores in localStorage, base64-encodes it, and returns it ready to
     * append as MainActivity's ?nativeSession=... query param -- see the
     * matching boot() change in app.js. */
    fun toHandoffParam(user: NativeUser): String {
        val json = JSONObject()
            .put("mobile", user.mobile)
            .put("name", user.name)
            .put("branch", user.branch)
            .put("role", user.role)
            .toString()
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
}
