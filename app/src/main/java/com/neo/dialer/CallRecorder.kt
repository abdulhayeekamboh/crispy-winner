package com.neo.dialer

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null
    val isRecording get() = rec != null

    fun start(label: String): Boolean {
        if (!ctx.hasPerm(Manifest.permission.RECORD_AUDIO)) return false
        val dir = ctx.getExternalFilesDir("Recordings") ?: return false
        dir.mkdirs()
        val safe = label.replace(Regex("[^0-9A-Za-z+]"), "_")
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "${safe}_$stamp.m4a")
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )
        for (src in sources) {
            val r = MediaRecorder()
            try {
                r.setAudioSource(src)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioEncodingBitRate(96000)
                r.setAudioSamplingRate(44100)
                r.setOutputFile(f.absolutePath)
                r.prepare()
                r.start()
                rec = r
                file = f
                return true
            } catch (e: Exception) {
                try { r.release() } catch (_: Exception) {}
            }
        }
        return false
    }

    fun stop() {
        try { rec?.stop() } catch (_: Exception) { file?.delete() }
        try { rec?.release() } catch (_: Exception) {}
        rec = null
    }
}
