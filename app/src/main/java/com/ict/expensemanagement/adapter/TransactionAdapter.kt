package com.ict.expensemanagement.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ict.expensemanagement.transaction.DetailedActivity
import com.ict.expensemanagement.R
import com.ict.expensemanagement.data.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class TransactionAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.label)
        val amount: TextView = view.findViewById(R.id.amount)
        val transactionDate: TextView = view.findViewById(R.id.transactionDate)
        val paymentMethod: TextView = view.findViewById(R.id.paymentMethod)
        val transactionIcon: ImageView = view.findViewById(R.id.transactionIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_layout, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.amount.context

        // Set label
        holder.label.text = transaction.label

        // Format amount without VAT
        if (transaction.amount >= 0) {
            holder.amount.text = "+ $${
                String.Companion.format(
                    Locale.US, "%.2f",
                    abs(transaction.amount)
                )}"
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.darkGray))
        } else {
            holder.amount.text = "- $${
                String.Companion.format(
                    Locale.US, "%.2f",
                    abs(transaction.amount)
                )}"
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.darkGray))
        }

        // Format date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val date = inputFormat.parse(transaction.transactionDate)
            holder.transactionDate.text = date?.let { outputFormat.format(it) } ?: transaction.transactionDate
        } catch (e: Exception) {
            holder.transactionDate.text = transaction.transactionDate
        }

        // Set payment method (default to "Cash" if not available)
        holder.paymentMethod.text = "Cash" // Can be extended to store payment method in Transaction model

        // Set icon based on label/category
        val iconRes = when {
            transaction.label.contains("Food", ignoreCase = true) -> R.drawable.ic_wallet
            transaction.label.contains("Uber", ignoreCase = true) ||
            transaction.label.contains("Transport", ignoreCase = true) -> R.drawable.ic_motorbike
            transaction.label.contains("Shopping", ignoreCase = true) -> R.drawable.ic_box
            else -> R.drawable.ic_wallet
        }
        holder.transactionIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailedActivity::class.java)
            intent.putExtra("transaction", transaction)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun setData(transactions: List<Transaction>) {
        this.transactions = transactions
        notifyDataSetChanged()
    }
}