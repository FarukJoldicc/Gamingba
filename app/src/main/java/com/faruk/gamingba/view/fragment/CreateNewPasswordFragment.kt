package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.FragmentCreateNewPasswordBinding
import com.faruk.gamingba.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "CreateNewPasswordFrag"

@AndroidEntryPoint
class CreateNewPasswordFragment : Fragment() {

    private var _binding: FragmentCreateNewPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val args: CreateNewPasswordFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateNewPasswordBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Validate the oobCode from the arguments
        validateOobCode()
        
        setupClickListeners()
        setupTextWatchers()
        setupPasswordToggles()
        observeViewModel()
        hideValidationErrors() // Initially hide all validation errors
    }
    
    private fun validateOobCode() {
        val oobCode = args.oobCode
        Log.d(TAG, "Received oobCode: $oobCode")
        
        if (oobCode.isEmpty()) {
            Log.e(TAG, "Empty oobCode received")
            Toast.makeText(requireContext(), "Invalid password reset link", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }
        
        // Verify the code is valid with Firebase
        viewModel.verifyPasswordResetCode(oobCode)
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.resetButton.setOnClickListener {
            // Show validation errors only when button is clicked
            if (viewModel.validateNewPasswordFields()) {
                viewModel.confirmPasswordReset(args.oobCode)
            }
        }
    }

    private fun setupTextWatchers() {
        binding.newPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setNewPassword(s?.toString() ?: "", false) // Don't validate yet
            }
        })

        binding.confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setConfirmPassword(s?.toString() ?: "", false) // Don't validate yet
            }
        })
    }

    private fun hideValidationErrors() {
        // Hide all error messages initially using INVISIBLE instead of GONE
        // This matches the approach used in login and register fragments
        binding.newPasswordError.visibility = View.INVISIBLE
        binding.confirmPasswordError.visibility = View.INVISIBLE
        binding.generalErrorText.visibility = View.INVISIBLE
    }

    private fun setupPasswordToggles() {
        var isNewPasswordVisible = false
        binding.newPasswordToggle.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            togglePasswordVisibility(
                binding.newPasswordEditText,
                binding.newPasswordToggle,
                isNewPasswordVisible
            )
        }

        var isConfirmPasswordVisible = false
        binding.confirmPasswordToggle.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(
                binding.confirmPasswordEditText,
                binding.confirmPasswordToggle,
                isConfirmPasswordVisible
            )
        }
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText, toggleButton: android.widget.ImageView, isVisible: Boolean) {
        if (isVisible) {
            // Show password
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.ic_password_toggle_off)
        } else {
            // Hide password
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.ic_password_toggle)
        }
        // Keep cursor at the end of text
        editText.setSelection(editText.text.length)
    }

    private fun observeViewModel() {
        // Observe navigation to login
        viewModel.navigateToLogin.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                // Navigate back to login screen
                findNavController().navigate(R.id.action_createNewPasswordFragment_to_loginFragment)
                viewModel.onNavigationHandled()
            }
        }

        // Observe success message
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetPasswordSuccess.collectLatest { success ->
                if (success != null) {
                    // Show success message and redirect spinner
                    binding.successLayout.visibility = View.VISIBLE
                }
            }
        }

        // Error states are directly bound via data binding
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 