package com.kkaloai.app.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kkaloai.app.MainActivity
import com.kkaloai.app.R
import com.kkaloai.app.data.local.MealDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlannedMealReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mealDao: MealDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val mealId = intent.getIntExtra(PlannedMealNotifier.EXTRA_MEAL_ID, -1)
        val mealName = intent.getStringExtra(PlannedMealNotifier.EXTRA_MEAL_NAME) ?: "your meal"

        when (intent.action) {
            ACTION_CONFIRM_ATE -> {
                val pending = goAsync()
                scope.launch {
                    runCatching { mealDao.markPlannedEaten(mealId, System.currentTimeMillis()) }
                    cancelNotif(context, mealId)
                    pending.finish()
                }
            }
            ACTION_DISMISS -> {
                cancelNotif(context, mealId)
            }
            else -> showAskNotification(context, mealId, mealName)
        }
    }

    private fun showAskNotification(context: Context, mealId: Int, mealName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.pm_notif_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(ch)
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPi = PendingIntent.getActivity(
            context, mealId, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confirmPi = PendingIntent.getBroadcast(
            context, mealId + 90_000,
            actionIntent(context, ACTION_CONFIRM_ATE, mealId, mealName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPi = PendingIntent.getBroadcast(
            context, mealId + 80_000,
            actionIntent(context, ACTION_DISMISS, mealId, mealName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.pm_notif_title, mealName))
            .setContentText(context.getString(R.string.pm_notif_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.pm_action_yes), confirmPi)
            .addAction(0, context.getString(R.string.pm_action_no), dismissPi)
            .build()

        nm.notify(NOTIF_ID_BASE + mealId, notif)
    }

    private fun cancelNotif(context: Context, mealId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_BASE + mealId)
    }

    private fun actionIntent(context: Context, action: String, mealId: Int, mealName: String) =
        Intent(context, PlannedMealReceiver::class.java).apply {
            this.action = action
            putExtra(PlannedMealNotifier.EXTRA_MEAL_ID, mealId)
            putExtra(PlannedMealNotifier.EXTRA_MEAL_NAME, mealName)
        }

    companion object {
        const val CHANNEL_ID = "planned_meals_channel"
        const val ACTION_CONFIRM_ATE = "com.kkaloai.app.PM_CONFIRM"
        const val ACTION_DISMISS = "com.kkaloai.app.PM_DISMISS"
        const val NOTIF_ID_BASE = 5000
    }
}
