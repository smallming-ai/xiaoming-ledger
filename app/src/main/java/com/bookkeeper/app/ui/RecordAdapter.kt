package com.bookkeeper.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bookkeeper.app.data.CatView
import com.bookkeeper.app.data.ExpenseRecord
import com.bookkeeper.app.databinding.ItemRecordBinding
import com.bookkeeper.app.util.Formatters

/** 账目列表适配器。支持点击删除按钮或长按删除。支持自定义支出类型。 */
class RecordAdapter(
    private val onDelete: (ExpenseRecord) -> Unit
) : RecyclerView.Adapter<RecordAdapter.VH>() {

    private val items = mutableListOf<ExpenseRecord>()
    private var catMap: Map<Int, CatView> = emptyMap()

    fun submit(list: List<ExpenseRecord>, categories: Map<Int, CatView>) {
        items.clear()
        items.addAll(list)
        catMap = categories
        notifyDataSetChanged()
    }

    class VH(val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val record = items[position]
        val cat = catMap[record.categoryId] ?: CatView(record.categoryId, "其他", "其他", null, 0xFF9E9E9E.toInt())
        val ctx = holder.itemView.context
        holder.binding.apply {
            categoryName.text = cat.name
            dot.backgroundTintList = ColorStateList.valueOf(cat.color(ctx))
            amount.text = "¥${Formatters.amount(record.amount)}"
            time.text = Formatters.itemTime(record.timestamp)
            note.text = record.note
            note.visibility = if (record.note.isNullOrBlank()) View.GONE else View.VISIBLE
            root.setOnLongClickListener {
                onDelete(record)
                true
            }
            btnDelete.setOnClickListener { onDelete(record) }
        }
    }
}
