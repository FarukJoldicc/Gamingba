package com.faruk.gamingba.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faruk.gamingba.model.repository.AuthRepository
import com.faruk.gamingba.model.state.HomeViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.faruk.gamingba.model.data.User
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    private val database: FirebaseDatabase
) : ViewModel() {

    // We cannot inject NavigationViewModel directly, so we'll need to observe it from the fragment
    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName
    
    private val _state = MutableStateFlow(HomeViewState())
    val state: StateFlow<HomeViewState> = _state
    
    init {
        loadUserData()
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
                // Error handling
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun setUserName(name: String) {
        _userName.value = name
    }
} 