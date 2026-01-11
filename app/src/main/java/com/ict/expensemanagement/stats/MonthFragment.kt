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
import com.ict.expensemanagement.databinding.FragmentMonthBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MonthFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MonthFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding : FragmentMonthBinding
    private lateinit var transactions: List<Transaction>
    private lateinit var  transactionAdapter: TransactionAdapter
    private  lateinit var  linearLayoutManager: LinearLayoutManager
    private val firebaseRepository = FirebaseRepository()
    private var year : Int? = 0
    private var month : Int? = 0
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

        binding = FragmentMonthBinding.inflate(inflater, container, false)
        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(requireContext())

        val recyclerView = binding.recyclerview
        recyclerView.adapter = transactionAdapter
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        val dateInput = binding.dateInput

        if (dateInput.text!!.equals("")) {
            val calendar = Calendar.getInstance()
            year = calendar.get(Calendar.YEAR)
            month = calendar.get(Calendar.MONTH) + 1
            dateInput.setText("${month!! + 1}/$year")
        }

        dateInput.setOnClickListener {
            showMonthPickerDialog()
        }

        if (year != null && month != null) {
            if (month!! < 10) {
                fetchAll("$year", "0$month")
            } else {
                fetchAll("$year", "$month")
            }
        }

        return binding.root
    }

    private fun showMonthPickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dateInput = binding.dateInput


        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth->

                dateInput.setText("${month + 1}/$year")
                if (month!! + 1 < 10) {
                    fetchAll("$year", "0${month+1}")
                } else {
                    fetchAll("$year", "${month+1}")
                }
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }
    private fun fetchAll(year: String, month: String) {
        Log.d("app", "Handle fetch")
        GlobalScope.launch {
            transactions = firebaseRepository.getTransactionsByMonth(year, month, userId!!)
            Log.d("app", "$year/$month")

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
        if (month!! < 10) {
            fetchAll("$year", "0$month")
        } else {
            fetchAll("$year", "$month")
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MonthFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MonthFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}