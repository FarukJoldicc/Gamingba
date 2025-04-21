package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("LoginFragment", "onViewCreated called")

        // Set up text change listeners
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

        // Set up login button click listener
        binding.loginButton.setOnClickListener {
            Log.d("LoginFragment", "Login button clicked")
            viewModel.onLoginClicked()
        }

        // Observe navigation events
        viewModel.navigateToRegister.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                Log.d("LoginFragment", "Navigating to register fragment")
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                viewModel.onNavigationHandled()
            }
        }

        // Observe auth result
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authResult.collectLatest { result ->
                result?.let {
                    if (it.isSuccess) {
                        Log.d("LoginFragment", "Login successful, navigating to home fragment")
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        Log.e("LoginFragment", "Login failed: ${it.exceptionOrNull()?.message}")
                        Toast.makeText(requireContext(), it.exceptionOrNull()?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
