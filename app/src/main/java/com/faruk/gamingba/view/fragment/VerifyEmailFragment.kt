package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.FragmentVerifyEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!
    private val args: VerifyEmailFragmentArgs by navArgs()
    private lateinit var auth: FirebaseAuth
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var verificationCheckFuture: ScheduledFuture<*>? = null

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
        auth.setLanguageCode("en") // Set language to avoid localized messages
        
        binding.verificationSpinner.visibility = View.GONE // Ensure spinner is hidden initially
        binding.textSuccessMessage.visibility = View.GONE // Ensure success message is hidden initially
        setupClickListeners()
        setupEmailVerificationMessage()
        
        // Start cooldown timer immediately
        startCooldownTimer()
        
        // Send verification email automatically on first load
        sendVerificationEmail()
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
        if (user == null) {
            Log.e("VerifyEmailFragment", "User is null, cannot send verification email.")
            return
        }
        
        try {
            user.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("VerifyEmailFragment", "Verification email sent successfully.")
                    } else {
                        Log.e("VerifyEmailFragment", "Failed to send verification email.", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e("VerifyEmailFragment", "Exception sending verification email", e)
        }
    }

    private fun startCooldownTimer() {
        if (isTimerRunning) return

        binding.buttonResendEmail.isEnabled = false
        isTimerRunning = true

        timer = object : CountDownTimer(45000, 1000) {  // 45 seconds cooldown
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

    override fun onResume() {
        super.onResume()
        Log.d("VerifyEmailFragment", "onResume called, starting verification check.")
        // Hide success message and spinner if they were previously shown
        binding.textSuccessMessage.visibility = View.GONE
        binding.verificationSpinner.visibility = View.GONE // Ensure spinner is hidden on resume
        startVerificationCheck()
    }

    override fun onPause() {
        super.onPause()
        Log.d("VerifyEmailFragment", "onPause called, stopping verification check.")
        stopVerificationCheck()
    }

    private fun startVerificationCheck() {
        // Avoid starting multiple checks
        if (verificationCheckFuture?.isDone == false) {
            return
        }
        // Check immediately and then every 2 seconds
        verificationCheckFuture = scheduler.scheduleAtFixedRate({
            // Ensure code runs on the main thread for UI updates and Firebase calls
            activity?.runOnUiThread {
                checkVerificationStatus()
            }
        }, 0, 2, TimeUnit.SECONDS) // Changed interval to 2 seconds
    }

    private fun stopVerificationCheck() {
        verificationCheckFuture?.cancel(true)
    }

    private fun checkVerificationStatus() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
             Log.d("VerifyEmailFragment", "User is null, stopping verification check.")
             stopVerificationCheck()
             return
        }

        try {
            currentUser.reload()?.addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    val isVerified = firebaseAuth.currentUser?.isEmailVerified == true // Re-check after reload
                    if (isVerified) {
                        Log.d("VerifyEmailFragment", "Email verified! Showing success state and navigating to home.")
                        stopVerificationCheck() // Stop periodic checking

                        // --- UI Changes for Verified State ---
                        binding.imageLogo.visibility = View.VISIBLE // Ensure logo is visible
                        binding.textVerifyTitle.visibility = View.GONE
                        binding.textVerifyMessage.visibility = View.GONE
                        binding.buttonResendEmail.visibility = View.GONE
                        binding.buttonGoToLogin.visibility = View.GONE

                        binding.verificationSpinner.visibility = View.VISIBLE // Show spinner NOW
                        binding.textSuccessMessage.visibility = View.VISIBLE // Show success text
                        // --- End UI Changes ---

                        lifecycleScope.launch {
                            delay(2000) // Wait 2 seconds to show the success state
                            // Ensure navigation happens only if still in this fragment
                            if (findNavController().currentDestination?.id == R.id.verifyEmailFragment) {
                                 Log.d("VerifyEmailFragment", "Navigating to HomeFragment.")
                                 // Navigate to Home instead of Login
                                 findNavController().navigate(R.id.action_verifyEmailFragment_to_homeFragment)
                             }
                        }
                    } else {
                        Log.d("VerifyEmailFragment", "Email still not verified.")
                        // Ensure standard UI elements are visible if not verified
                        binding.imageLogo.visibility = View.VISIBLE // Ensure logo is visible
                        binding.textVerifyTitle.visibility = View.VISIBLE
                        binding.textVerifyMessage.visibility = View.VISIBLE
                        binding.buttonResendEmail.visibility = View.VISIBLE
                        binding.buttonGoToLogin.visibility = View.VISIBLE
                        binding.textSuccessMessage.visibility = View.GONE // Keep success message hidden
                        binding.verificationSpinner.visibility = View.GONE // Keep spinner hidden
                    }
                } else {
                    Log.e("VerifyEmailFragment", "Failed to reload user.", reloadTask.exception)
                }
            }
        } catch (e: Exception) {
            Log.e("VerifyEmailFragment", "Exception during verification check", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
        stopVerificationCheck() // Ensure scheduler is stopped
        _binding = null
    }
} 