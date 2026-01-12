package com.ict.expensemanagement.transaction

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.R
import com.ict.expensemanagement.adapter.HomePagerAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityTotalExpensesBinding
import com.ict.expensemanagement.stats.CategoriesFragment
import com.ict.expensemanagement.stats.SpendsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class TotalExpensesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTotalExpensesBinding
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private lateinit var transactions: List<Transaction>
    private lateinit var viewPagerAdapter: HomePagerAdapter
    private val calendar = Calendar.getInstance()
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var isDaySelected: Boolean = false
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTotalExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        setupCalendar()
        setupTabs()
        setupBackButton()

        // Setup ViewPager page change listener to ensure fragments are created
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // When Categories tab is selected, ensure it has data
                if (position == 1) {
                    val categoriesFragment = viewPagerAdapter.getFragment(1) as? CategoriesFragment
                    categoriesFragment?.setTransactions(transactions)
                }
            }
        })

        fetchTransactions()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupCalendar() {
        val monthFormat = SimpleDateFormat("MMMM - yyyy", Locale.US)
        binding.monthYearText.text = monthFormat.format(calendar.time)

        binding.prevMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            binding.monthYearText.text = monthFormat.format(calendar.time)
            // Reset to first day when changing month, or today if viewing current month
            val currentDay = Calendar.getInstance()
            if (calendar.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == currentDay.get(Calendar.MONTH)) {
                selectedDay = currentDay.get(Calendar.DAY_OF_MONTH)
            } else {
                selectedDay = 1
            }
            isDaySelected = false
            updateCalendarDays()
            fetchTransactions()
        }

        binding.nextMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            binding.monthYearText.text = monthFormat.format(calendar.time)
            // Reset to first day when changing month, or today if viewing current month
            val currentDay = Calendar.getInstance()
            if (calendar.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == currentDay.get(Calendar.MONTH)) {
                selectedDay = currentDay.get(Calendar.DAY_OF_MONTH)
            } else {
                selectedDay = 1
            }
            isDaySelected = false
            updateCalendarDays()
            fetchTransactions()
        }

        updateCalendarDays()
    }

    private fun updateCalendarDays() {
        val calendarGrid = binding.calendarGrid
        calendarGrid.removeAllViews()

        val firstDayOfMonth = calendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)

        // Vietnam calendar: Monday is first day of week
        // If Sunday, offset = 6; else offset = firstDayOfWeek - 2
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Previous month trailing days
        val prevMonth = calendar.clone() as Calendar
        prevMonth.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until startOffset) {
            val dayView = TextView(this)
            val prevMonthDay = daysInPrevMonth - startOffset + i + 1
            dayView.text = prevMonthDay.toString()
            dayView.textSize = 14f
            dayView.gravity = Gravity.CENTER
            dayView.setPadding(8, 8, 8, 8)
            dayView.setTextColor(ContextCompat.getColor(this, R.color.placeholderGray))

            val row = 0
            val col = i
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                rowSpec = GridLayout.spec(row, 1f)
                columnSpec = GridLayout.spec(col, 1f)
                setMargins(4, 4, 4, 4)
            }
            dayView.layoutParams = params
            calendarGrid.addView(dayView)
        }

        // Current month days
        for (day in 1..daysInMonth) {
            val dayView = TextView(this)
            dayView.text = day.toString()
            dayView.textSize = 14f
            dayView.gravity = Gravity.CENTER
            dayView.setPadding(8, 8, 8, 8)

            val cellIndex = startOffset + day - 1
            val row = cellIndex / 7
            val col = cellIndex % 7

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                rowSpec = GridLayout.spec(row, 1f)
                columnSpec = GridLayout.spec(col, 1f)
                setMargins(4, 4, 4, 4)
            }
            dayView.layoutParams = params

            val isSelected = isDaySelected && day == selectedDay
            if (isSelected) {
                dayView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                dayView.setBackgroundResource(R.drawable.login_button_background)
            } else {
                dayView.setTextColor(ContextCompat.getColor(this, R.color.darkGray))
                dayView.background = null
            }

            dayView.setOnClickListener {
                selectedDay = day
                isDaySelected = true
                updateCalendarDays()
                fetchTransactions()
            }

            calendarGrid.addView(dayView)
        }

        // Next month leading days
        val totalCells = startOffset + daysInMonth
        val remainingCells = 42 - totalCells // 6 rows * 7 columns
        if (remainingCells > 0) {
            for (i in 1..remainingCells) {
                val dayView = TextView(this)
                dayView.text = i.toString()
                dayView.textSize = 14f
                dayView.gravity = Gravity.CENTER
                dayView.setPadding(8, 8, 8, 8)
                dayView.setTextColor(ContextCompat.getColor(this, R.color.placeholderGray))

                val cellIndex = startOffset + daysInMonth + i - 1
                val row = cellIndex / 7
                val col = cellIndex % 7

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    rowSpec = GridLayout.spec(row, 1f)
                    columnSpec = GridLayout.spec(col, 1f)
                    setMargins(4, 4, 4, 4)
                }
                dayView.layoutParams = params
                calendarGrid.addView(dayView)
            }
        }
    }

    private fun setupTabs() {
        viewPagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 2

        val tabLayout = binding.tabLayout
        tabLayout.tabMode = TabLayout.MODE_FIXED
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        TabLayoutMediator(tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Spends"
                1 -> "Categories"
                else -> ""
            }
        }.attach()
    }

    private fun fetchTransactions() {
        val id = userId ?: return
        lifecycleScope.launch {
            val allTransactions = withContext(Dispatchers.IO) {
                firebaseRepository.getTransactionsByUserId(id)
            }

            // Filter transactions: by day if user selected a day, otherwise by month
            val selectedYear = calendar.get(Calendar.YEAR)
            val selectedMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            val selectedDayLocal = selectedDay

            transactions = allTransactions.filter { transaction ->
                try {
                    val date = LocalDate.parse(transaction.transactionDate)
                    if (isDaySelected) {
                        date.year == selectedYear && date.monthValue == selectedMonth && date.dayOfMonth == selectedDayLocal
                    } else {
                        date.year == selectedYear && date.monthValue == selectedMonth
                    }
                } catch (e: Exception) {
                    false
                }
            }.sortedByDescending { it.transactionDate }

            updateTotalSpend()
            // Update SpendsFragment
            val spendsFragment = viewPagerAdapter.getFragment(0) as? SpendsFragment
            spendsFragment?.setTransactions(transactions)
            // Update CategoriesFragment - ensure fragment is created and view is ready
            binding.viewPager.post {
                val categoriesFragment = viewPagerAdapter.getFragment(1) as? CategoriesFragment
                if (categoriesFragment != null && categoriesFragment.view != null) {
                    categoriesFragment.setTransactions(transactions)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTotalSpend() {
        // Calculate total expenses (amount < 0)
        val totalExpense = transactions
            .filter { it.amount < 0 }
            .map { abs(it.amount) }
            .sum()

        // Calculate total income (amount > 0)
        val totalIncome = transactions
            .filter { it.amount > 0 }
            .map { it.amount }
            .sum()

        // Net = income - expense (what user expects as overall balance in period)
        val netAmount = totalIncome - totalExpense

        // Display total expense in the circle
        binding.totalSpendAmount.text = "$${String.Companion.format(Locale.US, "%,.2f", netAmount)}"
    }
}