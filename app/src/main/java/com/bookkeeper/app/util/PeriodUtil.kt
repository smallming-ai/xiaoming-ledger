package com.bookkeeper.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 统计周期类型 */
enum class PeriodType(val label: String) {
    DAY("日"),
    WEEK("周"),
    MONTH("月"),
    YEAR("年")
}

/**
 * 时间周期计算。按系统时区把某个统计周期换算为 [startMillis, endMillis) 的毫秒区间。
 * offset：0 表示当前周期，-1 表示上一个周期，1 表示下一个周期。
 */
object PeriodUtil {

    private val zone: ZoneId = ZoneId.systemDefault()

    fun bounds(type: PeriodType, offset: Int): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        return when (type) {
            PeriodType.DAY -> {
                val day = today.plusDays(offset.toLong())
                startOf(day) to startOf(day.plusDays(1))
            }
            PeriodType.WEEK -> {
                // 周从周一开始
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(offset.toLong())
                startOf(monday) to startOf(monday.plusWeeks(1))
            }
            PeriodType.MONTH -> {
                val first = today.withDayOfMonth(1).plusMonths(offset.toLong())
                startOf(first) to startOf(first.plusMonths(1))
            }
            PeriodType.YEAR -> {
                val first = today.withDayOfYear(1).plusYears(offset.toLong())
                startOf(first) to startOf(first.plusYears(1))
            }
        }
    }

    private fun startOf(day: LocalDate): Long =
        day.atStartOfDay(zone).toInstant().toEpochMilli()

    /** 今天 0 点的时间戳（用于每日消费额度统计） */
    fun startOfToday(): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    /** 某个时间戳所在那天的 0 点（用于记账默认日期/补记） */
    fun startOfDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    /** 周期标题文本 */
    fun title(type: PeriodType, offset: Int): String {
        val today = LocalDate.now(zone)
        return when (type) {
            PeriodType.DAY -> {
                val day = today.plusDays(offset.toLong())
                Formatters.dayTitle(day.atStartOfDay(zone).toInstant().toEpochMilli())
            }
            PeriodType.WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(offset.toLong())
                val sunday = monday.plusDays(6)
                "${Formatters.dayTitleShort(monday.atStartOfDay(zone).toInstant().toEpochMilli())} ~ " +
                    Formatters.dayTitleShort(sunday.atStartOfDay(zone).toInstant().toEpochMilli())
            }
            PeriodType.MONTH -> {
                val first = today.withDayOfMonth(1).plusMonths(offset.toLong())
                Formatters.monthTitle(first.atStartOfDay(zone).toInstant().toEpochMilli())
            }
            PeriodType.YEAR -> {
                val first = today.withDayOfYear(1).plusYears(offset.toLong())
                Formatters.yearTitle(first.atStartOfDay(zone).toInstant().toEpochMilli())
            }
        }
    }
}
