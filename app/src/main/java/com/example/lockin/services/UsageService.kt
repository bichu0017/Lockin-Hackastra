package com.example.lockin.services

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.lockin.data.models.AppUsageInfo
import java.util.*

class UsageService(private val context: Context) {
    fun getDailyUsage(): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        return stats.filter { it.totalTimeInForeground > 0 }
            .map {
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(it.packageName, 0)).toString()
                } catch (e: Exception) {
                    it.packageName
                }
                AppUsageInfo(it.packageName, appName, it.totalTimeInForeground)
            }
            .sortedByDescending { it.usageTimeMillis }
    }
}
