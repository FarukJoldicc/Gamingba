package com.faruk.gamingba

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.faruk.gamingba.databinding.ActivityMainBinding
import com.faruk.gamingba.viewmodel.NavigationViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams
import android.util.Log
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.ktx.Firebase
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.ui.setupWithNavController
import android.widget.LinearLayout
import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.faruk.gamingba.view.fragment.HomeFragment

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private val navigationViewModel: NavigationViewModel by viewModels()
    
    // Tab views
    private lateinit var homeTab: LinearLayout
    private lateinit var searchTab: LinearLayout
    private lateinit var profileTab: LinearLayout
    
    // Tab icons
    private lateinit var homeIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var profileIcon: ImageView
    
    // Tab texts
    private lateinit var homeText: TextView
    private lateinit var searchText: TextView
    private lateinit var profileText: TextView
    
    // Colors
    private var selectedColor = 0
    private var unselectedColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Set up data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = navigationViewModel

        // Get colors
        selectedColor = ContextCompat.getColor(this, R.color.button_cyan)
        unselectedColor = ContextCompat.getColor(this, android.R.color.white)

        // Hide the Action Bar
        supportActionBar?.hide()

        // Get references to views
        homeIcon = binding.bottomNavigation.homeIcon
        searchIcon = binding.bottomNavigation.searchIcon
        profileIcon = binding.bottomNavigation.profileIcon
        
        homeText = binding.bottomNavigation.homeText
        searchText = binding.bottomNavigation.searchText
        profileText = binding.bottomNavigation.profileText

        // Set up error handling
        setupErrorHandling()

        // Navigation setup
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Provide NavController to ViewModel
        navigationViewModel.setNavController(navController)
        
        // Setup bottom navigation visibility and synchronization
        setupNavigation()

        // Listen for tab selection changes
        lifecycleScope.launch {
            navigationViewModel.selectedTabId.collect { tabId ->
                updateTabAppearance(tabId)
            }
        }

        // Listen for username changes to update the Home tab text
        lifecycleScope.launch {
            navigationViewModel.userName.collectLatest { name ->
                homeText.text = getString(R.string.nav_home)
                
                // Update the HomeFragment's userName if it's currently displayed
                val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager?.fragments?.firstOrNull()
                if (currentFragment is HomeFragment) {
                    currentFragment.binding.userName = name
                }
            }
        }

        // Apply system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Handle the intent that started this activity
        handleIntent(intent)
        
        Log.d(TAG, "MainActivity onCreate completed")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        
        if (Intent.ACTION_VIEW == action && data != null) {
            Log.d(TAG, "Deep link received: $data")
            
            // Handle Firebase Dynamic Links
            FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener(this) { pendingDynamicLinkData ->
                    // Get deep link from result (may be null if no link is found)
                    val deepLink = pendingDynamicLinkData?.link
                    Log.d(TAG, "Firebase Dynamic Link resolved: $deepLink")
                    
                    if (deepLink != null) {
                        handleDeepLink(deepLink)
                    } else {
                        // Try to handle the intent data directly if no Firebase dynamic link
                        handleDeepLink(data)
                    }
                }
                .addOnFailureListener(this) { e ->
                    Log.e(TAG, "getDynamicLink:onFailure", e)
                    // Try to handle the intent data directly if Firebase dynamic link fails
                    handleDeepLink(data)
                }
        }
    }

    private fun handleDeepLink(uri: Uri) {
        Log.d(TAG, "Processing deep link: $uri")
        
        try {
            // Extract all parameters for debugging
            uri.queryParameterNames.forEach { param ->
                val value = uri.getQueryParameter(param)
                Log.d(TAG, "Query parameter: $param = $value")
            }
            
            // Check if this is a password reset link or email verification link
            val mode = uri.getQueryParameter("mode")
            val oobCode = uri.getQueryParameter("oobCode")
            
            if (mode != null && oobCode != null) {
                when (mode) {
                    "resetPassword" -> {
                        Log.d(TAG, "Password reset link detected with code: $oobCode")
                        
                        // Navigate to CreateNewPasswordFragment with the oobCode
                        navigateToPasswordReset(oobCode)
                    }
                    "verifyEmail" -> {
                        Log.d(TAG, "Email verification link detected with code: $oobCode")
                        
                        // Verify the email directly using Firebase Auth
                        verifyEmail(oobCode)
                    }
                    else -> {
                        Log.d(TAG, "Unhandled mode in deep link: $mode")
                        // Default to login screen for unrecognized links
                        navigateToLogin()
                    }
                }
            } else {
                // If no recognizable parameters, go to login
                Log.d(TAG, "No recognizable parameters in link")
                navigateToLogin()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing deep link", e)
            Toast.makeText(this, "Error processing the link", Toast.LENGTH_SHORT).show()
            
            // Navigate to login as fallback
            navigateToLogin()
        }
    }

    private fun navigateToPasswordReset(oobCode: String) {
        // Check the current destination before navigating
        val currentDestId = navController.currentDestination?.id
        
        when (currentDestId) {
            R.id.loginFragment -> {
                val action = com.faruk.gamingba.view.fragment.LoginFragmentDirections
                    .actionLoginFragmentToCreateNewPasswordFragment(oobCode)
                navController.navigate(action)
            }
            null -> {
                // App is starting fresh, navigate to login then to password reset
                navController.navigate(R.id.loginFragment)
                // Wait a moment to ensure the fragment is loaded
                findViewById<View>(android.R.id.content).postDelayed({
                    val action = com.faruk.gamingba.view.fragment.LoginFragmentDirections
                        .actionLoginFragmentToCreateNewPasswordFragment(oobCode)
                    navController.navigate(action)
                }, 300)
            }
            else -> {
                // Try to navigate back to login first
                try {
                    navController.navigate(R.id.loginFragment)
                    // Then navigate to password reset
                    findViewById<View>(android.R.id.content).postDelayed({
                        val action = com.faruk.gamingba.view.fragment.LoginFragmentDirections
                            .actionLoginFragmentToCreateNewPasswordFragment(oobCode)
                        navController.navigate(action)
                    }, 300)
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error", e)
                    Toast.makeText(this, "Error opening password reset", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verifyEmail(oobCode: String) {
        // Verify the email directly using Firebase Auth
        FirebaseAuth.getInstance().applyActionCode(oobCode)
            .addOnSuccessListener {
                Log.d(TAG, "Email verification successful")
                // Try to get the email from the current user, fallback to empty string
                val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                // Navigate to VerifyEmailFragment, passing the email
                val action = com.faruk.gamingba.view.fragment.LoginFragmentDirections
                    .actionLoginFragmentToVerifyEmailFragment(email, false)
                navController.navigate(action)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Email verification failed", e)
                Toast.makeText(this, "Email verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                // Navigate to login screen anyway
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        try {
            navController.navigate(R.id.loginFragment)
        } catch (navEx: Exception) {
            Log.e(TAG, "Failed to navigate to login", navEx)
        }
    }

    private fun setupErrorHandling() {
        // Override the default error handling
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            // Log the error but don't show it to the user
            Log.e(TAG, "Uncaught exception: ${throwable.message}")
            throwable.printStackTrace()
        }

        // Disable Firebase's default error handling
        FirebaseAuth.getInstance().setLanguageCode("en")
    }

    override fun onResume() {
        super.onResume()
        // No need to clear flags since we're not setting them anymore
        // window.clearFlags(LayoutParams.FLAG_NOT_TOUCHABLE)
        Log.d(TAG, "MainActivity resumed")
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // This override might be causing issues with EditText touch handling
    // Let's use a simpler approach that doesn't interfere with system services
    override fun getSystemService(name: String): Any? {
        // Let's log but not interfere with system services
        if (Context.WINDOW_SERVICE == name) {
            Log.d(TAG, "Window service requested")
        }
        return super.getSystemService(name)
    }

    private fun updateTabAppearance(selectedTabId: Int) {
        val selectedColor = ContextCompat.getColor(this, R.color.button_cyan)
        val unselectedColor = ContextCompat.getColor(this, android.R.color.white)
        
        // Reset all tabs
        homeIcon.setColorFilter(unselectedColor)
        searchIcon.setColorFilter(unselectedColor)
        profileIcon.setColorFilter(unselectedColor)
        
        homeText.visibility = View.GONE
        searchText.visibility = View.GONE
        profileText.visibility = View.GONE
        
        // Set the selected tab
        when (selectedTabId) {
            0 -> {
                homeIcon.setColorFilter(selectedColor)
                homeText.setTextColor(selectedColor)
                homeText.visibility = View.VISIBLE
            }
            1 -> {
                searchIcon.setColorFilter(selectedColor)
                searchText.setTextColor(selectedColor)
                searchText.visibility = View.VISIBLE
            }
            2 -> {
                profileIcon.setColorFilter(selectedColor)
                profileText.setTextColor(selectedColor)
                profileText.visibility = View.VISIBLE
            }
        }
    }

    private fun setupNavigation() {
        // Control navigation visibility based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Show navigation only on main tabs (home, search, profile)
            val showNav = when (destination.id) {
                R.id.homeFragment, R.id.searchFragment, R.id.profileFragment -> true
                else -> false
            }
            
            binding.bottomNavigation.root.visibility = if (showNav) View.VISIBLE else View.GONE
            
            // Update selected tab in ViewModel based on current destination
            if (showNav) {
                navigationViewModel.updateSelectedTabBasedOnDestination(destination.id)
            }
        }
    }
}
