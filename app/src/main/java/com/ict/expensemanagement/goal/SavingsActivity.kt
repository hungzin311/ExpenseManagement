package com.ict.expensemanagement.goal

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.R
import com.ict.expensemanagement.adapter.GoalAdapter
import com.ict.expensemanagement.data.entity.SavingsGoal
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivitySavingsBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
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
    private val goalsPrefsName = "goals_prefs"
    private val goalsKey = "goals_list"
    private val firebaseRepository = FirebaseRepository()

    companion object {
        private const val REQUEST_ADD_GOAL = 1001
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        setupRecyclerView()
        setupClickListeners()
        updateMonthYear()
        fetchSavingsData()
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchSavingsData() {
        GlobalScope.launch {
            if (userId != null) {
                val transactions = firebaseRepository.getTransactionsByUserId(userId!!)
                val today = LocalDate.now()
                val monthFiltered = transactions.filter { tran ->
                    try {
                        val d = LocalDate.parse(tran.transactionDate)
                        d.year == today.year && d.monthValue == today.monthValue
                    } catch (e: Exception) {
                        false
                    }
                }
                val totalIncome = monthFiltered.filter { it.amount > 0 }.sumOf { it.amount }
                val totalExpenses = monthFiltered.filter { it.amount < 0 }.sumOf { abs(it.amount) }
                currentSavings = totalIncome - totalExpenses

                goals = loadGoalsFromPrefs()
                val totalTarget = goals.sumOf { it.targetAmount }
                val totalGoalCurrent = goals.sumOf { it.currentAmount }

                runOnUiThread {
                    binding.currentSavingsAmount.text = "$${String.Companion.format(Locale.US, "%,.0f", currentSavings)}"
                    updateGoalTotals(totalGoalCurrent, totalTarget)
                    goalAdapter.updateGoals(goals)
                }
            }
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
                        val updatedGoals = goals.map {
                            if (it.id == goal.id) it.copy(currentAmount = value) else it
                        }
                        saveGoalsToPrefs(updatedGoals)
                        fetchSavingsData()
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ---- Persistence helpers ----
    private fun loadGoalsFromPrefs(): List<SavingsGoal> {
        val prefs = getSharedPreferences(goalsPrefsName, MODE_PRIVATE)
        val json = prefs.getString(goalsKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SavingsGoal>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SavingsGoal(
                        id = o.optInt("id", 0),
                        title = o.optString("title", ""),
                        targetAmount = o.optDouble("targetAmount", 0.0),
                        currentAmount = o.optDouble("currentAmount", 0.0),
                        iconResId = o.optInt("iconResId", R.drawable.ic_box),
                        userId = o.optString("userId", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveGoalsToPrefs(list: List<SavingsGoal>) {
        val prefs = getSharedPreferences(goalsPrefsName, MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { goal ->
            val o = JSONObject().apply {
                put("id", goal.id)
                put("title", goal.title)
                put("targetAmount", goal.targetAmount)
                put("currentAmount", goal.currentAmount)
                put("iconResId", goal.iconResId)
                put("userId", goal.userId)
            }
            arr.put(o)
        }
        prefs.edit().putString(goalsKey, arr.toString()).apply()
    }
}