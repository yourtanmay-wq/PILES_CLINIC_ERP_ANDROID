package com.tkbiswas.pilesclinic.native

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 📍🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §৫) — **ক্লিনিকে আছেন কিনা।**
 *
 * TK-এর সিদ্ধান্ত: **GPS** (ওয়াই-ফাই নয়)। ব্রাঞ্চের অবস্থান অ্যাপের ভিতরেই
 * (`ClinicLocations.kt`) — নতুন কোনো ডেটাবেস টেবিল বানানো হয়নি।
 *
 * ─── নিয়ম ─────────────────────────────────────────────────────────────────
 *  • GPS **সারাক্ষণ চলে না** — শুধু Staff IN TIME চাপার মুহূর্তে চালু হয়,
 *    ফল পাওয়ামাত্র বা সময় শেষে সঙ্গে সঙ্গে বন্ধ। ব্যাটারি খরচ নেই বললেই চলে।
 *  • দূরত্ব **ও** নির্ভুলতা — দুটোই দেখা হয়। খুব অনিশ্চিত অবস্থান (যেমন
 *    ৳±৫০০ মিটার) দিয়ে সিদ্ধান্ত নেওয়া হয় না।
 *  • **নকল অবস্থান** ধরা পড়লে গ্রহণ করা হয় না।
 *  • অনুমতি নেই · Location বন্ধ · সময় শেষ · দুর্বল নির্ভুলতা — প্রতিটার
 *    আলাদা বার্তা। কোনো অবস্থাতেই crash বা আটকে যাওয়া নয়।
 *  • ব্রাঞ্চের সংখ্যা বসানো না থাকলে **নীরবে গ্রহণ নয়** — স্পষ্ট বার্তা।
 *
 * ⚠️ **সীমাবদ্ধতা (সৎভাবে):** রুট-করা ফোনে বা কিছু পুরনো Android-এ নকল
 *    অবস্থান পুরোপুরি ধরা যায় না। এটা ফাঁকি **কঠিন করে**, একেবারে বন্ধ করে না।
 *    (`isFromMockProvider` Android ৮+ থেকে, `isMock` Android ১২+ থেকে।)
 */
object ClinicPresence {

    enum class Reason {
        INSIDE,
        OUTSIDE,
        NOT_CONFIGURED,     // ওই ব্রাঞ্চের সংখ্যা এখনো বসানো হয়নি
        NO_PERMISSION,
        LOCATION_OFF,
        TIMEOUT,
        LOW_ACCURACY,
        MOCK_DETECTED,
        UNKNOWN_BRANCH,
        ERROR
    }

    data class Outcome(
        val ok: Boolean,
        val reason: Reason,
        val message: String,
        val distanceMeters: Int? = null,
        /* 🏥🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ — *"কোন staff যদি অন্য ব্রাঞ্চে গিয়ে
           ডিউটি করতে যায় সেটার ব্যাবস্থা কি করা যায়"*): স্টাফ **যে ব্রাঞ্চের
           ক্লিনিকে দাঁড়িয়ে আছেন** তার নাম (যেমন "Cooch Behar")। শুধু
           `anyBranch = true` দিয়ে ডাকলে ভরে; নইলে আগের মতোই ফাঁকা, তাই
           পুরনো কোনো caller-এর কিছু বদলায় না। */
        val atBranch: String = ""
    )

    /** কত সেকেন্ড পর্যন্ত অবস্থানের জন্য অপেক্ষা করা হবে। */
    private const val TIMEOUT_MS = 12_000L

    /** এর চেয়ে বেশি অনিশ্চিত অবস্থান দিয়ে সিদ্ধান্ত নেওয়া হয় না (মিটার)। */
    private const val MAX_ACCURACY_M = 100f

    /**
     * 🔤🔒 V519 (২২.০৮.২০২৬, TK-নির্দেশ, ছবিসহ): *"এখানে বাংলায় কিছু লেখা
     * থাকবে না, এগুলো ইংলিশে থাকবে।"*
     *
     * আগে লেখাগুলো বাংলায় ছিল এবং ইংরেজি হত **শুধু `NoBengali` চালু থাকলে**,
     * অর্থাৎ কেবল কিশানগঞ্জ ব্রাঞ্চের স্টাফের ফোনে (খাতার সারি B158)।
     * TK-এর পাঠানো ছবিটা **JPE-JALPAI-13**-এর, তাই সেখানে বাংলাই দেখাচ্ছিল।
     * ⇒ হাজিরার এই লেখাগুলো এখন **সরাসরি ইংরেজিতেই** লেখা — সব ব্রাঞ্চে,
     *   সব স্টাফের ফোনে এক রকম। (V509-এ আঙুলের ছাপের পর্দায় TK একই
     *   নির্দেশ দিয়েছিলেন — এটা তারই ধারাবাহিকতা।)
     * ⛔ কোনো নিয়ম · দূরত্বের হিসাব · GPS পাহারা · ডেটা কিছুই বদলায়নি —
     *    শুধু পর্দার লেখা।
     */
    fun messageFor(reason: Reason, point: ClinicLocations.ClinicPoint?, distance: Int?): String =
        when (reason) {
            Reason.INSIDE -> ""
            Reason.OUTSIDE ->
                "You are not at the clinic" +
                    (if (distance != null) " (about $distance m away)" else "") +
                    ". Attendance can only be marked at the chamber."
            Reason.NOT_CONFIGURED ->
                "The location of ${point?.displayName ?: "this branch"} has not been set in the app yet. " +
                    "Please inform the Master - he can mark your attendance using Fix Attendance."
            Reason.NO_PERMISSION ->
                "Attendance needs Location permission for this app. Please allow it and try again."
            Reason.LOCATION_OFF ->
                "Your phone's Location is turned off. Please turn it on and try again."
            Reason.TIMEOUT ->
                "Could not get your location. Please move to an open area or stand near a window and try again."
            Reason.LOW_ACCURACY ->
                "Your location is not accurate enough yet. Please wait a moment or move near a window and try again."
            Reason.MOCK_DETECTED ->
                "A fake location was detected, so attendance was not taken. The Master will be informed."
            Reason.UNKNOWN_BRANCH ->
                "Your branch could not be identified. Please inform the Master."
            Reason.ERROR ->
                "Could not check your location. Please try again in a moment."
        }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun locationEnabled(lm: LocationManager): Boolean = try {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (_: Throwable) { false }

    /** Android যেটুকু বলতে পারে — নকল অবস্থান কিনা। */
    private fun isMock(loc: Location): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock
        else @Suppress("DEPRECATION") loc.isFromMockProvider
    } catch (_: Throwable) { false }

    /**
     * এখনকার অবস্থান নিয়ে ক্লিনিকে আছেন কিনা দেখে। **ঠিক একবার** [onResult] ডাকে।
     * ফল পাওয়ামাত্র GPS বন্ধ করে দেওয়া হয়।
     */
    /**
     * 🏥🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ) — **অন্য ব্রাঞ্চে গিয়ে ডিউটি।**
     *
     * TK-এর রিপোর্ট: *"আজকে Jalpaiguri staff CRP Coochbehar ক্লিনিকে এসেছিল এবং
     * ডিউটি করেছে কিন্তু সে Intime চাপতে পারে নাই"*।
     *
     * **আসল কারণ (কোডে মেপে দেখা, আন্দাজ নয়):** এই ফাংশন এতদিন শুধু স্টাফের
     * **নিজের** ব্রাঞ্চের পিন থেকে দূরত্ব মাপত। জলপাইগুড়ি (26.536368, 88.720920)
     * আর কোচবিহারের (26.327655, 89.442545) মাঝে প্রায় ৭৩ কিমি, ছাড় মাত্র
     * ১৫০ মিটার ⇒ কোচবিহারে দাঁড়িয়ে জলপাইগুড়ির স্টাফ সবসময় `OUTSIDE` পড়তেন,
     * IN TIME সম্পূর্ণ আটকে যেত।
     *
     * ⇒ [anyBranch] `true` দিলে **৫টা ব্রাঞ্চের যেকোনো একটার** কাছে থাকলেই পাশ,
     *   আর কোন ক্লিনিকে দাঁড়িয়ে আছেন সেটা [Outcome.atBranch]-এ ফেরত আসে।
     * ⛔ ডিফল্ট `false` — অর্থাৎ যে caller এটা চায় না তার আচরণ **এক অক্ষরও**
     *   বদলায়নি। দূরত্বের হিসাব · GPS পাহারা · নকল-অবস্থান ধরা · সময়সীমা —
     *   সব হুবহু আগের।
     */
    fun check(activity: AppCompatActivity, branchName: String?, anyBranch: Boolean = false, onResult: (Outcome) -> Unit) {
        val point = ClinicLocations.forBranchName(branchName)
        if (point == null) {
            onResult(Outcome(false, Reason.UNKNOWN_BRANCH, messageFor(Reason.UNKNOWN_BRANCH, null, null)))
            return
        }
        if (!point.isConfigured) {
            onResult(Outcome(false, Reason.NOT_CONFIGURED, messageFor(Reason.NOT_CONFIGURED, point, null)))
            return
        }
        if (!hasPermission(activity)) {
            onResult(Outcome(false, Reason.NO_PERMISSION, messageFor(Reason.NO_PERMISSION, point, null)))
            return
        }

        val lm = try {
            activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        } catch (_: Throwable) { null }
        if (lm == null) {
            onResult(Outcome(false, Reason.ERROR, messageFor(Reason.ERROR, point, null)))
            return
        }
        if (!locationEnabled(lm)) {
            onResult(Outcome(false, Reason.LOCATION_OFF, messageFor(Reason.LOCATION_OFF, point, null)))
            return
        }

        var done = false
        val handler = Handler(Looper.getMainLooper())
        var listener: LocationListener? = null

        fun stop() {
            try { listener?.let { lm.removeUpdates(it) } } catch (_: Throwable) { }
        }
        fun finish(r: Outcome) {
            if (done) return
            done = true
            stop()
            handler.removeCallbacksAndMessages(null)
            try { activity.runOnUiThread { onResult(r) } } catch (_: Throwable) { onResult(r) }
        }

        fun judge(loc: Location?) {
            if (loc == null) return
            if (isMock(loc)) { finish(Outcome(false, Reason.MOCK_DETECTED, messageFor(Reason.MOCK_DETECTED, point, null))); return }
            val acc = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE
            if (acc > MAX_ACCURACY_M) return          // আরও ভালো ফলের অপেক্ষা
            /* 🏥 V1179 — `anyBranch=false` হলে আগের মতোই **শুধু নিজের ব্রাঞ্চ**;
               `true` হলে ৫টা ব্রাঞ্চের প্রত্যেকটার দূরত্ব মেপে সবচেয়ে কাছেরটা
               ধরা হয়। ⛔ দুই ক্ষেত্রেই মাপার নিয়ম হুবহু এক। */
            val homePoint: ClinicLocations.ClinicPoint = point
            val candidates: List<ClinicLocations.ClinicPoint> =
                if (anyBranch) ClinicLocations.ALL.filter { it.isConfigured }
                else listOf(homePoint)
            var bestPoint: ClinicLocations.ClinicPoint = homePoint
            var bestDist = Int.MAX_VALUE
            for (c in candidates) {
                val target = Location("clinic").apply {
                    latitude = c.lat!!; longitude = c.lng!!
                }
                val dd = loc.distanceTo(target).toInt()
                if (dd < bestDist) { bestDist = dd; bestPoint = c }
            }
            if (bestDist == Int.MAX_VALUE) {
                finish(Outcome(false, Reason.NOT_CONFIGURED, messageFor(Reason.NOT_CONFIGURED, homePoint, null)))
                return
            }
            if (bestDist <= bestPoint.radiusMeters) {
                finish(Outcome(true, Reason.INSIDE, "", bestDist, bestPoint.displayName))
            } else {
                finish(Outcome(false, Reason.OUTSIDE, messageFor(Reason.OUTSIDE, bestPoint, bestDist), bestDist))
            }
        }

        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { judge(location) }
            @Deprecated("পুরনো Android-এ দরকার")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }

        try {
            // দুটো উৎসেই চাওয়া হয় — ঘরের ভিতরে NETWORK আগে আসতে পারে।
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (lm.isProviderEnabled(p)) lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper())
            }
            // সদ্য জানা অবস্থান থাকলে সেটাও দেখা হয় (দ্রুত ফল) — তবে
            // ২ মিনিটের বেশি পুরনো হলে নয়, নইলে বাড়ির অবস্থান কাজে লেগে যেত।
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                val last = try {
                    if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null
                } catch (_: SecurityException) { null }
                if (last != null && System.currentTimeMillis() - last.time <= 120_000L) judge(last)
                if (done) return
            }
        } catch (_: SecurityException) {
            finish(Outcome(false, Reason.NO_PERMISSION, messageFor(Reason.NO_PERMISSION, point, null))); return
        } catch (_: Throwable) {
            finish(Outcome(false, Reason.ERROR, messageFor(Reason.ERROR, point, null))); return
        }

        handler.postDelayed({
            // সময় শেষ — কিছুই আসেনি, নাকি এসেছে কিন্তু খুব অনিশ্চিত ছিল।
            finish(Outcome(false, Reason.TIMEOUT, messageFor(Reason.TIMEOUT, point, null)))
        }, TIMEOUT_MS)
    }
}
