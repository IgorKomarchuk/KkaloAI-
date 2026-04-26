package com.kkaloai.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.util.FileLogger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun syncMeal(userId: String, meal: MealEntry): Result<Unit> {
        return try {
            FileLogger.d("FirestoreRepository", "Syncing meal ${meal.id} for user $userId")
            firestore.collection("users")
                .document(userId)
                .collection("meals")
                .document(meal.id.toString())
                .set(meal)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            FileLogger.e("FirestoreRepository", "Failed to sync meal: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getMeals(userId: String): Result<List<MealEntry>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("meals")
                .get()
                .await()
            val meals = snapshot.toObjects(MealEntry::class.java)
            Result.success(meals)
        } catch (e: Exception) {
            FileLogger.e("FirestoreRepository", "Failed to fetch meals: ${e.message}", e)
            Result.failure(e)
        }
    }
}
