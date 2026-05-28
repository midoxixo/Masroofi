package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "month_budgets")
data class MonthBudget(
    @PrimaryKey val monthId: String, // format: "YYYY-MM", e.g. "2026-05"
    val baseIncome: Double,
    val note: String = ""
)
