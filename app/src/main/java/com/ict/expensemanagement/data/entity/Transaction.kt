package com.ict.expensemanagement.data.entity

import java.io.Serializable

data class Transaction(
    val id: Int = -1,
    val label: String = "",
    val amount: Double = 0.0,
    val description: String? = null,
    val transactionDate: String = "",
    val userId: String = "",
    var code: String = "",
    val linkedGoalId: Int? = null
) : Serializable {
    constructor() : this(-1, "", 0.0, "", "", "", "", null)

    fun setCode(){
        this.code =  "${this.label},${this.amount},${this.description},${this.transactionDate}"
    }
}

data class User(
    val id: String = "",
    val username: String = "",
    val passwordHash: String = "",
    val email: String = "",
    var code: String = ""
) : Serializable {
    constructor() : this("", "", "", "", "")

    fun setCode(){
        this.code =  "${this.username},${this.passwordHash},${this.email}"
    }
}

data class UserAndTransactions(
    val user: User,
    val transactions: List<Transaction>
) : Serializable

