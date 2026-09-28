package com.neo.dialer

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class DialpadFragment : Fragment() {
    private lateinit var display: TextView
    private val sb = StringBuilder()

    private val letters = mapOf(
        '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL",
        '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '0' to "+"
    )

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(ctx.dp(16), ctx.dp(24), ctx.dp(16), ctx.dp(8))
        }
        display = TextView(ctx).apply { textSize = 34f; gravity = Gravity.CENTER; maxLines = 1 }
        root.addView(display, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(90)))

        val grid = GridLayout(ctx).apply { columnCount = 3 }
        fun cell(v: View) {
            grid.addView(v, GridLayout.LayoutParams().apply {
                width = ctx.dp(84); height = ctx.dp(84); setMargins(ctx.dp(8), ctx.dp(6), ctx.dp(8), ctx.dp(6))
            })
        }
        for (k in "123456789*0#") {
            val text = SpannableStringBuilder(k.toString())
            letters[k]?.let {
                text.append("\n"); val st = text.length; text.append(it)
                text.setSpan(RelativeSizeSpan(0.35f), st, text.length, 0)
            }
            val b = TextView(ctx).apply {
                this.text = text; textSize = 28f; gravity = Gravity.CENTER
                background = circle(0x22888888)
                setOnClickListener { add(k) }
                if (k == '0') setOnLongClickListener { add('+'); true }
            }
            cell(b)
        }
        cell(View(ctx))
        cell(TextView(ctx).apply {
            text = "Call"; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); background = circle(0xFF34A853.toInt())
            setOnClickListener { ctx.placeCall(sb.toString()) }
        })
        cell(TextView(ctx).apply {
            text = "⌫"; textSize = 26f; gravity = Gravity.CENTER
            setOnClickListener { if (sb.isNotEmpty()) { sb.deleteCharAt(sb.length - 1); show() } }
            setOnLongClickListener { sb.clear(); show(); true }
        })
        root.addView(grid)
        return root
    }

    override fun onResume() {
        super.onResume()
        Shared.pendingNumber?.let { sb.clear(); sb.append(it); Shared.pendingNumber = null }
        show()
    }

    private fun add(ch: Char) { sb.append(ch); show() }
    private fun show() { display.text = sb.toString() }
}
