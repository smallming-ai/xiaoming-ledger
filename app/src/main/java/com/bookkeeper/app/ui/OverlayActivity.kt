package com.bookkeeper.app.ui

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bookkeeper.app.R
import com.bookkeeper.app.data.CategoryResolver
import com.bookkeeper.app.data.CustomCategory
import com.bookkeeper.app.data.Repository
import com.bookkeeper.app.databinding.OverlayActivityBinding
import com.bookkeeper.app.util.Formatters
import com.bookkeeper.app.util.PeriodUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 记账窗口（手动记账，可从通知栏"记一笔"进入）。只显示用户自己的常用支出类型 + "新建类型"按钮。 */
class OverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_DATE = "extra_date"
    }

    private lateinit var binding: OverlayActivityBinding

    /** 记账日期（当天 0 点时间戳），默认取当前页面日期，可点击修改用于补记 */
    private var recordDateMillis: Long = 0L
    private val dateFmt = SimpleDateFormat("yyyy年M月d日 EEE", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OverlayActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fixedAmount = intent?.getDoubleExtra(EXTRA_AMOUNT, -1.0)?.takeIf { it > 0 }
        if (fixedAmount != null) {
            binding.amountInput.setText(Formatters.amount(fixedAmount))
        } else {
            binding.overlayTitle.text = getString(R.string.manual_title)
        }

        // 记账日期：优先用当前页面日期（可补记昨天/上周等），否则默认今天
        recordDateMillis = intent?.getLongExtra(EXTRA_DATE, -1L)
            ?.takeIf { it > 0 }
            ?.let { PeriodUtil.startOfDay(it) }
            ?: PeriodUtil.startOfToday()
        refreshDateDisplay()

        binding.dateDisplay.setOnClickListener { showDatePicker() }
        buildCategoryRows()
        binding.skipButton.setOnClickListener { finish() }
    }

    private fun refreshDateDisplay() {
        binding.dateDisplay.text = dateFmt.format(Date(recordDateMillis))
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = recordDateMillis }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                recordDateMillis = PeriodUtil.startOfDay(picked.timeInMillis)
                refreshDateDisplay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun buildCategoryRows() {
        lifecycleScope.launch {
            val customs = Repository.activeCustomCategories(this@OverlayActivity)
            val container = binding.categoryContainer
            container.removeAllViews()

            val defs = customs.map { c ->
                CatDef(c.name, c.colorHex) { saveRecord(CategoryResolver.customExpenseId(c.id), c) }
            }.toMutableList()
            // 新建类型入口
            defs.add(CatDef("＋ 新建类型", 0xFF1E88E5.toInt()) { onAddNewType() })

            var i = 0
            while (i < defs.size) {
                val row = LinearLayout(this@OverlayActivity).apply { orientation = LinearLayout.HORIZONTAL }
                val d1 = defs[i]
                row.addView(makeButton(d1), weightParams(1f))
                if (i + 1 < defs.size) {
                    row.addView(makeButton(defs[i + 1]), weightParams(1f))
                }
                container.addView(row)
                i += 2
            }
        }
    }

    private fun makeButton(def: CatDef): MaterialButton =
        MaterialButton(this).apply {
            text = def.label
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(def.color)
            setOnClickListener { def.onClick() }
        }

    private fun onAddNewType() {
        CategoryDialogs.showCreate(this) { name, colorHex, persistent ->
            lifecycleScope.launch {
                Repository.addCustomCategory(this@OverlayActivity, name, colorHex, persistent)
                buildCategoryRows()
            }
        }
    }

    private fun saveRecord(categoryId: Int, custom: CustomCategory) {
        val amount = binding.amountInput.text.toString().trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.errorText.visibility = View.VISIBLE
            return
        }
        binding.errorText.visibility = View.GONE
        lifecycleScope.launch {
            // 记账日期用所选日期，时刻保留"当前时间点"（便于同一天内排序）
            val now = System.currentTimeMillis()
            val timestamp = recordDateMillis + (now - PeriodUtil.startOfToday())
            Repository.addByCategoryId(this@OverlayActivity, amount, categoryId, null, timestamp)
            // 单次应用的分类：用掉一次后自动移除
            if (!custom.persistent) {
                Repository.consumeCustomCategory(this@OverlayActivity, custom.id)
            }
            Toast.makeText(
                this@OverlayActivity,
                getString(R.string.overlay_recorded, custom.name, Formatters.amount(amount)),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private class CatDef(val label: String, val color: Int, val onClick: () -> Unit)

    private fun weightParams(weight: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight).apply {
            val m = dp(3)
            setMargins(m, m, m, m)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}