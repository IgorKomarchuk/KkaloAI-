package com.kkaloai.app.util

import kotlin.math.roundToInt

object MassFormatter {
    private const val GRAMS_PER_OUNCE = 28.3495f

    fun formatGrams(grams: Float, useImperial: Boolean): String = if (useImperial) {
        val oz = grams / GRAMS_PER_OUNCE
        "${"%.1f".format(oz)} oz"
    } else {
        "${grams.roundToInt()}g"
    }
}
