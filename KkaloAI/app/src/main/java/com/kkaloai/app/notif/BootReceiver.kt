package com.kkaloai.app.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kkaloai.app.data.local.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.MY_PACKAGE_REPLACED" &&
            intent.action != Intent.ACTION_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (userPreferences.reminderBreakfast.first()) {
                    ReminderScheduler.scheduleDaily(context, ReminderType.BREAKFAST, userPreferences.breakfastHour.first())
                }
                if (userPreferences.reminderLunch.first()) {
                    ReminderScheduler.scheduleDaily(context, ReminderType.LUNCH, userPreferences.lunchHour.first())
                }
                if (userPreferences.reminderDinner.first()) {
                    ReminderScheduler.scheduleDaily(context, ReminderType.DINNER, userPreferences.dinnerHour.first())
                }
                if (userPreferences.reminderWater.first()) {
                    ReminderScheduler.scheduleRepeatingHourly(context, ReminderType.WATER)
                }
                if (userPreferences.reminderStreak.first()) {
                    ReminderScheduler.scheduleDaily(context, ReminderType.STREAK, ReminderType.STREAK.defaultHour)
                }
                if (userPreferences.reminderCheckin.first()) {
                    ReminderScheduler.scheduleDaily(context, ReminderType.CHECKIN, ReminderType.CHECKIN.defaultHour, ReminderType.CHECKIN.defaultMinute)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
