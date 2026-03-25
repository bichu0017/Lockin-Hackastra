package com.example.lockin.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lockin.core.BehaviorClassifier
import com.example.lockin.core.FocusEngine
import com.example.lockin.core.InterventionController
import com.example.lockin.services.ModeManager
import com.example.lockin.services.ScoreManager
import com.example.lockin.services.TimerService
import com.example.lockin.services.TrackingService
import com.example.lockin.ui.widgets.ActivityCard
import com.example.lockin.ui.widgets.OverlayWidget
import com.example.lockin.ui.widgets.ScoreCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isOverlayGranted: Boolean,
    onToggleOverlayPermission: (Boolean) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { TrackingService(context) }
    val engine = remember { FocusEngine() }
    val classifier = remember { BehaviorClassifier() }
    val controller = remember { InterventionController() }
    val modeManager = remember { ModeManager() }
    val timer = remember { TimerService() }
    val scoreManager = remember { ScoreManager(context) }

    var score by remember { mutableDoubleStateOf(0.0) }
    var state by remember { mutableStateOf("Unknown") }
    var action by remember { mutableStateOf("NONE") }
    var currentMode by remember { mutableStateOf(modeManager.currentMode) }
    var expanded by remember { mutableStateOf(false) }

    // Daily Score state
    var dailyScore by remember { mutableIntStateOf(scoreManager.getScore()) }
    var hasDeductedForCurrentSession by remember { mutableStateOf(false) }

    // State for managing soft warning (1-minute usage)
    var hasTriggeredOneMinWarning by remember { mutableStateOf(false) }

    // State for managing the choice UI
    val sharedPrefs = remember { context.getSharedPreferences("lockin_prefs", Context.MODE_PRIVATE) }
    var showOverlayPrompt by remember { 
        mutableStateOf(!isOverlayGranted && !sharedPrefs.getBoolean("skip_overlay", false)) 
    }
    
    // Synchronize showOverlayPrompt if permission is granted externally
    LaunchedEffect(isOverlayGranted) {
        if (isOverlayGranted) {
            showOverlayPrompt = false
        }
    }
    
    val modes = listOf("Normal", "Strict", "Deep Work")
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(Unit) {
        timer.start {
            val data = tracker.generate()
            
            mainHandler.post {
                val newScore = engine.calculate(data)
                val newState = classifier.classify(newScore)
                val potentialAction = controller.action(newState, currentMode)
                
                // Independent condition 1: 30 seconds usage (Existing)
                val hasExceededTimeLimit = data.continuousAppUsageSeconds >= 30
                
                // NEW Independent condition 4: 1 minute usage (Soft Warning)
                val hasExceededOneMinLimit = data.continuousAppUsageSeconds >= 60

                // Independent condition 2: 15 seconds doomscrolling (time-based)
                val isDoomscrollingTime = data.continuousScrollSeconds >= 15
                // Independent condition 3: Doomscrolling (count-based - 7 consecutive items)
                val isDoomscrollingCount = data.scrollCount >= 7
                
                score = newScore
                state = newState

                // Reset flags if user is back in LockIn or idle
                if (data.isInLockIn || data.continuousAppUsageSeconds == 0L) {
                    hasTriggeredOneMinWarning = false
                }

                // Score Deduction Logic (Keep existing 30s threshold)
                if (hasExceededTimeLimit || isDoomscrollingCount) {
                    if (!hasDeductedForCurrentSession) {
                        scoreManager.deductPoints(10)
                        dailyScore = scoreManager.getScore()
                        hasDeductedForCurrentSession = true
                    }
                } else {
                    hasDeductedForCurrentSession = false
                }

                if (action == "NONE" && !data.isInLockIn) {
                    // Trigger Logic for All Conditions
                    if (hasExceededOneMinLimit && !hasTriggeredOneMinWarning) {
                         // NEW: 1-minute Soft Warning
                         if (Settings.canDrawOverlays(context)) {
                            action = "SOFT_WARN"
                            hasTriggeredOneMinWarning = true
                            
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            launchIntent?.let {
                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                context.startActivity(it)
                            }
                        }
                    } else if ((hasExceededTimeLimit || isDoomscrollingTime || isDoomscrollingCount) && potentialAction != "NONE") {
                        // Existing Logic (30s, Doomscroll)
                        if (Settings.canDrawOverlays(context)) {
                            action = potentialAction
                            
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            launchIntent?.let {
                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                context.startActivity(it)
                            }
                        }
                    }
                }
            }
        }
        onDispose {
            timer.stop()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "LockIn", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.List, contentDescription = "Analytics")
                    }
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.Info, contentDescription = "Dashboard")
                    }
                    IconButton(onClick = { 
                        dailyScore = scoreManager.getScore() 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScoreCard(
                score = dailyScore,
                classification = scoreManager.getClassification(dailyScore)
            )

            ActivityCard(state = state, score = score)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Focus Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = currentMode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Operational Mode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            modes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        currentMode = mode
                                        modeManager.setMode(mode)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overlay Permission",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isOverlayGranted) "Interventions active" else "Permission required",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = isOverlayGranted,
                            onCheckedChange = { onToggleOverlayPermission(it) }
                        )
                    }

                    if (showOverlayPrompt) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable overlays for better blocking.",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row {
                                    TextButton(onClick = { onToggleOverlayPermission(true) }) {
                                        Text("Enable")
                                    }
                                    TextButton(onClick = {
                                        sharedPrefs.edit().putBoolean("skip_overlay", true).apply()
                                        showOverlayPrompt = false
                                    }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (action != "NONE") {
            val data = tracker.generate()
            val overlayMessage = if (action == "SOFT_WARN") {
                "Take a quick break"
            } else if (data.scrollCount >= 7) {
                "You are out of focus"
            } else if (score == 0.0 && !data.isInLockIn && data.continuousScrollSeconds >= 15) {
                "You are not being productive"
            } else if (action == "BLOCK") {
                "STOP WASTING TIME"
            } else {
                "FOCUS"
            }

            OverlayWidget(
                message = overlayMessage,
                score = score,
                onDismiss = { 
                    action = "NONE" 
                    tracker.resetTimer()
                }
            )
        }
    }
}
