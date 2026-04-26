package com.kkaloai.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kkaloai.app.util.FileLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            FileLogger.d("AuthManager", "Attempting anonymous sign in...")
            val result = auth.signInAnonymously().await()
            val user = result.user
            if (user != null) {
                FileLogger.d("AuthManager", "Sign in successful. UID: ${user.uid}")
                _currentUser.value = user
                Result.success(user)
            } else {
                val error = Exception("User is null after sign in")
                FileLogger.e("AuthManager", "Sign in failed: User null", error)
                Result.failure(error)
            }
        } catch (e: Exception) {
            FileLogger.e("AuthManager", "Anonymous sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getUserId(): String? = auth.currentUser?.uid
}
