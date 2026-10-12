package com.bookkeeper.app.util

import android.content.Context

/** 应用级设置（SharedPreferences）：每日额度、首次引导等。 */
object SettingsPrefs {
    private const val PREFS = "bookkeeper_settings"
    private const val KEY_DAILY_QUOTA = "daily_quota"
    private const val KEY_MONTHLY_QUOTA = "monthly_quota"
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun dailyQuota(context: Context): Double =
        prefs(context).getFloat(KEY_DAILY_QUOTA, 60f).toDouble()

    fun setDailyQuota(context: Context, value: Double) {
        prefs(context).edit().putFloat(KEY_DAILY_QUOTA, value.toFloat()).apply()
    }

    fun monthlyQuota(context: Context): Double =
        prefs(context).getFloat(KEY_MONTHLY_QUOTA, 1800f).toDouble()

    fun setMonthlyQuota(context: Context, value: Double) {
        prefs(context).edit().putFloat(KEY_MONTHLY_QUOTA, value.toFloat()).apply()
    }

    fun firstLaunchDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun setFirstLaunchDone(context: Context, done: Boolean) {
        prefs(context).edit().putBoolean(KEY_FIRST_LAUNCH_DONE, done).apply()
    }
}
