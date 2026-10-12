package com.bookkeeper.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bookkeeper.app.R

/**
 * 共用的"新建支出类型"对话框：输入名称、选颜色、选长期保留或单次应用。
 * 通过 onCreate(name, colorHex, persistent) 回调把结果交给调用方。
 */
object CategoryDialogs {

    private val palette = intArrayOf(
        0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(),
        0xFF2196F3.toInt(), 0xFF00BCD4.toInt(), 0xFF4CAF50.toInt(),
        0xFFFF9800.toInt(), 0xFF795548.toInt()
    )

    fun showCreate(activity: AppCompatActivity, onCreate: (String, Int, Boolean) -> Unit) {
        val context = activity
        val dp = { v: Int -> (v * context.resources.displayMetrics.density).toInt() }

        val nameInput = EditText(context).apply {
            hint = "类型名称（如：奶茶、打车）"
            background = ContextCompat.getDrawable(context, R.drawable.bg_input)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#9E9E9E"))
            textSize = 15f
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val colorsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        var selectedColor = 0
        val dots = mutableListOf<ImageButton>()
        palette.forEachIndexed { index, color ->
            val dot = ImageButton(context).apply {
                background = oval(color, if (index == 0) dp(3) else 0, Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    val m = dp(5)
                    setMargins(m, 0, m, 0)
                }
                setOnClickListener {
                    selectedColor = index
                    dots.forEachIndexed { i, d ->
                        d.background = oval(palette[i], if (i == selectedColor) dp(3) else 0, Color.WHITE)
                    }
                }
            }
            dots.add(dot)
            colorsRow.addView(dot)
        }

        val radioGroup = RadioGroup(context).apply { orientation = RadioGroup.HORIZONTAL }
        val rbLong = RadioButton(context).apply { text = "长期保留" }
        val rbOnce = RadioButton(context).apply { text = "单次应用" }
        rbLong.isChecked = true
        radioGroup.addView(rbLong)
        radioGroup.addView(rbOnce)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
            addView(label(context, "名称"))
            addView(nameInput, lp())
            addView(label(context, "颜色"), lp(dp(12)))
            addView(colorsRow)
            addView(label(context, "保留方式"), lp(dp(12)))
            addView(radioGroup)
        }

        AlertDialog.Builder(context)
            .setTitle("新建支出类型")
            .setView(content)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    nameInput.error = "请输入名称"
                    return@setPositiveButton
                }
                onCreate(name, palette[selectedColor], rbLong.isChecked)
            }
            .setNegativeButton("取消", null)
            .create()
            .apply { window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE) }
            .show()
    }

    private fun label(context: android.content.Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(Color.BLACK)
            textSize = 14f
        }

    private fun lp(topMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { this.topMargin = topMargin }

    private fun oval(color: Int, stroke: Int, strokeColor: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (stroke > 0) setStroke(stroke, strokeColor)
        }
}
