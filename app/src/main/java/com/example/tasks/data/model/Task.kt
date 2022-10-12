package com.example.tasks.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import com.example.tasks.utils.Constants

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = CASCADE
        )
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val title: String,
    val priorityId: Int = Constants.PRIORITY_LOW,
    val dateTime: Long,
    val listId: Int,
    val description: String,
    var hasAlarm: Boolean = false,
    var isCompleted: Boolean = false,
    var progress: Int? = null,
    @ColumnInfo(name = "modified_at")
    var modifiedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    var createdAt: Long? = null
): java.io.Serializable