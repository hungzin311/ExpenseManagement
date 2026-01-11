package com.ict.expensemanagement.stats

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.adapter.TransactionAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.repository.FirebaseRepository
import com.ict.expensemanagement.databinding.FragmentWeekBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WeekFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WeekFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding : FragmentWeekBinding
    private lateinit var transactions: List<Transaction>
    private lateinit var  transactionAdapter: TransactionAdapter
    private  lateinit var  linearLayoutManager: LinearLayoutManager
    private val firebaseRepository = FirebaseRepository()
    private var year : Int? = 0
    private var weekOfYear : Int? = 0
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWeekBinding.inflate(inflater, container, false)
        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(requireContext())

        val recyclerView = binding.recyclerview
        recyclerView.adapter = transactionAdapter
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        val dateInput = binding.dateInput
        val convertInput = binding.convertInput

        dateInput.setOnClickListener {
            showWeekPickerDialog()
        }

        if (year != null && weekOfYear != null) {
            if (weekOfYear!! < 10) {
                fetchAll("$year", "0$weekOfYear")
            } else {
                fetchAll("$year", "$weekOfYear")
            }
        }
        return binding.root
    }

    private fun showWeekPickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dateInput = binding.dateInput
        val convertInput = binding.convertInput

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val weekOfYear = getWeekOfYear(year, month, dayOfMonth)
                val (startDate, endDate) = getStartAndEndOfWeek(year, weekOfYear)
                val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                val outputFormat = SimpleDateFormat("yyyy-MM-dd")

                val startParsedDate = inputFormat.parse(startDate.toString())
                val endParsedDate = inputFormat.parse(endDate.toString())
                val startDateFor = outputFormat.format(startParsedDate)
                val endDateFor = outputFormat.format(endParsedDate)
                dateInput.setText("$startDateFor - $endDateFor")
                convertInput.setText("$year-$weekOfYear")
                if (weekOfYear < 10) {
                    fetchAll("$year", "0$weekOfYear")
                } else {
                    fetchAll("$year", "$weekOfYear")
                }
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun getStartAndEndOfWeek(year: Int, weekOfYear: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.WEEK_OF_YEAR, weekOfYear)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK - 1)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

    private fun fetchAll(year: String, weekOfYear: String) {
        Log.d("app", "Handle fetch")
        GlobalScope.launch {
            transactions = firebaseRepository.getTransactionsByWeek(year, weekOfYear, userId!!)
            Log.d("app", "$year - $weekOfYear")
            activity?.runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateDashboard(){
        val totalAmount = transactions.map { it.amount }.sum()
        val budgetAmount = transactions.filter{ it.amount > 0 }.map{ it.amount }.sum()
        val expenseAmount = totalAmount - budgetAmount

        val balance = binding.balance
        val budget = binding.budget
        val expense = binding.expense

        balance.text = "${"%,.0f".format(Locale.US, totalAmount)} VND"
        budget.text = "${"%, .0f".format(Locale.US, budgetAmount)} VND"
        expense.text = "${"%, .0f".format(Locale.US, expenseAmount)} VND"
    }

    override fun onResume() {
        super.onResume()
        if (weekOfYear!! < 10) {
            fetchAll("$year", "0$weekOfYear")
        } else {
            fetchAll("$year", "$weekOfYear")
        }
    }

    fun getWeekOfYear(year: Int, month: Int, dayOfMonth: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth) // Months are 0-indexed
        return calendar.get(Calendar.WEEK_OF_YEAR)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WeekFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WeekFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}