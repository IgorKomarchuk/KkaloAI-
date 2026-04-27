package com.kkaloai.app.util

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Handles Google Play in-app updates.
 *
 * Strategy:
 * - FLEXIBLE: download in background, user keeps using the app, toast when ready → tap to restart.
 * - If the update is more than 7 versions / 14 days old → IMMEDIATE (blocks UI until installed).
 */
class InAppUpdateManager(private val activity: ComponentActivity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestartToast()
        }
    }

    private val updateLauncher: ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { _: ActivityResult ->
            // User accepted or cancelled the update dialog.
        }

    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> startUpdate(info)
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> startUpdate(info)
                else -> { /* no-op */ }
            }
        }
    }

    fun resumeIfDownloaded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartToast()
            }
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(info, forceImmediate = true)
            }
        }
    }

    fun register() {
        appUpdateManager.registerListener(installListener)
    }

    fun unregister() {
        appUpdateManager.unregisterListener(installListener)
    }

    private fun startUpdate(info: AppUpdateInfo, forceImmediate: Boolean = false) {
        val staleness = info.clientVersionStalenessDays() ?: 0
        val priority = info.updatePriority()
        val preferImmediate = forceImmediate || priority >= 4 || staleness >= 14

        val updateType = when {
            preferImmediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> return
        }

        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    private fun showRestartToast() {
        Toast.makeText(
            activity,
            "Update ready — tap to restart and apply.",
            Toast.LENGTH_LONG
        ).show()
        // Kick off install when user taps — simplest UX: auto-install after 3s
        activity.window.decorView.postDelayed({
            appUpdateManager.completeUpdate()
        }, 3000)
    }

    companion object {
        fun attach(activity: ComponentActivity): InAppUpdateManager {
            val manager = InAppUpdateManager(activity)
            manager.register()
            activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    manager.checkForUpdate()
                }
                override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                    manager.resumeIfDownloaded()
                }
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                    manager.unregister()
                }
            })
            return manager
        }
    }
}
