package com.ict.expensemanagement.transaction

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.adapter.TransactionAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityAddTransactionBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private lateinit var transactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val firebaseRepository = FirebaseRepository()

    @OptIn(DelicateCoroutinesApi::class) 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        setupRecyclerView()
        setupClickListeners()
        fetchLatestTransactions()
    }

    private fun setupRecyclerView() {
        transactions = emptyList()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(this)

        binding.recyclerview.adapter = transactionAdapter
        binding.recyclerview.layoutManager = linearLayoutManager
        binding.recyclerview.setHasFixedSize(true)
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addIncomeCard.setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }

        binding.addExpenseCard.setOnClickListener {
            // Navigate to Add Expense (can reuse AddIncomeActivity with different mode)
            val intent = Intent(this, AddIncomeActivity::class.java)
            intent.putExtra("isExpense", true)
            startActivity(intent)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchLatestTransactions() {
        GlobalScope.launch {
            if (userId != null) {
                transactions = firebaseRepository.getTransactionsByUserId(userId!!)
                    .sortedByDescending { it.transactionDate }
                    .take(10) // Get latest 10 transactions

                runOnUiThread {
                    transactionAdapter.setData(transactions)
                }
            }
        }
    }
}