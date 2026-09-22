package com.tkbiswas.pilesclinic.clinical

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import com.tkbiswas.pilesclinic.native.PhotoUtils

/**
 * 🔵🔒 V584 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — **ভাগ ৬ "রোগের ছবি"
 * A4 রিপোর্টে বসানোর** একমাত্র জায়গা।
 *
 * TK-নির্দেশ: *"যে সমস্ত জিনিস মিসিং আছে সেগুলো যুক্ত করবেন"* — ছবিটা এতদিন
 * প্রিন্টে যেত না।
 *
 * কীভাবে: পর্দায় যে `AnatomyView`-তে ডাক্তার আঁকেন, ঠিক **সেই একই View**
 * পর্দার বাইরে একবার তৈরি করে, সেভ-করা দাগ বসিয়ে, একটা bitmap-এ আঁকা হয়;
 * তারপর PNG → base64 data-URL। রিপোর্টের WebView-এ JavaScript বন্ধ থাকে,
 * তাই ক্যানভাসে আঁকা সম্ভব নয় — ছবি হিসেবেই বসাতে হয়।
 *
 * ⛔ **ঝুঁকিহীন:** নতুন আলাদা ফাইল। `AnatomyView`/`AnatomyModel`-এ এক অক্ষরও
 * বদলায়নি — শুধু তাদের আগে থেকে থাকা public `load()`/`parse()` ডাকা হয়।
 * কোনো নেটওয়ার্ক কল নেই (ছবির তালিকা ফোনে জমা থাকা cache থেকেই পড়া হয়),
 * তাই Supabase free-plan-এ বাড়তি egress শূন্য।
 *
 * ⚠️ কিছু আঁকা না থাকলে বা কোনো কারণে ছবি বানানো না গেলে ফাঁকা লেখা ফেরে —
 *    তখন রিপোর্টে ছবির ঘরটাই বসে না, কিছু ভাঙে না।
 */
object CheckupAnatomyImage {

    fun dataUrl(ctx: Context, saved: String?, w: Int = 640, h: Int = 560): String {
        if (saved.isNullOrBlank()) return ""
        val board = AnatomyModel.parse(saved)
        if (board.marks.isEmpty() && board.pic.isBlank()) return ""
        return try {
            val v = AnatomyView(ctx)
            v.load(saved) { key ->
                try { ctx.resources.getIdentifier(key, "drawable", ctx.packageName) }
                catch (_: Throwable) { 0 }
            }
            // ডাক্তারের নিজের যোগ করা ছবি (V573) — drawable নয়, তাই আলাদা পথ।
            if (!v.hasPicture() && board.pic.isNotBlank()) {
                val row = AnatomyPictureRepository.pictures(ctx).firstOrNull { it.key == board.pic }
                if (row != null && row.photo.isNotBlank())
                    v.setBaseBitmap(PhotoUtils.decodeDataUrl(row.photo))
            }
            v.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
            )
            v.layout(0, 0, w, h)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            v.draw(c)
            val bos = java.io.ByteArrayOutputStream()
            // JPEG — ছবিটা মূলত ফটো, PNG-তে ৫০০KB+ হয়ে রিপোর্ট ভারী করে দিত।
            // মান ৮৫-এ চোখে তফাত বোঝা যায় না, মাপ ~৭ ভাগের ১ হয়।
            // ⚠️ ওয়েবের `wlv1A4AnatImage()`-এর হুবহু একই মান।
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
            bmp.recycle()
            "data:image/jpeg;base64," +
                android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (_: Throwable) { "" } catch (_: OutOfMemoryError) { "" }
    }
}
