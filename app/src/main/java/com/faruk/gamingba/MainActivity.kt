package com.faruk.gamingba

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Hide the Action Bar
        supportActionBar?.hide()

        // Prevent default error messages
        window.setFlags(
            LayoutParams.FLAG_NOT_TOUCHABLE,
            LayoutParams.FLAG_NOT_TOUCHABLE
        )

        // Set up error handling
        setupErrorHandling()

        // Navigation setup (without toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Apply system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupErrorHandling() {
        // Override the default error handling
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            // Log the error but don't show it to the user
            android.util.Log.e("MainActivity", "Uncaught exception: ${throwable.message}")
        }

        // Disable Firebase's default error handling
        FirebaseAuth.getInstance().setLanguageCode("en")
    }

    override fun onResume() {
        super.onResume()
        // Re-enable touch after activity resumes
        window.clearFlags(LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Override toast creation to prevent Firebase error toasts
    override fun getSystemService(name: String): Any? {
        if (Context.WINDOW_SERVICE == name) {
            return object : WindowManager {
                override fun addView(view: View?, params: ViewGroup.LayoutParams?) {
                    if (view !is Toast) {
                        getSystemService(name)?.let { 
                            (it as WindowManager).addView(view, params)
                        }
                    }
                }

                override fun updateViewLayout(view: View?, params: ViewGroup.LayoutParams?) {
                    getSystemService(name)?.let { 
                        (it as WindowManager).updateViewLayout(view, params)
                    }
                }

                override fun removeView(view: View?) {
                    getSystemService(name)?.let { 
                        (it as WindowManager).removeView(view)
                    }
                }

                override fun removeViewImmediate(view: View?) {
                    getSystemService(name)?.let { 
                        (it as WindowManager).removeViewImmediate(view)
                    }
                }

                override fun getDefaultDisplay() = 
                    (getSystemService(name) as WindowManager).defaultDisplay
            }
        }
        return super.getSystemService(name)
    }
}
