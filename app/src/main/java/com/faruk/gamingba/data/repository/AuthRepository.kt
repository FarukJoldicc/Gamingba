package com.faruk.gamingba.data.repository

import com.faruk.gamingba.model.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    suspend fun register(email: String, password: String, firstName: String, lastName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(
                firstName = firstName,
                email = email,
                password = password
            )
            database.reference.child("users").child(result.user?.uid ?: "").setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val snapshot = database.reference.child("users").child(result.user?.uid ?: "").get().await()
            val user = snapshot.getValue(User::class.java) ?: throw Exception("User data not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val snapshot = database.reference.child("users").child(userId).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logout() {
        auth.signOut()
    }
} 