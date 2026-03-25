package com.example.lockin

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lockin.services.ScoreManager
import com.example.lockin.ui.screens.AnalyticsScreen
import com.example.lockin.ui.screens.DashboardScreen
import com.example.lockin.ui.screens.HomeScreen
import com.example.lockin.ui.theme.LockInTheme

class MainActivity : ComponentActivity() {
    
    private val usageStatsGrantedState = mutableStateOf(false)
    private val overlayGrantedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Reset score on launch
        ScoreManager(this).resetToInitial()
        
        refreshPermissions()
        
        setContent {
            LockInTheme {
                val usageStatsGranted by usageStatsGrantedState
                val overlayGranted by overlayGrantedState
                var currentScreen by remember { mutableStateOf("home") }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (usageStatsGranted) {
                        when (currentScreen) {
                            "home" -> HomeScreen(
                                isOverlayGranted = overlayGranted,
                                onToggleOverlayPermission = { requestedState ->
                                    handleOverlayToggle(requestedState)
                                },
                                onNavigateToDashboard = { currentScreen = "dashboard" },
                                onNavigateToAnalytics = { currentScreen = "analytics" }
                            )
                            "dashboard" -> DashboardScreen(
                                onBack = { currentScreen = "home" }
                            )
                            "analytics" -> AnalyticsScreen(
                                onBack = { currentScreen = "home" }
                            )
                        }
                    } else {
                        PermissionScreen {
                            refreshPermissions()
                        }
                    }
                }
            }
        }
    }

    private fun refreshPermissions() {
        usageStatsGrantedState.value = checkUsageStatsPermission()
        overlayGrantedState.value = Settings.canDrawOverlays(this)
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun handleOverlayToggle(requestedState: Boolean) {
        val isCurrentlyGranted = Settings.canDrawOverlays(this)
        
        if (requestedState && !isCurrentlyGranted) {
            // User wants to enable and it's not granted
            openOverlaySettings()
        } else if (!requestedState && isCurrentlyGranted) {
            // User wants to disable but it IS granted. 
            // We can't do it programmatically, so guide them.
            Toast.makeText(
                this, 
                "To disable this permission, turn it off in system settings.", 
                Toast.LENGTH_LONG
            ).show()
            openOverlaySettings()
        }
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback for some devices/OS versions
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    @Composable
    fun PermissionScreen(onRefresh: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LockIn needs Usage Access to monitor your focus and motivate you to stay productive.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Usage Access")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("I've granted permissions, Continue")
            }
        }
    }
}

// Extension to avoid unsafe check warning if possible, though unsafeCheckOpNoThrow is preferred for Q+
fun AppOpsManager.unsafeCheckNoThrow(op: String, uid: Int, packageName: String): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.unsafeCheckOpNoThrow(op, uid, packageName)
    } else {
        @Suppress("DEPRECATION")
        this.checkOpNoThrow(op, uid, packageName)
    }
}
