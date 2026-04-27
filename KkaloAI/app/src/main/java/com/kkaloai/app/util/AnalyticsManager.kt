package com.kkaloai.app.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logFoodRecognized(foodName: String, calories: Int, proteins: Float, carbs: Float, fats: Float) {
        val bundle = Bundle().apply {
            putString("food_name", foodName)
            putInt("calories", calories)
            putDouble("proteins", proteins.toDouble())
            putDouble("carbs", carbs.toDouble())
            putDouble("fats", fats.toDouble())
        }
        firebaseAnalytics.logEvent("food_recognized", bundle)
        FileLogger.d("Analytics", "Event: food_recognized ($foodName, $calories kcal)")
    }

    fun logMealSaved(totalCalories: Int, itemCount: Int) {
        val bundle = Bundle().apply {
            putInt("total_calories", totalCalories)
            putInt("item_count", itemCount)
        }
        firebaseAnalytics.logEvent("meal_saved", bundle)
        FileLogger.d("Analytics", "Event: meal_saved ($totalCalories kcal, $itemCount items)")
    }

    fun logScannerError(errorType: String, message: String) {
        val bundle = Bundle().apply {
            putString("error_type", errorType)
            putString("message", message)
        }
        firebaseAnalytics.logEvent("scanner_error", bundle)
        FileLogger.e("Analytics", "Event: scanner_error ($errorType: $message)")
    }
    
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
