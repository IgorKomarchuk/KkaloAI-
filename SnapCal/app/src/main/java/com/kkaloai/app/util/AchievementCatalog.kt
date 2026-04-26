package com.kkaloai.app.util

import androidx.annotation.StringRes
import com.kkaloai.app.R

data class AchievementDef(
    val code: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val emoji: String
)

object AchievementCatalog {

    val ALL: List<AchievementDef> = listOf(
        AchievementDef("first_scan",       R.string.ach_first_scan_title,       R.string.ach_first_scan_desc,       "📸"),
        AchievementDef("streak_3",         R.string.ach_streak_3_title,         R.string.ach_streak_3_desc,         "🔥"),
        AchievementDef("streak_7",         R.string.ach_streak_7_title,         R.string.ach_streak_7_desc,         "🔥"),
        AchievementDef("streak_30",        R.string.ach_streak_30_title,        R.string.ach_streak_30_desc,        "🏆"),
        AchievementDef("water_2l",         R.string.ach_water_2l_title,         R.string.ach_water_2l_desc,         "💧"),
        AchievementDef("water_10l",        R.string.ach_water_10l_title,        R.string.ach_water_10l_desc,        "🌊"),
        AchievementDef("barcode_first",    R.string.ach_barcode_first_title,    R.string.ach_barcode_first_desc,    "🔢"),
        AchievementDef("voice_first",      R.string.ach_voice_first_title,      R.string.ach_voice_first_desc,      "🎤"),
        AchievementDef("planner_used",     R.string.ach_planner_used_title,     R.string.ach_planner_used_desc,     "🤖"),
        AchievementDef("weekly_report",    R.string.ach_weekly_report_title,    R.string.ach_weekly_report_desc,    "📊"),
        AchievementDef("checkin_first",    R.string.ach_checkin_first_title,    R.string.ach_checkin_first_desc,    "💚"),
        AchievementDef("weight_logged",    R.string.ach_weight_logged_title,    R.string.ach_weight_logged_desc,    "⚖️"),
        AchievementDef("share_first",      R.string.ach_share_first_title,      R.string.ach_share_first_desc,      "📣"),
        AchievementDef("invite_first",     R.string.ach_invite_first_title,     R.string.ach_invite_first_desc,     "🎁"),
        AchievementDef("hundred_meals",    R.string.ach_hundred_meals_title,    R.string.ach_hundred_meals_desc,    "💯"),
        AchievementDef("early_bird",       R.string.ach_early_bird_title,       R.string.ach_early_bird_desc,       "🌅"),
        AchievementDef("night_owl",        R.string.ach_night_owl_title,        R.string.ach_night_owl_desc,        "🦉")
    )

    fun get(code: String): AchievementDef? = ALL.firstOrNull { it.code == code }
}
