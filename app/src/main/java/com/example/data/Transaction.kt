package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthId: String, // foreign lookup key, e.g., "2026-05"
    val amount: Double,
    val type: String, // "EXPENSE" or "EXTRA_INCOME"
    val category: String, // e.g., "Food", "Transport", "Rent", "Utilities", "Shopping", "Entertainment", "Other" or custom
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
