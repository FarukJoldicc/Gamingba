package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.FragmentLoginBinding
import com.faruk.gamingba.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import javax.inject.Inject
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.LoginManager

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable Firebase default error handling
        firebaseAuth.setLanguageCode("en")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("LoginFragment", "onViewCreated called")

        setupGoogleSignIn()
        setupTextWatchers()
        setupClickListeners()
        setupPasswordToggle()
        setupRegisterText()
        observeViewModel()
        setupErrorHandling()
        setupFacebookLogin()
    }

    private fun setupErrorHandling() {
        // Disable Firebase's default error handling
        firebaseAuth.setLanguageCode("en")
        
        // Override the default error handler
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            when (throwable) {
                is FirebaseAuthException -> {
                    // Log the error but don't show it to the user
                    Log.e("LoginFragment", "Firebase Auth Error: ${throwable.message}")
                    // Show our custom error message instead
                    viewModel.setLoginError("Invalid email or password")
                }
                else -> {
                    Log.e("LoginFragment", "Error: ${throwable.message}")
                    viewModel.setLoginError("An error occurred. Please try again.")
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.googleSignInButton.setOnClickListener {
            // Sign out before starting new sign-in flow
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    private fun setupPasswordToggle() {
        val passwordToggle = binding.passwordToggle
        val passwordEditText = binding.passwordEditText
        
        var isPasswordVisible = false
        
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                // Show password
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_password_toggle_off)
            } else {
                // Hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_password_toggle)
            }
            
            // Keep cursor at the end of text
            passwordEditText.setSelection(passwordEditText.text.length)
        }
    }

    private fun setupRegisterText() {
        val registerText = getString(R.string.don_t_have_an_account_yet_register)
        val spannable = SpannableString(registerText)
        
        // Find the position of "Register" in the text
        val registerStart = registerText.indexOf("Register")
        val registerEnd = registerStart + "Register".length
        
        // Apply cyan color to the "Register" text
        val cyanColor = ContextCompat.getColor(requireContext(), R.color.button_cyan)
        spannable.setSpan(ForegroundColorSpan(cyanColor), registerStart, registerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        // Set the spannable text to the TextView
        binding.goToRegisterButton.text = spannable
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("LoginFragment", "onActivityResult called. RequestCode: $requestCode")
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupTextWatchers() {
        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setEmail(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setPassword(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            Log.d("LoginFragment", "Login button clicked")
            viewModel.onLoginClicked()
        }
        
        binding.goToRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.facebookSignInButton.setOnClickListener {
            Log.d("LoginFragment", "Facebook icon clicked, triggering hidden login button")
            binding.facebookLoginButtonHidden.performClick()
        }
    }

    private fun setupFacebookLogin() {
        // Request email permission along with public_profile (default)
        binding.facebookLoginButtonHidden.setPermissions("email", "public_profile")
        // Explicitly set the fragment for the LoginButton callback
        binding.facebookLoginButtonHidden.setFragment(this)

        // Register callback for the hidden button
        binding.facebookLoginButtonHidden.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("LoginFragment", "FacebookCallback onSuccess triggered.")
                Log.d("LoginFragment", "Facebook login successful: ${result.accessToken.token}")
                viewModel.signInWithFacebook(result.accessToken)
            }

            override fun onCancel() {
                Log.d("LoginFragment", "FacebookCallback onCancel triggered.")
                // Using our own toast message instead of default
                Toast.makeText(context, "Facebook login canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.e("LoginFragment", "FacebookCallback onError triggered.", error)
                // Using our own toast message instead of default
                Toast.makeText(context, "Facebook login failed. Please try again", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun observeViewModel() {
        // Observe navigation events
        viewModel.navigateToRegister.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                Log.d("LoginFragment", "Navigating to register fragment")
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                viewModel.onNavigationHandled()
            }
        }

        viewModel.navigateToHome.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                Log.d("LoginFragment", "Navigating to home fragment")
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                viewModel.onNavigationHandled()
            }
        }

        // Observe auth result - updated to handle email verification
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authResult.collectLatest { result ->
                result?.let {
                    if (it.isSuccess) {
                        // Navigation to home is handled by navigateToHome LiveData
                        Log.d("LoginFragment", "Auth successful observed (isSuccess=true).")
                    } else {
                        // Check if the failure is due to email not being verified
                        if (it.exceptionOrNull() is AuthViewModel.EmailNotVerifiedException) {
                            // Navigation to verify email screen is handled by navigateToVerifyEmail LiveData
                            Log.d("LoginFragment", "Auth failed observed: Email not verified.")
                        } else {
                            // Other login errors are handled by viewModel.loginError LiveData binding
                            Log.e("LoginFragment", "Auth failed observed: ${it.exceptionOrNull()?.message}")
                        }
                    }
                    viewModel.clearAuthResult() // Clear the result after observing
                }
            }
        }

        // Observe navigation to Verify Email screen (for login attempts)
        viewModel.navigateToVerifyEmail.observe(viewLifecycleOwner) { email ->
            email?.let {
                // Check if we are currently on the LoginFragment to avoid double navigation
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    Log.d("LoginFragment", "Navigating to Verify Email screen for $email")
                    val action = LoginFragmentDirections.actionLoginFragmentToVerifyEmailFragment(it)
                    findNavController().navigate(action)
                    viewModel.onNavigationHandled() // Reset the trigger
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
