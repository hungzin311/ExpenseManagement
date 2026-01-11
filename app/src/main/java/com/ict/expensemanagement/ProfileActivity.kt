package com.ict.expensemanagement

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.auth.SignInActivity
import com.ict.expensemanagement.data.entity.User
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityProfileBinding
import com.ict.expensemanagement.goal.SavingsActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val firebaseRepository = FirebaseRepository()
    private lateinit var user: User
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Lấy userId trong onCreate()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid
        
        val usernameLayout = binding.profileUsername
        val emailLayout = binding.profileMail
        val moneyLayout = binding.profileMoney
        val logoutBtn = binding.logoutBtn
        val bottomNavigationView = binding.bottomNavView

        bottomNavigationView.selectedItemId = R.id.item_settings

        GlobalScope.launch {    
            val fetchedUser = firebaseRepository.getUserById(userId!!)
            val money = firebaseRepository.getMoneyByUserId(userId!!)

            runOnUiThread {
                if (fetchedUser != null) {
                    user = fetchedUser
                    usernameLayout.text = user.username
                    emailLayout.text = user.email
                    moneyLayout.text = "${"%, .0f".format(Locale.US, money)} VND"
                } else {
                    // Handle case when user is not found
                    usernameLayout.text = "User not found"
                    emailLayout.text = ""
                    moneyLayout.text = "0 VND"
                }
            }
        }

        logoutBtn.setOnClickListener { onLogoutButtonClick(it) }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.item_savings -> {
                    startActivity(Intent(this, SavingsActivity::class.java))
                    true
                }
                R.id.item_notification -> {
                    // TODO: Navigate to Notification screen
                    true
                }
                R.id.item_settings -> {
                    // Already on Settings
                    true
                }
                else -> false
            }
        }
    }

    fun onLogoutButtonClick(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, _ ->
                auth.signOut()
                startActivity(Intent(this, SignInActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}