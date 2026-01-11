package com.ict.expensemanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ict.expensemanagement.R
import com.ict.expensemanagement.data.entity.SavingsGoal
import java.util.Locale

class GoalAdapter(
    private var goals: List<SavingsGoal>,
    private val onGoalClick: (SavingsGoal) -> Unit = {}
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.goalIcon)
        val title: TextView = itemView.findViewById(R.id.goalTitle)
        val goalAmounts: TextView = itemView.findViewById(R.id.goalAmounts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        holder.title.text = goal.title
        holder.goalAmounts.text = "$${String.Companion.format(Locale.US, "%,.0f", goal.currentAmount)} / $${
            String.Companion.format(
                Locale.US, "%,.0f", goal.targetAmount)}"

        // Set icon based on goal title (simple mapping)
        val iconResId = when {
            goal.title.contains("Bike", ignoreCase = true) -> R.drawable.ic_motorbike
            goal.title.contains("Phone", ignoreCase = true) || goal.title.contains("iPhone", ignoreCase = true) -> R.drawable.ic_wallet
            else -> R.drawable.ic_box
        }
        holder.icon.setImageResource(iconResId)
        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.darkGray))

        holder.itemView.setOnClickListener {
            onGoalClick(goal)
        }
    }

    override fun getItemCount(): Int = goals.size

    fun updateGoals(newGoals: List<SavingsGoal>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}