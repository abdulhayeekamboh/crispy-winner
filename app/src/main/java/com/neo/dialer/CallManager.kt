package com.neo.dialer

import android.content.Context
import android.telecom.Call

object CallManager {
    var service: CallService? = null
    val calls = ArrayList<Call>()
    var listener: (() -> Unit)? = null
    var recorder: CallRecorder? = null

    val isRecording get() = recorder?.isRecording == true
    val current: Call?
        get() = calls.firstOrNull { it.state == Call.STATE_RINGING } ?: calls.lastOrNull()

    fun startRecording(ctx: Context, label: String): Boolean {
        if (isRecording) return true
        val r = CallRecorder(ctx.applicationContext)
        val ok = r.start(label)
        if (ok) recorder = r
        return ok
    }

    fun stopRecording() {
        recorder?.stop()
        recorder = null
    }

    fun notifyChanged() { listener?.invoke() }
}
