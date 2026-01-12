package com.ict.expensemanagement.transaction

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.R
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityDetailedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DetailedActivity : AppCompatActivity() {
    private lateinit var transaction : Transaction
    private lateinit var binding : ActivityDetailedBinding
    private val firebaseRepository = FirebaseRepository()
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailedBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val labelInput = binding.labelInput
        val amountInput = binding.amountInput
        val labelLayout = binding.labelLayout
        val amountLayout = binding.amountLayout
        val dateLayout = binding.dateLayout
        val descriptionInput = binding.descriptionInput
        val updateTransactionBtn = binding.updateTransactionBtn
        val dateInput = binding.dateInput
        val rootView = binding.rootView
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        dateInput.setOnClickListener {
            showDatePickerDialog()
        }

        transaction = intent.getSerializableExtra("transaction") as Transaction

        if (transaction != null) {
            labelInput.setText(transaction.label)
            amountInput.setText(DecimalFormat("0.##########").format(transaction.amount))
            descriptionInput.setText(transaction.description)
            dateInput.setText(transaction.transactionDate.toString())
        }

        rootView.setOnClickListener {
            this.window.decorView.clearFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        labelInput.addTextChangedListener {
            updateTransactionBtn.visibility = View.VISIBLE
            if (it!!.isNotEmpty())
                labelLayout.error = null
        }

        amountInput.addTextChangedListener {
            updateTransactionBtn.visibility = View.VISIBLE
            if (it!!.isNotEmpty())
                amountLayout.error = null
        }

        descriptionInput.addTextChangedListener {
            updateTransactionBtn.visibility = View.VISIBLE
        }

        dateInput.addTextChangedListener {
            updateTransactionBtn.visibility = View.VISIBLE
        }

        updateTransactionBtn.setOnClickListener {
            val label = labelInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()
            val description = descriptionInput.text.toString()
            val date = dateInput.text.toString()

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = LocalDate.parse(date, formatter).toString()

            if (label.isEmpty())
                labelLayout.error = "Please enter a valid label"

            else if (amount == null)
                amountLayout.error = "Please enter a valid amount"

            else if (date.isEmpty())
                dateLayout.error = "Please enter a valid date"

            else {
                val transaction = Transaction(
                    transaction.id,
                    label,
                    amount,
                    description,
                    localDate,
                    userId!!,
                    transaction.code
                )
                transaction.setCode()
                update(transaction)
            }

        }
        val closeBtn = findViewById<ImageButton>(R.id.closeBtn)
        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dateInput = binding.dateInput

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                var selectedDate: String

                if (month < 10 && dayOfMonth < 10)
                    selectedDate = "$year-0${month + 1}-0$dayOfMonth"
                else if (month < 10)
                    selectedDate = "$year-0${month + 1}-$dayOfMonth"
                else if (dayOfMonth < 10)
                    selectedDate = "$year-0${month + 1}-$dayOfMonth"
                else
                    selectedDate = "$year-${month + 1}-$dayOfMonth"

                dateInput.setText(selectedDate)
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun update(transaction: Transaction) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                firebaseRepository.updateTransaction(transaction)
            }
            finish()
        }
    }
}