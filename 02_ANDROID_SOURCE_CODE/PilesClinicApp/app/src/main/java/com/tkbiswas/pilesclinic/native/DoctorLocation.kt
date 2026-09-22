package com.tkbiswas.pilesclinic.native

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONObject

/**
 * 🛰️🔒 V1348 (১১.০৯.২০২৬, TK-নির্দেশ — "staff/branch/doctor সব"-এর লোকেশন,
 * ডাক্তারের অংশ)। TK-র স্পষ্ট নিষেধ: ডাক্তারের ফোনে কোনো নতুন বোতাম/কার্ড/
 * প্রম্পট-ডায়ালগ দেখানো যাবে না — *"না এরকম কিছু দেখাবে না... এমনি যদি
 * তার মোবাইলের লোকেশন অন থাকবে তাহলে আমি দেখতে পাবো অন্যথায় নয়"*।
 *
 * ⛔ কোনো foreground service নয়, কোনো persistent notification নয়, কোনো
 *    active GPS-listener নয় — শুধু `getLastKnownLocation()` (ফোনে অন্য কোনো
 *    অ্যাপ/সিস্টেম-সার্ভিস থেকে ইতিমধ্যে জমা থাকা শেষ অবস্থান, সম্পূর্ণ
 *    প্যাসিভ, ব্যাটারি-খরচ শূন্য)। লোকেশন সত্যিই অন না থাকলে/অনুমতি না
 *    থাকলে কিছুই জমা হয় না — TK-র ঠিক এই শর্তটাই কোডে বসানো হলো।
 * ⛔ RUPAM-এর `isFieldStaff`/`FieldVisit`-এর IN-OUT/কিমি-হিসাবের সাথে এর
 *    কোনো সম্পর্ক নেই — সম্পূর্ণ আলাদা, ছোট, ডাক্তার-শুধু টেবিল।
 */
object DoctorLocation {

    private fun hasAnyLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /* 🔵 Dashboard-এ ডাক্তার লগইন করলেই ডাকা হয় (onResume) — চুপচাপ, ব্যর্থ
       হলেও (অনুমতি নেই/লোকেশন অফ/নেট নেই) অ্যাপ কখনো থামবে না। */
    fun captureIfPossible(context: Context) {
        try {
            if (!RoleRules.isDoctor(context)) return
            if (!hasAnyLocationPermission(context)) return
            val mobile = StaffDirectory.normalizeMobile(NativeSession.current(context)?.mobile.orEmpty())
            if (mobile.isBlank()) return
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            var best: android.location.Location? = null
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                try {
                    val loc = lm.getLastKnownLocation(p) ?: continue
                    if (best == null || loc.time > best!!.time) best = loc
                } catch (_: Throwable) { }
            }
            val loc = best ?: return
            // ⛔ ৩ ঘণ্টার বেশি পুরনো ক্যাশ-করা ফিক্স "এখন" বলে চালানো হয় না।
            if (System.currentTimeMillis() - loc.time > 3 * 60 * 60 * 1000L) return
            val row = JSONObject()
                .put("mobile", mobile)
                .put("lat", loc.latitude)
                .put("lng", loc.longitude)
                .put("accuracy_m", loc.accuracy.toDouble())
                .put("updated_at", isoNow())
            Thread {
                try { ModuleAuth.upsertOnConflict("wn", "doctor_locations", row, "mobile") } catch (_: Throwable) { }
            }.start()
        } catch (_: Throwable) { }
    }

    private fun isoNow(): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date())
    }

    data class LastSeen(val lat: Double, val lng: Double, val updatedAt: String, val accuracyM: Double)

    /** মাস্টারের পর্দায় "Location" বোতাম চাপলে ডাকা হয় — নেটওয়ার্ক-থ্রেডে চালাতে হবে। */
    fun fetchLastSeen(mobile: String): LastSeen? {
        val m = StaffDirectory.normalizeMobile(mobile)
        if (m.isBlank()) return null
        val rows = ModuleAuth.getRows("wn", "doctor_locations", "select=*&mobile=eq.$m&limit=1")
        if (rows.length() == 0) return null
        val r = rows.getJSONObject(0)
        return LastSeen(r.optDouble("lat"), r.optDouble("lng"), r.optString("updated_at"), r.optDouble("accuracy_m"))
    }
}
