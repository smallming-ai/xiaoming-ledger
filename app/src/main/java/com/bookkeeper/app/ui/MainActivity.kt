package com.bookkeeper.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookkeeper.app.R
import com.bookkeeper.app.data.AppDatabase
import com.bookkeeper.app.data.CategoryResolver
import com.bookkeeper.app.data.CatView
import com.bookkeeper.app.data.ExpenseDao
import com.bookkeeper.app.data.ExpenseRecord
import com.bookkeeper.app.data.Repository
import com.bookkeeper.app.databinding.ActivityMainBinding
import com.bookkeeper.app.util.Formatters
import com.bookkeeper.app.util.Money
import com.bookkeeper.app.util.PeriodType
import com.bookkeeper.app.util.PeriodUtil
import com.bookkeeper.app.util.SettingsPrefs
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * 主界面：日/周/月/年 消费统计。
 *  - 每日消费额度框（额度由用户自定义，点击卡片可修改）；
 *  - 扇形统计图（环形图 + 图例，标签不重叠）；
 *  - 频率分布直方图（短分类名 + 柱顶数值，标签不重叠）；
 *  - 下方账目明细列表，长按可删除。
 * 无需无障碍服务；打开时启动通知栏"记一笔"快捷入口。
 */
class MainActivity : AppCompatActivity() {

    private companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordAdapter

    private var periodType: PeriodType = PeriodType.DAY
    private var offset: Int = 0

    private val dao: ExpenseDao by lazy { AppDatabase.get(this).expenseDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // 首次启动：弹出新用户引导
        if (!SettingsPrefs.firstLaunchDone(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        setupCharts()
        setupTabs()
        setupNav()
        setupList()

        binding.fabAdd.setOnClickListener {
            startActivity(
                Intent(this, OverlayActivity::class.java)
                    .putExtra(OverlayActivity.EXTRA_DATE, anchorDateMillis())
            )
        }
        binding.quotaCard.setOnClickListener { showQuotaDialog() }
        binding.monthlyQuotaCard.setOnClickListener { showMonthlyQuotaDialog() }

        requestNotificationPermissionIfNeeded()
        QuickRecordService.start(this)

        refresh()
        refreshQuota()
        refreshMonthlyQuota()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        refreshQuota()
        refreshMonthlyQuota()
    }

    /** 当前页面所代表的日期（记账默认日期，便于补记昨天/上周等）。今天在周期内则默认今天，否则默认该周期第一天。 */
    private fun anchorDateMillis(): Long {
        val now = System.currentTimeMillis()
        val (start, end) = PeriodUtil.bounds(periodType, offset)
        return if (now >= start && now < end) now else start
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            setHoleRadius(46f)
            setTransparentCircleRadius(50f)
            setDrawEntryLabels(false)      // 分类名只进图例，不画在扇区上（避免重叠）
            setDrawCenterText(true)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.form = Legend.LegendForm.CIRCLE
            legend.isWordWrapEnabled = true
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.xEntrySpace = 8f
            legend.yEntrySpace = 4f
        }
        binding.barChart.apply {
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.setDrawGridLines(false)
            axisLeft.textColor = Color.BLACK
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.setLabelRotationAngle(0f)   // 标签不倾斜，配合短分类名避免重叠
            xAxis.textColor = Color.BLACK
            legend.isEnabled = false
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                periodType = when (tab?.position ?: 0) {
                    1 -> PeriodType.WEEK
                    2 -> PeriodType.MONTH
                    3 -> PeriodType.YEAR
                    else -> PeriodType.DAY
                }
                offset = 0
                refresh()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupNav() {
        binding.prevPeriod.setOnClickListener {
            offset -= 1
            refresh()
        }
        binding.nextPeriod.setOnClickListener {
            offset += 1
            refresh()
        }
    }

    private fun setupList() {
        adapter = RecordAdapter { record ->
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        dao.deleteById(record.id)
                        refresh()
                        refreshQuota()
                        refreshMonthlyQuota()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.recordList.layoutManager = LinearLayoutManager(this)
        binding.recordList.adapter = adapter
    }

    private fun refresh() {
        val (start, end) = PeriodUtil.bounds(periodType, offset)
        binding.periodTitle.text = PeriodUtil.title(periodType, offset)
        lifecycleScope.launch {
            val records = dao.getBetween(start, end)
            val customs = Repository.allCustomCategories(this@MainActivity)
            val map = CategoryResolver.buildMap(customs)
            render(records, map)
        }
    }

    private fun render(records: List<ExpenseRecord>, map: Map<Int, CatView>) {
        binding.totalAmount.text = "¥${Formatters.amount(Money.sumOf(records.map { it.amount }))}"
        binding.totalCount.text = records.size.toString()
        binding.emptyText.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        renderPie(binding.pieChart, records, map)
        renderHistogram(binding.barChart, records, map)
        adapter.submit(records, map)
    }

    /** 环形统计图：分类金额占比（分类名+百分比进图例，中心显示总额，标签不重叠） */
    private fun renderPie(chart: PieChart, records: List<ExpenseRecord>, map: Map<Int, CatView>) {
        if (records.isEmpty()) {
            chart.data = null
            chart.setCenterText("")
            chart.invalidate()
            return
        }
        val agg = records.groupBy { it.categoryId }
            .mapNotNull { (id, list) ->
                val cat = map[id] ?: return@mapNotNull null
                val sum = Money.round(list.sumOf { it.amount })
                if (sum <= 0) null else (cat to sum)
            }
            .sortedByDescending { it.second }
        if (agg.isEmpty()) {
            chart.data = null
            chart.setCenterText("")
            chart.invalidate()
            return
        }
        val total = Money.round(agg.sumOf { it.second })
        val entries = agg.map { (cat, sum) ->
            val pct = sum / total * 100
            PieEntry(sum.toFloat(), "${cat.name}  ${Formatters.amount(pct)}%")
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = agg.map { it.first.color(this@MainActivity) }
            sliceSpace = 1.5f
            setDrawValues(false)          // 扇区内不画数值，交给图例与中心总额
            selectionShift = 5f
        }
        chart.apply {
            setUsePercentValues(false)
            setCenterText("¥${Formatters.amount(total)}")
            setCenterTextSize(15f)
            setCenterTextColor(Color.WHITE)
            this.data = PieData(dataSet)
            invalidate()
        }
    }

    /** 频率分布直方图：分类交易次数（短分类名，柱顶数值） */
    private fun renderHistogram(chart: BarChart, records: List<ExpenseRecord>, map: Map<Int, CatView>) {
        if (records.isEmpty()) {
            chart.data = null
            chart.invalidate()
            return
        }
        val agg = records.groupBy { it.categoryId }
            .mapNotNull { (id, list) ->
                val cat = map[id] ?: return@mapNotNull null
                cat to list.size
            }
            .sortedByDescending { it.second }
        val labels = agg.map { it.first.shortLabel }
        val entries = agg.mapIndexed { index, (_, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        val dataSet = BarDataSet(entries, "消费次数").apply {
            colors = agg.map { it.first.color(this@MainActivity) }
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }
        val data = BarData(dataSet)
        data.barWidth = 0.35f
        chart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = (labels.size - 1) + 0.5f
            xAxis.granularity = 1f
            xAxis.setLabelRotationAngle(0f)
            this.data = data
            invalidate()
        }
    }

    /** 每日额度：按"今天0点"起求和（每日天然归零），额度由用户自定义 */
    private fun refreshQuota() {
        val quota = SettingsPrefs.dailyQuota(this)
        binding.quotaLimit.text = "¥${Formatters.amount(quota)}"
        val startOfToday = PeriodUtil.startOfToday()
        val end = System.currentTimeMillis() + 1
        lifecycleScope.launch {
            val records = dao.getBetween(startOfToday, end)
            renderQuota(quota, Money.sumOf(records.map { it.amount }))
        }
    }

    private fun renderQuota(quota: Double, spent: Double) {
        val over = spent > quota
        binding.quotaUsed.text = "¥${Formatters.amount(spent)}"
        binding.quotaRemain.text =
            if (over) "已超支 ¥${Formatters.amount(spent - quota)}"
            else "剩余 ¥${Formatters.amount((quota - spent).coerceAtLeast(0.0))}"
        val statusColor =
            ContextCompat.getColor(this, if (over) R.color.amount_red else R.color.brand_blue)
        binding.quotaUsed.setTextColor(statusColor)
        binding.quotaRemain.setTextColor(
            ContextCompat.getColor(this, if (over) R.color.amount_red else R.color.text_secondary)
        )
        binding.quotaProgress.max = 100
        binding.quotaProgress.progress =
            if (quota > 0) (spent / quota * 100).toInt().coerceIn(0, 100) else 0
        binding.quotaProgress.progressTintList = ColorStateList.valueOf(statusColor)
    }

    private fun showQuotaDialog() {
        val input = EditText(this).apply {
            setText(Formatters.amount(SettingsPrefs.dailyQuota(this@MainActivity)))
            hint = "请输入每日额度（元）"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#9E9E9E"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        AlertDialog.Builder(this)
            .setTitle("设置每日额度")
            .setMessage("额度将用于首页每日消费进度条")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val v = input.text.toString().trim().toDoubleOrNull()
                if (v != null && v > 0) {
                    SettingsPrefs.setDailyQuota(this, v)
                    refreshQuota()
                } else {
                    Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 每月额度：按"本月0点"起求和 */
    private fun refreshMonthlyQuota() {
        val quota = SettingsPrefs.monthlyQuota(this)
        binding.monthlyQuotaLimit.text = "¥${Formatters.amount(quota)}"
        val (start, end) = PeriodUtil.bounds(PeriodType.MONTH, 0)
        lifecycleScope.launch {
            val records = dao.getBetween(start, end)
            renderMonthlyQuota(quota, Money.sumOf(records.map { it.amount }))
        }
    }

    private fun renderMonthlyQuota(quota: Double, spent: Double) {
        val over = spent > quota
        binding.monthlyQuotaUsed.text = "¥${Formatters.amount(spent)}"
        binding.monthlyQuotaRemain.text =
            if (over) "本月已超支 ¥${Formatters.amount(spent - quota)}"
            else "本月剩余 ¥${Formatters.amount((quota - spent).coerceAtLeast(0.0))}"
        val statusColor =
            ContextCompat.getColor(this, if (over) R.color.amount_red else R.color.brand_orange)
        binding.monthlyQuotaUsed.setTextColor(statusColor)
        binding.monthlyQuotaRemain.setTextColor(
            ContextCompat.getColor(this, if (over) R.color.amount_red else R.color.text_secondary)
        )
        binding.monthlyQuotaProgress.max = 100
        binding.monthlyQuotaProgress.progress =
            if (quota > 0) (spent / quota * 100).toInt().coerceIn(0, 100) else 0
        binding.monthlyQuotaProgress.progressTintList = ColorStateList.valueOf(statusColor)
    }

    private fun showMonthlyQuotaDialog() {
        val input = EditText(this).apply {
            setText(Formatters.amount(SettingsPrefs.monthlyQuota(this@MainActivity)))
            hint = "请输入每月额度（元）"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#9E9E9E"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        AlertDialog.Builder(this)
            .setTitle("设置每月额度")
            .setMessage("额度将用于首页每月消费进度条")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val v = input.text.toString().trim().toDoubleOrNull()
                if (v != null && v > 0) {
                    SettingsPrefs.setMonthlyQuota(this, v)
                    refreshMonthlyQuota()
                } else {
                    Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutDialog() =
        showScrollableTextDialog(R.string.about_title, R.string.about_usage)

    private fun showGuideDialog() =
        showScrollableTextDialog(R.string.guide_title, R.string.guide_usage)

    private fun showScrollableTextDialog(titleRes: Int, textRes: Int) {
        val tv = TextView(this).apply {
            text = getString(textRes)
            setTextColor(Color.BLACK)
            textSize = 15f
            setLineSpacing(dp(6).toFloat(), 1.0f)
            setPadding(dp(20), dp(16), dp(20), dp(16))
            isVerticalScrollBarEnabled = true
            movementMethod = ScrollingMovementMethod.getInstance()
        }
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(tv)
            .setPositiveButton(R.string.cancel, null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_test_popup -> {
                startActivity(Intent(this, OverlayActivity::class.java))
                true
            }
            R.id.action_set_quota -> {
                showQuotaDialog()
                true
            }
            R.id.action_set_monthly_quota -> {
                showMonthlyQuotaDialog()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_guide -> {
                showGuideDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
