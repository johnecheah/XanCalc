package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val type: String, // "STANDARD", "SCIENTIFIC", "CONVERSION"
    val timestamp: Long = System.currentTimeMillis()
)
