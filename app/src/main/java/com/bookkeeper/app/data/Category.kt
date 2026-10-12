package com.bookkeeper.app.data

import androidx.annotation.ColorRes
import com.bookkeeper.app.R

/**
 * 消费原因分类（七大类）。
 * 注意：id 一旦使用即固定，勿随意改动，否则会导致历史数据分类错乱。
 */
enum class Category(
    val id: Int,
    val label: String,
    /** 图表 X 轴使用的短分类名，避免直方图标签重叠 */
    val shortLabel: String,
    @ColorRes val colorRes: Int
) {
    SURVIVAL_FOOD(1, "生存饮食", "生存", R.color.cat_survival_food),
    COMFORT_FOOD(2, "舒适度饮食", "舒适", R.color.cat_comfort_food),
    GUANQING(3, "人情", "人情", R.color.cat_guanqing),
    DAILY_TOOLS(4, "日常必备工具", "必备", R.color.cat_daily_tools),
    SELF_IMPROVEMENT(5, "自我提升消费", "提升", R.color.cat_self_improvement),
    UTILITIES(6, "水电费", "水电", R.color.cat_utilities),
    OTHER(7, "其他原因", "其他", R.color.cat_other);

    companion object {
        fun fromId(id: Int): Category = entries.firstOrNull { it.id == id } ?: OTHER

        /** 弹窗中直接一键选择的六个常用分类（其他原因单独走填写） */
        fun quickSelectable(): List<Category> = entries.filter { it != OTHER }
    }
}
