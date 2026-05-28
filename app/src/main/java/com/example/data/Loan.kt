package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthId: String, // format: "YYYY-MM"
    val amount: Double,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
