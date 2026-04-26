package com.kkaloai.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.kkaloai.app.MainActivity
import com.kkaloai.app.R
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.data.remote.GeminiRepository
import com.kkaloai.app.util.FileLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class WeeklyReportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val mealDao: MealDao,
    private val geminiRepository: GeminiRepository,
    private val userPreferences: UserPreferences,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        FileLogger.d("WeeklyReportWorker", "Starting weekly report generation in background")
        val now = System.currentTimeMillis()
        val sevenDaysAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis

        val meals = mealDao.getMealsBetweenSync(sevenDaysAgo, now)
        if (meals.isEmpty()) {
            FileLogger.d("WeeklyReportWorker", "No meals logged, skipping report")
            return Result.success()
        }

        val result = geminiRepository.analyzeWeeklyProgress(meals)
        
        return if (result.isSuccess) {
            val report = result.getOrNull()
            if (report != null) {
                userPreferences.setLatestWeeklyReport(gson.toJson(report))
                showNotification()
                FileLogger.d("WeeklyReportWorker", "Weekly report generated successfully")
                Result.success()
            } else {
                Result.failure()
            }
        } else {
            FileLogger.e("WeeklyReportWorker", "Failed to generate report: ${result.exceptionOrNull()?.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun showNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weekly_report_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.wr_notif_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.wr_notif_title))
            .setContentText(context.getString(R.string.wr_notif_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
