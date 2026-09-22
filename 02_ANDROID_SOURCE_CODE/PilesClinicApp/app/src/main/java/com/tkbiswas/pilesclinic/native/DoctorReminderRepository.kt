package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * 🔔🔒 V1186 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **Doctor Note & Reminder**,
 * এখন নিজের টেবিলে।
 *
 * TK-এর কথা (হুবহু):
 *  · *"এটা প্রত্যেকের হোম স্ক্রিনে ই থাকবে"*
 *  · *"যে কোন staff, যে কোন ডাক্তার এবং মাস্টার এটা ক্রিয়েট করতে পারবে"*
 *  · *"যে ব্রাঞ্চের যে Doctor তাকেই করতে পারবে"* — তারপর পাঁচটা ব্রাঞ্চের তালিকা
 *  · *"পাঠানোর পর KH MANDAL কে Accept করতে হবে"*
 *  · *"পরে যেন History তে ও দেখতে পায়, যাতে কেউ অস্বীকার না করতে পারে"*
 *
 * ⛔ **পুরনো ব্যবস্থা ছোঁয়া হয়নি** — `patients.doctorReminder*` ঘরগুলো ও
 *    `DoctorReminderWorker`-এর পুরনো পথ হুবহু আগের মতোই চলে; এটা তার
 *    **পাশাপাশি** নতুন একটা টেবিল, তাই আগের কোনো রিমাইন্ডার হারায় না।
 * ⚠️ ক্লাউড ছুঁতে পারে — `Thread`/`Dispatchers.IO`-তে ডাকতে হবে।
 */
object DoctorReminderRepository {

    const val TABLE = "doctor_reminders"

    /* 🩺🔒 V1186 — TK-এর পাঁচটা তালিকা প্রকল্পের স্টাফ-তালিকার সঙ্গে মিলিয়ে
       দেখা হয়েছে; পাঁচটাই **একটাই নিয়মে** হুবহু মেলে:
           ওই ব্রাঞ্চের নিজের ডাক্তার + Dr. K.H MANDAL + TK BISWAS
       (Kishanganj → AMIT GOLDAR · Jalpaiguri → JAY BANIK ·
        Cooch Behar → K.H MANDAL · J.H MANDAL · GOKUL ·
        Falakata → SAIKAT ROY · Birpara → PRANAB BISWAS)
       ⇒ তাই পাঁচটা তালিকা হাতে না লিখে নিয়মটাই বসানো হলো — ভবিষ্যতে নতুন
         ডাক্তার এলে নিজে থেকেই ঠিক ব্রাঞ্চে বসবেন, কোড বদলাতে হবে না।
       ⛔ TK নিজে এই নিয়মটা মিলিয়ে দেখে পাশ করেছেন (০৭.০৯.২০২৬)। */
    private const val EVERYWHERE_DOCTOR = "7980993652"   // Dr. K.H MANDAL — সব ব্রাঞ্চে

    private fun digits(v: String) = v.filter { it.isDigit() }.takeLast(10)

    /** ওই ব্রাঞ্চের স্টাফ যাঁদের কাছে পাঠাতে পারবেন — নাম থেকে মোবাইল। */
    fun doctorsForBranch(branch: String): List<Pair<String, String>> {
        val all = try { StaffDirectory.allAccounts() } catch (_: Throwable) { emptyList() }
        val br = branch.trim()
        val out = ArrayList<Pair<String, String>>()
        for (a in all) {
            if (a.role != "doctor") continue
            val mine = a.branch.trim().equals(br, ignoreCase = true)
            val everywhere = digits(a.mobile) == EVERYWHERE_DOCTOR
            if (mine || everywhere) out.add(a.name to a.mobile)
        }
        // মাস্টার (TK BISWAS) সব ব্রাঞ্চেই থাকেন
        for (a in all) if (a.role == "master") out.add(a.name to a.mobile)
        return out.distinctBy { digits(it.second) }
    }

    private fun nowIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

    fun todayIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
            .format(java.util.Date())

    /** স্টাফ/ডাক্তার/মাস্টার — যে কেউ পাঠাতে পারেন। */
    fun send(
        patientId: String, patientName: String, patientMobile: String, branch: String,
        note: String, forMobile: String, forName: String, remindDate: String, remindTime: String,
        byUser: NativeUser?, disease: String = ""
    ): Boolean {
        return try {
            val row = JSONObject()
                .put("id", "drem_" + System.currentTimeMillis() + "_" + (0..999).random())
                .put("patientId", patientId)
                .put("patientName", patientName)
                .put("patientMobile", digits(patientMobile))
                .put("branch", branch)
                .put("disease", disease)          // 🩺 V1194 — TK: *"রোগের নাম দরকার তো"*
                .put("note", note)
                .put("forMobile", digits(forMobile))
                .put("forName", forName)
                .put("byMobile", digits(byUser?.mobile ?: ""))
                .put("byName", (byUser?.name ?: "").ifBlank { digits(byUser?.mobile ?: "") })
                .put("byBranch", byUser?.branch ?: "")
                .put("remindDate", remindDate)
                .put("remindTime", remindTime)
                .put("createdAt", nowIso())
                .put("acceptedBy", "")
                .put("acceptedByName", "")
                .put("acceptedAt", "")
                .put("cancelledBy", "")
                .put("cancelledByName", "")
                .put("cancelledAt", "")
                .put("active", true)
            SupabaseClient.upsert(TABLE, row).also { if (it) cacheAt = 0L }
        } catch (_: Throwable) { false }
    }

    /** যাঁকে পাঠানো হয়েছে তিনি মেনে নিলেন — কে ও কখন, দুটোই জমা থাকে। */
    fun accept(id: String, user: NativeUser?): Boolean {
        if (id.isBlank()) return false
        return try {
            SupabaseClient.updateById(
                TABLE, id,
                JSONObject()
                    .put("acceptedBy", digits(user?.mobile ?: ""))
                    .put("acceptedByName", (user?.name ?: "").ifBlank { digits(user?.mobile ?: "") })
                    .put("acceptedAt", nowIso())
            ).also { if (it) cacheAt = 0L }   // 🔄 V1193 — সঙ্গে সঙ্গে নতুন করে পড়া হবে
        } catch (_: Throwable) { false }
    }

    /**
     * এই ব্যবহারকারী যেগুলো দেখবেন। মাস্টার সব দেখেন; বাকিরা —
     * তাঁকে পাঠানো · তাঁর পাঠানো · অথবা তাঁর ব্রাঞ্চের "সব ডাক্তার"-কে পাঠানো।
     * ⛔ অন্য ব্রাঞ্চের কারো ব্যক্তিগত রিমাইন্ডার কখনো দেখা যায় না।
     */
    fun visibleFor(user: NativeUser?, historyMode: Boolean = false): List<JSONObject> {
        val me = digits(user?.mobile ?: "")
        val myBranch = (user?.branch ?: "").trim()
        val isMaster = user?.role == "master"
        return try {
            val filter = if (historyMode) "active=eq.true"
            else "active=eq.true&remindDate=gte.${todayIso()}"
            val rows = SupabaseClient.fetchList(TABLE, filter, 300, order = "createdAt.desc")
            val out = ArrayList<JSONObject>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                // 🚫 V1194 — বাতিল হওয়া সারি চলতি তালিকায় আসে না; History-তে আসে।
                if (!historyMode && r.optString("cancelledAt", "").isNotBlank()) continue
                if (isMaster) { out.add(r); continue }
                val forM = digits(r.optString("forMobile", ""))
                val byM = digits(r.optString("byMobile", ""))
                val br = r.optString("branch", "").trim()
                val mine = forM == me || byM == me ||
                    (forM.isEmpty() && br.equals(myBranch, ignoreCase = true))
                if (mine) out.add(r)
            }
            out
        } catch (_: Throwable) { emptyList() }
    }

    /* ─────────────────────────────────────────────────────────────
       🙈🔒 V1193 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু):
         · *"আমি তো পাঠালাম, তাহলে অল টাইম আমার হোম স্ক্রিনে কেন দেখাবে"*
         · *"এটা হাইড রাখার ব্যবস্থা তো রাখতে হবে"*
         · *"উপরের ঘন্টাতে নোটিফিকেশন আসুক"*
       ⇒ হোম-কার্ড ও ঘন্টা এখন **শুধু যেগুলো এই ব্যক্তির জন্য অপেক্ষা করছে**
         সেগুলোই দেখায়; নিজের পাঠানো কখনো নয়। Accept হলে সেটা পাঠানো
         ব্যক্তির ঘন্টায় একবার "Accepted" হয়ে আসে।
       ⛔ Doctor Reminder পর্দা ও History-তে **আগের মতোই সব** দেখা যায় —
          `visibleFor()` এক অক্ষরও বদলায়নি, তাই কিছুই হারায় না।
       ───────────────────────────────────────────────────────────── */

    private fun listHas(raw: String, mob: String): Boolean {
        if (mob.isEmpty()) return false
        return raw.split(",").any { it.trim() == mob }
    }

    private fun listAdd(raw: String, mob: String): String {
        if (mob.isEmpty()) return raw
        if (listHas(raw, mob)) return raw
        return if (raw.isBlank()) mob else "$raw,$mob"
    }

    /** এই ব্যক্তির হোম/ঘন্টা থেকে সরিয়ে দেওয়া — অন্য কারো পর্দায় কিছুই বদলায় না। */
    fun hide(row: JSONObject, user: NativeUser?): Boolean {
        val id = row.optString("id", "")
        val me = digits(user?.mobile ?: "")
        if (id.isBlank() || me.isEmpty()) return false
        return try {
            val next = listAdd(row.optString("hiddenBy", ""), me)
            val ok = SupabaseClient.updateById(TABLE, id, JSONObject().put("hiddenBy", next))
            if (ok) { row.put("hiddenBy", next); cacheAt = 0L }
            ok
        } catch (_: Throwable) { false }
    }

    /* 🚫🔒 V1194 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু):
         *"কেউ যদি ভুল করে বার্তাটা পাঠিয়ে দেয় তাহলে সে ডিলিট করতে পারবে"* ·
         *"ডিলিট লেখা থাকলে তো বিভ্রান্ত হতে পারে"* ⇒ লেখা **Cancel**।
       ⛔ **যিনি পাঠিয়েছেন কেবল তিনিই**, আর **Accept হওয়ার আগে পর্যন্ত**।
       ⛔ সারিটা **মোছা হয় না** — শুধু "বাতিল" চিহ্ন বসে, তাই History-তে
          "CANCELLED · কে · কখন" চিরকাল থাকে (TK: *"যাতে কেউ অস্বীকার না
          করতে পারে"*)। বাতিলের পর কারো তালিকা/হোম/ঘন্টায় আর আসে না। */
    fun canCancel(row: JSONObject, user: NativeUser?): Boolean {
        val me = digits(user?.mobile ?: "")
        if (me.isEmpty()) return false
        if (row.optString("acceptedAt", "").isNotBlank()) return false
        if (row.optString("cancelledAt", "").isNotBlank()) return false
        return digits(row.optString("byMobile", "")) == me
    }

    fun cancel(row: JSONObject, user: NativeUser?): Boolean {
        if (!canCancel(row, user)) return false
        val id = row.optString("id", "")
        if (id.isBlank()) return false
        return try {
            val at = nowIso()
            val nm = (user?.name ?: "").ifBlank { digits(user?.mobile ?: "") }
            val ok = SupabaseClient.updateById(
                TABLE, id,
                JSONObject()
                    .put("cancelledBy", digits(user?.mobile ?: ""))
                    .put("cancelledByName", nm)
                    .put("cancelledAt", at)
            )
            if (ok) {
                row.put("cancelledBy", digits(user?.mobile ?: ""))
                row.put("cancelledByName", nm)
                row.put("cancelledAt", at)
                cacheAt = 0L
            }
            ok
        } catch (_: Throwable) { false }
    }

    /** পাঠানো ব্যক্তি "Accepted" খবরটা দেখে নিলেন — আর ঘন্টায় আসবে না। */
    fun ack(row: JSONObject, user: NativeUser?): Boolean {
        val id = row.optString("id", "")
        val me = digits(user?.mobile ?: "")
        if (id.isBlank() || me.isEmpty()) return false
        return try {
            val next = listAdd(row.optString("ackBy", ""), me)
            val ok = SupabaseClient.updateById(TABLE, id, JSONObject().put("ackBy", next))
            if (ok) { row.put("ackBy", next); cacheAt = 0L }
            ok
        } catch (_: Throwable) { false }
    }

    /**
     * হোম-কার্ড ও ঘন্টার একটাই উৎস — **এই ব্যক্তির জন্য অপেক্ষা করছে** এমনগুলো।
     * ⛔ নিজের পাঠানো কখনো নয় · Accept হয়ে গেলে নয় · নিজে Hide করলে নয়।
     * ⛔ ঘন্টার সংখ্যা আর তালিকা একই ফাংশন থেকে আসে, তাই কখনো আলাদা হতে পারে না।
     */
    /* 💸🔒 V509-এর শিক্ষা (Egress) — ঘন্টা দিনে বহুবার গোনা হয়, আর
       `waitingFor` ও `acceptedNoticesFor` **দুটোই** একসাথে ডাকা হয়।
       তাই সারিগুলো একবার নামিয়ে **৬০ সেকেন্ড** স্মৃতিতে রাখা হয় — দুটো
       তালিকাই ওই একই সারি থেকে ছেঁকে বার করা হয়।
       ⛔ ফলাফল এক অক্ষরও বদলায় না, শুধু অকারণ পড়াটা বাদ যায়। */
    private var cacheRows: org.json.JSONArray? = null
    private var cacheAt = 0L

    private fun activeRows(): org.json.JSONArray {
        val now = android.os.SystemClock.elapsedRealtime()
        val c = cacheRows
        if (c != null && now - cacheAt < 60_000L) return c
        val rows = try {
            SupabaseClient.fetchList(TABLE, "active=eq.true", 300, order = "createdAt.desc")
        } catch (_: Throwable) { org.json.JSONArray() }
        cacheRows = rows; cacheAt = now
        return rows
    }

    fun waitingFor(user: NativeUser?): List<JSONObject> {
        val me = digits(user?.mobile ?: "")
        if (me.isEmpty()) return emptyList()
        val myBranch = (user?.branch ?: "").trim()
        val canAcceptRole = user?.role == "master" || user?.displayRole == "doctor" || user?.role == "doctor"
        return try {
            val rows = activeRows()
            val today = todayIso()
            val out = ArrayList<JSONObject>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                if (r.optString("remindDate", "") < today) continue
                if (r.optString("cancelledAt", "").isNotBlank()) continue   // 🚫 V1194
                if (r.optString("acceptedAt", "").isNotBlank()) continue
                if (listHas(r.optString("hiddenBy", ""), me)) continue
                if (digits(r.optString("byMobile", "")) == me) continue     // নিজের পাঠানো নয়
                val forM = digits(r.optString("forMobile", ""))
                val mine =
                    if (forM.isNotEmpty()) forM == me
                    else canAcceptRole && r.optString("branch", "").trim().equals(myBranch, ignoreCase = true)
                if (mine) out.add(r)
            }
            out
        } catch (_: Throwable) { emptyList() }
    }

    /** পাঠানো ব্যক্তির ঘন্টার জন্য — তাঁর পাঠানো যেগুলো Accept হয়েছে, এখনো দেখা হয়নি। */
    fun acceptedNoticesFor(user: NativeUser?): List<JSONObject> {
        val me = digits(user?.mobile ?: "")
        if (me.isEmpty()) return emptyList()
        return try {
            val rows = activeRows()
            val out = ArrayList<JSONObject>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                if (digits(r.optString("byMobile", "")) != me) continue
                if (r.optString("acceptedAt", "").isBlank()) continue
                if (listHas(r.optString("ackBy", ""), me)) continue
                out.add(r)
            }
            out
        } catch (_: Throwable) { emptyList() }
    }

    /** নাম বা নম্বরের টুকরো ধরে রোগী খোঁজা — টাইপ করার সঙ্গে সঙ্গে সাজেশনের জন্য। */
    fun searchPatients(q: String): org.json.JSONArray? {
        val t = q.trim()
        if (t.length < 3) return null
        val digitsOnly = t.filter { it.isDigit() }
        val enc = java.net.URLEncoder.encode("*$t*", "UTF-8")
        val filter = if (digitsOnly.length >= 4) "mobile=like.*$digitsOnly*" else "name=ilike.$enc"
        return try {
            SupabaseClient.fetchListSlimOrNull(
                "patients", filter, 8, "id,name,mobile,branch,disease", order = "name.asc"
            )
        } catch (_: Throwable) { null }
    }

    /** এই সারিটা এই ব্যবহারকারী Accept করতে পারেন কিনা (যাঁকে পাঠানো হয়েছে)। */
    fun canAccept(row: JSONObject, user: NativeUser?): Boolean {
        if (row.optString("acceptedAt", "").isNotBlank()) return false
        val me = digits(user?.mobile ?: "")
        if (me.isEmpty()) return false
        val forM = digits(row.optString("forMobile", ""))
        // নির্দিষ্ট একজনকে পাঠানো হলে **কেবল তিনিই** মেনে নিতে পারেন
        if (forM.isNotEmpty()) return forM == me
        /* "সব ডাক্তার"-কে পাঠানো হলে ওই ব্রাঞ্চের যেকোনো ডাক্তার মেনে নিতে
           পারেন; মাস্টার সব ব্রাঞ্চেই পারেন। ⛔ স্টাফ কখনো Accept করতে পারেন না —
           তাহলে "কেউ অস্বীকার করতে পারবে না" নিয়মটাই ভেঙে যেত (TK-নির্দেশ)। */
        if (user?.role == "master") return true
        if (user?.role != "doctor") return false
        return row.optString("branch", "").trim()
            .equals((user.branch).trim(), ignoreCase = true)
    }
}
