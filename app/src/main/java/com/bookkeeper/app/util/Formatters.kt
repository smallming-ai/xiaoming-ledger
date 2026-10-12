package com.bookkeeper.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    /** 金额统一保留两位小数：12 -> "12.00" */
    fun amount(value: Double): String = String.format(Locale.US, "%.2f", value)

    /** 列表项时间：MM-dd HH:mm */
    private val itemTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    fun itemTime(ts: Long): String = itemTime.format(Date(ts))

    /** 周期标题：日 -> "2026-10-05"，周 -> "10-05 ~ 10-11"，月 -> "2026年10月"，年 -> "2026年" */
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayFmtShort = SimpleDateFormat("MM-dd", Locale.getDefault())
    private val monthFmt = SimpleDateFormat("yyyy年M月", Locale.getDefault())
    private val yearFmt = SimpleDateFormat("yyyy年", Locale.getDefault())

    fun dayTitle(ts: Long): String = dayFmt.format(Date(ts))
    fun dayTitleShort(ts: Long): String = dayFmtShort.format(Date(ts))
    fun monthTitle(ts: Long): String = monthFmt.format(Date(ts))
    fun yearTitle(ts: Long): String = yearFmt.format(Date(ts))
}
