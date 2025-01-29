package com.unipi.mobdev.unipiplishopping.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.unipi.mobdev.unipiplishopping.data.model.LoggedInUser
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): Result<LoggedInUser> {
        try {
            var result: Result<LoggedInUser> = Result.Error(IOException("Unknown error"))
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()

            val user = authResult.user

            if (user != null) {
                val loggedInUser = LoggedInUser(user.uid, user.email ?: "Unknown", "", "", "")
                result = Result.Success(loggedInUser)
            } else {
                result = Result.Error(IOException("Authentication failed: User is null"))
            }
            return result

        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout(): Result<Boolean> {
        return try {
            firebaseAuth.signOut()
            Result.Success(true)
        } catch (e: Throwable) {
            Result.Error(IOException("Error logging out", e))
        }
    }
}