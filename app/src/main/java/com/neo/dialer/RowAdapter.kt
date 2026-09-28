package com.neo.dialer

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Row(val title: String, val subtitle: String, val key: String, val red: Boolean = false)

class RowAdapter(
    private val onClick: (Row) -> Unit,
    private val onLong: ((Row) -> Unit)? = null
) : RecyclerView.Adapter<RowAdapter.VH>() {

    var items: List<Row> = emptyList()
        set(v) { field = v; notifyDataSetChanged() }

    class VH(val root: LinearLayout, val t: TextView, val s: TextView) : RecyclerView.ViewHolder(root) {
        val defColor = t.currentTextColor
    }

    override fun onCreateViewHolder(p: ViewGroup, viewType: Int): VH {
        val c = p.context
        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(c.dp(20), c.dp(12), c.dp(20), c.dp(12))
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val tv = TypedValue()
        c.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        root.setBackgroundResource(tv.resourceId)
        val t = TextView(c).apply { textSize = 17f }
        val s = TextView(c).apply { textSize = 13f; alpha = 0.65f }
        root.addView(t); root.addView(s)
        return VH(root, t, s)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val r = items[i]
        h.t.text = r.title
        h.s.text = r.subtitle
        h.t.setTextColor(if (r.red) 0xFFE53935.toInt() else h.defColor)
        h.root.setOnClickListener { onClick(r) }
        h.root.setOnLongClickListener { onLong?.invoke(r); onLong != null }
    }

    override fun getItemCount() = items.size
}
