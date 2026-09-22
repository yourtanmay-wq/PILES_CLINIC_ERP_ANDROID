package com.tkbiswas.pilesclinic.native

import org.json.JSONArray

/**
 * 🔒🔒 খাতার সারি B110 — **বাতিল হওয়া রেকর্ডের টাকা হিসাবে ধরা হবে না**
 *
 * TK-এর নিয়ম (29.07.2026 বিকেল ৪.৫০, স্থায়ী):
 * *"মনে করুন রিজেক্ট করেছে, বা ডিলিট করেছে, অথবা রেজিস্ট্রেশন ক্যানসেল করেছে —
 *  সেই ক্ষেত্রে যেন টাকার পরিমাণও না শো করে। এটা করেছে মানে পেশেন্টকে তো টাকাটা
 *  ফেরত দিতে হয়েছে। এখানে আমাকে কেন দেখাবে? তাহলে তো হিসাব মিলবে না দিনের শেষে।"*
 *
 * TK-এর দুটো সিদ্ধান্ত (তিনি নিজে বেছে দিয়েছেন):
 *   ১. টাকার সারি **মোছা যাবে না** — থাকবে, শুধু হিসাবে ধরা হবে না।
 *   ২. নিয়মটা **পুরনো দিনেও** চলবে।
 *
 * ### কাকে "ফেরত দেওয়া" ধরা হয় (TK-এর সিদ্ধান্ত, 29.07.2026 বিকেল ৫.২০)
 * যে মোবাইলের **অন্তত একটা `Cancelled` সারি আছে**, আর **`Cancelled` ছাড়া অন্য
 * কোনো সারিই নেই**।
 *
 * | কী হয়েছিল | সারির অবস্থা | টাকা |
 * |---|---|---|
 * | Enquiry **Reject** / Registration **Cancel** | `Cancelled` | ❌ হিসাবে ধরা হবে না |
 * | **Delete** | ডিলিটের সময় ওই নম্বরের **সব** সারি `Cancelled` হয় | ❌ হিসাবে ধরা হবে না |
 * | Treatment **Incomplete** | `Incomplete` | ✅ **আগের মতোই গোনা হবে** |
 * | Reject-এর পরে আবার রেজিস্টার | `Active` সারি ফিরে আসে | ✅ **আগের মতোই গোনা হবে** |
 *
 * 🔒 **TK-এর সিদ্ধান্ত (29.07.2026 বিকেল ৫.২০):** *"Treatment রোগীকে Incomplete
 * করলে টাকা আগের মতোই গোনা হবে"* — কারণ চিকিৎসা অসম্পূর্ণ হলেও **টাকা ফেরত
 * দেওয়া হয় না**, ওটা ক্লিনিকেই থাকে। *"Delete করলে সব টাকা বাদ যাবে।"*
 *
 * ⛔ **`Cancelled` ছাড়া যে কোনো একটা সারি থাকলেই টাকা গোনা হয়** — Active হোক বা
 *    Incomplete। এটাই এই নিয়মের সবচেয়ে বড় সুরক্ষা: ভুল করে কারও আসল টাকা
 *    কখনো হিসাব থেকে হারাবে না।
 * ⛔ **কোনো টাকার সারি মোছা হয় না** — Payment History-তে সব আগের মতোই থাকে।
 * ⛔ হিসাবটা প্রতিবার সারির অবস্থা দেখে হয়, তাই **কোনো SQL বা এককালীন সংশোধন
 *    লাগে না**, আর পুরনো দিনেও নিয়মটা আপনা থেকেই চলে।
 * ⛔ খোঁজা ব্যর্থ হলে তালিকা **ফাঁকা** ফেরে — মানে কারও টাকা বাদ যায় না,
 *    সব আগের মতোই গোনা হয়। ভুল করে টাকা লুকিয়ে ফেলার চেয়ে সেটাই নিরাপদ।
 */
object RefundedRecords {

    /** যে সারিগুলো **আগেই নামানো হয়েছে** তা থেকে হিসাব — বাড়তি কোনো ক্লাউড-কল নেই।
     *  চেম্বার বোর্ড এটাই ব্যবহার করে। */
    fun fromRows(followUpRows: JSONArray): HashSet<String> {
        // `keep` = এমন সারি যার জন্য টাকা গোনা **চালু থাকবে** (Active · Incomplete ·
        // অন্য যা কিছু)। শুধু `Cancelled`-ই টাকা বাদ দেওয়ার চিহ্ন।
        val keep = HashSet<String>()
        val cancelled = HashSet<String>()
        for (i in 0 until followUpRows.length()) {
            val r = followUpRows.optJSONObject(i) ?: continue
            val m = r.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
            if (m.length != 10) continue
            if (r.s("status").ifBlank { "Active" } == "Cancelled") cancelled.add(m) else keep.add(m)
        }
        cancelled.removeAll(keep)
        return cancelled
    }

    /**
     * নিজে থেকে একবার পড়ে হিসাব — Today's Collection-এর মতো যে পর্দায়
     * `followups` আগে থেকে নামানো থাকে না।
     *
     * ⛔ **মাত্র একটাই অনুরোধ**, আর তাতে শুধু তিনটে ঘর নামে (`id,mobile,status`)।
     * ⛔ ব্যর্থ হলে **ফাঁকা** ফেরে — তখন কারও টাকা বাদ যায় না।
     */
    fun fetch(branchFilter: String?): HashSet<String> {
        /* 🔴🔒 V1361 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট, TK-বাছাই "ক") — এই একই
           নিয়ম (নিচের পুরনো কোড) এখন সার্ভারে (`tk_refunded_mobiles`) একবারে হয়ে
           ছোট একটা মোবাইল-তালিকা ফেরে — Today's Collection/Chamber Board/Income-
           Expense আর প্রতিবার ব্রাঞ্চের পুরো followups+patients নামায় না।
           ⛔ RPC ব্যর্থ হলে (পুরনো ডেটাবেসে ফাংশনটা এখনো না বসানো থাকলেও) নিচের
              প্রমাণিত ভারী-পথই চলে — টাকার হিসাব কখনো পাল্টায় না, শুধু গতি। */
        SupabaseClient.refundedMobilesRpc(branchFilter)?.let { return it }
        return try {
            val filter = if (branchFilter != null && branchFilter != "All")
                "branch=eq." + java.net.URLEncoder.encode(branchFilter, "UTF-8") else null
            // 🔵 stage-ও নামানো হয় — নিচে "registration নিজেই Cancel হয়েছে কি না" চেনার জন্য।
            val rows = SupabaseClient.fetchListSlimOrNull("followups", filter, 5000, "id,mobile,status,stage")
                ?: return HashSet()
            val base = fromRows(rows)
            if (base.isEmpty()) return base

            // 🔵🔒 B621 (11.08.2026, TK-নির্দেশ "খুব সাবধানে" · TK-র নিজের নীতি: "আসল টাকা
            // কখনো লুকোবে না")। সমস্যা: যে নম্বর আগে Enquiry-তে Reject হয়েছিল, পরে আবার
            // **রেজিস্টার + আসল পেমেন্ট** করলেও — যদি ওই active followup সারি ক্লাউডে কোনো
            // কারণে না থাকে — base নিয়ম নম্বরটাকে "শুধু Cancelled" ধরে টাকা লুকিয়ে দিত
            // (লাইভ প্রমাণ: PP · JPE-11082026-001 · 1234567890)। সমাধান: base থেকে সেই
            // নম্বরগুলো বাদ দিই যাদের **আসল রেজিস্টার্ড রোগী আছে (patients টেবিল) এবং
            // registration নিজে Cancel হয়নি**। তাই —
            //   • খাঁটি Enquiry Reject (patients-এ নেই) → আগের মতোই লুকোবে ✅
            //   • Registration Cancel / Visit Reject (Patient-stage সারি Cancelled) → লুকোবে ✅
            //   • Delete (patients থেকে মুছে গেছে) → লুকোবে ✅
            //   • Reject-এর পরে আবার Register (PP) → আর লুকোবে না, টাকা গোনা হবে ✅
            // ⛔ এতে হিসাব শুধু **বেশি** টাকা দেখাতে পারে (লুকোনো আসল টাকা ফেরে) — কখনো
            //    বাড়তি লুকোয় না, তাই ঝুঁকিহীন। patients-খোঁজা ব্যর্থ হলে base অটুট থাকে।
            val cancelledRegistration = HashSet<String>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val m = r.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
                if (m.length != 10) continue
                val st = r.s("status").ifBlank { "Active" }
                val stg = r.s("stage")
                if (st == "Cancelled" && (stg == "Patient" || stg == "Treatment" || stg == "Visit"))
                    cancelledRegistration.add(m)
            }
            val registered = HashSet<String>()
            try {
                val pats = SupabaseClient.fetchListSlimOrNull("patients", filter, 5000, "id,mobile")
                if (pats != null) for (i in 0 until pats.length()) {
                    val m = pats.getJSONObject(i).optString("mobile", "").filter { it.isDigit() }.takeLast(10)
                    if (m.length == 10) registered.add(m)
                }
            } catch (_: Throwable) { }
            for (m in registered) if (m !in cancelledRegistration) base.remove(m)
            base
        } catch (_: Throwable) {
            HashSet()
        }
    }

    /**
     * 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — "Fees Return" করা রোগী Chamber
     * Date থেকে **সম্পূর্ণ বাদ** যাবে (উপরের `Cancelled`-এর মতো শুধু টাকা
     * লুকানো না, পুরো সারিই)। ⛔ সম্পূর্ণ **আলাদা** ফাংশন — উপরের
     * `fetch()`/`fromRows()`-এর প্রমাণিত "Cancelled" নিয়ম এক অক্ষরও
     * ছোঁয়া হয়নি, নতুন এই status ("Returned") পুরনো কোনো ডেটার সাথে
     * মেশে না। ব্যর্থ হলে খালি সেট — নিরাপদ দিক (কারো সারি ভুলবশত বাদ যায় না)।
     */
    fun fetchReturnedVisits(branchFilter: String?): HashSet<String> {
        return try {
            val filter = if (branchFilter != null && branchFilter != "All")
                "branch=eq." + java.net.URLEncoder.encode(branchFilter, "UTF-8") else null
            val rows = SupabaseClient.fetchListSlimOrNull("followups", filter, 5000, "id,mobile,status,stage")
                ?: return HashSet()
            val out = HashSet<String>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val m = r.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
                if (m.length != 10) continue
                if (r.s("status").equals("Returned", ignoreCase = true) && r.s("stage") == "Patient") out.add(m)
            }
            out
        } catch (_: Throwable) {
            HashSet()
        }
    }
}
