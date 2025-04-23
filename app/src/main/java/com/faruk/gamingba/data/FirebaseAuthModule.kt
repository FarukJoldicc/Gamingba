package com.faruk.gamingba.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseAuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        // Disable automatic error handling
        val settings = FirebaseAuthSettings.Builder()
            .setAutoRetrievedSmsCodeForPhoneNumber(null)
            .build()
        auth.firebaseAuthSettings = settings
        return auth
    }
} 