package com.faruk.gamingba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faruk.gamingba.model.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Holds the result of login or register operation (success/failure)
    private val _authResult = MutableStateFlow<Result<Unit>?>(null)
    val authResult: StateFlow<Result<Unit>?> = _authResult

    // Holds the current logged-in Firebase user
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    // Email and password bound to the UI
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")

    // Used to show/hide loading state in the UI
    val isLoading = MutableStateFlow(false)

    init {
        _currentUser.value = repository.getCurrentUser()
    }

    // Called when user taps the login button
    fun onLoginClicked() {
        if (!isInputValid()) return
        login(email.value, password.value)
    }

    // Called when user taps the register button
    fun onRegisterClicked() {
        if (!isInputValid()) return
        register(email.value, password.value)
    }

    // Perform login and update auth result and user
    private fun login(email: String, password: String) {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.login(email, password)
            _authResult.value = result
            if (result.isSuccess) {
                _currentUser.value = repository.getCurrentUser()
            }
            isLoading.value = false
        }
    }

    // Perform registration and update auth result and user
    private fun register(email: String, password: String) {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.register(email, password)
            _authResult.value = result
            if (result.isSuccess) {
                _currentUser.value = repository.getCurrentUser()
            }
            isLoading.value = false
        }
    }

    // Clear the current auth result after it's been handled
    fun clearAuthResult() {
        _authResult.value = null
    }

    // Simple input validation (optional improvement)
    fun isInputValid(): Boolean {
        return email.value.isNotBlank() && password.value.length >= 6
    }

    // Logout the current user
    fun logout() {
        repository.logout()
        _currentUser.value = null
    }
}
