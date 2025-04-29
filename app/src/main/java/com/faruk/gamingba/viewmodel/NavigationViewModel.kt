package com.faruk.gamingba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.faruk.gamingba.R
import com.faruk.gamingba.model.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.faruk.gamingba.model.data.User
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val database: FirebaseDatabase
) : ViewModel() {

    private val _selectedTabId = MutableStateFlow(0) // Default to home tab
    val selectedTabId: StateFlow<Int> = _selectedTabId
    
    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName
    
    private var navController: NavController? = null
    
    init {
        loadUserData()
    }
    
    fun setNavController(controller: NavController) {
        navController = controller
    }
    
    fun onTabSelected(tabId: Int) {
        _selectedTabId.value = tabId
        navigateToTab(tabId)
    }
    
    private fun navigateToTab(tabId: Int) {
        navController?.let { controller ->
            val destination = when (tabId) {
                0 -> R.id.homeFragment
                1 -> R.id.searchFragment
                2 -> R.id.profileFragment
                else -> return
            }
            
            viewModelScope.launch {
                if (controller.currentDestination?.id != destination) {
                    controller.navigate(destination)
                }
            }
        }
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.getCurrentUser()
                firebaseUser?.uid?.let { uid ->
                    try {
                        val snapshot = database.reference.child("users").child(uid).get().await()
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            _userName.value = it.firstName
                        }
                    } catch (e: Exception) {
                        // Database fetch error
                        _userName.value = firebaseUser.displayName ?: "User"
                    }
                }
            } catch (e: Exception) {
                // Keep default value if error occurs
            }
        }
    }
    
    fun updateSelectedTabBasedOnDestination(destinationId: Int) {
        _selectedTabId.value = when (destinationId) {
            R.id.homeFragment -> 0
            R.id.searchFragment -> 1
            R.id.profileFragment -> 2
            else -> _selectedTabId.value // Keep current selection if unknown destination
        }
    }
} 