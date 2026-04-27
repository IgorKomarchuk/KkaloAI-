package com.kkaloai.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kkaloai.app.data.auth.AuthManager
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.util.FileLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Referral attribution layer.
 *
 * Code layout:
 *   referralCodes/{CODE}         →  { uid: "abc" }
 *   users/{uid}                  →  { referralCode, referredBy, referralCount, rewardEarned }
 *
 * Flow:
 *  1. On first Dashboard open we call ensureCode() — generates XXXXXX code for current user.
 *  2. When another user arrives via /invite/{CODE} deep link → we call claimInvite(code).
 *  3. claimInvite stores referredBy locally + bumps referrer's user doc referralCount + logs event.
 *  4. Real promo-code delivery (free month) happens outside the app via Play Developer API — this layer
 *     exposes rewardEarned so the Invite UI can show "You've earned N months".
 */
@Singleton
class ReferralRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager,
    private val userPreferences: UserPreferences,
    private val achievementEngine: com.kkaloai.app.util.AchievementEngine
) {
    suspend fun ensureCode(): String {
        userPreferences.myReferralCode.first()?.let { return it }
        val uid = authManager.getUserId() ?: return generateLocalFallback()
        val newCode = generateUniqueCode()
        runCatching {
            firestore.collection("referralCodes").document(newCode)
                .set(mapOf("uid" to uid))
                .await()
            firestore.collection("users").document(uid)
                .set(mapOf("referralCode" to newCode), SetOptions.merge())
                .await()
        }.onFailure { FileLogger.e("ReferralRepo", "ensureCode sync failed: ${it.message}", it) }
        userPreferences.setReferralCode(newCode)
        return newCode
    }

    private suspend fun generateLocalFallback(): String {
        val code = generateUniqueCode()
        userPreferences.setReferralCode(code)
        return code
    }

    /**
     * Called when someone opens the app via an invite deep link.
     * Self-claiming is silently rejected server-side through ownership check.
     */
    suspend fun claimInvite(code: String) {
        val normalized = code.uppercase().trim()
        if (normalized.isBlank()) return
        // Already claimed? Skip.
        if (userPreferences.referredBy.first() != null) return

        val currentUid = authManager.getUserId() ?: return
        val codeDoc = runCatching {
            firestore.collection("referralCodes").document(normalized).get().await()
        }.getOrNull()
        val inviterUid = codeDoc?.getString("uid")
        if (inviterUid == null || inviterUid == currentUid) {
            FileLogger.d("ReferralRepo", "Invalid or self-referral code: $normalized")
            return
        }

        userPreferences.setReferredBy(normalized)

        runCatching {
            firestore.collection("users").document(inviterUid)
                .set(
                    mapOf(
                        "referralCount" to com.google.firebase.firestore.FieldValue.increment(1)
                    ),
                    SetOptions.merge()
                )
                .await()
            firestore.collection("users").document(currentUid)
                .set(mapOf("referredBy" to normalized), SetOptions.merge())
                .await()
        }.onFailure { FileLogger.e("ReferralRepo", "claimInvite sync failed: ${it.message}", it) }

        achievementEngine.onInviteClaimed()
        FileLogger.d("ReferralRepo", "Claimed invite $normalized from uid $inviterUid")
    }

    private fun generateUniqueCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no ambiguous 0/O, 1/I
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
