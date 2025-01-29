package com.unipi.mobdev.unipiplishopping.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.unipi.mobdev.unipiplishopping.data.model.LoggedInUser
import java.io.IOException

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource) {

    // in-memory cache of the loggedInUser object
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        //user = null

        initializeUser()
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        // handle login
        return try {
            val result = dataSource.login(username, password)

            if (result is Result.Success) {
                setLoggedInUser(result.data)
                // If login was successful, get the user details from Firestore database and store them in the user object

                fetchUserDetails()
            }
            result
        } catch (e: Throwable) {
            Result.Error(IOException("Error logging in", e))
        }

    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    private fun initializeUser() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            user = LoggedInUser(firebaseUser.uid, firebaseUser.email ?: "Unknown", "", "", "")
            Log.d("LoginRepository", "User initialized: ${user?.userId}")

            // Fetch user details from Firestore asynchronously
            fetchUserDetails()
        } else {
            Log.d("LoginRepository", "No user is currently signed in")
        }
    }

    private fun fetchUserDetails() {
        // Ensure user is not null before attempting to fetch details
        user?.let { loggedInUser ->
            val db = Firebase.firestore
            db.collection("users").document(loggedInUser.userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        loggedInUser.displayName = document.getString("name") ?: "Unknown"
                        loggedInUser.email = document.getString("email") ?: "Unknown"
                        loggedInUser.name = document.getString("name") ?: "Unknown"
                        loggedInUser.surname = document.getString("surname") ?: "Unknown"
                        Log.d("LoginRepository", "User details fetched: $loggedInUser")
                    } else {
                        Log.d("LoginRepository", "No such document for user")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("LoginRepository", "Error fetching user details: ${exception.message}")
                }
        }
    }
}