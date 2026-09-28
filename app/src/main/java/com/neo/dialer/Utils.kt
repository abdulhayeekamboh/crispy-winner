package com.neo.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat

object Shared { var pendingNumber: String? = null }

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("neo", Context.MODE_PRIVATE)
    fun autoRecord(c: Context) = p(c).getBoolean("auto_record", false)
    fun setAutoRecord(c: Context, v: Boolean) = p(c).edit().putBoolean("auto_record", v).apply()
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.hasPerm(p: String) =
    ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

fun Context.placeCall(number: String) {
    if (number.isBlank()) return
    try {
        getSystemService(TelecomManager::class.java)
            .placeCall(Uri.fromParts("tel", number, null), Bundle())
    } catch (e: SecurityException) {
        Toast.makeText(this, "Allow the Phone permission first", Toast.LENGTH_LONG).show()
    }
}

fun Context.contactName(number: String): String? {
    if (number.isEmpty() || !hasPerm(Manifest.permission.READ_CONTACTS)) return null
    val uri = Uri.withAppendedPath(
        ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
    )
    return try {
        contentResolver.query(
            uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }
}

fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = radius
    setColor(color)
}

fun circle(color: Int) = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
}
