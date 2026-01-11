package com.ict.expensemanagement.data.entity

import java.io.Serializable

data class Category(
    val id: Int = 0,
    val name: String = "",
    val type: String = "", // "Income" or "Expense"
    val userId: String = ""
) : Serializable {
    constructor() : this(0, "", "", "")
}