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

    private val _authResult = MutableStateFlow<Result<Unit>?>(null)
    val authResult: StateFlow<Result<Unit>?> = _authResult

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    val email = MutableStateFlow("")
    val password = MutableStateFlow("")

    init {
        _currentUser.value = repository.getCurrentUser()
    }

    fun onLoginClicked() {
        login(email.value, password.value)
    }

    fun onRegisterClicked() {
        register(email.value, password.value)
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            _authResult.value = result
            if (result.isSuccess) {
                _currentUser.value = repository.getCurrentUser()
            }
        }
    }

    private fun register(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.register(email, password)
            _authResult.value = result
            if (result.isSuccess) {
                _currentUser.value = repository.getCurrentUser()
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
    }
}

