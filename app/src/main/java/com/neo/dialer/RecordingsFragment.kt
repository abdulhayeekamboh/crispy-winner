package com.neo.dialer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsFragment : Fragment() {
    private lateinit var rowAdapter: RowAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val ctx = requireContext()
        rowAdapter = RowAdapter(
            { open(File(it.key)) },
            { r ->
                AlertDialog.Builder(ctx).setTitle("Delete recording?")
                    .setPositiveButton("Delete") { _, _ -> File(r.key).delete(); load() }
                    .setNegativeButton("Cancel", null).show()
            })
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val sw = SwitchCompat(ctx).apply {
            text = "Auto-record all calls"
            isChecked = Prefs.autoRecord(ctx)
            setPadding(ctx.dp(20), ctx.dp(16), ctx.dp(20), ctx.dp(4))
            setOnCheckedChangeListener { _, v -> Prefs.setAutoRecord(ctx, v) }
        }
        val note = TextView(ctx).apply {
            text = "Android 10/11 blocks apps from capturing the other person directly. " +
                "Turn on speaker for the other side to be recorded. Long-press a file to delete."
            textSize = 12f; alpha = 0.6f
            setPadding(ctx.dp(20), 0, ctx.dp(20), ctx.dp(12))
        }
        root.addView(sw); root.addView(note)
        root.addView(RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = rowAdapter
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val dir = requireContext().getExternalFilesDir("Recordings")
        val fmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        rowAdapter.items = (dir?.listFiles() ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .map { Row(it.nameWithoutExtension, "${fmt.format(Date(it.lastModified()))} · ${it.length() / 1024} KB", it.absolutePath) }
    }

    private fun open(f: File) {
        val ctx = requireContext()
        try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "audio/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: Exception) {
            Toast.makeText(ctx, "No audio player found", Toast.LENGTH_SHORT).show()
        }
    }
}
