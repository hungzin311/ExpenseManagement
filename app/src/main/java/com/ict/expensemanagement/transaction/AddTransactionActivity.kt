package com.ict.expensemanagement.transaction

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.adapter.TransactionAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityAddTransactionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private lateinit var transactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val firebaseRepository = FirebaseRepository()

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

    override fun onResume() {
        super.onResume()
        fetchLatestTransactions()
    }

    private fun setupRecyclerView() {
        transactions = emptyList()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(this)

        binding.recyclerview.adapter = transactionAdapter
        binding.recyclerview.layoutManager = linearLayoutManager
        binding.recyclerview.setHasFixedSize(false)
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

    private fun fetchLatestTransactions() {
        val id = userId ?: return
        lifecycleScope.launch {
            val latest = withContext(Dispatchers.IO) {
                firebaseRepository.getTransactionsByUserId(id)
                    .sortedByDescending { it.transactionDate }
            }
            transactions = latest
            transactionAdapter.setData(transactions)
        }
    }
}