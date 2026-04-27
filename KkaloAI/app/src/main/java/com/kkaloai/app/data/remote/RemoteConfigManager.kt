package com.kkaloai.app.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.kkaloai.app.util.AnalyticsManager
import com.kkaloai.app.util.FileLogger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight wrapper for Firebase Remote Config — used for paywall A/B,
 * onboarding-skip-paywall toggle, trial length, default plan id.
 *
 * Defaults are baked in so the app behaves correctly before the first fetch.
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    private val analyticsManager: AnalyticsManager
) {
    private val rc: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
            setDefaultsAsync(
                mapOf(
                    KEY_PAYWALL_VARIANT to "A",
                    KEY_TRIAL_DAYS to 7L,
                    KEY_DEFAULT_PLAN to "annual",
                    KEY_ONBOARDING_SKIP_PAYWALL to false
                )
            )
        }
    }

    suspend fun init() {
        runCatching { rc.fetchAndActivate().await() }
            .onSuccess {
                FileLogger.d("RemoteConfig", "Fetched. variant=${paywallVariant()} plan=${defaultPlan()} trial=${trialDays()}")
                analyticsManager.setUserProperty("paywall_variant", paywallVariant())
                analyticsManager.setUserProperty("default_plan", defaultPlan())
                analyticsManager.setUserProperty("trial_days", trialDays().toString())
            }
            .onFailure { FileLogger.e("RemoteConfig", "fetch failed: ${it.message}", it) }
    }

    fun paywallVariant(): String = rc.getString(KEY_PAYWALL_VARIANT).ifEmpty { "A" }
    fun trialDays(): Int = rc.getLong(KEY_TRIAL_DAYS).toInt().coerceAtLeast(0)
    fun defaultPlan(): String = rc.getString(KEY_DEFAULT_PLAN).ifEmpty { "annual" }
    fun onboardingSkipsPaywall(): Boolean = rc.getBoolean(KEY_ONBOARDING_SKIP_PAYWALL)

    companion object {
        const val KEY_PAYWALL_VARIANT = "paywall_variant"
        const val KEY_TRIAL_DAYS = "trial_days"
        const val KEY_DEFAULT_PLAN = "default_plan"
        const val KEY_ONBOARDING_SKIP_PAYWALL = "onboarding_skip_paywall"
    }
}
