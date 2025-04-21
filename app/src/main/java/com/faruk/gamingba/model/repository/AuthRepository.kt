package com.faruk.gamingba.model.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.faruk.gamingba.model.data.User

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase
) {

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Attempting login for email: $email")
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "Login successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, firstName: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Starting registration process")
            Log.d("AuthRepository", "Email: $email")
            Log.d("AuthRepository", "Password length: ${password.length}")
            Log.d("AuthRepository", "First Name: $firstName")
            
            Log.d("AuthRepository", "Creating user with Firebase Auth")
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "Firebase Auth registration successful")
            
            authResult.user?.let { user ->
                Log.d("AuthRepository", "User created successfully, UID: ${user.uid}")
                Log.d("AuthRepository", "Saving user data to Realtime Database")
                val newUser = User(firstName = firstName, email = email)
                database.reference.child("users").child(user.uid).setValue(newUser).await()
                Log.d("AuthRepository", "User data saved successfully to Realtime Database")
            } ?: run {
                Log.e("AuthRepository", "User is null after successful registration")
                throw Exception("User is null after successful registration")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = firebaseAuth.currentUser != null
        Log.d("AuthRepository", "Checking if user is logged in: $isLoggedIn")
        return isLoggedIn
    }

    fun logout() {
        Log.d("AuthRepository", "Logging out user")
        firebaseAuth.signOut()
    }

    fun getCurrentUser() = firebaseAuth.currentUser
}
