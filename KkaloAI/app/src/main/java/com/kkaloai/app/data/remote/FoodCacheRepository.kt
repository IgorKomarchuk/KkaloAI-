package com.kkaloai.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.kkaloai.app.data.model.GeminiFoodResponse
import com.kkaloai.app.util.FileLogger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuts Gemini/Groq vision cost by deduping near-identical photos via aHash.
 * Hits are bumped via FieldValue.increment for LRU pruning later (Cloud Function).
 */
@Singleton
class FoodCacheRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {
    suspend fun lookup(hash: String): GeminiFoodResponse? = runCatching {
        val doc = firestore.collection("foodCache").document(hash).get().await()
        if (!doc.exists()) return@runCatching null
        val json = doc.getString("payloadJson") ?: return@runCatching null
        firestore.collection("foodCache").document(hash)
            .set(mapOf("hits" to FieldValue.increment(1), "lastUsed" to System.currentTimeMillis()), SetOptions.merge())
        gson.fromJson(json, GeminiFoodResponse::class.java)
    }.getOrElse {
        FileLogger.d("FoodCacheRepo", "lookup miss/error for $hash: ${it.message}")
        null
    }

    suspend fun save(hash: String, response: GeminiFoodResponse) {
        if (response.items.any { it.confidence < 0.7f }) return // skip low-quality
        runCatching {
            firestore.collection("foodCache").document(hash).set(
                mapOf(
                    "payloadJson" to gson.toJson(response),
                    "hits" to 1,
                    "lastUsed" to System.currentTimeMillis(),
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }.onFailure { FileLogger.e("FoodCacheRepo", "save failed: ${it.message}", it) }
    }

    suspend fun lookupBarcode(barcode: String): GeminiFoodResponse? =
        lookup("bc_$barcode")

    suspend fun saveBarcode(barcode: String, response: GeminiFoodResponse) =
        save("bc_$barcode", response)
}
