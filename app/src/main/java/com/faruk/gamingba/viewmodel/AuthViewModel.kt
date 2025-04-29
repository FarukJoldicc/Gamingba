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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.faruk.gamingba.model.data.User
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.faruk.gamingba.model.state.RegistrationState
import com.faruk.gamingba.model.state.ValidationState
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.facebook.login.LoginManager
import kotlinx.coroutines.delay
import com.google.firebase.auth.ActionCodeSettings

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

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> = _navigateToHome

    // New navigation event for verify email screen
    private val _navigateToVerifyEmail = MutableLiveData<String?>()
    val navigateToVerifyEmail: LiveData<String?> = _navigateToVerifyEmail

    // Password reset navigation
    private val _navigateToResetPassword = MutableLiveData<Boolean>()
    val navigateToResetPassword: LiveData<Boolean> = _navigateToResetPassword
    
    private val _navigateToCreateNewPassword = MutableLiveData<String?>()
    val navigateToCreateNewPassword: LiveData<String?> = _navigateToCreateNewPassword

    // Error messages for each field
    private val _firstNameError = MutableLiveData<String?>()
    val firstNameError: LiveData<String?> = _firstNameError

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    // Reset password fields and errors
    val resetEmail = MutableStateFlow("")
    val resetEmailLiveData = resetEmail.asLiveData()
    
    private val _resetPasswordError = MutableStateFlow<String?>(null)
    val resetPasswordError: StateFlow<String?> = _resetPasswordError
    
    private val _resetPasswordSuccess = MutableStateFlow<String?>(null)
    val resetPasswordSuccess: StateFlow<String?> = _resetPasswordSuccess
    
    // New password fields and errors
    val newPassword = MutableStateFlow("")
    val newPasswordLiveData = newPassword.asLiveData()
    
    val confirmPassword = MutableStateFlow("")
    val confirmPasswordLiveData = confirmPassword.asLiveData()
    
    private val _newPasswordError = MutableStateFlow<String?>(null)
    val newPasswordError: StateFlow<String?> = _newPasswordError
    
    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> = _confirmPasswordError

    // Track field interaction
    private val _firstNameTouched = MutableLiveData<Boolean>()
    val firstNameTouched: LiveData<Boolean> = _firstNameTouched

    private val _emailTouched = MutableLiveData<Boolean>()
    val emailTouched: LiveData<Boolean> = _emailTouched

    private val _passwordTouched = MutableLiveData<Boolean>()
    val passwordTouched: LiveData<Boolean> = _passwordTouched

    private val _formSubmitted = MutableStateFlow(false)
    val formSubmitted: StateFlow<Boolean> = _formSubmitted

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _registrationSuccessMessage = MutableLiveData<String?>()
    val registrationSuccessMessage: LiveData<String?> = _registrationSuccessMessage

    private val _loginSuccessMessage = MutableLiveData<String?>()
    val loginSuccessMessage: LiveData<String?> = _loginSuccessMessage

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
        // Remove all whitespace (spaces, tabs, newlines) before checking length
        val cleaned = password.replace("\\s+".toRegex(), "")
        return cleaned.length >= 6
    }

    private fun validateFirstName(firstName: String): Boolean {
        // Remove all whitespace (spaces, tabs, newlines) before checking length
        val cleaned = firstName.replace("\\s+".toRegex(), "")
        return cleaned.isNotBlank() && cleaned.length >= 2
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

    fun validateForm(): Boolean {
        _formSubmitted.value = true
        var isValid = true

        // Clear previous errors
        _emailError.value = null
        _passwordError.value = null
        _loginError.value = null

        // Validate email
        if (email.value.isEmpty()) {
            _emailError.value = "Email is required"
            isValid = false
        } else if (!validateEmail(email.value)) {
            _emailError.value = "Please enter a valid email address"
            isValid = false
        }

        // Validate password
        if (password.value.isEmpty()) {
            _passwordError.value = "Password is required"
            isValid = false
        } else if (!validatePassword(password.value)) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting login process for $email")
            isLoading.value = true
            _loginError.value = null
            try {
                // Use repository.loginAndGetUser which should return Result<FirebaseUser>
                val result = repository.loginAndGetUser(email, password)

                result.onSuccess { firebaseUser ->
                    if (firebaseUser.isEmailVerified) {
                        Log.d("AuthViewModel", "Login successful and email verified for $email")
                        _currentUser.value = firebaseUser
                        _authResult.value = Result.success(Unit) // Signal success
                        fetchUserData()
                        _loginSuccessMessage.value = "Login successful! Redirecting..."
                        // Delay navigation to home so user sees the message
                        kotlinx.coroutines.delay(1000)
                        _navigateToHome.value = true // Navigate to home
                    } else {
                        // Email not verified
                        Log.d("AuthViewModel", "Login successful but email NOT verified for $email")
                        _authResult.value = Result.failure(EmailNotVerifiedException()) // Signal specific state
                        _navigateToVerifyEmail.value = email // Navigate to verify screen
                    }
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "Login failed for $email", exception)
                    _loginError.value = "You have entered an incorrect email address or password"
                    _authResult.value = Result.failure(exception) // Signal general failure
                }
            } catch (e: Exception) {
                // Catch potential exceptions from the repository call itself
                Log.e("AuthViewModel", "Login exception for $email", e)
                _loginError.value = "An error occurred during login. Please try again."
                _authResult.value = Result.failure(e) // Signal failure
            } finally {
                isLoading.value = false
            }
        }
    }

    // Custom exception for clarity
    class EmailNotVerifiedException : Exception("Email is not verified.")

    fun onLoginClicked() {
        if (email.value.isEmpty() || password.value.isEmpty()) {
            _loginError.value = "Please enter both email and password"
            return
        }
        try {
            login(email.value, password.value)
        } catch (e: Exception) {
            _loginError.value = "You have entered an incorrect email address or password"
        }
    }

    fun onRegisterClicked() {
        _formSubmitted.value = true
        if (!validateInput().isValid) {
            _error.value = _validationState.value?.errors?.firstOrNull() ?: "Invalid input"
            return
        }
        register(email.value, password.value, firstNameInput.value)
    }

    fun register(email: String, password: String, firstName: String) {
        Log.d("AuthViewModel", "Starting registration process")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("AuthViewModel", "Calling repository.registerAndGetUser")
                val result = repository.registerAndGetUser(email, password, firstName)
                Log.d("AuthViewModel", "Repository returned result: $result")

                result.onSuccess { firebaseUser ->
                    Log.d("AuthViewModel", "Registration successful, sending verification email.")
                    try {
                        // Create ActionCodeSettings for email verification with Dynamic Links
                        val actionCodeSettings = ActionCodeSettings.newBuilder()
                            .setUrl("https://gamingba-e4849.firebaseapp.com/action")
                            .setHandleCodeInApp(true)
                            .setAndroidPackageName(
                                "com.faruk.gamingba",
                                true,
                                "1"
                            )
                            .build()
                        Log.d("AuthViewModel", "Calling sendEmailVerification with ActionCodeSettings")
                        firebaseUser.sendEmailVerification(actionCodeSettings).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("AuthViewModel", "Verification email sent successfully with Dynamic Link.")
                                _registrationState.postValue(RegistrationState.Success)
                                _registrationSuccessMessage.postValue("Registration successful! Redirecting...")
                                _navigateToVerifyEmail.postValue(email)
                            } else {
                                Log.e("AuthViewModel", "Failed to send verification email with Dynamic Link.", task.exception)
                                // Try fallback to standard verification if Dynamic Link fails
                                firebaseUser.sendEmailVerification().addOnCompleteListener { fallbackTask ->
                                    if (fallbackTask.isSuccessful) {
                                        Log.d("AuthViewModel", "Standard verification email sent successfully as fallback.")
                                        _registrationState.postValue(RegistrationState.Success)
                                        _registrationSuccessMessage.postValue("Registration successful! Redirecting...")
                                        _navigateToVerifyEmail.postValue(email)
                                    } else {
                                        Log.e("AuthViewModel", "Failed to send standard verification email.", fallbackTask.exception)
                                        _registrationState.postValue(RegistrationState.Error("Registration succeeded but failed to send verification email."))
                                        _error.postValue("Couldn't send verification email. Please try logging in.")
                                        _navigateToLogin.postValue(true)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Exception while sending verification email: ", e)
                        _registrationState.postValue(RegistrationState.Error("Exception while sending verification email."))
                        _error.postValue("Exception while sending verification email: ${e.message}")
                    }
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "Registration failed: ${exception.message}", exception)
                    val errorMessage = when {
                        exception.message?.contains("email") == true -> "This email is already registered"
                        exception.message?.contains("password") == true -> "Password is too weak"
                        exception.message?.contains("network") == true -> "Network error. Please check your connection"
                        else -> "Registration failed. Please try again"
                    }
                    // Post error back to the main thread from IO dispatcher
                    withContext(Dispatchers.Main) {
                        _registrationState.value = RegistrationState.Error(errorMessage)
                        _error.value = errorMessage
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration exception: ${e.message}", e)
                val errorMessage = "An unexpected error occurred. Please try again"
                // Post error back to the main thread from IO dispatcher
                withContext(Dispatchers.Main) {
                    _registrationState.value = RegistrationState.Error(errorMessage)
                    _error.value = errorMessage
                }
            }
        }
    }

    // Password Reset Functionality
    fun validateResetEmail(): Boolean {
        _resetPasswordError.value = null
        
        if (resetEmail.value.isEmpty()) {
            _resetPasswordError.value = "Email is required"
            return false
        } else if (!validateEmail(resetEmail.value)) {
            _resetPasswordError.value = "Please enter a valid email address"
            return false
        }
        
        return true
    }
    
    fun sendPasswordResetEmail() {
        if (!validateResetEmail()) {
            return
        }
        
        viewModelScope.launch {
            isLoading.value = true
            _resetPasswordError.value = null
            _resetPasswordSuccess.value = null
            
            try {
                Log.d("AuthViewModel", "Sending password reset email to: ${resetEmail.value}")
                
                // Explicitly use the getInstance() to ensure correct Firebase instance
                val firebaseAuth = FirebaseAuth.getInstance()
                
                // Check network connectivity
                if (!isNetworkAvailable()) {
                    _resetPasswordError.value = "Network error. Please check your internet connection and try again."
                    isLoading.value = false
                    return@launch
                }
                
                // Create ActionCodeSettings with Firebase Dynamic Links
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://gamingba-e4849.firebaseapp.com/action")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(
                        "com.faruk.gamingba",
                        true,
                        "1"
                    )
                    .build()
                
                // Use the password reset with ActionCodeSettings
                firebaseAuth.sendPasswordResetEmail(resetEmail.value.trim(), actionCodeSettings).await()
                
                Log.d("AuthViewModel", "Password reset email sent successfully with Dynamic Link")
                _resetPasswordSuccess.value = "We've sent a password reset link to your email"
                
                // No longer automatically navigate to login
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to send password reset email: ${e.message}", e)
                _resetPasswordError.value = when {
                    e.message?.contains("user record") == true -> "No account exists with this email"
                    e.message?.contains("network") == true -> "Network error. Please check your connection"
                    e.message?.contains("INVALID_EMAIL") == true -> "Invalid email format"
                    e.message?.contains("TOO_MANY_ATTEMPTS_TRY_LATER") == true -> "Too many attempts. Please try again later"
                    e.message?.contains("ERROR_USER_NOT_FOUND") == true -> "No account found with this email"
                    else -> "Failed to send password reset email. Please try again later."
                }
            } finally {
                isLoading.value = false
            }
        }
    }
    
    // Helper function to check network connectivity
    private fun isNetworkAvailable(): Boolean {
        // In a real implementation, you'd check network connectivity
        // For this example, we'll assume network is available
        return true
    }
    
    // Validate new password
    fun validateNewPassword(): Boolean {
        _newPasswordError.value = null
        
        if (newPassword.value.isEmpty()) {
            _newPasswordError.value = "New password is required"
            return false
        } else if (!validatePassword(newPassword.value)) {
            _newPasswordError.value = "Password must be at least 6 characters"
            return false
        }
        
        return true
    }
    
    // Validate confirm password
    fun validateConfirmPassword(): Boolean {
        _confirmPasswordError.value = null
        
        if (confirmPassword.value.isEmpty()) {
            _confirmPasswordError.value = "Please confirm your password"
            return false
        } else if (confirmPassword.value != newPassword.value) {
            _confirmPasswordError.value = "Passwords don't match"
            return false
        }
        
        return true
    }
    
    // Complete password reset
    fun confirmPasswordReset(oobCode: String) {
        // No longer need separate validation here as it's handled by validateNewPasswordFields()
        // which is called from the fragment before this method
        
        viewModelScope.launch {
            isLoading.value = true
            _resetPasswordError.value = null
            
            try {
                auth.confirmPasswordReset(oobCode, newPassword.value).await()
                _resetPasswordSuccess.value = "Password reset successful! Redirecting..."
                
                // Wait for 2 seconds then navigate back to login
                delay(2000)
                _navigateToLogin.value = true
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to reset password", e)
                _resetPasswordError.value = when {
                    e.message?.contains("expired") == true -> "Password reset link has expired. Please request a new one"
                    e.message?.contains("invalid") == true -> "Invalid reset link. Please request a new one"
                    else -> "Failed to reset password. Please try again"
                }
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun verifyPasswordResetCode(oobCode: String) {
        viewModelScope.launch {
            isLoading.value = true
            _resetPasswordError.value = null
            
            try {
                Log.d("AuthViewModel", "Verifying password reset code: $oobCode")
                
                if (oobCode.isBlank()) {
                    Log.e("AuthViewModel", "Empty oobCode provided")
                    _resetPasswordError.value = "Invalid reset code"
                    return@launch
                }
                
                val email = auth.verifyPasswordResetCode(oobCode).await()
                Log.d("AuthViewModel", "Code verification successful for email: $email")
                
                // Set the email in the state
                resetEmail.value = email
                
                // Navigate to create new password screen with the verified code
                _navigateToCreateNewPassword.value = oobCode
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Invalid or expired reset code: ${e.message}", e)
                _resetPasswordError.value = when {
                    e.message?.contains("expired") == true -> "Password reset link has expired. Please request a new one"
                    e.message?.contains("invalid") == true -> "Invalid reset link. Please request a new one"
                    e.message?.contains("INVALID_OOB_CODE") == true -> "Invalid reset code. Please request a new password reset link"
                    else -> "Password reset link is invalid or has expired"
                }
                // Navigate back to reset email page after showing error
                delay(3000)
                _navigateToResetPassword.value = true
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun onForgotPasswordClicked() {
        _navigateToResetPassword.value = true
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun logout() {
        repository.logout()
        LoginManager.getInstance().logOut()
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
        _navigateToHome.value = false
        _navigateToVerifyEmail.value = null // Reset verify email navigation trigger
        _navigateToResetPassword.value = false
        _navigateToCreateNewPassword.value = null
        _registrationSuccessMessage.value = null
        _loginSuccessMessage.value = null
        // Clear any success/error messages when navigating away
        _resetPasswordError.value = null
        _resetPasswordSuccess.value = null
        _newPasswordError.value = null
        _confirmPasswordError.value = null
    }

    // Reset all fields related to password reset
    fun clearPasswordResetData() {
        resetEmail.value = ""
        newPassword.value = ""
        confirmPassword.value = ""
        _resetPasswordError.value = null
        _resetPasswordSuccess.value = null
        _newPasswordError.value = null
        _confirmPasswordError.value = null
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

    fun setResetEmail(value: String) {
        resetEmail.value = value
        validateResetEmail()
    }
    
    fun setNewPassword(value: String, shouldValidate: Boolean = true) {
        newPassword.value = value
        if (shouldValidate) {
            validateNewPassword()
            if (confirmPassword.value.isNotEmpty()) {
                validateConfirmPassword()
            }
        }
    }
    
    fun setConfirmPassword(value: String, shouldValidate: Boolean = true) {
        confirmPassword.value = value
        if (shouldValidate) {
            validateConfirmPassword()
        }
    }

    // Added method to validate both password fields at once
    fun validateNewPasswordFields(): Boolean {
        // Clear any previous errors first
        _newPasswordError.value = null
        _confirmPasswordError.value = null
        _resetPasswordError.value = null
        
        // Validate both fields
        val isNewPasswordValid = validateNewPassword()
        val isConfirmPasswordValid = validateConfirmPassword()
        
        return isNewPasswordValid && isConfirmPasswordValid
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            isLoading.value = true
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            try {
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                user?.let { firebaseUser ->
                    val userRef = database.reference.child("users").child(firebaseUser.uid)
                    userRef.get().addOnSuccessListener { snapshot ->
                        if (!snapshot.exists()) {
                            val newUser = User(firstName = firebaseUser.displayName ?: "", email = firebaseUser.email ?: "")
                            userRef.setValue(newUser)
                        }
                    }
                    _currentUser.value = firebaseUser
                    _navigateToHome.value = true
                }
            } catch (e: Exception) {
                _error.value = "Google sign-in failed. Please try again"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun setLoginError(message: String) {
        _loginError.value = message
    }

    fun signInWithFacebook(token: AccessToken) {
        Log.d("AuthViewModel", "signInWithFacebook called")
        viewModelScope.launch {
            isLoading.value = true
            _loginError.value = null // Clear previous login errors
            val credential = FacebookAuthProvider.getCredential(token.token)
            try {
                val authResultFirebase = auth.signInWithCredential(credential).await()
                val user = authResultFirebase.user
                user?.let { firebaseUser ->
                    // Check if user exists in Realtime Database, create if not
                    val userRef = database.reference.child("users").child(firebaseUser.uid)
                    userRef.get().addOnSuccessListener { snapshot ->
                        if (!snapshot.exists()) {
                            // Extract name and email if possible (depends on Facebook permissions granted)
                            // Note: Facebook might not provide email even if requested
                            // Using display name as fallback for first name
                            val userFirstName = firebaseUser.displayName ?: ""
                            val userEmail = firebaseUser.email ?: ""
                            val newUser = User(firstName = userFirstName, email = userEmail)
                            userRef.setValue(newUser)
                        }
                    }.addOnFailureListener {
                        Log.e("AuthViewModel", "Failed to check/create user in DB for Facebook sign in", it)
                    }

                    _currentUser.value = firebaseUser
                    _authResult.value = Result.success(Unit) // Signal success for fragment observer
                    _navigateToHome.value = true // Navigate to home
                    Log.d("AuthViewModel", "Firebase sign in with Facebook successful.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Firebase sign in with Facebook failed", e)
                _authResult.value = Result.failure(e) // Signal failure
                // Set a user-friendly error message
                _loginError.value = "Facebook sign-in failed. Please try again."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearResetPasswordError() {
        _resetPasswordError.value = null
    }

    fun clearNewPasswordError() {
        _newPasswordError.value = null
    }

    fun clearConfirmPasswordError() {
        _confirmPasswordError.value = null
    }
}
