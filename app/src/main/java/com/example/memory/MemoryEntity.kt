package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String, // "preference", "language", "context", "history"
    val timestamp: Long = System.currentTimeMillis()
)
