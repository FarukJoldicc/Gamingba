package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.FragmentVerifyEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!
    private val args: VerifyEmailFragmentArgs by navArgs()
    private lateinit var auth: FirebaseAuth
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        setupClickListeners()
        setupEmailVerificationMessage()
    }

    private fun setupClickListeners() {
        // Resend Email Button
        binding.buttonResendEmail.setOnClickListener {
            sendVerificationEmail()
            startCooldownTimer()
        }

        // Go to Login Button
        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_verifyEmailFragment_to_loginFragment)
        }
    }

    private fun setupEmailVerificationMessage() {
        val userEmail = args.email
        val messageText = getString(R.string.verify_email_message_template, userEmail)
        binding.textVerifyMessage.text = messageText
    }

    private fun sendVerificationEmail() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("VerifyEmailFragment", "Verification email sent successfully.")
                    Toast.makeText(context, getString(R.string.email_sent_success), Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("VerifyEmailFragment", "Failed to send verification email.", task.exception)
                    Toast.makeText(
                        context,
                        getString(R.string.email_sent_failure, task.exception?.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun startCooldownTimer() {
        if (isTimerRunning) return

        binding.buttonResendEmail.isEnabled = false
        isTimerRunning = true

        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.buttonResendEmail.text = getString(R.string.resend_email_cooldown, secondsRemaining)
            }

            override fun onFinish() {
                binding.buttonResendEmail.isEnabled = true
                binding.buttonResendEmail.text = getString(R.string.resend_verification_email)
                isTimerRunning = false
            }
        }.start()
    }

    // Optional: Check verification status periodically or on resume?
    // For simplicity, we'll rely on the user logging in again after verification.
    /*
    override fun onResume() {
        super.onResume()
        checkVerificationStatus()
    }

    private fun checkVerificationStatus() {
        firebaseAuth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                val isVerified = firebaseAuth.currentUser?.isEmailVerified == true
                if (isVerified) {
                    Log.d("VerifyEmailFragment", "Email verified! Navigating to home.")
                    // Navigate to home - requires appropriate action in nav graph
                     findNavController().navigate(R.id.action_verifyEmailFragment_to_homeFragment) // Needs action created
                } else {
                    Log.d("VerifyEmailFragment", "Email still not verified.")
                }
            } else {
                Log.e("VerifyEmailFragment", "Failed to reload user.", reloadTask.exception)
            }
        }
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
        _binding = null
    }
} 