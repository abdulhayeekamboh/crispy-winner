package com.neo.dialer

import android.Manifest
import android.os.Bundle
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentsFragment : Fragment() {
    private lateinit var rowAdapter: RowAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val ctx = requireContext()
        rowAdapter = RowAdapter({ ctx.placeCall(it.key) })
        return RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = rowAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = requireContext()
        if (!ctx.hasPerm(Manifest.permission.READ_CALL_LOG)) return
        val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
        val rows = ArrayList<Row>()
        try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC LIMIT 200"
            )?.use { c ->
                while (c.moveToNext()) {
                    val num = c.getString(0) ?: ""
                    val name = c.getString(1)
                    val type = when (c.getInt(2)) {
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Declined"
                        CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
                        else -> "Call"
                    }
                    val dur = c.getLong(4)
                    val sub = "$type · ${fmt.format(Date(c.getLong(3)))}" +
                        if (dur > 0) " · ${dur / 60}m ${dur % 60}s" else ""
                    rows.add(Row(if (name.isNullOrEmpty()) num.ifEmpty { "Unknown" } else name,
                        sub, num, c.getInt(2) == CallLog.Calls.MISSED_TYPE))
                }
            }
        } catch (_: Exception) {}
        rowAdapter.items = rows
    }
}
