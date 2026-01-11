package com.ict.expensemanagement

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ict.expensemanagement.databinding.ActivityAddBudgetBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddBudgetBinding
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopBar()
        setupMonthPicker()
        setupSaveButton()

        updateMonthLabel()
    }

    private fun setupTopBar() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupMonthPicker() {
        binding.monthPickerCard.setOnClickListener { showMonthPicker() }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val amountText = binding.amountInput.text.toString()
                .replace(",", "")
                .replace("$", "")
                .trim()
            val amount = amountText.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                binding.amountLayout.error = "Please enter a valid amount"
                return@setOnClickListener
            } else {
                binding.amountLayout.error = null
            }

            saveBudget(amount)
        }
    }

    private fun showMonthPicker() {
        // Force day = 1 to keep month-only selection
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = 1

        val dialog = DatePickerDialog(
            this,
            { _, pickedYear, pickedMonth, _ ->
                calendar.set(Calendar.YEAR, pickedYear)
                calendar.set(Calendar.MONTH, pickedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                updateMonthLabel()
            },
            year,
            month,
            dayOfMonth
        )
        dialog.show()
    }

    private fun updateMonthLabel() {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
        binding.monthValue.text = formatter.format(calendar.time)
    }

    private fun saveBudget(amount: Double) {
        val formatterKey = SimpleDateFormat("yyyyMM", Locale.US)
        val key = "budget_${formatterKey.format(calendar.time)}"

        val prefs = getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat(key, amount.toFloat()).apply()

        Toast.makeText(this, "Budget saved for ${binding.monthValue.text}", Toast.LENGTH_SHORT).show()
        finish()
    }
}