package com.bookkeeper.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一条消费记录。amount 单位：元；categoryId 对应 [Category.id]。 */
@Entity(tableName = "expenses")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val categoryId: Int,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
