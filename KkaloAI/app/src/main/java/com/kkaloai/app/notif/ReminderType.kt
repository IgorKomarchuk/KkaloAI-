package com.kkaloai.app.notif

enum class ReminderType(
    val id: Int,
    val channelId: String,
    val channelName: String,
    val title: String,
    val body: String,
    val defaultHour: Int,
    val defaultMinute: Int,
    val isRepeatingHourly: Boolean = false
) {
    BREAKFAST(101, "meal_reminders", "Meal reminders", "Log your breakfast 🍳", "Snap your meal in 2 seconds", 9, 0),
    LUNCH(102, "meal_reminders", "Meal reminders", "Lunch time?", "Tap to log what you ate", 13, 0),
    DINNER(103, "meal_reminders", "Meal reminders", "Log your dinner 🍽", "Don't break your streak", 19, 0),
    WATER(104, "water_reminders", "Water reminders", "Stay hydrated 💧", "Log a glass of water", 11, 0, isRepeatingHourly = true),
    STREAK(105, "streak_reminders", "Streak reminders", "Keep your streak 🔥", "Log anything to save it", 21, 0),
    CHECKIN(106, "checkin_reminders", "Check-in reminders", "Daily check-in", "How did you feel today? (2 sec)", 21, 30)
}
