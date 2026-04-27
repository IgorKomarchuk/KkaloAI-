package com.kkaloai.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "kkaloai_prefs")

enum class HealthGoal { LOSS, MAINTAIN, BULK }
enum class Sex { MALE, FEMALE }
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9)
}

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val calorieGoalKey = intPreferencesKey("calorie_goal")
    private val isGlp1ModeKey = booleanPreferencesKey("is_glp1_mode")
    private val currentStreakKey = intPreferencesKey("current_streak")
    private val lastLoggedDayKey = longPreferencesKey("last_logged_day")
    private val waterGoalKey = intPreferencesKey("water_goal_ml")
    private val useImperialKey = booleanPreferencesKey("use_imperial")
    private val latestWeeklyReportKey = stringPreferencesKey("latest_weekly_report")
    private val healthGoalKey = stringPreferencesKey("health_goal")
    private val isHormonalModeKey = booleanPreferencesKey("is_hormonal_mode")
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")

    // Personalized profile (from onboarding)
    private val ageKey = intPreferencesKey("age")
    private val sexKey = stringPreferencesKey("sex")
    private val heightCmKey = intPreferencesKey("height_cm")
    private val weightKgKey = floatPreferencesKey("weight_kg")
    private val activityLevelKey = stringPreferencesKey("activity_level")
    private val weeklyRateKgKey = floatPreferencesKey("weekly_rate_kg")

    // Reminders — one toggle per type, plus hour/minute for meal reminders
    private val reminderBreakfastEnabledKey = booleanPreferencesKey("reminder_breakfast")
    private val reminderLunchEnabledKey = booleanPreferencesKey("reminder_lunch")
    private val reminderDinnerEnabledKey = booleanPreferencesKey("reminder_dinner")
    private val reminderWaterEnabledKey = booleanPreferencesKey("reminder_water")
    private val reminderStreakEnabledKey = booleanPreferencesKey("reminder_streak")
    private val reminderCheckinEnabledKey = booleanPreferencesKey("reminder_checkin")
    private val breakfastHourKey = intPreferencesKey("breakfast_hour")
    private val lunchHourKey = intPreferencesKey("lunch_hour")
    private val dinnerHourKey = intPreferencesKey("dinner_hour")

    // Referral
    private val myReferralCodeKey = stringPreferencesKey("my_referral_code")
    private val referredByKey = stringPreferencesKey("referred_by")
    private val referralCountKey = intPreferencesKey("referral_count")

    val calorieGoal: Flow<Int> = context.dataStore.data.map { it[calorieGoalKey] ?: DEFAULT_CALORIE_GOAL }
    val isGlp1Mode: Flow<Boolean> = context.dataStore.data.map { it[isGlp1ModeKey] ?: false }
    val currentStreak: Flow<Int> = context.dataStore.data.map { it[currentStreakKey] ?: 0 }
    val waterGoalMl: Flow<Int> = context.dataStore.data.map { it[waterGoalKey] ?: DEFAULT_WATER_GOAL_ML }
    val useImperial: Flow<Boolean> = context.dataStore.data.map { it[useImperialKey] ?: false }
    val isHormonalMode: Flow<Boolean> = context.dataStore.data.map { it[isHormonalModeKey] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[onboardingCompleteKey] ?: false }
    val latestWeeklyReportJson: Flow<String?> = context.dataStore.data.map { it[latestWeeklyReportKey] }

    val healthGoal: Flow<HealthGoal> = context.dataStore.data.map { prefs ->
        runCatching { HealthGoal.valueOf(prefs[healthGoalKey] ?: HealthGoal.LOSS.name) }
            .getOrDefault(HealthGoal.LOSS)
    }

    val reminderBreakfast: Flow<Boolean> = context.dataStore.data.map { it[reminderBreakfastEnabledKey] ?: true }
    val reminderLunch: Flow<Boolean> = context.dataStore.data.map { it[reminderLunchEnabledKey] ?: true }
    val reminderDinner: Flow<Boolean> = context.dataStore.data.map { it[reminderDinnerEnabledKey] ?: true }
    val reminderWater: Flow<Boolean> = context.dataStore.data.map { it[reminderWaterEnabledKey] ?: false }
    val reminderStreak: Flow<Boolean> = context.dataStore.data.map { it[reminderStreakEnabledKey] ?: true }
    val reminderCheckin: Flow<Boolean> = context.dataStore.data.map { it[reminderCheckinEnabledKey] ?: false }
    val breakfastHour: Flow<Int> = context.dataStore.data.map { it[breakfastHourKey] ?: 9 }
    val lunchHour: Flow<Int> = context.dataStore.data.map { it[lunchHourKey] ?: 13 }
    val dinnerHour: Flow<Int> = context.dataStore.data.map { it[dinnerHourKey] ?: 19 }

    val myReferralCode: Flow<String?> = context.dataStore.data.map { it[myReferralCodeKey] }
    val referredBy: Flow<String?> = context.dataStore.data.map { it[referredByKey] }
    val referralCount: Flow<Int> = context.dataStore.data.map { it[referralCountKey] ?: 0 }

    val age: Flow<Int?> = context.dataStore.data.map { it[ageKey] }
    val sex: Flow<Sex?> = context.dataStore.data.map { prefs ->
        prefs[sexKey]?.let { runCatching { Sex.valueOf(it) }.getOrNull() }
    }
    val heightCm: Flow<Int?> = context.dataStore.data.map { it[heightCmKey] }
    val weightKg: Flow<Float?> = context.dataStore.data.map { it[weightKgKey] }
    val activityLevel: Flow<ActivityLevel> = context.dataStore.data.map { prefs ->
        runCatching { ActivityLevel.valueOf(prefs[activityLevelKey] ?: ActivityLevel.LIGHT.name) }
            .getOrDefault(ActivityLevel.LIGHT)
    }
    val weeklyRateKg: Flow<Float> = context.dataStore.data.map { it[weeklyRateKgKey] ?: 0.5f }

    suspend fun setCalorieGoal(value: Int) = context.dataStore.edit { it[calorieGoalKey] = value }
    suspend fun setGlp1Mode(enabled: Boolean) = context.dataStore.edit { it[isGlp1ModeKey] = enabled }
    suspend fun setHealthGoal(goal: HealthGoal) = context.dataStore.edit { it[healthGoalKey] = goal.name }
    suspend fun setHormonalMode(enabled: Boolean) = context.dataStore.edit { it[isHormonalModeKey] = enabled }
    suspend fun setOnboardingComplete(done: Boolean) = context.dataStore.edit { it[onboardingCompleteKey] = done }
    suspend fun setWaterGoal(ml: Int) = context.dataStore.edit { it[waterGoalKey] = ml }
    suspend fun setUseImperial(enabled: Boolean) = context.dataStore.edit { it[useImperialKey] = enabled }
    suspend fun setLatestWeeklyReport(json: String) = context.dataStore.edit { it[latestWeeklyReportKey] = json }

    suspend fun setProfile(
        age: Int,
        sex: Sex,
        heightCm: Int,
        weightKg: Float,
        activity: ActivityLevel,
        weeklyRate: Float
    ) = context.dataStore.edit {
        it[ageKey] = age
        it[sexKey] = sex.name
        it[heightCmKey] = heightCm
        it[weightKgKey] = weightKg
        it[activityLevelKey] = activity.name
        it[weeklyRateKgKey] = weeklyRate
    }

    suspend fun setReminder(type: String, enabled: Boolean) = context.dataStore.edit {
        when (type) {
            "breakfast" -> it[reminderBreakfastEnabledKey] = enabled
            "lunch" -> it[reminderLunchEnabledKey] = enabled
            "dinner" -> it[reminderDinnerEnabledKey] = enabled
            "water" -> it[reminderWaterEnabledKey] = enabled
            "streak" -> it[reminderStreakEnabledKey] = enabled
            "checkin" -> it[reminderCheckinEnabledKey] = enabled
        }
    }

    suspend fun setMealHour(type: String, hour: Int) = context.dataStore.edit {
        when (type) {
            "breakfast" -> it[breakfastHourKey] = hour
            "lunch" -> it[lunchHourKey] = hour
            "dinner" -> it[dinnerHourKey] = hour
        }
    }

    suspend fun setReferralCode(code: String) = context.dataStore.edit { it[myReferralCodeKey] = code }
    suspend fun setReferredBy(code: String) = context.dataStore.edit { it[referredByKey] = code }
    suspend fun incrementReferralCount() = context.dataStore.edit {
        it[referralCountKey] = (it[referralCountKey] ?: 0) + 1
    }

    suspend fun updateStreak() {
        context.dataStore.edit { prefs ->
            val now = Calendar.getInstance()
            val today = now.get(Calendar.DAY_OF_YEAR).toLong()
            val lastDay = prefs[lastLoggedDayKey] ?: 0L
            if (today == lastDay) return@edit
            val currentStreak = prefs[currentStreakKey] ?: 0
            if (today == lastDay + 1) prefs[currentStreakKey] = currentStreak + 1
            else prefs[currentStreakKey] = 1
            prefs[lastLoggedDayKey] = today
        }
    }

    companion object {
        const val DEFAULT_CALORIE_GOAL = 2000
        const val DEFAULT_WATER_GOAL_ML = 2000
    }
}
