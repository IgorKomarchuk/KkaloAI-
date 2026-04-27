package com.kkaloai.app.util

import com.kkaloai.app.data.local.AchievementDao
import com.kkaloai.app.data.local.AchievementEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fire-and-forget unlock engine. ViewModels call [tryUnlock]; if the achievement
 * is new, it emits to [unlocks] for the UI to show a snackbar/toast.
 */
@Singleton
class AchievementEngine @Inject constructor(
    private val dao: AchievementDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _unlocks = MutableSharedFlow<AchievementDef>(extraBufferCapacity = 8)
    val unlocks: SharedFlow<AchievementDef> = _unlocks.asSharedFlow()

    fun tryUnlock(code: String) {
        scope.launch {
            val def = AchievementCatalog.get(code) ?: return@launch
            if (dao.isUnlocked(code)) return@launch
            val inserted = dao.unlock(AchievementEntry(code = code, unlockedAt = System.currentTimeMillis()))
            if (inserted != -1L) {
                _unlocks.emit(def)
            }
        }
    }

    fun onMealLogged(totalMealsCount: Int) {
        tryUnlock("first_scan")
        if (totalMealsCount >= 100) tryUnlock("hundred_meals")
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 5..8) tryUnlock("early_bird")
        if (hour >= 22 || hour < 4) tryUnlock("night_owl")
    }

    fun onStreak(days: Int) {
        if (days >= 3) tryUnlock("streak_3")
        if (days >= 7) tryUnlock("streak_7")
        if (days >= 30) tryUnlock("streak_30")
    }

    fun onWaterAdded(todayTotalMl: Int, lifetimeTotalMl: Int) {
        if (todayTotalMl >= 2000) tryUnlock("water_2l")
        if (lifetimeTotalMl >= 10_000) tryUnlock("water_10l")
    }

    fun onBarcodeScan() = tryUnlock("barcode_first")
    fun onVoiceScan() = tryUnlock("voice_first")
    fun onPlannerUsed() = tryUnlock("planner_used")
    fun onWeeklyReportRead() = tryUnlock("weekly_report")
    fun onCheckin() = tryUnlock("checkin_first")
    fun onWeightLogged() = tryUnlock("weight_logged")
    fun onShared() = tryUnlock("share_first")
    fun onInviteClaimed() = tryUnlock("invite_first")
}
