package com.kkaloai.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Renders branded share cards as Bitmaps without Compose dependencies.
 * Two formats:
 *  - SQUARE 1080x1080 — TikTok/Reels static, IG feed
 *  - STORY  1080x1920 — Stories, Reels cover, Shorts
 *
 * Brand from KkaloAI Launch Kit:
 *  - Background: graphite (#0F0F0F → #1A1A1A gradient)
 *  - Accent: gold (#D4AF37)
 *  - Text: pearl white (#F8F6F0)
 */
object ShareCardRenderer {

    private const val GRAPHITE_DARK = 0xFF0F0F0F.toInt()
    private const val GRAPHITE_LIGHT = 0xFF1A1A1A.toInt()
    private const val GOLD = 0xFFD4AF37.toInt()
    private const val GOLD_DIM = 0xCCD4AF37.toInt()
    private const val PEARL = 0xFFF8F6F0.toInt()
    private const val PEARL_DIM = 0x88F8F6F0.toInt()

    enum class Format(val width: Int, val height: Int) {
        SQUARE(1080, 1080),
        STORY(1080, 1920)
    }

    fun renderMealCard(
        format: Format,
        mealName: String,
        totalCalories: Int,
        proteinG: Int,
        carbsG: Int,
        fatsG: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(format.width, format.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawBackground(canvas, format)
        drawBrandHeader(canvas, format)
        drawMealHero(canvas, format, mealName, totalCalories)
        drawMacroRow(canvas, format, proteinG, carbsG, fatsG)
        drawFooter(canvas, format, "kkaloai.com")
        return bmp
    }

    fun renderStreakCard(format: Format, streakDays: Int): Bitmap {
        val bmp = Bitmap.createBitmap(format.width, format.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawBackground(canvas, format)
        drawBrandHeader(canvas, format)

        val cx = format.width / 2f
        val centerY = format.height / 2f
        val flame = "🔥"
        val flamePaint = textPaint(220f, GOLD, bold = true)
        canvas.drawText(flame, cx, centerY - 80f, flamePaint)

        val numberPaint = textPaint(280f, PEARL, bold = true)
        canvas.drawText("$streakDays", cx, centerY + 200f, numberPaint)

        val labelPaint = textPaint(56f, GOLD, bold = false)
        canvas.drawText("DAY STREAK", cx, centerY + 290f, labelPaint)

        val tagline = textPaint(40f, PEARL_DIM, bold = false)
        canvas.drawText("Tracking with KkaloAI", cx, centerY + 380f, tagline)

        drawFooter(canvas, format, "kkaloai.com")
        return bmp
    }

    fun renderWeeklyInsightCard(
        format: Format,
        insight: String,
        avgKcal: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(format.width, format.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawBackground(canvas, format)
        drawBrandHeader(canvas, format)

        val cx = format.width / 2f
        val titlePaint = textPaint(60f, GOLD, bold = true)
        canvas.drawText("THIS WEEK", cx, format.height * 0.32f, titlePaint)

        val avgPaint = textPaint(180f, PEARL, bold = true)
        canvas.drawText("$avgKcal", cx, format.height * 0.45f, avgPaint)

        val avgLabel = textPaint(40f, PEARL_DIM, bold = false)
        canvas.drawText("avg kcal / day", cx, format.height * 0.49f, avgLabel)

        val insightPaint = textPaint(48f, PEARL, bold = false)
        wrapAndDrawText(canvas, insight, cx, format.height * 0.65f, format.width - 140, insightPaint, 60f)

        drawFooter(canvas, format, "kkaloai.com")
        return bmp
    }

    /**
     * Save a bitmap to app's cache and return a FileProvider URI safe for Intent.ACTION_SEND.
     * Requires `xml/file_paths.xml` provider config.
     */
    fun saveAndGetUri(context: Context, bitmap: Bitmap, baseName: String): android.net.Uri {
        val dir = File(context.cacheDir, "shared")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$baseName-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    // ---------- private helpers ----------

    private fun drawBackground(canvas: Canvas, format: Format) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, format.height.toFloat(),
                GRAPHITE_DARK, GRAPHITE_LIGHT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, format.width.toFloat(), format.height.toFloat(), paint)
    }

    private fun drawBrandHeader(canvas: Canvas, format: Format) {
        val cx = format.width / 2f
        val brandPaint = textPaint(64f, GOLD, bold = true)
        canvas.drawText("KkaloAI", cx, format.height * 0.13f, brandPaint)

        val sub = textPaint(28f, PEARL_DIM, bold = false)
        canvas.drawText("AI Calorie Scanner", cx, format.height * 0.155f, sub)
    }

    private fun drawMealHero(canvas: Canvas, format: Format, mealName: String, kcal: Int) {
        val cx = format.width / 2f
        val nameY = format.height * 0.32f
        val namePaint = textPaint(56f, PEARL_DIM, bold = false)
        wrapAndDrawText(canvas, mealName, cx, nameY, format.width - 160, namePaint, 70f, maxLines = 2)

        val kcalY = format.height * 0.5f
        val kcalPaint = textPaint(280f, PEARL, bold = true)
        canvas.drawText("$kcal", cx, kcalY, kcalPaint)

        val unitPaint = textPaint(56f, GOLD, bold = true)
        canvas.drawText("kcal", cx, kcalY + 70f, unitPaint)
    }

    private fun drawMacroRow(canvas: Canvas, format: Format, p: Int, c: Int, f: Int) {
        val rowY = format.height * 0.78f
        val labels = listOf("PROTEIN" to p, "CARBS" to c, "FATS" to f)
        val cellW = format.width / 3f
        labels.forEachIndexed { idx, (label, value) ->
            val cx = cellW * idx + cellW / 2f
            val valPaint = textPaint(72f, PEARL, bold = true)
            canvas.drawText("${value}g", cx, rowY, valPaint)
            val lblPaint = textPaint(28f, GOLD_DIM, bold = false)
            canvas.drawText(label, cx, rowY + 50f, lblPaint)
        }
        // Divider line above macros
        val divPaint = Paint().apply {
            color = GOLD
            alpha = 80
            strokeWidth = 2f
        }
        canvas.drawLine(120f, rowY - 110f, format.width - 120f, rowY - 110f, divPaint)
    }

    private fun drawFooter(canvas: Canvas, format: Format, url: String) {
        val cx = format.width / 2f
        val y = format.height - 60f
        val footerPaint = textPaint(32f, PEARL_DIM, bold = false)
        canvas.drawText(url, cx, y, footerPaint)
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): Paint = Paint().apply {
        this.color = color
        textSize = size
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                   else Typeface.SANS_SERIF
    }

    private fun wrapAndDrawText(
        canvas: Canvas,
        text: String,
        cx: Float,
        startY: Float,
        maxWidth: Int,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int = 4
    ) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        val bounds = Rect()
        for (w in words) {
            val test = if (current.isEmpty()) w else "$current $w"
            paint.getTextBounds(test, 0, test.length, bounds)
            if (bounds.width() > maxWidth && current.isNotEmpty()) {
                lines += current
                current = w
                if (lines.size >= maxLines) break
            } else {
                current = test
            }
        }
        if (current.isNotEmpty() && lines.size < maxLines) lines += current
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, cx, startY + i * lineHeight, paint)
        }
    }
}
