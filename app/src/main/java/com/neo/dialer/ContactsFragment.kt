package com.neo.dialer

import android.Manifest
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsFragment : Fragment() {
    private lateinit var rowAdapter: RowAdapter
    private lateinit var search: EditText
    private var all: List<Row> = emptyList()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val ctx = requireContext()
        rowAdapter = RowAdapter({ ctx.placeCall(it.key) })
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        search = EditText(ctx).apply {
            hint = "Search contacts"
            setPadding(ctx.dp(20), ctx.dp(16), ctx.dp(20), ctx.dp(16))
            doAfterTextChanged { applyFilter() }
        }
        root.addView(search, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = rowAdapter
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onResume() {
        super.onResume()
        val ctx = requireContext()
        if (!ctx.hasPerm(Manifest.permission.READ_CONTACTS)) return
        val rows = ArrayList<Row>()
        try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val n = c.getString(1) ?: continue
                    rows.add(Row(c.getString(0) ?: n, n, n))
                }
            }
        } catch (_: Exception) {}
        all = rows
        applyFilter()
    }

    private fun applyFilter() {
        val q = search.text.toString().trim()
        rowAdapter.items = if (q.isEmpty()) all
        else all.filter { it.title.contains(q, true) || it.subtitle.replace(" ", "").contains(q.replace(" ", "")) }
    }
}
