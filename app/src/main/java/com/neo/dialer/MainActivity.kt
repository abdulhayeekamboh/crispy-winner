package com.neo.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            requestDefaultDialer()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val nav = BottomNavigationView(this).apply { inflateMenu(R.menu.bottom_nav) }
        root.addView(container)
        root.addView(nav, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(root)

        nav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_recents -> RecentsFragment()
                R.id.nav_contacts -> ContactsFragment()
                R.id.nav_recordings -> RecordingsFragment()
                else -> DialpadFragment()
            }
            supportFragmentManager.beginTransaction().replace(container.id, f).commit()
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_keypad
        handleIntent(intent)

        permLauncher.launch(arrayOf(
            Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.RECORD_AUDIO
        ))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
        recreate()
    }

    private fun handleIntent(i: Intent?) {
        val d = i?.data
        if (d != null && d.scheme == "tel") Shared.pendingNumber = d.schemeSpecificPart
    }

    private fun requestDefaultDialer() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER))
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            } else {
                val tm = getSystemService(TelecomManager::class.java)
                if (tm.defaultDialerPackage != packageName)
                    roleLauncher.launch(
                        Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                    )
            }
        } catch (_: Exception) {}
    }
}
