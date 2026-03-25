package com.example.lockin.services

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.lockin.data.models.ActivityData
import java.util.*
import kotlin.random.Random

class TrackingService(private val context: Context) {
    private val random = Random

    companion object {
        private var lastAppPackage: String? = null
        private var lastAppStartTime: Long = 0
        
        // Doomscrolling tracking
        private var lastScrollTime: Long = 0
        private var scrollStartTime: Long = 0
        private var scrollCount: Int = 0
        private var isCurrentlyScrolling: Boolean = false
    }

    fun generate(): ActivityData {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        // Use a 1-minute window to find the most recent foreground app
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60000, now)
        
        // Find the app used most recently that isn't ours
        val currentApp = stats?.filter { it.packageName != context.packageName }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName

        if (currentApp != null) {
            val appStat = stats.find { it.packageName == currentApp }
            if (appStat != null && (now - appStat.lastTimeUsed) < 10000) {
                if (currentApp != lastAppPackage) {
                    lastAppPackage = currentApp
                    lastAppStartTime = now
                    // Reset scroll count when app switches
                    scrollCount = 0
                }
            }
        }

        // Check if we are currently in our own app
        val selfStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
            ?.find { it.packageName == context.packageName }
        
        val isInLockIn = selfStats != null && (now - selfStats.lastTimeUsed) < 2000

        val continuousSeconds = if (lastAppStartTime > 0) {
            (now - lastAppStartTime) / 1000
        } else {
            0L
        }

        // Simulate Scroll Detection
        // In a real app, this would come from an AccessibilityService
        val currentScrollSpeed = if (isInLockIn) 0 else random.nextInt(20)
        
        if (currentScrollSpeed > 5) { // Threshold for "active scrolling"
            if (now - lastScrollTime < 3000) { // No pause longer than 3 seconds
                if (scrollStartTime == 0L) {
                    scrollStartTime = now
                }
                
                // Simulate detecting a new "item" scroll
                if (!isCurrentlyScrolling) {
                    scrollCount++
                    isCurrentlyScrolling = true
                }
            } else {
                scrollStartTime = now // Reset if pause was too long
                scrollCount = 1
            }
            lastScrollTime = now
        } else {
            isCurrentlyScrolling = false
            if (now - lastScrollTime > 3000) {
                scrollStartTime = 0L // Reset if idle for > 3 seconds
                scrollCount = 0
            }
        }

        val scrollDurationSeconds = if (scrollStartTime > 0L) {
            (now - scrollStartTime) / 1000
        } else {
            0L
        }

        return ActivityData(
            tabSwitches = random.nextInt(10),
            scrollSpeed = currentScrollSpeed,
            idleTime = random.nextInt(10),
            continuousAppUsageSeconds = continuousSeconds,
            continuousScrollSeconds = scrollDurationSeconds,
            scrollCount = scrollCount,
            simultaneousAppSwitches = random.nextInt(5),
            isInLockIn = isInLockIn
        )
    }
    
    fun resetTimer() {
        lastAppPackage = null
        lastAppStartTime = 0
        scrollStartTime = 0
        lastScrollTime = 0
        scrollCount = 0
        isCurrentlyScrolling = false
    }
}
