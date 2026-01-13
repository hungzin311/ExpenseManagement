package com.ict.expensemanagement.goal

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.data.entity.SavingsGoal
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityAddGoalBinding
import com.ict.expensemanagement.databinding.DialogContributionTypeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class AddGoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddGoalBinding
    private lateinit var auth: FirebaseAuth
    private val firebaseRepository = FirebaseRepository()
    private var userId: String? = null
    private var selectedDeadline: Calendar = Calendar.getInstance()
    private var selectedContributionType: String = "Yearly"
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        clearInputs()
        setupClickListeners()
        setupContributionType()
        setupDeadline()
    }

    private fun clearInputs() {
        binding.goalTitleInput.setText("")
        binding.amountInput.setText("")
        binding.contributionTypeInput.setText("Yearly", false)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        binding.deadlineInput.setText(dateFormat.format(selectedDeadline.time))
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addGoalButton.setOnClickListener {
            saveGoal()
        }

        // Clear placeholders when screen opens
        binding.goalTitleInput.setText("")
        binding.amountInput.setText("")
        binding.contributionTypeInput.setText("Yearly", false)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        binding.deadlineInput.setText(dateFormat.format(selectedDeadline.time))
    }

    private fun setupContributionType() {
        binding.contributionTypeInput.setText("Yearly", false)
        binding.contributionTypeInput.setOnClickListener {
            showContributionTypeDialog()
        }
        binding.contributionTypeLayout.setEndIconOnClickListener {
            showContributionTypeDialog()
        }
    }

    private fun showContributionTypeDialog() {
        val dialogBinding = DialogContributionTypeBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        // Set selected type
        updateContributionTypeSelection(dialogBinding, selectedContributionType)

        dialogBinding.weeklyOption.setOnClickListener {
            selectedContributionType = "Weekly"
            updateContributionTypeSelection(dialogBinding, "Weekly")
            binding.contributionTypeInput.setText("Weekly", false)
            dialog.dismiss()
        }

        dialogBinding.monthlyOption.setOnClickListener {
            selectedContributionType = "Monthly"
            updateContributionTypeSelection(dialogBinding, "Monthly")
            binding.contributionTypeInput.setText("Monthly", false)
            dialog.dismiss()
        }

        dialogBinding.yearlyOption.setOnClickListener {
            selectedContributionType = "Yearly"
            updateContributionTypeSelection(dialogBinding, "Yearly")
            binding.contributionTypeInput.setText("Yearly", false)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(R.color.transparent)
        dialog.show()
    }

    private fun updateContributionTypeSelection(binding: DialogContributionTypeBinding, selected: String) {
        // Reset all
        binding.weeklyCheck.visibility = View.GONE
        binding.monthlyCheck.visibility = View.GONE
        binding.yearlyCheck.visibility = View.GONE

        // Reset text colors using the specific IDs
        binding.weeklyText.setTextColor(ContextCompat.getColor(this, com.ict.expensemanagement.R.color.darkGray))
        binding.monthlyText.setTextColor(ContextCompat.getColor(this, com.ict.expensemanagement.R.color.darkGray))
        binding.yearlyText.setTextColor(ContextCompat.getColor(this, com.ict.expensemanagement.R.color.darkGray))

        when (selected) {
            "Weekly" -> {
                binding.weeklyCheck.visibility = View.VISIBLE
                binding.weeklyText.setTextColor(ContextCompat.getColor(this, com.ict.expensemanagement.R.color.primaryBlue))
            }
            "Monthly" -> {
                binding.monthlyCheck.visibility = View.VISIBLE
                binding.monthlyText.setTextColor(ContextCompat.getColor(this, com.ict.expensemanagement.R.color.primaryBlue))
            }
            "Yearly" -> {
                binding.yearlyCheck.visibility = View.VISIBLE
                binding.yearlyText.setTextColor(ContextCompat.getColor(this, com.ict.expensemanagement.R.color.primaryBlue))
            }
        }
    }

    private fun setupDeadline() {
        // Set default deadline to 2 years from now
        selectedDeadline.add(Calendar.YEAR, 2)
        selectedDay = selectedDeadline.get(Calendar.DAY_OF_MONTH)
        updateDeadlineDisplay()

        binding.deadlineInput.setOnClickListener {
            openDeadlinePicker()
        }

        binding.deadlineLayout.setEndIconOnClickListener {
            openDeadlinePicker()
        }
    }

    @SuppressLint("SetTextI18n")

    /**
     * Open different picker based on selected contribution type.
     * - Daily  -> full calendar (day)
     * - Weekly -> list of weeks in current month (future weeks only)
     * - Monthly-> list of future months
     * - Yearly -> list of future years
     */
    private fun openDeadlinePicker() {
        when (selectedContributionType) {
            "Weekly" -> showYearForWeekPicker()
            "Monthly" -> showYearForMonthPicker()
            "Yearly" -> showYearPickerDialog()
            else -> showYearForWeekPicker()
        }
    }

    private data class WeekItem(val label: String, val endDate: Calendar)

    private fun showWeekPickerDialog(year: Int) {
        val baseCal = selectedDeadline.clone() as Calendar
        baseCal.set(Calendar.YEAR, year)
        val df = SimpleDateFormat("dd/MM", Locale.US)
        val monthDf = SimpleDateFormat("MMMM yyyy", Locale.US)

        // Start from first day of month
        baseCal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = baseCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val weeks = mutableListOf<WeekItem>()
        var day = 1
        var weekIndex = 1
        while (day <= daysInMonth) {
            val startCal = baseCal.clone() as Calendar
            startCal.set(Calendar.DAY_OF_MONTH, day)
            val endCal = baseCal.clone() as Calendar
            val endDay = min(day + 6, daysInMonth)
            endCal.set(Calendar.DAY_OF_MONTH, endDay)

            // ignore past weeks
            val todayCal = Calendar.getInstance()
            if (!endCal.before(todayCal)) {
                val label = "Week $weekIndex (${df.format(startCal.time)} - ${df.format(endCal.time)})"
                weeks.add(WeekItem(label, endCal))
            }

            weekIndex++
            day += 7
        }

        if (weeks.isEmpty()) {
            Toast.makeText(this, "No future weeks in ${monthDf.format(baseCal.time)}", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = weeks.map { it.label }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(monthDf.format(baseCal.time))
            .setItems(labels) { dialog, which ->
                val item = weeks[which]
                selectedDeadline = item.endDate
                selectedDay = selectedDeadline.get(Calendar.DAY_OF_MONTH)
                updateDeadlineDisplay()
                dialog.dismiss()
            }
            .show()
    }

    private fun showMonthPickerDialog(year: Int) {
        val monthDf = SimpleDateFormat("MMMM yyyy", Locale.US)

        val base = selectedDeadline.clone() as Calendar
        base.set(Calendar.YEAR, year)
        base.set(Calendar.DAY_OF_MONTH, 1)

        val months = mutableListOf<Calendar>()
        val labels = mutableListOf<String>()
        val temp = base.clone() as Calendar
        // next 12 months including base
        repeat(12) {
            months.add(temp.clone() as Calendar)
            labels.add(monthDf.format(temp.time))
            temp.add(Calendar.MONTH, 1)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Select month")
            .setItems(labels.toTypedArray()) { dialog, which ->
                val cal = months[which]
                // set deadline to last day of that month
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                selectedDeadline = cal
                selectedDay = selectedDeadline.get(Calendar.DAY_OF_MONTH)
                updateDeadlineDisplay()
                dialog.dismiss()
            }
            .show()
    }

    private fun showYearPickerDialog() {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (0..10).map { thisYear + it } // next 11 years
        val labels = years.map { it.toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select year")
            .setItems(labels) { dialog, which ->
                val year = years[which]
                val cal = selectedDeadline.clone() as Calendar
                cal.set(Calendar.YEAR, year)
                // keep month/day but ensure it's a valid date
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (cal.get(Calendar.DAY_OF_MONTH) > maxDay) {
                    cal.set(Calendar.DAY_OF_MONTH, maxDay)
                }
                selectedDeadline = cal
                selectedDay = selectedDeadline.get(Calendar.DAY_OF_MONTH)
                updateDeadlineDisplay()
                dialog.dismiss()
            }
            .show()
    }

    private fun showYearForMonthPicker() {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (0..10).map { thisYear + it }
        val labels = years.map { it.toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select year")
            .setItems(labels) { dialog, which ->
                val year = years[which]
                showMonthPickerDialog(year)
                dialog.dismiss()
            }
            .show()
    }

    private fun showYearForWeekPicker() {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (0..10).map { thisYear + it }
        val labels = years.map { it.toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select year")
            .setItems(labels) { dialog, which ->
                val year = years[which]
                showWeekPickerDialog(year)
                dialog.dismiss()
            }
            .show()
    }

    private fun updateDeadlineDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        binding.deadlineInput.setText(dateFormat.format(selectedDeadline.time))
    }

    private fun saveGoal() {
        val title = binding.goalTitleInput.text.toString().trim()
        val amountText = binding.amountInput.text.toString()
            .replace(",", "")
            .replace("$", "")
            .trim()
        val amount = amountText.toDoubleOrNull()
        val currentAmountText = binding.currentAmountInput.text.toString()
            .replace(",", "")
            .replace("$", "")
            .trim()
        val currentAmount = if (currentAmountText.isBlank()) 0.0 else currentAmountText.toDoubleOrNull()
        when {
            title.isEmpty() -> {
                binding.goalTitleLayout.error = "Please enter goal title"
                return
            }
            amount == null || amount <= 0 -> {
                binding.amountLayout.error = "Please enter a valid amount"
                return
            }
            currentAmount == null || currentAmount < 0 -> {
                binding.currentAmountLayout.error = "Please enter a valid current amount"
                return
            }
            currentAmount > amount -> {
                binding.currentAmountLayout.error = "Current amount cannot exceed target"
                return
            }
            userId == null -> {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                // Create new goal
                val goal = SavingsGoal(
                    id = 0, // Will be generated by database
                    title = title,
                    targetAmount = amount,
                    currentAmount = currentAmount,
                    iconResId = com.ict.expensemanagement.R.drawable.ic_box,
                    userId = userId!!
                )

                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            firebaseRepository.insertGoal(goal)
                        }
                        Toast.makeText(this@AddGoalActivity, "Goal added successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@AddGoalActivity, "Failed to add goal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}