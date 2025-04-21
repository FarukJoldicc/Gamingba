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

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

