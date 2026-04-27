package com.kkaloai.app.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kkaloai.app.util.FileLogger

object PlannedMealNotifier {

    const val EXTRA_MEAL_ID = "meal_id"
    const val EXTRA_MEAL_NAME = "meal_name"
    private const val PI_BASE = 1_000_000

    /** Schedule a "Did you eat X?" reminder 30 min after the planned timestamp. */
    fun schedule(context: Context, mealId: Int, mealName: String, plannedTimestampMs: Long) {
        val triggerAt = plannedTimestampMs + 30 * 60 * 1000L
        if (triggerAt <= System.currentTimeMillis()) {
            FileLogger.d("PlannedMealNotifier", "Skip $mealId — trigger in past")
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, mealId, mealName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        FileLogger.d("PlannedMealNotifier", "Scheduled meal $mealId at $triggerAt")
    }

    fun cancel(context: Context, mealId: Int, mealName: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, mealId, mealName))
    }

    private fun pendingIntent(context: Context, mealId: Int, mealName: String): PendingIntent {
        val intent = Intent(context, PlannedMealReceiver::class.java).apply {
            action = "com.kkaloai.app.PLANNED_MEAL_$mealId"
            putExtra(EXTRA_MEAL_ID, mealId)
            putExtra(EXTRA_MEAL_NAME, mealName)
        }
        return PendingIntent.getBroadcast(
            context,
            PI_BASE + mealId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
