package com.tkbiswas.pilesclinic.native

import org.json.JSONArray

class DoctorVisitRepository {

    /** Matches doctorBranchListRows(): Master/field/branch="All" see
     * everything; branch staff see only their own branch.
     * TK-REPORTED BUG FIX (2026-07-23): this used to be "status=eq.Active"
     * only, so an old-data doctor row with a blank/missing status field
     * (e.g. migrated before "status" existed) was invisible here and in
     * search -- even though checkDuplicate() below still found it by
     * mobile (it never filtered by status), so Staff/Master got "Already
     * exists" on Add but could never find or view that same doctor. Now
     * blank status counts the same as Active. A genuinely different status
     * value (if one is ever introduced later) would still be excluded. */
    fun fetchList(branchFilter: String?): List<DoctorVisitItem> {
        val filters = mutableListOf("or=(status.eq.Active,status.is.null)")
        if (branchFilter != null && branchFilter != "All") {
            filters.add("branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}")
        }
        // TK-REQUESTED SAFETY FIX (2026-07-16): same fix as DoctorQueueRepository
        // -- explicit high limit so an older Active doctor-visit request never
        // silently falls outside the default 500-row window.
        // TK-REPORTED BUG FIX (2026-07-25, from Staff's live report --
        // "doctor list order keeps jumping around, sometimes top,
        // sometimes bottom, sometimes middle"): this used to inherit the
        // shared fetchList() default order (most-recently-UPDATED first),
        // which is right for an activity feed but wrong for a reference
        // list of doctors -- editing/calling ANY doctor made them jump to
        // the top, reshuffling everyone else's position under the staff's
        // feet. Now explicitly ordered by name (stable, alphabetical) so
        // the list stays in the same place regardless of who was edited
        // most recently.
        /* 🔵🔒 V515 (২২.০৮.২০২৬, TK-নির্দেশ — Egress অডিট): `fetchList` →
           `fetchListGuarded`। **অনুরোধ ও সাজানোর ক্রম হুবহু আগেরটাই**
           (`name.asc` — ২০২৬-০৭-২৫-এর "তালিকা লাফায়" বাগের সমাধান অটুট),
           শুধু V513/V514-এর পাহারার ভিতর দিয়ে যায়। */
        val rows = SupabaseClient.fetchListGuarded("doctor_visits", filters.joinToString("&"), 5000, order = "name.asc")
        val items = mutableListOf<DoctorVisitItem>()
        for (i in 0 until rows.length()) items.add(DoctorVisitModel.parse(rows.getJSONObject(i)))
        return items
    }

    /** TK-REQUESTED (2026-07-20): raw rows for the cache-first display
     *  pattern -- exactly the same query/filter as fetchList() above, but
     *  returns the untouched cloud rows so the screen can cache them and
     *  re-parse with the SAME DoctorVisitModel.parse (cached view == fresh
     *  view, byte-for-byte). fetchList() is left completely unchanged. */
    fun fetchListRaw(branchFilter: String?): JSONArray {
        // TK-REPORTED BUG FIX (2026-07-23): same blank-status fix as
        // fetchList() above, kept identical so the cache-first cached view
        // never disagrees with the live-fetch view.
        val filters = mutableListOf("or=(status.eq.Active,status.is.null)")
        if (branchFilter != null && branchFilter != "All") {
            filters.add("branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}")
        }
        // TK-REPORTED BUG FIX (2026-07-25): same stable-order fix as
        // fetchList() above -- MUST stay identical to it (same comment as
        // above: cached view == fresh view, byte-for-byte).
        /* 🔵🔒 V515: উপরের `fetchList()`-এর সঙ্গে **হুবহু এক** থাকতেই হবে
           (cached view == fresh view, byte-for-byte) — তাই এটাও পাহারার
           ভিতর দিয়ে, একই অনুরোধ, একই ক্রম। */
        return SupabaseClient.fetchListGuarded("doctor_visits", filters.joinToString("&"), 5000, order = "name.asc")
    }

    // TK-REPORTED CRITICAL BUG FIX (2026-07-24): fetchListRaw() above uses
    // plain fetchList(), which silently returns an EMPTY array on ANY
    // network failure (slow/very-weak connection included) -- completely
    // indistinguishable from "genuinely zero doctors for this branch".
    // DoctorVisitActivity.loadList() used to treat that empty result as
    // real and WRITE IT INTO THE LOCAL CACHE (saveCachedDoctors), which
    // then kept showing "No doctors found" on every future open too, even
    // once the network recovered -- a single bad-network moment could
    // permanently wipe the on-screen (not the cloud) doctor list. This
    // returns null on a genuine failure so the caller can tell the two
    // situations apart and NEVER overwrite the cache with a failure.
    fun fetchListRawOrNull(branchFilter: String?): JSONArray? {
        return SupabaseClient.fetchListOrNull("doctor_visits", listFilter(branchFilter), 5000)
    }

    private fun listFilter(branchFilter: String?): String {
        val filters = mutableListOf("or=(status.eq.Active,status.is.null)")
        if (branchFilter != null && branchFilter != "All") {
            filters.add("branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}")
        }
        return filters.joinToString("&")
    }

    /* ═══════════════════════════════════════════════════════════════════
       🔴🔒 V580 (২৩.০৮.২০২৬) — **Doctor/RMP বোর্ড: শুধু যেটুকু বদলেছে**

       TK-এর নির্দেশে ডেটাবেস মেপে দেখা গেছে (২৩.০৮ সকাল ৯:৫১) —
       `doctor_visits` টেবিলটা **৫ MB, ২০৬৪ সারি**, আর এই বোর্ড প্রতিবার
       **পুরো টেবিলটাই** নামাত। RMP-তে একটা কল লেখা হলেই পরের বার খোলায়
       আবার ৫ MB। দিনে কয়েকবার হলেই ৫০ MB+ — এটাই ছিল সবচেয়ে বড় বাকি ফুটো।

       এখন তিন ধাপ (চেম্বার বোর্ডে এই নিয়মটা আগে থেকেই চলছে — প্রমাণিত):
         ১. একটা **ছোট প্রশ্ন** — কতগুলো সারি আছে ও সবচেয়ে নতুন `updatedAt`
            কোনটা (এক সারি, কয়েকশো বাইট)।
         ২. সংখ্যা একই **আর** সময়ও একই ⇒ **একটাও সারি নামে না**, ফোনের
            জমা তালিকাটাই ফেরত যায়।
         ৩. শুধু সময় এগিয়েছে ⇒ **কেবল তার পরে বদলানো সারিগুলো** নামে, আর
            id ধরে জমা তালিকায় বসে।

       🔒 সন্দেহ হলেই **পুরোটা নামে** — জমা তালিকা নেই · প্রশ্নের উত্তর
          পাওয়া যায়নি · **সারির সংখ্যা বদলেছে** (কেউ যোগ/মোছা করেছে) ·
          সময় ফাঁকা · delta আনতে ব্যর্থ — সব ক্ষেত্রেই আগের হুবহু পুরো পড়া।
          তাই এই কোড কখনো "কম ডাক্তার" দেখাতে পারে না।
       ⛔ ফেরত আসা সারিতে **সব ঘরই** থাকে (`select=*` আগের মতোই) — তাই
          ডিলিটের সময় Trash-এ পুরো রেকর্ড যাওয়ার নিয়মও অটুট।
       ⛔ সাজানোর ক্রমও আগের মতোই (`updatedAt` অনুযায়ী নতুন আগে)।
       ═══════════════════════════════════════════════════════════════════ */
    fun fetchListRawSmartOrNull(branchFilter: String?, cached: JSONArray?): JSONArray? {
        val filter = listFilter(branchFilter)
        val full = { SupabaseClient.fetchListOrNull("doctor_visits", filter, 5000) }
        if (cached == null || cached.length() == 0) return full()

        val fp = SupabaseClient.fetchListFingerprintOrNull("doctor_visits", filter) ?: return full()
        val serverCount = fp.first
        val serverStamp = fp.second
        if (serverCount != cached.length()) return full()      // যোগ/মোছা হয়েছে
        if (serverStamp.isBlank()) return full()

        var localStamp = ""
        for (i in 0 until cached.length()) {
            val u = cached.optJSONObject(i)?.optString("updatedAt").orEmpty()
            if (u > localStamp) localStamp = u
        }
        if (localStamp.isBlank()) return full()
        if (serverStamp == localStamp) return cached           // ✅ কিছুই বদলায়নি

        val enc = java.net.URLEncoder.encode(localStamp, "UTF-8")
        val delta = SupabaseClient.fetchListOrNull(
            "doctor_visits", "$filter&updatedAt=gt.$enc", 5000) ?: return full()

        val byId = LinkedHashMap<String, org.json.JSONObject>()
        for (i in 0 until cached.length()) {
            val o = cached.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        for (i in 0 until delta.length()) {
            val o = delta.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        // সার্ভারের ক্রম হুবহু রাখা — নতুন `updatedAt` আগে
        val sorted = byId.values.sortedByDescending { it.optString("updatedAt").orEmpty() }
        val out = JSONArray()
        for (v in sorted) out.put(v)
        return out
    }

    /** 🔴🔒 V940 — `branch` যোগ হলো: ওই নম্বরটা **কোন ব্রাঞ্চে** আগে থেকে
     *  আছে সেটা জানাতে। ⛔ ডিফল্ট ফাঁকা, তাই পুরনো কোনো ব্যবহার ভাঙে না। */
    data class DuplicateDoctor(val found: Boolean, val name: String, val branch: String = "")

    // 🔒 (03.08.2026, TK-অনুমোদনে, খাতার সারি B190-এর গভীর পুনর্বিবেচনা) —
    // আগে এখানে `SupabaseClient.findByMobile()` ব্যবহার হত, যেটা নেট-ব্যর্থতায়ও
    // (ঠিক যেমন সত্যিই-না-পাওয়া গেলে) **খালি তালিকা** ফেরত দেয় — দুটো
    // পরিস্থিতি আলাদা করার উপায়ই ছিল না। TK-এর ধীর লাইনে এটা "নতুন ডাক্তার"
    // বলে ভুল সিদ্ধান্ত দিতে পারত, ঠিক Registration-এর সেই একই পুরনো ভুলের
    // (D27, "ONE MOBILE = ONE REGISTRATION") মতোই।
    // সমাধান: RegistrationRepository.checkDuplicatePatient()-এর হুবহু একই
    // প্রমাণিত প্যাটার্ন — `findByMobileOrNull()` (নেট-ব্যর্থতায় `null`,
    // সত্যিই-না-পাওয়া গেলে খালি তালিকা — দুটো এখন আলাদা করা যায়)। ক্লাউড
    // ব্যর্থ হলে (`null`) তবেই এই স্ক্রিনের **আগে থেকে লোড হওয়া** স্থানীয়
    // তালিকায় (caller থেকে পাঠানো, নতুন কোনো cloud-কল না) খোঁজা হয় — ঠিক
    // Registration-এ যেমন `LocalWorkflowStore` ব্যবহার হয়। ⛔ ক্লাউড থেকে
    // সত্যিই উত্তর এলে (খালি হোক বা না) স্থানীয় তালিকা মোটেও ছোঁয়া হয় না —
    // তাই স্বাভাবিক নেটে আচরণ অপরিবর্তিত। ⛔ `localFallback` ডিফল্ট `null`
    // (এই একটাই call-site, তাই পুরনো কোনো ব্যবহার ভাঙে না)।
    fun checkDuplicate(mobileDigitsOnly: String, localFallback: List<DoctorVisitItem>? = null,
                       forBranch: String = ""): DuplicateDoctor {
        val normalized = "+91$mobileDigitsOnly"
        // ১) ক্লাউডে প্রাইমারি mobile-এ মিলছে কিনা (আগের মতোই)।
        val cloud = SupabaseClient.findByMobileOrNull("doctor_visits", normalized, "name,branch", 20)
        if (cloud != null && cloud.length() > 0) {
            /* 🔴🔴🔒 V940 (০১.০৯.২০২৬, TK-নির্দেশ, অনুমতি নিয়ে) — *"একই নম্বর
               প্রতিটা ব্রাঞ্চে আলাদা RMP হিসেবে বসুক"*। তাই এখন **যে ব্রাঞ্চের
               জন্য সেভ করা হচ্ছে** সেই ব্রাঞ্চে আগে আছে কিনা সেটাই আসল প্রশ্ন।
               একই ব্রাঞ্চে থাকলে আগের মতোই আটকাবে; অন্য ব্রাঞ্চে থাকলে
               ডাকার জায়গা স্টাফকে জিজ্ঞাসা করে সেভ করতে দেবে।
               ⛔ `forBranch` ফাঁকা পাঠালে আচরণ **হুবহু আগের মতোই** (যে-কোনো
                  ব্রাঞ্চের মিলই ডুপ্লিকেট) — তাই পুরনো কোনো কল ভাঙে না। */
            var other: DuplicateDoctor? = null
            for (i in 0 until cloud.length()) {
                val r = cloud.optJSONObject(i) ?: continue
                val b = r.s("branch")
                if (forBranch.isBlank() || b.equals(forBranch, ignoreCase = true))
                    return DuplicateDoctor(true, r.s("name"), b)
                if (other == null) other = DuplicateDoctor(true, r.s("name"), b)
            }
            if (forBranch.isBlank()) return DuplicateDoctor(true, cloud.getJSONObject(0).s("name"))
            if (other != null) return other
            return DuplicateDoctor(false, "")
        }
        // ২) 🟢 B630 (11.08.2026): এই ফোনের জানা তালিকায় প্রাইমারি **বা বাড়তি নম্বরে**
        //    মিলছে কিনা — ক্লাউড-query শুধু প্রাইমারি mobile দেখে, তাই বাড়তি নম্বরের
        //    ডুপ্লিকেট এখানেই ধরা হয়। (ক্লাউড ব্যর্থ হলেও এই তালিকা কাজ করে — পুরনো fail-safe অটুট।)
        if (localFallback != null) {
            // 🔴🔒 V940 — এখানেও একই ব্রাঞ্চের নিয়ম (ক্লাউড ব্যর্থ হলে এই পথটাই চলে)।
            val hits = localFallback.filter { doctorItemHasNumber(it, mobileDigitsOnly) }
            val same = hits.firstOrNull { forBranch.isBlank() || it.branch.equals(forBranch, ignoreCase = true) }
            if (same != null) return DuplicateDoctor(true, same.name, same.branch)
            val other = hits.firstOrNull()
            if (other != null) return DuplicateDoctor(true, other.name, other.branch)
        }
        return DuplicateDoctor(false, "")
    }

    /** 🟢 B630: এই ডাক্তার-item-এ (প্রাইমারি `mobile` বা `altMobiles`-এর যেকোনোটায়)
     *  এই ১০-ডিজিট নম্বরটা আছে কিনা। */
    fun doctorItemHasNumber(item: DoctorVisitItem, tenDigits: String): Boolean {
        if (tenDigits.length != 10) return false
        if (item.mobile.filter { it.isDigit() }.takeLast(10) == tenDigits) return true
        if (item.altMobiles.isBlank()) return false
        return item.altMobiles.split(",").any { it.filter { c -> c.isDigit() }.takeLast(10) == tenDigits }
    }

    // 🔒 TK'S PERMANENT RULE (28.07.2026): whatever this phone saved must show
    // on this phone straight away. The optional context below changes nothing
    // anyone sees at the moment of saving -- it only notes the new doctor down
    // (MyPhoneWrites) so the Doctor/RMP list shows it even while the cloud copy
    // is still on its way. The save itself is exactly as it always was.
    fun addNewDoctor(name: String, mobileDigitsOnly: String, branch: String, area: String, remarks: String, nextCallDate: String, staffMobile: String, context: android.content.Context? = null, altMobiles: String = "", policeStation: String = ""): Boolean {
        // 🚓 V1034 — থানা (ডিফল্ট ফাঁকা, তাই পুরনো ডাক আগের মতোই চলে)।
        val row = DoctorVisitModel.buildNewDoctorRow(name, mobileDigitsOnly, branch, area, remarks, nextCallDate, staffMobile, altMobiles, policeStation)
        val ok = SupabaseClient.upsert("doctor_visits", row)
        try { MyPhoneWrites.remember(context, "doctor_visits", row.optString("id"), row) } catch (_: Throwable) { }
        return ok
    }

    companion object {
        /** 🔴🆕 V434 — শেষবার `logCall()` ব্যর্থ হলে লেখাটা **সারিতে বসানো
         *  হয়েছে কিনা** (নেট এলে নিজে থেকে যাবে) — শুধু স্টাফকে **সঠিক কথা**
         *  বলার জন্য। `true` = লেখার চেষ্টা হয়েছে ⇒ `CloudWriteQueue` ধরে
         *  রেখেছে, আবার লিখলে **দুইবার** হয়ে যাবে। `false` = লেখার আগেই
         *  থেমে গেছে (পুরনো হিস্ট্রি পড়া যায়নি) ⇒ আবার Save চাপতেই হবে।
         *  ⛔ শুধু বার্তা ঠিক করার জন্য — কোনো সেভ-লজিক এর উপর নির্ভর করে না। */
        @Volatile var lastCallWriteQueued: Boolean = false
    }

    /** Logs a call: fetches the current callHistory first so the update
     * appends to it instead of replacing it, matching saveDoctorCall(). */
    fun logCall(id: String, note: String, nextCallDate: String, staffMobile: String, context: android.content.Context? = null, expectedPatientDate: String = ""): Boolean {
        lastCallWriteQueued = false
        // 🔴🔴🔴🆕🔒 V434 (১৮.০৮.২০২৬, TK-এর "Save হচ্ছে না" রিপোর্ট খুঁজতে গিয়ে
        // **নিজের অডিটে ধরা পড়া পুরনো মারাত্মক দোষ** — TK আলাদা করে বলেননি):
        // এই লাইনটা আগে `fetchList()` ব্যবহার করত, আর `fetchList()` **নেট
        // ব্যর্থ হলেও ফাঁকা তালিকা** ফেরত দেয় (ভুল নাকি সত্যিই খালি — বোঝার
        // উপায় থাকত না)। ফলে দুর্বল নেটে পড়াটা ব্যর্থ হলে `existingHistory`
        // ফাঁকা ধরা হত, আর তার পরের লেখায় `callHistory` ঘরে **শুধু আজকের
        // একটামাত্র এন্ট্রি** বসে যেত ⇒ ঐ ডাক্তারের **আগের সব কল-হিস্ট্রি
        // চিরতরে মুছে যেত**। (লেখাটা সফল হত, তাই কেউ টেরও পেত না।)
        // **সমাধান:** `fetchListOrNull()` — এটা ব্যর্থ হলে `null` দেয়, সত্যিই
        // খালি হলে ফাঁকা তালিকা। পড়া ব্যর্থ হলে এখন **কিছুই লেখা হয় না**,
        // `false` ফেরে ⇒ স্টাফ আবার চাপলে তখন ঠিকঠাক বসবে।
        // ⛔ সফল পড়ায় আচরণ **হুবহু আগের মতোই** — এক অক্ষরও বদলায়নি।
        val existing = SupabaseClient.fetchListOrNull("doctor_visits", "id=eq.$id", 1)
            ?: return false
        val existingHistory = if (existing.length() > 0)
            existing.getJSONObject(0).optJSONArray("callHistory") ?: JSONArray() else JSONArray()
        val fields = DoctorVisitModel.buildCallUpdateFields(existingHistory, note, nextCallDate, staffMobile, expectedPatientDate)
        // এখান থেকে লেখার চেষ্টা শুরু — ব্যর্থ হলেও `SupabaseClient.updateById`
        // নিজেই `CloudWriteQueue`-তে বসিয়ে রাখে, তাই নেট এলে নিজে থেকেই যাবে।
        lastCallWriteQueued = true
        /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
           না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
           প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
        fields.put("updatedAt", java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).format(java.util.Date()))
        val ok = SupabaseClient.updateById("doctor_visits", id, fields)
        // Same permanent rule as addNewDoctor above: the remark just written on
        // this phone is noted down so the list shows it at once instead of the
        // older one -- TK: "রিমার্ক লিখছি হয়ে গেছে দেখায়, পরে পুরনোটাই থাকে."
        try { MyPhoneWrites.remember(context, "doctor_visits", id, fields) } catch (_: Throwable) { }
        return ok
    }

    /**
     * 🟢🔒 V836 (২৯.০৮.২০২৬, TK-নির্দেশ, ডেমো-ফটো পাশ) — কল-শেষের
     * নোটিফিকেশনের "📝 Add Remark" থেকে RMP-র কল-নোট লেখা।
     *
     * TK: *"কল কাটার পরে এখানে যেন Remarks লেখার অপশন আসে এবং Remarks
     * লিখলে যেন অটোমেটিক Update হয়ে যায় RMP section এ।"*
     * আর Next Call তারিখ — *"তবে বাধ্যতামূলক নয়"*।
     *
     * 🔴 কেন উপরের `logCall()` **সরাসরি ব্যবহার করা গেল না** (যাচাই করে ধরা,
     *    TK-কে আগে জানানো হয়েছে): `buildCallUpdateFields()` সবসময়
     *    `nextCallDate` **আর** `expectedPatientDate` দুটোই লিখে দেয়।
     *    নোটিফিকেশন থেকে ওই দুটো জানা থাকে না ⇒ ফাঁকা পাঠালে ওই ডাক্তারের
     *    **আগের Next Call ও Expected Patient তারিখ মুছে যেত**।
     *    তাই এখানে সারিটা পড়ে **আগের মানদুটো নিজেই ফেরত বসিয়ে** দেওয়া হয়।
     *
     * ⛔ নতুন কোনো লেখার নিয়ম বানানো হয়নি — একই প্রমাণিত
     *    `buildCallUpdateFields()` ব্যবহার হয়, তাই callHistory-র ধাঁচ,
     *    `callStatus`, `remarks`, `lastCallDate` হুবহু আগের মতোই বসে।
     * ⛔ V434-এর নিয়ম মানা: পড়া ব্যর্থ হলে (`null`) **কিছুই লেখা হয় না**,
     *    `false` ফেরে — পুরনো callHistory মুছে যাওয়ার সুযোগ নেই।
     * ⛔ `nextCallDate` ফাঁকা এলে আগেরটাই থাকে; স্টাফ তারিখ বাছলে তবেই বদলায়।
     */
    fun logCallKeepingDates(
        id: String, note: String, staffMobile: String,
        nextCallDate: String = "", context: android.content.Context? = null
    ): Boolean {
        if (id.isBlank() || note.isBlank()) return false
        val existing = SupabaseClient.fetchListOrNull("doctor_visits", "id=eq.$id", 1) ?: return false
        if (existing.length() == 0) return false
        val row = existing.getJSONObject(0)
        val existingHistory = row.optJSONArray("callHistory") ?: JSONArray()
        val oldNext = if (row.isNull("nextCallDate")) "" else row.optString("nextCallDate", "")
        val oldExpected = if (row.isNull("expectedPatientDate")) "" else row.optString("expectedPatientDate", "")
        val useNext = nextCallDate.trim().ifBlank { oldNext }
        val fields = DoctorVisitModel.buildCallUpdateFields(
            existingHistory, note, useNext, staffMobile, oldExpected
        )
        /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
           না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
           প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
        fields.put("updatedAt", java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).format(java.util.Date()))
        val ok = SupabaseClient.updateById("doctor_visits", id, fields)
        try { MyPhoneWrites.remember(context, "doctor_visits", id, fields) } catch (_: Throwable) { }
        return ok
    }

    // 🔒 TK-ORDER (30.07.2026 রাত, খাতার সারি B205 — TK: "স্টাফ তো ভুল করে
    // কিছু লিখতেই পারে, তার ভুল সংশোধনের রাস্তা তো করতে হবে")। এটা
    // logCall()-এর থেকে ইচ্ছে করেই আলাদা -- logCall() সবসময় নতুন এন্ট্রি
    // যোগ করে (B123-এর লক করা নিয়ম, এখানে ছোঁয়া হয়নি)। এই ফাংশন শুধু
    // সবচেয়ে উপরের (সবচেয়ে নতুন) callHistory এন্ট্রির `note`-টাই নিজের
    // জায়গায় বদলায় -- callCount/lastCallDate/nextCallDate/callStatus/
    // expectedPatientDate কিছুই ছোঁয়া হয় না (এই ঘরগুলো ফিল্ডেই পাঠানো হয় না)।
    fun editLastCallNote(id: String, existingHistory: JSONArray, fixedNote: String, context: android.content.Context? = null): Boolean {
        val newHistory = JSONArray()
        for (i in 0 until existingHistory.length()) {
            val entry = existingHistory.getJSONObject(i)
            if (i == 0) {
                val fixed = org.json.JSONObject(entry.toString())
                fixed.put("note", fixedNote)
                newHistory.put(fixed)
            } else {
                newHistory.put(entry)
            }
        }
        val fields = org.json.JSONObject()
            .put("callHistory", newHistory)
            .put("remarks", fixedNote)
        /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
           না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
           প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
        fields.put("updatedAt", java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).format(java.util.Date()))
        val ok = SupabaseClient.updateById("doctor_visits", id, fields)
        try { MyPhoneWrites.remember(context, "doctor_visits", id, fields) } catch (_: Throwable) { }
        return ok
    }

    // 🔒 খাতার সারি B193 (TK, 30.07.2026 রাত): Dashboard ঘন্টার জন্য — আজ
    // যাদের "প্রত্যাশিত পেশেন্ট" আসার তারিখ, তাদের সংখ্যা। ⛔ সস্তা
    // count-only অনুরোধ (PaymentRepository.fetchPendingBackdateCount()-এর
    // হুবহু একই প্যাটার্ন — শুধু id গোনা, পুরো সারি নামানো হয় না)।
    // branchFilter ফাঁকা/null হলে সব ব্রাঞ্চ (Master), নইলে শুধু সেই ব্রাঞ্চ
    // (স্টাফ/ফিল্ড অফিসার — বাকি সব জায়গার একই ব্রাঞ্চ-আলাদা-করার নিয়ম)।
    fun fetchExpectedTodayCount(branchFilter: String?): Int {
        val today = DoctorVisitModel.today()
        val branchPart = if (!branchFilter.isNullOrBlank())
            "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        return SupabaseClient.fetchCount("doctor_visits", "expectedPatientDate=eq.$today$branchPart")
            .coerceAtLeast(0)
    }

    // 🔒 TK-ORDER (30.07.2026 রাত, খাতার সারি B206 — TK: "তারিখ নির্ধারণ করলে
    // সেই উক্ত তারিখে সেই ডাক্তারকে কল করতে হবে, স্টাফের ঘন্টায় নোটিফিকেশন
    // আসবে সাউন্ড সহ")। Dashboard ঘন্টার জন্য — আজ যাদের "Next Call Date"
    // ঠিক আজকের, তাদের সংখ্যা। ⛔ হুবহু fetchExpectedTodayCount()-এর একই
    // প্যাটার্ন — সস্তা count-only অনুরোধ, পুরো সারি নামানো হয় না।
    // branchFilter ফাঁকা/null হলে সব ব্রাঞ্চ (Master), নইলে শুধু সেই ব্রাঞ্চ
    // (স্টাফ/ফিল্ড অফিসার — বাকি সব জায়গার একই ব্রাঞ্চ-আলাদা-করার নিয়ম)।
    fun fetchNextCallDueTodayCount(branchFilter: String?): Int {
        val today = DoctorVisitModel.today()
        val branchPart = if (!branchFilter.isNullOrBlank())
            "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        return SupabaseClient.fetchCount("doctor_visits", "nextCallDate=eq.$today$branchPart")
            .coerceAtLeast(0)
    }

    // 🆕 (06.08.2026, নতুন Notifications পাতার জন্য — TK-অনুমোদনে) — উপরের
    // fetchExpectedTodayCount()/fetchNextCallDueTodayCount() শুধু সংখ্যা
    // দেয় (সস্তা count-only অনুরোধ, ঘন্টার জন্য বানানো)। এই দুটো নতুন
    // ফাংশন **নাম-সহ পুরো তালিকা** দেয় — শুধু তখনই ডাকা হয় যখন স্টাফ নিজে
    // Notifications পাতা খোলেন (ঘন্টার প্রতিটা গণনায় না), তাই কোটার উপর
    // বাড়তি চাপ পড়ে না। ⛔ বাকি সব নিয়ম fetchExpectedTodayCount()-এর
    // মতোই — branchFilter ফাঁকা/null হলে সব ব্রাঞ্চ (Master)।
    fun fetchExpectedTodayList(branchFilter: String?): List<DoctorVisitItem> {
        val today = DoctorVisitModel.today()
        val branchPart = if (!branchFilter.isNullOrBlank())
            "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        val rows = SupabaseClient.fetchList("doctor_visits", "expectedPatientDate=eq.$today$branchPart", 500, order = "name.asc")
        val items = mutableListOf<DoctorVisitItem>()
        for (i in 0 until rows.length()) items.add(DoctorVisitModel.parse(rows.getJSONObject(i)))
        return items
    }

    fun fetchNextCallDueTodayList(branchFilter: String?): List<DoctorVisitItem> {
        val today = DoctorVisitModel.today()
        val branchPart = if (!branchFilter.isNullOrBlank())
            "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        val rows = SupabaseClient.fetchList("doctor_visits", "nextCallDate=eq.$today$branchPart", 500, order = "name.asc")
        val items = mutableListOf<DoctorVisitItem>()
        for (i in 0 until rows.length()) items.add(DoctorVisitModel.parse(rows.getJSONObject(i)))
        return items
    }

    // TK-REQUESTED ADDITION (2026-07-19): "Add Referral Income" moved out of
    // DoctorVisitActivity (where it was private) so it can also be opened
    // directly from a patient's own Follow-up card (Action menu) -- no need
    // to go find the referring doctor in Dr. Visit separately anymore. Same
    // exact save logic as before, just reusable now.

    /** Finds the doctor_visits row for a referring doctor, matched by name
     *  (case-insensitive) or mobile -- whichever the patient's record has.
     *  Used so the Follow-up card's "Add Referral Income" doesn't need the
     *  staff to search for the doctor by hand; the patient already knows
     *  who referred them. */
    /* 🔒 V1520 — branch is a financial boundary. If patientBranch is supplied,
       this lookup returns ONLY an RMP from that same branch. It never falls back
       to a same-name/mobile RMP from another branch. If branch is blank, legacy
       callers keep the old first-match behaviour. */
    fun findReferringDoctor(refName: String, refMobile: String, patientBranch: String = ""): org.json.JSONObject? {
        fun pickBestBranchMatch(rows: org.json.JSONArray): org.json.JSONObject? {
            if (rows.length() == 0) return null
            if (patientBranch.isNotBlank()) {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    if (row.optString("branch").equals(patientBranch, ignoreCase = true)) return row
                }
                return null
            }
            return rows.getJSONObject(0)
        }
        val mobile = refMobile.filter { it.isDigit() }.takeLast(10)
        if (mobile.length == 10) {
            val byMobile = SupabaseClient.findByMobile("doctor_visits", "+91$mobile", "id,name,mobile,branch", limit = 5)
            val picked = pickBestBranchMatch(byMobile)
            if (picked != null) return picked
        }
        val name = refName.trim()
        if (name.isBlank()) return null
        val rows = SupabaseClient.fetchList(
            "doctor_visits", "name=ilike.${java.net.URLEncoder.encode(name, "UTF-8")}", 5
        )
        return pickBestBranchMatch(rows)
    }

    /** Records a referral commission entry for a doctor -- patient, amount,
     *  Paid/Unpaid -- into referralPayments and recomputes referralPaid/
     *  referralDue. Identical logic to what DoctorVisitActivity always used;
     *  just given a proper home here so more than one screen can call it. */
    // TK-REQUESTED (2026-07-27): if this write failed at that moment the
    // referral income was lost for good -- this class had no `context`, so
    // it could never reach the retry queue every other save already uses.
    // The optional context below changes nothing the user sees; it only
    // parks a failed write so it is retried the next time any screen opens.
    // TK-APPROVED ADDITION (31.07.2026): two new OPTIONAL fields so RMP
    // Message 4 can show real Payment Mode / Reference No. instead of a
    // blank line. Defaults keep every existing caller working unchanged.
    fun addReferralEntry(docId: String, patient: String, patientMobile: String, amount: Double, status: String, context: android.content.Context? = null, mode: String = "", referenceNo: String = ""): Boolean {
        return try {
            val rows = SupabaseClient.fetchList("doctor_visits", "id=eq.$docId", 1)
            if (rows.length() == 0) return false
            val doc = rows.getJSONObject(0)
            val existing = doc.optJSONArray("referralPayments") ?: JSONArray()
            // 🔒 V1520 — final app-side duplicate backstop. One exact referral
            // (same patient mobile + same amount) may be recorded only once per day.
            val today = DoctorVisitModel.today()
            val mobKey = patientMobile.filter { it.isDigit() }.takeLast(10)
            for (i in 0 until existing.length()) {
                val e = existing.optJSONObject(i) ?: continue
                val oldMob = e.optString("patientMobile").filter { it.isDigit() }.takeLast(10)
                if (oldMob == mobKey && e.optString("date") == today &&
                    kotlin.math.abs(e.optDouble("amount", 0.0) - amount) <= 0.5) return false
            }
            val entry = org.json.JSONObject()
                .put("id", "ref_" + System.currentTimeMillis())
                .put("patient", patient)
                .put("patientMobile", patientMobile)
                .put("amount", amount)
                .put("status", status)
                .put("mode", mode)
                .put("referenceNo", referenceNo)
                .put("date", DoctorVisitModel.today())
                .put("createdAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))
            val newArr = JSONArray().put(entry)
            for (i in 0 until existing.length()) newArr.put(existing.get(i))
            var paid = 0.0; var due = 0.0
            for (i in 0 until newArr.length()) {
                val e = newArr.getJSONObject(i)
                val a = e.optDouble("amount", 0.0)
                if (e.optString("status").equals("Paid", true)) paid += a else due += a
            }
            val fields = org.json.JSONObject()
                .put("referralPayments", newArr)
                .put("referralPaid", paid)
                .put("referralDue", due)
            /* 🔴🔒 V817 (২৯.০৮.২০২৬, V816-এর পরে TK-নির্দেশে পুরো প্রজেক্ট যাচাই) —
               এই সারিটা **পুরো `referralPayments` তালিকাটাই** নতুন করে লেখে।
               কোনো ফোনে ওই একই ঘরগুলোর **পুরনো একটা snapshot** অপেক্ষমাণ থাকলে
               সেটা পরে চললে এই নতুন লেখাটা **মুছে গিয়ে পুরনোটা ফিরে আসত**।
               V378-এ ঠিক এই সুরক্ষাটা delete ও edit-এ বসানো ছিল, কিন্তু এখানে
               বাদ পড়েছিল — এখন তিন জায়গাতেই এক নিয়ম।
               ⛔ উপরের তালিকাটা এইমাত্র ক্লাউড থেকে পড়া, তাই আমাদেরটাই নবীনতম;
                  পুরনো snapshot বাদ দেওয়া সম্পূর্ণ নিরাপদ।
               ⛔ শুধু এই তিনটে ঘর — অন্য কোনো ঘরের অপেক্ষমাণ লেখা ছোঁয়া হয় না। */
            val refKeysV817 = setOf("referralPayments", "referralPaid", "referralDue")
            if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, refKeysV817) } catch (_: Throwable) {}
            try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, refKeysV817) } catch (_: Throwable) {}
            /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
               না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
               প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
            fields.put("updatedAt", java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).format(java.util.Date()))
            val ok = SupabaseClient.updateById("doctor_visits", docId, fields)
            if (!ok && context != null) {
                try { GenericUpdateQueue.queue(context, "doctor_visits", docId, fields) } catch (_: Throwable) { }
            }
            ok
        } catch (e: Exception) {
            false
        }
    }

    /* 🔒🔒 V1381 (১২.০৯.২০২৬, TK-রিপোর্ট — ANAND KUMAR-এর ₹3,250 Due দুইবার
       সেভ হয়ে গিয়েছিল, কোনো ওয়ার্নিং ছাড়াই) — PaymentRepository.
       todaysRefundLike()-এর হুবহু একই ধাঁচ, শুধু ডেটা payments টেবিল নয়,
       এই ডাক্তারের doctor_visits.referralPayments তালিকা থেকে আসে।
       ⛔ নেট খারাপ হলে চুপচাপ null — সৎ Referral Income কখনো আটকায় না। */
    fun todaysReferralLike(docId: String, patientMobile: String, amount: Double): org.json.JSONObject? {
        if (docId.isBlank() || amount <= 0.0) return null
        return try {
            val rows = SupabaseClient.fetchList("doctor_visits", "id=eq.$docId", 1, select = "referralPayments")
            if (rows.length() == 0) return null
            val list = rows.getJSONObject(0).optJSONArray("referralPayments") ?: return null
            val today = DoctorVisitModel.today()
            var hit: org.json.JSONObject? = null
            for (i in 0 until list.length()) {
                val e = list.optJSONObject(i) ?: continue
                if (e.optString("patientMobile").filter { it.isDigit() }.takeLast(10) != patientMobile.filter { it.isDigit() }.takeLast(10)) continue
                if (e.optString("date") != today) continue
                if (kotlin.math.abs(e.optDouble("amount", 0.0) - amount) <= 0.5) { hit = e; break }
            }
            hit
        } catch (_: Throwable) { null }
    }

    // ===================================================================
    // 🟢 B628 (11.08.2026, TK-নির্দেশ): Referral Income এন্ট্রি এডিট/ডিলিট —
    //   তিনবার-চাপ। মাস্টার ও একই-দিনের স্টাফ/ডাক্তার সরাসরি বদলায়; দিন
    //   পেরিয়ে গেলে স্টাফ/ডাক্তার শুধু অনুরোধ পাঠায় → মাস্টার Approve করলে
    //   তবেই বদলায় (payment_edit_requests-এর হুবহু একই প্রমাণিত প্যাটার্ন)।
    //   ⛔ addReferralEntry-এর হুবহু একই paid/due-হিসাব ও write-back পথ।
    // ===================================================================
    private fun refIsoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())

    /** referralPayments থেকে paid/due আবার হিসাব — addReferralEntry-এর হুবহু নিয়ম। */
    private fun recomputeReferralTotals(arr: JSONArray): Pair<Double, Double> {
        var paid = 0.0; var due = 0.0
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val a = e.optDouble("amount", 0.0)
            if (e.optString("status").equals("Paid", true)) paid += a else due += a
        }
        return Pair(paid, due)
    }

    /** Gives one legacy referral entry its own id before editing. Old entries
     *  created before ids were introduced are matched by ALL saved facts.
     *  Exactly one match is required; zero or multiple matches stop safely. */
    fun ensureLegacyReferralEntryId(
        docId: String, date: String, amount: Double, status: String, patient: String,
        context: android.content.Context? = null
    ): String {
        if (docId.isBlank()) return ""
        return try {
            val rows = SupabaseClient.fetchList("doctor_visits", "id=eq.$docId", 1)
            if (rows.length() == 0) return ""
            val doc = rows.getJSONObject(0)
            val arr = doc.optJSONArray("referralPayments") ?: return ""
            val matches = mutableListOf<Int>()
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                if (e.optString("id").isNotBlank()) continue
                val sameDate = e.optString("date") == date
                val sameAmount = kotlin.math.abs(e.optDouble("amount", 0.0) - amount) < 0.01
                val sameStatus = e.optString("status", "Unpaid").equals(status, true)
                val samePatient = e.optString("patient").trim().equals(patient.trim(), true)
                if (sameDate && sameAmount && sameStatus && samePatient) matches.add(i)
            }
            if (matches.size != 1) return ""
            val newId = "ref_legacy_" + java.util.UUID.randomUUID().toString().replace("-", "")
            arr.getJSONObject(matches.single()).put("id", newId)
            /* 🔴🔒 V817 (২৯.০৮.২০২৬, V816-এর পরে TK-নির্দেশে পুরো প্রজেক্ট যাচাই) —
               এই সারিটা **পুরো `referralPayments` তালিকাটাই** নতুন করে লেখে।
               কোনো ফোনে ওই একই ঘরগুলোর **পুরনো একটা snapshot** অপেক্ষমাণ থাকলে
               সেটা পরে চললে এই নতুন লেখাটা **মুছে গিয়ে পুরনোটা ফিরে আসত**।
               V378-এ ঠিক এই সুরক্ষাটা delete ও edit-এ বসানো ছিল, কিন্তু এখানে
               বাদ পড়েছিল — এখন তিন জায়গাতেই এক নিয়ম।
               ⛔ উপরের তালিকাটা এইমাত্র ক্লাউড থেকে পড়া, তাই আমাদেরটাই নবীনতম;
                  পুরনো snapshot বাদ দেওয়া সম্পূর্ণ নিরাপদ।
               ⛔ শুধু এই তিনটে ঘর — অন্য কোনো ঘরের অপেক্ষমাণ লেখা ছোঁয়া হয় না। */
            val refKeysV817 = setOf("referralPayments", "referralPaid", "referralDue")
            if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, refKeysV817) } catch (_: Throwable) {}
            try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, refKeysV817) } catch (_: Throwable) {}
            val fields = org.json.JSONObject().put("referralPayments", arr)
            /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
               না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
               প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
            fields.put("updatedAt", java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).format(java.util.Date()))
            val ok = SupabaseClient.updateById("doctor_visits", docId, fields)
            if (!ok && context != null) try { GenericUpdateQueue.queue(context, "doctor_visits", docId, fields) } catch (_: Throwable) {}
            if (ok) newId else ""
        } catch (_: Exception) { "" }
    }

    /** সরাসরি এডিট (মাস্টার / একই-দিন)। entryId দিয়ে ঠিক সারিটা খুঁজে amount+status বদলায়। */
    fun editReferralEntry(docId: String, entryId: String, newAmount: Double, newStatus: String, editorMobile: String, context: android.content.Context? = null, fallbackDate: String = "", fallbackAmount: Double = 0.0, fallbackStatus: String = "", fallbackPatient: String = ""): Boolean {
        return try {
            if (docId.isBlank() || newAmount <= 0) return false
            val rows = SupabaseClient.fetchList("doctor_visits", "id=eq.$docId", 1)
            if (rows.length() == 0) return false
            val doc = rows.getJSONObject(0)
            val arr = doc.optJSONArray("referralPayments") ?: JSONArray()
            var hitIndex = -1
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                if (entryId.isNotBlank() && e.optString("id") == entryId) { hitIndex = i; break }
            }
            if (hitIndex < 0 && entryId.isBlank()) {
                val matches = mutableListOf<Int>()
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    val exact = e.optString("id").isBlank() &&
                        e.optString("date") == fallbackDate &&
                        kotlin.math.abs(e.optDouble("amount", 0.0) - fallbackAmount) < 0.01 &&
                        e.optString("status", "Unpaid").equals(fallbackStatus, true) &&
                        e.optString("patient").trim().equals(fallbackPatient.trim(), true)
                    if (exact) matches.add(i)
                }
                if (matches.size == 1) hitIndex = matches.single()
            }
            if (hitIndex < 0) return false
            val target = arr.getJSONObject(hitIndex)
            if (target.optString("id").isBlank()) target.put("id", "ref_legacy_" + java.util.UUID.randomUUID().toString().replace("-", ""))
            target.put("amount", newAmount).put("status", newStatus)
                .put("editedAt", refIsoNow()).put("editedBy", editorMobile)
            arr.put(hitIndex, target)
            val (paid, due) = recomputeReferralTotals(arr)
            val fields = org.json.JSONObject()
                .put("referralPayments", arr).put("referralPaid", paid).put("referralDue", due)
            val referralKeys = setOf("referralPayments", "referralPaid", "referralDue")
            if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
            try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
            /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
               না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
               প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
            fields.put("updatedAt", java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).format(java.util.Date()))
            val ok = SupabaseClient.updateById("doctor_visits", docId, fields)
            if (!ok && context != null) try { GenericUpdateQueue.queue(context, "doctor_visits", docId, fields) } catch (_: Throwable) {}
            if (ok) {
                if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
                try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
                try { MyPhoneWrites.remember(context, "doctor_visits", docId, fields) } catch (_: Throwable) {}
            }
            ok
        } catch (_: Exception) { false }
    }

    /** সরাসরি ডিলিট (মাস্টার / একই-দিন)। entryId-র সারিটা বাদ দিয়ে paid/due আবার হিসাব। */
    fun deleteReferralEntry(docId: String, entryId: String, editorMobile: String, context: android.content.Context? = null, fallbackDate: String = "", fallbackAmount: Double = 0.0, fallbackStatus: String = "", fallbackPatient: String = ""): Boolean {
        return try {
            if (docId.isBlank()) return false
            val rows = SupabaseClient.fetchList("doctor_visits", "id=eq.$docId", 1)
            if (rows.length() == 0) return false
            val doc = rows.getJSONObject(0)
            val arr = doc.optJSONArray("referralPayments") ?: JSONArray()
            var hitIndex = -1
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                if (entryId.isNotBlank() && e.optString("id") == entryId) { hitIndex = i; break }
            }
            if (hitIndex < 0 && entryId.isBlank()) {
                val matches = mutableListOf<Int>()
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    val exact = e.optString("id").isBlank() &&
                        e.optString("date") == fallbackDate &&
                        kotlin.math.abs(e.optDouble("amount", 0.0) - fallbackAmount) < 0.01 &&
                        e.optString("status", "Unpaid").equals(fallbackStatus, true) &&
                        e.optString("patient").trim().equals(fallbackPatient.trim(), true)
                    if (exact) matches.add(i)
                }
                if (matches.size == 1) hitIndex = matches.single()
            }
            if (hitIndex < 0) return false
            val newArr = JSONArray()
            for (i in 0 until arr.length()) if (i != hitIndex) newArr.put(arr.get(i))
            val (paid, due) = recomputeReferralTotals(newArr)
            val fields = org.json.JSONObject()
                .put("referralPayments", newArr).put("referralPaid", paid).put("referralDue", due)
            val referralKeys = setOf("referralPayments", "referralPaid", "referralDue")
            // V378: an older queued snapshot of these same three fields could
            // otherwise replay later and resurrect the deleted income entry.
            if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
            try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
            /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
               না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
               প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
            fields.put("updatedAt", java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).format(java.util.Date()))
            val ok = SupabaseClient.updateById("doctor_visits", docId, fields)
            if (!ok && context != null) try { GenericUpdateQueue.queue(context, "doctor_visits", docId, fields) } catch (_: Throwable) {}
            if (ok) {
                if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
                try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, referralKeys) } catch (_: Throwable) {}
                try { MyPhoneWrites.remember(context, "doctor_visits", docId, fields) } catch (_: Throwable) {}
            }
            ok
        } catch (_: Exception) { false }
    }

    /** দিন পেরোনো স্টাফ/ডাক্তার: এডিট/ডিলিটের অনুরোধ — আসল row ছোঁয় না, শুধু pending সারি লেখে। */
    fun requestReferralEdit(
        docId: String, docName: String, docMobile: String, branch: String,
        entryId: String, patient: String, patientMobile: String,
        oldAmount: Double, newAmount: Double, oldStatus: String, newStatus: String,
        isDelete: Boolean, reason: String, staffMobile: String, staffName: String
    ): Boolean {
        val row = org.json.JSONObject()
            .put("id", "refeditreq_" + java.util.UUID.randomUUID().toString().replace("-", ""))
            .put("docId", docId).put("entryId", entryId)
            .put("docName", docName).put("docMobile", docMobile).put("branch", branch)
            .put("patient", patient).put("patientMobile", patientMobile)
            .put("oldAmount", oldAmount).put("newAmount", newAmount)
            .put("oldStatus", oldStatus).put("newStatus", newStatus)
            .put("isDelete", isDelete)
            .put("reason", reason)
            .put("requestedBy", staffMobile).put("requestedByName", staffName).put("requestedAt", refIsoNow())
            .put("status", "pending")
            .put("createdAt", refIsoNow()).put("updatedAt", refIsoNow())
        return SupabaseClient.upsert("referral_edit_requests", row)
    }

    /** মাস্টার bell-এর জন্য pending referral-এডিট সংখ্যা। */
    fun fetchPendingReferralEditCount(): Int =
        SupabaseClient.fetchCount("referral_edit_requests", "status=eq.pending").coerceAtLeast(0)

    /** Briefing-এ মাস্টারের দেখা pending referral-এডিট অনুরোধ। */
    fun fetchPendingReferralEditRequests(): List<ReferralEditRequest> {
        val rows = SupabaseClient.fetchList("referral_edit_requests", "status=eq.pending", 200)
        val list = mutableListOf<ReferralEditRequest>()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            list.add(
                ReferralEditRequest(
                    id = r.s("id"), docId = r.s("docId"), entryId = r.s("entryId"),
                    docName = r.s("docName"), docMobile = r.s("docMobile"), branch = r.s("branch"),
                    patient = r.s("patient"), patientMobile = r.s("patientMobile"),
                    oldAmount = r.optDouble("oldAmount", 0.0), newAmount = r.optDouble("newAmount", 0.0),
                    oldStatus = r.s("oldStatus"), newStatus = r.s("newStatus"),
                    isDelete = r.optBoolean("isDelete", false),
                    reason = r.s("reason"), requestedBy = r.s("requestedBy"), requestedByName = r.s("requestedByName"),
                    requestedAt = r.s("requestedAt"), status = r.s("status")
                )
            )
        }
        return list
    }

    /** মাস্টার Approve → আসল এডিট/ডিলিট প্রয়োগ + অনুরোধ approved মার্ক। */
    fun approveReferralEditRequest(req: ReferralEditRequest, masterMobile: String, context: android.content.Context? = null): Boolean {
        return try {
            val ok = if (req.isDelete)
                deleteReferralEntry(req.docId, req.entryId, masterMobile, context)
            else
                editReferralEntry(req.docId, req.entryId, req.newAmount, req.newStatus, masterMobile, context)
            if (ok) {
                SupabaseClient.updateById(
                    "referral_edit_requests", req.id,
                    org.json.JSONObject().put("status", "approved").put("approvedBy", masterMobile)
                        .put("approvedAt", refIsoNow()).put("updatedAt", refIsoNow())
                )
            }
            ok
        } catch (_: Exception) { false }
    }

    /** মাস্টার Reject → আসল টাকা অটুট, শুধু অনুরোধ rejected মার্ক। */
    fun rejectReferralEditRequest(requestId: String, masterMobile: String): Boolean {
        return SupabaseClient.updateById(
            "referral_edit_requests", requestId,
            org.json.JSONObject().put("status", "rejected").put("approvedBy", masterMobile)
                .put("approvedAt", refIsoNow()).put("updatedAt", refIsoNow())
        )
    }

    // TK-REQUESTED ADDITION (2026-07-23): Delete-Approval for Doctor/RMP.
    // Anyone who isn't Master calls requestDelete() -- this only marks the
    // row, nothing is removed. Master then sees the request and calls
    // either approveDelete() (real delete, via the same TrashHelper every
    // other module already uses) or rejectDeleteRequest() (clears the
    // request, row goes back to normal). Master deletes directly with
    // approveDelete() too, without a prior requestDelete() call -- the
    // Activity decides which path to call based on the current user's role.

    /** Marks this doctor's row as delete-requested by a non-Master staff.
     *  Does NOT delete anything. */
    fun requestDelete(id: String, staffMobile: String): Boolean {
        val fields = org.json.JSONObject()
            .put("deleteRequestedBy", staffMobile)
            .put("deleteRequestedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))
        /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
           না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
           প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
        fields.put("updatedAt", java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).format(java.util.Date()))
        return SupabaseClient.updateById("doctor_visits", id, fields)
    }

    /** Master rejects a pending delete request -- row goes back to normal,
     *  nothing is deleted. */
    fun rejectDeleteRequest(id: String): Boolean {
        val fields = org.json.JSONObject()
            .put("deleteRequestedBy", "")
            .put("deleteRequestedAt", "")
        /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
           না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
           প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
        fields.put("updatedAt", java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).format(java.util.Date()))
        return SupabaseClient.updateById("doctor_visits", id, fields)
    }

    /** Master approves (or directly performs, with no prior request) the
     *  delete -- moves the full record into Trash Bin, same as every other
     *  delete in the app, so it stays recoverable from there. */
    fun approveDelete(record: org.json.JSONObject, masterMobile: String, context: android.content.Context? = null): Boolean {
        // A record deleted on purpose must never be brought back by this
        // phone's own note (same guard CloudWriteQueue already applies).
        try { MyPhoneWrites.forget(context, "doctor_visits", record.optString("id")) } catch (_: Throwable) { }
        return TrashHelper.moveToTrash("doctor_visits", record, masterMobile)
    }

    /** 🔒 TK-ORDER (30.07.2026, খাতার সারি B155 — ছবিসহ: *"Referred Patient-এ
     *  ক্লিক করছি কিন্তু কোনো পেশেন্টের অস্তিত্ব খুঁজে পাওয়া যাচ্ছে না, অথচ raj roy
     *  নামে একটা পেশেন্ট পাঠিয়েছিল বলে ₹1,000 দেওয়া হয়েছে"*)।
     *
     *  18.07.2026-এর আগে তৈরি রেফারেল ইনকামের সারিতে **রোগীর মোবাইল নেওয়া হত না**
     *  (শুধু হাতে লেখা নাম), তাই ওই টাকার পিছনে কোনো রোগীর রেকর্ড থাকত না —
     *  "Referred" চিরকাল ০ দেখাত। এই ফাংশন স্টাফকে **পরে হাতে জুড়ে দেওয়ার** পথ
     *  দেয়: মোবাইলটা আগে `patients` টেবিলে সত্যিই আছে কিনা দেখা হয় (Activity-তে),
     *  তারপর ওই সারিতে নাম ও মোবাইল বসে।
     *
     *  ⛔ টাকার অঙ্ক · তারিখ · Paid/Unpaid — কিছুই ছোঁয়া হয় না, শুধু পরিচয় বসে।
     *  ⛔ কোনো আন্দাজ নেই: কোন সারিতে বসবে তা `entryId` দিয়ে ঠিক হয়, আর
     *     পুরনো সারিতে `id` না থাকলে তারিখ+অঙ্ক+নাম তিনটে মিলিয়ে খোঁজা হয়। */
    fun attachPatientToReferralEntry(
        docId: String, entryId: String, fallbackDate: String, fallbackAmount: Double,
        fallbackName: String, patientName: String, patientMobile: String,
        context: android.content.Context? = null
    ): Boolean {
        if (docId.isBlank()) return false
        val mob = patientMobile.filter { it.isDigit() }.takeLast(10)
        if (mob.length != 10) return false
        return try {
            val rows = SupabaseClient.fetchList("doctor_visits", "id=eq.$docId", 1)
            if (rows.length() == 0) return false
            val doc = rows.getJSONObject(0)
            val arr = doc.optJSONArray("referralPayments") ?: return false
            var hitIndex = -1
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val eid = e.optString("id", "")
                if (entryId.isNotBlank() && eid == entryId) { hitIndex = i; break }
            }
            if (hitIndex < 0) {
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    val sameDate = e.optString("date", "") == fallbackDate
                    val sameAmount = Math.abs(e.optDouble("amount", 0.0) - fallbackAmount) < 0.01
                    val sameName = e.optString("patient", "").trim().equals(fallbackName.trim(), true)
                    if (sameDate && sameAmount && sameName) { hitIndex = i; break }
                }
            }
            if (hitIndex < 0) return false
            val entry = arr.getJSONObject(hitIndex)
            entry.put("patientMobile", mob)
            if (patientName.isNotBlank()) entry.put("patient", patientName)
            arr.put(hitIndex, entry)
            /* 🔴🔒 V817 (২৯.০৮.২০২৬, V816-এর পরে TK-নির্দেশে পুরো প্রজেক্ট যাচাই) —
               এই সারিটা **পুরো `referralPayments` তালিকাটাই** নতুন করে লেখে।
               কোনো ফোনে ওই একই ঘরগুলোর **পুরনো একটা snapshot** অপেক্ষমাণ থাকলে
               সেটা পরে চললে এই নতুন লেখাটা **মুছে গিয়ে পুরনোটা ফিরে আসত**।
               V378-এ ঠিক এই সুরক্ষাটা delete ও edit-এ বসানো ছিল, কিন্তু এখানে
               বাদ পড়েছিল — এখন তিন জায়গাতেই এক নিয়ম।
               ⛔ উপরের তালিকাটা এইমাত্র ক্লাউড থেকে পড়া, তাই আমাদেরটাই নবীনতম;
                  পুরনো snapshot বাদ দেওয়া সম্পূর্ণ নিরাপদ।
               ⛔ শুধু এই তিনটে ঘর — অন্য কোনো ঘরের অপেক্ষমাণ লেখা ছোঁয়া হয় না। */
            val refKeysV817 = setOf("referralPayments", "referralPaid", "referralDue")
            if (context != null) try { GenericUpdateQueue.discardFields(context, "doctor_visits", docId, refKeysV817) } catch (_: Throwable) {}
            try { CloudWriteQueue.discardUpdateFields("doctor_visits", docId, refKeysV817) } catch (_: Throwable) {}
            val fields = org.json.JSONObject().put("referralPayments", arr)
            /* 🔴🔒 V992 (TK-রিপোর্ট: এডিট সেভ হয়েও পুরনোটাই দেখাত) — সময়-চিহ্ন
               না বসায় তালিকা "কিছু বদলায়নি" ধরে জমানো পুরনো কপি দেখাত। এখন
               প্রতিটা বদলের সাথে চিহ্নটাও বসে, তাই সব ফোনে নতুনটা নামে। */
            fields.put("updatedAt", java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).format(java.util.Date()))
            val ok = SupabaseClient.updateById("doctor_visits", docId, fields)
            if (!ok && context != null) {
                try { GenericUpdateQueue.queue(context, "doctor_visits", docId, fields) } catch (_: Throwable) { }
            }
            ok
        } catch (e: Exception) {
            false
        }
    }

    /** TK-REPORTED FIX (2026-07-23): "Add Referral Income" recorded the
     *  commission on the DOCTOR's row (referralPayments) but never touched
     *  the PATIENT's own row (refBy/refDoctorMobile) -- so "Referred
     *  Patients" (which scans the patients table for that link) kept
     *  showing 0 even though real, paid referral income existed for that
     *  same patient. This links them, but ONLY if the patient doesn't
     *  already have a different referring doctor set -- never silently
     *  overwrites an existing different referral relationship. */
    fun linkReferringDoctorIfBlank(patientRowId: String, doctorName: String, doctorMobile: String, context: android.content.Context? = null): Boolean {
        if (patientRowId.isBlank()) return false
        return try {
            val rows = SupabaseClient.fetchListSlim("patients", "id=eq.$patientRowId", 1,
                SupabaseClient.PATIENT_NO_PHOTO_COLS)   // 🔴 V794 — ছবি ছাড়া
            if (rows.length() == 0) return false
            val pat = rows.getJSONObject(0)
            val existingRefBy = pat.s("refBy")
            val existingRefMobile = pat.s("refDoctorMobile")
            if (existingRefBy.isNotBlank() || existingRefMobile.isNotBlank()) return true // already linked to someone -- leave as is
            val fields = org.json.JSONObject()
                .put("refBy", doctorName)
                .put("refDoctorMobile", doctorMobile)
            val ok = SupabaseClient.updateById("patients", patientRowId, fields)
            if (!ok && context != null) {
                try { GenericUpdateQueue.queue(context, "patients", patientRowId, fields) } catch (_: Throwable) { }
            }
            ok
        } catch (e: Exception) {
            false
        }
    }

    // 🔒 TK-ORDER (31.07.2026, খাতার সারি B211 — TK: "মাস্টার এডমিন বুঝবে কি করে
    // কোন আরএমপি ডাক্তার কত পেশেন্ট পাঠিয়েছে... এই মাসে · সর্বমোট · কোন
    // ডাক্তারের পারফরম্যান্স ভালো... এটা শুধু মাস্টারই দেখতে পারবে"): নতুন
    // "RMP Performance Report" — প্রতিটা ডাক্তারের এই-মাসের/সর্বমোট রেফার করা
    // পেশেন্ট-সংখ্যা ও রেফারেল ইনকাম এক জায়গায়।
    /** একজন রেফার-করা পেশেন্টের প্রয়োজনীয় তথ্য — কার্ডে "This Month"/"All-Time"
     *  চাপলে তালিকায় দেখানোর জন্য। rawDate খালি হলে সবার শেষে সাজে। */
    data class RmpReferredPatient(val id: String, val name: String, val mobile: String, val rawDate: String, val bill: Double)

    /** একজন ডাক্তারের সম্পূর্ণ পারফরম্যান্স-সারি। ⛔ referralPayments (তারিখ+
     *  পরিমাণ+Paid/Unpaid) ডাক্তারের নিজের raw row-এই থাকে (doctor.raw),
     *  তাই "Ref Paid" চাপলে আলাদা কোনো ক্লাউড-কল ছাড়াই বিস্তারিত দেখানো যায়। */
    data class RmpPerformanceRow(
        val doctor: DoctorVisitItem,
        val referred: List<RmpReferredPatient>,
        val thisMonthCount: Int,
        val allTimeCount: Int,
        val refPaid: Double,
        val mostRecentDate: String
    )

    /** ⛔ হুবহু loadList()-এর referredCount গণনার একই নিয়মে (নাম বা মোবাইল
     *  মিলিয়ে) — তাই এই রিপোর্টের সংখ্যা RMP তালিকার কার্ডে দেখানো সংখ্যার
     *  সাথে কখনো আলাদা হবে না। ⛔ একটাই patients-fetch (৫০০০ পর্যন্ত, ৭টা
     *  সরু কলাম) — ডাক্তার-প্রতি আলাদা কোনো ক্লাউড-কল নেই।
     *  branchFilter ফাঁকা/"All" হলে সব ব্রাঞ্চ (Master), নইলে শুধু সেই ব্রাঞ্চ।
     *
     *  🔴 B422 (05.08.2026, TK-রিপোর্ট — "Loading..." ২৫-২৭ সেকেন্ড আটকে
     *  থাকা, দ্রুত WiFi-তেও): আসল কারণ ধরা পড়েছে — নিচের patients-fetch
     *  আগে সবসময় filter=null পাঠাত, অর্থাৎ TK একটা নির্দিষ্ট ব্রাঞ্চ
     *  (যেমন Jalpaiguri) বেছে নিলেও, ক্লাউড থেকে **সবকটা ব্রাঞ্চের** ৫০০০
     *  পর্যন্ত রোগীর সারি টেনে আনা হতো — ব্রাঞ্চ-বাছাই শুধু তারপর, ফোনের
     *  ভিতরে, লুপে ফিল্টার হতো। ক্লিনিক সপ্তাহ ধরে চলছে বলে মোট রোগীর
     *  সংখ্যা এখন যথেষ্ট বড়, তাই এই ভারী ডাউনলোড ধীরগতির নয় এমন নেটেও
     *  ২৫ সেকেন্ডের সীমা (`SupabaseClient`-এর `callTimeout`) ছুঁয়ে ফেলত।
     *  **সমাধান:** ব্রাঞ্চ নির্দিষ্ট থাকলে (Master একটা বেছে নিলে) সেই
     *  ব্রাঞ্চের সারিই সরাসরি ক্লাউড থেকে চাওয়া হয় — ঠিক পাশের doctors-
     *  fetch (`fetchList()`) আগে থেকেই যেভাবে করে। "All" বাছলে (সব ব্রাঞ্চ
     *  মিলিয়ে দেখতে চাইলে) আগের মতোই সবকটা আসে — সেক্ষেত্রে স্বাভাবিকভাবেই
     *  একটু বেশি সময় লাগতে পারে, কিন্তু সেটা প্রত্যাশিত/ইচ্ছাকৃত ব্যবহার।
     *  ⛔ ফিল্টারের বাইরে ম্যাচিং/গণনা-লজিক এক অক্ষরও বদলায়নি — শুধু কম
     *  ডেটা টানা হয়, তাই দ্রুত হয়।
     */
    fun fetchRmpPerformance(branchFilter: String?): List<RmpPerformanceRow> {
        val doctors = fetchList(branchFilter)
        // V328: normally download only the small protected metric result.
        // Unknown/malformed/missing doctor identities are never guessed; the
        // entire unchanged legacy patient calculation below remains fallback.
        val fast = RmpCommissionRepository.legacyPerformance(branchFilter)
        if (fast.ok && fast.value != null) {
            val doctorsById = doctors.associateBy { it.id }
            val metrics = fast.value
            if (metrics.all { doctorsById.containsKey(it.rmpId) }) {
                return metrics.map { metric ->
                    RmpPerformanceRow(
                        doctor = doctorsById.getValue(metric.rmpId),
                        referred = emptyList(),
                        thisMonthCount = metric.thisMonthCount,
                        allTimeCount = metric.allTimeCount,
                        refPaid = metric.referralPaid,
                        mostRecentDate = metric.mostRecentDate
                    )
                }
            }
        }

        val patientFilter = if (!branchFilter.isNullOrBlank() && branchFilter != "All")
            "branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else null
        // 🔴🔒 V1362 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট) — `refDoctor` যোগ (V1356-এর
        // একই কারণ: আজকের অ্যাপ ডাক্তারের নাম রাখে refDoctor-এ, refBy-তে শুধু "Dr. Visit")।
        val patients = SupabaseClient.fetchListSlim(
            "patients", patientFilter, 5000,
            "id,name,mobile,refBy,refDoctor,refDoctorMobile,branch,registrationDate,date,bill"
        )
        val thisYm = DoctorVisitModel.today().take(7)
        val rows = mutableListOf<RmpPerformanceRow>()
        for (doc in doctors) {
            val docName = doc.name.trim().lowercase()
            val docMobile = doc.mobile.filter { it.isDigit() }.takeLast(10)
            val referred = mutableListOf<RmpReferredPatient>()
            for (i in 0 until patients.length()) {
                val p = patients.optJSONObject(i) ?: continue
                if (!branchFilter.isNullOrBlank() && branchFilter != "All") {
                    val pb = p.s("branch")
                    if (pb.isNotBlank() && !pb.equals(branchFilter, ignoreCase = true)) continue
                }
                val refBy = p.s("refBy").trim().lowercase()
                val refDoc = p.s("refDoctor").trim().lowercase()
                val refMob = p.s("refDoctorMobile").filter { it.isDigit() }.takeLast(10)
                val matches = (refBy.isNotBlank() && refBy == docName) || (refDoc.isNotBlank() && refDoc == docName) ||
                    (refMob.isNotBlank() && refMob == docMobile)
                if (!matches) continue
                val regDate = p.s("registrationDate").ifBlank { p.s("date") }
                referred.add(RmpReferredPatient(p.s("id"), p.s("name"), p.s("mobile"), regDate, p.optDouble("bill", 0.0)))
            }
            if (referred.isEmpty()) continue // ⛔ TK: "কোন কোন ডাক্তার পেশেন্ট পাঠিয়েছে" -- শূন্য-রেফারেল ডাক্তার এই রিপোর্টে দেখানো হয় না
            val thisMonthCount = referred.count { it.rawDate.take(7) == thisYm }
            val mostRecent = referred.mapNotNull { it.rawDate.ifBlank { null } }.maxOrNull() ?: ""
            rows.add(
                RmpPerformanceRow(
                    doctor = doc,
                    referred = referred.sortedByDescending { it.rawDate },
                    thisMonthCount = thisMonthCount,
                    allTimeCount = referred.size,
                    refPaid = doc.referralPaid,
                    mostRecentDate = mostRecent
                )
            )
        }
        // 🔒 TK-এর স্পষ্ট নির্দেশ (31.07.2026): "সাম্প্রতিক রেফারেলের তারিখ
        // অনুযায়ী" সাজানো -- সবচেয়ে নতুন রেফারেল সবার উপরে।
        return rows.sortedByDescending { it.mostRecentDate }
    }
}
