package com.faruk.gamingba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.faruk.gamingba.model.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.faruk.gamingba.model.data.User
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.faruk.gamingba.model.state.RegistrationState
import com.faruk.gamingba.model.state.ValidationState

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _authResult = MutableStateFlow<Result<Unit>?>(null)
    val authResult: StateFlow<Result<Unit>?> = _authResult

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _firstName = MutableStateFlow<String?>(null)
    val firstName: StateFlow<String?> = _firstName

    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val firstNameInput = MutableStateFlow("") // Input for registration

    val isLoading = MutableStateFlow(false)

    // Exposing StateFlow as LiveData for DataBinding
    val firstNameLiveData = firstNameInput.asLiveData()
    val emailLiveData = email.asLiveData()
    val passwordLiveData = password.asLiveData()

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState

    private val _validationState = MutableLiveData<ValidationState>()
    val validationState: LiveData<ValidationState> = _validationState

    // Navigation events
    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private val _navigateToRegister = MutableLiveData<Boolean>()
    val navigateToRegister: LiveData<Boolean> = _navigateToRegister

    // Error messages for each field
    private val _firstNameError = MutableLiveData<String?>()
    val firstNameError: LiveData<String?> = _firstNameError

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    // Track field interaction
    private val _firstNameTouched = MutableLiveData<Boolean>()
    val firstNameTouched: LiveData<Boolean> = _firstNameTouched

    private val _emailTouched = MutableLiveData<Boolean>()
    val emailTouched: LiveData<Boolean> = _emailTouched

    private val _passwordTouched = MutableLiveData<Boolean>()
    val passwordTouched: LiveData<Boolean> = _passwordTouched

    private val _formSubmitted = MutableLiveData<Boolean>()
    val formSubmitted: LiveData<Boolean> = _formSubmitted

    init {
        _currentUser.value = repository.getCurrentUser()
        fetchUserData()
    }

    private fun fetchUserData() {
        val uid = repository.getCurrentUser()?.uid ?: return
        database.reference.child("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    _firstName.value = it.firstName
                    email.value = it.email
                }
            }
            .addOnFailureListener {
                _firstName.value = null
                email.value = ""
            }
    }

    private fun validateEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun validatePassword(password: String): Boolean {
        return password.length >= 6 && 
               password.any { it.isDigit() } && 
               password.any { it.isUpperCase() } && 
               password.any { it.isLowerCase() }
    }

    private fun validateFirstName(firstName: String): Boolean {
        return firstName.isNotBlank() && firstName.length >= 2
    }

    fun validateInput(includeFirstName: Boolean = true): ValidationState {
        // Clear previous errors
        _firstNameError.value = null
        _emailError.value = null
        _passwordError.value = null
        
        val firstNameValid = !includeFirstName || validateFirstName(firstNameInput.value)
        val emailValid = validateEmail(email.value)
        val passwordValid = validatePassword(password.value)

        // Only show errors if the field has been touched or form was submitted
        if (includeFirstName && !firstNameValid && (_firstNameTouched.value == true || _formSubmitted.value == true)) {
            _firstNameError.value = "First name must be at least 2 characters long"
        }
        
        if (!emailValid && (_emailTouched.value == true || _formSubmitted.value == true)) {
            _emailError.value = "Please enter a valid email address"
        }
        
        if (!passwordValid && (_passwordTouched.value == true || _formSubmitted.value == true)) {
            _passwordError.value = when {
                password.value.isEmpty() -> "Password is required"
                password.value.length < 6 -> "Password must be at least 6 characters long"
                !password.value.any { it.isDigit() } -> "Password must contain at least one number"
                !password.value.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
                !password.value.any { it.isLowerCase() } -> "Password must contain at least one lowercase letter"
                else -> "Password must meet all requirements"
            }
        }

        val errors = mutableListOf<String>()
        if (!emailValid && (_emailTouched.value == true || _formSubmitted.value == true)) {
            errors.add("Please enter a valid email address")
        }
        if (!passwordValid && (_passwordTouched.value == true || _formSubmitted.value == true)) {
            errors.add("Password must meet all requirements")
        }
        if (includeFirstName && !firstNameValid && (_firstNameTouched.value == true || _formSubmitted.value == true)) {
            errors.add("First name must be at least 2 characters long")
        }

        val state = ValidationState(
            isValid = emailValid && passwordValid && firstNameValid,
            errors = errors
        )
        _validationState.value = state
        return state
    }

    fun onLoginClicked() {
        if (!validateInput(includeFirstName = false).isValid) {
            _error.value = _validationState.value?.errors?.firstOrNull() ?: "Invalid input"
            return
        }
        login(email.value, password.value)
    }

    fun onRegisterClicked() {
        _formSubmitted.value = true
        if (!validateInput().isValid) {
            _error.value = _validationState.value?.errors?.firstOrNull() ?: "Invalid input"
            return
        }
        register(email.value, password.value, firstNameInput.value)
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting login process")
            isLoading.value = true
            _error.value = null
            try {
                val result = repository.login(email, password)
                _authResult.value = result
                if (result.isSuccess) {
                    Log.d("AuthViewModel", "Login successful")
                    _currentUser.value = repository.getCurrentUser()
                    fetchUserData()
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                    Log.e("AuthViewModel", "Login failed: $errorMessage")
                    _error.value = when {
                        errorMessage.contains("password") -> "Invalid password"
                        errorMessage.contains("email") -> "Invalid email address"
                        errorMessage.contains("user") -> "No account found with this email"
                        else -> "Login failed. Please try again"
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login exception: ${e.message}")
                _error.value = "An unexpected error occurred. Please try again"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String, firstName: String) {
        Log.d("AuthViewModel", "Starting registration process")
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            _error.value = null
            try {
                val result = repository.register(email, password, firstName)
                result.onSuccess {
                    Log.d("AuthViewModel", "Registration successful")
                    _registrationState.value = RegistrationState.Success
                    // Navigate to login after successful registration
                    _navigateToLogin.value = true
                }.onFailure {
                    Log.e("AuthViewModel", "Registration failed: ${it.message}")
                    val errorMessage = when {
                        it.message?.contains("email") == true -> "This email is already registered"
                        it.message?.contains("password") == true -> "Password is too weak"
                        it.message?.contains("network") == true -> "Network error. Please check your connection"
                        else -> "Registration failed. Please try again"
                    }
                    _registrationState.value = RegistrationState.Error(errorMessage)
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration exception: ${e.message}")
                val errorMessage = "An unexpected error occurred. Please try again"
                _registrationState.value = RegistrationState.Error(errorMessage)
                _error.value = errorMessage
            }
        }
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _firstName.value = null
        auth.signOut()
        _userName.value = ""
        _error.value = null
        _passwordError.value = null
        _firstNameError.value = null
        _emailError.value = null
    }

    // Navigation methods
    fun navigateToLogin() {
        _navigateToLogin.value = true
    }

    fun navigateToRegister() {
        _navigateToRegister.value = true
    }

    fun onNavigationHandled() {
        _navigateToLogin.value = false
        _navigateToRegister.value = false
    }

    // Updated setters with field touch tracking
    fun setFirstName(value: Any?) {
        val text = value?.toString() ?: ""
        _firstNameTouched.value = true
        firstNameInput.value = text
        validateInput()
    }

    fun setEmail(value: Any?) {
        val text = value?.toString() ?: ""
        _emailTouched.value = true
        email.value = text
        validateInput()
    }

    fun setPassword(value: Any?) {
        val text = value?.toString() ?: ""
        _passwordTouched.value = true
        password.value = text
        validateInput()
    }
}
