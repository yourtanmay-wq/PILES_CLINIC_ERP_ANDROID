package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject

/**
 * 🟢🔒 V1144 (০৬.০৯.২০২৬, TK-অনুমোদিত) — রিমাইন্ডারের ক্লাউড-কাজ।
 *
 * ⛔ প্রজেক্টের প্রমাণিত পথেই (`SupabaseClient`) — নতুন কোনো নেটওয়ার্ক-ব্যবস্থা নয়।
 * ⛔ সরু পড়া: শুধু দরকারি ঘরগুলো, আর সংখ্যা গুনতে **মাত্র ৩টা ঘর** — Free
 *    plan-এ বাড়তি খরচ যতটা সম্ভব কম (V509-এর নিয়ম)।
 */
object ReminderRepository {

    private fun nowIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())

    private fun q(v: String): String = try {
        java.net.URLEncoder.encode(v, "UTF-8")
    } catch (_: Exception) { v }

    /** যাঁর কাছে এসেছে — তাঁর তালিকা (নতুনটা আগে)। */
    fun forMe(code: String, limit: Int = 60): List<ReminderItem> {
        if (code.isBlank()) return emptyList()
        val rows = SupabaseClient.fetchListOrNull(
            ReminderModel.TABLE, "toCode=eq.${q(code)}", limit,
            "remindOn.desc.nullslast", ReminderModel.COLS
        ) ?: return emptyList()
        return toList(rows)
    }

    /** যিনি পাঠিয়েছেন — তাঁর পাঠানো তালিকা। */
    fun sentByMe(code: String, limit: Int = 60): List<ReminderItem> {
        if (code.isBlank()) return emptyList()
        val rows = SupabaseClient.fetchListOrNull(
            ReminderModel.TABLE, "fromCode=eq.${q(code)}", limit,
            "remindOn.desc.nullslast", ReminderModel.COLS
        ) ?: return emptyList()
        return toList(rows)
    }

    /** এক রোগীর (মোবাইল ধরে) আগের সব রিমাইন্ডার — History-র জন্য। */
    fun historyFor(mobile: String, limit: Int = 30): List<ReminderItem> {
        val m = StaffDirectory.normalizeMobile(mobile)
        if (m.length != 10) return emptyList()
        val rows = SupabaseClient.fetchListOrNull(
            ReminderModel.TABLE, "patientMobile=eq.$m", limit,
            "remindOn.desc.nullslast", ReminderModel.COLS
        ) ?: return emptyList()
        return toList(rows)
    }

    /**
     * হোম পেজের সংখ্যা — যাঁর কাছে এসেছে অথচ এখনো **Done হয়নি** এমন কটা আছে।
     * ⛔ মাত্র তিনটে ঘর নামে, তাই এটা অ্যাপের সবচেয়ে সরু পড়াগুলোর একটা।
     */
    fun openCountFor(code: String): Int {
        if (code.isBlank()) return 0
        val rows = SupabaseClient.fetchListOrNull(
            ReminderModel.TABLE,
            "toCode=eq.${q(code)}&status=neq.${ReminderModel.ST_DONE}", 60,
            "remindOn.desc.nullslast", "id,patientName,branch"
        ) ?: return 0
        return rows.length()
    }

    /** হোম পেজের কার্ডে যে এক লাইন দেখা যায় ("Jalpaiguri · NAME — …")। */
    fun topLineFor(code: String): String {
        if (code.isBlank()) return ""
        val rows = SupabaseClient.fetchListOrNull(
            ReminderModel.TABLE,
            "toCode=eq.${q(code)}&status=neq.${ReminderModel.ST_DONE}", 1,
            "remindOn.desc.nullslast", "id,patientName,branch,type"
        ) ?: return ""
        if (rows.length() == 0) return ""
        val o = rows.optJSONObject(0) ?: return ""
        val br = o.optString("branch", "").trim()
        val nm = o.optString("patientName", "").trim()
        val ty = o.optString("type", "").trim()
        val parts = ArrayList<String>()
        if (br.isNotBlank()) parts.add(br)
        if (nm.isNotBlank()) parts.add(nm)
        val head = parts.joinToString(" · ")
        return if (ty.isNotBlank() && head.isNotBlank()) "$head — $ty" else head
    }

    /** নতুন রিমাইন্ডার পাঠানো। সফল হলে true। */
    fun send(
        branch: String, patientName: String, patientMobile: String, disease: String,
        type: String, details: String, remindOn: String,
        toCode: String, toName: String, fromCode: String, fromName: String
    ): Boolean {
        val now = nowIso()
        val row = JSONObject()
            .put("id", "rem_" + System.currentTimeMillis() + "_" +
                (100..999).random())
            .put("branch", branch)
            .put("patientName", patientName)
            .put("patientMobile", StaffDirectory.normalizeMobile(patientMobile))
            .put("disease", disease)
            .put("type", type)
            .put("details", details)
            .put("remindOn", remindOn)
            .put("toCode", toCode)
            .put("toName", toName)
            .put("fromCode", fromCode)
            .put("fromName", fromName)
            .put("status", ReminderModel.ST_SENT)
            .put("sentAt", now)
            .put("createdBy", fromCode)
            .put("createdAt", now)
            .put("updatedAt", now)
        return SupabaseClient.upsert(ReminderModel.TABLE, row)
    }

    /**
     * অবস্থা এগিয়ে দেওয়া — seen · accepted · done।
     * ⛔ পিছনে ফেরে না: Accepted-কে আর "Seen" করা যায় না, Done-কে আর কিছুই নয়।
     */
    fun advance(item: ReminderItem, to: String): Boolean {
        val order = listOf(
            ReminderModel.ST_SENT, ReminderModel.ST_SEEN,
            ReminderModel.ST_ACCEPTED, ReminderModel.ST_DONE
        )
        val cur = order.indexOf(item.status).let { if (it < 0) 0 else it }
        val next = order.indexOf(to)
        if (next <= cur) return false
        val now = nowIso()
        val f = JSONObject().put("status", to).put("updatedAt", now)
        when (to) {
            ReminderModel.ST_SEEN -> if (item.seenAt.isBlank()) f.put("seenAt", now)
            ReminderModel.ST_ACCEPTED -> {
                if (item.seenAt.isBlank()) f.put("seenAt", now)
                f.put("acceptedAt", now)
            }
            ReminderModel.ST_DONE -> {
                if (item.seenAt.isBlank()) f.put("seenAt", now)
                if (item.acceptedAt.isBlank()) f.put("acceptedAt", now)
                f.put("doneAt", now)
            }
        }
        return SupabaseClient.updateById(ReminderModel.TABLE, item.id, f)
    }

    private fun toList(rows: JSONArray): List<ReminderItem> {
        val out = ArrayList<ReminderItem>()
        for (i in 0 until rows.length()) {
            val o = rows.optJSONObject(i) ?: continue
            out.add(ReminderModel.from(o))
        }
        return out
    }
}
