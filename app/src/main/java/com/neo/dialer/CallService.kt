package com.neo.dialer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.app.NotificationCompat

class CallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state != Call.STATE_RINGING) cancelNotification()
            if (state == Call.STATE_ACTIVE) autoRecord(call)
            CallManager.notifyChanged()
        }
    }

    override fun onCallAdded(call: Call) {
        CallManager.service = this
        CallManager.calls.add(call)
        call.registerCallback(callback)
        if (call.state == Call.STATE_RINGING) showIncoming(call) else launchUi()
        if (call.state == Call.STATE_ACTIVE) autoRecord(call)
        CallManager.notifyChanged()
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        CallManager.calls.remove(call)
        cancelNotification()
        if (CallManager.calls.isEmpty()) CallManager.stopRecording()
        CallManager.notifyChanged()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallManager.notifyChanged()
    }

    override fun onDestroy() {
        CallManager.stopRecording()
        CallManager.service = null
        super.onDestroy()
    }

    private fun number(call: Call) = call.details.handle?.schemeSpecificPart ?: ""

    private fun autoRecord(call: Call) {
        if (Prefs.autoRecord(this) && !CallManager.isRecording) {
            val n = number(call)
            CallManager.startRecording(this, contactName(n) ?: n.ifEmpty { "unknown" })
        }
    }

    private fun launchUi() {
        try {
            startActivity(Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    private fun showIncoming(call: Call) {
        val n = number(call)
        val name = contactName(n) ?: n.ifEmpty { "Unknown" }
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("calls", "Incoming calls", NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(null, null)
            nm.createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, "calls")
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(name)
            .setContentText("Incoming call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        nm.notify(1, notif)
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java).cancel(1)
    }
}
