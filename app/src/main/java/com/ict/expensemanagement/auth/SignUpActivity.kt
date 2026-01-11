package com.ict.expensemanagement.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.R
import com.ict.expensemanagement.data.entity.User
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivitySignUpBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val firebaseRepository = FirebaseRepository()
    private lateinit var activity: Activity
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        activity = this

        // Setup sign in link with blue "Sign in here" text
        setupSignInLink()

        // Password visibility toggles
        binding.passwordVisibilityToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.confirmPasswordVisibilityToggle.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.button.setOnClickListener {
            val username = binding.userNameEt.text.toString()
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass.equals(confirmPass)) {
                    GlobalScope.launch {
                        if (firebaseRepository.checkUsernameExists(username)) {
                            runOnUiThread {
                                Toast.makeText(activity, "Username is already exist", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener{
                                if (it.isSuccessful) {
                                    val authUser = auth.currentUser
                                    val uid = authUser!!.uid
                                    val user = User(uid, username, pass, email, "")
                                    user.setCode()
                                    
                                    GlobalScope.launch {
                                        try {
                                            firebaseRepository.saveUser(user)
                                            runOnUiThread {
                                                val intent = Intent(activity, SignInActivity::class.java)
                                                startActivity(intent)
                                            }
                                        } catch (e: Exception) {
                                            runOnUiThread {
                                                Toast.makeText(activity, "Error when save user: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(activity, it.exception.toString(), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are Not Allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupSignInLink() {
        val fullText = "Already have an account? Sign in here"
        val spannableString = SpannableString(fullText)
        val blueColor = ContextCompat.getColor(this, R.color.primaryBlue)
        val startIndex = fullText.indexOf("Sign in here")
        val endIndex = startIndex + "Sign in here".length

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

        binding.passET.setSelection(binding.passET.text?.length ?: 0)
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible

        if (isConfirmPasswordVisible) {
            binding.confirmPassEt.transformationMethod = null
            binding.confirmPasswordVisibilityToggle.setImageResource(R.drawable.ic_eye_hidden)
        } else {
            binding.confirmPassEt.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.confirmPasswordVisibilityToggle.setImageResource(R.drawable.ic_eye)
        }

        binding.confirmPassEt.setSelection(binding.confirmPassEt.text?.length ?: 0)
    }
}