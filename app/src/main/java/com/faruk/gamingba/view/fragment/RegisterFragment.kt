package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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
import com.faruk.gamingba.databinding.FragmentRegisterBinding
import com.faruk.gamingba.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupLoginText()

        binding.registerButton.setOnClickListener {
            viewModel.onRegisterClicked()
        }

        lifecycleScope.launch {
            viewModel.authResult.collectLatest { result ->
                result?.onSuccess {
                    Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }?.onFailure {
                    Toast.makeText(requireContext(), "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupLoginText() {
        val loginText = "I already have an account? Login"
        val spannable = SpannableString(loginText)

        val loginStart = loginText.indexOf("Login")
        val loginEnd = loginStart + "Login".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireContext(), R.color.button_cyan)
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(clickableSpan, loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.goToLoginButton.text = spannable
        binding.goToLoginButton.movementMethod = LinkMovementMethod.getInstance()
        binding.goToLoginButton.highlightColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
    }
}
