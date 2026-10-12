package com.bookkeeper.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(record: ExpenseRecord): Long

    @Update
    suspend fun update(record: ExpenseRecord)

    @Delete
    suspend fun delete(record: ExpenseRecord)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAll(): List<ExpenseRecord>

    /** 区间查询：timestamp 属于 [startMillis, endMillis) */
    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp DESC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<ExpenseRecord>
}
