package com.bookkeeper.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CustomCategoryDao {

    @Insert
    suspend fun insert(category: CustomCategory): Long

    @Query("SELECT * FROM custom_categories ORDER BY id")
    suspend fun getAll(): List<CustomCategory>

    @Query("SELECT * FROM custom_categories WHERE active = 1 ORDER BY id")
    suspend fun getActive(): List<CustomCategory>

    @Query("UPDATE custom_categories SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("SELECT * FROM custom_categories WHERE id = :id")
    suspend fun getById(id: Long): CustomCategory?

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
