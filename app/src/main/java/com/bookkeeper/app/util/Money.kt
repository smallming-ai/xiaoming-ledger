package com.bookkeeper.app.util

/**
 * 金额工具：统一按"分"为整数单位做计算，避免浮点累加误差（如 0.00+0.00=0.01）。
 * 金额在存库与展示前都四舍五入到分。
 */
object Money {

    /** 元 -> 分 */
    fun cents(value: Double): Long = Math.round(value * 100)

    /** 分 -> 元 */
    fun fromCents(cents: Long): Double = cents / 100.0

    /** 四舍五入到分（元） */
    fun round(value: Double): Double = fromCents(cents(value))

    /** 一组金额按"分"累加，返回元（消除浮点误差） */
    fun sumOf(amounts: Collection<Double>): Double =
        fromCents(amounts.sumOf { cents(it) })
}
