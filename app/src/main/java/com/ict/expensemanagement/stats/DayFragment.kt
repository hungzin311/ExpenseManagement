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
import com.ict.expensemanagement.databinding.FragmentDayBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DayFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DayFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding : FragmentDayBinding
    private lateinit var transactions: List<Transaction>
    private lateinit var  transactionAdapter: TransactionAdapter
    private  lateinit var  linearLayoutManager: LinearLayoutManager
    private val firebaseRepository = FirebaseRepository()
    private  lateinit var transDate : String
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDayBinding.inflate(inflater, container, false)

        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(requireContext())

        val recyclerView = binding.recyclerview
        recyclerView.adapter = transactionAdapter
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        val dateInput = binding.dateInput

        dateInput.setOnClickListener {
            showDatePickerDialog()
        }

        if (dateInput.text!!.equals("")) {
            val calendar = Calendar.getInstance()
            val today = calendar.time
            transDate  = SimpleDateFormat("yyyy-MM-dd").format(today)
        } else {
            transDate = dateInput.text.toString()
        }
        fetchAll(transDate)
        return binding.root
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dateInput = binding.dateInput

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                var selectedDate : String

                if (month < 10 && dayOfMonth < 10)
                    selectedDate = "$year-0${month + 1}-0$dayOfMonth"
                else if (month < 10)
                    selectedDate = "$year-0${month + 1}-$dayOfMonth"
                else if (dayOfMonth < 10)
                    selectedDate = "$year-0${month + 1}-$dayOfMonth"
                else
                    selectedDate = "$year-${month + 1}-$dayOfMonth"
                dateInput.setText(selectedDate)
                fetchAll(selectedDate)
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun fetchAll(date : String) {
        GlobalScope.launch {
            transactions = firebaseRepository.getTransactionsByDate(date, userId!!)
            Log.d("app", date)

            if (transactions.isNotEmpty()) {
                Log.d("app", transactions[0].label)
            }

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

        balance.text = "${"%, .0f".format(Locale.US, totalAmount)} VND"
        budget.text = "${"%, .0f".format(Locale.US, budgetAmount)} VND"
        expense.text = "${"%, .0f".format(Locale.US, expenseAmount)} VND"
    }

    override fun onResume() {
        super.onResume()
        fetchAll(transDate)
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DayFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DayFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}