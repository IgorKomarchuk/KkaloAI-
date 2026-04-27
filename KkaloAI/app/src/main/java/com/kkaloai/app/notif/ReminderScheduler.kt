package com.kkaloai.app.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    fun scheduleDaily(context: Context, type: ReminderType, hour: Int, minute: Int = 0) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, type)
        cancel(context, type)

        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Fall back to inexact — still delivers roughly at the right time
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun scheduleRepeatingHourly(context: Context, type: ReminderType, startHour: Int = 11, endHour: Int = 20) {
        // For water: schedule next tick every 2 hours between startHour and endHour
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, type)
        cancel(context, type)

        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val nextHour = when {
            currentHour < startHour -> startHour
            currentHour >= endHour -> startHour // tomorrow
            else -> {
                // Next even 2-hour slot after current hour
                val offset = 2 - ((currentHour - startHour) % 2)
                (currentHour + offset).coerceAtMost(endHour)
            }
        }
        val triggerAt = Calendar.getInstance().apply {
            if (nextHour <= currentHour && currentHour >= endHour) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, nextHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun rescheduleNext(context: Context, type: ReminderType) {
        // After the reminder fires, schedule the next occurrence (daily reminders fire once).
        // For water, call scheduleRepeatingHourly; for dailies, advance by one day.
        if (type.isRepeatingHourly) {
            scheduleRepeatingHourly(context, type)
        } else {
            scheduleDaily(context, type, type.defaultHour, type.defaultMinute)
        }
    }

    fun cancel(context: Context, type: ReminderType) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, type))
    }

    private fun pendingIntent(context: Context, type: ReminderType): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.kkaloai.app.REMINDER_${type.name}"
            putExtra(ReminderReceiver.EXTRA_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            type.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
