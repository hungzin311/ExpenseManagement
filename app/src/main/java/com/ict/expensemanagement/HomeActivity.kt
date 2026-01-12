package com.ict.expensemanagement

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.adapter.TransactionAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityHomeBinding
import com.ict.expensemanagement.goal.SavingsActivity
import com.ict.expensemanagement.stats.SpendsFragment
import com.ict.expensemanagement.transaction.AddTransactionActivity
import com.ict.expensemanagement.transaction.TotalExpensesActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var deletedTransaction : Transaction
    private lateinit var transactions: List<Transaction>
    private lateinit var oldTransactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val firebaseRepository = FirebaseRepository()
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private val activity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        
        // Lấy userId trong onCreate()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid
        setContentView(view)
        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(this)
        
        // Setup Spends Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.spendsFragmentContainer, SpendsFragment())
                .commit()
        }
        
        val bottomNavigationView = binding.bottomNavView
        bottomNavigationView.selectedItemId = R.id.item_home
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_home -> {
                    // Already on Home
                    true
                }
                R.id.item_savings -> {
                    val intent = Intent(this, SavingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.item_notification -> {
                    // TODO: Navigate to Notification screen
                    true
                }
                R.id.item_settings -> {
                    startActivity(Intent(activity, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        val addBtn = binding.addBtn
        addBtn.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
        
        // Total Expense Card click - navigate to Total Expenses screen
        binding.totalExpenseCard.setOnClickListener {
            val intent = Intent(this, TotalExpensesActivity::class.java)
            startActivity(intent)
        }
        
        // Savings Button click - navigate to Savings screen
        binding.savingsButton.setOnClickListener {
            val intent = Intent(this, SavingsActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        // No need to sync anymore, Firebase handles it automatically
    }

    private fun fetchAll() {
        val id = userId ?: return
        lifecycleScope.launch {
            val allTransactions = withContext(Dispatchers.IO) {
                firebaseRepository.getTransactionsByUserId(id)
            }
            // Lọc theo tháng hiện tại để đồng bộ với Total Expenses
            val today = java.time.LocalDate.now()
            transactions = allTransactions.filter { tran ->
                try {
                    val date = java.time.LocalDate.parse(tran.transactionDate)
                    date.year == today.year && date.monthValue == today.monthValue
                } catch (e: Exception) {
                    false
                }
            }.sortedByDescending { it.transactionDate }

            updateDashboard()
            // Update SpendsFragment
            val spendsFragment = supportFragmentManager.findFragmentById(R.id.spendsFragmentContainer) as? SpendsFragment
            spendsFragment?.setTransactions(transactions)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset bottom navigation to Home when returning from other activities
        binding.bottomNavView.selectedItemId = R.id.item_home
        fetchAll()
    }

    @SuppressLint("SetTextI18n")
    private fun updateDashboard(){
        // Calculate Total Salary (income - positive amounts)
        val totalSalary = transactions.filter { it.amount > 0 }.map { it.amount }.sum()
        
        // Calculate Total Expense (expenses - negative amounts, convert to positive)
        val totalExpense = transactions.filter { it.amount < 0 }.map { kotlin.math.abs(it.amount) }.sum()
        
        // Update UI
        binding.totalSalary.text = "$${String.format(Locale.US, "%,.2f", totalSalary)}"
        binding.totalExpense.text = "$${String.format(Locale.US, "%,.2f", totalExpense)}"
    }

    private fun undoDelete() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                firebaseRepository.insertTransaction(deletedTransaction)
            }
            transactions = oldTransactions
            transactionAdapter.setData(transactions)
            updateDashboard()
        }
    }

    private fun showSnackbar() {
        val view : View = binding.coordinator
        val snackbar : Snackbar = Snackbar.make(view, "Transaction deleted!", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo"){
            undoDelete()
        }
            .setActionTextColor(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        deletedTransaction = transaction
        oldTransactions = transactions

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                firebaseRepository.deleteTransaction(transaction)
            }
            transactions = transactions.filter {it.id != transaction.id}
            updateDashboard()
            transactionAdapter.setData(transactions)
            showSnackbar()
        }
    }

    // Sync functions removed - Firebase Firestore handles synchronization automatically
}