package com.faruk.gamingba

import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import android.app.Application
import android.util.Log
import com.facebook.LoggingBehavior
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GamingBaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable Facebook SDK Logs
        FacebookSdk.setIsDebugEnabled(true)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.DEVELOPER_ERRORS)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.GRAPH_API_DEBUG_INFO)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
        
        try {
            // Initialize Facebook SDK
            FacebookSdk.setApplicationId(getString(R.string.facebook_app_id))
            FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
            FacebookSdk.sdkInitialize(applicationContext)
            AppEventsLogger.activateApp(this)
            Log.d("GamingBaApplication", "Facebook SDK initialized with App ID: ${getString(R.string.facebook_app_id)}")
        } catch (e: Exception) {
            Log.e("GamingBaApplication", "Error initializing Facebook SDK", e)
        }
    }
}
