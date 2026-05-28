package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MonthBudget::class, Transaction::class, CustomCategory::class, Loan::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "masroofy_database"
                )
                .fallbackToDestructiveMigration() // safe for early prototype development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
