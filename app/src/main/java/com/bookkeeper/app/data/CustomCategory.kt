package com.bookkeeper.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自定义的支出类型。
 * - persistent=true：长期保留，一直出现在选择列表里；
 * - persistent=false：单次应用，记录过一次后自动从列表移除（active 置 false）。
 */
@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: Int,
    val persistent: Boolean,
    val active: Boolean = true
)
