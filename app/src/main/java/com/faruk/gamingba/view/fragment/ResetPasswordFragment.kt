package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.faruk.gamingba.databinding.FragmentResetPasswordBinding
import com.faruk.gamingba.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Clear any previous data
        viewModel.clearPasswordResetData()
        
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.nextButton.setOnClickListener {
            viewModel.sendPasswordResetEmail()
        }
    }

    private fun setupTextWatchers() {
        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setResetEmail(s?.toString() ?: "")
                // Clear error message when user starts typing
                if (s?.isNotEmpty() == true) {
                    viewModel.clearResetPasswordError()
                }
            }
        })
    }

    private fun observeViewModel() {
        // Observe error message
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetPasswordError.collectLatest { error ->
                // Error is bound directly to the TextView via data binding
                // No need for additional handling here
            }
        }

        // Observe success message
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetPasswordSuccess.collectLatest { success ->
                if (success != null) {
                    // Show success view and hide form content
                    binding.emailSentView.visibility = View.VISIBLE
                    binding.formContent.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 