package com.example.tasks.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_list",
    indices = [Index(value = ["name"], unique = true)]
)
data class TaskList(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String,
    val iconId: Int,
    var items: Int = 0,
    @ColumnInfo(name = "modified_at")
    var modifiedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    var createdAt: Long? = null
) {

    override fun toString(): String {
        return name
    }

    companion object {
        fun getInitialList(): List<TaskList> {
            val personalList = TaskList(1, "Personal", 1)
            val workList = TaskList(2, "Work", 257)

            return listOf(personalList, workList)
        }
    }
}