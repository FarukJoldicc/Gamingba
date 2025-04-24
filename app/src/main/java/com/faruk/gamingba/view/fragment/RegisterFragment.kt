package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.FragmentRegisterBinding
import com.faruk.gamingba.model.state.RegistrationState
import com.faruk.gamingba.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import android.widget.Toast
import android.util.Log

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callbackManager = CallbackManager.Factory.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDataBinding()
        setupTextWatchers()
        setupClickListeners()
        setupLoginText()
        observeViewModel()
        setupGoogleSignIn()
        setupFacebookLogin()
        
        // Set up password visibility toggle
        val passwordToggle = view.findViewById<ImageView>(R.id.passwordToggle)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    private fun setupDataBinding() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        
        // Observe LiveData values to update EditText fields
        viewModel.firstNameLiveData.observe(viewLifecycleOwner) { firstName ->
            if (binding.firstNameEditText.text.toString() != firstName) {
                binding.firstNameEditText.setText(firstName)
            }
        }
        
        viewModel.emailLiveData.observe(viewLifecycleOwner) { email ->
            if (binding.emailEditText.text.toString() != email) {
                binding.emailEditText.setText(email)
            }
        }
        
        viewModel.passwordLiveData.observe(viewLifecycleOwner) { password ->
            if (binding.passwordEditText.text.toString() != password) {
                binding.passwordEditText.setText(password)
            }
        }
    }

    private fun setupTextWatchers() {
        binding.firstNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setFirstName(s?.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setEmail(s?.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setPassword(s?.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            viewModel.onRegisterClicked()
        }

        binding.goToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.facebookSignUpButton.setOnClickListener {
            Log.d("RegisterFragment", "Facebook icon clicked, triggering hidden login button")
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
                Log.d("RegisterFragment", "Facebook login successful: ${result.accessToken.token}")
                viewModel.signInWithFacebook(result.accessToken)
            }

            override fun onCancel() {
                Log.d("RegisterFragment", "Facebook login canceled.")
                Toast.makeText(context, "Facebook login canceled.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.e("RegisterFragment", "Facebook login error: ${error.message}")
                Toast.makeText(context, "Facebook login failed. Please try again.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupLoginText() {
        val loginText = "I already have an account? Login"
        val spannable = SpannableString(loginText)
        
        // Find the position of "Login" in the text
        val loginStart = loginText.indexOf("Login")
        val loginEnd = loginStart + "Login".length
        
        // Apply cyan color to the "Login" text
        val cyanColor = ContextCompat.getColor(requireContext(), R.color.button_cyan)
        spannable.setSpan(ForegroundColorSpan(cyanColor), loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        // Set the spannable text to the TextView
        binding.goToLoginButton.text = spannable
    }

    private fun observeViewModel() {
        viewModel.registrationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegistrationState.Success -> {
                    // Navigate to login fragment
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                is RegistrationState.Error -> {
                    // Error is already handled by the ViewModel and displayed in the layout
                }
                is RegistrationState.Loading -> {
                    // Loading state is handled by the layout
                }
            }
        }

        viewModel.navigateToLogin.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                viewModel.onNavigationHandled()
            }
        }

        viewModel.navigateToHome.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                if (findNavController().currentDestination?.id == R.id.registerFragment) {
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
                viewModel.onNavigationHandled()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

