package com.ict.expensemanagement.transaction

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.R
import com.ict.expensemanagement.data.entity.Category
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.ActivityAddIncomeBinding
import com.ict.expensemanagement.databinding.DialogAddCategoryBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddIncomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddIncomeBinding
    private lateinit var auth: FirebaseAuth
    private val firebaseRepository = FirebaseRepository()
    private var userId: String? = null
    private val calendar = Calendar.getInstance()
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedCategory: String = "Rewards"
    private var isExpense: Boolean = false
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        isExpense = intent.getBooleanExtra("isExpense", false)

        // Update title and categories if expense
        if (isExpense) {
            binding.titleText.text = "Add Expense"
            binding.incomeTitleLayout.hint = "Expense Title"
            binding.categoryLabel.text = "Expense Category"
            binding.category1Text.text = "Health"
            binding.category2Text.text = "Grocery"
            binding.addIncomeBtn.text = "ADD EXPENSE"
            selectedCategory = "Grocery"
            // Set Grocery as selected (category2)
            updateCategorySelection(binding.category2, true)
            updateCategorySelection(binding.category1, false)
        } else {
            binding.incomeTitleLayout.hint = "Income Title"
            binding.category1Text.text = "Salary"
            binding.category2Text.text = "Rewards"
            selectedCategory = "Rewards"
            // Set Rewards as selected (category2)
            updateCategorySelection(binding.category2, true)
            updateCategorySelection(binding.category1, false)
        }

        setupCalendar()
        setupCategoryButtons()
        setupClickListeners()
        loadCategories()

        // Set default date to today - already set in selectedDate initialization
        updateDateDisplay()

        // Setup date input click listener
        binding.dateInput.setOnClickListener {
            // Scroll to calendar section
            binding.root.findViewById<View>(R.id.calendarGrid)?.let { calendarView ->
                calendarView.requestFocus()
            }
        }
        binding.dateLayout.setOnClickListener {
            binding.dateInput.performClick()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addIncomeBtn.setOnClickListener {
            val title = binding.incomeTitleInput.text.toString().trim()
            val amountText = binding.amountInput.text.toString()
                .replace(",", "")
                .replace("$", "")
                .trim()
            val amount = amountText.toDoubleOrNull()

            when {
                title.isEmpty() -> {
                    binding.incomeTitleLayout.error = "Please enter income title"
                }
                amount == null || amount <= 0 -> {
                    binding.amountLayout.error = "Please enter a valid amount"
                }
                userId == null -> {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Income = positive, Expense = negative
                    val finalAmount = if (isExpense) -amount else amount
                    val description = binding.descriptionInput?.text?.toString()?.trim() ?: selectedCategory

                    val transaction = Transaction(
                        0,
                        title,
                        finalAmount,
                        selectedCategory,
                        selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        userId!!,
                        description
                    )
                    transaction.setCode()
                    insert(transaction)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupCalendar() {
        val monthFormat = SimpleDateFormat("MMMM - yyyy", Locale.US)
        binding.monthYearText.text = monthFormat.format(calendar.time)

        // Initialize selectedDay to today if viewing current month
        val currentDay = Calendar.getInstance()
        if (calendar.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == currentDay.get(Calendar.MONTH)) {
            selectedDay = currentDay.get(Calendar.DAY_OF_MONTH)
        } else {
            selectedDay = 1
        }

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
            updateCalendarDays()
            updateSelectedDateFromDay()
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
            updateCalendarDays()
            updateSelectedDateFromDay()
        }

        updateCalendarDays()
        updateSelectedDateFromDay()
    }

    private fun updateSelectedDateFromDay() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val day = if (selectedDay > daysInMonth) daysInMonth else selectedDay
        selectedDate = LocalDate.of(year, month, day)
        updateDateDisplay()
    }

    private fun updateDateDisplay() {
        binding.dateInput?.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    private fun updateCalendarDays() {
        val calendarGrid = binding.calendarGrid
        calendarGrid.removeAllViews()

        val firstDayOfMonth = calendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)

        // Vietnam calendar: Monday is first day of week
        // Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ..., Calendar.SATURDAY = 7
        // If Sunday, offset = 6 (put it at the end), else offset = firstDayOfWeek - 2
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = Calendar.getInstance()
        val isCurrentMonth = calendar.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == currentDay.get(Calendar.MONTH)

        // Get previous month's last days for empty cells
        val prevMonth = calendar.clone() as Calendar
        prevMonth.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Add days from previous month (grayed out)
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

        // Add day numbers for current month
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

            // Highlight only selected day (check if it's the month being viewed)
            val isSelected = day == selectedDay

            if (isSelected) {
                dayView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                dayView.setBackgroundResource(R.drawable.login_button_background)
            } else {
                dayView.setTextColor(ContextCompat.getColor(this, R.color.darkGray))
                dayView.background = null
            }

            dayView.setOnClickListener {
                selectedDay = day
                updateCalendarDays()
                updateSelectedDateFromDay()
                // Scroll to show the date input
                binding.dateInput?.requestFocus()
            }

            calendarGrid.addView(dayView)
        }

        // Fill remaining cells with next month days (if needed)
        val totalCells = startOffset + daysInMonth
        val remainingCells = 42 - totalCells // 6 rows * 7 columns = 42
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

    private fun setupCategoryButtons() {
        binding.category1.setOnClickListener {
            selectedCategory = if (isExpense) "Health" else "Salary"
            updateCategorySelection(binding.category1, true)
            updateCategorySelection(binding.category2, false)
            clearDynamicCategorySelection()
            updateDescriptionFromCategory()
        }

        binding.category2.setOnClickListener {
            selectedCategory = if (isExpense) "Grocery" else "Rewards"
            updateCategorySelection(binding.category2, true)
            updateCategorySelection(binding.category1, false)
            clearDynamicCategorySelection()
            updateDescriptionFromCategory()
        }

        binding.addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun updateDescriptionFromCategory() {
        binding.descriptionInput?.setText(selectedCategory)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadCategories() {
        if (userId == null) return

        GlobalScope.launch {
            val categoryType = if (isExpense) "Expense" else "Income"
            val categories = firebaseRepository.getCategoriesByType(userId!!, categoryType)

            runOnUiThread {
                displayCategories(categories)
            }
        }
    }

    private fun displayCategories(categories: List<Category>) {
        val container = binding.categoriesContainer
        container.removeAllViews()
        var matched = false

        categories.forEachIndexed { index, category ->
            val categoryCard = createCategoryCard(category.name)
            container.addView(categoryCard)

            // Highlight if currently selected
            if (category.name == selectedCategory) {
                updateCategorySelection(categoryCard, true)
                matched = true
            } else {
                updateCategorySelection(categoryCard, false)
            }
        }

        // If current selection not found and list not empty, select first
        if (!matched && categories.isNotEmpty()) {
            val first = container.getChildAt(0) as? MaterialCardView
            if (first != null) {
                selectedCategory = categories[0].name
                updateCategorySelection(first, false) // initial state: all unselected
            }
        }
    }

    private fun createCategoryCard(categoryName: String): MaterialCardView {
        val cardView = MaterialCardView(this)
        val density = resources.displayMetrics.density
        val params = LinearLayout.LayoutParams(
            (100 * density).toInt(),
            (80 * density).toInt()
        ).apply {
            marginEnd = (12 * density).toInt()
        }
        cardView.layoutParams = params
        cardView.radius = 12f * density
        cardView.cardElevation = 2f
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
        cardView.isClickable = true
        cardView.isFocusable = true
        cardView.foreground = getDrawable(android.R.drawable.list_selector_background)
        cardView.strokeColor = ContextCompat.getColor(this, R.color.lightGray)
        cardView.strokeWidth = (1 * density).toInt()

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.gravity = Gravity.CENTER
        linearLayout.setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())

        val textView = TextView(this)
        textView.text = categoryName
        textView.setTextColor(ContextCompat.getColor(this, R.color.darkGray))
        textView.textSize = 14f
        textView.setTypeface(null, Typeface.BOLD)

        linearLayout.addView(textView)
        cardView.addView(linearLayout)

        cardView.setOnClickListener {
            selectedCategory = categoryName
            clearAllCategorySelection()
            updateCategorySelection(cardView, true)
            updateDescriptionFromCategory()
        }

        return cardView
    }

    private fun clearAllCategorySelection() {
        updateCategorySelection(binding.category1, false)
        updateCategorySelection(binding.category2, false)
        clearDynamicCategorySelection()
    }

    private fun clearDynamicCategorySelection() {
        val container = binding.categoriesContainer
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is MaterialCardView) {
                updateCategorySelection(view, false)
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.addCategoryBtn.setOnClickListener {
            val categoryName = dialogBinding.categoryNameInput.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryType = if (isExpense) "Expense" else "Income"

            GlobalScope.launch {
                // Check if category already exists
                val existingCategory = firebaseRepository.getCategoryByName(userId!!, categoryType, categoryName)
                if (existingCategory != null) {
                    runOnUiThread {
                        Toast.makeText(this@AddIncomeActivity, "Category already exists", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Insert new category
                val newCategory = Category(0, categoryName, categoryType, userId!!)
                firebaseRepository.insertCategory(newCategory)

                runOnUiThread {
                    Toast.makeText(this@AddIncomeActivity, "Category added successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadCategories()
                }
            }
        }

        dialog.show()
    }

    private fun updateCategorySelection(cardView: MaterialCardView, selected: Boolean) {
        if (selected) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryBlue))
            cardView.strokeColor = ContextCompat.getColor(this, R.color.primaryBlue)
            val linearLayout = cardView.getChildAt(0) as? LinearLayout
            val textView = linearLayout?.getChildAt(0) as? TextView
            textView?.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
            cardView.strokeColor = ContextCompat.getColor(this, R.color.lightGray)
            val linearLayout = cardView.getChildAt(0) as? LinearLayout
            val textView = linearLayout?.getChildAt(0) as? TextView
            textView?.setTextColor(ContextCompat.getColor(this, R.color.placeholderGray))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun insert(transaction: Transaction) {
        GlobalScope.launch {
            firebaseRepository.insertTransaction(transaction)

            runOnUiThread {
                Toast.makeText(this@AddIncomeActivity, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}