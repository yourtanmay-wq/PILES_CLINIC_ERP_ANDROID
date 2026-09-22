package com.tkbiswas.pilesclinic.native

import android.content.Context

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Native rebuild Step 4 -- Follow-up.
 *
 * Fetches the same three stages the WebView's Follow-up tabs show
 * (Inquiry/Patient/Treatment -- labeled Enquiry/Visit/Patient in the UI,
 * same slightly confusing internal-vs-display naming the WebView already
 * has, kept identical rather than "fixed" without being asked), and
 * supports the two most common follow-up actions: updating the Last Remark
 * and setting the Next Follow-up Date.
 */
class FollowUpRepository(private val context: Context? = null) {

    companion object {
        // HISTORY (read this before touching the column lists below):
        // On 2026-07-27 these reads were first narrowed using a list taken
        // from this project's own 04_SUPABASE_DATABASE_SETUP files, and that
        // had to be PULLED BACK the same day -- those files do not match the
        // live table (columns were added over time), and a column that exists
        // live but is missing from the list silently comes back BLANK. A blank
        // "branch" or "patientId" can make a just-registered patient fail the
        // branch check and vanish from the Visit tab.
        //
        // WHAT CHANGED: TK ran the live check on his own database the same
        // day (information_schema.columns) and gave the real column list, so
        // the lists below are LIVE-VERIFIED, not guessed. The live followups
        // table had 5 columns the setup files did not know about (age,
        // convertedPatientId, lastCallDate, patientId, sex) and patients had
        // one (timeType) -- all of them are included below.
        //
        // THE RULE STAYS: never hand-write these lists from the setup files.
        // If the live table gains a column that this code must read, run
        //     select column_name from information_schema.columns
        //     where table_name in ('followups','patients');
        // on the LIVE Supabase and add it here.

        // TK-APPROVED (2026-07-27) — TK ran the live check himself, so these
        // lists are built from the LIVE database, not from the old setup files.
        // That was the exact condition written into this file on 2026-07-27
        // after the first attempt had to be pulled back.
        //
        // WHAT THESE ARE: every column that really exists live on this table,
        // MINUS "photo" only. The patient photo is stored inside the row as
        // text, so asking for "*" downloaded every patient's photo on every
        // list load -- the single heaviest thing on TK's 0.16 KB/s line.
        //
        // WHY IT IS SAFE NOW:
        //  - no column the code reads is left out (only "photo" is), so the
        //    branch check can never see a blank branch/patientId and hide a
        //    just-registered patient (the 2026-07-27 danger);
        //  - NO card on this screen shows a photo any more (TK removed the
        //    left column from the Visit and Patient cards on 27.07.2026, and
        //    the Enquiry card shows the call signal there), and View All reads
        //    the photo from its own separate query, which is untouched;
        //  - if a narrowed read fails for ANY reason, the same read is done
        //    again immediately with "*" (see slimFollowups/slimPatients), so
        //    the worst case is exactly today's behaviour.
        //
        // If a column is ever ADDED to the live table, nothing breaks: the app
        // simply does not read it here, the same as before it existed.
        private const val FOLLOWUP_COLS = "address,age,branch,callCount,convertedPatientId,createdAt,createdBy,date,disease,history,id,lastCallDate,lastRemark,mobile,name,nextFollow,noMoreCalls,patientId,refId,registrationDate,sex,stage,status,timeType,updatedAt,visitDate"
        // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — Supabase Egress কমানো (মাসে ১০ GB > ৫ GB
        // ফ্রি সীমা = ২০৫%)। Follow-up তালিকা প্রতিবার খুললে সব রোগীর এই ৫টা বড়
        // লেখা-ঘরও নামত, অথচ তালিকায় এগুলো দেখানোই হয় না; শুধু ডাক্তার-চেকআপ/
        // টাইমলাইন পর্দায় লাগে, আর ওরা নিজে `fetchList("patients")` (select=*) দিয়ে
        // পুরো row আলাদা করে টেনে আনে — তাই এখানে বাদ দিলে কিছু ভাঙে না, শুধু
        // ডেটা কম নামে। যাচাই: এই ৫টা ঘর FollowUp-এর কোনো ফাইলে পড়া হয় না
        // (আগের ছবি-বাদের মতোই নিরাপদ কৌশল)। বাদ: doctorFullNote · doctorAdvice ·
        // medicalHistory · previousTreatment · previousResult।
        private const val PATIENT_COLS = "address,age,bill,branch,complaint,completeApprovedBy,completeRequestedBy,createdAt,createdBy,date,decision,diagnosis,discount,disease,doctorComplete,id,mobile,name,occupation,patientId,previousCost,queue,queuedAt,refBy,refDoctor,refDoctorMobile,refundRestoredBy,registeredBy,registrationDate,sex,sinceWhen,stage,timeType,treatmentDuration,updatedAt,visitDate"

        /* 📉🔒 V1259 (০৯.০৯.২০২৬, TK-অনুমোদিত Egress-কাজ · খাতার সারি ৩৮০) —
           **Follow-up তালিকার `payments` পড়ায় ২২টা ঘরের বদলে ৭টা।**

           TK: *"কোন ভাল কাজ যেন খারাপ না হয়, সঠিকভাবে সততার সাথে গভীরে যাচাই
           করে কাজটা করতে হবে"*।

           🔬 **যাচাই (কোড ধরে, আন্দাজ নয়) — এই টেবিলের সারিগুলো এই ফাইলে
              ঠিক দুই জায়গায় ব্যবহার হয়, আর কোথাও নয়:**
           ① `paymentsForHigher` লুপ — পড়ে `payType` · `amount` · `mobile`
           ② টাকার মূল লুপ (`paidByPid` / `paidByMobileFallback`) — পড়ে
              `patientId` · `payType` · `amount` · `mobile`, আর
              `PaymentModel.isRefundRow()` / `isApprovedRefund()` পড়ে
              `payType` · `refundApprovalStatus`।
           ⇒ মোট **৫টা ঘর**।

           🛡️ **তার সঙ্গে দুটো ঘর ইচ্ছে করে রাখা হলো (নইলে ভালো কাজ খারাপ হত):**
           · `id` — `deltaUpsertOnlyOrNull()` সারিগুলো **`id` ধরে** মেলায়;
             `id` না থাকলে ওখানে সারি বাদ পড়ে যেত ⇒ টাকার হিসাব কম দেখাত।
           · `updatedAt` — পড়ার সাজানোর ক্রম (`updatedAt.desc.nullslast`)।

           ⛔ **একটাও সারি কমে না** — শুধু প্রতিটা সারির অব্যবহৃত ঘরগুলো
              (`dailyEvents` · `progress` · `remarks` · `payLabel` ·
              `paymentLabel` · `cashAmount` · `onlineAmount` · `mode` ·
              `patientCode` · `branch` · `name` · `date` · `receivedBy` ·
              `createdBy` · `createdAt`) আর নামে না ⇒ Paid · Due · Bill ·
              কার্ড · সংখ্যা — সব এক চুলও বদলায় না।
           ⛔ তিন জায়গাতেই **হুবহু এই একই তালিকা** বসাতে হবে — দুটো পড়া একই
              জমানো-চাবি (`followup:payments:<ব্রাঞ্চ>`) ব্যবহার করে, আলাদা
              তালিকা দিলে কে আগে ভরল তার উপর ফল নির্ভর করত।
           ⛔ ঘরের নাম ভুল হলে `fetchListSlimOrNull` নিজেই চওড়া পড়ায় ফিরে যায় —
              তখন বাড়তি ঘর আসে, কম নয়; তাই কিছু ভাঙার পথ নেই।
           ⛔ Chamber বোর্ড · টাকার পর্দা · রিপোর্ট — এরা `PAYMENT_COLS_LIST`
              আগের মতোই ব্যবহার করে, ছোঁয়া হয়নি। */
        private const val PAYMENT_COLS_FOLLOWUP =
            "id,patientId,mobile,amount,payType,refundApprovalStatus,updatedAt"

        /**
         * Narrowed read, with a one-time full-row safety net.
         *
         * If the narrowed read works even once, the column list is proven good
         * for this device, and after that a failure can only be the network --
         * so we do NOT spend a second request on a slow line. Only while the
         * list is still unproven does a failure trigger one retry with "*",
         * which is what protects against a wrong column name.
         */
        @Volatile private var narrowProven = false

        private fun slim(table: String, filter: String?, cols: String): JSONArray? {
            val narrow = SupabaseClient.fetchListOrNull(table, filter, 5000, select = cols)
            if (narrow != null) { narrowProven = true; return narrow }
            if (narrowProven) return null
            /* 🔴🔒 V800 (২৮.০৮.২০২৬) — TK: "আরো যাচাই করুন egress-এর ঝুঁকি আছে কিনা"।
               ─── যা ধরা পড়ল ────────────────────────────────────────────────────
               এই `slim()` ডাকা হয় **followups ও patients**-এর জন্য (নিচে দেখুন) —
               দুটোতেই রোগীর base64 ছবি আছে। সরু পড়াটা প্রথমবারেই ব্যর্থ হলে
               (দুর্বল নেট = খুব সাধারণ ব্যাপার) সোজা `select=*` × ৫০০০ সারি চলত।
               ঠিক এই একই দোষ trash-এ V798-এ সারানো হয়েছে (খাতার নিয়ম ৬.২ —
               "একটা দোষ পেলে পুরো প্রজেক্টে একই ধরনের সব জায়গা ঠিক করা")।
               ─── সারানো ───────────────────────────────────────────────────────
               মাঝখানে `SafeWideColumns` ধাপ — **ভারী ঘর (ছবি) ছাড়া বাকি সব ঘর**।
               এটা ঠিক সেই ধাপ যেটা `SupabaseClient.fetchListSlimOrNull()`-এ
               V493/V494-এ প্রমাণিত হয়ে বসানো আছে; এখানে ভুলে বাদ পড়েছিল।
               ⛔ শেষ ধাপের `select=*` **হুবহু আগের মতোই** রইল, তাই B446-এর
                  গ্যারান্টি ("খালি তালিকা / ₹0 কখনো দেখাবে না") অটুট। */
            val safe = SafeWideColumns.forTable(table, cols)
            if (safe != null) {
                val safeRead = SupabaseClient.fetchListOrNull(table, filter, 5000, select = safe)
                if (safeRead != null) return safeRead
            }
            return SupabaseClient.fetchListOrNull(table, filter, 5000)
        }

        private fun slimFollowups(filter: String?): JSONArray? = slim("followups", filter, FOLLOWUP_COLS)
        /* 📉🔒 V1260 (০৯.০৯.২০২৬, TK-অনুমোদিত Egress-কাজ · খাতার সারি ৩৮০) —
           **"কে কোন ধাপে আছে" মেলানোর চারটে পড়ায় ২৬ ঘরের বদলে ৫ ঘর।**

           TK: *"সত্যতা যাচাই করে গভীরে গিয়ে কাজ করবেন, কোন ভাল কাজের যেন
           ক্ষতি না হয়"*।

           🔬 **যাচাই (কোড ধরে, প্রতিটা লাইন খুলে দেখা):** Follow-up পর্দা খুললে
              তালিকার মূল পড়াটা ছাড়াও আরও কয়েকটা `followups` পড়া হয় — শুধু
              এটুকু জানার জন্য যে **কোন নম্বর ইতিমধ্যে উপরের ধাপে চলে গেছে বা
              বাতিল হয়েছে**, যাতে সেই কার্ড নিচের সেকশনে দুবার না দেখায়।
              ওই চারটে পড়ার সারি থেকে কোড **শুধু** এইগুলো পড়ে:
              · `fu:higher:…`     → `mobile` · `refId` · `patientId`
              · `fu:visitRejected` → `mobile`
              · `fu:treatmentAll`  → `mobile`
              · `fu:trIncomplete`  → `mobile`
              ⇒ আর একটাও ঘর নয় (প্রতিটা ব্যবহারের জায়গা খুঁজে মিলিয়ে দেখা)।

           ⛔ অথচ এতদিন প্রতিবার **২৬টা ঘর** নামত — তার মধ্যে `history`
              (প্রতিটা রোগীর পুরো কল-ইতিহাস) এই টেবিলের **সবচেয়ে ভারী** অংশ,
              যেটা এই চারটে পড়ায় কোনোদিন ছোঁয়াও হয় না।
           ⛔ **একটাও সারি কমে না** — একই ছাঁকনি · একই সারি · একই ফল; শুধু
              প্রতিটা সারি অনেক হালকা হয়ে আসে।
           ⛔ `id` ও `updatedAt` ইচ্ছে করে রাখা হলো (সাজানোর ক্রম ও ভবিষ্যতের
              মেলানো), যদিও এখনকার হিসাবে লাগে না।
           ⛔ **তালিকার মূল পড়াটা (`preCloud`) ছোঁয়া হয়নি** — কার্ডে "শেষ কল কে ·
              কখন" ওই `history` ঘর থেকেই আসে (TK-র স্থায়ী নিয়ম)।
           ⛔ **`fu:inqCancelled` ইচ্ছে করে বাদ রাখা হলো** — ওটা `noMoreCalls`
              ঘরটাও পড়ে, আর ওই ঘর ডেটাবেসে আছে কিনা এখান থেকে যাচাই করা যায় না
              (V1246-এর মন্তব্য দেখুন)। না থাকলে সরু পড়া ব্যর্থ হয়ে **উল্টে বেশি**
              ডেটা নামত। আন্দাজে হাত দেওয়া হয়নি।
           ⛔ সরু পড়া ব্যর্থ হলে `slim()` নিজেই চওড়া পড়ায় ফিরে যায় ⇒ তখন
              **বাড়তি** ঘর আসে, কম নয় — তাই ভাঙার পথ নেই। */
        private const val FOLLOWUP_COLS_LINK = "id,mobile,patientId,refId,updatedAt"
        private fun slimFollowupsLink(filter: String?): JSONArray? = slim("followups", filter, FOLLOWUP_COLS_LINK)
        private fun slimPatients(filter: String?): JSONArray? = slim("patients", filter, PATIENT_COLS)

        /**
         * V448 (19.08.2026) — durable Inquiry Reject/Restore decision from history.
         *
         * Some older rows were later rewritten to status=Active by a generic heal/view
         * path even though their append-only history still ended with a real Reject.
         * Status alone therefore cannot be the source of truth for those legacy rows.
         *
         * Evaluate explicit workflow decisions by their stored date/time rather than
         * by JSON array position. Ordinary free-text remarks are deliberately ignored
         * so text such as "not rejected" can never hide a card by accident.
         * A real Restore/Continue marker wins only when it is genuinely newer.
         */
        /** Parse history decision time without trusting array order.
         * V407 merged duplicate histories with jsonb_agg(distinct ...) and did not
         * ORDER that aggregate, so the array position is NOT a reliable chronology. */
        private fun inquiryDecisionMillis(h: JSONObject): Long? {
            val rawTime = h.optString("time", "").trim()
            val rawDate = h.optString("date", "").trim()
            val candidates = listOf(rawTime, rawDate).filter { it.isNotBlank() }
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd", "dd.MM.yyyy", "dd/MM/yyyy"
            )
            for (raw in candidates) {
                for (pat in patterns) {
                    try {
                        val f = java.text.SimpleDateFormat(pat, java.util.Locale.US)
                        f.isLenient = false
                        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val parsed = f.parse(raw)
                        if (parsed != null) return parsed.time
                    } catch (_: Throwable) { }
                }
            }
            return null
        }

        internal fun inquiryHistoryEndsTerminal(row: JSONObject): Boolean {
            var terminalSeen = false
            var activeSeen = false
            var newestTerminal: Long? = null
            var newestActive: Long? = null
            var v448ActiveSeen = false

            for (i in 0 until (row.optJSONArray("history")?.length() ?: 0)) {
                val h = row.optJSONArray("history")?.optJSONObject(i) ?: continue
                val hs = h.optString("status", "").trim().lowercase()
                val remark = h.s("remark").trim().lowercase()   // 🔴🔒 V696

                val activeDecision = hs in setOf("active", "restored", "continued", "continue") ||
                    remark.startsWith("restored & moved to") ||
                    remark == "restored from reject list" ||
                    remark == "continue entry" || remark == "continued" ||
                    remark == "continued (keep active)"
                val terminalDecision = hs in setOf("cancelled", "incomplete", "rejected", "closed") ||
                    remark == "rejected" || remark == "marked incomplete" ||
                    remark == "entry cancelled" || remark == "5-call limit — closed" ||
                    remark == "5-call limit - closed" || remark == "cancelled after 5 calls" ||
                    remark == "cancelled (duplicate number, never visited)" ||
                    remark == "cancelled by staff (signal 3-tap)" || remark == "deleted"

                if (!activeDecision && !terminalDecision) continue
                val whenMs = inquiryDecisionMillis(h)
                if (activeDecision) {
                    activeSeen = true
                    if (h.optString("decisionVersion", "").equals("V448", true)) v448ActiveSeen = true
                    if (whenMs != null && (newestActive == null || whenMs > newestActive!!)) newestActive = whenMs
                }
                if (terminalDecision) {
                    terminalSeen = true
                    if (whenMs != null && (newestTerminal == null || whenMs > newestTerminal!!)) newestTerminal = whenMs
                }
            }

            if (!terminalSeen) return false
            if (!activeSeen) return true
            // Never trust V407's merged array position. When both decisions have
            // usable dates, the later date/time wins. Equal/unknown chronology is
            // deliberately terminal-safe: the record stays in Reject until an
            // explicit V448 Restore writes a fresh timed Active marker.
            if (newestTerminal != null && newestActive != null) {
                return newestTerminal!! >= newestActive!!
            }
            if (newestTerminal != null && newestActive == null) return true
            // A V448 Restore/Continue marker is written only by an explicit user
            // action NOW, after all legacy history already exists. It can safely
            // supersede an old terminal entry whose date was too malformed to parse.
            if (newestTerminal == null && newestActive != null && v448ActiveSeen) return false
            return true
        }

        // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — Supabase Egress কমানো। স্টাফ শুধু নিজের
        // ব্রাঞ্চ দেখে, অথচ এতদিন সব ব্রাঞ্চের রোগী/পেমেন্ট নামত (সবচেয়ে ভারী ফেচ)।
        // নির্দিষ্ট একটা ব্রাঞ্চ হলে (স্টাফ সবসময়; master যখন একটা ব্রাঞ্চ বাছেন)
        // server থেকেই শুধু **সেই ব্রাঞ্চ + ফাঁকা-ব্রাঞ্চ** টানা হয় — ফাঁকা-ব্রাঞ্চ
        // রাখা হলো নিরাপত্তার জন্য (সদ্য-তৈরি সারি যার branch এখনো বসেনি সেটা যেন
        // না হারায়)। "All"/master-সব হলে `null` = আগের মতোই সব। ⛔ নিচের display-এর
        // branch/creator ছাঁকনি (fetchTab) একটুও বদলায়নি — দ্বিতীয় সুরক্ষা-জাল।
        // ⛔ staff branch-locked (নিজের ব্রাঞ্চ ছাড়া বাছতেই পারে না), তাই ভিন্ন-
        //    ব্রাঞ্চে নিজের সারি বানানোর সুযোগই নেই — কিছু হারানোর ঝুঁকি নেই।
        // `or=(...)` প্যাটার্ন এই প্রজেক্টেই বহু জায়গায় ব্যবহৃত (Dialer/Timeline/
        //    WorkNotebook/ChamberAttendance), তাই নিরাপদ ও প্রমাণিত।
        // 🔴🔒 V453 (20.08.2026, TK-অনুমোদিত): JPE-CRP-এর মতো একজন নির্দিষ্ট
        // স্টাফের জন্য একাধিক ব্রাঞ্চ (নিজের + অতিরিক্ত অনুমোদিত) দেখানোর
        // দরকার হতে পারে (CrossBranchStaffAccess.kt দেখুন)। তাই এখন কমা-
        // আলাদা একাধিক ব্রাঞ্চ-নাম গ্রহণ করা হয় ("Jalpaiguri,Falakata,Birpara")।
        // ⛔ একটাই ব্রাঞ্চ থাকলে (কমা নেই) আগের আচরণ **অক্ষত** — বাকি সবার
        //    জন্য এক অক্ষরও বদলায়নি।
        private fun branchScopeFilter(branchFilter: String?): String? {
            val b = branchFilter?.trim() ?: return null
            if (b.isEmpty() || b.equals("All", ignoreCase = true)) return null
            val parts = b.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size <= 1) {
                val enc = try { java.net.URLEncoder.encode(b, "UTF-8") } catch (_: Throwable) { b }
                return "or=(branch.eq.$enc,branch.is.null)"
            }
            val encParts = parts.joinToString(",") { p ->
                val enc = try { java.net.URLEncoder.encode(p, "UTF-8") } catch (_: Throwable) { p }
                "branch.eq.$enc"
            }
            return "or=($encParts,branch.is.null)"
        }
        /**
         * 📦🔒 V999 (০৩.০৯.২০২৬, TK-অনুমোদিত, মেপে নেওয়া) — **তালিকার মূল পড়াটা
         * এখন শুধু নিজের ব্রাঞ্চের সারি চায়।**
         *
         * **কেন (মাপা তথ্য, আন্দাজ নয়):** Supabase-এর লগে ২৪ ঘণ্টায় `followups`
         * একাই ১৩,৫৮৯ বার ডাকা হয় — সব ডাকের ৪০%, আর সবগুলোই ফোনের অ্যাপ থেকে।
         * অথচ প্রতিবার **পাঁচ ব্রাঞ্চের সব সারিই** নামত, দেখানো হত শুধু নিজের
         * ব্রাঞ্চেরটা (কোচবিহার ৪৮২ · কিষানগঞ্জ ৪৩০ · জলপাইগুড়ি ৩৫৫ ·
         * ফালাকাটা ৪৬ · বীরপাড়া ৬ — মোট ১৩১৯, ব্রাঞ্চ-ফাঁকা একটাও নেই)।
         *
         * **কেন নিরাপদ:** ফেরত আসা প্রতিটা সারি এমনিতেই `branchAllows()`-এর
         * ছাঁকনি পার হয় — অর্থাৎ যেগুলো এখানে বাদ পড়বে, সেগুলো পর্দায় আগেও
         * উঠত না। ছাঁকনিতে তিনটেই ধরা আছে: নিজের ব্রাঞ্চ · রোগীর কোডের
         * শুরুর তিন অক্ষর (`branchAllows`-এর দ্বিতীয় নিয়ম) · ব্রাঞ্চ ফাঁকা।
         * ⛔ মাস্টার/All-branch হলে ছাঁকনি বসেই না — আগের মতোই সব নামে।
         * ⛔ ছাঁকা পড়া ব্যর্থ হলে **হুবহু আগের পুরো পড়াটাই** চলে (নিচে দেখুন)।
         * ⛔ ইচ্ছে করেই **শুধু তালিকার মূল পড়াটায়** বসানো হলো। ধাপ-মেলানোর
         *    বাকি পড়াগুলো (`preHigher`, `preTreatmentStage` ইত্যাদি) ছোঁয়া হয়নি —
         *    ওগুলো "এই রোগী কি অন্য ধাপে চলে গেছে?" যাচাই করে, আর সেই সারি
         *    অন্য ব্রাঞ্চে থাকতে পারে; ছেঁকে দিলে একই নাম দু'বার দেখাত।
         */
        private fun followupBranchScope(branchFilter: String?): String {
            val b = branchFilter?.trim() ?: return ""
            if (b.isEmpty() || b.equals("All", ignoreCase = true)) return ""
            val parts = b.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return ""
            val clauses = ArrayList<String>()
            for (p in parts) {
                val enc = try { java.net.URLEncoder.encode(p, "UTF-8") } catch (_: Throwable) { p }
                clauses.add("branch.eq.$enc")
                val code = try { PatientIdGenerator.branchCode(p) } catch (_: Throwable) { "" }
                if (code.isNotBlank()) clauses.add("patientId.like.$code-*")
            }
            clauses.add("branch.is.null")
            return "&or=(" + clauses.joinToString(",") + ")"
        }

        private fun branchKeyPart(branchFilter: String?): String =
            branchFilter?.trim()?.takeIf { it.isNotEmpty() && !it.equals("All", ignoreCase = true) } ?: "all"

        private val LOCK = Any()
        // V450: suppress duplicate repair writes when fetchTab() is requested several
        // times for the same screen render. A failed write is already queued by the
        // normal retry mechanism, so one attempt per process is enough.
        private val legacyRejectRepairSeen = HashSet<String>()

        // TK-REPORTED (2026-07-26): one shared background worker for all
        // self-heal writes (see upsertWithHealRetry) so opening a tab never
        // waits for them. Kept inside THIS companion object . a Kotlin class
        // may only have one.
        private val healExecutor: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newSingleThreadExecutor()

        /** 🔴🔒 V1371 (১২.০৯.২০২৬, তালিকা ৪৬২-ঙ — TK-নির্দেশে গভীরে যাচাই করে):
         *  `loadCachedTab()` মেইন থ্রেডে চলে (পর্দা খোলা · ট্যাব বদল · প্রতি
         *  ২৫ সেকেন্ডের নিজে-নিজে রিফ্রেশ — সব একই পথে), আর আগে প্রতিবারই
         *  ডিস্কের পুরো JSON string নতুন করে parse করে `FollowUpItem` তালিকা
         *  বানাত — ব্রাঞ্চের বাস্তব তথ্য (মাস-বছরের রোগী) বড় হলে এটাই প্রতি
         *  ট্যাব-বদলে একটু দেরি করাত (নিয়ম ৭খ)। এখন raw JSON string অক্ষত
         *  থাকলে (ডিস্কের ক্যাশ পাল্টায়নি) আগেরবার বানানো তালিকাটাই আবার
         *  ব্যবহার হয়; না মিললে (নতুন সিঙ্ক এসেছে) স্বয়ংক্রিয়ভাবে আবার parse
         *  হয় — LocalWorkflowStore.snapshot-এর একই প্রমাণিত ধাঁচ।
         *  🔒 নিরাপদ কেন: `FollowUpItem` অপরিবর্তনীয় (`data class ... val`),
         *  আর `mergeOwnPhoneRows()` এই তালিকার একটা কপি (`ArrayList(cached)`)
         *  বানিয়ে তাতে বদল করে — মূল cached তালিকা কখনো ছোঁয়া হয় না, তাই
         *  একই object একাধিক পর্দা/instance শেয়ার করলেও ঝুঁকি নেই। */
        private val parsedTabCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, List<FollowUpItem>>>()

        // 🔴🔒 V456 (20.08.2026, TK-অনুমোদিত · ধাপ ১, শুধু Inquiry ট্যাব):
        // "শুধু বদলানো অংশটুকু নামুক" — Follow-up-এর সবচেয়ে ভারী একক-read
        // অংশ (`preCloud`, stage-এর সব followups সারি) delta করার ব্যবস্থা।
        // ⛔ নিচের সব জটিল কোড (enquiries জোড়া, branch-visibility,
        //    blank-branch repair) এই delta-র কথা জানেই না — তারা যা পায়
        //    (merge-করা preCloud) তাই নিয়ে **হুবহু আগের মতোই** কাজ করে।
        private const val FU_DELTA_PREFS = "followup_inquiry_delta_state"
        /* 📉🔒 V1258 (০৯.০৯.২০২৬, TK-অনুমোদিত Egress-প্লানের **ধাপ ৩**,
           খাতার সারি ৩৮০) — ৩০ মিনিট → **৩ ঘণ্টা**।

           এই ঘড়িটা শুধু একটা কাজেই লাগে — সময় পেরোলে delta ছেড়ে
           **পূর্ণ পড়া** (followups + patients + payments একসাথে) চালানো।
           সেই পূর্ণ পড়াটাই প্রকল্পের সবচেয়ে ভারী — তাই ব্যবধান ৬ গুণ হলে
           ওই খরচ ৬ ভাগের এক হয়।

           মাঝের সময়ে কি হারায় — মেপে দেখা (আন্দাজ নয়):
           · নতুন/বদলানো সারি → delta-তে আগের মতোই সঙ্গে সঙ্গে আসে।
           · Cancelled/Incomplete/Rejected/Closed → `deltaPreCloudOrNull()`
             নিজেই সরিয়ে দেয় (নিচে `terminal` তালিকা)।
           · ধাপ বদল (Patient → Treatment) → `fetchTab()`-এর `preHigher` পড়াটা
             **কখনোই delta নয়**, সবসময় তাজা — তাই পদোন্নতি পাওয়া সারি
             নিচের সেকশন থেকে আগের মতোই বাদ পড়ে।
           ⚠️ শুধু **সত্যিকারের ডিলিট** (Trash → Delete Forever) delta ধরতে পারে না —
             এতদিন সেটা সরতে সর্বোচ্চ ৩০ মিনিট লাগত, এখন সর্বোচ্চ ৩ ঘণ্টা।
             TK-কে এটা কাজের আগেই বলা হয়েছে (নিয়ম ৬)।
           ⛔ এই ঘড়ি শুধু **নিজে-নিজে রিফ্রেশ** পথে (`fetchTabDelta`) খাটে।
              পর্দা খোলা · Resume · ট্যাব বদল · ব্রাঞ্চ বদল — সবই আগের মতোই
              সরাসরি পূর্ণ `fetchTab()`, এক অক্ষরও বদলায়নি।
           ⛔ `DashboardActivity.BANNER_FULL_GAP_MS`-ও **একসাথে ৩ ঘণ্টা** করা হলো —
              দুটো আলাদা হলে ব্যানারের সংখ্যা আর তালিকার সংখ্যা বেমানান হত (TK-এর
              "৫৬ বনাম ৪৭"-এর সমস্যা) — তাই দুটো সবসময় এক মাপে রাখতে হবে। */
        private const val FU_FULL_REFRESH_INTERVAL_MS = 3L * 60L * 60L * 1000L   // ৩ ঘণ্টা (V1258; আগে ৩০ মিনিট)
        private const val FU_SAFETY_BACK_MS = 5_000L

        /* 🚀🔒 V1531 (১৮.০৯.২০২৬, TK-রিপোর্ট: "Visit ট্যাব থেকে Patient ট্যাবে
           যেতে সময় লেগে যাচ্ছে")। গভীরে গিয়ে যাচাই করে আসল কারণ পাওয়া গেল:
           · "Patient" ট্যাব (ভিতরের নাম "Treatment") নিজের fetchTab()-এ বাকি
             দুটো ট্যাবের চেয়ে সত্যিই বেশি কাজ করে — একটা বাড়তি নেটওয়ার্ক-কল
             (Cancelled/Incomplete বাদ দেওয়ার তালিকা) + সব patients-এর উপর
             আরেকটা লুপ (যারা টাকা দিয়েছেন কিন্তু এখনো তালিকায় ওঠেননি তাঁদের
             ধরার জন্য) — এই দুটোই ইচ্ছাকৃত, আগে TK-অনুমোদিত ফিক্স, একটা
             অক্ষরও ছোঁয়া হয়নি।
           · পর্দা খোলার পরপরই ব্যাকগ্রাউন্ডে তিনটে ট্যাবের জন্যই একবার
             fetchTab() চলে (FollowUpActivity.refreshTabCounts, খাতার সারি
             B31)। সেটা শেষ হওয়ার আগেই স্টাফ নিজে "Patient" ট্যাবে চাপলে,
             ট্যাব-বদলের নিজের fetchTab()-ও শুরু হয়ে যেত — অর্থাৎ একই ভারী
             কাজ **দুবার সমান্তরালে**, দুটোই "Patient" ট্যাবের সেই বাড়তি
             কাজটুকু নতুন করে করছে। তাই দেরিটা সবচেয়ে বেশি টের পাওয়া যেত
             ঠিক ওই ট্যাবেই, ঠিক ওই মুহূর্তে।
           ⛔ সাবধানতা: `fetchTab()`-এর ভিতরের একটাও লাইন এখানে বদলানো হয়নি —
              এটা শুধু তার **বাইরের একটা মোড়ক**। একই মুহূর্তে একই
              (stage+branch)-এর জন্য দ্বিতীয় ডাক এলে নতুন কাজ শুরু না করে
              আগেরটাই শেষ হওয়া পর্যন্ত অপেক্ষা করে সেই একই ফলাফল ফেরত দেয়।
              ফলাফল কী আসবে, cache-এ কী লেখা হবে — কিছুই বদলায়নি, শুধু কাজটা
              দুবারের বদলে একবার হয়। ব্যর্থ হলে (ব্যতিক্রম) মানচিত্র থেকে
              নিজে থেকেই সরে যায়, পরের ডাক আবার নতুন করে চেষ্টা করে —
              কোনো স্থায়ী "আটকে থাকা" ঝুঁকি নেই।
           ⛔ শুধু এই ফাইলের ভিতরের তিনটে জায়গায় (FollowUpActivity-র
              refreshTabCounts/loadTodayAllSections/loadTab) ব্যবহার করা
              হয়েছে — Dashboard/Dialer/FollowCalendar-এর নিজের পুরনো ডাক
              এক অক্ষরও ছোঁয়া হয়নি (ঝুঁকি কম রাখার জন্য, এই স্ক্রিনের বাইরে
              হাত দেওয়া হয়নি)। */
        private val fetchTabInFlight = java.util.concurrent.ConcurrentHashMap<String, Deferred<List<FollowUpItem>>>()
        // ⛔ GlobalScope নয় (kotlinx.coroutines 1.7.3-এ @DelicateCoroutinesApi —
        // opt-in ছাড়া কম্পাইলই হয় না)। নিজের ছোট্ট scope, SupervisorJob দিয়ে
        // যাতে একটা fetch ব্যর্থ হলে বাকি সমান্তরাল fetch-গুলো বাতিল না হয়।
        private val fetchTabScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

        /** [FollowUpRepository.fetchTab]-এর হুবহু একই ফল — শুধু একই মুহূর্তে
         *  একই (stage+branch)-এর জন্য দ্বিতীয় ডাক এলে আগেরটার সাথেই জোড়া
         *  লাগিয়ে দেয়, নতুন করে সেই ভারী কাজ শুরু করে না। */
        fun fetchTabDeduped(
            repo: FollowUpRepository, stage: String, branchFilter: String?,
            creatorName: String? = null, creatorMobile: String? = null
        ): Deferred<List<FollowUpItem>> {
            val key = "$stage|${branchFilter ?: ""}"
            // ConcurrentHashMap.compute একটাই চাবির জন্য একসাথে একবারই চলে
            // (thread-safe) — তাই দুটো থ্রেড একই মুহূর্তে এলেও নতুন কাজ
            // দুবার শুরু হওয়ার সুযোগ নেই।
            return fetchTabInFlight.compute(key) { _, existing ->
                if (existing != null && existing.isActive) existing
                else fetchTabScope.async {
                    try {
                        repo.fetchTab(stage, branchFilter, creatorName, creatorMobile)
                    } finally {
                        fetchTabInFlight.remove(key)
                    }
                }
            }!!
        }
    }

    // TK-REPORTED BUG FIX (2026-07-16): every write function below
    // (updateRemark, updateNextFollow, updateStatus, logEnquiryCall,
    // resetCallCount) used to do "return SupabaseClient.updateById(...) ||
    // context != null" -- meaning it ALWAYS reported success (as long as
    // this was called from a normal screen) even when the actual cloud
    // write silently failed. The local phone's own copy was correct, so
    // the staff who made the change saw it fine -- but nobody else
    // (another staff, Master, another device) ever received it, and
    // nothing ever retried it. Follow-up is the single most-used screen in
    // the app (every call, every remark, every status change goes through
    // it), so this is very likely the single biggest cause of "I updated
    // it but the other staff/Master doesn't see it" reports. Fixed the
    // same way as Enquiry/Registration/Payment (V77/V78): a small
    // pending-queue, retried from BottomNav.wire() on every screen open.
    private val pendingPrefs = context?.getSharedPreferences("piles_clinic_followup_pending", Context.MODE_PRIVATE)

    /* 🔴🔒 V1222 ① (০৮.০৯.২০২৬ — কিশানগঞ্জের স্টাফ Laxmi-র রিপোর্ট,
       TK-র অনুমতি নিয়ে): *"রিমার্ক লিখে নেক্সট ফলোআপও দিয়েছি, তবু আজকের কোনো
       আপডেট হয় নাই"*।
       **আসল দোষ:** এই ঘরটা একই সারির **আগের জমা কাজটা মুছে দিয়ে** নতুনটা বসাত।
       বাস্তব ক্রম হলো — আগে রিমার্ক সেভ (নেট দুর্বল ⇒ জমা হলো), তার ঠিক পরেই
       বাধ্যতামূলক ক্যালেন্ডারে তারিখ সেভ (ও-ও জমা হলো) ⇒ **রিমার্কটা জমা-তালিকা
       থেকেই মুছে যেত**, চিরতরে। কার্ডে তাই পুরনো রিমার্কই থেকে যেত।
       ⇒ এখন আগের জমা ঘরগুলোর সঙ্গে নতুন ঘরগুলো **জোড়া লাগানো হয়** (একই নামের
         ঘর হলে নতুনটাই থাকে — স্টাফের শেষ সিদ্ধান্তই জেতে)।
       ⛔ কোনো ঘর মোছা হয় না · পাঠানোর নিয়ম · ক্রম কিছুই বদলায়নি। */
    private fun queueFieldUpdate(id: String, fields: JSONObject) {
        val prefs = pendingPrefs ?: return
        if (id.isBlank()) return
        synchronized(LOCK) {
        val queue = loadFieldQueue()
        val next = JSONArray()
        val merged = JSONObject()
        for (i in 0 until queue.length()) {
            val e = queue.optJSONObject(i) ?: continue
            if (e.optString("id") != id) { next.put(e); continue }
            // একই সারির পুরনো জমা ঘরগুলো আগে তুলে রাখা হয়
            val old = e.optJSONObject("fields") ?: continue
            val ok = old.keys()
            while (ok.hasNext()) { val k = ok.next(); merged.put(k, old.opt(k)) }
        }
        // তারপর নতুন ঘরগুলো — একই নাম হলে নতুনটাই জেতে
        val nk = fields.keys()
        while (nk.hasNext()) { val k = nk.next(); merged.put(k, fields.opt(k)) }
        next.put(JSONObject().put("id", id).put("fields", merged))
        prefs.edit().putString("queue", next.toString()).commit()
        }
        // TK-REQUESTED (2026-07-25): sync immediately, even if the staff
        // closes the app right after saving . WorkManager does the upload
        // in the background; the same proven flushPending() work, sooner.
        context?.let { c2 -> try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(c2) } catch (_: Throwable) { } }
    }

    private fun loadFieldQueue(): JSONArray {
        val raw = pendingPrefs?.getString("queue", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    // TK-REPORTED BUG FIX (2026-07-24): the 3 self-heal writes below (Enquiry/
    // Visit/Treatment "genuinely missing everywhere" fallback) fired via a
    // bare Thread{} with no retry -- if that ONE attempt failed (weak
    // signal, screen closed mid-upload), the row was NEVER retried, so the
    // patient stayed stuck on "Registered (syncing...)" forever, exactly
    // what TK found (stuck for hours, not the intended few-seconds
    // transient state). Now uses the SAME proven pending-queue pattern as
    // everything else, retried from flushPending() (BottomNav, every screen
    // open) via upsert (not updateById -- this row may not exist yet).
    private val healPendingPrefs = context?.getSharedPreferences("piles_clinic_followup_heal_pending", Context.MODE_PRIVATE)

    /**
     * TK-REPORTED (2026-07-26): the Visit tab sat on "Loading..." for hours
     * while the Enquiry and Patient tabs opened normally.
     *
     * ROOT CAUSE: this self-heal write used to run INSIDE the tab's own load,
     * once per patient that has no followups row yet. On the Visit tab that
     * can be dozens of patients, so one tab open waited for dozens of
     * SEQUENTIAL network writes . minutes on a good line, effectively forever
     * on a weak one. The list itself was already complete before these writes;
     * they only repair the cloud for next time.
     *
     * FIX: the repair now runs in the BACKGROUND on one shared worker thread.
     * The tab shows its list immediately; the healing continues by itself and
     * still falls back to the retry queue if it fails. Nothing about WHAT is
     * written changed . same row, same table, same retry.
     */
    private fun upsertWithHealRetry(healRow: JSONObject) {
        // 12.08.2026, PP/GST live proof: a deleted patient's stale local copy
        // must never be allowed to manufacture a fresh Active follow-up later.
        // This check is self-heal-only and fail-closed; normal saves are untouched.
        if (!FollowUpHealGuard.liveSourceStillExists(healRow)) return
        try {
            healExecutor.execute {
                val ok = try { SupabaseClient.upsert("followups", healRow) } catch (_: Throwable) { false }
                if (!ok) queueHealRow(healRow)
            }
        } catch (_: Throwable) {
            // if the executor itself refuses, keep the old behaviour
            val ok = try { SupabaseClient.upsert("followups", healRow) } catch (_: Throwable) { false }
            if (!ok) queueHealRow(healRow)
        }
    }

    private fun queueHealRow(row: JSONObject) {
        val prefs = healPendingPrefs ?: return
        val id = row.optString("id")
        if (id.isBlank()) return
        synchronized(LOCK) {
            val raw = prefs.getString("queue", "[]") ?: "[]"
            val queue = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
            val next = JSONArray()
            for (i in 0 until queue.length()) {
                val e = queue.optJSONObject(i) ?: continue
                if (e.optString("id") != id) next.put(e)
            }
            next.put(row)
            prefs.edit().putString("queue", next.toString()).commit()
        }
    }

    private fun flushHealPending() {
        val prefs = healPendingPrefs ?: return
        synchronized(LOCK) {
            val raw = prefs.getString("queue", "[]") ?: "[]"
            val queue = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
            if (queue.length() == 0) return
            val stillPending = JSONArray()
            for (i in 0 until queue.length()) {
                val row = queue.optJSONObject(i) ?: continue
                // TK-REQUESTED (2026-07-26): never re-create a deleted row.
                if (DeletedGuard.isDeleted("followups", row.optString("id", ""), context)) continue
                // An id tombstone is device-local.  Re-check the live source too,
                // so a queued repair from another/older device cannot resurrect it.
                if (!FollowUpHealGuard.liveSourceStillExists(row)) continue
                val ok = try { SupabaseClient.upsert("followups", row) } catch (_: Throwable) { false }
                if (!ok) stillPending.put(row)
            }
            prefs.edit().putString("queue", stillPending.toString()).commit()
        }
    }

    /** Retries every "followups" field-update still stuck on this device.
     * Called from BottomNav.wire() on every screen open, same as Enquiry/
     * Registration/Payment. Safe to repeat -- re-writing the same fields
     * to the same id changes nothing if it already succeeded. Does
     * nothing (no network call) if nothing is pending. */
    /* 🔴🔴🔒 V1222 ④ — "যে কলটা ইতিহাসে বসানো যায়নি" তার জমা তালিকা।
       ⛔ এখানে কিছুই **মোছা** হয় না — নতুন সারি শুধু যোগ হয়। */
    private val historyPendingPrefs =
        context?.getSharedPreferences("piles_clinic_followup_history_pending", Context.MODE_PRIVATE)

    private fun queueHistoryEntry(id: String, entry: JSONObject) {
        val prefs = historyPendingPrefs ?: return
        if (id.isBlank()) return
        synchronized(LOCK) {
            val raw = prefs.getString("queue", "[]") ?: "[]"
            val q = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
            q.put(JSONObject().put("id", id).put("entry", entry))
            // সর্বোচ্চ ৫০০টা — পুরনোগুলো আগে যায়, নতুনগুলো কখনো বাদ পড়ে না
            val trimmed = JSONArray()
            val from = if (q.length() > 500) q.length() - 500 else 0
            for (i in from until q.length()) trimmed.put(q.opt(i))
            prefs.edit().putString("queue", trimmed.toString()).commit()
        }
        context?.let { c2 -> try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(c2) } catch (_: Throwable) { } }
    }

    /** একই কল দুবার বসছে কিনা — তারিখ · সময় · লেখা · স্টাফ মিলিয়ে। */
    private fun historyHasEntry(history: JSONArray, entry: JSONObject): Boolean {
        for (i in 0 until history.length()) {
            val e = history.optJSONObject(i) ?: continue
            if (e.optString("date") == entry.optString("date") &&
                e.optString("time") == entry.optString("time") &&
                e.optString("remark") == entry.optString("remark") &&
                e.optString("staff") == entry.optString("staff")) return true
        }
        return false
    }

    private fun flushHistoryPending() {
        val prefs = historyPendingPrefs ?: return
        synchronized(LOCK) {
            val raw = prefs.getString("queue", "[]") ?: "[]"
            val q = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
            if (q.length() == 0) return
            val left = JSONArray()
            for (i in 0 until q.length()) {
                val e = q.optJSONObject(i) ?: continue
                val id = e.optString("id")
                val entry = e.optJSONObject("entry") ?: continue
                if (id.isBlank()) continue
                if (DeletedGuard.isDeleted("followups", id, context)) continue   // মোছা সারিতে নয়
                val back = try { SupabaseClient.fetchListOrNull("followups", "id=eq.$id", 1, select = "id,history") } catch (_: Throwable) { null }
                if (back == null || back.length() == 0) { left.put(e); continue }  // এখনো পড়া যাচ্ছে না
                val row = back.optJSONObject(0) ?: run { left.put(e); null } ?: continue
                val history = row.optJSONArray("history") ?: JSONArray()
                if (historyHasEntry(history, entry)) continue                     // আগেই বসে গেছে
                // 🔴🔒 V1372 (তালিকা ৪৬২-চ) — এখানেও একই atomic RPC আগে চেষ্টা,
                // যাতে জমা-থাকা এই এন্ট্রি ফ্লাশ হওয়ার সময়ও অন্য ফোনের ঠিক ওই
                // মুহূর্তে লেখা এন্ট্রি চাপা না পড়ে। ব্যর্থ হলে আগের পথেই ফেরা।
                val rpcResult = SupabaseClient.appendFollowupHistory(id, entry)
                if (rpcResult != null) continue                                  // জমা থেকে বাদ, সফল
                history.put(entry)
                val fields = JSONObject().put("history", history).put("updatedAt", isoNow())
                val sent = SupabaseClient.updateById("followups", id, fields)
                if (!(sent && verifyQueuedUpdate(id, fields) == 1)) left.put(e)
            }
            prefs.edit().putString("queue", left.toString()).commit()
        }
    }

    /* 🔴🔒 V1222 ② — জমা কাজটা **সত্যিই** ক্লাউডে বসল কিনা মিলিয়ে দেখা।
       ফেরত: 1 = বসেছে (জমা থেকে বাদ) · 2 = সারিই নেই, তাই গোটা সারিটা heal
       তালিকায় পাঠানো হলো (জমা থেকে বাদ) · 0 = বসেনি, জমাই থাকুক।
       ⛔ কোনো তথ্য কখনো চুপচাপ ফেলে দেওয়া হয় না — এটাই এই সংশোধনের মূল কথা। */
    /** 🔴 V1360 — জমা ঘরটা ক্লাউডে বসেছে কি না: null/"" এক · সময়-ঘরে "Z" বনাম "+00:00"
     *  ধাঁচের তফাত উপেক্ষা (তারিখ+সময় সেকেন্ড পর্যন্ত মিললেই বসেছে) · বাকি হুবহু। */
    private fun queuedValueLanded(cloudV: Any?, sentV: Any?): Boolean {
        fun norm(v: Any?): String = when {
            v == null || v == JSONObject.NULL -> ""
            else -> v.toString().let { if (it == "null") "" else it }
        }
        val c = norm(cloudV); val s = norm(sentV)
        if (c == s) return true
        val iso = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")
        if (iso.containsMatchIn(c) && iso.containsMatchIn(s)) return c.take(19) == s.take(19)
        return false
    }

    private fun verifyQueuedUpdate(id: String, fields: JSONObject): Int {
        val back = try { SupabaseClient.fetchListOrNull("followups", "id=eq.$id", 1) } catch (_: Throwable) { null }
            ?: return 0                       // নেট খারাপ — জমাই থাকুক, পরে আবার
        if (back.length() == 0) {
            // সারিটা ক্লাউডে নেই। ফোনের নিজের কপির সঙ্গে জমা ঘরগুলো জুড়ে
            // গোটা সারিটা heal তালিকায় — DeletedGuard ও FollowUpHealGuard
            // সেখানে আগের মতোই পাহারায়, তাই ইচ্ছে করে মোছা সারি ফিরবে না।
            val ctx = context ?: return 0
            val mine = try { LocalWorkflowStore(ctx).findFollowUp(id) } catch (_: Throwable) { null }
                ?: return 0                   // ফোনেও নেই — জমা রেখে দেওয়াই নিরাপদ
            val row = JSONObject(mine.toString()).put("id", id)
            val k = fields.keys()
            while (k.hasNext()) { val key = k.next(); row.put(key, fields.opt(key)) }
            queueHealRow(row)
            return 2
        }
        val cloud = back.optJSONObject(0) ?: return 0
        var allLanded = true
        val keys = fields.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            // `updatedAt` ডেটাবেসের ট্রিগার নিজেই বদলাতে পারে — ওটা মেলানো হয় না
            if (key == "updatedAt") continue
            /* 🔴🔒 V1352 (১১.০৯.২০২৬, TK-রিপোর্ট — "হাই স্পিড নেট থাকলেও ৩টা
               Follow-up কিছুতেই Send হচ্ছে না, বারবার চেষ্টা করেও একই")।
               **আসল কারণ (কোডে মিলিয়ে পাওয়া):** `history`-ই একমাত্র জমা-ঘর
               যেটা একটা পুরো JSON তালিকা (`JSONArray`) — নিচের `.toString()`
               তুলনাটা এই ঘরে কখনো নির্ভরযোগ্য নয়, কারণ Postgres/PostgREST
               jsonb-এ পাঠানোর পর ভিতরের প্রতিটা এন্ট্রির ঘরগুলোর ক্রম বদলে
               দিতে পারে — লেখা হুবহু ঠিক বসলেও `.toString()` দুটো ভিন্ন হয়ে
               যায়, তাই চিরকাল "বসেনি" ধরা হত আর সারিটা পাঠানো-বাকি তালিকায়
               চিরতরে আটকে থাকত (আসল টাকা/রিমার্ক তবু ঠিকই ক্লাউডে বসত)।
               ⇒ `history`-র বেলায় এখন **বিষয়বস্তু মিলিয়ে** দেখা হয় (দৈর্ঘ্য +
                 শেষ এন্ট্রির remark/date/staff) — ক্রম বদলালেও ধরা পড়ে। */
            if (key == "history") {
                val sentHist = fields.optJSONArray("history")
                val cloudHist = cloud.optJSONArray("history")
                if (sentHist == null || cloudHist == null || sentHist.length() != cloudHist.length()) {
                    allLanded = false; break
                }
                val sentLast = sentHist.optJSONObject(sentHist.length() - 1)
                val cloudLast = cloudHist.optJSONObject(cloudHist.length() - 1)
                val sameLast = sentLast != null && cloudLast != null &&
                    sentLast.optString("remark") == cloudLast.optString("remark") &&
                    sentLast.optString("date") == cloudLast.optString("date") &&
                    sentLast.optString("staff") == cloudLast.optString("staff")
                if (!sameLast) { allLanded = false; break }
                continue
            }
            /* 🔴🔒 V1360 (১১.০৯.২০২৬, পুরো প্রজেক্ট যাচাইয়ে ধরা — V1352-এর `history`-র
               হুবহু একই জাতের দোষ, আরেকটা ঘরে): `lastRemarkAt` সার্ভারে timestamptz —
               আমরা পাঠাই "…T14:00:00.123Z", সার্ভার ফেরত দেয় "…T14:00:00.123+00:00"।
               লেখা হুবহু বসলেও `.toString()` তুলনা কখনো মেলে না ⇒ রিমার্কটা "পাঠানো
               বাকি" তালিকায় চিরকাল আটকে থাকত, প্রতিবার পর্দা খুললে আবার পাঠাত।
               একই ভাবে ফাঁকা ঘর: আমরা "" পাঠালে সার্ভার null ফেরাতে পারে।
               ⇒ এখন সময়-ঘর হলে প্রথম ১৯ অক্ষর (তারিখ+সময়, সেকেন্ড পর্যন্ত) মেলানো
                 হয়, আর null ও "" এক ধরা হয়। বাকি সব ঘরে আগের মতোই হুবহু মিল লাগে। */
            if (!queuedValueLanded(cloud.opt(key), fields.opt(key))) {
                allLanded = false; break
            }
        }
        if (allLanded) return 1
        // বসেনি — ক্লাউডেরটা আমাদের চেয়ে নবীন হলে অন্য কেউ পরে লিখেছেন,
        // তখন আমাদেরটা পুরনো; জোর করে বসালে তাঁর কাজ মুছে যেত।
        val ours = fields.optString("updatedAt", "")
        val theirs = cloud.optString("updatedAt", "")
        if (ours.isNotBlank() && theirs.isNotBlank() && theirs > ours) return 1
        return 0
    }

    fun flushPending() {
        flushHealPending()
        flushHistoryPending()   // 🔴 V1222 ④ — বসানো যায়নি এমন কলগুলো
        flushMobileTaskPending()
        val prefs = pendingPrefs ?: return
        synchronized(LOCK) {
        val queue = loadFieldQueue()
        if (queue.length() == 0) return
        val stillPending = JSONArray()
        for (i in 0 until queue.length()) {
            val e = queue.optJSONObject(i) ?: continue
            try {
                val id = e.optString("id")
                val fields = e.optJSONObject("fields") ?: continue
                // TK-REQUESTED (2026-07-26): if this follow-up row was deleted
                // in the meantime, drop the queued edit instead of writing it
                // back (a PATCH on a deleted row can also silently "succeed").
                if (DeletedGuard.isDeleted("followups", id, context)) continue
                /* 🔴🔒 V1222 ② (০৮.০৯.২০২৬ — Laxmi-র রিপোর্ট, TK-র অনুমতি নিয়ে)।
                   **আসল দোষ:** `updateById()` **সারি না মিললেও** (`row_not_matched`)
                   `true` ফেরায় (`outcome != 0`) — এই ফাইলের নিজের মন্তব্যেই লেখা
                   *"an update that matched no row still answers 200, so read it back
                   before believing it"*, আর সেই যাচাইটা `updateRemark()` /
                   `updateNextFollow()`-এ আছে, কিন্তু **এই পুনরায়-পাঠানোর পথে ছিল না**।
                   ⇒ জমা কাজটা "হয়ে গেছে" ধরে তালিকা থেকে মুছে ফেলা হত ⇒ স্টাফের
                     রিমার্ক ও পরের-কলের তারিখ চিরতরে হারাত, আর পরের রিফ্রেশে
                     পুরনো অবস্থা ফিরে আসত ("সকালের কলগুলো আবার দেখাচ্ছে")।
                   ⇒ এখন পাঠানোর পরে সারিটা **পড়ে মিলিয়ে** দেখা হয়:
                     · সারিই নেই ⇒ ফোনের নিজের কপি + জমা ঘরগুলো নিয়ে **heal**
                       তালিকায় পাঠানো হয় (গোটা সারিটা আবার বসে) — DeletedGuard ও
                       FollowUpHealGuard আগের মতোই পাহারায়, তাই ইচ্ছে করে মোছা সারি
                       কখনো ফিরে আসে না।
                     · সারি আছে কিন্তু ঘরগুলো বসেনি ⇒ ক্লাউডেরটা আমাদের চেয়ে নবীন
                       হলে ছেড়ে দেওয়া হয় (অন্য কেউ পরে লিখেছেন — হারানোর কিছু নেই),
                       নইলে জমা থেকেই থাকে, পরে আবার চেষ্টা হয়।
                   ⛔ বাড়তি পড়া শুধু তখনই যখন সত্যিই কিছু জমা আছে (রোজকার ব্যবহারে
                      তালিকা ফাঁকা ⇒ একটাও বাড়তি ক্লাউড-কল নেই)। */
                val sent = SupabaseClient.updateById("followups", id, fields)
                //  1 = সত্যিই বসেছে · 2 = heal-এ পাঠানো হলো · 0 = জমাই থাকুক
                val verdict = if (!sent) 0 else verifyQueuedUpdate(id, fields)
                if (verdict == 0) { stillPending.put(e); continue }
                if (verdict == 1) {
                    // TK-REPORTED BUG FIX (2026-07-16): same fix as
                    // Enquiry/Registration's flushPending() -- confirm the
                    // local cache row as SYNCED right when this retry
                    // succeeds, so LocalWorkflowStore's stale-cloud-refresh
                    // guard doesn't keep treating this record as having an
                    // un-synced local change forever.
                    context?.let { ctx ->
                        val syncRow = JSONObject(fields.toString()).put("id", id)
                        LocalWorkflowStore(ctx).upsertFollowUp(syncRow, "SYNCED")
                    }
                }
            } catch (_: Throwable) {
                stillPending.put(e)
            }
        }
        prefs.edit().putString("queue", stillPending.toString()).commit()
        }
    }

    // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on the
    // phone instantly" pattern already added to Doctor Queue -- a read-only
    // display cache of the last successfully fetched tab, saved on-device.
    // fetchTab() below (fetch/merge/branch-visibility/sort logic) is
    // completely unchanged; this only adds a save at the very end. Uses its
    // own field-name mapping (not FollowUpModel.parse(), which expects raw
    // Supabase column names like "date" rather than "recordDate") so this
    // stays fully independent of the live-fetch parsing path.
    private val CACHE_PREFS = "followup_tab_cache"

    fun loadCachedTab(stage: String, branchFilter: String?): List<FollowUpItem>? {
        val ctx = context ?: return null
        val prefs = ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        // 🔴 V448: old cached FollowUpItem objects do not contain status/history, so
        // a legacy Reject that V448 can now identify from history could flash back from
        // V447's display cache before the fresh cloud result arrived. Clear ONLY this
        // derived display cache once after the visibility-rule upgrade; no clinical row
        // or offline pending data is deleted. LocalWorkflowStore is merged immediately.
        if (prefs.getInt("_visibility_rule_version", 0) < 448) {
            prefs.edit().clear().putInt("_visibility_rule_version", 448).apply()
            return mergeOwnPhoneRows(stage, branchFilter, emptyList()).ifEmpty { null }
        }
        val key = "cache_${stage}_${branchFilter ?: "All"}"
        val json = prefs.getString(key, null)
            // ⚡ জমানো তালিকা না থাকলেও (প্রথমবার খোলা) ফোনের নিজের সেভ করা
            // রেকর্ড সঙ্গে সঙ্গে দেখাতে হবে — নইলে ধীর লাইনে পর্দা ফাঁকা থাকত।
            ?: return mergeOwnPhoneRows(stage, branchFilter, emptyList()).ifEmpty { null }
        val already = parsedTabCache[key]
        if (already != null && already.first == json) {
            return mergeOwnPhoneRows(stage, branchFilter, already.second)
        }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<FollowUpItem>()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                list.add(
                    FollowUpItem(
                        id = r.optString("id", ""), name = r.optString("name", ""), mobile = r.optString("mobile", ""),
                        branch = r.optString("branch", ""), disease = r.optString("disease", ""), stage = r.optString("stage", ""),
                        lastRemark = r.s("lastRemark"), nextFollow = r.s("nextFollow"),   // 🔴🔒 V696
                        recordDate = r.optString("recordDate", ""), callCount = r.optInt("callCount", 0),
                        createdAt = r.optString("createdAt", ""),   // 🔒 খাতার সারি B65
                        bill = r.optDouble("bill", 0.0), paid = r.optDouble("paid", 0.0),
                        patientId = r.optString("patientId", ""), address = r.optString("address", ""),
                        age = r.optString("age", ""), sex = r.optString("sex", ""),
                        photo = r.optString("photo", ""), updatedAt = r.optString("updatedAt", ""),
                        noMoreCalls = r.optBoolean("noMoreCalls", false),   // 📵 V1206
                        // 🏷️🔒 V712 — উপরের তিনটে ঘর ফেরত পড়া (পুরোনো জমানো তালিকায়
                        //    না থাকলে ফাঁকা — অর্থাৎ ঠিক আগের আচরণ, কিছুই ভাঙে না)।
                        timeType = r.optString("timeType", ""),
                        refDoctor = r.optString("refDoctor", ""),
                        addressTag = r.optString("addressTag", ""),
                        lastCallDate = r.optString("lastCallDate", ""), lastCallBy = FollowUpModel.prettyStaff(r.optString("lastCallBy", "")),
                        // ⏰🔒 V827 — পুরোনো জমানো তালিকায় ঘরটা নেই ⇒ ফাঁকা,
                        //    অর্থাৎ হুবহু আগের আচরণ; কিছুই ভাঙে না।
                        lastCallTime = r.optString("lastCallTime", ""),
                        // 🆕 V851 — উপরের `saveCachedTab`-এর জোড়া।
                        regDate = r.optString("regDate", ""),
                        regBy = r.optString("regBy", "")
                    )
                )
            }
            parsedTabCache[key] = json to list
            // ⚡ TK (28.07.2026): নিজের ফোনে করা কাজ সঙ্গে সঙ্গে দেখাতে হবে।
            mergeOwnPhoneRows(stage, branchFilter, list)
        } catch (t: Throwable) { null }
    }

    /**
     * ⚡ TK-এর নির্দেশ (28.07.2026 ২.৩০ pm, ফটো-প্রুফসহ — আজ রেজিস্টার করা রোগী
     * Visit-এ ৭–১০ মিনিট পরে আসছিল):
     *
     * **"আমি আমার ফোনে যা যা কাজ করবো, সেটা যেন সাথে সাথেই দেখায়।"**
     *
     * **আসল কারণ:** পর্দা খুললে প্রথমে **জমানো তালিকাটা** দেখানো হত, আর ওই
     * তালিকায় ফোনের নিজের নতুন রেকর্ড মেশানোই হত না। তাই ধীর লাইনে ক্লাউডের
     * উত্তর না আসা পর্যন্ত নিজের করা রেজিস্ট্রেশন চোখেই পড়ত না।
     *
     * **এখন:** জমানো তালিকা দেখানোর ঠিক আগেই ফোনের নিজের সেভ করা সারিগুলো
     * তাতে মিশিয়ে দেওয়া হয় — **তাই সেভ করার সঙ্গে সঙ্গেই তালিকায় দেখা যায়**,
     * ক্লাউডের জন্য এক সেকেন্ডও অপেক্ষা করতে হয় না।
     *
     * 🔒 এটা পুরনো লক করা নিয়মেরই সম্প্রসারণ: **"যে ফোনে রেকর্ড সেভ হয়েছে, সেই
     * ফোনের তালিকায় সেটা সব সময় থাকবে। ক্লাউড শুধু যোগ করতে পারবে, বাদ দিতে
     * পারবে না।"** এখানে কেবল **যোগ** করা হয় — জমানো তালিকার একটা সারিও বাদ
     * যায় না, কোনো সংখ্যা বদলায় না।
     *
     * ⛔ কোনো নতুন ক্লাউড-কল নেই — সবই ফোনের ভিতরের কাজ, তাই অ্যাপ ধীর হয় না।
     * ব্রাঞ্চের নিয়মও আগের মতোই মানা হয় (স্টাফ শুধু নিজের ব্রাঞ্চ দেখেন)।
     */
    private fun mergeOwnPhoneRows(
        stage: String, branchFilter: String?, cached: List<FollowUpItem>
    ): List<FollowUpItem> {
        val ctx = context ?: return cached
        return try {
            val local = LocalWorkflowStore(ctx).rowsForStage(stage)
            if (local.length() == 0) return cached
            // 🚨 TK-REPORTED (28.07.2026 ৪.৫০ pm): *"রিমার্ক লিখছি, দেখাচ্ছে হয়ে
            // গেছে, কিন্তু পরে গিয়ে দেখছি পুরনো রিমার্কই রয়ে গেছে।"*
            //
            // **আসল কারণ:** নতুন রেকর্ড যোগ করা হত, কিন্তু **যে রেকর্ড আগে থেকেই
            // জমানো তালিকায় আছে তার নতুন লেখা বসানো হত না** — তাই পুরনো রিমার্কই
            // চোখে পড়ত, যতক্ষণ না ধীর লাইনে ক্লাউডের উত্তর আসে।
            //
            // **এখন:** এই ফোনে যা লেখা হয়েছে সেটাই জেতে — পুরনো সারিটা বদলে
            // ফোনের নিজের লেখাটা বসে যায়। 🔒 এটা পুরনো লক করা নিয়মেরই অংশ:
            // "যে ফোনে রেকর্ড সেভ হয়েছে, সেই ফোনের তালিকায় সেটা সব সময় থাকবে।"
            //
            // ⚠️ টাকার ঘর (Bill/Paid) ফোনে থাকে না — তাই ওগুলো জমানো তালিকার
            // মানই রাখা হয়, কখনো ০ করে দেওয়া হয় না।
            val byId = HashMap<String, Int>()
            val out = ArrayList(cached)
            for ((idx, c) in out.withIndex()) if (c.id.isNotBlank()) byId[c.id] = idx
            val seenMobiles = HashSet<String>()
            for (c in out) {
                val d = c.mobile.filter { it.isDigit() }.takeLast(10)
                if (d.length == 10) seenMobiles.add(d)
            }
            val allBranch = branchFilter == null || branchFilter.isBlank() ||
                branchFilter.equals("All", ignoreCase = true)
            val extra = mutableListOf<FollowUpItem>()
            /* 🔴🔴🔒 V1319 (তালিকা ৪২৪, আসল কারণ): নিচের পুরনো-সারি-ভুলে-যাওয়ার কাজ এখন
               একসাথে জমিয়ে, লুপ শেষে **একবারই** — প্রতিটা সারির জন্য আলাদা করে ফোনের
               পুরো জমানো টেবিল ছোঁয়া বন্ধ (নিচের মন্তব্য দেখুন)। */
            val toForget = HashSet<String>()
            for (i in 0 until local.length()) {
                val r = local.optJSONObject(i) ?: continue
                val id = r.s("id")
                if (id.isBlank()) continue
                val rb = r.s("branch")
                if (!allBranch && rb.isNotBlank() && !rb.trim().equals(branchFilter?.trim(), ignoreCase = true)) continue
                val pos = byId[id]
                if (pos != null) {
                    // আগে থেকেই আছে — ফোনের নিজের নতুন লেখাটা বসিয়ে দাও
                    val old = out[pos]
                    /* 🔴🔴🔒 V1305 (১০.০৯.২০২৬, তালিকা ৪১৯ — জলপাইগুড়ি: BARNALI-র ফোনে Pending
                       Calls 9, CHANDANA-র 3, ক্লাউডে 3): এখানে ফোনের নিজের পুরনো লেখা
                       **নিঃশর্তে** জিতত — অন্য ফোন পরে কল করে নতুন তারিখ বসালেও এই ফোন
                       নিজের আগের তারিখটাই দেখাত, তাই "আজকের কল" বেশি গুনত। এখন ফোনের লেখা
                       জেতে **শুধু তার updatedAt ক্লাউড/জমানো সারির চেয়ে নতুন হলে** (V1285-এর
                       "পুরনো কপি নতুন ক্লাউড চাপা দেবে না" — একই নিয়ম, এবার দেখানোর দিকেও)।
                       পুরনো (আর দরকার নেই) সারিটা ফোনের খাতা থেকে সরিয়ে দেওয়া হয়, যাতে
                       বারবার ফিরে না আসে — শুধু SYNCED হলে; PENDING (এখনো ক্লাউডে যায়নি)
                       হলে ছোঁয়া হয় না, ওটা sync-এর কাজ। */
                    val localStamp = r.s("updatedAt")
                    val cloudStamp = old.updatedAt
                    val localNewer = localStamp.isNotBlank() && (cloudStamp.isBlank() || localStamp > cloudStamp)
                    if (!localNewer) {
                        if (r.s("_syncStatus") != "PENDING") toForget.add(id)
                        continue
                    }
                    out[pos] = old.copy(
                        lastRemark = r.s("lastRemark").ifBlank { old.lastRemark },
                        nextFollow = r.s("nextFollow").ifBlank { old.nextFollow },
                        callCount = if (r.has("callCount")) r.optInt("callCount", old.callCount) else old.callCount,
                        lastCallDate = r.s("lastCallDate").ifBlank { old.lastCallDate },
                        lastCallBy = r.s("lastCallBy").ifBlank { old.lastCallBy },
                        name = r.s("name").ifBlank { old.name },
                        disease = r.s("disease").ifBlank { old.disease },
                        patientId = r.s("patientId").ifBlank { old.patientId },
                        updatedAt = r.s("updatedAt").ifBlank { old.updatedAt }
                    )
                    continue
                }
                val d = r.s("mobile").filter { it.isDigit() }.takeLast(10)
                if (d.length == 10 && !seenMobiles.add(d)) continue
                // 🚨 TK-REPORTED, LIVE (29.07.2026 দুপুর ৩.১০, খাতার সারি B82 —
                // \"DEMO TEST\" +917777777777, ফটো-প্রুফসহ): *"অনেকদিন আগে নিজে
                // ডিলিট করেছি, তবু নিজে নিজে ফিরে আসছে... কিছুক্ষণ পরে সরে
                // যাচ্ছে, Refresh করলে আবার ফিরে আসছে।"*
                //
                // **প্রমাণ:** Follow-up-এর খোঁজা ও Global Search — **দুটোতেই
                // \"No records found\"**। অর্থাৎ ডেটাবেসে ওই রেকর্ড **নেই**;
                // কার্ডটা আসছিল **এই ফোনের নিজের জমানো খাতা** থেকে।
                //
                // **আসল কারণ:** এখানে ফোনের সারি যোগ করার সময় দেখা হত না সারিটা
                // **আগে ক্লাউডে গিয়েছিল কি না**। তাই মুছে ফেলা (বা বাতিল হয়ে
                // যাওয়া) পুরনো সারিও প্রতিবার ফিরে আসত। তাজা তালিকা এলে সেটা
                // আবার বাদ পড়ত — এটাই *\"সরে যাচ্ছে, আবার ফিরে আসছে\"*।
                //
                // **এখন `fetchTab()`-এর সেই একই B34 নিয়ম এখানেও:**
                //  • **\"PENDING\"** (এই ফোনে সেভ, এখনো ক্লাউডে যায়নি) → **সবসময়
                //    দেখাবে** — TK-এর লক করা নিয়ম \"ফোনে সেভ হওয়া রেকর্ড হারানো
                //    যাবে না\" অক্ষত।
                //  • **আগে ক্লাউডে গিয়েছিল** অথচ জমানো তালিকায় নেই → ওটা মুছে বা
                //    বাতিল হয়ে গেছে → **আর দেখানো হবে না**।
                // ⛔ যে সারি আগে থেকেই তালিকায় আছে তার নতুন লেখা বসানো (উপরের
                //    অংশ) এক অক্ষরও বদলায়নি।
                if (r.optString("_syncStatus") != "PENDING") continue
                extra.add(
                    FollowUpItem(
                        id = id,
                        name = r.s("name"),
                        mobile = r.s("mobile"),
                        branch = rb,
                        disease = r.s("disease"),
                        stage = r.s("stage").ifBlank { stage },
                        lastRemark = r.s("lastRemark"),
                        nextFollow = r.s("nextFollow"),
                        recordDate = r.s("date").ifBlank { r.s("visitDate").ifBlank { r.s("registrationDate") } },
                        createdAt = r.s("createdAt"),   // 🔒 খাতার সারি B65
                        callCount = r.optInt("callCount", 0),
                        // টাকার ঘর ইচ্ছে করে ০ — সদ্য তৈরি রেকর্ডে এখনো কোনো
                        // পেমেন্ট নেই। ক্লাউডের উত্তর এলে আসল সংখ্যা বসে যাবে।
                        bill = r.optDouble("bill", 0.0),
                        paid = r.optDouble("paid", 0.0),
                        patientId = r.s("patientId"),
                        address = r.s("address"),
                        age = r.s("age"),
                        sex = r.s("sex"),
                        photo = r.s("photo"),
                        updatedAt = r.s("updatedAt"),
                        lastCallDate = r.s("lastCallDate"),
                        lastCallBy = FollowUpModel.prettyStaff(r.s("lastCallBy")),
                        /* 🏷️🔒 V712 — এই ফোনে সেভ হওয়া (এখনো ক্লাউডে না যাওয়া)
                           সারিতেও ট্যাগের ঘরগুলো বসে। আগে বসত না, তাই সদ্য তোলা
                           এনকোয়ারিতে UNEXPECTED/RMP ট্যাগ দেখাত না।
                           ⛔ না থাকলে ফাঁকা — অর্থাৎ ঠিক আগের আচরণ। */
                        timeType = r.s("timeType"),
                        refDoctor = r.s("refDoctor")
                    )
                )
            }
            if (toForget.isNotEmpty()) {
                try { LocalWorkflowStore(ctx).forgetRecords("followups", toForget) } catch (_: Throwable) { }
            }
            if (extra.isEmpty()) out else extra + out
        } catch (_: Throwable) { cached }
    }

    private fun saveCachedTab(stage: String, branchFilter: String?, items: List<FollowUpItem>) {
        val ctx = context ?: return
        try {
            val arr = JSONArray()
            for (it in items) {
                arr.put(
                    JSONObject()
                        .put("id", it.id).put("name", it.name).put("mobile", it.mobile)
                        .put("branch", it.branch).put("disease", it.disease).put("stage", it.stage)
                        .put("lastRemark", it.lastRemark).put("nextFollow", it.nextFollow)
                        .put("noMoreCalls", it.noMoreCalls)   // 📵 V1206
                        .put("recordDate", it.recordDate).put("callCount", it.callCount)
                        .put("createdAt", it.createdAt)   // 🔒 খাতার সারি B65
                        .put("bill", it.bill).put("paid", it.paid).put("patientId", it.patientId)
                        .put("address", it.address).put("age", it.age).put("sex", it.sex)
                        .put("photo", it.photo).put("updatedAt", it.updatedAt)
                        .put("lastCallDate", it.lastCallDate).put("lastCallBy", it.lastCallBy)
                        /* ⏰🔒 V827 (২৯.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"LAST CALL
                           তারিখের পরে যেন Time থাকে"*)।
                           **আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয়):** V543-এ সময়টা
                           দেখানোর ব্যবস্থা হয়েছিল, কিন্তু **জমানো তালিকায়
                           (`saveCachedTab`) ঘরটা কখনো লেখাই হত না**, আর ফেরত
                           পড়ার সময়ও (`loadCachedTab`) তোলা হত না। Follow-up
                           পর্দা "আগে জমানোটা দেখাও" নিয়মে চলে — তাই পর্দা
                           খুললেই সময়হীন লাইনটা চোখে পড়ত, আর লাইন খারাপ থাকলে
                           চিরকালই সময় থাকত না।
                           ⛔ ছোট্ট একটা লেখা — জমানো ফাইল কার্যত বড় হয় না।
                           ⛔ ক্লাউডে একটাও বাড়তি অনুরোধ যায় না (সময়টা `history`
                              থেকেই আসে, যা আগে থেকেই তালিকার সঙ্গে আসে)। */
                        .put("lastCallTime", it.lastCallTime)
                        /* 🆕🔒 V851 — জমানো তালিকাতেও দুটো ঘর, নইলে পর্দা খোলার
                           সঙ্গে সঙ্গে (ক্যাশ থেকে) লাইনটা আবার `LAST CALL`
                           দেখাত, আর লাইন খারাপ থাকলে চিরকালই (V827-এর শিক্ষা)। */
                        .put("regDate", it.regDate)
                        .put("regBy", it.regBy)
                        /* 🏷️🔒 V712 (২৬.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"Tag এ Unexpected
                           লেখা নেই, কিন্তু View All-এ ক্লিক করলে আছে"*)।
                           **আসল কারণ:** এই তিনটে ঘর জমানো তালিকায় **লেখাই হত না**
                           (আগে ইচ্ছাকৃত আপস ছিল)। তাই লাইভ তালিকা আসার আগে —
                           বা লাইন খারাপ থাকলে চিরকাল — কার্ডে ঠিকানার ট্যাগ ·
                           UNEXPECTED · RMP তিনটেই উধাও থাকত, অথচ View All-এ
                           (যেটা আলাদা করে পড়ে) ঠিকই দেখা যেত।
                           ⛔ এগুলো ছোট লেখা, ছবি নয় — জমানো ফাইল বড় হয় না।
                           ⛔ ক্লাউডে একটাও বাড়তি অনুরোধ যায় না। */
                        .put("timeType", it.timeType)
                        .put("refDoctor", it.refDoctor)
                        .put("addressTag", it.addressTag)
                )
            }
            val key = "cache_${stage}_${branchFilter ?: "All"}"
            ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit().putString(key, arr.toString()).apply()
        } catch (_: Throwable) { }
    }

    /** stage: "Inquiry" (Enquiry tab), "Patient" (Visit tab), or
     * "Treatment" (Patient tab) -- same internal stage values app.js uses. */
    /** True if this locally-cached row was created BY THIS STAFF within the
     *  last 3 days. Used so a staff's own fresh entry can never disappear
     *  from their own phone just because one cloud query came back without
     *  it (see the merge in fetchTab). Deliberately narrow: someone else's
     *  record, or an old one, is never added back from local cache. */
    private fun isOwnRecentRow(row: JSONObject, creatorMobile: String?, creatorName: String?): Boolean {
        val by = row.optString("createdBy")
        val mine = (!creatorMobile.isNullOrBlank() && digits(by) == digits(creatorMobile)) ||
            (!creatorName.isNullOrBlank() && by.equals(creatorName, ignoreCase = true))
        if (!mine) return false
        val stampRaw = row.optString("updatedAt").ifBlank { row.optString("createdAt") }
            .ifBlank { row.optString("date") }
        val stamp = stampRaw.take(10)
        if (stamp.length != 10) return false
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -3)
        val cutoff = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        return stamp >= cutoff
    }

    // =========================================================================
    // 🔴🔒 V456 (20.08.2026, TK-নির্দেশ · ধাপ ১, শুধু Inquiry ট্যাব)
    //
    // ⛔ নিচের `fetchTab()` **এক অক্ষরও বদলানো হয়নি**। এই নতুন পথ শুধু
    //    Inquiry ট্যাবের preCloud (followups টেবিলের সারি) delta-fetch করে,
    //    তারপর সেই merge-করা তালিকা নিয়ে `fetchTab()`-কেই ডাকে (নিচের
    //    `fetchTabDelta()` দেখুন) — enquiries-জোড়া/visibility/dedup কিছুই
    //    ছোঁয়া হয় না।
    //
    // নিরাপত্তা-জাল (Doctor Queue-র একই ৩ স্তর, কিন্তু কড়া — ৩০ মিনিট):
    //  ১. since না থাকলে/৩ ঘণ্টা পার হলে → জোর করে পূর্ণ fetch
    //  ২. delta-কল ব্যর্থ হলে → পূর্ণ fetch
    //  ৩. প্রতিটা delta-সারি নিজের `status` দিয়ে যাচাই হয় — এখনও Active হলে
    //     জমানো preCloud-এ বসে/আপডেট হয়, Cancelled/Incomplete/Rejected/
    //     Closed হয়ে গেলে জমানো preCloud থেকে **সরিয়ে দেওয়া হয়**।
    // ⛔ সত্যিকারের DELETE (updatedAt বদলায় না) ধরা পড়বে না — ৩০ মিনিটের
    //    নিয়মিত পূর্ণ-fetch সেটা স্বয়ংক্রিয়ভাবে ঠিক করে দেবে।
    // =========================================================================
    private fun deltaPrefs() = context?.getSharedPreferences(FU_DELTA_PREFS, Context.MODE_PRIVATE)

    private fun fuStampNow(): String = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(System.currentTimeMillis() - FU_SAFETY_BACK_MS))
    } catch (_: Throwable) { "" }

    // =========================================================================
    // 🔴🔒 V456 (20.08.2026, ধাপ ২, TK-অনুমোদিত — "সমস্ত কাজ করতে হবে, তবে
    // অত্যন্ত সাবধানে"): Patient/Treatment ট্যাবের delta — preCloud (followups)
    // + prePatients + prePayments — তিনটেই।
    //
    // ⛔ একটা একক নিয়ম, সব জায়গায় মানা হয়েছে: **তিনটে read একসাথে delta বা
    //    একসাথে পূর্ণ** — কখনো "কিছুটা delta, কিছুটা পুরনো cache" মেশানো হয়
    //    না। একটাও ব্যর্থ/অজানা হলে তিনটেই পূর্ণ fetchTab()-এ ফেরত যায়। এটাই
    //    সবচেয়ে নিরাপদ, কারণ patients/payments একসাথে মিলিয়েই Due/Paid গণনা
    //    হয় (নিচের downstream কোড, ~লাইন ১৫০৮-১৬৭০)।
    //
    // কেন Patients/Payments-এ "সরানো" (removal) লজিক লাগে না (কোড পড়ে
    // নিশ্চিত করা হয়েছে, আন্দাজ নয়):
    //  - Patients: `PatientIdentity.pickPatientRow()` mobile ধরে গ্রুপ করে
    //    একটা row বাছে (branch → real-bill → first-row নিয়মে) — array-র
    //    ক্রম/সংখ্যা-জোড়া কোনোটার উপর নির্ভর করে না, শুধু **সম্পূর্ণ সেট**
    //    দরকার (25.07.2026-এর প্রমাণিত bug-fix comment অনুযায়ী)।
    //  - Payments: `paidByPid[pid] = (আগেরটা ?: 0.0) + paidEffect` — যোগফল,
    //    তাই ক্রম-নিরপেক্ষ। রিফান্ডও নতুন **আলাদা row**, পুরনো row মোছে না।
    //  তাই দুটোই "upsert-only" merge নিরাপদ — কোনো row কখনো সরানো হয় না,
    //  শুধু id ধরে নতুন/বদলানো row বসে।
    //
    // ⛔ সত্যিকারের hard-DELETE (কোনো row updatedAt না বদলে সরাসরি মুছে
    //    যাওয়া) delta-তে ধরা পড়বে না — DoctorQueue/Inquiry-র একই সীমাবদ্ধতা,
    //    ৩ ঘণ্টার নিয়মিত পূর্ণ-fetch স্বয়ংক্রিয়ভাবে ঠিক করে দেয়।
    // =========================================================================

    /**
     * 🔵🔴🔒 V518 (২২.০৮.২০২৬, TK-অনুমোদিত) — **এক নম্বরে একাধিক রোগী হলে
     * Follow-up ট্যাবেও প্রত্যেকে আলাদা।**
     *
     * এই পর্দায় তিন জায়গায় **মোবাইল ধরে** একত্র/বাদ দেওয়া হয় —
     *   (১) উঁচু ধাপে চলে গেলে নিচের ট্যাব থেকে বাদ,
     *   (২) একই ট্যাবে একই নম্বর একবারই,
     *   (৩) কার্ডের bill · Patient ID · ঠিকানা · বয়স জোড়া লাগানো।
     * এক পরিবারে দুজন রোগী হলে এতে স্ত্রী নিজের ট্যাব থেকে **উধাও** হয়ে
     * যেতেন, আর কার্ডে **স্বামীর** বিল/Patient ID বসে যেত।
     *
     * **সমাধান — "পরিচয়ের চাবি":** followups সারির `refId`-ই বলে দেয় সারিটা
     * কার। কিন্তু সেটা অন্ধভাবে ব্যবহার করা যাবে না — ভুল করে একই রোগীর
     * দুটো রেজিস্ট্রেশন হয়ে থাকলে `refId` পরিত্যক্ত সারিটাকেও দেখাতে পারে,
     * আর তাতে খাতার সারি **V143**-এর সুরক্ষা ভাঙত।
     *
     * তাই V516/V517-এর **একই প্রমাণিত চিহ্ন**: স্টাফ নিজে বেছে "Different
     * Patient — Same Mobile" চাপলে তবেই রোগীর আইডি হয়
     * `pat_<১০ সংখ্যা>_<...>` ধাঁচের। অন্য কোনো পথে এই ধাঁচ তৈরি হয় না।
     *   · এই ধাঁচ ⇒ চাবি হবে **ওই রোগীর নিজের আইডি** (আলাদা মানুষ)
     *   · বাকি সব ⇒ চাবি **আগের মতোই মোবাইল** (একটুও বদলায়নি)
     */
    private fun isDeclaredSeparatePatientId(rowId: String, mobileDigits: String): Boolean {
        if (mobileDigits.length != 10) return false
        val prefix = "pat_" + mobileDigits + "_"
        return rowId.startsWith(prefix) && rowId.length > prefix.length
    }

    /** এই Follow-up সারিটা কার — আলাদা রোগী হলে তাঁর আইডি, নইলে মোবাইল। */
    private fun identityKey(refId: String, mobileRaw: String): String {
        val m = digits(mobileRaw)
        return if (isDeclaredSeparatePatientId(refId, m)) refId else m
    }

    /* 📱🔒 V1288 (১০.০৯.২০২৬, তালিকা ৪১১-⑥ ক, TK: *"ক"*) — **বড় তালিকা আলাদা ফাইলে।**
       আগে precloud_/prepatients_/prepayments_ (মিলিয়ে মাস্টার-ফোনে ~৫ MB) আর ছোট
       since_/fullAt_ ঘড়ি — সব একটাই SharedPreferences ফাইলে; ফলে যেকোনো একটা লেখা
       (এমনকি ছোট ঘড়িটাও) পুরো ৫ MB ফাইল নতুন করে ডিস্কে লেখাত। এখন প্রতিটা বড়
       তালিকা নিজের ফাইলে (`followup_delta_big_<key>`) — একটা বদলালে শুধু সেটাই লেখা হয়।
       · প্রথমবার: পুরনো ফাইলে থাকলে সেখান থেকে নতুন ফাইলে সরিয়ে (লিখে) তবেই
         পুরনোটা থেকে মোছা — কিছু হারায় না। · নিয়ম/তথ্য একটুও বদলায়নি, শুধু কোথায় রাখা। */
    private fun bigPrefs(key: String) = context?.getSharedPreferences("followup_delta_big_$key", Context.MODE_PRIVATE)

    private fun loadCachedArray(key: String): JSONArray {
        val big = bigPrefs(key) ?: return JSONArray()
        return try {
            var raw = big.getString(key, null)
            if (raw == null) {
                // পুরনো জায়গা থেকে একবারই সরানো
                val old = deltaPrefs()
                raw = old?.getString(key, null) ?: return JSONArray()
                big.edit().putString(key, raw).apply()
                old.edit().remove(key).apply()
            }
            JSONArray(raw)
        } catch (_: Throwable) { JSONArray() }
    }

    private fun saveCachedArray(key: String, arr: JSONArray) {
        val big = bigPrefs(key) ?: return
        try {
            big.edit().putString(key, arr.toString()).apply()
            val old = deltaPrefs()
            if (old != null && old.contains(key)) old.edit().remove(key).apply()
        } catch (_: Throwable) { }
    }

    /** followups (preCloud)-এর জন্য — Inquiry-তে যে নিয়ম, এখানেও হুবহু একই
     *  (terminal status হলে সরানো)। stage অনুযায়ী আলাদা cache key। */
    private fun deltaPreCloudOrNull(stage: String, since: String): JSONArray? {
        val sinceEnc = try { java.net.URLEncoder.encode(since, "UTF-8") } catch (_: Throwable) { since }
        /* 🔴🔒 V1311 (তালিকা ৪২৩): আগে `stage=eq.` ছাঁকনি ছিল — যে সারি এই ধাপ **ছেড়ে** গেছে
           (Inquiry→Registered, Patient→Treatment) delta-য় আসতই না, তাই ৩ ঘণ্টা পর্যন্ত জমানো
           তালিকায় থেকে যেত। এখন সব ধাপের বদল আসে (সারি ক'টা — শেষ পড়ার পরে যা বদলেছে শুধু),
           আর অন্য ধাপের সারি এই তালিকা থেকে সরে। */
        val delta = try {
            slimFollowups("updatedAt=gt.$sinceEnc")
        } catch (_: Throwable) { null } ?: return null
        val cacheKey = "precloud_${stage.lowercase()}"
        val cached = loadCachedArray(cacheKey)
        if (delta.length() == 0) return cached   // 📱 V1288 (⑥ ক): কিছু বদলায়নি ⇒ লেখা নয়
        val byId = LinkedHashMap<String, JSONObject>()
        for (i in 0 until cached.length()) {
            val o = cached.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        val terminal = setOf("Cancelled", "Incomplete", "Rejected", "Closed")
        for (i in 0 until delta.length()) {
            val row = delta.getJSONObject(i)
            val id = row.optString("id"); if (id.isBlank()) continue
            if (row.optString("status").trim() in terminal || row.optString("stage").trim() != stage) byId.remove(id) else byId[id] = row
        }
        val merged = JSONArray(); for (v in byId.values) merged.put(v)
        saveCachedArray(cacheKey, merged)
        return merged
    }

    /** patients/payments — upsert-only (কখনো row সরানো হয় না, উপরের কারণেই)। */
    /* 💸🔒 V1282 (০৯.০৯.২০২৬, TK-র অনুমতি — তালিকা সারি ৪০৫, ধাপ ২ক; TK: *"আমি কোন
       প্রকার ঝুঁকি নিতে চাইছি না"*) — **Doctor Queue-র জন্য Follow-up-এর রোগী-তালিকা
       ভাগ করে দেওয়া।** মেপে দেখা: Queue-র প্রতিটা ঘর `PATIENT_COLS`-এর ভিতরেই আছে,
       আর দুটোর ব্রাঞ্চ-ছাঁকনিও হুবহু এক (`or=(branch.eq.X,branch.is.null)`, limit 5000)।
       ⇒ Follow-up-এর Patient/Treatment ট্যাব গত ৩ ঘণ্টায় পূর্ণ পড়া করে থাকলে, Queue
         আর নিজে ২ MB নামায় না — জমানো তালিকার উপরে শুধু `updatedAt` বদলানো সারি
         বসিয়ে সেটাই পায়।
       ⛔ **কখনো Follow-up-এর নিজের ঘড়ি (`since_`/`fullAt_`) ছোঁয় না** — শুধু পড়ে।
       ⛔ তালিকা তাজা না থাকলে (Follow-up খোলা হয়নি / ৩ ঘণ্টা পার) `null` ⇒ Queue
          **হুবহু আগের মতো নিজের পড়াই** করে — কোনো আচরণ বদলায় না। */
    fun sharedPatientsOrNull(branchFilter: String?): JSONArray? {
        val sp = deltaPrefs() ?: return null
        val branchKey = branchKeyPart(branchFilter)
        val now = System.currentTimeMillis()
        var bestSince: String? = null
        var bestFullAt = 0L
        for (stage in listOf("Treatment", "Patient")) {
            val k = "${stage.lowercase()}_$branchKey"
            val since = sp.getString("since_$k", null) ?: continue
            val fullAt = sp.getLong("fullAt_$k", 0L)
            if (since.isBlank() || fullAt <= 0L || (now - fullAt) > FU_FULL_REFRESH_INTERVAL_MS) continue
            if (fullAt > bestFullAt) { bestFullAt = fullAt; bestSince = since }
        }
        val since = bestSince ?: return null
        if (loadCachedArray("prepatients_$branchKey").length() == 0) return null
        return try {
            deltaUpsertOnlyOrNull("patients", PATIENT_COLS, branchScopeFilterPlain(branchFilter), "prepatients_$branchKey", since)
        } catch (_: Throwable) { null }
    }

    private fun deltaUpsertOnlyOrNull(table: String, cols: String, extraFilter: String, cacheKey: String, since: String): JSONArray? {
        val sinceEnc = try { java.net.URLEncoder.encode(since, "UTF-8") } catch (_: Throwable) { since }
        val filter = "updatedAt=gt.$sinceEnc" + (if (extraFilter.isNotBlank()) "&$extraFilter" else "")
        val delta = try {
            SupabaseClient.fetchListSlimOrNull(table, filter, 2000, cols)
        } catch (_: Throwable) { null } ?: return null
        val cached = loadCachedArray(cacheKey)
        /* 📱 V1288 (⑥ ক): ক্লাউডে নতুন/বদলানো সারি একটাও নেই ⇒ জমানো তালিকাটাই উত্তর —
           আবার লেখা হয় না (আগে প্রতি ৩০ সেকেন্ডে পুরো তালিকা নতুন করে ডিস্কে যেত)। */
        if (delta.length() == 0) return cached
        val byId = LinkedHashMap<String, JSONObject>()
        for (i in 0 until cached.length()) {
            val o = cached.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        for (i in 0 until delta.length()) {
            val row = delta.getJSONObject(i)
            val id = row.optString("id"); if (id.isBlank()) continue
            byId[id] = row   // upsert-only — কখনো remove নয়
        }
        val merged = JSONArray(); for (v in byId.values) merged.put(v)
        saveCachedArray(cacheKey, merged)
        return merged
    }

    /** stage="Patient"/"Treatment"-এর জন্য preCloud+prePatients+prePayments —
     *  তিনটে একসাথে delta, বা তিনটেই null (কলার তখন পূর্ণ fetchTab()-এ যাবে)। */
    private fun deltaPatientTreatmentTriple(stage: String, branchFilter: String?): Triple<JSONArray, JSONArray, JSONArray>? {
        val sp = deltaPrefs() ?: return null
        val branchKey = branchKeyPart(branchFilter)
        val stateKey = "${stage.lowercase()}_$branchKey"
        val since = sp.getString("since_$stateKey", null)
        val lastFullAt = sp.getLong("fullAt_$stateKey", 0L)
        val now = System.currentTimeMillis()
        if (since.isNullOrBlank() || (now - lastFullAt) > FU_FULL_REFRESH_INTERVAL_MS) return null

        val preCloud = deltaPreCloudOrNull(stage, since) ?: return null
        val branchExtra = branchScopeFilterPlain(branchFilter)
        val patients = deltaUpsertOnlyOrNull(
            "patients", PATIENT_COLS, branchExtra, "prepatients_$branchKey", since
        ) ?: return null
        val payments = deltaUpsertOnlyOrNull(
            "payments", PAYMENT_COLS_FOLLOWUP, branchExtra, "prepayments_$branchKey", since   // V1259
        ) ?: return null

        try { sp.edit().putString("since_$stateKey", fuStampNow()).apply() } catch (_: Throwable) { }
        return Triple(preCloud, patients, payments)
    }

    private fun markPatientTreatmentFullDone(stage: String, branchFilter: String?, preCloud: JSONArray, patients: JSONArray, payments: JSONArray) {
        val sp = deltaPrefs() ?: return
        val branchKey = branchKeyPart(branchFilter)
        val stateKey = "${stage.lowercase()}_$branchKey"
        try {
            saveCachedArray("precloud_${stage.lowercase()}", preCloud)
            saveCachedArray("prepatients_$branchKey", patients)
            saveCachedArray("prepayments_$branchKey", payments)
            sp.edit()
                .putString("since_$stateKey", fuStampNow())
                .putLong("fullAt_$stateKey", System.currentTimeMillis())
                .apply()
        } catch (_: Throwable) { }
    }

    /** branchScopeFilter() একই নিয়ম কিন্তু `or=(...)` শুরুর `filter=` ছাড়া
     *  raw query-part আকারে — deltaUpsertOnlyOrNull-এর extraFilter-এ বসানোর জন্য। */
    private fun branchScopeFilterPlain(branchFilter: String?): String {
        val f = branchScopeFilter(branchFilter) ?: return ""
        return f   // ইতিমধ্যেই "or=(...)" আকারে, সরাসরি &-এ জোড়া যায়
    }

    /* 📱 V1288 (⑥ ক): Inquiry-র তালিকাও একই আলাদা-ফাইল পথে (উপরের loadCachedArray/saveCachedArray)। */
    private fun loadCachedPreCloudInquiry(): JSONArray = loadCachedArray("precloud_inquiry")

    private fun saveCachedPreCloudInquiry(arr: JSONArray) = saveCachedArray("precloud_inquiry", arr)

    /** Inquiry ট্যাবের preCloud — delta বা পূর্ণ, নিরাপত্তা-জাল সহ। ব্যর্থ/
     *  প্রথমবার/দীর্ঘ-বিরতিতে `null` ফেরে — কলার তখন আগের পূর্ণ পথে (slimFollowups) যাবে। */
    private fun deltaPreCloudInquiryOrNull(): JSONArray? {
        val sp = deltaPrefs() ?: return null
        val since = sp.getString("since_inquiry", null)
        val lastFullAt = sp.getLong("fullAt_inquiry", 0L)
        val now = System.currentTimeMillis()
        if (since.isNullOrBlank() || (now - lastFullAt) > FU_FULL_REFRESH_INTERVAL_MS) return null // পূর্ণ-fetch পথে যাক

        val sinceEnc = try { java.net.URLEncoder.encode(since, "UTF-8") } catch (_: Throwable) { since }
        val delta = try {
            slimFollowups("updatedAt=gt.$sinceEnc")   // 🔴 V1311 (তালিকা ৪২৩): ধাপ-ছাঁকনি নেই — ধাপ ছেড়ে যাওয়া সারিও আসে (নিচে সরে)
        } catch (_: Throwable) { null } ?: return null   // ব্যর্থ → পূর্ণ-fetch পথে

        val cached = loadCachedPreCloudInquiry()
        if (delta.length() == 0) return cached   // 📱 V1288 (⑥ ক): কিছু বদলায়নি ⇒ লেখা নয়
        val byId = LinkedHashMap<String, JSONObject>()
        for (i in 0 until cached.length()) {
            val o = cached.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isNotBlank()) byId[id] = o
        }
        val terminal = setOf("Cancelled", "Incomplete", "Rejected", "Closed")
        for (i in 0 until delta.length()) {
            val row = delta.getJSONObject(i)
            val id = row.optString("id")
            if (id.isBlank()) continue
            val status = row.optString("status").trim()
            if (status in terminal || row.optString("stage").trim() != "Inquiry") byId.remove(id) else byId[id] = row
        }
        val merged = JSONArray()
        for (v in byId.values) merged.put(v)
        saveCachedPreCloudInquiry(merged)
        try { sp.edit().putString("since_inquiry", fuStampNow()).apply() } catch (_: Throwable) { }
        return merged
    }

    private fun markFuFullDone(preCloud: JSONArray) {
        val sp = deltaPrefs() ?: return
        try {
            saveCachedPreCloudInquiry(preCloud)
            sp.edit()
                .putString("since_inquiry", fuStampNow())
                .putLong("fullAt_inquiry", System.currentTimeMillis())
                .apply()
        } catch (_: Throwable) { }
    }

    /** শুধু auto-refresh পথের জন্য — স্ক্রিন প্রথম খোলা/Resume/ব্রাঞ্চ-বদল
     *  সবসময়ই সরাসরি `fetchTab()` (পূর্ণ, সবচেয়ে নিরাপদ) ব্যবহার করবে। */
    fun fetchTabDelta(stage: String, branchFilter: String?, creatorName: String? = null, creatorMobile: String? = null): List<FollowUpItem> {
        if (context == null) return fetchTab(stage, branchFilter, creatorName, creatorMobile)

        if (stage == "Patient" || stage == "Treatment") {
            val triple = deltaPatientTreatmentTriple(stage, branchFilter)
            if (triple == null) {
                // নিরাপত্তা-জাল: পূর্ণ fetchTab() চালিয়ে তিনটে cache-ই আবার জমানো।
                val result = fetchTab(stage, branchFilter, creatorName, creatorMobile)
                try {
                    /* 📉🔒 V1257 (০৯.০৯.২০২৬, TK-অনুমোদিত Egress-প্লানের **ধাপ ১**,
                       খাতার সারি ৩৮০) — TK: *"free Plan-এ চালানোর কথা ছিল"*।

                       🔴 **মাপা দোষ:** ঠিক উপরের `fetchTab()` এইমাত্র
                          followups + patients + payments তিনটেই নামিয়েছে; নিচে
                          জমা রাখার জন্য **হুবহু সেই তিনটেই আবার** নামত — একই
                          তথ্য দুবার।

                       ⇒ এখন `CloudReadCache`-এর **হুবহু সেই চাবিগুলো** দিয়েই চাওয়া
                         হয়, যেগুলো `fetchTab()` একটু আগে ভরে গেছে (২০ সেকেন্ডের
                         ভিতরে) ⇒ **নতুন কোনো ডাউনলোড হয় না**, একই সারিগুলোই ফেরে।
                       ⛔ ক্যাশ ফাঁকা/পুরনো হলে ভিতরের ডাকটা **হুবহু আগের পড়াটাই**
                          চালায় — তাই আচরণ এক চুলও বদলায় না, কিছু হারানোর পথ নেই।

                       ⚠️ **followups-এ ইচ্ছে করে হাত দেওয়া হয়নি, শুধু "All" ছাড়া**
                          (TK-কে আগেই জানানো): নিচে জমা হওয়া `precloud_<ধাপ>` ঘরটা
                          ব্রাঞ্চ ধরে আলাদা নয়। ব্রাঞ্চ বাছা থাকলে `fetchTab()`-এর
                          ক্যাশে **শুধু ওই ব্রাঞ্চের** সারি থাকে; সেটা ওখানে বসালে
                          পরে মাস্টার/অন্য ব্রাঞ্চে **কম সারি** দেখাত — ভালো কাজ
                          খারাপ হত। তাই ব্রাঞ্চ বাছা থাকলে ওই পড়াটা আগের মতোই
                          আলাদা করে হয়; "All" হলে ক্যাশের সারিটাই হুবহু এক, তাই
                          তখন সেটাই নেওয়া হয়। */
                    val bKey = branchKeyPart(branchFilter)
                    val fuBase = "stage=eq.$stage&status=not.in.(Cancelled,Incomplete,Rejected,Closed)"
                    val freshCloud = if (followupBranchScope(branchFilter).isEmpty())
                        CloudReadCache.get("fu:stage:$stage:$bKey") { slimFollowups(fuBase) }
                    else slimFollowups(fuBase)
                    val branchExtra = branchScopeFilterPlain(branchFilter)
                    val freshPatients = CloudReadCache.get("followup:patients:$bKey") {
                        SupabaseClient.fetchListSlimOrNull("patients", branchExtra.removePrefix("&"), 5000, PATIENT_COLS)
                    }
                    val freshPayments = CloudReadCache.get("followup:payments:$bKey") {
                        SupabaseClient.fetchListSlimOrNull("payments", branchExtra.removePrefix("&"), 5000, PAYMENT_COLS_FOLLOWUP)   // V1259
                    }
                    if (freshCloud != null && freshPatients != null && freshPayments != null) {
                        markPatientTreatmentFullDone(stage, branchFilter, freshCloud, freshPatients, freshPayments)
                    }
                } catch (_: Throwable) { }
                return result
            }
            val (preCloud, patients, payments) = triple
            return fetchTab(stage, branchFilter, creatorName, creatorMobile,
                preCloudOverride = preCloud, prePatientsOverride = patients, prePaymentsOverride = payments)
        }

        if (stage != "Inquiry") return fetchTab(stage, branchFilter, creatorName, creatorMobile)
        val delta = deltaPreCloudInquiryOrNull()
        if (delta == null) {
            // নিরাপত্তা-জাল: পূর্ণ fetchTab() চালিয়ে তার preCloud cache হিসেবে
            // জমিয়ে রাখা হয়, যাতে পরের delta-কল এখান থেকে এগোতে পারে।
            val result = fetchTab(stage, branchFilter, creatorName, creatorMobile)
            try {
                /* 📉🔒 V1257 (ধাপ ১, উপরের একই কারণ ও একই সাবধানতা) — `fetchTab()`
                   এইমাত্র এই পড়াটা করেছে; "All" হলে ক্যাশের সারিটা হুবহু এক, তাই
                   আর নামানো হয় না। ⛔ ব্রাঞ্চ বাছা থাকলে আগের মতোই আলাদা পড়া —
                   নইলে ব্রাঞ্চ-ধরে-আলাদা-নয় এমন ঘরে কম সারি জমা হয়ে যেত। */
                val fuBase = "stage=eq.Inquiry&status=not.in.(Cancelled,Incomplete,Rejected,Closed)"
                val fresh = if (followupBranchScope(branchFilter).isEmpty())
                    CloudReadCache.get("fu:stage:Inquiry:" + branchKeyPart(branchFilter)) { slimFollowups(fuBase) }
                else slimFollowups(fuBase)
                if (fresh != null) markFuFullDone(fresh)
            } catch (_: Throwable) { }
            return result
        }
        return fetchTab(stage, branchFilter, creatorName, creatorMobile, preCloudOverride = delta)
    }

    /* 🔴🔒 V1587 (১৭.০৯.২০২৬, TK-রিপোর্ট — COB-UTTAMA: "আমার এনকোয়ারিতে ওভারডিউ
       ছিল না, আজ কেন দেখাচ্ছে") ও TK-র স্পষ্ট নির্দেশ ("খুব সাবধানে, কোনো
       ঝুঁকি ছাড়া, সময় লাগলে লাগুক") —
       **আসল কারণ (গভীরে খুঁজে ধরা, আন্দাজ নয়):** কোনো সারি একবার এই ফোনে
       "PENDING" (এখনো ক্লাউডে যায়নি) হিসেবে জমা হলে, নিচের `fetchTab()`-এর
       নিয়ম অনুযায়ী সেটা **সবসময় দেখানো হয়** — এটা TK-রই লক করা, টাকা/
       রোগীর তথ্য হারানো ঠেকানোর স্থায়ী নিয়ম (মন্তব্য নিচেই আছে), **এখানে
       এক অক্ষরও ছোঁয়া হয়নি**।
       ফাঁকটা `markSyncedWhereCloudCaughtUp()`-এ — সেটা শুধু **এই ট্যাবের
       নিজের ধাপের** তাজা ক্লাউড-ফলের সাথেই মেলায়, "PENDING" পতাকা মুছে
       ফেলার জন্য। কোনো সারির ধাপ ক্লাউডে বদলে গেলে (যেমন Inquiry → পরে
       Registered, পেশেন্ট হয়ে যাওয়ায়) সেটা আর কোনোদিন এই ট্যাবের তাজা
       ফলেই দেখা যায় না — তাই তুলনাটা কখনো মেলে না, পতাকা চিরকাল "PENDING"
       থেকে যায়, আর পুরনো রিমার্ক/তারিখ নিয়ে কার্ডটা বারবার ফিরে আসে।
       **সমাধান (শুধু-পড়ার, লক্ষ্যভেদী, TK-অনুমোদিত):** সর্বোচ্চ ৩টা সত্যিই-
       পুরনো (২৪ ঘণ্টার বেশি) PENDING সারি, একটা-একটা করে সরাসরি `id` দিয়ে
       ক্লাউডে যাচাই — cloud-এর updatedAt সমান/নতুন পেলে তবেই পতাকাটা
       SYNCED হয়, যার ফলে নিচের প্রমাণিত পুরনো নিয়মই (স্বাভাবিকভাবে) সারিটা
       বাদ দেয়। ব্যর্থ/না-মেলা/২৪ ঘণ্টার কম হলে কিচ্ছু বদলায় না — সারি
       আগের মতোই দেখাতে থাকে (নিরাপদ দিক, "PENDING হারাবে না" নিয়ম অটুট)।
       ⛔ সত্যিই তাজা (২৪ ঘণ্টার কম) কোনো এডিট এই ফাংশন কখনো ছোঁয় না। */
    /* 🔴🔒 V1596 (১৭.০৯.২০২৬) — আগে এখানে একবারে মাত্র ৩টে সারি, ৩টে আলাদা
       ক্লাউড-ডাক দিয়ে যাচাই হত। V450-এর নিজের-থেকে-মেরামত (উপরে, "Recovered
       rejected duplicate") একসাথে অনেক সারিকে "PENDING" করে দিতে পারে —
       ৩টে/বার এত বড় জমাট কখনোই ধরতে পারত না, তাই ৮ দিন পরেও কার্ড Overdue-ই
       থেকে যাচ্ছিল (TK-রিপোর্ট)। এখন একবারে ২৫টা পর্যন্ত সারির id **একটাই**
       ক্লাউড-ডাকে (`id=in.(...)`) মেলানো হয় — বাড়তি কল কম, তবু ৮ গুণ দ্রুত
       সারে। ⛔ ২৪ ঘণ্টার নিরাপদ সীমা অটুট, যাচাইয়ের নিয়মও (`cloudUpdatedAt
       >= local.updatedAt`) এক অক্ষরও বদলায়নি। */
    private fun healStagelessPendingRows(ctx: Context, stage: String) {
        val nowMs = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000L
        val local = try { LocalWorkflowStore(ctx).rowsForStage(stage) } catch (_: Throwable) { return }
        val staleIds = ArrayList<String>()
        for (i in 0 until local.length()) {
            if (staleIds.size >= 25) break
            val row = local.optJSONObject(i) ?: continue
            if (row.optString("_syncStatus") != "PENDING") continue
            val id = row.optString("id"); if (id.isBlank()) continue
            val stamp = try { SupabaseClient.rowStampMs(row) } catch (_: Throwable) { 0L }
            if (stamp <= 0L || nowMs - stamp < oneDayMs) continue
            staleIds.add(id)
        }
        if (staleIds.isEmpty()) return
        try {
            val idList = staleIds.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
            val found = SupabaseClient.fetchList("followups", "id=in.($idList)", staleIds.size, select = "id,updatedAt")
            for (i in 0 until found.length()) {
                val r = found.optJSONObject(i) ?: continue
                val cid = r.optString("id"); val cUpdated = r.optString("updatedAt", "")
                if (cid.isNotBlank() && cUpdated.isNotBlank()) {
                    LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", cid, cUpdated)
                }
            }
        } catch (_: Throwable) { }
    }

    fun fetchTab(stage: String, branchFilter: String?, creatorName: String? = null, creatorMobile: String? = null, preCloudOverride: JSONArray? = null, prePatientsOverride: JSONArray? = null, prePaymentsOverride: JSONArray? = null): List<FollowUpItem> {
        // Fetch by stage only, then apply visibility CLIENT-SIDE: a record is
        // visible if the viewer is Master / All-branch, it is the same branch,
        // or the viewer created it (matched by mobile OR name). A blank-branch
        // record is NOT auto-visible to everyone anymore (SECURITY FIX,
        // 2026-07-15) — see the "visible" check below for why.
        // TK-REPORTED BUG FIX (2026-07-15): this used to read the LOCAL cache
        // first and only ever touch the cloud once (the very first time this
        // device ever opened this tab) -- after that, a device would show its
        // own frozen snapshot forever, so a same-branch staff (or Master) on a
        // different phone never saw entries another staff created. Now it
        // always asks the cloud first; the local cache is only a fallback for
        // when there is genuinely no internet, so the app still works offline.
        // PERFORMANCE FIX (2026-07-25, TK-reported: "tab switching is far too
        // slow"): this function used to make its network calls one after the
        // other, so one tab load waited for the SUM of 3-4 round trips. None
        // of them depend on each other, so they are all started together here
        // and simply read further down exactly where they were read before.
        // The queries themselves (table, filter, limit) are byte-for-byte the
        // same, so the Supabase quota cost is unchanged; only the waiting is
        // shorter. Every null-check / cache-fallback below is untouched.
        val higherStagesPre: String? = when (stage) {
            "Inquiry" -> "Patient,Treatment"
            "Patient" -> "Treatment"
            else -> null
        }
        val needsPatientJoin = stage == "Treatment" || stage == "Patient"
        var preCloud = JSONArray()
        var preHigher: JSONArray? = null
        var preCancelledInquiry: JSONArray? = null
        var preTerminalEnquiries: JSONArray? = null
        var preEnquiries = JSONArray()
        var prePatients = JSONArray()
        var prePayments: JSONArray? = null
        var preRejectedVisits: JSONArray? = null
        var preTreatmentStage: JSONArray? = null
        var preIncompleteTreatment: JSONArray? = null
            // 🚨 TK-REPORTED (2026-07-27): "প্রতিটা সেকশনে লোডিং হতে এত সময়
            // লাগছে, স্টাফ অ্যাপ ব্যবহার করতে চাইছে না।"
            //
            // WHY IT IS SLOW: opening Follow-up runs this whole function FOUR
            // times within the same second -- three times to work out the
            // Enquiry / Visit / Patient tab numbers, and once more for the
            // list actually being shown. Every one of those repeats the SAME
            // followups queries. So the same data was being downloaded up to
            // four times over, on the same weak line, before a single card
            // could appear.
            //
            // Each read below now goes through CloudReadCache: whoever asks
            // first downloads it, the other three share that answer. Same
            // queries, same rows, same number of DIFFERENT queries -- only the
            // repeats are gone. A FAILED read is never remembered, and ANY
            // save anywhere clears the whole cache instantly, so nothing on
            // screen can ever be stale.
        runBlocking {
            val jobs = mutableListOf<Deferred<Unit>>()
            jobs += async(Dispatchers.IO) {
                preCloud = if (preCloudOverride != null) preCloudOverride else {
                    val base = "stage=eq.$stage&status=not.in.(Cancelled,Incomplete,Rejected,Closed)"
                    val scope = followupBranchScope(branchFilter)   // 📦 V999
                    CloudReadCache.get("fu:stage:$stage:" + branchKeyPart(branchFilter)) {
                        // ছাঁকা পড়া আগে; ব্যর্থ হলে হুবহু আগের পুরো পড়া
                        (if (scope.isEmpty()) null else slimFollowups(base + scope))
                            ?: slimFollowups(base)
                    } ?: JSONArray()
                }
            }
            if (higherStagesPre != null) {
                jobs += async(Dispatchers.IO) {
                    preHigher = CloudReadCache.get("fu:higher:$higherStagesPre") {
                        slimFollowupsLink("stage=in.($higherStagesPre)&status=not.in.(Cancelled,Incomplete,Rejected,Closed)")   // V1260
                    }
                }
            }
            if (stage == "Inquiry") {
                jobs += async(Dispatchers.IO) {
                    preCancelledInquiry = CloudReadCache.get("fu:inqCancelled") {
                        // V445: include every terminal Inquiry status.  The active list is
                        // fetched separately; this small companion read is only the
                        // "do not resurrect" guard for duplicate rows of the same mobile.
                        /* 📵🔒 V1245 (০৯.০৯.২০২৬, TK-রিপোর্ট ও অনুমতি — খাতার সারি ৩৬৬):
                           TK: *"যে সমস্ত নাম্বারে আমরা কল করব না বলে ফাইনাল করেছি,
                           সেই নম্বরগুলোও কেন দেখাবে"*।
                           🔴 **আসল কারণ (কোড ধরে):** নিচের এনকোয়ারি-জাল `followups`
                              সারিটা না পেলে `enquiries` টেবিল থেকে কার্ডটা আবার বানায়,
                              আর সেই বানানো কার্ডে `noMoreCalls` **কখনো বসেই না** ⇒
                              ডিফল্ট false ⇒ থামানো নম্বর ফিরে আসে। জালের বাদ-দেওয়ার
                              তালিকাতেও শুধু Cancelled/Incomplete/Rejected/Closed ছিল।
                           ⇒ **এই একই পড়াতেই** এখন `noMoreCalls=true` সারিগুলোও আসে —
                             **নতুন কোনো ক্লাউড-ডাক নয়**, শুধু ছাঁকনিটা চওড়া (নিয়ম ১৩)।
                           ⛔ নিচে `closedInquiryMobiles` বানানোর সময় **status মিলিয়ে**
                              নেওয়া হয়, তাই "আর ফোন নয়" সারি ভুল করে *Reject* হিসেবে
                              গোনা হয় না — পুরনো আচরণ এক অক্ষরও বদলায়নি। */
                        /* 🛡️🔒 V1246 — **নিরাপত্তা:** `noMoreCalls` ঘরটা ডেটাবেসে
                           না থাকলে (V1206-এর SQL না চালানো থাকলে) এই চওড়া ছাঁকনি
                           ব্যর্থ হত ⇒ উত্তর `null` ⇒ নিচের এনকোয়ারি-জাল **পুরোটাই বন্ধ**
                           হয়ে যেত, নতুন এনকোয়ারিও দেখা যেত না। তাই ব্যর্থ হলে
                           সঙ্গে সঙ্গে **পুরনো সরু ছাঁকনিতেই** ফিরে যাওয়া হয় —
                           তখন আচরণ হুবহু V1244-এর মতো, কিছুই ভাঙে না।
                           ⛔ সফল হলে (SQL চালানো থাকলে) আগের মতোই দুরকম সারি আসে। */
                        slimFollowups("stage=eq.Inquiry&or=(status.in.(Cancelled,Incomplete,Rejected,Closed),noMoreCalls.is.true)")
                            ?: slimFollowups("stage=eq.Inquiry&status=in.(Cancelled,Incomplete,Rejected,Closed)")
                    }
                }
                jobs += async(Dispatchers.IO) {
                    /* 🔴🔒 V820 (২৯.০৮.২০২৬) — **Supabase লগ মেপে পাওয়া সবচেয়ে বড় ফুটো।**
                       লগে (Log Explorer, chunked উত্তর) গত এক ঘণ্টায় ২৪ বার এসেছে:
                       `?select=*&order=updatedAt.desc.nullslast&limit=5000&stage=eq.Inquiry`
                       — অর্থাৎ `enquiries` টেবিলের **সব ঘর, ৫০০০ সারি, সব ব্রাঞ্চের**।
                       এটাই ছিল দৈনিক ~৫০০ MB-র বড় অংশ।

                       দুটো বদল, দুটোই **কোড পড়ে প্রমাণ করে** নেওয়া:

                       ১) **শুধু দরকারি ঘর** (`ENQUIRY_COLS_INQUIRY_TAB`) — বিস্তারিত
                          ওই ধ্রুবকের মাথায়।

                       ২) **নিজের ব্রাঞ্চের সারিই** আনা হয়, যদি ছাঁকনিতে ঠিক একটাই
                          ব্রাঞ্চ থাকে। কেন এটা নিরাপদ (আন্দাজ নয়):
                          · নিচের প্রতিটা ব্যবহারেই সারিটা শেষে `branchAllows()`
                            দিয়েই বাছা হয় — অন্য ব্রাঞ্চের সারি আজও দেখানো হয় না।
                          · `branchAllows()` এখানে শুধু **ব্রাঞ্চের নাম** মেলায়,
                            কারণ তার দ্বিতীয় শর্তটা `patientId` ধরে চলে আর ঘরটা
                            `enquiries` টেবিলে নেই (সবসময় ফাঁকা)।
                          · ফাঁকা-ব্রাঞ্চ সারানোর কাজটাও (`branchFromEnquiries`)
                            **নিজের ব্রাঞ্চের** এনকোয়ারি থেকেই হয় — অন্য ব্রাঞ্চের
                            সারি পেলেও সেটা পরের ধাপে বাদই যেত। ⇒ ফল অভিন্ন।
                       ⛔ Master / "All" / একাধিক ব্রাঞ্চের ছাঁকনি হলে **আগের মতোই
                          সব** আনা হয় — কোনো ঝুঁকি নেওয়া হয়নি।
                       ⛔ জমানো কপির চাবিতে ব্রাঞ্চ যোগ করা হলো, নইলে এক ফোনে
                          মাস্টার ও স্টাফ পালা করে খুললে ভুল তালিকা দেখাতে পারত।
                       ⛔ সরু পড়া ব্যর্থ হলে `fetchListSlimOrNull` নিজেই আগের
                          পথে (`select=*`) ফিরে যায় — কিছুই ভাঙে না। */
                    val enqOneBranch = branchFilter
                        ?.takeIf { it.isNotBlank() && it != "All" }
                        ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                        ?.singleOrNull()
                        .orEmpty()
                    val enqScope =
                        if (enqOneBranch.isNotBlank())
                            "&branch=eq." + java.net.URLEncoder.encode(enqOneBranch, "UTF-8").replace("+", "%20")
                        else ""
                    preEnquiries = CloudReadCache.get("enq:inquiry:" + enqOneBranch.ifBlank { "all" }) {
                        SupabaseClient.fetchListSlimOrNull(
                            "enquiries", "stage=eq.Inquiry$enqScope", 5000,
                            SupabaseClient.ENQUIRY_COLS_INQUIRY_TAB
                        )
                    } ?: JSONArray()
                }
                jobs += async(Dispatchers.IO) {
                    // 🔴🔒 V447 (19.08.2026, TK live screenshot — Neha/Shyam still
                    // visible after old Reject): V445 only inspected enquiries whose
                    // *stage was still Inquiry*. Older web/mobile reject paths could
                    // legitimately leave the enquiry row with stage=Cancelled while
                    // status=Cancelled. That terminal row was therefore absent from
                    // preEnquiries, so an old Active duplicate follow-up survived.
                    // Read only non-Active enquiry status rows, independent of stage;
                    // later we accept only known terminal statuses. Tiny/slim read,
                    // cached once per render, no row is changed or deleted.
                    preTerminalEnquiries = CloudReadCache.get("enq:terminalStatus") {
                        SupabaseClient.fetchListSlimOrNull(
                            "enquiries", "status=neq.Active", 5000,
                            "id,mobile,status,stage,updatedAt"
                        )
                    }
                }
            }
            if (needsPatientJoin) {
                // TK-REPORTED (2026-07-27, "ডাটা লোড হতে প্রচুর সময় লাগে"):
                // these two are the heaviest reads in the whole app -- the
                // ENTIRE patients table and the ENTIRE payments table. Opening
                // the Follow-up screen runs this function four times within
                // the same second (three for the tab numbers, one for the list
                // being shown), so these two tables were being pulled up to
                // three times over, simultaneously, on the same connection.
                // They are now fetched ONCE and shared for a few seconds --
                // same query, same rows, same code reading them.
                // Failure is never cached, so a bad read is retried normally.
                jobs += async(Dispatchers.IO) {
                    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — Egress কমাতে: নির্দিষ্ট ব্রাঞ্চ হলে
                    // (স্টাফ সবসময়, বা master একটা ব্রাঞ্চ বাছলে) শুধু সেই ব্রাঞ্চ + ফাঁকা-
                    // ব্রাঞ্চ টানা হয়; "All"/master-সব হলে আগের মতোই সব। cache-key-ও
                    // ব্রাঞ্চ ধরে আলাদা, যাতে এক ব্রাঞ্চের ফল অন্যটায় মিশে না যায়।
                    val scope = branchScopeFilter(branchFilter)
                    prePatients = if (prePatientsOverride != null) prePatientsOverride else
                        CloudReadCache.get("followup:patients:" + branchKeyPart(branchFilter)) {
                            slimPatients(scope)
                        } ?: JSONArray()
                }
                jobs += async(Dispatchers.IO) {
                    val scope = branchScopeFilter(branchFilter)
                    prePayments = if (prePaymentsOverride != null) prePaymentsOverride else
                        CloudReadCache.get("followup:payments:" + branchKeyPart(branchFilter)) {
                            SupabaseClient.fetchListOrNull("payments", scope, 5000, select = PAYMENT_COLS_FOLLOWUP)   // V1259
                        }
                }
            }
            if (stage == "Patient") {
                jobs += async(Dispatchers.IO) {
                    preRejectedVisits = CloudReadCache.get("fu:visitRejected") {
                        slimFollowupsLink("stage=eq.Patient&status=in.(Cancelled,Incomplete,Rejected,Closed)")   // V1260
                    }
                }
                jobs += async(Dispatchers.IO) {
                    preTreatmentStage = CloudReadCache.get("fu:treatmentAll") {
                        slimFollowupsLink("stage=eq.Treatment")   // V1260
                    }
                }
            }
            if (stage == "Treatment") {
                jobs += async(Dispatchers.IO) {
                    preIncompleteTreatment = CloudReadCache.get("fu:trIncomplete") {
                        slimFollowupsLink("stage=eq.Treatment&status=in.(Cancelled,Incomplete,Rejected,Closed)")   // V1260
                    }
                }
            }
            jobs.awaitAll()
        }

        val cloud = preCloud
        val rows: JSONArray = if (cloud.length() > 0) {
            context?.let { ctx ->
                // 🚨 TK-REPORTED, চালু ক্লিনিক (28.07.2026, খাতার সারি B27):
                // এই এক লাইনটাই স্টাফদের ফোনে "isn't responding" আনছিল। আগে
                // প্রতিটা সারি আলাদা করে জমা হত — ৪৭টা সারি মানে ৪৭ বার তালা,
                // ৪৭ বার পুরো তালিকা পড়া-লেখা আর ৪৭ বার ডিস্কে লেখার অপেক্ষা;
                // ওই পুরো সময়টা পর্দা একই তালার জন্য দাঁড়িয়ে থাকত।
                // এখন একবারেই সব সারি জমা হয় (upsertFollowUps)।
                // ⛔ প্রতিটা সারির নিয়ম হুবহু আগের — তালিকায় যা দেখাত তাই দেখাবে।
                val store = LocalWorkflowStore(ctx)
                val batch = ArrayList<org.json.JSONObject>(cloud.length())
                for (i in 0 until cloud.length()) batch.add(cloud.getJSONObject(i))
                store.upsertFollowUps(batch, "SYNCED")
            }
            cloud
        } else {
            // Cloud returned nothing -- either truly empty, or no internet.
            // Fall back to whatever is cached locally so the screen never goes
            // blank / offline use still works.
            context?.let { LocalWorkflowStore(it).rowsForStage(stage) } ?: JSONArray()
        }
        // TK-REPORTED BUG FIX (2026-07-16): a staff's own just-saved entry
        // (Registration -> Visit tab, etc.) could vanish from this list --
        // even the whole tab could look empty -- if it hadn't finished
        // syncing to the cloud yet AND the cloud already had unrelated data
        // for a different branch (so the cloud branch above ran and
        // completely replaced local data, dropping the not-yet-synced
        // entry). Now any locally-pending row for this stage is always
        // merged in too, regardless of which branch above ran, so a staff
        // never loses sight of their own fresh entry while it's still
        // syncing in the background.
        val merged = JSONArray()
        for (i in 0 until rows.length()) merged.put(rows.getJSONObject(i))
        // ক্লাউড সত্যিই উত্তর দিয়েছে কি না (খাতার সারি B34) — উত্তর দিলে তবেই
        // মুছে যাওয়া সারি বাদ দেওয়া নিরাপদ।
        val cloudAnswered = cloud.length() > 0
        context?.let { ctx ->
            try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("followups", rows) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            try { healStagelessPendingRows(ctx, stage) } catch (_: Throwable) { }   // 🔴🔒 V1587
            val pending = LocalWorkflowStore(ctx).rowsForStage(stage)
            val idPosition = HashMap<String, Int>()
            for (i in 0 until merged.length()) {
                val existingId = merged.getJSONObject(i).optString("id")
                if (existingId.isNotBlank()) idPosition[existingId] = i
            }
            for (i in 0 until pending.length()) {
                val p = pending.getJSONObject(i)
                val pid = p.optString("id")
                if (pid.isBlank()) continue
                val stillPending = p.optString("_syncStatus") == "PENDING"
                val existingPos = idPosition[pid]
                if (!stillPending) {
                    // 🔒 PERMANENT RULE (TK's order, 27.07.2026, after the same
                    // "a registered patient is missing from the list" complaint
                    // came back a second time):
                    //
                    //   ANYTHING THIS PHONE HAS SAVED FOR THIS SECTION IS ALWAYS
                    //   SHOWN. The cloud can only ADD to that list, never take
                    //   away from it.
                    //
                    // This used to be a guess: a row already marked SYNCED was
                    // only added back if THIS staff had created it AND it was
                    // less than 3 days old. Both halves of that guess failed in
                    // real use -- a record saved from the front desk phone but
                    // created under another staff's login, or one a few days
                    // old, was left entirely to the cloud query, and if that one
                    // query came back without it (weak line, partial result,
                    // a moment before the row landed), the patient vanished from
                    // the screen of the very person who had just entered them.
                    //
                    // Now there is no guess left: every locally-stored row for
                    // this section is merged in. It cannot show anything wrong --
                    // rowsForStage() already drops Cancelled/Incomplete/Closed
                    // rows, every cloud refresh overwrites these same local rows
                    // with the cloud's version, and the branch check plus the
                    // higher-stage/rejected exclusions further down still apply
                    // to them exactly as to a cloud row. The only thing that
                    // changes is that a record can no longer silently disappear
                    // from the phone that saved it.
                    // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B34):
                    // *"আমি নিজে ডিলিট করে দিয়েছিলাম, তারপরে এখন সে কীভাবে চলে
                    // আসল?"* — উপরের নিয়মটার একটাই ফাঁক ছিল: **যে সারি একসময়
                    // ক্লাউডে ছিল ("SYNCED") সেটাও চিরকাল ফিরিয়ে আনা হত।**
                    // তাই মুছে ফেলা বা বাতিল করা রেকর্ড ফোনের জমানো তালিকা থেকে
                    // বারবার ফিরে আসত, আর পুরনো/আধা তথ্য নিয়ে আসত বলে রোগের
                    // নামের মতো ঘরগুলো ফাঁকা দেখাত।
                    //
                    // **এখন নিয়মটা এইরকম:**
                    //  • এই ফোনে করা কিন্তু **এখনো ক্লাউডে যায়নি** ("PENDING")
                    //    সারি — আগের মতোই **সব সময় দেখাবে**। TK-এর লক করা
                    //    নিয়ম ("ফোনে সেভ হওয়া রেকর্ড হারানো যাবে না") অক্ষত।
                    //  • **আগে ক্লাউডে ছিল** ("SYNCED") সারি — ক্লাউড যদি সত্যিই
                    //    উত্তর দিয়ে থাকে অথচ ওটা না পাঠায়, তার মানে ওটা মুছে বা
                    //    বাতিল হয়ে গেছে → **আর দেখানো হবে না**।
                    //  • ক্লাউড কিছুই না পাঠালে (লাইন খারাপ) আগের মতোই ফোনের
                    //    তালিকাই দেখায় — তখন কিছুই হারায় না।
                    if (existingPos == null && cloudAnswered) continue
                    if (existingPos == null) {
                        idPosition[pid] = merged.length()
                        merged.put(p)
                    }
                    continue
                }
                if (existingPos != null) {
                    // This id is already in the cloud result but with a pending
                    // (not-yet-synced) edit for the same record -- the pending
                    // version is always the newer one, so it replaces the stale
                    // cloud row instead of being ignored.
                    /* 🔴🔒 V1305 (তালিকা ৪১৯): "pending মানেই নতুন" — এই ধরে-নেওয়াটা ভুল ছিল।
                       pending লেখাটা আটকে থাকলে (নেট/কিউ) আর তার মধ্যে অন্য ফোন একই সারিতে
                       নতুন কিছু লিখলে, ক্লাউডের সারিটাই নতুন। তাই updatedAt মিলিয়ে —
                       ফোনেরটা নতুন হলে তবেই ফোনেরটা; নইলে ক্লাউডেরটাই থাকে। */
                    val localStamp = p.optString("updatedAt", "")
                    val cloudStamp = merged.getJSONObject(existingPos).optString("updatedAt", "")
                    if (localStamp.isNotBlank() && (cloudStamp.isBlank() || localStamp >= cloudStamp)) {
                        /* 🔴🔒 V1311 (তালিকা ৪২৩, রুল ৭ — V1306-এর একই নিয়ম): পুরো সারি বদলানো নয়;
                           ক্লাউডের সারির উপরে ফোনের ঘরগুলো — ফোনের কপি আধখানা হলেও নাম/রোগ/তারিখ হারায় না। */
                        val ov = JSONObject(merged.getJSONObject(existingPos).toString()); val ks = p.keys()
                        while (ks.hasNext()) { val k = ks.next(); ov.put(k, p.opt(k)) }
                        merged.put(existingPos, ov)
                    }
                } else {
                    idPosition[pid] = merged.length()
                    merged.put(p)
                }
            }
        }
        // 🔴🔒 V445 (19.08.2026, TK live screenshot): a mobile that was already
        // Reject/Closed could still reappear when an older duplicate followups row for
        // the SAME mobile+Inquiry stage remained Active.  The list used to judge rows
        // by id, so the Active duplicate survived even though a terminal sibling existed.
        //
        // Rule now: for Inquiry only, ANY terminal sibling (cloud OR this phone's local
        // cache, or the enquiry row's own terminal status) suppresses all Active duplicate
        // cards for that mobile.  A genuine Restore is safe because restoreAndMove()
        // reactivates EVERY matching followups/enquiries row; after a completed Restore
        // there is no terminal sibling left, so the card appears normally again.
        // No row is deleted and no payment/patient data is touched.
        val closedInquiryMobiles = HashSet<String>()
        /* 📵🔒 V1245 — *"আর ফোন নয়"* করা নম্বরগুলো আলাদা তালিকায়।
           ⛔ ইচ্ছে করেই `closedInquiryMobiles`-এ মেশানো হয়নি — ওটা Reject/Close-এর
              অর্থ বহন করে, আর "আর ফোন নয়" **Reject নয়** (কার্ডটা তালিকায় থাকে,
              শুধু কল-তালিকা ও ব্যানার থেকে সরে)। মিশিয়ে দিলে ভালো কাজ খারাপ হত। */
        val stoppedInquiryMobiles = HashSet<String>()
        if (stage == "Inquiry") {
            preCancelledInquiry?.let { terminalRows ->
                for (i in 0 until terminalRows.length()) {
                    val r = terminalRows.optJSONObject(i) ?: continue
                    val m = digits(r.s("mobile"))
                    if (m.isEmpty()) continue
                    /* 📵 V1245 — পড়াটা এখন দুরকম সারি আনে, তাই এখানে **status
                       মিলিয়ে** নেওয়া হয়: terminal হলে আগের তালিকায়, "আর ফোন নয়"
                       হলে নতুন তালিকায়। ⛔ আগের আচরণ হুবহু অক্ষত। */
                    val st = r.s("status").trim().lowercase()
                    if (st in setOf("cancelled", "incomplete", "rejected", "closed")) closedInquiryMobiles.add(m)
                    if (r.optBoolean("noMoreCalls", false)) stoppedInquiryMobiles.add(m)
                }
            }
            // V447: terminal enquiry rows must be judged by STATUS, not by stage=Inquiry.
            // Old rejects may have stage=Cancelled, so V445's preEnquiries scan could
            // never see them. A genuine Restore sets status back to Active on every
            // matching enquiry row, so this guard releases normally after Restore.
            preTerminalEnquiries?.let { terminalEnquiries ->
                for (i in 0 until terminalEnquiries.length()) {
                    val e = terminalEnquiries.optJSONObject(i) ?: continue
                    val st = e.s("status").trim().lowercase()
                    if (st in setOf("cancelled", "incomplete", "rejected", "closed")) {
                        val m = digits(e.s("mobile"))
                        if (m.isNotEmpty()) closedInquiryMobiles.add(m)
                    }
                }
            }
            // Keep the existing stage=Inquiry scan too; it also covers fresh/local rows
            // already present in the main enquiry read without needing the companion read.
            for (i in 0 until preEnquiries.length()) {
                val e = preEnquiries.optJSONObject(i) ?: continue
                val st = e.s("status").trim().lowercase()
                if (st in setOf("cancelled", "incomplete", "rejected", "closed")) {
                    val m = digits(e.s("mobile"))
                    if (m.isNotEmpty()) closedInquiryMobiles.add(m)
                }
            }
            context?.let { ctx ->
                try {
                    val localTerminal = LocalWorkflowStore(ctx).rejectedRowsForStage("Inquiry")
                    for (i in 0 until localTerminal.length()) {
                        val m = digits(localTerminal.optJSONObject(i)?.s("mobile").orEmpty())
                        if (m.isNotEmpty()) closedInquiryMobiles.add(m)
                    }
                } catch (_: Throwable) { }
            }

            // 🔴🔒 V448 (19.08.2026, TK live V447 proof — Neha/Shyam still
            // visible): STATUS can no longer be trusted by itself for old rows. A
            // generic Web ensureFollow() path used to rewrite an existing row to
            // Active while preserving its append-only history. Scan the rows that
            // are actually about to be rendered and let the latest explicit
            // Reject/Restore history decision win. This is read-only; no cloud row
            // is changed merely by opening Follow-up.
            for (i in 0 until merged.length()) {
                val r = merged.optJSONObject(i) ?: continue
                if (!r.s("stage").equals("Inquiry", true)) continue
                if (!inquiryHistoryEndsTerminal(r)) continue
                val m = digits(r.s("mobile"))
                if (m.isNotEmpty()) closedInquiryMobiles.add(m)
            }

            // V450: V448 could hide a legacy broken duplicate but left the bad Active
            // row untouched in cloud. Repair only rows for which we ALREADY have durable
            // reject evidence (terminal sibling/status/history). No new read is made:
            // this uses the arrays fetched above. After one successful write the row is
            // excluded by the normal cloud query forever; failures use the existing queue.
            if (closedInquiryMobiles.isNotEmpty()) {
                val now = isoNow()
                for (i in 0 until merged.length()) {
                    val r = merged.optJSONObject(i) ?: continue
                    if (!r.s("stage").equals("Inquiry", true)) continue
                    val m = digits(r.s("mobile"))
                    if (m.isEmpty() || !closedInquiryMobiles.contains(m)) continue
                    val st = r.s("status")
                    if (st.equals("Cancelled", true) || st.equals("Incomplete", true) ||
                        st.equals("Rejected", true) || st.equals("Closed", true)) continue
                    val id = r.s("id")
                    if (id.isBlank()) continue
                    val key = "followups:$id"
                    val shouldRepair = synchronized(LOCK) { legacyRejectRepairSeen.add(key) }
                    if (!shouldRepair) continue
                    val history = try { JSONArray((r.optJSONArray("history") ?: JSONArray()).toString()) } catch (_: Throwable) { JSONArray() }
                    history.put(JSONObject().put("date", FollowUpModel.today()).put("time", now)
                        .put("remark", "Recovered rejected duplicate").put("staff", "System repair")
                        .put("status", "Cancelled").put("decisionVersion", "V450"))
                    val fields = JSONObject().put("status", "Cancelled").put("nextFollow", "")
                        .put("history", history).put("updatedAt", now)
                    rememberEditOnThisPhone(id, fields, r)
                    /* 🔴🔒 V1596 (১৭.০৯.২০২৬, TK-রিপোর্ট — কল করেও/এনকোয়ারি বন্ধ
                       হয়ে যাওয়ার পরেও কার্ড চিরকাল Overdue) — গভীরে যাচাই করে
                       পাওয়া আসল কারণ: এই V450-এর নিজের-থেকে-মেরামতের লেখাটা
                       (`rememberEditOnThisPhone`) ফোনের কপিকে "PENDING" করে
                       দেয়, আর নিচের ক্লাউড-লেখা সত্যিই সফল হলেও (`ok=true`)
                       কেউ সেই "PENDING" পতাকাটা সঙ্গে সঙ্গে সরাত না — শুধু
                       V1587-এর সাধারণ (একবারে ৩টে, ২৪ ঘণ্টা পুরনো) heal-এর
                       ভরসায় থাকত, যেটা এত বড় ব্যাচের জন্য যথেষ্ট দ্রুত না।
                       ⇒ এখন ক্লাউড-লেখা সফল হলে **এই সারিটাই সঙ্গে সঙ্গে**
                       SYNCED করে দেওয়া হয় — অপেক্ষা করতে হয় না। */
                    healExecutor.submit {
                        val ok = try { SupabaseClient.updateById("followups", id, fields) } catch (_: Throwable) { false }
                        if (!ok) queueFieldUpdate(id, fields)
                        else context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", id, now) } catch (_: Throwable) { } }
                    }
                }
                // If a legacy enquiry row itself remained Active while a terminal
                // follow-up proves Reject, close that already-loaded source row too.
                // This avoids the enquiries fallback recreating the card later.
                for (i in 0 until preEnquiries.length()) {
                    val e = preEnquiries.optJSONObject(i) ?: continue
                    val m = digits(e.s("mobile"))
                    if (m.isEmpty() || !closedInquiryMobiles.contains(m)) continue
                    val st = e.s("status")
                    if (st.equals("Cancelled", true) || st.equals("Incomplete", true) ||
                        st.equals("Rejected", true) || st.equals("Closed", true)) continue
                    val id = e.s("id")
                    if (id.isBlank()) continue
                    val key = "enquiries:$id"
                    val shouldRepair = synchronized(LOCK) { legacyRejectRepairSeen.add(key) }
                    if (!shouldRepair) continue
                    val fields = JSONObject().put("status", "Cancelled").put("updatedAt", now)
                    context?.let { ctx ->
                        try { LocalWorkflowStore(ctx).upsertEnquiry(JSONObject(e.toString()).apply {
                            put("status", "Cancelled"); put("updatedAt", now)
                        }) } catch (_: Throwable) { }
                    }
                    healExecutor.submit {
                        val ok = try { SupabaseClient.updateById("enquiries", id, fields) } catch (_: Throwable) { false }
                        if (!ok) context?.let { ctx -> try { GenericUpdateQueue.queue(ctx, "enquiries", id, fields) } catch (_: Throwable) { } }
                    }
                }
            }
        }

        // TK APPROVED (2026-07-15) — SECURITY FIX: a record with a blank branch
        // must NOT be visible to every branch anymore. Only Master/All-branch
        // viewers, the same branch, or the actual creator (by mobile or name)
        // can see it. This closes a cross-branch data leak (a Kishanganj staff
        // was seeing a Jalpaiguri patient because that one record's branch was
        // empty). Nothing else in this visibility rule changed.
        val allBranch = branchFilter == null || branchFilter == "All"
        val items = mutableListOf<FollowUpItem>()
        val seenIds = HashSet<String>()
        for (i in 0 until merged.length()) {
            val row = merged.getJSONObject(i)
            if (stage == "Inquiry") {
                val mm = digits(row.s("mobile"))
                if (mm.isNotEmpty() && closedInquiryMobiles.contains(mm)) continue
            }
            val rb = row.s("branch")
            // TK-ORDER (2026-07-25, Falakata staff was seeing Cooch Behar and
            // Kishanganj enquiries): a staff must see ONLY their own branch.
            // The old rule also let a person see any record they had created
            // themselves, whatever branch it belonged to -- that is exactly how
            // other branches' rows leaked into this list. Removed. Master (All
            // Branch) is unaffected and still sees everything.
            // 🚨 TK-REPORTED (2026-07-27): "কখনো এনকোয়ারি করলে অন্য স্টাফ দেখতে
            // পায় না।" A followups row whose branch field is blank is invisible
            // to EVERY branch staff (only Master, on All Branch, sees it) --
            // and the note above says the Enquiry tab was deliberately left
            // that way, because unlike Visit/Patient it had nothing to correct
            // the blank branch from.
            //
            // It does have something: the enquiry's OWN row in the "enquiries"
            // table, which this tab already fetches, carries the branch the
            // staff picked when saving. So the blank is filled in from there
            // and then judged by the SAME branch rule as everything else.
            // Nothing is loosened -- a row that turns out to belong to another
            // branch is still hidden. Only a record that had simply lost its
            // branch label comes back to the staff it belongs to.
            val rbFixed = if (rb.isBlank() && stage == "Inquiry") {
                branchFromEnquiries(preEnquiries, row.s("mobile"))
            } else rb
            val visible = allBranch || branchAllows(rbFixed, row.s("patientId"), branchFilter)
            // TK-REPORTED BUG FIX (2026-07-26, "Master-কে দেখাচ্ছে, Staff-কে
            // দেখাচ্ছে না"): a followups row whose OWN branch field is blank
            // was dropped right here for every staff -- while Master (All
            // Branch) saw it fine, and Chamber Attendance saw it fine too
            // (Chamber reads the "patients" table, where the branch IS
            // filled in). The real branch for these rows is filled in a few
            // steps below, from the "patients" table, and the Visit/Patient
            // tabs ALREADY re-apply this exact same branch check afterwards
            // with that corrected branch. So for those two tabs a blank
            // branch is kept for now and judged on its real value a moment
            // later -- a row whose branch turns out to belong to another
            // branch is still removed by that second check, so this cannot
            // leak another branch's data. The Enquiry tab has no such second
            // check, so it is deliberately left exactly as before.
            val blankBranchPendingLookup = rb.isBlank() && needsPatientJoin
            if (!visible && !blankBranchPendingLookup) continue
            val parsed = FollowUpModel.parse(row)
            /* 🔵🔒 V525: ঘরটা ফাঁকা হলে এনকোয়ারির সারি থেকে ভরা হয় (উপরের
               বড় নোট দ্রষ্টব্য)। ⛔ ভরা থাকলে বা Enquiry ট্যাব না হলে
               `parsed`-ই যায় — এক অক্ষরও বদলায় না। */
            val item = if (stage == "Inquiry" && parsed.timeType.isBlank()) {
                val tt = timeTypeFromEnquiries(preEnquiries, parsed.mobile)
                if (tt.isBlank()) parsed else parsed.copy(timeType = tt)
            } else parsed
            if (seenIds.add(item.id)) items.add(item)
        }

        // TK-REPORTED BUG FIX (2026-07-18): same dual-write sync gap as the
        // Visit Card fix above -- Enquiry save() also writes to TWO tables
        // ("enquiries" directly, and a "followups" stage=Inquiry row that
        // THIS tab actually reads). If only the followups write fails to
        // sync, the enquiry silently never appeared here even though it's
        // genuinely saved. Safety net: fetch "enquiries" rows still
        // stage=Inquiry (i.e. not yet converted/closed) and add any whose
        // mobile isn't already represented above.
        if (stage == "Inquiry") {
            val presentMobiles = items.map { identityKey(it.refId, it.mobile) }.toHashSet()  // 🔵 V518: পরিচয় ধরে (একজন রোগী হলে এটা মোবাইলই)
            // TK-FOUND RISK (2026-07-18), fixed before this could ever ship:
            // Reject/Incomplete (updateStatus above) only ever updates the
            // "followups" row's status -- it NEVER touches the "enquiries"
            // table's own "stage" field, which stays "Inquiry" forever.
            // Without this check, the safety-net below would wrongly
            // resurrect an already-REJECTED enquiry back into this tab
            // (its followups row is Cancelled/excluded from `items` above,
            // but its enquiries row still says stage=Inquiry). Excluding
            // any mobile with a Cancelled/Incomplete Inquiry-stage
            // followups row closes that gap.
            // TK-REPORTED BUG FIX (2026-07-24): this exclusion check used
            // plain fetchList(), which silently returns an EMPTY result on
            // any network failure -- indistinguishable from "genuinely zero
            // rejected enquiries right now". A failure here used to mean
            // the fallback below ran with zero exclusions and could wrongly
            // resurrect a rejected enquiry. Now uses fetchListOrNull(): on
            // a real failure (null), the entire enquiries-fallback below is
            // skipped instead of running unprotected -- undercounting (a
            // genuinely new enquiry not showing up for one load) is far
            // safer than wrongly resurrecting an already-rejected one, and
            // the main cloud query above already covers the normal case.
            val cancelledInquiryOrNull = preCancelledInquiry
            // TK-REPORTED, LIVE (27.07.2026 — AMIT KUMAR): same defect as the
            // Visit tab. This check only decides who the enquiries-table safety
            // net below may ADD back; on a failure the freshly-fetched list was
            // being thrown away and the last CACHED list returned instead, so a
            // brand-new record (which cannot be in an older cache) could stay
            // invisible load after load. The fresh list is already filtered to
            // "stage=Inquiry & status not in (Cancelled,Incomplete)", so it can
            // never contain a rejected enquiry -- skipping the optional safety
            // net below is enough, and it is what the next `if` already does.
            if (cancelledInquiryOrNull != null) {
                val rejectedMobiles = HashSet<String>()
                rejectedMobiles.addAll(closedInquiryMobiles)
                /* 📵🔒 V1245 (TK-র অনুমতি) — *"আর ফোন নয়"* করা নম্বরও
                   জাল আর ফিরিয়ে আনবে না। ⛔ কার্ডটা মোছে না, Reject-ও হয় না —
                   শুধু এই জাল ওটাকে **নতুন করে বানায় না**, তাই স্টাফের সিদ্ধান্ত
                   আর উল্টে যায় না। */
                rejectedMobiles.addAll(stoppedInquiryMobiles)
                for (i in 0 until cancelledInquiryOrNull.length()) {
                    val m = digits(cancelledInquiryOrNull.getJSONObject(i).s("mobile"))
                    if (m.isNotEmpty()) rejectedMobiles.add(m)
                }
                val enquiries = preEnquiries
                for (i in 0 until enquiries.length()) {
                    val row = enquiries.getJSONObject(i)
                    // 🚨🚨 TK-REPORTED, LIVE (29.07.2026 বিকেল ৩.৩৪, ছবিসহ · খাতার সারি B108
                    //     — ANIKUL HAQUE · +919126568192, কিশনগঞ্জের স্টাফ Reject করেছিলেন
                    //     সকাল ১০.৩১-এ, তবু বিকেলে কার্ডটা তালিকায় ছিল)।
                    //     TK: *"Reject করেছিল Kishanganj STAFF, কিন্তু এখন আবার কেন শো করছে?
                    //     ডিলিট করলে আবার কেন চলে আসে?"*
                    //
                    // **আসল কারণ (কোড ধরে, আন্দাজ নয়):** এই নিচের অংশটা এনকোয়ারি
                    // ট্যাবের **জাল** — `followups` সারিটা কোনো কারণে না পেলে সে
                    // সোজা `enquiries` টেবিল থেকে কার্ডটা আবার বানিয়ে দেয়। কিন্তু
                    // সে বাদ দিত **শুধু সেই নম্বরগুলো**, যাদের `followups`-এ
                    // `stage=Inquiry` **আর** `status=Cancelled/Incomplete` সারি
                    // পাওয়া যায়। ওই সারিটা যদি অন্য stage-এ থাকে, বা তৈরিই না
                    // হয়ে থাকে, বা নেট-এর জন্য না আসে — **জাল কার্ডটা ফিরিয়ে
                    // আনত**। আর `enquiries` সারির **নিজের `status` ঘরটা এখানে
                    // কোনোদিন দেখাই হত না**, অথচ ঘরটা টেবিলে আছে ও
                    // `restoreAndMove()` সেটাকে `Active` করে — অর্থাৎ ঘরটার মানে
                    // অ্যাপ আগে থেকেই জানে।
                    //
                    // **ওষুধ (তিন স্তর, খাতার সারি B108):** (১) এখানে সারির নিজের
                    // status দেখা হয় — বন্ধ করা এনকোয়ারি আর কখনো ফেরে না;
                    // (২) Reject করলে `enquiries` সারিতেও দাগ পড়ে
                    // (`FollowUpRepository.markEnquiryClosedByMobile`);
                    // (৩) ডিলিট করলেও ওই সারিতে দাগ পড়ে ও Restore-এ ফিরে আসে
                    // (`TrashHelper`)। তিনটের যে কোনো একটাই যথেষ্ট — একসঙ্গে
                    // তিনটে থাকায় এই দোষ আর ফিরতে পারে না।
                    //
                    // ⛔ নতুন এনকোয়ারিতে কিছু বদলায় না: নতুন সারির status ফাঁকা
                    //    বা `Active`, তাই সে আগের মতোই দেখাবে।
                    // ⛔ কোনো নতুন ক্লাউড-কল নেই — `status` ঘরটা এই সারিতেই ছিল।
                    val eStatus = row.s("status").trim()
                    if (eStatus.equals("Cancelled", true) || eStatus.equals("Incomplete", true) ||
                        eStatus.equals("Rejected", true) || eStatus.equals("Closed", true)) continue
                    val m = digits(row.s("mobile"))
                    if (m.isEmpty() || presentMobiles.contains(m) || rejectedMobiles.contains(m)) continue
                    val rb = row.s("branch")
                    // TK-ORDER (2026-07-25): branch only, no creator exception.
                    // 🔴🔒 V453 (20.08.2026): branchAllows() ব্যবহার করা হলো (আগে
                    // সরাসরি equals ছিল) — যাতে কমা-আলাদা একাধিক ব্রাঞ্চ (V453
                    // cross-branch exception) এখানেও ঠিকভাবে কাজ করে। একটাই
                    // ব্রাঞ্চ থাকলে ফলাফল আগের মতোই অভিন্ন।
                    val visible = allBranch || branchAllows(rb, row.s("patientId"), branchFilter)
                    if (!visible) continue
                    // 🚨 TK-REPORTED (29.07.2026 সন্ধ্যা ৭.০০, ছবিসহ · খাতার সারি B95):
                    // *"LAST CALL তারিখ অনেক জায়গায় মিসিং ছিল — এরকম যেন না থাকে।"*
                    //
                    // **আসল কারণ (কোড দেখে, আন্দাজ নয়):** এই কার্ডগুলো `enquiries`
                    // টেবিল থেকে বানানো হয় (যখন `followups` সারিটা তৈরি হয়নি)।
                    // কার্ডে LAST CALL আসে `lastCallDate` ঘর থেকে, না থাকলে
                    // `history`-র শেষ সারি থেকে (খাতার সারি B50)। কিন্তু এই
                    // বানানো সারিতে **দুটোর একটাও দেওয়া হত না**, তাই সব সময়
                    // `LAST CALL —` দেখাত — যদিও এনকোয়ারির দিনেই কথা হয়েছিল।
                    //
                    // **সমাধান:** আসল `followups` সারিতে যা বসত (নিচের `healRow`
                    // দেখুন — `EnquiryModel.buildFollowUpRow()`-এর হুবহু নকল),
                    // ঠিক সেই একটাই ইতিহাস-সারি এখানেও বসানো হলো — এনকোয়ারির
                    // তারিখ · সেই দিনের কথা · যিনি ধরেছিলেন তাঁর পরিচয়।
                    // ⛔ **নতুন কোনো ক্লাউড-কল নেই** — এই তথ্যগুলো ওই এনকোয়ারির
                    //    সারিতেই আগে থেকে আছে। ⛔ ডেটাবেসে কিছু লেখা হয় না।
                    // ⛔ তথ্য না থাকলে আগের মতোই `LAST CALL —` থাকে।
                    val fbHistory = org.json.JSONArray()
                    if (row.s("date").isNotBlank()) {
                        fbHistory.put(
                            JSONObject()
                                .put("date", row.s("date"))
                                /* ⏰🔒 V1005 (০৩.০৯.২০২৬) — আগে এখানে `isoNow()` বসত,
                                   অর্থাৎ **পর্দা খোলার মুহূর্তটাই** পুরনো কলের সময়
                                   হিসেবে দেখাত। এখন এনকোয়ারির নিজের সময়টাই বসে;
                                   না থাকলে তবেই এখনকার সময়। */
                                .put("time", row.s("createdAt").ifBlank { isoNow() })
                                .put("remark", row.s("remarks"))
                                .put("staff", row.s("receivedBy").ifBlank { row.s("createdBy") })
                        )
                    }
                    val fallback = JSONObject()
                        .put("id", row.s("id"))
                        .put("name", row.s("name"))
                        .put("mobile", row.s("mobile"))
                        .put("branch", rb)
                        .put("disease", row.s("disease"))
                        .put("stage", "Inquiry")
                        .put("lastRemark", row.s("remarks").ifBlank { "Enquiry (syncing…)" })
                        .put("nextFollow", row.s("nextFollow"))
                        .put("date", row.s("date"))
                        .put("callCount", row.optInt("callCount", 0))
                        /* 📵🔒 V1245 — **দ্বিতীয় জাল।** উপরের বাদ-দেওয়ার তালিকা
                           কোনো কারণে (নেট) ফাঁকা এলেও যেন থামানো কার্ড কল-তালিকায়
                           না ওঠে, তাই ঘরটা এখানেও বসে। আগে এটা **কখনোই বসত না** ⇒
                           ডিফল্ট false ⇒ TK-র অভিযোগ। */
                        .put("noMoreCalls", m in stoppedInquiryMobiles)
                        .put("history", fbHistory)
                    items.add(FollowUpModel.parse(fallback))
                    presentMobiles.add(m)
                    // TK-REPORTED BUG FIX (2026-07-24): same self-heal as
                    // the Visit-tab fallback below -- this fallback means
                    // the real "followups" row for this Enquiry was never
                    // found (dual-write gap that never caught up), which
                    // can get permanently stuck if it happened on a device
                    // whose local retry-queue doesn't have it either (e.g.
                    // a different staff/device than the one that saved it).
                    // Writes the missing row directly, matching
                    // EnquiryModel.buildFollowUpRow()'s own structure, so
                    // this stops recurring for this enquiry from now on.
                    // 🔴🔴 TK-REPORTED (04.08.2026, ছবিসহ — MD SHARIF UDDIN /
                    // +918731910202, Enquiry Reject List-এ একই মানুষ দুইবার):
                    // গভীরে যাচাই করে ধরা পড়েছে — উপরের দুটো তালিকা
                    // (rejectedMobiles · presentMobiles) কোনো একটাতে এই নম্বর
                    // ধরা না পড়লে (ক্যাশ/টাইমিং-এর সরু ফাঁকে) এই সেলফ-হিল
                    // সরাসরি একটা **নতুন** followups সারি বানিয়ে ফেলত —
                    // ঠিক যেন এটা তাজা এনকোয়ারি, যদিও আসল সারিটা আগেই
                    // Reject করা ছিল। স্টাফ পরে ওই নতুন কার্ডটাও Reject
                    // করলে "Rejected" দুইবার দেখা যেত — কারো ভুল ছাড়াই।
                    //
                    // ⛔ সমাধান: নতুন সারি বানানোর ঠিক আগে এই নম্বরে
                    // `followups`-এ **সত্যিই কিছু আছে কিনা** সরাসরি একবার
                    // জিজ্ঞেস করা হয় (stage/status নির্বিশেষে, শুধু id)।
                    // থাকলে — নতুন সারি বানানো হয় না (আগেরটাই আসল, এখানে
                    // দ্বিতীয়বার বানানোর দরকার নেই)। নেট-ব্যর্থতায় (null)ও
                    // নিরাপদ পথে — বানানো হয় না, ভুল ঝুঁকি নেওয়ার চেয়ে এই
                    // একটা কার্ড একবার না-দেখানো ভালো (পরের বার নেট ঠিক
                    // হলে ঠিকই দেখা যাবে, ডেটা হারায় না)। ⛔ কোনো ডিজাইন/
                    // টাকার হিসাব ছোঁয়া হয়নি — শুধু ভুতুড়ে সারি তৈরি বন্ধ।
                    val alreadyExists = try {
                        SupabaseClient.findByMobileOrNull("followups", m, "id", 1)
                    } catch (_: Throwable) { null }
                    if (alreadyExists == null || alreadyExists.length() > 0) continue
                    val healRow = JSONObject()
                        /* 🔗 V1005 — ফোন ও কম্পিউটার একই id বানায়
                           (`fu_inq_<enquiry id>`), তাই ডুপ্লিকেট সারি হয় না। */
                        .put("id", "fu_inq_" + row.s("id"))
                        .put("refId", row.s("id"))
                        .put("mobile", row.s("mobile"))
                        .put("name", row.s("name"))
                        .put("branch", rb)
                        .put("disease", row.s("disease"))
                        .put("address", row.s("address"))
                        .put("stage", "Inquiry")
                        .put("date", row.s("date"))
                        .put("registrationDate", row.s("date"))
                        .put("visitDate", row.s("date"))
                        .put("lastRemark", row.s("remarks"))
                        .put("nextFollow", row.s("nextFollow"))
                        .put("timeType", row.s("timeType"))
                        /* 📶🔴🔒 V1268 (TK-রিপোর্ট: *"কল করেছে তাও ওয়াইফাই সিগনাল
                           কেন ওঠেনি?"*) — এখানে `enquiries.callCount` কপি হত, আর
                           ওই ঘরটা **সব সময় ০** (`EnquiryModel.buildEnquiryRow`)।
                           নিচে `history`-ও ফাঁকা লেখা হয় ⇒ মেরামত করা সারিতে
                           সিগন্যাল **চিরকাল ফাঁকা** থাকত।
                           ⇒ এখন অন্তত **১** — হুবহু সেই নিয়ম যেটা
                             `EnquiryModel.buildFollowUpRow()`-এ TK-র ৩১.০৭.২০২৬-এর
                             নির্দেশে আগে থেকেই বসানো আছে (*"Enquiry Form Save
                             হওয়ার সাথে সাথেই তো একটা signal হতে হতো"*)।
                           ⛔ এনকোয়ারিতে গোনা বেশি থাকলে সেটাই থাকে, কমে না। */
                        .put("callCount", maxOf(row.optInt("callCount", 0), 1))
                        .put("status", "Active")
                        .put("history", JSONArray())
                        .put("createdBy", row.s("createdBy"))
                        .put("createdAt", row.s("createdAt").ifBlank { row.s("date") })
                        .put("updatedAt", row.s("createdAt").ifBlank { row.s("date") })
                    upsertWithHealRetry(healRow)
                }
            }
        }

        // ONE-NUMBER-ONE-SECTION rule (web parity, TK): a mobile that has already
        // advanced to a higher stage must NOT also appear in a lower tab. So the
        // moment an Enquiry is registered (creates a Patient/Visit record) it auto-
        // hides from the Enquiry tab; the moment a Visit takes Advance payment
        // (creates a Treatment record) it auto-hides from the Visit tab.
        val higherStages: String? = when (stage) {
            "Inquiry" -> "Patient,Treatment"
            "Patient" -> "Treatment"
            else -> null
        }
        if (higherStages != null && items.isNotEmpty()) {
            val higher = JSONArray()
            context?.let { ctx ->
                for (hs in higherStages.split(",")) {
                    val localHigher = LocalWorkflowStore(ctx).rowsForStage(hs)
                    for (i in 0 until localHigher.length()) higher.put(localHigher.getJSONObject(i))
                }
            }
            // ROOT-CAUSE FIX (2026-07-17): this used to only check the cloud when
            // the LOCAL cache had zero higher-stage rows at all -- but the local
            // cache only ever holds whatever this one device has recently done
            // offline, not every patient's real stage. So if the local cache had
            // even one unrelated entry, a patient who had genuinely advanced to a
            // higher stage on the cloud (e.g. Treatment/Incomplete) never got
            // excluded here, and kept showing in BOTH the lower tab (Enquiry) and
            // the higher one (Incomplete Patient) at the same time. Cloud is now
            // always checked too and merged with local, so this can't happen
            // regardless of what the local cache happens to contain.
            // TK-REPORTED BUG FIX (2026-07-24): a patient registered on ONE
            // device could keep showing as a duplicate in the Enquiry tab on
            // ANY OTHER device -- root cause was this exact check silently
            // treating "the network call to check higher stages just
            // failed" the same as "genuinely nobody has advanced yet" (plain
            // fetchList() returns an empty array either way). A registration
            // done entirely on a different device has nothing in THIS
            // device's local cache either, so a momentary connection hiccup
            // during this one check was enough to wrongly keep showing an
            // already-registered patient in the lower tab. Now uses
            // fetchListOrNull(): on a genuine failure (null), this whole
            // tab load falls back to whatever was last correctly cached
            // (same safe pattern already used for the "patients" fetch
            // above) instead of computing a fresh result with a silently
            // broken exclusion check -- never shows a wrong duplicate, at
            // worst shows slightly-stale (but previously correct) data.
            val cloudHigherOrNull = preHigher
            if (cloudHigherOrNull == null) {
                // 🚨 TK-REPORTED, LIVE (27.07.2026 — AMIT KUMAR): second place
                // with the same defect as the Visit-tab safety net below. This
                // check only works out who has ALREADY advanced to a higher
                // stage (so they don't show in two tabs at once). When it failed
                // on a weak line, the whole freshly-fetched list was thrown away
                // and the last CACHED list returned -- so a patient registered
                // minutes ago on another phone could never appear here.
                //
                // Instead of falling back to stale data, work the exclusion out
                // from the payments table that this tab has ALREADY downloaded:
                // a Visit becomes a Treatment exactly when a real (non Visit-Fee,
                // non attendance) payment is taken, so those mobiles are the
                // higher-stage ones. Only if payments ALSO failed to load is
                // there nothing left to reason with, and only then does the old
                // cached-list behaviour still apply.
                val paymentsForHigher = prePayments
                if (paymentsForHigher == null) {
                    val cached = cachedPlusOwn(stage, branchFilter)
                    if (!cached.isNullOrEmpty()) return cached
                } else {
                    for (i in 0 until paymentsForHigher.length()) {
                        val pay = paymentsForHigher.optJSONObject(i) ?: continue
                        val payType = pay.optString("payType", "")
                        if (payType == "visit_fee" || payType == "attendance_mark" || PaymentModel.isMarkerOnlyRow(payType)) continue   /* 🔴🔒 V1301 (তালিকা ৪১৩): চিহ্ন-সারি (bill_edit · chamber_expected · attendance_mark) কখনো টাকা নয় */
                        if (pay.optDouble("amount", 0.0) <= 0.0) continue
                        val pm = digits(pay.s("mobile"))
                        if (pm.isNotEmpty()) higher.put(JSONObject().put("mobile", pm))
                    }
                }
            } else {
                for (i in 0 until cloudHigherOrNull.length()) higher.put(cloudHigherOrNull.getJSONObject(i))
            }
            /* 🔵🔒 V518: চাবি এখন "পরিচয়" — আলাদা রোগী হলে তাঁর নিজের আইডি,
               নইলে আগের মতোই মোবাইল। ⇒ স্বামী Treatment-এ চলে গেলে স্ত্রী
               আর ভুল করে Visit ট্যাব থেকে বাদ পড়েন না।
               ⛔ একজন রোগীর ক্ষেত্রে দুটো চাবিই মোবাইল — আচরণ অবিকল আগের। */
            val higherKeys = HashSet<String>()
            for (i in 0 until higher.length()) {
                val h = higher.getJSONObject(i)
                val hm = digits(h.s("mobile"))
                if (hm.isNotEmpty()) higherKeys.add(hm)
                /* উঁচু-ধাপের সারিটা যদি ঘোষিত আলাদা রোগীর হয়, তাঁর নিজের
                   চাবিটাও ধরা হয় — নইলে তিনি দুই ট্যাবে একসঙ্গে থাকতেন। */
                val hRef = h.s("refId")
                if (hRef.isNotBlank() && isDeclaredSeparatePatientId(hRef, hm)) higherKeys.add(hRef)
                /* payments থেকে আসা fallback সারিতে `refId` থাকে না, কিন্তু
                   `patientId` ঘরে রোগীর row id থাকে — সেটাও চাবি। */
                val hPid = h.s("patientId")
                if (hPid.isNotBlank() && isDeclaredSeparatePatientId(hPid, hm)) higherKeys.add(hPid)
            }
            if (higherKeys.isNotEmpty()) {
                val kept = items.filter { identityKey(it.refId, it.mobile) !in higherKeys }
                items.clear(); items.addAll(kept)
            }
        }

        // একই ট্যাবে একই নম্বর একবারই — সবচেয়ে নতুন রেকর্ডটা রাখো, বাকিগুলো বাদ।
        run {
            val seen = HashSet<String>()
            /* 🔵🔒 V518: "একই নম্বর একবারই" → "একই **রোগী** একবারই"।
               ⛔ একজন রোগীর ক্ষেত্রে চাবিটা মোবাইলই — নিয়ম হুবহু আগের মতোই। */
            val dedup = ArrayList<FollowUpItem>()
            /* 📵🔒 V1283 (তালিকা সারি ৪০৬) — একই রোগীর **যেকোনো** সারি "আর কল নয়"
               হলে যে সারিটা কার্ড হিসেবে টিকল সেটাও থামানো ধরা হয়। নইলে চিহ্নটা
               জোড়া সারিতে থেকে যেত আর কার্ড/গোনা অন্যটা দিয়ে চলত (SQL-এ মাপা)।
               ⛔ কোন সারি টিকবে — সেই নিয়ম (নতুন recordDate) এক অক্ষরও বদলায়নি। */
            val stoppedKeys = HashSet<String>()
            for (r in items) if (r.noMoreCalls) {
                val k = identityKey(r.refId, r.mobile)
                if (k.isNotEmpty()) stoppedKeys.add(k)
            }
            items.sortedByDescending { it.recordDate }.forEach { r ->
                val m = identityKey(r.refId, r.mobile)
                if (m.isNotEmpty() && seen.contains(m)) return@forEach
                if (m.isNotEmpty()) seen.add(m)
                dedup.add(if (!r.noMoreCalls && m.isNotEmpty() && m in stoppedKeys) r.copy(noMoreCalls = true) else r)
            }
            items.clear(); items.addAll(dedup)
        }

        // Patient tab (Treatment stage) AND Visit tab (Patient stage): join the
        // real patientId/address/age/sex/disease from the "patients" table by
        // mobile, matching the WebView's mergeFollow. ROOT CAUSE FIX (TK found,
        // 2026-07-15): this join used to run ONLY for the Treatment stage, so
        // Visit-tab cards never got a real Patient ID and silently fell back to
        // the internal "refId" (an internal linking key, e.g. "pat_f76c..."),
        // which is not meant to ever be shown to a person. Now both tabs join.
        if (stage == "Treatment" || stage == "Patient") {
            val patients = prePatients
            // TK-REPORTED BUG FIX (2026-07-23): if this specific fetch fails
            // (bad connection), "patients" comes back as an empty JSONArray
            // with NO error surfaced (SupabaseClient.fetchList() swallows
            // exceptions into an empty result) -- previously this silently
            // fell through and OVERWROTE every item's bill/paid/patientId
            // with 0/blank via the maxOf(...) merge below, then CACHED that
            // wrong zeroed result (fetchTab always ends with
            // saveCachedTab(...)), so the wrong ₹0 kept showing on every
            // later open too, not just this one -- exactly TK's report
            // (Staff's device on a weak connection stayed stuck on ₹0 even
            // after repeated re-opens). A real clinic with Treatment/Patient-
            // stage entries always has SOME "patients" rows, so an empty
            // result here almost always means the fetch failed, not that
            // there are genuinely zero patients. When that happens, fall
            // back to whatever was last successfully cached for this exact
            // tab (same stage+branch) instead of computing/caching a
            // degraded result -- "no new payment confirmed yet, so keep
            // showing the last known real numbers" per TK's instruction.
            // TK-REPORTED, LIVE (27.07.2026 — AMIT KUMAR): keeping the last
            // known real ₹ numbers is right (TK's instruction above), but
            // returning ONLY that cached list also hid anyone too new to be in
            // it -- the patient registered minutes ago. plusMissing() keeps the
            // cached rows exactly as they are and simply appends the
            // freshly-fetched patients the cache does not know about yet. A
            // just-registered patient genuinely has no bill and no payment, so
            // showing ₹0 for them is correct, not degraded.
            if (patients.length() == 0) {
                val cached = cachedPlusOwn(stage, branchFilter)
                if (!cached.isNullOrEmpty()) return plusMissing(cached, items)
            }
            // TK-REPORTED BUG FIX (2026-07-24): same protection as the
            // "patients" fetch just above (section 87) -- a failed payments
            // fetch used to silently look identical to "genuinely zero
            // payments", making every Paid/Due number wrong for this whole
            // tab load. Now falls back to cache on a real failure too.
            val paymentsOrNull = prePayments
            if (paymentsOrNull == null) {
                val cached = cachedPlusOwn(stage, branchFilter)
                if (!cached.isNullOrEmpty()) return plusMissing(cached, items)
            }
            val payments = paymentsOrNull ?: JSONArray()
            val billByMobile = HashMap<String, Double>()
            val idByMobile = HashMap<String, String>()
            val patientCodeByMobile = HashMap<String, String>()
            val addressByMobile = HashMap<String, String>()
            val ageByMobile = HashMap<String, String>()
            val sexByMobile = HashMap<String, String>()
            val diseaseByMobile = HashMap<String, String>()
            val nameByMobile = HashMap<String, String>()
            val branchByMobile = HashMap<String, String>()
            val createdByMobileMap = HashMap<String, String>()
            /* 🆕🔒 V851 (৩০.০৮.২০২৬, TK-অনুমোদিত) — TK: "যেগুলো রেজিস্ট্রেশন করা
               হয়েছে সেখানে লিখতে হবে কত তারিখে রেজিস্ট্রেশন হয়েছে এবং কে
               রেজিস্ট্রেশন করেছিল"। V850-এ Draft-এর কার্ডে বসেছে, এবার
               Follow-up পর্দার নিজের কার্ডেও (নিয়ম ৬.২ — একই দোষ সব জায়গায়)।
               ⛔ দুটো ঘরই `PATIENT_COLS`-এ **আগে থেকেই** আসে ⇒ নতুন কোনো
                  ক্লাউড-পড়া নেই, egress এক বিন্দুও বাড়ে না। */
            val regDateByMobile = HashMap<String, String>()
            val regByMobile = HashMap<String, String>()
            // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due"
            // workflow -- a patient Master has approved for this must also
            // stop showing in the active Treatment call/reminder tab, same
            // as a genuinely fully-paid patient already does below. Real
            // Due is untouched; only this tab's visibility is affected.
            val completeApprovedByMobile = HashMap<String, String>()
            // 🔴 TK-নির্দেশ (02.08.2026, B302.1) — completeApprovedByMobile-এর
            // হুবহু একই প্যাটার্ন, শুধু Refund-manually-restored-এর জন্য।
            val refundRestoredByMobile = HashMap<String, String>()
            // TK-REPORTED BUG FIX (2026-07-25): when TWO "patients" rows
            // exist for the same mobile (a duplicate registration), this
            // loop used to blindly overwrite every field with whichever row
            // came LAST in the list -- Supabase doesn't guarantee order, so
            // a genuine ₹20,000-bill patient could show Bill ₹0/Due ₹0 here
            // if their duplicate (broken/incomplete) row happened to be
            // fetched after the real one, exactly what TK's photo-proof
            // showed. Now the row with the real (non-zero) bill always wins
            // for THIS mobile, and every field (id/patientId/address/etc.)
            // is taken from that SAME winning row together -- never mixed
            // field-by-field from two different duplicate rows.
            // TK-REQUESTED (2026-07-27), ধাপ ৩খ — শেষ পর্দা (Follow-up): the loop
            // below already made sure every field comes from ONE row (never mixed
            // from two duplicates), but the row it picked was "the one with a real
            // bill" only. The rest of the app -- the money screen, Patient Details,
            // the Report Card, Draft, Chamber and Search -- uses the full shared
            // rule: CURRENT BRANCH first, then the row with a real bill, then the
            // first row. So a patient with rows in two branches could still show a
            // different Bill / Patient ID / address here than on the payment
            // screen. Same single rule now, in the same one function. Grouping
            // first also means the winning row is chosen from ALL of this person's
            // rows at once instead of depending on the order Supabase returned.
            // With a single row per person (the normal case) every value below is
            // exactly what it was before.
            /* 🔵🔴🔒 V518: ঘোষিত আলাদা রোগীরা (স্বামী/স্ত্রী) নিচের মোবাইল-ম্যাপে
               ঢোকেন **না** — তাঁদের তথ্য নিজের আইডি ধরে আলাদা রাখা হয়।
               ⛔ নইলে দুটো ক্ষতি হত: (ক) স্ত্রীর কার্ডে স্বামীর বিল/Patient ID
                  বসত, আর (খ) `pickPatientRow` স্ত্রীর সারিটাকেই বেছে নিয়ে
                  **স্বামীর** কার্ডেও ভুল তথ্য বসিয়ে দিতে পারত।
               ⛔ একজন রোগীর ক্ষেত্রে এই তালিকা ফাঁকা — নিচের সব হিসাব
                  হুবহু আগের মতোই চলে, `pickPatientRow`-ও অপরিবর্তিত। */
            val declaredById = HashMap<String, JSONObject>()
            /** যে নম্বরে অন্তত একজন ঘোষিত আলাদা রোগী আছেন — সেখানে মোবাইল-ভিত্তিক
             *  টাকার সেফটি-নেট আর নির্ভরযোগ্য নয় (নিচে দেখুন)। */
            val mobilesWithDeclared = HashSet<String>()
            val followUpRowsByMobile = HashMap<String, JSONArray>()
            for (i in 0 until patients.length()) {
                val p = patients.getJSONObject(i)
                val m = digits(p.s("mobile"))
                if (m.isEmpty()) continue
                val rid = p.s("id")
                if (isDeclaredSeparatePatientId(rid, m)) { declaredById[rid] = p; mobilesWithDeclared.add(m); continue }
                followUpRowsByMobile.getOrPut(m) { JSONArray() }.put(p)
            }
            for ((m, rows) in followUpRowsByMobile) {
                val p = PatientIdentity.pickPatientRow(rows, branchFilter.orEmpty()) ?: continue
                billByMobile[m] = p.optDouble("bill", 0.0)
                idByMobile[m] = p.s("id")
                patientCodeByMobile[m] = p.s("patientId")
                addressByMobile[m] = p.s("address")
                ageByMobile[m] = p.s("age")
                sexByMobile[m] = p.s("sex")
                diseaseByMobile[m] = p.s("disease").ifBlank { p.s("diagnosis") }
                nameByMobile[m] = p.s("name")
                branchByMobile[m] = p.s("branch")
                createdByMobileMap[m] = p.s("createdBy")
                // 🆕 V851 — রেজিস্ট্রেশনের তারিখ ও কে করেছিলেন।
                regDateByMobile[m] = p.s("registrationDate").ifBlank { p.s("date") }.take(10)
                regByMobile[m] = p.s("registeredBy").ifBlank { p.s("createdBy") }
                if (p.s("completeApprovedBy").isNotBlank()) completeApprovedByMobile[m] = p.s("completeApprovedBy")
                if (p.s("refundRestoredBy").isNotBlank()) refundRestoredByMobile[m] = p.s("refundRestoredBy")
            }
            val paidByPid = HashMap<String, Double>()
            // TK-REPORTED BUG FIX (2026-07-26, "Chamber-এ দেখাচ্ছে, Patient
            // তালিকায় কোথাও নেই"): the Treatment safety-net below only
            // treated a patient as "has really paid" when a payment row's
            // patientId matched that patients-row's internal id. When a
            // second patients row exists for the same person (a duplicate
            // registration, or findOrMakePatient() creating one because the
            // mobile lookup momentarily failed), the payment is filed under
            // the OTHER row's id -- so the safety net saw paid = 0, skipped
            // this patient, and they showed nowhere in Follow-up even though
            // Chamber Attendance (which reads "payments"/"patients"
            // directly) showed them plainly. Draft's own lists already index
            // payments by MOBILE for exactly this reason; indexing both ways
            // here makes the two agree. This map is used ONLY to decide
            // whether a MISSING patient gets ADDED back -- it can never hide
            // anyone who is currently visible.
            val paidByMobileFallback = HashMap<String, Double>()
            // 🔴🔴 TK-REPORTED (01.08.2026, ছবিসহ — KHADIMUL ISLAM, Follow-up
            // Due ₹17,000 vs Timeline-এর সঠিক ₹19,000): V238-এর `paidEffect`
            // (উপরে) Refund ঠিকভাবে বিয়োগ করে, কিন্তু নিচের সেফটি-নেট
            // `maxOf(cloudPaid, paidByMobile, items[idx].paid)`-এর তৃতীয় সংখ্যা
            // (`items[idx].paid`) আসে এই ফোনে **স্থানীয়ভাবে জমা রাখা** followups
            // row থেকে (LocalWorkflowStore.promoteToTreatment() — শুধু নতুন
            // Payment-এ **যোগ** করে, Refund কখনো ছোঁয় না)। তাই Refund হওয়ার পরেও
            // এই পুরনো (বড়, ভুল) সংখ্যাটাই `maxOf`-এ জিতে যাচ্ছিল। এই সেট দিয়ে
            // চিহ্নিত করা হচ্ছে কার approved refund আছে, যাতে নিচে তাদের জন্য এই
            // ভুল floor বাদ দেওয়া যায় — বাকি সবার জন্য (refund নেই) সেফটি-নেট
            // আগের মতোই অক্ষত।
            val hasApprovedRefundByPid = HashSet<String>()
            val hasApprovedRefundByMobile = HashSet<String>()
            for (i in 0 until payments.length()) {
                val pay = payments.getJSONObject(i)
                val pid = pay.s("patientId")
                val payType = pay.optString("payType", "")
                if (payType == "visit_fee" || payType == "attendance_mark" || PaymentModel.isMarkerOnlyRow(payType)) continue   /* 🔴🔒 V1301 (তালিকা ৪১৩): চিহ্ন-সারি (bill_edit · chamber_expected · attendance_mark) কখনো টাকা নয় */
                // V238: Follow-up Paid/Due must use the same net-payment rule as
                // Payment Details: approved refunds subtract; pending/rejected
                // refunds have no effect. The refund row remains untouched.
                val paidEffect = when {
                    PaymentModel.isApprovedRefund(pay) -> -pay.optDouble("amount", 0.0)
                    PaymentModel.isRefundRow(pay) -> 0.0
                    else -> pay.optDouble("amount", 0.0)
                }
                if (PaymentModel.isApprovedRefund(pay)) {
                    if (pid.isNotEmpty()) hasApprovedRefundByPid.add(pid)
                    val rmob = digits(pay.s("mobile"))
                    if (rmob.isNotEmpty()) hasApprovedRefundByMobile.add(rmob)
                }
                if (pid.isNotEmpty()) paidByPid[pid] = (paidByPid[pid] ?: 0.0) + paidEffect
                val pmob = digits(pay.s("mobile"))
                if (pmob.isNotEmpty()) paidByMobileFallback[pmob] = (paidByMobileFallback[pmob] ?: 0.0) + paidEffect
            }
            // TK-REPORTED BUG FIX (2026-07-18): Registration writes to TWO
            // separate tables -- "patients" (read directly by Chamber
            // Attendance) and "followups" stage=Patient (read by THIS Visit
            // tab). If only the followups write fails to sync (e.g. a bad
            // connection at save time) while the patients write succeeds,
            // that patient showed up in Chamber Attendance but silently
            // never appeared here -- with no error anywhere. Safety net:
            // for the Visit tab specifically, any patient row with no
            // matching entry already in `items` (and not already a
            // Treatment-stage patient, per the higher-stage exclusion
            // above) gets a fallback entry synthesized directly from the
            // patients row, so a patient can never be invisible here.
            if (stage == "Patient") {
                val presentMobiles = items.map { identityKey(it.refId, it.mobile) }.toHashSet()  // 🔵 V518: পরিচয় ধরে (একজন রোগী হলে এটা মোবাইলই)
                // TK-FOUND RISK (2026-07-18), fixed before this could ever
                // ship: this fallback only checked the "patients" table, so
                // it could wrongly resurrect (a) an already-Rejected visit
                // (Reject only updates the followups row, the patients row
                // is untouched) or (b) a patient who has ALREADY correctly
                // advanced to Treatment stage (their Patient-stage
                // followups row is gone/excluded, but they still exist in
                // "patients"). Both excluded below before adding anyone.
                // TK-REPORTED BUG FIX (2026-07-24): both exclusion checks
                // below used plain fetchList() (silently empty on a real
                // network failure, indistinguishable from "genuinely none")
                // -- same bug class just found and fixed for the Enquiry
                // tab. If either fails now (fetchListOrNull returns null),
                // skip this entire patients-table fallback block instead of
                // running it with a silently incomplete exclusion set --
                // undercounting one load is far safer than wrongly
                // resurrecting an already-Rejected or already-advanced-to-
                // Treatment patient back into this tab.
                val rejectedVisitsOrNull = preRejectedVisits
                val treatmentStageOrNull = preTreatmentStage
                // TK-REPORTED BUG FIX (2026-07-25, Kishanganj staff): if either
                // check above failed on a bad connection, this whole safety net
                // was skipped -- so a just.registered patient whose followups
                // row had not synced yet DISAPPEARED from the Visit tab for that
                // load, then came back on the next one ("the patient I
                // registered first is no longer showing"). Skipping is still the
                // right call (never resurrect a rejected patient), but showing
                // the last known good list is far better than showing one that
                // is silently missing people. Same cache.fallback pattern this
                // file already uses for the payments/patients fetches.
                // 🚨 TK-REPORTED, LIVE (27.07.2026 — AMIT KUMAR / KNE-27072026-001,
                // registered by KNE-LAXMI): "রেজিস্ট্রেশন করেছি, রোগীর পাতায় ও
                // টাকায় দেখাচ্ছে, কিন্তু ভিজিট সেকশনে আসেনি।"
                //
                // ROOT CAUSE (here): the two checks above (rejected visits /
                // Treatment-stage rows) exist ONLY to decide who the
                // patients-table safety net below may ADD back. But when either
                // of them failed on a weak line, this threw away the perfectly
                // good list that had just been fetched and returned the LAST
                // CACHED list instead -- and a patient registered minutes ago on
                // ANOTHER phone is, by definition, not in this phone's cache. So
                // the newest patient was the exact one who could never appear,
                // load after load, while Chamber/Patient Details/Payment (which
                // read the patients + payments tables) showed him plainly.
                //
                // The fresh list cannot contain a rejected patient -- the cloud
                // query itself is "stage=Patient & status not in
                // (Cancelled,Incomplete)", and the local merge only takes Active
                // rows. So when a check fails we now simply SKIP the optional
                // safety net below (the `if` that follows already does exactly
                // that) and show the real, freshly-fetched list. Nothing is
                // resurrected; nobody is hidden.
                if (rejectedVisitsOrNull != null && treatmentStageOrNull != null) {
                val excludeMobiles = HashSet<String>()
                for (i in 0 until rejectedVisitsOrNull.length()) {
                    val m = digits(rejectedVisitsOrNull.getJSONObject(i).s("mobile"))
                    if (m.isNotEmpty()) excludeMobiles.add(m)
                }
                // TK-REPORTED BUG FIX (2026-07-22): the cloud check above misses a
                // just-rejected Visit that hasn't finished syncing yet, so this
                // fallback (built from the untouched "patients" table) could bring
                // a just-rejected patient right back into the Visit list. Merge in
                // the local pending Cancelled/Incomplete rows too, so the
                // exclusion applies immediately, not just after the next sync.
                context?.let { ctx ->
                    val localRejected = LocalWorkflowStore(ctx).rejectedRowsForStage("Patient")
                    for (i in 0 until localRejected.length()) {
                        val m = digits(localRejected.getJSONObject(i).s("mobile"))
                        if (m.isNotEmpty()) excludeMobiles.add(m)
                    }
                }
                for (i in 0 until treatmentStageOrNull.length()) {
                    val m = digits(treatmentStageOrNull.getJSONObject(i).s("mobile"))
                    if (m.isNotEmpty()) excludeMobiles.add(m)
                }
                val localPatientRows = context?.let { LocalWorkflowStore(it).rowsForStage("Patient") } ?: JSONArray()
                val localByMobile = HashMap<String, JSONObject>()
                for (i in 0 until localPatientRows.length()) {
                    val lr = localPatientRows.getJSONObject(i)
                    val lm = digits(lr.s("mobile"))
                    if (lm.isNotEmpty()) localByMobile[lm] = lr
                }
                for (i in 0 until patients.length()) {
                    val p = patients.getJSONObject(i)
                    val m = digits(p.s("mobile"))
                    /* 🔵 V518: এই রোগীর নিজের চাবি — ঘোষিত আলাদা রোগী হলে তাঁর
                       আইডি, নইলে মোবাইল। ⇒ স্বামী তালিকায় আছেন বলে স্ত্রী আর
                       বাদ পড়েন না। ⛔ একজন রোগী হলে চাবিটা মোবাইলই — আচরণ অবিকল আগের। */
                    val pKey = identityKey(p.s("id"), m)
                    if (m.isEmpty() || presentMobiles.contains(pKey) || excludeMobiles.contains(m)) continue
                    val pBranch = p.s("branch")
                    // TK-ORDER (2026-07-25): branch only, no creator exception.
                    val visible = allBranch || branchAllows(pBranch, p.s("patientId"), branchFilter)
                    if (!visible) continue
                    // TK-REPORTED BUG FIX (2026-07-19): use the REAL locally-
                    // cached remark/date when this patient's followups row is
                    // sitting un-synced on the device, instead of always
                    // showing the generic placeholder.
                    val local = localByMobile[m]
                    val fallback = JSONObject()
                        .put("id", local?.s("id") ?: p.s("id"))
                        .put("name", p.s("name"))
                        .put("mobile", p.s("mobile"))
                        .put("branch", pBranch)
                        .put("disease", p.s("disease").ifBlank { p.s("diagnosis") })
                        .put("stage", "Patient")
                        .put("lastRemark", local?.s("lastRemark")?.ifBlank { null } ?: "Registered (syncing…)")
                        .put("nextFollow", local?.s("nextFollow") ?: "")
                        .put("date", local?.s("date")?.ifBlank { null } ?: p.s("registrationDate").ifBlank { p.s("date") })
                        .put("patientId", p.s("patientId"))
                        .put("address", p.s("address"))
                        .put("age", p.s("age"))
                        .put("sex", p.s("sex"))
                        .put("photo", p.s("photo"))
                        // 🔒 খাতার সারি B95 (TK, 29.07.2026): এনকোয়ারির মতোই এখানেও
                        // LAST CALL সব সময় `—` দেখাত, কারণ এই বানানো সারিতে
                        // `lastCallDate` বা `history` কিছুই দেওয়া হত না।
                        // এখন **এই ফোনে জমা থাকা আসল সারিটা থাকলে** তার কল-তারিখ ও
                        // ইতিহাস এখানেও বসে — অর্থাৎ যা সত্যি তাই দেখায়।
                        // ⛔ ফোনে সারিটা না থাকলে আগের মতোই `—` থাকে, বানানো কিছু
                        //    দেখানো হয় না। ⛔ নতুন কোনো ক্লাউড-কল নেই — এই তথ্য
                        //    ফোনের নিজের খাতাতেই আছে। ⛔ ডেটাবেসে কিছু লেখা হয় না।
                        .put("lastCallDate", local?.s("lastCallDate") ?: "")
                        .put("history", local?.optJSONArray("history") ?: org.json.JSONArray())
                    items.add(FollowUpModel.parse(fallback))
                    presentMobiles.add(m)
                    // TK-REPORTED BUG FIX (2026-07-24): "Registered
                    // (syncing…)" was meant to be a few-seconds-to-minutes
                    // transient state (BottomNav's retry queue finishing the
                    // job) -- TK found it stuck for HOURS on patients this
                    // device never registered itself, so there is nothing
                    // local to retry FROM (the pending-queue is per-device,
                    // SharedPreferences-based, and simply doesn't exist on a
                    // device that didn't do the original save). Self-heal:
                    // whenever this fallback is used AND there is no local
                    // trace either (local == null -- the exact "genuinely
                    // missing everywhere" signal), write the real followups
                    // row directly right here, best-effort, so the NEXT load
                    // (this device or any other) finds it via the normal
                    // path and this permanently stops recurring for this
                    // patient. Never overwrites anything -- this row doesn't
                    // exist yet by definition (that's why we're here).
                    if (local == null) {
                        // 🔴🔴 TK-REPORTED (04.08.2026 — একই ক্লাসের বাগ প্রজেক্টে
                        // আর কোথায় আছে খুঁজে দেখা): "local == null" মানে শুধু
                        // এই ফোনে কিছু নেই — ক্লাউডে আসল সারিটা থাকতেই পারে
                        // (অন্য ফোনে/আগে তৈরি হয়েছে)। এখানেও নতুন সারি
                        // বানানোর আগে ক্লাউডে সরাসরি একবার জিজ্ঞেস করা হলো —
                        // থাকলে দ্বিতীয় সারি বানানো হয় না।
                        val alreadyExistsV = try {
                            SupabaseClient.findByMobileOrNull("followups", m, "id", 1)
                        } catch (_: Throwable) { null }
                        if (alreadyExistsV != null && alreadyExistsV.length() == 0) {
                        val healRow = JSONObject()
                            /* 🔗 V1005 — কম্পিউটারের self-heal-এর হুবহু একই id,
                               তাই দুই যন্ত্রে দুটো সারি হয় না। */
                            .put("id", "fu_pat_" + p.s("id"))
                            .put("refId", p.s("id"))
                            .put("patientId", p.s("patientId"))
                            .put("mobile", p.s("mobile"))
                            .put("name", p.s("name"))
                            .put("branch", pBranch)
                            .put("disease", p.s("disease").ifBlank { p.s("diagnosis") })
                            .put("address", p.s("address"))
                            .put("age", p.s("age"))
                            .put("sex", p.s("sex"))
                            .put("stage", "Patient")
                            .put("date", p.s("registrationDate").ifBlank { p.s("date") })
                            .put("registrationDate", p.s("registrationDate").ifBlank { p.s("date") })
                            .put("visitDate", p.s("visitDate").ifBlank { p.s("registrationDate").ifBlank { p.s("date") } })
                            .put("lastRemark", "Registered patient / Visit created")
                            .put("nextFollow", "")
                            .put("callCount", 0)
                            .put("status", "Active")
                            .put("history", JSONArray())
                            .put("createdBy", p.s("createdBy"))
                            .put("createdAt", p.s("createdAt"))
                            .put("updatedAt", p.s("createdAt"))
                        upsertWithHealRetry(healRow)
                        }
                    }
                }
                }
            }

            // TK-REPORTED BUG FIX (2026-07-18): same dual-write gap, this
            // time on the Advance/Treatment Payment step (PaymentRepository.
            // kt) — the payment amount itself is always saved directly to
            // "payments" (so the money is never lost), but the SEPARATE
            // write that bumps this patient's followups row to
            // stage=Treatment is its own network call and can fail
            // independently. Safety net: any patient with a genuine
            // Treatment/Advance payment recorded (paidByPid > 0) but not
            // yet showing here gets a fallback Treatment entry too.
            if (stage == "Treatment") {
                val presentMobiles = items.map { identityKey(it.refId, it.mobile) }.toHashSet()  // 🔵 V518: পরিচয় ধরে (একজন রোগী হলে এটা মোবাইলই)
                // TK-FOUND RISK (2026-07-18): a patient marked "Incomplete"
                // (moved to Draft's Incomplete Patient list) genuinely has
                // paidByPid > 0 (they DID pay something before treatment
                // was marked incomplete) -- without this exclusion, this
                // fallback would wrongly bring them back into this tab.
                // TK-REPORTED BUG FIX (2026-07-24): same silent-failure bug
                // class as the Enquiry/Visit tab fixes above -- plain
                // fetchList() here could silently return empty on a real
                // network failure, wrongly resurrecting an Incomplete
                // patient. Now skips this whole fallback block on a genuine
                // failure (fetchListOrNull returns null) instead.
                val incompleteTreatmentOrNull = preIncompleteTreatment
                // TK-REPORTED, LIVE (27.07.2026 — AMIT KUMAR): same defect as
                // the Visit/Enquiry tabs. This check only decides who the
                // safety net below may ADD back; on a failure the fresh list
                // was thrown away and the last CACHED list returned, hiding
                // anything too new to be in that cache. The fresh list already
                // excludes Cancelled/Incomplete rows at the query itself, so
                // skipping the optional safety net is enough.
                if (incompleteTreatmentOrNull != null) {
                val incompleteMobiles = HashSet<String>()
                for (i in 0 until incompleteTreatmentOrNull.length()) {
                    val m = digits(incompleteTreatmentOrNull.getJSONObject(i).s("mobile"))
                    if (m.isNotEmpty()) incompleteMobiles.add(m)
                }
                // TK-REPORTED BUG FIX (2026-07-22): same fix as the Visit-tab
                // fallback above -- also exclude locally-pending Cancelled/
                // Incomplete rows so a just-marked-Incomplete Treatment patient
                // can't be resurrected here before the cloud catches up.
                context?.let { ctx ->
                    val localIncomplete = LocalWorkflowStore(ctx).rejectedRowsForStage("Treatment")
                    for (i in 0 until localIncomplete.length()) {
                        val m = digits(localIncomplete.getJSONObject(i).s("mobile"))
                        if (m.isNotEmpty()) incompleteMobiles.add(m)
                    }
                }
                for (i in 0 until patients.length()) {
                    val p = patients.getJSONObject(i)
                    val m = digits(p.s("mobile"))
                    val pKey = identityKey(p.s("id"), m)   // 🔵 V518 — উপরের একই নিয়ম
                    if (m.isEmpty() || presentMobiles.contains(pKey) || incompleteMobiles.contains(m)) continue
                    val pid = p.s("id")
                    // See the paidByMobileFallback comment above: id-match
                    // first (exactly as before), mobile-match only as a
                    // wider net so a genuinely-paid patient can never fall
                    // through this gap and become invisible everywhere.
                    val paid = maxOf(paidByPid[pid] ?: 0.0, paidByMobileFallback[m] ?: 0.0)
                    if (paid <= 0.0) continue
                    val pBranch = p.s("branch")
                    // TK-ORDER (2026-07-25): branch only, no creator exception.
                    val visible = allBranch || branchAllows(pBranch, p.s("patientId"), branchFilter)
                    if (!visible) continue
                    val fallback = JSONObject()
                        .put("id", p.s("id"))
                        .put("name", p.s("name"))
                        .put("mobile", p.s("mobile"))
                        .put("branch", pBranch)
                        .put("disease", p.s("disease").ifBlank { p.s("diagnosis") })
                        .put("stage", "Treatment")
                        .put("lastRemark", "Treatment payment / Advance received (syncing…)")
                        .put("nextFollow", "")
                        .put("date", p.s("registrationDate").ifBlank { p.s("date") })
                        .put("patientId", p.s("patientId"))
                        .put("address", p.s("address"))
                        .put("age", p.s("age"))
                        .put("sex", p.s("sex"))
                        .put("photo", p.s("photo"))
                        .put("bill", p.optDouble("bill", 0.0))
                        .put("paid", paid)
                    items.add(FollowUpModel.parse(fallback))
                    presentMobiles.add(m)
                    // TK-REPORTED BUG FIX (2026-07-24): same self-heal as
                    // the Enquiry/Visit tab fallbacks above -- a genuine
                    // Advance/Treatment payment exists (paid > 0, checked
                    // just above) but the matching Treatment-stage
                    // "followups" row was never found; write it directly so
                    // this stops recurring for this patient.
                    //
                    // 🔴🔴 TK-REPORTED (04.08.2026 — একই ক্লাসের বাগ প্রজেক্টে
                    // আর কোথায় আছে খুঁজে দেখা, Enquiry Reject-এর ভুতুড়ে
                    // ডুপ্লিকেট ধরার পরে): এখানেও একই ফাঁক ছিল — উপরের
                    // `incompleteMobiles` কোনো কারণে এই নম্বর মিস করলে
                    // (ক্যাশ/টাইমিং) সরাসরি নতুন সারি বানিয়ে ফেলত। এখানেও
                    // একই সরাসরি-চেক পাহারা বসানো হলো।
                    val alreadyExistsT = try {
                        SupabaseClient.findByMobileOrNull("followups", m, "id", 1)
                    } catch (_: Throwable) { null }
                    if (alreadyExistsT == null || alreadyExistsT.length() > 0) continue
                    val healRow = JSONObject()
                        .put("id", "fu_" + java.util.UUID.randomUUID().toString().replace("-", ""))
                        .put("refId", p.s("id"))
                        .put("patientId", p.s("patientId"))
                        .put("mobile", p.s("mobile"))
                        .put("name", p.s("name"))
                        .put("branch", pBranch)
                        .put("disease", p.s("disease").ifBlank { p.s("diagnosis") })
                        .put("address", p.s("address"))
                        .put("age", p.s("age"))
                        .put("sex", p.s("sex"))
                        .put("stage", "Treatment")
                        .put("date", p.s("registrationDate").ifBlank { p.s("date") })
                        .put("registrationDate", p.s("registrationDate").ifBlank { p.s("date") })
                        .put("visitDate", p.s("visitDate").ifBlank { p.s("registrationDate").ifBlank { p.s("date") } })
                        .put("lastRemark", "Treatment payment / Advance received")
                        .put("nextFollow", "")
                        .put("callCount", 0)
                        .put("status", "Active")
                        .put("history", JSONArray())
                        .put("createdBy", p.s("createdBy"))
                        .put("createdAt", p.s("createdAt"))
                        .put("updatedAt", p.s("createdAt"))
                    upsertWithHealRetry(healRow)
                }
                }
            }

            for (idx in items.indices) {
                val m = digits(items[idx].mobile)
                /* 🔵🔒 V518: এই কার্ডটা ঘোষিত আলাদা রোগীর হলে তাঁর **নিজের**
                   সারি থেকে সব নেওয়া হয় — বিল · Patient ID · ঠিকানা · বয়স ·
                   লিঙ্গ · রোগ · নাম · ব্রাঞ্চ।
                   ⛔ নইলে `own` null, আর নিচের প্রতিটা লাইন **হুবহু আগের
                      মোবাইল-ম্যাপ** থেকেই পড়ে — একটুও বদলায়নি। */
                val own = declaredById[items[idx].refId]
                val cloudBill = if (own != null) own.optDouble("bill", 0.0) else billByMobile[m]
                val pid = (if (own != null) own.s("id") else idByMobile[m]) ?: ""
                // TK-REQUESTED (2026-07-27), ধাপ ২: a payment can be filed
                // under EITHER the patients row id (this app) or the human
                // Patient ID code (the web app's Chamber screen). Only the
                // row id was counted here, so money taken on the computer
                // could be missing from this card's Paid -- and the Due then
                // showed too high. The two buckets are separate (one payment
                // row carries one identity), so adding them can never count
                // the same payment twice.
                val pcode = (if (own != null) own.s("patientId") else patientCodeByMobile[m].orEmpty())
                val paidByCode = if (pcode.isNotBlank() && pcode != pid) (paidByPid[pcode] ?: 0.0) else 0.0
                val cloudPaid = (paidByPid[pid] ?: 0.0) + paidByCode
                val bill = maxOf(cloudBill ?: 0.0, items[idx].bill)
                // 🔎 TK-ORDERED CODE AUDIT (27.07.2026): the two buckets above
                // cover a payment filed under this patient's row id and under
                // their Patient ID code. They do NOT cover a payment filed under
                // a SECOND patients row for the same person (a duplicate
                // registration, or a row Chamber's findOrMakePatient created when
                // a mobile lookup momentarily failed) -- that money simply was
                // not counted, so this card showed Paid too LOW and Due too HIGH,
                // while Patient Details (which also reads payments by mobile)
                // showed the right total. That mismatch is exactly the class of
                // problem TK keeps reporting.
                // Safe to close now: by TK's locked rule ONE MOBILE = ONE
                // REGISTRATION, so every non-fee payment on this mobile is this
                // patient's own. paidByMobileFallback is built from the SAME
                // already-fetched payments with the SAME exclusions (Visit Fee
                // and attendance marks skipped), so this adds no request and can
                // never count a payment twice -- it only refuses to show LESS
                // than what was really taken.
                /* 🔴🔴🔒 V518 (২২.০৮.২০২৬) — **টাকা কখনো মিশতে পারবে না।**
                   উপরের মন্তব্যে লেখা আছে *"by TK's locked rule ONE MOBILE =
                   ONE REGISTRATION, so every non-fee payment on this mobile is
                   this patient's own"* — V516-এর পরে **সেই ধরে-নেওয়াটা আর সত্যি
                   নয়**। এক নম্বরে স্বামী ও স্ত্রী থাকলে `paidByMobileFallback`
                   দুজনের টাকা একসঙ্গে যোগ করত ⇒ দুজনেরই Paid বেশি, Due কম দেখাত।

                   তাই যে নম্বরে ঘোষিত আলাদা রোগী আছেন, সেখানে মোবাইল-ভিত্তিক
                   সেফটি-নেটটা **ব্যবহার করা হয় না** — তখন হিসাব হয় রোগীর নিজের
                   আইডি ধরে (`cloudPaid`), যেটা পুরোপুরি সঠিক (V516-এ প্রমাণিত:
                   প্রত্যেকের Visit Fee তাঁর নিজের row id-তেই বসে)।
                   ⛔ followups সারির নিজের `paid` floor আগের মতোই থাকে — ওটা
                      প্রতি-সারি, তাই কখনো মেশে না, আর "কম দেখানো"র ঝুঁকিও নেই।
                   ⛔ সাধারণ নম্বরে (একজন রোগী) `mixedMobile` false ⇒ **হুবহু
                      আগের আচরণ**, সেফটি-নেট অক্ষত। */
                val mixedMobile = (own != null) || mobilesWithDeclared.contains(m)
                val paidByMobile = if (mixedMobile) 0.0 else (paidByMobileFallback[m] ?: 0.0)
                // 🔴🔴 TK-REPORTED FIX (01.08.2026): `items[idx].paid` (এই ফোনের
                // স্থানীয় floor) Refund-অন্ধ, তাই যার approved refund আছে তার
                // জন্য এই floor বাদ — শুধু live payments থেকে সঠিকভাবে
                // (refund-বিয়োগ সহ) হিসাব-করা দুটো সংখ্যাই ব্যবহার হয়। Refund
                // নেই এমন সবার জন্য আগের সেফটি-নেট (কখনো কম না দেখানো) অক্ষত।
                /* 🔵 V518: মিশ্র নম্বরে "এই নম্বরে refund আছে" দিয়ে বিচার করা
                   যায় না — refund কার তা বলা যায় না। তখন শুধু রোগীর নিজের
                   আইডি ধরেই দেখা হয়। ⛔ সাধারণ নম্বরে আগের মতোই দুটোই। */
                val refundExists = (pid.isNotEmpty() && hasApprovedRefundByPid.contains(pid)) ||
                    (!mixedMobile && hasApprovedRefundByMobile.contains(m))
                val paid = if (refundExists) maxOf(cloudPaid, paidByMobile)
                    else maxOf(cloudPaid, paidByMobile, items[idx].paid)
                items[idx] = items[idx].copy(
                    bill = bill,
                    paid = paid,
                    patientId = (if (own != null) own.s("patientId") else patientCodeByMobile[m].orEmpty()).ifBlank { items[idx].patientId },
                    address = (if (own != null) own.s("address") else addressByMobile[m].orEmpty()).ifBlank { items[idx].address },
                    age = ageByMobile[m].orEmpty().ifBlank { items[idx].age },
                    sex = sexByMobile[m].orEmpty().ifBlank { items[idx].sex },
                    disease = diseaseByMobile[m].orEmpty().ifBlank { items[idx].disease },
                    name = (if (own != null) own.s("name") else nameByMobile[m].orEmpty()).ifBlank { items[idx].name },
                    branch = branchByMobile[m].orEmpty().ifBlank { items[idx].branch },
                    hasApprovedRefund = refundExists,
                    refundManuallyRestored = refundRestoredByMobile.containsKey(m),
                    /* 🆕 V851 — রোগীর নিজের সারি থাকলে তবেই ভরে; নিছক এনকোয়ারিতে
                       ফাঁকা থাকে ⇒ ওই কার্ডে আগের মতোই `LAST CALL` দেখায়। */
                    regDate = (if (own != null) own.s("registrationDate").ifBlank { own.s("date") }.take(10)
                               else regDateByMobile[m].orEmpty()),
                    regBy = FollowUpModel.prettyStaff(
                        if (own != null) own.s("registeredBy").ifBlank { own.s("createdBy") }
                        else regByMobile[m].orEmpty()
                    )
                )
            }

            // TK-REQUESTED (2026-07-18): "যে ব্রাঞ্চে আছে, সেই ব্রাঞ্চের সবাই
            // দেখবে" -- branch can change (just enriched live above), so
            // re-apply the same visibility rule using the CURRENT branch.
            // A patient moved OUT of this viewer's branch drops out; one
            // moved IN becomes visible, all without a second network call
            // (uses the same allBranch/branchFilter/creatorMobile already
            // used for the very first visibility check in this function).
            if (!allBranch) {
                val stillVisible = items.filter {
                    val m = digits(it.mobile)
                    branchAllows(it.branch, it.patientId, branchFilter) ||
                        (!creatorMobile.isNullOrBlank() && createdByMobileMap[m] == creatorMobile)
                }
                items.clear(); items.addAll(stillVisible)
            }

            if (stage == "Treatment") {
                // TK APPROVED (2026-07-15): once a patient's bill is fully paid
                // (Due == 0), the record moves to Draft's "Complete" list only and
                // must no longer appear in the Patient (Treatment) follow-up tab.
                // Scoped to the Patient/Treatment tab only — Visit tab is unaffected.
                // TK-REQUESTED ADDITION (2026-07-24): also exclude a patient
                // Master has approved for "Complete despite Due" (real Due
                // stays > 0, untouched by this — only this tab's visibility
                // changes, matching the fully-paid case just above).
                // 🔴 TK-নির্দেশ (02.08.2026): এখন এই একই জায়গায় একটা তৃতীয় শর্তও
                // যোগ হলো — Approved Refund আছে আর নেট জমা ঠিক ₹0 (0.5-এর কম,
                // paisa-স্তরের রাউন্ডিং সহনশীলতা) হলে সেই রোগীও এই ট্যাব থেকে
                // সরে যায় (Draft-এর নতুন "Refunded" ঘরে দেখা যাবে, রেকর্ড অক্ষত)।
                // ⛔ এখানে শুধু `items` (রেন্ডার-করা তালিকা) ফিল্টার হয় — আসল
                // `followups.stage` কলাম একটুও বদলায় না, তাই Visit/Enquiry
                // ট্যাবের বিদ্যমান exclusion-লজিক (যেগুলো আসল stage দেখেই বাদ
                // দেয়) স্বয়ংক্রিয়ভাবেই এই রোগীকে ওখানেও আটকে দেয় — আলাদা কিছু
                // লিখতে হয়নি।
                // 🔴 B302.1 (02.08.2026): TK Draft-এর "Refunded" ঘর থেকে হাতে
                // করে ফিরিয়ে আনলে (`patients.refundRestoredBy` — Complete-
                // despite-Due-এর হুবহু একই প্যাটার্ন) — তখন `hasApprovedRefund`
                // সত্যি থাকলেও আর বাদ পড়ে না, `refundManuallyRestored` উপরের
                // শর্তকে ফিরিয়ে দেয়।
                val stillDue = items.filterNot {
                    (it.bill > 0 && it.paid >= it.bill) || completeApprovedByMobile.containsKey(digits(it.mobile)) ||
                        (it.hasApprovedRefund && it.paid <= 0.5 && !it.refundManuallyRestored)
                }
                items.clear(); items.addAll(stillDue)
            }
        }
        // Newest first — the most recent enquiry/record shows at the TOP,
        // matching the web's sortFollowRowsByRecent().
        val result = items.sortedByDescending { it.recordDate }
        // 🔒 খাতার সারি B172 (TK, 30.07.2026): কার্ডের ঠিকানা-ট্যাগ বসানো —
        // ⛔ **এই ধাপটা `saveCachedTab`-এর পরে**, তাই অফলাইন/জমানো তালিকার
        // আচরণ এক অক্ষরও বদলায়নি (জমানো তালিকায় ঠিকানা-ট্যাগ বেঁধে রাখা হয় না;
        // পরের বার লাইভ লোডেই আবার ঠিকভাবে বসবে — নিরাপদ, ছোট আপস)।
        // ⛔ **একটাই ছোট ব্যাচ-অনুরোধ** (`address_tags` টেবিল থেকে, শুধু আইডি ও
        // মান — ছবি/অন্য কিছু নেই); ব্যর্থ হলে প্রতিটা কার্ড নিজের ঠিকানা থেকে
        // auto-বেছে নেওয়া ডিফল্টে ফিরে যায়, কিছুই ভাঙে না।
        // 🔒 খাতার সারি B173 (TK, 30.07.2026): Patient (Treatment) কার্ডে এই
        // ট্যাগ আর দেখানো হয় না ("View All-এ চাপলে সব দেখা যাবে") — তাই ওই
        // ট্যাবে এই ব্যাচ-অনুরোধটাও আর করা হয় না, বাড়তি একটা ক্লাউড-কলও বাঁচল।
        /* 🏷️🔒 V712 — **জমানো তালিকা এখন ঠিকানার ট্যাগ বসানোর পরে লেখা হয়**
           (আগে তার আগে লেখা হত, তাই ট্যাগটা কখনোই জমত না — উপরের বড় নোট দেখুন)।
           ⛔ Treatment ট্যাবে ঠিকানার ট্যাগ দেখানোই হয় না (খাতার সারি B173),
              তাই সেখানে আগের মতোই বাড়তি কোনো অনুরোধ যায় না — শুধু জমা করে ফেরত। */
        if (stage == "Treatment") { saveCachedTab(stage, branchFilter, result); return result }
        val tagged = try {
            val saved = AddressTagRepository.fetchSavedTags(result.map { it.mobile })
            result.map { it ->
                val key = AddressTagRepository.keyFor(it.mobile)
                val tag = saved[key] ?: AddressTagRepository.defaultTagFromAddress(it.address)
                if (tag.isBlank()) it else it.copy(addressTag = tag)
            }
        } catch (_: Throwable) { result }
        saveCachedTab(stage, branchFilter, tagged)
        return tagged
    }

    private fun digits(s: String): String = s.filter { it.isDigit() }.takeLast(10)

    /**
     * TK-REPORTED BUG FIX (2026-07-26, from TK's photo-proofs): a record can
     * carry a Patient ID from one branch (COB-26072026-001 = Cooch Behar)
     * while its own "branch" text field says something else (JALPAIGURI) --
     * because the branch field is editable/re-written by later saves, while
     * the Patient ID is fixed at registration and is what is actually
     * printed on the patient's papers. When the two disagreed, the branch
     * text alone decided visibility, so the branch that genuinely owns that
     * patient could not see them at all (Master, who sees All Branch, could
     * -- exactly what TK saw).
     *
     * A record is now visible to a branch if EITHER its branch text OR its
     * Patient ID's branch code belongs to that branch. This can only ever
     * ADD the rightful branch back; it never hides anyone, and it never
     * exposes a record to a branch that matches neither.
     */
    /**
     * 🚨 TK-REPORTED, LIVE (2026-07-27): "কোচবিহারে ৩ জনের Registration হয়েছে,
     * স্টাফ বলছে ২ জন দেখাচ্ছে, একজন দেখাচ্ছে না।"
     *
     * ROOT CAUSE FOUND HERE. Building the Visit tab needs SIX separate cloud
     * reads. If ANY ONE of them fails -- which on a weak line is not rare, it
     * is likely -- this file gives up on building a fresh list and returns
     * `loadCachedTab(...)`, the list saved the LAST time everything worked.
     * That saved list was made BEFORE today's registrations, so a patient
     * registered ten minutes ago is simply not in it. Nothing on screen says
     * the list is old, so to the staff the patient has "disappeared".
     *
     * THE FIX: a patient this device itself registered must NEVER be missing,
     * no matter what the network did. The saved list is still shown (that part
     * was right -- an old list beats a blank screen), but every record for
     * this stage that is sitting in this phone's own storage and is NOT in
     * that saved list is added on top of it.
     *
     * This cannot lose or alter anything: it only ADDS rows that the phone
     * itself already holds, and rowsForStage() already excludes anything
     * Cancelled/Incomplete, so a rejected patient can never come back.
     */
    /**
     * TK-REPORTED, LIVE (27.07.2026 — AMIT KUMAR / KNE-27072026-001):
     * whenever this screen falls back to the last cached list, the cache is by
     * definition older than the newest record -- so the patient registered
     * minutes ago was exactly the one who could never appear. This keeps every
     * cached row untouched (same order, same ₹ numbers) and only appends the
     * freshly-fetched rows whose mobile the cache does not have at all.
     * The fresh rows have already passed the same stage/status/branch checks,
     * so nothing rejected or out-of-branch can slip in this way.
     */
    private fun plusMissing(cached: List<FollowUpItem>, fresh: List<FollowUpItem>): List<FollowUpItem> {
        if (fresh.isEmpty()) return cached
        return try {
            val seen = HashSet<String>()
            for (c in cached) {
                val m = digits(c.mobile)
                if (m.isNotEmpty()) seen.add(m)
            }
            val extra = ArrayList<FollowUpItem>()
            for (f in fresh) {
                val m = digits(f.mobile)
                if (m.isEmpty() || seen.contains(m)) continue
                seen.add(m)
                extra.add(f)
            }
            if (extra.isEmpty()) cached else extra + cached
        } catch (_: Throwable) {
            cached
        }
    }

    private fun cachedPlusOwn(stage: String, branchFilter: String?): List<FollowUpItem>? {
        val cached = loadCachedTab(stage, branchFilter) ?: return null
        val ctx = context ?: return cached
        return try {
            val seen = HashSet<String>()
            for (c in cached) {
                val m = digits(c.mobile)
                if (m.isNotEmpty()) seen.add(m)
            }
            val extra = ArrayList<FollowUpItem>()
            val local = LocalWorkflowStore(ctx).rowsForStage(stage)
            for (i in 0 until local.length()) {
                val row = local.optJSONObject(i) ?: continue
                val item = FollowUpModel.parse(row)
                val m = digits(item.mobile)
                if (m.isEmpty() || seen.contains(m)) continue
                if (!branchAllows(item.branch, item.patientId, branchFilter)) continue
                seen.add(m)
                extra.add(item)
            }
            if (extra.isEmpty()) cached else extra + cached
        } catch (_: Throwable) {
            cached
        }
    }

    /** TK-REPORTED (2026-07-27): finds the branch this enquiry was actually
     *  saved under, by looking the same mobile up in the "enquiries" rows this
     *  tab has already fetched. Used only to fill in a BLANK branch on a
     *  followups row -- it never overrides a branch that is already set, so it
     *  cannot move a record to the wrong branch. Returns "" when nothing is
     *  found, which leaves today's behaviour exactly as it was. */
    /**
     * 🔴🔵🔒 V525 (২২.০৮.২০২৬, TK-রিপোর্ট — *"Unexpected time ট্যাগটা কিভাবে
     * সরে গেল?"*) — **পুরোনো কার্ডেও ট্যাগটা ফিরিয়ে আনা।**
     *
     * **আসল কারণ (কোডে প্রমাণিত):** Enquiry কার্ডের 🌙 Unexpected / 🕘 Official
     * ট্যাগ আসে `followups` সারির `timeType` ঘর থেকে (`FollowUpAdapter`)।
     * ফোনের অ্যাপ ওই ঘরটা লেখে (`EnquiryModel.buildFollowUpRow`), কিন্তু
     * **কম্পিউটারের (ওয়েব) `ensureFollow()` কখনোই লিখত না** — তাই কম্পিউটার
     * থেকে করা এনকোয়ারির কার্ডে ট্যাগটা **কোনোদিনই আসেনি** (V512 থেকেই)।
     * ওয়েবের দিকটা এই একই সংস্করণে ঠিক করা হয়েছে, কিন্তু **আগে জমা হয়ে
     * যাওয়া** সারিগুলোতে ঘরটা ফাঁকাই থেকে যাবে।
     *
     * **সমাধান:** ফাঁকা হলে **এনকোয়ারির নিজের সারি** থেকে পড়ে নেওয়া —
     * যেটা এই ট্যাব **আগে থেকেই** এনে রেখেছে (`preEnquiries`)।
     *
     * ⛔ **বাড়তি একটাও cloud-read নেই** — ঠিক যেভাবে পাশের
     *    `branchFromEnquiries()` ফাঁকা branch ভরে (একই তালিকা, একই নিয়ম)।
     * ⛔ ঘরটা ভরা থাকলে **কিছুই ছোঁয়া হয় না** — আচরণ অবিকল আগের।
     * ⛔ কোনো সারি লেখা/বদলানো হয় না — শুধু **দেখানোর** সময় ভরা হয়।
     * ⛔ টাকার হিসাব সম্পূর্ণ অস্পৃশ্য: Extra Income `patients.timeType`
     *    দেখে (V418-এর SQL), `followups`-এরটা নয়।
     */
    private fun timeTypeFromEnquiries(enquiries: JSONArray, mobile: String): String {
        return try {
            val want = digits(mobile)
            if (want.isEmpty()) return ""
            for (i in 0 until enquiries.length()) {
                val e = enquiries.optJSONObject(i) ?: continue
                if (digits(e.s("mobile")) != want) continue
                val t = e.s("timeType")
                if (t.isNotBlank()) return t
            }
            ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun branchFromEnquiries(enquiries: JSONArray, mobile: String): String {
        return try {
            val want = digits(mobile)
            if (want.isEmpty()) return ""
            for (i in 0 until enquiries.length()) {
                val e = enquiries.optJSONObject(i) ?: continue
                if (digits(e.s("mobile")) != want) continue
                val b = e.s("branch")
                if (b.isNotBlank()) return b
            }
            ""
        } catch (_: Throwable) {
            ""
        }
    }

    // 🔴🔒 V453 (20.08.2026): উপরের `branchScopeFilter()`-এর মতোই কমা-আলাদা
    // একাধিক ব্রাঞ্চ গ্রহণ করে (CrossBranchStaffAccess.kt)। একটাই ব্রাঞ্চ
    // থাকলে আগের আচরণ অক্ষত।
    private fun branchAllows(rowBranch: String, patientId: String, branchFilter: String?): Boolean {
        if (branchFilter.isNullOrBlank() || branchFilter == "All") return true
        val parts = branchFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        for (part in parts) {
            if (rowBranch.equals(part, ignoreCase = true)) return true
            val code = patientId.substringBefore('-').trim()
            if (code.isNotBlank() && code.length == 3 &&
                code.equals(PatientIdGenerator.branchCode(part), ignoreCase = true)) return true
        }
        return false
    }


    /**
     * 🚨 TK'S RULE (2026-07-28): "আমার ফোনে আমি লিখেছি, তাহলে আমাকেই বা কেন দেরিতে
     * দেখাবে?" -- whatever is typed on THIS phone must be on screen at once,
     * whether the cloud is quick, slow or asleep.
     *
     * The phone already kept its own copy of every record it had saved, and an
     * edit updated that copy. But a record first entered on ANOTHER phone (a
     * different branch's front desk) was simply not in this phone's copy at
     * all, so the edit had nowhere local to go -- the screen kept showing the
     * old text until the cloud caught up. Now, if the copy is missing, it is
     * created here with the new text and marked as still-to-send, so the
     * screen updates immediately and the retry queue still finishes the cloud
     * side quietly in the background.
     */
    private fun rememberEditOnThisPhone(id: String, fields: JSONObject, known: JSONObject?) {
        val ctx = context ?: return
        try {
            val store = LocalWorkflowStore(ctx)
            if (store.updateLocalFollowUp(id, fields)) return
            val base = if (known != null && known.length() > 0) JSONObject(known.toString()) else JSONObject()
            base.put("id", id)
            val keys = fields.keys()
            while (keys.hasNext()) { val k = keys.next(); base.put(k, fields.opt(k)) }
            if (base.optString("stage").isNotBlank() || base.optString("mobile").isNotBlank()) {
                store.upsertFollowUp(base, "PENDING")
            }
        } catch (_: Exception) {}
    }

    /**
     * 📝🔒 V827 (২৯.০৮.২০২৬, TK-নির্দেশ) — নতুন, **ঐচ্ছিক** ঘর `stampCallDate`।
     *
     * TK-এর সমস্যা: *"কিশনগঞ্জের স্টাফ কল রিসিভ করেছিল, কিন্তু নম্বরটা
     * জলপাইগুড়ির এনকোয়ারি — সে রিমার্ক লিখতে পারে না। … রিমার্কটা ফলোআপ
     * কার্ডে চলে যেতে হবে, যাতে জলপাইগুড়ির স্টাফ বোঝে লাস্ট কে কথা বলেছিল।"*
     *
     * কার্ডে লেখা থাকে `LAST CALL <তারিখ> (<স্টাফ>)`. স্টাফের নামটা আসে
     * history-র শেষ সারি থেকে, আর তারিখটা `lastCallDate` থেকে — **দুটো আলাদা
     * ঘর**। তাই শুধু রিমার্ক লিখলে **পুরনো তারিখের পাশে নতুন নাম** বসে যেত,
     * যেটা মিথ্যা।
     *
     * TK-অনুমোদিত তৃতীয় পথ: তারিখটা আজকের হবে, **কিন্তু কল-গোনা বাড়বে না** —
     * তাই "৫ কলের পর বাতিল" নিয়মে এক অক্ষরও প্রভাব পড়ে না।
     *
     * ⛔ ডিফল্ট `false` ⇒ আগের প্রতিটা ডাক (Chamber · Dialer · Appointment ·
     *    Follow-up) হুবহু আগের মতোই চলে।
     * ⛔ `incrementCall = true` হলে সেই পুরনো নিয়মই আগে চলে; এই ঘরটা তখন
     *    বাড়তি কিছু করে না (তারিখ ওখানেই বসে যায়)।
     */
    /**
     * 🏷🔒 V1192 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **প্রতিটা লেখায় উৎসের চিহ্ন।**
     *
     * TK-এর রিপোর্ট থেকে যে ফাঁকটা বেরিয়েছিল: চেম্বারে লেখা **চিকিৎসা** আর
     * ফোন-কলের **রিমার্ক** — দুটোই এই একই `history`-তে জমা হয়, আলাদা চেনার
     * কিছু ছিল না। ফলে Report Card ও CHECK-UP Queue যখন ওই দিনের লেখা তুলে
     * আনে (V1189/V1191), তখন কল-রিমার্ক "চিকিৎসা" সেজে বসে যেতে পারত।
     *
     * ⇒ এখন প্রতিটা নতুন সারিতে একটা ছোট চিহ্ন বসে: `src` = `"treat"` (চেম্বারের
     *   চিকিৎসা) অথবা `"call"` (ফোন-কল)। জানা না গেলে চিহ্নটা বসেই না।
     * ⛔ **পর্দায় কিচ্ছু বদলায় না** — চিহ্নটা শুধু ভিতরে; History-র Type কলাম,
     *    কল-গোনা, লেখা — সব হুবহু আগের মতোই।
     * ⛔ **পুরনো সারিতে চিহ্ন নেই**, আর সেগুলো আগের মতোই দেখানো হয় (V1189/V1191-এ
     *    TK যা প্রুফে পাশ করেছেন) — শুধু **স্পষ্টভাবে `"call"` লেখা** সারিগুলোই
     *    চিকিৎসার জায়গায় বাদ যায়। তাই আজ থেকে ভুলটা আর হতেই পারবে না, অথচ
     *    পুরনো কিছুই হারায় না।
     * ⛔ ডেটাবেসে নতুন কোনো টেবিল/কলাম লাগে না — `history` আগে থেকেই JSON।
     */
    /* 💰🔒 V1403 — `systemNote = true` = অ্যাপের নিজের লেখা নোট (যেমন "Bill corrected…"),
       স্টাফের কল নয় ⇒ হিস্ট্রি/lastRemark-এ লেখা হয়, কিন্তু কল-গোনা · lastCallDate ·
       (তার সূত্রে V1065-এর `nextFollow`=আজ) — কিছুই বসে না। TK-র CSV-তে পেন্ডিং ১২-র
       ৭টা ঠিক এই নোট থেকেই "আজকের কল"-এ ঢুকেছিল। ওয়েবের বিল-ঠিক পথ আগে থেকেই
       কল ধরে না — এখন ফোনও এক। ⛔ ডিফল্ট false ⇒ বাকি সব ডাক অপরিবর্তিত। */
    fun updateRemark(id: String, remark: String, staffName: String, incrementCall: Boolean = false, stampCallDate: Boolean = false, source: String = "", systemNote: Boolean = false, eventDate: String = ""): Boolean {
        // V1653: Chamber Date may be a past selected day. Generic callers keep today's behaviour.
        val eventDay = eventDate.take(10).takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) } ?: FollowUpModel.today()
        val eventStamp = if (eventDay == FollowUpModel.today()) isoNow() else eventDay + "T00:00:00.000Z"
        // Match the WebView's updateFollowAction: append to the history log and,
        // when this is an enquiry call, bump callCount (capped at 5) + stamp today.
        val existing = SupabaseClient.fetchList(
            "followups", "id=eq.$id", 1,
            select = "id,mobile,stage,name,branch,disease,status,history,callCount,lastCallDate,nextFollow,noMoreCalls,updatedAt"
        )
        val row = if (existing.length() > 0) existing.getJSONObject(0)
            else context?.let { LocalWorkflowStore(it).findFollowUp(id) } ?: JSONObject()
        val history = row.optJSONArray("history") ?: JSONArray()
        // TK-REQUESTED ADDITION (2026-07-20): capture time-of-day too (not
        // just the date), needed for the Enquiry & Call History table's
        // "Time" column and Unexpected-hour flag. Older history entries made
        // before this change have no "time" field and will show "—" for
        // Time on screen; every entry from now on carries it.
        //
        // 🔒 খাতার সারি B54 (TK, 28.07.2026 রাত): **ফাঁকা রিমার্ক দিয়ে আগের
        // রিমার্ক কখনো মুছে যাবে না।** আগে ঘরটা ফাঁকা করে Save করলে
        // `lastRemark` ফাঁকা হয়ে যেত — লেখা কথাটা চিরতরে চলে যেত। এখন ফাঁকা
        // হলে **আগের রিমার্কটাই থাকে** আর ইতিহাসে ফাঁকা সারিও জমে না।
        // এটা শেষ পাহারা — পর্দাতেও আলাদা করে আটকানো আছে, কিন্তু কোনো পথেই
        // যেন কথাটা হারাতে না পারে।
        //
        // 🔒 খাতার সারি B58 (29.07.2026, যাচাই করতে গিয়ে ধরা পড়েছে — TK-কে
        // জানিয়ে তাঁর অনুমতি নিয়ে ঠিক করা):
        // নেট না থাকলে (বা সারিটা কোথাও না পাওয়া গেলে) `row` ফাঁকা থেকে যেত।
        // তখন পুরনো নিয়মে —
        //   • `history` হিসেবে শুধু আজকের এই একটা সারি লেখা হত, ফলে ক্লাউডে
        //     জমা থাকা **রোগীর পুরো কল-ইতিহাস মুছে যেতে পারত**, আর
        //   • `callCount` ০ ধরে ১ লেখা হত, ফলে **গোনা ৩ থেকে ১-এ নেমে যেত**।
        // এখন সারিটা সত্যিই পড়া গেলে তবেই ইতিহাস ও গোনায় হাত পড়ে; না পাওয়া
        // গেলে ওদুটো **ছোঁয়াই হয় না** (রিমার্কটা তবু সেভ/সারিবদ্ধ হয়)।
        // ⛔ জানা না থাকলে হাত না দেওয়াই নিরাপদ — ভুল অঙ্ক লিখে দেওয়ার চেয়ে
        //    আগেরটা অক্ষত রাখা ভালো।
        val haveRow = row.length() > 0
        // 🔴🔒 V1372 (১২.০৯.২০২৬, তালিকা ৪৬২-চ): দুই ফোনে একই রোগীর history
        // একসাথে লিখলে একজনের এন্ট্রি হারানোর ঝুঁকি — নিচে RPC সফল হলে এই
        // এন্ট্রি সরাসরি সার্ভারে জুড়ে যায়, তাই এই ফোন আর পুরো array নিজে
        // ফিরিয়ে লিখবে না (নিচে fields-এ "history" বসে না)।
        var historyAppendedViaRpc = false
        if (remark.isNotBlank() && haveRow) {
            /* 🏷 V1192 — উৎসের চিহ্ন। কল-ফ্ল্যাগ থাকলে নিজে থেকেই "call",
               নইলে caller যা বলেছে (চেম্বারের পথে "treat")। কিছুই জানা না গেলে
               ঘরটা বসেই না — তখন আচরণ হুবহু আগের মতোই। */
            val src = when {
                source.isNotBlank() -> source
                incrementCall || stampCallDate -> "call"
                else -> ""
            }
            val entry = JSONObject().put("date", eventDay).put("time", isoNow())
                .put("remark", remark).put("staff", staffName)
            if (src.isNotBlank()) entry.put("src", src)
            val rpcResult = SupabaseClient.appendFollowupHistory(id, entry)
            if (rpcResult != null) {
                historyAppendedViaRpc = true
            } else {
                // ⛔ RPC ব্যর্থ (পুরনো ডেটাবেসে ফাংশন না থাকলেও) — আগের
                // read-modify-write পথেই ফিরে যাওয়া, আচরণ আগের মতোই থাকে।
                history.put(entry)
            }
        }
        /* 🔴🔴🔒 V1222 ④ (০৮.০৯.২০২৬ — TK, স্টাফের সামনে অপমানিত হয়ে, ছবিসহ:
           *"Staff কল করেছে, প্রতিবার Remarks লিখেছে, কিন্তু App-এর মধ্যে দেখাচ্ছে না"*
           — Susmita Das (+919635608042): ফোনের কল-লগে ১৭.০৭ · ১৮.০৭ · ২৪.০৭-এর
           কল আছে, অথচ অ্যাপের "Enquiry Calls" টেবিলে ওই তিনটে কলের কিছুই নেই।)

           **আসল কারণ (কোডে মেপে পাওয়া):** উপরের `haveRow` — সারিটা ক্লাউড বা
           ফোন কোথাও থেকেই পড়া না গেলে (নেট দুর্বল) `history`-তে **হাত দেওয়াই হয়
           না**। নিয়মটা B58-এ ইচ্ছে করেই বসানো, কারণ তখন শুধু আজকের একটা সারি
           লিখলে রোগীর **পুরো কল-ইতিহাস মুছে যেত** — সেটা আরও বড় সর্বনাশ।
           ⇒ কিন্তু ফল দাঁড়াত: রিমার্কটা `lastRemark`-এ বসত, আর **কলটা ইতিহাসে
             কোথাও উঠত না** — চিরতরে। স্টাফ "সেভ হয়েছে" দেখে পরের কলে চলে যেতেন।

           ⇒ **সমাধান (ইতিহাস মোছার ঝুঁকি এক ফোঁটাও না বাড়িয়ে):** এমন সময়ে
             সারিটা ফোনে **জমা** থাকে। পরে যখন সারিটা সত্যিই পড়া যায়, তখন
             ক্লাউডের **আসল ইতিহাসের শেষে** সারিটা জুড়ে দেওয়া হয়।
           ⛔ ফাঁকা/আন্দাজের ইতিহাস কখনো লেখা হয় না — সারি পড়া গেলে তবেই।
           ⛔ একই সারি দুবার বসে না (তারিখ · সময় · লেখা · স্টাফ মিলিয়ে দেখা হয়)।
           ⛔ `callCount` এখানে ছোঁয়া হয় না — "দিনে একবার" ও ৫-এর সীমা অটুট। */
        if (remark.isNotBlank() && !haveRow) {
            val src2 = when {
                source.isNotBlank() -> source
                incrementCall || stampCallDate -> "call"
                else -> ""
            }
            val late = JSONObject().put("date", eventDay).put("time", isoNow())
                .put("remark", remark).put("staff", staffName)
            if (src2.isNotBlank()) late.put("src", src2)
            queueHistoryEntry(id, late)
        }

        val fields = JSONObject().put("updatedAt", isoNow())
        // 🔴🔒 V1372 — history RPC দিয়ে সরাসরি সার্ভারে জুড়ে গেলে এখানে আর
        // পাঠানো হয় না (নইলে এই ফোনের পুরনো কপি RPC-এর করা এন্ট্রি চাপা দিয়ে দিত)।
        if (haveRow && !historyAppendedViaRpc) fields.put("history", history)
        if (remark.isNotBlank()) fields.put("lastRemark", remark)
        /* 🔴🔒 V814 (২৮.০৮.২০২৬, TK-রিপোর্ট "ASBEN এখনো কেন?") — লেখাটা **কবে
           লেখা হলো** সেটা এখন আলাদা ঘরে বসে। `updatedAt` অন্য কাজেও (যেমন
           `updateNextFollow`) আজকের হয়ে যেত, তাই সেটা দিয়ে "আজকের নোট কি না"
           বোঝা যেত না — পুরনো লেখা আজকের সেজে চেম্বার-বন্ধের পাহারা পার হত।
           ⛔ ঘরটা **শুধু তখনই** লেখা হয় যখন রিমার্কের কথাটা সত্যিই বদলায়। */
        if (remark.isNotBlank()) fields.put("lastRemarkAt", eventStamp)
        // Final safety gate: regardless of which Follow-up screen calls this
        // function, empty text can never change Last Call or Call Count.
        /* 🔴🔴🔒 V1149 (০৬.০৯.২০২৬, TK-সিদ্ধান্ত: *"শুধু রিমার্ক লিখলে তবেই সেটা
           কল বলে গণ্য করা হয় — এই নিয়মটাই থাকতে হবে"*, আর পরে *"খ করুন"*)।
           🔴 **যা ধরা পড়ল (TK-এর SQL-এ মেপে):** ১৫০২টা সারির মধ্যে **১৩৯৩টায়**
              গোনা আর হিস্ট্রি দুই রকম বলত — প্রায় সবগুলোতেই **গোনা কম**।
              কারণ: গোনা বাড়ত **কেবল `incrementCall = true` পথে**, অথচ রিমার্ক
              লেখা হলেই হিস্ট্রিতে সারি বসত। ⇒ যে পর্দাগুলো ওটা পাঠাত না,
              সেখানে কল হিস্ট্রিতে উঠত কিন্তু গোনায় নয়।
           ⇒ এখন TK-র নিয়মই কোডে: **রিমার্ক লেখা মানেই একটা কল** — কোন পর্দা
             থেকে লেখা হলো তাতে কিছু আসে যায় না।
           ⛔ "দিনে একবার" নিয়ম অটুট (খাতার সারি B53) · ৫ বারের সীমা অটুট ·
              ফাঁকা রিমার্কে কিছুই বদলায় না · সারিটা পড়া না গেলে হাত পড়ে না। */
        if (remark.isNotBlank() && haveRow && !systemNote) {   // 💰 V1403 — অ্যাপের নোট কল নয়
            // 🔒 খাতার সারি B53 (TK, 28.07.2026 রাত): **দিনে একবারই।**
            // আগে একই দিনে দুটো রিমার্ক লিখলে দাগ (call signal) দু'ঘর বেড়ে
            // যেত, অথচ কম্পিউটারের নিয়ম চিরকাল ছিল "দিনে একবার"
            // (`updateFollowAction`: lastCallDate===today হলে গোনা বাড়ে না)।
            // তাই ফোন ও কম্পিউটারে এক রোগীর দাগ আলাদা হয়ে যেত।
            // এখন দুটোই এক নিয়মে — আজ আগে কল গোনা হয়ে থাকলে গোনা বাড়ে না,
            // শুধু তারিখটা আজকের হয়ে যায়। ⛔ ৫ বারের সীমা অক্ষত।
            val todayStr = FollowUpModel.today()
            val lastCall = if (row.isNull("lastCallDate")) "" else row.optString("lastCallDate", "")
            val current = row.optInt("callCount", 0)
            val newCount = if (lastCall == todayStr) current else (current + 1).coerceAtMost(5)
            fields.put("callCount", newCount)
            fields.put("lastCallDate", todayStr)
        }
        /* 📝🔒 V827 — কল-গোনা ছোঁয়া হয় না, শুধু তারিখটা আজকের হয়, যাতে কার্ডের
           `LAST CALL <তারিখ> (<স্টাফ>)` লাইনটা সত্যি কথা বলে।
           ⛔ উপরের `incrementCall` পথে ইতিমধ্যে তারিখ বসে গেলে এখানে আর কিছু
              করা হয় না (দুই পথ কখনো একে অপরের উপর লিখবে না)। */
        if (stampCallDate && !incrementCall && remark.isNotBlank() && haveRow) {
            fields.put("lastCallDate", FollowUpModel.today())
        }
        /* 📞🔴🔒 V1065 (০৪.০৯.২০২৬) — **V998-এর বাকি দরজাটা।**
           TK ছবি দিয়ে বললেন *"এই সমস্যার কথা এর আগে কি আপনাকে বলা হয় নাই"* —
           হ্যাঁ, বলা হয়েছিল (তালিকার সারি ৯১ · V998, আর তার আগে সারি B214)।
           **আমার ব্যর্থতা:** V998-এ শুধু **ডায়ালারের** পথটা সেরেছিলাম, কিন্তু
           রিমার্ক/টাকার এই পথেও হুবহু একই ফাঁক ছিল — `lastCallDate` আজকের
           হয়ে যেত, অথচ `nextFollow` পুরনোই থাকত ⇒ **পরের কল, শেষ কলের আগে**,
           সারিটা চিরকাল "Overdue" (TK-এর ছবিতে NOOR ALAM: শেষ কল ০১.০৯,
           পরের কল ০২.০৮ — লেখা ছিল *"Treatment payment / Advance received"*,
           অর্থাৎ কল নয়, টাকার পথ)।
           ⇒ এখন তারিখ বসার সাথে সাথেই `nextFollow` **ফাঁকা বা আজকের আগের হলে**
             আজকের দিন বসে — V998-এর হুবহু একই নিয়ম।
           ⛔ **ভবিষ্যতের তারিখ কখনো ছোঁয়া হয় না।**
           ⛔ স্টাফ নিজে তারিখ বাছলে সেটাই জেতে (`nextFollow` আগেই বসানো থাকলে
              এখানে আর কিছু করা হয় না)।
           ⛔ সারি তালিকা থেকে হারায় না — "বকেয়া" থেকে "আজকের"-এ সরে আসে। */
        /* 📵🔒 V1206 (০৮.০৯.২০২৬, TK-রিপোর্ট: *"বার বার নো মোর কল দাবার পরেও
           আবার এই পেশেন্টের নাম কেনো শো করছে"*) — **এটাই ছিল আসল দোষ।**
           V1065-এর এই নিয়মটা ফাঁকা `nextFollow` দেখলেই আজকের দিন বসিয়ে দিত,
           অর্থাৎ "ইচ্ছে করে থামানো"-কেও "কখনো বসানোই হয়নি" ধরত। ⇒ এখন
           `noMoreCalls` সত্যি হলে এই নিয়ম **ছোঁয়াই হয় না**।
           ⛔ V1065-এর আসল উদ্দেশ্য (কল হলেও সারিটা চিরকাল "বকেয়া" না থাকা)
              অন্য সব সারিতে হুবহু আগের মতোই কাজ করে। */
        val stoppedRow = haveRow && row.optBoolean("noMoreCalls", false)
        if (fields.has("lastCallDate") && !fields.has("nextFollow") && haveRow && !stoppedRow) {
            val todayStr = FollowUpModel.today()
            val nf = if (row.isNull("nextFollow")) "" else row.optString("nextFollow", "").trim()
            if (nf.isBlank() || nf < todayStr) fields.put("nextFollow", todayStr)
        }
        rememberEditOnThisPhone(id, fields, row)
        // TK-REPORTED BUG FIX (2026-07-16): if this cloud write fails, queue
        // it for a silent retry (BottomNav.wire()) instead of losing it --
        // the return value/messaging below is UNCHANGED on purpose (still
        // "success" whenever there's a local context, exactly as before)
        // so no existing screen's behavior changes; only the underlying
        // reliability improves.
        val cloudOk = SupabaseClient.updateById("followups", id, fields)
        // 🚨 TK-REPORTED (2026-07-28): a remark said "saved" but the old one
        // stayed. Supabase answers 200 OK to an update that matched NO row, so
        // "cloudOk" alone was never proof. Read the row back and check the
        // remark really is there; if it is not, keep it in the retry queue so
        // it is written for real later instead of being lost silently.
        val reallySaved = cloudOk && try {
            // 🔒 খাতার সারি B54: রিমার্ক ফাঁকা থাকলে `lastRemark` ইচ্ছে করেই
            // বদলানো হয় না (আগেরটা রাখা হয়) — তখন "নতুন লেখাটা বসেছে কিনা"
            // মিলিয়ে দেখার কিছু নেই, নইলে সারিটা অপেক্ষমাণ তালিকায় চিরকাল
            // আটকে থাকত।
            if (remark.isBlank()) true
            else {
                val back = SupabaseClient.fetchList("followups", "id=eq.$id", 1, select = "id,lastRemark")
                back.length() > 0 && back.getJSONObject(0).s("lastRemark") == remark   // 🔴🔒 V696
            }
        } catch (_: Exception) { false }
        /* 🔴🔒 V1597 (১৭.০৯.২০২৬, TK-রিপোর্ট — একই ক্লাসের দোষ পুরো ফাইলে খুঁজে
           বের করে সারানো, নিয়ম ৭)। **আসল গভীর কারণ:** এই ফাংশন (আর এই ফাইলের
           আরও পাঁচটা — updateNextFollow/updateStatus/closeSiblingFollowUps/
           logEnquiryCall/resetCallCount) `rememberEditOnThisPhone()`-এ ফোনের
           কপি "PENDING" করে দেয়, কিন্তু ক্লাউড-লেখা **সত্যিই সফল** (verified)
           হলেও সেই পতাকা তক্ষুনি সরানো হতো না — শুধু পরের একই-ধাপের তালিকায়
           কাকতালীয়ভাবে মিলে গেলে, নয়তো ২৪ ঘণ্টা পরের ধীর heal-এর ভরসায়
           থাকত। যেই সেভে ধাপ/status/nextFollow-এর "বালতি" বদলায় (যেমন এই
           ফাংশনের নিজের ভিতরের nextFollow-অটো-বসানো), পরের তালিকা আর মেলে
           না ⇒ কার্ড চিরকাল পুরনো/Overdue দেখাত। এটাই V998/V1065/V1206/
           V1222/V1587-এর পরেও বারবার ফিরে আসার আসল কারণ — প্রতিটা আলাদা
           উপসর্গ সারানো হয়েছিল, এই মূল ফাঁকটা নয়।
           ⇒ এখন verified সফল হলে **সঙ্গে সঙ্গে** SYNCED করা হয়। */
        if (!reallySaved) queueFieldUpdate(id, fields)
        else context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", id, fields.optString("updatedAt")) } catch (_: Throwable) { } }
        return reallySaved
    }

    /* 📵🔒 V1206 — `stop` **শুধু তখনই** লেখা হয় যখন ডাকার জায়গা নিজে বলে দেয়:
       "আর কল লাগবে না" ⇒ true, স্টাফ নতুন তারিখ বাছলে ⇒ false (আবার চালু,
       আলাদা কোনো বোতাম লাগে না)।
       ⛔ ডিফল্ট `null` ⇒ বাকি সব পুরোনো ডাক (Dialer · Appointment · Draft ·
          Timeline) **এক অক্ষরও বদলায়নি**, ঘরটা তারা ছোঁয়ই না। */
    /* 🔴🔒 V1222 ③ (০৮.০৯.২০২৬ — Laxmi-র রিপোর্ট, TK-র অনুমতি নিয়ে):
       ডাকা জায়গাটা যেন জানতে পারে কাজটা **সত্যিই ক্লাউডে বসল, নাকি শুধু ফোনে
       জমা হলো** — নইলে স্টাফকে মিথ্যে "হয়ে গেছে" দেখানো হয়।
       ⛔ `updateNextFollow()`-এর ফেরত এক অক্ষরও বদলায়নি (আগের মতোই জমা হলেও
          সফল), তাই কোনো পর্দার পুরনো আচরণ ভাঙে না — এটা শুধু বাড়তি একটা সৎ খবর। */
    fun updateNextFollowSaved(id: String, nextFollow: String, stop: Boolean? = null): Boolean =
        updateNextFollowInner(id, nextFollow, stop)

    fun updateNextFollow(id: String, nextFollow: String, stop: Boolean? = null): Boolean =
        updateNextFollowInner(id, nextFollow, stop) || context != null

    private fun updateNextFollowInner(id: String, nextFollow: String, stop: Boolean? = null): Boolean {
        val fields = JSONObject()
            .put("nextFollow", nextFollow)
            .put("updatedAt", isoNow())
        if (stop != null) fields.put("noMoreCalls", stop)
        val knownRow = try {
            val found = SupabaseClient.fetchList(
                "followups", "id=eq.$id", 1,
                select = "id,mobile,stage,name,branch,disease,status,nextFollow,noMoreCalls,updatedAt"
            )
            if (found.length() > 0) found.getJSONObject(0) else null
        } catch (_: Exception) { null }
        rememberEditOnThisPhone(id, fields, knownRow)
        val cloudOk = SupabaseClient.updateById("followups", id, fields)
        /* 📵🔒 V1283 (১০.০৯.২০২৬, TK-রিপোর্ট ষষ্ঠবার ও অনুমতি — তালিকা সারি ৪০৬):
           TK: *"staff-দের বক্তব্য তারা আর কল করতে চায় না বলে চেপেছে, তারপরও
           আবার সেই সমস্ত নম্বর ফিরে আসে Today pending call-এ"*।
           🔴 **SQL-প্রমাণে ধরা আসল কারণ:** একই নম্বরে **জোড়া সারি** থাকে (heal-এ
           বানানো `fu_mskc7kv…`/`fu_pat_…`)। থামানোর চিহ্ন বসত শুধু **এই একটা**
           সারিতে (`ensureFollowUpRowId()` ক্লাউডের প্রথমটা বাছে), অথচ কার্ড ও
           গোনা চলে **জোড়াটা** দিয়ে; পরের কল/টাকায় V1065-এর নিয়ম জোড়াটায়
           আজকের তারিখ বসাত (৪০ থামানো সারির ৩টায় হুবহু এটাই মাপা)।
           ⇒ এখন থামালে/চালু করলে ওই নম্বরের **সব চালু সারিতে** একই চিহ্ন বসে
             (থামালে তারিখও ফাঁকা); চালু করলে (`stop=false`) জোড়ার শুধু চিহ্ন
             ওঠে — ওদের তারিখ ছোঁয়া হয় না।
           ⛔ Cancelled/Incomplete/Rejected/Closed সারি ছোঁয়া হয় না · কিছু মোছে না ·
              ইতিহাস অটুট · ব্যর্থ হলে জমা (queue) থেকে পরে আবার চেষ্টা।
           ⛔ `stop == null` (সাধারণ তারিখ-বদল) হলে এই ব্লক চলেই না — আগের আচরণ অটুট। */
        if (stop != null) {
            try {
                val kMob = digits(knownRow?.s("mobile").orEmpty())
                if (kMob.length == 10) {
                    val sibs = SupabaseClient.fetchListSlimOrNull(
                        "followups",
                        "mobile=like.*$kMob&status=not.in.(Cancelled,Incomplete,Rejected,Closed)",
                        20, "id,mobile,stage,status,nextFollow,noMoreCalls,updatedAt"
                    )
                    if (sibs != null) for (i in 0 until sibs.length()) {
                        val s = sibs.optJSONObject(i) ?: continue
                        val sid = s.optString("id", "")
                        if (sid.isBlank() || sid == id) continue
                        if (s.optBoolean("noMoreCalls", false) == stop && (!stop || s.s("nextFollow").isBlank())) continue
                        val sf = JSONObject().put("noMoreCalls", stop).put("updatedAt", isoNow())
                        if (stop) sf.put("nextFollow", "")
                        /* ⛔ জোড়ার জন্য ফোনের স্থানীয় কপিতে শুধু **থাকলে** ঘরগুলো বদলানো হয়;
                           নতুন আধখানা সারি (নাম ছাড়া) তৈরি করা হয় না — নইলে অন্য ট্যাবে
                           নামহীন কার্ড উঠতে পারত। ক্লাউডে বসল কি না সেটাই আসল। */
                        try { context?.let { LocalWorkflowStore(it).updateLocalFollowUp(sid, sf) } } catch (_: Throwable) { }
                        if (!SupabaseClient.updateById("followups", sid, sf)) queueFieldUpdate(sid, sf)
                    }
                }
            } catch (_: Throwable) { }
        }
        /* 📅🔒 V1245 (০৯.০৯.২০২৬, TK-রিপোর্ট ও অনুমতি — খাতার সারি ৩৬৬):
           TK: *"স্টাফরা বলছে রিমার্ক লিখেছি, পরের তারিখও দিয়েছি — তারপরেও কেন
           এগুলো আসবে"*।
           🔴 **আসল কারণ (কোড ধরে):** নতুন তারিখটা এতদিন **শুধু `followups`
              সারিতেই** বসত। কিন্তু এনকোয়ারি ট্যাবের জাল কার্ডটা বানায়
              `enquiries` টেবিল থেকে, আর ওই সারির `nextFollow` লেখা হয় **শুধু
              এনকোয়ারি তৈরির দিনে** — তারপর কোনোদিন বদলায় না। ⇒ জাল একবার
              চললেই কার্ডে **পুরনো তারিখটাই** ফিরে আসত, স্টাফের দেওয়া নতুন
              তারিখ নয় — দেখে মনে হত কেউ কল করেইনি।
           ⇒ এখন Inquiry ধাপে নতুন তারিখটা **এনকোয়ারির সারিতেও** বসে, ঠিক
             V445-এর প্রমাণিত ধাঁচে (দুই টেবিলের কথা এক রাখা)।
           ⛔ **বাড়তি কোনো পড়া নেই** — `knownRow` উপরে এমনিতেই আনা হয়েছে,
              সেখান থেকেই stage ও মোবাইল নেওয়া হয় (নিয়ম ১৩)।
           ⛔ শুধু `nextFollow` ঘরটাই — নাম · নম্বর · রিমার্ক · status · ইতিহাস
              কিছুই ছোঁয়া হয় না; সারি মোছাও হয় না।
           ⛔ ব্যর্থ হলে কিছুই ভাঙে না — `followups`-এর তারিখটাই আসল, এটা শুধু
              জালের জন্য বাড়তি সুরক্ষা।
           ⛔ Inquiry ছাড়া অন্য ধাপে (Visit/Patient) চলেই না — ওদের জাল আলাদা। */
        try {
            val kStage = knownRow?.s("stage").orEmpty().trim()
            val kMob = digits(knownRow?.s("mobile").orEmpty())
            if (cloudOk && kStage.equals("Inquiry", true) && kMob.length == 10 && nextFollow.isNotBlank()) {
                val eRows = SupabaseClient.findByMobileOrNull("enquiries", kMob, "id", 50)
                if (eRows != null) {
                    for (i in 0 until eRows.length()) {
                        val eid = eRows.optJSONObject(i)?.optString("id").orEmpty()
                        if (eid.isBlank()) continue
                        val ef = JSONObject().put("nextFollow", nextFollow).put("updatedAt", isoNow())
                        if (!SupabaseClient.updateById("enquiries", eid, ef)) {
                            context?.let { try { GenericUpdateQueue.queue(it, "enquiries", eid, ef) } catch (_: Throwable) { } }
                        }
                    }
                }
            }
        } catch (_: Throwable) { }
        // Same proof as the remark just above: an update that matched no row
        // still answers 200, so read it back before believing it.
        val reallySaved = cloudOk && try {
            val back = SupabaseClient.fetchList("followups", "id=eq.$id", 1, select = "id,nextFollow")
            back.length() > 0 && back.getJSONObject(0).optString("nextFollow") == nextFollow
        } catch (_: Exception) { false }
        if (!reallySaved) queueFieldUpdate(id, fields)
        else context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", id, fields.optString("updatedAt")) } catch (_: Throwable) { } }   // 🔴🔒 V1597
        return reallySaved || context != null
    }

    /**
     * Continue / Cancel an entry (native port of visitedContinue()/visitedCancel()
     * and signalContinueEntry()/signalCancelEntry()): "Continue" keeps the record
     * Active in its stage; "Cancel" sets status=Cancelled, which fetchTab()
     * already filters out (status=neq.Cancelled), removing it from the list.
     */
    // TK-REQUESTED (2026-07-21): Reject / Mark Incomplete never recorded WHO
    // did it. Fixed WITHOUT any new Supabase column -- reuses the exact same
    // "history" array field updateRemark() already writes to successfully
    // (proven working), just appending one more {date,time,remark,staff}
    // entry here too. staffName defaults to "" so every OTHER existing
    // caller of updateStatus (that doesn't pass it) behaves exactly as
    // before -- nothing else about this function changes.
    /**
     * 🚨 খাতার সারি B108 (TK, 29.07.2026 বিকেল ৩.৩৪, ছবিসহ — ANIKUL HAQUE):
     * *"Reject করেছিল Kishanganj STAFF, কিন্তু এখন আবার কেন শো করছে?"*
     *
     * Reject/Close করলে এতদিন **শুধু `followups` টেবিলে** দাগ পড়ত, `enquiries`
     * টেবিলে কিছুই না। অথচ এনকোয়ারি ট্যাবের জাল ওই `enquiries` টেবিল থেকেই
     * কার্ড ফিরিয়ে আনতে পারে — তাই কার্ডটা বারবার ফিরে আসত।
     *
     * এই ফাংশন ওই নম্বরের এনকোয়ারি সারিতেও দাগ বসায়, তাই দুই টেবিলের কথা
     * সবসময় এক থাকে।
     *
     * ⛔ **শুধু `status` ঘরটাই ছোঁয়া হয়** — নাম · নম্বর · রিমার্ক · তারিখ ·
     *    কে করেছিলেন, কিছুই বদলায় না, কোনো সারি মোছা হয় না।
     * ⛔ ব্যর্থ হলে কিছুই ভাঙে না — `followups`-এর দাগটা আগের মতোই থাকে,
     *    আর নেট ফিরলে সারিতে আবার চেষ্টা করা যায়।
     * ⛔ শুধু Reject/Incomplete-এর সময়েই চলে (রোজকার কাজে নয়), তাই ক্লাউডে
     *    বাড়তি চাপ নেই।
     * ⛔ Restore (`EnquiryRepository.restoreAndMove`) এই ঘরটাকেই `Active` করে
     *    দেয়, তাই ফিরিয়ে আনার পথও আগে থেকেই আছে।
     */
    /* 🔴🔴🔒 V445 (TK-রিপোর্ট ১৮.০৮.২০২৬, ছবিসহ — "রিজেক্ট/ডিলিট করা নম্বর
       অটোমেটিক ফিরে আসে")। **আসল কারণ (কোড ধরে যাচাই):** Reject করলে
       followups **ও** enquiries দুই টেবিলেই "Cancelled" বসানোর কথা, কিন্তু
       enquiries-এর অংশটা (`markEnquiryClosedByMobile`) নেট একটু বিলম্ব/
       ব্যর্থ হলে **চুপচাপ কিছুই না করে ফিরে যেত — কোনো রিট্রাই ছাড়াই।**
       ফলে followups-এ Cancelled বসলেও enquiries-এ পুরনো "Active" রয়ে যেত;
       পরে কোনো ফোন সিঙ্ক করার সময় সেই "Active" enquiries সারি দেখেই আবার
       নতুন কার্ড self-heal হয়ে যেত — এটাই "ভুতুড়ে ফিরে আসা"। (রিট্রাই-লাইন
       V446-এ patients.doctorComplete-এর সাথে একটাই জায়গায় একত্র করা হলো,
       নিচে দেখুন `mobileTaskPrefs`।) ⛔ close-করার আসল যুক্তি এক অক্ষরও
       বদলায়নি — শুধু ব্যর্থতার নিরাপত্তা-জাল যোগ হলো। */

    /* 🔴🔴🔒 V446 (TK-নির্দেশ ১৮.০৮.২০২৬ — "শুধু Enquiry বললেন, Visit ও Patient-এর
       কথা বললেন না")। TK ঠিক ধরেছেন — খুঁজে দেখা গেল **হুবহু একই বাগ** এখানেও
       ছিল: Patient/Treatment-stage Reject-এ `patients.doctorComplete=true`
       বসানোর অংশটাও (CHECK-UP Queue থেকে সরানোর জন্য, ০৪.০৮.২০২৬-এর ফিক্স)
       একই ambiguous `SupabaseClient.findByMobile` ব্যবহার করত — লুকআপ ব্যর্থ
       হলে চুপচাপ কিছুই করত না, রিট্রাই ছাড়াই। Rejected রোগী তাই CHECK-UP
       Queue-তে "ফিরে" থাকতে পারত। **সমাধান হুবহু একই প্যাটার্নে** — একটাই
       ভাগের-করা রিট্রাই-লাইন (`kind` দিয়ে enquiry/patient আলাদা), দুটোই
       এখন `findByMobileOrNull` + ব্যর্থ হলে রিট্রাই-লাইনে জমা। */
    private val mobileTaskPrefs = context?.getSharedPreferences("piles_clinic_mobiletask_pending", Context.MODE_PRIVATE)
    private fun queueMobileTask(
        kind: String, mobile: String, status: String, stage: String = "",
        remark: String = "", staffName: String = ""
    ) {
        val p = mobileTaskPrefs ?: return
        synchronized(LOCK) {
            val arr = try { JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            val next = JSONArray()
            var alreadyThere = false
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val same = e.optString("kind") == kind &&
                    e.optString("mobile").filter { it.isDigit() }.takeLast(10) == mobile.filter { it.isDigit() }.takeLast(10) &&
                    e.optString("stage") == stage
                if (same) {
                    if (!alreadyThere) {
                        next.put(JSONObject()
                            .put("kind", kind).put("mobile", mobile).put("status", status)
                            .put("stage", stage).put("remark", remark).put("staffName", staffName))
                        alreadyThere = true
                    }
                } else next.put(e)
            }
            if (!alreadyThere) next.put(JSONObject()
                .put("kind", kind).put("mobile", mobile).put("status", status)
                .put("stage", stage).put("remark", remark).put("staffName", staffName))
            p.edit().putString("queue", next.toString()).apply()
        }
    }
    private fun flushMobileTaskPending() {
        val p = mobileTaskPrefs ?: return
        synchronized(LOCK) {
            val arr = try { JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            if (arr.length() == 0) return
            val stillPending = JSONArray()
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val kind = e.optString("kind")
                val mob = e.optString("mobile")
                val status = e.optString("status")
                val stage = e.optString("stage")
                if (mob.isBlank()) continue
                val done = when (kind) {
                    "patient" -> markPatientDoctorCompleteByMobile(mob)
                    "followup_stage" -> closeSiblingFollowUpsInternal(
                        mob, stage, "", status, e.s("remark"), e.s("staffName"), false   // 🔴🔒 V696
                    )
                    else -> markEnquiryClosedByMobile(mob, status)
                }
                if (!done) stillPending.put(e)
            }
            p.edit().putString("queue", stillPending.toString()).apply()
        }
    }

    /** Patient/Treatment-stage Reject/Incomplete-এর সময় ওই মোবাইলের
     *  `patients.doctorComplete=true` বসায়, যাতে CHECK-UP Queue থেকে সরে
     *  (০৪.০৮.২০২৬-এর ফিক্সের ঠিক একই কাজ, শুধু এখন নিরাপদ + রিট্রাই-যোগ্য)। */
    fun markPatientDoctorCompleteByMobile(mobile: String): Boolean {
        return try {
            val d = mobile.filter { it.isDigit() }.takeLast(10)
            if (d.length != 10) return false
            val rows = SupabaseClient.findByMobileOrNull("patients", d, "id,doctorComplete", 20)
            if (rows == null) { queueMobileTask("patient", mobile, ""); return false }
            if (rows.length() == 0) return true
            var ok = true
            for (i in 0 until rows.length()) {
                val prow = rows.getJSONObject(i)
                val pid = prow.optString("id")
                if (pid.isBlank()) continue
                if (prow.optBoolean("doctorComplete", false)) continue
                val pfields = JSONObject().put("doctorComplete", true).put("updatedAt", isoNow())
                if (!SupabaseClient.updateById("patients", pid, pfields)) {
                    ok = false
                    context?.let { try { GenericUpdateQueue.queue(it, "patients", pid, pfields) } catch (_: Throwable) { } }
                }
            }
            ok
        } catch (_: Throwable) { queueMobileTask("patient", mobile, ""); false }
    }

    fun markEnquiryClosedByMobile(mobile: String, status: String): Boolean {
        return try {
            val d = mobile.filter { it.isDigit() }.takeLast(10)
            if (d.length != 10) return false
            val rows = SupabaseClient.findByMobileOrNull("enquiries", d, "id,status", 50)
            if (rows == null) { queueMobileTask("enquiry", mobile, status); return false }
            if (rows.length() == 0) return true
            var allOk = true
            for (i in 0 until rows.length()) {
                val eid = rows.getJSONObject(i).optString("id")
                if (eid.isBlank()) continue
                val fields = JSONObject().put("status", status).put("updatedAt", isoNow())
                if (!SupabaseClient.updateById("enquiries", eid, fields)) {
                    allOk = false
                    context?.let { try { GenericUpdateQueue.queue(it, "enquiries", eid, fields) } catch (_: Throwable) { } }
                }
            }
            if (!allOk) queueMobileTask("enquiry", mobile, status)
            allOk
        } catch (_: Throwable) { queueMobileTask("enquiry", mobile, status); false }
    }

    // V215 (§16/§6, 31.07.2026): `cloudConfirmedOut` — ঐচ্ছিক out-param।
    // null থাকলে আগের সব caller হুবহু আগের মতোই কাজ করে (return unchanged)।
    // যে caller সত্যিকারের cloud-confirm জানতে চায় (Incomplete/Reject) সে একটা
    // BooleanArray(1) পাঠায়; ভিতরে cloudConfirmedOut[0] = আসল cloudOk বসে।
    // updateById নিজেই "matched no row" ধরে (return=representation), তাই cloudOk
    // মানে সত্যিই cloud-এ সারিটা বদলেছে — মিথ্যা "Saved" আর দেখানো লাগে না।
    fun updateStatus(
        id: String, status: String, remark: String = "", staffName: String = "",
        cloudConfirmedOut: BooleanArray? = null, mobileHint: String = "", stageHint: String = ""
    ): Boolean {
        val fields = JSONObject().put("status", status).put("updatedAt", isoNow())
        if (remark.isNotBlank()) fields.put("lastRemark", remark)
        /* 🔴🔒 V814 (২৮.০৮.২০২৬, TK-রিপোর্ট "ASBEN এখনো কেন?") — লেখাটা **কবে
           লেখা হলো** সেটা এখন আলাদা ঘরে বসে। `updatedAt` অন্য কাজেও (যেমন
           `updateNextFollow`) আজকের হয়ে যেত, তাই সেটা দিয়ে "আজকের নোট কি না"
           বোঝা যেত না — পুরনো লেখা আজকের সেজে চেম্বার-বন্ধের পাহারা পার হত।
           ⛔ ঘরটা **শুধু তখনই** লেখা হয় যখন রিমার্কের কথাটা সত্যিই বদলায়। */
        if (remark.isNotBlank()) fields.put("lastRemarkAt", isoNow())
        val terminal = status.equals("Cancelled", true) || status.equals("Incomplete", true) ||
            status.equals("Rejected", true) || status.equals("Closed", true)
        var knownRow: JSONObject? = null
        if (staffName.isNotBlank() || (terminal && (mobileHint.isBlank() || stageHint.isBlank()))) {
            val existing = SupabaseClient.fetchList(
                "followups", "id=eq.$id", 1,
                select = "id,mobile,stage,name,branch,disease,status,history,refId,patientId,updatedAt"
            )
            val row = if (existing.length() > 0) existing.getJSONObject(0)
                else context?.let { LocalWorkflowStore(it).findFollowUp(id) } ?: JSONObject()
            knownRow = row
            if (staffName.isNotBlank()) {
                val history = row.optJSONArray("history") ?: JSONArray()
                history.put(JSONObject().put("date", FollowUpModel.today()).put("time", isoNow())
                    .put("remark", remark.ifBlank { status }).put("staff", staffName)
                    .put("status", status).put("decisionVersion", "V450"))
                fields.put("history", history)
            }
        }
        rememberEditOnThisPhone(id, fields, knownRow)
        val mainCloudOk = if (id.isNotBlank()) SupabaseClient.updateById("followups", id, fields) else false
        if (!mainCloudOk && id.isNotBlank()) queueFieldUpdate(id, fields)
        // 🔴🔒 V1597 — status/terminal বদল মানেই পরের একই-ধাপের তালিকা থেকে
        // সারিটা সরে যায়, তাই সঙ্গে সঙ্গে SYNCED না করলে চিরকাল আটকে থাকত।
        else if (id.isNotBlank()) context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", id, fields.optString("updatedAt")) } catch (_: Throwable) { } }

        var durableCloudOk = mainCloudOk
        if (terminal) {
            val mob = mobileHint.ifBlank { knownRow?.s("mobile").orEmpty() }.ifBlank {
                try { SupabaseClient.fetchListSlim("followups", "id=eq.$id", 1, "id,mobile,stage")
                    .optJSONObject(0)?.s("mobile").orEmpty() } catch (_: Throwable) { "" }
            }
            val stg = stageHint.ifBlank { knownRow?.s("stage").orEmpty() }.ifBlank {
                try { SupabaseClient.fetchListSlim("followups", "id=eq.$id", 1, "id,mobile,stage")
                    .optJSONObject(0)?.s("stage").orEmpty() } catch (_: Throwable) { "" }
            }
            val enquiryOk = if (mob.isNotBlank()) markEnquiryClosedByMobile(mob, status) else false
            /* 🔵🔒 V536: যে কার্ডটার status বদলাচ্ছে, **সেই কার্ডের পরিচয়** সাথে
               যায় — তাই এক নম্বরে দু'জন থাকলে অন্যজনের কার্ড ছোঁয়া হয় না।
               ⛔ `knownRow` না থাকলে তিনটেই ফাঁকা ⇒ হুবহু আগের আচরণ। */
            val siblingsOk = if (mob.isNotBlank() && stg.isNotBlank())
                closeSiblingFollowUps(
                    mob, stg, "", status, remark, staffName,
                    knownRow?.s("refId").orEmpty(),
                    knownRow?.s("patientId").orEmpty(),
                    knownRow?.s("name").orEmpty()
                ) else false
            val patientOk = if (mob.isNotBlank() && (stg.equals("Patient", true) || stg.equals("Treatment", true)))
                markPatientDoctorCompleteByMobile(mob) else true
            durableCloudOk = mainCloudOk && enquiryOk && siblingsOk && patientOk
        }
        cloudConfirmedOut?.set(0, durableCloudOk)
        return mainCloudOk || context != null
    }

    /** খাতার সারি (30.07.2026, ডুপ্লিকেট Reject বাগ): একই মোবাইল ও একই
     *  stage-এর অন্য যেকোনো এখনো-Active `followups` সারি — সবগুলোকেই
     *  একই status/history দিয়ে বন্ধ করে। updateStatus()-এর হুবহু একই
     *  fields-বানানোর নিয়ম পুনর্ব্যবহার করা হলো, যাতে দুই জায়গায় দুই
     *  রকম না হয়ে যায়। */
    // 🔵 খাতার সারি (Claude, 09.09.2026 — TK: Reject করা নম্বর নতুন অ্যাপে আবার আসছিল,
    //    আগের ফিক্স অসম্পূর্ণ ছিল): এটা এখন **public** — Reject-এর সময় সরাসরি ডাকা হয়
    //    কার্ডে থাকা **আসল মোবাইল** দিয়ে (excludeId=""), যাতে followups সারি id-এর উপর
    //    নির্ভর না করে মোবাইল ধরেই Cancelled হয়। id ভুল/না-মিললেও নম্বর আর ফিরবে না।
    fun closeSiblingFollowUps(
        mobile: String, stage: String, excludeId: String, status: String, remark: String, staffName: String,
        myRowId: String = "", myCode: String = "", myName: String = ""   // 🔵 V536 (ফাঁকা = আগের আচরণ)
    ): Boolean = closeSiblingFollowUpsInternal(mobile, stage, excludeId, status, remark, staffName, true,
        myRowId, myCode, myName)

    /**
     * 🔵🔒 V536 (২২.০৮.২০২৬, TK-নির্দেশ): `myRowId` / `myCode` / `myName` দিলে
     * এক নম্বরে দু'জন আলাদা রোগী থাকলে **অন্যজনের কার্ডে হাত পড়ে না**।
     * ⛔ তিনটেই ফাঁকা, বা প্রমাণ না থাকলে ⇒ **হুবহু আগের আচরণ**।
     */
    private fun closeSiblingFollowUpsInternal(
        mobile: String, stage: String, excludeId: String, status: String, remark: String,
        staffName: String, queueOnFailure: Boolean,
        myRowId: String = "", myCode: String = "", myName: String = ""
    ): Boolean {
        val d = mobile.filter { it.isDigit() }.takeLast(10)
        if (d.length != 10 || stage.isBlank()) return false
        val siblings = SupabaseClient.fetchListSlimOrNull(
            "followups",
            "mobile=like.*$d&stage=eq.$stage&status=not.in.(Cancelled,Incomplete,Rejected,Closed)",
            50, "id,mobile,stage,status,history,refId,patientId,name"   // 🔵 V536: বাছাইয়ের ৩টে ঘর
        )
        if (siblings == null) {
            if (queueOnFailure) queueMobileTask("followup_stage", mobile, status, stage, remark, staffName)
            return false
        }
        var allOk = true
        for (i in 0 until siblings.length()) {
            val row = siblings.getJSONObject(i)
            val sid = row.optString("id")
            if (sid.isBlank() || sid == excludeId) continue
            // 🔵 V536: প্রমাণসহ অন্য রোগীর সারি ⇒ ছোঁয়া হয় না।
            if (PatientIdentity.provablyOtherPatient(row, d, myRowId, myCode, myName)) continue
            val fields = JSONObject().put("status", status).put("updatedAt", isoNow())
            if (remark.isNotBlank()) fields.put("lastRemark", remark)
        /* 🔴🔒 V814 (২৮.০৮.২০২৬, TK-রিপোর্ট "ASBEN এখনো কেন?") — লেখাটা **কবে
           লেখা হলো** সেটা এখন আলাদা ঘরে বসে। `updatedAt` অন্য কাজেও (যেমন
           `updateNextFollow`) আজকের হয়ে যেত, তাই সেটা দিয়ে "আজকের নোট কি না"
           বোঝা যেত না — পুরনো লেখা আজকের সেজে চেম্বার-বন্ধের পাহারা পার হত।
           ⛔ ঘরটা **শুধু তখনই** লেখা হয় যখন রিমার্কের কথাটা সত্যিই বদলায়। */
        if (remark.isNotBlank()) fields.put("lastRemarkAt", isoNow())
            if (staffName.isNotBlank()) {
                val history = row.optJSONArray("history") ?: JSONArray()
                history.put(JSONObject().put("date", FollowUpModel.today()).put("time", isoNow())
                    .put("remark", remark.ifBlank { status }).put("staff", staffName)
                    .put("status", status).put("decisionVersion", "V450"))
                fields.put("history", history)
            }
            rememberEditOnThisPhone(sid, fields, row)
            if (!SupabaseClient.updateById("followups", sid, fields)) {
                allOk = false
                queueFieldUpdate(sid, fields)
            } else {
                // 🔴🔒 V1597
                context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", sid, fields.optString("updatedAt")) } catch (_: Throwable) { } }
            }
        }
        // One tiny, reject-only verification prevents a stale/duplicate Active row from
        // being reported as closed. This does not add any recurring screen-load request.
        val stillOpen = SupabaseClient.fetchListSlimOrNull(
            "followups",
            "mobile=like.*$d&stage=eq.$stage&status=not.in.(Cancelled,Incomplete,Rejected,Closed)",
            1, "id"
        )
        if (stillOpen == null || stillOpen.length() > 0) allOk = false
        if (!allOk && queueOnFailure) queueMobileTask("followup_stage", mobile, status, stage, remark, staffName)
        return allOk
    }

    /**
     * Log an enquiry call (web signalTripleTap): one call per day max; otherwise
     * increment callCount and stamp lastCallDate=today. Returns:
     *   -1 = already called today; else the new callCount (>=1).
     */
    fun logEnquiryCall(id: String): Int {
        val today = FollowUpModel.today()
        val rows = SupabaseClient.fetchList(
            "followups", "id=eq.$id", 1,
            select = "id,mobile,stage,name,branch,disease,status,lastCallDate,callCount,nextFollow,updatedAt"
        )
        val row = if (rows.length() > 0) rows.getJSONObject(0)
            else context?.let { LocalWorkflowStore(it).findFollowUp(id) } ?: return -2
        val lastCall = if (row.isNull("lastCallDate")) "" else row.optString("lastCallDate", "")
        if (lastCall == today) return -1
        val newCount = row.optInt("callCount", 0) + 1
        val fields = JSONObject()
            .put("callCount", newCount)
            .put("lastCallDate", today)
            .put("updatedAt", isoNow())
        /* 📞🔒 V998 (০৩.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — Susmita Das · MD RAJ):
           *"NEXT CALL, LAST CALL-এর আগের তারিখ, তাই চিরকাল Overdue"*।

           **আসল কারণ (কোড ধরে, আন্দাজ নয়):** ডায়ালার থেকে কল হলে
           `DialerRepository.saveCallRemark()` এই ফাংশনটা ডাকে — এটা কল গোনে ও
           `lastCallDate` আজকের করে, **কিন্তু `nextFollow` কখনো ছোঁয় না**।
           স্টাফ পরের কলের তারিখ বেছে দিলে তবেই V873-এর পথে সেটা বসে; না
           দিলে পুরনো তারিখটাই থেকে যায় ⇒ সারিটা চিরকাল "Overdue"।

           **এখন:** তারিখটা **ফাঁকা বা আজকের আগের** হলেই আজকের দিন বসে —
           অর্থাৎ সারিটা "আজকের কল"-এ ফেরে, পুরনো তারিখে আটকে থাকে না।
           ⛔ **ভবিষ্যতের তারিখ কখনো ছোঁয়া হয় না** — স্টাফের বেছে দেওয়া
              পরের তারিখ অক্ষত।
           ⛔ স্টাফ নিজে তারিখ বাছলে সেটাই শেষ কথা: `saveCallRemark()`-এ
              `updateNextFollow()` এই ফাংশনের **পরে** চলে, তাই সে-ই জেতে।
           ⛔ সারি তালিকা থেকে হারায় না — "বকেয়া" থেকে "আজকের"-এ সরে আসে। */
        val curNext = if (row.isNull("nextFollow")) "" else row.optString("nextFollow", "")
        if (curNext.isBlank() || curNext < today) fields.put("nextFollow", today)
        rememberEditOnThisPhone(id, fields, row)
        val ok = SupabaseClient.updateById("followups", id, fields)
        if (!ok) queueFieldUpdate(id, fields)
        // 🔴🔒 V1597 — এই ফাংশনই nextFollow-কে "আজকের"-এ সরায় (V998), তাই
        // বালতি বদলের সঙ্গে সঙ্গেই SYNCED না করলে ঠিক এই বাগটাই আবার তৈরি হয়।
        else context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", id, fields.optString("updatedAt")) } catch (_: Throwable) { } }
        return if (ok || context != null) newCount else -2
    }

    /** Reset an enquiry's call count after "Continue" past the 5-call limit.
     *
     * 🔴 B377 (TK-নির্দেশ, 03.08.2026 — "Continue চাপার সাথে সাথেই কমপক্ষে ১
     * বার দেখানো উচিত"): আগে এখানে সরাসরি ০ বসানো হতো, তাই Continue করার
     * পরে 📶 সিগন্যাল পুরো Nill দেখাত — যেন এই এন্ট্রি একদম নতুন, কখনো কল
     * করাই হয়নি। এখন ১ বসে (নতুন Enquiry-র B215 নিয়মের সাথে মিলিয়ে —
     * "ফর্ম/এন্ট্রি সচল থাকলে সিগন্যাল কখনো একদম শূন্য দেখাবে না")।
     * ⛔ ওয়েবেও (`app.js`-এর `followActionGuard`) আসল ফলাফল ইতিমধ্যে ১-ই
     * ছিল — ওখানে ০ বসানোর ঠিক পরের লাইনেই (`updateFollowAction`-এর
     * `inc` হিসাব) কল-লগের সাধারণ বৃদ্ধি (+1) একই মুহূর্তে চলে, তাই আসল
     * সেভ হওয়া মান কখনোই ০ থাকেনি — শুধু Android-এর এই আলাদা ফাংশনে
     * (কোনো পরের বৃদ্ধি ছাড়াই) সত্যিকারের ০-তেই আটকে থাকত। তাই এই বদল
     * Android-কে ওয়েবের প্রকৃত আচরণের সাথে মেলাচ্ছে, নতুন কিছু বানাচ্ছে না।
     * ⛔ **৫-বার-সীমা/Cancel Entry পথ এক অক্ষরও বদলায়নি** — শুধু Continue-এর
     * পরের সংখ্যা। */
    fun resetCallCount(id: String): Boolean {
        val fields = JSONObject().put("callCount", 1).put("updatedAt", isoNow())
        rememberEditOnThisPhone(id, fields, null)
        val cloudOk = SupabaseClient.updateById("followups", id, fields)
        if (!cloudOk) queueFieldUpdate(id, fields)
        else context?.let { ctx -> try { LocalWorkflowStore(ctx).markRowSyncedIfCloudCaughtUp("followups", id, fields.optString("updatedAt")) } catch (_: Throwable) { } }   // 🔴🔒 V1597
        return cloudOk || context != null
    }

    /**
     * 🚨 TK-REPORTED, LIVE (29.07.2026 সকাল ১০.৪২, খাতার সারি B78 — কিশনগঞ্জের
     * স্টাফ লক্ষ্মী, ফটো-প্রুফসহ): *"বারবার Reject করছে, কিন্তু Reject হচ্ছে না।"*
     * পর্দায় **"Moved to Reject list"** দেখাত, তবু কার্ডটা তালিকায় থেকে যেত।
     *
     * **আসল কারণ (কোড ধরে খুঁজে পাওয়া, আন্দাজ নয়):**
     * এনকোয়ারি ট্যাবে কিছু কার্ড আসে **`enquiries` টেবিল থেকে** — যখন ওই
     * নম্বরের `followups` সারিটা কোনো কারণে তৈরি হয়নি (সেভের সময় লাইন কেটে
     * যাওয়া ইত্যাদি)। ওই কার্ডের `id` তখন **`enquiries` সারির আইডি**।
     * Reject চাপলে `followups` টেবিলে ওই আইডি দিয়ে বদল পাঠানো হত — যেখানে
     * ওই আইডির কোনো সারিই নেই। **Supabase তখনো "200 OK" বলে** (শূন্য সারি
     * বদলালেও), তাই অ্যাপ ভাবত কাজ হয়ে গেছে ও "Moved to Reject list" দেখাত —
     * অথচ ডেটাবেসে কিছুই বদলায়নি, তাই পরের বার তালিকায় নামটা আবার আসত।
     *
     * **এই ফাংশন সেটাই বন্ধ করে:** বদল পাঠানোর আগে নিশ্চিত করে যে
     * `followups`-এ সত্যিই একটা সারি **আছে** — না থাকলে সারিটা **তৈরি করে
     * দেয়**, তারপর তার আসল আইডি ফেরত দেয়। তাই Reject · রিমার্ক · পরের কলের
     * তারিখ — কোনোটাই আর ফাঁকা জায়গায় গিয়ে হারাবে না।
     *
     * 🔒 **ডুপ্লিকেট তৈরি হওয়া ঠেকানো (সবচেয়ে জরুরি):** খোঁজার জন্য
     * `fetchListOrNull` ব্যবহার হয় — **নেট খারাপ হলে সেটা `null` ফেরত দেয়**,
     * আর তখন পুরনো আচরণেই ফিরে যাওয়া হয় (কার্ডের নিজের আইডি), **নতুন সারি
     * তৈরি করা হয় না**। শুধু ক্লাউড স্পষ্ট করে "এই নম্বরের কোনো সারি নেই"
     * বললে তবেই একটা সারি লেখা হয়। ⛔ তাই একই রোগীর দুটো সারি কখনো হবে না।
     *
     * ⛔ কোনো ডিজাইন · ছাঁকনি · টাকার হিসাব বদলায় না। বাড়তি ক্লাউড-কল নেই —
     * আগের `resolveFollowUpId()`-ও ঠিক এই দুটো খোঁজাই করত।
     */
    fun ensureFollowUpRowId(item: FollowUpItem): String {
        return try {
            val m = item.mobile.filter { it.isDigit() }.takeLast(10)
            if (m.length != 10) return item.id

            val sameStage = SupabaseClient.fetchListOrNull(
                "followups", "mobile=like.*$m&stage=eq.${item.stage}", 5, select = "id"
            ) ?: return item.id                       // নেট খারাপ — পুরনো আচরণ
            if (sameStage.length() > 0) {
                val rid = sameStage.getJSONObject(0).optString("id")
                if (rid.isNotBlank()) return rid
            }

            val anyStage = SupabaseClient.fetchListOrNull(
                "followups", "mobile=like.*$m", 5, select = "id"
            ) ?: return item.id                       // নেট খারাপ — পুরনো আচরণ
            if (anyStage.length() > 0) {
                val rid = anyStage.getJSONObject(0).optString("id")
                if (rid.isNotBlank()) return rid
            }

            // ক্লাউডে এই নম্বরের কোনো সারি নেই। তার মানে দুটোর একটা —
            //   (ক) সারিটা **এই ফোনে** সেভ হয়েছে কিন্তু ক্লাউডে ওঠেনি, অথবা
            //   (খ) সারিটা সত্যিই কোথাও নেই (কম্পিউটার থেকে শুধু এনকোয়ারি
            //       লেখা হয়েছিল, ফলো-আপের সারি তৈরি হয়নি)।
            // (ক)-এর ক্ষেত্রে **নতুন সারি বানানো যাবে না** — তাহলে ফোনের
            // পুরনো সারিটা Active থেকে যেত আর Reject করা কার্ড ফোনের তালিকায়
            // আবার ফিরে আসত। তাই আগে ফোনের নিজের খাতা দেখা হয়।
            val ctx = context
            if (ctx != null) {
                val mine = try { LocalWorkflowStore(ctx).findFollowUpByMobile(item.mobile) } catch (_: Throwable) { null }
                val mineId = mine?.optString("id") ?: ""
                if (mineId.isNotBlank()) return mineId
            }

            // (খ) — সত্যিই কোথাও নেই, এখন একটা সারি তৈরি করা নিরাপদ।
            // গঠনটা এনকোয়ারি-ফলব্যাকের `healRow`-এর হুবহু একই, যাতে দুই পথে
            // তৈরি সারি কখনো আলাদা না হয়।
            // 🔒 খাতার সারি B80 (TK, 29.07.2026 — *"এন্ট্রিটা আমি করেছিলাম, অন্য
            // জনের নাম কেন দেখাচ্ছে?"*): নতুন সারিতে **কে তৈরি করেছিলেন** সেটা
            // অবশ্যই বসাতে হবে, নইলে ঘরটা ফাঁকা থেকে যেত আর পরে "কে করেছে"
            // বলাই যেত না। আসল এনকোয়ারি সারি থেকে সেটা তুলে আনা হয় —
            // ⛔ এই একটামাত্র পড়া শুধু এই বিরল ক্ষেত্রেই হয় (যখন সারিটা সত্যিই
            // কোথাও নেই), তাই রোজকার ব্যবহারে বাড়তি কোনো ক্লাউড-কল নেই।
            var origCreatedBy = ""
            try {
                val src = SupabaseClient.findByMobile("enquiries", item.mobile, "createdBy,receivedBy", 1)
                if (src.length() > 0) {
                    val r = src.getJSONObject(0)
                    origCreatedBy = r.s("createdBy").ifBlank { r.s("receivedBy") }
                }
            } catch (_: Throwable) { }

            val newId = "fu_" + java.util.UUID.randomUUID().toString().replace("-", "")
            val now = isoNow()
            val row = JSONObject()
                .put("id", newId)
                .put("refId", item.id)
                .put("mobile", item.mobile)
                .put("name", item.name)
                .put("branch", item.branch)
                .put("disease", item.disease)
                .put("address", item.address)
                .put("stage", item.stage.ifBlank { "Inquiry" })
                .put("date", item.recordDate)
                .put("registrationDate", item.recordDate)
                .put("visitDate", item.recordDate)
                .put("lastRemark", item.lastRemark)
                .put("nextFollow", item.nextFollow)
                .put("callCount", item.callCount)
                .put("status", "Active")
                .put("history", JSONArray())
                .put("createdBy", origCreatedBy)
                .put("createdAt", item.createdAt.ifBlank { item.recordDate.ifBlank { now } })
                .put("updatedAt", now)
            val written = try { SupabaseClient.upsert("followups", row) } catch (_: Throwable) { false }
            if (written) newId else item.id
        } catch (_: Exception) { item.id }
    }

    /**
     * 🚨 TK-REPORTED, LIVE (29.07.2026 রাত ৮.১০, দুটো ছবিসহ · খাতার সারি B96):
     * *"Reject করছি এবং ডিলিট করছি — কোনটাতেই কোনো কাজ হয় না।"*
     * (JHINUK BISWAS · 7872272742 · "Enquiry only — not registered")
     *
     * **আসল কারণ (কোড ধরে, আন্দাজ নয়):** এটা **খাতার সারি B78-এর হুবহু একই
     * রোগ**, কিন্তু ওই ওষুধটা তখন শুধু Follow-up ও Follow-up ক্যালেন্ডার পর্দায়
     * বসানো হয়েছিল — **রোগীর Timeline পর্দায় বসানো হয়নি**।
     * ওখানকার `resolveFollowUpIdHere()` সারিটা না পেলে **এনকোয়ারির আইডিটাই**
     * ফেরত দিত; তারপর `followups?id=eq.<এনকোয়ারির আইডি>`-তে বদল পাঠানো হত —
     * **কোনো সারির সঙ্গে মেলে না, অথচ Supabase "200 OK" বলে**, তাই অ্যাপ
     * ভাবত কাজ হয়ে গেছে আর আসলে কিছুই হত না।
     *
     * এই ফাংশনটা উপরের `ensureFollowUpRowId(item)`-এর **হুবহু একই কাজ** করে,
     * শুধু `FollowUpItem`-এর বদলে আলাদা ঘরগুলো নেয় — কারণ Timeline পর্দায়
     * `FollowUpItem` থাকে না। ⛔ নিয়ম · ধাপ · পাহারা সব এক, তাই দুই পথে ফল
     * কখনো আলাদা হবে না।
     */
    fun ensureFollowUpRowIdFor(
        mobile: String, stage: String, name: String, branch: String,
        disease: String = "", lastRemark: String = "", nextFollow: String = "",
        recordDate: String = "", fallbackId: String = ""
    ): String {
        // ⚠️ `bill` ও `paid` ঘর দুটোর কোনো ডিফল্ট নেই (`FollowUpModel.kt` দেখে
        // মিলিয়ে নেওয়া), তাই দুটোই স্পষ্ট করে দিতে হয় — নইলে বিল্ড ভাঙত।
        // ⛔ এখানে টাকার কোনো হিসাব লাগে না, তাই ০ — সারি খোঁজা/তৈরির কাজে
        //    এই দুটো ঘর ব্যবহারই হয় না।
        val item = FollowUpItem(
            id = fallbackId,
            name = name,
            mobile = mobile,
            branch = branch,
            disease = disease,
            stage = stage.ifBlank { "Inquiry" },
            lastRemark = lastRemark,
            nextFollow = nextFollow,
            recordDate = recordDate,
            bill = 0.0,
            paid = 0.0
        )
        return ensureFollowUpRowId(item)
    }

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
}
