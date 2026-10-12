package com.bookkeeper.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bookkeeper.app.R
import com.bookkeeper.app.data.Category
import com.bookkeeper.app.data.Repository
import com.bookkeeper.app.databinding.ActivityOnboardingBinding
import com.bookkeeper.app.util.SettingsPrefs
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * 首次使用入场窗口：
 *  1. 从内置 7 类里勾选常用支出类型（多选）；
 *  2. 可添加自定义支出类型；
 *  3. 设置每日消费金额。
 * 完成后写入用户自己的支出类型集合，日常记账只显示这些类型 + 新建入口。
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val selected = mutableSetOf<Int>()
    private val pendingCustom = mutableListOf<PendingCustom>()

    private class PendingCustom(val name: String, val colorHex: Int, val persistent: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildBuiltinToggles()
        requestNotificationPermission()

        binding.btnAddCustom.setOnClickListener {
            CategoryDialogs.showCreate(this) { name, colorHex, persistent ->
                pendingCustom.add(PendingCustom(name, colorHex, persistent))
                renderCustom()
            }
        }
        binding.btnStart.setOnClickListener { startSetup() }
    }

    private fun buildBuiltinToggles() {
        val cats = Category.entries
        var i = 0
        while (i < cats.size) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(makeToggle(cats[i]), weightParams(1f))
            if (i + 1 < cats.size) {
                row.addView(makeToggle(cats[i + 1]), weightParams(1f))
            }
            binding.builtinContainer.addView(row)
            i += 2
        }
    }

    private fun makeToggle(cat: Category): MaterialButton =
        MaterialButton(this).apply {
            text = cat.label
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this@OnboardingActivity, cat.colorRes)
            )
            strokeWidth = dp(2)
            strokeColor = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.WHITE, Color.TRANSPARENT)
            )
            isCheckable = true
            addOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(cat.id) else selected.remove(cat.id)
            }
        }

    private fun renderCustom() {
        val container = binding.customContainer
        container.removeAllViews()
        binding.customEmpty.visibility = if (pendingCustom.isEmpty()) View.VISIBLE else View.GONE
        pendingCustom.forEachIndexed { index, pc ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply {
                    val m = dp(4)
                    setMargins(0, m, m, m)
                }
                background = ContextCompat.getDrawable(this@OnboardingActivity, R.drawable.bg_dot)
                backgroundTintList = ColorStateList.valueOf(pc.colorHex)
            })

            row.addView(TextView(this).apply {
                text = pc.name
                setTextColor(Color.BLACK)
                textSize = 14f
                setPadding(dp(4), 0, dp(4), 0)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            row.addView(TextView(this).apply {
                text = if (pc.persistent) "长期" else "单次"
                setTextColor(Color.WHITE)
                textSize = 11f
                setPadding(dp(6), dp(2), dp(6), dp(2))
                background = ContextCompat.getDrawable(this@OnboardingActivity, R.drawable.bg_badge)
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@OnboardingActivity, if (pc.persistent) R.color.brand_blue else R.color.brand_orange)
                )
            })

            row.addView(ImageButton(this).apply {
                setImageResource(R.drawable.ic_close)
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@OnboardingActivity, R.color.text_secondary)
                )
                background = null
                setOnClickListener {
                    pendingCustom.removeAt(index)
                    renderCustom()
                }
            }, LinearLayout.LayoutParams(dp(36), dp(36)))

            container.addView(row)
        }
    }

    private fun startSetup() {
        if (selected.isEmpty() && pendingCustom.isEmpty()) {
            Toast.makeText(this, "请至少选择一个支出类型", Toast.LENGTH_SHORT).show()
            return
        }
        val quotaStr = binding.quotaInput.text.toString().trim()
        val quota = quotaStr.toDoubleOrNull()?.takeIf { it > 0 } ?: 60.0
        SettingsPrefs.setDailyQuota(this, quota)

        lifecycleScope.launch {
            Category.entries.filter { selected.contains(it.id) }.forEach { cat ->
                Repository.addCustomCategory(
                    this@OnboardingActivity,
                    cat.label,
                    ContextCompat.getColor(this@OnboardingActivity, cat.colorRes),
                    true
                )
            }
            pendingCustom.forEach { pc ->
                Repository.addCustomCategory(this@OnboardingActivity, pc.name, pc.colorHex, pc.persistent)
            }
            SettingsPrefs.setFirstLaunchDone(this@OnboardingActivity, true)
            finish()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
        }
    }

    private fun weightParams(weight: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight).apply {
            val m = dp(3)
            setMargins(m, m, m, m)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
