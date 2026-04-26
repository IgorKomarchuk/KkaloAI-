package com.kkaloai.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object SharingUtils {

    private const val APP_LINK = "https://kkaloai.com"
    private const val HASHTAGS = "#KkaloAI #CalorieTracker #AI #FoodScanner #HealthyEating"

    fun shareToSocial(context: Context, mealName: String, calories: Int) {
        val shareText = """
            🥗 KkaloAI scanned my meal: $mealName
            🔥 Result: $calories kcal

            This AI scanner is magic! Get it here: $APP_LINK
            $HASHTAGS
        """.trimIndent()

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share your food scan"))
    }

    fun shareLinkOnly(context: Context) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Checking out this AI Calorie Scanner! $APP_LINK")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    /** Share a PNG image (meal/streak/insight card) — optimal for TikTok/Reels/Shorts. */
    fun shareImage(context: Context, imageUri: Uri, captionText: String? = null) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            captionText?.let { putExtra(Intent.EXTRA_TEXT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share to…"))
    }

    fun shareMealCard(
        context: Context,
        mealName: String,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatsG: Int,
        format: ShareCardRenderer.Format = ShareCardRenderer.Format.SQUARE
    ) {
        val bmp = ShareCardRenderer.renderMealCard(format, mealName, calories, proteinG, carbsG, fatsG)
        val uri = ShareCardRenderer.saveAndGetUri(context, bmp, "meal-card")
        val caption = "Just scanned $mealName — $calories kcal with KkaloAI 🥗\n$APP_LINK\n$HASHTAGS"
        shareImage(context, uri, caption)
    }

    fun shareStreakCard(
        context: Context,
        streakDays: Int,
        format: ShareCardRenderer.Format = ShareCardRenderer.Format.STORY
    ) {
        val bmp = ShareCardRenderer.renderStreakCard(format, streakDays)
        val uri = ShareCardRenderer.saveAndGetUri(context, bmp, "streak-card")
        val caption = "On a $streakDays-day streak with KkaloAI 🔥\n$APP_LINK\n$HASHTAGS"
        shareImage(context, uri, caption)
    }

    fun shareWeeklyInsight(
        context: Context,
        insight: String,
        avgKcal: Int,
        format: ShareCardRenderer.Format = ShareCardRenderer.Format.STORY
    ) {
        val bmp = ShareCardRenderer.renderWeeklyInsightCard(format, insight, avgKcal)
        val uri = ShareCardRenderer.saveAndGetUri(context, bmp, "weekly-card")
        val caption = "My week with KkaloAI 📊\n$APP_LINK\n$HASHTAGS"
        shareImage(context, uri, caption)
    }
}
