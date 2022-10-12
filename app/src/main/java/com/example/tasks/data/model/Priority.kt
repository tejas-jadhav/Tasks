package com.example.tasks.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "priority")
data class Priority(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val priorityValue: Int,
    val name: String,
    val colorHex: String
)