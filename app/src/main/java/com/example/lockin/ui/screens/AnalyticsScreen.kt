package com.example.lockin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lockin.services.ScoreManager
import com.example.lockin.services.UsageService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val usageService = remember { UsageService(context) }
    val scoreManager = remember { ScoreManager(context) }
    
    val usageStats = remember { usageService.getDailyUsage() }
    val currentScore = remember { scoreManager.getScore() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card 1: Screen Time Graph (Mocked Hourly Distribution)
            GraphCard(title = "Screen Time Today (Hourly)") {
                ScreenTimeBarChart()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: App Usage Distribution
            GraphCard(title = "App Usage Distribution") {
                AppUsagePieChart(usageStats.take(5))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Focus Score Trend (Mocked Trend)
            GraphCard(title = "Focus Score Trend") {
                ScoreTrendLineChart(currentScore)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GraphCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun ScreenTimeBarChart() {
    val data = listOf(20f, 45f, 30f, 80f, 60f, 95f, 40f) // Mocked hourly data
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2f)
        
        data.forEachIndexed { index, value ->
            val barHeight = (value / 100f) * height
            drawRect(
                color = primaryColor,
                topLeft = Offset(
                    x = (index * 2f + 0.5f) * barWidth,
                    y = height - barHeight
                ),
                size = Size(barWidth, barHeight)
            )
        }
        
        // Baseline
        drawLine(
            color = Color.Gray,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 2f
        )
    }
}

@Composable
fun AppUsagePieChart(stats: List<com.example.lockin.data.models.AppUsageInfo>) {
    val colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF9C27B0))
    val totalTime = stats.sumOf { it.usageTimeMillis }.toFloat()
    
    if (totalTime == 0f) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No usage data yet")
        }
        return
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = 0f
        stats.forEachIndexed { index, app ->
            val sweepAngle = (app.usageTimeMillis.toFloat() / totalTime) * 360f
            drawArc(
                color = colors.getOrElse(index) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension),
                topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun ScoreTrendLineChart(currentScore: Int) {
    val scores = listOf(100f, 90f, 80f, 85f, 70f, 60f, currentScore.toFloat())
    val trendColor = if (currentScore > 70) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val spacing = width / (scores.size - 1)
        
        val path = Path().apply {
            scores.forEachIndexed { index, score ->
                val x = index * spacing
                val y = height - (score / 100f * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = trendColor,
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Data points
        scores.forEachIndexed { index, score ->
            drawCircle(
                color = trendColor,
                radius = 4.dp.toPx(),
                center = Offset(index * spacing, height - (score / 100f * height))
            )
        }
    }
}
