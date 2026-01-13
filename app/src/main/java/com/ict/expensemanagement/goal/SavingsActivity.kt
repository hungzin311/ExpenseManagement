package com.ict.expensemanagement.goal

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.R
import com.ict.expensemanagement.adapter.GoalAdapter
import com.ict.expensemanagement.data.entity.SavingsGoal
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivitySavingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class SavingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavingsBinding
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private lateinit var goalAdapter: GoalAdapter
    private var currentSavings: Double = 0.0
    private var goals: List<SavingsGoal> = emptyList()
    private val firebaseRepository = FirebaseRepository()

    companion object {
        private const val REQUEST_ADD_GOAL = 1001
        private const val PREFS_ROLLOVER = "savings_rollover_prefs"
        private const val KEY_LAST_MONTH = "last_month_key"
        private const val KEY_LAST_REMAINING_GOAL_MONTH = "last_remaining_goal_month_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        setupRecyclerView()
        setupClickListeners()
        updateMonthYear()
        handleMonthRolloverIfNeeded()
        fetchSavingsData()
    }

    private fun monthKey(date: LocalDate): String = "${date.year}-${date.monthValue}"
    private fun handleMonthRolloverIfNeeded() {
        val id = userId ?: return
        val prefs = getSharedPreferences(PREFS_ROLLOVER, MODE_PRIVATE)

        val today = LocalDate.now()
        val currentKey = monthKey(today)
        val lastKey = prefs.getString(KEY_LAST_MONTH, null)

        // First run: initialize key_last_month (save trong share preference)
        if (lastKey == null) {
            prefs.edit().putString(KEY_LAST_MONTH, currentKey).apply()
            return
        }

        // No rollover
        if (lastKey == currentKey) return

        val prevMonth = today.minusMonths(1)
        val prevMonthKey = monthKey(prevMonth)

        // key này để xác dinh da xu ly thang truoc chua
        if (prefs.getString(KEY_LAST_REMAINING_GOAL_MONTH, null) == prevMonthKey) {
            prefs.edit().putString(KEY_LAST_MONTH, currentKey).apply()
            return
        }

        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
        val remainingTitle = "Remaining ${prevMonth.format(monthYearFormatter)}"
        val prevMonthEnd = today.withDayOfMonth(1).minusDays(1) // last day of previous month

        lifecycleScope.launch {
            try {
                val created = withContext(Dispatchers.IO) {
                    val transactions = firebaseRepository.getTransactionsByUserId(id)
                    val prevFiltered = transactions.filter { tran ->
                        try {
                            val d = LocalDate.parse(tran.transactionDate)
                            d.year == prevMonth.year && d.monthValue == prevMonth.monthValue
                        } catch (_: Exception) {
                            false
                        }
                    }

                    val totalIncome = prevFiltered.filter { it.amount > 0 }.sumOf { it.amount }
                    val totalExpenses = prevFiltered.filter { it.amount < 0 }.sumOf { abs(it.amount) }
                    val prevSavings = totalIncome - totalExpenses

                    if (prevSavings <= 0.0) return@withContext false

                    // Avoid duplicates if the goal title already exists
                    val existing = firebaseRepository.getGoalByTitle(id, remainingTitle)
                    if (existing != null) return@withContext false

                    val goal = SavingsGoal(
                        id = 0,
                        title = remainingTitle,
                        targetAmount = prevSavings,
                        currentAmount = prevSavings,
                        iconResId = R.drawable.ic_box,
                        userId = id
                    )

                    val goalId = firebaseRepository.insertGoal(goal)

                    // Record the deposit on the last day of the previous month
                    val deposit = Transaction(
                        id = -1,
                        label = "Goal Deposit - $remainingTitle",
                        amount = -prevSavings,
                        description = "Auto rollover from ${prevMonth.format(monthYearFormatter)}",
                        transactionDate = prevMonthEnd.toString(),
                        userId = id,
                        code = "",
                        linkedGoalId = goalId
                    ).apply { setCode() }
                    firebaseRepository.insertTransaction(deposit)
                    true
                }

                // Mark rollover processed (even if nothing was created)
                prefs.edit()
                    .putString(KEY_LAST_MONTH, currentKey)
                    .putString(KEY_LAST_REMAINING_GOAL_MONTH, prevMonthKey)
                    .apply()

                if (created) {
                    fetchSavingsData()
                }
            } catch (_: Exception) {
                prefs.edit().putString(KEY_LAST_MONTH, currentKey).apply()
            }
        }
    }

    private fun setupRecyclerView() {
        goalAdapter = GoalAdapter(emptyList()) { goal ->
            showEditGoalDialog(goal)
        }
        binding.goalsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.goalsRecyclerView.adapter = goalAdapter
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.moreOptionsButton.setOnClickListener {
            val intent = Intent(this, AddGoalActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_GOAL)
        }

        binding.addGoalFab.setOnClickListener {
            val intent = Intent(this, AddGoalActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_GOAL)
        }
    }

    private fun updateGoalTotals(totalCurrent: Double, totalTarget: Double) {
        binding.goalCurrentValue.text = "$${String.Companion.format(Locale.US, "%,.0f", totalCurrent)}"
        binding.goalTargetValue.text = "$${String.Companion.format(Locale.US, "%,.0f", totalTarget)}"
        val percent = if (totalTarget > 0) {
            ((totalCurrent / totalTarget) * 100).coerceIn(0.0, 100.0)
        } else 0.0
        binding.goalProgress.setProgress(percent.toInt(), true)
    }

    @SuppressLint("SetTextI18n")
    private fun updateMonthYear() {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        binding.monthYearText.text = monthFormat.format(calendar.time)
    }

    private fun fetchSavingsData() {
        val id = userId ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val transactions = firebaseRepository.getTransactionsByUserId(id)
                val today = LocalDate.now()
                val monthFiltered = transactions.filter { tran ->
                    try {
                        val d = LocalDate.parse(tran.transactionDate)
                        d.year == today.year && d.monthValue == today.monthValue
                    } catch (e: Exception) {
                        false
                    }
                }
                // saving in this month
                val totalIncome = monthFiltered.filter { it.amount > 0 }.sumOf { it.amount }
                val totalExpenses = monthFiltered.filter { it.amount < 0 }.sumOf { abs(it.amount) }
                val savings = totalIncome - totalExpenses
                // current goal amount
                val loadedGoals = firebaseRepository.getGoalsByUserId(id)
                val totalTarget = loadedGoals.sumOf { it.targetAmount }
                val totalGoalCurrent = loadedGoals.sumOf { it.currentAmount }
                SavingsUiState(
                    currentSavings = savings,
                    goals = loadedGoals,
                    totalGoalCurrent = totalGoalCurrent,
                    totalTarget = totalTarget
                )
            }
            currentSavings = result.currentSavings
            goals = result.goals
            binding.currentSavingsAmount.text = "$${String.Companion.format(Locale.US, "%,.0f", currentSavings)}"
            updateGoalTotals(result.totalGoalCurrent, result.totalTarget)
            goalAdapter.updateGoals(goals)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchSavingsData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_GOAL && resultCode == RESULT_OK) {
            fetchSavingsData()
        }
    }

    private fun showEditGoalDialog(goal: SavingsGoal) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (goal.currentAmount > 0) String.Companion.format(Locale.US, "%.0f", goal.currentAmount) else "")
            setSelection(text.length)
        }

        val container = FrameLayout(this).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Current amount for \"${goal.title}\"")
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                val raw = input.text.toString().replace(",", "").trim()
                val value = raw.toDoubleOrNull()
                when {
                    value == null || value < 0 -> {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                    value > goal.targetAmount -> {
                        Toast.makeText(this, "Current amount cannot exceed target", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val updatedGoal = goal.copy(currentAmount = value)
                        lifecycleScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    firebaseRepository.updateGoal(updatedGoal)
                                }
                                recordGoalAdjustment(goal, value) {
                                    fetchSavingsData()
                                    dialog.dismiss()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@SavingsActivity, "Failed to update goal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNeutralButton("Delete") { dialog, _ ->
                dialog.dismiss()
                showDeleteGoalConfirmation(goal)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDeleteGoalConfirmation(goal: SavingsGoal) {
        if (goal.id == 0) {
            Toast.makeText(this, "Goal not synced yet", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete goal")
            .setMessage("Are you sure you want to delete \"${goal.title}\"?")
            .setPositiveButton("Delete") { dialog, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // Refund money currently allocated to this goal back into savings
                            if (goal.currentAmount > 0) {
                                val refund = Transaction(
                                    id = -1,
                                    label = "Goal Deleted - ${goal.title}",
                                    amount = goal.currentAmount,
                                    description = "Refund from deleted goal \"${goal.title}\"",
                                    transactionDate = LocalDate.now().toString(),
                                    userId = userId!!,
                                    code = "",
                                    linkedGoalId = null
                                ).apply { setCode() }
                                firebaseRepository.insertTransaction(refund)
                            }
                            firebaseRepository.deleteGoal(goal.id)
                        }
                        Toast.makeText(this@SavingsActivity, "Goal deleted", Toast.LENGTH_SHORT).show()
                        fetchSavingsData()
                    } catch (e: Exception) {
                        Toast.makeText(this@SavingsActivity, "Failed to delete goal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun recordGoalAdjustment(goal: SavingsGoal, newAmount: Double, onComplete: () -> Unit) {
        val previousAmount = goal.currentAmount
        val delta = newAmount - previousAmount
        if (delta == 0.0 || userId == null) {
            onComplete()
            return
        }

        val adjustmentType = if (delta > 0) "Goal Deposit" else "Goal Withdrawal"
        val transaction = Transaction(
            id = -1,
            label = "$adjustmentType - ${goal.title}",
            amount = -delta,
            description = "Goal adjustment for ${goal.title}",
            transactionDate = LocalDate.now().toString(),
            userId = userId!!,
            code = "",
            linkedGoalId = goal.id
        ).apply { setCode() }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                firebaseRepository.insertTransaction(transaction)
            }
            onComplete()
        }
    }

    private data class SavingsUiState(
        val currentSavings: Double,
        val goals: List<SavingsGoal>,
        val totalGoalCurrent: Double,
        val totalTarget: Double
    )
}