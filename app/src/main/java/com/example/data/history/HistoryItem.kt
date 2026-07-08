package com.example.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.ValidationType

@Entity(tableName = "validation_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val input: String,
    val type: ValidationType,
    val timestamp: Long = System.currentTimeMillis()
)
