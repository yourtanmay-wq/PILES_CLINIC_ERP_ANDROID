package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.tkbiswas.pilesclinic.print.BranchCatalog
import java.io.File
import java.io.FileOutputStream

/**
 * 🔒 TK-APPROVED (28.07.2026, প্রুফ ১৩ ও ১৪) — WhatsApp-এ যে ছবিটা যাবে।
 *
 * ক্লিনিকের নিজের ডিজাইন করা কার্ড: গাঢ় সবুজ হেডারে **প্রজেক্টের নিজের আসল
 * লোগো** (কিশনগঞ্জে kishanganj-final-logo.jpg, বাকি ব্রাঞ্চে
 * maa-ayurved-final-logo.jpg — BranchCatalog.logoAssetPath থেকেই), পাশে
 * ক্লিনিকের নাম ও ঠিকানা, নিচে সাদা অংশে তিন ভাষার লেখা, একদম নিচে হেল্পলাইন।
 *
 * ⛔ ছবিতে কোথাও কোনো AI বা ডিজাইন টুলের নাম নেই, কোনো ওয়াটারমার্ক নেই —
 * শুধু ক্লিনিকের নাম ও রোগীর খবর (TK-এর নির্দেশ)।
 *
 * ছবিটা অ্যাপ নিজেই আঁকে, তাই ইন্টারনেট লাগে না এবং কোনো খরচ নেই।
 */
object PatientMessageCard {

    private const val W = 1080
    private const val PAD = 56f

    private fun paint(size: Float, color: Int, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    /** লেখা কার্ডের চওড়ার মধ্যে ভেঙে দেয়, যাতে কোথাও কেটে না যায়। */
    private fun wrap(text: String, p: Paint, maxWidth: Float): List<String> {
        val out = ArrayList<String>()
        for (raw in text.split("\n")) {
            if (raw.isBlank()) { out.add(""); continue }
            var line = StringBuilder()
            for (word in raw.split(" ")) {
                val trial = if (line.isEmpty()) word else line.toString() + " " + word
                if (p.measureText(trial) <= maxWidth) {
                    line = StringBuilder(trial)
                } else {
                    if (line.isNotEmpty()) out.add(line.toString())
                    line = StringBuilder(word)
                }
            }
            out.add(line.toString())
        }
        return out
    }

    private fun loadLogo(context: Context, assetPath: String): Bitmap? = try {
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
    } catch (_: Throwable) { null }

    /**
     * কার্ডটা এঁকে cache/images-এ PNG হিসেবে রাখে এবং শেয়ার করার Uri ফেরত দেয়।
     * কিছু ভুল হলে null — তখন শুধু লেখাটাই পাঠানো হয়, কিছু ভাঙে না।
     */
    fun build(context: Context, branch: String, bodyText: String): Uri? {
        return try {
            val info = BranchCatalog.byName(branch)
            val bodyPaint = paint(30f, Color.rgb(58, 72, 92))
            val maxW = W - PAD * 2
            val lines = wrap(bodyText, bodyPaint, maxW)

            val headerH = 240
            val footerH = 120
            val lineH = 44
            val bodyH = (lines.size * lineH) + 60
            val height = headerH + bodyH + footerH

            val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.rgb(253, 252, 249))

            // header
            val hp = Paint(Paint.ANTI_ALIAS_FLAG)
            hp.shader = LinearGradient(
                0f, 0f, W.toFloat(), headerH.toFloat(),
                Color.rgb(8, 46, 38), Color.rgb(14, 76, 56), Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, W.toFloat(), headerH.toFloat(), hp)
            val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(198, 160, 74); strokeWidth = 5f
            }
            c.drawRect(0f, headerH - 5f, W.toFloat(), headerH.toFloat(), gold)

            // logo on a white tile
            val logo = loadLogo(context, info.logoAssetPath)
            var textLeft = PAD
            if (logo != null) {
                val tile = RectF(PAD, 40f, PAD + 160f, 200f)
                val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                c.drawRoundRect(tile, 24f, 24f, white)
                val inner = Rect(
                    (tile.left + 14).toInt(), (tile.top + 14).toInt(),
                    (tile.right - 14).toInt(), (tile.bottom - 14).toInt()
                )
                c.drawBitmap(logo, null, inner, Paint(Paint.FILTER_BITMAP_FLAG))
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(198, 160, 74); style = Paint.Style.STROKE; strokeWidth = 4f
                }
                c.drawRoundRect(tile, 24f, 24f, border)
                textLeft = PAD + 190f
            }

            val nameParts = info.clinicName.split(" ")
            val topName = nameParts.dropLast(2).joinToString(" ").ifBlank { info.clinicName }
            val subName = nameParts.takeLast(2).joinToString(" ")
            c.drawText(topName, textLeft, 96f, paint(46f, Color.WHITE, true))
            c.drawText(subName, textLeft, 148f, paint(38f, Color.rgb(198, 160, 74), true))
            c.drawText(info.addressLine, textLeft, 190f, paint(24f, Color.rgb(196, 214, 206)))

            // body
            var y = headerH + 52f
            for (ln in lines) {
                if (ln.isNotBlank()) c.drawText(ln, PAD, y, bodyPaint)
                y += lineH
            }

            // footer
            val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(8, 46, 38) }
            c.drawRect(0f, (height - footerH).toFloat(), W.toFloat(), height.toFloat(), fp)
            c.drawText(
                "Helpline  " + info.phoneLine, PAD, height - footerH + 74f,
                paint(32f, Color.rgb(198, 160, 74), true)
            )
            val tag = "Kshar Sutra \u00b7 Ayurveda"
            val tagPaint = paint(24f, Color.rgb(196, 214, 206))
            c.drawText(tag, W - PAD - tagPaint.measureText(tag), height - footerH + 72f, tagPaint)

            val dir = File(context.cacheDir, "images")
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, "clinic_message.png")
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()

            FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", out
            )
        } catch (_: Throwable) {
            null
        }
    }
}
