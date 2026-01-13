package com.ict.expensemanagement.data.entity

import java.io.Serializable

data class SavingsGoal(
    val id: Int = 0,
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val iconResId: Int = 0, // For icon resource
    val userId: String = ""
) : Serializable {
    fun getProgressPercentage(): Int {
        return if (targetAmount > 0) {
            ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }
}