package com.ict.expensemanagement.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ict.expensemanagement.adapter.TransactionAdapter
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.databinding.FragmentSpendsBinding

class SpendsFragment : Fragment() {
    private var _binding: FragmentSpendsBinding? = null
    private val binding get() = _binding!!
    private var transactionAdapter: TransactionAdapter? = null
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var transactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(requireContext())

        binding.recyclerview.adapter = transactionAdapter
        binding.recyclerview.layoutManager = linearLayoutManager
        binding.recyclerview.setHasFixedSize(true)

        // Setup swipe to delete
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                // Handle delete if needed
            }
        }
        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(binding.recyclerview)
    }

    fun setTransactions(transactions: List<Transaction>) {
        this.transactions = transactions
        transactionAdapter?.setData(transactions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}