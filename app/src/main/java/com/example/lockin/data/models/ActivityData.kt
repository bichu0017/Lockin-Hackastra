package com.example.lockin.data.models

data class ActivityData(
    val tabSwitches: Int,
    val scrollSpeed: Int,
    val idleTime: Int,
    val continuousAppUsageSeconds: Long,
    val continuousScrollSeconds: Long,
    val scrollCount: Int,
    val simultaneousAppSwitches: Int,
    val isInLockIn: Boolean = false
)
