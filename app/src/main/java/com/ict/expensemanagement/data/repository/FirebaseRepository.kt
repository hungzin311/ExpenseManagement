package com.ict.expensemanagement.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.ict.expensemanagement.data.entity.Category
import com.ict.expensemanagement.data.entity.SavingsGoal
import com.ict.expensemanagement.data.entity.Transaction
import com.ict.expensemanagement.data.entity.User
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.Calendar

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance().reference
    private val usersRef = database.child("users")
    private val transactionsRef = database.child("transactions")
    private val categoriesRef = database.child("categories")
    private val goalsRef = database.child("goals")

    private fun generateId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    // ========== USER OPERATIONS ==========
    suspend fun getUserById(userId: String): User? = try {
        val snapshot = usersRef.child(userId).get().await()
        snapshot.getValue(User::class.java)
    } catch (_: Exception) {
        null
    }

    suspend fun saveUser(user: User) {
        usersRef.child(user.id).setValue(user).await()
    }

    suspend fun updateUser(user: User) {
        usersRef.child(user.id).setValue(user).await()
    }

    suspend fun getMoneyByUserId(userId: String): Double = try {
        val snapshot = transactionsRef.get().await()
        snapshot.children
            .filter { it.child("userId").getValue(String::class.java) == userId }
            .sumOf { child -> child.child("amount").getValue(Double::class.java) ?: 0.0 }
    } catch (_: Exception) {
        0.0
    }

    suspend fun checkUsernameExists(username: String): Boolean = try {
        val snapshot = usersRef.orderByChild("username").equalTo(username).limitToFirst(1).get().await()
        snapshot.exists()
    } catch (_: Exception) {
        false
    }

    // ========== TRANSACTION OPERATIONS ==========
    suspend fun getAllTransactions(): List<Transaction> = try {
        val snapshot = transactionsRef.get().await()
        snapshot.children.mapNotNull { it.getValue(Transaction::class.java) }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getTransactionsByUserId(userId: String): List<Transaction> = try {
        val snapshot = transactionsRef.get().await()
        snapshot.children
            .mapNotNull { it.getValue(Transaction::class.java) }
            .filter { it.userId == userId }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getTransactionsByDate(date: String, userId: String): List<Transaction> {
        return getTransactionsByUserId(userId).filter { it.transactionDate == date }
    }

    suspend fun getTransactionsByWeek(year: String, week: String, userId: String): List<Transaction> {
        val weekInt = week.toIntOrNull() ?: return emptyList()
        val yearInt = year.toIntOrNull() ?: return emptyList()
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            set(Calendar.YEAR, yearInt)
            set(Calendar.WEEK_OF_YEAR, weekInt)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val startOfWeek = LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        val datesInWeek = (0..6).map { startOfWeek.plusDays(it.toLong()).toString() }.toSet()
        return getTransactionsByUserId(userId).filter { it.transactionDate in datesInWeek }
    }

    suspend fun getTransactionsByMonth(year: String, month: String, userId: String): List<Transaction> {
        val prefix = "$year-$month"
        return getTransactionsByUserId(userId).filter { it.transactionDate.startsWith(prefix) }
    }

    suspend fun insertTransaction(transaction: Transaction): String {
        var newId = if (transaction.id != -1) transaction.id else generateId()
        var nodeRef = transactionsRef.child(newId.toString())

        // Ensure we never overwrite an existing transaction node
        while (nodeRef.get().await().exists()) {
            newId = generateId()
            nodeRef = transactionsRef.child(newId.toString())
        }

        val transactionWithId = transaction.copy(id = newId)
        nodeRef.setValue(transactionWithId).await()
        return newId.toString()
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        val nodeId = when {
            transaction.id != -1 -> transaction.id.toString()
            else -> findTransactionNodeByCode(transaction.userId, transaction.code)
        } ?: return

        transactionsRef.child(nodeId).removeValue().await()
    }

    suspend fun updateTransaction(transaction: Transaction) {
        val nodeId = when {
            transaction.id != -1 -> transaction.id.toString()
            else -> findTransactionNodeByCode(transaction.userId, transaction.code)
        } ?: return

        transactionsRef.child(nodeId).setValue(transaction).await()
    }

    suspend fun getTransactionWithLargestId(): Transaction? = try {
        val snapshot = transactionsRef.orderByChild("id").limitToLast(1).get().await()
        snapshot.children.firstOrNull()?.getValue(Transaction::class.java)
    } catch (_: Exception) {
        null
    }

    private suspend fun findTransactionNodeByCode(userId: String, code: String): String? = try {
        val snapshot = transactionsRef.get().await()
        snapshot.children.firstOrNull {
            it.child("userId").getValue(String::class.java) == userId &&
                    it.child("code").getValue(String::class.java) == code
        }?.key
    } catch (_: Exception) {
        null
    }

    // ========== CATEGORY OPERATIONS ==========
    suspend fun getCategoriesByUserId(userId: String): List<Category> = try {
        val snapshot = categoriesRef.get().await()
        snapshot.children
            .mapNotNull { it.getValue(Category::class.java) }
            .filter { it.userId == userId }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getCategoriesByType(userId: String, type: String): List<Category> {
        return getCategoriesByUserId(userId).filter { it.type == type }
    }

    suspend fun getCategoryByName(userId: String, type: String, name: String): Category? {
        return getCategoriesByUserId(userId).firstOrNull { it.type == type && it.name == name }
    }

    suspend fun insertCategory(category: Category): String {
        val newId = if (category.id != 0) category.id else generateId()
        val categoryWithId = category.copy(id = newId)
        categoriesRef.child(newId.toString()).setValue(categoryWithId).await()
        return newId.toString()
    }

    suspend fun deleteCategory(category: Category) {
        if (category.id == 0) return
        categoriesRef.child(category.id.toString()).removeValue().await()
    }

    suspend fun updateCategory(category: Category) {
        if (category.id == 0) return
        categoriesRef.child(category.id.toString()).setValue(category).await()
    }

    // ========== SAVINGS GOAL OPERATIONS ==========
    suspend fun getGoalsByUserId(userId: String): List<SavingsGoal> = try {
        val snapshot = goalsRef.get().await()
        snapshot.children
            .mapNotNull { it.getValue(SavingsGoal::class.java) }
            .filter { it.userId == userId }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun insertGoal(goal: SavingsGoal): Int {
        var newId = if (goal.id != 0) goal.id else generateId()
        var nodeRef = goalsRef.child(newId.toString())

        while (nodeRef.get().await().exists()) {
            newId = generateId()
            nodeRef = goalsRef.child(newId.toString())
        }

        val goalWithId = goal.copy(id = newId)
        nodeRef.setValue(goalWithId).await()
        return newId
    }

    suspend fun updateGoal(goal: SavingsGoal) {
        if (goal.id == 0) return
        goalsRef.child(goal.id.toString()).setValue(goal).await()
    }

    suspend fun deleteGoal(goalId: Int) {
        if (goalId == 0) return
        goalsRef.child(goalId.toString()).removeValue().await()
    }
}
