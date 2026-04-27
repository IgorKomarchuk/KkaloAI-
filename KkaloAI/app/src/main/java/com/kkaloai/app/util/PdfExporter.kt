package com.kkaloai.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kkaloai.app.data.model.WeeklyMealPlanResponse
import java.io.File
import java.io.FileOutputStream

/**
 * Native (no iText) PDF export for the weekly meal plan.
 * One A4 page per day, header with brand + day total kcal.
 */
object PdfExporter {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 36f

    fun exportWeeklyPlan(context: Context, plan: WeeklyMealPlanResponse): android.net.Uri {
        val doc = PdfDocument()
        plan.days.forEachIndexed { i, day ->
            val info = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, i + 1).create()
            val page = doc.startPage(info)
            drawDay(page.canvas, day, plan.summary.takeIf { i == 0 })
            doc.finishPage(page)
        }

        val dir = File(context.cacheDir, "shared")
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "kkaloai-weekly-plan-${System.currentTimeMillis()}.pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()

        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", out
        )
    }

    private fun drawDay(canvas: Canvas, day: com.kkaloai.app.data.model.PlanDay, optionalIntro: String?) {
        val brandPaint = textPaint(22f, 0xFFD4AF37.toInt(), bold = true)
        canvas.drawText("KkaloAI — Weekly Meal Plan", MARGIN, MARGIN + 22, brandPaint)

        val dayTitle = textPaint(28f, 0xFF111111.toInt(), bold = true)
        canvas.drawText(day.dayLabel, MARGIN, MARGIN + 70, dayTitle)

        val totalPaint = textPaint(16f, 0xFF555555.toInt(), bold = false)
        canvas.drawText("${day.totalKcal} kcal total", MARGIN, MARGIN + 96, totalPaint)

        var y = MARGIN + 140f
        val rowPaint = textPaint(14f, 0xFF222222.toInt(), bold = false)
        val lblPaint = textPaint(12f, 0xFF888888.toInt(), bold = false)

        day.meals.forEach { m ->
            canvas.drawText("${m.timeOfDay.uppercase()} — ${m.name}", MARGIN, y, textPaint(15f, 0xFF111111.toInt(), bold = true))
            y += 20
            canvas.drawText(
                "${m.calories} kcal · P ${m.proteins.toInt()}g · C ${m.carbs.toInt()}g · F ${m.fats.toInt()}g",
                MARGIN, y, rowPaint
            )
            y += 28
        }

        if (optionalIntro != null) {
            canvas.drawText("Plan summary: $optionalIntro", MARGIN, A4_HEIGHT - MARGIN, lblPaint)
        }

        // Footer
        canvas.drawText("kkaloai.com", A4_WIDTH - MARGIN - 80, A4_HEIGHT - MARGIN, lblPaint)
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): Paint = Paint().apply {
        this.color = color
        textSize = size
        isAntiAlias = true
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }
}
