package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Shared image helpers for the photo screens: downscale + JPEG-compress a picked
 * gallery image into a small data-URL, and decode a data-URL back to a bitmap.
 * Kept small so both Patient Photo and User Photo use identical encoding.
 */
object PhotoUtils {

    /** TK-APPROVED (2026-07-26): was maxSide=400, quality=70 (~22 KB), which
     *  looked soft on the Patient Card and on print. Raised to 600 / 85 --
     *  measured at roughly 54 KB as a JPEG, about 72 KB once stored as a
     *  data-URL, so it stays far below TK's 100-150 KB ceiling and costs only
     *  about 2.5x the old size, not the 3-4x that was feared. Every OLD photo
     *  is untouched -- this only affects photos taken from now on. Callers,
     *  screens, shapes and layout are all unchanged; the two numbers are the
     *  only difference. PatientPhotoActivity has its own private copy of this
     *  same logic and was updated to match, so both screens stay identical. */
    /**
     * 🔵🔒 V524 (২২.০৮.২০২৬, TK-নির্দেশ) — **ছবি কাত হয়ে যাওয়ার আসল কারণ।**
     *
     * ফোনের ক্যামেরা ছবিটা প্রায়ই **সোজা করে সাজায় না** — সেন্সর যেভাবে
     * পেয়েছে সেভাবেই রেখে দেয়, আর পাশে একটা ছোট নোট (EXIF `Orientation`)
     * লিখে দেয়: *"দেখানোর সময় ৯০°/১৮০°/২৭০° ঘুরিয়ে নিও।"*
     *
     * **সমস্যা যেটা ছিল:** নিচের `encodeResized()` ওই নোটটা **পড়তই না** —
     * শুধু pixel নিয়ে ছোট করে JPEG বানাত। ফলে ছবিটা **কাত হয়ে** জমা হত
     * (TK-এর ছবিতে BIKASH SAHA ও MUR MAHAMMAD ALI-র কার্ডে যেমন)।
     * প্রজেক্টের কোথাও EXIF পড়া হত না — পুরো প্রজেক্ট খুঁজে যাচাই করা।
     *
     * ⛔ `android.media.ExifInterface` **Android-এরই নিজের** (API 24 থেকে
     *    InputStream পড়তে পারে; এই অ্যাপের minSdk 24)। **কোনো নতুন
     *    লাইব্রেরি যোগ করা হয়নি।**
     * ⛔ নোট না থাকলে/পড়া না গেলে `0` ফেরে — তখন আচরণ **হুবহু আগের মতোই**।
     */
    private fun exifRotation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            @Suppress("DEPRECATION")
            when (android.media.ExifInterface(ins).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (_: Throwable) { 0 }

    /**
     * 🔵🔒 V524: একটা bitmap-কে ঘুরিয়ে দেওয়া। `degrees` ০ হলে **একই bitmap-ই**
     * ফেরে (নতুন কিছু তৈরি হয় না, স্মৃতিও নষ্ট হয় না)।
     */
    fun rotated(src: Bitmap, degrees: Int): Bitmap {
        val d = ((degrees % 360) + 360) % 360
        if (d == 0) return src
        return try {
            val m = android.graphics.Matrix().apply { postRotate(d.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        } catch (_: Throwable) { src }   // স্মৃতি কম পড়লে আগের ছবিটাই থাক
    }

    /** 🔵🔒 V524: bitmap → data-URL, ঠিক যে মাপ ও মানে এই ফাইল বরাবর বানায়। */
    fun encodeBitmap(bmp: Bitmap, maxSide: Int = 600, quality: Int = 85): String? = try {
        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height).toFloat()
        val small = if (scale < 1f) Bitmap.createScaledBitmap(
            bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
        ) else bmp
        val out = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, quality, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (_: Throwable) { null }

    fun encodeResized(context: Context, uri: Uri, maxSide: Int = 600, quality: Int = 85): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null
            // 🔵 V524: ক্যামেরার নোট মেনে আগে সোজা করা, তারপর আগের মতোই ছোট করা।
            val upright = rotated(original, exifRotation(context, uri))
            val scale = maxSide.toFloat() / maxOf(upright.width, upright.height).toFloat()
            val bmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    upright, (upright.width * scale).toInt(), (upright.height * scale).toInt(), true
                )
            } else upright
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeDataUrl(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank()) return null
        return try {
            val comma = dataUrl.indexOf(',')
            if (comma < 0) return null
            val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
