package com.kkaloai.app.util

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Average-hash (aHash) — simpler than pHash but ~95% effective for food-photo dedup.
 * 8x8 grayscale → average → 64-bit hash → hex string.
 */
object ImageHasher {

    fun aHashHex(bitmap: Bitmap): String {
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        val grays = pixels.map { p ->
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            ((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt()
        }
        val avg = grays.average()
        var bits = 0L
        grays.forEachIndexed { i, v ->
            if (v >= avg) bits = bits or (1L shl i)
        }
        return bits.toULong().toString(16).padStart(16, '0')
    }
}
