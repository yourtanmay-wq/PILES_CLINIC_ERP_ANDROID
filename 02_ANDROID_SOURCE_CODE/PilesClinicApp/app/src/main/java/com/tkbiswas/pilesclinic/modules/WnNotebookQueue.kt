package com.tkbiswas.pilesclinic.modules

import android.content.Context
import org.json.JSONObject

/**
 * 🔵 TK-ORDER (07.08.2026): Work Notebook-এর দিন-সারি (notebook_days) কখনো যেন
 * হারিয়ে না যায় — এমনকি সেভ কোনো কারণে সঙ্গে সঙ্গে না বসলেও।
 *
 * এটা একটা ছোট, স্থায়ী "জমা খাতা" (ফোনের নিজের SharedPreferences):
 *   · সেভ ব্যর্থ হলে ওই দিনের পুরো `day` (IN TIME/OUT TIME/নোট সহ, ঠিক যে সময়
 *     চাপা হয়েছিল সেই সময় নিয়ে) এখানে জমা থাকে — প্রতি (staff_code, work_date)-এর
 *     জন্য একটাই, সবসময় সর্বশেষটা।
 *   · অ্যাপ/Work Notebook আবার খুললে জমা থাকা দিনগুলো নিজে থেকেই ক্লাউডে বসিয়ে
 *     দেওয়া হয় (flushPendingNotebook)। জমা যেহেতু আগেই ধরা সময় নিয়ে বসে,
 *     তাই WhatsApp-এ যাওয়া সময় আর অ্যাপের সময় সবসময় এক থাকে।
 *   · সফল হলে জমা কপিটা মুছে ফেলা হয়।
 *
 * ⛔ কিছুই স্থায়ীভাবে ভাঙে না — এটা শুধু একটা নিরাপত্তা-জাল। ব্যর্থ না হলে
 *    (স্বাভাবিক ক্ষেত্রে) এটা ব্যবহারই হয় না।
 */
object WnNotebookQueue {

    private const val PREF = "wn_notebook_pending"

    private fun keyOf(staffCode: String, workDate: String): String = "$staffCode|$workDate"

    /** সেভ ব্যর্থ হলে ওই দিনের সারি জমা রাখা হয় (প্রতি দিন একটাই — সর্বশেষটা)। */
    fun enqueue(ctx: Context, row: JSONObject) {
        try {
            val sc = row.optString("staff_code", "")
            val wd = row.optString("work_date", "")
            if (sc.isBlank() || wd.isBlank()) return
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(keyOf(sc, wd), row.toString()).apply()
        } catch (_: Throwable) { }
    }

    /** সফলভাবে বসে গেলে জমা কপিটা সরিয়ে ফেলা হয়। */
    fun remove(ctx: Context, row: JSONObject) {
        try {
            val sc = row.optString("staff_code", "")
            val wd = row.optString("work_date", "")
            if (sc.isBlank() || wd.isBlank()) return
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .remove(keyOf(sc, wd)).apply()
        } catch (_: Throwable) { }
    }

    /** জমা থাকা সব দিন (ক্লাউডে বসানোর জন্য)। */
    fun pending(ctx: Context): List<JSONObject> {
        return try {
            val all = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).all
            all.values.mapNotNull { v ->
                try { JSONObject(v as String) } catch (_: Throwable) { null }
            }
        } catch (_: Throwable) { emptyList() }
    }
}
