package com.kkaloai.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kkaloai.app.data.auth.AuthManager
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.data.model.GeminiFoodResponse
import com.kkaloai.app.data.remote.ReferralRepository
import kotlinx.coroutines.flow.first
import com.kkaloai.app.ui.dashboard.DashboardScreen
import com.kkaloai.app.ui.invite.InviteScreen
import com.kkaloai.app.ui.onboarding.OnboardingScreen
import com.kkaloai.app.ui.paywall.PaywallScreen
import com.kkaloai.app.ui.scanner.BarcodeScannerScreen
import com.kkaloai.app.ui.scanner.ConfirmResultScreen
import com.kkaloai.app.ui.scanner.ScannerScreen
import com.kkaloai.app.ui.settings.PrivacyPolicyScreen
import com.kkaloai.app.ui.settings.RemindersScreen
import com.kkaloai.app.ui.settings.SettingsScreen
import com.kkaloai.app.ui.favorites.FavoritesScreen
import com.kkaloai.app.ui.weight.WeightEntryScreen
import com.kkaloai.app.ui.theme.KkaloAiTheme
import com.kkaloai.app.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

object Routes {
    const val ONBOARDING = "onboarding"
    const val PAYWALL = "paywall"
    const val DASHBOARD = "dashboard"
    const val SCANNER = "scanner"
    const val BARCODE = "barcode"
    const val CONFIRM = "confirm"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val FAVORITES = "favorites"
    const val WEIGHT = "weight"
    const val PLANNER = "planner"
    const val WEEKLY_REPORT = "weekly_report"
    const val INVITE = "invite"
    const val REMINDERS = "reminders"
    const val STATS = "stats"
    const val ACHIEVEMENTS = "achievements"
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var referralRepository: ReferralRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.d("MainActivity", "onCreate started")

        com.kkaloai.app.util.InAppUpdateManager.attach(this)

        val initialOnboardingDone = runBlocking { userPreferences.onboardingComplete.first() }
        FileLogger.d("MainActivity", "onboardingComplete=$initialOnboardingDone")

        lifecycleScope.launch {
            if (!authManager.isUserLoggedIn()) {
                authManager.signInAnonymously()
            }
            extractReferralCodeFromIntent(intent)?.let { code ->
                referralRepository.claimInvite(code)
            }
        }

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.kkaloai.app.worker.WeeklyReportWorker>(
            7, java.util.concurrent.TimeUnit.DAYS
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklyReportWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            KkaloAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var pendingResult by remember { mutableStateOf<GeminiFoodResponse?>(null) }

                    NavHost(
                        navController = navController,
                        startDestination = if (initialOnboardingDone) Routes.DASHBOARD else Routes.ONBOARDING
                    ) {
                        composable(Routes.ONBOARDING) {
                            LaunchedEffect(Unit) { FileLogger.d("Navigation", "Navigated to ONBOARDING") }
                            OnboardingScreen(
                                onFinish = {
                                    FileLogger.d("Navigation", "Finish Onboarding -> PAYWALL")
                                    navController.navigate(Routes.PAYWALL) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Routes.PAYWALL) {
                            LaunchedEffect(Unit) { FileLogger.d("Navigation", "Navigated to PAYWALL") }
                            PaywallScreen(
                                onUnlocked = {
                                    FileLogger.d("Navigation", "Unlocked -> DASHBOARD")
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.PAYWALL) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Routes.DASHBOARD) {
                            LaunchedEffect(Unit) { FileLogger.d("Navigation", "Navigated to DASHBOARD") }
                            DashboardScreen(
                                onSnapClick = {
                                    FileLogger.d("Navigation", "SnapClick -> SCANNER")
                                    navController.navigate(Routes.SCANNER)
                                },
                                onBarcodeClick = {
                                    FileLogger.d("Navigation", "BarcodeClick -> BARCODE")
                                    navController.navigate(Routes.BARCODE)
                                },
                                onFavoritesClick = {
                                    navController.navigate(Routes.FAVORITES)
                                },
                                onWeightClick = {
                                    navController.navigate(Routes.WEIGHT)
                                },
                                onSettingsClick = {
                                    FileLogger.d("Navigation", "SettingsClick -> SETTINGS")
                                    navController.navigate(Routes.SETTINGS)
                                },
                                onPlannerClick = {
                                    navController.navigate(Routes.PLANNER)
                                },
                                onStatsClick = {
                                    navController.navigate(Routes.STATS)
                                },
                                onAchievementsClick = {
                                    navController.navigate(Routes.ACHIEVEMENTS)
                                },
                                onWeeklyReportClick = {
                                    navController.navigate(Routes.WEEKLY_REPORT)
                                }
                            )
                        }

                        composable(Routes.WEEKLY_REPORT) {
                            com.kkaloai.app.ui.dashboard.WeeklyReportScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.PLANNER) {
                            com.kkaloai.app.ui.planner.PlannerScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.SCANNER) {
                            LaunchedEffect(Unit) { FileLogger.d("Navigation", "Navigated to SCANNER") }

                            val scannerViewModel: com.kkaloai.app.ui.scanner.ScannerViewModel = androidx.hilt.navigation.compose.hiltViewModel()

                            ScannerScreen(
                                viewModel = scannerViewModel,
                                onResultFound = { result ->
                                    FileLogger.d("Navigation", "Scanner result -> CONFIRM")
                                    pendingResult = result
                                    navController.navigate(Routes.CONFIRM)
                                }
                            )
                        }

                        composable(Routes.BARCODE) {
                            LaunchedEffect(Unit) { FileLogger.d("Navigation", "Navigated to BARCODE") }
                            BarcodeScannerScreen(
                                onResultFound = { result ->
                                    pendingResult = result
                                    navController.navigate(Routes.CONFIRM) {
                                        popUpTo(Routes.BARCODE) { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.CONFIRM) {
                            LaunchedEffect(Unit) { FileLogger.d("Navigation", "Navigated to CONFIRM") }
                            val result = pendingResult
                            if (result != null) {
                                ConfirmResultScreen(
                                    response = result,
                                    onConfirmed = { _ ->
                                        FileLogger.d("Navigation", "ConfirmResult -> DASHBOARD")
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                                        }
                                    },
                                    onCancel = {
                                        FileLogger.d("Navigation", "ConfirmCancel -> back")
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                FileLogger.e("Navigation", "pendingResult null in CONFIRM")
                                navController.popBackStack()
                            }
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY) },
                                onReminders = { navController.navigate(Routes.REMINDERS) },
                                onInvite = { navController.navigate(Routes.INVITE) }
                            )
                        }

                        composable(Routes.PRIVACY) {
                            PrivacyPolicyScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.FAVORITES) {
                            FavoritesScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.WEIGHT) {
                            WeightEntryScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.INVITE) {
                            InviteScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.REMINDERS) {
                            RemindersScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.STATS) {
                            com.kkaloai.app.ui.stats.StatsScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.ACHIEVEMENTS) {
                            com.kkaloai.app.ui.achievements.AchievementsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        extractReferralCodeFromIntent(intent)?.let { code ->
            lifecycleScope.launch { referralRepository.claimInvite(code) }
        }
    }

    private fun extractReferralCodeFromIntent(intent: android.content.Intent?): String? {
        val data = intent?.data ?: return null
        if (data.host != "kkaloai.com") return null
        val segs = data.pathSegments
        return if (segs.size >= 2 && segs[0] == "invite") segs[1] else null
    }
}
