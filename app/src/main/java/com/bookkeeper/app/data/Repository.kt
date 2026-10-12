package com.bookkeeper.app.data

import android.content.Context
import com.bookkeeper.app.util.Money

/** 账目仓库：统一封装 Room DAO 的访问。 */
object Repository {

    fun dao(context: Context): ExpenseDao = AppDatabase.get(context).expenseDao()

    fun customDao(context: Context): CustomCategoryDao = AppDatabase.get(context).customCategoryDao()

    /** 读取某个时间区间内的所有记录（时间区间由 PeriodUtil 计算）。 */
    suspend fun recordsBetween(context: Context, startMillis: Long, endMillis: Long): List<ExpenseRecord> =
        dao(context).getBetween(startMillis, endMillis)

    /** 按分类 id 记账（内置分类 id 1..7，自定义分类 id >= 100）。金额先四舍五入到分再入库。 */
    suspend fun addByCategoryId(
        context: Context,
        amount: Double,
        categoryId: Int,
        note: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Long = dao(context).insert(
        ExpenseRecord(
            amount = Money.round(amount),
            categoryId = categoryId,
            note = note?.takeIf { it.isNotBlank() },
            timestamp = timestamp
        )
    )

    suspend fun deleteById(context: Context, id: Long) = dao(context).deleteById(id)

    // ---- 自定义支出类型 ----

    suspend fun addCustomCategory(
        context: Context,
        name: String,
        colorHex: Int,
        persistent: Boolean
    ): Long = customDao(context).insert(
        CustomCategory(name = name, colorHex = colorHex, persistent = persistent)
    )

    suspend fun activeCustomCategories(context: Context): List<CustomCategory> =
        customDao(context).getActive()

    suspend fun allCustomCategories(context: Context): List<CustomCategory> =
        customDao(context).getAll()

    suspend fun deleteCustomCategory(context: Context, id: Long) =
        customDao(context).deleteById(id)

    /** 单次应用的分类用掉一次后置为不可用。 */
    suspend fun consumeCustomCategory(context: Context, id: Long) =
        customDao(context).setActive(id, false)
}
