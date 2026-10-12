package com.bookkeeper.app.data

import android.content.Context
import androidx.core.content.ContextCompat

/**
 * 内置分类与用户自定义分类的统一展示模型。
 * id 区分：内置分类为 1..7；自定义分类使用 >= 100 的 id（100 + 自定义表主键）。
 */
data class CatView(
    val id: Int,
    val name: String,
    val shortLabel: String,
    val colorRes: Int?,
    val colorHex: Int?
) {
    fun color(context: Context): Int =
        colorRes?.let { ContextCompat.getColor(context, it) }
            ?: colorHex
            ?: 0xFF9E9E9E.toInt()
}

object CategoryResolver {
    const val CUSTOM_BASE = 100
    const val BUILTIN_MIN = 1
    const val BUILTIN_MAX = 7

    fun isCustom(id: Int): Boolean = id >= CUSTOM_BASE

    fun customDbId(id: Int): Long = (id - CUSTOM_BASE).toLong()

    fun customExpenseId(dbId: Long): Int = (CUSTOM_BASE + dbId).toInt()

    fun builtInViews(): List<CatView> = Category.entries.map { c ->
        CatView(c.id, c.label, c.shortLabel, c.colorRes, null)
    }

    fun fromCustom(c: CustomCategory): CatView =
        CatView(customExpenseId(c.id), c.name, shortLabel(c.name), null, c.colorHex)

    private fun shortLabel(name: String): String =
        if (name.length <= 4) name else name.take(4)

    /** 由一组自定义分类构建 id -> 展示模型 的映射（含内置分类）。 */
    fun buildMap(customs: List<CustomCategory>): Map<Int, CatView> {
        val map = LinkedHashMap<Int, CatView>()
        builtInViews().forEach { map[it.id] = it }
        customs.forEach { c ->
            val v = fromCustom(c)
            map[v.id] = v
        }
        return map
    }
}
