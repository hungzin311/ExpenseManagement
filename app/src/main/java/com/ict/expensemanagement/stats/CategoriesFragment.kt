package com.ict.expensemanagement.stats

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.ict.expensemanagement.R
import com.ict.expensemanagement.adapter.TransactionAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.databinding.FragmentCategoriesBinding
import java.util.Locale
import kotlin.math.abs

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private var transactionAdapter: TransactionAdapter? = null
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var transactions: List<Transaction> = emptyList()

    // Color palette for Income chart - Green/Blue tones (money coming in)
    private val incomeColors = listOf(
        Color.parseColor("#10B981"), // Emerald green
        Color.parseColor("#3B82F6"), // Blue
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#8B5CF6"), // Purple
        Color.parseColor("#6366F1"), // Indigo
        Color.parseColor("#14B8A6"), // Teal
        Color.parseColor("#0EA5E9"), // Sky blue
        Color.parseColor("#22C55E"), // Green
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#3B82F6")  // Blue
    )

    // Color palette for Expense chart - Red/Orange tones (money going out)
    private val expenseColors = listOf(
        Color.parseColor("#EF4444"), // Red
        Color.parseColor("#F97316"), // Orange
        Color.parseColor("#F59E0B"), // Amber
        Color.parseColor("#EC4899"), // Pink
        Color.parseColor("#DC2626"), // Dark red
        Color.parseColor("#EA580C"), // Dark orange
        Color.parseColor("#F43F5E"), // Rose
        Color.parseColor("#FB7185"), // Light pink
        Color.parseColor("#FDBA74"), // Light orange
        Color.parseColor("#F87171")  // Light red
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(requireContext())

        binding.recyclerview.adapter = transactionAdapter
        binding.recyclerview.layoutManager = linearLayoutManager
        binding.recyclerview.setHasFixedSize(true)

        setupPieCharts()
    }

    private fun setupPieCharts() {
        // Setup Income Chart
        setupPieChart(binding.incomeChart)

        // Setup Expense Chart
        setupPieChart(binding.expenseChart)
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(false) // We'll show percentages manually
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 60f // Larger hole for semi-circular look
        pieChart.transparentCircleRadius = 65f
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)

        // Start rotation from top (-90 degrees) for semi-circular display
        pieChart.rotationAngle = -90f
        pieChart.isRotationEnabled = false // Disable manual rotation

        pieChart.isHighlightPerTapEnabled = true
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(16f)
        pieChart.setEntryLabelTypeface(Typeface.DEFAULT_BOLD)

        // Center text (optional - can show total or other info)
        pieChart.centerText = ""
        pieChart.setDrawCenterText(false)

        // No extra offsets needed as we're clipping in the layout
    }

    fun setTransactions(transactions: List<Transaction>) {
        this.transactions = transactions
        transactionAdapter?.setData(transactions)
        updateCharts()
    }

    private fun updateCharts() {
        // Update Income Chart
        updateIncomeChart()

        // Update Expense Chart
        updateExpenseChart()
    }

    private fun updateIncomeChart() {
        // Filter only income (positive amounts)
        val incomes = transactions.filter { it.amount > 0 }

        if (incomes.isEmpty()) {
            binding.incomeChart.data = null
            binding.incomeChart.invalidate()
            binding.incomeLegendLayout.removeAllViews()
            return
        }

        // Group by Income Category (description field)
        val categoryMap = incomes.groupBy { transaction ->
            transaction.description?.takeIf { it.isNotBlank() } ?: transaction.label
        }
            .mapValues { (_, transactions) ->
                transactions.sumOf { it.amount }
            }
            .toList()
            .sortedByDescending { it.second }

        val totalIncome = categoryMap.sumOf { it.second }

        if (totalIncome == 0.0) {
            binding.incomeChart.data = null
            binding.incomeChart.invalidate()
            binding.incomeLegendLayout.removeAllViews()
            return
        }

        // Create pie entries
        val entries = categoryMap.mapIndexed { index, (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = incomeColors
        dataSet.valueTextSize = 18f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.2f
        dataSet.valueLinePart2Length = 0.3f
        dataSet.valueLineColor = Color.TRANSPARENT
        dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        // Custom formatter to show percentage
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val percentage = (value / totalIncome * 100)
                return "${percentage.toInt()}%"
            }
        }

        val pieData = PieData(dataSet)
        binding.incomeChart.data = pieData
        binding.incomeChart.invalidate()

        // Update legend
        updateLegend(categoryMap, totalIncome, binding.incomeLegendLayout, incomeColors)
    }

    private fun updateExpenseChart() {
        // Filter only expenses (negative amounts)
        val expenses = transactions.filter { it.amount < 0 }

        if (expenses.isEmpty()) {
            binding.expenseChart.data = null
            binding.expenseChart.invalidate()
            binding.expenseLegendLayout.removeAllViews()
            return
        }

        // Group by Expense Category (description field)
        val categoryMap = expenses.groupBy { transaction ->
            transaction.description?.takeIf { it.isNotBlank() } ?: transaction.label
        }
            .mapValues { (_, transactions) ->
                transactions.sumOf { abs(it.amount) }
            }
            .toList()
            .sortedByDescending { it.second }

        val totalExpense = categoryMap.sumOf { it.second }

        if (totalExpense == 0.0) {
            binding.expenseChart.data = null
            binding.expenseChart.invalidate()
            binding.expenseLegendLayout.removeAllViews()
            return
        }

        // Create pie entries
        val entries = categoryMap.mapIndexed { index, (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = expenseColors
        dataSet.valueTextSize = 18f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.2f
        dataSet.valueLinePart2Length = 0.3f
        dataSet.valueLineColor = Color.TRANSPARENT
        dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        // Custom formatter to show percentage
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val percentage = (value / totalExpense * 100)
                return "${percentage.toInt()}%"
            }
        }

        val pieData = PieData(dataSet)
        binding.expenseChart.data = pieData
        binding.expenseChart.invalidate()

        // Update legend
        updateLegend(categoryMap, totalExpense, binding.expenseLegendLayout, expenseColors)
    }

    private fun updateLegend(
        categoryMap: List<Pair<String, Double>>,
        total: Double,
        legendLayout: LinearLayout,
        colors: List<Int>
    ) {
        legendLayout.removeAllViews()

        categoryMap.forEachIndexed { index, (category, amount) ->
            val percentage = (amount / total * 100)

            val legendItem = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dpToPx()
                }
            }

            val colorView = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx()).apply {
                    marginEnd = 8.dpToPx()
                }
                // Make it circular
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colors[index % colors.size])
                }
            }

            val textView = TextView(requireContext()).apply {
                text = "$category - ${String.Companion.format(Locale.US, "%.0f", percentage)}%"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.darkGray))
                textSize = 14f
            }

            legendItem.addView(colorView)
            legendItem.addView(textView)
            legendLayout.addView(legendItem)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}