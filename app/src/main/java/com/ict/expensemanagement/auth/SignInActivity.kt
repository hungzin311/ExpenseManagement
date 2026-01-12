package com.ict.expensemanagement.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.ict.expensemanagement.HomeActivity
import com.ict.expensemanagement.R
import com.ict.expensemanagement.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Setup register link with blue "Register here" text
        setupRegisterLink()

        // Password visibility toggle
        binding.passwordVisibilityToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        // Forgot password click
        binding.forgotPasswordTextView.setOnClickListener {
            handleForgotPassword()
        }

        // Register link click
        binding.textView.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Login button click
        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        hideError()
                        Toast.makeText(this, "You have signed in successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        showError()
                        val exception = task.exception
                        val errorMessage = when {
                            exception is FirebaseAuthException -> {
                                when ((exception as FirebaseAuthException).errorCode) {
                                    "ERROR_INVALID_EMAIL" -> "Invalid email address"
                                    "ERROR_WRONG_PASSWORD" -> "Wrong password"
                                    "ERROR_USER_NOT_FOUND" -> "User not found. Please register first"
                                    "ERROR_USER_DISABLED" -> "This account has been disabled"
                                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later"
                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection"
                                    else -> exception.message ?: "Login failed"
                                }
                            }
                            else -> exception?.message ?: "Login failed. Please try again"
                        }
                        Log.e("SignInActivity", "Login failed: ${exception?.message}", exception)
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are Not Allowed", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear error when user starts typing (when clicked --> focus = True)
        binding.emailEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearError()
            }
        }

        binding.passET.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearError()
            }
        }

        // Also clear error when text changes, before and after: no-op
        binding.emailEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearError()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.passET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearError()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRegisterLink() {
        val fullText = "Don't have an account? Register here"
        val spannableString = SpannableString(fullText)
        val blueColor = ContextCompat.getColor(this, R.color.primaryBlue)
        val startIndex = fullText.indexOf("Register here")
        val endIndex = startIndex + "Register here".length

        if (startIndex >= 0) {
            spannableString.setSpan(
                ForegroundColorSpan(blueColor),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.textView.text = spannableString
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.passET.transformationMethod = null
            binding.passwordVisibilityToggle.setImageResource(R.drawable.ic_eye_hidden)
        } else {
            binding.passET.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.passwordVisibilityToggle.setImageResource(R.drawable.ic_eye)
        }

        // Move cursor to end
        binding.passET.setSelection(binding.passET.text?.length ?: 0)
    }

    private fun handleForgotPassword() {
        val email = binding.emailEt.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError() {
        binding.errorBanner.visibility = View.VISIBLE
        binding.usernameContainer.setBackgroundResource(R.drawable.rounded_input_background_error)
        binding.passwordContainer.setBackgroundResource(R.drawable.rounded_input_background_error)
    }

    private fun hideError() {
        binding.errorBanner.visibility = View.GONE
        binding.usernameContainer.setBackgroundResource(R.drawable.rounded_input_background)
        binding.passwordContainer.setBackgroundResource(R.drawable.rounded_input_background)
    }

    private fun clearError() {
        if (binding.errorBanner.visibility == View.VISIBLE) {
            hideError()
        }
    }
}