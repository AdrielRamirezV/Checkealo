package com.checkealo.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.checkealo.app.ui.MainScreen
import com.checkealo.app.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var hasNotificationPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermission()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainScreen(
                    viewModel = viewModel,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestPermission = {
                        openNotificationSettings()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        viewModel.clearTestStatus() // Reset any old MQTT/WA test outputs when coming back to screen
        
        // Force the system to rebind the notification listener service if it was suspended/killed in the background
        if (hasNotificationPermission) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(this, com.checkealo.app.service.NotificationService::class.java)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermission() {
        hasNotificationPermission = isNotificationListenerServiceEnabled(this)
    }

    private fun isNotificationListenerServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null) {
                    if (cn.packageName == pkgName) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback for older devices / different OS wrappers
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }
}
