package com.ict.expensemanagement.data.entity

import java.io.Serializable

data class SavingsGoal(
    val id: Int,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val iconResId: Int = 0, // For icon resource
    val userId: String
) : Serializable {
    fun getProgressPercentage(): Int {
        return if (targetAmount > 0) {
            ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }
}