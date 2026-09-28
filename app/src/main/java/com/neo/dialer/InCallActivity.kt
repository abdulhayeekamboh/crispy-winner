package com.neo.dialer

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class InCallActivity : AppCompatActivity() {

    private lateinit var nameV: TextView
    private lateinit var numberV: TextView
    private lateinit var stateV: TextView
    private lateinit var muteB: TextView
    private lateinit var speakerB: TextView
    private lateinit var recordB: TextView
    private lateinit var holdB: TextView
    private lateinit var answerRow: LinearLayout
    private lateinit var endRow: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var ending = false
    private var lastNumber: String? = null
    private var lastName = ""

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            if (!ending) handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = 0xFF101418.toInt()
        buildUi()
    }

    private fun chip(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 14f
        background = rounded(0xFF2A2F36.toInt(), dp(18).toFloat())
        setOnClickListener { onClick() }
    }

    private fun setOn(v: TextView, on: Boolean) {
        v.background = rounded(if (on) 0xFF34A853.toInt() else 0xFF2A2F36.toInt(), dp(18).toFloat())
    }

    private fun bigButton(label: String, color: Int, onClick: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 16f
        background = rounded(color, dp(32).toFloat())
        setOnClickListener { onClick() }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101418.toInt())
            setPadding(dp(20), dp(56), dp(20), dp(32))
        }
        nameV = TextView(this).apply { textSize = 32f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        numberV = TextView(this).apply { textSize = 16f; setTextColor(0xFFAAAAAA.toInt()); gravity = Gravity.CENTER }
        stateV = TextView(this).apply { textSize = 18f; setTextColor(0xFF34A853.toInt()); gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }
        root.addView(nameV, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(numberV, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(stateV, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(View(this), LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        muteB = chip("Mute") { toggleMute() }
        val keypadB = chip("Keypad") { showKeypad() }
        speakerB = chip("Speaker") { toggleSpeaker() }
        recordB = chip("Record") { toggleRecord() }
        holdB = chip("Hold") { toggleHold() }
        val addB = chip("Add call") {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        val grid = GridLayout(this).apply { columnCount = 3 }
        for (b in listOf(muteB, keypadB, speakerB, recordB, holdB, addB)) {
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply { width = 0; height = dp(84); setMargins(dp(6), dp(6), dp(6), dp(6)) }
            grid.addView(b, lp)
        }
        root.addView(grid, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(View(this), LinearLayout.LayoutParams(MATCH_PARENT, dp(24)))

        answerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val decline = bigButton("Decline", 0xFFE53935.toInt()) { CallManager.current?.reject(false, null) }
        val answer = bigButton("Answer", 0xFF34A853.toInt()) { CallManager.current?.answer(VideoProfile.STATE_AUDIO_ONLY) }
        answerRow.addView(decline, LinearLayout.LayoutParams(0, dp(64), 1f).apply { setMargins(dp(8), 0, dp(8), 0) })
        answerRow.addView(answer, LinearLayout.LayoutParams(0, dp(64), 1f).apply { setMargins(dp(8), 0, dp(8), 0) })
        root.addView(answerRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        endRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val end = bigButton("End call", 0xFFE53935.toInt()) { CallManager.current?.disconnect() }
        endRow.addView(end, LinearLayout.LayoutParams(dp(200), dp(64)))
        root.addView(endRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)
    }

    private fun toggleMute() {
        val s = CallManager.service ?: return
        s.setMuted(!(s.callAudioState?.isMuted ?: false))
    }

    private fun toggleSpeaker() {
        val s = CallManager.service ?: return
        val on = s.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
        s.setAudioRoute(if (on) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER)
    }

    private fun toggleHold() {
        val c = CallManager.current ?: return
        if (c.state == Call.STATE_HOLDING) c.unhold() else c.hold()
    }

    private fun toggleRecord() {
        if (CallManager.isRecording) {
            CallManager.stopRecording()
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
        } else {
            val label = lastName.ifEmpty { lastNumber ?: "call" }
            if (!CallManager.startRecording(this, label))
                Toast.makeText(this, "Could not start recording (check Microphone permission)", Toast.LENGTH_LONG).show()
        }
        refresh()
    }

    private fun showKeypad() {
        val call = CallManager.current ?: return
        val grid = GridLayout(this).apply { columnCount = 3; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        for (k in "123456789*0#") {
            val b = TextView(this).apply {
                text = k.toString(); textSize = 26f; gravity = Gravity.CENTER
                setOnClickListener {
                    call.playDtmfTone(k)
                    handler.postDelayed({ call.stopDtmfTone() }, 150)
                }
            }
            grid.addView(b, GridLayout.LayoutParams().apply { width = dp(80); height = dp(64) })
        }
        AlertDialog.Builder(this).setView(grid).setPositiveButton("Close", null).show()
    }

    private fun refresh() {
        val call = CallManager.current
        if (call == null) {
            if (!ending) { ending = true; handler.postDelayed({ finish() }, 600) }
            stateV.text = "Call ended"
            return
        }
        val n = call.details.handle?.schemeSpecificPart ?: ""
        if (n != lastNumber) { lastNumber = n; lastName = contactName(n) ?: "" }
        nameV.text = if (lastName.isNotEmpty()) lastName else if (n.isNotEmpty()) n else "Unknown"
        numberV.text = if (lastName.isNotEmpty()) n else ""

        val st = call.state
        stateV.text = when (st) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Call ended"
            Call.STATE_ACTIVE -> {
                val t = call.details.connectTimeMillis
                val sec = if (t > 0) ((System.currentTimeMillis() - t) / 1000).toInt() else 0
                String.format("%02d:%02d", sec / 60, sec % 60)
            }
            else -> ""
        }
        val ringing = st == Call.STATE_RINGING
        answerRow.visibility = if (ringing) View.VISIBLE else View.GONE
        endRow.visibility = if (ringing) View.GONE else View.VISIBLE

        val audio = CallManager.service?.callAudioState
        setOn(muteB, audio?.isMuted == true)
        setOn(speakerB, audio?.route == CallAudioState.ROUTE_SPEAKER)
        setOn(holdB, st == Call.STATE_HOLDING)
        setOn(recordB, CallManager.isRecording)
        recordB.text = if (CallManager.isRecording) "Recording ●" else "Record"
    }

    override fun onResume() {
        super.onResume()
        ending = false
        CallManager.listener = { refresh() }
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        CallManager.listener = null
        handler.removeCallbacks(tick)
    }
}
